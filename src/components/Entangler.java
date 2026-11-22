package components;

/** Every {@code interval} seconds, roots the {@code count} towers nearest it within {@code radius}. */
public final class Entangler {
    public final float radius;
    public final int count;
    public final float seconds;
    public final float interval;
    public float timer;

    public Entangler(float radius, int count, float seconds, float interval) {
        this.radius = radius;
        this.count = count;
        this.seconds = seconds;
        this.interval = interval;
        this.timer = 2;
    }
}
