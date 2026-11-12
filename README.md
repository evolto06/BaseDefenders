# Base Defenders

A tower defense game in plain Java (Swing/Java2D, no libraries, no asset files) built on an
Entity-Component-System architecture. Hold the keep against 30 waves of monsters, then see how
long you last in endless mode.

## Run

Requires JDK 21 or newer.

```
run.bat            (Windows)
./run.sh           (Git Bash / Linux / macOS)
```

Or by hand: `javac -d bin -sourcepath src src/main/Main.java`, then `java -cp bin main.Main`.

## How to play

The game opens on a title screen; **Play** leads to the map select.

| Map | Difficulty | Notes |
| --- | --- | --- |
| Green Meadow | Easy | Long winding road |
| Dusty Canyon | Normal | Wide switchbacks, monsters 12% tougher |
| Frostbite Pass | Hard | Shorter road, monsters 25% tougher |
| Ember Fields | Brutal | Nearly straight road, monsters 60% tougher |

Your best wave on each map is remembered between sessions.

In a match the field fills the window. Along the bottom sit the next-wave preview, your hand of
six tower cards over the mana bar, and the wave/speed/pause controls.

- **Mana** regenerates constantly (faster each wave) and is the only currency. Unspent mana is
  wasted once the bar is full.
- **Build**: drag a card from the hand onto a free tile, or press `1`-`6` (or click a card) and
  then click tiles. The first wave waits until you press `Space`.
- **Upgrade (tiers 2, 3, 4)**: drag a tower onto an identical tower of the same tier, or select
  it and press `E` (or the Upgrade button on its card) to pull in the nearest twin. The two merge
  into one tower of the next tier: 2.2x damage, more range, stronger effect, one tile. This works
  for base towers and hybrids alike. Tier 4 is the maximum.
- **Fuse**: drag a base tower onto a *different* base tower of the same tier. They fuse into a
  hybrid with a new effect and a trade-off. Press `C` for the fusion chart of every pair.
- Dropping a card on a tier-1 tower builds the new tower straight into it.
- Selecting a tower outlines every tower it can merge with, and its card shows what the next
  tier would give plus the damage and kills it has racked up.
- Hover a card, or a monster in the next-wave preview, for details. Press `H` for in-game help.
- Sell a selected tower with `X` for 60% of what went into it.

| Key | Action |
| --- | --- |
| `1`-`6` | Pick a tower to build |
| `Space` | Start the first wave / call the next one early (bonus mana) |
| `E` | Upgrade the selected tower with its nearest twin |
| `H` | Help |
| `F` | Game speed x1 / x2 / x3 |
| `P` | Pause (you can still build) |
| `X` / `Delete` | Sell selected tower |
| `C` | Fusion chart |
| `Esc` / right-click | Cancel; `Esc` with nothing held opens the pause menu |
| `R` | Restart (from the pause or defeat menu) |

### Base towers

| Tower | Cost | What it does |
| --- | --- | --- |
| Ballista | 40 | Fast single-target bolts, long range |
| Mortar | 60 | Splash damage; cannot hit flyers |
| Frost Obelisk | 50 | Pulse that slows everything in range |
| Tesla Coil | 70 | Chain lightning, ignores armor |
| Venom Spitter | 55 | Poisons its target, spreading shots across unpoisoned enemies |
| Sun Prism | 80 | Constant beam that ramps to 3x damage on a held target |

### Hybrids

| Hybrid | Made from | What it does | Trade-off |
| --- | --- | --- | --- |
| Flak Battery | Ballista + Mortar | Rapid small blasts, +50% vs flyers | Shorter range |
| Glacier Lance | Ballista + Frost | Huge range, freezes its target | Very slow reload |
| Storm Rail | Ballista + Tesla | Beam pierces every enemy in a line | Slow recharge |
| Blight Bow | Ballista + Venom | Three poisoned bolts at three targets | Weak direct hits |
| Giant Slayer | Ballista + Prism | Removes a share of the target's max health | Slow, wasted on small fry |
| Blizzard Mortar | Mortar + Frost | Leaves a slowing, damaging field | Low impact damage, no flyers |
| Plasma Mortar | Mortar + Tesla | Armor-ignoring blast that burns | Short range, no flyers |
| Acid Mortar | Mortar + Venom | Splash strips armor and poisons | Low damage alone, no flyers |
| Cluster Mortar | Mortar + Prism | Shell bursts into four bomblets | Slow reload, no flyers |
| Tempest Spire | Frost + Tesla | Long chains make enemies Brittle (take more damage) | Low damage |
| Miasma Obelisk | Frost + Venom | Pulse slows and poisons everything in range | Short range |
| Aurora Prism | Frost + Prism | Ramping beam that slows its target | One target at a time |
| Plague Coil | Tesla + Venom | Chained poison that spreads when its host dies | Low direct damage |
| Arc Prism | Tesla + Prism | Ramping beam split across three targets | Shorter range |
| Siphon Ray | Venom + Prism | Generates extra mana while it fires | Weak damage |

### Monsters

Slimes split in two when killed. Goblins are fast. Orcs are armored (magic ignores armor). Bats
fly straight to the keep along the dotted trail, ignoring the road. Shamans heal the monsters
around them. Wraiths cannot be slowed or frozen. A Golem boss arrives every fifth wave.

## Architecture

```
src/
  ecs/         World (entities + component stores), Store, Query, GameSystem
  components/  Plain data: Position, Health, Tower, Enemy, PathFollower, Status, Projectile, ...
  systems/     All behaviour, one class per concern
  game/        Game context, tower/enemy/map definitions, themes, waves, prefabs, generated art
  main/        Window and frame loop
```

- **Entities** are int handles (slot index + generation), so a stale reference to a dead enemy
  safely resolves to "not alive".
- **Components** are data only and live in one dense array per type; each entity has a bitmask.
- **Systems** run in a fixed order at a fixed 60 Hz step (`Game.simulation`): waves, economy,
  status effects, movement, healers, spatial index, towers, projectiles, zones, deaths, effects.
  Input, building and rendering run once per frame and keep working while paused.
- Global state that is not an entity (mana, lives, wave, UI selection) lives on `Game`.
- Enemies are indexed in a uniform grid rebuilt each step, so targeting, splash and chain
  lightning only look at nearby cells.
- All art is generated at startup in `game/Assets.java` (ASCII pixel maps for monsters,
  primitives for towers and terrain).

To add a tower, add a constant to `TowerType`, its attack to `TowerSystem.fire` (and
`ProjectileSystem.impact` if it shoots), and its turret art to `Assets.turretArt`.
