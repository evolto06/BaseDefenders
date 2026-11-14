package systems;

import components.Position;
import components.Rally;
import ecs.GameSystem;
import ecs.Query;
import ecs.Store;
import game.Game;
import java.awt.Color;

/** War Drummers keep the monsters around them moving faster, unless they are hexed. */
public final class RallySystem implements GameSystem {
    private static final Color DRUM = new Color(0xFDBA74);
    private static final float BEAT = 0.8f;
    /** Haste from one beat outlasts the gap to the next, so it only lapses when the drumming stops. */
    private static final float HASTE_SECONDS = 1.1f;

    private final Game game;
    private final Query query;
    private final Store<Rally> rallies;
    private final Store<Position> positions;

    public RallySystem(Game game) {
        this.game = game;
        this.query = game.world.query(Rally.class, Position.class);
        this.rallies = game.world.store(Rally.class);
        this.positions = game.world.store(Position.class);
    }

    @Override
    public void update(float dt) {
        query.fetch();
        for (int i = 0; i < query.size(); i++) {
            int e = query.get(i);
            Rally rally = rallies.get(e);
            if ((rally.timer -= dt) > 0) continue;
            rally.timer = BEAT;
            if (game.combat.hexed(e)) continue;
            Position p = positions.get(e);
            int n = game.grid.query(p.x, p.y, rally.radius);
            for (int k = 0; k < n; k++) game.combat.haste(game.grid.result(k), rally.haste, HASTE_SECONDS);
            game.prefabs.ring(p.x, p.y, rally.radius * 0.5f, DRUM, 0.35f);
        }
    }
}
