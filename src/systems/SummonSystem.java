package systems;

import components.Enemy;
import components.PathFollower;
import components.Position;
import components.Summoner;
import ecs.GameSystem;
import ecs.Query;
import ecs.Store;
import game.Game;
import java.awt.Color;

/** Necromancers and Liches raise minions beside themselves for as long as they are not hexed. */
public final class SummonSystem implements GameSystem {
    private static final Color RAISE = new Color(0xC4B5FD);
    private static final float SPREAD = 9;
    /** No more are raised while the field is this crowded. */
    private static final int FIELD_LIMIT = 400;
    /** Nor on the last stretch of road, where the towers would get no chance to answer. */
    private static final float LAST_STRETCH = 260;

    private final Game game;
    private final Query query;
    private final Store<Summoner> summoners;
    private final Store<Enemy> enemies;
    private final Store<Position> positions;
    private final Store<PathFollower> followers;

    public SummonSystem(Game game) {
        this.game = game;
        this.query = game.world.query(Summoner.class, Enemy.class, Position.class, PathFollower.class);
        this.summoners = game.world.store(Summoner.class);
        this.enemies = game.world.store(Enemy.class);
        this.positions = game.world.store(Position.class);
        this.followers = game.world.store(PathFollower.class);
    }

    @Override
    public void update(float dt) {
        query.fetch();
        for (int i = 0; i < query.size(); i++) {
            int e = query.get(i);
            Summoner summoner = summoners.get(e);
            // A hexed summoner makes no progress on its ritual.
            if (game.combat.hexed(e) || (summoner.timer -= dt) > 0) continue;
            summoner.timer = summoner.interval;
            PathFollower f = followers.get(e);
            if (game.enemyCount >= FIELD_LIMIT || f.remaining < LAST_STRETCH) continue;
            Position p = positions.get(e);
            float hpScale = enemies.get(e).hpScale;
            for (int k = 0; k < summoner.count; k++) {
                float dx = (game.rng.nextFloat() - 0.5f) * 2 * SPREAD, dy = (game.rng.nextFloat() - 0.5f) * 2 * SPREAD;
                game.prefabs.enemy(summoner.minion, hpScale, p.x + dx, p.y + dy, f.laneX + dx, f.laneY + dy,
                        f.xs, f.ys, f.next, f.remaining);
            }
            game.prefabs.ring(p.x, p.y, 22, RAISE, 0.4f);
        }
    }
}
