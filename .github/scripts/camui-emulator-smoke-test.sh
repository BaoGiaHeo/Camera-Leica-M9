#!/usr/bin/env bash
set -eu
mkdir -p emulator-artifacts
: > emulator-artifacts/command-status.txt

timeout 60s adb wait-for-device > emulator-artifacts/adb-wait.txt 2>&1
echo "wait_for_device=0" >> emulator-artifacts/command-status.txt

# adb wait-for-device can return before PackageManager/ActivityManager are ready.
ready=0
for i in $(seq 1 90); do
  boot="$(adb shell getprop sys.boot_completed 2>&1 | tr -d '\r' || true)"
  package_service="$(adb shell service check package 2>&1 | tr -d '\r' || true)"
  activity_service="$(adb shell service check activity 2>&1 | tr -d '\r' || true)"
  printf 'attempt=%s boot=%s package=%s activity=%s\n' "$i" "$boot" "$package_service" "$activity_service" >> emulator-artifacts/boot-readiness.txt
  if [ "$boot" = "1" ] && printf "%s" "$package_service" | grep -q found && printf "%s" "$activity_service" | grep -q found; then
    ready=1
    break
  fi
  sleep 4
done

adb shell getprop > emulator-artifacts/getprop.txt 2>&1 || true
adb shell service list > emulator-artifacts/services.txt 2>&1 || true
if [ "$ready" -ne 1 ]; then
  echo "Android framework services did not become ready; app was not installed/launched." > emulator-artifacts/boot-failure.txt
  adb logcat -b all -d -v threadtime -t 15000 > emulator-artifacts/logcat-boot-failure.txt 2>&1 || true
  adb shell ps -A > emulator-artifacts/processes-boot-failure.txt 2>&1 || true
  echo "framework_ready=0" >> emulator-artifacts/command-status.txt
  exit 21
fi
echo "framework_ready=1" >> emulator-artifacts/command-status.txt

adb install -r CamUI-src/app/build/outputs/apk/debug/app-debug.apk > emulator-artifacts/install.txt 2>&1
echo "install=0" >> emulator-artifacts/command-status.txt
adb shell pm grant com.fujifilm.mode.debug android.permission.CAMERA > emulator-artifacts/permission.txt 2>&1

# Bypass the one-time language/storage/publisher introduction so this smoke run
# exercises the actual camera viewfinder on a clean debug installation.
printf '%s\n' \
  "<?xml version='1.0' encoding='utf-8' standalone='yes' ?>" \
  "<map><boolean name='first_activation_completed' value='true' /></map>" \
  | adb shell "run-as com.fujifilm.mode.debug sh -c 'mkdir -p shared_prefs && cat > shared_prefs/fujifilm_settings.xml'"
adb shell run-as com.fujifilm.mode.debug cat shared_prefs/fujifilm_settings.xml > emulator-artifacts/seeded-preferences.txt
echo "first_activation_bypassed=1" >> emulator-artifacts/command-status.txt

# Force the first landscape side for deterministic camera layout verification.
adb shell settings put system accelerometer_rotation 0
adb shell settings put system user_rotation 1
adb logcat -c
adb shell am force-stop com.fujifilm.mode.debug > emulator-artifacts/force-stop.txt 2>&1
adb shell am start -W -n com.fujifilm.mode.debug/com.example.camui.MainActivity > emulator-artifacts/start-result.txt 2>&1 || true
echo "am_start_command_completed=1" >> emulator-artifacts/command-status.txt

# ART verification and first Compose class loading are slow on this small CI emulator.
# Wait up to 150 seconds for ActivityTaskManager to confirm the actual first frame.
startup_ready=0
for i in $(seq 1 75); do
  if adb logcat -d -v brief -s ActivityTaskManager:I 2>/dev/null | grep -q "Displayed com.fujifilm.mode.debug/com.example.camui.MainActivity"; then
    startup_ready=1
    break
  fi
  if [ -z "$(adb shell pidof com.fujifilm.mode.debug 2>/dev/null | tr -d '\r')" ]; then
    break
  fi
  sleep 2
done
if [ "$startup_ready" -ne 1 ]; then
  echo "activity_not_displayed_within_150s=1" >> emulator-artifacts/command-status.txt
  adb shell pidof com.fujifilm.mode.debug > emulator-artifacts/app-pid.txt 2>&1 || true
  adb logcat -d -v threadtime -t 16000 > emulator-artifacts/logcat-startup-timeout.txt 2>&1 || true
  exit 26
fi
echo "activity_displayed=1" >> emulator-artifacts/command-status.txt

# Dismiss Android's first-use immersive-mode confirmation if it is present.
adb shell uiautomator dump /sdcard/fuji-window.xml > emulator-artifacts/immersive-uiautomator.txt 2>&1 || true
adb shell cat /sdcard/fuji-window.xml > emulator-artifacts/immersive-window.xml 2>&1 || true
python - <<'PY'
import re
import xml.etree.ElementTree as ET
path = "emulator-artifacts/immersive-window.xml"
try:
    root = ET.parse(path).getroot()
except Exception:
    root = None
point = None
if root is not None:
    for node in root.iter("node"):
        if node.attrib.get("text", "").strip() == "Got it":
            match = re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", node.attrib.get("bounds", ""))
            if match:
                x1, y1, x2, y2 = map(int, match.groups())
                point = ((x1 + x2) // 2, (y1 + y2) // 2)
                break
if point:
    with open("emulator-artifacts/immersive-dismiss-point.txt", "w") as stream:
        stream.write(f"{point[0]} {point[1]}")
PY
if [ -s emulator-artifacts/immersive-dismiss-point.txt ]; then
  read immersive_x immersive_y < emulator-artifacts/immersive-dismiss-point.txt
  adb shell input tap "$immersive_x" "$immersive_y"
  echo "immersive_notice_dismissed=1" >> emulator-artifacts/command-status.txt
else
  echo "immersive_notice_absent=1" >> emulator-artifacts/command-status.txt
fi
sleep 10

adb shell pidof com.fujifilm.mode.debug > emulator-artifacts/app-pid.txt 2>&1 || true
adb shell dumpsys activity activities > emulator-artifacts/activity-landscape-90.txt 2>&1 || true
adb shell uiautomator dump /sdcard/fuji-viewfinder-90.xml > emulator-artifacts/viewfinder-uiautomator-90.txt 2>&1 || true
adb shell cat /sdcard/fuji-viewfinder-90.xml > emulator-artifacts/viewfinder-window-90.xml 2>&1 || true
adb exec-out screencap -p > emulator-artifacts/camera-landscape-90.png 2> emulator-artifacts/screencap-landscape-90-error.txt || true
adb logcat -d -v threadtime -t 12000 > emulator-artifacts/logcat-landscape-90.txt 2>&1 || true

# Rotate to the opposite landscape side. The app remains landscape-only; this
# checks left/right control placement and cutout handling at both 90° and 270°.
adb shell settings put system user_rotation 3
sleep 5
adb shell dumpsys activity activities > emulator-artifacts/activity-landscape-270.txt 2>&1 || true
adb shell uiautomator dump /sdcard/fuji-viewfinder-270.xml > emulator-artifacts/viewfinder-uiautomator-270.txt 2>&1 || true
adb shell cat /sdcard/fuji-viewfinder-270.xml > emulator-artifacts/viewfinder-window-270.xml 2>&1 || true
adb exec-out screencap -p > emulator-artifacts/camera-landscape-270.png 2> emulator-artifacts/screencap-landscape-270-error.txt || true
adb shell pidof com.fujifilm.mode.debug > emulator-artifacts/app-pid-landscape.txt 2>&1 || true
adb logcat -d -v threadtime -t 16000 > emulator-artifacts/logcat-landscape-270.txt 2>&1 || true

# Keep raw logs even when startup fails; report app crash signatures separately.
app_pid="$(tr -d '\r\n ' < emulator-artifacts/app-pid.txt)"
if grep -Eq 'FATAL EXCEPTION|am_crash|Process: com\.fujifilm\.mode\.debug' emulator-artifacts/logcat-landscape.txt; then
  if grep -Eq 'Process: com\.fujifilm\.mode\.debug|am_crash.*com\.fujifilm\.mode\.debug' emulator-artifacts/logcat-landscape.txt; then
    echo "app_crash_signature=1" >> emulator-artifacts/command-status.txt
    exit 22
  fi
fi
if [ -n "$app_pid" ] && grep -Eq "Fatal signal [0-9]+.*pid $app_pid \(com\.fujifilm\.mode\.debug" emulator-artifacts/logcat-landscape.txt; then
  echo "app_native_crash_signature=1" >> emulator-artifacts/command-status.txt
  exit 22
fi
if [ ! -s emulator-artifacts/app-pid.txt ]; then
  echo "app_process_missing_after_launch=1" >> emulator-artifacts/command-status.txt
  exit 23
fi
echo "app_process_alive=1" >> emulator-artifacts/command-status.txt

# Run instrumentation only after the plain install + cold start was observed.
gradle --no-daemon -p CamUI-src :app:connectedDeveloperAndroidTest --stacktrace --console=plain

# End-to-end crash-report test: this trigger exists only in the debug source set.
adb shell am force-stop com.fujifilm.mode.debug
adb logcat -c
adb shell am start -W -n com.fujifilm.mode.debug/com.example.camui.CrashTestActivity > emulator-artifacts/crash-test-start.txt 2>&1
report_visible=0
for i in $(seq 1 20); do
  adb shell uiautomator dump /sdcard/crash-report-window.xml > emulator-artifacts/crash-test-uiautomator.txt 2>&1 || true
  adb shell cat /sdcard/crash-report-window.xml > emulator-artifacts/crash-report-window.xml 2>&1 || true
  if grep -q 'FujiFilm Mode đã gặp lỗi' emulator-artifacts/crash-report-window.xml && grep -q 'CRASH_REPORT_TEST_SENTINEL' emulator-artifacts/crash-report-window.xml; then
    report_visible=1
    break
  fi
  sleep 1
done
adb exec-out screencap -p > emulator-artifacts/crash-report-screen.png 2> emulator-artifacts/crash-report-screencap-error.txt || true
adb logcat -d -v threadtime -t 12000 > emulator-artifacts/logcat-crash-report.txt 2>&1 || true
if [ "$report_visible" -ne 1 ]; then
  echo "crash_report_screen_not_shown=1" >> emulator-artifacts/command-status.txt
  exit 24
fi
echo "crash_report_screen_shown=1" >> emulator-artifacts/command-status.txt

# Read the actual button bounds from the Android accessibility hierarchy.
python - <<'PY'
import re
import xml.etree.ElementTree as ET

root = ET.parse("emulator-artifacts/crash-report-window.xml").getroot()
buttons = {}
for node in root.iter("node"):
    label = node.attrib.get("text", "")
    if label in ("Sao chép log", "OK – Đóng ứng dụng"):
        match = re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", node.attrib.get("bounds", ""))
        if match:
            x1, y1, x2, y2 = map(int, match.groups())
            buttons[label] = ((x1 + x2) // 2, (y1 + y2) // 2)
required = ("Sao chép log", "OK – Đóng ứng dụng")
missing = [label for label in required if label not in buttons]
if missing:
    raise SystemExit("Crash report buttons not accessible: " + ", ".join(missing))
with open("emulator-artifacts/crash-report-button-centers.txt", "w") as out:
    out.write(" ".join(map(str, buttons[required[0]] + buttons[required[1]])))
PY
read copy_x copy_y ok_x ok_y < emulator-artifacts/crash-report-button-centers.txt
adb shell input tap "$copy_x" "$copy_y"
sleep 1
adb shell dumpsys clipboard > emulator-artifacts/clipboard-after-copy.txt 2>&1 || true
adb shell input tap "$ok_x" "$ok_y"

# OK must release the original process so its uncaught exception can terminate it.
main_process_exited=0
for i in $(seq 1 15); do
  if [ -z "$(adb shell pidof com.fujifilm.mode.debug 2>/dev/null | tr -d '\r')" ]; then
    main_process_exited=1
    break
  fi
  sleep 1
done
adb shell dumpsys activity activities > emulator-artifacts/activity-after-crash-report-ok.txt 2>&1 || true
adb logcat -d -v threadtime -t 20000 > emulator-artifacts/logcat-final.txt 2>&1 || true
if [ "$main_process_exited" -ne 1 ]; then
  echo "crash_report_ok_did_not_release_main_process=1" >> emulator-artifacts/command-status.txt
  exit 25
fi
echo "crash_report_ok_closed_main_process=1" >> emulator-artifacts/command-status.txt
