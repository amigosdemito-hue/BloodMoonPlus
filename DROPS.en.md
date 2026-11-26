# Blood Moon Drops (v1.0.3-beta)

🌐 [Español](DROPS.md) · **English**

> Generated from `loot.yml`, `items.yml` and `config.yml`. Every percentage is computed from the
> files themselves — none is hand-written.

## What changed in this version

- **No copy limit.** The 3-copy cap and the serial number are gone: an item can be lost,
  duplicated or broken, so limiting it only punished the player. A mythic can drop as often as
  the roll decides.
- **Pity is per table.** The bad-luck counter is stored per **table and rarity**, so grinding
  horde mobs no longer feeds luck into the boss tables.
- **No repeats inside one batch.** A boss rolling 5 times cannot drop the same item twice while
  its table still has alternatives.
- **Validation on load.** Negative weights, `NaN` or `drop-chance` outside 0..1 are reported and
  fixed instead of silently skewing the roll.
- **`/luna chances` shows the final probability** of every item (rarity × weight × drop-chance)
  and your current pity in that table.

## How the roll works

```
 mob dies
      │
      ▼
 [1] DOES IT DROP?   table drop-chance × difficulty
      │ yes
      ▼
 [2] HOW MANY ROLLS? rolls (1-2 horde, 4-6 bosses)
      │
      ▼
 [3] WHICH RARITY?   rarity weight in that table (+ luck + pity)
      │
      ▼
 [4] WHICH ITEM?     item weight inside the rarity
      │
      ▼
 [5] DOES IT LAND?   the item's own drop-chance
      │
      ▼
    🎁 item with rarity, hunter and enchantments
```

## Step 1 — does the mob drop anything?

```
real chance = table drop-chance × (1 + (difficulty − 1) × loot-chance)
```

`difficulty.scaling.loot-chance` defaults to **0.25**.

| Difficulty | Multiplier | `mob_horde` (60%) | `mob_elite` (45%) |
|---|---|---|---|
| x1 | ×1.00 | 60.0% | 45.0% |
| x2 | ×1.25 | 75.0% | 56.2% |
| x3 | ×1.50 | 90.0% | 67.5% |
| x5 | ×2.00 | 100.0% | 90.0% |
| x10 | ×3.25 | 100.0% | 100.0% |

## Step 2 — how many rolls?

`rolls` accepts a range; a whole number between both values is drawn.

| Table | Rolls |
|---|---|
| `mob_horde` | 1-2 |
| `mob_elite` | 2-3 |
| `survivors` | 2-3 |
| `boss_rey_zombi` | 4-5 |
| `boss_arquera` | 4-5 |
| `boss_verdugo` | 5-6 |
| `boss_top_damage` | 1 |

## Step 3 — rarity roll

Every table declares its own weights; a rarity missing from the table falls back to the global
`rarities:` weight. These are the **real** percentages per roll (no luck, no pity):

| Table | Common | Uncommon | Rare | Epic | Legendary | Mythic |
|---|---|---|---|---|---|---|
| `mob_horde` | 59.55% | 25.81% | 10.92% | 3.18% | 0.50% | 0.05% |
| `mob_elite` | 26.48% | 33.10% | 26.48% | 11.92% | 1.85% | 0.16% |
| `survivors` | 33.14% | 33.14% | 22.09% | 9.94% | 1.55% | 0.13% |
| `boss_rey_zombi` | — | — | 48.66% | 36.50% | 13.38% | 1.46% |
| `boss_arquera` | — | — | 47.96% | 35.97% | 14.39% | 1.68% |
| `boss_verdugo` | — | — | 31.67% | 45.25% | 20.36% | 2.71% |
| `boss_top_damage` | — | — | — | 38.22% | 47.77% | 14.01% |

### Luck from difficulty

`luck-affected` rarities (Rare and above) multiply their weight by
`1 + (difficulty − 1) × 0.50`: x3 means ×2.0, x5 means ×3.0.

### Pity (bad-luck protection)

Every roll in a table that does **not** yield the rarity adds 1 point for that player **in that
table**; the weight is multiplied by `1 + points × pity-per-roll` and resets when it drops.

| Rarity | Pity per roll | Max points | Max multiplier |
|---|---|---|---|
| Common | — | — | — |
| Uncommon | — | — | — |
| Rare | — | — | — |
| Epic | +0.02 | 150 | ×4.0 |
| Legendary | +0.05 | 200 | ×11.0 |
| Mythic | +0.08 | 300 | ×25.0 |

> The cap applies per table: 200 legendary points in `boss_verdugo` are not added to the ones in
> `mob_horde`. Boss pity now measures real luck against bosses.

## Steps 4 and 5 — item and its own drop-chance

Inside the chosen rarity items compete by weight. Then each one applies its own `drop-chance`;
if it fails, **that roll gives nothing** (it is not replaced by another item).

That double filter is what makes a mythic a mythic: 0.05% rarity per roll and, on top, a
0.5-0.8% own chance on each of the 14 items.

## Loot tables

| Table | Drop | Rolls | Guaranteed |
|---|---|---|---|
| `mob_horde` | 60% | 1-2 | — |
| `mob_elite` | 45% | 2-3 | — |
| `survivors` | 100% | 2-3 | — |
| `boss_rey_zombi` | 100% | 4-5 | Bottle o' Enchanting ×8-16, Diamond ×3-6 |
| `boss_arquera` | 100% | 4-5 | Arrows ×32-64, Diamond ×2-5 |
| `boss_verdugo` | 100% | 5-6 | Netherite scrap ×3-6, Bottle o' Enchanting ×6-12 |
| `boss_top_damage` | 100% | 1 | — |

## Full catalogue (104 items)

Stackable materials: **16**. Named gear: **88** (14 classes × 6 rarities).

The **real %** column is the chance of that exact item per `mob_horde` roll at difficulty 1,
counting rarity, item weight and its own drop-chance.

> A horde mythic is in the order of **1 in millions of rolls**: a token prize. The real path is
> bosses, where the rarity roll already starts at Rare and a mythic is worth 1.5-2.7% per roll
> (and the boss rolls 4-6 times). `boss_top_damage` is the best mythic table in the game: 14%.

### Common (20)

| Item | Material | Amount | Drop | Real % | Enchantments |
|---|---|---|---|---|---|
| Beef | `COOKED_BEEF` | 3-8 | 100% | 6.14% | — |
| Arrows | `ARROW` | 6-16 | 100% | 4.60% | — |
| Gunpowder | `GUNPOWDER` | 2-6 | 100% | 4.60% | — |
| Iron | `IRON_INGOT` | 3-7 | 100% | 3.84% | — |
| Bone | `BONE` | 3-7 | 100% | 3.84% | — |
| String | `STRING` | 2-5 | 100% | 3.07% | — |
| Espada Oxidada | `WOODEN_SWORD` | 1 | 50% | 1.53% | sharpness II · unbreaking I |
| Hacha Oxidada | `WOODEN_AXE` | 1 | 50% | 1.53% | efficiency II · sharpness II · unbreaking I |
| Pico Oxidado | `WOODEN_PICKAXE` | 1 | 50% | 1.53% | efficiency II · unbreaking I |
| Pala Oxidada | `WOODEN_SHOVEL` | 1 | 50% | 1.38% | efficiency II · unbreaking I |
| Azada Oxidada | `WOODEN_HOE` | 1 | 45% | 1.11% | efficiency II · unbreaking I |
| Arco Astillado | `BOW` | 1 | 45% | 1.24% | power II · unbreaking II |
| Ballesta Astillada | `CROSSBOW` | 1 | 45% | 1.11% | unbreaking II · quick_charge I |
| Tridente Oxidado | `TRIDENT` | 1 | 35% | 0.537% | impaling II · unbreaking II · loyalty I |
| Maza Oxidada | `MACE` | 1 | 35% | 0.645% | unbreaking II · density I |
| Casco de Cuero | `LEATHER_HELMET` | 1 | 45% | 0.967% | protection I · unbreaking I |
| Peto de Cuero | `LEATHER_CHESTPLATE` | 1 | 45% | 0.967% | protection I · unbreaking I |
| Grebas de Cuero | `LEATHER_LEGGINGS` | 1 | 45% | 0.967% | protection I · unbreaking I |
| Botas de Cuero | `LEATHER_BOOTS` | 1 | 45% | 0.967% | protection I · unbreaking I |
| Escudo Astillado | `SHIELD` | 1 | 40% | 0.737% | unbreaking I |

### Uncommon (19)

| Item | Material | Amount | Drop | Real % | Enchantments |
|---|---|---|---|---|---|
| Gold | `GOLD_INGOT` | 2-5 | 95% | 2.24% | — |
| Golden apple | `GOLDEN_APPLE` | 1 | 90% | 1.42% | — |
| Bottle o' Enchanting | `EXPERIENCE_BOTTLE` | 3-9 | 90% | 2.12% | — |
| Ender eye | `ENDER_EYE` | 1-2 | 75% | 0.885% | — |
| Obsidian | `OBSIDIAN` | 1-3 | 65% | 0.767% | — |
| Espada de Piedra Lunar | `STONE_SWORD` | 1 | 45% | 0.708% | sharpness III · unbreaking II · looting I |
| Hacha de Piedra Lunar | `STONE_AXE` | 1 | 45% | 0.708% | efficiency III · sharpness III · unbreaking II |
| Pico de Piedra Lunar | `STONE_PICKAXE` | 1 | 45% | 0.708% | efficiency III · unbreaking II · fortune I |
| Pala de Piedra Lunar | `STONE_SHOVEL` | 1 | 45% | 0.637% | efficiency III · unbreaking II · fortune I |
| Azada de Piedra Lunar | `STONE_HOE` | 1 | 40% | 0.504% | efficiency III · unbreaking II · replanter I |
| Arco de Hueso Lunar | `BOW` | 1 | 40% | 0.566% | power III · unbreaking III · punch I |
| Ballesta de Hueso Lunar | `CROSSBOW` | 1 | 40% | 0.504% | unbreaking III · piercing II · quick_charge II |
| Tridente de los Mares | `TRIDENT` | 1 | 30% | 0.236% | impaling III · unbreaking III · loyalty II · +1 |
| Maza Pesada | `MACE` | 1 | 30% | 0.283% | unbreaking III · density II · breach I |
| Casco de Cadena Lunar | `CHAINMAIL_HELMET` | 1 | 40% | 0.441% | protection II · unbreaking II · respiration I |
| Peto de Cadena Lunar | `CHAINMAIL_CHESTPLATE` | 1 | 40% | 0.441% | protection II · unbreaking II |
| Grebas de Cadena Lunar | `CHAINMAIL_LEGGINGS` | 1 | 40% | 0.441% | protection II · unbreaking II |
| Botas de Cadena Lunar | `CHAINMAIL_BOOTS` | 1 | 40% | 0.441% | protection II · unbreaking II · feather_falling I |
| Escudo Reforzado | `SHIELD` | 1 | 35% | 0.330% | unbreaking II |

### Rare (18)

| Item | Material | Amount | Drop | Real % | Enchantments |
|---|---|---|---|---|---|
| Diamond | `DIAMOND` | 1-3 | 55% | 0.890% | — |
| Ender pearl | `ENDER_PEARL` | 1-2 | 50% | 0.607% | — |
| Espada Sanguinaria | `DIAMOND_SWORD` | 1 | 5% | 0.030% (1 in 3.297) | sharpness V · unbreaking III · looting II · vampire II · venom II · mending I |
| Daga de las Sombras | `IRON_SWORD` | 1 | 5% | 0.028% (1 in 3.533) | sharpness IV · ice_aspect III · looting III · unbreaking III · double_strike II · sweeping_edge II · nimble I |
| Hacha Sangrienta | `IRON_AXE` | 1 | 20% | 0.113% | efficiency IV · sharpness IV · unbreaking III · decapitator I · vampire I |
| Pico Umbrío | `DIAMOND_PICKAXE` | 1 | 30% | 0.146% | efficiency V · fortune III · unbreaking III · smelter II · mending I · veinminer I |
| Pala Sangrienta | `IRON_SHOVEL` | 1 | 20% | 0.097% (1 in 1.030) | efficiency IV · unbreaking III · smelter II |
| Azada Sangrienta | `IRON_HOE` | 1 | 20% | 0.081% (1 in 1.236) | efficiency IV · unbreaking III · smelter II · replanter I |
| Arco Carmesí | `BOW` | 1 | 5% | 0.030% (1 in 3.297) | power V · unbreaking III · punch II · sniper II · mending I |
| Arco de la Tormenta | `BOW` | 1 | 5% | 0.028% (1 in 3.533) | power V · electrified_arrows III · unbreaking III · punch II · flame I |
| Ballesta Carmesí | `CROSSBOW` | 1 | 15% | 0.085% (1 in 1.178) | piercing III · quick_charge III · unbreaking III · multishot I · sniper I |
| Tridente Sangriento | `TRIDENT` | 1 | 12% | 0.049% (1 in 2.061) | impaling IV · loyalty III · unbreaking III · infernus I · mending I |
| Maza de Hueso | `MACE` | 1 | 15% | 0.073% (1 in 1.374) | density III · unbreaking III · breach II · mending I |
| Casco de Hierro Carmesí | `IRON_HELMET` | 1 | 15% | 0.073% (1 in 1.374) | protection III · unbreaking III · elemental_protection II · respiration II |
| Peto de Hierro Carmesí | `IRON_CHESTPLATE` | 1 | 15% | 0.073% (1 in 1.374) | protection III · unbreaking III · elemental_protection II · fire_shield I |
| Grebas de Hierro Carmesí | `IRON_LEGGINGS` | 1 | 15% | 0.073% (1 in 1.374) | protection III · unbreaking III · stopping_force I |
| Botas de Hierro Carmesí | `IRON_BOOTS` | 1 | 15% | 0.073% (1 in 1.374) | protection III · unbreaking III · feather_falling II · speed I |
| Escudo Carmesí | `SHIELD` | 1 | 12% | 0.049% (1 in 2.061) | unbreaking III · restore I |

### Epic (19)

| Item | Material | Amount | Drop | Real % | Enchantments |
|---|---|---|---|---|---|
| Enchanted golden apple | `ENCHANTED_GOLDEN_APPLE` | 1 | 25% | 0.067% (1 in 1.486) | — |
| Totem of Undying | `TOTEM_OF_UNDYING` | 1 | 45% | 0.182% | — |
| Netherite scrap | `NETHERITE_SCRAP` | 1-2 | 40% | 0.162% | — |
| Espada de la Ruina | `NETHERITE_SWORD` | 1 | 16% | 0.026% (1 in 3.870) | sharpness V · cutter III · looting III · unbreaking III · decapitator II · vampire II · wither II · mending I |
| Hacha de la Ventisca | `DIAMOND_AXE` | 1 | 15% | 0.022% (1 in 4.503) | efficiency V · sharpness V · ice_aspect III · unbreaking III · double_strike II · paralyze II |
| Hacha del Verdugo | `NETHERITE_AXE` | 1 | 15% | 0.020% (1 in 4.954) | efficiency V · sharpness V · cutter III · unbreaking III · decapitator II · vampire II · mending I |
| Pico del Abismo | `DIAMOND_PICKAXE` | 1 | 12% | 0.016% (1 in 6.192) | efficiency V · fortune III · smelter III · unbreaking III · veinminer II · mending I · telekinesis I |
| Pala del Abismo | `DIAMOND_SHOVEL` | 1 | 12% | 0.015% (1 in 6.880) | efficiency V · fortune III · unbreaking III · mending I · telekinesis I · tunnel I |
| Azada del Abismo | `DIAMOND_HOE` | 1 | 10% | 0.011% (1 in 9.288) | efficiency V · unbreaking III · mending I · replanter I · telekinesis I |
| Arco del Abismo | `BOW` | 1 | 12% | 0.016% (1 in 6.192) | power V · unbreaking III · explosive_arrows II · poisoned_arrows II · punch II · sniper II · flame I · infinity I · mending I |
| Ballesta del Abismo | `CROSSBOW` | 1 | 15% | 0.020% (1 in 4.954) | piercing IV · electrified_arrows III · quick_charge III · unbreaking III · mending I · multishot I |
| Tridente Profundo | `TRIDENT` | 1 | 10% | 0.012% (1 in 8.256) | impaling V · loyalty III · unbreaking III · infernus II · channeling I · mending I |
| Maza Carmesí | `MACE` | 1 | 10% | 0.012% (1 in 8.256) | density IV · breach III · unbreaking III · wind_burst II · mending I · soulbound I |
| Yelmo del Cazador de Sangre | `NETHERITE_HELMET` | 1 | 12% | 0.016% (1 in 6.192) | protection IV · elemental_protection III · respiration III · unbreaking III · saturation II · mending I · water_breathing I |
| Peto del Cazador de Sangre | `NETHERITE_CHESTPLATE` | 1 | 12% | 0.016% (1 in 6.192) | protection IV · elemental_protection III · unbreaking III · dragon_heart II · hardened II · mending I |
| Grebas del Cazador de Sangre | `NETHERITE_LEGGINGS` | 1 | 12% | 0.016% (1 in 6.192) | protection IV · elemental_protection III · unbreaking III · stopping_force II · mending I |
| Botas del Cazador de Sangre | `NETHERITE_BOOTS` | 1 | 12% | 0.016% (1 in 6.192) | feather_falling IV · protection IV · unbreaking III · jumping I · mending I · speed I |
| Botas del Estrondo | `NETHERITE_BOOTS` | 1 | 12% | 0.016% (1 in 6.192) | protection IV · soul_speed III · unbreaking III · jumping II · flame_walker I · mending I · rebound I |
| Escudo del Guardián | `SHIELD` | 1 | 10% | 0.011% (1 in 9.288) | unbreaking III · restore II · mending I · soulbound I |

### Legendary (14)

| Item | Material | Amount | Drop | Real % | Enchantments |
|---|---|---|---|---|---|
| Colmillo Vampírico | `NETHERITE_SWORD` | 1 | 6% | 0.0023% (1 in 42.651) | sharpness V · looting III · sweeping_edge III · thunder III · unbreaking III · vampire III · fire_aspect II · rocket II · wither II · mending I |
| Hacha del Abismo | `NETHERITE_AXE` | 1 | 6% | 0.0021% (1 in 47.390) | efficiency V · sharpness V · thunder IV · unbreaking III · decapitator II · vampire II · mending I · treefeller I |
| Pico Espectral | `NETHERITE_PICKAXE` | 1 | 6% | 0.0021% (1 in 47.390) | efficiency V · smelter IV · fortune III · haste III · unbreaking III · veinminer III · tunnel II · mending I · telekinesis I |
| Pala Espectral | `NETHERITE_SHOVEL` | 1 | 5% | 0.0016% (1 in 63.976) | efficiency V · fortune III · smelter III · tunnel III · unbreaking III · mending I · telekinesis I |
| Azada Espectral | `NETHERITE_HOE` | 1 | 5% | 0.0014% (1 in 73.116) | efficiency V · smelter III · unbreaking III · haste II · mending I · replanter I · telekinesis I |
| Arco Lunar | `BOW` | 1 | 6% | 0.0023% (1 in 42.651) | power V · unbreaking III · vampiric_arrows III · dragonfire_arrows II · explosive_arrows II · punch II · sniper II · flame I · infinity I · mending I |
| Ballesta del Alba | `CROSSBOW` | 1 | 5% | 0.0018% (1 in 56.868) | piercing IV · electrified_arrows III · quick_charge III · unbreaking III · explosive_arrows II · sniper II · vampiric_arrows II · mending I · multishot I |
| Tridente de la Tormenta | `TRIDENT` | 1 | 5% | 0.0018% (1 in 56.868) | impaling V · infernus III · loyalty III · unbreaking III · channeling I · mending I · soulbound I |
| Martillo del Trueno | `MACE` | 1 | 5% | 0.0018% (1 in 56.868) | density V · breach IV · unbreaking III · wind_burst III · restore II · mending I · soulbound I |
| Yelmo del Eclipse | `NETHERITE_HELMET` | 1 | 5% | 0.0018% (1 in 56.868) | protection V · elemental_protection IV · respiration III · thorns III · unbreaking III · saturation II · mending I · night_vision I |
| Peto del Guardián Carmesí | `NETHERITE_CHESTPLATE` | 1 | 4.5% | 0.0016% (1 in 63.186) | protection V · dragon_heart III · fire_shield III · thorns III · unbreaking III · cold_steel II · hardened II · mending I |
| Grebas del Titán | `NETHERITE_LEGGINGS` | 1 | 5% | 0.0018% (1 in 56.868) | protection V · elemental_protection IV · stopping_force III · unbreaking III · thorns II · mending I |
| Botas del Noctámbulo | `NETHERITE_BOOTS` | 1 | 6% | 0.0023% (1 in 42.651) | protection V · feather_falling IV · depth_strider III · soul_speed III · unbreaking III · flame_walker II · jumping II · speed II · mending I |
| Égida Carmesí | `SHIELD` | 1 | 5% | 0.0020% (1 in 51.181) | restore III · unbreaking III · mending I · soulbound I |

### Mythic (14)

| Item | Material | Amount | Drop | Real % | Enchantments |
|---|---|---|---|---|---|
| Hoja del Eclipse Carmesí | `NETHERITE_SWORD` | 1 | 0.8% | 0.000029% (1 in 3.425.500) | sharpness V · thunder V · looting III · paralyze III · sweeping_edge III · unbreaking III · vampire III · double_strike II · fire_aspect II · wither II · mending I |
| Hacha del Apocalipsis | `NETHERITE_AXE` | 1 | 0.6% | 0.000022% (1 in 4.567.333) | efficiency V · sharpness V · thunder V · unbreaking III · vampire III · decapitator II · double_strike II · mending I · treefeller I |
| Pico del Eclipse | `NETHERITE_PICKAXE` | 1 | 0.6% | 0.000022% (1 in 4.567.333) | efficiency V · smelter V · fortune III · tunnel III · unbreaking III · veinminer III · blast_mining II · mending I · telekinesis I |
| Pala del Eclipse | `NETHERITE_SHOVEL` | 1 | 0.5% | 0.000016% (1 in 6.089.778) | efficiency V · smelter V · fortune III · haste III · tunnel III · unbreaking III · mending I · telekinesis I |
| Guadaña del Segador | `NETHERITE_HOE` | 1 | 0.5% | 0.000018% (1 in 5.480.800) | efficiency V · smelter V · haste III · unbreaking III · mending I · replanter I · telekinesis I |
| Arco del Eclipse | `BOW` | 1 | 0.7% | 0.000026% (1 in 3.914.857) | power V · dragonfire_arrows III · explosive_arrows III · unbreaking III · vampiric_arrows III · electrified_arrows II · punch II · sniper II · flame I · infinity I · mending I |
| Ballesta del Eclipse | `CROSSBOW` | 1 | 0.6% | 0.000020% (1 in 5.074.815) | piercing IV · dragonfire_arrows III · electrified_arrows III · explosive_arrows III · quick_charge III · unbreaking III · vampiric_arrows III · sniper II · mending I · multishot I |
| Tridente del Abismo Rojo | `TRIDENT` | 1 | 0.7% | 0.000026% (1 in 3.914.857) | impaling V · infernus III · loyalty III · restore III · unbreaking III · channeling I · mending I · soulbound I |
| Maza del Coloso | `MACE` | 1 | 0.6% | 0.000022% (1 in 4.567.333) | density V · breach IV · restore III · unbreaking III · wind_burst III · mending I · soulbound I |
| Corona de la Luna de Sangre | `NETHERITE_HELMET` | 1 | 0.6% | 0.000022% (1 in 4.567.333) | protection V · elemental_protection IV · respiration III · thorns III · unbreaking III · saturation II · mending I · night_vision I · water_breathing I |
| Peto del Eclipse Carmesí | `NETHERITE_CHESTPLATE` | 1 | 0.5% | 0.000018% (1 in 5.480.800) | dragon_heart V · protection V · elemental_protection IV · fire_shield IV · cold_steel III · thorns III · unbreaking III · hardened II · mending I |
| Grebas del Eclipse | `NETHERITE_LEGGINGS` | 1 | 0.5% | 0.000018% (1 in 5.480.800) | protection V · elemental_protection IV · stopping_force III · swift_sneak III · thorns III · unbreaking III · mending I |
| Botas del Eclipse | `NETHERITE_BOOTS` | 1 | 0.6% | 0.000022% (1 in 4.567.333) | protection V · elemental_protection IV · feather_falling IV · depth_strider III · soul_speed III · unbreaking III · flame_walker II · jumping II · speed II · mending I · rebound I |
| Égida del Eclipse | `SHIELD` | 1 | 0.5% | 0.000015% (1 in 6.851.000) | restore III · unbreaking III · mending I · soulbound I |

## Configuration

```yaml
loot:
  default-table: mob_horde
  survivor-table: survivors
  direct-to-inventory: false
  require-player-kill: true
  keep-vanilla-drops: false
  glow-rarities: [LEGENDARIO, MITICO]
```

## Check the real probabilities in game

```
/luna chances mob_horde
/luna chances boss_verdugo
```

Example output (trimmed):

```
Table mob_horde · base drop chance: 60% · rolls: 1-2
Common 59.55%
   - Beef 6.14%
   - Arrows 4.60%
Uncommon 25.81%
   - Gold 2.24%
   - Golden apple 1.42%
Rare 10.92%
   - Diamond 0.890%
   - Ender pearl 0.607%
```

## Balance notes

- **No item is limited**: there is no copy counter and no serial number.
- **Rare and above go straight to the inventory**; commons drop on the ground.
- **Legendary and Mythic glow** on the ground and ignore lava and explosions (`loot.glow-rarities`).
- **No curses**: on death everything drops and anyone can pick it up.
- Enchantment levels never exceed their real max (5 on legendaries and mythics).
