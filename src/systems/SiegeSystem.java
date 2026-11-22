package systems;

import components.Chiller;
import components.Entangler;
import components.Position;
import components.Tower;
import ecs.GameSystem;
import ecs.Query;
import ecs.Store;
import game.Game;
import java.awt.Color;

/**
 * Bosses that strike at the towers themselves: an Elder Treant's roots hold the towers nearest it
 * so they cannot fire, and a Frost Giant's breath slows the reload of every tower around it.
 * A hexed boss does neither.
 */
public final class SiegeSystem implements GameSystem {
    private static final Color ROOTS = new Color(0x6B8E23), FROST = new Color(0xBAE6FD);
    private static final float RETRY = 0.3f;
    /** How often a Frost Giant's breath is renewed, and how long each breath lasts. */
    private static final float CHILL_EVERY = 0.25f, CHILL_SECONDS = 0.6f;

    private final Game game;
    private final Query entanglerQuery, chillerQuery, towerQuery;
    private final Store<Entangler> entanglers;
    private final Store<Chiller> chillers;
    private final Store<Position> positions;
    private final Store<Tower> towers;
    /** The towers an Entangler is about to root, nearest first. */
    private int[] chosen = new int[4];
    private float[] chosenDist = new float[4];

    public SiegeSystem(Game game) {
        this.game = game;
        this.entanglerQuery = game.world.query(Entangler.class, Position.class);
        this.chillerQuery = game.world.query(Chiller.class, Position.class);
        this.towerQuery = game.world.query(Tower.class, Position.class);
        this.entanglers = game.world.store(Entangler.class);
        this.chillers = game.world.store(Chiller.class);
        this.positions = game.world.store(Position.class);
        this.towers = game.world.store(Tower.class);
    }

    @Override
    public void update(float dt) {
        entanglerQuery.fetch();
        chillerQuery.fetch();
        if (entanglerQuery.size() == 0 && chillerQuery.size() == 0) return;
        towerQuery.fetch();
        for (int i = 0; i < entanglerQuery.size(); i++) entangle(entanglerQuery.get(i), dt);
        for (int i = 0; i < chillerQuery.size(); i++) chill(chillerQuery.get(i), dt);
    }

    private void entangle(int e, float dt) {
        Entangler roots = entanglers.get(e);
        if ((roots.timer -= dt) > 0) return;
        roots.timer = RETRY;
        if (game.combat.hexed(e)) return;
        Position p = positions.get(e);
        if (chosen.length < roots.count) {
            chosen = new int[roots.count];
            chosenDist = new float[roots.count];
        }
        int found = 0;
        float r2 = roots.radius * roots.radius;
        for (int i = 0; i < towerQuery.size(); i++) {
            int t = towerQuery.get(i);
            if (towers.get(t).tangleTime > 0) continue;
            Position tp = positions.get(t);
            float dx = tp.x - p.x, dy = tp.y - p.y, d2 = dx * dx + dy * dy;
            if (d2 > r2) continue;
            // Insertion into the short list of nearest towers.
            int slot = Math.min(found, roots.count - 1);
            if (found >= roots.count && d2 >= chosenDist[slot]) continue;
            while (slot > 0 && chosenDist[slot - 1] > d2) {
                chosen[slot] = chosen[slot - 1];
                chosenDist[slot] = chosenDist[slot - 1];
                slot--;
            }
            chosen[slot] = t;
            chosenDist[slot] = d2;
            if (found < roots.count) found++;
        }
        if (found == 0) return;
        for (int k = 0; k < found; k++) {
            Tower tower = towers.get(chosen[k]);
            tower.tangleTime = roots.seconds;
            Position tp = positions.get(chosen[k]);
            game.prefabs.ring(tp.x, tp.y, 26, ROOTS, 0.4f);
            game.prefabs.burst(tp.x, tp.y, ROOTS, 5);
        }
        game.prefabs.ring(p.x, p.y, 30, ROOTS, 0.4f);
        roots.timer = roots.interval;
    }

    private void chill(int e, float dt) {
        Chiller breath = chillers.get(e);
        if ((breath.timer -= dt) > 0) return;
        breath.timer = CHILL_EVERY;
        if (game.combat.hexed(e)) return;
        Position p = positions.get(e);
        float r2 = breath.radius * breath.radius;
        for (int i = 0; i < towerQuery.size(); i++) {
            int t = towerQuery.get(i);
            Position tp = positions.get(t);
            float dx = tp.x - p.x, dy = tp.y - p.y;
            if (dx * dx + dy * dy <= r2) towers.get(t).chillTime = CHILL_SECONDS;
        }
        if (game.rng.nextFloat() < 0.25f) game.prefabs.ring(p.x, p.y, breath.radius, FROST, 0.5f);
    }
}
