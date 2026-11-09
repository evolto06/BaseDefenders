package ecs;

/**
 * A snapshot of the entities matching a component mask. {@link #fetch()} refills it; because it
 * is a snapshot, systems may create and destroy entities while iterating.
 */
public final class Query {
    private final World world;
    private final long mask;
    private int[] ids = new int[128];
    private int size;

    Query(World world, long mask) {
        this.world = world;
        this.mask = mask;
    }

    public Query fetch() {
        if (ids.length < world.liveCount) ids = new int[Math.max(world.liveCount, ids.length * 2)];
        long[] masks = world.masks;
        boolean[] live = world.live;
        int[] generations = world.generations;
        int n = world.highWater;
        int count = 0;
        for (int i = 0; i < n; i++) {
            if (live[i] && (masks[i] & mask) == mask) ids[count++] = generations[i] << World.INDEX_BITS | i;
        }
        size = count;
        return this;
    }

    public int size() {
        return size;
    }

    public int get(int i) {
        return ids[i];
    }
}
