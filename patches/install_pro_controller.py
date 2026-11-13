from pathlib import Path
import os

root = Path(os.environ["PROJECT_DIR"])
main = root / "smali_classes3/com/xcam/offline/MainActivity.smali"
s = main.read_text()

ui = "    invoke-static {v6}, Lcom/xcam/offline/UiV111Feature;->install(Landroid/app/Activity;)V"
pro = "    invoke-static {v6}, Lcom/xcam/offline/ProCameraFeature;->install(Landroid/app/Activity;)V"
if pro not in s:
    if s.count(ui) != 1:
        raise SystemExit("Cannot locate camera UI install point")
    s = s.replace(ui, ui + "\n" + pro, 1)

preview = '    .local v14, "previewBuilder":Landroidx/camera/core/Preview$Builder;'
preview_call = "    invoke-static {v1, v14, v12}, Lcom/xcam/offline/ProCameraFeature;->configure(Landroid/app/Activity;Landroidx/camera/core/ExtendableBuilder;Landroid/hardware/camera2/CameraCharacteristics;)V"
if preview_call not in s:
    if s.count(preview) != 1:
        raise SystemExit("Cannot locate Preview.Builder")
    s = s.replace(preview, preview + "\n" + preview_call, 1)

capture = '    .local v9, "captureBuilder":Landroidx/camera/core/ImageCapture$Builder;'
capture_call = "    invoke-static {v1, v9, v12}, Lcom/xcam/offline/ProCameraFeature;->configure(Landroid/app/Activity;Landroidx/camera/core/ExtendableBuilder;Landroid/hardware/camera2/CameraCharacteristics;)V"
if capture_call not in s:
    if s.count(capture) != 1:
        raise SystemExit("Cannot locate ImageCapture.Builder")
    s = s.replace(capture, capture + "\n" + capture_call, 1)

start = s.index(".method private final focusAt(FF)V")
end = s.index(".end method", start)
block = s[start:end]
if ":manual_focus_return" not in block:
    anchor = '    .param p2, "y"    # F\n\n    .line 436'
    guard = """    # Manual focus bypasses tap-to-autofocus.
    const-string v0, "pro_camera"
    const/4 v1, 0x0
    invoke-virtual {p0, v0, v1}, Landroid/app/Activity;->getSharedPreferences(Ljava/lang/String;I)Landroid/content/SharedPreferences;
    move-result-object v0
    const-string v1, "focus_mode"
    const/4 v2, 0x0
    invoke-interface {v0, v1, v2}, Landroid/content/SharedPreferences;->getInt(Ljava/lang/String;I)I
    move-result v0
    if-nez v0, :manual_focus_return

    .line 436"""
    if block.count(anchor) != 1:
        raise SystemExit("Cannot locate focusAt insertion point")
    block = block.replace(anchor, '    .param p2, "y"    # F\n\n' + guard, 1)
    block += "\n    :manual_focus_return\n    return-void\n"
    s = s[:start] + block + s[end:]
main.write_text(s)

zoom_files = list(root.rglob("ZoomUiFeature.smali"))
if len(zoom_files) == 0:
    print("NOTICE: ZoomUiFeature.smali absent; skipping optional 2x UI patch.")
elif len(zoom_files) != 1:
    raise SystemExit(f"Expected at most one ZoomUiFeature.smali, found {len(zoom_files)}")
else:
    if len(zoom_files) != 1:
        raise SystemExit(f"Expected exactly one ZoomUiFeature.smali, found {len(zoom_files)}")
    zoom = zoom_files[0]
    z = zoom.read_text()
    zstart = z.index(".method public static install(Landroid/app/Activity;)V")
    zend = z.index(".end method", zstart)
    zb = z[zstart:zend]
    if 'const-string v11, "2x"' not in zb:
        if "    .locals 12" not in zb:
            raise SystemExit("Unexpected zoom install register layout")
        zb = zb.replace("    .locals 12", "    .locals 14", 1)
        marker = '    const-string v10, "1\\u00d7"'
        if zb.count(marker) != 1:
            raise SystemExit("Cannot locate 1x zoom control")
        zb = zb.replace(marker, "    move v13, v8\n\n" + marker, 1)
        call = "invoke-static {p0, v10, v11, v0, v8}, Lcom/xcam/offline/ZoomUiFeature;->makeZoomButton"
        if zb.count(call) != 1:
            raise SystemExit("Cannot locate 1x button dimensions")
        zb = zb.replace(call, "invoke-static {p0, v10, v11, v0, v13}, Lcom/xcam/offline/ZoomUiFeature;->makeZoomButton", 1)
        anchor = "    move-result-object v8\n\n    .line 52"
        button = """    move-result-object v8
    
        const-string v10, "2\\u00d7"
        const-string v11, "2x"
        invoke-static {p0, v10, v11, v0, v13}, Lcom/xcam/offline/ZoomUiFeature;->makeZoomButton(Landroid/app/Activity;Ljava/lang/String;Ljava/lang/String;II)Landroid/widget/TextView;
        move-result-object v12
    
        .line 52"""
        if zb.count(anchor) != 1:
            raise SystemExit("Cannot locate zoom button insertion point")
        zb = zb.replace(anchor, button, 1)
        add = "    invoke-virtual {v6, v8}, Landroid/widget/LinearLayout;->addView(Landroid/view/View;)V"
        if zb.count(add) != 1:
            raise SystemExit("Cannot locate 1x view insertion point")
        zb = zb.replace(add, add + "\n    invoke-virtual {v6, v12}, Landroid/widget/LinearLayout;->addView(Landroid/view/View;)V", 1)
        old = "    invoke-interface {v0, v2}, Ljava/util/List;->add(Ljava/lang/Object;)Z"
        if zb.count(old) != 1:
            raise SystemExit("Cannot locate third zoom preset registration")
        zb = zb.replace(old, "    invoke-interface {v0, v12}, Ljava/util/List;->add(Ljava/lang/Object;)Z", 1)
        z = z[:zstart] + zb + z[zend:]
        zoom.write_text(z)
    if 'const-string v11, "2x"' not in zoom.read_text()[zoom.read_text().index(".method public static install(Landroid/app/Activity;)V"):]:
        raise SystemExit("2x zoom control was not written to the source")
    print("2x zoom control verified in " + str(zoom))
    if 'const-string v11, "2x"' not in zoom.read_text()[zoom.read_text().index(".method public static install(Landroid/app/Activity;)V"):]:
        raise SystemExit("2x zoom control was not written to the source")
    print("2x zoom control verified in " + str(zoom))
print("Camera2 Pro controller installation completed.")
