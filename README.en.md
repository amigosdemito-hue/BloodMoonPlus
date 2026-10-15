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
| Abyssal Warden | 540 s | 80% | 2.0 |
| **Crimson Scourge** | 660 s | 50% | 2.5 |

## Overview of the 9 bosses

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

### ☠️ Crimson Scourge (`azote_carmesi`) — FINAL BOSS
- **Mob:** WITHER, knockback resistance 0.95 (won't budge). **900 health** (up to 1024 after the difficulty multiplier), damage 4.0, speed 1.25.
- **Bar:** purple, segmented ×12.
- **Abilities:** WITHER SKULLS every 7 s (**breaks blocks**, like the vanilla Wither) · PULL (pulls within 20 m) every 16 s · AURA of wither for 8 s (16 m) · summons 4 Nether skeletons every 20 s · darkness roar for 6 s · **SELF_HEAL** (heals 80 health every 24 s).
- **Phase 2 (enraged) at 50%** health: abilities at **55%** cooldown, damage **×1.75** — the server's damage peak.
- **Spawn:** 660 s, 50%, minimum difficulty 2.5. **EXP:** 1500.
- **Loot:** table `boss_verdugo`.
- **Note:** it is the **only Wither allowed**. The plugin cancels the spawn of any other Wither (through a spell, egg or spawner); the optional datapack `BloodMoonWitherControl` finishes it off every tick as a second safety layer. Its skulls break blocks: protect your base or fight in the open.

## Second phase (enraged)

4 bosses have a configurable phase 2 in `bosses.yml`:

| Boss | Triggers at... | Cooldown | Damage |
|------|----------------|----------|--------|
| `verdugo_noche` | 40% health | ×0.65 | ×1.5 |
| `devastador_sangre` | 45% health | ×0.6 | ×1.6 |
| `warden_abismal` | 50% health | ×0.6 | ×1.6 |
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
- **Legendary weapons have the Curse of Vanishing**: if you die with them, they disappear (a real risk to carrying them).
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

| Item | Material | Drop | Abilities |
|------|----------|------|-----------|
| Diamond | DIAMOND (1-3) | 50% | — |
| Ender Pearl | ENDER_PEARL (1-2) | 45% | — |
| Blood Sword | DIAMOND_SWORD | **5%** | Lifesteal 5%, Poison (20%: 4 s) |
| Crimson Bow | BOW | **5%** | Damage +6% |
| **Shadow Dagger** | IRON_SWORD | **5%** | Crit (25%: ×1.8), Slowness, Poison · Passive: Speed I |
| **Storm Bow** | BOW | **5%** | Lightning without a storm (15%: 6), Damage +8% |
| **Umbral Pickaxe** | DIAMOND_PICKAXE | 25% | Fortune III, Efficiency V · Passive: Haste I |

### Epics

| Item | Material | Drop | Abilities |
|------|----------|------|-----------|
| Enchanted Golden Apple | ENCHANTED_GOLDEN_APPLE | 20% | — |
| Totem | TOTEM_OF_UNDYING | 15% | — |
| Netherite Scrap | NETHERITE_SCRAP (1-2) | 18% | — |
| Executioner's Axe | NETHERITE_AXE | 12% | Damage +10%, Execute (≤30% health: +50%) |
| Hunter's Helm | NETHERITE_HELMET | 10% | Reduction 3%, Reflect 15% |
| Hunter's Chestplate | NETHERITE_CHESTPLATE | 10% | Reduction 4% · Passive: Resistance I |
| Hunter's Leggings | NETHERITE_LEGGINGS | 10% | Reduction 4% |
| Hunter's Boots | NETHERITE_BOOTS | 10% | Reduction 3% |
| **Ruin Sword** | NETHERITE_SWORD | 14% | Crit (20%: ×1.9), Bleed, Lifesteal 6% · Passive: Strength I |
| **Abyssal Crossbow** | CROSSBOW | 12% | Chain Lightning, Lightning (10%: 6), Piercing IV |
| **Thunder Boots** | NETHERITE_BOOTS | 10% | Dodge 8%, Levitate (25%), Blast Wave (20%) · Passive: Jump Boost I |
| **Blizzard Axe** | DIAMOND_AXE | 12% | Crit (22%: ×1.9), Slowness, Wither |

### Legendaries

> ⚠️ **All legendary weapons carry the Curse of Vanishing** (`vanishing_curse`):
> if you die with one in your inventory, the weapon **disappears** — it doesn't drop to the ground and nobody can pick it up.
> It only applies to weapons (Fang, Lunar Bow, Hammer, Crossbow), not to legendary armor.

| Item | Material | Drop | Abilities |
|------|----------|------|-----------|
| Vampiric Fang | NETHERITE_SWORD | **5%** | Lifesteal 12%, Wither (20%: 5 s) · Passive: Strength I |
| Lunar Bow | BOW | 5% | Damage +15%, Ignite (30%: 6 s), Slowness (25%: 4 s) |
| Nightwalker Boots | NETHERITE_BOOTS | 5% | Dodge 10%, Reduction 5% · Passives: Speed I, Jump Boost I |
| Aegis of Thorns | SHIELD | 4% | Passive: Resistance I |
| **Thunder Hammer** | MACE | 5% | Crit (25%: ×2.2), Lightning (25%: 9), Boss Slayer +25% · Passive: Strength I |
| **Crimson Guardian Chestplate** | NETHERITE_CHESTPLATE | 4.5% | **Vengeful lightning (20%: a bolt strikes the attacker, no storm)**, Reduction 5% · Passives: Resistance I, Healing Aura (radius 6) |
| **Dawn Crossbow** | CROSSBOW | 5% | Chain Lightning III, Damage +15%, Piercing IV |

### Mythics (max 3 per server, with a serial number)

| Item | Material | Drop | Abilities |
|------|----------|------|-----------|
| Crimson Eclipse Blade | NETHERITE_SWORD | 0.8% | Lifesteal 20%, Lightning (15%: 8), Boss Slayer +35%, Execute · Passive: Strength I |
| Blood Moon Crown | NETHERITE_HELMET | 0.6% | Reduction 8%, Thorns, Reflect 20% · Passives: Night Vision, Health Boost II, Regeneration I |
| Reaper's Scythe | NETHERITE_HOE | 0.5% | Execute, Wither, Lifesteal 10%, Crit (25%: ×2.2) |
| Red Abyss Trident | TRIDENT | 0.7% | Lightning (30%: 10), Ignite (50%: 8 s), Damage +20% |
| Colossus Mace | MACE | 0.6% | Damage +25%, Boss Slayer +40%, Slowness |
| **Apocalypse Axe** | NETHERITE_AXE | 0.6% | Brutal crit (30%: ×2.5), Lifesteal 15%, Boss Slayer +40%, Bleed · Passive: Strength II |
| **Crimson Eclipse Chestplate** | NETHERITE_CHESTPLATE | 0.5% | **Vengeful lightning (25%)**, Blast Wave (no block breaking), Reduction 6% · Passives: Resistance I, Regeneration I, Healing Aura (radius 8) |
| **Eclipse Greaves** | NETHERITE_LEGGINGS | 0.5% | Dodge 10%, Reduction 4% · Passives: Speed I, Slow Falling |

---

## ⚔️ Item abilities (19)

### Weapons (12)
| Ability | Parameters | Effect |
|---------|-----------|--------|
| `DAMAGE_BONUS` | `f` | Multiplies damage |
| `LIFESTEAL` | `f` | Steals % of health on hit |
| `LIGHTNING` | `prob:damage` | Summons lightning (no storm) |
| `IGNITE` | `prob:sec` | Sets the enemy on fire |
| `WITHER` | `prob:sec` | Applies wither |
| `SLOW` | `prob:sec` | Applies slowness |
| `EXECUTE` | `healthFrac:bonusFrac` | Bonus against low-health enemies |
| `BOSS_SLAYER` | `f` | Bonus against Blood Moon mobs |
| `CHAIN_LIGHTNING` | `prob:chains:damage:decay` | Lightning that jumps to N enemies |
| `POISON` | `prob:sec:level` | Applies poison |
| `BLEED` | `prob:sec:level` | Damage over time with particles |
| `CRIT` | `prob:multiplier` | Critical hit with particles |

### Armor (7)
| Ability | Parameters | Effect |
|---------|-----------|--------|
| `DAMAGE_REDUCTION` | `f` | Reduces incoming damage |
| `DODGE` | `prob` | Dodges the hit |
| `REFLECT` | `f` | Returns % of damage to the attacker |
| `THORNS` | `prob:damage` | Damages the attacker |
| `THUNDER_GUARD` | `prob:damage:attacker\|self` | If you're hit, a **lightning bolt without a storm** falls on the attacker (or on you with `self`) |
| `BLAST_WAVE` | `prob:damage:force` | Shockwave that damages and launches the attacker; **does not break blocks** |
| `LEVITATE` | `prob:sec:level` | Leaves the attacker floating |

### Passives (attributes = potion effects)
| Passive | Parameters | Effect |
|---------|-----------|--------|
| `strength`, `speed`, `resistance`, `health_boost`, `regeneration`, `haste`, `night_vision`, `slow_falling`, `jump_boost`... | `effect:level` | Continuous attribute while worn |
| `HEAL_AURA` | `radius:heal` | Heals nearby allies |

---

## 🔧 View probabilities in-game

```
/luna chances mob_horde
/luna chances boss_verdugo
```

Shows the real rarity percentages and of each item in the table.

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
