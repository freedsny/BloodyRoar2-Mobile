# Bloody Roar 2 Mobile

<img src="docs/icono.png" width="420" alt="Icono de Bloody Roar 2 Mobile">

Mi versión personalizada del port nativo de **Bloody Roar II** para Android.
Parte de la v1.0 de [nevertheaxolot/BloodyRoar2mobileRecomp](https://github.com/nevertheaxolot/BloodyRoar2mobileRecomp)
(que a su vez viene de [novapowers0/BloodyRoar2Recomp](https://github.com/novapowers0/BloodyRoar2Recomp))
y le cambia la presentación:

- **Icono propio** (zarpazos dorados sobre rojo oscuro), adaptativo para Android 8+.
- **Nombre:** "Bloody Roar 2 Mobile".
- **Paquete propio** (`io.github.freedsny.br2mobile`): se instala junto a la versión original sin chocar.
- **Botón GUARDAR** (abajo al centro, con los controles táctiles): abre el menú de *save states*
  del motor, 12 ranuras para guardar la partida exactamente donde vas.
- **Menú de Trucos** (botón "Trucos" del launcher): desbloqueos, vida y bestia infinitas, tiempo infinito y más.

El motor del juego (`libpsx_android.so`) queda **idéntico** a la v1.0; el parche cambia el icono,
el nombre y el identificador, y agrega el botón GUARDAR y los trucos.

## Guardar la partida

El juego guarda en la memory card solo lo que guardaba en la PS1 (personajes desbloqueados,
combos, opciones), y hay que guardarlo desde el menú del juego. Para seguir **donde lo dejaste**
usa el botón **GUARDAR**:

| En el menú de save states | Botón |
|---|---|
| Elegir ranura (1–12) | Cruceta |
| Guardar en la ranura | ○ (círculo) |
| Cargar la ranura | START |
| Cerrar el menú | GUARDAR otra vez |

Con control físico el mismo menú se abre con **Select + R1**.
Las ranuras y la memory card viven dentro de la app: si la desinstalas se borran.

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
| `patch/apply_patch.sh` | Pone icono, nombre, paquete, botón GUARDAR y trucos en el APK descompilado |
| `patch/smali/` | Botón GUARDAR (envía la tecla F7 al motor) |
| `patch/java/` | Menú de Trucos del launcher (Java → `classes4.dex`) |
| `patch/cheats/` | Plugin de trucos (`br2cheats.c` → `libbr2cheats.so`) y su paquete de mods |
| `patch/build_extras.sh` | Compila el plugin (clang, sin NDK) y el menú (javac + d8) |
| `.github/workflows/build.yml` | Descarga la v1.0 original (verifica su SHA-256), compila los extras, aplica el parche, firma y publica |

Para sacar una versión nueva: cambia lo que quieras, sube a `main` y crea una etiqueta `v1.0-mobile.N`.
El APK aparece en *Releases*.

## Licencia

El proyecto original es MIT (ver `LICENSE-upstream`). El icono y los scripts de este repo son de Freed.
