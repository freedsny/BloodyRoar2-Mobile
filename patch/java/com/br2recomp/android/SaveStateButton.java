package com.br2recomp.android;

import android.app.Activity;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.libsdl.app.SDLActivity;

import java.io.File;

/**
 * Botones táctiles "GUARDAR" y "CARGAR" (guardado rápido, ranura 1).
 *
 * El motor solo ofrece un menú de save states (tecla F7) que en Android no se
 * dibuja: la pantalla queda negra. Así que estos botones lo manejan "a
 * ciegas": abren el menú con F7 y, un instante después, mandan la tecla que
 * guarda (S) o carga (Enter) la ranura seleccionada (la 1). El motor cierra el
 * menú solo después de guardar o cargar.
 *
 * Para confirmar, se busca el archivo de la ranura 1
 * (files/<bios>/state_XXXXXXXX_slot00.pst) y se avisa con un mensaje.
 */
public final class SaveStateButton {
    private static final String TAG = "BR2SaveState";

    /** Tiempo para que el motor entre al menú antes de mandar la segunda tecla. */
    private static final long MENU_DELAY_MS = 300;
    /** Cuánto esperar a que aparezca el archivo guardado. */
    private static final long SAVE_TIMEOUT_MS = 6000;
    private static final long POLL_MS = 250;

    private static final int GOLD = 0xFFF6C94A;
    private static final int BG = 0xB03A080B;

    private static boolean busy;

    private SaveStateButton() {}

    /** Llamado por MainActivity.br2AddTouchOverlay() después del control táctil. */
    public static void attach(final Activity activity) {
        float d = activity.getResources().getDisplayMetrics().density;

        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);

        TextView save = pill(activity, "GUARDAR", d);
        TextView load = pill(activity, "CARGAR", d);
        LinearLayout.LayoutParams gap = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        gap.leftMargin = (int) (12 * d);
        row.addView(save);
        row.addView(load, gap);

        save.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { quickSave(activity); }
        });
        load.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { quickLoad(activity); }
        });

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        lp.bottomMargin = (int) (10 * d);
        activity.addContentView(row, lp);
    }

    private static TextView pill(Activity a, String text, float d) {
        TextView t = new TextView(a);
        t.setText(text);
        t.setTextColor(GOLD);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        int h = (int) (14 * d), v = (int) (6 * d);
        t.setPadding(h, v, h, v);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(BG);
        bg.setCornerRadius(20 * d);
        bg.setStroke((int) (1.5f * d), GOLD);
        t.setBackground(bg);
        t.setAlpha(0.85f);
        return t;
    }

    private static void press(int keycode) {
        SDLActivity.onNativeKeyDown(keycode);
        SDLActivity.onNativeKeyUp(keycode);
    }

    private static void toast(Activity a, String msg) {
        Toast.makeText(a, msg, Toast.LENGTH_SHORT).show();
    }

    static void quickSave(final Activity a) {
        if (busy) return;
        busy = true;
        final File before = findSlot1(a);
        final long beforeTime = before != null ? before.lastModified() : 0;
        final long beforeSize = before != null ? before.length() : -1;
        final Handler h = new Handler(Looper.getMainLooper());
        press(KeyEvent.KEYCODE_F7);              // abre el menú (pausa)
        h.postDelayed(new Runnable() {
            @Override public void run() {
                press(KeyEvent.KEYCODE_S);        // guarda la ranura 1 y cierra
                final long start = SystemClock.uptimeMillis();
                h.postDelayed(new Runnable() {
                    @Override public void run() {
                        File f = findSlot1(a);
                        boolean changed = f != null
                                && (f.lastModified() != beforeTime || f.length() != beforeSize);
                        if (changed) {
                            busy = false;
                            toast(a, "Partida guardada");
                        } else if (SystemClock.uptimeMillis() - start < SAVE_TIMEOUT_MS) {
                            h.postDelayed(this, POLL_MS);
                        } else {
                            busy = false;
                            Log.w(TAG, "no apareció el archivo de la ranura 1");
                            toast(a, "No se pudo confirmar el guardado");
                        }
                    }
                }, POLL_MS);
            }
        }, MENU_DELAY_MS);
    }

    static void quickLoad(final Activity a) {
        if (busy) return;
        if (findSlot1(a) == null) {
            // Sin archivo el motor dejaría el menú abierto (pantalla negra).
            toast(a, "Todavía no hay partida guardada");
            return;
        }
        busy = true;
        final Handler h = new Handler(Looper.getMainLooper());
        press(KeyEvent.KEYCODE_F7);              // abre el menú (pausa)
        h.postDelayed(new Runnable() {
            @Override public void run() {
                press(KeyEvent.KEYCODE_ENTER);    // carga la ranura 1 y cierra
                busy = false;
                toast(a, "Partida cargada");
            }
        }, MENU_DELAY_MS);
    }

    /** Archivo de la ranura 1: files/<bios>/state_XXXXXXXX_slot00.pst */
    static File findSlot1(Activity a) {
        File root = a.getFilesDir();
        File best = pick(root, null);
        File[] dirs = root.listFiles();
        if (dirs != null) {
            for (File dir : dirs) {
                if (dir.isDirectory() && !dir.getName().equals("netplay")
                        && !dir.getName().equals("mods")) {
                    best = pick(dir, best);
                }
            }
        }
        return best;
    }

    private static File pick(File dir, File best) {
        File[] files = dir.listFiles();
        if (files == null) return best;
        for (File f : files) {
            String n = f.getName();
            if (f.isFile() && n.startsWith("state_") && n.endsWith("_slot00.pst")
                    && (best == null || f.lastModified() > best.lastModified())) {
                best = f;
            }
        }
        return best;
    }
}
