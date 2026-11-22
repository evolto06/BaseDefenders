package systems;

import components.Enemy;
import components.Position;
import components.Zone;
import ecs.GameSystem;
import ecs.Query;
import ecs.Store;
import ecs.World;
import game.DamageType;
import game.Game;

/** Lingering ground fields slow and damage the ground enemies standing in them. */
public final class ZoneSystem implements GameSystem {
    private static final float TICK = 0.25f;

    private final Game game;
    private final Query query;
    private final Store<Zone> zones;
    private final Store<Position> positions;
    private final Store<Enemy> enemies;

    public ZoneSystem(Game game) {
        this.game = game;
        this.query = game.world.query(Zone.class, Position.class);
        this.zones = game.world.store(Zone.class);
        this.positions = game.world.store(Position.class);
        this.enemies = game.world.store(Enemy.class);
    }

    @Override
    public void update(float dt) {
        query.fetch();
        for (int i = 0; i < query.size(); i++) {
            int e = query.get(i);
            Zone zone = zones.get(e);
            if ((zone.tick -= dt) > 0) continue;
            zone.tick += TICK;
            Position p = positions.get(e);
            game.combat.source = zone.owner;
            int n = game.grid.query(p.x, p.y, zone.radius);
            for (int k = 0; k < n; k++) {
                int enemy = game.grid.result(k);
                if (enemies.get(enemy).type.flying) continue;
                game.combat.slow(enemy, zone.slow, TICK * 2);
                game.combat.tick(enemy, zone.dps * TICK, DamageType.MAGIC);
            }
        }
        game.combat.source = World.NONE;
    }
}
