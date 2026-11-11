package systems;

import ecs.GameSystem;
import game.Game;

/** Mana regenerates constantly, up to a cap. Refunds and bonuses may exceed the cap. */
public final class EconomySystem implements GameSystem {
    private final Game game;

    public EconomySystem(Game game) {
        this.game = game;
    }

    @Override
    public void update(float dt) {
        float cap = game.manaCap();
        if (game.mana < cap) game.mana = Math.min(cap, game.mana + game.manaRegen() * dt);
    }
}
