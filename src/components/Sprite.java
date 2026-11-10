package components;

import java.awt.image.BufferedImage;

/** A pre-scaled image drawn centred on the entity, with a mirrored variant for facing left. */
public final class Sprite {
    public BufferedImage image;
    public BufferedImage flipped;
    public boolean flip;

    public Sprite(BufferedImage image, BufferedImage flipped) {
        this.image = image;
        this.flipped = flipped;
    }
}
