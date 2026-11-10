package components;

public final class Health {
    public float hp, max;
    /** Flat reduction applied to each physical hit. */
    public float armor;
    /** The tower that last damaged this entity, or -1. */
    public int lastHitBy = -1;

    public Health(float hp, float armor) {
        this.hp = hp;
        this.max = hp;
        this.armor = armor;
    }
}
