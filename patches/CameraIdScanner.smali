.class public final Lcom/xcam/offline/CameraIdScanner;
.super Ljava/lang/Object;
.source "CameraIdScanner.java"

.method private constructor <init>()V
    .locals 0
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    return-void
.end method

.method private static scan(Landroid/content/Context;)Ljava/lang/String;
    .locals 12
    const-string v0, "camera"
    invoke-virtual {p0, v0}, Landroid/content/Context;->getSystemService(Ljava/lang/String;)Ljava/lang/Object;
    move-result-object v0
    check-cast v0, Landroid/hardware/camera2/CameraManager;
    if-eqz v0, :no_manager
    invoke-virtual {v0}, Landroid/hardware/camera2/CameraManager;->getCameraIdList()[Ljava/lang/String;
    move-result-object v1
    new-instance v4, Ljava/lang/StringBuilder;
    invoke-direct {v4}, Ljava/lang/StringBuilder;-><init>()V
    const-string v2, "CAMERA ID SCANNER\nSố camera ID Android công bố: "
    invoke-virtual {v4, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    move-result-object v4
    array-length v2, v1
    invoke-virtual {v4, v2}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;
    move-result-object v4
    const-string v2, "\nCamera logic có thể đại diện cho nhiều camera vật lý.\n\n"
    invoke-virtual {v4, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    move-result-object v4
    const/4 v3, 0x0
    :loop
    array-length v2, v1
    if-ge v3, v2, :done
    aget-object v5, v1, v3
    invoke-virtual {v0, v5}, Landroid/hardware/camera2/CameraManager;->getCameraCharacteristics(Ljava/lang/String;)Landroid/hardware/camera2/CameraCharacteristics;
    move-result-object v6
    const-string v2, "ID: "
    invoke-virtual {v4, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    move-result-object v4
    invoke-virtual {v4, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    move-result-object v4
    const-string v2, "\nFacing: "
    invoke-virtual {v4, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    move-result-object v4
    sget-object v7, Landroid/hardware/camera2/CameraCharacteristics;->LENS_FACING:Landroid/hardware/camera2/CameraCharacteristics$Key;
    invoke-virtual {v6, v7}, Landroid/hardware/camera2/CameraCharacteristics;->get(Landroid/hardware/camera2/CameraCharacteristics$Key;)Ljava/lang/Object;
    move-result-object v7
    check-cast v7, Ljava/lang/Integer;
    if-eqz v7, :facing_unknown
    invoke-virtual {v7}, Ljava/lang/Integer;->intValue()I
    move-result v7
    if-nez v7, :facing_not_back
    const-string v8, "BACK (sau)"
    goto :facing_append
    :facing_not_back
    const/4 v8, 0x1
    if-ne v7, v8, :facing_not_front
    const-string v8, "FRONT (trước)"
    goto :facing_append
    :facing_not_front
    const/4 v8, 0x2
    if-ne v7, v8, :facing_unknown
    const-string v8, "EXTERNAL (ngoài)"
    goto :facing_append
    :facing_unknown
    const-string v8, "UNKNOWN / không công bố"
    :facing_append
    invoke-virtual {v4, v8}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    move-result-object v4
    const-string v2, "\nFocal length: "
    invoke-virtual {v4, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    move-result-object v4
    sget-object v7, Landroid/hardware/camera2/CameraCharacteristics;->LENS_INFO_AVAILABLE_FOCAL_LENGTHS:Landroid/hardware/camera2/CameraCharacteristics$Key;
    invoke-virtual {v6, v7}, Landroid/hardware/camera2/CameraCharacteristics;->get(Landroid/hardware/camera2/CameraCharacteristics$Key;)Ljava/lang/Object;
    move-result-object v7
    check-cast v7, [F
    if-eqz v7, :focal_missing
    array-length v8, v7
    if-lez v8, :focal_missing
    const/4 v8, 0x0
    aget v8, v7, v8
    invoke-static {v8}, Ljava/lang/Float;->toString(F)Ljava/lang/String;
    move-result-object v8
    invoke-virtual {v4, v8}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    move-result-object v4
    const-string v2, " mm"
    invoke-virtual {v4, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    move-result-object v4
    goto :physical_start
    :focal_missing
    const-string v8, "không công bố"
    invoke-virtual {v4, v8}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    move-result-object v4
    :physical_start
    const-string v2, "\nPhysical camera IDs: "
    invoke-virtual {v4, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    move-result-object v4
    :try_start_physical
    invoke-virtual {v6}, Ljava/lang/Object;->getClass()Ljava/lang/Class;
    move-result-object v8
    const-string v9, "getPhysicalCameraIds"
    const/4 v10, 0x0
    new-array v10, v10, [Ljava/lang/Class;
    invoke-virtual {v8, v9, v10}, Ljava/lang/Class;->getMethod(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;
    move-result-object v8
    invoke-virtual {v8, v6, v10}, Ljava/lang/reflect/Method;->invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;
    move-result-object v8
    if-eqz v8, :physical_empty
    invoke-virtual {v8}, Ljava/lang/Object;->toString()Ljava/lang/String;
    move-result-object v8
    invoke-virtual {v4, v8}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    move-result-object v4
    goto :physical_end
    :physical_empty
    const-string v8, "không có / không được công bố"
    invoke-virtual {v4, v8}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    move-result-object v4
    :try_end_physical
    .catch Ljava/lang/Throwable; {:try_start_physical .. :try_end_physical} :physical_error
    goto :physical_end
    :physical_error
    move-exception v8
    const-string v8, "không hỗ trợ API hoặc không đọc được"
    invoke-virtual {v4, v8}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    move-result-object v4
    :physical_end
    const-string v2, "\n\n"
    invoke-virtual {v4, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    move-result-object v4
    add-int/lit8 v3, v3, 0x1
    goto :loop
    :done
    invoke-virtual {v4}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;
    move-result-object v0
    return-object v0
    :no_manager
    const-string v0, "CameraManager không khả dụng trên thiết bị này."
    return-object v0
.end method

.method public static show(Landroid/content/Context;)V
    .locals 5
    :try_start_show
    invoke-static {p0}, Lcom/xcam/offline/CameraIdScanner;->scan(Landroid/content/Context;)Ljava/lang/String;
    move-result-object v0
    :try_end_show
    .catch Ljava/lang/Throwable; {:try_start_show .. :try_end_show} :scan_error
    goto :show_dialog
    :scan_error
    move-exception v0
    new-instance v1, Ljava/lang/StringBuilder;
    invoke-direct {v1}, Ljava/lang/StringBuilder;-><init>()V
    const-string v2, "Không thể quét camera: "
    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    move-result-object v1
    invoke-virtual {v0}, Ljava/lang/Object;->getClass()Ljava/lang/Class;
    move-result-object v2
    invoke-virtual {v2}, Ljava/lang/Class;->getSimpleName()Ljava/lang/String;
    move-result-object v2
    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    move-result-object v1
    const-string v2, ": "
    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    move-result-object v1
    invoke-virtual {v0}, Ljava/lang/Throwable;->getMessage()Ljava/lang/String;
    move-result-object v0
    if-eqz v0, :error_no_message
    invoke-virtual {v1, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    move-result-object v1
    goto :error_done
    :error_no_message
    const-string v0, "không rõ nguyên nhân"
    invoke-virtual {v1, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    move-result-object v1
    :error_done
    invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;
    move-result-object v0
    :show_dialog
    new-instance v1, Landroid/widget/TextView;
    invoke-direct {v1, p0}, Landroid/widget/TextView;-><init>(Landroid/content/Context;)V
    invoke-virtual {v1, v0}, Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V
    const/4 v2, -0x1
    invoke-virtual {v1, v2}, Landroid/widget/TextView;->setTextColor(I)V
    const/high16 v2, 0x41600000
    invoke-virtual {v1, v2}, Landroid/widget/TextView;->setTextSize(F)V
    const/16 v2, 0x1c
    const/16 v3, 0x14
    invoke-virtual {v1, v2, v3, v2, v3}, Landroid/widget/TextView;->setPadding(IIII)V
    const/4 v2, 0x1
    invoke-virtual {v1, v2}, Landroid/widget/TextView;->setTextIsSelectable(Z)V
    new-instance v2, Landroid/widget/ScrollView;
    invoke-direct {v2, p0}, Landroid/widget/ScrollView;-><init>(Landroid/content/Context;)V
    invoke-virtual {v2, v1}, Landroid/widget/ScrollView;->addView(Landroid/view/View;)V
    new-instance v1, Landroid/app/AlertDialog$Builder;
    invoke-direct {v1, p0}, Landroid/app/AlertDialog$Builder;-><init>(Landroid/content/Context;)V
    const-string v3, "Camera ID Scanner"
    invoke-virtual {v1, v3}, Landroid/app/AlertDialog$Builder;->setTitle(Ljava/lang/CharSequence;)Landroid/app/AlertDialog$Builder;
    move-result-object v1
    invoke-virtual {v1, v2}, Landroid/app/AlertDialog$Builder;->setView(Landroid/view/View;)Landroid/app/AlertDialog$Builder;
    move-result-object v1
    const-string v2, "Đóng"
    const/4 v3, 0x0
    invoke-virtual {v1, v2, v3}, Landroid/app/AlertDialog$Builder;->setPositiveButton(Ljava/lang/CharSequence;Landroid/content/DialogInterface$OnClickListener;)Landroid/app/AlertDialog$Builder;
    move-result-object v1
    invoke-virtual {v1}, Landroid/app/AlertDialog$Builder;->show()Landroid/app/AlertDialog;
    return-void
.end method
