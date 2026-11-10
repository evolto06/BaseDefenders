package game;

/** A raw mouse or key event in logical screen coordinates, queued from the UI thread. */
public record InputEvent(Kind kind, int x, int y, int code) {
    public enum Kind { MOVE, PRESS, RELEASE, KEY }
}
