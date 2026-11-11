package systems;

import components.Status;
import ecs.GameSystem;
import ecs.Query;
import ecs.Store;
import ecs.World;
import game.DamageType;
import game.Game;

/** Ticks debuff timers and applies burn and poison damage. */
public final class StatusSystem implements GameSystem {
    private final Game game;
    private final Query query;
    private final Store<Status> statuses;

    public StatusSystem(Game game) {
        this.game = game;
        this.query = game.world.query(Status.class);
        this.statuses = game.world.store(Status.class);
    }

    @Override
    public void update(float dt) {
        query.fetch();
        for (int i = 0; i < query.size(); i++) {
            int e = query.get(i);
            Status s = statuses.get(e);
            if (s.slowTime > 0 && (s.slowTime -= dt) <= 0) s.slow = 0;
            if (s.stunTime > 0) s.stunTime -= dt;
            if (s.shredTime > 0) s.shredTime -= dt;
            if (s.brittleTime > 0 && (s.brittleTime -= dt) <= 0) s.brittle = 0;
            if (s.burnTime > 0) {
                game.combat.source = s.burnOwner;
                game.combat.damage(e, s.burnDps * dt, DamageType.MAGIC);
                if ((s.burnTime -= dt) <= 0) s.burnDps = 0;
            }
            if (s.poisonTime > 0) {
                game.combat.source = s.poisonOwner;
                game.combat.damage(e, s.poisonDps * dt, DamageType.MAGIC);
                if ((s.poisonTime -= dt) <= 0) {
                    s.poisonDps = 0;
                    s.contagious = false;
                }
            }
        }
        game.combat.source = World.NONE;
    }
}
