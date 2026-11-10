package game;

import java.util.Random;

/** One map laid out on the tile grid: the road enemies walk and the scenery that blocks building. */
public final class Level {
    public static final int TILE = 48, COLS = 24, ROWS = 10;
    public static final int WIDTH = COLS * TILE, HEIGHT = ROWS * TILE;
    public static final int CLEAR = 0, TREE = 1, ROCK = 2;
    private static final int SCENERY = 15;

    public final MapDef def;
    public final boolean[][] path = new boolean[COLS][ROWS];
    public final int[][] decor = new int[COLS][ROWS];
    public final float[] pathX, pathY;
    public final float pathLength;
    /** Flyers ignore the road and cross straight from the spawn to the base. */
    public final float[] flyX, flyY;
    public final float flyLength;
    private final int entryRow, exitRow;

    public Level(MapDef def) {
        this.def = def;
        int[][] route = def.route;
        int n = route.length;
        entryRow = route[0][1];
        exitRow = route[n - 1][1];
        pathX = new float[n];
        pathY = new float[n];
        for (int i = 0; i < n; i++) {
            pathX[i] = center(route[i][0]);
            pathY[i] = center(route[i][1]);
        }
        float length = 0;
        for (int i = 0; i + 1 < n; i++) {
            int c0 = Math.min(route[i][0], route[i + 1][0]), c1 = Math.max(route[i][0], route[i + 1][0]);
            int r0 = Math.min(route[i][1], route[i + 1][1]), r1 = Math.max(route[i][1], route[i + 1][1]);
            for (int c = Math.max(c0, 0); c <= Math.min(c1, COLS - 1); c++) {
                for (int r = r0; r <= r1; r++) path[c][r] = true;
            }
            length += Math.abs(pathX[i + 1] - pathX[i]) + Math.abs(pathY[i + 1] - pathY[i]);
        }
        pathLength = length;
        flyX = new float[] {pathX[0], pathX[n - 1]};
        flyY = new float[] {pathY[0], pathY[n - 1]};
        flyLength = (float) Math.hypot(flyX[1] - flyX[0], flyY[1] - flyY[0]);

        Random random = new Random(def.seed);
        for (int placed = 0; placed < SCENERY; ) {
            int c = random.nextInt(COLS), r = random.nextInt(ROWS);
            if (path[c][r] || decor[c][r] != CLEAR) continue;
            decor[c][r] = random.nextInt(3) == 0 ? ROCK : TREE;
            placed++;
        }
    }

    public static float center(int tile) {
        return tile * TILE + TILE / 2f;
    }

    public boolean inBounds(int col, int row) {
        return col >= 0 && col < COLS && row >= 0 && row < ROWS;
    }

    /** Like {@code path[col][row]} but also true for the road where it runs off the field. */
    public boolean isPath(int col, int row) {
        if (row < 0 || row >= ROWS) return false;
        if (col < 0) return row == entryRow;
        if (col >= COLS) return row == exitRow;
        return path[col][row];
    }

    public boolean buildable(int col, int row) {
        return inBounds(col, row) && !path[col][row] && decor[col][row] == CLEAR;
    }
}
