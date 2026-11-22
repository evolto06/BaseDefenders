package components;

/** Walks above ground for {@code up} seconds, then tunnels under the road for {@code down}. */
public final class Burrower {
    public final float up, down;
    public float timer;
    /** Whether it was underground last step, to notice when it comes up. */
    public boolean under;

    public Burrower(float up, float down) {
        this.up = up;
        this.down = down;
        this.timer = up;
    }
}
