# BloodMoonPlus — Guía de Jefes y Loot

Todo lo que necesitas saber sobre los **9 jefes** de la Luna de Sangre y el **sistema de drops** (45 items).

---

# 🩸 PARTE 1 — JEFES

## Cómo funcionan los jefes

- Cada jefe tiene una **barra de vida** propia (con color y estilo configurables).
- Aparecen según la **dificultad del evento**: cada jefe declara una dificultad mínima; si el evento no la alcanza, ese jefe no aparece.
- Al morir sueltan su **tabla de loot** + experiencia, y el jugador que **más daño** hizo recibe un premio extra.
- Todos los jefes llevan la etiqueta `bloodmoon_boss` (scoreboard tag), que usan el plugin y el datapack para reconocerlos.
- La vida y el daño se multiplican por la dificultad del evento. **La vida máxima se limita a 1024** (límite del juego).

### Programación de aparición

| Jefe | Segundos al inicio | Prob. | Dif. mínima |
|------|-------------------|-------|-------------|
| Rey Zombi Sangriento | 90 s | 100% | 1.0 |
| Centinela de Hueso | 180 s | 70% | 1.0 |
| Nigromante Carmesí | 240 s | 70% | 1.5 |
| Devastador Ancestral | 300 s | 60% | 1.5 |
| Verdugo de la Noche | 330 s | 60% | 1.5 |
| Cazador de las Sombras | 360 s | 50% | 1.0 |
| Arquera Carmesí | 420 s | 60% | 1.0 |
| Guardián del Abismo | 540 s | 80% | 2.0 |
| **Azote Carmesí** | 660 s | 50% | 2.5 |

## Resumen de los 9 jefes

| ID | Mob | Vida | Daño | Escala | Fase 2 | EXP |
|----|-----|------|------|--------|--------|-----|
| `rey_zombi` | ZOMBIE | 300 | 2.0 | 1.6× | — | 500 |
| `centinela_hueso` | SKELETON (blanco) | 280 | 2.0 | 1.6× | — | 550 |
| `nigromante_carmesí` | EVOKER | 320 | 2.2 | 1.5× | — | 600 |
| `cazador_sombras` | STRAY | 260 | 2.8 | 1.3× | — | 500 |
| `devastador_sangre` | RAVAGER | 380 | 2.4 | 1.6× | ✅ 45% | 700 |
| `arquera_carmesi` | SKELETON | 220 | 1.8 | 1.4× | — | 400 |
| `verdugo_noche` | WITHER_SKELETON | 400 | 2.5 | 1.8× | ✅ 40% | 800 |
| `warden_abismal` | WARDEN | 750 | 3.6 | 1.2× | ✅ 50% | 1100 |
| `azote_carmesi` | WITHER | 900 | 4.0 | 1.0× | ✅ 50% | 1500 |

## Fichas detalladas

### 👑 Rey Zombi Sangriento (`rey_zombi`)
- **Mob:** ZOMBIE con armadura netherita completa + espada netherita, escala 1.6×, resistencia al empuje 0.6.
- **Barra:** roja, segmentada ×10.
- **Habilidades:** invoca 4 zombis cada 25 s · SLAM (radio 7, daño 4, empuje) cada 12 s.
- **Aparición:** 90 s, 100%, dificultad mínima 1.0. **EXP:** 500.
- **Loot:** tabla `boss_rey_zombi`.

### 🛡️ Centinela de Hueso (`centinela_hueso`)
- **Mob:** esqueleto **blanco** con **escudo + espada netherita**, armadura de diamante completa. Misma altura que el Rey Zombi (escala 1.6×), resistencia al empuje 0.5.
- **Barra:** blanca, segmentada ×10.
- **Habilidades:** GUARD (bloquea con el escudo 4 s cada 18 s: resistencia + absorción) · SLAM (radio 6, daño 5) · invoca 3 esqueletos cada 30 s.
- **Aparición:** 180 s, 70%, dificultad mínima 1.0. **EXP:** 550.
- **Loot:** tabla `boss_arquera`.

### 🔮 Nigromante Carmesí (`nigromante_carmesí`)
- **Mob:** EVOKER con tótem en mano y casco dorado, escala 1.5×.
- **Barra:** púrpura, segmentada ×10.
- **Habilidades:** invoca 5 zombis cada 18 s · invoca 3 arañas cada 30 s · AURA de debilidad (8 m) · rayo cada 40 s.
- **Aparición:** 240 s, 70%, dificultad mínima 1.5. **EXP:** 600.
- **Loot:** tabla `boss_rey_zombi`.

### 🏹 Cazador de las Sombras (`cazador_sombras`)
- **Mob:** STRAY con arco y peto de cuero. El más rápido de todos (velocidad 1.4), escala 1.3×.
- **Barra:** negra, segmentada ×10.
- **Habilidades:** AURA de invisibilidad 8 s cada 20 s (se desvanece y reaparece) · rayo cada 25 s.
- **Aparición:** 360 s, 50%, dificultad mínima 1.0. **EXP:** 500.
- **Loot:** tabla `boss_arquera`.

### 💀 Verdugo de la Noche (`verdugo_noche`)
- **Mob:** WITHER_SKELETON con hacha netherita y casco netherita. **El más grande** (escala 1.8×), resistencia al empuje 0.8.
- **Barra:** rosa, segmentada ×12.
- **Habilidades:** AURA de marchitar 6 s (12 m) · SLAM (radio 8, daño 5) · invoca 3 esqueletos de la Nether cada 30 s.
- **Fase 2 (enloquecido) al 40%** de vida: habilidades al 65% de cooldown, daño ×1.5.
- **Aparición:** 330 s, 60%, dificultad mínima 1.5. **EXP:** 800.
- **Loot:** tabla `boss_verdugo`.

### 🐗 Devastador Ancestral (`devastador_sangre`)
- **Mob:** RAVAGER sin equipo, escala 1.6×, resistencia al empuje 0.9 (casi no se mueve).
- **Barra:** roja, segmentada ×12.
- **Habilidades:** EMBESTIDA cada 14 s (se lanza a toda velocidad hacia un jugador y arrolla ~1 s) · rugido de lentitud 6 s (14 m) · SLAM (radio 7, daño 5).
- **Fase 2 (enloquecido) al 45%** de vida: habilidades al 60% de cooldown, daño ×1.6.
- **Aparición:** 300 s, 60%, dificultad mínima 1.5. **EXP:** 700.
- **Loot:** tabla `boss_verdugo`.

### 🏹 Arquera Carmesí (`arquera_carmesi`)
- **Mob:** SKELETON con casco y peto de diamante, escala 1.4×.
- **Barra:** púrpura, segmentada ×10.
- **Habilidades:** invoca 3 rayos sobre jugadores cercanos cada 15 s · AURA de lentitud 5 s (12 m).
- **Aparición:** 420 s, 60%, dificultad mínima 1.0. **EXP:** 400.
- **Loot:** tabla `boss_arquera`.

### 🌊 Guardián del Abismo (`warden_abismal`)
- **Mob:** WARDEN, escala 1.2×, resistencia al empuje total (1.0). **750 de vida.**
- **Barra:** azul, segmentada ×12.
- **Habilidades:** ONDA SÓNICA cada 12 s (radio 11, daño 13, lanza a los jugadores lejos) · PULL (atrae a 15 m, fuerza 1.4) cada 20 s · rugido de **oscuridad** 6 s (16 m) · SLAM (radio 8, daño 7).
- **Fase 2 (enloquecido) al 50%** de vida: habilidades al 60% de cooldown, daño ×1.6.
- **Aparición:** 540 s, 80%, dificultad mínima 2.0. **EXP:** 1100.
- **Loot:** tabla `boss_verdugo`.

### ☠️ Azote Carmesí (`azote_carmesi`) — JEFE FINAL
- **Mob:** WITHER, resistencia al empuje 0.95 (no se deja mover). **900 de vida** (hasta 1024 tras el multiplicador de dificultad), daño 4.0, velocidad 1.25.
- **Barra:** púrpura, segmentada ×12.
- **Habilidades:** CALAVERAS DE WITHER cada 7 s (**sí rompe bloques**, como el Wither vanilla) · PULL (atrae a 20 m) cada 16 s · AURA de marchitar 8 s (16 m) · invoca 4 esqueletos de la Nether cada 20 s · rugido de oscuridad 6 s · **SELF_HEAL** (se cura 80 de vida cada 24 s).
- **Fase 2 (enloquecido) al 50%** de vida: habilidades al **55%** de cooldown, daño **×1.75** — el pico de daño del servidor.
- **Aparición:** 660 s, 50%, dificultad mínima 2.5. **EXP:** 1500.
- **Loot:** tabla `boss_verdugo`.
- **Nota:** es el **único Wither permitido**. El plugin cancela el spawn de cualquier otro Wither (por hechizo, huevo o spawner); el datapack opcional `BloodMoonWitherControl` lo remata a cada tick como segunda capa de seguridad. Sus calaveras rompen bloques: protege tu base o pelea en campo abierto.

## Segunda fase (enloquecido)

4 jefes tienen fase 2 configurable en `bosses.yml`:

| Jefe | Se activa al... | Cooldown | Daño |
|------|-----------------|----------|------|
| `verdugo_noche` | 40% de vida | ×0.65 | ×1.5 |
| `devastador_sangre` | 45% de vida | ×0.6 | ×1.6 |
| `warden_abismal` | 50% de vida | ×0.6 | ×1.6 |
| `azote_carmesi` | 50% de vida | ×0.55 | ×1.75 |

Al activarse (una sola vez por jefe):
1. Gana **Velocidad II + Fuerza II**.
2. **Todas sus habilidades se recargan al instante** y salen más seguido.
3. Explosión de partículas rojas + rugido de dragón, y se anuncia a los jugadores cercanos: *«¡{boss} ENLOQUECIÓ!»*.

## Habilidades de jefes (DSL de `bosses.yml`)

| Habilidad | Formato | Efecto |
|-----------|---------|--------|
| `SUMMON` | `cd:cantidad:TIPO` | Invoca refuerzos |
| `SLAM` | `cd:radio:daño` | Golpe de área con empuje |
| `LIGHTNING` | `cd:objetivos:daño` | Rayos sobre jugadores cercanos |
| `AURA` | `cd:radio:efecto:seg:nivel` | Efecto de poción en área (wither, slowness...) |
| `SONIC_BOOM` | `cd:radio:daño` | Oda expansiva que lanza lejos (estilo Warden) |
| `PULL` | `cd:radio:fuerza` | Atrae a los jugadores hacia el jefe |
| `SKULL` | `cd:cantidad` | Calaveras de wither que **rompen bloques** |
| `CHARGE` | `cd:fuerza:daño` | Embestida: se lanza y arrolla ~1 s |
| `ROAR` | `cd:radio:efecto:seg:nivel` | Rugido con efecto en área (darkness...) |
| `GUARD` | `cd:segundos` | Bloqueo: resistencia + absorción |
| `SELF_HEAL` | `cd:cantidad` | Se regenera vida (jefe tanque) |

## Loot por jefe

Los jefes usan tablas propias con **peso 0 en Común y Poco común**: solo sueltan **Raro o superior**, y siempre tienen drops **garantizados** además de las tiradas.

| Tabla | La usan | Garantizado | Tiradas | Raro | Épico | Leg. | Mítico |
|-------|---------|-------------|---------|------|-------|------|--------|
| `boss_rey_zombi` | Rey Zombi, Nigromante | Botella XP 8-16, Diamante 3-6 | 4-5 | 48.7% | 36.5% | 13.4% | 1.5% |
| `boss_arquera` | Arquera, Cazador, Centinela | Flechas 32-64, Diamante 2-5 | 4-5 | 48.0% | 36.0% | 14.4% | 1.7% |
| `boss_verdugo` | Verdugo, Devastador, Guardián, **Azote** | Fragmento de netherita 3-6, Botella XP 6-12 | 5-6 | 31.7% | 45.2% | 20.4% | 2.7% |
| `boss_top_damage` | Mayor daño contra cualquier jefe | — | 1 | — | 38.2% | 47.8% | 14.0% |

> Los porcentajes son el sorteo de rareza **antes** del drop-chance individual de cada item
> (los míticos aún aplican su 0.5-0.8%). Cuanto más fuerte el jefe, mejor la tabla:
> el **Azote Carmesí** usa la mejor (`boss_verdugo`).

---

# 💎 PARTE 2 — LOOT

## Cómo funciona la probabilidad de drop

Cada muerte de un mob de la horda recorre **5 pasos**. Si entiendes estos 5, entiendes todo el loot:

```
 kill del mob
      │
      ▼
 [1] ¿SUELTA ALGO?  ── No (90% en x1) ──▶ nada
      │ Sí (10%)
      ▼
 [2] ¿CUÁNTAS TIRADAS?  ── se sortea 1 o 2
      │
      ▼
 [3] ¿QUÉ RAREZA?  ── sorteo por pesos (Común 59.6% ... Mítico 0.05%)
      │
      ▼
 [4] ¿QUÉ ITEM?  ── sorteo por peso dentro de esa rareza
      │
      ▼
 [5] ¿SALE?  ── drop-chance individual del item
      │               │
     Sí             No ──▶ la tirada NO da nada
      ▼
    🎁 item
```

### Paso 1 — ¿Suelta algo el mob? (probabilidad de la tabla)

Cada tabla de loot tiene una probabilidad base que se multiplica por la dificultad del evento:

```
probabilidad real = tabla × (1 + (dificultad − 1) × 0.25)
```

| Dificultad | Multiplicador | `mob_horde` (10%) | `mob_elite` (45%) |
|------------|---------------|-------------------|-------------------|
| x1 | ×1.00 | 10% | 45% |
| x2 | ×1.25 | 12.5% | 56% |
| x3 | ×1.50 | 15% | 67.5% |
| x5 | ×2.00 | 20% | 90% |
| x10 | ×3.25 | 32.5% | 100% |

> Un evento programado con `difficulty: 3` da **media vez más** loot que uno normal.

### Paso 2 — ¿Cuántas tiradas?

Si el paso 1 pasó, se tira la tabla: en `mob_horde` es `1-2`, es decir, se sortea **un número al azar entre 1 y 2** items (promedio 1.5). Cada tirada es **independiente** y recorre los pasos 3-5 por su cuenta. Mobs élite y supervivientes tiran 2-3; los jefes 4-6.

### Paso 3 — Sorteo de la rareza (pesos relativos)

No todos los colores pesan lo mismo. Este es el sorteo real de `mob_horde`:

| Rareza | Peso | **% real por tirada** |
|--------|------|----------------------|
| Común | 600 | **59.6%** |
| Poco común | 260 | **25.8%** |
| Raro | 110 | **10.9%** |
| Épico | 32 | **3.2%** |
| Legendario | 5 | **0.50%** |
| Mítico | 0.5 | **0.05%** |

**Suerte (luck):** las rarezas marcadas como `luck-affected` (Raro en adelante) multiplican su peso por `1 + (dificultad − 1) × 0.5`. En un evento x3, Raro+ **duplican** su peso: cuanto más duro el evento, mejor el botín.

### Paso 4 — Sorteo del item dentro de la rareza

Todos los items de esa rareza compiten por peso. Ejemplo, si sale **Raro** (pesos 40/30/15/15/14/14/12):

- Diamante: 28.6% · Perla de ender: 21.4% · Espada Sanguinaria: 10.7% · Arco Carmesí: 10.7% · Daga: 10% · Arco Tormenta: 10% · Pico Umbrío: 8.6%

### Paso 5 — Drop-chance individual del item (¡clave!)

El item sorteado tiene su **propia** probabilidad de drop. **Si falla, esa tirada no da nada** (no se reemplaza por otro item):

| Drop-chance | Items | Resultado |
|-------------|-------|-----------|
| 100% | Comunes y materiales simples | El item sorteado **siempre cae** |
| 25-50% | Diamante, perlas, obsidiana | Cae la mayoría de veces |
| **5%** | **Armas rotas Raro/Legendario** | Capado a propósito |
| 10-20% | Épicos | Baja probabilidad |
| 0.5-0.8% | Míticos | "Oveja rosa" |

> Por eso los comunes son 100%: si el sorteo te dio un Común, no hay segunda oportunidad de fallar.
> Y por eso una Espada Sanguinaria al 5% es **mucho** más rara de lo que sugiere su peso del 10.7%:
> 10.7% × 5% = **0.54% real** cuando ya te salió Raro.

### Suerte (luck) y pity (protección contra mala suerte)

- **Luck:** peso de Raro+ × `1 + (dificultad − 1) × 0.5` (solo rinde en eventos duros).
- **Pity:** cada tirada que **no** te da una rareza alta suma 1 punto *para ti*. Multiplicador: `1 + puntos × pity-per-roll`. Al obtenerla, **se reinicia a 0**.

| Rareza | Pity por tirada fallida | Máx. puntos | Multiplicador máximo |
|--------|------------------------|-------------|----------------------|
| Épico | +0.02 | 150 | ×4 |
| Legendario | +0.05 | 200 | ×11 |
| Mítico | +0.08 | 300 | ×25 |

> El pity es **por jugador y por rareza**: si llevas 300 tiradas sin mítico, tu probabilidad de mítico es 25 veces la base. Los jefes y la recompensa de supervivientes aceleran esto muchísimo.

### Ejemplo real: 100 kills en un evento x1

1. ~10 kills pasan el paso 1 (10%).
2. Esos 10 generan **~15 tiradas** (promedio 1.5 por kill).
3. Resultado esperado de esas 15 tiradas:

| Sale... | Tiradas | Items reales (tras su drop-chance) |
|---------|---------|-------------------------------------|
| Común | 8.9 | ~8.9 items (siempre caen) |
| Poco común | 3.9 | ~3.1 items |
| Raro | 1.6 | ~0.45 items (diamantes y perlas casi siempre) |
| Épico | 0.48 | ~0.06 items |
| Legendario | 0.075 | ~0.004 items |
| Mítico | 0.0075 | casi nada (los míticos se consiguen en jefes) |

> Traducción: **~12 items por cada 100 kills**, en su mayoría comida y materiales, de vez en cuando un diamante.
> El botín de verdad está en los **jefes** (tablas Raro+ garantizado), en la **recompensa de superviviente**
> (100%, 2-3 tiradas) y en el **premio al mayor daño** (solo Épico+).

### Reglas globales de balance

- **Míticos:** máximo **3 ejemplares en todo el servidor** (número de serie en el lore); al agotarse, dejan de caer.
- **Armas rotas Raro/Legendario capadas al 5%**: Espada Sanguinaria, Arco Carmesí, Daga de las Sombras, Arco de la Tormenta y Colmillo Vampírico.
- **Armas legendarias con Maldición de desvanecimiento**: si mueres con ellas, desaparecen (riesgo real de llevarlas).
- **Encantamientos:** el nivel nunca supera **7** (se recorta al cargar).
- Solo cuenta matar a mobs de la horda con un jugador cerca (`require-player-kill`).
- Los items Legendario y Mítico **brillan** al caer e ignoran lava y explosiones.

### Distribución de rareza por tabla

Porcentajes del paso 3 en cada tabla (antes del drop-chance individual):

| Tabla | Común | Poco común | Raro | Épico | Leg. | Mítico |
|-------|-------|-----------|------|-------|------|--------|
| `mob_horde` (10%, 1-2 tiradas) | 59.6% | 25.8% | 10.9% | 3.2% | 0.50% | 0.05% |
| `mob_elite` (45%, 2-3 tiradas) | 26.5% | 33.1% | 26.5% | 11.9% | 1.85% | 0.16% |
| `supervivientes` (100%, 2-3 tiradas) | 33.1% | 33.1% | 22.1% | 9.9% | 1.55% | 0.13% |
| `boss_rey_zombi` (100%, 4-5) | — | — | 48.7% | 36.5% | 13.4% | 1.5% |
| `boss_arquera` (100%, 4-5) | — | — | 48.0% | 36.0% | 14.4% | 1.7% |
| `boss_verdugo` (100%, 5-6) | — | — | 31.7% | 45.2% | 20.4% | 2.7% |
| `boss_top_damage` (100%, 1) | — | — | — | 38.2% | 47.8% | 14.0% |

### Referencia de rarezas

| Rareza | Suerte | Pity | Aviso global | Brilla al caer |
|--------|--------|------|--------------|----------------|
| Común | — | — | — | — |
| Poco común | — | — | — | — |
| Raro | ✅ | — | — | — |
| Épico | ✅ | 0.02/tirada | — | — |
| Legendario | ✅ | 0.05/tirada | ✅ | ✅ |
| Mítico | ✅ | 0.08/tirada | ✅ | ✅ |

---

## 📦 Lista completa de items (45)

### Comunes (siempre caen)

| Item | Material | Cantidad |
|------|----------|----------|
| Carne | COOKED_BEEF | 3-8 |
| Flechas | ARROW | 6-16 |
| Pólvora | GUNPOWDER | 2-6 |
| Hierro | IRON_INGOT | 2-5 |
| Hueso | BONE | 2-6 |
| Cuerda | STRING | 1-4 |

### Poco comunes

| Item | Material | Cantidad | Drop |
|------|----------|----------|------|
| Oro | GOLD_INGOT | 2-4 | 90% |
| Manzana dorada | GOLDEN_APPLE | 1 | 85% |
| Botella XP | EXPERIENCE_BOTTLE | 3-9 | 85% |
| Ojo de ender | ENDER_EYE | 1-2 | 70% |
| Obsidiana | OBSIDIAN | 1-3 | 60% |

### Raros

> **Nota de balance:** las **armas** Raro/Legendario rotas están capadas al **5%** de drop
> (Espada Sanguinaria, Arco Carmesí, Daga de las Sombras, Arco de la Tormenta, Colmillo Vampírico).
> Los míticos ya eran más raros de por sí (0.5-0.8%).

| Item | Material | Drop | Habilidades |
|------|----------|------|-------------|
| Diamante | DIAMOND (1-3) | 50% | — |
| Perla de ender | ENDER_PEARL (1-2) | 45% | — |
| Espada Sanguinaria | DIAMOND_SWORD | **5%** | Robavida 5%, Veneno (20%: 4 s) |
| Arco Carmesí | BOW | **5%** | Daño +6% |
| **Daga de las Sombras** | IRON_SWORD | **5%** | Crítico (25%: ×1.8), Lentitud, Veneno · Pasiva: Velocidad I |
| **Arco de la Tormenta** | BOW | **5%** | Rayo sin tormenta (15%: 6), Daño +8% |
| **Pico Umbrío** | DIAMOND_PICKAXE | 25% | Fortuna III, Eficiencia V · Pasiva: Haste I |

### Épicos

| Item | Material | Drop | Habilidades |
|------|----------|------|-------------|
| Manzana encantada | ENCHANTED_GOLDEN_APPLE | 20% | — |
| Tótem | TOTEM_OF_UNDYING | 15% | — |
| Fragmento de netherita | NETHERITE_SCRAP (1-2) | 18% | — |
| Hacha del Verdugo | NETHERITE_AXE | 12% | Daño +10%, Ejecución (≤30% vida: +50%) |
| Yelmo del Cazador | NETHERITE_HELMET | 10% | Reducción 3%, Reflejo 15% |
| Peto del Cazador | NETHERITE_CHESTPLATE | 10% | Reducción 4% · Pasiva: Resistencia I |
| Pantalón del Cazador | NETHERITE_LEGGINGS | 10% | Reducción 4% |
| Botas del Cazador | NETHERITE_BOOTS | 10% | Reducción 3% |
| **Espada de la Ruina** | NETHERITE_SWORD | 14% | Crítico (20%: ×1.9), Sangrado, Robavida 6% · Pasiva: Fuerza I |
| **Ballesta del Abismo** | CROSSBOW | 12% | Cadena de rayos, Rayo (10%: 6), Perfora IV |
| **Botas del Estrondo** | NETHERITE_BOOTS | 10% | Esquiva 8%, Levitación (25%), Embate (20%) · Pasiva: Salto I |
| **Hacha de la Ventisca** | DIAMOND_AXE | 12% | Crítico (22%: ×1.9), Lentitud, Marchitar |

### Legendarios

> ⚠️ **Todas las armas legendarias llevan Maldición de desvanecimiento** (`vanishing_curse`):
> si mueres con ella en el inventario, el arma **desaparece** — no cae al suelo y nadie puede recogerla.
> Solo aplica a armas (Colmillo, Arco Lunar, Martillo, Ballesta), no a la armadura legendaria.

| Item | Material | Drop | Habilidades |
|------|----------|------|-------------|
| Colmillo Vampírico | NETHERITE_SWORD | **5%** | Robavida 12%, Marchitar (20%: 5 s) · Pasiva: Fuerza I |
| Arco Lunar | BOW | 5% | Daño +15%, Incendiar (30%: 6 s), Lentitud (25%: 4 s) |
| Botas del Noctámbulo | NETHERITE_BOOTS | 5% | Esquiva 10%, Reducción 5% · Pasivas: Velocidad I, Salto I |
| Égida de Espinas | SHIELD | 4% | Pasiva: Resistencia I |
| **Martillo del Trueno** | MACE | 5% | Crítico (25%: ×2.2), Rayo (25%: 9), Verdugo de jefes +25% · Pasiva: Fuerza I |
| **Peto del Guardián Carmesí** | NETHERITE_CHESTPLATE | 4.5% | **Rayo vengador (20%: cae un rayo sobre el atacante, sin tormenta)**, Reducción 5% · Pasivas: Resistencia I, Aura de curación (radio 6) |
| **Ballesta del Alba** | CROSSBOW | 5% | Cadena de rayos III, Daño +15%, Perfora IV |

### Míticos (máx. 3 por servidor, con número de serie)

| Item | Material | Drop | Habilidades |
|------|----------|------|-------------|
| Hoja del Eclipse Carmesí | NETHERITE_SWORD | 0.8% | Robavida 20%, Rayos (15%: 8), Verdugo de jefes +35%, Ejecución · Pasiva: Fuerza I |
| Corona de la Luna de Sangre | NETHERITE_HELMET | 0.6% | Reducción 8%, Espinas, Reflejo 20% · Pasivas: Visión nocturna, Vida extra II, Regeneración I |
| Guadaña del Segador | NETHERITE_HOE | 0.5% | Ejecución, Marchitar, Robavida 10%, Crítico (25%: ×2.2) |
| Tridente del Abismo Rojo | TRIDENT | 0.7% | Rayos (30%: 10), Incendiar (50%: 8 s), Daño +20% |
| Maza del Coloso | MACE | 0.6% | Daño +25%, Verdugo de jefes +40%, Lentitud |
| **Hacha del Apocalipsis** | NETHERITE_AXE | 0.6% | Crítico brutal (30%: ×2.5), Robavida 15%, Verdugo de jefes +40%, Sangrado · Pasiva: Fuerza II |
| **Peto del Eclipse Carmesí** | NETHERITE_CHESTPLATE | 0.5% | **Rayo vengador (25%)**, Embate (sin romper bloques), Reducción 6% · Pasivas: Resistencia I, Regeneración I, Aura de curación (radio 8) |
| **Grebas del Eclipse** | NETHERITE_LEGGINGS | 0.5% | Esquiva 10%, Reducción 4% · Pasivas: Velocidad I, Caída lenta |

---

## ⚔️ Habilidades de items (19)

### Armas (12)
| Ability | Parámetros | Efecto |
|---------|-----------|--------|
| `DAMAGE_BONUS` | `f` | Multiplica el daño |
| `LIFESTEAL` | `f` | Roba % de vida al dañar |
| `LIGHTNING` | `prob:daño` | Invoca rayo (sin tormenta) |
| `IGNITE` | `prob:seg` | Quema al enemigo |
| `WITHER` | `prob:seg` | Aplica marchitar |
| `SLOW` | `prob:seg` | Aplica lentitud |
| `EXECUTE` | `vidaFrac:bonusFrac` | Bonus a enemigos con poca vida |
| `BOSS_SLAYER` | `f` | Bonus contra mobs de la Luna de Sangre |
| `CHAIN_LIGHTNING` | `prob:cadenas:daño:decaimiento` | Rayo que salta a N enemigos |
| `POISON` | `prob:seg:nivel` | Aplica veneno |
| `BLEED` | `prob:seg:nivel` | Daño en el tiempo con partículas |
| `CRIT` | `prob:multiplicador` | Golpe crítico con partículas |

### Armadura (7)
| Ability | Parámetros | Efecto |
|---------|-----------|--------|
| `DAMAGE_REDUCTION` | `f` | Reduce el daño recibido |
| `DODGE` | `prob` | Esquiva el golpe |
| `REFLECT` | `f` | Devuelve % del daño al atacante |
| `THORNS` | `prob:daño` | Daña al atacante |
| `THUNDER_GUARD` | `prob:daño:attacker\|self` | Si te pegan, cae un **rayo sin tormenta** sobre el atacante (o sobre ti con `self`) |
| `BLAST_WAVE` | `prob:daño:fuerza` | Onda expansiva que daña y lanza al atacante; **no rompe bloques** |
| `LEVITATE` | `prob:seg:nivel` | El atacante queda flotando |

### Pasivos (atributos = efectos de poción)
| Pasivo | Parámetros | Efecto |
|--------|-----------|--------|
| `strength`, `speed`, `resistance`, `health_boost`, `regeneration`, `haste`, `night_vision`, `slow_falling`, `jump_boost`... | `efecto:nivel` | Atributo continuo mientras se porta |
| `HEAL_AURA` | `radio:curación` | Cura a los aliados cercanos |

---

## 🔧 Ver probabilidades en el servidor

```
/luna chances mob_horde
/luna chances boss_verdugo
```

Muestra los porcentajes reales de rareza y de cada item de la tabla.

---

## 📜 Licencia

**Todos los derechos reservados.**
El código de este proyecto es público **únicamente para lectura y auditoría de
seguridad**. No se permite copiarlo, modificarlo, reutilizarlo ni
redistribuirlo. Sí puedes descargar y **ejecutar** el plugin compilado en tu
servidor. Consulta el archivo [`LICENSE`](LICENSE) para los detalles.

---

## 📤 Publicación en Modrinth

Los materiales listos para publicar (descripción, changelog, icono y guía paso a
paso) están en la carpeta [`modrinth/`](modrinth/). Empieza por
[`modrinth/PUBLISH.md`](modrinth/PUBLISH.md).
