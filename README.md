# Zentones

Tonos de llamada y de mensaje, generales y **por app**, con tienda, editor y 4 interfaces completas:
**Pixel** (Material You expresivo), **HyperOS**, **One UI** y **Sony** (sobria). Al abrirla por primera vez se aplica sola la interfaz de tu marca (Xiaomi/Redmi/POCO → HyperOS, Samsung → One UI, Sony → Sony, resto → Pixel); se cambia en Ajustes. Las barras y menús son de cristal con desenfoque real (Android 12+).

## Qué hace

| Pestaña | Funciones |
|---|---|
| **Tonos** | Tono de llamada, notificaciones y alarma del sistema · 18 tonos de serie sintetizados al estilo de los clásicos de iPhone (Bambú, Ding, Tri-tono, Reflejo, Apertura…) · Mis tonos · Crear tono desde cualquier audio |
| **Tienda** | Catálogo propio (140 tonos originales en 10 categorías: Retro 8-bit, Terror nocturno, Radio antigua, Sci-fi, Lo-fi, Marimba, Electrónica, Zen, Gaming, Alertas) + búsqueda en **Freesound** (cientos de miles de sonidos CC). Al descargar se abre el editor |
| **Apps** | Eliges un tono para cada app (WhatsApp, Telegram, Instagram…) y una guía paso a paso te lleva a donde se selecciona |
| **Ajustes** | Estilo de interfaz, claro/oscuro, permisos, API key de Freesound, actualizaciones desde GitHub |

**Editor**: forma de onda con tiradores, fundido de entrada/salida, normalizar, ganancia ±12 dB, escuchar el recorte y guardar como llamada o mensaje. También se abre con *Compartir → Zentones* desde cualquier app de música o archivos.

### Cómo funcionan los tonos por app
Zentones no se queda escuchando ni suena por encima de otras apps. Eliges la app y el tono, Zentones copia el tono a las carpetas de *Tonos* o *Notificaciones* del móvil y te guía paso a paso para seleccionarlo:
- **WhatsApp y Telegram** eligen el tono en sus propios ajustes: la guía te dice dónde está y abre la app.
- **El resto de apps** usan las categorías de notificación de Android: la guía abre directamente los ajustes de notificaciones de esa app y solo tienes que tocar *Sonido* y elegir «Zentones · …».

## Publicar y actualizar (como Compass Launcher)
Todo lo hace el workflow `build` al subir a `main`:
1. Compila `Zentones.apk`, firmado siempre con `zentones.jks` (va en el repo, así cada APK se instala encima del anterior).
2. Si la versión de `gradle.properties` (`zentones.version`) es nueva, crea la release `vX.Y.Z` con las notas de `docs/changelog.json`. Las apps instaladas la detectan al abrirse y se actualizan.
3. Publica la web (`docs/`) en GitHub Pages con el catálogo de la tienda.

Para sacar una versión: sube `zentones.version`, añade sus notas a `docs/changelog.json` y haz push.

Opcional: el secreto `FREESOUND_KEY` mete tu API key de Freesound en la app para que la búsqueda funcione sin configurar nada.

## Web
La web (`docs/`) se publica sola en GitHub Pages (si la primera vez falla, actívalo en *Settings → Pages → Source: GitHub Actions*). Queda en `https://<usuario>.github.io/<repo>/` y muestra:
- botón y QR de descarga del último APK (`releases/latest/download/Zentones.apk`),
- las novedades de cada versión, sacadas de las Releases,
- la tienda con todos los tonos del catálogo, para escucharlos.

Se vuelve a publicar con cada push a `main`.

## Ampliar la tienda
El catálogo vive en `catalog/` de tu repo y la app lo lee de `raw.githubusercontent.com/<usuario>/<repo>/main/catalog/catalog.json`. **Basta con hacer push para que aparezcan tonos nuevos, sin publicar versión.**

- Generar más tonos originales: `python3 tools/generate_catalog.py --per 30` (necesita numpy, scipy y ffmpeg).
- Añadir tus propios audios: mete el `.ogg`/`.mp3` en `catalog/tones/` y añade una entrada en `catalog.json`:
  ```json
  {"id": "mio-01", "name": "Mi tono", "category": "gaming", "kind": "call",
   "file": "tones/mio-01.ogg", "duration": 6.5, "author": "Yo", "license": "CC0"}
  ```
  Sube solo audio que sea tuyo o tenga licencia que lo permita. Los rips de juegos (FNAF, Portal…) y de YouTube tienen copyright.

## Compilar en local
Android Studio (Ladybug o superior) → abrir la carpeta → Run. O `./gradlew assembleDebug`.

Requisitos: Android 10+ (minSdk 29). Stack: Kotlin 2.1, Jetpack Compose (BOM 2025.05), Material 3, [Haze](https://github.com/chrisbanes/haze) para el cristal.
