---
name: run
description: Build the debug APK and install it on a connected device or emulator. Use when you want to test changes on a real device.
disable-model-invocation: true
---

Run the following command from the project root to build and install the debug APK:

```bash
./gradlew installDebug
```

Before running:
1. Confirm a device or emulator is connected (`adb devices` to check).
2. If the build fails, read the error output carefully — common causes are missing SDK components or a broken `libs.versions.toml` dependency.

After install, the app launches automatically on the device. Check logcat for runtime errors:

```bash
adb logcat -s WeatherApp
```
