package systems;

import components.Healer;
import components.Health;
import components.Position;
import ecs.GameSystem;
import ecs.Query;
import ecs.Store;
import game.Game;
import java.awt.Color;

/** Shamans periodically mend the wounded monsters around them. */
public final class HealerSystem implements GameSystem {
    private static final Color HEAL = new Color(0x6EE7A0);
    private static final float RETRY = 0.3f;

    private final Game game;
    private final Query query;
    private final Store<Healer> healers;
    private final Store<Position> positions;
    private final Store<Health> healths;

    public HealerSystem(Game game) {
        this.game = game;
        this.query = game.world.query(Healer.class, Position.class);
        this.healers = game.world.store(Healer.class);
        this.positions = game.world.store(Position.class);
        this.healths = game.world.store(Health.class);
    }

    @Override
    public void update(float dt) {
        query.fetch();
        for (int i = 0; i < query.size(); i++) {
            int e = query.get(i);
            Healer healer = healers.get(e);
            if ((healer.timer -= dt) > 0) continue;
            Position p = positions.get(e);
            boolean healed = false;
            int n = game.grid.query(p.x, p.y, healer.radius);
            for (int k = 0; k < n; k++) {
                int other = game.grid.result(k);
                Health h = healths.get(other);
                if (other == e || h.hp >= h.max) continue;
                h.hp = Math.min(h.max, h.hp + h.max * healer.fraction);
                healed = true;
            }
            if (healed) game.prefabs.ring(p.x, p.y, healer.radius, HEAL, 0.5f);
            healer.timer = healed ? healer.interval : RETRY;
        }
    }
}
