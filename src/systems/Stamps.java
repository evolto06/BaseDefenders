package systems;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Soft shapes the field draws by the hundred (shadows, glows, explosion flashes), painted once
 * and then stamped as images. Filling a translucent shape costs many times more than copying a
 * small picture of one, which is what decides the frame rate when Java draws without the GPU.
 */
final class Stamps {
    private static final int MAX_DIAMETER = 320;
    private static final int SHADOW_ALPHA = 70;
    private static final int GLOW_LEVELS = 16, GLOW_MAX = 64;
    /** A sprite is kept at sizes from this much narrower or shorter up to SIZE_SPAN wider or taller. */
    private static final int SIZE_SHRINK = 24, SIZE_SPAN = 32;
    /** How many headings a turning turret is kept at: three degrees apart. */
    private static final int TURNS = 120;
    private static final Composite[] ALPHAS = new Composite[256];

    static {
        for (int i = 0; i < ALPHAS.length; i++) ALPHAS[i] = AlphaComposite.SrcOver.derive(i / 255f);
    }

    private final Map<Color, BufferedImage[]> discs = new HashMap<>();
    private final Map<Color, BufferedImage[]> flashes = new HashMap<>();
    private final Map<Color, BufferedImage[][]> glows = new HashMap<>();
    private final Map<BufferedImage, BufferedImage[]> sizes = new IdentityHashMap<>();
    private final Map<BufferedImage, BufferedImage[]> turns = new IdentityHashMap<>();
    private final BufferedImage[] shadows = new BufferedImage[64];
    private BufferedImage hexMark;

    /** A shared translucency level, so fading a stamp does not allocate a composite per draw. */
    static Composite alpha(float fraction) {
        return ALPHAS[Math.clamp(Math.round(fraction * 255), 0, 255)];
    }

    /** Whether a shape this wide is stamped; anything larger is rare enough to fill directly. */
    static boolean stamped(int diameter) {
        return diameter > 0 && diameter < MAX_DIAMETER;
    }

    /** A solid disc of the colour. Draw it through {@link #alpha} to make it a glow. */
    BufferedImage disc(Color color, int diameter) {
        BufferedImage[] bySize = discs.computeIfAbsent(color, c -> new BufferedImage[MAX_DIAMETER]);
        if (bySize[diameter] == null) bySize[diameter] = paint(diameter, diameter, color, null);
        return bySize[diameter];
    }

    /**
     * A small disc already faded to roughly {@code opacity}, for the glows every tower wears:
     * copying a ready-faded picture is several times quicker than fading one as it is drawn.
     */
    BufferedImage glow(Color color, int diameter, float opacity) {
        BufferedImage[][] levels = glows.computeIfAbsent(color, c -> new BufferedImage[GLOW_LEVELS + 1][GLOW_MAX]);
        int level = Math.clamp(Math.round(opacity * GLOW_LEVELS), 0, GLOW_LEVELS);
        if (levels[level][diameter] == null) {
            Color faded = new Color(color.getRed(), color.getGreen(), color.getBlue(), level * 255 / GLOW_LEVELS);
            levels[level][diameter] = paint(diameter, diameter, faded, null);
        }
        return levels[level][diameter];
    }

    /**
     * The sprite stretched to {@code w} by {@code h}, for the squash and stretch of a walking
     * monster, or null if the size is outside the range kept (the caller then scales it itself).
     */
    BufferedImage sized(BufferedImage sprite, int w, int h) {
        if (w == sprite.getWidth() && h == sprite.getHeight()) return sprite;
        int dw = w - sprite.getWidth() + SIZE_SHRINK, dh = h - sprite.getHeight() + SIZE_SHRINK;
        if (dw < 0 || dw >= SIZE_SPAN || dh < 0 || dh >= SIZE_SPAN || w <= 0 || h <= 0) return null;
        BufferedImage[] variants = sizes.get(sprite);
        if (variants == null) sizes.put(sprite, variants = new BufferedImage[SIZE_SPAN * SIZE_SPAN]);
        int key = dw * SIZE_SPAN + dh;
        if (variants[key] == null) {
            variants[key] = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = variants[key].createGraphics();
            g.drawImage(sprite, 0, 0, w, h, null);
            g.dispose();
        }
        return variants[key];
    }

    /**
     * The turret turned to the nearest of {@link #TURNS} headings, on a square canvas just big
     * enough for any of them; draw it centred on the tower.
     */
    BufferedImage turned(BufferedImage turret, float angle) {
        BufferedImage[] frames = turns.get(turret);
        if (frames == null) turns.put(turret, frames = new BufferedImage[TURNS + 1]);
        int step = Math.floorMod(Math.round(angle / (2 * (float) Math.PI) * TURNS), TURNS);
        if (frames[step] == null) {
            // The last slot holds the canvas every heading shares, sized to the turret's reach.
            if (frames[TURNS] == null) {
                int size = 2 * reach(turret) + 2;
                frames[TURNS] = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            }
            int size = frames[TURNS].getWidth();
            frames[step] = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = frames[step].createGraphics();
            g.translate(size / 2, size / 2);
            g.rotate(step * 2 * Math.PI / TURNS);
            g.drawImage(turret, -turret.getWidth() / 2, -turret.getHeight() / 2, null);
            g.dispose();
        }
        return frames[step];
    }

    /** How far, in pixels, the furthest painted pixel lies from the middle of the picture. */
    private static int reach(BufferedImage image) {
        float cx = image.getWidth() / 2f, cy = image.getHeight() / 2f;
        double furthest = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (image.getRGB(x, y) >>> 24 != 0) furthest = Math.max(furthest, Math.hypot(x + 0.5 - cx, y + 0.5 - cy));
            }
        }
        return (int) Math.ceil(furthest);
    }

    /** A disc with a white core half its width: an explosion flash. */
    BufferedImage flash(Color color, int diameter) {
        BufferedImage[] bySize = flashes.computeIfAbsent(color, c -> new BufferedImage[MAX_DIAMETER]);
        if (bySize[diameter] == null) bySize[diameter] = paint(diameter, diameter, color, Color.WHITE);
        return bySize[diameter];
    }

    /** The shadow under a monster of the given radius: twice as wide as it is tall. */
    BufferedImage shadow(int radius) {
        if (shadows[radius] == null) {
            shadows[radius] = paint(radius * 2, radius, new Color(0, 0, 0, SHADOW_ALPHA), null);
        }
        return shadows[radius];
    }

    /** A small violet cross on a dark outline, worn by hexed monsters. */
    BufferedImage hexMark(Color color, Color outline) {
        if (hexMark == null) {
            hexMark = new BufferedImage(9, 9, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = hexMark.createGraphics();
            for (int pass = 0; pass < 2; pass++) {
                // The outline pass draws each stroke one pixel fatter all round.
                g.setColor(pass == 0 ? outline : color);
                for (int i = 1; i <= 7; i++) {
                    int grow = pass == 0 ? 1 : 0;
                    g.fillRect(i - grow, i - grow, 1 + grow * 2, 1 + grow * 2);
                    g.fillRect(i - grow, 8 - i - grow, 1 + grow * 2, 1 + grow * 2);
                }
            }
            g.dispose();
        }
        return hexMark;
    }

    private static BufferedImage paint(int width, int height, Color color, Color core) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(color);
        g.fillOval(0, 0, width, height);
        if (core != null) {
            g.setColor(core);
            g.fillOval(width / 4, height / 4, width / 2, height / 2);
        }
        g.dispose();
        return image;
    }
}
