# Changelog — BloodMoonPlus

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

<!--
Note: make sure the version number below matches the version you publish on
Modrinth. Lines marked with ⭐ are the most eye-catching features for the
project page.
-->

## [1.0.9-beta] — Narrow tooltips, up-to-date configs and two moons a week

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

## [1.0.8-beta] — Nothing left lying around

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

## [1.0.7-beta] — Seven at a time, forty at most

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

## [1.0.6-beta] — The moon takes its mobs with it

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

## [1.0.5-beta] — One start alert, one label

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

## [1.0.4-beta] — Discord embeds for the Blood Moon alerts

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

## [1.0.3-beta] — ExcellentEnchants replaces the custom item abilities

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
- **ExcellentEnchants became a soft dependency in this very version (1.0.3-beta):** `plugin.yml`
  now lists it in `softdepend` (1.0.2-beta listed only PlaceholderAPI, Multiverse-Core and
  WorldGuard), because the custom enchantments on the 104 items are EE's (`vampire`, `thunder`,
  `tunnel`, `veinminer`, `dragon_heart`…). It is optional to install, not to remove: without it
  (and NightCore) the plugin still starts, **new** items get just the vanilla enchantments and the
  console warns once. Do not uninstall EE with gear already handed out: Minecraft deletes items
  whose enchantment is missing, so run `/luna refresh <player|all>` with EE installed first if you
  ever need to drop it. NightCore is EE's own dependency, not this plugin's.

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
- `items.yml` keeps writing EE enchantments **without a namespace** (`vampire: 2`): the lookup
  tries the vanilla id first and then `excellentenchants:<id>` in the live registry, so the
  plugin never compiles against EE's API.

### Added
- `items.migrate-on-join` in `config.yml`.
- Automatic `items.yml.bak` / `bosses.yml.bak` copies before the first upgrade.

## [1.0.2-beta] — 7 languages, protected gear, the Cataclysm Maw and a full audit

The biggest update since 1.0.1. The plugin now speaks **seven languages**, Moon gear can no longer
be stripped away, a new boss punishes standing still, and two full code audits plus live testing on
a Paper 26.2 server closed every bug they found — including the main-thread stall behind the TPS
drops.

**Highlights**
- **Seven complete languages** (Spanish, English, Portuguese, French, German, Italian, Russian) with
  build-time tests that fail if a translation loses a key or a `{placeholder}`.
- **Moon gear is protected**: no grindstone, no lost enchantments, and items players already own are
  upgraded automatically when they join.
- **New boss — Fauces del Cataclismo**: 320 HP, `VORTEX` absorption, `SWARM` minions and an `EXPLODE`
  detonation it survives.
- **Curse of Vanishing** for every Rare-to-Mythic weapon, tool and armour piece.
- **The main thread no longer waits on the statistics lock**, so the watchdog stall is gone.
- **30+ fixes**: participation, survivors, boss placeholders, per-world config keys, commands and
  messages that were translated but never sent.
- **Updating is just the jar** — nothing to migrate, no data to delete (see the new README section).

The detail of every change is below.

### Changed — the Cataclysm Maw now explodes (and survives it)
- **320 HP** instead of 180, so it can afford a long fight with its own detonations.
- **New `EXPLODE` ability**: every **50 s** the boss detonates with a **15-block radius**, up to
12 damage at the center falling to 25% at the edge, plus knockback. It never breaks blocks in the
normal phase; **only in its enraged phase** (below 40% health) it also opens a ~6-block crater.
It **cannot kill the boss**: bosses are now immune to all explosion damage (their own, their
  minions', TNT, beds, grenades), so the explosion is a threat, never a suicide.
- **Renamed**: the boss is now **Fauces del Cataclismo** ("Cataclysm Maw") and its minions are
**Crías del Cataclismo**, so the name itself announces what to expect: it devours you and it
detonates.
- **New `SWARM` ability**: every **35 s** it summons **3 mini-bosses** (charged creepers with 30 HP,
a 3 s fuse and a 3-block blast) that chase players, explode and die in the process. Their blast
does **no damage to the original boss** and never breaks blocks, even outside an event.
- Coherent rotation: cooldowns 12 / 16 / 24 / 35 / 50 s are staggered so the kit never fires in
one burst, and at most **6 mini-bosses** can be alive per boss.
- Net effect: a boss that keeps pressure on with pulls, slams and charges while periodically
detonating in the middle of a farming spot, so afk mob farms inside the event world pay for it.

### Balance — the horde stops killing itself and a boss owns the pool
- **Hostile mobs no longer damage Blood Moon mobs.** A creeper exploding next to the pack, a stray
arrow, a boss area attack or a natural zombie that wanders in could wipe half a wave before it
reached a player. Damage to a horde mob, boss or minion is now cancelled whenever the attacker is
a hostile mob — horde, boss, minion or a natural monster — whatever the source: melee, projectile
or explosion. Players, iron golems, snow golems, wolves and pets still fight the horde, and the
world (lava, fall, drowning) still kills it.
- **No new wave while a boss with minions is alive.** Only bosses that summon (SUMMON/SWARM) take
over the shared pool: waves pause until they die, so the minions they still have to summon always
have room and are never crowded out by the horde. A boss without summons does not stop the waves.
Minion summons respect `horde.max-alive-total` too, so a boss fight cannot push the pool over its
limit either.

### Performance
- **The main thread no longer waits on the statistics lock.** `PlaceholderSnapshotService.publish()`
ran every 10 ticks and called `StatisticsStore.get()`, which took the same lock the asynchronous
`statistics.yml` writer held while doing disk I/O. On a slow disk that blocked the server thread
long enough for the Paper watchdog to dump it. Statistics are immutable, so `get()` is now a
lock-free read of a `volatile` snapshot.
- The boss tick now resolves nearby players **once per boss per tick** instead of once per
ability, explosion particles were trimmed (1 emitter + 95 particles instead of 303), a detonation
processes at most **48 entities** and **1400 blocks**, and the minion summon is capped at 6 alive
per boss. Nothing in the kit allocates unbounded work per tick, so a large farm next to the fight
cannot stall the server.
- **A horde wave no longer sweeps the world twice per player.** `spawnWave` called
`getNearbyEntities(48,48,48)` — a 97³ entity sweep that touches hundreds of entities with mods
installed — twice for every eligible player, only to count live horde mobs. The cap
(`horde.max-alive-total`) now reads a per-world counter that
`countHordeAlive` already refreshed every ~2 s, so a wave does no entity sweeping at all.
- **Shared pool of 1000 horde mobs across the worlds with the event running.** `horde.max-alive-total`
is a single budget for every active event world, and only event worlds count towards it: a world
without a session neither feeds the pool nor receives horde spawns. Within that pool each player
has a ceiling of `horde.max-alive-per-player` (80), which keeps phones from drowning in mobs, and
waves are spread across the eligible players of all event worlds so no player or world hoards the
free room. Bosses and their minions count too.
- **Dead mobs come back progressively, wave by wave.** Removing an instant refill means a kill only
returns its slot to the pool; the next wave spends it, up to its normal size, so killing 500 at a
time never produces a 500-mob burst. Bosses whose timer expires now wait for the next wave and
spawn at its start with their room reserved.
- **Spawn search stopped allocating thousands of blocks and strings.** `findSpawn` ran up to 12
attempts × 17 vertical steps per mob, building three `Block`s and three `toString().contains(...)`
per candidate. It now tries 8 positions, checks the cheap conditions first (feet passable, head
passable) and compares `Material` constants against a small set. Wave size is unchanged.
- **Mob names are parsed once.** Every spawned horde mob re-ran the MiniMessage parser (and the
legacy-colour converter) on its configured name; the `Component` is now built once in `reload()`
and shared by every mob of that type.
- **The world clock is no longer re-sent every second.** With `event.lock-night` the session tick
called `setTime` every second, so every player received a time packet every second; the night is
now only re-applied once the world has drifted more than 30 s past it. Boss bars are sent only
when a player's visibility actually changes, and the session tick reads the world's player list
once instead of six times.

### Added — 7 languages
- **Spanish, English, Portuguese, French, German, Italian and Russian**, complete and selectable in
  `plugins/BloodMoonPlus/locales/locales.yml` (`Language: pt`, `Language: ru`, …). All seven files are
  copied on first start and `/luna reload` applies the change without restarting.
- An unknown language now logs a warning and falls back to English instead of loading an empty file.
- New tests fail the build if any locale misses a key or loses a `{placeholder}` relative to Spanish,
  so translations can no longer silently drift.
- **Death-rule warnings**: the per-world `DyingLossesInventory` / `DyingLossesExperience` messages were
  translated but never sent; players now get them when the event starts.
- **Locale cleanup in all 7 languages**: removed 9 keys that no code could ever print
  (`plugin-reloaded`, `event.cannot-stop-world`, `event.world-permanent`,
  `schedule.warning-title/body`, `boss.spawn-failed`, `cmd.survivors-*`) and wired 3 that were
  missing: `general-error` (any unexpected command failure now answers the player instead of
  going silent), `cmd.usage-horde` (invalid arguments) and `boss.mythic-unavailable` (MythicMobs
  configured but not installed, which used to fall back to the vanilla boss silently).

### Added — Blood Moon gear can no longer be stripped
- **Grindstone blocked**: Blood Moon items cannot be disenchanted or have their curses removed
  anymore (the grindstone result is cancelled and the player is told why). Adding enchantments and
  renaming are still allowed, as are repairs and upgrades.
- **Anvil and smithing guarded**: the result never keeps fewer enchantments, lower levels or fewer
  abilities than the original item.
- **`/luna refresh <player|all>`** (aliases `/luna actualizar`, `/luna refrescar`): forces that
  upgrade right away for one player or every online player, reporting how many items changed.
- **Existing gear is upgraded on join**: items players already own — renamed or not — receive the
  enchantments, the vanishing curse and the abilities that were added to `items.yml` after they got
  them. Nothing is ever removed and durability is untouched.

### Fixed — full code audit
- **The boss VORTEX lasted twice as long as configured.** The runnable steps every 2 ticks but
decremented the remaining time by one per step, so `VORTEX:16` absorbed for 32 s.
- **The storm special event was invisible.** The session tick turned the storm off on the same
tick the special turned it on. The session now records how long the storm must last and the tick
only clears the weather afterwards; the dead `setStorm(true)` in the start sequence is gone.
- **Special events all fired 30 s after minute 5.** The per-tick probability reached 100% within
30 seconds of the window opening. It now grows linearly with a cumulative value of 1 across the
5-10 minute window, so eclipses, storms, fog and silence are spread out as documented.
- **`CommandsOnStart` / `CommandsOnEnd` of each world were read and never executed.** They now run
when the event starts and ends in that world, with `{world}`, `{difficulty}` and `%world%`
placeholders, exactly like the global lists in `config.yml`.
- **Manual stops counted participation up to the planned end.** `SurvivorRewards`
`MinimumParticipationSeconds` was measured against a future timestamp, so everyone qualified;
it is now cut at the real stop time. Death counts also use the session counter.
- **`/luna killmobs` only cleaned worlds with an active event**, so a boss summoned by hand could
not be removed. It now sweeps every loaded world for tagged mobs and bosses, event or not.
- **`/luna difficulty <x N> <world>` ignored the world** even though tab-completion offered it,
and always changed the global difficulty. Passing a world now changes only that world.
- Minor: the weekly schedule no longer keeps warning keys from past days forever, and
`/luna boss <id>` tab-completion suggests worlds as well as players.

### Fixed — third audit, live-tested on Paper 26.2
- **The `SWARM` ability of the Cataclysm Maw crashed the boss tick.** Its creeper minions were given
  a **3-second fuse (60 ticks)**, but Paper rejects any fuse above `maxFuseTicks` (30 by default)
  with `IllegalArgumentException: ticks > maxFuseTicks`. `spawnMini()` now raises the entity's limit
  before setting the fuse and clamps it anyway, and a failing ability no longer aborts the rest of
  the boss kit or retries twice per second: it warns once and its cooldown is restarted.
- **`/luna killmobs <world>` now honours the world.** Tab-completion offered every loaded world,
  but the argument was ignored and the whole server was swept. With a world only that world is
  cleaned, and an unknown world is reported instead of silently cleaning everything.
- **Named horde mobs finally show their name.** `horde.mobs[].name` was applied with
  `customName()` but never `setCustomNameVisible(true)`, so the configured name was invisible.
- **`SurvivorRewards.DisqualifyOnWorldLeave` did nothing.** The key was parsed into
  `SurvivorPolicy` and then never read, so a player who left the event world mid-event still
  collected the survivor reward at the end. The session now records who abandoned its world, and
  with the option enabled that player is disqualified and told why
  (`event.survivor-disqualified-world-leave`, translated into all 7 languages).
- **Survivor reward messages can use the per-player placeholders.** `%participation_seconds%`,
  `%participation_percent%`, `%died%` and `%survived%` were documented as filled at runtime but
  no code ever filled them, so they always printed nothing; they now resolve from the session's
  real join time and death list (an explicit `:value` suffix still wins).

### Removed
- Dead code found in the third audit: the unused `survivors` list in `EventManager.stopOne()`,
  the never-read `sessionUuid` field of `SessionPlaceholderState` (a random UUID was generated
  every 0.5 s for nothing), the unused `BossManager.aliveSnapshot()`, two `if` branches in
  `SurvivorManager` that checked a policy and then did exactly the same `continue`, and the
  `firstLoad` check in `LocaleManager.reload()` that could never be true.
- `SurvivorRewards.RequireOnlineAtEnd`, which could never be honoured: survivor rewards are only
  paid to players online in the event world (there is no mailbox for offline rewards), so `true`
  and `false` behaved exactly the same. It is gone from `SurvivorPolicy` and from
  `worlds-template/config.yml`; existing per-world files may keep the key, it is simply ignored.
- Dead code left over from earlier refactors: `EventManager.hasGlobalSession()`,
`LootManager.hasTable()`, `DataStore.syncReload()`, `BloodMoonPlus.keys()`,
`Keys.instance()`, `ScheduleManager.zone()`, `ScheduleManager.intervalHours()`, an unused
`EventManager.start(World, double, int)` overload and a no-op `keepVanilla` line.

### Fixed ⭐
- **The historical stats counted mob kills as player deaths.** `total_death_events` and
  `last_event.death_count` were fed with the number of horde mobs killed by players. Player deaths
  are now counted separately, so `total_death_events`, `death_count` and `unique_deaths` finally
  describe the same thing (`%bloodmoon_death_count%`, `%bloodmoon_last_event_death_count%`).
- **Survivors could be negative.** `participants - deaths` was used as the survivor count, so a
  player who died without being registered as a participant (another world, creative mode) made it
  drop below zero. Survivors are now counted exactly as participants who never died (statistics
  and `%bloodmoon_survivors_current%`).
- **Per-world placeholders showed another world's data.** With the global event, the placeholder
  service only published the first world's session, so `%bloodmoon_world%`,
  `%bloodmoon_time_remaining%`, `%bloodmoon_participating%` and `%bloodmoon_survivor_*%` returned
  the first world for every player. Each world now has its own published snapshot and each player
  is read from their own world's session (console still falls back to the global one).
- **Special events only fired in one world.** The 5-10 minute timer for eclipses, storms, fog and
  silence was a single field shared by the manager, so with the global event only the first world
  ticked it. The timer now lives in each world's session, so every world gets its own specials.
- Minor: the placeholder player cache no longer keeps disconnected players forever.
- **Participation was fake**: `%bloodmoon_participating_seconds%`,
  `%bloodmoon_participating_percent%` and `SurvivorRewards.MinimumParticipationSeconds` used the
  whole event duration for every player. They now use each player's real join time, recorded when
  they first take part in the event.
- **`%bloodmoon_horde_count%` now counts the real horde mobs alive** in the event world
  (tagged mobs that are not bosses), refreshed every ~2 s. Before it returned
  participants x 8, an invented estimate.
- **`%bloodmoon_event_elapsed%` uses the real session time** (it assumed a fixed 20 minutes,
  so 25/30-minute events showed the wrong value).
- **`%bloodmoon_bosses_defeated_session%` counts bosses defeated in that session** and now
  actually resolves: PAPI splits the placeholder in three parts, so the old case name never
  matched and the placeholder always returned nothing.
- **Per-world config keys that did nothing now work**: `BedNotAllowed` (no sleeping during the
  event), `DyingLossesInventory` (death clears the inventory), `DyingLossesExperience` (death
  drops no experience) and `WorldPermanent` (the event does not end on its own; only
  `/luna stop` stops it).
- **`Boss.Rewards` now runs its commands** on boss death, with `KILLER` / `TOP_DAMAGE` / `ALL`
  modes, `RequirePlayerKiller`, `RewardOnce` and the ;s / ;f / ;p suffixes.
- **`CommandsOnStart` / `CommandsOnEnd` / `SurvivorRewards.Commands` accept the documented
  `%world%`, `%player%` and `%difficulty%`** (they only replaced the brace form and ignored
  difficulty). `SurvivorRewards.Messages` now understands legacy `&` colour codes.
- **Damage reflection, thorns, thunder guard and blast wave apply their damage through the
  re-entrancy guard**, so they cannot re-trigger weapon abilities in a cascade.

### Fixed — full second audit (live-tested on Paper 26.2)
- **A manually spawned boss stayed invisible in `%bloodmoon_boss_*%` when no event was running.** Boss
  snapshots were only published for worlds with a session, and `/luna boss` refused to run without
  one. Boss state is now published per world whether or not there is an event, `/luna boss <id>`
  works outside an event (spawning next to the executing player), and `<id> <jugador|mundo>` accepts
  a loaded world as well as a player. The spawn announcement and the dragon growl are now sent only
  to the boss's own world instead of the whole server.
- **`%bloodmoon_boss_alive%` stayed true forever after the boss died.** The boss snapshot was only
  published while a boss was alive and never reset, so `boss_alive`, `boss_name`, `boss_health`
  and `boss_state` kept the dead boss until a reload. The boss state is now published per world
  and switches to `DEFEATED` when it dies (also fixes two simultaneous events sharing one boss).
- **Ambient sounds and fog ran 20 times instead of once.** `now / 1000 % N == 0` is true for a whole
  second (20 ticks), so the cave sound, the heartbeat and the red fog repeated 20 times in a row.
  They are now driven by per-session timestamps.
- **Bleed damage had no attacker.** A mob killed by bleeding gave no loot, no kill credit and did
  not count towards the boss damage ranking. The attacker's UUID is stored with the bleed mark and
  used for the damage.
- **`SurvivorRewards` ignored `event.reward-on-stop`.** Manual stops never ran the granular survivor
  commands/messages even with that option enabled; only the legacy loot table fired.
- **`%participation_seconds%`, `%participation_percent%`, `%died%`, `%survived%` crashed the
  reward command when written without a value**, and `%world_uuid%` threw an NPE when the world had
  already been unloaded. Both are now empty strings instead of an exception.
- **Messages leaked to other worlds.** `horde-descending`, `player-died`, `mob-slain` and
  `survivors-none` were sent server-wide instead of only to the event world.
- **Orphan horde mobs survived a restart** when the event ran in two worlds: the cleanup only ran
  when no session was resumed at all. Every world without a resumed session is cleaned now.
- **`/luna chances` printed `NaN%`** for a table with no valid rarity weights; it now says the
  table has no valid rarities.
- **`/luna give` said "item does not exist" for a unique item that had reached its limit**; it now
  reports the limit reached.
- Sleeping during the event was blocked silently; the player now gets the `bed-not-allowed`
  message.
- Statistics were saved asynchronously even while the plugin shut down, where the scheduler
  rejects new tasks; that flush now happens inline.
- `DataStore`'s dirty flag is `volatile` (it is written under the lock and read outside it).
- Removed dead code (`StatisticsStore.FileUtils`, unused MythicMobs fields).

### Changed ⭐
- **Every weapon, tool and armor piece from Rare to Mythic now carries the Curse of Vanishing**
  (`vanishing_curse`): swords, axes, pickaxes, shovels, hoes, bows, crossbows, tridents, maces and
  helmets, chestplates, leggings, boots and elytra. Previously only four legendary weapons had it,
  so Rare and Epic gear (Blood Sword, Crimson Bow, Shadow Dagger, Storm Bow, Umbral Pickaxe,
  Executioner's Axe, Ruin Sword, Abyssal Crossbow, Blizzard Axe, Hunter's armor, Thunder Boots) and
  the Mythics (Eclipse Blade, Reaper's Scythe, Abyss Trident, Colossus Mace, Apocalypse Axe,
  Eclipse Crown/Chest/Legs) could be kept after dying. Shields never carry it.
- The curse is applied **when the item is built**, so it also covers servers that already have an
  `items.yml` (no config edit needed) and any item added to `items.yml` in the future.
- New `items.yml` setting **`settings.vanishing-curse-min-rarity: RARO`**: minimum rarity that
  receives the curse; `OFF` (or `NONE`) disables it.

### Added 🎯
- **`/luna boss <id> [jugador]`**: with a player name the boss appears next to that player, in the
  world of the event they are in; without a name it keeps summoning on a random eligible player.
  The tab-complete offers the boss ids and then the online players.
- **`/luna horde [jugador]`**: the wave falls only on that player (their own mob cap/saturation),
  instead of on every eligible player of the world. Without arguments it stays the global wave.

### Fixed (found on a live Paper 26.2 test server)
- **`bosses.yml` no longer deletes a boss when one field is wrong.** `bar-color: BLACK` (not a valid
  Adventure colour) removed `cazador_sombras` entirely with a cryptic enum error and only 8 of 9
  bosses loaded. Each field is now validated separately, the warning names the field, the bad value
  and the valid options, and the shipped file uses `PURPLE`.
- **`/luna status` works.** It only existed as `/luna` with no arguments, so typing `/luna status`
  printed the usage message. `estado` is accepted too, and the usage line no longer advertises the
  non-existent `survivors` subcommand.
- **Multi-word placeholders were never resolved.** PlaceholderAPI splits the name, so
  `%bloodmoon_death_count%`, `%bloodmoon_last_event_world%`, `%bloodmoon_total_bosses_spawned%`,
  `%bloodmoon_boss_health_percent%`, `%bloodmoon_items_unique_owned%` and the other long names
  returned nothing. The expansion now matches the whole suffix; verified with `papi parse` for 40+
  placeholders with and without an active event.
- **New locale keys now reach servers that already have `locales/*.yml`.** Locale files are never
  overwritten, so a version adding keys showed `[i18n missing: ...]`. Strings now fall back to the
  copies inside the jar (server file → en.yml → jar), and command feedback passes the world so old
  files no longer print a literal `{world}`.
- **Quiet startup:** `Idioma activo` was printed three times and `BloodMoonPlus recargado.` appeared
  on first boot; the locale is now announced once and only when it changes.
- **No more world-clock warning** on every global event: Nether/End are detected before touching the
  clock instead of warning each time.

### Removed
- `messages.yml` (packaged but never read by any code) and the attempt to install
  `distribution/README.txt` + `distribution/EXAMPLES/*` resources that never existed in the jar.
- Per-world keys that were parsed but unused and cannot be honoured: `Boss.VanillaBossBar.*`
  (the bar comes from `bosses.yml`), `SurvivorRewards.IncludeLateJoiners`,
  `SurvivorRewards.DisqualifyOnDisconnect`, `SurvivorRewards.RewardOncePerSession` and
  `MythicMobs.UseMythicMobsRewards` / `RunBloodMoonRewardCommands`.

### Documentation
- `README.md` / `README.en.md`, `DROPS.md` / `DROPS.en.md`: the Curse of Vanishing note now covers
  "Rare and above: weapons, tools and armor".
- `worlds-template/config.yml`: only documents keys the plugin actually honours, and explains the
  real behaviour of `WorldPermanent` and the boss rewards.

### Compatibility
- Paper **26.2** and **26.3**. Built with Java 25.

### Added — Charged Absorber boss (`creeper_cargado`)
- **New boss: a charged (electric) creeper twice as tall** as a normal one (scale 2.0×), 180 health,
  green segmented bar and its own phase 2 at 40%. It **does not explode**: the fuse is cancelled, so
  it never kills itself or breaks the terrain, and its threat comes from its abilities.
- **New ability `VORTEX`** (`cd:radius:sec:force:damage`): **absorption** — it drags every nearby
  player toward the boss for several seconds and damages them when they reach its body. The Charged
  Absorber fires it every 16 s (radius 18 m, 4 s, force 1.6) and finishes with SLAM and CHARGE.
- `bosses.yml` accepts `powered: true` on any creeper boss to spawn it charged.

### Fixed — horde mobs survived vanilla transformations
- **A Blood Moon zombie that drowned stopped being a horde mob.** Vanilla builds the Drowned from a
  **new entity**, so it lost the plugin's data: it no longer counted in `%bloodmoon_horde_count%` and
  **survived both `/luna stop` and the end of the event**. Transformations (drowned, zombie villager
  and any other) now copy the Blood Moon mark, the scaled health, the speed and the custom name into
  the new entity, and horde mobs additionally carry a `bloodmoon_horde` scoreboard tag as a second
  safety net.

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
