package systems;

import components.Tower;
import ecs.GameSystem;
import ecs.Query;
import ecs.Store;
import game.EnemyType;
import game.Game;
import game.TowerType;

/** Shows a contextual tip the first time each mechanic becomes relevant. */
public final class TutorialSystem implements GameSystem {
    private static final float SHOW_SECONDS = 10;
    private static final float FULL_MANA_REPEAT = 45;
    private static final int MERGE = 1, FUSE = 2, BATS = 4, BOSS = 8;

    private final Game game;
    private final Query towerQuery;
    private final Store<Tower> towers;
    /** Tips already given; kept across restarts so a second run is not nagged. */
    private int shown;
    private float fullManaCooldown;

    public TutorialSystem(Game game) {
        this.game = game;
        this.towerQuery = game.world.query(Tower.class);
        this.towers = game.world.store(Tower.class);
    }

    @Override
    public void update(float dt) {
        game.hintTime = Math.max(0, game.hintTime - dt);
        fullManaCooldown -= dt;
        if (game.gameOver) {
            game.hintTime = 0;
            return;
        }
        towerQuery.fetch();
        if (game.wave == 0) {
            // Before the first wave the tip stays up and walks the player through the opening.
            game.hint = towerQuery.size() == 0
                    ? "Drag a card from your hand onto a tile beside the road (or press 1-6, then click a tile)."
                    : "Keep building while you have mana. Press SPACE when you are ready for the first wave.";
            game.hintTime = 1;
            return;
        }
        if (game.hintTime > 0) return;

        if (unseen(MERGE) && hasPair(true)) {
            show(MERGE, "Two identical towers! Drag one onto the other, or select one and press E, to upgrade it.");
        } else if (unseen(FUSE) && game.wave >= 2 && hasPair(false)) {
            show(FUSE, "Drag a tower onto a different base tower of the same tier to fuse a hybrid. Press C for the chart.");
        } else if (unseen(BATS) && game.nextPlan.counts.containsKey(EnemyType.BAT)) {
            show(BATS, "Bats next wave: they fly along the dotted trail, and Mortars cannot hit them.");
        } else if (unseen(BOSS) && game.nextPlan.counts.containsKey(EnemyType.GOLEM)) {
            show(BOSS, "A Golem is coming next wave. It is armored, so bring magic damage.");
        } else if (fullManaCooldown <= 0 && game.mana >= game.manaCap() - 1) {
            game.hint = "Your mana is full and going to waste. Build or upgrade something.";
            game.hintTime = SHOW_SECONDS / 2;
            fullManaCooldown = FULL_MANA_REPEAT;
        }
    }

    private boolean unseen(int tip) {
        return (shown & tip) == 0;
    }

    private void show(int tip, String text) {
        shown |= tip;
        game.hint = text;
        game.hintTime = SHOW_SECONDS;
    }

    /** Whether two towers exist that could upgrade (identical) or fuse (different bases). */
    private boolean hasPair(boolean identical) {
        for (int i = 0; i < towerQuery.size(); i++) {
            Tower a = towers.get(towerQuery.get(i));
            for (int j = i + 1; j < towerQuery.size(); j++) {
                Tower b = towers.get(towerQuery.get(j));
                if ((a.type == b.type) == identical && TowerType.merge(a.type, a.tier, b.type, b.tier) != null) {
                    return true;
                }
            }
        }
        return false;
    }
}
