package components;

/** Periodically heals nearby allies by a fraction of their maximum health. */
public final class Healer {
    public final float radius;
    public final float fraction;
    public final float interval;
    public float timer;

    public Healer(float radius, float fraction, float interval) {
        this.radius = radius;
        this.fraction = fraction;
        this.interval = interval;
        this.timer = interval;
    }
}
