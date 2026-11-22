package systems;

import components.Position;
import components.Shielder;
import components.Status;
import ecs.GameSystem;
import ecs.Query;
import ecs.Store;
import game.Game;
import java.awt.Color;

/**
 * Aegis Wardens raise a shield over every monster around them, unless they are hexed. A shield
 * swallows one hit whole, so slow, heavy shots are wasted on it and rapid or area fire breaks it.
 */
public final class ShieldSystem implements GameSystem {
    private static final Color SHIELD = new Color(0xFDE68A);
    private static final float RETRY = 0.3f;

    private final Game game;
    private final Query query;
    private final Store<Shielder> shielders;
    private final Store<Position> positions;
    private final Store<Status> statuses;

    public ShieldSystem(Game game) {
        this.game = game;
        this.query = game.world.query(Shielder.class, Position.class);
        this.shielders = game.world.store(Shielder.class);
        this.positions = game.world.store(Position.class);
        this.statuses = game.world.store(Status.class);
    }

    @Override
    public void update(float dt) {
        query.fetch();
        for (int i = 0; i < query.size(); i++) {
            int e = query.get(i);
            Shielder shielder = shielders.get(e);
            if ((shielder.timer -= dt) > 0) continue;
            shielder.timer = RETRY;
            if (game.combat.hexed(e)) continue;
            Position p = positions.get(e);
            boolean raised = false;
            int n = game.grid.query(p.x, p.y, shielder.radius);
            for (int k = 0; k < n; k++) {
                Status s = statuses.get(game.grid.result(k));
                if (s == null || s.shield) continue;
                s.shield = true;
                raised = true;
            }
            if (!raised) continue;
            game.prefabs.ring(p.x, p.y, shielder.radius, SHIELD, 0.45f);
            shielder.timer = shielder.interval;
        }
    }
}
