package components;

import game.EnemyType;

/** Periodically raises minions beside itself, which walk on from where it stands. */
public final class Summoner {
    public final EnemyType minion;
    public final int count;
    public final float interval;
    public float timer;

    public Summoner(EnemyType minion, int count, float interval) {
        this.minion = minion;
        this.count = count;
        this.interval = interval;
        this.timer = interval;
    }
}
