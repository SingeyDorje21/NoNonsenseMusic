# Run the Android app on Pixel 10 Pro

Run these commands in a normal PowerShell window from the repository root.

The Android SDK and emulator data on this computer are stored at `D:\Android\Sdk` and `D:\Android\User`. The old paths on C: are junctions to those folders.

```powershell
$flutter = 'C:\Users\SINGEY~1\develop\flutter\bin\flutter.bat'
$adb = 'D:\Android\Sdk\platform-tools\adb.exe'
$emulator = 'D:\Android\Sdk\emulator\emulator.exe'

& $emulator -avd Pixel_10_Pro
```

When the emulator has finished booting, open another PowerShell window in the repository root:

```powershell
$flutter = 'C:\Users\SINGEY~1\develop\flutter\bin\flutter.bat'
cd app
& $flutter pub get
& $flutter run -d emulator-5554
```

The app defaults to `https://incidents-navy-objectives-database.trycloudflare.com`. This temporary tunnel must be running for sign-in and music to work. You can change its address on the sign-in screen. To use another address for a build, pass `--dart-define="SERVER=https://your-server.example"` to `flutter run` or `flutter build apk`.

To build and install a debug APK instead of using `flutter run`:

```powershell
$flutter = 'C:\Users\SINGEY~1\develop\flutter\bin\flutter.bat'
$adb = 'D:\Android\Sdk\platform-tools\adb.exe'
cd app
& $flutter build apk --debug
& $adb install -r .\build\app\outputs\flutter-apk\app-debug.apk
& $adb shell am start -n dev.nononsense.nononsense/.MainActivity
```

The `SINGEY~1` path avoids a Flutter native asset build issue with spaces in the SDK path. On another computer, use that computer's Flutter SDK path.
