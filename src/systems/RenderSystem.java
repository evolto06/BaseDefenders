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
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.concurrent.ThreadLocalRandom;

/** Draws the playing field: terrain, towers, monsters, shots, effects and build overlays. */
public final class RenderSystem {
    private static final int TILE = Level.TILE;
    private static final int HALF = TILE / 2;
    private static final float FLY_HEIGHT = 14;
    private static final float ARC_HEIGHT = 30;
    private static final float SQUASH = 0.07f;

    private static final Color SHADOW = new Color(0, 0, 0, 70);
    private static final Color TOWER_SHADOW = new Color(0, 0, 0, 60);
    private static final Color BAR_BACK = new Color(0x1B1F2A);
    private static final Color HP_HIGH = new Color(0x4ADE80), HP_MID = new Color(0xFACC15), HP_LOW = new Color(0xF87171);
    private static final Color CHILL = new Color(0x7DD3FC), BURN = new Color(0xFB923C), BRITTLE = new Color(0xC084FC);
    private static final Color POISON = new Color(0xA3E635), SHRED = new Color(0xFDE68A);
    private static final Color FROZEN = new Color(186, 230, 253, 130);
    private static final Color RANGE_FILL = new Color(255, 255, 255, 28), RANGE_LINE = new Color(255, 255, 255, 150);
    private static final Color OK_FILL = new Color(74, 222, 128, 70), OK_LINE = new Color(0x4ADE80);
    private static final Color BAD_FILL = new Color(248, 113, 113, 90);
    private static final Color SLOT = new Color(255, 255, 255, 34);
    private static final Color LABEL_BACK = new Color(15, 18, 26, 220);
    private static final Color PORTAL = new Color(0xC4A1FF);
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
        AffineTransform savedTransform = g.getTransform();
        Shape savedClip = g.getClip();
        g.translate(0, Layout.TOP);
        g.clipRect(0, 0, Level.WIDTH, Level.HEIGHT);

        g.drawImage(game.background, 0, 0, null);
        drawPortal(g);
        towerQuery.fetch();
        drawBuildSlots(g);
        drawZones(g);
        drawTowers(g);
        enemyQuery.fetch();
        drawEnemies(g, false);
        drawProjectiles(g);
        drawEnemies(g, true);
        drawEffects(g);
        g.drawImage(game.assets.vignette, 0, 0, null);
        drawBuildOverlay(g);
        drawBanners(g);

        g.setTransform(savedTransform);
        g.setClip(savedClip);
    }

    // ---- world ---------------------------------------------------------------------------

    /** Swirling arcs over the cave mouth the monsters pour out of. */
    private void drawPortal(Graphics2D g) {
        int cx = 2, cy = Math.round(game.level.pathY[0]);
        Composite saved = g.getComposite();
        g.setColor(PORTAL);
        g.setStroke(STROKES[1]);
        for (int k = 0; k < 3; k++) {
            int r = 9 + k * 6;
            int start = (int) (game.uiTime * (140 - k * 35) + k * 120) % 360;
            g.setComposite(AlphaComposite.SrcOver.derive(0.75f - k * 0.2f));
            g.drawArc(cx - r, cy - r, r * 2, r * 2, start, 120);
        }
        g.setComposite(saved);
    }

    /** While a tower is picked in the shop, marks every free tile it could be built on. */
    private void drawBuildSlots(Graphics2D g) {
        if (game.placing == null || game.gameOver) return;
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
            g.setComposite(AlphaComposite.SrcOver.derive(0.26f * fade));
            g.fillOval(x - r, y - r, r * 2, r * 2);
            g.setComposite(AlphaComposite.SrcOver.derive(0.7f * fade));
            g.setStroke(STROKES[1]);
            g.drawOval(x - r, y - r, r * 2, r * 2);
            // Drifting flecks inside the field.
            g.setColor(Color.WHITE);
            for (int k = 0; k < 7; k++) {
                double angle = k * 0.9 + game.uiTime * (0.4 + k * 0.07);
                double dist = r * (0.25 + 0.1 * k);
                g.fillRect(x + (int) (Math.cos(angle) * dist), y + (int) (Math.sin(angle) * dist), 2, 2);
            }
        }
        g.setComposite(saved);
    }

    private void drawTowers(Graphics2D g) {
        for (int i = 0; i < towerQuery.size(); i++) {
            Tower t = towers.get(towerQuery.get(i));
            int x = t.col * TILE, y = t.row * TILE;
            g.setColor(TOWER_SHADOW);
            g.fillRoundRect(x + 5, y + 8, TILE - 6, TILE - 8, 12, 12);
            g.drawImage(game.assets.base(t.type, t.tier), x, y, null);
            if (t.tier == TowerType.MAX_TIER) {
                // Max-tier towers hum with a golden glow.
                int alpha = 50 + (int) (35 * Math.sin(game.uiTime * 3 + t.col));
                g.setColor(new Color(251, 191, 36, alpha));
                g.fillOval(x + 7, y + 7, TILE - 14, TILE - 14);
            }
            BufferedImage turret = game.assets.turret(t.type);
            if (t.type.rotates) {
                AffineTransform saved = g.getTransform();
                g.translate(x + HALF, y + HALF);
                g.rotate(t.angle);
                g.drawImage(turret, -HALF - Math.round(t.recoil * 4), -HALF, null);
                if (t.recoil > 0.55f) {
                    g.setColor(new Color(255, 244, 200, Math.round(t.recoil * 200)));
                    g.fillOval(HALF - 6, -5, 10, 10);
                }
                g.setTransform(saved);
            } else {
                g.drawImage(turret, x, y, null);
                if (t.recoil > 0) {
                    Color c = t.type.colorB;
                    g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.round(t.recoil * 140)));
                    g.fillOval(x + 6, y + 6, TILE - 12, TILE - 12);
                }
            }
            int pipsLeft = x + HALF - t.tier * 4;
            for (int pip = 0; pip < t.tier; pip++) {
                g.setColor(BAR_BACK);
                g.fillRect(pipsLeft + pip * 8, y + TILE - 9, 7, 6);
                g.setColor(PIPS[t.tier - 1]);
                g.fillRect(pipsLeft + pip * 8 + 1, y + TILE - 8, 5, 4);
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
            BufferedImage image = sprite.flip ? sprite.flipped : sprite.image;
            int w = image.getWidth(), ht = image.getHeight();
            int r = Math.round(enemy.type.radius);
            int cx = Math.round(p.x), cy = Math.round(p.y);

            // Walkers squash and stretch as they step; flyers flap and bob in the air.
            float scaleX, scaleY, lift;
            if (flying) {
                scaleX = 0.76f + 0.24f * Math.abs((float) Math.sin(enemy.animTime * 11));
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

            g.setColor(SHADOW);
            g.fillOval(cx - r, cy + ht / 2 - r / 2 - 2, r * 2, r);
            g.drawImage(image, left, top, dw, dh, null);
            if (enemy.flash > 0) {
                g.setComposite(AlphaComposite.SrcOver.derive(Math.min(1, enemy.flash) * 0.75f));
                g.drawImage(game.assets.enemySilhouette(enemy.type)[side], left, top, dw, dh, null);
                g.setComposite(saved);
            }

            if (s != null) {
                if (s.stunTime > 0) {
                    g.setColor(FROZEN);
                    g.fillOval(left - 1, top - 1, dw + 2, dh + 2);
                }
                int ring = r + 3, ringY = top + dh / 2;
                if (s.slowTime > 0) ring = statusRing(g, CHILL, cx, ringY, ring);
                if (s.burnTime > 0) ring = statusRing(g, BURN, cx, ringY, ring);
                if (s.poisonTime > 0) ring = statusRing(g, POISON, cx, ringY, ring);
                if (s.brittleTime > 0) ring = statusRing(g, BRITTLE, cx, ringY, ring);
                if (s.shredTime > 0) statusRing(g, SHRED, cx, ringY, ring);
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

    private static int statusRing(Graphics2D g, Color color, int cx, int cy, int radius) {
        g.setColor(color);
        g.drawOval(cx - radius, cy - radius, radius * 2, radius * 2);
        return radius + 2;
    }

    private void drawProjectiles(Graphics2D g) {
        projectileQuery.fetch();
        Composite saved = g.getComposite();
        Composite faint = AlphaComposite.SrcOver.derive(0.35f);
        for (int i = 0; i < projectileQuery.size(); i++) {
            int e = projectileQuery.get(i);
            Projectile pr = projectiles.get(e);
            Position p = positions.get(e);
            int x = Math.round(p.x), y = Math.round(p.y);
            if (pr.lobbed) {
                int height = Math.round((float) Math.sin(Math.PI * pr.progress) * ARC_HEIGHT);
                Color color = shellColor(pr.source);
                g.setColor(SHADOW);
                g.fillOval(x - 4, y - 2, 8, 4);
                g.setColor(color.darker().darker());
                g.fillOval(x - 5, y - height - 5, 10, 10);
                g.setColor(color);
                g.fillOval(x - 4, y - height - 4, 7, 7);
                g.setColor(Color.WHITE);
                g.fillRect(x - 2, y - height - 3, 2, 2);
                continue;
            }
            float dx = (float) Math.cos(pr.angle), dy = (float) Math.sin(pr.angle);
            switch (pr.source) {
                case ICE_LANCE, SUNBOW -> {
                    Color color = pr.source == TowerType.SUNBOW ? SUN_SHOT : ICE_SHOT;
                    g.setColor(color);
                    g.setComposite(faint);
                    g.setStroke(STROKES[4]);
                    g.drawLine(x - Math.round(dx * 30), y - Math.round(dy * 30), x, y);
                    g.setComposite(saved);
                    g.setStroke(STROKES[2]);
                    g.drawLine(x - Math.round(dx * 18), y - Math.round(dy * 18), x, y);
                    g.setStroke(STROKES[0]);
                    g.setColor(Color.WHITE);
                    g.drawLine(x - Math.round(dx * 14), y - Math.round(dy * 14), x, y);
                }
                case FLAK -> {
                    g.setColor(Color.DARK_GRAY);
                    g.fillOval(x - 3, y - 3, 7, 7);
                    g.setColor(TowerType.TESLA.color);
                    g.fillOval(x - 2, y - 2, 4, 4);
                }
                case VENOM, BLIGHT -> {
                    g.setColor(ACID_SHOT);
                    g.setComposite(faint);
                    g.fillOval(x - Math.round(dx * 9) - 3, y - Math.round(dy * 9) - 3, 6, 6);
                    g.fillOval(x - Math.round(dx * 16) - 2, y - Math.round(dy * 16) - 2, 4, 4);
                    g.setComposite(saved);
                    g.setColor(VENOM_DARK);
                    g.fillOval(x - 4, y - 4, 9, 9);
                    g.setColor(ACID_SHOT);
                    g.fillOval(x - 3, y - 3, 6, 6);
                }
                default -> {
                    g.setColor(pr.source.color);
                    g.setComposite(faint);
                    g.setStroke(STROKES[1]);
                    g.drawLine(x - Math.round(dx * 24), y - Math.round(dy * 24), x, y);
                    g.setComposite(saved);
                    g.drawLine(x - Math.round(dx * 11), y - Math.round(dy * 11), x, y);
                    g.setColor(Color.WHITE);
                    g.fillRect(x - 1, y - 1, 3, 3);
                }
            }
        }
    }

    private static Color shellColor(TowerType type) {
        return switch (type) {
            case PLASMA -> PLASMA_SHOT;
            case BLIZZARD -> ICE_SHOT;
            case ACID -> ACID_SHOT;
            case CLUSTER -> SUN_SHOT;
            default -> IRON_SHOT;
        };
    }

    private void drawEffects(Graphics2D g) {
        effectQuery.fetch();
        Composite saved = g.getComposite();
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < effectQuery.size(); i++) {
            int e = effectQuery.get(i);
            Effect fx = effects.get(e);
            Position p = positions.get(e);
            Lifetime life = lifetimes.get(e);
            float left = Math.clamp(life.remaining / life.total, 0f, 1f);
            int x = Math.round(p.x), y = Math.round(p.y);
            g.setComposite(AlphaComposite.SrcOver.derive(left));
            g.setColor(fx.color);
            switch (fx.kind) {
                case BEAM -> {
                    int x2 = Math.round(fx.x2), y2 = Math.round(fx.y2);
                    int width = Math.clamp(Math.round(fx.width), 1, 5);
                    g.setComposite(AlphaComposite.SrcOver.derive(left * 0.3f));
                    g.setStroke(STROKES[width + 2]);
                    g.drawLine(x, y, x2, y2);
                    g.setComposite(AlphaComposite.SrcOver.derive(left));
                    g.setStroke(STROKES[width - 1]);
                    g.drawLine(x, y, x2, y2);
                    g.setStroke(STROKES[0]);
                    g.setColor(Color.WHITE);
                    g.drawLine(x, y, x2, y2);
                    g.fillOval(x2 - 3, y2 - 3, 6, 6);
                }
                case BOLT -> {
                    final int segments = 5;
                    float nx = -(fx.y2 - p.y), ny = fx.x2 - p.x;
                    float length = Math.max(1, (float) Math.sqrt(nx * nx + ny * ny));
                    int[] xs = new int[segments + 1], ys = new int[segments + 1];
                    xs[0] = x;
                    ys[0] = y;
                    for (int k = 1; k <= segments; k++) {
                        float t = k / (float) segments;
                        float jitter = k == segments ? 0 : random.nextFloat(-7, 7);
                        xs[k] = Math.round(p.x + (fx.x2 - p.x) * t + nx / length * jitter);
                        ys[k] = Math.round(p.y + (fx.y2 - p.y) * t + ny / length * jitter);
                    }
                    g.setComposite(AlphaComposite.SrcOver.derive(left * 0.3f));
                    g.setStroke(STROKES[5]);
                    g.drawPolyline(xs, ys, segments + 1);
                    g.setComposite(AlphaComposite.SrcOver.derive(left));
                    g.setStroke(STROKES[1]);
                    g.drawPolyline(xs, ys, segments + 1);
                    g.setStroke(STROKES[0]);
                    g.setColor(Color.WHITE);
                    g.drawPolyline(xs, ys, segments + 1);
                }
                case RING -> {
                    int r = Math.round(fx.radius * (0.35f + 0.65f * (1 - left)));
                    g.setStroke(STROKES[2]);
                    g.drawOval(x - r, y - r, r * 2, r * 2);
                }
                case FLASH -> {
                    int r = Math.round(fx.radius * (0.5f + 0.5f * (1 - left)));
                    g.setComposite(AlphaComposite.SrcOver.derive(left * 0.55f));
                    g.fillOval(x - r, y - r, r * 2, r * 2);
                    g.setColor(Color.WHITE);
                    g.fillOval(x - r / 2, y - r / 2, r, r);
                }
                case SPARK -> {
                    int size = Math.round(fx.radius);
                    g.fillRect(x - size / 2, y - size / 2, size, size);
                }
                case TEXT -> {
                    g.setFont(TEXT);
                    int width = g.getFontMetrics().stringWidth(fx.text);
                    int tx = Math.clamp(x - width / 2, 4, Math.max(4, Level.WIDTH - width - 4));
                    g.setColor(Color.BLACK);
                    g.drawString(fx.text, tx + 1, y + 1);
                    g.setColor(fx.color);
                    g.drawString(fx.text, tx, y);
                }
            }
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
        g.setColor(RANGE_FILL);
        g.fillOval(cx - r, cy - r, r * 2, r * 2);
        g.setColor(RANGE_LINE);
        g.setStroke(STROKES[0]);
        g.drawOval(cx - r, cy - r, r * 2, r * 2);
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
        if (game.hurtFlash > 0) {
            // A monster got through.
            g.setColor(new Color(220, 38, 38, Math.round(Math.min(1, game.hurtFlash) * 90)));
            g.fillRect(0, 0, Level.WIDTH, Level.HEIGHT);
        }
        boolean boss = !game.gameOver && drawBossBar(g);
        if (game.toastTime > 0 && !game.gameOver) {
            int top = boss ? 40 : 14;
            Composite saved = g.getComposite();
            g.setComposite(AlphaComposite.SrcOver.derive(Math.min(1, game.toastTime * 2)));
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
        for (int i = 0; i < enemyQuery.size(); i++) {
            int e = enemyQuery.get(i);
            if (!enemies.get(e).type.boss()) continue;
            Health h = healths.get(e);
            hp += Math.max(0, h.hp);
            max += h.max;
            count++;
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
        String text = count > 1 ? "GOLEM x" + count : "GOLEM";
        g.setColor(Color.WHITE);
        g.drawString(text, x + (width - g.getFontMetrics().stringWidth(text)) / 2, y + 13);
        return true;
    }
}
