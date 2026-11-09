package ecs;

/** A unit of game logic that runs once per simulation step over the entities it queries. */
public interface GameSystem {
    void update(float dt);
}
