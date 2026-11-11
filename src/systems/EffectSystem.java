package systems;

import components.Lifetime;
import components.Position;
import components.Velocity;
import ecs.GameSystem;
import ecs.Query;
import ecs.Store;
import game.Game;

/** Drifts particles and expires anything with a lifetime. */
public final class EffectSystem implements GameSystem {
    private final Game game;
    private final Query moving, timed;
    private final Store<Position> positions;
    private final Store<Velocity> velocities;
    private final Store<Lifetime> lifetimes;

    public EffectSystem(Game game) {
        this.game = game;
        this.moving = game.world.query(Position.class, Velocity.class);
        this.timed = game.world.query(Lifetime.class);
        this.positions = game.world.store(Position.class);
        this.velocities = game.world.store(Velocity.class);
        this.lifetimes = game.world.store(Lifetime.class);
    }

    @Override
    public void update(float dt) {
        moving.fetch();
        for (int i = 0; i < moving.size(); i++) {
            int e = moving.get(i);
            Position p = positions.get(e);
            Velocity v = velocities.get(e);
            p.x += v.vx * dt;
            p.y += v.vy * dt;
            float keep = Math.max(0, 1 - v.drag * dt);
            v.vx *= keep;
            v.vy *= keep;
        }
        timed.fetch();
        for (int i = 0; i < timed.size(); i++) {
            int e = timed.get(i);
            if ((lifetimes.get(e).remaining -= dt) <= 0) game.world.destroy(e);
        }
    }
}
