package components;

/** Slows the reload of every tower within {@code radius}. */
public final class Chiller {
    public final float radius;
    public float timer;

    public Chiller(float radius) {
        this.radius = radius;
    }
}
