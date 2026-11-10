package game;

import static game.DamageType.MAGIC;
import static game.DamageType.PHYSICAL;

import java.awt.Color;

/**
 * Every tower kind: six base towers and the fifteen hybrids made by fusing two different bases.
 * All numbers are tier-1 values; the tier methods scale them.
 */
public enum TowerType {
    //                          attack  cost range reload dmg  type      air   turns  shot speed  colour
    ARROW("Ballista",      Attack.BOLT,  40, 150, 0.5f, 12,   PHYSICAL, true,  true,  420, 0xB5803A,
            "Rapid bolts at a single target. Long range."),
    CANNON("Mortar",       Attack.SHELL, 60, 125, 1.7f, 28,   PHYSICAL, false, true,  260, 0x8B93A1,
            "Explosive shells splash an area. Cannot hit flyers."),
    FROST("Frost Obelisk", Attack.PULSE, 50,  95, 1.1f, 4,    MAGIC,    true,  false, 0,   0x38BDF8,
            "Icy pulses chill every enemy in range."),
    TESLA("Tesla Coil",    Attack.CHAIN, 70, 115, 1.0f, 18,   MAGIC,    true,  false, 0,   0xFACC15,
            "Lightning arcs between enemies. Ignores armor."),
    VENOM("Venom Spitter", Attack.BOLT,  55, 130, 0.8f, 5,    MAGIC,    true,  true,  300, 0x84CC16,
            "Spits poison that eats away at its victims. Spreads its shots across the unpoisoned."),
    PRISM("Sun Prism",     Attack.BEAM,  80, 120, 0.1f, 2.8f, MAGIC,    true,  false, 0,   0xFB923C,
            "A constant beam that burns up to 3x hotter the longer it holds one target."),

    FLAK(ARROW, CANNON, "Flak Battery", Attack.BOLT, 125, 0.4f, 14, PHYSICAL, true, true, 480,
            "Rapid shrapnel bursts. +50% damage to flyers.", "Shorter range than a Ballista."),
    ICE_LANCE(ARROW, FROST, "Glacier Lance", Attack.BOLT, 230, 2.2f, 100, MAGIC, true, true, 700,
            "Huge-range lance that freezes its target solid.", "Very slow reload."),
    RAILGUN(ARROW, TESLA, "Storm Rail", Attack.RAIL, 170, 1.9f, 55, MAGIC, true, true, 0,
            "A beam that pierces every enemy in a line.", "Slow to recharge."),
    BLIGHT(ARROW, VENOM, "Blight Bow", Attack.BOLT, 150, 1.0f, 6, MAGIC, true, true, 380,
            "Looses three poisoned bolts at three different targets.", "Each bolt hits weakly."),
    SUNBOW(ARROW, PRISM, "Giant Slayer", Attack.BOLT, 190, 1.2f, 55, MAGIC, true, true, 600,
            "Searing bolts burn away a share of the target's maximum health.", "Slow, and wasted on small fry."),
    BLIZZARD(CANNON, FROST, "Blizzard Mortar", Attack.SHELL, 135, 2.4f, 14, MAGIC, false, true, 240,
            "Shells leave a freezing field that slows and chills.", "Low impact damage. Cannot hit flyers."),
    PLASMA(CANNON, TESLA, "Plasma Mortar", Attack.SHELL, 100, 1.8f, 34, MAGIC, false, true, 260,
            "Armor-ignoring blasts that set enemies ablaze.", "Short range. Cannot hit flyers."),
    ACID(CANNON, VENOM, "Acid Mortar", Attack.SHELL, 125, 2.0f, 24, MAGIC, false, true, 250,
            "Corrosive splash dissolves armor and poisons.", "Low damage alone. Cannot hit flyers."),
    CLUSTER(CANNON, PRISM, "Cluster Mortar", Attack.SHELL, 140, 2.0f, 50, PHYSICAL, false, true, 250,
            "Each shell bursts into four bomblets around the blast.", "Slow reload. Cannot hit flyers."),
    STORM(FROST, TESLA, "Tempest Spire", Attack.CHAIN, 120, 1.0f, 12, MAGIC, true, false, 0,
            "Long chains turn enemies Brittle so they take more damage.", "Low damage of its own."),
    MIASMA(FROST, VENOM, "Miasma Obelisk", Attack.PULSE, 100, 1.2f, 4, MAGIC, true, false, 0,
            "Toxic pulses slow and poison everything in range.", "Short range, slow to kill."),
    AURORA(FROST, PRISM, "Aurora Prism", Attack.BEAM, 130, 0.1f, 4.0f, MAGIC, true, false, 0,
            "A freezing beam that slows its target while it ramps up.", "One target at a time."),
    PLAGUE(TESLA, VENOM, "Plague Coil", Attack.CHAIN, 115, 1.1f, 10, MAGIC, true, false, 0,
            "Chains spread a plague that jumps to nearby enemies when its host dies.", "Low direct damage."),
    ARC(TESLA, PRISM, "Arc Prism", Attack.BEAM, 105, 0.1f, 2.0f, MAGIC, true, false, 0,
            "Splits its ramping beam across three targets.", "Shorter range, weaker per beam."),
    SIPHON(VENOM, PRISM, "Siphon Ray", Attack.BEAM, 120, 0.1f, 2.2f, MAGIC, true, false, 0,
            "Drains its target, generating extra mana while it fires.", "Weak damage.");

    /** How a tower delivers its attack. */
    public enum Attack {
        /** Homing projectile. */
        BOLT,
        /** Lobbed shell that lands on a spot. */
        SHELL,
        /** Instant burst hitting everything in range. */
        PULSE,
        /** Lightning that jumps between enemies. */
        CHAIN,
        /** Instant piercing line. */
        RAIL,
        /** Continuous beam that ramps up on a held target. */
        BEAM
    }

    public static final int MAX_TIER = 4;
    public static final TowerType[] BASES = {ARROW, CANNON, FROST, TESLA, VENOM, PRISM};
    public static final TowerType[] HYBRIDS;
    /** A beam deals this many times its base damage once fully ramped. */
    public static final float BEAM_RAMP = 3;
    public static final float BEAM_RAMP_SECONDS = 3;
    private static final float TIER_DAMAGE = 2.2f;

    static {
        HYBRIDS = new TowerType[values().length - BASES.length];
        int i = 0;
        for (TowerType t : values()) {
            if (t.hybrid()) HYBRIDS[i++] = t;
        }
    }

    public final String label;
    public final Attack attack;
    public final int cost;
    public final float range, cooldown, damage;
    public final DamageType damageType;
    public final boolean hitsAir;
    /** Whether the turret turns to face its target. */
    public final boolean rotates;
    public final float projectileSpeed;
    public final Color color, colorB;
    public final String description;
    /** The downside of a hybrid; null for base towers. */
    public final String tradeoff;
    public final TowerType parentA, parentB;

    TowerType(String label, Attack attack, int cost, float range, float cooldown, float damage,
              DamageType damageType, boolean hitsAir, boolean rotates, float projectileSpeed, int rgb,
              String description) {
        this.label = label;
        this.attack = attack;
        this.cost = cost;
        this.range = range;
        this.cooldown = cooldown;
        this.damage = damage;
        this.damageType = damageType;
        this.hitsAir = hitsAir;
        this.rotates = rotates;
        this.projectileSpeed = projectileSpeed;
        this.color = new Color(rgb);
        this.colorB = this.color;
        this.description = description;
        this.tradeoff = null;
        this.parentA = null;
        this.parentB = null;
    }

    TowerType(TowerType a, TowerType b, String label, Attack attack, float range, float cooldown, float damage,
              DamageType damageType, boolean hitsAir, boolean rotates, float projectileSpeed,
              String description, String tradeoff) {
        this.label = label;
        this.attack = attack;
        this.cost = a.cost + b.cost;
        this.range = range;
        this.cooldown = cooldown;
        this.damage = damage;
        this.damageType = damageType;
        this.hitsAir = hitsAir;
        this.rotates = rotates;
        this.projectileSpeed = projectileSpeed;
        this.color = a.color;
        this.colorB = b.color;
        this.description = description;
        this.tradeoff = tradeoff;
        this.parentA = a;
        this.parentB = b;
    }

    public boolean hybrid() {
        return parentA != null;
    }

    public boolean canHit(EnemyType enemy) {
        return hitsAir || !enemy.flying;
    }

    // ---- merging -------------------------------------------------------------------------

    /**
     * The tower produced by merging two towers, or null if they cannot merge. Two identical
     * towers of the same tier (base or hybrid) upgrade a tier; two different base towers of the
     * same tier fuse into a hybrid of that tier.
     */
    public static TowerType merge(TowerType a, int tierA, TowerType b, int tierB) {
        if (tierA != tierB) return null;
        if (a == b) return tierA < MAX_TIER ? a : null;
        return hybridOf(a, b);
    }

    public static int mergedTier(TowerType a, int tier, TowerType b) {
        return a == b ? tier + 1 : tier;
    }

    public static TowerType hybridOf(TowerType a, TowerType b) {
        for (TowerType t : HYBRIDS) {
            if ((t.parentA == a && t.parentB == b) || (t.parentA == b && t.parentB == a)) return t;
        }
        return null;
    }

    // ---- tier-scaled stats ---------------------------------------------------------------

    private static float tierScale(int tier) {
        return (float) Math.pow(TIER_DAMAGE, tier - 1);
    }

    /** Damage per hit; for beams, per tick before ramping. */
    public float damage(int tier) {
        return damage * tierScale(tier);
    }

    /** Sustained damage per second against one target, for display. */
    public float dps(int tier) {
        return damage(tier) / cooldown;
    }

    public float range(int tier) {
        return range * (1 + 0.08f * (tier - 1));
    }

    /** How many enemies one attack is aimed at. */
    public int targets() {
        return this == BLIGHT || this == ARC ? 3 : 1;
    }

    public float splash(int tier) {
        float base = switch (this) {
            case CANNON -> 46;
            case FLAK -> 30;
            case BLIZZARD -> 40;
            case PLASMA -> 52;
            case ACID -> 50;
            case CLUSTER -> 40;
            default -> 0;
        };
        return base * (1 + 0.1f * (tier - 1));
    }

    public int chains(int tier) {
        return switch (this) {
            case TESLA -> 2 + tier;
            case STORM -> 4 + tier;
            case PLAGUE -> 3 + tier;
            default -> 0;
        };
    }

    public float slow(int tier) {
        return switch (this) {
            case FROST -> 0.30f + 0.06f * (tier - 1);
            case BLIZZARD -> 0.45f + 0.05f * (tier - 1);
            case MIASMA -> 0.25f + 0.04f * (tier - 1);
            case AURORA -> 0.45f + 0.05f * (tier - 1);
            case STORM -> 0.20f;
            case ICE_LANCE -> 0.40f;
            default -> 0;
        };
    }

    public float stun(int tier) {
        return this == ICE_LANCE ? 0.6f + 0.2f * tier : 0;
    }

    public float brittle(int tier) {
        return this == STORM ? 0.25f + 0.05f * (tier - 1) : 0;
    }

    public float burnDps(int tier) {
        return this == PLASMA ? damage(tier) * 0.3f : 0;
    }

    public float poisonDps(int tier) {
        float base = switch (this) {
            case VENOM -> 5.5f;
            case BLIGHT -> 4;
            case ACID -> 6;
            case MIASMA -> 5;
            case PLAGUE -> 4.5f;
            default -> 0;
        };
        return base * tierScale(tier);
    }

    public float poisonSeconds() {
        return this == MIASMA ? 3 : 4;
    }

    /** Seconds an Acid Mortar hit leaves its victims without armor. */
    public float shredSeconds(int tier) {
        return this == ACID ? 3 + tier : 0;
    }

    /** Share of the target's maximum health a Giant Slayer bolt removes. */
    public float percentHealth(int tier) {
        return this == SUNBOW ? 0.04f + 0.01f * (tier - 1) : 0;
    }

    public float manaPerSecond(int tier) {
        return this == SIPHON ? 0.4f + 0.8f * tier : 0;
    }

    public float zoneRadius(int tier) {
        return this == BLIZZARD ? 56 * (1 + 0.06f * (tier - 1)) : 0;
    }

    public float zoneDps(int tier) {
        return this == BLIZZARD ? damage(tier) * 0.4f : 0;
    }

    /** One line describing this tower's special effect at the given tier. */
    public String special(int tier) {
        return switch (this) {
            case ARROW -> "Single target, hits flyers";
            case CANNON -> "Splash radius " + Math.round(splash(tier));
            case FROST -> "Slows " + pct(slow(tier)) + "% for 1.5s";
            case TESLA -> "Chains to " + chains(tier) + " enemies";
            case VENOM -> "Poison " + Math.round(poisonDps(tier)) + " dmg/s for 4s";
            case PRISM -> "Ramps to x3 on a held target";
            case FLAK -> "Splash " + Math.round(splash(tier)) + ", +50% vs flyers";
            case ICE_LANCE -> String.format("Freezes target for %.1fs", stun(tier));
            case RAILGUN -> "Pierces all enemies in a line";
            case BLIGHT -> "3 targets, poison " + Math.round(poisonDps(tier)) + " dmg/s";
            case SUNBOW -> "+" + pct(percentHealth(tier)) + "% of max health per hit";
            case BLIZZARD -> "Field: " + pct(slow(tier)) + "% slow, " + Math.round(zoneDps(tier)) + " dmg/s";
            case PLASMA -> "Burns for " + Math.round(burnDps(tier)) + " dmg/s over 3s";
            case ACID -> "No armor for " + Math.round(shredSeconds(tier)) + "s, poison "
                    + Math.round(poisonDps(tier)) + "/s";
            case CLUSTER -> "Splash " + Math.round(splash(tier)) + " + 4 bomblets";
            case STORM -> "Chains " + chains(tier) + ", Brittle +" + pct(brittle(tier)) + "% dmg";
            case MIASMA -> pct(slow(tier)) + "% slow, poison " + Math.round(poisonDps(tier)) + " dmg/s";
            case AURORA -> "Slows target " + pct(slow(tier)) + "%, ramps to x3";
            case PLAGUE -> "Chains " + chains(tier) + ", contagious poison " + Math.round(poisonDps(tier)) + "/s";
            case ARC -> "3 beams, each ramps to x3";
            case SIPHON -> String.format("+%.1f mana/s while firing", manaPerSecond(tier));
        };
    }

    private static int pct(float fraction) {
        return Math.round(fraction * 100);
    }
}
