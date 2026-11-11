package game;

import components.Effect;
import components.Enemy;
import components.Healer;
import components.Health;
import components.Lifetime;
import components.PathFollower;
import components.Position;
import components.Projectile;
import components.Sprite;
import components.Status;
import components.Tower;
import components.Velocity;
import components.Zone;
import ecs.World;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.Random;

/** Assembles entities from components. Nothing else in the game knows what an entity is made of. */
public final class Prefabs {
    /** How far, in pixels, an enemy may walk from the centre line of its route. */
    private static final float LANE_SPREAD = 8;

    private final World world;
    private Level level;
    private final Assets assets;
    private final Random rng;

    public Prefabs(World world, Level level, Assets assets, Random rng) {
        this.world = world;
        this.level = level;
        this.assets = assets;
        this.rng = rng;
    }

    public void setLevel(Level level) {
        this.level = level;
    }

    // ---- actors --------------------------------------------------------------------------

    /** Spawns an enemy at the start of its route. */
    public int enemy(EnemyType type, float hpScale) {
        float[] xs = type.flying ? level.flyX : level.pathX;
        float[] ys = type.flying ? level.flyY : level.pathY;
        float length = type.flying ? level.flyLength : level.pathLength;
        float laneX = (rng.nextFloat() - 0.5f) * 2 * LANE_SPREAD, laneY = (rng.nextFloat() - 0.5f) * 2 * LANE_SPREAD;
        return enemy(type, hpScale, xs[0] + laneX, ys[0] + laneY, laneX, laneY, xs, ys, 1, length);
    }

    /** Spawns an enemy part-way along a route, e.g. the halves of a split slime. */
    public int enemy(EnemyType type, float hpScale, float x, float y, float laneX, float laneY,
                     float[] xs, float[] ys, int next, float remaining) {
        int e = world.create();
        world.add(e, new Position(x, y));
        world.add(e, new Enemy(type, hpScale, rng.nextFloat() * 6));
        world.add(e, new Health(type.hp * hpScale, type.armor));
        PathFollower follower = world.add(e, new PathFollower(xs, ys, next, type.speed, remaining));
        follower.laneX = laneX;
        follower.laneY = laneY;
        world.add(e, new Status());
        BufferedImage[] images = assets.enemy(type);
        world.add(e, new Sprite(images[0], images[1]));
        if (type == EnemyType.SHAMAN) world.add(e, new Healer(85, 0.08f, 2.5f));
        return e;
    }

    public int tower(TowerType type, int tier, int col, int row, int invested) {
        int e = world.create();
        world.add(e, new Position(Level.center(col), Level.center(row)));
        Tower tower = world.add(e, new Tower(type, tier, col, row, invested));
        tower.angle = (float) -Math.PI / 2;
        return e;
    }

    // ---- shots ---------------------------------------------------------------------------

    public void homing(int owner, TowerType source, int tier, float x, float y, int target, float tx, float ty) {
        int e = world.create();
        world.add(e, new Position(x, y));
        Projectile p = world.add(e, new Projectile(source, tier, target, tx, ty));
        p.owner = owner;
        p.speed = source.projectileSpeed;
        p.angle = (float) Math.atan2(ty - y, tx - x);
    }

    public void lobbed(int owner, TowerType source, int tier, float x, float y, float tx, float ty) {
        int e = world.create();
        world.add(e, new Position(x, y));
        Projectile p = world.add(e, new Projectile(source, tier, World.NONE, tx, ty));
        p.owner = owner;
        p.lobbed = true;
        p.sx = x;
        p.sy = y;
        p.duration = Math.max(0.25f, (float) Math.hypot(tx - x, ty - y) / source.projectileSpeed);
    }

    public void zone(int owner, float x, float y, float radius, float slow, float dps, Color color,
                     float seconds) {
        int e = world.create();
        world.add(e, new Position(x, y));
        world.add(e, new Zone(radius, slow, dps, color)).owner = owner;
        world.add(e, new Lifetime(seconds));
    }

    // ---- visual effects ------------------------------------------------------------------

    public void beam(float x1, float y1, float x2, float y2, Color color, float width, float seconds) {
        line(Effect.Kind.BEAM, x1, y1, x2, y2, color, width, seconds);
    }

    /** A jagged lightning arc. */
    public void bolt(float x1, float y1, float x2, float y2, Color color) {
        line(Effect.Kind.BOLT, x1, y1, x2, y2, color, 2, 0.14f);
    }

    private void line(Effect.Kind kind, float x1, float y1, float x2, float y2, Color color,
                      float width, float seconds) {
        int e = world.create();
        world.add(e, new Position(x1, y1));
        Effect fx = world.add(e, new Effect(kind, color));
        fx.x2 = x2;
        fx.y2 = y2;
        fx.width = width;
        world.add(e, new Lifetime(seconds));
    }

    /** A ring that expands out to {@code radius} while fading. */
    public void ring(float x, float y, float radius, Color color, float seconds) {
        int e = world.create();
        world.add(e, new Position(x, y));
        Effect fx = world.add(e, new Effect(Effect.Kind.RING, color));
        fx.radius = radius;
        world.add(e, new Lifetime(seconds));
    }

    /** A filled disc that swells and fades. */
    public void flash(float x, float y, float radius, Color color, float seconds) {
        int e = world.create();
        world.add(e, new Position(x, y));
        Effect fx = world.add(e, new Effect(Effect.Kind.FLASH, color));
        fx.radius = radius;
        world.add(e, new Lifetime(seconds));
    }

    public void explosion(float x, float y, float radius, Color color) {
        flash(x, y, radius * 0.8f, color, 0.22f);
        ring(x, y, radius, color, 0.32f);
        burst(x, y, color, 6);
    }

    public void text(float x, float y, String text, Color color) {
        int e = world.create();
        world.add(e, new Position(x, y));
        Effect fx = world.add(e, new Effect(Effect.Kind.TEXT, color));
        fx.text = text;
        world.add(e, new Velocity(0, -28, 0));
        world.add(e, new Lifetime(1.1f));
    }

    public void burst(float x, float y, Color color, int count) {
        for (int i = 0; i < count; i++) {
            double angle = rng.nextDouble() * Math.PI * 2;
            float speed = 40 + rng.nextFloat() * 90;
            int e = world.create();
            world.add(e, new Position(x, y));
            Effect fx = world.add(e, new Effect(Effect.Kind.SPARK, color));
            fx.radius = 2 + rng.nextInt(2);
            world.add(e, new Velocity((float) Math.cos(angle) * speed, (float) Math.sin(angle) * speed, 4));
            world.add(e, new Lifetime(0.25f + rng.nextFloat() * 0.25f));
        }
    }
}
