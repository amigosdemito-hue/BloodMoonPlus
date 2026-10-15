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
`adventure`, `mobs`, `game-mechanics`, `equipment`, `magic`, `utility`

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

## 4) Publicar por API (automatizado)

Hay un script listo: `modrinth/upload.ps1`.

```powershell
# 1) Crea un token en https://modrinth.com/settings/personal-access-tokens
#    con el scope "Create versions" (y "Read projects").
# 2) Expórtalo en la sesión:
$env:MODRINTH_TOKEN = "mrp_XXXXXXXXXXXXXXXXXXXXXXXXXXXX"

# 3) Ejecuta (ajusta la ruta del jar si hace falta):
cd "C:\Users\david garcia\Desktop\BloodMoonPlus\BloodMoonPlus"
.\modrinth\upload.ps1 -ProjectSlug "bloodmoon-plus" -VersionNumber "1.0.0" -GameVersions "26.2","26.3"
```

> La primera vez necesitas crear el proyecto (los datos de la sección 1) por la
> web; el script solo sirve para **subir versiones** a un proyecto existente.

---

## 5) Enlaces del proyecto (rellenar tras crear el repo)

- **Source code**: `https://github.com/amigosdemito-hue/bloodmoonplus`
- **Issues**: `https://github.com/amigosdemito-hue/bloodmoonplus/issues`
- **Wiki / Docs**: el `README.md` del repositorio

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
