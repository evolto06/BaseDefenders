package game;

/** A player action for the build system to carry out. */
public record Command(Kind kind, TowerType type, int col, int row, int col2, int row2) {
    public enum Kind { PLACE, MERGE, UPGRADE, SELL }

    public static Command place(TowerType type, int col, int row) {
        return new Command(Kind.PLACE, type, col, row, 0, 0);
    }

    /** Merge the tower at (fromCol, fromRow) into the one at (toCol, toRow). */
    public static Command merge(int fromCol, int fromRow, int toCol, int toRow) {
        return new Command(Kind.MERGE, null, fromCol, fromRow, toCol, toRow);
    }

    /** Merge the nearest identical tower into the one at (col, row). */
    public static Command upgrade(int col, int row) {
        return new Command(Kind.UPGRADE, null, col, row, 0, 0);
    }

    public static Command sell(int col, int row) {
        return new Command(Kind.SELL, null, col, row, 0, 0);
    }
}
