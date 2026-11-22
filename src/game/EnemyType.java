package game;

import java.awt.Color;

public enum EnemyType {
    //                     hp speed armor lives radius flying immune cost interval scale colour
    SLIME("Slime",         38,  44,  0,  1, 12, false, false, 2, 0.75f, 2.0f, 0x5BBF4A,
            "Splits into two Slimelets when killed, unless it dies hexed."),
    MINI_SLIME("Slimelet", 13,  62,  0,  1,  8, false, false, 1, 0.40f, 1.5f, 0x5BBF4A,
            "Small and quick."),
    GOBLIN("Goblin",       24,  86,  0,  1, 10, false, false, 1, 0.40f, 2.0f, 0x7FB23A,
            "Fast and fragile. Comes in swarms."),
    ORC("Orc",            120,  34,  4,  2, 13, false, false, 4, 1.30f, 2.0f, 0x4E8F3A,
            "Armored: physical hits are reduced. Costs 2 lives."),
    BAT("Bat",             30,  72,  0,  1, 10, true,  false, 3, 0.60f, 2.0f, 0x7C4DBE,
            "Flies straight to the keep along the dotted trail. Mortars cannot hit it."),
    SHAMAN("Shaman",       70,  40,  1,  2, 12, false, false, 6, 1.80f, 2.0f, 0x3F6FD8,
            "Heals the monsters around it. A hex silences it. Costs 2 lives."),
    WRAITH("Wraith",       55,  62,  0,  1, 11, false, true,  3, 0.90f, 2.0f, 0xBFEFF5,
            "Cannot be slowed or frozen, unless it is hexed."),
    TROLL("Troll",        150,  36,  1,  2, 13, false, false, 5, 1.40f, 2.0f, 0x5E8C6A,
            "Regrows its health as it walks. A hex stops that. Costs 2 lives."),
    KNIGHT("Rune Knight", 105,  40,  2,  2, 12, false, false, 5, 1.20f, 2.0f, 0x6D5BD0,
            "Warded: magic deals 60% less. Bring physical damage or hex it. Costs 2 lives."),
    GARGOYLE("Gargoyle",   62,  54,  5,  2, 12, true,  false, 5, 1.00f, 2.0f, 0x7B8494,
            "Flies, and its stone hide is armored. Mortars cannot hit it. Costs 2 lives."),
    DRUMMER("War Drummer", 60,  44,  0,  1, 11, false, false, 5, 1.60f, 2.0f, 0xC2562B,
            "Its drumming speeds up every monster around it. A hex silences it."),
    NECROMANCER("Necromancer", 80, 34, 0, 2, 12, false, false, 8, 2.20f, 2.0f, 0x7E3FA8,
            "Raises Skeletons as it walks. A hex silences it. Costs 2 lives."),
    SKELETON("Skeleton",   14,  58,  0,  1,  9, false, false, 1, 0.40f, 1.5f, 0xE8E0D0,
            "Raised by a Necromancer or a Lich. Weak, but they keep coming."),
    WARDEN("Aegis Warden", 80, 38,  2,  2, 12, false, false, 6, 1.80f, 2.0f, 0xD4A72C,
            "Shields the monsters around it. A shield swallows the next hit whole, freeze and all, "
            + "so fast-firing and area towers break them best. A hex silences it. Costs 2 lives."),
    IMP("Blink Imp",       40,  64,  0,  1, 10, false, false, 3, 0.70f, 2.0f, 0xE05AA8,
            "Blinks ahead down the road when it is frozen or struck hard, shaking off the ice. "
            + "A hex grounds it."),
    BORER("Dune Borer",    95,  46,  2,  2, 12, false, false, 5, 1.20f, 2.0f, 0xC2915A,
            "Burrows under the road for stretches, where nothing can touch it, and comes up thawed. "
            + "A hex keeps it above ground. Costs 2 lives."),
    YETI("Yeti",          150,  36,  1,  2, 13, false, false, 6, 1.50f, 2.0f, 0xDDE8F0,
            "At home in the cold: slows and freezes slide off it and mend it instead. "
            + "A hex numbs it. Costs 2 lives."),
    SALAMANDER("Salamander", 70, 56, 0,  1, 11, false, false, 4, 0.90f, 2.0f, 0xF97316,
            "Fireproof. Thaws from a freeze in a quarter of the time and bolts off in a fury, "
            + "and slows only half hold it. A hex calms it."),
    GOLEM("Golem",       1100,  22,  8, 10, 22, false, false, 0, 5.00f, 2.0f, 0x8A8F98,
            "Boss. Heavy armor, shrugs off slows. Costs 10 lives."),
    DRAGON("Dragon",      650,  27,  3, 10, 22, true,  false, 0, 5.00f, 2.0f, 0xD9482B,
            "Boss. Flies straight for the keep, out of reach of Mortars. Costs 10 lives."),
    LICH("Lich",          700,  24,  2, 10, 20, false, false, 0, 5.00f, 2.0f, 0x8B5CF6,
            "Boss. Mends the monsters around it and raises Skeletons; magic deals 30% less. Hex it."),
    HYDRA("Hydra",        700,  24,  3, 10, 22, false, false, 0, 5.00f, 2.0f, 0x1F9E89,
            "Boss. Regrows its health and splits into three Hydra Spawn when slain. Hex it."),
    HYDRA_SPAWN("Hydra Spawn", 90, 48, 1, 3, 13, false, false, 0, 1.00f, 2.0f, 0x1F9E89,
            "The brood a slain Hydra leaves behind. Costs 3 lives."),
    TREANT("Elder Treant", 1300, 18, 6, 10, 22, false, false, 0, 5.00f, 2.0f, 0x5B7A2E,
            "Boss of Green Meadow. Its bark is armored, and its roots tangle the towers nearest it "
            + "so they cannot fire. Hex it to still the roots. Costs 10 lives."),
    SAND_WYRM("Sand Wyrm", 900, 26,  3, 10, 22, false, false, 0, 5.00f, 2.0f, 0xC2915A,
            "Boss of Dusty Canyon. Burrows under the road out of reach and calls up Dune Borers. "
            + "A hex keeps it above ground and quiet. Costs 10 lives."),
    FROST_GIANT("Frost Giant", 1000, 22, 3, 10, 22, false, false, 0, 5.00f, 2.0f, 0x7DB4D6,
            "Boss of Frostbite Pass. Cold mends it, and its frozen breath halves the fire rate of "
            + "towers near it. Hex it. Costs 10 lives."),
    PHOENIX("Phoenix",     600, 28,  1, 10, 22, true,  false, 0, 5.00f, 2.0f, 0xF97316,
            "Boss of Ember Fields. Flies, is fireproof, and rises again from its ashes at half "
            + "strength unless it dies hexed. Costs 10 lives."),
    PHOENIX_REBORN("Reborn Phoenix", 300, 34, 1, 10, 20, true, false, 0, 5.00f, 2.0f, 0xFDE047,
            "A Phoenix risen from its ashes: faster, and fireproof still. Costs 10 lives.");

    /** The bosses every map shares, in the order they first appear. Each map adds its own after them. */
    public static final EnemyType[] BOSSES = {GOLEM, DRAGON, LICH, HYDRA};

    public final String label;
    public final float hp, speed, armor;
    /** Lives lost when this enemy reaches the base. */
    public final int lives;
    public final float radius;
    public final boolean flying;
    /** Immune to slows, stuns and fear while it is not hexed. */
    public final boolean immune;
    /** Wave budget cost; 0 means it never fills out a wave: bosses and what monsters spawn. */
    public final int cost;
    public final float interval;
    public final float scale;
    public final Color color;
    /** What makes this monster different, for the wave preview tooltip. */
    public final String trait;

    EnemyType(String label, float hp, float speed, float armor, int lives, float radius, boolean flying,
              boolean immune, int cost, float interval, float scale, int rgb, String trait) {
        this.label = label;
        this.hp = hp;
        this.speed = speed;
        this.armor = armor;
        this.lives = lives;
        this.radius = radius;
        this.flying = flying;
        this.immune = immune;
        this.cost = cost;
        this.interval = interval;
        this.scale = scale;
        this.color = new Color(rgb);
        this.trait = trait;
    }

    public boolean boss() {
        return switch (this) {
            case GOLEM, DRAGON, LICH, HYDRA, TREANT, SAND_WYRM, FROST_GIANT, PHOENIX, PHOENIX_REBORN -> true;
            default -> false;
        };
    }

    /** Whether it strengthens the monsters around it: the first thing a Witch Hunter shoots. */
    public boolean caster() {
        return switch (this) {
            case SHAMAN, DRUMMER, NECROMANCER, WARDEN, LICH -> true;
            default -> false;
        };
    }

    /** Share of magic damage its ward turns aside while it is not hexed. */
    public float ward() {
        return switch (this) {
            case KNIGHT -> 0.6f;
            case LICH -> 0.3f;
            default -> 0;
        };
    }

    /** Whether the cold mends it instead of slowing or freezing it, while it is not hexed. */
    public boolean coldborn() {
        return this == YETI || this == FROST_GIANT;
    }

    /** Whether burns cannot touch it. */
    public boolean fireproof() {
        return switch (this) {
            case SALAMANDER, PHOENIX, PHOENIX_REBORN -> true;
            default -> false;
        };
    }

    /** What it breaks into, or rises again as, when killed unhexed, or null. */
    public EnemyType splitsInto() {
        return switch (this) {
            case SLIME -> MINI_SLIME;
            case HYDRA -> HYDRA_SPAWN;
            case PHOENIX -> PHOENIX_REBORN;
            default -> null;
        };
    }

    public int splitCount() {
        return switch (this) {
            case HYDRA -> 3;
            case PHOENIX -> 1;
            default -> 2;
        };
    }
}
