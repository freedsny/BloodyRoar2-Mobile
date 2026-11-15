package org.libsdl.app;

/**
 * Solo para compilar: la clase real ya viene dentro del APK original.
 * Este archivo NO se empaqueta (build_extras.sh lo usa como classpath).
 */
public class SDLActivity extends android.app.Activity {
    public static native void onNativeKeyDown(int keycode);
    public static native void onNativeKeyUp(int keycode);
}
