package game;

import java.awt.Rectangle;

/**
 * Logical screen layout shared by input handling and drawing: a slim status bar on top, the
 * field, and a bottom bar holding the hand of tower cards with the mana bar beneath it.
 */
public final class Layout {
    public static final int WIDTH = Level.WIDTH;
    /** Height of the status bar; the field starts below it. */
    public static final int TOP = 40;
    /** Top of the bottom bar. */
    public static final int BAR_Y = TOP + Level.HEIGHT;
    public static final int BAR_H = 136;
    public static final int HEIGHT = BAR_Y + BAR_H;

    // Status bar.
    public static final Rectangle MENU = new Rectangle(8, 6, 70, 28);
    public static final Rectangle HELP = new Rectangle(WIDTH - 36, 6, 28, 28);

    // Bottom bar: next wave on the left, the hand in the middle, controls on the right.
    public static final Rectangle PREVIEW = new Rectangle(12, BAR_Y + 8, 232, 120);
    /** The widest a wave preview entry gets; entries narrow when a wave has many kinds of monster. */
    public static final int PREVIEW_SLOT = 60;
    public static final int CARD_W = 82, CARD_H = 96, CARD_GAP = 6;
    public static final int HAND_X = 256;
    public static final int HAND_W = TowerType.BASES.length * (CARD_W + CARD_GAP) - CARD_GAP;
    public static final Rectangle[] CARDS = new Rectangle[TowerType.BASES.length];
    public static final Rectangle MANA = new Rectangle(HAND_X + 18, BAR_Y + 110, HAND_W - 18, 18);
    public static final Rectangle NEXT = new Rectangle(884, BAR_Y + 8, 256, 46);
    public static final Rectangle SPEED = new Rectangle(884, BAR_Y + 62, 80, 32);
    public static final Rectangle PAUSE = new Rectangle(972, BAR_Y + 62, 80, 32);
    public static final Rectangle CHART = new Rectangle(1060, BAR_Y + 62, 80, 32);

    // The selected tower's card floats over a corner of the field.
    public static final int TOWER_CARD_W = 236, TOWER_CARD_H = 240;

    // Pause and defeat menu.
    public static final Rectangle MENU_CARD = new Rectangle((WIDTH - 340) / 2, TOP + 96, 340, 262);
    public static final Rectangle[] MENU_BUTTONS = new Rectangle[3];

    // Fusion chart overlay: a header row and column plus one cell per pair of base towers.
    public static final int RECIPE_CELL = 54;
    private static final int RECIPE_GRID = (TowerType.BASES.length + 1) * RECIPE_CELL;
    public static final Rectangle CHART_CARD =
            new Rectangle((WIDTH - RECIPE_GRID - 436) / 2, (HEIGHT - RECIPE_GRID - 88) / 2, RECIPE_GRID + 436, RECIPE_GRID + 88);
    public static final int RECIPE_X = CHART_CARD.x + 24, RECIPE_Y = CHART_CARD.y + 60;
    public static final Rectangle CHART_DETAILS =
            new Rectangle(RECIPE_X + RECIPE_GRID + 24, CHART_CARD.y + 60, 364, 240);

    // Title screen and map select.
    public static final Rectangle[] TITLE_BUTTONS = new Rectangle[3];
    public static final Rectangle[] MAP_CARDS = new Rectangle[MapDef.values().length];
    public static final Rectangle BACK = new Rectangle(26, HEIGHT - 62, 140, 40);

    static {
        for (int i = 0; i < CARDS.length; i++) {
            CARDS[i] = new Rectangle(HAND_X + i * (CARD_W + CARD_GAP), BAR_Y + 8, CARD_W, CARD_H);
        }
        for (int i = 0; i < MENU_BUTTONS.length; i++) {
            MENU_BUTTONS[i] = new Rectangle(MENU_CARD.x + 40, MENU_CARD.y + 110 + i * 46, MENU_CARD.width - 80, 38);
        }
        for (int i = 0; i < TITLE_BUTTONS.length; i++) {
            TITLE_BUTTONS[i] = new Rectangle((WIDTH - 280) / 2, 350 + i * 60, 280, 48);
        }
        int gap = 20, width = (WIDTH - 52 - gap * (MAP_CARDS.length - 1)) / MAP_CARDS.length;
        for (int i = 0; i < MAP_CARDS.length; i++) {
            MAP_CARDS[i] = new Rectangle(26 + i * (width + gap), 150, width, 330);
        }
    }

    /** The selected tower's card sits on the side of the field away from the tower. */
    public static Rectangle towerCard(int towerCol) {
        int x = towerCol >= Level.COLS / 2 ? 10 : WIDTH - TOWER_CARD_W - 10;
        return new Rectangle(x, BAR_Y - TOWER_CARD_H - 10, TOWER_CARD_W, TOWER_CARD_H);
    }

    public static Rectangle upgradeButton(Rectangle card) {
        return new Rectangle(card.x + 8, card.y + card.height - 38, 126, 30);
    }

    public static Rectangle sellButton(Rectangle card) {
        return new Rectangle(card.x + 140, card.y + card.height - 38, 88, 30);
    }

    /**
     * The tower shown by the fusion chart cell under the point: a base tower on the headers and
     * the diagonal, a hybrid elsewhere, or null when the point is off the chart.
     */
    public static TowerType recipeAt(int x, int y) {
        int col = Math.floorDiv(x - RECIPE_X, RECIPE_CELL) - 1, row = Math.floorDiv(y - RECIPE_Y, RECIPE_CELL) - 1;
        int n = TowerType.BASES.length;
        if (col < -1 || row < -1 || col >= n || row >= n || (col < 0 && row < 0)) return null;
        if (col < 0) return TowerType.BASES[row];
        if (row < 0 || row == col) return TowerType.BASES[col];
        return TowerType.hybridOf(TowerType.BASES[row], TowerType.BASES[col]);
    }

    /** Width of one wave preview entry when the wave has this many kinds of monster. */
    public static int previewSlot(int entries) {
        return Math.min(PREVIEW_SLOT, (PREVIEW.width - 16) / Math.max(1, entries));
    }

    /** Index of the wave preview entry under the point, or -1. */
    public static int previewSlotAt(int x, int y, int entries) {
        if (y < PREVIEW.y + 26 || y >= PREVIEW.y + PREVIEW.height || x < PREVIEW.x + 10) return -1;
        int slot = (x - PREVIEW.x - 10) / previewSlot(entries);
        return slot < entries ? slot : -1;
    }

    private Layout() {}
}
