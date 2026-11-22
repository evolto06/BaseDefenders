package systems;

import components.Blinker;
import components.PathFollower;
import components.Position;
import components.Sprite;
import components.Status;
import ecs.GameSystem;
import ecs.Query;
import ecs.Store;
import game.Game;
import java.awt.Color;

/**
 * Blink Imps that were frozen or struck hard last step hop on down the road, shaking off the ice,
 * then need time to recharge. A hexed Imp stays put.
 */
public final class BlinkSystem implements GameSystem {
    private static final Color BLINK = new Color(0xF0ABFC);
    /** An Imp never blinks closer to the keep than this. */
    private static final float KEEP_MARGIN = 80;

    private final Game game;
    private final Query query;
    private final Store<Blinker> blinkers;
    private final Store<Status> statuses;
    private final Store<PathFollower> followers;
    private final Store<Position> positions;
    private final Store<Sprite> sprites;

    public BlinkSystem(Game game) {
        this.game = game;
        this.query = game.world.query(Blinker.class, Status.class, PathFollower.class, Position.class);
        this.blinkers = game.world.store(Blinker.class);
        this.statuses = game.world.store(Status.class);
        this.followers = game.world.store(PathFollower.class);
        this.positions = game.world.store(Position.class);
        this.sprites = game.world.store(Sprite.class);
    }

    @Override
    public void update(float dt) {
        query.fetch();
        for (int i = 0; i < query.size(); i++) {
            int e = query.get(i);
            Blinker b = blinkers.get(e);
            if (b.timer > 0) b.timer -= dt;
            if (!b.pending) continue;
            b.pending = false;
            PathFollower f = followers.get(e);
            float distance = Math.min(b.distance, f.remaining - KEEP_MARGIN);
            if (game.combat.hexed(e) || distance <= 0) continue;
            b.timer = b.cooldown;
            Status s = statuses.get(e);
            s.stunTime = 0;
            s.slow = 0;
            s.slowTime = 0;
            Position p = positions.get(e);
            game.prefabs.flash(p.x, p.y, 14, BLINK, 0.2f);
            MovementSystem.advance(f, p, sprites.get(e), distance);
            game.prefabs.ring(p.x, p.y, 20, BLINK, 0.3f);
            game.prefabs.burst(p.x, p.y, BLINK, 4);
        }
    }
}
