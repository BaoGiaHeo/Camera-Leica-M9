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
    const/4 v5, 0x0
    aput-object v4, v3, v5
    const-string v4, "S\u1ee3i \u0111\u1ed1t"
    const/4 v5, 0x1
    aput-object v4, v3, v5
    const-string v4, "Hu\u1ef3nh quang"
    const/4 v5, 0x2
    aput-object v4, v3, v5
    const-string v4, "Hu\u1ef3nh quang \u1ea5m"
    const/4 v5, 0x3
    aput-object v4, v3, v5
    const-string v4, "\u00c1nh s\u00e1ng ban ng\u00e0y"
    const/4 v5, 0x4
    aput-object v4, v3, v5
    const-string v4, "Tr\u1eddi nhi\u1ec1u m\u00e2y"
    const/4 v5, 0x5
    aput-object v4, v3, v5
    const-string v4, "Ch\u1ea1ng v\u1ea1ng"
    const/4 v5, 0x6
    aput-object v4, v3, v5
    const-string v4, "B\u00f3ng r\u00e2m"
    const/4 v5, 0x7
    aput-object v4, v3, v5
    const/4 v4, 0x0
    goto :show_choice

    :case_focus_mode
    const-string v1, "focus_mode"
    const-string v2, "Ch\u1ebf \u0111\u1ed9 l\u1ea5y n\u00e9t"
    const/4 v3, 0x2
    new-array v3, v3, [Ljava/lang/CharSequence;
    const-string v4, "T\u1ef1 \u0111\u1ed9ng"
    const/4 v5, 0x0
    aput-object v4, v3, v5
    const-string v4, "Th\u1ee7 c\u00f4ng"
    const/4 v5, 0x1
    aput-object v4, v3, v5
    const/4 v4, 0x0
    goto :show_choice

    :case_focus_distance
    const-string v1, "focus_index"
    const-string v2, "Kho\u1ea3ng l\u1ea5y n\u00e9t"
    const/4 v3, 0x7
    new-array v3, v3, [Ljava/lang/CharSequence;
    const-string v4, "V\u00f4 c\u1ef1c"
    const/4 v5, 0x0
    aput-object v4, v3, v5
    const-string v4, "2 m"
    const/4 v5, 0x1
    aput-object v4, v3, v5
    const-string v4, "1 m"
    const/4 v5, 0x2
    aput-object v4, v3, v5
    const-string v4, "0,5 m"
    const/4 v5, 0x3
    aput-object v4, v3, v5
    const-string v4, "0,25 m"
    const/4 v5, 0x4
    aput-object v4, v3, v5
    const-string v4, "0,125 m"
    const/4 v5, 0x5
    aput-object v4, v3, v5
    const-string v4, "0,1 m"
    const/4 v5, 0x6
    aput-object v4, v3, v5
    const/4 v4, 0x1

    :show_choice
    invoke-static {p0, v1, v2, v3, v4}, Lcom/xcam/offline/ProCameraFeature;->showChoice(Landroid/app/Activity;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/CharSequence;I)V
    return-void

    :switch_data
    .packed-switch 0x0
        :case_exposure
        :case_iso
        :case_shutter
        :case_wb
        :case_focus_mode
        :case_focus_distance
    .end packed-switch
.end method

.method private static showChoice(Landroid/app/Activity;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/CharSequence;I)V
    .locals 5
    :try_start
    const-string v0, "pro_camera"
    const/4 v1, 0x0
    invoke-virtual {p0, v0, v1}, Landroid/app/Activity;->getSharedPreferences(Ljava/lang/String;I)Landroid/content/SharedPreferences;
    move-result-object v0
    invoke-interface {v0, p1, p4}, Landroid/content/SharedPreferences;->getInt(Ljava/lang/String;I)I
    move-result v0
    new-instance v1, Landroid/app/AlertDialog$Builder;
    invoke-direct {v1, p0}, Landroid/app/AlertDialog$Builder;-><init>(Landroid/content/Context;)V
    invoke-virtual {v1, p2}, Landroid/app/AlertDialog$Builder;->setTitle(Ljava/lang/CharSequence;)Landroid/app/AlertDialog$Builder;
    new-instance v2, Lcom/xcam/offline/ProCameraFeature$ChoiceClick;
    invoke-direct {v2, p0, p1}, Lcom/xcam/offline/ProCameraFeature$ChoiceClick;-><init>(Landroid/app/Activity;Ljava/lang/String;)V
    invoke-virtual {v1, p3, v0, v2}, Landroid/app/AlertDialog$Builder;->setSingleChoiceItems([Ljava/lang/CharSequence;ILandroid/content/DialogInterface$OnClickListener;)Landroid/app/AlertDialog$Builder;
    const-string v2, "Hu\u1ef7"
    const/4 v3, 0x0
    invoke-virtual {v1, v2, v3}, Landroid/app/AlertDialog$Builder;->setNegativeButton(Ljava/lang/CharSequence;Landroid/content/DialogInterface$OnClickListener;)Landroid/app/AlertDialog$Builder;
    invoke-virtual {v1}, Landroid/app/AlertDialog$Builder;->show()Landroid/app/AlertDialog;
    :try_end
    .catch Ljava/lang/Throwable; {:try_start .. :try_end} :catch_all
    return-void
    :catch_all
    move-exception v0
    return-void
.end method

.method public static configure(Landroid/app/Activity;Landroidx/camera/core/ExtendableBuilder;Landroid/hardware/camera2/CameraCharacteristics;)V
    .locals 22
    if-eqz p0, :done
    if-eqz p1, :done
    if-eqz p2, :done
    :try_start
    move-object/from16 v4, p0
    move-object/from16 v2, p1
    move-object/from16 v11, p2
    const-string v0, "pro_camera"
    const/4 v1, 0x0
    invoke-virtual {v4, v0, v1}, Landroid/app/Activity;->getSharedPreferences(Ljava/lang/String;I)Landroid/content/SharedPreferences;
    move-result-object v0
    new-instance v1, Landroidx/camera/camera2/interop/Camera2Interop$Extender;
    invoke-direct {v1, v2}, Landroidx/camera/camera2/interop/Camera2Interop$Extender;-><init>(Landroidx/camera/core/ExtendableBuilder;)V

    # Manual sensor exposure is applied only when Camera2 advertises MANUAL_SENSOR.
    const-string v2, "exposure_mode"
    const/4 v3, 0x0
    invoke-interface {v0, v2, v3}, Landroid/content/SharedPreferences;->getInt(Ljava/lang/String;I)I
    move-result v2
    if-eqz v2, :awb
    sget-object v2, Landroid/hardware/camera2/CameraCharacteristics;->REQUEST_AVAILABLE_CAPABILITIES:Landroid/hardware/camera2/CameraCharacteristics$Key;
    invoke-virtual {v11, v2}, Landroid/hardware/camera2/CameraCharacteristics;->get(Landroid/hardware/camera2/CameraCharacteristics$Key;)Ljava/lang/Object;
    move-result-object v2
    check-cast v2, [I
    const/4 v4, 0x1
    invoke-static {v2, v4}, Lcom/xcam/offline/ProCameraFeature;->containsInt([II)Z
    move-result v2
    if-eqz v2, :awb

    # ISO preset; clamp to the active lens's real sensor range.
    const-string v2, "iso_index"
    const/4 v4, 0x2
    invoke-interface {v0, v2, v4}, Landroid/content/SharedPreferences;->getInt(Ljava/lang/String;I)I
    move-result v2
    packed-switch v2, :iso_switch
    const/16 v2, 0x190
    goto :iso_done
    :iso_0
    const/16 v2, 0x64
    goto :iso_done
    :iso_1
    const/16 v2, 0xc8
    goto :iso_done
    :iso_2
    const/16 v2, 0x190
    goto :iso_done
    :iso_3
    const/16 v2, 0x320
    goto :iso_done
    :iso_4
    const/16 v2, 0x640
    goto :iso_done
    :iso_5
    const/16 v2, 0xc80
    goto :iso_done
    :iso_6
    const/16 v2, 0x1900
    :iso_done
    sget-object v4, Landroid/hardware/camera2/CameraCharacteristics;->SENSOR_INFO_SENSITIVITY_RANGE:Landroid/hardware/camera2/CameraCharacteristics$Key;
    invoke-virtual {v11, v4}, Landroid/hardware/camera2/CameraCharacteristics;->get(Landroid/hardware/camera2/CameraCharacteristics$Key;)Ljava/lang/Object;
    move-result-object v4
    check-cast v4, Landroid/util/Range;
    if-eqz v4, :awb
    invoke-virtual {v4}, Landroid/util/Range;->getLower()Ljava/lang/Comparable;
    move-result-object v5
    check-cast v5, Ljava/lang/Number;
    invoke-virtual {v5}, Ljava/lang/Number;->intValue()I
    move-result v5
    invoke-virtual {v4}, Landroid/util/Range;->getUpper()Ljava/lang/Comparable;
    move-result-object v4
    check-cast v4, Ljava/lang/Number;
    invoke-virtual {v4}, Ljava/lang/Number;->intValue()I
    move-result v4
    invoke-static {v2, v5}, Ljava/lang/Math;->max(II)I
    move-result v2
    invoke-static {v2, v4}, Ljava/lang/Math;->min(II)I
    move-result v2
    move v12, v2

    # Shutter time in nanoseconds; clamp to the active sensor's exposure range.
    const-string v2, "shutter_index"
    const/4 v4, 0x4
    invoke-interface {v0, v2, v4}, Landroid/content/SharedPreferences;->getInt(Ljava/lang/String;I)I
    move-result v2
    packed-switch v2, :shutter_switch
    const-wide/32 v4, 0x00fe502a
    goto :shutter_done
    :shutter_0
    const-wide/32 v4, 0x000f4240
    goto :shutter_done
    :shutter_1
    const-wide/32 v4, 0x001e8480
    goto :shutter_done
    :shutter_2
    const-wide/32 v4, 0x003d0900
    goto :shutter_done
    :shutter_3
    const-wide/32 v4, 0x007a1200
    goto :shutter_done
    :shutter_4
    const-wide/32 v4, 0x00fe502a
    goto :shutter_done
    :shutter_5
    const-wide/32 v4, 0x01fca055
    goto :shutter_done
    :shutter_6
    const-wide/32 v4, 0x03f940aa
    goto :shutter_done
    :shutter_7
    const-wide/32 v4, 0x07f28155
    goto :shutter_done
    :shutter_8
    const-wide/32 v4, 0x0bebc200
    goto :shutter_done
    :shutter_9
    const-wide/32 v4, 0x1dcd6500
    goto :shutter_done
    :shutter_10
    const-wide/32 v4, 0x3b9aca00
    goto :shutter_done
    :shutter_11
    const-wide/32 v4, 0x77359400
    goto :shutter_done
    :shutter_12
    const-wide/32 v4, 0xee6b2800
    goto :shutter_done
    :shutter_13
    const-wide/32 v4, 0xee6b2800
    const-wide/16 v6, 0x2
    mul-long/2addr v4, v6
    goto :shutter_done
    :shutter_14
    const-wide/32 v4, 0xee6b2800
    const-wide/16 v6, 0x4
    mul-long/2addr v4, v6
    goto :shutter_done
    :shutter_15
    const-wide/32 v4, 0xee6b2800
    const-wide/16 v6, 0x8
    mul-long/2addr v4, v6
    :shutter_done
    sget-object v6, Landroid/hardware/camera2/CameraCharacteristics;->SENSOR_INFO_EXPOSURE_TIME_RANGE:Landroid/hardware/camera2/CameraCharacteristics$Key;
    invoke-virtual {v11, v6}, Landroid/hardware/camera2/CameraCharacteristics;->get(Landroid/hardware/camera2/CameraCharacteristics$Key;)Ljava/lang/Object;
    move-result-object v6
    check-cast v6, Landroid/util/Range;
    if-eqz v6, :awb
    invoke-virtual {v6}, Landroid/util/Range;->getLower()Ljava/lang/Comparable;
    move-result-object v7
    check-cast v7, Ljava/lang/Number;
    invoke-virtual {v7}, Ljava/lang/Number;->longValue()J
    move-result-wide v7
    invoke-virtual {v6}, Landroid/util/Range;->getUpper()Ljava/lang/Comparable;
    move-result-object v6
    check-cast v6, Ljava/lang/Number;
    invoke-virtual {v6}, Ljava/lang/Number;->longValue()J
    move-result-wide v9
    invoke-static {v4, v5, v7, v8}, Ljava/lang/Math;->max(JJ)J
    move-result-wide v4
    invoke-static {v4, v5, v9, v10}, Ljava/lang/Math;->min(JJ)J
    move-result-wide v4

    sget-object v6, Landroid/hardware/camera2/CaptureRequest;->CONTROL_AE_MODE:Landroid/hardware/camera2/CaptureRequest$Key;
    const/4 v7, 0x0
    invoke-static {v7}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;
    move-result-object v7
    invoke-virtual {v1, v6, v7}, Landroidx/camera/camera2/interop/Camera2Interop$Extender;->setCaptureRequestOption(Landroid/hardware/camera2/CaptureRequest$Key;Ljava/lang/Object;)Landroidx/camera/camera2/interop/Camera2Interop$Extender;
    sget-object v6, Landroid/hardware/camera2/CaptureRequest;->SENSOR_SENSITIVITY:Landroid/hardware/camera2/CaptureRequest$Key;
    invoke-static {v12}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;
    move-result-object v7
    invoke-virtual {v1, v6, v7}, Landroidx/camera/camera2/interop/Camera2Interop$Extender;->setCaptureRequestOption(Landroid/hardware/camera2/CaptureRequest$Key;Ljava/lang/Object;)Landroidx/camera/camera2/interop/Camera2Interop$Extender;
    sget-object v6, Landroid/hardware/camera2/CaptureRequest;->SENSOR_EXPOSURE_TIME:Landroid/hardware/camera2/CaptureRequest$Key;
    invoke-static {v4, v5}, Ljava/lang/Long;->valueOf(J)Ljava/lang/Long;
    move-result-object v7
    invoke-virtual {v1, v6, v7}, Landroidx/camera/camera2/interop/Camera2Interop$Extender;->setCaptureRequestOption(Landroid/hardware/camera2/CaptureRequest$Key;Ljava/lang/Object;)Landroidx/camera/camera2/interop/Camera2Interop$Extender;

    :awb
    const-string v2, "wb_index"
    const/4 v3, 0x0
    invoke-interface {v0, v2, v3}, Landroid/content/SharedPreferences;->getInt(Ljava/lang/String;I)I
    move-result v2
    if-lez v2, :focus
    add-int/lit8 v2, v2, 0x1
    sget-object v3, Landroid/hardware/camera2/CameraCharacteristics;->CONTROL_AWB_AVAILABLE_MODES:Landroid/hardware/camera2/CameraCharacteristics$Key;
    invoke-virtual {v11, v3}, Landroid/hardware/camera2/CameraCharacteristics;->get(Landroid/hardware/camera2/CameraCharacteristics$Key;)Ljava/lang/Object;
    move-result-object v3
    check-cast v3, [I
    move v4, v2
    invoke-static {v3, v4}, Lcom/xcam/offline/ProCameraFeature;->containsInt([II)Z
    move-result v3
    if-eqz v3, :focus
    sget-object v3, Landroid/hardware/camera2/CaptureRequest;->CONTROL_AWB_MODE:Landroid/hardware/camera2/CaptureRequest$Key;
    invoke-static {v2}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;
    move-result-object v2
    invoke-virtual {v1, v3, v2}, Landroidx/camera/camera2/interop/Camera2Interop$Extender;->setCaptureRequestOption(Landroid/hardware/camera2/CaptureRequest$Key;Ljava/lang/Object;)Landroidx/camera/camera2/interop/Camera2Interop$Extender;

    :focus
    const-string v2, "focus_mode"
    const/4 v3, 0x0
    invoke-interface {v0, v2, v3}, Landroid/content/SharedPreferences;->getInt(Ljava/lang/String;I)I
    move-result v2
    if-eqz v2, :done
    sget-object v2, Landroid/hardware/camera2/CameraCharacteristics;->LENS_INFO_MINIMUM_FOCUS_DISTANCE:Landroid/hardware/camera2/CameraCharacteristics$Key;
    invoke-virtual {v11, v2}, Landroid/hardware/camera2/CameraCharacteristics;->get(Landroid/hardware/camera2/CameraCharacteristics$Key;)Ljava/lang/Object;
    move-result-object v2
    check-cast v2, Ljava/lang/Float;
    if-eqz v2, :done
    invoke-virtual {v2}, Ljava/lang/Float;->floatValue()F
    move-result v3
    const/4 v4, 0x0
    cmpl-float v4, v3, v4
    if-lez v4, :done
    const-string v4, "focus_index"
    const/4 v5, 0x1
    invoke-interface {v0, v4, v5}, Landroid/content/SharedPreferences;->getInt(Ljava/lang/String;I)I
    move-result v4
    invoke-static {v4, v3}, Lcom/xcam/offline/ProCameraFeature;->focusDiopters(IF)F
    move-result v4
    const/4 v5, 0x0
    invoke-static {v4, v5}, Ljava/lang/Math;->max(FF)F
    move-result v4
    invoke-static {v4, v3}, Ljava/lang/Math;->min(FF)F
    move-result v4
    sget-object v5, Landroid/hardware/camera2/CaptureRequest;->CONTROL_AF_MODE:Landroid/hardware/camera2/CaptureRequest$Key;
    const/4 v6, 0x0
    invoke-static {v6}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;
    move-result-object v6
    invoke-virtual {v1, v5, v6}, Landroidx/camera/camera2/interop/Camera2Interop$Extender;->setCaptureRequestOption(Landroid/hardware/camera2/CaptureRequest$Key;Ljava/lang/Object;)Landroidx/camera/camera2/interop/Camera2Interop$Extender;
    sget-object v5, Landroid/hardware/camera2/CaptureRequest;->LENS_FOCUS_DISTANCE:Landroid/hardware/camera2/CaptureRequest$Key;
    invoke-static {v4}, Ljava/lang/Float;->valueOf(F)Ljava/lang/Float;
    move-result-object v6
    invoke-virtual {v1, v5, v6}, Landroidx/camera/camera2/interop/Camera2Interop$Extender;->setCaptureRequestOption(Landroid/hardware/camera2/CaptureRequest$Key;Ljava/lang/Object;)Landroidx/camera/camera2/interop/Camera2Interop$Extender;
    :try_end
    .catch Ljava/lang/Throwable; {:try_start .. :try_end} :catch_all
    :done
    return-void
    :catch_all
    move-exception v0
    return-void
.end method

.method private static containsInt([II)Z
    .locals 3
    if-eqz p0, :false
    array-length v0, p0
    const/4 v1, 0x0
    :loop
    if-ge v1, v0, :false
    aget v2, p0, v1
    if-ne v2, p1, :next
    const/4 v0, 0x1
    return v0
    :next
    add-int/lit8 v1, v1, 0x1
    goto :loop
    :false
    const/4 v0, 0x0
    return v0
.end method

.method private static focusDiopters(IF)F
    .locals 1
    packed-switch p0, :distance_switch
    const v0, 0x3f800000
    return v0
    :distance_0
    const/4 v0, 0x0
    return v0
    :distance_1
    const/high16 v0, 0x3f000000    # 0.5
    return v0
    :distance_2
    const/high16 v0, 0x3f800000    # 1.0
    return v0
    :distance_3
    const/high16 v0, 0x40000000    # 2.0
    return v0
    :distance_4
    const/high16 v0, 0x40800000    # 4.0
    return v0
    :distance_5
    const/high16 v0, 0x41000000    # 8.0
    return v0
    :distance_6
    const v0, 0x41200000    # 10.0
    return v0
    :distance_switch
    .packed-switch 0x0
        :distance_0
        :distance_1
        :distance_2
        :distance_3
        :distance_4
        :distance_5
        :distance_6
    .end packed-switch
.end method

.method private static rebind(Landroid/app/Activity;)V
    .locals 4
    :try_start
    invoke-virtual {p0}, Ljava/lang/Object;->getClass()Ljava/lang/Class;
    move-result-object v0
    const-string v1, "bindCamera"
    const/4 v2, 0x0
    new-array v3, v2, [Ljava/lang/Class;
    invoke-virtual {v0, v1, v3}, Ljava/lang/Class;->getDeclaredMethod(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;
    move-result-object v0
    const/4 v1, 0x1
    invoke-virtual {v0, v1}, Ljava/lang/reflect/AccessibleObject;->setAccessible(Z)V
    new-array v1, v2, [Ljava/lang/Object;
    invoke-virtual {v0, p0, v1}, Ljava/lang/reflect/Method;->invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;
    :try_end
    .catch Ljava/lang/Throwable; {:try_start .. :try_end} :catch_all
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
