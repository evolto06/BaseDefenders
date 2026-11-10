package components;

/** Timed debuffs on an enemy. Each effect keeps its strongest value and longest duration. */
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
    /** Towers credited with the damage-over-time effects, or -1. */
    public int burnOwner = -1, poisonOwner = -1;
}
