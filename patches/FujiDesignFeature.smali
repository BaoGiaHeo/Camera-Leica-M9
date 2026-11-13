.class public final Lcom/xcam/offline/FujiDesignFeature;
.super Ljava/lang/Object;
.source "PhotonCameraUiFeature.java"

.field private static final BACKGROUND:I
.field private static final PANEL:I
.field private static final ACCENT:I
.field private static final BORDER:I

.method static constructor <clinit>()V
    .locals 4
    const v0, -0xf2f2f3
    sput v0, Lcom/xcam/offline/FujiDesignFeature;->BACKGROUND:I
    const v1, -0xe5e5e6
    sput v1, Lcom/xcam/offline/FujiDesignFeature;->PANEL:I
    const v2, -0x32cc
    sput v2, Lcom/xcam/offline/FujiDesignFeature;->ACCENT:I
    const v3, 0x26ffffff
    sput v3, Lcom/xcam/offline/FujiDesignFeature;->BORDER:I
    return-void
.end method

.method private constructor <init>()V
    .locals 0
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    return-void
.end method

.method public static install(Landroid/app/Activity;)V
    .locals 5
    if-eqz p0, :done
    :try_start
    invoke-virtual {p0}, Landroid/app/Activity;->getWindow()Landroid/view/Window;
    move-result-object v0
    sget v1, Lcom/xcam/offline/FujiDesignFeature;->BACKGROUND:I
    invoke-virtual {v0, v1}, Landroid/view/Window;->setStatusBarColor(I)V
    invoke-virtual {v0, v1}, Landroid/view/Window;->setNavigationBarColor(I)V

    invoke-virtual {p0}, Ljava/lang/Object;->getClass()Ljava/lang/Class;
    move-result-object v1
    const-string v2, "root"
    invoke-virtual {v1, v2}, Ljava/lang/Class;->getDeclaredField(Ljava/lang/String;)Ljava/lang/reflect/Field;
    move-result-object v1
    const/4 v2, 0x1
    invoke-virtual {v1, v2}, Ljava/lang/reflect/AccessibleObject;->setAccessible(Z)V
    invoke-virtual {v1, p0}, Ljava/lang/reflect/Field;->get(Ljava/lang/Object;)Ljava/lang/Object;
    move-result-object v1
    instance-of v2, v1, Landroid/view/ViewGroup;
    if-eqz v2, :done
    check-cast v1, Landroid/view/ViewGroup;
    sget v2, Lcom/xcam/offline/FujiDesignFeature;->BACKGROUND:I
    invoke-virtual {v1, v2}, Landroid/view/View;->setBackgroundColor(I)V
    invoke-static {v1}, Lcom/xcam/offline/FujiDesignFeature;->applyBrand(Landroid/view/View;)V
    const/16 v2, 0x10
    invoke-static {p0, v1, v2}, Lcom/xcam/offline/FujiDesignFeature;->styleTree(Landroid/app/Activity;Landroid/view/View;I)V
    :try_end
    .catch Ljava/lang/Throwable; {:try_start .. :try_end} :catch_all
    :done
    return-void
    :catch_all
    move-exception v0
    return-void
.end method

.method private static styleTree(Landroid/app/Activity;Landroid/view/View;I)V
    .locals 8
    if-eqz p1, :done
    if-lez p2, :done

    instance-of v0, p1, Landroid/widget/TextView;
    if-eqz v0, :seekbar
    move-object v0, p1
    check-cast v0, Landroid/widget/TextView;
    const-string v1, "sans-serif-medium"
    const/4 v2, 0x0
    invoke-static {v1, v2}, Landroid/graphics/Typeface;->create(Ljava/lang/String;I)Landroid/graphics/Typeface;
    move-result-object v1
    invoke-virtual {v0, v1}, Landroid/widget/TextView;->setTypeface(Landroid/graphics/Typeface;)V
    const/4 v1, -0x1
    invoke-virtual {v0, v1}, Landroid/widget/TextView;->setTextColor(I)V
    invoke-virtual {v0}, Landroid/view/View;->isClickable()Z
    move-result v1
    if-eqz v1, :seekbar
    new-instance v1, Landroid/graphics/drawable/GradientDrawable;
    invoke-direct {v1}, Landroid/graphics/drawable/GradientDrawable;-><init>()V
    const/4 v2, 0x0
    invoke-virtual {v1, v2}, Landroid/graphics/drawable/GradientDrawable;->setShape(I)V
    const v2, -0xd9d9da
    invoke-virtual {v1, v2}, Landroid/graphics/drawable/GradientDrawable;->setColor(I)V
    const/high16 v2, 0x41800000
    invoke-static {p0, v2}, Lcom/xcam/offline/FujiDesignFeature;->dp(Landroid/app/Activity;F)I
    move-result v2
    int-to-float v2, v2
    invoke-virtual {v1, v2}, Landroid/graphics/drawable/GradientDrawable;->setCornerRadius(F)V
    const/high16 v2, 0x3f800000
    invoke-static {p0, v2}, Lcom/xcam/offline/FujiDesignFeature;->dp(Landroid/app/Activity;F)I
    move-result v2
    sget v3, Lcom/xcam/offline/FujiDesignFeature;->BORDER:I
    invoke-virtual {v1, v2, v3}, Landroid/graphics/drawable/GradientDrawable;->setStroke(II)V
    invoke-virtual {v0, v1}, Landroid/widget/TextView;->setBackground(Landroid/graphics/drawable/Drawable;)V

    :seekbar
    instance-of v0, p1, Landroid/widget/SeekBar;
    if-eqz v0, :switches
    move-object v0, p1
    check-cast v0, Landroid/widget/SeekBar;
    sget v1, Lcom/xcam/offline/FujiDesignFeature;->ACCENT:I
    invoke-static {v1}, Landroid/content/res/ColorStateList;->valueOf(I)Landroid/content/res/ColorStateList;
    move-result-object v1
    invoke-virtual {v0, v1}, Landroid/widget/ProgressBar;->setProgressTintList(Landroid/content/res/ColorStateList;)V
    invoke-virtual {v0, v1}, Landroid/widget/SeekBar;->setThumbTintList(Landroid/content/res/ColorStateList;)V

    :switches
    instance-of v0, p1, Landroid/widget/CompoundButton;
    if-eqz v0, :groups
    move-object v0, p1
    check-cast v0, Landroid/widget/CompoundButton;
    sget v1, Lcom/xcam/offline/FujiDesignFeature;->ACCENT:I
    invoke-static {v1}, Landroid/content/res/ColorStateList;->valueOf(I)Landroid/content/res/ColorStateList;
    move-result-object v1
    invoke-virtual {v0, v1}, Landroid/widget/CompoundButton;->setButtonTintList(Landroid/content/res/ColorStateList;)V

    :groups
    instance-of v0, p1, Landroid/view/ViewGroup;
    if-eqz v0, :done
    move-object v0, p1
    check-cast v0, Landroid/view/ViewGroup;
    invoke-virtual {v0}, Landroid/view/ViewGroup;->getChildCount()I
    move-result v2
    instance-of v1, p1, Landroid/widget/LinearLayout;
    if-eqz v1, :children
    const/4 v1, 0x3
    if-lt v2, v1, :children
    new-instance v1, Landroid/graphics/drawable/GradientDrawable;
    invoke-direct {v1}, Landroid/graphics/drawable/GradientDrawable;-><init>()V
    const/4 v3, 0x0
    invoke-virtual {v1, v3}, Landroid/graphics/drawable/GradientDrawable;->setShape(I)V
    sget v3, Lcom/xcam/offline/FujiDesignFeature;->PANEL:I
    invoke-virtual {v1, v3}, Landroid/graphics/drawable/GradientDrawable;->setColor(I)V
    const/high16 v3, 0x41800000
    invoke-static {p0, v3}, Lcom/xcam/offline/FujiDesignFeature;->dp(Landroid/app/Activity;F)I
    move-result v3
    int-to-float v3, v3
    invoke-virtual {v1, v3}, Landroid/graphics/drawable/GradientDrawable;->setCornerRadius(F)V
    const/high16 v3, 0x3f800000
    invoke-static {p0, v3}, Lcom/xcam/offline/FujiDesignFeature;->dp(Landroid/app/Activity;F)I
    move-result v3
    sget v4, Lcom/xcam/offline/FujiDesignFeature;->BORDER:I
    invoke-virtual {v1, v3, v4}, Landroid/graphics/drawable/GradientDrawable;->setStroke(II)V
    invoke-virtual {p1, v1}, Landroid/view/View;->setBackground(Landroid/graphics/drawable/Drawable;)V

    :children
    add-int/lit8 p2, p2, -0x1
    const/4 v3, 0x0
    :child_loop
    if-ge v3, v2, :done
    invoke-virtual {v0, v3}, Landroid/view/ViewGroup;->getChildAt(I)Landroid/view/View;
    move-result-object v4
    invoke-static {p0, v4, p2}, Lcom/xcam/offline/FujiDesignFeature;->styleTree(Landroid/app/Activity;Landroid/view/View;I)V
    add-int/lit8 v3, v3, 0x1
    goto :child_loop

    :done
    return-void
.end method

.method private static applyBrand(Landroid/view/View;)V
    .locals 5
    instance-of v0, p0, Landroid/widget/TextView;
    if-eqz v0, :check_group
    move-object v0, p0
    check-cast v0, Landroid/widget/TextView;
    invoke-virtual {v0}, Landroid/widget/TextView;->getText()Ljava/lang/CharSequence;
    move-result-object v1
    const-string v2, "Fujifilm Mode"
    invoke-static {v1, v2}, Landroid/text/TextUtils;->equals(Ljava/lang/CharSequence;Ljava/lang/CharSequence;)Z
    move-result v1
    if-eqz v1, :check_group

    const-string v1, "Camera Leica M9"
    invoke-virtual {v0, v1}, Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V

    const/4 v1, 0x0
    invoke-virtual {v0, v1}, Landroid/view/View;->setVisibility(I)V
    const/high16 v1, 0x41b00000    # 22.0f
    invoke-virtual {v0, v1}, Landroid/widget/TextView;->setTextSize(F)V
    sget v1, Lcom/xcam/offline/FujiDesignFeature;->GOLD:I
    invoke-virtual {v0, v1}, Landroid/widget/TextView;->setTextColor(I)V
    const-string v1, "sans-serif-condensed"
    const/4 v2, 0x1
    invoke-static {v1, v2}, Landroid/graphics/Typeface;->create(Ljava/lang/String;I)Landroid/graphics/Typeface;
    move-result-object v1
    invoke-virtual {v0, v1}, Landroid/widget/TextView;->setTypeface(Landroid/graphics/Typeface;)V
    const v1, 0x3dcccccd    # 0.1f
    invoke-virtual {v0, v1}, Landroid/widget/TextView;->setLetterSpacing(F)V

    :check_group
    instance-of v0, p0, Landroid/view/ViewGroup;
    if-eqz v0, :done
    move-object v0, p0
    check-cast v0, Landroid/view/ViewGroup;
    invoke-virtual {v0}, Landroid/view/ViewGroup;->getChildCount()I
    move-result v2
    const/4 v1, 0x0
    :loop
    if-ge v1, v2, :done
    invoke-virtual {v0, v1}, Landroid/view/ViewGroup;->getChildAt(I)Landroid/view/View;
    move-result-object v3
    invoke-static {v3}, Lcom/xcam/offline/FujiDesignFeature;->applyBrand(Landroid/view/View;)V
    add-int/lit8 v1, v1, 0x1
    goto :loop
    :done
    return-void
.end method

.method private static dp(Landroid/app/Activity;F)I
    .locals 2
    invoke-virtual {p0}, Landroid/app/Activity;->getResources()Landroid/content/res/Resources;
    move-result-object v0
    invoke-virtual {v0}, Landroid/content/res/Resources;->getDisplayMetrics()Landroid/util/DisplayMetrics;
    move-result-object v0
    iget v0, v0, Landroid/util/DisplayMetrics;->density:F
    mul-float/2addr p1, v0
    invoke-static {p1}, Ljava/lang/Math;->round(F)I
    move-result v1
    return v1
.end method
