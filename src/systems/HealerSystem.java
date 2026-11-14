package systems;

import components.Enemy;
import components.Healer;
import components.Health;
import components.Position;
import components.Regen;
import components.Status;
import ecs.GameSystem;
import ecs.Query;
import ecs.Store;
import game.Game;
import java.awt.Color;

/**
 * Monster healing: healers periodically mend the wounded around them, and regenerating monsters
 * mend themselves. A hexed monster does neither, and cannot be mended by anyone else.
 */
public final class HealerSystem implements GameSystem {
    private static final Color HEAL = new Color(0x6EE7A0);
    private static final float RETRY = 0.3f;
    /**
     * A monster that was just mended ignores every healer's pulse for this long, so a crowd of
     * healers keeps more monsters topped up but never heals any one of them without limit.
     */
    private static final float MEND_GUARD = 1.5f;
    /** A boss is too big to mend quickly: a pulse restores this share of what it would otherwise. */
    private static final float BOSS_MEND = 0.25f;

    private final Game game;
    private final Query healerQuery, regenQuery;
    private final Store<Healer> healers;
    private final Store<Regen> regens;
    private final Store<Position> positions;
    private final Store<Health> healths;
    private final Store<Status> statuses;
    private final Store<Enemy> enemies;

    public HealerSystem(Game game) {
        this.game = game;
        this.healerQuery = game.world.query(Healer.class, Position.class);
        this.regenQuery = game.world.query(Regen.class, Health.class);
        this.healers = game.world.store(Healer.class);
        this.regens = game.world.store(Regen.class);
        this.positions = game.world.store(Position.class);
        this.healths = game.world.store(Health.class);
        this.statuses = game.world.store(Status.class);
        this.enemies = game.world.store(Enemy.class);
    }

    @Override
    public void update(float dt) {
        healerQuery.fetch();
        for (int i = 0; i < healerQuery.size(); i++) {
            int e = healerQuery.get(i);
            Healer healer = healers.get(e);
            if ((healer.timer -= dt) > 0) continue;
            healer.timer = RETRY;
            if (game.combat.hexed(e)) continue;
            Position p = positions.get(e);
            boolean healed = false;
            int n = game.grid.query(p.x, p.y, healer.radius);
            for (int k = 0; k < n; k++) {
                int other = game.grid.result(k);
                Status s = statuses.get(other);
                if (other == e || s == null || s.mendGuard > 0) continue;
                float amount = healths.get(other).max * healer.fraction;
                if (enemies.get(other).type.boss()) amount *= BOSS_MEND;
                if (!game.combat.heal(other, amount)) continue;
                s.mendGuard = MEND_GUARD;
                healed = true;
            }
            if (!healed) continue;
            game.prefabs.ring(p.x, p.y, healer.radius, HEAL, 0.5f);
            healer.timer = healer.interval;
        }

        regenQuery.fetch();
        for (int i = 0; i < regenQuery.size(); i++) {
            int e = regenQuery.get(i);
            game.combat.heal(e, healths.get(e).max * regens.get(e).perSecond * dt);
        }
    }
}
