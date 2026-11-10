package components;

import java.awt.Color;

/** A short-lived visual with no gameplay effect. */
public final class Effect {
    public enum Kind { BEAM, BOLT, RING, FLASH, TEXT, SPARK }

    public final Kind kind;
    public final Color color;
    public float x2, y2;
    public float radius;
    public float width;
    public String text;

    public Effect(Kind kind, Color color) {
        this.kind = kind;
        this.color = color;
    }
}
