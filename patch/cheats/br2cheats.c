/*
 * Trucos de Bloody Roar 2 Mobile (Bloody Roar II USA, SCUS-94424).
 *
 * Plugin para el sistema de mods del motor psxrecomp. Cada truco es un
 * "plugin de VBlank" con su propio id; el paquete de mods br2mobile.cheats
 * (manifest.toml) lo activa cuando el jugador lo marca en el menú "Trucos".
 * El motor llama la función del truco en cada VBlank del juego, en su propio
 * hilo de emulación.
 *
 * Los códigos son los GameShark de la base de datos de libretro
 * (cht/Sony - PlayStation/Bloody Roar II (USA) (GameShark).cht). Solo se usan
 * códigos tipo 80 (escribir 16 bits).
 *
 *  - Direcciones de datos (tabla de desbloqueos 0x801C0Fxx): se reescriben en
 *    cada VBlank, como un GameShark, porque el juego vuelve a leer/limpiar la
 *    tabla al entrar a los menús.
 *  - Direcciones dentro del código del juego (0x8011E800-0x801BF000): este
 *    motor recompila el código a nativo, así que escribir la RAM no basta. Se
 *    usa psx_mod_write_code_word(), que además manda esa instrucción al
 *    intérprete de RAM modificada del motor. Solo se escribe si el valor
 *    cambió, así que en la práctica se aplica una sola vez.
 *
 * Se compila sin libc (-nostdlib): solo usa funciones de libpsx_android.so.
 */

typedef unsigned int u32;
typedef unsigned short u16;

/* API de plugins del motor (runtime/include/mod_plugins.h). */
extern int psx_mod_register_vblank_plugin(const char *id, void (*callback)(void));
extern int psx_mod_game_started(void);
extern u16 psx_mod_read_half(u32 address);
extern void psx_mod_write_half(u32 address, u16 value);
extern u32 psx_mod_read_word(u32 address);
extern void psx_mod_write_code_word(u32 address, u32 value);

#define CODE_START 0x8011E800u /* load_address del ejecutable */
#define CODE_END   0x801BF000u /* load_address + text_size   */

typedef struct {
    u32 addr;
    u16 value;
} GsLine;

static int is_code(u32 addr) {
    return addr >= CODE_START && addr < CODE_END;
}

static void apply(const GsLine *lines, int count) {
    int i;
    if (!psx_mod_game_started())
        return;
    for (i = 0; i < count; i++) {
        u32 addr = lines[i].addr;
        u16 value = lines[i].value;
        if (is_code(addr)) {
            u32 word_addr = addr & ~3u;
            u32 old_word = psx_mod_read_word(word_addr);
            u32 new_word = (addr & 2u)
                ? ((old_word & 0x0000FFFFu) | ((u32)value << 16))
                : ((old_word & 0xFFFF0000u) | (u32)value);
            if (new_word != old_word)
                psx_mod_write_code_word(word_addr, new_word);
        } else if (psx_mod_read_half(addr) != value) {
            psx_mod_write_half(addr, value);
        }
    }
}

#define N(a) ((int)(sizeof(a) / sizeof((a)[0])))
#define CHEAT(name, ...)                                         \
    static const GsLine k_##name[] = { __VA_ARGS__ };            \
    static void cb_##name(void) { apply(k_##name, N(k_##name)); }

/* --- Desbloqueos (datos) ------------------------------------------------- */
CHEAT(personajes, { 0x801C0FF4u, 0xFFFF })
CHEAT(custom,     { 0x801C0FECu, 0xFFFF }, { 0x801C0FEEu, 0xFFFF })
CHEAT(peliculas,  { 0x801C0FD0u, 0xFFFF })
CHEAT(imagenes,   { 0x801C0FFCu, 0xFFFF }, { 0x801C0FFEu, 0xFFFF },
                  { 0x801C1000u, 0xFFFF }, { 0x801C1002u, 0xFFFF },
                  { 0x801C1004u, 0xFFFF })

/* --- Vida y bestia (datos de cada luchador) ------------------------------
 * Los GameShark de vida y bestia cambian instrucciones del juego, y en este
 * motor no hacían efecto (probado en el celular: la barra de bestia no se
 * quedaba llena). Por eso se reescriben los datos del luchador en cada
 * VBlank. Las direcciones salen del ejecutable SCUS_944.24:
 *
 *  - Los dos luchadores viven en el scratchpad de la PS1 (0x1F800000):
 *    J1 en 0x1F8000E8 y J2 en 0x1F8001F0 (0x108 bytes cada uno). La función
 *    del fotograma (0x801664A4) llama a 0x80178584 con esas dos direcciones.
 *  - +0xAC vida actual, +0xAE vida recuperable, +0xB0 vida máxima.
 *  - +0xD6 barra de bestia visible: 0..0x3BFF (0x1E00 = la mitad, que es con
 *    lo que se empieza cada round; -1 = vacía). +0xD8 = lo que se le va a
 *    sumar o restar en el siguiente fotograma.
 *
 * Como el scratchpad lo usan otras partes del juego (menús, videos), solo se
 * escribe si los datos parecen de un luchador en pelea. */
#define FIGHTER_P1 0x1F8000E8u
#define FIGHTER_P2 0x1F8001F0u
#define F_MODEL    0x38u
#define F_HP       0xACu
#define F_HP_BLUE  0xAEu
#define F_HP_MAX   0xB0u
#define F_BEAST    0xD6u
#define F_BEAST_IN 0xD8u
#define BEAST_FULL  0x3BFF
#define BEAST_START 0x1E00
#define BEAST_START_FULL 0x3A00 /* lo mismo que el GameShark original */

static short sh16(u32 addr) { return (short)psx_mod_read_half(addr); }

static int fighter_ok(u32 f, int index) {
    u32 model;
    short hp, hp_max, beast;
    if (!psx_mod_game_started()) return 0;
    (void)index;
    model = psx_mod_read_word(f + F_MODEL);
    if (model < 0x80010000u || model >= 0x80200000u) return 0;
    hp_max = sh16(f + F_HP_MAX);
    hp = sh16(f + F_HP);
    beast = sh16(f + F_BEAST);
    if (hp_max <= 0 || hp > hp_max) return 0;
    if (beast < -1 || beast > BEAST_FULL) return 0;
    return 1;
}

static void set_half(u32 addr, short value) {
    if (sh16(addr) != value) psx_mod_write_half(addr, (u16)value);
}

static void full_health(u32 f, int index) {
    short hp_max;
    if (!fighter_ok(f, index)) return;
    hp_max = sh16(f + F_HP_MAX);
    set_half(f + F_HP, hp_max);
    set_half(f + F_HP_BLUE, hp_max);
}

static void full_beast(u32 f, int index) {
    if (!fighter_ok(f, index)) return;
    set_half(f + F_BEAST_IN, 0);
    set_half(f + F_BEAST, BEAST_FULL);
}

/* Al empezar cada round el juego pone la barra en la mitad (0x1E00) con la
 * vida llena: en ese momento se sube a casi llena. */
static void start_beast(u32 f, int index) {
    if (!fighter_ok(f, index)) return;
    if (sh16(f + F_BEAST) == BEAST_START && sh16(f + F_BEAST_IN) == 0
            && sh16(f + F_HP) == sh16(f + F_HP_MAX))
        psx_mod_write_half(f + F_BEAST, BEAST_START_FULL);
}

static void cb_vida_j1(void)       { full_health(FIGHTER_P1, 0); }
static void cb_vida_j2(void)       { full_health(FIGHTER_P2, 1); }
static void cb_vida_ambos(void)    { cb_vida_j1(); cb_vida_j2(); }
static void cb_bestia_j1(void)     { full_beast(FIGHTER_P1, 0); }
static void cb_bestia_j2(void)     { full_beast(FIGHTER_P2, 1); }
static void cb_bestia_ambos(void)  { cb_bestia_j1(); cb_bestia_j2(); }
static void cb_bestia_llena(void)  { start_beast(FIGHTER_P1, 0); start_beast(FIGHTER_P2, 1); }

/* --- Otros (código) ------------------------------------------------------- */
CHEAT(tiempo,     { 0x80151356u, 0x2400 })
CHEAT(una_pelea,  { 0x801529ECu, 0x0007 }, { 0x801529EEu, 0x2402 })
CHEAT(distancia,  { 0x80145BCEu, 0x1000 })

/* --- Mejoras (mods oficiales de NovaPowers para Bloody Roar II) ----------
 * Plugins de activación: el motor los llama una sola vez, antes de crear la
 * ventana y el renderizador. Copian lo que hacen br2_widescreen_plugin.c,
 * br2_turbo_plugin.c y br2_enhancement_plugin.c del proyecto de PC
 * (novapowers0/BloodyRoar2Recomp), que no venían compilados en el APK. */
extern int psx_mod_register_activation_plugin(const char *id, void (*callback)(void));
extern int psx_mod_set_fixed_display_aspect(u32 numerator, u32 denominator);
extern int psx_mod_set_adaptive_display_aspect(u32 max_numerator, u32 max_denominator);
extern int psx_mod_set_native_vblank_rate(u32 frames_per_second);
extern int psx_mod_set_auto_skip_fmv(int enabled);

/* Pantalla ancha: el motor "aplasta" la geometría 3D (GTE X-squash) y estira
 * la imagen, así que en las peleas se ve más mundo a los lados. Los menús y
 * videos 2D se quedan en 4:3. Los cortes de pantalla del juego ya vienen
 * ajustados en game_us.toml [widescreen]. */
static void act_ancho_16_9(void) {
    (void)psx_mod_set_fixed_display_aspect(16u, 9u);
}
static void act_ancho_completo(void) {
    /* Sigue el tamaño real de la pantalla, de 4:3 hasta 21:9. */
    (void)psx_mod_set_fixed_display_aspect(16u, 9u);
    (void)psx_mod_set_adaptive_display_aspect(21u, 9u);
}

/* Turbo: el juego corre a más cuadros por segundo (todo va más rápido). */
static void act_turbo_75(void)  { (void)psx_mod_set_native_vblank_rate(75u); }
static void act_turbo_120(void) { (void)psx_mod_set_native_vblank_rate(120u); }

/* Saltar los videos de la intro (el motor presiona START por ti). */
static void act_saltar_intro(void) { (void)psx_mod_set_auto_skip_fmv(1); }

/* Los ids deben coincidir con los [[plugin]] de manifest.toml. */
__attribute__((constructor))
static void br2cheats_register(void) {
    psx_mod_register_activation_plugin("br2mobile.cheats.ancho-16-9",     act_ancho_16_9);
    psx_mod_register_activation_plugin("br2mobile.cheats.ancho-completo", act_ancho_completo);
    psx_mod_register_activation_plugin("br2mobile.cheats.turbo-75",       act_turbo_75);
    psx_mod_register_activation_plugin("br2mobile.cheats.turbo-120",      act_turbo_120);
    psx_mod_register_activation_plugin("br2mobile.cheats.saltar-intro",   act_saltar_intro);

    psx_mod_register_vblank_plugin("br2mobile.cheats.personajes",   cb_personajes);
    psx_mod_register_vblank_plugin("br2mobile.cheats.custom",       cb_custom);
    psx_mod_register_vblank_plugin("br2mobile.cheats.peliculas",    cb_peliculas);
    psx_mod_register_vblank_plugin("br2mobile.cheats.imagenes",     cb_imagenes);
    psx_mod_register_vblank_plugin("br2mobile.cheats.vida-j1",      cb_vida_j1);
    psx_mod_register_vblank_plugin("br2mobile.cheats.vida-j2",      cb_vida_j2);
    psx_mod_register_vblank_plugin("br2mobile.cheats.vida-ambos",   cb_vida_ambos);
    psx_mod_register_vblank_plugin("br2mobile.cheats.bestia-j1",    cb_bestia_j1);
    psx_mod_register_vblank_plugin("br2mobile.cheats.bestia-j2",    cb_bestia_j2);
    psx_mod_register_vblank_plugin("br2mobile.cheats.bestia-ambos", cb_bestia_ambos);
    psx_mod_register_vblank_plugin("br2mobile.cheats.bestia-llena", cb_bestia_llena);
    psx_mod_register_vblank_plugin("br2mobile.cheats.tiempo",       cb_tiempo);
    psx_mod_register_vblank_plugin("br2mobile.cheats.una-pelea",    cb_una_pelea);
    psx_mod_register_vblank_plugin("br2mobile.cheats.distancia",    cb_distancia);
}
