package systems;

import game.Layout;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/** Shared look of the interface: palette, fonts and the few widgets every screen draws. */
final class Ui {
    static final Color BAR_TOP = new Color(0x232938), BAR_BOTTOM = new Color(0x141821);
    static final Color CARD = new Color(0x12151D), CARD_EDGE = new Color(0x2E3648);
    static final Color BUTTON_TOP = new Color(0x38425A), BUTTON_BOTTOM = new Color(0x262D3D);
    static final Color HOVER_TOP = new Color(0x4A5876), HOVER_BOTTOM = new Color(0x333D54);
    static final Color GO_TOP = new Color(0x2F9E5B), GO_BOTTOM = new Color(0x1F7040);
    static final Color GO_HOVER_TOP = new Color(0x3DBB6F), GO_HOVER_BOTTOM = new Color(0x27874E);
    static final Color BORDER = new Color(0x4A556E);
    static final Color TEXT = new Color(0xE5E7EB), MUTED = new Color(0x9AA3B2);
    static final Color MANA = new Color(0x38BDF8), MANA_LIGHT = new Color(0x7DD3FC), MANA_DARK = new Color(0x0C3A57);
    static final Color GOLD = new Color(0xFBBF24), GOOD = new Color(0x4ADE80);
    static final Color WARN = new Color(0xFB923C), BAD = new Color(0xF87171);
    static final Color DIM = new Color(12, 14, 20, 150);
    static final Color VEIL = new Color(6, 8, 12, 200);
    static final Color CELL = new Color(0x1E2330), CELL_HOVER = new Color(0x3A4459);
    static final Color SHINE = new Color(255, 255, 255, 60);

    static final Font TINY = new Font(Font.SANS_SERIF, Font.PLAIN, 11);
    static final Font TINY_BOLD = new Font(Font.SANS_SERIF, Font.BOLD, 11);
    static final Font BODY = new Font(Font.SANS_SERIF, Font.BOLD, 13);
    static final Font PLAIN = new Font(Font.SANS_SERIF, Font.PLAIN, 14);
    static final Font HEAD = new Font(Font.SANS_SERIF, Font.BOLD, 15);
    static final Font LARGE = new Font(Font.SANS_SERIF, Font.BOLD, 18);
    static final Font BIG = new Font(Font.SANS_SERIF, Font.BOLD, 26);
    static final Font HUGE = new Font(Font.SANS_SERIF, Font.BOLD, 64);
    static final int LINE = 14;

    private static final String[][] HELP_RULES = {
        {"Goal", "Monsters walk the road to your keep. Each one that arrives costs lives. Survive 30 waves."},
        {"Mana", "Refills on its own, faster every wave. It stops when the bar is full, so keep spending it."},
        {"Build", "Drag a card from your hand onto a free tile, or press 1-7 and click a tile."},
        {"Upgrade", "Drag a tower onto its twin (same tower, same tier), or select it and press E. Tier 4 is the top."},
        {"Fuse", "Drag a base tower onto a different base tower of the same tier to make a hybrid with a new power."},
        {"Scout", "Hover the next-wave icons to see what is coming. Press C for the fusion chart."},
        {"Hex", "Healers, drummers and summoners make a wave stronger. A hexed monster loses its powers."},
    };
    private static final String[][] HELP_KEYS = {
        {"1-7", "Pick a card"}, {"Space", "Start / call next wave"}, {"E", "Upgrade selected"},
        {"X", "Sell selected"}, {"F", "Game speed"}, {"P", "Pause menu"},
        {"C", "Fusion chart"}, {"H", "This help"}, {"Esc", "Cancel / pause"},
    };

    private Ui() {}

    /** A rounded button. {@code primary} buttons are green: the thing to press next. */
    static void button(Graphics2D g, Rectangle r, boolean lit, boolean active, boolean primary) {
        Color top = primary ? (lit ? GO_HOVER_TOP : GO_TOP) : (lit ? HOVER_TOP : BUTTON_TOP);
        Color bottom = primary ? (lit ? GO_HOVER_BOTTOM : GO_BOTTOM) : (lit ? HOVER_BOTTOM : BUTTON_BOTTOM);
        g.setPaint(new GradientPaint(0, r.y, top, 0, r.y + r.height, bottom));
        g.fillRoundRect(r.x, r.y, r.width, r.height, 10, 10);
        g.setColor(active ? GOLD : BORDER);
        g.drawRoundRect(r.x, r.y, r.width - 1, r.height - 1, 10, 10);
    }

    /** A button with centred text in the current font, lit when the pointer is over it. */
    static void labelButton(Graphics2D g, Rectangle r, String text, boolean enabled, boolean primary,
                            int mouseX, int mouseY) {
        button(g, r, enabled && r.contains(mouseX, mouseY), false, primary && enabled);
        FontMetrics metrics = g.getFontMetrics();
        g.setColor(enabled ? (primary ? Color.WHITE : TEXT) : MUTED);
        g.drawString(text, r.x + (r.width - metrics.stringWidth(text)) / 2,
                r.y + (r.height + metrics.getAscent() - metrics.getDescent()) / 2);
        if (!enabled) {
            g.setColor(DIM);
            g.fillRoundRect(r.x, r.y, r.width, r.height, 10, 10);
        }
    }

    static void card(Graphics2D g, Rectangle r) {
        g.setColor(CARD);
        g.fillRoundRect(r.x, r.y, r.width, r.height, 12, 12);
        g.setColor(CARD_EDGE);
        g.drawRoundRect(r.x, r.y, r.width - 1, r.height - 1, 12, 12);
    }

    /** Draws word-wrapped text and returns the baseline for the next line. */
    static int wrap(Graphics2D g, String text, int x, int y, int width, int lineHeight) {
        FontMetrics metrics = g.getFontMetrics();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            if (line.length() > 0 && metrics.stringWidth(line + " " + word) > width) {
                g.drawString(line.toString(), x, y);
                y += lineHeight;
                line.setLength(0);
            }
            if (line.length() > 0) line.append(' ');
            line.append(word);
        }
        g.drawString(line.toString(), x, y);
        return y + lineHeight;
    }

    /** Text centred on {@code centerX} with a drop shadow. */
    static void centered(Graphics2D g, String text, Font font, Color color, int centerX, int baseline) {
        g.setFont(font);
        int x = centerX - g.getFontMetrics().stringWidth(text) / 2;
        g.setColor(Color.BLACK);
        g.drawString(text, x + 2, baseline + 2);
        g.setColor(color);
        g.drawString(text, x, baseline);
    }

    /**
     * Draws a screen-sized picture in one pass. At a fractional screen scale it is smoothed only
     * when the graphics card does the work: in software, smoothing a picture this size costs
     * more than the rest of the frame put together.
     */
    static void layer(Graphics2D g, BufferedImage image, int x, int y) {
        double scale = g.getTransform().getScaleX();
        boolean whole = Math.abs(scale - Math.rint(scale)) < 0.01;
        boolean smooth = !whole && g.getDeviceConfiguration().getImageCapabilities().isAccelerated();
        Object saved = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, smooth
                ? RenderingHints.VALUE_INTERPOLATION_BILINEAR : RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(image, x, y, null);
        if (saved != null) g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, saved);
    }

    /** Draws a picture as large as fits a square box, centred in it and keeping its proportions. */
    static void fit(Graphics2D g, BufferedImage image, int x, int y, int box) {
        float scale = box / (float) Math.max(image.getWidth(), image.getHeight());
        int w = Math.round(image.getWidth() * scale), h = Math.round(image.getHeight() * scale);
        g.drawImage(image, x + (box - w) / 2, y + (box - h) / 2, w, h, null);
    }

    static Color pulse(Color color, float time) {
        int alpha = 150 + (int) (105 * Math.sin(time * 5));
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    static Color blend(Color a, Color b, float towardB) {
        float keep = 1 - towardB;
        return new Color(Math.round(a.getRed() * keep + b.getRed() * towardB),
                Math.round(a.getGreen() * keep + b.getGreen() * towardB),
                Math.round(a.getBlue() * keep + b.getBlue() * towardB));
    }

    static String compact(float value) {
        if (value >= 1_000_000) return String.format("%.1fM", value / 1_000_000);
        if (value >= 10_000) return String.format("%.1fk", value / 1000);
        return String.valueOf(Math.round(value));
    }

    static void diamond(Graphics2D g, int cx, int cy, int r) {
        g.setColor(MANA);
        g.fillPolygon(new int[] {cx, cx + r, cx, cx - r}, new int[] {cy - r - 2, cy, cy + r + 2, cy}, 4);
        g.setColor(Color.WHITE);
        g.fillPolygon(new int[] {cx, cx, cx - r + 3}, new int[] {cy - r + 2, cy, cy}, 3);
    }

    /** The how-to-play card, shown over whatever screen is behind it. */
    static void drawHelp(Graphics2D g) {
        g.setColor(VEIL);
        g.fillRect(0, 0, Layout.WIDTH, Layout.HEIGHT);
        Rectangle r = new Rectangle((Layout.WIDTH - 760) / 2, (Layout.HEIGHT - 470) / 2, 760, 470);
        card(g, r);
        g.setFont(BIG);
        g.setColor(GOLD);
        g.drawString("How to play", r.x + 32, r.y + 50);

        int y = r.y + 90;
        for (String[] rule : HELP_RULES) {
            g.setFont(HEAD);
            g.setColor(MANA_LIGHT);
            g.drawString(rule[0], r.x + 32, y);
            g.setFont(PLAIN);
            g.setColor(TEXT);
            y = wrap(g, rule[1], r.x + 130, y, r.width - 170, 19) + 12;
        }

        y += 6;
        g.setColor(CARD_EDGE);
        g.fillRect(r.x + 32, y - 14, r.width - 64, 1);
        for (int i = 0; i < HELP_KEYS.length; i++) {
            int x = r.x + 32 + (i % 3) * 236, rowY = y + 14 + (i / 3) * 30;
            g.setFont(BODY);
            int keyWidth = g.getFontMetrics().stringWidth(HELP_KEYS[i][0]) + 16;
            g.setColor(BUTTON_TOP);
            g.fillRoundRect(x, rowY - 16, keyWidth, 22, 8, 8);
            g.setColor(TEXT);
            g.drawString(HELP_KEYS[i][0], x + 8, rowY);
            g.setFont(PLAIN);
            g.setColor(MUTED);
            g.drawString(HELP_KEYS[i][1], x + keyWidth + 10, rowY);
        }
        g.setFont(BODY);
        g.setColor(MUTED);
        String close = "Click or press any key to close";
        g.drawString(close, r.x + (r.width - g.getFontMetrics().stringWidth(close)) / 2, r.y + r.height - 22);
    }
}
