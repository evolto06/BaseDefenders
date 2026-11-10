package game;

import java.util.prefs.Preferences;

/** The best wave cleared on each map, remembered between sessions where the platform allows it. */
public final class Records {
    private final int[] best = new int[MapDef.values().length];
    private Preferences store;

    public Records() {
        try {
            store = Preferences.userRoot().node("basedefenders");
            for (MapDef map : MapDef.values()) best[map.ordinal()] = store.getInt(key(map), 0);
        } catch (RuntimeException e) {
            // No preference store available: records simply last for this session.
            store = null;
        }
    }

    public int best(MapDef map) {
        return best[map.ordinal()];
    }

    public void record(MapDef map, int wave) {
        if (wave <= best[map.ordinal()]) return;
        best[map.ordinal()] = wave;
        if (store == null) return;
        try {
            store.putInt(key(map), wave);
        } catch (RuntimeException e) {
            store = null;
        }
    }

    private static String key(MapDef map) {
        return "best." + map.name().toLowerCase();
    }
}
