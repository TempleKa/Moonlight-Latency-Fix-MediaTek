# Moonlight Latency Fix

**Experimental Android utility for Moonlight game streaming, developed and tested on a MediaTek Helio G99 device.**

Moonlight Latency Fix attempts to improve decoding latency and frame pacing by keeping **CPU 0** periodically active. It uses a foreground service to run a small CPU-affined shell loop, without requiring root access.

> [!IMPORTANT]
> This is an experimental workaround, not an official Moonlight feature. It does not modify Moonlight, and lower latency is **not guaranteed**. Compatibility and results depend on the device, Android build, CPU scheduler, and power-management behavior.

## Features

- Three selectable CPU 0 activity modes
- Start, Stop, and mode selection from the Android notification
- Foreground service that continues while Moonlight is open
- Dark interface with clear running/stopped states
- No root required on devices that permit the underlying commands
- No network access or Moonlight integration required by the optimization loop

## Optimization modes

| Mode | CPU 0 workload | Notes |
| --- | --- | --- |
| **Normal** | `sleep 0.01` (10 ms) in a loop | Original low-activity mode |
| **Boost** | `sleep 0.001` (1 ms) in a loop | More frequent wake-ups |
| **Compute Boost** | 1,000 shell arithmetic increments, then `sleep 0.01` (10 ms), repeated | Adds computation between sleeps |

Only one mode runs at a time. **Stop optimization before changing modes.**

These are *requested* sleep intervals, not guaranteed scheduling periods or fixed CPU-load percentages. The 1,000-iteration workload is an experimental implementation and is not guaranteed to reproduce results from earlier manual tests.

## Compatibility

The project targets **Android 8.0+ (API 26+)** at the application level. This is a build requirement, **not a guarantee** that the optimization works on every Android 8.0+ device.

The mechanism is not MediaTek-specific. It may work on other MediaTek chipsets and on Snapdragon, Exynos, Tensor, or other Android devices, but those platforms have not been verified.

The device must provide a usable `taskset` command and allow the app's worker process to execute on CPU 0. Android CPU affinity restrictions, background policies, and vendor power management may prevent operation or limit its effect.

### Tested device

| Item | Configuration |
| --- | --- |
| Device | Samsung Galaxy Tab A11 (SM-X130) |
| Chipset | MediaTek Helio G99 |
| Android | Android 16 |
| Streaming client | Moonlight |

This is the development/test device, **not an exclusive compatibility list**. Report results from other devices in GitHub Issues, ideally including the device model, chipset, Android version, mode used, and measured latency.

## Installation

### From a GitHub Release

1. Open this repository's **Releases** page.
2. Download the APK attached to a published release, if available.
3. Install the APK on your Android device (you may need to allow installation from that source).
4. Open **Moonlight Latency Fix** and allow notification permission if prompted. The app waits for the permission result before initializing its notification. You can then start optimization normally.

**Note:** This source repository does not automatically include a prebuilt APK. A release must be created and an APK uploaded separately.

### Build from source

1. Open the Android Studio project in **Android Studio**.
2. Use **JDK 17** and install **Android SDK Platform 35**.
3. Allow Gradle to synchronize. The project uses Android Gradle Plugin 8.7.3.
4. Choose **Build → Build APK(s)**.
5. Find the debug build at `app/build/outputs/apk/debug/app-debug.apk`.

To install with ADB from PowerShell, run this from the directory containing `adb.exe` (adjust the APK path):

```powershell
.\adb install -r "path\to\app-debug.apk"
```

For public distribution, build and sign a **release APK** using a private signing key. Never commit signing keys or passwords.

## How to use

1. Open the app and grant notification permission when prompted. Once permission is granted, the app initializes its persistent notification with controls.
2. Select **Normal**, **Boost**, or **Compute Boost**.
3. Tap **Start optimization**.
4. Open Moonlight and compare decoding latency and perceived frame pacing using consistent stream settings.
5. Tap **Stop** to stop the CPU worker.

### Notification controls

- **Mode:** cycles Normal → Boost → Compute Boost → Normal, while optimization is stopped.
- **Start / Stop:** toggles the currently selected optimization.
- **Exit:** stops the worker and removes the foreground notification.
- **Tap notification:** opens the app.

The notification remains present after **Stop**, so optimization can be restarted without reopening the app. Use **Exit** to remove it. Android may collapse or rearrange notification actions depending on the device.

## Limitations and safety

- Lower *average decoding latency* does not necessarily mean smoother frame pacing; evaluate both.
- Experimental modes can increase CPU activity, battery use, and device temperature.
- The CPU worker may fail if `taskset`, CPU affinity, or the requested scheduling behavior is restricted.
- Android may stop or restrict foreground services under some conditions.
- The notification reports the worker's state, **not proof** that Moonlight performance improved.
- Google Play distribution may require additional review and justification for the foreground service type (`specialUse`).

## Project status

**Experimental / community testing welcome.** Currently verified only on the device listed above. Results from other chipsets are especially useful.

## License

No open-source license has been assigned in this repository yet. If you want to allow reuse and modification, add a `LICENSE` file (for example, MIT) after confirming you have the rights to license the code. Publishing source code publicly does not by itself grant others permission to reuse it.
