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

/** Walks enemies along their route; those that reach the end cost lives. */
public final class MovementSystem implements GameSystem {
    private static final Color LEAK = new Color(0xFF5A5A);

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

            float budget = f.speed * dt * (s == null ? 1 : 1 - s.slow);
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
                    Sprite sprite = sprites.get(e);
                    if (sprite != null && Math.abs(dx) > 1) sprite.flip = dx < 0;
                }
            }
            if (f.next >= f.xs.length) reachBase(e, enemy, p);
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
