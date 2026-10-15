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
