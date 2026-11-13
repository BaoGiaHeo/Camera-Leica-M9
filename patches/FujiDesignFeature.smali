.class public final Lcom/xcam/offline/FujiDesignFeature;
.super Ljava/lang/Object;
.source "FujiDesignFeature.java"

.field private static final GOLD:I
.field private static final PANEL:I

.method static constructor <clinit>()V
    .locals 2
    const v0, -0x17e6e2e5
    sput v0, Lcom/xcam/offline/FujiDesignFeature;->PANEL:I
    const v1, -0x7f375695
    sput v1, Lcom/xcam/offline/FujiDesignFeature;->GOLD:I
    return-void
.end method

.method private constructor <init>()V
    .locals 0
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    return-void
.end method

.method public static install(Landroid/app/Activity;)V
    .locals 4
    if-eqz p0, :done
    :try_start
    invoke-virtual {p0}, Landroid/app/Activity;->getWindow()Landroid/view/Window;
    move-result-object v0
    sget v1, Lcom/xcam/offline/FujiDesignFeature;->PANEL:I
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
    if-eqz v2, :try_end
    check-cast v1, Landroid/view/ViewGroup;

    const/4 v2, 0x3
    invoke-virtual {v1, v2}, Landroid/view/ViewGroup;->getChildAt(I)Landroid/view/View;
    move-result-object v2
    if-eqz v2, :bottom_panel
    invoke-static {p0, v2}, Lcom/xcam/offline/FujiDesignFeature;->stylePanel(Landroid/app/Activity;Landroid/view/View;)V
    invoke-static {v2}, Lcom/xcam/offline/FujiDesignFeature;->applyBrand(Landroid/view/View;)V

    :bottom_panel
    const/4 v2, 0x4
    invoke-virtual {v1, v2}, Landroid/view/ViewGroup;->getChildAt(I)Landroid/view/View;
    move-result-object v2
    if-eqz v2, :try_end
    invoke-static {p0, v2}, Lcom/xcam/offline/FujiDesignFeature;->stylePanel(Landroid/app/Activity;Landroid/view/View;)V
    invoke-static {v2}, Lcom/xcam/offline/FujiDesignFeature;->applyBrand(Landroid/view/View;)V
    :try_end
    .catch Ljava/lang/Throwable; {:try_start .. :try_end} :catch_all
    :done
    return-void
    :catch_all
    move-exception v0
    return-void
.end method

.method private static stylePanel(Landroid/app/Activity;Landroid/view/View;)V
    .locals 4
    new-instance v0, Landroid/graphics/drawable/GradientDrawable;
    invoke-direct {v0}, Landroid/graphics/drawable/GradientDrawable;-><init>()V
    sget v1, Lcom/xcam/offline/FujiDesignFeature;->PANEL:I
    invoke-virtual {v0, v1}, Landroid/graphics/drawable/GradientDrawable;->setColor(I)V

    const/high16 v1, 0x41c00000    # 24.0f
    invoke-static {p0, v1}, Lcom/xcam/offline/FujiDesignFeature;->dp(Landroid/app/Activity;F)I
    move-result v1
    int-to-float v1, v1
    invoke-virtual {v0, v1}, Landroid/graphics/drawable/GradientDrawable;->setCornerRadius(F)V

    const/high16 v1, 0x3f800000    # 1.0f
    invoke-static {p0, v1}, Lcom/xcam/offline/FujiDesignFeature;->dp(Landroid/app/Activity;F)I
    move-result v1
    sget v2, Lcom/xcam/offline/FujiDesignFeature;->GOLD:I
    invoke-virtual {v0, v1, v2}, Landroid/graphics/drawable/GradientDrawable;->setStroke(II)V
    invoke-virtual {p1, v0}, Landroid/view/View;->setBackground(Landroid/graphics/drawable/Drawable;)V

    const/high16 v1, 0x41000000    # 8.0f
    invoke-static {p0, v1}, Lcom/xcam/offline/FujiDesignFeature;->dp(Landroid/app/Activity;F)I
    move-result v1
    int-to-float v1, v1
    invoke-virtual {p1, v1}, Landroid/view/View;->setElevation(F)V
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
