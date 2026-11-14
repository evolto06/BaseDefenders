package game;

import components.Enemy;
import components.Health;
import components.Status;
import components.Tower;
import ecs.Store;
import ecs.World;

/**
 * The single place damage, healing and debuffs are applied, so armor, wards, immunities and
 * hexes stay consistent.
 */
public final class Combat {
    private static final float MIN_PHYSICAL = 0.2f;
    /** Hits smaller than this share of max health (damage-over-time ticks) do not flash. */
    private static final float FLASH_THRESHOLD = 0.02f;
    /** How much of a slow, and of a stun, fear or hex's duration, lands on a boss. */
    private static final float BOSS_SLOW = 0.5f, BOSS_STUN = 0.3f, BOSS_HEX = 0.5f;
    /** After fleeing, a monster cannot be frightened again for this long. */
    private static final float FEAR_GUARD = 4;

    private final Store<Health> healths;
    private final Store<Status> statuses;
    private final Store<Enemy> enemies;
    private final Store<Tower> towers;
    /** Running total of health removed, for balance testing. */
    public double totalDamage;
    /**
     * The tower credited with damage dealt from now on, or {@link World#NONE}. Systems set this
     * around the attack they are resolving.
     */
    public int source = World.NONE;

    public Combat(World world) {
        healths = world.store(Health.class);
        statuses = world.store(Status.class);
        enemies = world.store(Enemy.class);
        towers = world.store(Tower.class);
    }

    public void damage(int entity, float amount, DamageType type) {
        Health h = healths.get(entity);
        if (h == null || h.hp <= 0) return;
        Status s = statuses.get(entity);
        Enemy enemy = enemies.get(entity);
        if (type == DamageType.PHYSICAL && (s == null || s.shredTime <= 0)) {
            amount = Math.max(amount * MIN_PHYSICAL, amount - h.armor);
        }
        if (type == DamageType.MAGIC && enemy != null && !hexed(s)) amount *= 1 - enemy.type.ward();
        if (s != null && s.brittleTime > 0) amount *= 1 + s.brittle;
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
            damage(entity, amount, DamageType.MAGIC);
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

    public void slow(int entity, float fraction, float seconds) {
        Status s = statuses.get(entity);
        Enemy enemy = enemies.get(entity);
        if (s == null || enemy == null || unshakable(enemy, s)) return;
        if (enemy.type.boss()) fraction *= BOSS_SLOW;
        s.slow = Math.max(s.slow, fraction);
        s.slowTime = Math.max(s.slowTime, seconds);
    }

    public void stun(int entity, float seconds) {
        Status s = statuses.get(entity);
        Enemy enemy = enemies.get(entity);
        if (s == null || enemy == null || unshakable(enemy, s)) return;
        if (enemy.type.boss()) seconds *= BOSS_STUN;
        s.stunTime = Math.max(s.stunTime, seconds);
    }

    /** Sends the monster fleeing back down the road, unless it only just stopped running. */
    public void fear(int entity, float seconds) {
        Status s = statuses.get(entity);
        Enemy enemy = enemies.get(entity);
        if (s == null || enemy == null || s.fearGuard > 0 || unshakable(enemy, s)) return;
        if (enemy.type.boss()) seconds *= BOSS_STUN;
        s.fearTime = seconds;
        s.fearGuard = seconds + FEAR_GUARD;
    }

    public void burn(int entity, float dps, float seconds) {
        Status s = statuses.get(entity);
        if (s == null) return;
        s.burnDps = Math.max(s.burnDps, dps);
        s.burnTime = Math.max(s.burnTime, seconds);
        s.burnOwner = source;
    }

    public void poison(int entity, float dps, float seconds, boolean contagious) {
        Status s = statuses.get(entity);
        if (s == null) return;
        s.poisonDps = Math.max(s.poisonDps, dps);
        s.poisonTime = Math.max(s.poisonTime, seconds);
        s.contagious |= contagious;
        s.poisonOwner = source;
    }

    public void brittle(int entity, float fraction, float seconds) {
        Status s = statuses.get(entity);
        if (s == null) return;
        s.brittle = Math.max(s.brittle, fraction);
        s.brittleTime = Math.max(s.brittleTime, seconds);
    }

    /** Strips armor for a while. */
    public void shred(int entity, float seconds) {
        Status s = statuses.get(entity);
        if (s != null) s.shredTime = Math.max(s.shredTime, seconds);
    }

    /** Silences the monster's powers and stops it being healed. Bosses shake it off sooner. */
    public void hex(int entity, float seconds) {
        Status s = statuses.get(entity);
        Enemy enemy = enemies.get(entity);
        if (s == null || enemy == null) return;
        if (enemy.type.boss()) seconds *= BOSS_HEX;
        s.hexTime = Math.max(s.hexTime, seconds);
    }

    /** Turns the healing the monster receives into damage. */
    public void rot(int entity, float seconds) {
        Status s = statuses.get(entity);
        if (s == null) return;
        s.rotTime = Math.max(s.rotTime, seconds);
        s.rotOwner = source;
    }

    /** Makes the monster explode for a share of its maximum health if it dies in time. */
    public void doom(int entity, float fraction, float seconds) {
        Status s = statuses.get(entity);
        if (s == null) return;
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
