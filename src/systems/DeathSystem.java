package systems;

import components.Enemy;
import components.Health;
import components.PathFollower;
import components.Position;
import components.Status;
import components.Tower;
import ecs.GameSystem;
import ecs.Query;
import ecs.Store;
import ecs.World;
import game.EnemyType;
import game.Game;
import java.awt.Color;

/** Removes enemies whose health has run out, with their death effects. */
public final class DeathSystem implements GameSystem {
    private static final float SPLIT_SPREAD = 7;
    private static final float CONTAGION_RADIUS = 64, CONTAGION_SECONDS = 4;
    private static final Color PLAGUE = new Color(0xBEF264);

    private final Game game;
    private final Query query;
    private final Store<Enemy> enemies;
    private final Store<Health> healths;
    private final Store<Position> positions;
    private final Store<PathFollower> followers;
    private final Store<Status> statuses;
    private final Store<Tower> towers;

    public DeathSystem(Game game) {
        this.game = game;
        this.query = game.world.query(Enemy.class, Health.class, Position.class);
        this.enemies = game.world.store(Enemy.class);
        this.healths = game.world.store(Health.class);
        this.positions = game.world.store(Position.class);
        this.followers = game.world.store(PathFollower.class);
        this.statuses = game.world.store(Status.class);
        this.towers = game.world.store(Tower.class);
    }

    @Override
    public void update(float dt) {
        query.fetch();
        for (int i = 0; i < query.size(); i++) {
            int e = query.get(i);
            if (healths.get(e).hp > 0) continue;
            Enemy enemy = enemies.get(e);
            Position p = positions.get(e);
            game.kills++;
            Tower killer = towers.get(healths.get(e).lastHitBy);
            if (killer != null) killer.kills++;
            game.prefabs.burst(p.x, p.y, enemy.type.color, enemy.type.boss() ? 24 : 6);
            if (enemy.type == EnemyType.SLIME) {
                // A slime splits into two slimelets that carry on from where it died.
                PathFollower f = followers.get(e);
                for (int side = -1; side <= 1; side += 2) {
                    float shift = side * SPLIT_SPREAD;
                    game.prefabs.enemy(EnemyType.MINI_SLIME, enemy.hpScale, p.x + shift, p.y + shift,
                            f.laneX + shift, f.laneY + shift, f.xs, f.ys, f.next, f.remaining);
                }
            }
            Status s = statuses.get(e);
            if (s != null && s.contagious && s.poisonTime > 0) {
                // The plague outlives its host.
                game.combat.source = s.poisonOwner;
                int n = game.grid.query(p.x, p.y, CONTAGION_RADIUS);
                for (int k = 0; k < n; k++) {
                    int other = game.grid.result(k);
                    if (other != e) game.combat.poison(other, s.poisonDps, CONTAGION_SECONDS, true);
                }
                game.combat.source = World.NONE;
                game.prefabs.ring(p.x, p.y, CONTAGION_RADIUS, PLAGUE, 0.4f);
            }
            game.world.destroy(e);
        }
    }
}
