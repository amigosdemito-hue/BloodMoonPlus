# Changelog — BloodMoonPlus

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

<!--
Note: make sure the version number below matches the version you publish on
Modrinth. Lines marked with ⭐ are the most eye-catching features for the
project page.
-->

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

## [1.2.0] — Stability, loot delivery and lighter spawns

A stability and gameplay-feel release aimed at modded servers with 20 players, with
compilation errors fixed and a better experience for rare drops.

### Fixed
- Fixed outstanding compilation errors in `Keys`, `DataStore`, `EventManager` and
  `BloodMoonPlus`, including the `cfg` field that prevented reloading `data.yml` in memory.
- Fixed the case where a rare item missing rarity metadata could still be left on the ground
  instead of being delivered to the player.

### Added
- **Direct inventory delivery for rare-or-higher loot**: RARE, EPIC, LEGENDARY and
  MYTHIC items now go to the killer's inventory when there is a killer, instead of being
  dropped on the ground.
- **Lighter wave spawning under load**: the plugin lowers the per-wave cap when there are
  many eligible players or many already-alive mobs nearby, to avoid flooding the world on
  servers where other mods also spawn entities.

### Changed
- Less frequent thunder, ambient sounds and red fog particles during the event, to reduce
  noise and entity tick load with mods and 20 players.
- Visual lightning and particle burst use every 5th mob instead of every 3rd mob in waves.
- Clean up stale ambient passive potion effects on player respawn.
- Bleed damage now uses the item's configured value instead of a fixed 1.0.

### Added ⭐
- **Evento global**: la Luna de Sangre ahora se inicia en todos los mundos del servidor
  a la vez, en vez de por mundo. `/luna start` inicia el evento en todos los mundos;
  `/luna stop` lo detiene en todos. Los comandos de dificultad, jefe, horda y limpieza
  también operan sobre la sesión global.
- **Dificultad por defecto 1** en todas las funciones del evento: el config, los
  inicios manuales y los avisos del scheduler usan dificultad 1 cuando no se especifica.
- **Loot más alto**: drop-chance de la horda subida de 10% a 60%. Hierro y hueso pasan
  de 2-5 / 2-6 a 3-7. Oro, manzana dorada, botella XP, ojo de ender y obsidiana suben
  sus probabilidades. Totem pasa de 15% a 45%, fragmento de netherita de 18% a 40%,
  diamante de 50% a 55%, perla de ender de 45% a 50%. Materiales épicos y legendarios
  también suben ligeramente (totem y netherita son los más notables).

### Compatibility
- Paper **26.2** and **26.3**. Built with Java 25.
