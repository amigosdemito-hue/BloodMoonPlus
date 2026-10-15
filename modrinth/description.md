# 🩸 BloodMoonPlus — Luna de Sangre

Un plugin de **Luna de Sangre** para servidores Paper, con **horarios fijos en hora de México**, **9 jefes**, **45 items** repartidos por rareza, encantamientos hasta **nivel 7**, **oleadas progresivas**, **placeholders**, soporte multi-idioma y **inmersión total** (niebla roja, rayos, sonidos ambientales y anuncios constantes).

Optimizado para rendir bien en servidores de **40+ jugadores** sin cambiar la jugabilidad.

---

## ✨ Lo más destacado

- **🗓️ Horarios fijos** — modo `weekly` (por día de la semana a una hora real, zona horaria configurable, por defecto `America/Mexico_City`) o `interval` (cada N horas). Se pueden poner **varias funciones el mismo día**.
- **👹 9 jefes** con barra de vida propia, fases de **enrage**, dificultad mínima, habilidades y loot propio.
- **🎁 45 items** organizados por rareza: comunes (siempre caen), raros, épicos, legendarios y **míticos** (extremadamente raros, ~0.1%).
- **⚔️ Encantamientos personalizados** hasta **nivel 7**, con habilidades propias (crítico, guardia de trueno, onda expansiva, levitación, sangrado…).
- **🌊 Oleadas progresivas** — 1.ª oleada 5, 2.ª 10, 3.ª 15… hasta un tope configurable.
- **🌫️ Inmersión** — niebla roja, rayos al aparecer hordas/jefes, sonidos ambientales y anuncios periódicos. Sin lluvia, pero con truenos.
- **📊 Estadísticas y supervivientes** — quién sobrevivió, quién más daño hizo, ranking, premios al MVP.
- **🌍 Multi-idioma (i18n)** y **configuración por mundo**.
- **🔌 Integración opcional** con PlaceholderAPI, Multiverse-Core y WorldGuard (MythicMobs como modo alternativo).
- **🚫 Control del Wither** — el Wither normal está bloqueado; solo puede aparecer el jefe **Azote Carmesí**.

---

## 📥 Instalación

1. Descarga el `.jar` de la pestaña **Versions**.
2. Colócalo en la carpeta `plugins/` de tu servidor **Paper**.
3. Reinicia el servidor.
4. (Opcional) Instala **PlaceholderAPI** para los placeholders.
5. Configura `plugins/BloodMoonPlus/config.yml` y ejecuta `/luna reload`.

> Requiere **Paper**. Compatible con Paper **26.2** y **26.3**.

---

## 🎮 Comandos

Comando principal: **`/luna`** (alias: `/bm`, `/lunadesangre`, `/bloodmoon`).

| Comando | Qué hace |
|---|---|
| `/luna next` | Muestra la próxima Luna de Sangre |
| `/luna start [dificultad]` | Inicia el evento manualmente |
| `/luna stop` | Detiene el evento |
| `/luna difficulty <n>` | Ajusta la dificultad del evento actual |
| `/luna boss <id>` | Invoca un jefe concreto |
| `/luna horde` | Fuerza una oleada |
| `/luna killmobs` | Elimina los mobs del evento |
| `/luna give <item> [cantidad]` | Entrega un item del plugin |
| `/luna items` | Lista los items disponibles |
| `/luna chances` | Muestra las probabilidades de drop |
| `/luna reload` | Recarga la configuración |

### Permisos

| Permiso | Descripción | Por defecto |
|---|---|---|
| `bloodmoon.use` | Ver estado y próximos eventos | Todos |
| `bloodmoon.admin` | Administrar el evento | OP |

---

## 🗓️ Horarios y dificultad

En modo `weekly` cada entrada acepta una hora simple o un bloque con dificultad y duración:

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
      - time: "20:40"        # dos funciones el mismo día
        difficulty: 3
        duration-minutes: 25
```

La **dificultad** es un multiplicador continuo (0.1 – 10.0). Escala vida, daño, velocidad, tamaño de oleada, ritmo de aparición, probabilidad de loot y suerte de rareza.

---

## 🩸 Jefes

| Jefe | Dificultad mínima |
|---|---|
| Rey Zombi Sangriento | 1.0 |
| Centinela de Hueso | 1.0 |
| Arquera Carmesí | 1.0 |
| Cazador de Sombras | 1.0 |
| Nigromante Carmesí | 1.5 |
| Devastador Ancestral | 1.5 |
| Warden Abismal | 2.0 |
| Azote Carmesí (Wither) | 2.5 |

Cada jefe tiene barra de vida, fases de **enrage** y su propia tabla de loot. Consulta el **README** completo del proyecto para el detalle de cada drop.

---

## 🧩 Placeholders

Con **PlaceholderAPI** instalado (namespace `%bloodmoon_*%`): estado del evento, tiempo restante, próxima luna, dificultad, jefes vivos, oleada actual, supervivientes, estadísticas del jugador y más.

---

## 🚫 Datapack del Wither (opcional)

El plugin ya bloquea el Wither normal por sí solo. Si además quieres una capa de datos, el proyecto incluye el datapack **BloodMoonWitherControl** (en `datapacks/`), que elimina cualquier Wither que **no** sea el jefe de la Luna de Sangre. Es opcional.

---

## ⚡ Rendimiento

Pensado para redes de **~40 jugadores**: escaneos de entidades locales (no de mundo completo), caché de parseo de items, snapshots de placeholders optimizados y menús/action-bars eficientes. Para servidores muy grandes puedes ajustar `horde.wave-max-size` y `horde.max-alive-per-player` en la config.

---

## 🛠️ Compilar desde el código

```bash
git clone <tu-repositorio>
cd BloodMoonPlus
./gradlew build
```

El `.jar` queda en `build/libs/`. Compila con **Java 25**.

---

## 📜 Licencia

**Todos los derechos reservados.** El código es público únicamente para lectura y auditoría de seguridad; **no** está permitido copiarlo, modificarlo, reutilizarlo ni redistribuirlo. Puedes descargar y ejecutar el plugin en tu servidor. Ver el archivo `LICENSE`.

---

## ❤️ Créditos

Inspirado y construido para superar a *BloodMoon-Reborn*. Hecho con cariño para comunidades hispanohablantes. 🌙🩸
