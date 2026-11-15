.class public Lcom/br2recomp/android/SaveStateButton;
.super Landroid/widget/TextView;
.source "SaveStateButton.java"

# Boton tactil "GUARDAR" de Bloody Roar 2 Mobile.
# Al tocarlo envia la tecla F7, que abre/cierra el menu de save states del
# motor (12 ranuras). Dentro del menu: cruceta = elegir ranura,
# circulo (tecla S) = guardar, START (tecla Enter) = cargar.
#
# Equivale a este Java:
#   public class SaveStateButton extends TextView implements View.OnTouchListener {
#     public SaveStateButton(Context c) { ...estilo...; setOnTouchListener(this); }
#     public boolean onTouch(View v, MotionEvent e) {
#       DOWN -> SDLActivity.onNativeKeyDown(KEYCODE_F7); UP/CANCEL -> onNativeKeyUp(KEYCODE_F7)
#     }
#     public static void attach(Activity a) { a.addContentView(new SaveStateButton(a), abajo-centro); }
#   }

# interfaces
.implements Landroid/view/View$OnTouchListener;


# static fields
.field private static final KEY_F7:I = 0x89


# direct methods
.method public constructor <init>(Landroid/content/Context;)V
    .locals 6

    invoke-direct {p0, p1}, Landroid/widget/TextView;-><init>(Landroid/content/Context;)V

    # v0 = densidad de pantalla (px por dp)
    invoke-virtual {p1}, Landroid/content/Context;->getResources()Landroid/content/res/Resources;

    move-result-object v0

    invoke-virtual {v0}, Landroid/content/res/Resources;->getDisplayMetrics()Landroid/util/DisplayMetrics;

    move-result-object v0

    iget v0, v0, Landroid/util/DisplayMetrics;->density:F

    # texto
    const-string v1, "GUARDAR"

    invoke-virtual {p0, v1}, Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V

    # dorado del icono (#F6C94A)
    const v1, -0x936b6

    invoke-virtual {p0, v1}, Landroid/widget/TextView;->setTextColor(I)V

    # 12 sp
    const/4 v1, 0x2

    const/high16 v2, 0x41400000    # 12.0f

    invoke-virtual {p0, v1, v2}, Landroid/widget/TextView;->setTextSize(IF)V

    sget-object v1, Landroid/graphics/Typeface;->DEFAULT_BOLD:Landroid/graphics/Typeface;

    invoke-virtual {p0, v1}, Landroid/widget/TextView;->setTypeface(Landroid/graphics/Typeface;)V

    # relleno 14dp x 6dp
    const/high16 v1, 0x41600000    # 14.0f

    mul-float/2addr v1, v0

    float-to-int v1, v1

    const/high16 v2, 0x40c00000    # 6.0f

    mul-float/2addr v2, v0

    float-to-int v2, v2

    invoke-virtual {p0, v1, v2, v1, v2}, Landroid/widget/TextView;->setPadding(IIII)V

    # fondo: pastilla rojo oscuro semitransparente con borde dorado
    new-instance v3, Landroid/graphics/drawable/GradientDrawable;

    invoke-direct {v3}, Landroid/graphics/drawable/GradientDrawable;-><init>()V

    const v4, -0x4fc5f7f5    # 0xB03A080B

    invoke-virtual {v3, v4}, Landroid/graphics/drawable/GradientDrawable;->setColor(I)V

    const/high16 v4, 0x41a00000    # 20.0f

    mul-float/2addr v4, v0

    invoke-virtual {v3, v4}, Landroid/graphics/drawable/GradientDrawable;->setCornerRadius(F)V

    const/high16 v4, 0x3fc00000    # 1.5f

    mul-float/2addr v4, v0

    float-to-int v4, v4

    const v5, -0x936b6

    invoke-virtual {v3, v4, v5}, Landroid/graphics/drawable/GradientDrawable;->setStroke(II)V

    invoke-virtual {p0, v3}, Landroid/widget/TextView;->setBackground(Landroid/graphics/drawable/Drawable;)V

    const v1, 0x3f59999a    # 0.85f

    invoke-virtual {p0, v1}, Landroid/widget/TextView;->setAlpha(F)V

    invoke-virtual {p0, p0}, Landroid/widget/TextView;->setOnTouchListener(Landroid/view/View$OnTouchListener;)V

    return-void
.end method

.method public static attach(Landroid/app/Activity;)V
    .locals 4

    new-instance v0, Lcom/br2recomp/android/SaveStateButton;

    invoke-direct {v0, p0}, Lcom/br2recomp/android/SaveStateButton;-><init>(Landroid/content/Context;)V

    # WRAP_CONTENT x WRAP_CONTENT, abajo al centro (Gravity.BOTTOM | CENTER_HORIZONTAL)
    new-instance v1, Landroid/widget/FrameLayout$LayoutParams;

    const/4 v2, -0x2

    const/16 v3, 0x51

    invoke-direct {v1, v2, v2, v3}, Landroid/widget/FrameLayout$LayoutParams;-><init>(III)V

    # margen inferior 10dp
    invoke-virtual {p0}, Landroid/app/Activity;->getResources()Landroid/content/res/Resources;

    move-result-object v2

    invoke-virtual {v2}, Landroid/content/res/Resources;->getDisplayMetrics()Landroid/util/DisplayMetrics;

    move-result-object v2

    iget v2, v2, Landroid/util/DisplayMetrics;->density:F

    const/high16 v3, 0x41200000    # 10.0f

    mul-float/2addr v2, v3

    float-to-int v2, v2

    iput v2, v1, Landroid/view/ViewGroup$MarginLayoutParams;->bottomMargin:I

    invoke-virtual {p0, v0, v1}, Landroid/app/Activity;->addContentView(Landroid/view/View;Landroid/view/ViewGroup$LayoutParams;)V

    return-void
.end method


# virtual methods
.method public onTouch(Landroid/view/View;Landroid/view/MotionEvent;)Z
    .locals 3

    invoke-virtual {p2}, Landroid/view/MotionEvent;->getActionMasked()I

    move-result v0

    const/16 v1, 0x89    # KEYCODE_F7

    if-nez v0, :check_release

    # ACTION_DOWN: presiona F7 y oscurece el boton
    invoke-static {v1}, Lorg/libsdl/app/SDLActivity;->onNativeKeyDown(I)V

    const v0, 0x3ee66666    # 0.45f

    invoke-virtual {p0, v0}, Landroid/widget/TextView;->setAlpha(F)V

    goto :done

    :check_release
    const/4 v2, 0x1    # ACTION_UP

    if-eq v0, v2, :release

    const/4 v2, 0x3    # ACTION_CANCEL

    if-ne v0, v2, :done

    :release
    invoke-static {v1}, Lorg/libsdl/app/SDLActivity;->onNativeKeyUp(I)V

    const v0, 0x3f59999a    # 0.85f

    invoke-virtual {p0, v0}, Landroid/widget/TextView;->setAlpha(F)V

    :done
    const/4 v0, 0x1

    return v0
.end method
