.class public final Lcom/xcam/offline/ProCameraFeature;
.super Ljava/lang/Object;
.source "ProCameraFeature.java"

.method private constructor <init>()V
    .locals 0
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    return-void
.end method

.method public static install(Landroid/app/Activity;)V
    .locals 10
    if-eqz p0, :done
    :try_start
    invoke-virtual {p0}, Ljava/lang/Object;->getClass()Ljava/lang/Class;
    move-result-object v0
    const-string v1, "root"
    invoke-virtual {v0, v1}, Ljava/lang/Class;->getDeclaredField(Ljava/lang/String;)Ljava/lang/reflect/Field;
    move-result-object v0
    const/4 v1, 0x1
    invoke-virtual {v0, v1}, Ljava/lang/reflect/AccessibleObject;->setAccessible(Z)V
    invoke-virtual {v0, p0}, Ljava/lang/reflect/Field;->get(Ljava/lang/Object;)Ljava/lang/Object;
    move-result-object v0
    instance-of v1, v0, Landroid/widget/FrameLayout;
    if-eqz v1, :done
    check-cast v0, Landroid/widget/FrameLayout;
    const-string v1, "pro-camera-entry"
    invoke-virtual {v0, v1}, Landroid/widget/FrameLayout;->findViewWithTag(Ljava/lang/Object;)Landroid/view/View;
    move-result-object v1
    if-nez v1, :done

    new-instance v1, Landroid/widget/TextView;
    invoke-direct {v1, p0}, Landroid/widget/TextView;-><init>(Landroid/content/Context;)V
    const-string v2, "PRO"
    invoke-virtual {v1, v2}, Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V
    const/high16 v2, 0x41500000    # 13.0f
    invoke-virtual {v1, v2}, Landroid/widget/TextView;->setTextSize(F)V
    const v2, -0x7f375695
    invoke-virtual {v1, v2}, Landroid/widget/TextView;->setTextColor(I)V
    const/16 v2, 0x11
    invoke-virtual {v1, v2}, Landroid/widget/TextView;->setGravity(I)V
    const/4 v2, 0x1
    invoke-virtual {v1, v2}, Landroid/widget/TextView;->setClickable(Z)V
    const-string v2, "pro-camera-entry"
    invoke-virtual {v1, v2}, Landroid/widget/TextView;->setTag(Ljava/lang/Object;)V

    new-instance v2, Landroid/graphics/drawable/GradientDrawable;
    invoke-direct {v2}, Landroid/graphics/drawable/GradientDrawable;-><init>()V
    const/4 v3, 0x0
    invoke-virtual {v2, v3}, Landroid/graphics/drawable/GradientDrawable;->setShape(I)V
    const v3, -0xe8e8e9
    invoke-virtual {v2, v3}, Landroid/graphics/drawable/GradientDrawable;->setColor(I)V
    const/high16 v3, 0x41100000    # 9.0f
    invoke-static {p0, v3}, Lcom/xcam/offline/ProCameraFeature;->dp(Landroid/app/Activity;F)I
    move-result v3
    int-to-float v3, v3
    invoke-virtual {v2, v3}, Landroid/graphics/drawable/GradientDrawable;->setCornerRadius(F)V
    const/high16 v3, 0x3f800000    # 1.0f
    invoke-static {p0, v3}, Lcom/xcam/offline/ProCameraFeature;->dp(Landroid/app/Activity;F)I
    move-result v3
    const v4, -0x7f375695
    invoke-virtual {v2, v3, v4}, Landroid/graphics/drawable/GradientDrawable;->setStroke(II)V
    invoke-virtual {v1, v2}, Landroid/widget/TextView;->setBackground(Landroid/graphics/drawable/Drawable;)V

    new-instance v2, Lcom/xcam/offline/ProCameraFeature$OpenClick;
    invoke-direct {v2, p0}, Lcom/xcam/offline/ProCameraFeature$OpenClick;-><init>(Landroid/app/Activity;)V
    invoke-virtual {v1, v2}, Landroid/widget/TextView;->setOnClickListener(Landroid/view/View$OnClickListener;)V

    new-instance v2, Landroid/widget/FrameLayout$LayoutParams;
    const/high16 v3, 0x42800000    # 64.0f
    invoke-static {p0, v3}, Lcom/xcam/offline/ProCameraFeature;->dp(Landroid/app/Activity;F)I
    move-result v3
    const/high16 v4, 0x42000000    # 32.0f
    invoke-static {p0, v4}, Lcom/xcam/offline/ProCameraFeature;->dp(Landroid/app/Activity;F)I
    move-result v4
    const/16 v5, 0x35
    invoke-direct {v2, v3, v4, v5}, Landroid/widget/FrameLayout$LayoutParams;-><init>(III)V
    const/high16 v3, 0x41800000    # 16.0f
    invoke-static {p0, v3}, Lcom/xcam/offline/ProCameraFeature;->dp(Landroid/app/Activity;F)I
    move-result v3
    const/high16 v4, 0x42500000    # 52.0f
    invoke-static {p0, v4}, Lcom/xcam/offline/ProCameraFeature;->dp(Landroid/app/Activity;F)I
    move-result v4
    const/high16 v5, 0x41800000    # 16.0f
    invoke-static {p0, v5}, Lcom/xcam/offline/ProCameraFeature;->dp(Landroid/app/Activity;F)I
    move-result v5
    const/4 v6, 0x0
    invoke-virtual {v2, v6, v4, v5, v6}, Landroid/view/ViewGroup$MarginLayoutParams;->setMargins(IIII)V
    invoke-virtual {v0, v1, v2}, Landroid/widget/FrameLayout;->addView(Landroid/view/View;Landroid/view/ViewGroup$LayoutParams;)V
    :try_end
    .catch Ljava/lang/Throwable; {:try_start .. :try_end} :catch_all
    :done
    return-void
    :catch_all
    move-exception v0
    return-void
.end method

.method public static show(Landroid/app/Activity;)V
    .locals 5