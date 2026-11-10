package components;

/** Walks a polyline of waypoints. {@code remaining} is the distance left to the base. */
public final class PathFollower {
    public final float[] xs, ys;
    /** Sideways offset from the route's centre line, so a crowd does not walk in single file. */
    public float laneX, laneY;
    public int next;
    public float speed;
    public float remaining;

    public PathFollower(float[] xs, float[] ys, int next, float speed, float remaining) {
        this.xs = xs;
        this.ys = ys;
        this.next = next;
        this.speed = speed;
        this.remaining = remaining;
    }
}
