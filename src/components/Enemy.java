package components;

import game.EnemyType;

public final class Enemy {
    public final EnemyType type;
    /** Wave health multiplier, inherited by anything this enemy spawns on death. */
    public final float hpScale;
    public float animTime;
    /** 1 when just hit, decaying to 0. Drives the white hit flash. */
    public float flash;

    public Enemy(EnemyType type, float hpScale, float animTime) {
        this.type = type;
        this.hpScale = hpScale;
        this.animTime = animTime;
    }
}
