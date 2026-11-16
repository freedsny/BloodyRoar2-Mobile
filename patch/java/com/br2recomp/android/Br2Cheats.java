package com.br2recomp.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Menú "Trucos y mejoras" de Bloody Roar 2 Mobile (trucos GameShark y los
 * mods oficiales de pantalla ancha, turbo y saltar la intro).
 *
 * - {@link #openMenu(Activity)} lo abre el botón "Trucos" del launcher y guarda
 *   lo elegido en SharedPreferences.
 * - {@link #install(Context)} se llama al arrancar el juego (MainActivity.onCreate,
 *   antes de que SDL arranque el motor). Copia el paquete de mods
 *   br2mobile.cheats a files/mods/bundled/ y escribe files/mods/state.toml con
 *   los trucos activos. El motor lee eso al iniciar y llama a
 *   libbr2cheats.so en cada VBlank para cada truco activo.
 */
public final class Br2Cheats {
    private static final String TAG = "BR2Cheats";
    private static final String PREFS = "br2mobile_cheats";
    private static final String PKG = "br2mobile.cheats";
    private static final String VERSION = "1.0.0";
    private static final String ASSET_MANIFEST = "br2mobile_cheats/manifest.toml";

    /** Ids de feature; deben coincidir con manifest.toml y br2cheats.c. */
    static final String[] IDS = {
        "ancho-16-9", "ancho-completo", "turbo-75", "turbo-120", "saltar-intro",
        "personajes", "custom", "peliculas", "imagenes",
        "vida-j1", "vida-j2", "vida-ambos",
        "bestia-j1", "bestia-j2", "bestia-ambos", "bestia-llena",
        "tiempo", "una-pelea", "distancia",
    };

    static final String[] LABELS = {
        "Pantalla ancha 16:9",
        "Pantalla ancha completa (se ajusta al celular)",
        "Turbo x1.25 (más rápido)",
        "Turbo x2 (el doble de rápido)",
        "Saltar videos de la intro",
        "Todos los personajes (Gado y Shen Long)",
        "Todo el modo Custom",
        "Todas las películas",
        "Todas las imágenes",
        "Vida infinita J1",
        "Vida infinita J2",
        "Vida infinita J1 y J2",
        "Bestia infinita J1",
        "Bestia infinita J2",
        "Bestia infinita J1 y J2",
        "Bestia llena al empezar",
        "Tiempo infinito",
        "Una sola pelea para ganar (arcade)",
        "Golpes a cualquier distancia",
    };

    /** Trucos del mismo grupo (distinto de 0) se excluyen entre sí. */
    private static final int[] GROUP = {
        3, 3, 4, 4, 0,
        0, 0, 0, 0,
        1, 1, 1,
        2, 2, 2, 0,
        0, 0, 0,
    };

    private Br2Cheats() {}

    static boolean[] load(Context c) {
        SharedPreferences p = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        boolean[] on = new boolean[IDS.length];
        for (int i = 0; i < IDS.length; i++) on[i] = p.getBoolean(IDS[i], false);
        return on;
    }

    static int save(Context c, boolean[] on) {
        SharedPreferences.Editor e = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit();
        int count = 0;
        for (int i = 0; i < IDS.length; i++) {
            e.putBoolean(IDS[i], on[i]);
            if (on[i]) count++;
        }
        e.apply();
        return count;
    }

    /** Abre el menú de trucos (desde el launcher). */
    public static void openMenu(final Activity activity) {
        openMenu(activity, null, null);
    }

    /**
     * Abre el menú de trucos.
     * @param onSaved  si no es null, se llama después de "Guardar" o "Quitar todos"
     *                 cuando algo cambió (en el juego: para ofrecer reiniciar).
     * @param onClosed si no es null, se llama cuando el menú se cierra sin cambios.
     */
    public static void openMenu(final Activity activity, final Runnable onSaved,
                                final Runnable onClosed) {
        final boolean[] on = load(activity);
        final boolean[] before = on.clone();
        final boolean[] handled = {false};
        AlertDialog.Builder b = new AlertDialog.Builder(
                activity, android.R.style.Theme_DeviceDefault_Dialog_Alert);
        b.setTitle("Trucos y mejoras");
        b.setMultiChoiceItems(LABELS, on, new DialogInterface.OnMultiChoiceClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which, boolean checked) {
                on[which] = checked;
                if (!checked || GROUP[which] == 0) return;
                ListView list = ((AlertDialog) dialog).getListView();
                for (int j = 0; j < IDS.length; j++) {
                    if (j != which && GROUP[j] == GROUP[which] && on[j]) {
                        on[j] = false;
                        list.setItemChecked(j, false);
                    }
                }
            }
        });
        b.setPositiveButton("Guardar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                int n = save(activity, on);
                boolean changed = !java.util.Arrays.equals(before, on);
                if (onSaved != null && changed) {
                    handled[0] = true;
                    onSaved.run();
                    return;
                }
                Toast.makeText(activity,
                        n == 0 ? "Trucos desactivados"
                               : n + (n == 1 ? " opción activa" : " opciones activas")
                                 + ". Se aplican al iniciar el juego.",
                        Toast.LENGTH_LONG).show();
            }
        });
        b.setNeutralButton("Quitar todos", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                boolean[] none = new boolean[IDS.length];
                save(activity, none);
                if (onSaved != null && !java.util.Arrays.equals(before, none)) {
                    handled[0] = true;
                    onSaved.run();
                    return;
                }
                Toast.makeText(activity, "Trucos desactivados", Toast.LENGTH_SHORT).show();
            }
        });
        b.setNegativeButton("Cancelar", null);
        AlertDialog d = b.create();
        d.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                if (!handled[0] && onClosed != null) onClosed.run();
            }
        });
        d.show();
    }

    /**
     * Instala el paquete de mods y escribe el estado de los trucos. Se llama
     * antes de arrancar el motor. Si algo falla, borra state.toml para que el
     * juego arranque sin trucos en vez de no arrancar.
     */
    public static void install(Context c) {
        File mods = new File(c.getFilesDir(), "mods");
        File state = new File(mods, "state.toml");
        try {
            File pkgDir = new File(mods, "bundled/" + PKG + "/" + VERSION);
            if (!pkgDir.isDirectory() && !pkgDir.mkdirs())
                throw new IllegalStateException("no se pudo crear " + pkgDir);

            byte[] manifest = readAsset(c, ASSET_MANIFEST);
            writeFile(new File(pkgDir, "manifest.toml"), manifest);

            boolean[] on = load(c);
            StringBuilder sb = new StringBuilder();
            sb.append("format_version = 2\n\n[[package]]\nid = \"").append(PKG)
              .append("\"\nversion = \"").append(VERSION).append("\"\n");
            int active = 0;
            for (int i = 0; i < IDS.length; i++) {
                sb.append("\n[[feature]]\npackage_id = \"").append(PKG)
                  .append("\"\nid = \"").append(IDS[i])
                  .append("\"\nenabled = ").append(on[i] ? "true" : "false").append('\n');
                if (on[i]) active++;
            }
            writeFile(state, sb.toString().getBytes(StandardCharsets.UTF_8));
            Log.i(TAG, "trucos activos: " + active);
        } catch (Throwable t) {
            Log.e(TAG, "no se pudieron preparar los trucos; se arranca sin ellos", t);
            //noinspection ResultOfMethodCallIgnored
            state.delete();
        }
    }

    private static byte[] readAsset(Context c, String name) throws Exception {
        InputStream in = c.getAssets().open(name);
        try {
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            return out.toByteArray();
        } finally {
            in.close();
        }
    }

    /** Escribe a un .tmp y luego renombra, para no dejar archivos a medias. */
    private static void writeFile(File f, byte[] data) throws Exception {
        File tmp = new File(f.getPath() + ".tmp");
        OutputStream out = new FileOutputStream(tmp);
        try {
            out.write(data);
        } finally {
            out.close();
        }
        if (!tmp.renameTo(f))
            throw new IllegalStateException("no se pudo escribir " + f);
    }
}
