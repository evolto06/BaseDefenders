package ecs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Entity registry and component storage.
 *
 * An entity is an int handle: the low 16 bits are a slot index, the high bits a generation
 * counter. Slots are recycled, and the generation makes stale handles (e.g. a tower still
 * pointing at a dead enemy) resolve to "not alive" instead of to whoever reused the slot.
 *
 * Components live in one dense array per component type, indexed by slot, with a bitmask per
 * entity so queries are a single linear scan with a mask compare.
 */
public final class World {
    public static final int NONE = -1;

    static final int INDEX_BITS = 16;
    static final int INDEX_MASK = (1 << INDEX_BITS) - 1;
    private static final int GEN_MASK = 0x7FFF;
    private static final int MAX_ENTITIES = 1 << INDEX_BITS;
    private static final int MAX_TYPES = 64;

    private final Map<Class<?>, Store<?>> byType = new HashMap<>();
    private final List<Store<?>> stores = new ArrayList<>();

    int capacity = 1024;
    long[] masks = new long[capacity];
    int[] generations = new int[capacity];
    boolean[] live = new boolean[capacity];
    int highWater;
    int liveCount;

    private int[] free = new int[capacity];
    private int freeCount;
    private int[] doomed = new int[capacity];
    private int doomedCount;

    public int create() {
        int index;
        if (freeCount > 0) {
            index = free[--freeCount];
        } else {
            if (highWater == capacity) grow();
            index = highWater++;
        }
        live[index] = true;
        masks[index] = 0;
        liveCount++;
        return generations[index] << INDEX_BITS | index;
    }

    /** Marks the entity dead immediately; its slot and components are released on {@link #flush()}. */
    public void destroy(int entity) {
        if (!alive(entity)) return;
        int index = entity & INDEX_MASK;
        live[index] = false;
        liveCount--;
        doomed[doomedCount++] = index;
    }

    public boolean alive(int entity) {
        if (entity < 0) return false;
        int index = entity & INDEX_MASK;
        return index < highWater && live[index] && generations[index] == entity >>> INDEX_BITS;
    }

    /** Releases the slots of destroyed entities. Call once per frame, between system passes. */
    public void flush() {
        for (int i = 0; i < doomedCount; i++) {
            int index = doomed[i];
            long mask = masks[index];
            for (int s = 0; mask != 0; s++, mask >>>= 1) {
                if ((mask & 1) != 0) stores.get(s).data[index] = null;
            }
            masks[index] = 0;
            generations[index] = (generations[index] + 1) & GEN_MASK;
            free[freeCount++] = index;
        }
        doomedCount = 0;
    }

    public void clear() {
        for (int i = 0; i < highWater; i++) {
            if (live[i]) destroy(generations[i] << INDEX_BITS | i);
        }
        flush();
    }

    public int entityCount() {
        return liveCount;
    }

    @SuppressWarnings("unchecked")
    public <T> Store<T> store(Class<T> type) {
        Store<?> store = byType.get(type);
        if (store == null) {
            if (stores.size() == MAX_TYPES) throw new IllegalStateException("Too many component types");
            store = new Store<T>(this, stores.size(), capacity);
            stores.add(store);
            byType.put(type, store);
        }
        return (Store<T>) store;
    }

    @SuppressWarnings("unchecked")
    public <T> T add(int entity, T component) {
        if (!alive(entity)) throw new IllegalArgumentException("Entity is not alive");
        Store<T> store = (Store<T>) store(component.getClass());
        int index = entity & INDEX_MASK;
        store.data[index] = component;
        masks[index] |= store.bit;
        return component;
    }

    public <T> T get(int entity, Class<T> type) {
        return store(type).get(entity);
    }

    public boolean has(int entity, Class<?> type) {
        return store(type).has(entity);
    }

    public void remove(int entity, Class<?> type) {
        if (!alive(entity)) return;
        Store<?> store = store(type);
        int index = entity & INDEX_MASK;
        store.data[index] = null;
        masks[index] &= ~store.bit;
    }

    /** Builds a reusable query matching entities that have every listed component. */
    public Query query(Class<?>... all) {
        long mask = 0;
        for (Class<?> type : all) mask |= store(type).bit;
        return new Query(this, mask);
    }

    private void grow() {
        if (capacity == MAX_ENTITIES) throw new IllegalStateException("Entity limit reached");
        capacity *= 2;
        masks = Arrays.copyOf(masks, capacity);
        generations = Arrays.copyOf(generations, capacity);
        live = Arrays.copyOf(live, capacity);
        free = Arrays.copyOf(free, capacity);
        doomed = Arrays.copyOf(doomed, capacity);
        for (Store<?> store : stores) store.data = Arrays.copyOf(store.data, capacity);
    }
}
