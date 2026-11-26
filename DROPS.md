# Drops de la Luna de Sangre (v1.0.3-beta)

🌐 **Español** · [English](DROPS.en.md)

> Generado a partir de `loot.yml`, `items.yml` y `config.yml`. Todos los porcentajes son los
> reales del archivo: no hay números escritos a mano.

## Lo que cambia en esta versión

- **Sin límite de ejemplares.** Se quitó el tope de 3 copias y el número de serie: si un objeto
  se puede perder, duplicar o romper, limitarlo solo castigaba al que jugaba. Cada mítico puede
  caer tantas veces como el sorteo decida.
- **Pity por tabla.** El acumulador de mala suerte ya no es global por rareza: se guarda por
  **tabla y rareza**, así que matar mobs de la horda no regala suerte en las tablas de jefe.
- **Sin repeticiones en una misma tirada.** Un jefe que tira 5 veces no puede soltar dos veces
  el mismo objeto si su tabla tiene alternativas.
- **Validación al cargar.** Pesos negativos, `NaN` o `drop-chance` fuera de 0..1 se avisan en
  consola y se corrigen, en vez de desviar el sorteo en silencio.
- **`/luna chances` con la probabilidad final** de cada objeto (rareza × peso × drop-chance) y tu
  pity actual en esa tabla.

## Cómo funciona el sorteo

```
 muerte del mob
      │
      ▼
 [1] ¿SUELTA ALGO?  ── drop-chance de la tabla × dificultad
      │ sí
      ▼
 [2] ¿CUÁNTAS TIRADAS?  ── rolls (1-2 en la horda, 4-6 en jefes)
      │
      ▼
 [3] ¿QUÉ RAREZA?  ── peso de la rareza en esa tabla (+ suerte + pity)
      │
      ▼
 [4] ¿QUÉ OBJETO?  ── peso del objeto dentro de la rareza
      │
      ▼
 [5] ¿SALE?  ── drop-chance propio del objeto
      │
      ▼
    🎁 objeto con rareza, cazador y encantamientos
```

## Paso 1 — ¿Suelta algo el mob?

```
probabilidad real = drop-chance de la tabla × (1 + (dificultad − 1) × loot-chance)
```

`difficulty.scaling.loot-chance` vale **0.25** por defecto.

| Dificultad | Multiplicador | `mob_horde` (60%) | `mob_elite` (45%) |
|---|---|---|---|
| x1 | ×1.00 | 60.0% | 45.0% |
| x2 | ×1.25 | 75.0% | 56.2% |
| x3 | ×1.50 | 90.0% | 67.5% |
| x5 | ×2.00 | 100.0% | 90.0% |
| x10 | ×3.25 | 100.0% | 100.0% |

## Paso 2 — ¿Cuántas tiradas?

`rolls` admite rango y se sortea un número entero entre los dos valores.

| Tabla | Tiradas |
|---|---|
| `mob_horde` | 1-2 |
| `mob_elite` | 2-3 |
| `survivors` | 2-3 |
| `boss_rey_zombi` | 4-5 |
| `boss_arquera` | 4-5 |
| `boss_verdugo` | 5-6 |
| `boss_top_damage` | 1 |

## Paso 3 — Sorteo de la rareza

Cada tabla declara sus propios pesos; si una rareza no aparece, se usa el peso global de
`rarities:`. Estos son los porcentajes **reales** por tirada (sin suerte ni pity):

| Tabla | Común | Poco común | Raro | Épico | Legendario | Mítico |
|---|---|---|---|---|---|---|
| `mob_horde` | 59.55% | 25.81% | 10.92% | 3.18% | 0.50% | 0.05% |
| `mob_elite` | 26.48% | 33.10% | 26.48% | 11.92% | 1.85% | 0.16% |
| `survivors` | 33.14% | 33.14% | 22.09% | 9.94% | 1.55% | 0.13% |
| `boss_rey_zombi` | — | — | 48.66% | 36.50% | 13.38% | 1.46% |
| `boss_arquera` | — | — | 47.96% | 35.97% | 14.39% | 1.68% |
| `boss_verdugo` | — | — | 31.67% | 45.25% | 20.36% | 2.71% |
| `boss_top_damage` | — | — | — | 38.22% | 47.77% | 14.01% |

### Suerte por dificultad

Las rarezas con `luck-affected: true` (Raro, Épico, Legendario y Mítico) multiplican su peso por
`1 + (dificultad − 1) × 0.50`. En un evento x3 pesan ×2.0; en x5, ×3.0.

### Pity (protección contra la mala suerte)

Cada tirada de una tabla que **no** da la rareza suma 1 punto para ese jugador **en esa tabla**;
el peso se multiplica por `1 + puntos × pity-per-roll` y al obtenerla se reinicia.

| Rareza | Pity por tirada | Máx. puntos | Multiplicador máximo |
|---|---|---|---|
| Común | — | — | — |
| Poco común | — | — | — |
| Raro | — | — | — |
| Épico | +0.02 | 150 | ×4.0 |
| Legendario | +0.05 | 200 | ×11.0 |
| Mítico | +0.08 | 300 | ×25.0 |

> El tope se aplica por tabla: 200 puntos de legendario en `boss_verdugo` no se suman a los de
> `mob_horde`. Así el pity de los jefes mide la suerte real contra los jefes.

## Paso 4 y 5 — Objeto y drop-chance propio

Dentro de la rareza elegida, los objetos compiten por peso. Después cada uno aplica su
`drop-chance`; si falla, **esa tirada no da nada** (no se sustituye por otro objeto).

Ese doble filtro es lo que hace que un mítico sea un mítico: 0.05% de rareza por tirada y, encima,
un 0.5-0.8% propio de cada uno de los 14 objetos.

## Tablas de loot

| Tabla | Drop | Tiradas | Garantizado |
|---|---|---|---|
| `mob_horde` | 60% | 1-2 | — |
| `mob_elite` | 45% | 2-3 | — |
| `survivors` | 100% | 2-3 | — |
| `boss_rey_zombi` | 100% | 4-5 | Botella de XP ×8-16, Diamante ×3-6 |
| `boss_arquera` | 100% | 4-5 | Flechas ×32-64, Diamante ×2-5 |
| `boss_verdugo` | 100% | 5-6 | Fragmento de netherita ×3-6, Botella de XP ×6-12 |
| `boss_top_damage` | 100% | 1 | — |

## Catálogo completo (104 objetos)

Materiales apilables: **16**. Equipo con nombre y lore: **88** (14 clases × 6 rarezas).

Los porcentajes de la columna **% real** son la probabilidad de ese objeto concreto por tirada de
`mob_horde` a dificultad 1, contando rareza, peso del objeto y su propio drop-chance.

> Un mítico en la horda está en el orden de **1 entre millones de tiradas**: es un premio
> testimonial. La vía real son los jefes, donde el sorteo de rareza ya parte de Raro y el
> mítico vale 1.5-2.7% por tirada (y el jefe tira 4-6 veces). `boss_top_damage` es la mejor
> tabla del juego para un mítico: 14% por tirada.

### Común (20)

| Objeto | Material | Cantidad | Drop | % real | Encantamientos |
|---|---|---|---|---|---|
| Carne | `COOKED_BEEF` | 3-8 | 100% | 6.14% | — |
| Flechas | `ARROW` | 6-16 | 100% | 4.60% | — |
| Pólvora | `GUNPOWDER` | 2-6 | 100% | 4.60% | — |
| Hierro | `IRON_INGOT` | 3-7 | 100% | 3.84% | — |
| Hueso | `BONE` | 3-7 | 100% | 3.84% | — |
| Cuerda | `STRING` | 2-5 | 100% | 3.07% | — |
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

### Poco común (19)

| Objeto | Material | Cantidad | Drop | % real | Encantamientos |
|---|---|---|---|---|---|
| Oro | `GOLD_INGOT` | 2-5 | 95% | 2.24% | — |
| Manzana dorada | `GOLDEN_APPLE` | 1 | 90% | 1.42% | — |
| Botella de XP | `EXPERIENCE_BOTTLE` | 3-9 | 90% | 2.12% | — |
| Ojo de ender | `ENDER_EYE` | 1-2 | 75% | 0.885% | — |
| Obsidiana | `OBSIDIAN` | 1-3 | 65% | 0.767% | — |
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

### Raro (18)

| Objeto | Material | Cantidad | Drop | % real | Encantamientos |
|---|---|---|---|---|---|
| Diamante | `DIAMOND` | 1-3 | 55% | 0.890% | — |
| Perla de ender | `ENDER_PEARL` | 1-2 | 50% | 0.607% | — |
| Espada Sanguinaria | `DIAMOND_SWORD` | 1 | 5% | 0.030% (1 entre 3.297) | sharpness V · unbreaking III · looting II · vampire II · venom II · mending I |
| Daga de las Sombras | `IRON_SWORD` | 1 | 5% | 0.028% (1 entre 3.533) | sharpness IV · ice_aspect III · looting III · unbreaking III · double_strike II · sweeping_edge II · nimble I |
| Hacha Sangrienta | `IRON_AXE` | 1 | 20% | 0.113% | efficiency IV · sharpness IV · unbreaking III · decapitator I · vampire I |
| Pico Umbrío | `DIAMOND_PICKAXE` | 1 | 30% | 0.146% | efficiency V · fortune III · unbreaking III · smelter II · mending I · veinminer I |
| Pala Sangrienta | `IRON_SHOVEL` | 1 | 20% | 0.097% (1 entre 1.030) | efficiency IV · unbreaking III · smelter II |
| Azada Sangrienta | `IRON_HOE` | 1 | 20% | 0.081% (1 entre 1.236) | efficiency IV · unbreaking III · smelter II · replanter I |
| Arco Carmesí | `BOW` | 1 | 5% | 0.030% (1 entre 3.297) | power V · unbreaking III · punch II · sniper II · mending I |
| Arco de la Tormenta | `BOW` | 1 | 5% | 0.028% (1 entre 3.533) | power V · electrified_arrows III · unbreaking III · punch II · flame I |
| Ballesta Carmesí | `CROSSBOW` | 1 | 15% | 0.085% (1 entre 1.178) | piercing III · quick_charge III · unbreaking III · multishot I · sniper I |
| Tridente Sangriento | `TRIDENT` | 1 | 12% | 0.049% (1 entre 2.061) | impaling IV · loyalty III · unbreaking III · infernus I · mending I |
| Maza de Hueso | `MACE` | 1 | 15% | 0.073% (1 entre 1.374) | density III · unbreaking III · breach II · mending I |
| Casco de Hierro Carmesí | `IRON_HELMET` | 1 | 15% | 0.073% (1 entre 1.374) | protection III · unbreaking III · elemental_protection II · respiration II |
| Peto de Hierro Carmesí | `IRON_CHESTPLATE` | 1 | 15% | 0.073% (1 entre 1.374) | protection III · unbreaking III · elemental_protection II · fire_shield I |
| Grebas de Hierro Carmesí | `IRON_LEGGINGS` | 1 | 15% | 0.073% (1 entre 1.374) | protection III · unbreaking III · stopping_force I |
| Botas de Hierro Carmesí | `IRON_BOOTS` | 1 | 15% | 0.073% (1 entre 1.374) | protection III · unbreaking III · feather_falling II · speed I |
| Escudo Carmesí | `SHIELD` | 1 | 12% | 0.049% (1 entre 2.061) | unbreaking III · restore I |

### Épico (19)

| Objeto | Material | Cantidad | Drop | % real | Encantamientos |
|---|---|---|---|---|---|
| Manzana encantada | `ENCHANTED_GOLDEN_APPLE` | 1 | 25% | 0.067% (1 entre 1.486) | — |
| Tótem | `TOTEM_OF_UNDYING` | 1 | 45% | 0.182% | — |
| Fragmento de netherita | `NETHERITE_SCRAP` | 1-2 | 40% | 0.162% | — |
| Espada de la Ruina | `NETHERITE_SWORD` | 1 | 16% | 0.026% (1 entre 3.870) | sharpness V · cutter III · looting III · unbreaking III · decapitator II · vampire II · wither II · mending I |
| Hacha de la Ventisca | `DIAMOND_AXE` | 1 | 15% | 0.022% (1 entre 4.503) | efficiency V · sharpness V · ice_aspect III · unbreaking III · double_strike II · paralyze II |
| Hacha del Verdugo | `NETHERITE_AXE` | 1 | 15% | 0.020% (1 entre 4.954) | efficiency V · sharpness V · cutter III · unbreaking III · decapitator II · vampire II · mending I |
| Pico del Abismo | `DIAMOND_PICKAXE` | 1 | 12% | 0.016% (1 entre 6.192) | efficiency V · fortune III · smelter III · unbreaking III · veinminer II · mending I · telekinesis I |
| Pala del Abismo | `DIAMOND_SHOVEL` | 1 | 12% | 0.015% (1 entre 6.880) | efficiency V · fortune III · unbreaking III · mending I · telekinesis I · tunnel I |
| Azada del Abismo | `DIAMOND_HOE` | 1 | 10% | 0.011% (1 entre 9.288) | efficiency V · unbreaking III · mending I · replanter I · telekinesis I |
| Arco del Abismo | `BOW` | 1 | 12% | 0.016% (1 entre 6.192) | power V · unbreaking III · explosive_arrows II · poisoned_arrows II · punch II · sniper II · flame I · infinity I · mending I |
| Ballesta del Abismo | `CROSSBOW` | 1 | 15% | 0.020% (1 entre 4.954) | piercing IV · electrified_arrows III · quick_charge III · unbreaking III · mending I · multishot I |
| Tridente Profundo | `TRIDENT` | 1 | 10% | 0.012% (1 entre 8.256) | impaling V · loyalty III · unbreaking III · infernus II · channeling I · mending I |
| Maza Carmesí | `MACE` | 1 | 10% | 0.012% (1 entre 8.256) | density IV · breach III · unbreaking III · wind_burst II · mending I · soulbound I |
| Yelmo del Cazador de Sangre | `NETHERITE_HELMET` | 1 | 12% | 0.016% (1 entre 6.192) | protection IV · elemental_protection III · respiration III · unbreaking III · saturation II · mending I · water_breathing I |
| Peto del Cazador de Sangre | `NETHERITE_CHESTPLATE` | 1 | 12% | 0.016% (1 entre 6.192) | protection IV · elemental_protection III · unbreaking III · dragon_heart II · hardened II · mending I |
| Grebas del Cazador de Sangre | `NETHERITE_LEGGINGS` | 1 | 12% | 0.016% (1 entre 6.192) | protection IV · elemental_protection III · unbreaking III · stopping_force II · mending I |
| Botas del Cazador de Sangre | `NETHERITE_BOOTS` | 1 | 12% | 0.016% (1 entre 6.192) | feather_falling IV · protection IV · unbreaking III · jumping I · mending I · speed I |
| Botas del Estrondo | `NETHERITE_BOOTS` | 1 | 12% | 0.016% (1 entre 6.192) | protection IV · soul_speed III · unbreaking III · jumping II · flame_walker I · mending I · rebound I |
| Escudo del Guardián | `SHIELD` | 1 | 10% | 0.011% (1 entre 9.288) | unbreaking III · restore II · mending I · soulbound I |

### Legendario (14)

| Objeto | Material | Cantidad | Drop | % real | Encantamientos |
|---|---|---|---|---|---|
| Colmillo Vampírico | `NETHERITE_SWORD` | 1 | 6% | 0.0023% (1 entre 42.651) | sharpness V · looting III · sweeping_edge III · thunder III · unbreaking III · vampire III · fire_aspect II · rocket II · wither II · mending I |
| Hacha del Abismo | `NETHERITE_AXE` | 1 | 6% | 0.0021% (1 entre 47.390) | efficiency V · sharpness V · thunder IV · unbreaking III · decapitator II · vampire II · mending I · treefeller I |
| Pico Espectral | `NETHERITE_PICKAXE` | 1 | 6% | 0.0021% (1 entre 47.390) | efficiency V · smelter IV · fortune III · haste III · unbreaking III · veinminer III · tunnel II · mending I · telekinesis I |
| Pala Espectral | `NETHERITE_SHOVEL` | 1 | 5% | 0.0016% (1 entre 63.976) | efficiency V · fortune III · smelter III · tunnel III · unbreaking III · mending I · telekinesis I |
| Azada Espectral | `NETHERITE_HOE` | 1 | 5% | 0.0014% (1 entre 73.116) | efficiency V · smelter III · unbreaking III · haste II · mending I · replanter I · telekinesis I |
| Arco Lunar | `BOW` | 1 | 6% | 0.0023% (1 entre 42.651) | power V · unbreaking III · vampiric_arrows III · dragonfire_arrows II · explosive_arrows II · punch II · sniper II · flame I · infinity I · mending I |
| Ballesta del Alba | `CROSSBOW` | 1 | 5% | 0.0018% (1 entre 56.868) | piercing IV · electrified_arrows III · quick_charge III · unbreaking III · explosive_arrows II · sniper II · vampiric_arrows II · mending I · multishot I |
| Tridente de la Tormenta | `TRIDENT` | 1 | 5% | 0.0018% (1 entre 56.868) | impaling V · infernus III · loyalty III · unbreaking III · channeling I · mending I · soulbound I |
| Martillo del Trueno | `MACE` | 1 | 5% | 0.0018% (1 entre 56.868) | density V · breach IV · unbreaking III · wind_burst III · restore II · mending I · soulbound I |
| Yelmo del Eclipse | `NETHERITE_HELMET` | 1 | 5% | 0.0018% (1 entre 56.868) | protection V · elemental_protection IV · respiration III · thorns III · unbreaking III · saturation II · mending I · night_vision I |
| Peto del Guardián Carmesí | `NETHERITE_CHESTPLATE` | 1 | 4.5% | 0.0016% (1 entre 63.186) | protection V · dragon_heart III · fire_shield III · thorns III · unbreaking III · cold_steel II · hardened II · mending I |
| Grebas del Titán | `NETHERITE_LEGGINGS` | 1 | 5% | 0.0018% (1 entre 56.868) | protection V · elemental_protection IV · stopping_force III · unbreaking III · thorns II · mending I |
| Botas del Noctámbulo | `NETHERITE_BOOTS` | 1 | 6% | 0.0023% (1 entre 42.651) | protection V · feather_falling IV · depth_strider III · soul_speed III · unbreaking III · flame_walker II · jumping II · speed II · mending I |
| Égida Carmesí | `SHIELD` | 1 | 5% | 0.0020% (1 entre 51.181) | restore III · unbreaking III · mending I · soulbound I |

### Mítico (14)

| Objeto | Material | Cantidad | Drop | % real | Encantamientos |
|---|---|---|---|---|---|
| Hoja del Eclipse Carmesí | `NETHERITE_SWORD` | 1 | 0.8% | 0.000029% (1 entre 3.425.500) | sharpness V · thunder V · looting III · paralyze III · sweeping_edge III · unbreaking III · vampire III · double_strike II · fire_aspect II · wither II · mending I |
| Hacha del Apocalipsis | `NETHERITE_AXE` | 1 | 0.6% | 0.000022% (1 entre 4.567.333) | efficiency V · sharpness V · thunder V · unbreaking III · vampire III · decapitator II · double_strike II · mending I · treefeller I |
| Pico del Eclipse | `NETHERITE_PICKAXE` | 1 | 0.6% | 0.000022% (1 entre 4.567.333) | efficiency V · smelter V · fortune III · tunnel III · unbreaking III · veinminer III · blast_mining II · mending I · telekinesis I |
| Pala del Eclipse | `NETHERITE_SHOVEL` | 1 | 0.5% | 0.000016% (1 entre 6.089.778) | efficiency V · smelter V · fortune III · haste III · tunnel III · unbreaking III · mending I · telekinesis I |
| Guadaña del Segador | `NETHERITE_HOE` | 1 | 0.5% | 0.000018% (1 entre 5.480.800) | efficiency V · smelter V · haste III · unbreaking III · mending I · replanter I · telekinesis I |
| Arco del Eclipse | `BOW` | 1 | 0.7% | 0.000026% (1 entre 3.914.857) | power V · dragonfire_arrows III · explosive_arrows III · unbreaking III · vampiric_arrows III · electrified_arrows II · punch II · sniper II · flame I · infinity I · mending I |
| Ballesta del Eclipse | `CROSSBOW` | 1 | 0.6% | 0.000020% (1 entre 5.074.815) | piercing IV · dragonfire_arrows III · electrified_arrows III · explosive_arrows III · quick_charge III · unbreaking III · vampiric_arrows III · sniper II · mending I · multishot I |
| Tridente del Abismo Rojo | `TRIDENT` | 1 | 0.7% | 0.000026% (1 entre 3.914.857) | impaling V · infernus III · loyalty III · restore III · unbreaking III · channeling I · mending I · soulbound I |
| Maza del Coloso | `MACE` | 1 | 0.6% | 0.000022% (1 entre 4.567.333) | density V · breach IV · restore III · unbreaking III · wind_burst III · mending I · soulbound I |
| Corona de la Luna de Sangre | `NETHERITE_HELMET` | 1 | 0.6% | 0.000022% (1 entre 4.567.333) | protection V · elemental_protection IV · respiration III · thorns III · unbreaking III · saturation II · mending I · night_vision I · water_breathing I |
| Peto del Eclipse Carmesí | `NETHERITE_CHESTPLATE` | 1 | 0.5% | 0.000018% (1 entre 5.480.800) | dragon_heart V · protection V · elemental_protection IV · fire_shield IV · cold_steel III · thorns III · unbreaking III · hardened II · mending I |
| Grebas del Eclipse | `NETHERITE_LEGGINGS` | 1 | 0.5% | 0.000018% (1 entre 5.480.800) | protection V · elemental_protection IV · stopping_force III · swift_sneak III · thorns III · unbreaking III · mending I |
| Botas del Eclipse | `NETHERITE_BOOTS` | 1 | 0.6% | 0.000022% (1 entre 4.567.333) | protection V · elemental_protection IV · feather_falling IV · depth_strider III · soul_speed III · unbreaking III · flame_walker II · jumping II · speed II · mending I · rebound I |
| Égida del Eclipse | `SHIELD` | 1 | 0.5% | 0.000015% (1 entre 6.851.000) | restore III · unbreaking III · mending I · soulbound I |

## Configuración

```yaml
loot:
  default-table: mob_horde
  survivor-table: survivors
  direct-to-inventory: false
  require-player-kill: true
  keep-vanilla-drops: false
  glow-rarities: [LEGENDARIO, MITICO]
```

## Ver las probabilidades reales

```
/luna chances mob_horde
/luna chances boss_verdugo
```

Salida de ejemplo (recortada):

```
Tabla mob_horde · prob. base de drop: 60% · tiradas: 1-2
Común 59.55%
   - Carne 6.14%
   - Flechas 4.60%
Poco común 25.81%
   - Oro 2.24%
   - Manzana dorada 1.42%
Raro 10.92%
   - Diamante 0.890%
   - Perla de ender 0.607%
```

## Notas de balance

- **Ningún objeto está limitado**: no hay contador de ejemplares ni número de serie.
- Los objetos **Raro y superiores van directo al inventario**; los comunes caen al suelo.
- Los **Legendario y Mítico brillan** al caer e ignoran lava y explosiones (`loot.glow-rarities`).
- **Sin maldiciones**: al morir, todo cae al suelo y cualquiera puede recogerlo.
- Los niveles de encantamiento nunca superan su máximo real (5 en legendarios y míticos).
