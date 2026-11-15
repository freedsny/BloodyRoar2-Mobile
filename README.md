# Bloody Roar 2 Mobile

<img src="docs/icono.png" width="420" alt="Icono de Bloody Roar 2 Mobile">

Mi versión personalizada del port nativo de **Bloody Roar II** para Android.
Parte de la v1.0 de [nevertheaxolot/BloodyRoar2mobileRecomp](https://github.com/nevertheaxolot/BloodyRoar2mobileRecomp)
(que a su vez viene de [novapowers0/BloodyRoar2Recomp](https://github.com/novapowers0/BloodyRoar2Recomp))
y le cambia la presentación:

- **Icono propio** (zarpazos dorados sobre rojo oscuro), adaptativo para Android 8+.
- **Nombre:** "Bloody Roar 2 Mobile".
- **Paquete propio** (`io.github.freedsny.br2mobile`): se instala junto a la versión original sin chocar.
- **Botones GUARDAR y CARGAR** (abajo al centro, con los controles táctiles): guardan y cargan
  la partida exactamente donde vas (guardado rápido).
- **Menú de Trucos** (botón "Trucos" del launcher): desbloqueos, vida y bestia infinitas, tiempo infinito y más.

El motor del juego (`libpsx_android.so`) queda **idéntico** a la v1.0; el parche cambia el icono,
el nombre y el identificador, y agrega los botones GUARDAR/CARGAR y los trucos.

## Guardar la partida

El juego guarda en la memory card solo lo que guardaba en la PS1 (personajes desbloqueados,
combos, opciones), y hay que guardarlo desde el menú del juego. Para seguir **donde lo dejaste**
usa los botones **GUARDAR** y **CARGAR** (abajo al centro). Un mensaje confirma
"Partida guardada" o "Partida cargada". Se usa una sola ranura: guardar otra vez la reemplaza.

El menú de *save states* del motor (tecla F7, o Select + R1 con control físico) no se dibuja en
Android, por eso los botones lo manejan solos: abren el menú y mandan guardar o cargar.
La partida guardada y la memory card viven dentro de la app: si la desinstalas se borran.

## Trucos

Se eligen en el botón **Trucos** del launcher y se aplican al iniciar el juego.
Son los códigos GameShark de Bloody Roar II (USA) de la base de datos de libretro, aplicados con
el sistema de mods del motor (`patch/cheats/`):

- Todos los personajes (Gado y Shen Long), todo el modo Custom, todas las películas, todas las imágenes
- Vida infinita J1 / J2 / ambos
- Bestia infinita J1 / J2 / ambos, bestia llena al empezar
- Tiempo infinito, una sola pelea para ganar (arcade), golpes a cualquier distancia

Truco: activa los desbloqueos, entra al juego y guarda desde su menú en la memory card;
así quedan guardados aunque después apagues los trucos.

## Qué necesitas para jugar

- La imagen del juego **Bloody Roar II (USA)**, serial SCUS-94424, en un solo archivo `.bin`
  (552,729,408 bytes, MD5 `fa5b8e192dbfef77fb2fe3416a1b9469`). La versión europea no funciona en este port.
- El **OpenBIOS** (`openbios.bin`, SHA-256 `fabe498f…1011c57`), el BIOS libre de PCSX-Redux con el que se compiló el port.

Este repo **no incluye** el juego ni ningún archivo con derechos de autor.

## Cómo está hecho

| Carpeta | Qué hay |
|---|---|
| `icon/` | `make_icon.py` genera el icono en SVG; `svg/` son las capas listas |
| `patch/apply_patch.sh` | Pone icono, nombre, paquete, botones GUARDAR/CARGAR y trucos en el APK descompilado |
| `patch/java/` | Botones GUARDAR/CARGAR y menú de Trucos (Java → `classes4.dex`) |
| `patch/java-stubs/` | `SDLActivity` falso solo para compilar (no se empaqueta) |
| `patch/cheats/` | Plugin de trucos (`br2cheats.c` → `libbr2cheats.so`) y su paquete de mods |
| `patch/build_extras.sh` | Compila el plugin (clang, sin NDK) y el código Java (javac + d8) |
| `.github/workflows/build.yml` | Descarga la v1.0 original (verifica su SHA-256), compila los extras, aplica el parche, firma y publica |

Para sacar una versión nueva: sube `versionName`/`versionCode` en `patch/apply_patch.sh` y sube a `main`.
El APK aparece en *Releases* como `v<versión>` (si la versión no cambió, se reemplaza el APK de esa Release).

## Licencia

El proyecto original es MIT (ver `LICENSE-upstream`). El icono y los scripts de este repo son de Freed.
