package components;

/** Blinks {@code distance} pixels down the road when frozen or struck hard, then recharges. */
public final class Blinker {
    public final float distance;
    public final float cooldown;
    public float timer;
    /** Set by a freeze or a heavy hit; the blink happens at the start of the next step. */
    public boolean pending;

    public Blinker(float distance, float cooldown) {
        this.distance = distance;
        this.cooldown = cooldown;
    }
}
