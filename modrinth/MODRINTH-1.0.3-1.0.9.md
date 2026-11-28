# BloodMoonPlus — versiones pendientes en Modrinth (1.0.3-beta → 1.0.9-beta)

Modrinth tiene publicadas **1.0.0**, **1.0.1** y **1.0.2-beta**. Faltan estas siete versiones,
todas con `loaders: paper` y `game_versions: [26.2, 26.3]`.

| Versión | Canal | Jar | Bytes | SHA-256 |
|---|---|---|---|---|
| `1.0.3-beta` | beta | `BloodMoonPlus-1.0.3-beta.jar` | 227949 | `65738cc88ce7c35f93e686d59e4e9d4040426f1b68cb8c6a05fd34055bcd35a2` |
| `1.0.4-beta` | beta | `BloodMoonPlus-1.0.4-beta.jar` | 233878 | `c6df5a0348638d8337d677a49b11cb76932bb082099a28b47db79e3e083508b5` |
| `1.0.5-beta` | beta | `BloodMoonPlus-1.0.5-beta.jar` | 234932 | `4c35ef11e46779bda293f504e9eb770b44f9d81dd1c17fe6b17d91da001eaee7` |
| `1.0.6-beta` | beta | `BloodMoonPlus-1.0.6-beta.jar` | 235178 | `55588974cb99822c761a7f28b802c599b7bd56e13cec8df4be15ac9bf195b04c` |
| `1.0.7-beta` | beta | `BloodMoonPlus-1.0.7-beta.jar` | 235312 | `386611cf1d40c9cdc491a50cb4dd18e70e59085474619fccb51483a9d3348797` |
| `1.0.8-beta` | beta | `BloodMoonPlus-1.0.8-beta.jar` | 237623 | `22e0d8d031a5db9c12357b281d7858914c431a75fd33ddebaef21399ca8d5a61` |
| `1.0.9-beta` | beta | `BloodMoonPlus-1.0.9-beta.jar` | 260081 | `4bf90c128059ae6c8953a3b57b5bccefdc10aea50e03de06dca9bbaa2bbc5a6a` |

Cada sección de abajo es el changelog exacto de esa versión, listo para pegar en el campo
**Changelog** del formulario de Modrinth (`upload.ps1` lo toma solo de `modrinth/CHANGELOG.md`).

---

## 1.0.3-beta

Moon gear no longer carries above-vanilla enchant levels or the plugin's own item abilities. Every
weapon, tool and armour piece now ships **vanilla enchantments at their maximum** plus
[ExcellentEnchants](https://nightexpressdev.com/excellentenchants/) enchantments, with that plugin's
own per-level chance settings. Legendary and mythic gear raises the vanilla cap to **5** (Protection
5, Thorns 5...). Bosses are untouched: their LIGHTNING, AURA, SUMMON, SWARM and EXPLODE abilities work
exactly as before.

**Highlights**
- ⭐ **No curses on Moon gear**: the automatic **Curse of Vanishing** is gone for every rarity and
  material, so a rare sword or a mythic chestplate drops on death like anything else. Gear already
  handed out loses the curse on join, on pickup and with `/luna refresh`, and no item definition
  carries a curse any more.
- ⭐ **No more Sharpness 7 or Protection 7**: every enchantment is trimmed to its maximum
  (Sharpness 5, Unbreaking 3, Looting 3...), with **level 5 vanilla enchantments on legendary and
  mythic gear** (Protection 5, Thorns 5...).
- ⭐ **Lightning comes from ExcellentEnchants**: `thunder` (weapons) and `electrified_arrows` (bows)
  use EE's probability manager, configured in `plugins/ExcellentEnchants/enchants/`.
- **The 19 custom item abilities are gone** (crit, lifesteal, thunder guard, blast wave, levitate,
  bleed...) and are replaced by EE enchantments: `vampire`, `double_strike`, `venom`, `wither`,
  `ice_aspect`, `fire_shield`, `stopping_force`, `dragon_heart`, `hardened` and friends.
- **Existing gear is migrated** on join (`items.migrate-on-join`), **the moment it is picked up or
  taken out of a chest**, and whenever you run `/luna refresh <player|all>`. Above-max levels are
  lowered, missing enchantments are added, the Curse of Vanishing is stripped and the old ability
  data is removed.
- **ExcellentEnchants is optional to install, not to remove**: without it (and NightCore) the plugin
  still starts, **new** items get just the vanilla enchantments and the console warns once. Do not
  uninstall EE with gear already handed out: Minecraft deletes items whose enchantment is missing, so
  run `/luna refresh <player|all>` with EE installed first if you ever need to drop it.

### Updating from 1.0.2 — your `items.yml` is NOT replaced

The plugin only writes `items.yml` when the file does not exist, so an updated server keeps its old
copy (Protection 4 and the removed ability lists). The refresh applies **that** file, so
`/luna refresh` would leave Protection 4 in place.

1. Stop the server and back up `plugins/BloodMoonPlus/items.yml` (the plugin also leaves
   `items.yml.bak`).
2. Replace it with the `items.yml` shipped inside the jar — or edit it by hand: at least
   `protection: 5` (plus Thorns 5 and friends) on the `LEGENDARIO`/`MITICO` pieces and the
   ExcellentEnchants entries.
3. Start with **ExcellentEnchants and NightCore installed** and check the console:
   `Items cargados: 45` and no unknown-enchantment warning.
4. Run `/luna refresh all` (or let players reconnect/pick their gear up). That is the step that
   turns an already-handed-out Protection 4 chestplate into Protection 5. Without EE installed,
   Minecraft deletes items that carry `excellentenchants:*`, so refresh before removing it.

`config.yml` only gains `items.migrate-on-join` (default `true`).

### Fixed
- Gear with `glow: true` no longer breaks the refresh: building/refreshing it threw
  `IllegalStateException: We don't have enchantment_glint_override!` because
  `hasEnchantmentGlintOverride()` was never checked. `/luna refresh` works on every item again.
- Console noise during an event: global announcements no longer go through `Bukkit.broadcast`, and
  each world's start/stop is a single info line. Every other line in an event log is Paper logging
  each named-mob death (`settings.log-named-deaths` in `spigot.yml`); set it to `false` for a short
  log.
- The grindstone now only cancels the result when it would really strip enchantments, so repairing
  a Moon piece that has none works again.- `%bloodmoon_total_bosses_spawned%` counts every boss (it sat behind the `announce` gate and
  skipped `announce: false` bosses) and `%bloodmoon_boss_type%` reports `MYTHICMOBS` for Mythic
  bosses instead of always `VANILLA`.

### Removed
- The item ability engine (`ItemAbilities`) and the `abilities`/`passives` fields of `items.yml`.
- `settings.max-enchant-level` (the old level-7 cap). Vanilla caps now apply per enchantment.
- The automatic **Curse of Vanishing** and `settings.vanishing-curse-min-rarity`: the key is no
  longer read, so an old `items.yml` can keep it without effect (or delete it).
- The `%bloodmoon_abilities_count%` placeholder.

### Changed
- `items.yml` enchantments rewritten: vanilla at max + up to 3 EE enchantments per item.
- The unbreakable flag and serial numbers survive the migration, and anything still carrying the
  Curse of Vanishing is cleaned up; old passive potion effects (infinite duration) are cleared when
  a player joins.
- Event and boss mobs no longer vanish when players move away: they are marked as not despawnable
  **and** saved with their chunk, so they survive both the distance despawn and the chunk being
  unloaded. `Entity#setPersistent` only controls saving, so on its own it was not enough, and
  `Mob#setPersistenceRequired` does not exist on 26.1/26.2; the mobs use the compatible call. The
  event end, `/luna stop` and the startup cleanup still remove them.
- Enchantment lookup goes through the live registry (`RegistryAccess`) instead of the deprecated
  `Registry.ENCHANTMENT` field, so ExcellentEnchants enchantments are always current.

### Added
- `items.migrate-on-join` in `config.yml`.
- Automatic `items.yml.bak` / `bosses.yml.bak` copies before the first upgrade.

---

## 1.0.4-beta

The Discord webhook no longer posts a line of text: every alert is now an **embed** with a title, a
short atmospheric line and the data laid out in fields. The countdown also stops firing while an
event is already running.

**Highlights**
- ⭐ **Embed alerts**: each webhook message carries a coloured embed (deep wine → crimson → bright
  red as the moon approaches, amber for the dawn, grey for a manual stop), inline fields for
  countdown, start time, difficulty, world, duration and result, and a `BloodMoonPlus · …` footer.
- ⭐ **Live Discord timestamps** (`<t:…:R>` / `<t:…:f>`): the countdown and the end time tick by
  themselves and are shown in each reader's own language.
- ⭐ **No countdown while the moon is up**: a scheduled warning that fired mid-event used to
  announce the next moon to players standing inside the current one.

### Added
- 14 new `discord.*` keys in all seven locales. The four old one-line strings are now the embed's
  flavour text, so existing locale files keep working (missing keys fall back to the jar).

### Fixed
- The scheduled warning now uses the exact slot it belongs to for its start time, instead of
  always taking the next upcoming moon.

### Compatibility
- Paper **26.2** and **26.3**. Built with Java 25. Jar: `BloodMoonPlus-1.0.4-beta.jar`.

---

## 1.0.5-beta

When the moon rose in several worlds at once, the Discord start embed named only the first one,
while the end embed said `global`.

### Fixed
- ⭐ The start embed's **World** field now says `global` when the event started in more than one
  world, exactly like the end alert; a single-world start still shows that world's name.
- The start alert is emitted once per batch from `startIn()`, after the worlds have actually
  started, instead of from the first world's start path, so it also covers the real number of
  worlds that came up.

### Compatibility
- Paper **26.2** and **26.3**. Built with Java 25. Jar: `BloodMoonPlus-1.0.5-beta.jar`.

---

## 1.0.6-beta

Mobs from an ended Blood Moon kept attacking, even with the event off, because the cleanup could
not reach the ones stored in chunks that were not loaded.

### Fixed
- ⭐ **No leftovers in unloaded chunks.** When the event ends, every world it ran in is flagged, and
  the first time one of its chunks loads, the mobs tagged by the event inside it are removed. Before,
  cleanup only walked the entities of loaded chunks, so a mob (or a boss minion) standing far away
  survived the end of the moon and came back to attack later, with no event running.
- ⭐ **Bosses are matched by world name** when the event stops. A session could still hold a `World`
  object from before the world was reloaded, and the old object comparison missed its bosses, which
  then stayed alive next to the players.
- **Two extra cleanup passes** (2 s and 10 s after the end) catch minions a boss had already queued
  to summon and that were born after the immediate cleanup.
- **Events that expired while the server was offline**, and worlds unloaded mid-event, are now
  flagged too, so their leftovers are swept when the world comes back.
- Cleanup no longer ignores `event.remove-mobs-on-end: false`: bosses are always retired, the horde
  is only removed when the option is on, as documented.
- The per-player spawn quota is released for mobs removed by the chunk sweep, so a player does not
  keep a used-up quota for mobs that no longer exist.

### Compatibility
- Paper **26.2** and **26.3**. Built with Java 25. Jar: `BloodMoonPlus-1.0.6-beta.jar`.

---

## 1.0.7-beta

One player alone could end up with hundreds of mobs and a flood of wooden and leather drops: the
wave grew with every horde, boss summons had no per-player ceiling, and every kill rolled one or
two items.

### Changed
- ⭐ **Fixed horde size: 7 mobs per horde.** `horde.base-size: 7` with `horde.wave-max-size: 7`,
  so waves no longer grow (5, 10, 15… up to 30) as the event goes on.
- ⭐ **One horde, then a 30-second wait.** `horde.interval-seconds: 30`.
- ⭐ **40 mobs per player, minions included.** `horde.max-alive-per-player: 40` (was 80). The
  per-player quota is now charged inside `spawn()`, the single spawn point, so boss `SUMMON`/`SWARM`
  minions and MythicMobs bosses can no longer bypass it.
- ⭐ **1000 active mobs server-wide is a hard ceiling.** Once the shared pool
  (`horde.max-alive-total`) is full, `spawn()` creates nothing else, boss summons included.
- **Comfortable band, no bursts:** a horde is always 7 mobs, so the field builds up to 30-40 per
  player over the first few hordes and never spikes with a 30-mob dump.

### Fixed
- **No more junk loot.** Common and uncommon gear is now iron: the wooden tools and leather armour
  and the stone tools and chainmail armour of the first two rarities were replaced by their iron
  versions. Nothing below iron drops any more.
- **Half the kills drop nothing, and each drop is a single item.** `mob_horde` goes from 60% with
  1-2 rolls to **50% with 1 roll**, and commons are ~50% of the rarity draw, so a horde kill no
  longer showers two or three items on the ground.

### Compatibility
- Paper **26.2** and **26.3**. Built with Java 25. Jar: `BloodMoonPlus-1.0.7-beta.jar`.
- Existing `config.yml` and `loot.yml` are not overwritten: copy the new `horde` values and the
  `mob_horde` numbers, or delete the old files so the defaults are regenerated.

---

## 1.0.8-beta

Skeletons, strays, bogged, pillagers and drowned kept shooting: every arrow and trident is an entity,
and stuck arrows never despawn, so a 20-minute moon could leave thousands of them behind and drag
the TPS down. The ground also filled with unclaimed loot.

### Added
- ⭐ **Event debris cleanup** (`cleanup:` section). Every `cleanup.interval-seconds` (45 s) the worlds
  with an active moon drop the projectiles fired by Blood Moon mobs: arrows (skeleton, stray, bogged),
  tridents (drowned), crossbow bolts (pillager) and fireballs. Stuck arrows go on the first pass, any
  projectile older than `projectile-max-age-seconds` (20) goes too, and the world is kept under
  `max-projectiles-per-world` (150) by removing the oldest ones. Player arrows are never touched.
- ⭐ **Old loot is swept too.** Items dropped by the event that have been on the ground for more than
  `item-min-age-seconds` (90) are removed. Only event loot carries the marker, so nothing a player
  drops is ever deleted.
- **Full cleanup when the moon ends:** projectiles and aged loot are purged on stop, again 2 s and
  10 s later, and the chunk guard also removes them from chunks that were unloaded when the moon
  ended.

### Changed
- **Fewer ranged mobs in the horde.** Skeletons 25→16, strays 10→6, bogged 9→6, drowned 14→10 and
  pillagers 13→8; zombies 40→48, husks 10→12 and spiders 12→16. Ranged mobs go from ~45% to ~32% of
  the horde, which cuts projectile spam at the source.
- ⭐ **Bosses have their own quota.** The 40-per-player ceiling and the 1000-mob pool now count only
  horde mobs and boss minions. A scheduled boss never waits for a free slot and never pushes a
  player to 41; the minions it summons do count inside the 40.

### Fixed
- **Your own loot is never swept.** Overflow from a full inventory was dropped through the same path
  that marks event loot, so the periodic cleanup could remove an item that already belonged to a
  player. Only what an event mob or boss drops carries the marker now.
- **Player arrows are never touched.** Projectiles are matched by their shooter, so an arrow you are
  aiming with is not part of the sweep, no matter how old it is.

### Compatibility
- Paper **26.2** and **26.3**. Built with Java 25. Jar: `BloodMoonPlus-1.0.8-beta.jar`.
- Existing `config.yml` is not overwritten: add the `cleanup:` section (and the new `horde.mobs`
  weights if you want the rebalance), or delete the file so the defaults are regenerated.

---

## 1.0.9-beta

The 1.0.8 tooltip rewrite only reached servers that deleted their `items.yml`. The plugin never
overwrites that file, so upgraded servers kept the old four-enchantment lore line and every weapon
tooltip was still wider than the screen. Now the plugin updates the descriptions in place.

This version also ships a **new default calendar — two Blood Moons a week, Wednesday 09:30 and
Saturday 17:30** — with the old every-other-day schedule kept one comment away, and the Discord
countdown moves to **two hours and one hour before** the event.

### Added ⭐
- ⭐ **`items.yml` gets the new descriptions without being deleted.** On startup the plugin reads the
  server's `items.yml` and, for every item that also exists in the jar catalogue, replaces **only**
  its `lore:` block. Names, materials, rarities, enchantments, items you added by hand, comments and
  the file's formatting stay exactly as they are, and an `items.yml.pre-vN.bak` copy is written
  first.
- **`config-version`** marks the content version of the file. An equal or higher number means the
  file is left alone, so lore you edited yourself survives every update; lower or delete it to
  force another sync.
- ⭐ **The same in-place update for `config.yml`.** Old configs receive the keys they never had —
  `cleanup`, `items.migrate-on-join`, `discord`, `announce` entries and any other new key — appended to
  the end of their section with the comments that document them. Everything you already set (values,
  your own keys, your comments, the file's order) stays exactly as it is, and a
  `config.yml.pre-v2.bak` copy is written first. The result is validated before saving: if any of your
  values would change, nothing is written.
- **`settings.sync-lore: false`** turns the merge off completely (items.yml); setting
  `config-version` above the current one freezes `config.yml` the same way.

### Added ⭐
- ⭐ **`/luna disable` and `/luna enable`** turn the Blood Moon off and on **for the whole server**: no
  scheduled event, no `/luna start`, no Discord countdown and no chat warning fires until it is
  enabled again. The switch is stored in `data.yml`, so a restart does not bring the event back; a
  moon already running finishes on its own (`/luna stop` to cut it). `/luna status` and `/luna next`
  report the state. Spanish aliases: `/luna desactivar` and `/luna activar`.

### Changed ⭐
- ⭐ **New default calendar: two moons a week** — `WEDNESDAY 09:30` and `SATURDAY 17:30`
  (`America/Mexico_City`). The old **every-other-day** schedule (Monday, Wednesday, Friday and
  Sunday) ships right below it in `config.yml`, commented, so switching back is just moving the
  `#`. Your existing `config.yml` is never rewritten: if you already run a calendar, it stays.
- ⭐ **Discord countdown now warns two hours and one hour before** (`discord.warnings-minutes:
  [120, 60]`, was `[60, 30, 10, 5]`). Chat warnings keep their own list
  (`schedule.warnings-minutes`).
- ⭐ **Every weapon, tool, armour piece and shield lore is now two lines at most:** one short
  flavour line (≤ 34 visible characters) and a single line with the signature ExcellentEnchants
  extras (≤ 44), tinted with the item's rarity colour. Real enchantments keep their own lines,
  drawn by Minecraft, so the tooltip no longer pushes past the edge of the screen.

### Fixed
- **The schedule placeholders follow `/luna disable`.** `%bloodmoon_next%` and its variants
  (`%bloodmoon_next_when%`, `%bloodmoon_next_in%`, `%bloodmoon_next_in_seconds%`,
  `%bloodmoon_next_difficulty%`) return the disabled text and `0` seconds instead of a date that
  will never arrive, and the new `%bloodmoon_disabled%` reports the server-wide switch, so scoreboards
  and holograms stop counting down to a moon that will not happen.
- **Scheduled bosses never spawned with `horde.enabled: false`** (or with an empty `horde.mobs`).
  The queue of bosses whose time had come was only drained when a wave spawned, so with waves off
  it grew forever and no boss ever appeared. They are summoned as soon as their time comes.

### Compatibility
- Paper **26.2** and **26.3**. Built with Java 25. Jar: `BloodMoonPlus-1.0.9-beta.jar`.
- **Nothing to edit by hand:** `items.yml` (lore) and `config.yml` (new keys) are updated in place,
  each with its own `.pre-vN.bak` copy, so an old config keeps your settings and gains the new
  sections. Items already handed out refresh on join, on pickup or with `/luna refresh <player|all>`.

---
