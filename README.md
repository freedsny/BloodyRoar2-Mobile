# Bloody Roar 2 Mobile

<img src="docs/icono.png" width="420" alt="Icono de Bloody Roar 2 Mobile">

Mi versión personalizada del port nativo de **Bloody Roar II** para Android.
Parte de la v1.0 de [nevertheaxolot/BloodyRoar2mobileRecomp](https://github.com/nevertheaxolot/BloodyRoar2mobileRecomp)
(que a su vez viene de [novapowers0/BloodyRoar2Recomp](https://github.com/novapowers0/BloodyRoar2Recomp))
y le cambia la presentación:

- **Icono propio** (zarpazos dorados sobre rojo oscuro), adaptativo para Android 8+.
- **Nombre:** "Bloody Roar 2 Mobile".
- **Paquete propio** (`io.github.freedsny.br2mobile`): se instala junto a la versión original sin chocar.

El motor del juego, el código Java y los archivos internos quedan **idénticos** a la v1.0;
el parche solo toca el icono, el nombre y el identificador de la app.

## Qué necesitas para jugar

- La imagen del juego **Bloody Roar II (USA)**, serial SCUS-94424, en un solo archivo `.bin`
  (552,729,408 bytes, MD5 `fa5b8e192dbfef77fb2fe3416a1b9469`). La versión europea no funciona en este port.
- El **OpenBIOS** (`openbios.bin`, SHA-256 `fabe498f…1011c57`), el BIOS libre de PCSX-Redux con el que se compiló el port.

Este repo **no incluye** el juego ni ningún archivo con derechos de autor.

## Cómo está hecho

| Carpeta | Qué hay |
|---|---|
| `icon/` | `make_icon.py` genera el icono en SVG; `svg/` son las capas listas |
| `patch/apply_patch.sh` | Pone icono, nombre y paquete en el APK descompilado |
| `.github/workflows/build.yml` | Descarga la v1.0 original (verifica su SHA-256), aplica el parche, firma y publica |

Para sacar una versión nueva: cambia lo que quieras, sube a `main` y crea una etiqueta `v1.0-mobile.N`.
El APK aparece en *Releases*.

## Licencia

El proyecto original es MIT (ver `LICENSE-upstream`). El icono y los scripts de este repo son de Freed.
