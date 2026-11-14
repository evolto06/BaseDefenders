package components;

/** Keeps every monster within {@code radius} moving faster by {@code haste}. */
public final class Rally {
    public final float radius;
    public final float haste;
    public float timer;

    public Rally(float radius, float haste) {
        this.radius = radius;
        this.haste = haste;
    }
}
