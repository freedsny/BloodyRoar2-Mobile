# Bloody Roar 2 Mobile

<img src="docs/icono.png" width="420" alt="Icono de Bloody Roar 2 Mobile">

Mi versión personalizada del port nativo de **Bloody Roar II** (PlayStation) para Android.
Parte de la v1.0 de [nevertheaxolot/BloodyRoar2mobileRecomp](https://github.com/nevertheaxolot/BloodyRoar2mobileRecomp)
(que a su vez viene de [novapowers0/BloodyRoar2Recomp](https://github.com/novapowers0/BloodyRoar2Recomp))
y le agrega lo que le faltaba para jugarlo cómodo en el celular:

- **Guardar y cargar la partida** donde vas, con dos botones en pantalla.
- **Menú de Trucos**: desbloquear todo, vida infinita, bestia infinita, tiempo infinito y más.
- **Icono y nombre propios**, con un paquete aparte que se instala junto al original sin chocar.

**[⬇️ Descargar la última versión (Releases)](https://github.com/freedsny/BloodyRoar2-Mobile/releases/latest)**

> Este repo **no incluye** el juego ni ningún archivo con derechos de autor. Necesitas tu propia
> copia de Bloody Roar II (USA).

---

## Contenido

- [Novedades](#novedades)
- [Qué necesitas](#qué-necesitas)
- [Instalar y actualizar](#instalar-y-actualizar)
- [Cómo jugar](#cómo-jugar)
- [Guardar la partida](#guardar-la-partida)
- [Trucos](#trucos)
- [Problemas y soluciones](#problemas-y-soluciones)
- [Cómo está hecho](#cómo-está-hecho)
- [Sacar una versión nueva](#sacar-una-versión-nueva)
- [Créditos y licencia](#créditos-y-licencia)

---

## Novedades

### v1.0-mobile.3 — Guardar y cargar que sí funcionan
- Dos botones abajo en el centro: **GUARDAR** y **CARGAR**.
- Al tocarlos sale un mensaje: *"Partida guardada"* o *"Partida cargada"*.
- **Probado en el celular: guarda y carga bien.**
- Corrige el botón de la versión anterior, que dejaba la pantalla negra (ver
  [por qué](#por-qué-hay-un-parpadeo-negro)).
- Se instala encima de la v1.0-mobile.2 sin desinstalar.

### v1.0-mobile.2 — Trucos y primer botón de guardado
- Nuevo **menú de Trucos** en la pantalla de inicio (el botón "Mods", que decía
  "próximamente", ahora es **Trucos**), con 14 trucos.
- Primer botón GUARDAR. Abría el menú de guardado del motor, que en Android no se ve:
  quedaba la pantalla negra y no guardaba. Se arregló en la v1.0-mobile.3.
- El proceso de GitHub Actions ahora publica la Release solo, cada vez que se sube algo a `main`.

### v1.0-mobile.1 — Presentación propia
- Icono propio (zarpazos dorados sobre rojo oscuro), adaptativo para Android 8+.
- Nombre "Bloody Roar 2 Mobile" y paquete propio `io.github.freedsny.br2mobile`.

---

## Qué necesitas

| Archivo | Detalle |
|---|---|
| **Bloody Roar II (USA)** | Serial SCUS-94424, en un solo archivo `.bin` de 552,729,408 bytes, MD5 `fa5b8e192dbfef77fb2fe3416a1b9469`. **La versión europea no funciona** en este port. |
| **OpenBIOS** | `openbios.bin` (SHA-256 `fabe498f…1011c57`), el BIOS libre de PCSX-Redux con el que se compiló el port. |
| **Celular Android** | Android 8 o superior, de 64 bits (arm64). |

## Instalar y actualizar

1. Entra a [Releases](https://github.com/freedsny/BloodyRoar2-Mobile/releases/latest) y descarga el
   archivo `BloodyRoar2Mobile-v….apk`.
2. Ábrelo en el celular y permite instalar apps de origen desconocido si te lo pide.

**Actualizar:** desde la v1.0-mobile.2 todas las versiones están firmadas con la misma llave, así que
la nueva se instala **encima**, sin desinstalar, y conservas tus partidas y la memory card.
Si vienes de la v1.0-mobile.1 hay que desinstalarla una vez, porque tenía otra llave.

> ⚠️ Desinstalar la app borra la partida guardada y la memory card, porque viven dentro de la app.

## Cómo jugar

1. Abre **Bloody Roar 2 Mobile**.
2. **Select game (iso/bin)**: elige tu archivo `.bin` del juego (la primera vez lo copia y tarda un poco).
3. **Select OpenBIOS**: elige tu `openbios.bin`.
4. (Opcional) **Trucos**: marca los que quieras.
5. **Play the game**.

**Controles:**
- **Táctiles**: cruceta a la izquierda, botones △ ○ ✕ □ a la derecha, L/R/L2/R2 arriba, Select y Start abajo.
  Se pueden mover y cambiar de tamaño en *Controls Customize*, y se pueden apagar en el inicio.
- **Control físico** (Bluetooth o USB): si hay uno conectado, se usa ese y se ocultan los táctiles
  (y con ellos los botones GUARDAR/CARGAR).

## Guardar la partida

Hay dos formas de guardar, y sirven para cosas distintas:

| | Guardado rápido (botones) | Memory card (del juego) |
|---|---|---|
| **Qué guarda** | Todo, exactamente donde vas (en plena pelea, en un menú…) | Lo mismo que en la PS1: personajes desbloqueados, combos Custom, opciones |
| **Cómo** | Botón **GUARDAR** / **CARGAR** abajo en el centro | Desde el menú de opciones del propio juego |
| **Cuántas** | Una sola: guardar otra vez reemplaza la anterior | Lo que quepa en la tarjeta |

Con los botones:
- **GUARDAR** → sale *"Partida guardada"*.
- **CARGAR** → sale *"Partida cargada"* y vuelves justo a donde guardaste.
- Si tocas CARGAR sin haber guardado nunca, sale *"Todavía no hay partida guardada"* y no pasa nada.

### Por qué hay un parpadeo negro

El motor trae un menú de guardado con 12 espacios (tecla F7, o Select + R1 con control físico),
pero **en Android ese menú no se dibuja**: el juego se pausa y la pantalla queda negra. Por eso los
botones lo manejan solos: abren el menú, mandan "guardar" o "cargar" sobre el espacio 1, y el menú
se cierra. Ese abrir y cerrar es el parpadeo, y es normal.

## Trucos

Se eligen en el botón **Trucos** de la pantalla de inicio, con **Guardar**. Se aplican **al iniciar
el juego**: si cambias algo, sal y vuelve a entrar con *Play the game*.

| Grupo | Truco | Tipo |
|---|---|---|
| Desbloqueos | Todos los personajes (Gado y Shen Long) | Datos |
| | Todo el modo Custom | Datos |
| | Todas las películas | Datos |
| | Todas las imágenes | Datos |
| Combate | Vida infinita J1 · J2 · J1 y J2 | Código |
| | Bestia infinita J1 · J2 · J1 y J2 | Código |
| | Bestia llena al empezar | Código |
| | Tiempo infinito | Código |
| Otros | Una sola pelea para ganar (arcade) | Código |
| | Golpes a cualquier distancia | Código |

- Las opciones que chocan entre sí se desmarcan solas (por ejemplo, vida infinita J1 y J2).
- **Quitar todos** apaga todos los trucos de una vez.
- Los de **datos** son los más seguros. Los de **código** cambian instrucciones del juego (ver
  [cómo funcionan](#trucos-1)); si alguno no hace efecto, avísame cuál.

> 💡 **Desbloquear para siempre:** activa los desbloqueos, entra al juego y **guarda desde el menú del
> juego en la memory card**. Así quedan guardados aunque después apagues los trucos.

Los códigos son los GameShark de Bloody Roar II (USA) de la
[base de datos de trucos de libretro](https://github.com/libretro/libretro-database).

## Problemas y soluciones

| Problema | Solución |
|---|---|
| Con trucos activos el juego no arranca | Entra a **Trucos** → **Quitar todos** y vuelve a jugar. |
| Un truco no hace efecto | Recuerda que se aplican al iniciar: sal y vuelve a entrar. Si sigue igual, repórtalo. |
| GUARDAR dice *"No se pudo confirmar el guardado"* | El motor no logró escribir la partida; repórtalo. |
| La pantalla quedó negra y no vuelve | Se abrió el menú de guardado del motor (por ejemplo, con Select + R1 en un control físico). Ciérralo con Select + R1 otra vez. |
| Con control físico no salen GUARDAR/CARGAR | Los botones van con los controles táctiles. Desconecta el control para guardar o cargar. |
| Android no deja instalar encima | Pasa si vienes de la v1.0-mobile.1 (otra llave): desinstálala una vez. |
| "Al salir pierdo mi progreso" | Al cerrar la app el juego arranca desde el principio, como una consola. Usa **GUARDAR** antes de salir y **CARGAR** al volver. |

---

## Cómo está hecho

El motor del juego (`libpsx_android.so`, el port recompilado) **no se toca**. El proceso de GitHub
Actions descarga el APK v1.0 original, verifica su SHA-256 y le aplica el parche de este repo:

```text
APK v1.0 original ──► apktool d (con código)
                         │
   build_extras.sh ──────┼──► libbr2cheats.so  (plugin de trucos, C)
                         │    classes.dex      (botones y menú, Java)
                         ▼
                    apply_patch.sh  (icono, nombre, paquete, enganches)
                         ▼
                    apktool b  +  classes4.dex  ──►  firmar  ──►  Release
```

### Carpetas

| Carpeta / archivo | Qué hay |
|---|---|
| `icon/` | `make_icon.py` genera el icono en SVG; `svg/` son las capas listas |
| `patch/apply_patch.sh` | Pone icono, nombre y paquete, y engancha los botones y los trucos en el APK descompilado |
| `patch/build_extras.sh` | Compila el plugin (clang, sin NDK) y el código Java (javac + d8) |
| `patch/java/` | `SaveStateButton` (botones GUARDAR/CARGAR) y `Br2Cheats` (menú de trucos) |
| `patch/java-stubs/` | `SDLActivity` falso, solo para compilar (no se empaqueta) |
| `patch/cheats/br2cheats.c` | Plugin nativo de trucos → `libbr2cheats.so` |
| `patch/cheats/manifest.toml` | Paquete de mods `br2mobile.cheats` con un *feature* por truco |
| `.github/workflows/build.yml` | Compila, parchea, firma y publica la Release |

### Botones GUARDAR / CARGAR

`SaveStateButton` agrega los dos botones encima de los controles táctiles. Como el menú de *save
states* del motor no se dibuja en Android, los botones mandan teclas al motor a través de SDL:

- **GUARDAR**: F7 (abre el menú) → 300 ms → **S** (guarda el espacio 1 y cierra).
  Luego busca el archivo `files/<bios>/state_XXXXXXXX_slot00.pst` y confirma cuando cambia.
- **CARGAR**: si ese archivo existe, F7 → 300 ms → **Enter** (carga el espacio 1 y cierra).
  Si no existe no hace nada, porque el motor dejaría el menú abierto (pantalla negra).

### Trucos

El motor (psxrecomp) tiene un sistema de mods que lee `files/mods/` al arrancar, también en
Android, y llama a los *plugins* activos en cada VBlank del juego, en su propio hilo de emulación.

1. `Br2Cheats.install()` corre en `MainActivity.onCreate`, antes de que arranque el motor. Copia
   `manifest.toml` a `files/mods/bundled/br2mobile.cheats/1.0.0/` y escribe `files/mods/state.toml`
   con los trucos marcados. Si algo falla, borra `state.toml` para que el juego arranque sin trucos.
2. `libbr2cheats.so` se carga junto al motor: `getLibraries()` queda en
   `SDL2, br2cheats, psx_android`, porque SDL busca `SDL_main` en la **última** librería. Al cargarse,
   registra un plugin por truco con `psx_mod_register_vblank_plugin()`.
3. En cada VBlank, cada truco activo aplica sus códigos GameShark (tipo `80`, 16 bits):
   - **Datos** (tabla de desbloqueos `0x801C0Fxx`): se reescriben en cada VBlank, como un GameShark,
     porque el juego limpia la tabla al entrar a los menús.
   - **Código** (dentro del ejecutable, `0x8011E800–0x801BF000`): este motor recompila el juego a
     código nativo, así que escribir la RAM no basta. Se usa `psx_mod_write_code_word()`, que manda
     esa instrucción al intérprete del motor. Solo se escribe si cambió (en la práctica, una vez).

El paquete y el `state.toml` se probaron con el lector de mods del propio motor (mismo commit del
APK), y el plugin se probó con un motor simulado.

### Firma

Sin los secretos `KEYSTORE_BASE64` / `KEYSTORE_PASSWORD`, el APK se firma con la llave de depuración
que trae uber-apk-signer, que es **siempre la misma**; por eso las versiones se instalan una encima
de otra. Configurar una llave propia más adelante obligaría a desinstalar una vez.

## Sacar una versión nueva

1. Haz los cambios.
2. Sube `versionCode` y `versionName` en `patch/apply_patch.sh` (las líneas `sed … apktool.yml` del final).
3. Sube a `main`.

GitHub Actions compila y publica la Release `v<versionName>` (por ejemplo `v1.0-mobile.4`). Si la
versión no cambió, reemplaza el APK de esa misma Release. También funciona crear una etiqueta `v*`.

Para compilar en tu propia máquina hace falta Java 21, `clang` + `lld`, `rsvg-convert`, el Android SDK
(`android.jar` y `d8`) y apktool; los pasos son los mismos de `.github/workflows/build.yml`.

## Créditos y licencia

- Port recompilado: [novapowers0/BloodyRoar2Recomp](https://github.com/novapowers0/BloodyRoar2Recomp) y
  [nevertheaxolot/BloodyRoar2mobileRecomp](https://github.com/nevertheaxolot/BloodyRoar2mobileRecomp),
  sobre el motor psxrecomp. Licencia MIT (ver `LICENSE-upstream`).
- Códigos de trucos: [libretro-database](https://github.com/libretro/libretro-database).
- Icono, botones, menú de trucos y scripts de este repo: Freed.
