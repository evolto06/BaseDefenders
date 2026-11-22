package systems;

import components.Enemy;
import components.PathFollower;
import components.Position;
import components.Sprite;
import components.Status;
import ecs.GameSystem;
import ecs.Query;
import ecs.Store;
import game.Game;
import java.awt.Color;

/** Walks enemies along their route, or back down it while they flee; those that reach the end cost lives. */
public final class MovementSystem implements GameSystem {
    private static final Color LEAK = new Color(0xFF5A5A);
    /** A Dune Borer tunnels this much faster than it walks. */
    private static final float BURROW_SPEED = 1.3f;

    private final Game game;
    private final Query query;
    private final Store<Enemy> enemies;
    private final Store<PathFollower> followers;
    private final Store<Position> positions;
    private final Store<Status> statuses;
    private final Store<Sprite> sprites;

    public MovementSystem(Game game) {
        this.game = game;
        this.query = game.world.query(Enemy.class, PathFollower.class, Position.class);
        this.enemies = game.world.store(Enemy.class);
        this.followers = game.world.store(PathFollower.class);
        this.positions = game.world.store(Position.class);
        this.statuses = game.world.store(Status.class);
        this.sprites = game.world.store(Sprite.class);
    }

    @Override
    public void update(float dt) {
        query.fetch();
        for (int i = 0; i < query.size(); i++) {
            int e = query.get(i);
            Enemy enemy = enemies.get(e);
            PathFollower f = followers.get(e);
            Position p = positions.get(e);
            Status s = statuses.get(e);
            enemy.flash = Math.max(0, enemy.flash - dt * 6);
            if (s != null && s.stunTime > 0) continue;
            enemy.animTime += dt;

            float budget = f.speed * dt * (s == null ? 1 : (1 + s.haste) * (1 - s.slow));
            if (s != null && s.burrowTime > 0) budget *= BURROW_SPEED;
            if (s != null && s.fearTime > 0) {
                retreat(f, p, sprites.get(e), budget);
                continue;
            }
            advance(f, p, sprites.get(e), budget);
            if (f.next >= f.xs.length) reachBase(e, enemy, p);
        }
    }

    /** Carries a monster {@code budget} pixels on along its route, stopping at the end of it. */
    static void advance(PathFollower f, Position p, Sprite sprite, float budget) {
        while (budget > 0 && f.next < f.xs.length) {
            float tx = f.xs[f.next] + f.laneX, ty = f.ys[f.next] + f.laneY;
            float dx = tx - p.x, dy = ty - p.y;
            float dist = (float) Math.sqrt(dx * dx + dy * dy);
            if (dist <= budget) {
                p.x = tx;
                p.y = ty;
                f.next++;
                f.remaining -= dist;
                budget -= dist;
            } else {
                p.x += dx / dist * budget;
                p.y += dy / dist * budget;
                f.remaining -= budget;
                budget = 0;
                if (sprite != null && Math.abs(dx) > 1) sprite.flip = dx < 0;
            }
        }
    }

    /** A frightened monster runs back the way it came, no further than where it entered. */
    private static void retreat(PathFollower f, Position p, Sprite sprite, float budget) {
        while (budget > 0) {
            int previous = f.next - 1;
            float tx = f.xs[previous] + f.laneX, ty = f.ys[previous] + f.laneY;
            float dx = tx - p.x, dy = ty - p.y;
            float dist = (float) Math.sqrt(dx * dx + dy * dy);
            if (dist <= budget) {
                p.x = tx;
                p.y = ty;
                f.remaining += dist;
                budget -= dist;
                if (previous == 0) return;
                f.next = previous;
            } else {
                p.x += dx / dist * budget;
                p.y += dy / dist * budget;
                f.remaining += budget;
                budget = 0;
                if (sprite != null && Math.abs(dx) > 1) sprite.flip = dx < 0;
            }
        }
    }

    private void reachBase(int e, Enemy enemy, Position p) {
        game.world.destroy(e);
        game.lives -= enemy.type.lives;
        game.hurtFlash = 1;
        game.prefabs.text(Math.min(p.x, game.level.pathX[game.level.pathX.length - 1] - 60), p.y - 20,
                "-" + enemy.type.lives, LEAK);
        if (game.lives <= 0) {
            game.lives = 0;
            game.gameOver = true;
        }
    }
}
