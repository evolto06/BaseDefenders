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
