package components;

import ecs.World;
import game.TowerType;

public final class Tower {
    public TowerType type;
    public int tier;
    public final int col, row;
    /** Total mana spent on this tower and everything merged into it. */
    public int invested;
    public float cooldown;
    public float angle;
    public int target = World.NONE;
    /** 1 right after firing, decaying to 0. Drives recoil and muzzle flash. */
    public float recoil;
    /** Beam towers: 0..1 build-up on the held target. */
    public float ramp;
    /** While positive, an Elder Treant's roots hold the tower and it cannot fire. */
    public float tangleTime;
    /** While positive, a Frost Giant's breath slows the tower's reload. */
    public float chillTime;
    /** Lifetime statistics shown on the tower's card. */
    public float damageDealt;
    public int kills;

    public Tower(TowerType type, int tier, int col, int row, int invested) {
        this.type = type;
        this.tier = tier;
        this.col = col;
        this.row = row;
        this.invested = invested;
    }
}
