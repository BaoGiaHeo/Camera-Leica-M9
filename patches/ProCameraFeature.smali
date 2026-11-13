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
    if-eqz p0, :done
    :try_start
    new-instance v0, Landroid/app/AlertDialog$Builder;
    invoke-direct {v0, p0}, Landroid/app/AlertDialog$Builder;-><init>(Landroid/content/Context;)V
    const-string v1, "B\u1ed8 \u0110I\u1ec0U KHI\u1ec2N CAMERA PRO"
    invoke-virtual {v0, v1}, Landroid/app/AlertDialog$Builder;->setTitle(Ljava/lang/CharSequence;)Landroid/app/AlertDialog$Builder;
    const/4 v1, 0x6
    new-array v1, v1, [Ljava/lang/CharSequence;
    const-string v2, "Ph\u01a1i s\u00e1ng: Auto / Manual"
    const/4 v3, 0x0
    aput-object v2, v1, v3
    const-string v2, "ISO"
    const/4 v3, 0x1
    aput-object v2, v1, v3
    const-string v2, "T\u1ed1c \u0111\u1ed9 m\u00e0n tr\u1eadp"
    const/4 v3, 0x2
    aput-object v2, v1, v3
    const-string v2, "C\u00e2n b\u1eb1ng tr\u1eafng"
    const/4 v3, 0x3
    aput-object v2, v1, v3
    const-string v2, "L\u1ea5y n\u00e9t: Auto / Manual"
    const/4 v3, 0x4
    aput-object v2, v1, v3
    const-string v2, "Kho\u1ea3ng l\u1ea5y n\u00e9t th\u1ee7 c\u00f4ng"
    const/4 v3, 0x5
    aput-object v2, v1, v3
    new-instance v2, Lcom/xcam/offline/ProCameraFeature$MenuClick;
    invoke-direct {v2, p0}, Lcom/xcam/offline/ProCameraFeature$MenuClick;-><init>(Landroid/app/Activity;)V
    invoke-virtual {v0, v1, v2}, Landroid/app/AlertDialog$Builder;->setItems([Ljava/lang/CharSequence;Landroid/content/DialogInterface$OnClickListener;)Landroid/app/AlertDialog$Builder;
    const-string v1, "EV v\u1eabn \u0111\u01b0\u1ee3c \u0111i\u1ec1u ch\u1ec9nh tr\u00ean thanh EV c\u1ee7a m\u00e0n ch\u1ee5p. EV ch\u1ec9 c\u00f3 t\u00e1c d\u1ee5ng khi ph\u01a1i s\u00e1ng Auto."
    invoke-virtual {v0, v1}, Landroid/app/AlertDialog$Builder;->setMessage(Ljava/lang/CharSequence;)Landroid/app/AlertDialog$Builder;
    const-string v1, "\u0110\u00f3ng"
    const/4 v2, 0x0
    invoke-virtual {v0, v1, v2}, Landroid/app/AlertDialog$Builder;->setNegativeButton(Ljava/lang/CharSequence;Landroid/content/DialogInterface$OnClickListener;)Landroid/app/AlertDialog$Builder;
    invoke-virtual {v0}, Landroid/app/AlertDialog$Builder;->show()Landroid/app/AlertDialog;
    :try_end
    .catch Ljava/lang/Throwable; {:try_start .. :try_end} :catch_all
    :done
    return-void
    :catch_all
    move-exception v0
    return-void
.end method

.method static showOptions(Landroid/app/Activity;I)V
    .locals 9
    const-string v0, "pro_camera"
    const/4 v1, 0x0
    invoke-virtual {p0, v0, v1}, Landroid/app/Activity;->getSharedPreferences(Ljava/lang/String;I)Landroid/content/SharedPreferences;
    packed-switch p1, :switch_data
    return-void

    :case_exposure
    const-string v1, "exposure_mode"
    const-string v2, "Ch\u1ebf \u0111\u1ed9 ph\u01a1i s\u00e1ng"
    const/4 v3, 0x2
    new-array v3, v3, [Ljava/lang/CharSequence;
    const-string v4, "T\u1ef1 \u0111\u1ed9ng"
    const/4 v5, 0x0
    aput-object v4, v3, v5
    const-string v4, "Th\u1ee7 c\u00f4ng (Manual)"
    const/4 v5, 0x1
    aput-object v4, v3, v5
    const/4 v4, 0x0
    goto :show_choice

    :case_iso
    const-string v1, "iso_index"
    const-string v2, "ISO \u2014 ch\u1ec9nh \u0111\u1ed9 nh\u1ea1y s\u00e1ng"
    const/4 v3, 0x7
    new-array v3, v3, [Ljava/lang/CharSequence;
    const-string v4, "ISO 100"
    const/4 v5, 0x0
    aput-object v4, v3, v5
    const-string v4, "ISO 200"
    const/4 v5, 0x1
    aput-object v4, v3, v5
    const-string v4, "ISO 400"
    const/4 v5, 0x2
    aput-object v4, v3, v5
    const-string v4, "ISO 800"
    const/4 v5, 0x3
    aput-object v4, v3, v5
    const-string v4, "ISO 1600"
    const/4 v5, 0x4
    aput-object v4, v3, v5
    const-string v4, "ISO 3200"
    const/4 v5, 0x5
    aput-object v4, v3, v5
    const-string v4, "ISO 6400"
    const/4 v5, 0x6
    aput-object v4, v3, v5
    const/4 v4, 0x2
    goto :show_choice

    :case_shutter
    const-string v1, "shutter_index"
    const-string v2, "T\u1ed1c \u0111\u1ed9 m\u00e0n tr\u1eadp"
    const/16 v3, 0x11
    new-array v3, v3, [Ljava/lang/CharSequence;
    const-string v4, "1/1000 gi\u00e2y"
    const/4 v5, 0x0
    aput-object v4, v3, v5
    const-string v4, "1/500 gi\u00e2y"
    const/4 v5, 0x1
    aput-object v4, v3, v5
    const-string v4, "1/250 gi\u00e2y"
    const/4 v5, 0x2
    aput-object v4, v3, v5
    const-string v4, "1/125 gi\u00e2y"
    const/4 v5, 0x3
    aput-object v4, v3, v5
    const-string v4, "1/60 gi\u00e2y"
    const/4 v5, 0x4
    aput-object v4, v3, v5
    const-string v4, "1/30 gi\u00e2y"
    const/4 v5, 0x5
    aput-object v4, v3, v5
    const-string v4, "1/15 gi\u00e2y"
    const/4 v5, 0x6
    aput-object v4, v3, v5
    const-string v4, "1/8 gi\u00e2y"
    const/4 v5, 0x7
    aput-object v4, v3, v5
    const-string v4, "1/4 gi\u00e2y"
    const/16 v5, 0x8
    aput-object v4, v3, v5
    const-string v4, "1/2 gi\u00e2y"
    const/16 v5, 0x9
    aput-object v4, v3, v5
    const-string v4, "1 gi\u00e2y"
    const/16 v5, 0xa
    aput-object v4, v3, v5
    const-string v4, "2 gi\u00e2y"
    const/16 v5, 0xb
    aput-object v4, v3, v5
    const-string v4, "4 gi\u00e2y"
    const/16 v5, 0xc
    aput-object v4, v3, v5
    const-string v4, "8 gi\u00e2y"
    const/16 v5, 0xd
    aput-object v4, v3, v5
    const-string v4, "15 gi\u00e2y"
    const/16 v5, 0xe
    aput-object v4, v3, v5
    const-string v4, "20 gi\u00e2y"
    const/16 v5, 0xf
    aput-object v4, v3, v5
    const-string v4, "30 gi\u00e2y"
    const/16 v5, 0x10
    aput-object v4, v3, v5
    const/4 v4, 0x4
    goto :show_choice

    :case_wb
    const-string v1, "wb_index"
    const-string v2, "C\u00e2n b\u1eb1ng tr\u1eafng (WB)"
    const/16 v3, 0x8
    new-array v3, v3, [Ljava/lang/CharSequence;
    const-string v4, "Auto"
