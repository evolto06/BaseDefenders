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
seven tower cards over the mana bar, and the wave/speed/pause controls.

- **Mana** regenerates constantly (faster each wave) and is the only currency. Unspent mana is
  wasted once the bar is full.
- **Build**: drag a card from the hand onto a free tile, or press `1`-`7` (or click a card) and
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
| `1`-`7` | Pick a tower to build |
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
| Hex Totem | 50 | Hexes its target, healers and other casters first: no healing, no powers |

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
| Witch Hunter | Ballista + Hex | Shoots healers, drummers and summoners first for triple damage, and hexes | Slow reload, ordinary against the rest |
| Doom Mortar | Mortar + Hex | Blast hexes and dooms: the doomed explode when they die | Weak blast of its own, no flyers |
| Dread Obelisk | Frost + Hex | Pulse hexes everything in range and sends it fleeing back down the road | Almost no damage, bosses hardly flinch |
| Reaper Coil | Tesla + Hex | Hexing chain lightning that executes badly wounded monsters | Weaker arcs |
| Rot Spitter | Venom + Hex | Rot turns healing and regeneration into damage | Only a poison dart when nothing heals |
| Soul Prism | Prism + Hex | Hexing beam that keeps its heat when its target dies | Weaker beam |

### Monsters

| Monster | From wave | What it does |
| --- | --- | --- |
| Slime | 1 | Splits in two when killed |
| Goblin | 2 | Fast, comes in swarms |
| Orc | 3 | Armored: physical hits are reduced (magic ignores armor) |
| Bat | 4 | Flies straight to the keep along the dotted trail |
| Aegis Warden | 6 | Shields the monsters around it; a shield swallows the next hit whole, freeze and all |
| Shaman | 7 | Heals the monsters around it |
| Wraith | 9 | Cannot be slowed or frozen |
| Troll | 11 | Regrows its health as it walks |
| Rune Knight | 13 | Warded: magic deals 60% less |
| Gargoyle | 16 | Flies, and is armored |
| War Drummer | 18 | Speeds up every monster around it |
| Necromancer | 21 | Raises Skeletons as it walks |

Each map also has a resident monster that is met nowhere else. It arrives on wave 8 and leads a
group every other wave after that, so every map asks its own question of your defence. All four
punish a defence built on freezing everything with Glacier Lances.

| Map | Resident | What it does |
| --- | --- | --- |
| Green Meadow | Blink Imp | Blinks ahead down the road when frozen or hit hard, shaking off the ice |
| Dusty Canyon | Dune Borer | Burrows under the road for stretches, untouchable, and comes up thawed |
| Frostbite Pass | Yeti | Slows and freezes slide off it and mend it instead |
| Ember Fields | Salamander | Fireproof; thaws from a freeze in a quarter of the time and sprints off, and slows only half hold it |

A boss wave comes every fifth wave, led by the next boss in line and backed by the ones before
it; wave 30 fields all four.

| Boss | First seen | What it does |
| --- | --- | --- |
| Golem | 5 | Heavy armor, shrugs off slows |
| Dragon | 10 | Flies straight for the keep, out of reach of Mortars |
| Lich | 15 | Mends the monsters around it, raises Skeletons, resists magic |
| Hydra | 20 | Regrows its health and splits into three Hydra Spawn when slain |

### Hexes and healing

A **hexed** monster cannot be healed and loses its powers: Shamans and Liches stop healing,
Trolls and Hydras stop regrowing, Drummers, Wardens and summoners fall silent, Rune Knights lose
their ward, Wraiths can be slowed, Imps cannot blink, Borers cannot dig, Yetis and Salamanders
feel the cold like anyone else, and Slimes and Hydras die without splitting. Bosses shake a hex off
in half the time. The Hex Totem and five of its hybrids apply it; the Rot Spitter instead makes
healing hurt its victim.

Healers do not stack without limit: a monster that was just mended ignores every healer for the
next 1.5 seconds, and a healing pulse restores only a quarter as much on a boss.

A **shield** takes one hit and everything riding on it (damage, freeze, slow, hex). Poison, burns
and blizzard fields seep through without breaking it, so rapid fire, chain lightning, pulses and
damage over time beat Wardens; a single heavy shot is wasted on them.

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
  status effects, blinks, burrows, movement, healers, drummers, summoners, shields, spatial
  index, towers, projectiles, zones, deaths, effects.
  Input, building and rendering run once per frame and keep working while paused.
- Global state that is not an entity (mana, lives, wave, UI selection) lives on `Game`.
- Enemies are indexed in a uniform grid rebuilt each step, so targeting, splash and chain
  lightning only look at nearby cells. A burrowed monster is left out of it, which is all it
  takes to make it untouchable.
- All art is generated at startup in `game/Assets.java` (ASCII pixel maps for monsters,
  primitives for terrain) and `game/TowerArt.java` (one pixel map per tower).
- Drawing is built to stay fast without the graphics card, because Java turns its Direct3D
  pipeline off on many laptops and then paints everything in software:
  - The field is drawn at its own size into an off-screen picture and copied to the window in
    one pass (`RenderSystem`). Text and build overlays go straight to the screen, so they stay
    sharp at any window or display scale.
  - Terrain and tower platforms sit in a ground layer that is repainted only when a tower is
    built, merged or sold.
  - Shadows, glows, explosion flashes, turret headings and the squash of a walking monster are
    pictures painted once and stamped (`Stamps`); beams, lightning, rings and sparks are
    blended straight into the pixels (`Pixels`). Neither allocates while the game runs, which
    keeps the garbage collector from pausing the frame.
  - The status bar, the bottom bar and the selected tower's card are painted into pictures and
    repainted a few times a second, or at once when the pointer moves onto something
    (`HudSystem`).

To add a tower, add a constant to `TowerType`, its attack to `TowerSystem.fire` (and
`ProjectileSystem.impact` if it shoots), and its sprite to `TowerArt.turret`. A tower that
turns is drawn from above pointing right (top half only, it is mirrored); one that stands
still is drawn from the front, with `TowerType.emitterLift` saying where its shots start.
A new base tower needs a hybrid for every other base; the hand, the number keys and the fusion
chart size themselves from `TowerType.BASES`.

To add a monster, add a constant to `EnemyType` and its sprite to `Assets.enemyArt`, give it a
power by attaching a component in `Prefabs.enemy`, and list it in `WavePlan.UNLOCK_ORDER` (or
`EnemyType.BOSSES`, or make it a map's `MapDef.resident`). Powers should check `Combat.hexed` so a hex can switch them off. A sprite
is drawn facing right, at any width and height, and shown at `EnemyType.scale` times its size.
Most monsters are one picture, brought to life by squashing it; one that needs real frames, as
the Dragon does for its wingbeat, returns them from `Assets.frameArt`.
