package systems;

import components.Position;
import components.Tower;
import ecs.GameSystem;
import ecs.Query;
import ecs.Store;
import ecs.World;
import game.Command;
import game.Game;
import game.TowerType;
import java.awt.Color;

/** Carries out the player's build commands: placing, merging and selling towers. */
public final class BuildSystem implements GameSystem {
    private static final Color GOLD = new Color(0xFCD34D);
    private static final Color MANA = new Color(0x7DD3FC);

    private final Game game;
    private final Store<Tower> towers;
    private final Store<Position> positions;
    private final Query towerQuery;

    public BuildSystem(Game game) {
        this.game = game;
        this.towers = game.world.store(Tower.class);
        this.positions = game.world.store(Position.class);
        this.towerQuery = game.world.query(Tower.class);
    }

    @Override
    public void update(float dt) {
        Command command;
        while ((command = game.commands.poll()) != null) {
            if (game.gameOver) continue;
            switch (command.kind()) {
                case PLACE -> place(command.type(), command.col(), command.row());
                case MERGE -> merge(command.col(), command.row(), command.col2(), command.row2());
                case UPGRADE -> upgrade(command.col(), command.row());
                case SELL -> sell(command.col(), command.row());
            }
        }
    }

    private void place(TowerType type, int col, int row) {
        if (!game.level.inBounds(col, row)) return;
        int existing = game.towerAt[col][row];
        if (existing != World.NONE) {
            // Building on top of a tier-1 tower merges the new tower straight into it.
            Tower tower = towers.get(existing);
            TowerType result = TowerType.merge(type, 1, tower.type, tower.tier);
            if (result == null) {
                game.toast(explain(type, 1, tower.type, tower.tier));
                return;
            }
            if (!pay(type.cost)) return;
            applyMerge(existing, result, TowerType.mergedTier(type, 1, tower.type), type.cost);
            return;
        }
        if (!game.level.buildable(col, row)) {
            game.toast("Can't build there");
            return;
        }
        if (!pay(type.cost)) return;
        int e = game.prefabs.tower(type, 1, col, row, type.cost);
        game.towerAt[col][row] = e;
        Position p = positions.get(e);
        game.prefabs.ring(p.x, p.y, 30, type.color, 0.3f);
    }

    private void merge(int fromCol, int fromRow, int toCol, int toRow) {
        if (!game.level.inBounds(fromCol, fromRow) || !game.level.inBounds(toCol, toRow)) return;
        int from = game.towerAt[fromCol][fromRow], to = game.towerAt[toCol][toRow];
        Tower a = towers.get(from), b = towers.get(to);
        if (a == null || b == null || from == to) return;
        TowerType result = TowerType.merge(a.type, a.tier, b.type, b.tier);
        if (result == null) {
            game.toast(explain(a.type, a.tier, b.type, b.tier));
            return;
        }
        applyMerge(to, result, TowerType.mergedTier(a.type, a.tier, b.type), a.invested);
        game.world.destroy(from);
        game.towerAt[fromCol][fromRow] = World.NONE;
    }

    /** One-click upgrade: pulls the nearest identical tower into this one. */
    private void upgrade(int col, int row) {
        if (!game.level.inBounds(col, row)) return;
        Tower tower = towers.get(game.towerAt[col][row]);
        if (tower == null) return;
        Tower twin = nearestTwin(towerQuery.fetch(), towers, tower);
        if (twin == null) {
            game.toast(tower.tier == TowerType.MAX_TIER ? tower.type.label + " is already at max tier"
                    : "Needs another tier " + tower.tier + " " + tower.type.label);
            return;
        }
        merge(twin.col, twin.row, col, row);
    }

    /** The closest other tower of the same type and tier, or null. Expects a fetched query. */
    public static Tower nearestTwin(Query fetched, Store<Tower> towers, Tower tower) {
        if (tower.tier >= TowerType.MAX_TIER) return null;
        Tower best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (int i = 0; i < fetched.size(); i++) {
            Tower other = towers.get(fetched.get(i));
            if (other == null || other == tower || other.type != tower.type || other.tier != tower.tier) continue;
            int distance = Math.abs(other.col - tower.col) + Math.abs(other.row - tower.row);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = other;
            }
        }
        return best;
    }

    private void applyMerge(int entity, TowerType result, int tier, int addedInvestment) {
        Tower tower = towers.get(entity);
        boolean fused = result != tower.type;
        tower.type = result;
        tower.tier = tier;
        tower.invested += addedInvestment;
        tower.cooldown = 0.3f;
        tower.target = World.NONE;
        game.selected = entity;
        Position p = positions.get(entity);
        game.prefabs.ring(p.x, p.y, 44, GOLD, 0.45f);
        game.prefabs.burst(p.x, p.y, GOLD, 10);
        game.prefabs.text(p.x, p.y - 26, fused ? result.label + "!" : "Tier " + tier + "!", GOLD);
    }

    private void sell(int col, int row) {
        if (!game.level.inBounds(col, row)) return;
        int e = game.towerAt[col][row];
        Tower tower = towers.get(e);
        if (tower == null) return;
        int refund = Math.round(tower.invested * Game.SELL_REFUND);
        game.mana += refund;
        Position p = positions.get(e);
        game.prefabs.text(p.x, p.y - 20, "+" + refund, MANA);
        game.world.destroy(e);
        game.towerAt[col][row] = World.NONE;
        if (game.selected == e) game.selected = World.NONE;
    }

    private boolean pay(int cost) {
        if (game.mana < cost) {
            game.toast("Not enough mana");
            game.manaFlash = 1;
            return false;
        }
        game.mana -= cost;
        return true;
    }

    private static String explain(TowerType a, int tierA, TowerType b, int tierB) {
        if (tierA != tierB) return "Towers must be the same tier to merge";
        if (a == b) return b.label + " is already at max tier";
        return "Only two different base towers can fuse";
    }
}
