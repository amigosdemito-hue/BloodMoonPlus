# 🩸 BloodMoonPlus — Blood Moon

A **Blood Moon** event plugin for Paper servers, featuring **fixed schedules (Mexico time)**, **9 bosses**, **45 items** spread across rarities, enchantments up to **level 7**, **progressive waves**, **placeholders**, multi-language support, **full immersion** (red fog, lightning, ambient sounds and constant announcements) and **rare-loot inventory delivery**.

Built to run smoothly on **40+ player** servers without changing the gameplay.

---

## ✨ Highlights

- **🗓️ Fixed schedules** — `weekly` mode (per weekday at a real time, configurable timezone, defaults to `America/Mexico_City`) or `interval` mode (every N hours). You can set **multiple runs on the same day**.
- **👹 9 bosses** with custom health bars, **enrage** phases, minimum difficulty, abilities and their own loot.
- **🎁 45 items** sorted by rarity: common (always drop), rare, epic, legendary and **mythic** (extremely rare, ~0.1%).
- **📦 Rare loot delivery**: rare, epic, legendary and mythic drops now go straight to the killer's inventory when there is one.
- **⚔️ Custom enchantments** up to **level 7**, each with unique abilities (crit, thunder guard, blast wave, levitate, bleeding…).
- **🌊 Progressive waves** — wave 1: 5 mobs, wave 2: 10, wave 3: 15… up to a configurable cap.
- **🌫️ Immersion** — red fog, lightning when hordes/bosses appear, ambient sounds and periodic announcements. No rain, but thunder.
- **📊 Stats & survivors** — who survived, who dealt the most damage, rankings and MVP rewards.
- **🌍 Multi-language (i18n)** and **per-world configuration**.
- **🔌 Optional integrations** with PlaceholderAPI, Multiverse-Core and WorldGuard (MythicMobs as an alternative mode).
- **🚫 Wither control** — the normal Wither is blocked; only the **Crimson Scourge** boss can spawn.

---

## 📥 Installation

1. Download the `.jar` from the **Versions** tab.
2. Drop it into the `plugins/` folder of your **Paper** server.
3. Restart the server.
4. (Optional) Install **PlaceholderAPI** for placeholders.
5. Configure `plugins/BloodMoonPlus/config.yml` and run `/luna reload`.

> Requires **Paper**. Compatible with Paper **26.2** and **26.3**.

---

## 🎮 Commands

Main command: **`/luna`** (aliases: `/bm`, `/lunadesangre`, `/bloodmoon`).

| Command | Description |
|---|---|
| `/luna next` | Shows the next Blood Moon |
| `/luna start [difficulty]` | Starts the event manually |
| `/luna stop` | Stops the event |
| `/luna difficulty <n>` | Adjusts the difficulty of the current event |
| `/luna boss <id>` | Summons a specific boss |
| `/luna horde` | Forces a wave |
| `/luna killmobs` | Removes the event mobs |
| `/luna give <item> [amount]` | Gives one of the plugin's items |
| `/luna items` | Lists the available items |
| `/luna chances` | Shows the drop chances |
| `/luna reload` | Reloads the configuration |

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
| Crimson Necromancer | 1.5 |
| Ancestral Devastator | 1.5 |
| Night Executioner | 1.5 |
| Abyssal Warden | 2.0 |
| Crimson Scourge (Wither) | 2.5 |

Every boss has a health bar, **enrage** phases and its own loot table. See the project's full **README** for every drop in detail.

---

## 🧩 Placeholders

With **PlaceholderAPI** installed (namespace `%bloodmoon_*%`): event status, time left, next moon, difficulty, live bosses, current wave, survivors, player stats and more.

---

## 🚫 Wither datapack (optional)

The plugin already blocks the normal Wither on its own. If you also want a data-layer safeguard, the project ships the **BloodMoonWitherControl** datapack (in `datapacks/`), which removes any Wither that is **not** the Blood Moon boss. It is optional.

---

## ⚡ Performance

Designed for **~40 player** networks: local (not whole-world) entity scans, item-parse caching, optimized placeholder snapshots and efficient menus/action bars. For very large servers you can tune `horde.wave-max-size` and `horde.max-alive-per-player` in the config.

---

## 🛠️ Build from source

```bash
git clone <your-repository>
cd BloodMoonPlus
./gradlew build
```

The `.jar` is produced in `build/libs/`. Built with **Java 25**.

---

## 🔒 License

**All rights reserved.** The source code is public for reading and security review only; copying, modifying, reusing or redistributing it is **not** allowed. You may download and run the compiled plugin on your server. See the `LICENSE` file for details.

---

## ❤️ Credits

Inspired by — and built to surpass — *BloodMoon-Reborn*. Made with care for Spanish-speaking communities. 🌙🩸
