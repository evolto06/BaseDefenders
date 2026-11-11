package systems;

import components.Enemy;
import components.Health;
import components.PathFollower;
import components.Position;
import components.Status;
import components.Tower;
import ecs.GameSystem;
import ecs.Query;
import ecs.Store;
import ecs.World;
import game.Game;
import game.SpatialGrid;
import game.TowerType;
import game.TowerType.Attack;
import java.awt.Color;

/** Picks targets and fires. Each attack pattern lives here. */
public final class TowerSystem implements GameSystem {
    private static final float CHAIN_JUMP = 90;
    private static final float TESLA_DECAY = 0.75f;
    private static final float RAIL_HALF_WIDTH = 10;
    private static final float SLOW_SECONDS = 1.5f;
    private static final float BRITTLE_SECONDS = 3;
    private static final float BEAM_SLOW_SECONDS = 0.4f;
    /** Added to a candidate's score to push it behind every candidate without the penalty. */
    private static final float LOW_PRIORITY = 100_000;

    private static final Color FROST_RING = new Color(0x7DD3FC);
    private static final Color MIASMA_RING = new Color(0xA3E635);
    private static final Color TESLA_BOLT = new Color(0xFDE047);
    private static final Color STORM_BOLT = new Color(0x67E8F9);
    private static final Color PLAGUE_BOLT = new Color(0xBEF264);
    private static final Color RAIL_BEAM = new Color(0xFEF08A);
    private static final Color SUN_BEAM = new Color(0xFDBA74);
    private static final Color AURORA_BEAM = new Color(0x7DD3FC);
    private static final Color SIPHON_BEAM = new Color(0xA3E635);

    private final Game game;
    private final SpatialGrid grid;
    private final Query towerQuery;
    private final Store<Tower> towers;
    private final Store<Position> positions;
    private final Store<Enemy> enemies;
    private final Store<Health> healths;
    private final Store<PathFollower> followers;
    private final Store<Status> statuses;
    /** Enemies already chosen by the attack being resolved (multi-shot targets, chain hits). */
    private final int[] picked = new int[16];

    public TowerSystem(Game game) {
        this.game = game;
        this.grid = game.grid;
        this.towerQuery = game.world.query(Tower.class, Position.class);
        this.towers = game.world.store(Tower.class);
        this.positions = game.world.store(Position.class);
        this.enemies = game.world.store(Enemy.class);
        this.healths = game.world.store(Health.class);
        this.followers = game.world.store(PathFollower.class);
        this.statuses = game.world.store(Status.class);
    }

    @Override
    public void update(float dt) {
        towerQuery.fetch();
        for (int i = 0; i < towerQuery.size(); i++) {
            int e = towerQuery.get(i);
            Tower t = towers.get(e);
            Position p = positions.get(e);
            t.cooldown -= dt;
            t.recoil = Math.max(0, t.recoil - dt * 5);

            float range = t.type.range(t.tier);
            boolean ready = t.cooldown <= 0;
            boolean beam = t.type.attack == Attack.BEAM;
            // Beams hold their target to build up heat. Everything else re-picks whenever it
            // is ready to fire, and in between keeps tracking so the turret turns smoothly.
            boolean keep = valid(t.target, p, range) && (beam || !ready);
            if (!keep) {
                int previous = t.target;
                t.target = acquire(t, p, range, 0);
                if (beam && t.target != previous) t.ramp = 0;
            }
            Position tp = positions.get(t.target);
            if (tp == null) {
                t.cooldown = Math.max(0, t.cooldown);
                t.ramp = 0;
                continue;
            }
            if (t.type.rotates) t.angle = (float) Math.atan2(tp.y - p.y, tp.x - p.x);
            if (ready) {
                game.combat.source = e;
                fire(t, p, tp, range);
                game.combat.source = World.NONE;
                t.cooldown += t.type.cooldown;
                t.recoil = 1;
            }
        }
    }

    private boolean valid(int target, Position p, float range) {
        Position tp = positions.get(target);
        Health h = healths.get(target);
        if (tp == null || h == null || h.hp <= 0) return false;
        float dx = tp.x - p.x, dy = tp.y - p.y;
        return dx * dx + dy * dy <= range * range;
    }

    /**
     * The enemy in range closest to the base that this tower can hit, skipping the first
     * {@code skip} entries of {@link #picked}.
     */
    private int acquire(Tower t, Position p, float range, int skip) {
        int best = World.NONE;
        float bestScore = Float.MAX_VALUE;
        boolean spreadsBrittle = t.type == TowerType.STORM;
        boolean spreadsPoison = t.type.poisonDps(1) > 0;
        int n = grid.query(p.x, p.y, range);
        search:
        for (int k = 0; k < n; k++) {
            int e = grid.result(k);
            if (!t.type.canHit(enemies.get(e).type)) continue;
            for (int s = 0; s < skip; s++) {
                if (picked[s] == e) continue search;
            }
            float score = followers.get(e).remaining;
            // Debuff towers spread their effect around instead of re-marking the same enemy.
            Status status = statuses.get(e);
            if (spreadsBrittle && status.brittleTime > 0.5f) score += LOW_PRIORITY;
            if (spreadsPoison && status.poisonTime > 1) score += LOW_PRIORITY;
            if (score < bestScore) {
                bestScore = score;
                best = e;
            }
        }
        return best;
    }

    /** Fills {@link #picked} with the primary target plus extra ones; returns how many. */
    private int pickTargets(Tower t, Position p, float range) {
        int count = 0;
        picked[count++] = t.target;
        while (count < t.type.targets()) {
            int extra = acquire(t, p, range, count);
            if (extra == World.NONE) break;
            picked[count++] = extra;
        }
        return count;
    }

    private void fire(Tower t, Position p, Position tp, float range) {
        switch (t.type.attack) {
            case BOLT -> bolts(t, p, range);
            case SHELL -> lob(t, p, tp);
            case PULSE -> pulse(t, p, range);
            case CHAIN -> chain(t, p);
            case RAIL -> rail(t, p, tp, range);
            case BEAM -> beam(t, p, range);
        }
    }

    private void bolts(Tower t, Position p, float range) {
        float mx = p.x + (float) Math.cos(t.angle) * 18, my = p.y + (float) Math.sin(t.angle) * 18;
        int count = pickTargets(t, p, range);
        for (int k = 0; k < count; k++) {
            Position tp = positions.get(picked[k]);
            game.prefabs.homing(game.combat.source, t.type, t.tier, mx, my, picked[k], tp.x, tp.y);
        }
    }

    /** Lobs a shell at where the target will be when the shell lands. */
    private void lob(Tower t, Position p, Position tp) {
        float tx = tp.x, ty = tp.y;
        PathFollower f = followers.get(t.target);
        Status s = statuses.get(t.target);
        if (f.next < f.xs.length && s.stunTime <= 0) {
            float flight = (float) Math.hypot(tx - p.x, ty - p.y) / t.type.projectileSpeed;
            float dx = f.xs[f.next] + f.laneX - tx, dy = f.ys[f.next] + f.laneY - ty;
            float dist = (float) Math.sqrt(dx * dx + dy * dy);
            float lead = Math.min(dist, f.speed * (1 - s.slow) * flight);
            if (dist > 0.01f) {
                tx += dx / dist * lead;
                ty += dy / dist * lead;
            }
        }
        game.prefabs.lobbed(game.combat.source, t.type, t.tier, p.x, p.y, tx, ty);
    }

    private void pulse(Tower t, Position p, float range) {
        boolean toxic = t.type == TowerType.MIASMA;
        float damage = t.type.damage(t.tier), slow = t.type.slow(t.tier);
        int n = grid.query(p.x, p.y, range);
        for (int k = 0; k < n; k++) {
            int e = grid.result(k);
            game.combat.damage(e, damage, t.type.damageType);
            game.combat.slow(e, slow, SLOW_SECONDS);
            if (toxic) game.combat.poison(e, t.type.poisonDps(t.tier), t.type.poisonSeconds(), false);
        }
        game.prefabs.ring(p.x, p.y, range, toxic ? MIASMA_RING : FROST_RING, 0.35f);
    }

    private void chain(Tower t, Position p) {
        TowerType type = t.type;
        Color color = type == TowerType.STORM ? STORM_BOLT : type == TowerType.PLAGUE ? PLAGUE_BOLT : TESLA_BOLT;
        float damage = type.damage(t.tier);
        int max = Math.min(type.chains(t.tier), picked.length);
        float fromX = p.x, fromY = p.y - 6;
        int current = t.target;
        int hits = 0;
        while (current != World.NONE && hits < max) {
            Position cp = positions.get(current);
            game.prefabs.bolt(fromX, fromY, cp.x, cp.y, color);
            game.combat.damage(current, damage, type.damageType);
            switch (type) {
                case STORM -> {
                    game.combat.brittle(current, type.brittle(t.tier), BRITTLE_SECONDS);
                    game.combat.slow(current, type.slow(t.tier), 1);
                }
                case PLAGUE -> game.combat.poison(current, type.poisonDps(t.tier), type.poisonSeconds(), true);
                default -> damage *= TESLA_DECAY;
            }
            picked[hits++] = current;
            fromX = cp.x;
            fromY = cp.y;
            current = nearestUnhit(fromX, fromY, hits);
        }
    }

    private int nearestUnhit(float x, float y, int hits) {
        int best = World.NONE;
        float bestDist = Float.MAX_VALUE;
        int n = grid.query(x, y, CHAIN_JUMP);
        search:
        for (int k = 0; k < n; k++) {
            int e = grid.result(k);
            for (int h = 0; h < hits; h++) {
                if (picked[h] == e) continue search;
            }
            Position p = positions.get(e);
            float dx = p.x - x, dy = p.y - y;
            float dist = dx * dx + dy * dy;
            if (dist < bestDist) {
                bestDist = dist;
                best = e;
            }
        }
        return best;
    }

    /** Fires a line through the target, hitting everything along its full length. */
    private void rail(Tower t, Position p, Position tp, float range) {
        float dx = tp.x - p.x, dy = tp.y - p.y;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        if (dist < 0.01f) return;
        dx /= dist;
        dy /= dist;
        float length = range * 1.15f;
        float damage = t.type.damage(t.tier);
        int n = grid.query(p.x, p.y, length + 24);
        for (int k = 0; k < n; k++) {
            int e = grid.result(k);
            Position q = positions.get(e);
            float rx = q.x - p.x, ry = q.y - p.y;
            float along = rx * dx + ry * dy;
            if (along < 0 || along > length) continue;
            float across = Math.abs(rx * dy - ry * dx);
            if (across <= RAIL_HALF_WIDTH + enemies.get(e).type.radius) {
                game.combat.damage(e, damage, t.type.damageType);
            }
        }
        game.prefabs.beam(p.x + dx * 16, p.y + dy * 16, p.x + dx * length, p.y + dy * length, RAIL_BEAM, 5, 0.22f);
    }

    /** One tick of a continuous beam: damage grows while the primary target is held. */
    private void beam(Tower t, Position p, float range) {
        TowerType type = t.type;
        t.ramp = Math.min(1, t.ramp + type.cooldown / TowerType.BEAM_RAMP_SECONDS);
        float damage = type.damage(t.tier) * (1 + (TowerType.BEAM_RAMP - 1) * t.ramp);
        Color color = switch (type) {
            case AURORA -> AURORA_BEAM;
            case ARC -> TESLA_BOLT;
            case SIPHON -> SIPHON_BEAM;
            default -> SUN_BEAM;
        };
        int count = pickTargets(t, p, range);
        for (int k = 0; k < count; k++) {
            Position tp = positions.get(picked[k]);
            game.combat.damage(picked[k], damage, type.damageType);
            if (type == TowerType.AURORA) game.combat.slow(picked[k], type.slow(t.tier), BEAM_SLOW_SECONDS);
            game.prefabs.beam(p.x, p.y - 4, tp.x, tp.y, color, 2 + t.ramp * 3, type.cooldown + 0.04f);
        }
        float cap = game.manaCap();
        if (type == TowerType.SIPHON && game.mana < cap) {
            game.mana = Math.min(cap, game.mana + type.manaPerSecond(t.tier) * type.cooldown);
        }
    }
}
