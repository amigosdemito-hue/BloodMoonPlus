# BloodMoonPlus — Guía de Jefes y Loot

🌐 **Español** · [English](README.en.md)

Todo lo que necesitas saber sobre los **10 jefes** de la Luna de Sangre y el **sistema de drops** (104 objetos), más el catálogo completo y cómo se calculan las probabilidades.

---

## 🔌 Dependencias

**Obligatorio: Paper 26.2 o superior con Java 25.** El plugin no necesita ningún otro plugin para arrancar.

| Plugin | Versión probada | Tipo | Qué aporta | Si falta |
|---|---|---|---|---|
| **Paper** | 26.2 / 26.3 (compilado contra la API 26.3) | Obligatorio | Servidor | El plugin no carga |
| **ExcellentEnchants** + **NightCore** | EE 5.4.3 · NightCore 2.16.7 | Opcional, muy recomendado | Los encantamientos custom (`vampire`, `thunder`, `tunnel`, `veinminer`, `dragon_heart`…) de los 104 objetos | Los objetos se crean solo con los vanilla y la consola avisa; los ya repartidos con EE se pierden al entrar (Minecraft borra los encantamientos que no existen) |
| **PlaceholderAPI** | 2.11.6 | Opcional | Los placeholders `%bloodmoon_*%` | Todo lo demás funciona; simplemente no hay placeholders |
| **MythicMobs** | 5.12+ | Opcional | Jefes con `mode: MYTHICMOBS` en `bosses.yml` | Se usa el jefe vanilla si `mythic-fallback: true`; con el fallback desactivado, ese jefe no aparece |

- En `plugin.yml` van como `softdepend`, así que el orden de carga es correcto sin obligarte a instalarlos (ninguna dependencia dura).
- **NightCore no se usa directamente**: es una dependencia de ExcellentEnchants, no de este plugin.
- El puente con MythicMobs es por reflexión (versión 5.x): si instalas otra mayor, el plugin avisa y sigue funcionando con los jefes vanilla.

---

## 🔔 Avisos en Discord

El plugin puede anunciar en un canal de Discord **cuánto falta para la luna, cuándo empieza y cuándo termina**, sin instalar un bot ni darle un token a nadie: se usa un **webhook** del canal.

### Crear el webhook

1. En Discord: **Ajustes del canal → Integraciones → Webhooks → Nuevo webhook**.
2. Elige el canal, ponle nombre y pulsa **Copiar URL del webhook**.
3. Pega la URL en `plugins/BloodMoonPlus/config.yml`:

```yaml
discord:
  enabled: true
  webhook-url: "https://discord.com/api/webhooks/123456789/abcdef..."
  username: "Luna de Sangre"
  avatar-url: ""
  warnings-minutes: [60, 30, 10, 5]
  notify:
    warnings: true
    start: true
    end: true
```

4. `/luna reload`. La consola confirma con `Avisos de Discord activados (antes: [60, 30, 10, 5] min)`.

### Qué se publica

Cada aviso es un **embed** de Discord, no una línea de texto: emoji y título corto, una frase de
ambientación y los datos en campos en línea (cuándo, dificultad, mundo, duración). Las horas usan
marcas de tiempo de Discord (`<t:...:R>`), así que cada persona ve la cuenta atrás en vivo y en su
propio idioma.

| Aviso | Cuándo | Qué muestra |
|---|---|---|
| Cuenta atrás | En cada valor de `warnings-minutes`, solo si no hay una luna en curso | Cuenta atrás en vivo, hora de inicio y dificultad |
| Inicio | Al arrancar el evento (una sola vez aunque sean varios mundos) | Mundo, dificultad, duración y cuándo termina |
| Fin | Al terminar sola o con `/luna stop` | Mundo, duración y resultado (`Amanecer` o `Detenida por un administrador`) |

- Los textos viven en `locales/<idioma>.yml` (`discord.*`), así que respetan el idioma activo del servidor.
- **Si ya hay una luna en marcha no se envía la cuenta atrás del siguiente hueco**: antes llegaba un «faltan 10 min» en mitad del evento.
- El envío va en un hilo aparte: si Discord está lento, caído o limitando peticiones, el evento no se frena; solo queda un aviso en consola.
- La URL se valida al cargar (solo `discord.com`, `canary`, `ptb` o `discordapp`). Una URL mal pegada deja la función apagada y lo dice en consola en vez de fallar en cada evento.
- `allowed_mentions` va vacío: aunque un texto contenga `@everyone`, el mensaje nunca pingea al canal.
- Para probar sin Discord: apunta `webhook-url` a `http://127.0.0.1:PUERTO/...`; ese destino se acepta para pruebas locales.

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
| Fauces del Cataclismo | 270 s | 70% | 1.0 |
| Guardián del Abismo | 540 s | 80% | 2.0 |
| **Azote Carmesí** | 660 s | 50% | 2.5 |

## Resumen de los 10 jefes

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
| `creeper_cargado` | CREEPER (cargado) | 320 | 2.2 | 2.0× | ✅ 40% | 650 |
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

### 🟩 Fauces del Cataclismo (`creeper_cargado`) — creeper cargado
- **Mob:** CREEPER **cargado (eléctrico)** del **doble de alto** que uno normal (escala 2.0×). **320 de vida**, daño 2.2, velocidad 1.05, resistencia al empuje 0.6.
- **Barra:** verde, segmentada ×10.
- **Habilidades (recargas escalonadas 12/16/24/35/50, nunca coinciden):** SLAM cada 12 s (radio 6, daño 4) · **VORTEX** cada 16 s (radio 18 m, 4 s de absorción, fuerza 1.6, 2.5 de daño al tocar su cuerpo) · CHARGE cada 24 s (fuerza 1.2, 6 de daño) · **SWARM** cada 35 s (3 **Crías del Cataclismo**: creepers cargados de 30 PV, mecha de 3 s y explosión de radio 3 que mueren al explotar) · **EXPLODE** cada 50 s (radio **15 m**, hasta 12 de daño en el centro con caída al 25% en el borde, más empuje).
- **Inmunidad:** **no le hace daño ninguna explosión** — ni la suya, ni la de sus crías, ni TNT o camas — así que puede detonar sin autodestruirse. Máximo 6 crías vivas a la vez.
- **Fase 2 (enloquecido) al 40%** de vida: habilidades al 60% de cooldown, daño ×1.5 y **su explosión abre un cráter de ~6 bloques** (en fase normal no toca el terreno).
- **Aparición:** 270 s, 70%, dificultad mínima 1.0. **EXP:** 650.
- **Loot:** tabla `boss_verdugo`.
- **Nota:** es el jefe que castiga las granjas AFK: su explosión de 15 m golpea a **todos** los mobs y jugadores del área, y las crías obligan a moverse aunque te quedes a distancia.

### ☠️ Azote Carmesí (`azote_carmesi`) — JEFE FINAL
- **Mob:** WITHER, resistencia al empuje 0.95 (no se deja mover). **900 de vida** (hasta 1024 tras el multiplicador de dificultad), daño 4.0, velocidad 1.25.
- **Barra:** púrpura, segmentada ×12.
- **Habilidades:** CALAVERAS DE WITHER cada 7 s (**sí rompe bloques**, como el Wither vanilla) · PULL (atrae a 20 m) cada 16 s · AURA de marchitar 8 s (16 m) · invoca 4 esqueletos de la Nether cada 20 s · rugido de oscuridad 6 s · **SELF_HEAL** (se cura 80 de vida cada 24 s).
- **Fase 2 (enloquecido) al 50%** de vida: habilidades al **55%** de cooldown, daño **×1.75** — el pico de daño del servidor.
- **Aparición:** 660 s, 50%, dificultad mínima 2.5. **EXP:** 1500.
- **Loot:** tabla `boss_verdugo`.
- **Nota:** es el **único Wither permitido**. El plugin cancela el spawn de cualquier otro Wither (por hechizo, huevo o spawner); el datapack opcional `BloodMoonWitherControl` lo remata a cada tick como segunda capa de seguridad. Sus calaveras rompen bloques: protege tu base o pelea en campo abierto.

## Segunda fase (enloquecido)

5 jefes tienen fase 2 configurable en `bosses.yml`:

| Jefe | Se activa al... | Cooldown | Daño |
|------|-----------------|----------|------|
| `verdugo_noche` | 40% de vida | ×0.65 | ×1.5 |
| `devastador_sangre` | 45% de vida | ×0.6 | ×1.6 |
| `warden_abismal` | 50% de vida | ×0.6 | ×1.6 |
| `creeper_cargado` | 40% de vida | ×0.6 | ×1.5 |
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
| `VORTEX` | `cd:radio:seg:fuerza:daño` | **Absorción**: arrastra a los jugadores hacia el jefe varios segundos y los daña al tocar su cuerpo |
| `SKULL` | `cd:cantidad` | Calaveras de wither que **rompen bloques** |
| `CHARGE` | `cd:fuerza:daño` | Embestida: se lanza y arrolla ~1 s |
| `ROAR` | `cd:radio:efecto:seg:nivel` | Rugido con efecto en área (darkness...) |
| `GUARD` | `cd:segundos` | Bloqueo: resistencia + absorción |
| `SELF_HEAL` | `cd:cantidad` | Se regenera vida (jefe tanque) |

## Loot por jefe

Los jefes usan tablas propias con **peso 0 en Común y Poco común**: solo sueltan **Raro o
superior**, y siempre tienen drops **garantizados** además de las tiradas.

| Tabla | La usan | Garantizado | Tiradas | Raro | Épico | Leg. | Mítico |
|---|---|---|---|---|---|---|---|
| `boss_rey_zombi` | Rey Zombi, Nigromante | Botella de XP ×8-16, Diamante ×3-6 | 4-5 | 48.7% | 36.5% | 13.4% | 1.5% |
| `boss_arquera` | Arquera, Cazador, Centinela | Flechas ×32-64, Diamante ×2-5 | 4-5 | 48.0% | 36.0% | 14.4% | 1.7% |
| `boss_verdugo` | Verdugo, Devastador, Guardián, Fauces, **Azote** | Fragmento de netherita ×3-6, Botella de XP ×6-12 | 5-6 | 31.7% | 45.2% | 20.4% | 2.7% |
| `boss_top_damage` | Mayor daño contra cualquier jefe | — | 1 | 0.0% | 38.2% | 47.8% | 14.0% |

> Los porcentajes son el sorteo de rareza **antes** del drop-chance propio de cada objeto.
> Cuanto más fuerte el jefe, mejor la tabla: el **Azote Carmesí** usa la mejor (`boss_verdugo`).

---

# 💎 PARTE 2 — LOOT

## Cómo funciona la probabilidad de drop

Cada muerte de un mob de la horda recorre **5 pasos**:

```
 [1] ¿suelta algo?      drop-chance de la tabla × dificultad
 [2] ¿cuántas tiradas?  rolls (1 en la horda, 4-6 en jefes)
 [3] ¿qué rareza?       peso de la rareza (+ suerte + pity)
 [4] ¿qué objeto?       peso del objeto dentro de la rareza
 [5] ¿sale?             drop-chance propio del objeto
```

### Paso 1 — ¿Suelta algo el mob?

`probabilidad real = drop-chance × (1 + (dificultad − 1) × 0.25)`

Con `mob_horde` al 50%: **x1 → 50%**, x2 → 62.5%, x3 → 75%, x10 → 100%.

### Paso 2 — ¿Cuántas tiradas?

`mob_horde` tira **1** objeto (solo la mitad de los mobs sueltan algo); los élites y los supervivientes 2-3; los jefes
4-6. Cada tirada es independiente y no repite el mismo objeto que otra tirada del mismo lote.

### Paso 3 — Sorteo de la rareza

| Rareza | Peso en `mob_horde` | % real por tirada | Aviso global |
|---|---|---|---|
| Común | 500 | **49.63%** | — |
| Poco común | 360 | **35.73%** | — |
| Raro | 110 | **10.92%** | — |
| Épico | 32 | **3.18%** | — |
| Legendario | 5 | **0.50%** | sí |
| Mítico | 0.5 | **0.05%** | sí |

**Suerte:** Raro, Épico, Legendario y Mítico multiplican su peso por `1 + (dificultad − 1) × 0.50`.
En x3 pesan el doble.

### Paso 4 — Sorteo del objeto dentro de la rareza

Todos los objetos de esa rareza compiten por peso. La probabilidad de cada uno está en
[`DROPS.md`](DROPS.md), con la columna **%% real** ya multiplicada por su drop-chance.

### Paso 5 — Drop-chance propio del objeto

Si el objeto sorteado falla su propio `drop-chance`, **esa tirada no da nada**. Los comunes caen
siempre; las armas rotas raras bajan al 5%%; los míticos al 0.5-0.8%%.

### Pity por tabla y rareza

Cada tirada fallida de una rareza suma 1 punto **en esa tabla**; el peso se multiplica por
`1 + puntos × pity-per-roll` y se reinicia al obtenerla.

| Rareza | Pity por tirada | Máx. puntos | Multiplicador máximo |
|---|---|---|---|
| Común | — | — | — |
| Poco común | — | — | — |
| Raro | — | — | — |
| Épico | +0.02 | 150 | ×4.0 |
| Legendario | +0.05 | 200 | ×11.0 |
| Mítico | +0.08 | 300 | ×25.0 |

> Antes el pity era global por rareza: farmear mobs de la horda acababa regalando míticos en los
> jefes. Ahora cada tabla mide su propia racha.

### Reglas de balance

- **Sin límite de ejemplares**: no hay tope de copias ni número de serie. Si cae, es tuyo.
- **Sin maldiciones**: al morir cae al suelo (también el equipo Mítico) y cualquiera lo recoge.
- **Un mismo lote no repite objeto** si la tabla tiene alternativas.
- **Pesos validados al cargar**: valores negativos, `NaN` o `drop-chance > 1` se avisan y corrigen.
- **Todos los objetos caen en alguna tabla**: un test lo comprueba (antes el tridente de la
  tormenta existía pero no aparecía en ninguna).
- Los **Legendario y Mítico brillan** al caer e ignoran lava y explosiones.

---

## 📦 Catálogo completo (104 objetos)

**88 piezas de equipo** (14 clases × 6 rarezas) + **16 materiales apilables**.

| Rareza | Objetos | Qué lleva |
|---|---|---|
| Común | 20 | materiales básicos y equipo de hierro |
| Poco común | 19 | materiales útiles y equipo de hierro |
| Raro | 18 | diamantes, perlas y armas con 1-2 encantamientos custom |
| Épico | 19 | tótem, netherita y piezas con 2-3 custom |
| Legendario | 14 | una pieza por clase, 3-4 custom y anuncio global |
| Mítico | 14 | una pieza por clase, 5-6 custom, brillo y anuncio global |

### Versiones por clase

| Clase | Común | Poco común | Raro | Épico | Legendario | Mítico |
|---|---|---|---|---|---|---|
| **Espada** | Espada Oxidada | Espada de Hierro Lunar | Espada Sanguinaria, Daga de las Sombras | Espada de la Ruina | Colmillo Vampírico | Hoja del Eclipse Carmesí |
| **Hacha** | Hacha Oxidada | Hacha de Hierro Lunar | Hacha Sangrienta | Hacha de la Ventisca, Hacha del Verdugo | Hacha del Abismo | Hacha del Apocalipsis |
| **Pico** | Pico Oxidado | Pico de Hierro Lunar | Pico Umbrío | Pico del Abismo | Pico Espectral | Pico del Eclipse |
| **Pala** | Pala Oxidada | Pala de Hierro Lunar | Pala Sangrienta | Pala del Abismo | Pala Espectral | Pala del Eclipse |
| **Azada** | Azada Oxidada | Azada de Hierro Lunar | Azada Sangrienta | Azada del Abismo | Azada Espectral | Guadaña del Segador |
| **Arco** | Arco Astillado | Arco de Hueso Lunar | Arco Carmesí, Arco de la Tormenta | Arco del Abismo | Arco Lunar | Arco del Eclipse |
| **Ballesta** | Ballesta Astillada | Ballesta de Hueso Lunar | Ballesta Carmesí | Ballesta del Abismo | Ballesta del Alba | Ballesta del Eclipse |
| **Tridente** | Tridente Oxidado | Tridente de los Mares | Tridente Sangriento | Tridente Profundo | Tridente de la Tormenta | Tridente del Abismo Rojo |
| **Maza** | Maza Oxidada | Maza Pesada | Maza de Hueso | Maza Carmesí | Martillo del Trueno | Maza del Coloso |
| **Casco** | Casco de Hierro Gastado | Casco de Hierro Lunar | Casco de Hierro Carmesí | Yelmo del Cazador de Sangre | Yelmo del Eclipse | Corona de la Luna de Sangre |
| **Peto** | Peto de Hierro Gastado | Peto de Hierro Lunar | Peto de Hierro Carmesí | Peto del Cazador de Sangre | Peto del Guardián Carmesí | Peto del Eclipse Carmesí |
| **Grebas** | Grebas de Hierro Gastadas | Grebas de Hierro Lunar | Grebas de Hierro Carmesí | Grebas del Cazador de Sangre | Grebas del Titán | Grebas del Eclipse |
| **Botas** | Botas de Hierro Gastadas | Botas de Hierro Lunar | Botas de Hierro Carmesí | Botas del Cazador de Sangre, Botas del Estrondo | Botas del Noctámbulo | Botas del Eclipse |
| **Escudo** | Escudo Astillado | Escudo Reforzado | Escudo Carmesí | Escudo del Guardián | Égida Carmesí | Égida del Eclipse |

### Los 14 míticos

| Objeto | Clase | Encantamientos |
|---|---|---|
| Hoja del Eclipse Carmesí | Espada | sharpness V · thunder V · looting III · paralyze III · sweeping_edge III · unbreaking III · vampire III · double_strike II · fire_aspect II · wither II · mending I |
| Hacha del Apocalipsis | Hacha | efficiency V · sharpness V · thunder V · unbreaking III · vampire III · decapitator II · double_strike II · mending I · treefeller I |
| Pico del Eclipse | Pico | efficiency V · smelter V · fortune III · tunnel III · unbreaking III · veinminer III · blast_mining II · mending I · telekinesis I |
| Pala del Eclipse | Pala | efficiency V · smelter V · fortune III · haste III · tunnel III · unbreaking III · mending I · telekinesis I |
| Guadaña del Segador | Azada | efficiency V · smelter V · haste III · unbreaking III · mending I · replanter I · telekinesis I |
| Arco del Eclipse | Arco | power V · dragonfire_arrows III · explosive_arrows III · unbreaking III · vampiric_arrows III · electrified_arrows II · punch II · sniper II · flame I · infinity I · mending I |
| Ballesta del Eclipse | Ballesta | piercing IV · dragonfire_arrows III · electrified_arrows III · explosive_arrows III · quick_charge III · unbreaking III · vampiric_arrows III · sniper II · mending I · multishot I |
| Tridente del Abismo Rojo | Tridente | impaling V · infernus III · loyalty III · restore III · unbreaking III · channeling I · mending I · soulbound I |
| Maza del Coloso | Maza | density V · breach IV · restore III · unbreaking III · wind_burst III · mending I · soulbound I |
| Corona de la Luna de Sangre | Casco | protection V · elemental_protection IV · respiration III · thorns III · unbreaking III · saturation II · mending I · night_vision I · water_breathing I |
| Peto del Eclipse Carmesí | Peto | dragon_heart V · protection V · elemental_protection IV · fire_shield IV · cold_steel III · thorns III · unbreaking III · hardened II · mending I |
| Grebas del Eclipse | Grebas | protection V · elemental_protection IV · stopping_force III · swift_sneak III · thorns III · unbreaking III · mending I |
| Botas del Eclipse | Botas | protection V · elemental_protection IV · feather_falling IV · depth_strider III · soul_speed III · unbreaking III · flame_walker II · jumping II · speed II · mending I · rebound I |
| Égida del Eclipse | Escudo | restore III · unbreaking III · mending I · soulbound I |

La lista completa con cantidades, drop-chance y probabilidad final está en
[`DROPS.md`](DROPS.md).

---

## 🌍 Idiomas

El plugin trae **7 idiomas completos** y se elige en `plugins/BloodMoonPlus/locales/locales.yml`:

| Código | Idioma |
|---|---|
| `es` | Español |
| `en` | English |
| `pt` | Português |
| `fr` | Français |
| `de` | Deutsch |
| `it` | Italiano |
| `ru` | Русский |

```yaml
Language: es            # código de la tabla
UseBundledLocales: true # claves que falten caen al inglés
```

- Los 7 archivos se copian a `plugins/BloodMoonPlus/locales/` en el primer arranque.
- `/luna reload` aplica el cambio en caliente.
- Un test verifica que los 7 idiomas tengan las mismas claves y los mismos placeholders.

---

## 🛡️ Protección de los objetos de la Luna

| Acción | ¿Permitida? |
|---|---|
| Añadir encantamientos (mesa, libros, herrería) | ✅ Sí |
| Renombrar en el yunque | ✅ Sí |
| Reparar / mejorar | ✅ Sí |
| **Quitarlos del grindstone** (desencantar) | ❌ No |
| **Perder encantamientos, rareza o dueño** | ❌ No |

- El **grindstone** anula el resultado y avisa al jugador; una pieza sin encantamientos sí se
  puede reparar.
- En **yunque** y **mesa de herrería** se conservan encantamientos, rareza, dueño e
  indestructibilidad aunque la combinación los perdiera.
- **Puesta al día automática**: al entrar, al recoger el objeto del suelo y al sacarlo de un
  cofre o shulker se aplican los encantamientos de `items.yml`, se corrigen los niveles y se
  regenera la lore (descripción, rareza y cazador). No se pierde el desgaste ni el nombre
  personalizado ni los encantamientos extra.
- **`/luna refresh <jugador|all>`** lo aplica al instante a inventario, armadura, mano secundaria
  y cofre del End. Requiere `bloodmoon.admin`.

---

## 🔧 Ver probabilidades en el servidor

```
/luna chances mob_horde
/luna chances boss_verdugo
```

Muestra la probabilidad **final** de cada objeto (rareza × peso × drop-chance), lo garantizado
y tu pity actual en esa tabla.

---

## 🔄 Actualizar de la 1.0.2 a la 1.0.3

La 1.0.3 sustituye las habilidades propias por encantamientos de ExcellentEnchants, sube a 5 el
tope vanilla en legendarios y míticos, y el catálogo pasa a **104 objetos**. El `items.yml` de tu
servidor **no se reemplaza solo**: el plugin solo lo escribe si no existe.

1. Detén el servidor y guarda una copia de `plugins/BloodMoonPlus/items.yml`.
2. Sustituye tu `items.yml` por el del `.jar` (o renómbralo y arranca una vez) y vuelve a aplicar
   tus personalizaciones.
3. Arranca **con ExcellentEnchants y NightCore** y comprueba la consola: `Items cargados: 104` y
   ningún aviso de encantamientos desconocidos o no aplicables.
4. Ejecuta `/luna refresh all` (o deja que cada jugador reconecte o recoja sus objetos).

### Cambios en `data.yml` y `loot.yml`

- Ya no se guardan contadores de ejemplares ni números de serie: las claves `created.*` que
  queden en `data.yml` se ignoran y puedes borrarlas.
- El **pity** ahora se guarda por tabla (`pity.<uuid>.<tabla>.<rareza>`). Los valores del formato
  anterior (`pity.<uuid>.<rareza>`) se ignoran: cada jugador empieza con el pity a 0.
- Los objetos repartidos con número de serie conservan su lore hasta la siguiente puesta al día,
  que quita esa línea (ya no se usa).
- `loot.yml` añade el tridente de la tormenta a la tabla de la horda y documenta el nuevo pity.

> ⚠️ No quites ExcellentEnchants con objetos repartidos: Minecraft borra los objetos cuyo
> encantamiento no existe. Primero `/luna refresh all` en un servidor donde EE haya cargado.

### Consola limpia durante el evento

```yaml
settings:
  log-named-deaths: false
```

---

## 📜 Licencia

**Todos los derechos reservados.** El código es público únicamente para lectura y auditoría de
seguridad. Consulta [`LICENSE`](LICENSE).

---

## 📤 Publicación en Modrinth

Los materiales listos para publicar están en [`modrinth/`](modrinth/). Empieza por
[`modrinth/PUBLISH.md`](modrinth/PUBLISH.md).
