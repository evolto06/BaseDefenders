package systems;

public abstract interface  GameSystem {

    boolean active = true;
    abstract void update();
    boolean isActive();

}
