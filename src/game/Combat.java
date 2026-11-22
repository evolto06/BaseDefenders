package game;

import components.Blinker;
import components.Enemy;
import components.Health;
import components.Position;
import components.Status;
import components.Tower;
import ecs.Store;
import ecs.World;
import java.awt.Color;

/**
 * The single place damage, healing and debuffs are applied, so armor, wards, immunities,
 * shields, burrows and hexes stay consistent.
 */
public final class Combat {
    private static final float MIN_PHYSICAL = 0.2f;
    /** Hits smaller than this share of max health (damage-over-time ticks) do not flash. */
    private static final float FLASH_THRESHOLD = 0.02f;
    /** How much of a slow, and of a stun, fear or hex's duration, lands on a boss. */
    private static final float BOSS_SLOW = 0.5f, BOSS_STUN = 0.3f, BOSS_HEX = 0.5f;
    /** After fleeing, a monster cannot be frightened again for this long. */
    private static final float FEAR_GUARD = 4;
    /** A broken shield swallows the rest of the hit: anything else it carries this step. */
    private static final float SHIELD_BREAK = 0.01f;
    /** A Blink Imp blinks away from a single hit of at least this share of its health. */
    private static final float BLINK_HIT = 0.2f;
    /** Share of its health a Yeti regains from a chill, and how often the cold can do that. */
    private static final float COLD_MEND = 0.04f, COLD_GUARD = 0.5f;
    /** A Salamander keeps this much of a freeze and of a slow, and bolts this much faster after a freeze. */
    private static final float SALAMANDER_THAW = 0.25f, SALAMANDER_SLOW = 0.5f;
    private static final float FURY = 0.6f, FURY_SECONDS = 3;
    private static final Color SHIELD = new Color(0xFDE68A), COLD_MEND_RING = new Color(0xE0F2FE);
    private static final Color FURY_SPARK = new Color(0xFB923C);

    private final Store<Health> healths;
    private final Store<Status> statuses;
    private final Store<Enemy> enemies;
    private final Store<Tower> towers;
    private final Store<Blinker> blinkers;
    private final Store<Position> positions;
    private final Prefabs prefabs;
    /** True while a damage-over-time tick is applied: ticks do not break shields or set off blinks. */
    private boolean ticking;
    /** Running total of health removed, for balance testing. */
    public double totalDamage;
    /**
     * The tower credited with damage dealt from now on, or {@link World#NONE}. Systems set this
     * around the attack they are resolving.
     */
    public int source = World.NONE;

    public Combat(World world, Prefabs prefabs) {
        healths = world.store(Health.class);
        statuses = world.store(Status.class);
        enemies = world.store(Enemy.class);
        towers = world.store(Tower.class);
        blinkers = world.store(Blinker.class);
        positions = world.store(Position.class);
        this.prefabs = prefabs;
    }

    public void damage(int entity, float amount, DamageType type) {
        Health h = healths.get(entity);
        if (h == null || h.hp <= 0) return;
        Status s = statuses.get(entity);
        Enemy enemy = enemies.get(entity);
        if (s != null && s.burrowTime > 0) return;
        if (s != null && s.shield && !ticking) {
            // The shield takes the whole hit, and whatever else rides on it, then breaks.
            s.shield = false;
            s.shieldBreak = SHIELD_BREAK;
            Position p = positions.get(entity);
            if (p != null) {
                prefabs.ring(p.x, p.y, 22, SHIELD, 0.3f);
                prefabs.burst(p.x, p.y, SHIELD, 4);
            }
            return;
        }
        if (type == DamageType.PHYSICAL && (s == null || s.shredTime <= 0)) {
            amount = Math.max(amount * MIN_PHYSICAL, amount - h.armor);
        }
        if (type == DamageType.MAGIC && enemy != null && !hexed(s)) amount *= 1 - enemy.type.ward();
        if (s != null && s.brittleTime > 0) amount *= 1 + s.brittle;
        if (!ticking && amount >= h.max * BLINK_HIT && amount < h.hp) readyBlink(entity, s);
        float dealt = Math.min(amount, h.hp);
        totalDamage += dealt;
        Tower tower = towers.get(source);
        if (tower != null) {
            tower.damageDealt += dealt;
            h.lastHitBy = source;
        }
        h.hp -= amount;
        if (amount >= h.max * FLASH_THRESHOLD && enemy != null) enemy.flash = 1;
    }

    /** Damage over time (burn, poison, fields, rot): it does not break shields or set off blinks. */
    public void tick(int entity, float amount, DamageType type) {
        ticking = true;
        damage(entity, amount, type);
        ticking = false;
    }

    /**
     * Restores health, unless the monster is hexed; a rotting monster is hurt by the same amount
     * instead. Returns whether anything happened.
     */
    public boolean heal(int entity, float amount) {
        Health h = healths.get(entity);
        if (h == null || h.hp <= 0) return false;
        Status s = statuses.get(entity);
        if (s != null && s.rotTime > 0) {
            int credited = source;
            source = s.rotOwner;
            tick(entity, amount, DamageType.MAGIC);
            source = credited;
            return true;
        }
        if (hexed(s) || h.hp >= h.max) return false;
        h.hp = Math.min(h.max, h.hp + amount);
        return true;
    }

    /** Whether the entity is hexed: unable to heal, be healed or use its powers. */
    public boolean hexed(int entity) {
        return hexed(statuses.get(entity));
    }

    private static boolean hexed(Status s) {
        return s != null && s.hexTime > 0;
    }

    /** Whether slows, stuns and fear slide off the entity right now. */
    private boolean unshakable(Enemy enemy, Status s) {
        return enemy.type.immune && !hexed(s);
    }

    /** Whether nothing can be laid on the monster now: it is underground, or a shield just broke. */
    private static boolean untouchable(Status s) {
        return s == null || s.burrowTime > 0 || s.shieldBreak > 0;
    }

    public void slow(int entity, float fraction, float seconds) {
        Status s = statuses.get(entity);
        Enemy enemy = enemies.get(entity);
        if (untouchable(s) || enemy == null || unshakable(enemy, s)) return;
        if (!hexed(s)) {
            if (enemy.type == EnemyType.YETI) {
                mendFromCold(entity, s);
                return;
            }
            if (enemy.type == EnemyType.SALAMANDER) fraction *= SALAMANDER_SLOW;
        }
        if (enemy.type.boss()) fraction *= BOSS_SLOW;
        s.slow = Math.max(s.slow, fraction);
        s.slowTime = Math.max(s.slowTime, seconds);
    }

    /** Freezes the monster in place. */
    public void stun(int entity, float seconds) {
        Status s = statuses.get(entity);
        Enemy enemy = enemies.get(entity);
        if (untouchable(s) || enemy == null || unshakable(enemy, s)) return;
        if (!hexed(s)) {
            switch (enemy.type) {
                case YETI -> {
                    mendFromCold(entity, s);
                    return;
                }
                case SALAMANDER -> {
                    // Its fire thaws the ice fast, and leaves it furious.
                    seconds *= SALAMANDER_THAW;
                    haste(entity, FURY, FURY_SECONDS);
                    Position p = positions.get(entity);
                    if (p != null) prefabs.burst(p.x, p.y, FURY_SPARK, 4);
                }
                case IMP -> readyBlink(entity, s);
                default -> { }
            }
        }
        if (enemy.type.boss()) seconds *= BOSS_STUN;
        s.stunTime = Math.max(s.stunTime, seconds);
    }

    /** A Yeti is at home in the cold: a chill mends it a little, at most every {@link #COLD_GUARD} seconds. */
    private void mendFromCold(int entity, Status s) {
        if (s.coldGuard > 0) return;
        s.coldGuard = COLD_GUARD;
        Health h = healths.get(entity);
        if (!heal(entity, h.max * COLD_MEND)) return;
        Position p = positions.get(entity);
        if (p != null) prefabs.ring(p.x, p.y, 18, COLD_MEND_RING, 0.35f);
    }

    /** Readies a Blink Imp to blink away at the start of the next step, if it has recharged. */
    private void readyBlink(int entity, Status s) {
        Blinker b = blinkers.get(entity);
        if (b != null && b.timer <= 0 && !hexed(s)) b.pending = true;
    }

    /** Sends the monster fleeing back down the road, unless it only just stopped running. */
    public void fear(int entity, float seconds) {
        Status s = statuses.get(entity);
        Enemy enemy = enemies.get(entity);
        if (untouchable(s) || enemy == null || s.fearGuard > 0 || unshakable(enemy, s)) return;
        if (enemy.type.boss()) seconds *= BOSS_STUN;
        s.fearTime = seconds;
        s.fearGuard = seconds + FEAR_GUARD;
    }

    public void burn(int entity, float dps, float seconds) {
        Status s = statuses.get(entity);
        Enemy enemy = enemies.get(entity);
        // A Salamander is fireproof.
        if (untouchable(s) || enemy == null || enemy.type == EnemyType.SALAMANDER) return;
        s.burnDps = Math.max(s.burnDps, dps);
        s.burnTime = Math.max(s.burnTime, seconds);
        s.burnOwner = source;
    }

    public void poison(int entity, float dps, float seconds, boolean contagious) {
        Status s = statuses.get(entity);
        if (untouchable(s)) return;
        s.poisonDps = Math.max(s.poisonDps, dps);
        s.poisonTime = Math.max(s.poisonTime, seconds);
        s.contagious |= contagious;
        s.poisonOwner = source;
    }

    public void brittle(int entity, float fraction, float seconds) {
        Status s = statuses.get(entity);
        if (untouchable(s)) return;
        s.brittle = Math.max(s.brittle, fraction);
        s.brittleTime = Math.max(s.brittleTime, seconds);
    }

    /** Strips armor for a while. */
    public void shred(int entity, float seconds) {
        Status s = statuses.get(entity);
        if (!untouchable(s)) s.shredTime = Math.max(s.shredTime, seconds);
    }

    /** Silences the monster's powers and stops it being healed. Bosses shake it off sooner. */
    public void hex(int entity, float seconds) {
        Status s = statuses.get(entity);
        Enemy enemy = enemies.get(entity);
        if (untouchable(s) || enemy == null) return;
        if (enemy.type.boss()) seconds *= BOSS_HEX;
        s.hexTime = Math.max(s.hexTime, seconds);
    }

    /** Turns the healing the monster receives into damage. */
    public void rot(int entity, float seconds) {
        Status s = statuses.get(entity);
        if (untouchable(s)) return;
        s.rotTime = Math.max(s.rotTime, seconds);
        s.rotOwner = source;
    }

    /** Makes the monster explode for a share of its maximum health if it dies in time. */
    public void doom(int entity, float fraction, float seconds) {
        Status s = statuses.get(entity);
        if (untouchable(s)) return;
        s.doom = Math.max(s.doom, fraction);
        s.doomTime = Math.max(s.doomTime, seconds);
        s.doomOwner = source;
    }

    public void haste(int entity, float fraction, float seconds) {
        Status s = statuses.get(entity);
        if (s == null) return;
        s.haste = Math.max(s.haste, fraction);
        s.hasteTime = Math.max(s.hasteTime, seconds);
    }
}
