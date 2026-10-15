# Changelog — BloodMoonPlus

El formato sigue [Keep a Changelog](https://keepachangelog.com/es/1.1.0/).

<!--
Nota: ajusta el número de versión de la sección de abajo para que coincida con
la versión que publiques en Modrinth. Las etiquetas marcadas con ⭐ son las
funciones más llamativas para la página del proyecto.
-->

## [1.0.0] — Primera versión pública

Primera publicación oficial. Un plugin de Luna de Sangre completo que mejora y
supera a *BloodMoon-Reborn 1.1.1*.

### Añadido ⭐
- **Horarios fijos** en hora de México con dos modos: `weekly` (día de la semana + hora real) y `interval` (cada N horas). Varias funciones el mismo día.
- **9 jefes** con barra de vida, dificultad mínima, fases de *enrage*, habilidades y loot propio.
- **45 items** por rareza: comunes (drop garantizado), raros, épicos, legendarios y míticos (~0.1%) ⭐.
- **Encantamientos personalizados** hasta nivel **7** y habilidades propias: crítico, guardia de trueno, onda expansiva, levitación y sangrado ⭐.
- **Oleadas progresivas**: 5 → 10 → 15 → … con tope configurable ⭐.
- **Inmersión**: niebla roja, rayos al aparecer hordas/jefes/muerte de jugador, sonidos ambientales y anuncios constantes. Sin lluvia, con truenos ⭐.
- **12 tipos de mobs hostiles** y hordas progresivas.
- **Estadísticas y supervivientes**: ranking, MVP por daño, quién sobrevivió.
- **Placeholders** (`%bloodmoon_*%`): estado, tiempo restante, próxima luna, dificultad, jefes, oleada, estadísticas… vía PlaceholderAPI.
- **Internacionalización (i18n)** y **configuración por mundo**.
- **Control del Wither**: bloqueo del Wither normal a nivel de plugin + **datapack opcional** (`BloodMoonWitherControl`) que elimina cualquier Wither que no sea el jefe.
- **Protección del terreno** por el evento, con excepción de los jefes.

### Cambiado
- Dificultad como **multiplicador continuo** (0.1 – 10.0, por defecto 1.0) con 7 factores de escalado.
- Los items "rotos" raros/legendarios/míticos bajan su probabilidad al **5%**.
- Los legendarios incluyen **`vanishing_curse`**.

### Corregido
- **Error masivo en `PlayerDeathEvent`**: los mensajes de muerte de vanilla (con códigos `§`) rompían MiniMessage. Ahora se convierten de *legacy* a *MiniMessage* con un respaldo que nunca falla.
- **Hordas que se detenían** tras morir un jugador: se separa el estado "muerto ahora" de "murió alguna vez", y las hordas se reanudan al reaparecer.
- **Compatibilidad con dificultad pacífico** (ya no crashea).

### Rendimiento
- Eliminados escaneos de mundo completo y parseos por golpe.
- Escaneos de entidades locales y caché de parseo de items.
- Snapshots de placeholders optimizados y action-bars más eficientes. Pensado para **40+ jugadores**.

### Compatibilidad
- Paper **26.2** y **26.3**. Compilado con Java 25.
