.class public final Lcom/xcam/offline/FujiCameraUiFeature;
.super Ljava/lang/Object;
.source "CamerAwesomeInspiredUi.java"

.method private constructor <init>()V
    .locals 0
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    return-void
.end method

.method public static install(Landroid/app/Activity;)V
    .locals 8
    if-eqz p0, :done
    :try_start
    invoke-virtual {p0}, Landroid/app/Activity;->getWindow()Landroid/view/Window;
    move-result-object v0
    const/high16 v1, -0x1000000
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
    move-result-object v3
    instance-of v4, v3, Landroid/widget/FrameLayout;
    if-eqz v4, :done
    check-cast v3, Landroid/widget/FrameLayout;
    const/high16 v4, -0x1000000
    invoke-virtual {v3, v4}, Landroid/view/View;->setBackgroundColor(I)V
    invoke-virtual {v3}, Landroid/view/ViewGroup;->getChildCount()I
    move-result v4
    const/4 v5, 0x5
    if-lt v4, v5, :done
    const/4 v4, 0x3
    invoke-virtual {v3, v4}, Landroid/view/ViewGroup;->getChildAt(I)Landroid/view/View;
    move-result-object v4
    const v5, 0xe60b0b0b
    invoke-static {p0, v4, v5}, Lcom/xcam/offline/FujiCameraUiFeature;->stylePanel(Landroid/app/Activity;Landroid/view/View;I)V
    const/4 v4, 0x4
    invoke-virtual {v3, v4}, Landroid/view/ViewGroup;->getChildAt(I)Landroid/view/View;
    move-result-object v4
    const v5, 0xf20a0a0a
    invoke-static {p0, v4, v5}, Lcom/xcam/offline/FujiCameraUiFeature;->stylePanel(Landroid/app/Activity;Landroid/view/View;I)V
    invoke-static {p0}, Lcom/xcam/offline/FujiCameraUiFeature;->styleShutter(Landroid/app/Activity;)V
    :try_end
    .catch Ljava/lang/Throwable; {:try_start .. :try_end} :catch_all
    :done
    return-void
    :catch_all
    move-exception v0
    return-void
.end method

.method private static stylePanel(Landroid/app/Activity;Landroid/view/View;I)V
    .locals 4
    if-eqz p1, :done
    new-instance v0, Landroid/graphics/drawable/GradientDrawable;
    invoke-direct {v0}, Landroid/graphics/drawable/GradientDrawable;-><init>()V
    const/4 v1, 0x0
    invoke-virtual {v0, v1}, Landroid/graphics/drawable/GradientDrawable;->setShape(I)V
    invoke-virtual {v0, p2}, Landroid/graphics/drawable/GradientDrawable;->setColor(I)V
    const/high16 v1, 0x41700000
    invoke-static {p0, v1}, Lcom/xcam/offline/FujiCameraUiFeature;->dp(Landroid/app/Activity;F)I
    move-result v1
    int-to-float v1, v1
    invoke-virtual {v0, v1}, Landroid/graphics/drawable/GradientDrawable;->setCornerRadius(F)V
    const/high16 v1, 0x3f800000
    invoke-static {p0, v1}, Lcom/xcam/offline/FujiCameraUiFeature;->dp(Landroid/app/Activity;F)I
    move-result v1
    const v2, 0x40ffffff
    invoke-virtual {v0, v1, v2}, Landroid/graphics/drawable/GradientDrawable;->setStroke(II)V
    invoke-virtual {p1, v0}, Landroid/view/View;->setBackground(Landroid/graphics/drawable/Drawable;)V
    const/high16 v0, 0x41000000
    invoke-static {p0, v0}, Lcom/xcam/offline/FujiCameraUiFeature;->dp(Landroid/app/Activity;F)I
    move-result v0
    int-to-float v0, v0
    invoke-virtual {p1, v0}, Landroid/view/View;->setElevation(F)V
    const/4 v2, 0x6
    invoke-static {p0, p1, v2}, Lcom/xcam/offline/FujiCameraUiFeature;->styleTree(Landroid/app/Activity;Landroid/view/View;I)V
    :done
    return-void
.end method

.method private static styleTree(Landroid/app/Activity;Landroid/view/View;I)V
    .locals 9
    if-eqz p1, :done
    if-lez p2, :done
    instance-of v0, p1, Landroid/widget/TextView;
    if-eqz v0, :groups
    move-object v0, p1
    check-cast v0, Landroid/widget/TextView;
    const-string v1, "sans-serif-medium"
    const/4 v2, 0x0
    invoke-static {v1, v2}, Landroid/graphics/Typeface;->create(Ljava/lang/String;I)Landroid/graphics/Typeface;
    move-result-object v1
    invoke-virtual {v0, v1}, Landroid/widget/TextView;->setTypeface(Landroid/graphics/Typeface;)V
    const/4 v1, -0x1
    invoke-virtual {v0, v1}, Landroid/widget/TextView;->setTextColor(I)V
    invoke-virtual {v0}, Landroid/widget/TextView;->getText()Ljava/lang/CharSequence;
    move-result-object v1
    const-string v2, "Fujifilm Mode"
    invoke-static {v1, v2}, Landroid/text/TextUtils;->equals(Ljava/lang/CharSequence;Ljava/lang/CharSequence;)Z
    move-result v1
    if-eqz v1, :clickable
    const-string v1, "FUJIFILM"
    invoke-virtual {v0, v1}, Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V
    const/4 v1, 0x0
    invoke-virtual {v0, v1}, Landroid/view/View;->setVisibility(I)V
    const/high16 v1, 0x41900000
    invoke-virtual {v0, v1}, Landroid/widget/TextView;->setTextSize(F)V
    const v1, 0xffd8d8d8
    invoke-virtual {v0, v1}, Landroid/widget/TextView;->setTextColor(I)V
    const-string v1, "sans-serif-condensed"
    const/4 v2, 0x1
    invoke-static {v1, v2}, Landroid/graphics/Typeface;->create(Ljava/lang/String;I)Landroid/graphics/Typeface;
    move-result-object v1
    invoke-virtual {v0, v1}, Landroid/widget/TextView;->setTypeface(Landroid/graphics/Typeface;)V
    const v1, 0x3dcccccd
    invoke-virtual {v0, v1}, Landroid/widget/TextView;->setLetterSpacing(F)V
    :clickable
    invoke-virtual {p1}, Landroid/view/View;->isClickable()Z
    move-result v1
    if-eqz v1, :groups
    new-instance v1, Landroid/graphics/drawable/GradientDrawable;
    invoke-direct {v1}, Landroid/graphics/drawable/GradientDrawable;-><init>()V
    const/4 v2, 0x0
    invoke-virtual {v1, v2}, Landroid/graphics/drawable/GradientDrawable;->setShape(I)V
    const v2, 0xff242424
    invoke-virtual {v1, v2}, Landroid/graphics/drawable/GradientDrawable;->setColor(I)V
    const/high16 v2, 0x41600000
    invoke-static {p0, v2}, Lcom/xcam/offline/FujiCameraUiFeature;->dp(Landroid/app/Activity;F)I
    move-result v2
    int-to-float v2, v2
    invoke-virtual {v1, v2}, Landroid/graphics/drawable/GradientDrawable;->setCornerRadius(F)V
    const/high16 v2, 0x3f800000
    invoke-static {p0, v2}, Lcom/xcam/offline/FujiCameraUiFeature;->dp(Landroid/app/Activity;F)I
    move-result v2
    const v3, 0x55ffffff
    invoke-virtual {v1, v2, v3}, Landroid/graphics/drawable/GradientDrawable;->setStroke(II)V
    invoke-virtual {v0, v1}, Landroid/widget/TextView;->setBackground(Landroid/graphics/drawable/Drawable;)V
    const/high16 v2, 0x41400000
    invoke-static {p0, v2}, Lcom/xcam/offline/FujiCameraUiFeature;->dp(Landroid/app/Activity;F)I
    move-result v2
    const/high16 v3, 0x41000000
    invoke-static {p0, v3}, Lcom/xcam/offline/FujiCameraUiFeature;->dp(Landroid/app/Activity;F)I
    move-result v3
    invoke-virtual {v0, v2, v3, v2, v3}, Landroid/widget/TextView;->setPadding(IIII)V
    :groups
    instance-of v0, p1, Landroid/view/ViewGroup;
    if-eqz v0, :done
    move-object v0, p1
    check-cast v0, Landroid/view/ViewGroup;
    invoke-virtual {v0}, Landroid/view/ViewGroup;->getChildCount()I
    move-result v2
    add-int/lit8 p2, p2, -0x1
    const/4 v3, 0x0
    :child_loop
    if-ge v3, v2, :done
    invoke-virtual {v0, v3}, Landroid/view/ViewGroup;->getChildAt(I)Landroid/view/View;
    move-result-object v4
    invoke-static {p0, v4, p2}, Lcom/xcam/offline/FujiCameraUiFeature;->styleTree(Landroid/app/Activity;Landroid/view/View;I)V
    add-int/lit8 v3, v3, 0x1
    goto :child_loop
    :done
    return-void
.end method

.method private static styleShutter(Landroid/app/Activity;)V
    .locals 5
    :try_start
    invoke-virtual {p0}, Ljava/lang/Object;->getClass()Ljava/lang/Class;
    move-result-object v0
    const-string v1, "shutterButton"
    invoke-virtual {v0, v1}, Ljava/lang/Class;->getDeclaredField(Ljava/lang/String;)Ljava/lang/reflect/Field;
    move-result-object v0
    const/4 v1, 0x1
    invoke-virtual {v0, v1}, Ljava/lang/reflect/AccessibleObject;->setAccessible(Z)V
    invoke-virtual {v0, p0}, Ljava/lang/reflect/Field;->get(Ljava/lang/Object;)Ljava/lang/Object;
    move-result-object v2
    instance-of v3, v2, Landroid/view/View;
    if-eqz v3, :done
    check-cast v2, Landroid/view/View;
    new-instance v0, Landroid/graphics/drawable/GradientDrawable;
    invoke-direct {v0}, Landroid/graphics/drawable/GradientDrawable;-><init>()V
    const/4 v1, 0x0
    invoke-virtual {v0, v1}, Landroid/graphics/drawable/GradientDrawable;->setShape(I)V
    const v1, 0xffc62828
    invoke-virtual {v0, v1}, Landroid/graphics/drawable/GradientDrawable;->setColor(I)V
    const/high16 v1, 0x41700000
    invoke-static {p0, v1}, Lcom/xcam/offline/FujiCameraUiFeature;->dp(Landroid/app/Activity;F)I
    move-result v1
    int-to-float v1, v1
    invoke-virtual {v0, v1}, Landroid/graphics/drawable/GradientDrawable;->setCornerRadius(F)V
    const/high16 v1, 0x40000000
    invoke-static {p0, v1}, Lcom/xcam/offline/FujiCameraUiFeature;->dp(Landroid/app/Activity;F)I
    move-result v1
    const v3, -0x1
    invoke-virtual {v0, v1, v3}, Landroid/graphics/drawable/GradientDrawable;->setStroke(II)V
    invoke-virtual {v2, v0}, Landroid/view/View;->setBackground(Landroid/graphics/drawable/Drawable;)V
    instance-of v1, v2, Landroid/widget/TextView;
    if-eqz v1, :done
    check-cast v2, Landroid/widget/TextView;
    const v1, -0x1
    invoke-virtual {v2, v1}, Landroid/widget/TextView;->setTextColor(I)V
    :try_end
    .catch Ljava/lang/Throwable; {:try_start .. :try_end} :catch_all
    :done
    return-void
    :catch_all
    move-exception v0
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
