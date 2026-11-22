package components;

/** Timed effects on an enemy. Each one keeps its strongest value and longest duration. */
public final class Status {
    /** Fraction of movement speed removed, 0..1. */
    public float slow, slowTime;
    public float stunTime;
    public float burnDps, burnTime;
    /** Extra fraction of damage taken from every source. */
    public float brittle, brittleTime;
    public float poisonDps, poisonTime;
    /** A contagious poison jumps to nearby enemies when this one dies. */
    public boolean contagious;
    /** While positive, armor counts as zero. */
    public float shredTime;
    /** While positive, the monster cannot be healed and its own powers are silenced. */
    public float hexTime;
    /** While positive, healing and regeneration hurt the monster instead. */
    public float rotTime;
    /** A doomed monster explodes on death for this fraction of its maximum health. */
    public float doom, doomTime;
    /** While positive, the monster flees back down the road. */
    public float fearTime;
    /** Seconds until it can be frightened again. */
    public float fearGuard;
    /** Seconds until another healer's pulse can mend it, so stacked healers do not multiply. */
    public float mendGuard;
    /** An Aegis Warden's shield, which swallows the next hit whole. */
    public boolean shield;
    /**
     * Positive for the rest of the step in which a shield broke, so the effects riding on the
     * hit that broke it are swallowed too.
     */
    public float shieldBreak;
    /** While positive, the monster is underground: nothing can target, hit or touch it. */
    public float burrowTime;
    /** Seconds until the cold can mend a Yeti again, so a lingering chill does not stack. */
    public float coldGuard;
    /** Fraction of extra movement speed from a War Drummer nearby. */
    public float haste, hasteTime;
    /** Towers credited with the lingering effects, or -1. */
    public int burnOwner = -1, poisonOwner = -1, rotOwner = -1, doomOwner = -1;
}
