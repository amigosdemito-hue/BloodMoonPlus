# BloodMoonPlus — Bosses & Loot Guide

🌐 **English** · [Español](README.md)

Everything you need to know about the **10 bosses** of the Blood Moon and the **drop system** (104 items), plus the full catalogue and how the probabilities are calculated.

---

## 🔌 Dependencies

**Required: Paper 26.2 or newer with Java 25.** No other plugin is needed to start.

| Plugin | Tested version | Type | What it adds | If missing |
|---|---|---|---|---|
| **Paper** | 26.2 / 26.3 (built against API 26.3) | Required | Server | The plugin does not load |
| **ExcellentEnchants** + **NightCore** | EE 5.4.3 · NightCore 2.16.7 | Optional, strongly recommended | The custom enchantments (`vampire`, `thunder`, `tunnel`, `veinminer`, `dragon_heart`…) on the 104 items | Items are created with vanilla enchantments only and the console warns; items already handed out with EE enchantments are lost on join (Minecraft deletes unknown enchantments) |
| **PlaceholderAPI** | 2.11.6 | Optional | The `%bloodmoon_*%` placeholders | Everything else works; there are simply no placeholders |
| **MythicMobs** | 5.12+ | Optional | Bosses with `mode: MYTHICMOBS` in `bosses.yml` | The vanilla boss is used when `mythic-fallback: true`; with the fallback disabled that boss does not appear |

- They are declared as `softdepend` in `plugin.yml`: load order is correct without forcing you to install them (no hard dependencies).
- **NightCore is not used directly**: it is a dependency of ExcellentEnchants, not of this plugin.
- The MythicMobs bridge uses reflection (5.x): on another major version the plugin warns and keeps running with the vanilla bosses.

---

## 🔔 Discord alerts

The plugin can post to a Discord channel **how long until the Blood Moon, when it starts and when it ends**, without a bot or a token: it uses a channel **webhook**.

### Create the webhook

1. In Discord: **Channel settings → Integrations → Webhooks → New webhook**.
2. Pick the channel, give it a name and click **Copy Webhook URL**.
3. Paste it into `plugins/BloodMoonPlus/config.yml`:

```yaml
discord:
  enabled: true
  webhook-url: "https://discord.com/api/webhooks/123456789/abcdef..."
  username: "Blood Moon"
  avatar-url: ""
  warnings-minutes: [120, 60]
  notify:
    warnings: true
    start: true
    end: true
```

4. `/luna reload`. The console confirms with `Avisos de Discord activados (antes: [120, 60] min)`.

> `warnings-minutes` accepts any minutes (`[120, 60]` = two hours and one hour before, the new
> default; `[60, 30, 10, 5]` still works). Chat warnings are configured separately in
> `schedule.warnings-minutes`.

### What gets posted

Every alert is a Discord **embed**, not a line of text: emoji and a short title, one atmospheric
line and the data in inline fields (when, difficulty, world, duration). Times use Discord
timestamps (`<t:...:R>`), so each person sees a live countdown in their own language.

| Alert | When | What it shows |
|---|---|---|
| Countdown | At every `warnings-minutes` value, only while no moon is running | Live countdown, start time and difficulty |
| Start | When the event begins (once, even with several worlds) | World, difficulty, duration and when it ends |
| End | When it finishes on its own or via `/luna stop` | World, duration and result (`Dawn` or `Stopped by an administrator`) |

- The texts live in `locales/<language>.yml` (`discord.*`), so they follow the server's active language.
- **While a moon is already running the next countdown is not posted**: a “10 min left” used to arrive in the middle of the event.
- Delivery runs off the main thread: a slow, down or rate-limited Discord never stalls the event; it only leaves a console warning.
- The URL is validated on load (only `discord.com`, `canary`, `ptb` or `discordapp`). A bad paste disables the feature with a console warning instead of failing on every event.
- `allowed_mentions` is empty, so a text containing `@everyone` never pings the channel.
- To test without Discord, point `webhook-url` at `http://127.0.0.1:PORT/...`; loopback is accepted for local testing.

---

## 🛑 Turning the Blood Moon off and on

```
/luna disable     → no more events until you enable it again
/luna enable      → the normal calendar comes back
```

- Turns the event off **server-wide**: the schedule (weekly and interval) stops firing, and neither
  the Discord countdown nor the 10/5/1-minute chat warning is sent.
- `/luna start` is refused while it is disabled (the other admin commands keep working: `/luna boss`,
  `/luna horde`, `/luna killmobs`, `/luna reload`…).
- A moon **already running** finishes on its own; use `/luna stop` to cut it short.
- The state lives in `data.yml`, so a restart does **not** switch it back on by itself. `/luna status`
  and `/luna next` say that it is disabled, as do the `%bloodmoon_next%` placeholders (`_when`,
  `_in`, `_in_seconds`, `_difficulty`) and `%bloodmoon_disabled%`. Spanish aliases:
  `/luna desactivar` and `/luna activar`. Requires `bloodmoon.admin`.

---

# 🩸 PART 1 — BOSSES

## How bosses work

- Each boss has its own **health bar** (color and style configurable).
- They spawn based on the **event difficulty**: every boss declares a minimum difficulty; if the event doesn't reach it, that boss won't appear.
- On death they drop their **loot table** + experience, and the player who dealt the **most damage** gets an extra reward.
- All bosses carry the `bloodmoon_boss` tag (scoreboard tag), which the plugin and the datapack use to recognize them.
- Health and damage are multiplied by the event difficulty. **Maximum health is capped at 1024** (the game limit).

### Spawn schedule

| Boss | Seconds after start | Chance | Min. difficulty |
|------|--------------------|--------|-----------------|
| Bloody Zombie King | 90 s | 100% | 1.0 |
| Bone Sentinel | 180 s | 70% | 1.0 |
| Crimson Necromancer | 240 s | 70% | 1.5 |
| Ancestral Devastator | 300 s | 60% | 1.5 |
| Night Executioner | 330 s | 60% | 1.5 |
| Shadow Hunter | 360 s | 50% | 1.0 |
| Crimson Archer | 420 s | 60% | 1.0 |
| Charged Absorber | 270 s | 70% | 1.0 |
| Abyssal Warden | 540 s | 80% | 2.0 |
| **Crimson Scourge** | 660 s | 50% | 2.5 |

## Overview of the 10 bosses

| ID | Mob | Health | Damage | Scale | Phase 2 | EXP |
|----|-----|--------|--------|-------|---------|-----|
| `rey_zombi` | ZOMBIE | 300 | 2.0 | 1.6× | — | 500 |
| `centinela_hueso` | SKELETON (white) | 280 | 2.0 | 1.6× | — | 550 |
| `nigromante_carmesí` | EVOKER | 320 | 2.2 | 1.5× | — | 600 |
| `cazador_sombras` | STRAY | 260 | 2.8 | 1.3× | — | 500 |
| `devastador_sangre` | RAVAGER | 380 | 2.4 | 1.6× | ✅ 45% | 700 |
| `arquera_carmesi` | SKELETON | 220 | 1.8 | 1.4× | — | 400 |
| `verdugo_noche` | WITHER_SKELETON | 400 | 2.5 | 1.8× | ✅ 40% | 800 |
| `warden_abismal` | WARDEN | 750 | 3.6 | 1.2× | ✅ 50% | 1100 |
| `creeper_cargado` | CREEPER (charged) | 180 | 2.2 | 2.0× | ✅ 40% | 650 |
| `azote_carmesi` | WITHER | 900 | 4.0 | 1.0× | ✅ 50% | 1500 |

## Detailed profiles

### 👑 Bloody Zombie King (`rey_zombi`)
- **Mob:** ZOMBIE with a full netherite armor set + netherite sword, scale 1.6×, knockback resistance 0.6.
- **Bar:** red, segmented ×10.
- **Abilities:** summons 4 zombies every 25 s · SLAM (radius 7, damage 4, knockback) every 12 s.
- **Spawn:** 90 s, 100%, minimum difficulty 1.0. **EXP:** 500.
- **Loot:** table `boss_rey_zombi`.

### 🛡️ Bone Sentinel (`centinela_hueso`)
- **Mob:** **white** skeleton with a **shield + netherite sword**, full diamond armor. Same height as the Zombie King (scale 1.6×), knockback resistance 0.5.
- **Bar:** white, segmented ×10.
- **Abilities:** GUARD (blocks with the shield for 4 s every 18 s: resistance + absorption) · SLAM (radius 6, damage 5) · summons 3 skeletons every 30 s.
- **Spawn:** 180 s, 70%, minimum difficulty 1.0. **EXP:** 550.
- **Loot:** table `boss_arquera`.

### 🔮 Crimson Necromancer (`nigromante_carmesí`)
- **Mob:** EVOKER holding a totem and a golden helmet, scale 1.5×.
- **Bar:** purple, segmented ×10.
- **Abilities:** summons 5 zombies every 18 s · summons 3 spiders every 30 s · AURA of weakness (8 m) · lightning every 40 s.
- **Spawn:** 240 s, 70%, minimum difficulty 1.5. **EXP:** 600.
- **Loot:** table `boss_rey_zombi`.

### 🏹 Shadow Hunter (`cazador_sombras`)
- **Mob:** STRAY with a bow and leather chestplate. The fastest of all (speed 1.4), scale 1.3×.
- **Bar:** black, segmented ×10.
- **Abilities:** AURA of invisibility for 8 s every 20 s (vanishes and reappears) · lightning every 25 s.
- **Spawn:** 360 s, 50%, minimum difficulty 1.0. **EXP:** 500.
- **Loot:** table `boss_arquera`.

### 💀 Night Executioner (`verdugo_noche`)
- **Mob:** WITHER_SKELETON with a netherite axe and netherite helmet. **The tallest** (scale 1.8×), knockback resistance 0.8.
- **Bar:** pink, segmented ×12.
- **Abilities:** AURA of wither for 6 s (12 m) · SLAM (radius 8, damage 5) · summons 3 Nether skeletons every 30 s.
- **Phase 2 (enraged) at 40%** health: abilities at 65% cooldown, damage ×1.5.
- **Spawn:** 330 s, 60%, minimum difficulty 1.5. **EXP:** 800.
- **Loot:** table `boss_verdugo`.

### 🐗 Ancestral Devastator (`devastador_sangre`)
- **Mob:** RAVAGER with no equipment, scale 1.6×, knockback resistance 0.9 (barely moves).
- **Bar:** red, segmented ×12.
- **Abilities:** CHARGE every 14 s (launches at full speed toward a player and tramples for ~1 s) · slowness roar for 6 s (14 m) · SLAM (radius 7, damage 5).
- **Phase 2 (enraged) at 45%** health: abilities at 60% cooldown, damage ×1.6.
- **Spawn:** 300 s, 60%, minimum difficulty 1.5. **EXP:** 700.
- **Loot:** table `boss_verdugo`.

### 🏹 Crimson Archer (`arquera_carmesi`)
- **Mob:** SKELETON with a diamond helmet and chestplate, scale 1.4×.
- **Bar:** purple, segmented ×10.
- **Abilities:** summons 3 lightning bolts on nearby players every 15 s · AURA of slowness for 5 s (12 m).
- **Spawn:** 420 s, 60%, minimum difficulty 1.0. **EXP:** 400.
- **Loot:** table `boss_arquera`.

### 🌊 Abyssal Warden (`warden_abismal`)
- **Mob:** WARDEN, scale 1.2×, full knockback resistance (1.0). **750 health.**
- **Bar:** blue, segmented ×12.
- **Abilities:** SONIC BOOM every 12 s (radius 11, damage 13, throws players far away) · PULL (pulls within 15 m, force 1.4) every 20 s · **darkness** roar for 6 s (16 m) · SLAM (radius 8, damage 7).
- **Phase 2 (enraged) at 50%** health: abilities at 60% cooldown, damage ×1.6.
- **Spawn:** 540 s, 80%, minimum difficulty 2.0. **EXP:** 1100.
- **Loot:** table `boss_verdugo`.

### 🟩 Charged Absorber (`creeper_cargado`) — charged creeper
- **Mob:** a **charged (electric) CREEPER** **twice as tall** as a normal one (scale 2.0×). **180 health**, damage 2.2, speed 1.05, knockback resistance 0.6.
- **Bar:** green, segmented ×10.
- **Abilities:** **VORTEX** every 16 s (radius 18 m, 4 s of absorption, force 1.6, 2.5 damage on contact) · SLAM (radius 6, damage 4) · CHARGE (force 1.2, 6 damage).
- **Phase 2 (enraged) at 40%** health: abilities at 60% cooldown, damage ×1.5.
- **Spawn:** 270 s, 70%, minimum difficulty 1.0. **EXP:** 650.
- **Loot:** table `boss_verdugo`.
- **Note:** it **does not explode**. `powered: true` only gives it the charged look: the fuse is disabled, so it never kills itself or breaks the terrain. Its threat is VORTEX, which **absorbs** players (drags them toward it for 4 s) and damages them on contact.

### ☠️ Crimson Scourge (`azote_carmesi`) — FINAL BOSS
- **Mob:** WITHER, knockback resistance 0.95 (won't budge). **900 health** (up to 1024 after the difficulty multiplier), damage 4.0, speed 1.25.
- **Bar:** purple, segmented ×12.
- **Abilities:** WITHER SKULLS every 7 s (**breaks blocks**, like the vanilla Wither) · PULL (pulls within 20 m) every 16 s · AURA of wither for 8 s (16 m) · summons 4 Nether skeletons every 20 s · darkness roar for 6 s · **SELF_HEAL** (heals 80 health every 24 s).
- **Phase 2 (enraged) at 50%** health: abilities at **55%** cooldown, damage **×1.75** — the server's damage peak.
- **Spawn:** 660 s, 50%, minimum difficulty 2.5. **EXP:** 1500.
- **Loot:** table `boss_verdugo`.
- **Note:** it is the **only Wither allowed**. The plugin cancels the spawn of any other Wither (through a spell, egg or spawner); the optional datapack `BloodMoonWitherControl` finishes it off every tick as a second safety layer. Its skulls break blocks: protect your base or fight in the open.

## Second phase (enraged)

5 bosses have a configurable phase 2 in `bosses.yml`:

| Boss | Triggers at... | Cooldown | Damage |
|------|----------------|----------|--------|
| `verdugo_noche` | 40% health | ×0.65 | ×1.5 |
| `devastador_sangre` | 45% health | ×0.6 | ×1.6 |
| `warden_abismal` | 50% health | ×0.6 | ×1.6 |
| `creeper_cargado` | 40% health | ×0.6 | ×1.5 |
| `azote_carmesi` | 50% health | ×0.55 | ×1.75 |

When it triggers (once per boss):
1. It gains **Speed II + Strength II**.
2. **All its abilities recharge instantly** and fire more often.
3. A red particle burst + dragon roar, and nearby players are told: *"¡{boss} ENLOQUECIÓ!"* ("{boss} went mad!").

## Boss abilities (the `bosses.yml` DSL)

| Ability | Format | Effect |
|---------|--------|--------|
| `SUMMON` | `cd:amount:TYPE` | Summons reinforcements |
| `SLAM` | `cd:radius:damage` | Area hit with knockback |
| `LIGHTNING` | `cd:targets:damage` | Lightning on nearby players |
| `AURA` | `cd:radius:effect:sec:level` | Potion effect in an area (wither, slowness...) |
| `SONIC_BOOM` | `cd:radius:damage` | Shockwave that throws players far away (Warden style) |
| `PULL` | `cd:radius:force` | Pulls players toward the boss |
| `VORTEX` | `cd:radius:sec:force:damage` | **Absorption**: drags players toward the boss for several seconds and damages them on contact |
| `SKULL` | `cd:amount` | Wither skulls that **break blocks** |
| `CHARGE` | `cd:force:damage` | Charge: launches forward and tramples ~1 s |
| `ROAR` | `cd:radius:effect:sec:level` | Roar with an area effect (darkness...) |
| `GUARD` | `cd:seconds` | Block: resistance + absorption |
| `SELF_HEAL` | `cd:amount` | Heals health (tank boss) |

## Loot per boss

Bosses use their own tables with **weight 0 on Common and Uncommon**: they only drop **Rare or
better**, and always include **guaranteed** drops on top of the rolls.

| Table | Used by | Guaranteed | Rolls | Rare | Epic | Leg. | Mythic |
|---|---|---|---|---|---|---|---|
| `boss_rey_zombi` | Zombie King, Necromancer | Bottle o' Enchanting ×8-16, Diamond ×3-6 | 4-5 | 48.7% | 36.5% | 13.4% | 1.5% |
| `boss_arquera` | Archer, Hunter, Sentinel | Arrows ×32-64, Diamond ×2-5 | 4-5 | 48.0% | 36.0% | 14.4% | 1.7% |
| `boss_verdugo` | Executioner, Devastator, Warden, Maw, **Scourge** | Netherite scrap ×3-6, Bottle o' Enchanting ×6-12 | 5-6 | 31.7% | 45.2% | 20.4% | 2.7% |
| `boss_top_damage` | Top damage against any boss | — | 1 | — | 38.2% | 47.8% | 14.0% |

> Percentages are the rarity roll **before** each item's own drop-chance. The harder the boss,
> the better the table: the **Crimson Scourge** uses the best one (`boss_verdugo`).

---

# 💎 PART 2 — LOOT

## How the drop probability works

Every horde mob death goes through **5 steps**:

```
 [1] does it drop?     table drop-chance × difficulty
 [2] how many rolls?   rolls (1 horde, 4-6 bosses)
 [3] which rarity?     rarity weight (+ luck + pity)
 [4] which item?       item weight inside the rarity
 [5] does it land?     the item's own drop-chance
```

### Step 1 — does the mob drop anything?

`real chance = drop-chance × (1 + (difficulty − 1) × 0.25)`

With `mob_horde` at 50%: **x1 → 50%**, x2 → 62.5%, x3 → 75%, x10 → 100%.

### Step 2 — how many rolls?

`mob_horde` rolls **1** item (only half of the mobs drop anything); elites and survivors 2-3; bosses 4-6. Every roll
is independent and will not repeat an item already given in the same batch.

### Step 3 — rarity roll

| Rarity | Weight in `mob_horde` | Real % per roll | Global announce |
|---|---|---|---|
| Common | 500 | **49.63%** | — |
| Uncommon | 360 | **35.73%** | — |
| Rare | 110 | **10.92%** | — |
| Epic | 32 | **3.18%** | — |
| Legendary | 5 | **0.50%** | yes |
| Mythic | 0.5 | **0.05%** | yes |

**Luck:** Rare, Epic, Legendary and Mythic multiply their weight by `1 + (difficulty − 1) × 0.50`.
At x3 they weigh twice as much.

### Step 4 — item roll inside the rarity

Every item of that rarity competes by weight. The odds of each one are in
[`DROPS.en.md`](DROPS.en.md), where the **Real %** column already includes its drop-chance.

### Step 5 — the item's own drop-chance

If the chosen item fails its own `drop-chance`, **that roll gives nothing**. Commons always drop;
broken rare weapons sit at 5%%; mythics at 0.5-0.8%%.

### Pity per table and rarity

Every failed roll of a rarity adds 1 point **in that table**; the weight is multiplied by
`1 + points × pity-per-roll` and resets when it drops.

| Rarity | Pity per roll | Max points | Max multiplier |
|---|---|---|---|
| Common | — | — | — |
| Uncommon | — | — | — |
| Rare | — | — | — |
| Epic | +0.02 | 150 | ×4.0 |
| Legendary | +0.05 | 200 | ×11.0 |
| Mythic | +0.08 | 300 | ×25.0 |

> Pity used to be global per rarity: grinding horde mobs ended up handing mythics at bosses.
> Now every table tracks its own streak.

### Balance rules

- **No copy limit**: no cap and no serial number. If it drops, it is yours.
- **No curses**: on death it falls to the ground (mythics too) and anyone can pick it up.
- **A batch never repeats an item** while the table still has alternatives.
- **Weights are validated on load**: negative values, `NaN` or `drop-chance > 1` are reported and fixed.
- **Every item drops somewhere**: a test enforces it (the storm trident used to exist without
  appearing in any table).
- **Legendary and Mythic glow** on the ground and ignore lava and explosions.

---

## 📦 Full catalogue (104 items)

**88 gear pieces** (14 classes × 6 rarities) + **16 stackable materials**.

| Rarity | Items | What it holds |
|---|---|---|
| Common | 20 | basic materials and iron gear |
| Uncommon | 19 | useful materials and iron gear |
| Rare | 18 | diamonds, ender pearls and weapons with 1-2 custom enchantments |
| Epic | 19 | totem, netherite and pieces with 2-3 custom enchantments |
| Legendary | 14 | one piece per class, 3-4 customs and a global announce |
| Mythic | 14 | one piece per class, 5-6 customs, glow and global announce |

### Versions per class

| Class | Common | Uncommon | Rare | Epic | Legendary | Mythic |
|---|---|---|---|---|---|---|
| **Sword** | Espada Oxidada | Espada de Hierro Lunar | Espada Sanguinaria, Daga de las Sombras | Espada de la Ruina | Colmillo Vampírico | Hoja del Eclipse Carmesí |
| **Axe** | Hacha Oxidada | Hacha de Hierro Lunar | Hacha Sangrienta | Hacha de la Ventisca, Hacha del Verdugo | Hacha del Abismo | Hacha del Apocalipsis |
| **Pickaxe** | Pico Oxidado | Pico de Hierro Lunar | Pico Umbrío | Pico del Abismo | Pico Espectral | Pico del Eclipse |
| **Shovel** | Pala Oxidada | Pala de Hierro Lunar | Pala Sangrienta | Pala del Abismo | Pala Espectral | Pala del Eclipse |
| **Hoe** | Azada Oxidada | Azada de Hierro Lunar | Azada Sangrienta | Azada del Abismo | Azada Espectral | Guadaña del Segador |
| **Bow** | Arco Astillado | Arco de Hueso Lunar | Arco Carmesí, Arco de la Tormenta | Arco del Abismo | Arco Lunar | Arco del Eclipse |
| **Crossbow** | Ballesta Astillada | Ballesta de Hueso Lunar | Ballesta Carmesí | Ballesta del Abismo | Ballesta del Alba | Ballesta del Eclipse |
| **Trident** | Tridente Oxidado | Tridente de los Mares | Tridente Sangriento | Tridente Profundo | Tridente de la Tormenta | Tridente del Abismo Rojo |
| **Mace** | Maza Oxidada | Maza Pesada | Maza de Hueso | Maza Carmesí | Martillo del Trueno | Maza del Coloso |
| **Helmet** | Casco de Hierro Gastado | Casco de Hierro Lunar | Casco de Hierro Carmesí | Yelmo del Cazador de Sangre | Yelmo del Eclipse | Corona de la Luna de Sangre |
| **Chestplate** | Peto de Hierro Gastado | Peto de Hierro Lunar | Peto de Hierro Carmesí | Peto del Cazador de Sangre | Peto del Guardián Carmesí | Peto del Eclipse Carmesí |
| **Leggings** | Grebas de Hierro Gastadas | Grebas de Hierro Lunar | Grebas de Hierro Carmesí | Grebas del Cazador de Sangre | Grebas del Titán | Grebas del Eclipse |
| **Boots** | Botas de Hierro Gastadas | Botas de Hierro Lunar | Botas de Hierro Carmesí | Botas del Cazador de Sangre, Botas del Estrondo | Botas del Noctámbulo | Botas del Eclipse |
| **Shield** | Escudo Astillado | Escudo Reforzado | Escudo Carmesí | Escudo del Guardián | Égida Carmesí | Égida del Eclipse |

### The 14 mythics

| Item | Class | Enchantments |
|---|---|---|
| Hoja del Eclipse Carmesí | Sword | sharpness V · thunder V · looting III · paralyze III · sweeping_edge III · unbreaking III · vampire III · double_strike II · fire_aspect II · wither II · mending I |
| Hacha del Apocalipsis | Axe | efficiency V · sharpness V · thunder V · unbreaking III · vampire III · decapitator II · double_strike II · mending I · treefeller I |
| Pico del Eclipse | Pickaxe | efficiency V · smelter V · fortune III · tunnel III · unbreaking III · veinminer III · blast_mining II · mending I · telekinesis I |
| Pala del Eclipse | Shovel | efficiency V · smelter V · fortune III · haste III · tunnel III · unbreaking III · mending I · telekinesis I |
| Guadaña del Segador | Hoe | efficiency V · smelter V · haste III · unbreaking III · mending I · replanter I · telekinesis I |
| Arco del Eclipse | Bow | power V · dragonfire_arrows III · explosive_arrows III · unbreaking III · vampiric_arrows III · electrified_arrows II · punch II · sniper II · flame I · infinity I · mending I |
| Ballesta del Eclipse | Crossbow | piercing IV · dragonfire_arrows III · electrified_arrows III · explosive_arrows III · quick_charge III · unbreaking III · vampiric_arrows III · sniper II · mending I · multishot I |
| Tridente del Abismo Rojo | Trident | impaling V · infernus III · loyalty III · restore III · unbreaking III · channeling I · mending I · soulbound I |
| Maza del Coloso | Mace | density V · breach IV · restore III · unbreaking III · wind_burst III · mending I · soulbound I |
| Corona de la Luna de Sangre | Helmet | protection V · elemental_protection IV · respiration III · thorns III · unbreaking III · saturation II · mending I · night_vision I · water_breathing I |
| Peto del Eclipse Carmesí | Chestplate | dragon_heart V · protection V · elemental_protection IV · fire_shield IV · cold_steel III · thorns III · unbreaking III · hardened II · mending I |
| Grebas del Eclipse | Leggings | protection V · elemental_protection IV · stopping_force III · swift_sneak III · thorns III · unbreaking III · mending I |
| Botas del Eclipse | Boots | protection V · elemental_protection IV · feather_falling IV · depth_strider III · soul_speed III · unbreaking III · flame_walker II · jumping II · speed II · mending I · rebound I |
| Égida del Eclipse | Shield | restore III · unbreaking III · mending I · soulbound I |

The full list with amounts, drop-chance and final probability is in
[`DROPS.en.md`](DROPS.en.md).

---

## 🌍 Languages

The plugin ships **7 complete languages**, selected in `plugins/BloodMoonPlus/locales/locales.yml`:

| Code | Language |
|---|---|
| `es` | Español |
| `en` | English |
| `pt` | Português |
| `fr` | Français |
| `de` | Deutsch |
| `it` | Italiano |
| `ru` | Русский |

```yaml
Language: es            # table code
UseBundledLocales: true # missing keys fall back to English
```

- All 7 files are copied to `plugins/BloodMoonPlus/locales/` on first start.
- `/luna reload` applies the change live.
- A test checks that all 7 languages have the same keys and placeholders.

---

## 🛡️ Blood Moon item protection

| Action | Allowed? |
|---|---|
| Add enchantments (table, books, smithing) | ✅ Yes |
| Rename on the anvil | ✅ Yes |
| Repair / upgrade | ✅ Yes |
| **Strip them on a grindstone** (disenchant) | ❌ No |
| **Lose enchantments, rarity or owner** | ❌ No |

- The **grindstone** cancels the result and warns the player; a piece without enchantments can
  still be repaired.
- **Anvil** and **smithing table** keep enchantments, rarity, owner and unbreakability even when
  the combination would have dropped them.
- **Automatic refresh**: on join, when picking the item up and when taking it out of a chest or
  shulker, the `items.yml` enchantments are applied, levels clamped and the lore regenerated
  (description, rarity and hunter). Durability, custom names and extra enchantments are kept.
- **`/luna refresh <player|all>`** applies it instantly to inventory, armor, off hand and ender
  chest. Requires `bloodmoon.admin`.

---

## 🔧 Check probabilities in game

```
/luna chances mob_horde
/luna chances boss_verdugo
```

It shows each item's **final** probability (rarity × weight × drop-chance), the guaranteed drops
and your current pity in that table.

---

## 🔄 Updating from 1.0.2 to 1.0.3

1.0.3 replaces the old item abilities with ExcellentEnchants enchantments, raises the vanilla cap
to 5 on legendaries and mythics, and the catalogue grows to **104 items**. Your server's
`items.yml` is **not replaced automatically**: the plugin only writes it when it does not exist.

> Since **1.0.9** the descriptions no longer need to be copied by hand: on startup the plugin
> merges **only the `lore:` blocks** of the catalogue into your `items.yml` (names, enchantments,
> rarities, your own items and your comments stay as they are) and writes an
> `items.yml.pre-vN.bak` copy first. That automatic step covers the text only; for any other
> catalogue change, the steps below still apply.
>
> The same happens with **`config.yml`**: on startup the new keys (`cleanup`, `discord`,
> `items.migrate-on-join`…) are **appended** to the end of their section, with their comments, and a
> `config.yml.pre-vN.bak` copy is written first. Your values, comments and own keys are never
> touched, and the result is validated before saving (if any of your values would change, nothing
> is written).

1. Stop the server and back up `plugins/BloodMoonPlus/items.yml`.
2. Replace it with the one inside the `.jar` (or rename it and start once) and re-apply your
   customizations.
3. Start **with ExcellentEnchants and NightCore** and check the console: `Items cargados: 104` and
   no warnings about unknown or unsupported enchantments.
4. Run `/luna refresh all` (or let players reconnect or pick their items up).

### Changes in `data.yml` and `loot.yml`

- Copy counters and serial numbers are no longer stored: any `created.*` keys left in `data.yml`
  are ignored and can be deleted.
- **Pity** is now stored per table (`pity.<uuid>.<table>.<rarity>`). Values in the old format
  (`pity.<uuid>.<rarity>`) are ignored: every player starts at 0.
- Items handed out with a serial number keep that lore line until the next refresh, which removes
  it (it is no longer used).
- `loot.yml` adds the storm trident to the horde table and documents the new pity.

> ⚠️ Do not remove ExcellentEnchants while items are out there: Minecraft deletes items whose
> enchantment does not exist. Run `/luna refresh all` on a server where EE has loaded first.

---

## 📜 License

**All rights reserved.** The code is public for reading and security auditing only. See
[`LICENSE`](LICENSE).

---

## 📤 Publishing on Modrinth

Ready-to-publish material lives in [`modrinth/`](modrinth/). Start with
[`modrinth/PUBLISH.md`](modrinth/PUBLISH.md).
