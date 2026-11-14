package systems;

import static systems.Ui.BAD;
import static systems.Ui.BODY;
import static systems.Ui.BORDER;
import static systems.Ui.CARD;
import static systems.Ui.CELL;
import static systems.Ui.CELL_HOVER;
import static systems.Ui.DIM;
import static systems.Ui.GOLD;
import static systems.Ui.GOOD;
import static systems.Ui.HEAD;
import static systems.Ui.LINE;
import static systems.Ui.MANA;
import static systems.Ui.MANA_DARK;
import static systems.Ui.MANA_LIGHT;
import static systems.Ui.MUTED;
import static systems.Ui.SHINE;
import static systems.Ui.TEXT;
import static systems.Ui.TINY;
import static systems.Ui.TINY_BOLD;
import static systems.Ui.WARN;

import components.Tower;
import ecs.Query;
import ecs.Store;
import game.DamageType;
import game.EnemyType;
import game.Game;
import game.Layout;
import game.TowerType;
import game.WavePlan;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Draws the in-match interface: status bar on top, and along the bottom the next-wave preview,
 * the hand of tower cards over the mana bar, and the controls. Details float over the field.
 */
public final class HudSystem {
    private static final Color TIP_BACK = new Color(15, 18, 26, 230);
    private static final Color CARD_FRAME = new Color(0x0B0D12);
    private static final Color CARD_FACE_TOP = new Color(0x394359), CARD_FACE_BOTTOM = new Color(0x161A24);
    private static final Color CARD_SHADOW = new Color(0, 0, 0, 90);
    private static final Color WIPE = new Color(8, 10, 16, 185);
    private static final int TOOLTIP_W = 236, TOOLTIP_H = 178;
    private static final int MANA_SEGMENTS = 10;
    /** Rows of field above the bottom bar that a raised card reaches into. */
    private static final int LIP = 16;
    /** The bars are repainted at least once in this many frames. */
    private static final int REFRESH_FRAMES = 4;
    private static final int BADGE_W = 30, BADGE_H = 26;
    private static final AffineTransform DEVICE = new AffineTransform();
    private static final Rectangle[] BUTTONS = {
        Layout.NEXT, Layout.SPEED, Layout.PAUSE, Layout.CHART, Layout.MENU, Layout.HELP
    };

    private final Game game;
    private final Store<Tower> towers;
    private final Query towerQuery;
    private BufferedImage topBar, bottomBar, bottomLip, bottomBody, towerCard;
    /** Per card: its face, plain and picked, and its cost badge, unaffordable and affordable. */
    private BufferedImage[] cardFaces, cardBadges;
    private double barScale;
    private int barState, barAge;

    public HudSystem(Game game) {
        this.game = game;
        this.towers = game.world.store(Tower.class);
        this.towerQuery = game.world.query(Tower.class);
    }

    public void render(Graphics2D g) {
        drawBars(g);
        drawTips(g);
        drawTowerCard(g);
        drawHint(g);
        if (game.paused || game.gameOver) drawMenu(g);
        if (game.victoryScreen) drawVictory(g);
        if (game.showChart) drawChart(g);
        if (game.showHelp) Ui.drawHelp(g);
    }

    // ---- the bars ------------------------------------------------------------------------

    /**
     * The status bar and the bottom bar change little from frame to frame but are slow to paint
     * (gradients, rounded shapes, text), so they are painted into pictures at the screen's own
     * resolution and those are copied to the screen. They are repainted a few times a second
     * for the ticking numbers, and at once when the pointer moves onto or off something. The
     * selected tower's card is kept the same way.
     */
    private void drawBars(Graphics2D g) {
        AffineTransform at = g.getTransform();
        double scale = at.getScaleX();
        int state = barState();
        boolean animating = game.wave == 0 || game.manaFlash > 0;
        if (topBar == null || scale != barScale || state != barState || animating || ++barAge >= REFRESH_FRAMES) {
            if (topBar == null || scale != barScale) allocateBars(scale);
            barState = state;
            barAge = 0;
            paintBars(g);
        }
        Graphics2D device = (Graphics2D) g.create();
        device.setTransform(DEVICE);
        int x = (int) Math.round(at.getTranslateX()), y = (int) Math.round(at.getTranslateY());
        device.drawImage(topBar, x, y, null);
        int lipTop = y + (int) Math.round((Layout.BAR_Y - LIP) * scale);
        device.drawImage(bottomLip, x, lipTop, null);
        device.drawImage(bottomBody, x, lipTop + bottomLip.getHeight(), null);
        device.dispose();
    }

    private void allocateBars(double scale) {
        barScale = scale;
        cardFaces = new BufferedImage[Layout.CARDS.length * 2];
        cardBadges = new BufferedImage[Layout.CARDS.length * 2];
        int width = (int) Math.ceil(Layout.WIDTH * scale);
        int lipRows = (int) Math.ceil(LIP * scale);
        topBar = new BufferedImage(width, (int) Math.ceil(Layout.TOP * scale), BufferedImage.TYPE_INT_RGB);
        bottomBar = new BufferedImage(width, (int) Math.ceil((Layout.BAR_H + LIP) * scale), BufferedImage.TYPE_INT_ARGB);
        // Only the lip, where a raised card overlaps the field, needs to be see-through; the
        // rest is kept as an opaque copy, which is far cheaper to put on the screen.
        bottomLip = bottomBar.getSubimage(0, 0, width, lipRows);
        bottomBody = new BufferedImage(width, bottomBar.getHeight() - lipRows, BufferedImage.TYPE_INT_RGB);
        towerCard = new BufferedImage((int) Math.ceil(Layout.TOWER_CARD_W * scale),
                (int) Math.ceil(Layout.TOWER_CARD_H * scale), BufferedImage.TYPE_INT_ARGB);
    }

    private void paintBars(Graphics2D screen) {
        Graphics2D g = strip(topBar, screen, 0);
        g.setColor(BORDER);
        g.fillRect(0, 0, Layout.WIDTH, Layout.TOP + 1);
        drawStatusBar(g);
        g.dispose();

        g = bottomBar.createGraphics();
        g.setComposite(AlphaComposite.Clear);
        g.fillRect(0, 0, bottomBar.getWidth(), bottomBar.getHeight());
        g.dispose();
        g = strip(bottomBar, screen, Layout.BAR_Y - LIP);
        g.setPaint(new GradientPaint(0, Layout.BAR_Y, Ui.BAR_TOP, 0, Layout.HEIGHT, Ui.BAR_BOTTOM));
        g.fillRect(0, Layout.BAR_Y, Layout.WIDTH, Layout.BAR_H + 1);
        // The border starts one row above the bar, so no seam can show where it meets the field.
        g.setColor(BORDER);
        g.fillRect(0, Layout.BAR_Y - 1, Layout.WIDTH, 3);
        drawPreview(g);
        drawMana(g);
        drawHand(g);
        drawControls(g);
        g.dispose();

        g = bottomBody.createGraphics();
        g.drawImage(bottomBar, 0, -bottomLip.getHeight(), null);
        g.dispose();
        paintTowerCard(screen);
    }

    /** Graphics that paint into a bar's picture exactly as the screen would have painted it. */
    private Graphics2D strip(BufferedImage image, Graphics2D screen, int top) {
        Graphics2D g = image.createGraphics();
        g.setRenderingHints(screen.getRenderingHints());
        g.scale(barScale, barScale);
        g.translate(0, -top);
        return g;
    }

    /** What the pointer is on, and the facts that change how the bars look at a glance. */
    private int barState() {
        int over = -1;
        for (int i = 0; i < Layout.CARDS.length; i++) {
            if (Layout.CARDS[i].contains(game.mouseX, game.mouseY)) over = i;
        }
        for (int i = 0; i < BUTTONS.length; i++) {
            if (BUTTONS[i].contains(game.mouseX, game.mouseY)) over = 20 + i;
        }
        int slot = Layout.previewSlotAt(game.mouseX, game.mouseY, game.nextPlan.counts.size());
        if (slot >= 0) over = 40 + slot;
        Tower selected = shownTower();
        if (selected != null) {
            Rectangle card = Layout.towerCard(selected.col);
            if (Layout.upgradeButton(card).contains(game.mouseX, game.mouseY)) over = 60;
            if (Layout.sellButton(card).contains(game.mouseX, game.mouseY)) over = 61;
        }
        int state = over;
        state = state * 31 + (selected == null ? -1 : game.selected);
        state = state * 31 + (game.placing == null ? -1 : game.placing.ordinal());
        state = state * 31 + game.wave;
        state = state * 31 + game.lives;
        state = state * 31 + game.speed;
        state = state * 31 + game.map.ordinal();
        state = state * 31 + (game.cardDrag ? 1 : 0) + (game.spawning() ? 2 : 0) + (game.gameOver ? 4 : 0);
        return state;
    }

    /** Details of the card or the upcoming monster under the pointer, floating over the field. */
    private void drawTips(Graphics2D g) {
        if (!game.cardDrag) {
            for (int i = 0; i < Layout.CARDS.length; i++) {
                Rectangle r = Layout.CARDS[i];
                if (!r.contains(game.mouseX, game.mouseY)) continue;
                int x = Math.clamp(r.x + r.width / 2 - TOOLTIP_W / 2, 6, Layout.WIDTH - TOOLTIP_W - 6);
                Rectangle tip = new Rectangle(x, Layout.BAR_Y - TOOLTIP_H - 12, TOOLTIP_W, TOOLTIP_H);
                Ui.card(g, tip);
                drawCard(g, tip, TowerType.BASES[i], 1, null);
            }
        }
        WavePlan plan = game.nextPlan;
        int hovered = Layout.previewSlotAt(game.mouseX, game.mouseY, plan.counts.size());
        if (hovered < 0) return;
        int slot = 0;
        for (EnemyType type : plan.counts.keySet()) {
            if (slot++ == hovered) drawEnemyTip(g, type, plan);
        }
    }

    // ---- status bar --------------------------------------------------------------------

    private void drawStatusBar(Graphics2D g) {
        g.setPaint(new GradientPaint(0, 0, Ui.BAR_TOP, 0, Layout.TOP, Ui.BAR_BOTTOM));
        g.fillRect(0, 0, Layout.WIDTH, Layout.TOP);
        g.setColor(BORDER);
        g.fillRect(0, Layout.TOP - 2, Layout.WIDTH, 2);

        g.setFont(BODY);
        Ui.labelButton(g, Layout.MENU, "Menu", true, false, game.mouseX, game.mouseY);
        g.setColor(MUTED);
        g.drawString(game.map.label, 92, 25);

        heart(g, 262, 20);
        g.setFont(HEAD);
        g.setColor(game.lives <= 5 ? BAD : TEXT);
        g.drawString(String.valueOf(game.lives), 278, 25);

        g.setColor(TEXT);
        String wave = game.wave <= Game.FINAL_WAVE
                ? "Wave " + game.wave + " / " + Game.FINAL_WAVE
                : "Wave " + game.wave + "  (endless)";
        g.drawString(wave, 336, 25);

        // What is happening right now: monsters left, or the countdown to the next wave.
        int left = game.enemyCount + (game.activePlan == null ? 0 : game.activePlan.spawns.size() - game.spawnIndex);
        g.setFont(BODY);
        int x = 500;
        if (game.gameOver) {
            g.setColor(BAD);
            g.drawString("Defeated", x, 25);
        } else if (game.wave == 0) {
            g.setColor(Ui.pulse(GOLD, game.uiTime));
            g.drawString("Press SPACE to start the first wave", x, 25);
        } else {
            if (left > 0) {
                g.setColor(TEXT);
                String text = left + (left == 1 ? " monster left" : " monsters left");
                g.drawString(text, x, 25);
                x += g.getFontMetrics().stringWidth(text) + 18;
            }
            if (!game.spawning()) {
                float fraction = Math.clamp(game.waveTimer / WaveSystem.BREAK, 0f, 1f);
                g.setColor(MUTED);
                String text = "Next wave " + (int) Math.ceil(Math.max(0, game.waveTimer)) + "s";
                g.drawString(text, x, 25);
                x += g.getFontMetrics().stringWidth(text) + 8;
                g.setColor(MANA_DARK);
                g.fillRoundRect(x, 16, 80, 8, 6, 6);
                g.setColor(GOLD);
                g.fillRoundRect(x, 16, Math.round(80 * (1 - fraction)), 8, 6, 6);
            }
        }

        g.setColor(MUTED);
        String right = "Slain " + game.kills + "    " + game.fps + " fps";
        g.drawString(right, Layout.HELP.x - 14 - g.getFontMetrics().stringWidth(right), 25);
        g.setFont(HEAD);
        Ui.labelButton(g, Layout.HELP, "?", true, false, game.mouseX, game.mouseY);
    }

    private static void heart(Graphics2D g, int cx, int cy) {
        g.setColor(BAD);
        g.fillOval(cx - 9, cy - 8, 10, 10);
        g.fillOval(cx - 1, cy - 8, 10, 10);
        g.fillPolygon(new int[] {cx - 9, cx + 9, cx}, new int[] {cy - 1, cy - 1, cy + 9}, 3);
        g.setColor(SHINE);
        g.fillOval(cx - 6, cy - 6, 4, 3);
    }

    // ---- the hand ----------------------------------------------------------------------

    private void drawHand(Graphics2D g) {
        for (int i = 0; i < Layout.CARDS.length; i++) {
            Rectangle r = Layout.CARDS[i];
            TowerType type = TowerType.BASES[i];
            boolean active = game.placing == type;
            boolean hover = r.contains(game.mouseX, game.mouseY);
            boolean affordable = game.mana >= type.cost;
            // The picked card rises out of the hand.
            int y = r.y - (active ? 14 : hover ? 5 : 0);
            int w = r.width, h = r.height;

            // A card's face and its cost badge never change, so each is painted once and stamped.
            int face = i * 2 + (active ? 1 : 0), number = i + 1;
            if (cardFaces[face] == null) {
                cardFaces[face] = picture(g, w + 2, h + 5, p -> paintCardFace(p, type, number, w, h, active));
            }
            stamp(g, cardFaces[face], r.x, y);

            if (!affordable) {
                // A dark wipe drains away as mana approaches the price.
                Shape savedClip = g.getClip();
                g.clipRect(r.x + 3, y + 3, w - 6, Math.round((h - 6) * (1 - game.mana / type.cost)));
                g.setColor(WIPE);
                g.fillRoundRect(r.x + 3, y + 3, w - 6, h - 6, 12, 12);
                g.setClip(savedClip);
            }

            int badge = i * 2 + (affordable ? 1 : 0);
            if (cardBadges[badge] == null) {
                cardBadges[badge] = picture(g, BADGE_W, BADGE_H, p -> paintCostBadge(p, type.cost, affordable));
            }
            stamp(g, cardBadges[badge], r.x + (w - BADGE_W) / 2, y + h - 15 - BADGE_H / 2);
        }
    }

    private void paintCardFace(Graphics2D g, TowerType type, int number, int w, int h, boolean active) {
        g.setColor(CARD_SHADOW);
        g.fillRoundRect(2, 5, w, h, 16, 16);
        g.setColor(active ? GOLD : CARD_FRAME);
        g.fillRoundRect(0, 0, w, h, 16, 16);
        g.setPaint(new GradientPaint(0, 0, Ui.blend(type.color, CARD_FACE_TOP, 0.6f), 0, h,
                Ui.blend(type.color, CARD_FACE_BOTTOM, 0.85f)));
        g.fillRoundRect(3, 3, w - 6, h - 6, 12, 12);
        g.setColor(SHINE);
        g.fillRoundRect(6, 5, w - 12, 3, 3, 3);
        g.drawImage(game.assets.icon(type, 1), (w - 48) / 2, 7, null);
        g.setFont(TINY_BOLD);
        g.setColor(MUTED);
        g.drawString(String.valueOf(number), 8, 18);
        g.setColor(TEXT);
        g.drawString(type.label, (w - g.getFontMetrics().stringWidth(type.label)) / 2, 67);
    }

    private static void paintCostBadge(Graphics2D g, int price, boolean affordable) {
        int cx = BADGE_W / 2, cy = BADGE_H / 2;
        g.setColor(CARD_FRAME);
        g.fillOval(0, 0, BADGE_W, BADGE_H);
        g.setPaint(new GradientPaint(0, 2, MANA_LIGHT, 0, BADGE_H - 2, affordable ? MANA : MANA_DARK));
        g.fillOval(2, 2, BADGE_W - 4, BADGE_H - 4);
        g.setFont(BODY);
        String cost = String.valueOf(price);
        int costX = cx - g.getFontMetrics().stringWidth(cost) / 2;
        g.setColor(Color.BLACK);
        g.drawString(cost, costX + 1, cy + 6);
        g.setColor(affordable ? Color.WHITE : BAD);
        g.drawString(cost, costX, cy + 5);
    }

    /** Paints a {@code w} by {@code h} picture at the bars' resolution, with the look of {@code like}. */
    private BufferedImage picture(Graphics2D like, int w, int h, Consumer<Graphics2D> painter) {
        BufferedImage image = new BufferedImage((int) Math.ceil(w * barScale), (int) Math.ceil(h * barScale),
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHints(like.getRenderingHints());
        g.scale(barScale, barScale);
        painter.accept(g);
        g.dispose();
        return image;
    }

    /** Copies a {@link #picture} to where (x, y) falls on the screen, pixel for pixel. */
    private static void stamp(Graphics2D g, BufferedImage image, int x, int y) {
        AffineTransform at = g.getTransform();
        g.setTransform(DEVICE);
        g.drawImage(image, (int) Math.round(at.getTranslateX() + x * at.getScaleX()),
                (int) Math.round(at.getTranslateY() + y * at.getScaleY()), null);
        g.setTransform(at);
    }

    /** The mana bar under the hand: the regenerating resource every card is paid with. */
    private void drawMana(Graphics2D g) {
        Rectangle r = Layout.MANA;
        float cap = game.manaCap();
        int filled = Math.round(r.width * Math.min(1, game.mana / cap));
        g.setColor(CARD_FRAME);
        g.fillRoundRect(r.x - 2, r.y - 2, r.width + 4, r.height + 4, 12, 12);
        g.setColor(MANA_DARK);
        g.fillRoundRect(r.x, r.y, r.width, r.height, 10, 10);
        if (filled > 6) {
            g.setPaint(new GradientPaint(0, r.y, MANA_LIGHT, 0, r.y + r.height, MANA));
            g.fillRoundRect(r.x, r.y, filled, r.height, 10, 10);
            g.setColor(SHINE);
            g.fillRoundRect(r.x + 3, r.y + 2, filled - 6, 4, 4, 4);
        }
        g.setColor(CARD_FRAME);
        for (int i = 1; i < MANA_SEGMENTS; i++) {
            g.fillRect(r.x + r.width * i / MANA_SEGMENTS, r.y, 2, r.height);
        }
        if (game.manaFlash > 0) {
            // Tried to buy something unaffordable.
            g.setColor(new Color(248, 113, 113, Math.round(Math.min(1, game.manaFlash) * 200)));
            g.fillRoundRect(r.x, r.y, r.width, r.height, 10, 10);
        }
        if (game.mana >= cap - 1) {
            g.setColor(GOLD);
            g.drawRoundRect(r.x - 1, r.y - 1, r.width + 1, r.height + 1, 11, 11);
        }

        // Droplet badge with the current amount at the left end.
        int cx = Layout.HAND_X + 6, cy = r.y + r.height / 2;
        g.setColor(CARD_FRAME);
        g.fillRoundRect(cx - 24, cy - 15, 48, 30, 30, 30);
        g.setPaint(new GradientPaint(0, cy - 13, MANA_LIGHT, 0, cy + 13, MANA));
        g.fillRoundRect(cx - 22, cy - 13, 44, 26, 26, 26);
        g.setFont(BODY);
        String amount = String.valueOf((int) game.mana);
        int amountX = cx - g.getFontMetrics().stringWidth(amount) / 2;
        g.setColor(Color.BLACK);
        g.drawString(amount, amountX + 1, cy + 6);
        g.setColor(Color.WHITE);
        g.drawString(amount, amountX, cy + 5);

        g.setFont(TINY_BOLD);
        String rate = String.format("max %d   +%.1f/s", (int) cap, game.manaRegen());
        int rateX = r.x + r.width - g.getFontMetrics().stringWidth(rate) - 8;
        g.setColor(Color.BLACK);
        g.drawString(rate, rateX + 1, r.y + 14);
        g.setColor(Color.WHITE);
        g.drawString(rate, rateX, r.y + 13);
    }

    // ---- tower details -----------------------------------------------------------------

    /** The selected tower's card, floating over the corner of the field away from the tower. */
    private void drawTowerCard(Graphics2D g) {
        Tower selected = shownTower();
        if (selected == null) return;
        Rectangle r = Layout.towerCard(selected.col);
        stamp(g, towerCard, r.x, r.y);
    }

    /** The tower whose card is open, or null. */
    private Tower shownTower() {
        Tower selected = towers.get(game.selected);
        return game.placing != null || game.gameOver ? null : selected;
    }

    /** Repaints the open tower card's picture; it is refreshed along with the bars. */
    private void paintTowerCard(Graphics2D screen) {
        Tower selected = shownTower();
        if (selected == null) return;
        Rectangle r = Layout.towerCard(selected.col);
        Graphics2D g = towerCard.createGraphics();
        g.setComposite(AlphaComposite.Clear);
        g.fillRect(0, 0, towerCard.getWidth(), towerCard.getHeight());
        g.setComposite(AlphaComposite.SrcOver);
        g.setRenderingHints(screen.getRenderingHints());
        g.scale(barScale, barScale);
        g.translate(-r.x, -r.y);
        Ui.card(g, r);
        drawCard(g, r, selected.type, selected.tier, selected);
        g.dispose();
    }

    /** Tower details. {@code tower} is the placed tower being inspected, or null for a catalogue entry. */
    private void drawCard(Graphics2D g, Rectangle r, TowerType type, int tier, Tower tower) {
        int x = r.x + 10, width = r.width - 20;
        boolean hasNext = tower != null && tier < TowerType.MAX_TIER;
        g.drawImage(game.assets.icon(type, tier), r.x + 6, r.y + 6, null);
        g.setFont(HEAD);
        g.setColor(TEXT);
        g.drawString(type.label, r.x + 60, r.y + 24);
        g.setFont(TINY_BOLD);
        g.setColor(GOLD);
        g.drawString("Tier " + tier + (tier == TowerType.MAX_TIER ? "  MAX" : "")
                + (type.hybrid() ? "   Hybrid" : ""), r.x + 60, r.y + 40);
        g.setColor(MUTED);
        String subtitle;
        if (tower != null) subtitle = Ui.compact(tower.damageDealt) + " damage, " + tower.kills + " kills";
        else if (type.hybrid()) subtitle = type.parentA.label + " + " + type.parentB.label;
        else subtitle = "Cost " + type.cost;
        g.drawString(subtitle, r.x + 60, r.y + 54);

        // Stats, with what the next tier would bring shown in green.
        int y = r.y + 76;
        boolean beam = type.attack == TowerType.Attack.BEAM;
        g.setFont(TINY_BOLD);
        stat(g, x, y, "Damage", damageText(type, tier, beam), hasNext ? damageText(type, tier + 1, beam) : null);
        stat(g, x + 114, y, "Range", String.valueOf(Math.round(type.range(tier))),
                hasNext ? String.valueOf(Math.round(type.range(tier + 1))) : null);
        y += LINE;
        g.setColor(TEXT);
        String kind = type.damageType == DamageType.MAGIC ? "Magic" : "Physical";
        g.drawString(beam ? "Constant beam    " + kind : String.format("Reload %.1fs    %s", type.cooldown, kind), x, y);
        y += LINE;
        g.setColor(GOOD);
        g.drawString(type.special(tier), x, y);
        y += LINE + 2;
        g.setFont(TINY);
        g.setColor(MUTED);
        y = Ui.wrap(g, type.description, x, y, width, LINE);
        if (type.tradeoff != null) {
            g.setColor(WARN);
            y = Ui.wrap(g, "Trade-off: " + type.tradeoff, x, y, width, LINE);
        }
        if (tower == null) return;

        if (!type.hybrid() && tier < TowerType.MAX_TIER) {
            g.setColor(MANA_LIGHT);
            Ui.wrap(g, "Drag onto a different T" + tier + " base tower to fuse.", x, y + 2, width, LINE);
        }
        Tower twin = BuildSystem.nearestTwin(towerQuery.fetch(), towers, tower);
        g.setFont(BODY);
        String upgrade = tier == TowerType.MAX_TIER ? "Max tier" : twin == null ? "Needs a twin" : "Upgrade  [E]";
        Ui.labelButton(g, Layout.upgradeButton(r), upgrade, twin != null, true, game.mouseX, game.mouseY);
        Ui.labelButton(g, Layout.sellButton(r), "Sell " + Math.round(tower.invested * Game.SELL_REFUND), true, false,
                game.mouseX, game.mouseY);
    }

    private static String damageText(TowerType type, int tier, boolean beam) {
        return beam ? Math.round(type.dps(tier)) + "/s" : String.valueOf(Math.round(type.damage(tier)));
    }

    /** "Name value", optionally followed by a green arrow to the next tier's value. */
    private static void stat(Graphics2D g, int x, int y, String name, String value, String next) {
        FontMetrics metrics = g.getFontMetrics();
        String text = name + " " + value;
        g.setColor(TEXT);
        g.drawString(text, x, y);
        if (next == null || next.equals(value)) return;
        g.setColor(GOOD);
        g.drawString(" > " + next, x + metrics.stringWidth(text), y);
    }

    // ---- wave preview and controls -----------------------------------------------------

    private void drawPreview(Graphics2D g) {
        Rectangle r = Layout.PREVIEW;
        Ui.card(g, r);
        WavePlan plan = game.nextPlan;
        g.setFont(TINY_BOLD);
        g.setColor(MUTED);
        g.drawString("NEXT: WAVE " + plan.wave, r.x + 12, r.y + 19);
        int slotWidth = Layout.previewSlot(plan.counts.size());
        int size = Math.min(44, slotWidth - 6);
        int hoveredSlot = Layout.previewSlotAt(game.mouseX, game.mouseY, plan.counts.size());
        int slot = 0;
        g.setFont(BODY);
        for (Map.Entry<EnemyType, Integer> entry : plan.counts.entrySet()) {
            int x = r.x + 10 + slot * slotWidth;
            int middle = x + (slotWidth - 4) / 2;
            if (slot == hoveredSlot) {
                g.setColor(CELL_HOVER);
                g.fillRoundRect(x - 2, r.y + 28, slotWidth - 4, 84, 8, 8);
            }
            BufferedImage image = game.assets.enemy(entry.getKey())[0];
            Ui.fit(g, image, middle - size / 2, r.y + 58 - size / 2, size);
            g.setColor(TEXT);
            String count = "x" + entry.getValue();
            g.drawString(count, middle - g.getFontMetrics().stringWidth(count) / 2, r.y + 102);
            slot++;
        }
    }

    private void drawEnemyTip(Graphics2D g, EnemyType type, WavePlan plan) {
        Rectangle tip = new Rectangle(Layout.PREVIEW.x, Layout.BAR_Y - 138, TOOLTIP_W, 126);
        Ui.card(g, tip);
        Ui.fit(g, game.assets.enemy(type)[0], tip.x + 10, tip.y + 10, 32);
        g.setFont(HEAD);
        g.setColor(TEXT);
        g.drawString(type.label, tip.x + 52, tip.y + 24);
        g.setFont(TINY_BOLD);
        g.setColor(GOLD);
        String kind = type.boss() ? (type.flying ? "Flying boss" : "Boss") : type.flying ? "Flying" : "Ground";
        g.drawString(kind, tip.x + 52, tip.y + 40);
        g.setColor(TEXT);
        g.drawString("Health " + Ui.compact(type.hp * plan.hpScale * game.map.toughness) + "    Speed " + Math.round(type.speed)
                + "    Armor " + Math.round(type.armor), tip.x + 10, tip.y + 62);
        g.setFont(TINY);
        g.setColor(MUTED);
        Ui.wrap(g, type.trait, tip.x + 10, tip.y + 80, tip.width - 20, LINE);
    }

    private void drawControls(Graphics2D g) {
        String next;
        boolean canCall = true;
        if (game.gameOver) {
            next = "Restart   [R]";
        } else if (game.wave == 0) {
            next = "Start first wave   [SPACE]";
        } else if (game.spawning()) {
            next = "Wave in progress";
            canCall = false;
        } else {
            int bonus = WaveSystem.earlyBonus(game);
            next = "Call next wave" + (bonus > 0 ? "  +" + bonus : "") + "   [SPACE]";
        }
        g.setFont(HEAD);
        Ui.labelButton(g, Layout.NEXT, next, canCall, true, game.mouseX, game.mouseY);
        g.setFont(BODY);
        Ui.labelButton(g, Layout.SPEED, "Speed x" + game.speed, true, false, game.mouseX, game.mouseY);
        Ui.labelButton(g, Layout.PAUSE, "Pause", true, false, game.mouseX, game.mouseY);
        Ui.labelButton(g, Layout.CHART, "Fusions", true, false, game.mouseX, game.mouseY);
        g.setFont(TINY);
        g.setColor(MUTED);
        g.drawString("F speed   P pause   C fusions   H help   Esc cancel", Layout.NEXT.x, Layout.BAR_Y + 118);
    }

    // ---- tips and overlays -------------------------------------------------------------

    /** The current contextual tip, just above the bottom bar (or up top while a tower card is open). */
    private void drawHint(Graphics2D g) {
        if (game.hintTime <= 0 || game.gameOver || game.paused || game.showHelp || game.showChart
                || game.victoryScreen) return;
        Composite saved = g.getComposite();
        g.setComposite(AlphaComposite.SrcOver.derive(Math.min(1, game.hintTime * 2)));
        g.setFont(BODY);
        int width = g.getFontMetrics().stringWidth(game.hint) + 62, height = 30;
        boolean cardOpen = game.world.alive(game.selected) && game.placing == null;
        int x = (Layout.WIDTH - width) / 2, y = cardOpen ? Layout.TOP + 56 : Layout.BAR_Y - height - 12;
        g.setColor(TIP_BACK);
        g.fillRoundRect(x, y, width, height, 12, 12);
        g.setColor(GOLD);
        g.drawRoundRect(x, y, width - 1, height - 1, 12, 12);
        g.fillRoundRect(x + 6, y + 6, 34, 18, 8, 8);
        g.setFont(TINY_BOLD);
        g.setColor(CARD);
        g.drawString("TIP", x + 14, y + 19);
        g.setFont(BODY);
        g.setColor(TEXT);
        g.drawString(game.hint, x + 50, y + 20);
        g.setComposite(saved);
    }

    /** The pause menu, which doubles as the defeat screen. */
    private void drawMenu(Graphics2D g) {
        g.setColor(Ui.VEIL);
        g.fillRect(0, Layout.TOP, Layout.WIDTH, Layout.BAR_Y - Layout.TOP);
        Rectangle r = Layout.MENU_CARD;
        Ui.card(g, r);
        int cx = r.x + r.width / 2;
        if (game.gameOver) {
            Ui.centered(g, "The keep has fallen", Ui.BIG, BAD, cx, r.y + 46);
            Ui.centered(g, "Wave " + game.wave + "   " + game.kills + " monsters slain", BODY, MUTED, cx, r.y + 74);
        } else {
            Ui.centered(g, "Paused", Ui.BIG, TEXT, cx, r.y + 46);
            Ui.centered(g, game.map.label + "   wave " + game.wave, BODY, MUTED, cx, r.y + 74);
        }
        Game.MenuAction[] actions = game.menuActions();
        g.setFont(HEAD);
        for (int i = 0; i < actions.length; i++) {
            Ui.labelButton(g, Layout.MENU_BUTTONS[i], actions[i].label, true, i == 0, game.mouseX, game.mouseY);
        }
    }

    private void drawVictory(Graphics2D g) {
        g.setColor(Ui.VEIL);
        g.fillRect(0, 0, Layout.WIDTH, Layout.HEIGHT);
        int cx = Layout.WIDTH / 2, cy = Layout.HEIGHT / 2;
        Ui.centered(g, "VICTORY", Ui.HUGE, GOLD, cx, cy - 40);
        Ui.centered(g, game.map.label + " stands after " + Game.FINAL_WAVE + " waves.", Ui.BIG, TEXT, cx, cy + 10);
        Ui.centered(g, game.kills + " monsters slain, " + game.lives + " lives to spare", HEAD, MUTED, cx, cy + 44);
        Ui.centered(g, "Click or press any key to keep going in endless mode", HEAD,
                Ui.pulse(GOOD, game.uiTime), cx, cy + 90);
    }

    /** What every pair of base towers fuses into, with the hovered tower's details beside it. */
    private void drawChart(Graphics2D g) {
        g.setColor(Ui.VEIL);
        g.fillRect(0, 0, Layout.WIDTH, Layout.HEIGHT);
        Rectangle r = Layout.CHART_CARD;
        Ui.card(g, r);
        g.setFont(Ui.BIG);
        g.setColor(GOLD);
        g.drawString("Fusion chart", r.x + 24, r.y + 40);

        int cell = Layout.RECIPE_CELL, n = TowerType.BASES.length;
        TowerType hovered = Layout.recipeAt(game.mouseX, game.mouseY);
        for (int row = -1; row < n; row++) {
            for (int col = -1; col < n; col++) {
                if (row < 0 && col < 0) continue;
                int x = Layout.RECIPE_X + (col + 1) * cell, y = Layout.RECIPE_Y + (row + 1) * cell;
                TowerType type = Layout.recipeAt(x + 1, y + 1);
                if (row >= 0 && col >= 0) {
                    g.setColor(type == hovered && row != col ? CELL_HOVER : CELL);
                    g.fillRoundRect(x + 1, y + 1, cell - 2, cell - 2, 8, 8);
                }
                if (row == col) {
                    // Diagonal: a tower with its twin is an upgrade, not a fusion.
                    g.setFont(HEAD);
                    g.setColor(GOLD);
                    g.drawString("T+", x + 17, y + 33);
                    continue;
                }
                g.drawImage(game.assets.icon(type, 1), x + 3, y + 3, null);
            }
        }

        Rectangle details = Layout.CHART_DETAILS;
        if (hovered != null) {
            g.setColor(CELL);
            g.fillRoundRect(details.x, details.y, details.width, details.height, 10, 10);
            drawCard(g, details, hovered, 1, null);
        } else {
            g.setFont(Ui.PLAIN);
            g.setColor(TEXT);
            int y = Ui.wrap(g, "Pick a row and a column: the cell where they cross is the hybrid those two base "
                    + "towers fuse into. Hover any cell for details.", details.x, details.y + 18, details.width, 19);
            g.setColor(MUTED);
            y = Ui.wrap(g, "To fuse, drag one base tower onto a different base tower of the same tier. The hybrid "
                    + "keeps that tier.", details.x, y + 10, details.width, 19);
            g.setColor(GOLD);
            Ui.wrap(g, "T+ marks a tower with its twin: that is an upgrade to the next tier, up to tier 4. "
                    + "Hybrids upgrade the same way.", details.x, y + 10, details.width, 19);
        }
        g.setFont(BODY);
        g.setColor(MUTED);
        String close = "Click or press any key to close";
        g.drawString(close, details.x, r.y + r.height - 24);
    }
}
