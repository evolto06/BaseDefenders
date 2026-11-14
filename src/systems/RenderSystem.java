package systems;

import components.Effect;
import components.Enemy;
import components.Health;
import components.Lifetime;
import components.Position;
import components.Projectile;
import components.Sprite;
import components.Status;
import components.Tower;
import components.Zone;
import ecs.Query;
import ecs.Store;
import game.EnemyType;
import game.Game;
import game.Layout;
import game.Level;
import game.TowerType;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Draws the playing field: terrain, towers, monsters, shots, effects and build overlays.
 *
 * The field is pixel art, so it is drawn at its own size into an off-screen picture and that is
 * copied to the screen in one pass, whatever size the window is. Everything that never moves
 * (terrain, tower platforms) is kept in a ground layer that is only repainted when a tower is
 * built, merged or sold. Text and the build overlays are drawn straight to the screen, so they
 * stay sharp at any scale.
 */
public final class RenderSystem {
    private static final int TILE = Level.TILE;
    private static final int HALF = TILE / 2;
    private static final float FLY_HEIGHT = 14;
    private static final float ARC_HEIGHT = 30;
    private static final float SQUASH = 0.07f;
    /** Drawn animation frames shown per second: a Dragon beats its wings two and a half times. */
    private static final float FRAME_RATE = 5;
    private static final int BOLT_SEGMENTS = 5;

    private static final Color MAX_GLOW = new Color(251, 191, 36), MUZZLE = new Color(255, 244, 200);
    private static final float FAINT = 0.35f;
    private static final Color PLASMA_RIM = new Color(0x70397A), ICE_RIM = new Color(0x5A7081);
    private static final Color SUN_RIM = new Color(0x7B5A38), IRON_RIM = new Color(0x24292F);
    private static final Color TOWER_SHADOW = new Color(0, 0, 0, 60);
    private static final Color BAR_BACK = new Color(0x1B1F2A);
    private static final Color HP_HIGH = new Color(0x4ADE80), HP_MID = new Color(0xFACC15), HP_LOW = new Color(0xF87171);
    private static final Color CHILL = new Color(0x7DD3FC), BURN = new Color(0xFB923C), BRITTLE = new Color(0xC084FC);
    private static final Color POISON = new Color(0xA3E635), SHRED = new Color(0xFDE68A);
    private static final Color ROT = new Color(0xB45309), DOOM = new Color(0xF43F5E), HEX = new Color(0xD8B4FE);
    private static final Color HEX_SHOT = new Color(0xC084FC), HEX_DARK = new Color(0x581C87);
    private static final Color ROT_SHOT = new Color(0xCA8A04), ROT_DARK = new Color(0x422006);
    private static final Color SILVER_SHOT = new Color(0xE5E7EB);
    private static final Color RANGE_FILL = new Color(255, 255, 255, 28), RANGE_LINE = new Color(255, 255, 255, 150);
    private static final Color OK_FILL = new Color(74, 222, 128, 70), OK_LINE = new Color(0x4ADE80);
    private static final Color BAD_FILL = new Color(248, 113, 113, 90);
    private static final Color SLOT = new Color(255, 255, 255, 34);
    private static final Color LABEL_BACK = new Color(15, 18, 26, 220);
    private static final Color[] PORTAL_ARCS = {new Color(0x9E81D0), new Color(0x8369AE), new Color(0x66518A)};
    private static final Color HURT = new Color(220, 38, 38), FROST_GLAZE = new Color(186, 230, 253);
    private static final Color FLAK_SHELL = new Color(0x404040);
    private static final Color BOSS_BACK = new Color(0x3A1D1D);
    private static final Color PLASMA_SHOT = new Color(0xE879F9), ICE_SHOT = new Color(0xBAE6FD);
    private static final Color ACID_SHOT = new Color(0xA3E635), SUN_SHOT = new Color(0xFDBA74);
    private static final Color IRON_SHOT = new Color(0x4B5563), VENOM_DARK = new Color(0x3F6212);
    private static final Color[] PIPS = {
        new Color(0xE5E7EB), new Color(0xE0965A), new Color(0xE5EAF2), new Color(0xFBBF24)
    };
    private static final Stroke[] STROKES = new Stroke[8];
    private static final Font SMALL = new Font(Font.SANS_SERIF, Font.BOLD, 12);
    private static final Font TEXT = new Font(Font.SANS_SERIF, Font.BOLD, 14);
    private static final Font SUBTITLE = new Font(Font.SANS_SERIF, Font.BOLD, 18);

    static {
        for (int i = 0; i < STROKES.length; i++) {
            STROKES[i] = new BasicStroke(i + 1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        }
    }

    private final Game game;
    private final Query towerQuery, enemyQuery, projectileQuery, effectQuery, zoneQuery;
    private final Store<Tower> towers;
    private final Store<Position> positions;
    private final Store<Enemy> enemies;
    private final Store<Sprite> sprites;
    private final Store<Health> healths;
    private final Store<Status> statuses;
    private final Store<Projectile> projectiles;
    private final Store<Effect> effects;
    private final Store<Lifetime> lifetimes;
    private final Store<Zone> zones;
    private final Stamps stamps = new Stamps();
    /** The frame being built, at the field's own size. */
    private final BufferedImage field = new BufferedImage(Level.WIDTH, Level.HEIGHT, BufferedImage.TYPE_INT_RGB);
    /** Blends beams, rings and sparks into {@link #field} without the garbage Java2D would make. */
    private final Pixels pixels = new Pixels(field);
    /** Terrain plus every tower's platform; see {@link #ground()}. */
    private final BufferedImage ground = new BufferedImage(Level.WIDTH, Level.HEIGHT, BufferedImage.TYPE_INT_RGB);
    private final BufferedImage[] rangeStamps = new BufferedImage[4];
    private int rangeNext;
    private BufferedImage groundTerrain;
    private long groundTowers;

    public RenderSystem(Game game) {
        this.game = game;
        towerQuery = game.world.query(Tower.class, Position.class);
        enemyQuery = game.world.query(Enemy.class, Position.class, Sprite.class, Health.class);
        projectileQuery = game.world.query(Projectile.class, Position.class);
        effectQuery = game.world.query(Effect.class, Position.class, Lifetime.class);
        zoneQuery = game.world.query(Zone.class, Position.class, Lifetime.class);
        towers = game.world.store(Tower.class);
        positions = game.world.store(Position.class);
        enemies = game.world.store(Enemy.class);
        sprites = game.world.store(Sprite.class);
        healths = game.world.store(Health.class);
        statuses = game.world.store(Status.class);
        projectiles = game.world.store(Projectile.class);
        effects = game.world.store(Effect.class);
        lifetimes = game.world.store(Lifetime.class);
        zones = game.world.store(Zone.class);
    }

    public void render(Graphics2D g) {
        towerQuery.fetch();
        enemyQuery.fetch();
        effectQuery.fetch();

        Graphics2D f = field.createGraphics();
        f.drawImage(ground(), 0, 0, null);
        drawPortal(f);
        drawZones(f);
        drawTowers(f);
        drawEnemies(f, false);
        drawProjectiles(f);
        drawEnemies(f, true);
        drawEffects(f);
        f.dispose();
        // A monster got through.
        if (game.hurtFlash > 0) pixels.rect(0, 0, Level.WIDTH, Level.HEIGHT, HURT, Math.min(1, game.hurtFlash) * 0.35f);

        AffineTransform savedTransform = g.getTransform();
        Shape savedClip = g.getClip();
        g.translate(0, Layout.TOP);
        g.clipRect(0, 0, Level.WIDTH, Level.HEIGHT);
        Ui.layer(g, field, 0, 0);
        drawTexts(g);
        drawBuildOverlay(g);
        drawBanners(g);
        g.setTransform(savedTransform);
        g.setClip(savedClip);
    }

    /**
     * The terrain with every tower's shadow, platform and tier pips painted on, and the build
     * slots while a tower is picked. It is repainted only when one of those changes, so a board
     * full of towers costs one copy per frame.
     */
    private BufferedImage ground() {
        boolean slots = game.placing != null && !game.gameOver;
        long built = slots ? 19 : 17;
        for (int i = 0; i < towerQuery.size(); i++) {
            int e = towerQuery.get(i);
            Tower t = towers.get(e);
            built = (built * 31 + e) * 31 + t.type.ordinal() * 8 + t.tier;
        }
        if (groundTerrain == game.background && built == groundTowers) return ground;
        groundTerrain = game.background;
        groundTowers = built;
        Graphics2D g = ground.createGraphics();
        g.drawImage(game.background, 0, 0, null);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        for (int i = 0; i < towerQuery.size(); i++) {
            Tower t = towers.get(towerQuery.get(i));
            int x = t.col * TILE, y = t.row * TILE;
            g.setColor(TOWER_SHADOW);
            g.fillRoundRect(x + 5, y + 8, TILE - 6, TILE - 8, 12, 12);
            g.drawImage(game.assets.base(t.type, t.tier), x, y, null);
            int pipsLeft = x + HALF - t.tier * 4;
            for (int pip = 0; pip < t.tier; pip++) {
                g.setColor(BAR_BACK);
                g.fillRect(pipsLeft + pip * 8, y + TILE - 9, 7, 6);
                g.setColor(PIPS[t.tier - 1]);
                g.fillRect(pipsLeft + pip * 8 + 1, y + TILE - 8, 5, 4);
            }
        }
        if (slots) {
            // While a tower is picked from the hand, every free tile it could go on is marked.
            g.setColor(SLOT);
            g.setStroke(STROKES[0]);
            for (int c = 0; c < Level.COLS; c++) {
                for (int r = 0; r < Level.ROWS; r++) {
                    if (game.level.buildable(c, r) && !game.world.alive(game.towerAt[c][r])) {
                        g.drawRoundRect(c * TILE + 4, r * TILE + 4, TILE - 9, TILE - 9, 8, 8);
                    }
                }
            }
        }
        g.dispose();
        return ground;
    }

    // ---- world ---------------------------------------------------------------------------

    /** Swirling arcs over the cave mouth the monsters pour out of, fainter the further out. */
    private void drawPortal(Graphics2D g) {
        int cx = 2, cy = Math.round(game.level.pathY[0]);
        g.setStroke(STROKES[1]);
        for (int k = 0; k < PORTAL_ARCS.length; k++) {
            int r = 9 + k * 6;
            int start = (int) (game.uiTime * (140 - k * 35) + k * 120) % 360;
            g.setColor(PORTAL_ARCS[k]);
            g.drawArc(cx - r, cy - r, r * 2, r * 2, start, 120);
        }
    }

    private void drawZones(Graphics2D g) {
        zoneQuery.fetch();
        Composite saved = g.getComposite();
        for (int i = 0; i < zoneQuery.size(); i++) {
            int e = zoneQuery.get(i);
            Zone zone = zones.get(e);
            Position p = positions.get(e);
            float fade = Math.min(1, lifetimes.get(e).remaining / 0.6f);
            int r = Math.round(zone.radius);
            int x = Math.round(p.x), y = Math.round(p.y);
            g.setColor(zone.color);
            g.setComposite(Stamps.alpha(0.26f * fade));
            if (Stamps.stamped(r * 2)) g.drawImage(stamps.disc(zone.color, r * 2), x - r, y - r, null);
            else g.fillOval(x - r, y - r, r * 2, r * 2);
            g.setComposite(saved);
            pixels.ring(x, y, r, 2, zone.color, 0.7f * fade);
            // Drifting flecks inside the field.
            for (int k = 0; k < 7; k++) {
                double angle = k * 0.9 + game.uiTime * (0.4 + k * 0.07);
                double dist = r * (0.25 + 0.1 * k);
                pixels.rect(x + (int) (Math.cos(angle) * dist), y + (int) (Math.sin(angle) * dist), 2, 2,
                        Color.WHITE, 0.7f * fade);
            }
        }
        g.setComposite(saved);
    }

    /** The parts of each tower that move or glow; its platform is already in the ground layer. */
    private void drawTowers(Graphics2D g) {
        for (int i = 0; i < towerQuery.size(); i++) {
            Tower t = towers.get(towerQuery.get(i));
            int x = t.col * TILE, y = t.row * TILE;
            if (t.tier == TowerType.MAX_TIER) {
                // Max-tier towers hum with a golden glow.
                float hum = (50 + 35 * (float) Math.sin(game.uiTime * 3 + t.col)) / 255;
                g.drawImage(stamps.glow(MAX_GLOW, TILE - 14, hum), x + 7, y + 7, null);
            }
            BufferedImage turret = game.assets.turret(t.type);
            if (t.type.rotates) {
                float cos = (float) Math.cos(t.angle), sin = (float) Math.sin(t.angle);
                BufferedImage turned = stamps.turned(turret, t.angle);
                int kick = Math.round(t.recoil * 4), half = turned.getWidth() / 2;
                g.drawImage(turned, x + HALF - half - Math.round(cos * kick), y + HALF - half - Math.round(sin * kick), null);
                if (t.recoil > 0.55f) {
                    // Muzzle flash at the end of the barrel.
                    int mx = x + HALF + Math.round(cos * (HALF - 1)), my = y + HALF + Math.round(sin * (HALF - 1));
                    g.drawImage(stamps.glow(MUZZLE, 10, t.recoil * 200 / 255), mx - 5, my - 5, null);
                }
            } else {
                // A standing tower glows from behind as it fires, with a spark on its emitter.
                if (t.recoil > 0) {
                    g.drawImage(stamps.glow(t.type.colorB, TILE - 6, t.recoil * 120 / 255), x + 3, y + 3, null);
                }
                g.drawImage(turret, x, y, null);
                if (t.recoil > 0) {
                    int tip = y + HALF - Math.round(t.type.emitterLift());
                    g.drawImage(stamps.glow(Color.WHITE, 8, t.recoil * 170 / 255), x + HALF - 4, tip - 4, null);
                }
            }
        }
    }

    private void drawEnemies(Graphics2D g, boolean flying) {
        g.setStroke(STROKES[1]);
        Composite saved = g.getComposite();
        for (int i = 0; i < enemyQuery.size(); i++) {
            int e = enemyQuery.get(i);
            Enemy enemy = enemies.get(e);
            if (enemy.type.flying != flying) continue;
            Position p = positions.get(e);
            Sprite sprite = sprites.get(e);
            Health h = healths.get(e);
            Status s = statuses.get(e);
            int side = sprite.flip ? 1 : 0;
            // A monster with drawn frames steps through them; the rest are animated by squashing.
            int frames = game.assets.enemyFrames(enemy.type);
            int frame = frames > 1 ? (int) (enemy.animTime * FRAME_RATE) % frames : 0;
            BufferedImage image = game.assets.enemy(enemy.type, frame)[side];
            int w = image.getWidth(), ht = image.getHeight();
            int r = Math.round(enemy.type.radius);
            int cx = Math.round(p.x), cy = Math.round(p.y);

            // Walkers squash and stretch as they step; flyers flap and bob in the air.
            float scaleX, scaleY, lift;
            if (flying) {
                scaleX = frames > 1 ? 1 : 0.76f + 0.24f * Math.abs((float) Math.sin(enemy.animTime * 11));
                scaleY = 1;
                lift = FLY_HEIGHT + (float) Math.sin(enemy.animTime * 4) * 3;
            } else {
                float step = (float) Math.sin(enemy.animTime * enemy.type.speed * 0.16f);
                scaleX = 1 - SQUASH * step;
                scaleY = 1 + SQUASH * step;
                lift = Math.max(0, step) * 2;
            }
            int dw = Math.round(w * scaleX), dh = Math.round(ht * scaleY);
            int left = cx - dw / 2, top = cy + ht / 2 - Math.round(lift) - dh;

            g.drawImage(stamps.shadow(r), cx - r, cy + ht / 2 - r / 2 - 2, null);
            stretched(g, image, left, top, dw, dh);
            if (enemy.flash > 0) {
                g.setComposite(Stamps.alpha(Math.min(1, enemy.flash) * 0.75f));
                stretched(g, game.assets.enemySilhouette(enemy.type, frame)[side], left, top, dw, dh);
                g.setComposite(saved);
            }

            if (s != null) {
                if (s.stunTime > 0) {
                    // Frozen solid: glazed over with ice.
                    int glaze = Math.min(Math.max(dw, dh) + 2, 62);
                    g.drawImage(stamps.glow(FROST_GLAZE, glaze, 0.5f), cx - glaze / 2, top + dh / 2 - glaze / 2, null);
                }
                int ring = r + 3, ringY = top + dh / 2;
                if (s.slowTime > 0) ring = statusRing(CHILL, cx, ringY, ring);
                if (s.burnTime > 0) ring = statusRing(BURN, cx, ringY, ring);
                if (s.poisonTime > 0) ring = statusRing(POISON, cx, ringY, ring);
                if (s.brittleTime > 0) ring = statusRing(BRITTLE, cx, ringY, ring);
                if (s.shredTime > 0) ring = statusRing(SHRED, cx, ringY, ring);
                if (s.rotTime > 0) ring = statusRing(ROT, cx, ringY, ring);
                if (s.doomTime > 0) statusRing(DOOM, cx, ringY, ring);
                // A violet cross: its powers are switched off.
                if (s.hexTime > 0) g.drawImage(stamps.hexMark(HEX, BAR_BACK), cx - 4, top - 18, null);
            }

            if (h.hp < h.max) {
                int barWidth = Math.max(22, r * 2 + 6);
                float fraction = Math.max(0, h.hp / h.max);
                g.setColor(BAR_BACK);
                g.fillRect(cx - barWidth / 2 - 1, top - 8, barWidth + 2, 5);
                g.setColor(fraction > 0.6f ? HP_HIGH : fraction > 0.3f ? HP_MID : HP_LOW);
                g.fillRect(cx - barWidth / 2, top - 7, Math.round(barWidth * fraction), 3);
            }
        }
    }

    private int statusRing(Color color, int cx, int cy, int radius) {
        pixels.ring(cx, cy, radius, 2, color, 1);
        return radius + 2;
    }

    /** Draws a sprite at a size a little off its own, from a kept copy at that size where there is one. */
    private void stretched(Graphics2D g, BufferedImage sprite, int x, int y, int w, int h) {
        BufferedImage sized = stamps.sized(sprite, w, h);
        if (sized != null) g.drawImage(sized, x, y, null);
        else g.drawImage(sprite, x, y, w, h, null);
    }

    private void drawProjectiles(Graphics2D g) {
        projectileQuery.fetch();
        for (int i = 0; i < projectileQuery.size(); i++) {
            int e = projectileQuery.get(i);
            Projectile pr = projectiles.get(e);
            Position p = positions.get(e);
            int x = Math.round(p.x), y = Math.round(p.y);
            if (pr.lobbed) {
                int height = Math.round((float) Math.sin(Math.PI * pr.progress) * ARC_HEIGHT);
                g.drawImage(stamps.shadow(4), x - 4, y - 2, null);
                g.drawImage(stamps.disc(shellRim(pr.source), 10), x - 5, y - height - 5, null);
                g.drawImage(stamps.disc(shellColor(pr.source), 7), x - 4, y - height - 4, null);
                pixels.rect(x - 2, y - height - 3, 2, 2, Color.WHITE, 1);
                continue;
            }
            float dx = (float) Math.cos(pr.angle), dy = (float) Math.sin(pr.angle);
            switch (pr.source) {
                case ICE_LANCE, SUNBOW, HUNTER -> {
                    Color color = pr.source == TowerType.SUNBOW ? SUN_SHOT
                            : pr.source == TowerType.HUNTER ? SILVER_SHOT : ICE_SHOT;
                    pixels.line(x - dx * 30, y - dy * 30, x, y, 5, color, FAINT);
                    pixels.line(x - dx * 18, y - dy * 18, x, y, 3, color, 1);
                    pixels.line(x - dx * 14, y - dy * 14, x, y, 1, Color.WHITE, 1);
                }
                case FLAK -> {
                    g.drawImage(stamps.disc(FLAK_SHELL, 7), x - 3, y - 3, null);
                    g.drawImage(stamps.disc(TowerType.TESLA.color, 4), x - 2, y - 2, null);
                }
                case VENOM, BLIGHT, ROT, HEX -> {
                    Color glob = pr.source == TowerType.HEX ? HEX_SHOT : pr.source == TowerType.ROT ? ROT_SHOT : ACID_SHOT;
                    Color rim = pr.source == TowerType.HEX ? HEX_DARK : pr.source == TowerType.ROT ? ROT_DARK : VENOM_DARK;
                    g.drawImage(stamps.glow(glob, 6, FAINT), x - Math.round(dx * 9) - 3, y - Math.round(dy * 9) - 3, null);
                    g.drawImage(stamps.glow(glob, 4, FAINT), x - Math.round(dx * 16) - 2, y - Math.round(dy * 16) - 2, null);
                    g.drawImage(stamps.disc(rim, 9), x - 4, y - 4, null);
                    g.drawImage(stamps.disc(glob, 6), x - 3, y - 3, null);
                }
                default -> {
                    pixels.line(x - dx * 24, y - dy * 24, x, y, 2, pr.source.color, FAINT);
                    pixels.line(x - dx * 11, y - dy * 11, x, y, 2, pr.source.color, 1);
                    pixels.rect(x - 1, y - 1, 3, 3, Color.WHITE, 1);
                }
            }
        }
    }

    /** The dark edge of a lobbed shell. */
    private static Color shellRim(TowerType type) {
        return switch (type) {
            case PLASMA -> PLASMA_RIM;
            case BLIZZARD -> ICE_RIM;
            case ACID -> VENOM_DARK;
            case CLUSTER -> SUN_RIM;
            case DOOM -> HEX_DARK;
            default -> IRON_RIM;
        };
    }

    private static Color shellColor(TowerType type) {
        return switch (type) {
            case PLASMA -> PLASMA_SHOT;
            case BLIZZARD -> ICE_SHOT;
            case ACID -> ACID_SHOT;
            case CLUSTER -> SUN_SHOT;
            case DOOM -> HEX_SHOT;
            default -> IRON_SHOT;
        };
    }

    /** Every effect but floating text, which {@link #drawTexts} puts on the screen itself. */
    private void drawEffects(Graphics2D g) {
        Composite saved = g.getComposite();
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < effectQuery.size(); i++) {
            int e = effectQuery.get(i);
            Effect fx = effects.get(e);
            if (fx.kind == Effect.Kind.TEXT) continue;
            Position p = positions.get(e);
            Lifetime life = lifetimes.get(e);
            float left = Math.clamp(life.remaining / life.total, 0f, 1f);
            int x = Math.round(p.x), y = Math.round(p.y);
            switch (fx.kind) {
                case BEAM -> {
                    // A wide faint glow, the beam itself, and a white-hot core ending in a dot.
                    int x2 = Math.round(fx.x2), y2 = Math.round(fx.y2);
                    int width = Math.clamp(Math.round(fx.width), 1, 5);
                    pixels.line(x, y, x2, y2, width + 3, fx.color, left * 0.3f);
                    pixels.line(x, y, x2, y2, width, fx.color, left);
                    pixels.line(x, y, x2, y2, 1, Color.WHITE, left);
                    pixels.rect(x2 - 2, y2 - 2, 5, 5, Color.WHITE, left);
                }
                case BOLT -> {
                    // A jagged path, redrawn at random each frame so it crackles.
                    float nx = -(fx.y2 - p.y), ny = fx.x2 - p.x;
                    float length = Math.max(1, (float) Math.sqrt(nx * nx + ny * ny));
                    float fromX = p.x, fromY = p.y;
                    for (int k = 1; k <= BOLT_SEGMENTS; k++) {
                        float t = k / (float) BOLT_SEGMENTS;
                        float jitter = k == BOLT_SEGMENTS ? 0 : random.nextFloat(-7, 7);
                        float toX = p.x + (fx.x2 - p.x) * t + nx / length * jitter;
                        float toY = p.y + (fx.y2 - p.y) * t + ny / length * jitter;
                        pixels.line(fromX, fromY, toX, toY, 6, fx.color, left * 0.3f);
                        pixels.line(fromX, fromY, toX, toY, 2, fx.color, left);
                        pixels.line(fromX, fromY, toX, toY, 1, Color.WHITE, left);
                        fromX = toX;
                        fromY = toY;
                    }
                }
                case RING -> pixels.ring(x, y, Math.round(fx.radius * (0.35f + 0.65f * (1 - left))), 3, fx.color, left);
                case FLASH -> {
                    int r = Math.round(fx.radius * (0.5f + 0.5f * (1 - left)));
                    g.setComposite(Stamps.alpha(left * 0.55f));
                    if (Stamps.stamped(r * 2)) {
                        g.drawImage(stamps.flash(fx.color, r * 2), x - r, y - r, null);
                    } else {
                        g.setColor(fx.color);
                        g.fillOval(x - r, y - r, r * 2, r * 2);
                        g.setColor(Color.WHITE);
                        g.fillOval(x - r / 2, y - r / 2, r, r);
                    }
                    g.setComposite(saved);
                }
                case SPARK -> {
                    int size = Math.round(fx.radius);
                    pixels.rect(x - size / 2, y - size / 2, size, size, fx.color, left);
                }
                default -> { }
            }
        }
    }

    /** Floating numbers and shouts, drawn on the screen itself so the lettering stays sharp. */
    private void drawTexts(Graphics2D g) {
        Composite saved = g.getComposite();
        g.setFont(TEXT);
        for (int i = 0; i < effectQuery.size(); i++) {
            int e = effectQuery.get(i);
            Effect fx = effects.get(e);
            if (fx.kind != Effect.Kind.TEXT) continue;
            Position p = positions.get(e);
            Lifetime life = lifetimes.get(e);
            int x = Math.round(p.x), y = Math.round(p.y);
            int width = g.getFontMetrics().stringWidth(fx.text);
            int tx = Math.clamp(x - width / 2, 4, Math.max(4, Level.WIDTH - width - 4));
            g.setComposite(Stamps.alpha(Math.clamp(life.remaining / life.total, 0f, 1f)));
            g.setColor(Color.BLACK);
            g.drawString(fx.text, tx + 1, y + 1);
            g.setColor(fx.color);
            g.drawString(fx.text, tx, y);
        }
        g.setComposite(saved);
    }

    // ---- build interface ---------------------------------------------------------------

    private void drawBuildOverlay(Graphics2D g) {
        if (game.gameOver) return;
        Tower selected = towers.get(game.selected);
        int col = game.hoverCol, row = game.hoverRow;
        boolean dragging = selected != null && game.dragCol >= 0 && (col != game.dragCol || row != game.dragRow);
        if (selected != null) {
            drawRange(g, selected.col, selected.row, selected.type.range(selected.tier));
            drawMergePartners(g, selected, dragging);
            g.setColor(Color.WHITE);
            g.setStroke(STROKES[1]);
            g.drawRoundRect(selected.col * TILE + 1, selected.row * TILE + 1, TILE - 3, TILE - 3, 8, 8);
        }
        if (!dragging && game.placing == null && col >= 0) {
            // Hovering any other tower previews its reach.
            Tower hovered = towers.get(game.towerAt[col][row]);
            if (hovered != null && hovered != selected) {
                drawRange(g, hovered.col, hovered.row, hovered.type.range(hovered.tier));
                label(g, hovered.type.label + "  T" + hovered.tier, col * TILE + HALF, row * TILE - 8);
            }
        }
        if (dragging) {
            drawDrag(g, selected);
        } else if (game.placing != null && col >= 0) {
            drawPlacement(g, game.placing, col, row);
        }
    }

    /** Outlines every tower the selected one could merge with; brighter while dragging. */
    private void drawMergePartners(Graphics2D g, Tower source, boolean dragging) {
        float pulse = 0.5f + 0.5f * (float) Math.sin(game.uiTime * 8);
        Color fill = new Color(74, 222, 128, dragging ? 40 + Math.round(pulse * 60) : 0);
        Color line = new Color(74, 222, 128, dragging ? 255 : 110 + Math.round(pulse * 90));
        for (int i = 0; i < towerQuery.size(); i++) {
            Tower t = towers.get(towerQuery.get(i));
            if (t == source || TowerType.merge(source.type, source.tier, t.type, t.tier) == null) continue;
            tile(g, t.col * TILE, t.row * TILE, fill, line);
        }
    }

    private void drawPlacement(Graphics2D g, TowerType type, int col, int row) {
        int x = col * TILE, y = row * TILE;
        Tower existing = towers.get(game.towerAt[col][row]);
        if (existing != null) {
            TowerType result = TowerType.merge(type, 1, existing.type, existing.tier);
            if (result == null) return;
            int tier = TowerType.mergedTier(type, 1, existing.type);
            drawRange(g, col, row, result.range(tier));
            tile(g, x, y, OK_FILL, OK_LINE);
            label(g, mergeLabel(result, tier), x + HALF, y - 8);
            return;
        }
        if (!game.level.buildable(col, row)) {
            tile(g, x, y, BAD_FILL, null);
            return;
        }
        drawRange(g, col, row, type.range(1));
        Composite saved = g.getComposite();
        g.setComposite(AlphaComposite.SrcOver.derive(0.65f));
        g.drawImage(game.assets.icon(type, 1), x, y, null);
        g.setComposite(saved);
        if (game.mana < type.cost) tile(g, x, y, BAD_FILL, null);
    }

    private void drawDrag(Graphics2D g, Tower source) {
        int mx = game.mouseX, my = game.mouseY - Layout.TOP;
        g.setColor(RANGE_LINE);
        g.setStroke(STROKES[1]);
        g.drawLine(source.col * TILE + HALF, source.row * TILE + HALF, mx, my);
        Composite saved = g.getComposite();
        g.setComposite(AlphaComposite.SrcOver.derive(0.75f));
        g.drawImage(game.assets.icon(source.type, source.tier), mx - HALF, my - HALF, null);
        g.setComposite(saved);

        if (game.hoverCol < 0) return;
        Tower target = towers.get(game.towerAt[game.hoverCol][game.hoverRow]);
        if (target == null) return;
        TowerType result = TowerType.merge(source.type, source.tier, target.type, target.tier);
        String text = result != null ? mergeLabel(result, TowerType.mergedTier(source.type, source.tier, target.type))
                : source.tier != target.tier ? "Needs the same tier"
                : source.type == target.type ? "Already max tier"
                : "Hybrids only merge with their twin";
        label(g, text, target.col * TILE + HALF, target.row * TILE - 8);
    }

    private static String mergeLabel(TowerType result, int tier) {
        return "= " + result.label + "  T" + tier;
    }

    private void drawRange(Graphics2D g, int col, int row, float range) {
        int r = Math.round(range), cx = col * TILE + HALF, cy = row * TILE + HALF;
        Ui.layer(g, rangeStamp(r), cx - r - 1, cy - r - 1);
    }

    /** A picture of a range circle of the given radius; the last few sizes asked for are kept. */
    private BufferedImage rangeStamp(int r) {
        int size = r * 2 + 2;
        for (BufferedImage stamp : rangeStamps) {
            if (stamp != null && stamp.getWidth() == size) return stamp;
        }
        BufferedImage stamp = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = stamp.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(RANGE_FILL);
        g.fillOval(1, 1, r * 2, r * 2);
        g.setColor(RANGE_LINE);
        g.setStroke(STROKES[0]);
        g.drawOval(1, 1, r * 2, r * 2);
        g.dispose();
        rangeStamps[rangeNext++ % rangeStamps.length] = stamp;
        return stamp;
    }

    private static void tile(Graphics2D g, int x, int y, Color fill, Color line) {
        g.setColor(fill);
        g.fillRoundRect(x + 1, y + 1, TILE - 2, TILE - 2, 8, 8);
        if (line == null) return;
        g.setColor(line);
        g.setStroke(STROKES[1]);
        g.drawRoundRect(x + 1, y + 1, TILE - 3, TILE - 3, 8, 8);
    }

    private static void label(Graphics2D g, String text, int centerX, int baseline) {
        g.setFont(SMALL);
        FontMetrics metrics = g.getFontMetrics();
        int width = metrics.stringWidth(text) + 12;
        int x = Math.clamp(centerX - width / 2, 2, Level.WIDTH - width - 2);
        int y = Math.max(20, baseline);
        g.setColor(LABEL_BACK);
        g.fillRoundRect(x, y - 15, width, 20, 8, 8);
        g.setColor(Color.WHITE);
        g.drawString(text, x + 6, y);
    }

    private void drawBanners(Graphics2D g) {
        boolean boss = !game.gameOver && drawBossBar(g);
        if (game.toastTime > 0 && !game.gameOver) {
            int top = boss ? 40 : 14;
            Composite saved = g.getComposite();
            g.setComposite(Stamps.alpha(Math.min(1, game.toastTime * 2)));
            g.setFont(SUBTITLE);
            int width = g.getFontMetrics().stringWidth(game.toast) + 28;
            g.setColor(LABEL_BACK);
            g.fillRoundRect((Level.WIDTH - width) / 2, top, width, 34, 12, 12);
            g.setColor(Color.WHITE);
            g.drawString(game.toast, (Level.WIDTH - width) / 2 + 14, top + 23);
            g.setComposite(saved);
        }
    }

    /** One shared health bar along the top for every boss on the field. Returns whether one is alive. */
    private boolean drawBossBar(Graphics2D g) {
        float hp = 0, max = 0;
        int count = 0;
        EnemyType kind = null;
        boolean mixed = false;
        for (int i = 0; i < enemyQuery.size(); i++) {
            int e = enemyQuery.get(i);
            EnemyType type = enemies.get(e).type;
            if (!type.boss()) continue;
            Health h = healths.get(e);
            hp += Math.max(0, h.hp);
            max += h.max;
            count++;
            mixed |= kind != null && kind != type;
            kind = type;
        }
        if (count == 0) return false;
        int width = 360, x = (Level.WIDTH - width) / 2, y = 12;
        g.setColor(LABEL_BACK);
        g.fillRoundRect(x - 6, y - 5, width + 12, 26, 12, 12);
        g.setColor(BOSS_BACK);
        g.fillRoundRect(x, y, width, 16, 8, 8);
        g.setColor(HP_LOW);
        g.fillRoundRect(x, y, Math.max(8, Math.round(width * hp / max)), 16, 8, 8);
        g.setFont(SMALL);
        String name = mixed ? "BOSSES" : kind.label.toUpperCase();
        String text = count > 1 ? name + " x" + count : name;
        g.setColor(Color.WHITE);
        g.drawString(text, x + (width - g.getFontMetrics().stringWidth(text)) / 2, y + 13);
        return true;
    }
}
