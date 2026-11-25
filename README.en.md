# BloodMoonPlus — Bosses & Loot Guide

🌐 **English** · [Español](README.md)

Everything you need to know about the **9 bosses** of the Blood Moon and the **drop system** (45 items).

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

Bosses use their own tables with a **weight of 0 for Common and Uncommon**: they only drop **Rare or better**, and always have **guaranteed** drops on top of the rolls.

| Table | Used by | Guaranteed | Rolls | Rare | Epic | Leg. | Mythic |
|-------|---------|------------|-------|------|------|------|--------|
| `boss_rey_zombi` | Zombie King, Necromancer | XP Bottle 8-16, Diamond 3-6 | 4-5 | 48.7% | 36.5% | 13.4% | 1.5% |
| `boss_arquera` | Archer, Hunter, Sentinel | Arrows 32-64, Diamond 2-5 | 4-5 | 48.0% | 36.0% | 14.4% | 1.7% |
| `boss_verdugo` | Executioner, Devastator, Warden, **Scourge** | Netherite Scrap 3-6, XP Bottle 6-12 | 5-6 | 31.7% | 45.2% | 20.4% | 2.7% |
| `boss_top_damage` | Top damage against any boss | — | 1 | — | 38.2% | 47.8% | 14.0% |

> The percentages are the rarity roll **before** each item's individual drop-chance
> (mythics still apply their 0.5-0.8%). The stronger the boss, the better the table:
> the **Crimson Scourge** uses the best one (`boss_verdugo`).

---

# 💎 PART 2 — LOOT

## How drop probability works

Every horde mob kill goes through **5 steps**. If you understand these 5, you understand all the loot:

```
 mob kill
      │
      ▼
 [1] DOES IT DROP ANYTHING?  ── No (90% at x1) ──▶ nothing
      │ Yes (10%)
      ▼
 [2] HOW MANY ROLLS?  ── rolls 1 or 2
      │
      ▼
 [3] WHICH RARITY?  ── weighted roll (Common 59.6% ... Mythic 0.05%)
      │
      ▼
 [4] WHICH ITEM?  ── weighted roll within that rarity
      │
      ▼
 [5] DOES IT DROP?  ── the item's individual drop-chance
      │               │
     Yes            No ──▶ the roll gives nothing
      ▼
    🎁 item
```

### Step 1 — Does the mob drop anything? (table probability)

Every loot table has a base probability that is multiplied by the event difficulty:

```
real probability = table × (1 + (difficulty − 1) × 0.25)
```

| Difficulty | Multiplier | `mob_horde` (10%) | `mob_elite` (45%) |
|------------|------------|-------------------|-------------------|
| x1 | ×1.00 | 10% | 45% |
| x2 | ×1.25 | 12.5% | 56% |
| x3 | ×1.50 | 15% | 67.5% |
| x5 | ×2.00 | 20% | 90% |
| x10 | ×3.25 | 32.5% | 100% |

> An event scheduled with `difficulty: 3` gives **half again** as much loot as a normal one.

### Step 2 — How many rolls?

If step 1 passed, the table rolls: in `mob_horde` it is `1-2`, meaning **a random number between 1 and 2** items (average 1.5). Each roll is **independent** and goes through steps 3-5 on its own. Elite mobs and survivors roll 2-3; bosses 4-6.

### Step 3 — Rarity roll (relative weights)

Not all colors weigh the same. This is the real roll for `mob_horde`:

| Rarity | Weight | **Real % per roll** |
|--------|--------|---------------------|
| Common | 600 | **59.6%** |
| Uncommon | 260 | **25.8%** |
| Rare | 110 | **10.9%** |
| Epic | 32 | **3.2%** |
| Legendary | 5 | **0.50%** |
| Mythic | 0.5 | **0.05%** |

**Luck:** rarities flagged as `luck-affected` (Rare and above) multiply their weight by `1 + (difficulty − 1) × 0.5`. In a x3 event, Rare+ **doubles** its weight: the harder the event, the better the loot.

### Step 4 — Item roll within the rarity

All items of that rarity compete by weight. Example, if **Rare** comes up (weights 40/30/15/15/14/14/12):

- Diamond: 28.6% · Ender Pearl: 21.4% · Blood Sword: 10.7% · Crimson Bow: 10.7% · Dagger: 10% · Storm Bow: 10% · Umbral Pickaxe: 8.6%

### Step 5 — The item's individual drop-chance (key!)

The rolled item has its **own** drop probability. **If it fails, that roll gives nothing** (it is not replaced by another item):

| Drop-chance | Items | Result |
|-------------|-------|--------|
| 100% | Commons and simple materials | The rolled item **always drops** |
| 25-50% | Diamond, pearls, obsidian | Drops most of the time |
| **5%** | **Broken Rare/Legendary weapons** | Capped on purpose |
| 10-20% | Epics | Low probability |
| 0.5-0.8% | Mythics | "Pink sheep" |

> That's why commons are 100%: if the roll gave you a Common, there is no second chance to fail.
> And that's why a Blood Sword at 5% is **far** rarer than its 10.7% weight suggests:
> 10.7% × 5% = **0.54% real** once Rare has already come up.

### Luck and pity (bad-luck protection)

- **Luck:** weight of Rare+ × `1 + (difficulty − 1) × 0.5` (only pays off in hard events).
- **Pity:** every roll that does **not** give you a high rarity adds 1 point *for you*. Multiplier: `1 + points × pity-per-roll`. When you get it, it **resets to 0**.

| Rarity | Pity per failed roll | Max points | Max multiplier |
|--------|----------------------|------------|----------------|
| Epic | +0.02 | 150 | ×4 |
| Legendary | +0.05 | 200 | ×11 |
| Mythic | +0.08 | 300 | ×25 |

> Pity is **per player and per rarity**: if you've gone 300 rolls without a mythic, your mythic chance is 25 times the base. Bosses and the survivor reward speed this up enormously.

### Real example: 100 kills in a x1 event

1. ~10 kills pass step 1 (10%).
2. Those 10 generate **~15 rolls** (average 1.5 per kill).
3. Expected result of those 15 rolls:

| Comes up... | Rolls | Real items (after their drop-chance) |
|-------------|-------|--------------------------------------|
| Common | 8.9 | ~8.9 items (always drop) |
| Uncommon | 3.9 | ~3.1 items |
| Rare | 1.6 | ~0.45 items (diamonds and pearls almost always) |
| Epic | 0.48 | ~0.06 items |
| Legendary | 0.075 | ~0.004 items |
| Mythic | 0.0075 | almost nothing (mythics come from bosses) |

> In short: **~12 items per 100 kills**, mostly food and materials, occasionally a diamond.
> The real loot is in the **bosses** (guaranteed Rare+ tables), the **survivor reward**
> (100%, 2-3 rolls) and the **top-damage prize** (Epic+ only).

### Global balance rules

- **Mythics:** at most **3 copies on the whole server** (serial number in the lore); once exhausted, they stop dropping.
- **Broken Rare/Legendary weapons capped at 5%**: Blood Sword, Crimson Bow, Shadow Dagger, Storm Bow and Vampiric Fang.
- **Rare-or-higher weapons, tools and armor have the Curse of Vanishing**: if you die with them, they disappear (a real risk to carrying them). *(Since 1.0.2.)*
- **Enchantments:** the level never exceeds **7** (trimmed on load).
- Kills only count when a player is nearby (`require-player-kill`).
- Legendary and Mythic items **glow** when dropped and ignore lava and explosions.

### Rarity distribution per table

Step 3 percentages in each table (before the individual drop-chance):

| Table | Common | Uncommon | Rare | Epic | Leg. | Mythic |
|-------|--------|----------|------|------|------|--------|
| `mob_horde` (10%, 1-2 rolls) | 59.6% | 25.8% | 10.9% | 3.2% | 0.50% | 0.05% |
| `mob_elite` (45%, 2-3 rolls) | 26.5% | 33.1% | 26.5% | 11.9% | 1.85% | 0.16% |
| `supervivientes` (100%, 2-3 rolls) | 33.1% | 33.1% | 22.1% | 9.9% | 1.55% | 0.13% |
| `boss_rey_zombi` (100%, 4-5) | — | — | 48.7% | 36.5% | 13.4% | 1.5% |
| `boss_arquera` (100%, 4-5) | — | — | 48.0% | 36.0% | 14.4% | 1.7% |
| `boss_verdugo` (100%, 5-6) | — | — | 31.7% | 45.2% | 20.4% | 2.7% |
| `boss_top_damage` (100%, 1) | — | — | — | 38.2% | 47.8% | 14.0% |

### Rarity reference

| Rarity | Luck | Pity | Global warning | Glows on drop |
|--------|------|------|----------------|---------------|
| Common | — | — | — | — |
| Uncommon | — | — | — | — |
| Rare | ✅ | — | — | — |
| Epic | ✅ | 0.02/roll | — | — |
| Legendary | ✅ | 0.05/roll | ✅ | ✅ |
| Mythic | ✅ | 0.08/roll | ✅ | ✅ |

---

## 📦 Full item list (45)

### Commons (always drop)

| Item | Material | Amount |
|------|----------|--------|
| Meat | COOKED_BEEF | 3-8 |
| Arrows | ARROW | 6-16 |
| Gunpowder | GUNPOWDER | 2-6 |
| Iron | IRON_INGOT | 2-5 |
| Bone | BONE | 2-6 |
| String | STRING | 1-4 |

### Uncommons

| Item | Material | Amount | Drop |
|------|----------|--------|------|
| Gold | GOLD_INGOT | 2-4 | 90% |
| Golden Apple | GOLDEN_APPLE | 1 | 85% |
| XP Bottle | EXPERIENCE_BOTTLE | 3-9 | 85% |
| Eye of Ender | ENDER_EYE | 1-2 | 70% |
| Obsidian | OBSIDIAN | 1-3 | 60% |

### Rares

> **Balance note:** the broken Rare/Legendary **weapons** are capped at a **5%** drop
> (Blood Sword, Crimson Bow, Shadow Dagger, Storm Bow, Vampiric Fang).
> Mythics were already rarer on their own (0.5-0.8%).

| Item | Material | Drop | Enchantments |
|------|----------|------|-----------|
| Diamond | DIAMOND (1-3) | 50% | — |
| Ender Pearl | ENDER_PEARL (1-2) | 45% | — |
| Blood Sword | DIAMOND_SWORD | **5%** | Vampire II · Venom I (Sharpness V · Looting II) |
| Crimson Bow | BOW | **5%** | Sniper I (Power V · Punch II) |
| **Shadow Dagger** | IRON_SWORD | **5%** | Double Strike I · Ice Aspect II (Sharpness IV · Looting III) |
| **Storm Bow** | BOW | **5%** | Electrified Arrows II (Power V · Flame) |
| **Umbral Pickaxe** | DIAMOND_PICKAXE | 25% | Haste II · Smelter I (Fortune III · Efficiency V) |

### Epics

| Item | Material | Drop | Enchantments |
|------|----------|------|-----------|
| Enchanted Golden Apple | ENCHANTED_GOLDEN_APPLE | 20% | — |
| Totem | TOTEM_OF_UNDYING | 15% | — |
| Netherite Scrap | NETHERITE_SCRAP (1-2) | 18% | — |
| Executioner's Axe | NETHERITE_AXE | 12% | Cutter II · Decapitator I (Sharpness V · Efficiency V) |
| Hunter's Helm | NETHERITE_HELMET | 10% | Hardened II (Protection IV · Respiration III) |
| Hunter's Chestplate | NETHERITE_CHESTPLATE | 10% | Hardened II · Dragon Heart II (Protection IV) |
| Hunter's Leggings | NETHERITE_LEGGINGS | 10% | Hardened II · Regrowth II (Protection IV) |
| Hunter's Boots | NETHERITE_BOOTS | 10% | Speed I (Protection IV · Feather Falling IV) |
| **Ruin Sword** | NETHERITE_SWORD | 14% | Vampire I · Wither II (Sharpness V · Looting III) |
| **Abyssal Crossbow** | CROSSBOW | 12% | Electrified Arrows I (Multishot · Piercing IV) |
| **Thunder Boots** | NETHERITE_BOOTS | 10% | Jumping II · Stopping Force I (Soul Speed III) |
| **Blizzard Axe** | DIAMOND_AXE | 12% | Ice Aspect III · Paralyze II (Sharpness V) |

### Legendaries

> ⚠️ **Every weapon, tool and armor piece from Rare to Mythic carries the Curse of Vanishing**
> (`vanishing_curse`): if you die with one in your inventory, the item **disappears** — it doesn't drop
> to the ground and nobody can pick it up. It applies to swords, axes, pickaxes, shovels, hoes, bows,
> crossbows, tridents, maces and armor (helmet, chestplate, leggings, boots, elytra) of Rare, Epic,
> Legendary and Mythic rarity. Shields never carry it.
> Controlled by `settings.vanishing-curse-min-rarity` in `items.yml` (`OFF` disables it).

| Item | Material | Drop | Enchantments |
|------|----------|------|-----------|
| Vampiric Fang | NETHERITE_SWORD | **5%** | Vampire III · Wither II (Sharpness V · Fire Aspect II) |
| Lunar Bow | BOW | 5% | Vampiric Arrows II · Sniper II (Power V · Infinity) |
| Nightwalker Boots | NETHERITE_BOOTS | 5% | Speed II · Night Vision I (Feather Falling IV) |
| Aegis of Thorns | SHIELD | 4% | Unbreaking III · Mending |
| **Thunder Hammer** | MACE | 5% | **Thunder II** · Double Strike I (Density V · Breach IV) |
| **Crimson Guardian Chestplate** | NETHERITE_CHESTPLATE | 4.5% | Hardened II · **Fire Shield II** · Thorns III |
| **Dawn Crossbow** | CROSSBOW | 5% | **Electrified Arrows III** (Piercing IV) |

### Mythics (max 3 per server, with a serial number)

| Item | Material | Drop | Enchantments |
|------|----------|------|-----------|
| Crimson Eclipse Blade | NETHERITE_SWORD | 0.8% | Vampire III · **Thunder II** · Wither II (Sharpness V) |
| Blood Moon Crown | NETHERITE_HELMET | 0.6% | Dragon Heart III · Night Vision I (Protection V) |
| Reaper's Scythe | NETHERITE_HOE | 0.5% | Wither II · Vampire II (Sharpness V · Fortune III) |
| Red Abyss Trident | TRIDENT | 0.7% | Infernus III · **Thunder I** (Impaling V) |
| Colossus Mace | MACE | 0.6% | **Thunder I** · Paralyze I (Density V · Breach IV) |
| **Apocalypse Axe** | NETHERITE_AXE | 0.6% | Double Strike II · Vampire II (Sharpness V · Efficiency V) |
| **Crimson Eclipse Chestplate** | NETHERITE_CHESTPLATE | 0.5% | Hardened II · **Fire Shield II** · Stopping Force I |
| **Eclipse Greaves** | NETHERITE_LEGGINGS | 0.5% | Speed II · Lightweight I (Swift Sneak III) |

---

## ⚔️ Item enchantments

Moon gear no longer carries custom abilities or above-vanilla levels. It ships:

- **Vanilla enchantments at their max**: Sharpness 5, Unbreaking 3, Looting 3… **Legendary and
  mythic** gear raises the cap to **5** even where vanilla stops lower (Protection 5, Thorns 5…).
  Any higher value in `items.yml` is trimmed when the item is built.
- **[ExcellentEnchants](https://nightexpressdev.com/excellentenchants/) enchantments**: `vampire`,
  `thunder`, `electrified_arrows`, `dragon_heart`, `hardened`, `haste`... at the level that plugin
  allows.

> ⚠️ **Never remove ExcellentEnchants from a server with gear already handed out**: Minecraft deletes
> any item whose enchantment does not exist, so those items would vanish when their owner logs in. If
> the server starts without EE (for example on a Paper version it does not support yet), **new** items
> get only the vanilla enchantments, the console warns once, and older EE items are lost on login.
> Before removing EE, reinstall it and run `/luna refresh <player|all>` on a server where EE loaded.

Each enchantment's chance (including `thunder`'s lightning) is configured in
`plugins/ExcellentEnchants/enchants/<id>.yml`, not in BloodMoon. For thunder without a storm:
`thunderstorm_only: false`.

---

## 🔧 View probabilities in-game

```
/luna chances mob_horde
/luna chances boss_verdugo
```

Shows the real rarity percentages and of each item in the table.

---

## 🔄 Updating from 1.0.2 to 1.0.3 (the `items.yml` DOES change)

1.0.3 drops the custom abilities, replaces them with ExcellentEnchants enchantments and raises the
vanilla cap to 5 on legendary and mythic gear. **Your current `items.yml` is not replaced
automatically**: the plugin only writes it when it does not exist, so an updated server keeps the
old file (Protection 4, ability lists) and the refresh applies *that* file — `/luna refresh all`
would leave Protection 4 in place.

1. Stop the server and back up `plugins/BloodMoonPlus/items.yml` (the plugin also leaves
   `items.yml.bak`).
2. Replace it with the `items.yml` inside the jar (or rename it and start once so it is generated
   again) and re-apply your customisations. If you prefer editing yours, at least set
   **`protection: 5`** (plus Thorns 5…) on the `LEGENDARIO`/`MITICO` pieces and add the EE
   enchantments.
3. Start with **ExcellentEnchants and NightCore installed** and check the console:
   `Items cargados: 45` and no unknown-enchantment warning.
4. Run `/luna refresh all` (or let players reconnect or pick their gear up): that is when a
   Protection 4 chestplate already handed out becomes Protection 5.

> Shift-clicking (or using a hotbar key) to pull gear out of a chest, shulker or the ender chest
> refreshes it as it lands in your inventory. Two cases stay out on purpose: gear **dragged** with
> the mouse is refreshed on the next click on it, and gear still **stored** in a chest or in the
> ender chest is only refreshed on join or with `/luna refresh`.

> ⚠️ Never remove ExcellentEnchants while gear is out: Minecraft deletes items whose enchantment
> does not exist. Run `/luna refresh all` on a server where EE loaded first.

`config.yml` only gains `items.migrate-on-join: true` (refresh on join; set it to `false` if you
prefer controlling it with `/luna refresh`).

---

## 🔄 Updating from 1.0.1 (or older)

**Only the `.jar` needs to be replaced.** Nothing is migrated and no data is deleted.

1. If an event is running, run `/luna stop` and **stop the server**.
2. **Delete the old jar** from `plugins/` (`BloodMoonPlus-1.0.1.jar`). That is the one thing you
   must remove: both jars declare `name: BloodMoonPlus` and Bukkit rejects the second with
   *“plugin name is already in use”* (or loads the one you did not want).
3. Copy the current `BloodMoonPlus-1.0.3-beta.jar` into `plugins/` and start the server. There is
   no need to go through 1.0.2 first.
4. Check the console: `Items cargados: 45`, `Jefes cargados: 10`.

The five new languages (`pt`, `fr`, `de`, `it`, `ru`) are copied automatically on first start.

### What you must NOT delete

| File | What you lose by deleting it |
|---|---|
| `data.yml` | The running event, the serial numbers of unique items and pity. **Limited mythics would become available again.** |
| `statistics.yml` | The event, death and boss history. |
| `config.yml` | All your settings. It did not change between 1.0.1 and 1.0.2, so there is nothing to migrate. |
| `locales/*.yml` | Your custom translations: the plugin never overwrites them. |
| `worlds/<world>/config.yml` | Per-world settings. Retired keys (`IncludeLateJoiners`, `VanillaBossBar`, `UseMythicMobsRewards`…) are ignored without errors. |

### What happens to `bosses.yml` if you don't update it

Your server's `bosses.yml` is **never overwritten**. If you keep yours:

- You keep your bosses and your tuning, but the new boss **`creeper_cargado`** (Cataclysm Maw, with
  `VORTEX` absorption, `SWARM` minions and its `EXPLODE` detonation) **will not exist**.
- `bar-color: BLACK` on `cazador_sombras` **no longer deletes the boss**: each field is validated
  separately and falls back to `RED` with a warning naming the field, the value and the valid
  options. Before, that value removed the hunter and only 8 of 9 bosses loaded.
- The file shipped in the jar changes that colour to `PURPLE`, so updating it turns the hunter's
  bar purple.

To get the new boss **without losing your tuning**: back up your `bosses.yml`, delete or rename the
original, start once so the new file is generated, then re-apply your changes (or copy just the
`creeper_cargado:` block by hand from the jar).

### `items.yml` and `messages.yml`

- **For 1.0.2, `items.yml` needed no changes.** Only comments and the
  `settings.vanishing-curse-min-rarity` key were added; the default (`RARO`) lives in the code, so
  the Curse of Vanishing already applies. Add the key only if you want `OFF` or a higher threshold.
  **In 1.0.3 it does change: see "Updating from 1.0.2 to 1.0.3".**
- **`messages.yml`** is no longer read by any code since 1.0.2: you can safely delete it.

---

## 📜 License

**All rights reserved.**
This project's code is public **for reading and security review only**. Copying,
modifying, reusing or redistributing it is not allowed. You may download and
**run** the compiled plugin on your server. See the [`LICENSE`](LICENSE) file
for details.

---

## 📤 Modrinth publishing

The ready-to-publish materials (description, changelog, icon and step-by-step
guide) are in the [`modrinth/`](modrinth/) folder. Start with
[`modrinth/PUBLISH.md`](modrinth/PUBLISH.md).
