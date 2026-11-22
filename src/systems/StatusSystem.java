package systems;

import components.Status;
import ecs.GameSystem;
import ecs.Query;
import ecs.Store;
import ecs.World;
import game.DamageType;
import game.Game;

/** Ticks the timers of every lingering effect and applies burn and poison damage. */
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
            if (s.hasteTime > 0 && (s.hasteTime -= dt) <= 0) s.haste = 0;
            if (s.stunTime > 0) s.stunTime -= dt;
            if (s.shredTime > 0) s.shredTime -= dt;
            if (s.hexTime > 0) s.hexTime -= dt;
            if (s.rotTime > 0) s.rotTime -= dt;
            if (s.fearTime > 0) s.fearTime -= dt;
            if (s.fearGuard > 0) s.fearGuard -= dt;
            if (s.mendGuard > 0) s.mendGuard -= dt;
            if (s.coldGuard > 0) s.coldGuard -= dt;
            if (s.shieldBreak > 0) s.shieldBreak -= dt;
            if (s.burrowTime > 0) s.burrowTime -= dt;
            if (s.brittleTime > 0 && (s.brittleTime -= dt) <= 0) s.brittle = 0;
            if (s.doomTime > 0 && (s.doomTime -= dt) <= 0) s.doom = 0;
            if (s.burnTime > 0) {
                game.combat.source = s.burnOwner;
                game.combat.tick(e, s.burnDps * dt, DamageType.MAGIC);
                if ((s.burnTime -= dt) <= 0) s.burnDps = 0;
            }
            if (s.poisonTime > 0) {
                game.combat.source = s.poisonOwner;
                game.combat.tick(e, s.poisonDps * dt, DamageType.MAGIC);
                if ((s.poisonTime -= dt) <= 0) {
                    s.poisonDps = 0;
                    s.contagious = false;
                }
            }
        }
        game.combat.source = World.NONE;
    }
}
