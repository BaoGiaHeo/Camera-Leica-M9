import 'package:camerawesome/camerawesome_plugin.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

const Color _ink = Color(0xFF090909);
const Color _panel = Color(0xF20D0D0D);
const Color _silver = Color(0xFFE5E5E5);
const Color _muted = Color(0xFF9A9A9A);
const Color _red = Color(0xFFC62828);

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  SystemChrome.setPreferredOrientations(const [DeviceOrientation.portraitUp]);
  SystemChrome.setSystemUIOverlayStyle(const SystemUiOverlayStyle(
    statusBarColor: _ink,
    statusBarIconBrightness: Brightness.light,
    systemNavigationBarColor: _ink,
    systemNavigationBarIconBrightness: Brightness.light,
  ));
  runApp(const FujifilmModeApp());
}

class FujifilmModeApp extends StatelessWidget {
  const FujifilmModeApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Fujifilm Mode (app-debug)',
      debugShowCheckedModeBanner: true,
      theme: ThemeData(
        brightness: Brightness.dark,
        scaffoldBackgroundColor: _ink,
        colorScheme: const ColorScheme.dark(
          primary: _silver,
          secondary: _red,
          surface: _panel,
        ),
        useMaterial3: true,
      ),
      home: const FujifilmCameraScreen(),
    );
  }
}

class FujifilmCameraScreen extends StatelessWidget {
  const FujifilmCameraScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: _ink,
      body: CameraAwesomeBuilder.awesome(
        saveConfig: SaveConfig.photo(),
        sensorConfig: SensorConfig.single(
          sensor: Sensor.position(SensorPosition.back),
          aspectRatio: CameraAspectRatios.ratio_4_3,
          flashMode: FlashMode.auto,
          zoom: 0.0,
        ),
        enablePhysicalButton: true,
        previewFit: CameraPreviewFit.cover,
        theme: AwesomeTheme(
          bottomActionsBackgroundColor: _panel,
          buttonTheme: AwesomeButtonTheme(
            backgroundColor: const Color(0xFF242424),
            foregroundColor: _silver,
            iconSize: 25,
            padding: const EdgeInsets.all(14),
            buttonBuilder: (child, onTap) {
              return ClipOval(
                child: Material(
                  color: const Color(0xFF242424),
                  shape: const CircleBorder(
                    side: BorderSide(color: Color(0xFF777777), width: 1),
                  ),
                  child: InkWell(
                    onTap: onTap,
                    splashColor: _red.withValues(alpha: 0.28),
                    highlightColor: _red.withValues(alpha: 0.12),
                    child: child,
                  ),
                ),
              );
            },
          ),
        ),
        topActionsBuilder: (state) => AwesomeTopActions(
          state: state,
          padding: const EdgeInsets.fromLTRB(16, 12, 16, 10),
          children: [
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                mainAxisSize: MainAxisSize.min,
                children: [
                  const Text(
                    'FUJIFILM',
                    style: TextStyle(
                      color: _silver,
                      fontSize: 19,
                      fontWeight: FontWeight.w800,
                      letterSpacing: 2.4,
                      height: 1,
                    ),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    'MODE  ·  DIGITAL CAMERA',
                    style: TextStyle(
                      color: _muted.withValues(alpha: 0.95),
                      fontSize: 9,
                      fontWeight: FontWeight.w600,
                      letterSpacing: 1.6,
                    ),
                  ),
                ],
              ),
            ),
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
              decoration: BoxDecoration(
                color: const Color(0xFF202020),
                borderRadius: BorderRadius.circular(7),
                border: Border.all(color: const Color(0xFF686868)),
              ),
              child: const Text(
                '4:3',
                style: TextStyle(
                  color: _silver,
                  fontSize: 11,
                  fontWeight: FontWeight.w700,
                  letterSpacing: 1,
                ),
              ),
            ),
          ],
        ),
        middleContentBuilder: (state) => Column(
          children: [
            const Spacer(),
            Container(
              margin: const EdgeInsets.symmetric(horizontal: 16),
              padding: const EdgeInsets.fromLTRB(12, 9, 12, 10),
              decoration: BoxDecoration(
                color: _panel,
                borderRadius: BorderRadius.circular(12),
                border: Border.all(color: const Color(0x44777777)),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Row(
                    children: [
                      Text(
                        'FILM SIMULATION',
                        style: TextStyle(
                          color: _silver,
                          fontSize: 10,
                          fontWeight: FontWeight.w800,
                          letterSpacing: 1.8,
                        ),
                      ),
                      Spacer(),
                      Text(
                        'LIVE PREVIEW',
                        style: TextStyle(
                          color: _muted,
                          fontSize: 8,
                          fontWeight: FontWeight.w600,
                          letterSpacing: 1.2,
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 7),
                  AwesomeFilterWidget(
                    state: state,
                    filterListPosition: FilterListPosition.aboveButton,
                    filterListPadding: const EdgeInsets.only(top: 4),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 12),
          ],
        ),
        bottomActionsBuilder: (state) => AwesomeBottomActions(
          state: state,
          left: AwesomeFlashButton(state: state),
          right: AwesomeCameraSwitchButton(
            state: state,
            scale: 1.0,
            onSwitchTap: (cameraState) {
              cameraState.switchCameraSensor(
                aspectRatio: cameraState.sensorConfig.aspectRatio,
              );
            },
          ),
        ),
      ),
    );
  }
}
