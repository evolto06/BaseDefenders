package systems;

import components.Enemy;
import components.Health;
import components.Position;
import ecs.GameSystem;
import ecs.Query;
import game.Game;

/** Rebuilds the enemy spatial grid after movement so later systems can do range queries. */
public final class SpatialSystem implements GameSystem {
    private final Game game;
    private final Query enemies;

    public SpatialSystem(Game game) {
        this.game = game;
        this.enemies = game.world.query(Enemy.class, Position.class, Health.class);
    }

    @Override
    public void update(float dt) {
        game.grid.rebuild(enemies.fetch());
        game.enemyCount = enemies.size();
    }
}
