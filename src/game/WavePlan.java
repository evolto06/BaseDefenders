package game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** The spawn schedule for one wave, generated deterministically from the wave number and map. */
public final class WavePlan {
    public record Spawn(float time, EnemyType type) {}

    private static final EnemyType[] UNLOCK_ORDER = {
        EnemyType.SLIME, EnemyType.GOBLIN, EnemyType.ORC, EnemyType.BAT, EnemyType.WARDEN, EnemyType.SHAMAN,
        EnemyType.WRAITH, EnemyType.TROLL, EnemyType.KNIGHT, EnemyType.GARGOYLE, EnemyType.DRUMMER,
        EnemyType.NECROMANCER
    };
    private static final int[] UNLOCK_WAVE = {1, 2, 3, 4, 6, 7, 9, 11, 13, 16, 18, 21};
    /**
     * When the map's resident monster first appears. From then on it leads a group every other
     * wave, so each map keeps asking its own question of the defence.
     */
    private static final int RESIDENT_WAVE = 8;
    private static final int BOSS_EVERY = 5;
    /** Largest number of one monster type in a wave, so swarms stay readable. */
    private static final int MAX_GROUP = 40;

    public final int wave;
    public final float hpScale;
    public final List<Spawn> spawns = new ArrayList<>();
    public final Map<EnemyType, Integer> counts = new EnumMap<>(EnemyType.class);

    private WavePlan(int wave) {
        this.wave = wave;
        float w = wave - 1;
        this.hpScale = 1 + 0.2f * w + 0.022f * w * w;
    }

    public static WavePlan forWave(int wave, MapDef map) {
        WavePlan plan = new WavePlan(wave);
        Random random = new Random(9176L * wave + 13);

        List<EnemyType> pool = new ArrayList<>();
        EnemyType newcomer = null;
        for (int i = 0; i < UNLOCK_ORDER.length; i++) {
            if (wave >= UNLOCK_WAVE[i]) pool.add(UNLOCK_ORDER[i]);
            if (wave == UNLOCK_WAVE[i]) newcomer = UNLOCK_ORDER[i];
        }
        EnemyType resident = map.resident;
        if (wave >= RESIDENT_WAVE) pool.add(resident);
        if (wave == RESIDENT_WAVE) newcomer = resident;
        Collections.shuffle(pool, random);
        if (wave > RESIDENT_WAVE && wave % 2 == 0) {
            pool.remove(resident);
            pool.add(0, resident);
        }
        if (newcomer != null) {
            // A newly unlocked monster always shows up on its first wave.
            pool.remove(newcomer);
            pool.add(0, newcomer);
        }

        boolean bossWave = wave % BOSS_EVERY == 0;
        float budget = (12 + wave * 5.5f) * (bossWave ? 0.6f : 1f);
        int groups = Math.min(pool.size(), wave < 2 ? 1 : wave < 8 ? 2 : 3);
        float pace = Math.max(0.55f, 1 - 0.012f * wave);
        for (int g = 0; g < groups; g++) {
            EnemyType type = pool.get(g);
            int count = Math.clamp(Math.round(budget / groups / type.cost), 1, MAX_GROUP);
            for (int k = 0; k < count; k++) plan.add(g * 3f + k * type.interval * pace, type);
        }
        if (bossWave) {
            // Each boss wave is led by the next boss in line, backed by the ones before it, so
            // a new boss arrives beside a familiar one and the final wave fields all four.
            int bosses = 1 + wave / 10;
            int lead = wave / BOSS_EVERY - 1;
            for (int k = 0; k < bosses; k++) {
                plan.add(5 + k * 6f, EnemyType.BOSSES[Math.floorMod(lead - k, EnemyType.BOSSES.length)]);
            }
        }
        plan.spawns.sort(Comparator.comparingDouble(Spawn::time));
        return plan;
    }

    public boolean boss() {
        return wave % BOSS_EVERY == 0;
    }

    private void add(float time, EnemyType type) {
        spawns.add(new Spawn(time, type));
        counts.merge(type, 1, Integer::sum);
    }
}
