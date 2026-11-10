package components;

import game.TowerType;

/** A shot in flight. Homing shots chase {@code target}; lobbed shots arc to a fixed point. */
public final class Projectile {
    public final TowerType source;
    public final int tier;
    public int target;
    /** The tower that fired this shot, or -1. */
    public int owner = -1;
    public float tx, ty;
    public float speed;
    public float angle;
    public boolean lobbed;
    public float sx, sy;
    public float progress, duration;

    public Projectile(TowerType source, int tier, int target, float tx, float ty) {
        this.source = source;
        this.tier = tier;
        this.target = target;
        this.tx = tx;
        this.ty = ty;
    }
}
