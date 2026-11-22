package game;

import components.Health;
import components.Position;
import components.Status;
import ecs.Query;
import ecs.Store;
import ecs.World;
import java.util.Arrays;

/**
 * Uniform grid of living enemies, rebuilt once per step with a counting sort, so range queries
 * touch only nearby cells instead of every enemy. Monsters underground are left out, so no tower,
 * blast or aura can find them.
 */
public final class SpatialGrid {
    private static final int CELL = Level.TILE;
    // One extra ring of cells catches enemies that are still walking in from off the field.
    private static final int COLS = Level.COLS + 2, ROWS = Level.ROWS + 2;

    private final Store<Position> positions;
    private final Store<Health> healths;
    private final Store<Status> statuses;
    private final int[] start = new int[COLS * ROWS + 1];
    private final int[] cursor = new int[COLS * ROWS];
    private int[] cellOf = new int[256];
    private int[] items = new int[256];
    private int[] result = new int[256];

    public SpatialGrid(World world) {
        positions = world.store(Position.class);
        healths = world.store(Health.class);
        statuses = world.store(Status.class);
    }

    public void rebuild(Query enemies) {
        int n = enemies.size();
        if (items.length < n) {
            int size = Math.max(n, items.length * 2);
            items = new int[size];
            cellOf = new int[size];
            result = new int[size];
        }
        Arrays.fill(start, 0);
        for (int i = 0; i < n; i++) {
            int e = enemies.get(i);
            Status s = statuses.get(e);
            if (s != null && s.burrowTime > 0) {
                cellOf[i] = -1;
                continue;
            }
            Position p = positions.get(e);
            int cell = p == null ? 0 : row(p.y) * COLS + col(p.x);
            cellOf[i] = cell;
            start[cell + 1]++;
        }
        for (int c = 0; c < cursor.length; c++) {
            start[c + 1] += start[c];
            cursor[c] = start[c];
        }
        for (int i = 0; i < n; i++) {
            if (cellOf[i] >= 0) items[cursor[cellOf[i]]++] = enemies.get(i);
        }
    }

    /**
     * Collects living enemies whose centre is within {@code radius} of the point. Returns the
     * count; read them with {@link #result(int)}. Results are overwritten by the next query.
     */
    public int query(float x, float y, float radius) {
        int c0 = col(x - radius), c1 = col(x + radius);
        int r0 = row(y - radius), r1 = row(y + radius);
        float r2 = radius * radius;
        int count = 0;
        for (int r = r0; r <= r1; r++) {
            for (int c = c0; c <= c1; c++) {
                int cell = r * COLS + c;
                for (int k = start[cell], end = start[cell + 1]; k < end; k++) {
                    int e = items[k];
                    Position p = positions.get(e);
                    if (p == null) continue;
                    float dx = p.x - x, dy = p.y - y;
                    if (dx * dx + dy * dy > r2) continue;
                    Health h = healths.get(e);
                    if (h == null || h.hp <= 0) continue;
                    result[count++] = e;
                }
            }
        }
        return count;
    }

    public int result(int i) {
        return result[i];
    }

    private static int col(float x) {
        return Math.clamp((int) Math.floor(x / CELL) + 1, 0, COLS - 1);
    }

    private static int row(float y) {
        return Math.clamp((int) Math.floor(y / CELL) + 1, 0, ROWS - 1);
    }
}
