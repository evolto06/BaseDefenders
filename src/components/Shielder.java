package components;

/** Periodically shields every monster within {@code radius}, itself included. */
public final class Shielder {
    public final float radius;
    public final float interval;
    public float timer;

    public Shielder(float radius, float interval) {
        this.radius = radius;
        this.interval = interval;
        this.timer = 1;
    }
}
