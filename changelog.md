# Changelog

## 1.0.0 — Initial commit
- Initial project scaffold and README.
- Added basic ECS skeleton:
    - Entities: Entity, EntityManager
    - Components: placeholder package
    - Systems: System stub
- Core application UI:
    - Main launcher, Game, GamePanel (window + rendering panel setup)
- Project metadata:
    - .gitignore
    - README.md

## 1.0.1 — Initial Engine
- Trial and error for ECS Engine
    - Entity creation via Entity Manager seems fine for now
    - Rendering system needs fixing
- Relearning Java Graphics Libraries
- Renamed src folders to appropriate Java convention
- Reusing old project sprite for testing purposes

## 1.0.2 — Initial Engine
- Trial and error for ECS Engine
    - Entity creation via Entity Manager seems fine for now
    - Rendering system fixed
- Component API buggy, getting component (BufferedImage) returns null

## 1.0.3 — Initial Engine
- Deleted most of the systems and initial gamepanel
- - the architectural structure i chose is too complicated 
- - will rebuild from scratch
- Debugged entity creation, for now Position is working
## 1.0.4 - Initial Engine 
- From now own I will work on 1 aspect of the game at a time
- Sprite Component now properly loads sprites
- - Scaling test is not yet possible due to lack of rendering system
- Next on the chopping block will be the rendering system rebuilt



### Known issues / limitations
- No game loop, input handling, or concrete components/systems implemented yet.
- No tests or build scripts included.
- Entities currently use runtime-type mapping for components — consider component IDs or bitmasks for performant queries later.

## 2.0.0 — Playable rewrite
- Rewrote the ECS core (`ecs/`): generational int entity handles, dense per-type component
  stores with bitmask queries, deferred destruction.
- Full tower defense game: regenerating mana, 4 base towers, tier 1-4 merge upgrades,
  6 fusion hybrids, 8 monster types, 30 generated waves plus endless mode.
- Fixed-step simulation with a spatial grid for range queries; BufferStrategy rendering.
- All art generated in code; `res/` is no longer used.
- Old skeleton moved to `legacy/`.

## 2.1.0 — More towers, better looks, balance pass
- Fixed: dragging two towers together did nothing while a shop tower was picked, which blocked
  hybrid upgrades and tiers 3-4. Dragging now always merges; a plain click still builds onto a tower.
- Two new base towers (Venom Spitter, Sun Prism) and nine new hybrids: 21 tower kinds in all.
- New mechanics: poison, armor shred, ramping beams, contagious plague, percent-health damage,
  cluster bomblets, mana siphon.
- Graphics: textured terrain, tier-coloured tower platforms, hit flashes, squash/flap animation,
  glowing beams and lightning, explosions, portal swirl, vignette, restyled HUD with fusion chart.
- Balance: per-tower damage-per-mana pass, gentler late-wave health curve, smaller bat and shaman
  floods, swarm size cap.

## 2.2.0 — UI/UX pass
- The first wave waits for the player; contextual tips explain building, upgrading, fusing,
  flyers and bosses the first time each comes up.
- One-click Upgrade button and `E` key merge the nearest twin into the selected tower.
- Tower card shows next-tier stats, damage dealt and kills. Hovering a tower previews its range.
- Wave preview tooltips, monsters-left counter, next-wave countdown bar, early-call bonus on the button.
- Feedback: mana bar flashes when you cannot afford something, the field flashes when a monster
  gets through, bosses get a health bar, hand cursor over anything clickable.
- Help screen (`H` or the `?` button) and a proper victory screen.

## 2.3.0 — Start screen, maps, bottom card bar
- Title screen and a map select with four themed maps (meadow, desert, snow, ember), each with
  its own road and monster toughness. Best wave per map is saved.
- The side panel is gone. The field is now 24x10 tiles across the full window, with a bottom bar:
  next-wave preview, a hand of six tower cards over a segmented mana bar, and the controls.
- Cards can be dragged onto the field to build; unaffordable cards show a wipe that drains as
  mana fills.
- Tower details float over the field corner; the fusion chart is an overlay (`C`).
- Pause menu (Resume / Restart / Change map), also used as the defeat screen.

## 2.4.0 — Tower art
- Every one of the 21 towers has its own hand-drawn sprite; no two share a shape any more.
  Turning turrets are seen from above (ballista, mortar, viper head, twin flak guns, rail gun...),
  standing towers from the front (ice shards, coil, sun lens, storm cloud, rune monolith, skull...).
- Turning turrets sit on a turntable, standing towers on a dais.
- Standing towers fire from their emitter instead of the tile centre: lightning leaves the coil's
  ball, beams leave the lens, and the Arc Prism throws one beam from each of its three lenses.

## 2.5.0 — Hexes, more monsters, more bosses
- New base tower, the Hex Totem, and six hybrids: Witch Hunter, Doom Mortar, Dread Obelisk,
  Reaper Coil, Rot Spitter and Soul Prism. 28 tower kinds in all; the hand holds seven cards
  (`1`-`7`) and the fusion chart is 7x7.
- Hex: a hexed monster cannot be healed and loses its powers (healing, regrowth, drumming,
  summoning, wards, slow immunity, splitting). Bosses shake it off in half the time.
- New mechanics: doom (the doomed explode on death for a share of their health), fear (monsters
  flee back down the road), execute, rot (healing hurts instead), heat that survives a kill,
  and towers that pick out healers and other casters first.
- Five new monsters: Troll (regenerates), Rune Knight (resists magic), Gargoyle (armored flyer),
  War Drummer (speeds others up) and Necromancer (raises Skeletons).
- Three new bosses: Dragon (flies), Lich (heals, summons, resists magic) and Hydra (regenerates,
  splits). Boss waves rotate through them and wave 30 fields all four.
- Healing no longer stacks without limit: a monster that was just mended ignores every healer
  for 1.5 seconds, and healing pulses restore a quarter as much on bosses.
- The wave preview narrows its entries to fit up to seven kinds of monster; hexed monsters are
  marked with a violet cross; the boss bar names the boss on the field.

## 2.6.0 — Performance
- Found the cause of the slow frames: on machines where Java disables Direct3D (most Intel
  graphics) everything is painted in software, and at a fractional display scale such as 125%
  every sprite was being smoothed and every shape antialiased. A busy endless frame took about
  76 ms; it now takes about 6.
- The field is drawn at its own size off screen and scaled to the window in one pass. At a
  fractional scale it is smoothed only when the graphics card can do it; otherwise the pixels
  are simply enlarged.
- Terrain, the vignette and tower platforms are a cached ground layer. Shadows, glows, flashes,
  turret headings and monster squash frames are pre-painted stamps. Beams, lightning, rings and
  sparks are blended directly into the pixels.
- The status bar, bottom bar, hand cards and the selected tower's card are cached and repainted
  about fifteen times a second instead of every frame.
- Drawing no longer produces megabytes of garbage a second, so the collector pauses a few times
  a minute instead of every few seconds.
- Frames are paced to a steady beat, so one slow frame or overlong sleep no longer drags the
  rate down. The menus and the fusion chart are several times faster too.

## 2.7.0 — Monster art
- The four bosses are redrawn at the same pixel size as everything else instead of being a small
  picture blown up three times, so they carry far more detail at the same size on screen.
- The Dragon is seen from the side, with a long neck, horns, a spade-tipped tail and a wing that
  beats: it is the first monster with drawn animation frames. It is wider than it is tall.
- The Hydra has three snake heads on separate necks over a coiled body, and the Hydra Spawn is
  its own single-headed hatchling rather than a shrunken Hydra.
- The Golem is a hulk of mossy boulders with a molten crack; the Lich carries a staff and orb.
- The Gargoyle crouches under wings arched overhead, so it no longer reads as a grey Bat, and the
  Wraith is a hooded spectre with two cold lights instead of a plain ghost.

## 2.8.0 — Monsters that answer the freeze
- Freezing everything with Glacier Lances is no longer a safe plan: five new monsters each punish
  it in a different way.
- Aegis Warden (all maps, from wave 6): shields the monsters around it every five seconds. A
  shield swallows the next hit whole, freeze and all, so slow heavy shots are wasted on it while
  rapid fire, chains, pulses and damage over time break it. A hex silences it, and Witch Hunters
  and Hex Totems go for it first.
- Each map now has a resident monster, met only there. It arrives on wave 8 and leads a group
  every other wave after that:
    - Green Meadow, Blink Imp: blinks ahead down the road when frozen or hit hard.
    - Dusty Canyon, Dune Borer: burrows under the road, where nothing can touch it, and comes
      up thawed.
    - Frostbite Pass, Yeti: slows and freezes mend it instead.
    - Ember Fields, Salamander: fireproof, thaws from a freeze in a quarter of the time and
      sprints off, and slows only half hold it.
- A hex switches every one of these powers off.
- Tips introduce the Warden and each map's resident the wave before they arrive.
- In a test with a fixed defence of fourteen tier-3 towers, an all-Glacier-Lance build now falls
  up to five waves sooner, while a mixed build does as well as before.

## 2.9.0 — Map bosses
- Each map now has a boss of its own. It leads wave 25, which used to bring the Golem back, and
  joins the final wave beside the four shared bosses:
    - Green Meadow, Elder Treant: armored bark, and roots that tangle the two towers nearest
      it so they cannot fire for a few seconds.
    - Dusty Canyon, Sand Wyrm: burrows out of reach like a Dune Borer and calls up Borers.
    - Frostbite Pass, Frost Giant: cold mends it, and its breath halves the fire rate of
      every tower near it.
    - Ember Fields, Phoenix: flies, is fireproof, and rises again at half strength unless it
      dies hexed.
- Wave 30 fields all five bosses, 14 seconds apart instead of 6, so the finale is a gauntlet
  rather than a single crush.
- Tangled towers are bound with vines and chilled towers ringed with frost. A tip warns of each
  map boss the wave before it arrives.
