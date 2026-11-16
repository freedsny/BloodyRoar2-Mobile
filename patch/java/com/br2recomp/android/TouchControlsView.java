package com.br2recomp.android;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.View;

import org.libsdl.app.SDLActivity;

import java.util.HashSet;
import java.util.Set;

/**
 * Controles táctiles con aspecto de control de PlayStation.
 *
 * Reemplaza al TouchControlsView original de la v1.0 (apply_patch.sh borra su
 * smali). La lógica es la misma — mismas teclas, misma posición/tamaño
 * guardados en "br2recomp_prefs" (tcl_N_x/y/s), mismo modo edición que usa
 * ControlsEditorActivity — y solo cambia el dibujo:
 *
 *  - Cruceta en forma de cruz con flechas.
 *  - △ ○ ✕ □ redondos con sus colores (verde, rojo, azul y rosado).
 *  - Gatillos L1/R1/L2/R2 como botones de hombro y SELECT/START como cápsulas.
 */
public class TouchControlsView extends View {
    public static final int E_COUNT = 8;
    public static final int E_DPAD = 0;
    public static final int E_FACE = 1;
    public static final int E_L1 = 2;
    public static final int E_R1 = 3;
    public static final int E_L2 = 4;
    public static final int E_R2 = 5;
    public static final int E_SEL = 6;
    public static final int E_START = 7;
    public static final float SCALE_MAX = 1.8f;
    public static final float SCALE_MIN = 0.6f;
    public static final String[] E_NAMES = {"Cruceta", "Botones", "L1", "R1", "L2", "R2", "Select", "Start"};

    // Teclas Android que el motor tiene mapeadas al control de PS1.
    private static final int K_UP = 19, K_DOWN = 20, K_LEFT = 21, K_RIGHT = 22;
    private static final int K_TRIANGLE = 29, K_CIRCLE = 47, K_CROSS = 52, K_SQUARE = 54;
    private static final int K_L1 = 45, K_R1 = 51, K_L2 = 33, K_R2 = 46;
    private static final int K_SELECT = 60, K_START = 66;

    private static final float[] DEF_X = {0.097f, 0.863f, 0.16f, 0.8f, 0.06f, 0.93f, 0.225f, 0.72f};
    private static final float[] DEF_Y = {0.54f, 0.55f, 0.075f, 0.075f, 0.075f, 0.075f, 0.88f, 0.88f};
    private static final float[] BASE_R = {0f, 0f, 0.085f, 0.085f, 0.06f, 0.06f, 0.065f, 0.065f};
    private static final String[] PILL_TEXT = {"", "", "L1", "R1", "L2", "R2", "SELECT", "START"};
    private static final int[] PILL_KEY = {0, 0, K_L1, K_R1, K_L2, K_R2, K_SELECT, K_START};

    // Colores (ARGB sin alfa; el alfa lo pone la transparencia elegida).
    private static final int BODY = 0x22232A;
    private static final int BODY_ON = 0x4A4D5C;
    private static final int EDGE = 0x8A8FA3;
    private static final int TEXT = 0xE6E8EF;
    private static final int ARROW = 0xB8BCC8;
    private static final int GREEN = 0x2FD6A8;   // △
    private static final int RED = 0xFF5470;     // ○
    private static final int BLUE = 0x6EA8FF;    // ✕
    private static final int PINK = 0xF27BD2;    // □

    private final float[] ex = new float[E_COUNT];
    private final float[] ey = new float[E_COUNT];
    private final float[] es = new float[E_COUNT];
    private final float[] pcx = new float[E_COUNT];
    private final float[] pcy = new float[E_COUNT];
    private final float[] pr = new float[E_COUNT];
    private final Set<Integer> down = new HashSet<>();

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint symbol = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint label = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint screenRect = new Paint();
    private final Path path = new Path();
    private final RectF rect = new RectF();

    private int alpha = 140;
    private boolean editMode = false;
    private int selected = -1;
    private boolean dragging = false;
    private float grabDx, grabDy;
    private int vw, vh;
    private float dpCx, dpCy, dpR;
    private float fbCx, fbCy, fbR, fbOff;
    private Listener listener;

    public interface Listener {
        void onSelected(int index, float scale);
    }

    public TouchControlsView(Context context) {
        super(context);
        fill.setStyle(Paint.Style.FILL);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeJoin(Paint.Join.ROUND);
        symbol.setStyle(Paint.Style.STROKE);
        symbol.setStrokeCap(Paint.Cap.ROUND);
        symbol.setStrokeJoin(Paint.Join.ROUND);
        label.setTextAlign(Paint.Align.CENTER);
        label.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
        ring.setStyle(Paint.Style.STROKE);
        ring.setColor(0xFFFFFFFF);
        ring.setStrokeWidth(4f);
        screenRect.setColor(0xFF161A26);
        setFocusable(false);
        resetLayout();
    }

    // ---------------------------------------------------------------- API

    public void setAlphaPercent(int pct) {
        if (pct < 10) pct = 10;
        if (pct > 100) pct = 100;
        alpha = pct * 255 / 100;
        invalidate();
    }

    public void setEditMode(boolean on) {
        editMode = on;
        invalidate();
    }

    public void setListener(Listener l) {
        listener = l;
    }

    public void select(int i) {
        selected = i;
        if (listener != null && i >= 0) listener.onSelected(i, es[i]);
        invalidate();
    }

    public void setSelectedScale(float s) {
        if (selected < 0) return;
        es[selected] = clamp(s, SCALE_MIN, SCALE_MAX);
        layoutAll();
        invalidate();
    }

    public void resetLayout() {
        for (int i = 0; i < E_COUNT; i++) {
            ex[i] = DEF_X[i];
            ey[i] = DEF_Y[i];
            es[i] = 1f;
        }
        layoutAll();
        invalidate();
    }

    public void loadLayout(SharedPreferences p) {
        for (int i = 0; i < E_COUNT; i++) {
            ex[i] = clamp(p.getFloat("tcl_" + i + "_x", DEF_X[i]), 0.02f, 0.98f);
            ey[i] = clamp(p.getFloat("tcl_" + i + "_y", DEF_Y[i]), 0.02f, 0.98f);
            es[i] = clamp(p.getFloat("tcl_" + i + "_s", 1f), SCALE_MIN, SCALE_MAX);
        }
        layoutAll();
        invalidate();
    }

    public void saveLayout(SharedPreferences p) {
        SharedPreferences.Editor ed = p.edit();
        for (int i = 0; i < E_COUNT; i++) {
            ed.putFloat("tcl_" + i + "_x", ex[i]);
            ed.putFloat("tcl_" + i + "_y", ey[i]);
            ed.putFloat("tcl_" + i + "_s", es[i]);
        }
        ed.apply();
    }

    /** Suelta todas las teclas presionadas (al ocultar los controles). */
    public void releaseAllKeys() {
        releaseAll();
    }

    // ------------------------------------------------------------- layout

    private static float clamp(float v, float lo, float hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        vw = w;
        vh = h;
        layoutAll();
    }

    private void layoutAll() {
        if (vw <= 0 || vh <= 0) return;
        float w = vw, h = vh;
        stroke.setStrokeWidth(Math.max(2f, 0.006f * h));
        ring.setStrokeWidth(Math.max(3f, 0.012f * h));
        dpR = 0.21f * h * es[E_DPAD];
        dpCx = ex[E_DPAD] * w;
        dpCy = ey[E_DPAD] * h;
        fbR = 0.095f * h * es[E_FACE];
        fbOff = 0.215f * h * es[E_FACE];
        fbCx = ex[E_FACE] * w;
        fbCy = ey[E_FACE] * h;
        for (int i = 2; i < E_COUNT; i++) {
            pcx[i] = ex[i] * w;
            pcy[i] = ey[i] * h;
            pr[i] = BASE_R[i] * h * es[i];
        }
    }

    private float centerX(int i) {
        return i == E_DPAD ? dpCx : (i == E_FACE ? fbCx : pcx[i]);
    }

    private float centerY(int i) {
        return i == E_DPAD ? dpCy : (i == E_FACE ? fbCy : pcy[i]);
    }

    private float hitRadius(int i) {
        if (i == E_DPAD) return dpR * 1.1f;
        if (i == E_FACE) return fbOff + fbR * 1.3f;
        return pr[i] * 1.35f;
    }

    // -------------------------------------------------------------- input

    private static boolean near(float x, float y, float cx, float cy, float r) {
        return Math.hypot(x - cx, y - cy) <= 1.25f * r;
    }

    private void collect(float x, float y, Set<Integer> out) {
        float dx = x - dpCx, dy = y - dpCy;
        if (Math.hypot(dx, dy) <= dpR * 1.4f) {
            float dz = dpR * 0.28f;
            if (dx < -dz) out.add(K_LEFT);
            if (dx > dz) out.add(K_RIGHT);
            if (dy < -dz) out.add(K_UP);
            if (dy > dz) out.add(K_DOWN);
        }
        if (near(x, y, fbCx, fbCy - fbOff, fbR)) out.add(K_TRIANGLE);
        if (near(x, y, fbCx + fbOff, fbCy, fbR)) out.add(K_CIRCLE);
        if (near(x, y, fbCx, fbCy + fbOff, fbR)) out.add(K_CROSS);
        if (near(x, y, fbCx - fbOff, fbCy, fbR)) out.add(K_SQUARE);
        for (int i = 2; i < E_COUNT; i++) {
            if (near(x, y, pcx[i], pcy[i], pr[i])) out.add(PILL_KEY[i]);
        }
    }

    private int hitTest(float x, float y) {
        int best = -1;
        float bestRatio = 1f;
        for (int i = 0; i < E_COUNT; i++) {
            float ratio = (float) Math.hypot(x - centerX(i), y - centerY(i)) / hitRadius(i);
            if (ratio <= bestRatio) {
                bestRatio = ratio;
                best = i;
            }
        }
        return best;
    }

    private boolean onEditTouch(MotionEvent e) {
        int action = e.getActionMasked();
        float x = e.getX(), y = e.getY();
        if (action == MotionEvent.ACTION_DOWN) {
            int hit = hitTest(x, y);
            if (hit >= 0) {
                dragging = true;
                grabDx = centerX(hit) - x;
                grabDy = centerY(hit) - y;
                select(hit);
            } else {
                dragging = false;
            }
        } else if (action == MotionEvent.ACTION_MOVE) {
            if (dragging && selected >= 0 && vw > 0 && vh > 0) {
                ex[selected] = clamp((grabDx + x) / vw, 0.02f, 0.98f);
                ey[selected] = clamp((grabDy + y) / vh, 0.02f, 0.98f);
                layoutAll();
                invalidate();
            }
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            dragging = false;
        }
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (editMode) return onEditTouch(e);
        int action = e.getActionMasked();
        int skip = -1;
        if (action == MotionEvent.ACTION_POINTER_UP || action == MotionEvent.ACTION_UP) {
            skip = e.getActionIndex();
        }
        Set<Integer> now = new HashSet<>();
        if (action != MotionEvent.ACTION_CANCEL) {
            for (int i = 0; i < e.getPointerCount(); i++) {
                if (i != skip) collect(e.getX(i), e.getY(i), now);
            }
        }
        boolean changed = false;
        for (int k : now) {
            if (!down.contains(k)) {
                SDLActivity.onNativeKeyDown(k);
                changed = true;
            }
        }
        for (int k : down) {
            if (!now.contains(k)) {
                SDLActivity.onNativeKeyUp(k);
                changed = true;
            }
        }
        if (changed) {
            down.clear();
            down.addAll(now);
            invalidate();
        }
        return true;
    }

    private void releaseAll() {
        if (!editMode) {
            for (int k : down) SDLActivity.onNativeKeyUp(k);
        }
        down.clear();
        invalidate();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (!hasFocus) releaseAll();
    }

    @Override
    protected void onDetachedFromWindow() {
        releaseAll();
        super.onDetachedFromWindow();
    }

    // ------------------------------------------------------------ drawing

    private int a(int base) {
        return Math.min(255, alpha + base);
    }

    private static int argb(int alpha, int rgb) {
        return (alpha << 24) | (rgb & 0xFFFFFF);
    }

    private boolean on(int key) {
        return down.contains(key);
    }

    private void body(Canvas c, boolean pressed) {
        fill.setColor(argb(pressed ? a(70) : alpha, pressed ? BODY_ON : BODY));
    }

    private void edge(boolean pressed) {
        stroke.setColor(argb(pressed ? a(120) : a(40), pressed ? TEXT : EDGE));
    }

    private void drawDpad(Canvas c) {
        float arm = dpR * 0.62f;       // ancho de cada brazo
        float half = arm / 2f;
        float corner = arm * 0.22f;

        // Cuerpo en forma de cruz
        path.reset();
        rect.set(dpCx - dpR, dpCy - half, dpCx + dpR, dpCy + half);
        path.addRoundRect(rect, corner, corner, Path.Direction.CW);
        rect.set(dpCx - half, dpCy - dpR, dpCx + half, dpCy + dpR);
        path.addRoundRect(rect, corner, corner, Path.Direction.CW);
        path.setFillType(Path.FillType.WINDING);
        body(c, false);
        c.drawPath(path, fill);

        // Brazos presionados
        drawArm(c, 0, -1, on(K_UP), half, corner);
        drawArm(c, 0, 1, on(K_DOWN), half, corner);
        drawArm(c, -1, 0, on(K_LEFT), half, corner);
        drawArm(c, 1, 0, on(K_RIGHT), half, corner);

        // Centro hundido
        fill.setColor(argb(a(10), 0x15161B));
        c.drawCircle(dpCx, dpCy, half * 0.55f, fill);

        // Flechas
        drawArrow(c, 0, -1, on(K_UP), half);
        drawArrow(c, 0, 1, on(K_DOWN), half);
        drawArrow(c, -1, 0, on(K_LEFT), half);
        drawArrow(c, 1, 0, on(K_RIGHT), half);
    }

    private void drawArm(Canvas c, int dx, int dy, boolean pressed, float half, float corner) {
        if (!pressed) return;
        float near = half * 0.9f;
        if (dx != 0) {
            float x0 = dpCx + dx * near, x1 = dpCx + dx * dpR;
            rect.set(Math.min(x0, x1), dpCy - half, Math.max(x0, x1), dpCy + half);
        } else {
            float y0 = dpCy + dy * near, y1 = dpCy + dy * dpR;
            rect.set(dpCx - half, Math.min(y0, y1), dpCx + half, Math.max(y0, y1));
        }
        body(c, true);
        c.drawRoundRect(rect, corner, corner, fill);
    }

    private void drawArrow(Canvas c, int dx, int dy, boolean pressed, float half) {
        float tip = dpR * 0.82f;
        float base = dpR * 0.52f;
        float w = half * 0.5f;
        float tx = dpCx + dx * tip, ty = dpCy + dy * tip;
        float bx = dpCx + dx * base, by = dpCy + dy * base;
        float px = -dy * w, py = dx * w;
        path.reset();
        path.moveTo(tx, ty);
        path.lineTo(bx + px, by + py);
        path.lineTo(bx - px, by - py);
        path.close();
        fill.setColor(argb(pressed ? 255 : a(60), pressed ? 0xFFFFFF : ARROW));
        c.drawPath(path, fill);
    }

    private void drawFace(Canvas c, float cx, float cy, int kind, boolean pressed) {
        int color = kind == 0 ? GREEN : kind == 1 ? RED : kind == 2 ? BLUE : PINK;
        float r = pressed ? fbR * 0.94f : fbR;

        body(c, pressed);
        c.drawCircle(cx, cy, r, fill);
        stroke.setColor(argb(pressed ? a(140) : a(30), pressed ? color : EDGE));
        c.drawCircle(cx, cy, r, stroke);

        symbol.setColor(argb(pressed ? 255 : a(90), color));
        symbol.setStrokeWidth(Math.max(3f, r * 0.15f));
        float s = r * 0.48f;
        switch (kind) {
            case 0: // △
                path.reset();
                path.moveTo(cx, cy - s);
                path.lineTo(cx + s * 0.95f, cy + s * 0.65f);
                path.lineTo(cx - s * 0.95f, cy + s * 0.65f);
                path.close();
                c.drawPath(path, symbol);
                break;
            case 1: // ○
                c.drawCircle(cx, cy, s * 0.9f, symbol);
                break;
            case 2: // ✕
                c.drawLine(cx - s * 0.8f, cy - s * 0.8f, cx + s * 0.8f, cy + s * 0.8f, symbol);
                c.drawLine(cx - s * 0.8f, cy + s * 0.8f, cx + s * 0.8f, cy - s * 0.8f, symbol);
                break;
            default: // □
                c.drawRect(cx - s * 0.75f, cy - s * 0.75f, cx + s * 0.75f, cy + s * 0.75f, symbol);
                break;
        }
    }

    private void drawShoulder(Canvas c, int i) {
        boolean pressed = on(PILL_KEY[i]);
        float r = pr[i];
        float hw = r * 1.25f, hh = r * 0.7f;
        rect.set(pcx[i] - hw, pcy[i] - hh, pcx[i] + hw, pcy[i] + hh);
        float corner = hh * 0.8f;
        body(c, pressed);
        c.drawRoundRect(rect, corner, corner, fill);
        edge(pressed);
        c.drawRoundRect(rect, corner, corner, stroke);
        drawLabel(c, PILL_TEXT[i], pcx[i], pcy[i], r * 0.62f, pressed);
    }

    private void drawCapsule(Canvas c, int i) {
        boolean pressed = on(PILL_KEY[i]);
        float r = pr[i];
        float hw = r * 1.3f, hh = r * 0.42f;
        rect.set(pcx[i] - hw, pcy[i] - hh, pcx[i] + hw, pcy[i] + hh);
        body(c, pressed);
        c.drawRoundRect(rect, hh, hh, fill);
        edge(pressed);
        c.drawRoundRect(rect, hh, hh, stroke);
        drawLabel(c, PILL_TEXT[i], pcx[i], pcy[i], r * 0.38f, pressed);
    }

    private void drawLabel(Canvas c, String t, float cx, float cy, float size, boolean pressed) {
        label.setTextSize(size);
        label.setColor(argb(pressed ? 255 : a(90), TEXT));
        c.drawText(t, cx, cy - (label.ascent() + label.descent()) / 2f, label);
    }

    @Override
    protected void onDraw(Canvas c) {
        if (editMode && vw > 0 && vh > 0) {
            float gw = vh * 4f / 3f;
            float left = (vw - gw) / 2f;
            c.drawRect(left, 0, left + gw, vh, screenRect);
        }
        drawDpad(c);
        drawFace(c, fbCx, fbCy - fbOff, 0, on(K_TRIANGLE));
        drawFace(c, fbCx + fbOff, fbCy, 1, on(K_CIRCLE));
        drawFace(c, fbCx, fbCy + fbOff, 2, on(K_CROSS));
        drawFace(c, fbCx - fbOff, fbCy, 3, on(K_SQUARE));
        drawShoulder(c, E_L1);
        drawShoulder(c, E_R1);
        drawShoulder(c, E_L2);
        drawShoulder(c, E_R2);
        drawCapsule(c, E_SEL);
        drawCapsule(c, E_START);
        if (editMode && selected >= 0) {
            c.drawCircle(centerX(selected), centerY(selected), hitRadius(selected), ring);
        }
    }
}
