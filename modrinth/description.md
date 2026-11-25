# 🩸 BloodMoonPlus — Blood Moon · 1.0.2-beta

A **Blood Moon** event plugin for Paper servers: **7 complete languages**, **10 bosses**, **45 items** spread across rarities, **protected Moon gear**, **vanilla-max enchantments plus ExcellentEnchants**, **progressive waves**, **placeholders**, **full immersion** (red fog, lightning, ambient sounds and announcements) and **rare-loot inventory delivery**.

Built to run smoothly on **40+ player** servers without changing the gameplay.

> **Requires Paper 26.2 or 26.3 · Java 25.** Updating from 1.0.1? **Only the `.jar` needs to be replaced.**
> See [Updating from 1.0.1](#-updating-from-101).

---

## ✨ Highlights

- **🌍 7 languages** — Spanish, English, Portuguese, French, German, Italian and Russian, complete and switchable in `locales/locales.yml` with `/luna reload` (no restart). An unknown code warns and falls back to English, and the build fails if a translation loses a key or a `{placeholder}`.
- **🟩 New boss — Fauces del Cataclismo** (`creeper_cargado`): a charged creeper **twice as tall**, **320 HP**, immune to every explosion. It **absorbs** players with `VORTEX`, summons **Crías del Cataclismo** with `SWARM` and **detonates** itself every 50 s without dying — only in its enraged phase does the blast open a crater.
- **🛡️ Moon gear cannot be stripped** — the grindstone is blocked for Blood Moon items, the anvil and smithing table can never return a result with fewer enchantments, and the gear players already own is **upgraded automatically when they join** (renamed items included). `/luna refresh <player|all>` forces it instantly.
- **🗓️ Fixed schedules** — `weekly` mode (per weekday at a real time, configurable timezone, defaults to `America/Mexico_City`) or `interval` mode (every N hours). You can set **multiple runs on the same day**.
- **🌐 Server-wide event** — one Blood Moon for **every loaded world at the same time** (`event.global: true`): night, hordes, bosses, the boss bar and survivor rewards all run everywhere together. Set it to `false` to go back to a `worlds` list.
- **👹 10 bosses** with custom health bars, **enrage** phases, minimum difficulty, abilities and their own loot.
- **🎁 45 items** sorted by rarity: common (always drop), rare, epic, legendary and **mythic** (extremely rare, ~0.1%, max 3 per server with a serial number).
- **💀 Curse of Vanishing** on every **Rare-to-Mythic** weapon, tool and armour piece, so high-rarity gear is lost on death as its rarity implies (toggle with `settings.vanishing-curse-min-rarity`).
- **📦 Rare loot delivery**: rare, epic, legendary and mythic drops go straight to the killer's inventory when there is one.
- **⚔️ ExcellentEnchants integration**: every item ships vanilla enchantments at their max (level 5 on legendary and mythic gear, e.g. Protection 5) plus EE enchantments (`thunder`, `vampire`, `electrified_arrows`, `dragon_heart`…), with EE's own per-level chance settings. No custom ability engine.
- **🌊 Progressive waves** — wave 1: 5 mobs, wave 2: 10, wave 3: 15… up to a configurable cap.
- **🌫️ Immersion** — red fog, lightning when hordes/bosses appear, ambient sounds and periodic announcements. No rain, but thunder.
- **📊 Stats & survivors** — who survived, who dealt the most damage, rankings and MVP rewards, all counted from real participation time instead of estimates.
- **🔌 Optional integrations** with PlaceholderAPI, Multiverse-Core and WorldGuard (MythicMobs as an alternative boss mode).
- **🚫 Wither control** — the normal Wither is blocked; only the **Crimson Scourge** boss can spawn.
- **⚡ No TPS drops** — the placeholder/statistics path no longer takes a lock while the file is being written, which is what used to stall the main thread and trigger the Paper watchdog.

---

## 🌍 Languages

| Language | Code | Language | Code |
|---|---|---|---|
| Spanish | `es` | Italian | `it` |
| English | `en` | Russian | `ru` |
| Portuguese | `pt` | French | `fr` |
| German | `de` | | |

Change `Language:` in `plugins/BloodMoonPlus/locales/locales.yml` and run **`/luna reload`**. Every file is copied on first start; the plugin never overwrites your edited files, and any key missing there falls back to the copy inside the jar, so you never see `[i18n missing]`.

---

## 📥 Installation

1. Download the `.jar` from the **Versions** tab.
2. Drop it into the `plugins/` folder of your **Paper** server.
3. Restart the server.
4. (Optional) Install **PlaceholderAPI** for placeholders.
5. Configure `plugins/BloodMoonPlus/config.yml` and run `/luna reload`.

---

## 🔄 Updating from 1.0.1

**Only the `.jar` needs to be replaced.** Nothing is migrated and no data is deleted.

1. If an event is running, run `/luna stop` and **stop the server**.
2. **Delete the old jar** from `plugins/`. That is the one thing you must remove: both jars declare `name: BloodMoonPlus` and Bukkit rejects the second with *"plugin name is already in use"*.
3. Copy the new `BloodMoonPlus-1.0.2-beta.jar` into `plugins/` and start the server.

**Do NOT delete:** `data.yml` (running event, serial numbers of unique items and pity — mythics would become available again), `statistics.yml` (history), `config.yml` (it did not change in this version), `locales/*.yml` (your translations) or `worlds/<world>/config.yml`.

**`bosses.yml`:** your server's file is never overwritten. If you keep it, you keep your bosses and tuning, but the new **`creeper_cargado`** will not exist. The old `bar-color: BLACK` on the shadow hunter no longer deletes the boss (each field is validated separately and falls back to `RED` with a clear warning); the shipped file uses `PURPLE`. To get the new boss without losing your changes, back up your file, let the plugin generate the new one once and re-apply your adjustments.

**`items.yml`** needs no changes (the vanishing-curse default lives in the code), and **`messages.yml`** is no longer read — you can delete it.

---

## 🎮 Commands

Main command: **`/luna`** (aliases: `/bm`, `/lunadesangre`, `/bloodmoon`).

| Command | Description |
|---|---|
| `/luna` · `/luna status` | Current event, time left and next Blood Moon |
| `/luna next` | Lists the next 6 scheduled moons |
| `/luna start [xN] [minutes] [world]` | Starts the event manually |
| `/luna stop [world\|all]` | Stops the event (whole server or one world) |
| `/luna difficulty <xN> [world]` | Adjusts the difficulty of the current event |
| `/luna boss <id> [player\|world]` | Summons a specific boss, next to a player or in a world |
| `/luna horde [player]` | Forces a wave (optionally only on a player) |
| `/luna killmobs [world]` | Removes the event mobs and bosses |
| `/luna refresh <player\|all>` | Applies the gear update immediately |
| `/luna give <player> <item> [amount]` | Gives one of the plugin's items |
| `/luna items` | Lists the available items |
| `/luna chances <table>` | Shows the real drop chances of a table |
| `/luna reload` | Reloads the configuration and locales |

Spanish aliases work too: `/luna estado`, `/luna iniciar`, `/luna detener`, `/luna dificultad`, `/luna jefe`, `/luna horda`, `/luna actualizar`…

### Permissions

| Permission | Description | Default |
|---|---|---|
| `bloodmoon.use` | View status and upcoming events | Everyone |
| `bloodmoon.admin` | Manage the event | OP |

---

## 🗓️ Schedules & difficulty

In `weekly` mode, each entry accepts either a simple time or a block with difficulty and duration:

```yaml
schedule:
  mode: weekly
  timezone: America/Mexico_City
  days:
    MONDAY:
      - time: "21:30"
        difficulty: 1
        duration-minutes: 20
    FRIDAY:
      - time: "13:40"
        difficulty: 2
        duration-minutes: 20
      - time: "20:40"        # two runs on the same day
        difficulty: 3
        duration-minutes: 25
```

**Difficulty** is a continuous multiplier (0.1 – 10.0). It scales health, damage, speed, wave size, spawn rate, loot chance and rarity luck.

---

## 🩸 Bosses

| Boss | Min. difficulty |
|---|---|
| Bloody Zombie King | 1.0 |
| Bone Sentinel | 1.0 |
| Crimson Archer | 1.0 |
| Shadow Hunter | 1.0 |
| **Fauces del Cataclismo** (`creeper_cargado`, 2× tall, 320 HP) | 1.0 |
| Crimson Necromancer | 1.5 |
| Ancestral Devastator | 1.5 |
| Night Executioner | 1.5 |
| Abyssal Warden | 2.0 |
| Crimson Scourge (Wither) | 2.5 |

Every boss has a health bar, an **enrage** phase below 40% health, its own ability rotation and its own loot table. The Cataclysm Maw mixes `SLAM`, `VORTEX` (absorption), `CHARGE`, `SWARM` (3 minions, max 6 alive) and `EXPLODE` (15-block radius, up to 12 damage, crater only when enraged). See the project's **README** for every drop in detail.

---

## 🧩 Placeholders

With **PlaceholderAPI** installed (namespace `%bloodmoon_*%`): event status, time left, next moon, difficulty, live bosses (name, health, percentage, state), current wave, participants, survivors, real participation seconds/percent, per-player stats and lifetime history — 40+ placeholders, including long names such as `%bloodmoon_last_event_death_count%` or `%bloodmoon_boss_health_percent%`.

---

## 🚫 Wither datapack (optional)

The plugin already blocks the normal Wither on its own. If you also want a data-layer safeguard, the project ships the **BloodMoonWitherControl** datapack (in `datapacks/`), which removes any Wither that is **not** the Blood Moon boss. It is optional.

---

## ⚡ Performance

Designed for **~40 player** networks: local (not whole-world) entity scans, item-parse caching, one player lookup per boss per tick, capped particles and explosion work, and lock-free statistics reads. For very large servers you can tune `horde.wave-max-size`, the shared `horde.max-alive-total` pool (1000 horde mobs across the worlds with the event running, bosses and minions included) and the per-player ceiling `horde.max-alive-per-player` (80, useful when many players join from phones).

---

## 🛠️ Build from source

```bash
git clone <your-repository>
cd BloodMoonPlus
./gradlew build
```

The `.jar` is produced in `build/libs/`. Built with **Java 25**; the project ships **109 unit tests**.

---

## 🔒 License

**All rights reserved.** The source code is public for reading and security review only; copying, modifying, reusing or redistributing it is **not** allowed. You may download and run the compiled plugin on your server. See the `LICENSE` file for details.

---

## ❤️ Credits

Inspired by — and built to surpass — *BloodMoon-Reborn*. Made with care for Spanish-speaking communities. 🌙🩸
