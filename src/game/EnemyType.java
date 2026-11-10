package game;

import java.awt.Color;

public enum EnemyType {
    //                     hp speed armor lives radius flying immune cost interval scale colour
    SLIME("Slime",         38,  44,  0,  1, 12, false, false, 2, 0.75f, 2.0f, 0x5BBF4A,
            "Splits into two Slimelets when killed."),
    MINI_SLIME("Slimelet", 13,  62,  0,  1,  8, false, false, 1, 0.40f, 1.5f, 0x5BBF4A,
            "Small and quick."),
    GOBLIN("Goblin",       24,  86,  0,  1, 10, false, false, 1, 0.40f, 2.0f, 0x7FB23A,
            "Fast and fragile. Comes in swarms."),
    ORC("Orc",            120,  34,  4,  2, 13, false, false, 4, 1.30f, 2.0f, 0x4E8F3A,
            "Armored: physical hits are reduced. Costs 2 lives."),
    BAT("Bat",             30,  72,  0,  1, 10, true,  false, 3, 0.60f, 2.0f, 0x7C4DBE,
            "Flies straight to the keep along the dotted trail. Mortars cannot hit it."),
    SHAMAN("Shaman",       70,  40,  1,  2, 12, false, false, 6, 1.80f, 2.0f, 0x3F6FD8,
            "Heals the monsters around it. Costs 2 lives."),
    WRAITH("Wraith",       55,  62,  0,  1, 11, false, true,  3, 0.90f, 2.0f, 0xBFEFF5,
            "Cannot be slowed or frozen."),
    GOLEM("Golem",       1100,  22,  8, 10, 22, false, false, 0, 5.00f, 3.0f, 0x8A8F98,
            "Boss. Heavy armor, shrugs off slows. Costs 10 lives.");

    public final String label;
    public final float hp, speed, armor;
    /** Lives lost when this enemy reaches the base. */
    public final int lives;
    public final float radius;
    public final boolean flying;
    /** Immune to slows and stuns. */
    public final boolean immune;
    /** Wave budget cost; 0 means it is only spawned as a boss. */
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
        return this == GOLEM;
    }
}
