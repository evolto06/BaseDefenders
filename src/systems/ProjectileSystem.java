package systems;

import components.Enemy;
import components.Health;
import components.Position;
import components.Projectile;
import components.Status;
import ecs.GameSystem;
import ecs.Query;
import ecs.Store;
import ecs.World;
import game.Game;
import game.TowerType;
import java.awt.Color;

/** Flies shots to their targets and resolves what happens on impact. */
public final class ProjectileSystem implements GameSystem {
    private static final float FLAK_AIR_BONUS = 1.5f;
    private static final float BURN_SECONDS = 3;
    private static final float ZONE_SECONDS = 3.5f;

    private static final Color WOOD = new Color(0xD9B77A);
    private static final Color FIRE = new Color(0xFDBA4D);
    private static final Color ICE = new Color(0xBAE6FD);
    private static final Color TOXIC = new Color(0xA3E635);
    private static final Color PLASMA = new Color(0xE879F9);
    private static final Color SUN = new Color(0xFDBA74);
    private static final Color HEX = new Color(0xC084FC), ROT = new Color(0xA16207), SILVER = new Color(0xE5E7EB);
    private static final float BOSS_PERCENT_FACTOR = 0.25f;
    private static final float BOMBLET_RADIUS = 0.7f, BOMBLET_DAMAGE = 0.6f;

    private final Game game;
    private final Query query;
    private final Store<Projectile> projectiles;
    private final Store<Position> positions;
    private final Store<Health> healths;
    private final Store<Enemy> enemies;
    private final Store<Status> statuses;

    public ProjectileSystem(Game game) {
        this.game = game;
        this.query = game.world.query(Projectile.class, Position.class);
        this.projectiles = game.world.store(Projectile.class);
        this.positions = game.world.store(Position.class);
        this.healths = game.world.store(Health.class);
        this.enemies = game.world.store(Enemy.class);
        this.statuses = game.world.store(Status.class);
    }

    @Override
    public void update(float dt) {
        query.fetch();
        for (int i = 0; i < query.size(); i++) {
            int e = query.get(i);
            Projectile pr = projectiles.get(e);
            Position p = positions.get(e);
            if (pr.lobbed ? flyArc(pr, p, dt) : flyHoming(pr, p, dt)) {
                game.combat.source = pr.owner;
                impact(pr, pr.tx, pr.ty);
                game.combat.source = World.NONE;
                game.world.destroy(e);
            }
        }
    }

    /** Returns true on arrival. */
    private boolean flyArc(Projectile pr, Position p, float dt) {
        pr.progress = Math.min(1, pr.progress + dt / pr.duration);
        p.x = pr.sx + (pr.tx - pr.sx) * pr.progress;
        p.y = pr.sy + (pr.ty - pr.sy) * pr.progress;
        return pr.progress >= 1;
    }

    /**
     * Chases the target; if it dies or burrows mid-flight, carries on to where it was. Returns
     * true on arrival.
     */
    private boolean flyHoming(Projectile pr, Position p, float dt) {
        Position tp = positions.get(pr.target);
        Health th = healths.get(pr.target);
        Status ts = statuses.get(pr.target);
        if (tp != null && th != null && th.hp > 0 && (ts == null || ts.burrowTime <= 0)) {
            pr.tx = tp.x;
            pr.ty = tp.y;
        } else {
            pr.target = World.NONE;
        }
        float dx = pr.tx - p.x, dy = pr.ty - p.y;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        float step = pr.speed * dt;
        if (dist <= step) return true;
        p.x += dx / dist * step;
        p.y += dy / dist * step;
        pr.angle = (float) Math.atan2(dy, dx);
        return false;
    }

    private void impact(Projectile pr, float x, float y) {
        TowerType type = pr.source;
        int tier = pr.tier;
        float damage = type.damage(tier);
        float radius = type.splash(tier);
        switch (type) {
            case ARROW -> {
                game.combat.damage(pr.target, damage, type.damageType);
                if (pr.target != World.NONE) game.prefabs.burst(x, y, WOOD, 2);
            }
            case VENOM, BLIGHT -> {
                if (pr.target == World.NONE) return;
                game.combat.damage(pr.target, damage, type.damageType);
                game.combat.poison(pr.target, type.poisonDps(tier), type.poisonSeconds(), false);
                game.prefabs.burst(x, y, TOXIC, 3);
            }
            case HEX -> {
                if (pr.target == World.NONE) return;
                game.combat.damage(pr.target, damage, type.damageType);
                game.combat.hex(pr.target, type.hexSeconds(tier));
                game.prefabs.burst(x, y, HEX, 3);
            }
            case HUNTER -> {
                Enemy enemy = enemies.get(pr.target);
                if (enemy == null) return;
                boolean caster = enemy.type.caster();
                game.combat.damage(pr.target, caster ? damage * type.casterBonus() : damage, type.damageType);
                game.combat.hex(pr.target, type.hexSeconds(tier));
                if (caster) game.prefabs.flash(x, y, 14, HEX, 0.2f);
                game.prefabs.burst(x, y, SILVER, caster ? 5 : 2);
            }
            case ROT -> {
                if (pr.target == World.NONE) return;
                game.combat.damage(pr.target, damage, type.damageType);
                game.combat.poison(pr.target, type.poisonDps(tier), type.poisonSeconds(), false);
                game.combat.rot(pr.target, type.rotSeconds(tier));
                game.prefabs.burst(x, y, ROT, 3);
            }
            case SUNBOW -> {
                Health h = healths.get(pr.target);
                Enemy enemy = enemies.get(pr.target);
                if (h == null || enemy == null) return;
                float share = type.percentHealth(tier) * (enemy.type.boss() ? BOSS_PERCENT_FACTOR : 1);
                game.combat.damage(pr.target, damage + h.max * share, type.damageType);
                game.prefabs.flash(x, y, 16, SUN, 0.2f);
                game.prefabs.burst(x, y, SUN, 4);
            }
            case ICE_LANCE -> {
                if (pr.target == World.NONE) return;
                float stun = type.stun(tier);
                game.combat.damage(pr.target, damage, type.damageType);
                game.combat.stun(pr.target, stun);
                game.combat.slow(pr.target, type.slow(tier), stun + 1.5f);
                game.prefabs.ring(x, y, 26, ICE, 0.3f);
                game.prefabs.burst(x, y, ICE, 5);
            }
            case FLAK -> {
                splash(type, tier, x, y, radius, damage, FLAK_AIR_BONUS);
                game.prefabs.flash(x, y, radius * 0.7f, FIRE, 0.15f);
                game.prefabs.burst(x, y, FIRE, 3);
            }
            case CANNON, PLASMA, ACID -> {
                splash(type, tier, x, y, radius, damage, 1);
                game.prefabs.explosion(x, y, radius, type == TowerType.CANNON ? FIRE : type == TowerType.PLASMA ? PLASMA : TOXIC);
            }
            case CLUSTER -> {
                splash(type, tier, x, y, radius, damage, 1);
                game.prefabs.explosion(x, y, radius, FIRE);
                // Four bomblets land in a ring around the main blast, at a random rotation.
                double spin = game.rng.nextDouble() * Math.PI / 2;
                for (int k = 0; k < 4; k++) {
                    float bx = x + (float) Math.cos(spin + k * Math.PI / 2) * radius;
                    float by = y + (float) Math.sin(spin + k * Math.PI / 2) * radius;
                    splash(type, tier, bx, by, radius * BOMBLET_RADIUS, damage * BOMBLET_DAMAGE, 1);
                    game.prefabs.explosion(bx, by, radius * BOMBLET_RADIUS, SUN);
                }
            }
            case DOOM -> {
                splash(type, tier, x, y, radius, damage, 1);
                game.prefabs.explosion(x, y, radius, HEX);
            }
            case BLIZZARD -> {
                splash(type, tier, x, y, radius, damage, 1);
                game.prefabs.zone(pr.owner, x, y, type.zoneRadius(tier), type.slow(tier), type.zoneDps(tier), ICE, ZONE_SECONDS);
                game.prefabs.burst(x, y, ICE, 6);
            }
            default -> { }
        }
    }

    /** Damages everything in the blast and applies the source tower's on-hit effects. */
    private void splash(TowerType type, int tier, float x, float y, float radius, float damage, float airBonus) {
        int n = game.grid.query(x, y, radius);
        for (int k = 0; k < n; k++) {
            int e = game.grid.result(k);
            boolean flying = enemies.get(e).type.flying;
            if (flying && !type.hitsAir) continue;
            if (type == TowerType.ACID) game.combat.shred(e, type.shredSeconds(tier));
            if (type == TowerType.DOOM) {
                game.combat.hex(e, type.hexSeconds(tier));
                game.combat.doom(e, type.doom(tier), type.hexSeconds(tier));
            }
            game.combat.damage(e, flying ? damage * airBonus : damage, type.damageType);
            if (type.burnDps(tier) > 0) game.combat.burn(e, type.burnDps(tier), BURN_SECONDS);
            if (type.poisonDps(tier) > 0) game.combat.poison(e, type.poisonDps(tier), type.poisonSeconds(), false);
        }
    }
}
