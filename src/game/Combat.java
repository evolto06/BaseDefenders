package game;

import components.Enemy;
import components.Health;
import components.Status;
import components.Tower;
import ecs.Store;
import ecs.World;

/** The single place damage and debuffs are applied, so armor and immunities stay consistent. */
public final class Combat {
    private static final float MIN_PHYSICAL = 0.2f;
    /** Hits smaller than this share of max health (damage-over-time ticks) do not flash. */
    private static final float FLASH_THRESHOLD = 0.02f;

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
        if (type == DamageType.PHYSICAL && (s == null || s.shredTime <= 0)) {
            amount = Math.max(amount * MIN_PHYSICAL, amount - h.armor);
        }
        if (s != null && s.brittleTime > 0) amount *= 1 + s.brittle;
        float dealt = Math.min(amount, h.hp);
        totalDamage += dealt;
        Tower tower = towers.get(source);
        if (tower != null) {
            tower.damageDealt += dealt;
            h.lastHitBy = source;
        }
        h.hp -= amount;
        if (amount >= h.max * FLASH_THRESHOLD) {
            Enemy enemy = enemies.get(entity);
            if (enemy != null) enemy.flash = 1;
        }
    }

    public void slow(int entity, float fraction, float seconds) {
        Status s = statuses.get(entity);
        Enemy enemy = enemies.get(entity);
        if (s == null || enemy == null || enemy.type.immune) return;
        if (enemy.type.boss()) fraction *= 0.5f;
        s.slow = Math.max(s.slow, fraction);
        s.slowTime = Math.max(s.slowTime, seconds);
    }

    public void stun(int entity, float seconds) {
        Status s = statuses.get(entity);
        Enemy enemy = enemies.get(entity);
        if (s == null || enemy == null || enemy.type.immune) return;
        if (enemy.type.boss()) seconds *= 0.3f;
        s.stunTime = Math.max(s.stunTime, seconds);
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
}
