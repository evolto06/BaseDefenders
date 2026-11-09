package ecs;

/** Dense per-type component array. Systems cache these once and read components by entity handle. */
public final class Store<T> {
    private final World world;
    final long bit;
    Object[] data;

    Store(World world, int id, int capacity) {
        this.world = world;
        this.bit = 1L << id;
        this.data = new Object[capacity];
    }

    /** Returns the component, or null if the entity is dead or does not have one. */
    @SuppressWarnings("unchecked")
    public T get(int entity) {
        return world.alive(entity) ? (T) data[entity & World.INDEX_MASK] : null;
    }

    public boolean has(int entity) {
        return get(entity) != null;
    }
}
