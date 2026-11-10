package components;

/** Free movement in pixels per second, with optional drag. Used by particles. */
public final class Velocity {
    public float vx, vy;
    public float drag;

    public Velocity(float vx, float vy, float drag) {
        this.vx = vx;
        this.vy = vy;
        this.drag = drag;
    }
}
