# Drops de la Luna de Sangre (v1.0.1)

🌐 **Español** · [English](DROPS.en.md)

> Documento generado a partir de `loot.yml` + `items.yml` + `config.yml` actuales.

## Cómo funciona el sistema

1. Al morir un mob de la horda, se tira la **drop-chance de la tabla**: en `mob_horde` es **60%** (sube con la dificultad).
2. Si suelta algo, la tabla tira **1-2 items** (`rolls`).
3. Por cada tirada se elige una **RAREZA** según su peso y luego un **ITEM** dentro de esa rareza.
4. Cada item tiene además su propio **drop-chance**: los comunes caen siempre; los poderosos son muy raros.
5. El resultado se entrega así:
   - Items **Raro, Épico, Legendario y Mítico → directo al inventario** del que lo mató (no caen al suelo).
   - Comunes y Poco comunes caen al suelo (si `loot.direct-to-inventory: false`).
   - Los LEGENDARIO/MITICO brillan e ignoran lava y explosiones (`loot.glow-rarities`).

**Protecciones anti-mala-suerte (pity):** cada tirada sin obtener una rareza Épico/Legendario/Mítico suma 1 "stack" por jugador; el peso se multiplica por `(1 + stacks × pity-per-roll)`. Al obtenerla, se reinicia.

**Suerte por dificultad (luck):** las rarezas Raro en adelante son `luck-affected`: su peso sube con `difficulty.scaling.rarity-luck` (0.5 → en x3 pesan un 50% más).

---

## Resumen por rareza (tabla `mob_horde`)

| Rareza | Peso | Drop típico por item | Broadcast | Pity |
|--------|------|----------------------|-----------|------|
| Común | 600 | 100% (siempre) | no | — |
| Poco común | 250 | 65-95% (casi siempre) | no | — |
| Raro | 100 | 5-55% | no | — |
| Épico | 40 | 12-45% | no | 0.02/tirada (máx 150) |
| Legendario | 10 | 4,5-6% | **sí** | 0.05/tirada (máx 200) |
| Mítico | 1,5 | 0,5-0,8% ("oveja rosa") | **sí** | 0.08/tirada (máx 300) |

> Los pesos de arriba son los globales de `rarities:`; la tabla `mob_horde` usa sus propios pesos
> (COMUN 600 · POCO_COMUN 260 · RARO 110 · EPICO 32 · LEGENDARIO 5 · MITICO 0.5) que equivalen a
> ≈ **59,6% / 25,8% / 10,9% / 3,2% / 0,5% / 0,05%** de probabilidad por tirada.

> **Encantamientos:** vanilla al máximo de cada uno (filo 5, irrompible 3...) y hasta 5 en los
> legendarios y míticos (protección 5), más encantamientos de **ExcellentEnchants** (`vampire`,
> `thunder`, `electrified_arrows`...). Ya no hay efectos pasivos de poción.

---

## Lista completa de drops (45 items)

### Comunes — 100% (siempre caen)

| Item | Material | Cantidad |
|------|----------|----------|
| Carne | COOKED_BEEF | 3-8 |
| Flechas | ARROW | 6-16 |
| Pólvora | GUNPOWDER | 2-6 |
| Hierro | IRON_INGOT | 3-7 |
| Hueso | BONE | 3-7 |
| Cuerda | STRING | 2-5 |

### Poco comunes — 65-95%

| Item | Material | Cantidad | Drop |
|------|----------|----------|------|
| Oro | GOLD_INGOT | 2-5 | 95% |
| Manzana dorada | GOLDEN_APPLE | 1 | 90% |
| Botella XP | EXPERIENCE_BOTTLE | 3-9 | 90% |
| Ojo de ender | ENDER_EYE | 1-2 | 75% |
| Obsidiana | OBSIDIAN | 1-3 | 65% |

### Raros — 5-55%

> **Balance:** las armas rotas (Espada Sanguinaria, Arco Carmesí, Daga de las Sombras,
> Arco de la Tormenta) están capadas al **5%**; los materiales comunes son frecuentes.

| Item | Material | Drop | Encantamientos |
|------|----------|------|-------------|
| Diamante | DIAMOND (1-3) | 55% | — |
| Perla de ender | ENDER_PEARL (1-2) | 50% | — |
| Pico Umbrío | DIAMOND_PICKAXE | 30% | Prisa II · Fundidor I (Fortuna III · Eficiencia V) |
| Espada Sanguinaria | DIAMOND_SWORD | **5%** | Vampírico II · Veneno I (Filo V · Botín II) |
| Arco Carmesí | BOW | **5%** | Francotirador I (Poder V · Empuje II) |
| Daga de las Sombras | IRON_SWORD | **5%** | Golpe doble I · Aspecto de hielo II (Filo IV · Botín III) |
| Arco de la Tormenta | BOW | **5%** | Flechas electrizadas II (Poder V · Llama) |

### Épicos — 12-45%

> Subida respecto a la versión anterior: **tótem al 45%** y **fragmento de netherita al 40%** (estaban 15%/18%).

| Item | Material | Cantidad | Drop | Encantamientos |
|------|----------|----------|------|-------------|
| Tótem | TOTEM_OF_UNDYING | 1 | **45%** | — |
| Fragmento de netherita | NETHERITE_SCRAP | 1-2 | **40%** | — |
| Manzana encantada | ENCHANTED_GOLDEN_APPLE | 1 | 25% | — |
| Espada de la Ruina | NETHERITE_SWORD | 1 | 16% | Vampírico I · Marchitar II (Filo V · Botín III) |
| Hacha del Verdugo | NETHERITE_AXE | 1 | 15% | Cortador II · Decapitador I (Filo V · Eficiencia V) |
| Ballesta del Abismo | CROSSBOW | 1 | 15% | Flechas electrizadas I (Multidisparo · Perfora IV) |
| Hacha de la Ventisca | DIAMOND_AXE | 1 | 15% | Aspecto de hielo III · Parálisis II (Filo V) |
| Yelmo del Cazador | NETHERITE_HELMET | 1 | 12% | Endurecido II (Protección IV · Respiración III) |
| Peto del Cazador | NETHERITE_CHESTPLATE | 1 | 12% | Endurecido II · Corazón de dragón II (Protección IV) |
| Grebas del Cazador | NETHERITE_LEGGINGS | 1 | 12% | Endurecido II · Regeneración II (Protección IV) |
| Botas del Cazador | NETHERITE_BOOTS | 1 | 12% | Velocidad I (Protección IV · Caída de pluma IV) |
| Botas del Estrondo | NETHERITE_BOOTS | 1 | 12% | Salto II · Fuerza de frenado I (Velocidad del alma III) |

### Legendarios — 4,5-6% (con anuncio global)

> ⚠️ Las **armas, herramientas y armaduras de Raro en adelante** llevan **Maldición de desvanecimiento**:
> al morir, el objeto desaparece en lugar de caer. Incluye las Raras y Épicas (Espada Sanguinaria,
> Arco Carmesí, Daga de las Sombras, Arco de la Tormenta, Pico Umbrío, Hacha del Verdugo,
> Espada de la Ruina, Ballesta del Abismo, Hacha de la Ventisca), la armadura (Yelmo, Peto, Grebas
> y Botas del Cazador, Botas del Estrondo, Botas del Noctámbulo, Peto del Guardián, Corona, Peto y
> Grebas del Eclipse) y todos los Míticos. Los escudos no la llevan.

| Item | Material | Drop | Encantamientos |
|------|----------|------|-------------|
| Colmillo Vampírico | NETHERITE_SWORD | 6% | Vampírico III · Marchitar II (Filo V · Aspecto ígneo II) |
| Arco Lunar | BOW | 6% | Flechas vampíricas II · Francotirador II (Poder V · Infinidad) |
| Botas del Noctámbulo | NETHERITE_BOOTS | 6% | Velocidad II · Visión nocturna I (Caída de pluma IV) |
| Égida de Espinas | SHIELD | 5% | Irrompible III · Reparación |
| Martillo del Trueno | MACE | 5% | **Trueno II** · Golpe doble I (Densidad V · Brecha IV) |
| Ballesta del Alba | CROSSBOW | 5% | **Flechas electrizadas III** (Perfora IV) |
| Peto del Guardián Carmesí | NETHERITE_CHESTPLATE | 4,5% | Endurecido II · **Escudo ígneo II** · Espinas III |

### Míticos — 0,5-0,8% (limitados a 3 ejemplares, con anuncio global)

| Item | Material | Drop | Encantamientos |
|------|----------|------|-------------|
| Hoja del Eclipse Carmesí | NETHERITE_SWORD | 0,8% | Vampírico III · **Trueno II** · Marchitar II (Filo V) |
| Tridente del Abismo Rojo | TRIDENT | 0,7% | Infernal III · **Trueno I** (Empalador V) |
| Corona de la Luna de Sangre | NETHERITE_HELMET | 0,6% | Corazón de dragón III · Visión nocturna I (Protección V) |
| Maza del Coloso | MACE | 0,6% | **Trueno I** · Parálisis I (Densidad V · Brecha IV) |
| Hacha del Apocalipsis | NETHERITE_AXE | 0,6% | Golpe doble II · Vampírico II (Filo V · Eficiencia V) |
| Guadaña del Segador | NETHERITE_HOE | 0,5% | Marchitar II · Vampírico II (Filo V · Fortuna III) |
| Peto del Eclipse Carmesí | NETHERITE_CHESTPLATE | 0,5% | Endurecido II · **Escudo ígneo II** · Fuerza de frenado I |
| Grebas del Eclipse | NETHERITE_LEGGINGS | 0,5% | Velocidad II · Ligero I (Sigilo III) |

> Cada mítico tiene **número de serie** grabado; al agotarse los 3 ejemplares, deja de caer por completo.

---

## Tablas de loot

| Tabla | Drop chance | Tiradas | Notas |
|-------|-------------|---------|-------|
| `mob_horde` | **60%** | 1-2 | Base de la horda (todos los mobs etiquetados) |
| `mob_elite` | **45%** | 2-3 | Verdugo Menor (WITHER_SKELETON de la horda); pesos sesgados a rarezas altas |
| `survivors` | **100%** | 2-3 | Premio a cada superviviente al terminar el evento |
| `boss_rey_zombi` | 100% | 4-5 | Garantizado: Botella XP (8-16) + Diamante (3-6). Sin comunes/poco comunes (mín. Raro) |
| `boss_arquera` | 100% | 4-5 | Garantizado: Flechas (32-64) + Diamante (2-5). Mín. Raro |
| `boss_verdugo` | 100% | 5-6 | Garantizado: Fragmento de netherita (3-6) + Botella XP (6-12). Mín. Raro |
| `boss_top_damage` | 100% | 1 | Premio al mayor daño: solo Épico / Legendario / Mítico |

---

## Cómo escala con la dificultad

Fórmula: `valor = base × (1 + (N − 1) × factor)`

| Parámetro | Factor | x1 | x2 | x3 |
|-----------|--------|----|----|----|
| Suerte de loot (`loot-chance`) | 0.25 | 60% | 75% | 90% |
| Peso de rarezas altas (`rarity-luck`) | 0.5 | ×1 | ×1.5 | ×2 |

---

## Configuración relacionada (`config.yml`)

```yaml
loot:
  default-table: mob_horde
  survivor-table: survivors
  direct-to-inventory: false    # false = comunes al suelo, Raro+ van directo al inventario
  require-player-kill: true     # sin jugador que mate, no hay loot
  keep-vanilla-drops: false
  glow-rarities: [LEGENDARIO, MITICO]   # brillan e ignoran lava/explosiones

announce:
  loot: true                    # anuncio global al obtener LEGENDARIO / MÍTICO
```

---

## Ver probabilidades reales en el servidor

```
/luna chances <tabla>
```

Ejemplo:
```
/luna chances mob_horde
```

> El comando muestra los porcentajes ya incluyendo pity, suerte de dificultad y límites de míticos.
