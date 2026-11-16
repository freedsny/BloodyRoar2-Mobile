package com.br2recomp.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.input.InputManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.util.Log;
import android.view.Gravity;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Lo que la app pone encima del juego. Reemplaza a
 * MainActivity.br2AddTouchOverlay() de la v1.0 (apply_patch.sh cambia ese
 * método por una llamada a {@link #attach}).
 *
 *  - Controles táctiles estilo PlayStation ({@link TouchControlsView}), si el
 *    jugador 1 usa la pantalla (no hay mando conectado al iniciar).
 *  - Botones GUARDAR, CARGAR y MENÚ abajo al centro, siempre (también con mando).
 *  - MENÚ: pausa el juego y ofrece Trucos y mejoras, mostrar/ocultar los
 *    controles táctiles, reiniciar el juego y volver al inicio.
 *  - Si se conecta o desconecta un mando durante el juego, ofrece reiniciar
 *    (el motor elige mando o pantalla para el jugador 1 solo al arrancar).
 */
public final class Br2GameUi {
    private static final String TAG = "BR2GameUi";
    private static final String PREFS = "br2recomp_prefs";
    private static final String PREF_TOUCH = "touch_controls";
    /** Espacio de guardado rápido que usan GUARDAR/CARGAR (0 = el primero). */
    private static final String LOAD_SLOT_ENV = "PSX_LOAD_SLOT=0";

    private static TouchControlsView touchView;
    private static boolean padAtStart;
    private static boolean paused;
    private static boolean dialogOpen;

    private Br2GameUi() {}

    /** Llamado por MainActivity en lugar de br2AddTouchOverlay(). */
    public static void attach(Activity a, boolean useOverlay, int alphaPercent) {
        try {
            padAtStart = hasGamepad();
            if (useOverlay) addTouchView(a, alphaPercent);
            addButtons(a);
            watchGamepads(a);
        } catch (Throwable t) {
            Log.e(TAG, "no se pudo preparar la interfaz del juego", t);
        }
    }

    // ------------------------------------------------------------ vistas

    private static void addTouchView(Activity a, int alphaPercent) {
        TouchControlsView v = new TouchControlsView(a);
        v.setAlphaPercent(alphaPercent);
        v.loadLayout(a.getSharedPreferences(PREFS, Context.MODE_PRIVATE));
        a.addContentView(v, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        touchView = v;
    }

    private static void addButtons(final Activity a) {
        float d = a.getResources().getDisplayMetrics().density;
        LinearLayout row = new LinearLayout(a);
        row.setOrientation(LinearLayout.HORIZONTAL);

        TextView save = SaveStateButton.pill(a, "GUARDAR", d);
        TextView load = SaveStateButton.pill(a, "CARGAR", d);
        TextView menu = SaveStateButton.pill(a, "MENÚ", d);
        row.addView(save);
        row.addView(load, gap(d));
        row.addView(menu, gap(d));

        save.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { SaveStateButton.quickSave(a); }
        });
        load.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { SaveStateButton.quickLoad(a); }
        });
        menu.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { openMenu(a); }
        });

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        lp.bottomMargin = (int) (10 * d);
        a.addContentView(row, lp);
    }

    private static LinearLayout.LayoutParams gap(float d) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        p.leftMargin = (int) (10 * d);
        return p;
    }

    // ------------------------------------------------------------- pausa

    /**
     * Pausa el juego abriendo el menú de save states del motor (F7), que en
     * Android no se dibuja: la pantalla queda negra detrás del diálogo.
     */
    private static void pause() {
        if (paused) return;
        SaveStateButton.press(KeyEvent.KEYCODE_F7);
        paused = true;
    }

    private static void resume() {
        if (!paused) return;
        SaveStateButton.press(KeyEvent.KEYCODE_F7);
        paused = false;
    }

    // -------------------------------------------------------------- menú

    private static void openMenu(final Activity a) {
        if (dialogOpen) return;
        dialogOpen = true;
        pause();

        final boolean touchAvailable = !padAtStart;
        final boolean touchVisible = touchView != null && touchView.getVisibility() == View.VISIBLE;
        final String[] items;
        if (touchAvailable) {
            items = new String[] {
                "Trucos y mejoras",
                touchVisible ? "Ocultar controles táctiles" : "Mostrar controles táctiles",
                "Reiniciar el juego",
                "Volver al inicio",
                "Continuar",
            };
        } else {
            items = new String[] {
                "Trucos y mejoras",
                "Reiniciar el juego",
                "Volver al inicio",
                "Continuar",
            };
        }

        final boolean[] keepPaused = {false};
        AlertDialog d = new AlertDialog.Builder(a, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle(padAtStart ? "Juego en pausa · usando mando" : "Juego en pausa")
                .setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String item = items[which];
                        if (item.startsWith("Trucos")) {
                            keepPaused[0] = true;
                            openCheats(a);
                        } else if (item.startsWith("Ocultar")) {
                            setTouchVisible(a, false);
                        } else if (item.startsWith("Mostrar")) {
                            setTouchVisible(a, true);
                        } else if (item.startsWith("Reiniciar")) {
                            keepPaused[0] = true;
                            askRestart(a, "Reiniciar el juego",
                                    "El juego vuelve a empezar desde el inicio.");
                        } else if (item.startsWith("Volver")) {
                            keepPaused[0] = true;
                            askLauncher(a);
                        }
                    }
                })
                .create();
        d.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                // Si se abrió otro diálogo (trucos, reiniciar…), ese sigue a cargo.
                if (!keepPaused[0]) {
                    dialogOpen = false;
                    resume();
                }
            }
        });
        d.show();
    }

    private static void openCheats(final Activity a) {
        dialogOpen = true;
        Br2Cheats.openMenu(a, new Runnable() {
            @Override public void run() {
                askRestart(a, "Aplicar trucos y mejoras",
                        "Los cambios se aplican al reiniciar el juego.");
            }
        }, new Runnable() {
            @Override public void run() {
                dialogOpen = false;
                resume();
            }
        });
    }

    private static void setTouchVisible(Activity a, boolean visible) {
        if (visible && touchView == null) {
            SharedPreferences p = a.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            addTouchView(a, p.getInt("touch_alpha", 20) + 20);
        }
        if (touchView != null) {
            if (!visible) touchView.releaseAllKeys();
            touchView.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
        // Lo recuerda también para la próxima vez (el mismo ajuste del inicio).
        a.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putBoolean(PREF_TOUCH, visible).apply();
        Toast.makeText(a, visible ? "Controles táctiles visibles" : "Controles táctiles ocultos",
                Toast.LENGTH_SHORT).show();
    }

    /**
     * Pregunta antes de reiniciar. "Guardar y reiniciar" guarda en el espacio
     * rápido y arranca el juego cargándolo (PSX_LOAD_SLOT), así se sigue en
     * el mismo punto con los cambios aplicados.
     */
    private static void askRestart(final Activity a, String title, String message) {
        dialogOpen = true;
        final boolean[] chosen = {false};
        AlertDialog d = new AlertDialog.Builder(a, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle(title)
                .setMessage(message + "\n\n\"Guardar y reiniciar\" guarda tu partida y vuelve justo a donde ibas.")
                .setPositiveButton("Guardar y reiniciar", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface dialog, int which) {
                        chosen[0] = true;
                        saveThenRestart(a);
                    }
                })
                .setNeutralButton("Reiniciar sin guardar", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface dialog, int which) {
                        chosen[0] = true;
                        restartGame(a, false);
                    }
                })
                .setNegativeButton("Ahora no", null)
                .create();
        d.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override public void onDismiss(DialogInterface dialog) {
                dialogOpen = false;
                if (!chosen[0]) resume();
            }
        });
        d.show();
    }

    private static void saveThenRestart(final Activity a) {
        // GUARDAR maneja el menú del motor por su cuenta: primero se quita la pausa.
        resume();
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override public void run() {
                SaveStateButton.quickSave(a, new Runnable() {
                    @Override public void run() { restartGame(a, true); }
                });
            }
        }, 400);
    }

    private static void askLauncher(final Activity a) {
        dialogOpen = true;
        final boolean[] chosen = {false};
        AlertDialog d = new AlertDialog.Builder(a, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("Volver al inicio")
                .setMessage("Se cierra el juego y vuelves a la pantalla de inicio de la app. "
                        + "Lo que no hayas guardado con GUARDAR se pierde.")
                .setPositiveButton("Volver al inicio", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface dialog, int which) {
                        chosen[0] = true;
                        Intent i = new Intent();
                        i.setClassName(a, "com.br2recomp.android.SelectGameActivity");
                        relaunch(a, i);
                    }
                })
                .setNegativeButton("Cancelar", null)
                .create();
        d.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override public void onDismiss(DialogInterface dialog) {
                dialogOpen = false;
                if (!chosen[0]) resume();
            }
        });
        d.show();
    }

    // --------------------------------------------------------- reiniciar

    /** Vuelve a abrir el juego con los mismos datos (juego, BIOS, ajustes). */
    static void restartGame(Activity a, boolean loadQuickSave) {
        Intent i = new Intent(a.getIntent());
        i.setClass(a, a.getClass());
        SharedPreferences p = a.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        i.putExtra("TOUCH_CONTROLS", p.getBoolean(PREF_TOUCH, true));

        // Quita un PSX_LOAD_SLOT anterior y lo agrega si toca cargar.
        String env = i.getStringExtra("ENV_TEXT");
        StringBuilder sb = new StringBuilder();
        if (env != null) {
            for (String line : env.split("\\r?\\n")) {
                if (!line.trim().startsWith("PSX_LOAD_SLOT")) sb.append(line).append('\n');
            }
        }
        if (loadQuickSave && SaveStateButton.findSlot1(a) != null) sb.append(LOAD_SLOT_ENV).append('\n');
        i.putExtra("ENV_TEXT", sb.toString());
        relaunch(a, i);
    }

    /**
     * El motor no se puede reiniciar dentro del mismo proceso: se pide la
     * pantalla nueva y se cierra el proceso; Android la abre en uno nuevo.
     */
    private static void relaunch(Activity a, Intent i) {
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        a.startActivity(i);
        Process.killProcess(Process.myPid());
    }

    // ------------------------------------------------------------- mandos

    static boolean hasGamepad() {
        try {
            for (int id : InputDevice.getDeviceIds()) {
                if (isGamepad(InputDevice.getDevice(id))) return true;
            }
        } catch (Throwable t) {
            Log.e(TAG, "detección de mando falló", t);
        }
        return false;
    }

    /** Mismo criterio que MainActivity.br2HasGamepad() de la v1.0. */
    private static boolean isGamepad(InputDevice d) {
        if (d == null || d.isVirtual()) return false;
        int src = d.getSources();
        return (src & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD
                || (src & InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK;
    }

    private static void watchGamepads(final Activity a) {
        InputManager im = (InputManager) a.getSystemService(Context.INPUT_SERVICE);
        if (im == null) return;
        im.registerInputDeviceListener(new InputManager.InputDeviceListener() {
            @Override
            public void onInputDeviceAdded(int deviceId) {
                if (!padAtStart && isGamepad(InputDevice.getDevice(deviceId))) {
                    offerRestartForPad(a, "Se conectó un mando",
                            "Para jugar con el mando hay que reiniciar el juego. "
                            + "Los controles táctiles se ocultan solos.");
                }
            }

            @Override
            public void onInputDeviceRemoved(int deviceId) {
                if (padAtStart && !hasGamepad()) {
                    offerRestartForPad(a, "Se desconectó el mando",
                            "Para jugar con la pantalla hay que reiniciar el juego.");
                }
            }

            @Override
            public void onInputDeviceChanged(int deviceId) {}
        }, new Handler(Looper.getMainLooper()));
    }

    private static void offerRestartForPad(Activity a, String title, String message) {
        if (dialogOpen || a.isFinishing()) return;
        pause();
        askRestart(a, title, message);
    }
}
