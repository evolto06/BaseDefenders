package systems;

import components.Burrower;
import components.PathFollower;
import components.Position;
import components.Status;
import ecs.GameSystem;
import ecs.Query;
import ecs.Store;
import game.Game;
import java.awt.Color;

/**
 * Dune Borers tunnel under the road for stretches, out of reach of everything, and come up
 * thawed. A hexed Borer cannot dig.
 */
public final class BurrowSystem implements GameSystem {
    private static final Color DUST = new Color(0xD6B47E);
    private static final float RETRY = 0.3f;
    /** No diving on the last stretch of road, so a Borer always surfaces before the keep. */
    private static final float LAST_STRETCH = 220;

    private final Game game;
    private final Query query;
    private final Store<Burrower> burrowers;
    private final Store<Status> statuses;
    private final Store<PathFollower> followers;
    private final Store<Position> positions;

    public BurrowSystem(Game game) {
        this.game = game;
        this.query = game.world.query(Burrower.class, Status.class, PathFollower.class, Position.class);
        this.burrowers = game.world.store(Burrower.class);
        this.statuses = game.world.store(Status.class);
        this.followers = game.world.store(PathFollower.class);
        this.positions = game.world.store(Position.class);
    }

    @Override
    public void update(float dt) {
        query.fetch();
        for (int i = 0; i < query.size(); i++) {
            int e = query.get(i);
            Burrower b = burrowers.get(e);
            Status s = statuses.get(e);
            Position p = positions.get(e);
            if (b.under) {
                if (s.burrowTime > 0) continue;
                b.under = false;
                b.timer = b.up;
                game.prefabs.burst(p.x, p.y, DUST, 6);
                continue;
            }
            if ((b.timer -= dt) > 0) continue;
            if (game.combat.hexed(e) || followers.get(e).remaining < LAST_STRETCH) {
                b.timer = RETRY;
                continue;
            }
            // Down it goes, leaving the ice and every hold on it behind.
            s.burrowTime = b.down;
            s.stunTime = 0;
            s.slow = 0;
            s.slowTime = 0;
            s.fearTime = 0;
            b.under = true;
            game.prefabs.ring(p.x, p.y, 18, DUST, 0.3f);
            game.prefabs.burst(p.x, p.y, DUST, 6);
        }
    }
}
