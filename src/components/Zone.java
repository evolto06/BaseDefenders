package components;

import java.awt.Color;

/** A lingering ground field that slows and damages ground enemies inside it. */
public final class Zone {
    public final float radius;
    public final float slow;
    public final float dps;
    public final Color color;
    public float tick;
    /** The tower credited with this field's damage, or -1. */
    public int owner = -1;

    public Zone(float radius, float slow, float dps, Color color) {
        this.radius = radius;
        this.slow = slow;
        this.dps = dps;
        this.color = color;
    }
}
