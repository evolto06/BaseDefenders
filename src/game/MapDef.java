package game;

/**
 * The playable maps. A route lists the road's corners in tile coordinates; its first and last
 * points lie just off the left and right edges of the field. Harder maps have shorter roads
 * and tougher monsters, and each map has a resident monster and a boss met nowhere else.
 */
public enum MapDef {
    MEADOW("Green Meadow", "Easy", 1.0f, Theme.MEADOW, 11, EnemyType.IMP, EnemyType.TREANT,
            "A long, winding road. Plenty of time to shoot.",
            new int[][] {{-1, 1}, {4, 1}, {4, 7}, {9, 7}, {9, 2}, {14, 2}, {14, 8}, {19, 8}, {19, 4}, {24, 4}}),
    CANYON("Dusty Canyon", "Normal", 1.12f, Theme.DESERT, 23, EnemyType.BORER, EnemyType.SAND_WYRM,
            "Wide switchbacks through the sand. Monsters are 12% tougher.",
            new int[][] {{-1, 2}, {6, 2}, {6, 7}, {12, 7}, {12, 2}, {18, 2}, {18, 7}, {24, 7}}),
    FROST("Frostbite Pass", "Hard", 1.25f, Theme.SNOW, 37, EnemyType.YETI, EnemyType.FROST_GIANT,
            "A shorter road over the ice. Monsters are 25% tougher.",
            new int[][] {{-1, 8}, {5, 8}, {5, 3}, {11, 3}, {11, 6}, {17, 6}, {17, 1}, {24, 1}}),
    EMBER("Ember Fields", "Brutal", 1.6f, Theme.EMBER, 51, EnemyType.SALAMANDER, EnemyType.PHOENIX,
            "Barely a bend before your keep. Monsters are 60% tougher.",
            new int[][] {{-1, 4}, {7, 4}, {7, 7}, {16, 7}, {16, 2}, {24, 2}});

    public final String label, difficulty, blurb;
    /** Multiplier on every monster's health on this map. */
    public final float toughness;
    public final Theme theme;
    /** Seed for scenery placement. */
    public final long seed;
    public final int[][] route;
    /** The monster found only on this map, which keeps turning up once it has arrived. */
    public final EnemyType resident;
    /** This map's own boss, which joins the shared ones on the later boss waves. */
    public final EnemyType boss;

    MapDef(String label, String difficulty, float toughness, Theme theme, long seed, EnemyType resident,
           EnemyType boss, String blurb, int[][] route) {
        this.label = label;
        this.difficulty = difficulty;
        this.toughness = toughness;
        this.theme = theme;
        this.seed = seed;
        this.blurb = blurb;
        this.route = route;
        this.resident = resident;
        this.boss = boss;
    }
}
