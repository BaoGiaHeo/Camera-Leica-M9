import 'package:camerawesome/camerawesome_plugin.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

const Color _ink = Color(0xFF090909);
const Color _panel = Color(0xFF171717);
const Color _silver = Color(0xFFE5E5E5);
const Color _muted = Color(0xFF9A9A9A);
const Color _red = Color(0xFFC62828);

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  // Let Android rotate the complete interface with the device orientation.
  SystemChrome.setPreferredOrientations(const [
    DeviceOrientation.portraitUp,
    DeviceOrientation.portraitDown,
    DeviceOrientation.landscapeLeft,
    DeviceOrientation.landscapeRight,
  ]);
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
            buttonBuilder: (child, onTap) => ClipOval(
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
            ),
          ),
        ),
        topActionsBuilder: (state) => AwesomeTopActions(
          state: state,
          padding: const EdgeInsets.fromLTRB(16, 10, 12, 8),
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
            IconButton(
              tooltip: 'Settings',
              onPressed: () => Navigator.of(context).push(
                MaterialPageRoute(builder: (_) => const CameraOptionsScreen()),
              ),
              icon: const Icon(Icons.tune_rounded, color: _silver, size: 25),
              style: IconButton.styleFrom(
                backgroundColor: const Color(0xFF242424),
                side: const BorderSide(color: Color(0xFF686868)),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(9),
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
                color: const Color(0xF20D0D0D),
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
            onSwitchTap: (cameraState) => cameraState.switchCameraSensor(
              aspectRatio: cameraState.sensorConfig.aspectRatio,
            ),
          ),
        ),
      ),
    );
  }
}

class CameraOptionsScreen extends StatefulWidget {
  const CameraOptionsScreen({super.key});

  @override
  State<CameraOptionsScreen> createState() => _CameraOptionsScreenState();
}

class _CameraOptionsScreenState extends State<CameraOptionsScreen> {
  String _ratio = '4:3';
  String _stabilization = 'OIS';

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        backgroundColor: _ink,
        foregroundColor: _silver,
        title: const Text(
          'CAMERA OPTIONS',
          style: TextStyle(fontSize: 15, fontWeight: FontWeight.w800, letterSpacing: 1.7),
        ),
      ),
      body: OrientationBuilder(
        builder: (context, orientation) {
          final wide = orientation == Orientation.landscape;
          return SafeArea(
            child: SingleChildScrollView(
              padding: const EdgeInsets.fromLTRB(16, 8, 16, 24),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  _sectionLabel('CAPTURE FORMAT'),
                  _optionPanel(
                    icon: Icons.aspect_ratio_rounded,
                    title: 'Screen Ratio',
                    subtitle: 'Choose the framing for your composition',
                    child: Wrap(
                      spacing: 8,
                      runSpacing: 8,
                      children: [
                        for (final ratio in const ['4:3', '16:9', '3:2', '1:1', 'MAX'])
                          _choice(ratio, _ratio == ratio, () => setState(() => _ratio = ratio)),
                      ],
                    ),
                  ),
                  const SizedBox(height: 14),
                  _sectionLabel('VIEWFINDER'),
                  _optionPanel(
                    icon: Icons.videocamera_outlined,
                    title: 'Preview Stabilization',
                    subtitle: 'Select a stabilization preference',
                    child: Wrap(
                      spacing: 8,
                      runSpacing: 8,
                      children: [
                        for (final mode in const ['OIS', 'EIS', 'EIS+'])
                          _choice(mode, _stabilization == mode,
                              () => setState(() => _stabilization = mode)),
                      ],
                    ),
                  ),
                  const SizedBox(height: 14),
                  _sectionLabel('LIBRARY'),
                  _navigationPanel(
                    icon: Icons.tune_rounded,
                    title: 'Contents Management',
                    subtitle: 'Manage film looks and framing overlays',
                    onTap: () => Navigator.of(context).push(
                      MaterialPageRoute(builder: (_) => const ContentsManagementScreen()),
                    ),
                    height: wide ? 104 : 112,
                  ),
                  const SizedBox(height: 14),
                  _sectionLabel('SYSTEM'),
                  _navigationPanel(
                    icon: Icons.settings_rounded,
                    title: 'Settings',
                    subtitle: 'Open the main application settings',
                    onTap: () => Navigator.of(context).push(
                      MaterialPageRoute(builder: (_) => const MainSettingsScreen()),
                    ),
                    height: wide ? 104 : 112,
                  ),
                  const SizedBox(height: 18),
                  Text(
                    'Selected ratio: $_ratio    ·    Stabilization: $_stabilization',
                    textAlign: TextAlign.center,
                    style: const TextStyle(color: _muted, fontSize: 10, letterSpacing: 0.5),
                  ),
                ],
              ),
            ),
          );
        },
      ),
    );
  }
}

class ContentsManagementScreen extends StatelessWidget {
  const ContentsManagementScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: _subPageBar('CONTENTS MANAGEMENT'),
      body: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          children: [
            _navigationPanel(
              icon: Icons.color_lens_outlined,
              title: 'LUT Manage',
              subtitle: 'Organize and manage LUT color profiles',
              height: 112,
              onTap: () => Navigator.of(context).push(
                MaterialPageRoute(builder: (_) => const ManagementDetailScreen(
                  title: 'LUT MANAGE',
                  description: 'LUT profiles will be listed here when LUT import and storage are connected.',
                  icon: Icons.color_lens_outlined,
                )),
              ),
            ),
            const SizedBox(height: 14),
            _navigationPanel(
              icon: Icons.crop_free_rounded,
              title: 'Frame Manage',
              subtitle: 'Manage frame guides and overlays',
              height: 112,
              onTap: () => Navigator.of(context).push(
                MaterialPageRoute(builder: (_) => const ManagementDetailScreen(
                  title: 'FRAME MANAGE',
                  description: 'Frame overlays will be listed here when frame import and storage are connected.',
                  icon: Icons.crop_free_rounded,
                )),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class MainSettingsScreen extends StatelessWidget {
  const MainSettingsScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: _subPageBar('SETTINGS'),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: const [
          _SettingsInfoTile(
            icon: Icons.high_quality_outlined,
            title: 'Image Quality',
            subtitle: 'Image quality preferences',
          ),
          _SettingsInfoTile(
            icon: Icons.tune_rounded,
            title: 'Camera Controls',
            subtitle: 'Camera behavior and control preferences',
          ),
          _SettingsInfoTile(
            icon: Icons.info_outline_rounded,
            title: 'About',
            subtitle: 'Fujifilm Mode · app-debug',
          ),
        ],
      ),
    );
  }
}

class ManagementDetailScreen extends StatelessWidget {
  final String title;
  final String description;
  final IconData icon;

  const ManagementDetailScreen({
    super.key,
    required this.title,
    required this.description,
    required this.icon,
  });

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: _subPageBar(title),
      body: Center(
        child: Padding(
          padding: const EdgeInsets.all(28),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(icon, color: _silver, size: 42),
              const SizedBox(height: 16),
              Text(description, textAlign: TextAlign.center,
                  style: const TextStyle(color: _muted, height: 1.5)),
            ],
          ),
        ),
      ),
    );
  }
}

class _SettingsInfoTile extends StatelessWidget {
  final IconData icon;
  final String title;
  final String subtitle;

  const _SettingsInfoTile({
    required this.icon,
    required this.title,
    required this.subtitle,
  });

  @override
  Widget build(BuildContext context) => Container(
    margin: const EdgeInsets.only(bottom: 10),
    decoration: _panelDecoration(),
    child: ListTile(
      leading: Icon(icon, color: _silver),
      title: Text(title, style: const TextStyle(color: _silver, fontWeight: FontWeight.w700)),
      subtitle: Text(subtitle, style: const TextStyle(color: _muted, fontSize: 12)),
      trailing: const Icon(Icons.chevron_right, color: _muted),
    ),
  );
}

PreferredSizeWidget _subPageBar(String title) => AppBar(
  backgroundColor: _ink,
  foregroundColor: _silver,
  title: Text(title, style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w800, letterSpacing: 1.5)),
);

Widget _sectionLabel(String text) => Padding(
  padding: const EdgeInsets.only(left: 4, bottom: 8),
  child: Text(text, style: const TextStyle(color: _muted, fontSize: 10, fontWeight: FontWeight.w800, letterSpacing: 1.8)),
);

BoxDecoration _panelDecoration() => BoxDecoration(
  color: _panel,
  borderRadius: BorderRadius.circular(13),
  border: Border.all(color: const Color(0xFF484848)),
);

Widget _optionPanel({
  required IconData icon,
  required String title,
  required String subtitle,
  required Widget child,
}) => Container(
  padding: const EdgeInsets.all(14),
  decoration: _panelDecoration(),
  child: Column(
    crossAxisAlignment: CrossAxisAlignment.start,
    children: [
      Row(children: [
        Icon(icon, color: _silver, size: 21),
        const SizedBox(width: 10),
        Expanded(child: Text(title, style: const TextStyle(color: _silver, fontSize: 14, fontWeight: FontWeight.w800))),
      ]),
      const SizedBox(height: 5),
      Text(subtitle, style: const TextStyle(color: _muted, fontSize: 11)),
      const SizedBox(height: 13),
      child,
    ],
  ),
);

Widget _choice(String label, bool selected, VoidCallback onTap) => ChoiceChip(
  label: Text(label, style: TextStyle(
    color: selected ? Colors.white : _silver,
    fontWeight: FontWeight.w700,
    letterSpacing: 0.5,
  )),
  selected: selected,
  onSelected: (_) => onTap(),
  selectedColor: _red,
  backgroundColor: const Color(0xFF252525),
  side: BorderSide(color: selected ? _red : const Color(0xFF666666)),
  showCheckmark: false,
);

Widget _navigationPanel({
  required IconData icon,
  required String title,
  required String subtitle,
  required VoidCallback onTap,
  double height = 112,
}) => SizedBox(
  height: height,
  child: Material(
    color: _panel,
    borderRadius: BorderRadius.circular(13),
    child: InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(13),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
        decoration: _panelDecoration(),
        child: Row(
          children: [
            Container(
              width: 44,
              height: 44,
              decoration: BoxDecoration(color: const Color(0xFF292929), borderRadius: BorderRadius.circular(11)),
              child: Icon(icon, color: _silver, size: 23),
            ),
            const SizedBox(width: 14),
            Expanded(
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(title, style: const TextStyle(color: _silver, fontSize: 14, fontWeight: FontWeight.w800)),
                  const SizedBox(height: 4),
                  Text(subtitle, maxLines: 2, overflow: TextOverflow.ellipsis,
                      style: const TextStyle(color: _muted, fontSize: 11)),
                ],
              ),
            ),
            const Icon(Icons.chevron_right_rounded, color: _muted, size: 23),
          ],
        ),
      ),
    ),
  ),
);
