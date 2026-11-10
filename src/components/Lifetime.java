package components;

/** Destroys the entity when it runs out. */
public final class Lifetime {
    public float remaining;
    public final float total;

    public Lifetime(float seconds) {
        this.remaining = seconds;
        this.total = seconds;
    }
}
