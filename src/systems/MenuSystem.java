package systems;

import game.Game;
import game.Layout;
import game.MapDef;
import game.TowerType;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/** Draws the screens outside a match: the title and the map select. */
public final class MenuSystem {
    public static final String[] TITLE_LABELS = {"Play", "How to play", "Quit"};
    private static final Color SCRIM = new Color(8, 10, 16, 175);
    private static final Color[] DIFFICULTY = {Ui.GOOD, Ui.GOLD, Ui.WARN, Ui.BAD};

    private final Game game;
    private BufferedImage backdrop;

    public MenuSystem(Game game) {
        this.game = game;
    }

    public void render(Graphics2D g) {
        drawBackdrop(g);
        if (game.screen == Game.Screen.TITLE) drawTitle(g);
        else drawMaps(g);
        if (game.showHelp) Ui.drawHelp(g);
    }

    /** The first map's terrain, blown up to fill the window and dimmed. Scaled once, then reused. */
    private void drawBackdrop(Graphics2D g) {
        if (backdrop == null) {
            BufferedImage terrain = game.assets.background(MapDef.MEADOW);
            float scale = Layout.HEIGHT / (float) terrain.getHeight();
            backdrop = new BufferedImage(Math.round(terrain.getWidth() * scale), Layout.HEIGHT, BufferedImage.TYPE_INT_RGB);
            Graphics2D b = backdrop.createGraphics();
            b.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            b.drawImage(terrain, 0, 0, backdrop.getWidth(), Layout.HEIGHT, null);
            b.setColor(SCRIM);
            b.fillRect(0, 0, backdrop.getWidth(), Layout.HEIGHT);
            b.dispose();
        }
        // Drift slowly sideways so the menu is not a still picture.
        int slack = backdrop.getWidth() - Layout.WIDTH;
        int x = -Math.round(slack * (0.5f + 0.5f * (float) Math.sin(game.uiTime * 0.08f)));
        Ui.layer(g, backdrop, x, 0);
    }

    private void drawTitle(Graphics2D g) {
        int cx = Layout.WIDTH / 2;
        Ui.centered(g, "BASE DEFENDERS", Ui.HUGE, Ui.GOLD, cx, 190);
        Ui.centered(g, "Build. Merge. Fuse. Hold the keep.", Ui.LARGE, Ui.TEXT, cx, 232);

        // A parade of the base towers, bobbing in turn.
        TowerType[] bases = TowerType.BASES;
        int spacing = 68, left = cx - (bases.length * spacing - 20) / 2;
        for (int i = 0; i < bases.length; i++) {
            int bob = (int) Math.round(Math.sin(game.uiTime * 3 + i * 0.9) * 4);
            g.drawImage(game.assets.icon(bases[i], 1 + i % TowerType.MAX_TIER), left + i * spacing, 262 + bob, null);
        }

        g.setFont(Ui.LARGE);
        for (int i = 0; i < Layout.TITLE_BUTTONS.length; i++) {
            Ui.labelButton(g, Layout.TITLE_BUTTONS[i], TITLE_LABELS[i], true, i == 0, game.mouseX, game.mouseY);
        }
        g.setFont(Ui.TINY);
        g.setColor(Ui.MUTED);
        g.drawString("Enter: play    H: help", 16, Layout.HEIGHT - 14);
    }

    private void drawMaps(Graphics2D g) {
        Ui.centered(g, "Choose a map", Ui.BIG, Ui.GOLD, Layout.WIDTH / 2, 84);
        Ui.centered(g, "Harder maps have shorter roads and tougher monsters. Your best wave is saved per map.",
                Ui.PLAIN, Ui.MUTED, Layout.WIDTH / 2, 114);
        MapDef[] maps = MapDef.values();
        for (int i = 0; i < maps.length; i++) drawMapCard(g, maps[i], Layout.MAP_CARDS[i], i);
        g.setFont(Ui.HEAD);
        Ui.labelButton(g, Layout.BACK, "Back  [Esc]", true, false, game.mouseX, game.mouseY);
    }

    private void drawMapCard(Graphics2D g, MapDef map, Rectangle r, int index) {
        boolean hover = r.contains(game.mouseX, game.mouseY);
        int y = r.y - (hover ? 6 : 0);
        Rectangle lifted = new Rectangle(r.x, y, r.width, r.height);
        Ui.card(g, lifted);
        if (hover) {
            g.setColor(Ui.GOLD);
            g.drawRoundRect(r.x, y, r.width - 1, r.height - 1, 12, 12);
            g.drawRoundRect(r.x + 1, y + 1, r.width - 3, r.height - 3, 11, 11);
        }
        BufferedImage terrain = game.assets.background(map);
        int thumbW = r.width - 16, thumbH = thumbW * terrain.getHeight() / terrain.getWidth();
        g.drawImage(terrain, r.x + 8, y + 8, thumbW, thumbH, null);
        g.setColor(Ui.CARD_EDGE);
        g.drawRect(r.x + 8, y + 8, thumbW - 1, thumbH - 1);

        int textY = y + thumbH + 38;
        g.setFont(Ui.LARGE);
        g.setColor(Ui.TEXT);
        g.drawString(map.label, r.x + 14, textY);
        g.setFont(Ui.BODY);
        g.setColor(DIFFICULTY[Math.min(index, DIFFICULTY.length - 1)]);
        g.drawString(map.difficulty, r.x + 14, textY + 22);
        g.setColor(Ui.MUTED);
        String key = "[" + (index + 1) + "]";
        g.drawString(key, r.x + r.width - 14 - g.getFontMetrics().stringWidth(key), textY + 22);
        g.setFont(Ui.PLAIN);
        Ui.wrap(g, map.blurb, r.x + 14, textY + 50, r.width - 28, 19);

        int best = game.records.best(map);
        g.setFont(Ui.BODY);
        if (best >= Game.FINAL_WAVE) {
            g.setColor(Ui.GOLD);
            g.drawString("Cleared!  Best wave " + best, r.x + 14, y + r.height - 50);
        } else {
            g.setColor(best > 0 ? Ui.MANA_LIGHT : Ui.MUTED);
            g.drawString(best > 0 ? "Best: wave " + best + " / " + Game.FINAL_WAVE : "Not played yet",
                    r.x + 14, y + r.height - 50);
        }
        Rectangle play = new Rectangle(r.x + 14, y + r.height - 40, r.width - 28, 28);
        Ui.button(g, play, hover, false, true);
        g.setColor(Color.WHITE);
        g.drawString("Play", play.x + (play.width - g.getFontMetrics().stringWidth("Play")) / 2, play.y + 19);
    }
}
