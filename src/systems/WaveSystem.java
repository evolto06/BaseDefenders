package systems;

import components.Enemy;
import ecs.GameSystem;
import ecs.Query;
import game.Game;
import game.WavePlan;
import java.util.List;

/** Spawns the active wave on schedule and counts down to the next one. */
public final class WaveSystem implements GameSystem {
    public static final float BREAK = 15;
    private static final float BREAK_WHEN_CLEAR = 5;
    private static final float EARLY_BONUS_PER_SECOND = 2;

    private final Game game;
    private final Query enemies;

    public WaveSystem(Game game) {
        this.game = game;
        this.enemies = game.world.query(Enemy.class);
    }

    @Override
    public void update(float dt) {
        boolean called = game.callWave;
        game.callWave = false;

        if (game.activePlan != null) {
            game.spawnClock += dt;
            List<WavePlan.Spawn> spawns = game.activePlan.spawns;
            while (game.spawnIndex < spawns.size() && spawns.get(game.spawnIndex).time() <= game.spawnClock) {
                game.prefabs.enemy(spawns.get(game.spawnIndex).type(), game.activePlan.hpScale * game.map.toughness);
                game.spawnIndex++;
            }
            if (game.spawnIndex == spawns.size()) {
                game.activePlan = null;
                game.waveTimer = BREAK;
            }
            return;
        }

        // The first wave waits until the player asks for it.
        if (game.wave == 0) {
            if (called) start(false);
            return;
        }

        boolean fieldClear = enemies.fetch().size() == 0;
        if (game.wave == Game.FINAL_WAVE && !game.victory && !fieldClear) return;
        if (game.wave > 0 && fieldClear) {
            if (!game.waveCleared) {
                game.waveCleared = true;
                game.records.record(game.map, game.wave);
                if (game.wave == Game.FINAL_WAVE && !game.victory) {
                    game.victory = true;
                    game.victoryScreen = true;
                }
            }
            game.waveTimer = Math.min(game.waveTimer, BREAK_WHEN_CLEAR);
        }
        game.waveTimer -= dt;
        if (called || game.waveTimer <= 0) start(called);
    }

    /** Mana granted for calling the next wave right now instead of waiting out the break. */
    public static int earlyBonus(Game game) {
        if (game.wave == 0 || game.spawning()) return 0;
        return Math.round(Math.max(0, game.waveTimer) * EARLY_BONUS_PER_SECOND);
    }

    private void start(boolean called) {
        int bonus = called ? earlyBonus(game) : 0;
        game.mana += bonus;
        game.wave++;
        game.activePlan = game.nextPlan;
        game.nextPlan = WavePlan.forWave(game.wave + 1, game.map);
        game.spawnIndex = 0;
        game.spawnClock = 0;
        game.waveCleared = false;
        String title = "Wave " + game.wave;
        if (game.activePlan.boss()) title += " - BOSS";
        if (bonus > 0) title += "   (+" + bonus + " mana for calling early)";
        game.toast(title);
    }
}
