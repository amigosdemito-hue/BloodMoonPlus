# 📤 Guía de publicación en Modrinth — BloodMoonPlus

Todo lo necesario para publicar el plugin ya está preparado. Sigue esta lista.

> ⚠️ **Modrinth exige que el contenido del proyecto esté en INGLÉS.** Por eso
> `summary.txt`, `description.md` y `CHANGELOG.md` ya están en inglés. Los
> campos de esta guía (que solo es para ti) siguen en español.
> Si quieres, dentro de la descripción puedes añadir una sección en español al
> final, pero la descripción principal debe ser en inglés.

---

## 0) Antes de publicar

- [ ] Compilar el `.jar` final: `.\gradlew.bat build --no-daemon`
      → queda en `build\libs\BloodMoonPlus-<versión>.jar`
- [ ] Probar el `.jar` en un servidor de pruebas.
- [ ] Versión de esta publicación: **1.0.0** (ya alineada en `build.gradle.kts`,
      `plugin.yml` vía `${version}` y `LocaleManager.VERSION`).
- [ ] Confirmar que la **licencia** es "All Rights Reserved" (ver `LICENSE`).

---

## 1) Datos del proyecto (pestaña "Project")

| Campo | Valor sugerido |
|---|---|
| **Name** | `BloodMoonPlus` |
| **Slug** | `bloodmoon-plus` (si está tomado: `luna-de-sangre-plus`) |
| **Summary** | (copiar de `modrinth/summary.txt`) |
| **Description** | Pegar el contenido de `modrinth/description.md` |
| **Type** | `Plugin` |
| **Icon** | `modrinth/icon.png` (256×256) |
| **License** | `All-Rights-Reserved` |
| **Environment** | **Server** (no cliente) |

### Categorías sugeridas
**Máximo 3** destacadas: `adventure`, `mobs`, `game-mechanics`
Adicionales (`additional_categories`): `equipment`, `magic`, `utility`

### Etiquetas (tags) manuales (en inglés)
`blood moon`, `bloodmoon`, `bosses`, `loot`, `waves`, `hordes`, `schedules`,
`placeholders`, `events`, `wither`, `hardcore`, `pve`

---

## 2) Versión (pestaña "Versions → Create version")

| Campo | Valor |
|---|---|
| **Version number** | `1.0.0` |
| **Name** | `1.0.0 — First public release` |
| **Release channel** | `Release` |
| **Changelog** | Pegar `modrinth/CHANGELOG.md` (la sección de esta versión) |
| **Loader(s)** | `Paper` (opcional: `Purpur`) |
| **Game version(s)** | La(s) versión(es) de Minecraft correspondientes a Paper **26.2 / 26.3** |
| **File** | `BloodMoonPlus-<versión>.jar` |
| **Dependencies** | Ninguna obligatoria. Opcional: `PlaceholderAPI` (Required=false) |

> ⚠️ Verifica en la lista de Modrinth qué versión de Minecraft corresponde a tu
> Paper 26.2/26.3 y marca solo esas. Si no aparece, marca la más cercana y
> anótalo en el changelog.

---

## 3) Publicar por la web (rápido)

1. Entra a https://modrinth.com/dashboard/projects → **Create a project**.
2. Rellena los campos de la sección 1.
3. Sube `modrinth/icon.png` como icono.
4. Crea la versión de la sección 2 y sube el `.jar`.
5. Revisa y publica.

---

## 4) Publicar por API (automatizado, todo de una vez)

Hay un script que **crea el proyecto y sube la versión** de una sola pasada:
`modrinth/publish.ps1`.

```powershell
# 1) Crea un token en https://modrinth.com/settings/personal-access-tokens
#    con los scopes: PROJECT_CREATE, PROJECT_READ, PROJECT_WRITE,
#                     VERSION_CREATE y VERSION_READ.
# 2) Guarda el token en modrinth\token.txt  (una sola línea; está en .gitignore)
#    ... o expórtalo en la sesión:  $env:MODRINTH_TOKEN = "mrp_xxx"
# 3) Ejecuta (primero en seco para ver qué hará):
cd "C:\Users\david garcia\Desktop\BloodMoonPlus\BloodMoonPlus"
.\modrinth\publish.ps1 -DryRun
.\modrinth\publish.ps1
```

El script:
- Crea el proyecto `bloodmoon-plus` (tipo mod/Plugin, entorno Server, icono,
  resumen y descripción) si aún no existe.
- Sube `BloodMoonPlus-1.0.0.jar` como versión **1.0.0** (Paper, MC 26.2/26.3).
- Deja la licencia como **All-Rights-Reserved** y luego **envía el proyecto a
  revisión** (`status = processing`).

### Detalles reales de la API (aprendidos, importantes)

- **Licencia ARR**: se envía `license_id = "LicenseRef-All-Rights-Reserved"`
  (no está en `/v2/tag/license`, pero es lo que usa Modrinth para "All Rights
  Reserved").
- **`categories` admite máximo 3**; las extra van en `additional_categories`.
- El proyecto se crea como **borrador** (`is_draft = true`) y después se sube la
  versión y se envía a revisión con `PATCH { "status": "processing" }`
  (enviar `requested_status` **no** sirve: ya viene en `approved` por defecto).
- `project_id` en la versión debe ser el **ID base62** (p. ej. `ilXTRql2`), no el
  slug. `upload.ps1` lo resuelve solo.
- La API exige enviar `client_side`, `server_side`, `initial_versions` y
  `license_id` aunque los documentos los marquen como opcionales/deprecados.

> Para **subir solo otra versión** a un proyecto ya existente usa
> `modrinth\upload.ps1` (mismo token, mismos scopes), y para reenviar a revisión:
> `.\modrinth\publish.ps1 -SkipProject -SkipVersion -Submit`.

---

## 5) Enlaces del proyecto (ya incluidos por el script)

- **Source code**: `https://github.com/amigosdemito-hue/BloodMoonPlus`
- **Issues**: `https://github.com/amigosdemito-hue/BloodMoonPlus/issues`
- **Wiki / Docs**: el `README.md` / `README.en.md` del repositorio

---

## 6) Checklist final

- [ ] Icono 256×256 subido
- [ ] Descripción con formato correcto (markdown)
- [ ] Licencia "All Rights Reserved" seleccionada
- [ ] Loader = Paper, Environment = Server
- [ ] Versiones de Minecraft correctas
- [ ] `.jar` adjunto y probado
- [ ] Changelog pegado
- [ ] Enlaces de código fuente añadidos
