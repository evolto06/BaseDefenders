package game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** The spawn schedule for one wave, generated deterministically from the wave number. */
public final class WavePlan {
    public record Spawn(float time, EnemyType type) {}

    private static final EnemyType[] UNLOCK_ORDER = {
        EnemyType.SLIME, EnemyType.GOBLIN, EnemyType.ORC, EnemyType.BAT, EnemyType.SHAMAN, EnemyType.WRAITH
    };
    private static final int[] UNLOCK_WAVE = {1, 2, 3, 4, 7, 9};
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

    public static WavePlan forWave(int wave) {
        WavePlan plan = new WavePlan(wave);
        Random random = new Random(9176L * wave + 13);

        List<EnemyType> pool = new ArrayList<>();
        EnemyType newcomer = null;
        for (int i = 0; i < UNLOCK_ORDER.length; i++) {
            if (wave >= UNLOCK_WAVE[i]) pool.add(UNLOCK_ORDER[i]);
            if (wave == UNLOCK_WAVE[i]) newcomer = UNLOCK_ORDER[i];
        }
        Collections.shuffle(pool, random);
        if (newcomer != null) {
            // A newly unlocked monster always shows up on its first wave.
            pool.remove(newcomer);
            pool.add(0, newcomer);
        }

        boolean bossWave = wave % 5 == 0;
        float budget = (12 + wave * 5.5f) * (bossWave ? 0.6f : 1f);
        int groups = Math.min(pool.size(), wave < 2 ? 1 : wave < 8 ? 2 : 3);
        float pace = Math.max(0.55f, 1 - 0.012f * wave);
        for (int g = 0; g < groups; g++) {
            EnemyType type = pool.get(g);
            int count = Math.clamp(Math.round(budget / groups / type.cost), 1, MAX_GROUP);
            for (int k = 0; k < count; k++) plan.add(g * 3f + k * type.interval * pace, type);
        }
        if (bossWave) {
            int bosses = 1 + wave / 10;
            for (int k = 0; k < bosses; k++) plan.add(5 + k * 6f, EnemyType.GOLEM);
        }
        plan.spawns.sort(Comparator.comparingDouble(Spawn::time));
        return plan;
    }

    private void add(float time, EnemyType type) {
        spawns.add(new Spawn(time, type));
        counts.merge(type, 1, Integer::sum);
    }
}
