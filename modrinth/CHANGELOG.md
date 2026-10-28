# Changelog — BloodMoonPlus

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

<!--
Note: make sure the version number below matches the version you publish on
Modrinth. Lines marked with ⭐ are the most eye-catching features for the
project page.
-->

## [1.0.1] — Global event overhaul: all worlds, fixed night, configurable chat, faster waves

The definitive stability release. The Blood Moon now truly covers the whole server, the
chat can be silenced per category, and the loot/wave timings were tuned with player
feedback. Earlier buggy versions (1.2.0 / 1.2.1) were removed from Modrinth; this
version supersedes them and restores the fixed behaviour of 1.0.0 on top of the new
features.

### Added ⭐
- **`event.global` (default `true`)**: the event runs in **every loaded world at the same
  time** — night, horde waves, boss spawns, boss bar, participants and survivor rewards in
  each world, all started and stopped together. Set it to `false` to use the `worlds` list
  (or the per-slot `worlds` list).
- **Configurable chat announcements** (`announce:` in `config.yml`) ⭐. By default **only
  hordes, bosses and loot** are announced; mob-kill spam ("X slew Zombi"), player death
  messages, event start/end summaries, special events, survivor results and schedule
  warnings are all **off**. Every category has its own `true/false` switch and applies
  with `/luna reload`.
- **First horde 3 seconds after the moon rises**: `event.first-wave-delay-seconds` now
  means *exact* seconds to the first wave. Previously the first wave waited a full extra
  interval (10 s + 25 s = 35 s, and up to a minute if no eligible player was found at that
  tick).
- **`/luna` status shows the countdown** to the next Blood Moon (date, human-readable time
  and difficulty) both when no event is active and while one runs; `/luna next` (alias
  `/luna proxima`) lists the next 6 scheduled moons.

### Changed ⭐
- **Loot buffed** ⭐: horde drop-chance raised from 10% to **60%**, totem from 15% to
  **45%**, netherite scrap from 18% to **40%**, diamond 50% → 55%, ender pearl 45% → 50%,
  iron/bone 2-5 / 2-6 → **3-7**, and the rest of the common/poco-comun table up
  (gold 95%, golden apple 90%, XP bottle 90%). See `DROPS.md` for the full table.
- **Rare-or-higher loot goes straight to the inventory**: RARE, EPIC, LEGENDARY and
  MYTHIC drops are delivered to the killer; if the inventory is full the remainder falls
  **at the player's feet** (legendary/mythic items glow and are immune to lava and
  explosions).
- **Default difficulty is x1** in the config, in manual starts and in scheduler warnings.
- **Lighter spawning under load**: the per-wave cap shrinks when many players or many
  event mobs are already nearby, avoiding lag on modded servers with 20 players (the
  reason the old build felt heavy).

### Fixed ⭐
- **The event is global for real**: night (`lock-night`) was only applied to the world
  that ran the start command, and the horde/boss tick only drove a single "global"
  session, so other worlds got the announcements but never the night, the mobs, the bar
  or the participants. Each world now has its own session with its own night, waves, bar
  and participants, started and ended together.
- **Event survives a server restart**: the active event is stored per world name in
  `data.yml`; the intermediate versions stored a single `global` key that was silently
  dropped on boot, cancelling events in progress.
- **`/luna stop <world>` stops only that world**; `/luna stop` and `/luna stop all` stop
  the whole event.
- **Commands work on every world of the running event**: `/luna difficulty`, `/luna boss`,
  `/luna horde` and `/luna killmobs` iterate over all sessions instead of a single global
  one.
- **No `NullPointerException` when loot has no killer**: with
  `loot.require-player-kill: false`, a rare-or-higher drop used to crash
  `EntityDeathEvent` after vanilla drops had been cleared, so the item vanished and the
  console filled with stack traces. The item now falls on the ground.
- **Damage re-entrancy guard**: damage applied by the plugin itself (lightning, chain
  lightning, bleed) no longer re-enters the combat listener and re-triggers weapon
  abilities recursively.
- **Silent loot failures now warn once in the console**: an item pointing to an unknown
  `rarity` used to be dropped from the roll without any warning (less loot, no reason
  shown), and a typo in a `loot-table` made the table return zero items on every kill.
  Both cases now log a single clear warning, and `loot.yml` is loaded **before** `items.yml`
  so the rarity validation runs with the right data.
- **Historical stats record the real world and the last boss defeated**, once per event
  (aggregated across worlds) instead of once per world.
- **Session cleanup**: a world unloaded mid-event no longer leaves an orphan session or a
  stale `active` entry in `data.yml`.
- **`schedule.skip-if-no-players` works as documented**: with nobody online the scheduled
  event is skipped instead of starting on an empty server.
- **Peaceful-difficulty compatibility** (no longer crashes).
- **Death messages**: vanilla death messages (with `§` codes) used to break MiniMessage;
  they are converted from legacy with a fallback that never throws.

### Performance
- Whole-world scans removed from wave spawning: nearby event mobs are counted with
  `getNearbyEntities` around each player, cheaper with 40+ players and other mods
  spawning entities.
- Lighter ambience: less frequent thunder, ambient sounds and red fog particles; visual
  lightning and particle bursts on every 5th mob of a wave instead of every 3rd.

### Compatibility
- Paper **26.2** and **26.3**. Built with Java 25.

## [1.0.0] — First public release

First official release. A complete Blood Moon plugin that improves on and goes
beyond *BloodMoon-Reborn 1.1.1*.

### Added ⭐
- **Fixed schedules** in Mexico time with two modes: `weekly` (weekday + real time) and `interval` (every N hours). Multiple runs on the same day.
- **9 bosses** with health bars, minimum difficulty, **enrage** phases, abilities and their own loot.
- **45 items** by rarity: common (guaranteed drop), rare, epic, legendary and mythic (~0.1%) ⭐.
- **Custom enchantments** up to **level 7** plus unique abilities: crit, thunder guard, blast wave, levitate and bleeding ⭐.
- **Progressive waves**: 5 → 10 → 15 → … with a configurable cap ⭐.
- **Immersion**: red fog, lightning when hordes/bosses spawn or a player dies, ambient sounds and constant announcements. No rain, but thunder ⭐.
- **12 hostile mob types** and progressive hordes.
- **Stats & survivors**: rankings, damage MVP, who survived.
- **Placeholders** (`%bloodmoon_*%`): status, time left, next moon, difficulty, bosses, wave, stats… via PlaceholderAPI.
- **Internationalization (i18n)** and **per-world configuration**.
- **Wither control**: the normal Wither is blocked at the plugin level + an optional **datapack** (`BloodMoonWitherControl`) that removes any Wither that is not the boss.
- **Terrain protection** during the event, with bosses exempted.

### Changed
- Difficulty is now a **continuous multiplier** (0.1 – 10.0, default 1.0) with 7 scaling factors.
- "Broken" rare/legendary/mythic items now drop at a **5%** chance.
- Legendary items include **`vanishing_curse`**.

### Fixed
- **Massive `PlayerDeathEvent` error**: vanilla death messages (with `§` codes) broke MiniMessage. They are now converted from *legacy* to *MiniMessage* with a fallback that never throws.
- **Hordes stopping** after a player died: the "currently dead" state is now separated from the "died at least once" state, so hordes resume on respawn.
- **Peaceful-difficulty compatibility** (no longer crashes).

### Performance
- Removed whole-world scans and per-hit parsing.
- Local entity scans and item-parse caching.
- Optimized placeholder snapshots and more efficient action bars. Designed for **40+ players**.

### Compatibility
- Paper **26.2** and **26.3**. Built with Java 25.

## [1.2.1] — The global event, done right + crash fixes

> ⚠️ **Withdrawn.** Versions 1.2.0 and 1.2.1 were removed from Modrinth: their version
> numbers created confusion and the event-global work was not complete. Their fixes are
> all included in 1.0.1 (see above).

A patch that makes the global Blood Moon actually global, restores two behaviours that
1.2.0 broke, and fixes two crash/abuse paths in loot and combat.

### Fixed ⭐
- **The global event now spans every world**: each world gets its own session (night,
horde waves, boss bar, bosses, participants and survivor rewards), all started and ended
together by `/luna start`, `/luna stop` and the scheduler.
- **The event survives a server restart again**: the active event is stored per world name.
- **`schedule.skip-if-no-players` works as documented again**.
- **No more `NullPointerException` when loot has no killer**.
- **Damage re-entrancy guard restored**.
- **Historical stats record the real world and the last boss defeated**, once per event.
- **Session cleanup**: a world unloaded mid-event no longer leaves an orphan session.
- `/luna stop <world>` stops only that world; `/luna stop` / `all` stop the whole event.

### Changed ⭐
- **Bleed damage uses the item's configured value** (`BLEED:prob:seg:daño`).
- **`loot.direct-to-inventory` works again**: `true` sends every drop to the killer's
  inventory; `false` (default) sends only rare-or-higher drops.
- Difficulty, boss, horde and cleanup commands apply to **all** worlds of the running event.

### Added
- **`event.global`** (default `true`): the event covers **every loaded world** at once.

### Compatibility
- Paper **26.2** and **26.3**. Built with Java 25.

## [1.2.0] — Stability, loot delivery and lighter spawns

> ⚠️ **Withdrawn.** See the note on 1.2.1 — superseded by 1.0.1.

A stability and gameplay-feel release aimed at modded servers with 20 players.

### Fixed
- Fixed outstanding compilation errors in `Keys`, `DataStore`, `EventManager` and
  `BloodMoonPlus`.
- Fixed the case where a rare item missing rarity metadata could still be left on the
  ground instead of being delivered to the player.

### Added ⭐
- **Global Blood Moon session**: `/luna start` starts the event for the whole server.
- **Direct inventory delivery for rare-or-higher loot**.
- **Lighter wave spawning under load** (adaptive per-wave cap).
- **Default difficulty x1** in config, manual starts and scheduler warnings.
- **Loot buffed**: table drop-chance 10% → 60%, totem 15% → 45%, netherite scrap
  18% → 40%, and higher chances across the common/poco-comun rows.

### Changed
- Less frequent thunder, ambient sounds and red fog particles during the event.
- Visual lightning and particle burst use every 5th mob instead of every 3rd.

### Compatibility
- Paper **26.2** and **26.3**. Built with Java 25.
