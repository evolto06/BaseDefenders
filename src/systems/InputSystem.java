package systems;

import components.Tower;
import ecs.GameSystem;
import ecs.Store;
import ecs.World;
import game.Command;
import game.Game;
import game.InputEvent;
import game.Layout;
import game.Level;
import game.MapDef;
import game.TowerType;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

/** Turns raw mouse and key events into interface state changes and build commands. */
public final class InputSystem implements GameSystem {
    private final Game game;
    private final Store<Tower> towers;
    /** Whether the card being dragged was already the picked one when it was pressed. */
    private boolean cardWasPicked;

    public InputSystem(Game game) {
        this.game = game;
        this.towers = game.world.store(Tower.class);
    }

    @Override
    public void update(float dt) {
        InputEvent event;
        while ((event = game.input.poll()) != null) {
            boolean playing = game.screen == Game.Screen.PLAYING;
            switch (event.kind()) {
                case MOVE -> hover(event.x(), event.y());
                case PRESS -> {
                    hover(event.x(), event.y());
                    // A click anywhere closes whichever full-screen card is open.
                    if (closeOverlay()) break;
                    if (playing) press(event.x(), event.y(), event.code());
                    else if (event.code() == MouseEvent.BUTTON1) pressMenuScreen(event.x(), event.y());
                }
                case RELEASE -> {
                    hover(event.x(), event.y());
                    if (playing && event.code() == MouseEvent.BUTTON1) release();
                }
                case KEY -> {
                    if (closeOverlay()) break;
                    if (playing) key(event.code());
                    else keyMenuScreen(event.code());
                }
            }
        }
        if (!game.world.alive(game.selected)) game.selected = World.NONE;
    }

    private boolean closeOverlay() {
        if (!game.showHelp && !game.victoryScreen && !game.showChart) return false;
        game.showHelp = false;
        game.victoryScreen = false;
        game.showChart = false;
        endDrags();
        return true;
    }

    private void endDrags() {
        game.dragCol = -1;
        game.cardDrag = false;
    }

    private void hover(int x, int y) {
        game.mouseX = x;
        game.mouseY = y;
        boolean onField = game.screen == Game.Screen.PLAYING
                && x >= 0 && x < Layout.WIDTH && y >= Layout.TOP && y < Layout.BAR_Y;
        game.hoverCol = onField ? x / Level.TILE : -1;
        game.hoverRow = onField ? (y - Layout.TOP) / Level.TILE : -1;
        game.cursorHand = clickable(x, y);
    }

    private static boolean any(Rectangle[] rects, int x, int y) {
        for (Rectangle r : rects) {
            if (r.contains(x, y)) return true;
        }
        return false;
    }

    private boolean clickable(int x, int y) {
        if (game.showHelp || game.showChart || game.victoryScreen) return false;
        switch (game.screen) {
            case TITLE:
                return any(Layout.TITLE_BUTTONS, x, y);
            case MAPS:
                return any(Layout.MAP_CARDS, x, y) || Layout.BACK.contains(x, y);
            default:
                break;
        }
        if (menuOpen()) {
            for (int i = 0; i < game.menuActions().length; i++) {
                if (Layout.MENU_BUTTONS[i].contains(x, y)) return true;
            }
            return false;
        }
        Rectangle card = openTowerCard();
        if (card != null && card.contains(x, y)) {
            return Layout.upgradeButton(card).contains(x, y) || Layout.sellButton(card).contains(x, y);
        }
        if (game.hoverCol >= 0) return game.towerAt[game.hoverCol][game.hoverRow] != World.NONE;
        return any(Layout.CARDS, x, y) || Layout.NEXT.contains(x, y) || Layout.SPEED.contains(x, y)
                || Layout.PAUSE.contains(x, y) || Layout.CHART.contains(x, y) || Layout.MENU.contains(x, y)
                || Layout.HELP.contains(x, y);
    }

    private boolean menuOpen() {
        return game.paused || game.gameOver;
    }

    /** The floating card of the selected tower, or null when none is showing. */
    private Rectangle openTowerCard() {
        Tower tower = towers.get(game.selected);
        return tower == null || game.placing != null || game.gameOver ? null : Layout.towerCard(tower.col);
    }

    // ---- title and map select ----------------------------------------------------------

    private void pressMenuScreen(int x, int y) {
        if (game.screen == Game.Screen.TITLE) {
            if (Layout.TITLE_BUTTONS[0].contains(x, y)) game.screen = Game.Screen.MAPS;
            else if (Layout.TITLE_BUTTONS[1].contains(x, y)) game.showHelp = true;
            else if (Layout.TITLE_BUTTONS[2].contains(x, y)) game.quit = true;
            return;
        }
        if (Layout.BACK.contains(x, y)) {
            game.screen = Game.Screen.TITLE;
            return;
        }
        MapDef[] maps = MapDef.values();
        for (int i = 0; i < maps.length; i++) {
            if (Layout.MAP_CARDS[i].contains(x, y)) {
                game.start(maps[i]);
                return;
            }
        }
    }

    private void keyMenuScreen(int code) {
        if (code == KeyEvent.VK_H || code == KeyEvent.VK_F1) {
            game.showHelp = true;
        } else if (game.screen == Game.Screen.TITLE) {
            if (code == KeyEvent.VK_ENTER || code == KeyEvent.VK_SPACE) game.screen = Game.Screen.MAPS;
        } else if (code == KeyEvent.VK_ESCAPE) {
            game.screen = Game.Screen.TITLE;
        } else if (code >= KeyEvent.VK_1 && code < KeyEvent.VK_1 + MapDef.values().length) {
            game.start(MapDef.values()[code - KeyEvent.VK_1]);
        }
    }

    // ---- in a match --------------------------------------------------------------------

    private void press(int x, int y, int button) {
        if (button == MouseEvent.BUTTON3) {
            cancel();
            return;
        }
        if (button != MouseEvent.BUTTON1) return;
        if (menuOpen()) {
            Game.MenuAction[] actions = game.menuActions();
            for (int i = 0; i < actions.length; i++) {
                if (Layout.MENU_BUTTONS[i].contains(x, y)) run(actions[i]);
            }
            return;
        }
        if (Layout.HELP.contains(x, y)) {
            game.showHelp = true;
            return;
        }
        if (Layout.MENU.contains(x, y)) {
            game.paused = true;
            return;
        }
        Rectangle card = openTowerCard();
        if (card != null && card.contains(x, y)) {
            if (Layout.upgradeButton(card).contains(x, y)) upgradeSelected();
            else if (Layout.sellButton(card).contains(x, y)) sellSelected();
            return;
        }
        if (y >= Layout.BAR_Y) {
            pressBar(x, y);
            return;
        }
        if (game.hoverCol < 0) return;
        int tower = game.towerAt[game.hoverCol][game.hoverRow];
        if (game.placing != null && tower == World.NONE) {
            game.commands.add(Command.place(game.placing, game.hoverCol, game.hoverRow));
            return;
        }
        game.selected = tower;
        if (tower != World.NONE) {
            game.dragCol = game.hoverCol;
            game.dragRow = game.hoverRow;
        }
    }

    private void pressBar(int x, int y) {
        for (int i = 0; i < Layout.CARDS.length; i++) {
            if (Layout.CARDS[i].contains(x, y)) {
                // Pressing a card picks it up: drag it onto the field, or let go and click a tile.
                TowerType type = TowerType.BASES[i];
                cardWasPicked = game.placing == type;
                game.placing = type;
                game.selected = World.NONE;
                game.dragCol = -1;
                game.cardDrag = true;
                return;
            }
        }
        if (Layout.NEXT.contains(x, y)) nextOrRestart();
        else if (Layout.SPEED.contains(x, y)) cycleSpeed();
        else if (Layout.PAUSE.contains(x, y)) game.paused = true;
        else if (Layout.CHART.contains(x, y)) game.showChart = true;
    }

    private void release() {
        if (game.cardDrag) {
            game.cardDrag = false;
            if (game.hoverCol >= 0) {
                // Dropped on the field: play the card there and put it back in the hand.
                game.commands.add(Command.place(game.placing, game.hoverCol, game.hoverRow));
                game.placing = null;
            } else if (cardWasPicked) {
                // Clicked the card that was already picked: put it down.
                game.placing = null;
            }
            return;
        }
        if (game.dragCol < 0) return;
        // Dropping a dragged tower on another one asks to merge them. Releasing on the tower
        // that was pressed is a plain click: with a card picked, that builds it onto the tower.
        boolean moved = game.hoverCol != game.dragCol || game.hoverRow != game.dragRow;
        if (!moved) {
            if (game.placing != null) game.commands.add(Command.place(game.placing, game.dragCol, game.dragRow));
        } else if (game.hoverCol >= 0 && game.towerAt[game.hoverCol][game.hoverRow] != World.NONE) {
            game.commands.add(Command.merge(game.dragCol, game.dragRow, game.hoverCol, game.hoverRow));
        }
        game.dragCol = -1;
    }

    private void key(int code) {
        if (menuOpen()) {
            if (code == KeyEvent.VK_R) game.reset();
            else if (!game.gameOver && (code == KeyEvent.VK_P || code == KeyEvent.VK_ESCAPE)) game.paused = false;
            return;
        }
        if (code >= KeyEvent.VK_1 && code < KeyEvent.VK_1 + TowerType.BASES.length) {
            // The number keys pick the cards of the hand, left to right.
            choose(TowerType.BASES[code - KeyEvent.VK_1]);
            return;
        }
        switch (code) {
            case KeyEvent.VK_ESCAPE -> {
                // Esc backs out of whatever is in hand; with nothing in hand it opens the menu.
                if (game.placing != null || game.selected != World.NONE) cancel();
                else game.paused = true;
            }
            case KeyEvent.VK_SPACE -> game.callWave = true;
            case KeyEvent.VK_P -> game.paused = true;
            case KeyEvent.VK_F -> cycleSpeed();
            case KeyEvent.VK_E -> upgradeSelected();
            case KeyEvent.VK_C -> game.showChart = true;
            case KeyEvent.VK_X, KeyEvent.VK_DELETE -> sellSelected();
            case KeyEvent.VK_H, KeyEvent.VK_F1 -> game.showHelp = true;
            default -> { }
        }
    }

    private void run(Game.MenuAction action) {
        switch (action) {
            case RESUME -> game.paused = false;
            case RESTART -> game.reset();
            case MAPS -> {
                game.paused = false;
                game.screen = Game.Screen.MAPS;
            }
        }
    }

    private void choose(TowerType type) {
        game.placing = game.placing == type ? null : type;
        game.selected = World.NONE;
        endDrags();
    }

    private void cancel() {
        game.placing = null;
        game.selected = World.NONE;
        endDrags();
    }

    private void sellSelected() {
        Tower tower = towers.get(game.selected);
        if (tower != null) game.commands.add(Command.sell(tower.col, tower.row));
    }

    private void upgradeSelected() {
        Tower tower = towers.get(game.selected);
        if (tower != null) game.commands.add(Command.upgrade(tower.col, tower.row));
    }

    private void nextOrRestart() {
        if (game.gameOver) game.reset();
        else game.callWave = true;
    }

    private void cycleSpeed() {
        game.speed = game.speed % Game.MAX_SPEED + 1;
    }
}
