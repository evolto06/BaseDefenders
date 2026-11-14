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
    GOLEM("Golem",       1100,  22,  8, 10, 22, false, false, 0, 5.00f, 3.0f, 0x8A8F98,
            "Boss. Heavy armor, shrugs off slows. Costs 10 lives."),
    DRAGON("Dragon",      650,  27,  3, 10, 22, true,  false, 0, 5.00f, 3.0f, 0xD9482B,
            "Boss. Flies straight for the keep, out of reach of Mortars. Costs 10 lives."),
    LICH("Lich",          700,  24,  2, 10, 20, false, false, 0, 5.00f, 3.0f, 0x8B5CF6,
            "Boss. Mends the monsters around it and raises Skeletons; magic deals 30% less. Hex it."),
    HYDRA("Hydra",        700,  24,  3, 10, 22, false, false, 0, 5.00f, 3.0f, 0x1F9E89,
            "Boss. Regrows its health and splits into three Hydra Spawn when slain. Hex it."),
    HYDRA_SPAWN("Hydra Spawn", 90, 48, 1, 3, 13, false, false, 0, 1.00f, 2.0f, 0x1F9E89,
            "The brood a slain Hydra leaves behind. Costs 3 lives.");

    /** The bosses, in the order they first appear. */
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
            case GOLEM, DRAGON, LICH, HYDRA -> true;
            default -> false;
        };
    }

    /** Whether it strengthens the monsters around it: the first thing a Witch Hunter shoots. */
    public boolean caster() {
        return switch (this) {
            case SHAMAN, DRUMMER, NECROMANCER, LICH -> true;
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

    /** What it breaks into when killed unhexed, or null. */
    public EnemyType splitsInto() {
        return switch (this) {
            case SLIME -> MINI_SLIME;
            case HYDRA -> HYDRA_SPAWN;
            default -> null;
        };
    }

    public int splitCount() {
        return this == HYDRA ? 3 : 2;
    }
}
