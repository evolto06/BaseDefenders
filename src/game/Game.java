package game;

import ecs.GameSystem;
import ecs.World;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import systems.BuildSystem;
import systems.DeathSystem;
import systems.EconomySystem;
import systems.EffectSystem;
import systems.HealerSystem;
import systems.HudSystem;
import systems.InputSystem;
import systems.MenuSystem;
import systems.MovementSystem;
import systems.ProjectileSystem;
import systems.RallySystem;
import systems.RenderSystem;
import systems.ShieldSystem;
import systems.SpatialSystem;
import systems.StatusSystem;
import systems.SummonSystem;
import systems.TutorialSystem;
import systems.TowerSystem;
import systems.WaveSystem;
import systems.ZoneSystem;

/**
 * The game context: the ECS world, the shared services systems use, the global (non-entity)
 * state such as mana and lives, and the system schedule.
 */
public final class Game {
    public static final float STEP = 1f / 60f;
    public static final int START_LIVES = 20;
    public static final int FINAL_WAVE = 30;
    public static final float SELL_REFUND = 0.6f;
    public static final int MAX_SPEED = 3;

    public final World world = new World();
    /** Which screen is showing. Matches only run on {@link Screen#PLAYING}. */
    public enum Screen { TITLE, MAPS, PLAYING }

    /** Buttons on the pause and defeat menu. */
    public enum MenuAction {
        RESUME("Resume"), RESTART("Restart"), MAPS("Change map");

        public final String label;

        MenuAction(String label) {
            this.label = label;
        }
    }

    private static final MenuAction[] PAUSE_MENU = {MenuAction.RESUME, MenuAction.RESTART, MenuAction.MAPS};
    private static final MenuAction[] DEFEAT_MENU = {MenuAction.RESTART, MenuAction.MAPS};

    public final Assets assets = new Assets();
    public final Records records = new Records();
    public Screen screen = Screen.TITLE;
    public MapDef map = MapDef.MEADOW;
    public Level level = new Level(map);
    public BufferedImage background = assets.background(map);
    public final Random rng = new Random();
    public final SpatialGrid grid = new SpatialGrid(world);
    public final Prefabs prefabs = new Prefabs(world, level, assets, rng);
    public final Combat combat = new Combat(world, prefabs);

    /** Raw input, written by the UI thread and drained by the input system. */
    public final Queue<InputEvent> input = new ConcurrentLinkedQueue<>();
    public final ArrayDeque<Command> commands = new ArrayDeque<>();
    /** Tower entity on each tile, or {@link World#NONE}. */
    public final int[][] towerAt = new int[Level.COLS][Level.ROWS];

    // Economy and progress.
    public float mana;
    public int lives, wave, kills;

    // Wave flow. activePlan is non-null while a wave is still spawning.
    public WavePlan nextPlan, activePlan;
    public int spawnIndex;
    public float spawnClock, waveTimer;
    public boolean callWave, waveCleared;

    public boolean paused, gameOver, victory;
    public int speed = 1;

    // Interface state.
    public TowerType placing;
    public int selected = World.NONE;
    public int mouseX, mouseY;
    public int hoverCol = -1, hoverRow = -1;
    public int dragCol = -1, dragRow = -1;
    public String toast = "";
    public float toastTime;
    public float uiTime;
    public int fps;
    public int enemyCount;
    /** 1 when triggered, decaying to 0: the mana bar flashes red, the field flashes on a leak. */
    public float manaFlash, hurtFlash;
    public boolean showHelp, showChart, victoryScreen;
    /** Set when the player asks to leave; the window loop exits on it. */
    public volatile boolean quit;
    /** True while a card pressed in the hand is being dragged towards the field. */
    public boolean cardDrag;
    /** Whether the pointer is over something clickable. */
    public boolean cursorHand;
    /** Contextual tip shown along the bottom of the field while {@code hintTime} is positive. */
    public String hint = "";
    public float hintTime;

    private final GameSystem[] simulation;
    private final InputSystem inputSystem;
    private final BuildSystem buildSystem;
    private final TutorialSystem tutorialSystem;
    private final RenderSystem renderSystem;
    private final HudSystem hudSystem;
    private final MenuSystem menuSystem;
    private float accumulator;

    public Game() {
        simulation = new GameSystem[] {
            new WaveSystem(this),
            new EconomySystem(this),
            new StatusSystem(this),
            new MovementSystem(this),
            new HealerSystem(this),
            new RallySystem(this),
            new SummonSystem(this),
            new ShieldSystem(this),
            new SpatialSystem(this),
            new TowerSystem(this),
            new ProjectileSystem(this),
            new ZoneSystem(this),
            new DeathSystem(this),
            new EffectSystem(this),
        };
        inputSystem = new InputSystem(this);
        buildSystem = new BuildSystem(this);
        tutorialSystem = new TutorialSystem(this);
        renderSystem = new RenderSystem(this);
        hudSystem = new HudSystem(this);
        menuSystem = new MenuSystem(this);
        reset();
    }

    /** Begins a fresh match on the given map. */
    public void start(MapDef chosen) {
        map = chosen;
        level = new Level(chosen);
        prefabs.setLevel(level);
        background = assets.background(chosen);
        reset();
        screen = Screen.PLAYING;
    }

    public MenuAction[] menuActions() {
        return gameOver ? DEFEAT_MENU : PAUSE_MENU;
    }

    /** Restarts the current map. */
    public void reset() {
        world.clear();
        commands.clear();
        for (int[] column : towerAt) Arrays.fill(column, World.NONE);
        mana = 130;
        lives = START_LIVES;
        wave = 0;
        kills = 0;
        nextPlan = WavePlan.forWave(1);
        activePlan = null;
        waveTimer = 0;
        enemyCount = 0;
        victoryScreen = false;
        hurtFlash = 0;
        callWave = false;
        waveCleared = false;
        paused = false;
        gameOver = false;
        victory = false;
        placing = null;
        selected = World.NONE;
        dragCol = -1;
        cardDrag = false;
        showChart = false;
        accumulator = 0;
        toastTime = 0;
    }

    public float manaRegen() {
        return 5 + 0.3f * wave;
    }

    public float manaCap() {
        return 200 + 20 * wave;
    }

    public boolean spawning() {
        return activePlan != null;
    }

    public void toast(String message) {
        toast = message;
        toastTime = 2.5f;
    }

    /** Advances the game by one rendered frame of {@code realDt} seconds. */
    public void frame(float realDt) {
        realDt = Math.min(realDt, 0.1f);
        uiTime += realDt;
        toastTime = Math.max(0, toastTime - realDt);
        manaFlash = Math.max(0, manaFlash - realDt * 2.5f);
        hurtFlash = Math.max(0, hurtFlash - realDt * 2.5f);
        inputSystem.update(realDt);
        if (screen != Screen.PLAYING) return;
        buildSystem.update(realDt);
        tutorialSystem.update(realDt);
        world.flush();
        if (paused || gameOver || showHelp || showChart || victoryScreen) return;
        accumulator += realDt * speed;
        while (accumulator >= STEP && !gameOver) {
            accumulator -= STEP;
            for (GameSystem system : simulation) system.update(STEP);
            world.flush();
        }
    }

    /** Draws the whole logical screen ({@link Layout#WIDTH} x {@link Layout#HEIGHT}). */
    public void render(Graphics2D g) {
        if (screen != Screen.PLAYING) {
            menuSystem.render(g);
            return;
        }
        renderSystem.render(g);
        hudSystem.render(g);
    }
}
