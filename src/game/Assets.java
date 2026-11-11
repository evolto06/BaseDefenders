package game;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

/**
 * All art, generated in code at startup: monsters from ASCII pixel maps, towers and terrain
 * from primitives on a small canvas, then scaled up with nearest-neighbour for a pixel-art look.
 */
public final class Assets {
    /** Towers and terrain are drawn at this tile size and doubled. */
    private static final int ART = Level.TILE / 2;
    /** Platform rim colour per tier: stone, bronze, silver, gold. */
    private static final int[] TIER_RIM = {0x5B6472, 0xB87333, 0xC9D1DC, 0xF5C542};
    private static final int[] TIER_RIM_DARK = {0x2B303A, 0x6B3F16, 0x6B7280, 0x92660A};

    private final Map<MapDef, BufferedImage> backgrounds = new EnumMap<>(MapDef.class);
    /** Darkens the edges of the field. */
    public final BufferedImage vignette;
    private final Map<EnemyType, BufferedImage[]> enemies = new EnumMap<>(EnemyType.class);
    private final Map<EnemyType, BufferedImage[]> silhouettes = new EnumMap<>(EnemyType.class);
    private final Map<TowerType, BufferedImage[]> bases = new EnumMap<>(TowerType.class);
    private final Map<TowerType, BufferedImage> turrets = new EnumMap<>(TowerType.class);
    private final Map<TowerType, BufferedImage[]> icons = new EnumMap<>(TowerType.class);
    private final Map<TowerType, BufferedImage> smallIcons = new EnumMap<>(TowerType.class);

    public Assets() {
        for (EnemyType type : EnemyType.values()) {
            BufferedImage image = scale(enemyArt(type), type.scale);
            enemies.put(type, new BufferedImage[] {image, mirror(image)});
            BufferedImage white = silhouette(image);
            silhouettes.put(type, new BufferedImage[] {white, mirror(white)});
        }
        for (TowerType type : TowerType.values()) {
            BufferedImage turret = turretArt(type);
            BufferedImage[] tierBases = new BufferedImage[TowerType.MAX_TIER];
            BufferedImage[] tierIcons = new BufferedImage[TowerType.MAX_TIER];
            for (int tier = 1; tier <= TowerType.MAX_TIER; tier++) {
                BufferedImage base = towerBase(type, tier);
                BufferedImage icon = new BufferedImage(ART, ART, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = icon.createGraphics();
                g.drawImage(base, 0, 0, null);
                g.drawImage(turret, 0, 0, null);
                g.dispose();
                if (tier == 1) smallIcons.put(type, icon);
                tierBases[tier - 1] = scale(base, 2);
                tierIcons[tier - 1] = scale(icon, 2);
            }
            bases.put(type, tierBases);
            icons.put(type, tierIcons);
            turrets.put(type, scale(turret, 2));
        }
        vignette = vignette();
    }

    /** The painted terrain of a map, built the first time it is asked for. */
    public BufferedImage background(MapDef map) {
        return backgrounds.computeIfAbsent(map, m -> paintTerrain(new Level(m)));
    }

    /** Index 0 faces right, index 1 is mirrored. */
    public BufferedImage[] enemy(EnemyType type) {
        return enemies.get(type);
    }

    /** All-white copies of the enemy sprites, drawn over them as a hit flash. */
    public BufferedImage[] enemySilhouette(EnemyType type) {
        return silhouettes.get(type);
    }

    public BufferedImage base(TowerType type, int tier) {
        return bases.get(type)[tier - 1];
    }

    public BufferedImage turret(TowerType type) {
        return turrets.get(type);
    }

    /** Full-tile picture of the whole tower. */
    public BufferedImage icon(TowerType type, int tier) {
        return icons.get(type)[tier - 1];
    }

    public BufferedImage smallIcon(TowerType type) {
        return smallIcons.get(type);
    }

    // ---- monsters ------------------------------------------------------------------------

    private static BufferedImage enemyArt(EnemyType type) {
        return switch (type) {
            case SLIME, MINI_SLIME -> pixelArt(new String[] {
                "................",
                "................",
                "................",
                "................",
                ".....oooooo.....",
                "...oogggggGoo...",
                "..ogggggggggGo..",
                ".ogghgggggggGGo.",
                ".oghgggggggggGo.",
                "ogggwwgggwwggGGo",
                "ogggwkgggwkggGGo",
                "oggggggggggggGGo",
                "oggggmmmmmgggGGo",
                "oGggggggggggGGGo",
                ".oGGGGGGGGGGGGo.",
                "..oooooooooooo..",
            }, "ogGhwkm", 0x1F5C1F, 0x5BBF4A, 0x3E9A38, 0xB8F0A0, 0xFFFFFF, 0x102010, 0x2A6B2A);
            case GOBLIN -> pixelArt(new String[] {
                "................",
                "................",
                ".....oooooo.....",
                ".o..oggggggo..o.",
                ".ogoggggggggogo.",
                ".ogggwkggwkgggo.",
                "..oggggggggggo..",
                "...ogggmmgggo...",
                "....oggggggo....",
                "...oobbbbbboo...",
                "..ogobbbbbbogo..",
                "..ogobbyybbogo..",
                "..oo.obbbbo.oo..",
                ".....obooobo....",
                "....ogo..ogo....",
                "....ooo..ooo....",
            }, "ogwkmby", 0x243B14, 0x8BC34A, 0xFFFFFF, 0x101010, 0x3B1F1F, 0x7A4B24, 0xE0B040);
            case ORC -> pixelArt(new String[] {
                "................",
                "....oooooooo....",
                "...oggggggggo...",
                "..oggggggggggo..",
                "..ogrkggggrkgo..",
                "..oggggggggggo..",
                "..ogwgmmmmgwgo..",
                "...oggggggggo...",
                ".oossssssssssoo.",
                "ogosssSssSsssogo",
                "ogossssssssssogo",
                "ogosssSssSsssogo",
                ".oo.obbbbbbo.oo.",
                "....obbo.obbo...",
                "....obbo.obbo...",
                "....oooo.oooo...",
            }, "ogrkwmsSb", 0x1B2E14, 0x4E8F3A, 0xE23B3B, 0x101010, 0xF2F2E0, 0x2A1A1A, 0x6B5A48,
                    0x9C8A70, 0x4A3A2C);
            case BAT -> pixelArt(new String[] {
                "................",
                "................",
                "................",
                ".o....o..o....o.",
                "opo...oooo...opo",
                "oppo.oppppo.oppo",
                "opppoppppppopppo",
                "opppprpppprppppo",
                "oppppppwwppppppo",
                ".oppppppppppppo.",
                ".op.oppppppo.po.",
                ".o...oppppo...o.",
                "......oppo......",
                ".......oo.......",
                "................",
                "................",
            }, "oprw", 0x2A1846, 0x7C4DBE, 0xFF4040, 0xFFFFFF);
            case SHAMAN -> pixelArt(new String[] {
                "..............y.",
                "....oooooo...yYy",
                "...owwwwwwo...y.",
                "...owkwwkwo...s.",
                "...owwwwwwo...s.",
                "....owkkwo....s.",
                "...oobbbboo...s.",
                "..obbbbbbbbo..s.",
                ".obbbBbbBbbbooso",
                ".obbbBbbBbbbbgso",
                ".oobbBbbBbboo.s.",
                "..obbBbbBbbo..s.",
                "..obbbbbbbbo..s.",
                "..obbbbbbbbo..s.",
                ".obbbbbbbbbbo.s.",
                ".oooooooooooo.s.",
            }, "owkbBsyYg", 0x16204A, 0xE8E0D0, 0x101010, 0x3F6FD8, 0x2B4FA8, 0x7A4B24, 0xFFE14A,
                    0xFFF6B0, 0x8BC34A);
            case WRAITH -> pixelArt(new String[] {
                "................",
                ".....oooooo.....",
                "...oowwwwwwoo...",
                "..owwwwwwwwwwo..",
                ".owwwwwwwwwwwwo.",
                ".owwkkwwwwkkwwo.",
                ".owwkkwwwwkkwwo.",
                ".owwwwwwwwwwwwo.",
                ".owwwwwkkwwwwwo.",
                ".owwwwwkkwwwwwo.",
                ".owwwwwwwwwwwwo.",
                ".owwwwwwwwwwwwo.",
                ".owwwwwwwwwwwwo.",
                ".owwwowwwwowwwo.",
                ".owwo.owwo.owwo.",
                "..oo...oo...oo..",
            }, "owk", 0xB0306070, 0xC8D8F8FF, 0xFF103040);
            case GOLEM -> pixelArt(new String[] {
                "....oooooooo....",
                "...osssssssso...",
                "...osyyssyyso...",
                "...osyyssyyso...",
                ".ooossssssssooo.",
                "osssSssssssSssso",
                "ossssSssssSsssso",
                "osssossssssossso",
                "osssossSSssossso",
                "osssossssssossso",
                "osso.osssso.osso",
                ".oo..osssso..oo.",
                "....osso.osso...",
                "....osso.osso...",
                "...ossso.ossso..",
                "...ooooo.ooooo..",
            }, "osSy", 0x2B2F36, 0x8A8F98, 0x5F646D, 0xFFA020);
        };
    }

    /**
     * Builds an image from rows of characters. '.' is transparent; every other character is
     * looked up in {@code keys} to pick a colour. Colours without an alpha byte are opaque.
     */
    private static BufferedImage pixelArt(String[] rows, String keys, int... colors) {
        int width = rows[0].length();
        BufferedImage image = new BufferedImage(width, rows.length, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < rows.length; y++) {
            if (rows[y].length() != width) {
                throw new IllegalStateException("Sprite row " + y + " is " + rows[y].length() + " wide: " + rows[y]);
            }
            for (int x = 0; x < width; x++) {
                char c = rows[y].charAt(x);
                if (c == '.') continue;
                int key = keys.indexOf(c);
                if (key < 0) throw new IllegalStateException("Unknown sprite colour '" + c + "'");
                int argb = colors[key];
                image.setRGB(x, y, argb >>> 24 == 0 ? argb | 0xFF000000 : argb);
            }
        }
        return image;
    }

    private static BufferedImage silhouette(BufferedImage source) {
        BufferedImage white = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                white.setRGB(x, y, source.getRGB(x, y) | 0x00FFFFFF);
            }
        }
        return white;
    }

    // ---- towers --------------------------------------------------------------------------

    /** The platform a turret sits on. Its rim shows the tier; its trim shows the tower's parents. */
    private static BufferedImage towerBase(TowerType type, int tier) {
        BufferedImage image = new BufferedImage(ART, ART, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        fillRound(g, TIER_RIM_DARK[tier - 1], 1, 1, 22, 22, 6);
        fillRound(g, TIER_RIM[tier - 1], 2, 2, 20, 19, 5);
        fillRound(g, 0x6E7786, 3, 3, 18, 17, 4);
        fillRound(g, 0x8891A0, 3, 3, 18, 15, 4);
        rect(g, 0xA7AFBC, 5, 4, 14, 1);
        g.setColor(type.color);
        g.fillRect(4, 19, 8, 2);
        g.setColor(type.colorB);
        g.fillRect(12, 19, 8, 2);
        oval(g, 0x434B58, 6, 6, 12, 12);
        oval(g, 0x535C6B, 7, 7, 10, 10);
        if (tier >= 3) {
            // Studs at the corners for the upper tiers.
            int stud = tier == 4 ? 0xFFF3B0 : 0xFFFFFF;
            rect(g, stud, 3, 3, 2, 2);
            rect(g, stud, 19, 3, 2, 2);
            rect(g, stud, 3, 16, 2, 2);
            rect(g, stud, 19, 16, 2, 2);
        }
        g.dispose();
        return image;
    }

    /** Turret art on a transparent canvas, pointing right, rotating about the centre. */
    private static BufferedImage turretArt(TowerType type) {
        BufferedImage image = new BufferedImage(ART, ART, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        switch (type) {
            case ARROW -> {
                rect(g, 0x5C3A1A, 5, 11, 13, 2);
                rect(g, 0x9A6A35, 6, 11, 11, 1);
                rect(g, 0x5C3A1A, 15, 5, 2, 14);
                rect(g, 0x5C3A1A, 13, 4, 2, 2);
                rect(g, 0x5C3A1A, 13, 18, 2, 2);
                g.setColor(new Color(0xE5E7EB));
                g.drawLine(13, 5, 8, 11);
                g.drawLine(8, 12, 13, 18);
                rect(g, 0xD1D5DB, 17, 11, 5, 2);
                rect(g, 0xFFFFFF, 22, 11, 1, 2);
            }
            case CANNON -> cannon(g, 0x1F2937, 0x4B5563, 0x1F2937, 0x7C8696);
            case FROST -> crystal(g, 0x0369A1, 0x7DD3FC, 0xBAE6FD);
            case TESLA -> {
                prongs(g, 0x9CA3AF);
                coil(g, 0x374151, 0x9CA3AF, 0xFDE047, 0xFFFFFF);
            }
            case VENOM -> {
                oval(g, 0x365314, 4, 6, 13, 12);
                oval(g, 0x65A30D, 5, 7, 11, 10);
                rect(g, 0x365314, 13, 9, 8, 6);
                rect(g, 0x4D7C0F, 13, 10, 7, 4);
                rect(g, 0xBEF264, 20, 10, 3, 4);
                rect(g, 0xD9F99D, 7, 9, 3, 2);
            }
            case PRISM -> {
                rays(g, 0xFDE047);
                crystal(g, 0x9A3412, 0xFB923C, 0xFED7AA);
                oval(g, 0xFEF3C7, 10, 9, 4, 5);
            }
            case FLAK -> {
                rect(g, 0x1F2937, 12, 7, 10, 3);
                rect(g, 0x1F2937, 12, 14, 10, 3);
                rect(g, 0x9CA3AF, 21, 7, 2, 3);
                rect(g, 0x9CA3AF, 21, 14, 2, 3);
                oval(g, 0x3F4A22, 5, 5, 13, 14);
                oval(g, 0x6B7F3A, 6, 6, 11, 12);
                rect(g, 0xB5803A, 9, 11, 5, 2);
            }
            case ICE_LANCE -> {
                rect(g, 0x0C4A6E, 3, 11, 14, 2);
                rect(g, 0x38BDF8, 14, 5, 2, 14);
                rect(g, 0x38BDF8, 12, 4, 2, 2);
                rect(g, 0x38BDF8, 12, 18, 2, 2);
                rect(g, 0x7DD3FC, 4, 10, 3, 4);
                rect(g, 0xE0F2FE, 16, 11, 7, 2);
            }
            case RAILGUN -> {
                oval(g, 0x374151, 3, 7, 10, 10);
                rect(g, 0x111827, 8, 8, 15, 2);
                rect(g, 0x111827, 8, 14, 15, 2);
                rect(g, 0xFDE047, 10, 10, 12, 4);
                rect(g, 0xFEF9C3, 10, 11, 12, 2);
                oval(g, 0xB5803A, 5, 9, 6, 6);
            }
            case BLIGHT -> {
                rect(g, 0x3F6212, 5, 11, 12, 2);
                rect(g, 0x65A30D, 15, 4, 2, 16);
                rect(g, 0x65A30D, 13, 3, 2, 2);
                rect(g, 0x65A30D, 13, 19, 2, 2);
                rect(g, 0xD9F99D, 17, 6, 5, 1);
                rect(g, 0xD9F99D, 17, 11, 6, 2);
                rect(g, 0xD9F99D, 17, 17, 5, 1);
            }
            case SUNBOW -> {
                rect(g, 0x78350F, 4, 10, 14, 4);
                rect(g, 0xB45309, 5, 11, 12, 2);
                rect(g, 0xF59E0B, 15, 4, 3, 16);
                rect(g, 0xF59E0B, 13, 3, 3, 2);
                rect(g, 0xF59E0B, 13, 19, 3, 2);
                rect(g, 0xFEF3C7, 18, 11, 5, 2);
                rect(g, 0xFB923C, 7, 10, 4, 4);
                rect(g, 0xFFFFFF, 8, 11, 2, 2);
            }
            case BLIZZARD -> cannon(g, 0x0C4A6E, 0x38BDF8, 0xE0F2FE, 0xBAE6FD);
            case PLASMA -> {
                oval(g, 0x3B0764, 5, 6, 12, 12);
                oval(g, 0x7E22CE, 6, 7, 10, 10);
                rect(g, 0x3B0764, 11, 8, 9, 8);
                rect(g, 0x9333EA, 11, 9, 8, 6);
                oval(g, 0xF0ABFC, 17, 8, 6, 8);
                rect(g, 0xFFFFFF, 19, 11, 2, 2);
            }
            case ACID -> cannon(g, 0x1A2E05, 0x4D7C0F, 0xD9F99D, 0x84CC16);
            case CLUSTER -> {
                oval(g, 0x1F2937, 4, 6, 12, 12);
                oval(g, 0x4B5563, 5, 7, 10, 10);
                rect(g, 0x1F2937, 11, 6, 11, 12);
                rect(g, 0x374151, 11, 7, 10, 10);
                rect(g, 0xFB923C, 18, 8, 3, 3);
                rect(g, 0xFB923C, 18, 13, 3, 3);
                rect(g, 0x9A3412, 13, 8, 3, 3);
                rect(g, 0x9A3412, 13, 13, 3, 3);
            }
            case STORM -> {
                rect(g, 0xFDE047, 3, 3, 2, 2);
                rect(g, 0xFDE047, 19, 3, 2, 2);
                rect(g, 0xFDE047, 3, 19, 2, 2);
                rect(g, 0xFDE047, 19, 19, 2, 2);
                crystal(g, 0x155E75, 0x67E8F9, 0xA5F3FC);
                oval(g, 0xFDE047, 9, 8, 6, 6);
                rect(g, 0xFFFFFF, 11, 10, 2, 2);
            }
            case MIASMA -> {
                crystal(g, 0x1A2E05, 0x65A30D, 0xA3E635);
                oval(g, 0x7DD3FC, 9, 9, 6, 5);
                rect(g, 0xFFFFFF, 11, 10, 2, 2);
            }
            case AURORA -> {
                rays(g, 0xBAE6FD);
                crystal(g, 0x0369A1, 0x7DD3FC, 0xE0F2FE);
                oval(g, 0xFB923C, 9, 8, 6, 6);
                rect(g, 0xFFFFFF, 11, 10, 2, 2);
            }
            case PLAGUE -> {
                prongs(g, 0x84CC16);
                coil(g, 0x1A2E05, 0x65A30D, 0xFDE047, 0xD9F99D);
            }
            case ARC -> {
                rect(g, 0xFDE047, 11, 1, 2, 5);
                rect(g, 0xFDE047, 2, 16, 5, 2);
                rect(g, 0xFDE047, 17, 16, 5, 2);
                coil(g, 0x374151, 0xFACC15, 0xFB923C, 0xFFFFFF);
            }
            case SIPHON -> {
                crystal(g, 0x3F6212, 0x84CC16, 0xD9F99D);
                g.setColor(new Color(0x0C4A6E));
                g.fillPolygon(new int[] {12, 16, 12, 8}, new int[] {6, 11, 17, 11}, 4);
                g.setColor(new Color(0x38BDF8));
                g.fillPolygon(new int[] {12, 15, 12, 9}, new int[] {7, 11, 16, 11}, 4);
                rect(g, 0xFFFFFF, 11, 9, 1, 3);
            }
        }
        g.dispose();
        return image;
    }

    private static void cannon(Graphics2D g, int dark, int mid, int muzzle, int shine) {
        oval(g, dark, 5, 6, 12, 12);
        oval(g, mid, 6, 7, 10, 10);
        rect(g, dark, 11, 9, 11, 6);
        rect(g, mid, 11, 10, 10, 4);
        rect(g, muzzle, 20, 8, 3, 8);
        rect(g, shine, 12, 10, 7, 1);
    }

    private static void crystal(Graphics2D g, int outline, int dark, int light) {
        g.setColor(new Color(outline));
        g.fillPolygon(new int[] {12, 19, 12, 5}, new int[] {1, 11, 22, 11}, 4);
        g.setColor(new Color(dark));
        g.fillPolygon(new int[] {12, 17, 12, 7}, new int[] {3, 11, 20, 11}, 4);
        g.setColor(new Color(light));
        g.fillPolygon(new int[] {12, 12, 7}, new int[] {3, 20, 11}, 3);
        rect(g, 0xFFFFFF, 10, 8, 1, 4);
    }

    private static void coil(Graphics2D g, int ring, int body, int core, int spark) {
        oval(g, ring, 5, 5, 14, 14);
        oval(g, body, 7, 7, 10, 10);
        oval(g, core, 9, 9, 6, 6);
        rect(g, spark, 11, 11, 2, 2);
    }

    private static void prongs(Graphics2D g, int rgb) {
        rect(g, rgb, 11, 2, 2, 4);
        rect(g, rgb, 11, 18, 2, 4);
        rect(g, rgb, 2, 11, 4, 2);
        rect(g, rgb, 18, 11, 4, 2);
    }

    private static void rays(Graphics2D g, int rgb) {
        rect(g, rgb, 4, 4, 2, 2);
        rect(g, rgb, 18, 4, 2, 2);
        rect(g, rgb, 4, 18, 2, 2);
        rect(g, rgb, 18, 18, 2, 2);
        rect(g, rgb, 2, 11, 2, 1);
        rect(g, rgb, 20, 11, 2, 1);
    }

    // ---- terrain -------------------------------------------------------------------------

    private static BufferedImage paintTerrain(Level level) {
        Theme theme = level.def.theme;
        int t = ART;
        int width = Level.COLS * t, height = Level.ROWS * t;
        BufferedImage small = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Random random = new Random(42 + level.def.seed);
        float[][] groundNoise = noise(random, width, height, 14);
        float[][] roadNoise = noise(random, width, height, 7);

        // Ground: posterised noise gives soft patches instead of a checkerboard.
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int c = x / t, r = y / t;
                int rgb;
                if (level.path[c][r]) {
                    rgb = theme.road[Math.min(theme.road.length - 1, (int) (roadNoise[x][y] * theme.road.length))];
                    int lx = x % t, ly = y % t;
                    // A dark kerb where road meets ground, with a lighter lip inside it.
                    int edge = Math.min(
                            Math.min(level.isPath(c - 1, r) ? t : lx, level.isPath(c + 1, r) ? t : t - 1 - lx),
                            Math.min(level.isPath(c, r - 1) ? t : ly, level.isPath(c, r + 1) ? t : t - 1 - ly));
                    if (edge <= 1) rgb = theme.kerb;
                    else if (edge == 2) rgb = theme.lip;
                } else {
                    rgb = theme.ground[Math.min(theme.ground.length - 1, (int) (groundNoise[x][y] * theme.ground.length))];
                }
                small.setRGB(x, y, rgb);
            }
        }

        Graphics2D g = small.createGraphics();
        for (int c = 0; c < Level.COLS; c++) {
            for (int r = 0; r < Level.ROWS; r++) {
                int x = c * t, y = r * t;
                if (level.path[c][r]) {
                    for (int i = 0; i < 3; i++) {
                        int px = x + 4 + random.nextInt(t - 9), py = y + 4 + random.nextInt(t - 9);
                        rect(g, theme.pebbleDark, px, py, 2, 1);
                        rect(g, theme.pebbleLight, px, py - 1, 1, 1);
                    }
                } else if (level.decor[c][r] == Level.CLEAR) {
                    for (int i = 0; i < 2; i++) {
                        // Tufts.
                        int px = x + 2 + random.nextInt(t - 6), py = y + 3 + random.nextInt(t - 6);
                        rect(g, theme.tuftDark, px, py, 1, 2);
                        rect(g, theme.tuftDark, px + 2, py, 1, 2);
                        rect(g, theme.tuftLight, px + 1, py - 1, 1, 2);
                    }
                    if (theme.specks.length > 0 && random.nextInt(7) == 0) {
                        int px = x + 3 + random.nextInt(t - 7), py = y + 3 + random.nextInt(t - 7);
                        int speck = theme.specks[random.nextInt(theme.specks.length)];
                        rect(g, speck, px - 1, py, 3, 1);
                        rect(g, speck, px, py - 1, 1, 3);
                    }
                }
            }
        }
        for (int r = 0; r < Level.ROWS; r++) {
            for (int c = 0; c < Level.COLS; c++) {
                int x = c * t, y = r * t;
                if (level.decor[c][r] == Level.TREE) {
                    g.setColor(new Color(0, 0, 0, 70));
                    g.fillOval(x + 2, y + 15, 21, 8);
                    tree(g, theme, x, y);
                } else if (level.decor[c][r] == Level.ROCK) {
                    g.setColor(new Color(0, 0, 0, 70));
                    g.fillOval(x + 2, y + 14, 21, 8);
                    oval(g, theme.rock[0], x + 3, y + 6, 18, 15);
                    oval(g, theme.rock[1], x + 4, y + 6, 15, 12);
                    oval(g, theme.rock[2], x + 5, y + 6, 10, 7);
                    rect(g, theme.rock[3], x + 8, y + 8, 3, 1);
                    rect(g, theme.rock[0], x + 13, y + 12, 1, 4);
                }
            }
        }

        // A dotted trail marks the straight line flyers take from the cave to the keep.
        g.setColor(theme.trail);
        float fx0 = level.flyX[0] / 2, fy0 = level.flyY[0] / 2, fx1 = level.flyX[1] / 2, fy1 = level.flyY[1] / 2;
        int dots = (int) (Math.hypot(fx1 - fx0, fy1 - fy0) / 8);
        for (int i = 2; i < dots - 3; i++) {
            float along = i / (float) dots;
            g.fillRect(Math.round(fx0 + (fx1 - fx0) * along), Math.round(fy0 + (fy1 - fy0) * along), 2, 1);
        }

        // The monster cave where the road enters, and the keep it leads to.
        int entryY = (int) (level.pathY[0] / Level.TILE) * t;
        oval(g, 0x2A1F3D, -12, entryY - 4, 26, 32);
        oval(g, 0x4B3470, -11, entryY - 2, 22, 28);
        oval(g, 0x0F0919, -10, entryY + 2, 18, 20);
        int exitY = (int) (level.pathY[level.pathY.length - 1] / Level.TILE) * t;
        int bx = (Level.COLS - 1) * t + 6;
        g.setColor(new Color(0, 0, 0, 80));
        g.fillOval(bx - 4, exitY + 24, 26, 10);
        rect(g, 0x3F4753, bx, exitY - 12, 18, 46);
        rect(g, 0x6B7280, bx + 2, exitY - 10, 16, 42);
        rect(g, 0x858D9B, bx + 2, exitY - 10, 3, 42);
        for (int i = 0; i < 3; i++) {
            rect(g, 0x3F4753, bx + i * 7, exitY - 17, 5, 6);
            rect(g, 0x6B7280, bx + 1 + i * 7, exitY - 16, 3, 4);
        }
        for (int i = 0; i < 4; i++) rect(g, 0x565E6B, bx + 5 + (i % 2) * 6, exitY - 6 + i * 9, 4, 2);
        oval(g, 0x14171D, bx - 5, exitY + 2, 16, 12);
        rect(g, 0x14171D, bx, exitY + 8, 11, 14);
        rect(g, 0x2A2F38, bx + 3, exitY + 6, 1, 16);
        rect(g, 0x2A2F38, bx + 7, exitY + 6, 1, 16);
        rect(g, 0x2B303A, bx + 8, exitY - 28, 1, 12);
        rect(g, 0xDC2626, bx + 9, exitY - 28, 8, 3);
        rect(g, 0xB91C1C, bx + 9, exitY - 25, 6, 3);
        g.dispose();
        return scale(small, 2);
    }

    /** One piece of tree-like scenery in the theme's style, within the tile at (x, y). */
    private static void tree(Graphics2D g, Theme theme, int x, int y) {
        int[] c = theme.canopy;
        switch (theme.tree) {
            case ROUND -> {
                rect(g, theme.trunk, x + 10, y + 12, 4, 9);
                oval(g, c[0], x + 1, y, 22, 17);
                oval(g, c[1], x + 2, y + 1, 19, 14);
                oval(g, c[2], x + 4, y + 1, 13, 10);
                oval(g, c[3], x + 6, y + 2, 7, 5);
            }
            case CACTUS -> {
                rect(g, c[0], x + 9, y + 4, 6, 17);
                rect(g, c[1], x + 10, y + 4, 4, 17);
                oval(g, c[1], x + 9, y + 2, 6, 5);
                rect(g, c[1], x + 5, y + 11, 5, 3);
                rect(g, c[1], x + 5, y + 7, 3, 6);
                rect(g, c[1], x + 14, y + 13, 5, 3);
                rect(g, c[1], x + 16, y + 9, 3, 6);
                rect(g, c[3], x + 11, y + 4, 1, 16);
                rect(g, c[3], x + 6, y + 8, 1, 4);
                rect(g, c[3], x + 17, y + 10, 1, 4);
            }
            case PINE -> {
                rect(g, theme.trunk, x + 11, y + 17, 2, 4);
                g.setColor(new Color(c[0]));
                g.fillPolygon(new int[] {x + 3, x + 12, x + 21}, new int[] {y + 19, y + 9, y + 19}, 3);
                g.setColor(new Color(c[1]));
                g.fillPolygon(new int[] {x + 5, x + 12, x + 19}, new int[] {y + 14, y + 5, y + 14}, 3);
                g.setColor(new Color(c[2]));
                g.fillPolygon(new int[] {x + 7, x + 12, x + 17}, new int[] {y + 9, y + 1, y + 9}, 3);
                rect(g, c[3], x + 11, y + 2, 2, 2);
                rect(g, c[3], x + 9, y + 7, 3, 1);
                rect(g, c[3], x + 13, y + 12, 4, 1);
                rect(g, c[3], x + 6, y + 17, 4, 1);
            }
            case DEAD -> {
                rect(g, c[0], x + 10, y + 7, 4, 14);
                rect(g, c[1], x + 11, y + 7, 2, 14);
                g.setColor(new Color(c[1]));
                g.drawLine(x + 11, y + 12, x + 6, y + 6);
                g.drawLine(x + 12, y + 10, x + 18, y + 4);
                g.drawLine(x + 12, y + 15, x + 17, y + 12);
                g.drawLine(x + 6, y + 6, x + 5, y + 3);
                g.drawLine(x + 18, y + 4, x + 20, y + 5);
            }
        }
    }

    /** Smooth value noise in 0..1, interpolated from a coarse random lattice. */
    private static float[][] noise(Random random, int width, int height, int cell) {
        int gw = width / cell + 2, gh = height / cell + 2;
        float[][] lattice = new float[gw][gh];
        for (float[] column : lattice) {
            for (int j = 0; j < gh; j++) column[j] = random.nextFloat();
        }
        float[][] out = new float[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int gx = x / cell, gy = y / cell;
                float fx = smooth((x % cell) / (float) cell), fy = smooth((y % cell) / (float) cell);
                float top = lattice[gx][gy] + (lattice[gx + 1][gy] - lattice[gx][gy]) * fx;
                float bottom = lattice[gx][gy + 1] + (lattice[gx + 1][gy + 1] - lattice[gx][gy + 1]) * fx;
                // A little per-pixel grain keeps the patch borders from looking like contours.
                out[x][y] = Math.clamp(top + (bottom - top) * fy + (random.nextFloat() - 0.5f) * 0.12f, 0f, 0.999f);
            }
        }
        return out;
    }

    private static float smooth(float t) {
        return t * t * (3 - 2 * t);
    }

    private static BufferedImage vignette() {
        BufferedImage image = new BufferedImage(Level.WIDTH, Level.HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        float radius = (float) Math.hypot(Level.WIDTH, Level.HEIGHT) / 2;
        g.setPaint(new RadialGradientPaint(Level.WIDTH / 2f, Level.HEIGHT / 2f, radius,
                new float[] {0f, 0.62f, 1f},
                new Color[] {new Color(0, 0, 0, 0), new Color(0, 0, 0, 0), new Color(0, 10, 20, 120)}));
        g.fillRect(0, 0, Level.WIDTH, Level.HEIGHT);
        g.dispose();
        return image;
    }

    // ---- helpers -------------------------------------------------------------------------

    private static void rect(Graphics2D g, int rgb, int x, int y, int w, int h) {
        g.setColor(new Color(rgb));
        g.fillRect(x, y, w, h);
    }

    private static void oval(Graphics2D g, int rgb, int x, int y, int w, int h) {
        g.setColor(new Color(rgb));
        g.fillOval(x, y, w, h);
    }

    private static void fillRound(Graphics2D g, int rgb, int x, int y, int w, int h, int arc) {
        g.setColor(new Color(rgb));
        g.fillRoundRect(x, y, w, h, arc, arc);
    }

    private static BufferedImage scale(BufferedImage source, float factor) {
        int w = Math.round(source.getWidth() * factor), h = Math.round(source.getHeight() * factor);
        BufferedImage scaled = new BufferedImage(w, h, source.getType());
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(source, 0, 0, w, h, null);
        g.dispose();
        return scaled;
    }

    private static BufferedImage mirror(BufferedImage source) {
        int w = source.getWidth(), h = source.getHeight();
        BufferedImage mirrored = new BufferedImage(w, h, source.getType());
        Graphics2D g = mirrored.createGraphics();
        g.drawImage(source, w, 0, -w, h, null);
        g.dispose();
        return mirrored;
    }
}
