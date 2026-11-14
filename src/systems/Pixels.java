package systems;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

/**
 * Blends lines, rings and rectangles straight into a picture's pixels. Java2D can draw all of
 * these, but each translucent shape it draws allocates a few kilobytes; with a hundred beams
 * and rings on screen that is megabytes of garbage a second and a collector pause every few
 * seconds. These loops allocate nothing.
 */
final class Pixels {
    private final int[] data;
    private final int width, height;

    /** The picture must be {@link BufferedImage#TYPE_INT_RGB}. */
    Pixels(BufferedImage image) {
        data = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
        width = image.getWidth();
        height = image.getHeight();
    }

    void rect(int x, int y, int w, int h, Color color, float opacity) {
        int alpha = alpha(opacity);
        if (alpha == 0) return;
        int rgb = color.getRGB();
        for (int row = Math.max(0, y), end = Math.min(height, y + h); row < end; row++) {
            span(x, x + w, row, rgb & 0xFF00FF, rgb & 0x00FF00, alpha);
        }
    }

    /** A straight line {@code thickness} pixels thick, measured along the axis it crosses most slowly. */
    void line(float x0, float y0, float x1, float y1, int thickness, Color color, float opacity) {
        int alpha = alpha(opacity);
        if (alpha == 0) return;
        int rgb = color.getRGB(), rb = rgb & 0xFF00FF, g = rgb & 0x00FF00;
        float dx = x1 - x0, dy = y1 - y0;
        int half = thickness / 2;
        if (Math.abs(dx) >= Math.abs(dy)) {
            if (x0 > x1) {
                float swap = x0;
                x0 = x1;
                x1 = swap;
                y0 = y1;
            }
            float slope = dx == 0 ? 0 : dy / dx;
            for (int x = Math.max(0, Math.round(x0)), end = Math.min(width - 1, Math.round(x1)); x <= end; x++) {
                int top = Math.round(y0 + (x - x0) * slope) - half;
                for (int y = Math.max(0, top), stop = Math.min(height, top + thickness); y < stop; y++) {
                    blend(y * width + x, rb, g, alpha);
                }
            }
        } else {
            if (y0 > y1) {
                float swap = y0;
                y0 = y1;
                y1 = swap;
                x0 = x1;
            }
            float slope = dx / dy;
            for (int y = Math.max(0, Math.round(y0)), end = Math.min(height - 1, Math.round(y1)); y <= end; y++) {
                int left = Math.round(x0 + (y - y0) * slope) - half;
                span(left, left + thickness, y, rb, g, alpha);
            }
        }
    }

    /** A circle outline {@code thickness} pixels thick. */
    void ring(int cx, int cy, int radius, int thickness, Color color, float opacity) {
        int alpha = alpha(opacity);
        if (alpha == 0 || radius <= 0) return;
        int rgb = color.getRGB(), rb = rgb & 0xFF00FF, g = rgb & 0x00FF00;
        int outer = radius + thickness / 2, inner = Math.max(0, outer - thickness);
        for (int dy = Math.max(-outer, -cy), end = Math.min(outer, height - 1 - cy); dy <= end; dy++) {
            int reach = (int) Math.sqrt(outer * outer - dy * dy);
            if (Math.abs(dy) >= inner) {
                span(cx - reach, cx + reach + 1, cy + dy, rb, g, alpha);
            } else {
                int hole = (int) Math.sqrt(inner * inner - dy * dy);
                span(cx - reach, cx - hole, cy + dy, rb, g, alpha);
                span(cx + hole + 1, cx + reach + 1, cy + dy, rb, g, alpha);
            }
        }
    }

    private static int alpha(float opacity) {
        return Math.clamp(Math.round(opacity * 256), 0, 256);
    }

    /** Blends the pixels from x0 up to but not including x1 on row y, which must be on the picture. */
    private void span(int x0, int x1, int y, int rb, int g, int alpha) {
        for (int i = y * width + Math.max(0, x0), end = y * width + Math.min(width, x1); i < end; i++) {
            blend(i, rb, g, alpha);
        }
    }

    private void blend(int i, int rb, int g, int alpha) {
        int under = data[i], keep = 256 - alpha;
        data[i] = ((((under & 0xFF00FF) * keep + rb * alpha) >>> 8) & 0xFF00FF)
                | ((((under & 0x00FF00) * keep + g * alpha) >>> 8) & 0x00FF00);
    }
}
