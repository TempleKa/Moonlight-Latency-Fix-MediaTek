# Moonlight Latency Fix

**Two experimental Android apps for Moonlight game streaming.** This repository contains both **Moonlight Latency Fix** (non-root) and **MoonlightLatencyFix-ROOT**. They target the same latency problem through **two fundamentally different optimization methods**. Development and manual testing have focused on the Samsung Galaxy Tab A11 (SM-X130), powered by the MediaTek Helio G99.

| Edition | Root required? | How it works |
| --- | --- | --- |
| **Moonlight Latency Fix (original)** | **No** | Periodically activates **CPU 0** using a small CPU-affined shell loop to influence scheduling and reduce decoding latency. Offers Normal, Boost, and Compute Boost activity modes. |
| **MoonlightLatencyFix-ROOT** | **Yes** | Directly adjusts the **minimum CPU frequencies** of the Little and Big clusters, with independent CPU governor and display refresh-rate controls. It does **not** use the original CPU 0 activity loops. |

**They are not just root and non-root builds of the same optimization.** The original app relies on periodic CPU activity, while the ROOT edition changes CPU frequency policy through privileged system interfaces. They are maintained **together in this repository**, use different Android package IDs, and can be installed side by side. Neither modifies Moonlight itself.

> [!IMPORTANT]
> Both editions are experimental workarounds, not official Moonlight features. Lower latency is **not guaranteed**. Results depend on the device, Android version, CPU scheduler, kernel, and power management. The ROOT edition naturally requires a rooted device.

## About this project

I'm not a developer and have very limited programming knowledge. I built and tested this utility with the help of ChatGPT, which guided me through implementation, testing, debugging, and different optimization approaches.

The project grew out of experiments on my Samsung Galaxy Tab A11, which I bought primarily for streaming games from my PC. After trying a non-root workaround that periodically wakes CPU0, I wanted to explore whether direct CPU and refresh-rate controls could improve Moonlight decoding behavior further.

The ROOT version is a separate experimental approach. It does not use the same CPU0 wake-up loop as the non-root app. This edition is currently designed specifically for the Samsung Galaxy Tab A11 (SM-X130) with the MediaTek Helio G99. Its CPU policies and frequency settings are not automatically adapted to other processors or devices.

I would eventually like to expand compatibility to other MediaTek devices by implementing automatic CPU policy detection and device-specific frequency handling. However, this is only a future goal, and the current version should not be considered compatible with other devices.

Some technical explanations may not be entirely accurate, and results on other devices are unknown. :)

## Repository layout

Both applications belong in **this same GitHub repository**, with a shared root `README.md` and separate Android Studio project directories:

```text
MoonlightLatencyFix/
├── README.md
├── MoonlightLatencyFix/         # Original non-root Android Studio project
└── MoonlightLatencyFix-ROOT/    # Root Android Studio project
```

Each directory contains its own Gradle configuration and Android application. Open the appropriate **subdirectory** in Android Studio to build that edition. The apps are independent at runtime, but **not separate GitHub repositories**.

## Choose an edition

### Non-root edition
**Moonlight Latency Fix v1.0.0**

The original experimental utility that periodically wakes CPU0 to improve decoding latency and frame pacing. No root access required.

[**Download Non-root APK**](https://github.com/TempleKa/Moonlight-Latency-Fix-MediaTek/releases/download/v1.0.0/MoonlightLatencyFix.apk)

### ROOT edition (Experimental)
**Moonlight Latency Fix ROOT v1.0.0**

A separate experimental utility that directly controls CPU minimum frequencies, CPU governors, and display refresh rate. Root access is required.

> [!WARNING]
> The current ROOT version was developed and tested exclusively on the Samsung Galaxy Tab A11 (SM-X130, MediaTek Helio G99). CPU policies, BOOST frequencies, and refresh-rate controls are currently device-specific. Using this version on other devices is not recommended.

[**Download ROOT APK**](https://github.com/TempleKa/Moonlight-Latency-Fix-MediaTek/releases/download/root-v1.0.0/MoonlightLatencyFix-ROOT.apk)

## Original edition: Moonlight Latency Fix (no root)

The original app attempts to improve decoding latency and frame pacing by keeping **CPU 0** periodically active. It uses a foreground service to run a small CPU-affined shell loop, without requiring root access.

### Features

- Three selectable CPU 0 activity modes
- Start, Stop, and mode selection from the Android notification
- Foreground service that continues while Moonlight is open
- Dark interface with clear running/stopped states
- No root required on devices that permit the underlying commands
- No network access or Moonlight integration required by the optimization loop

### Optimization modes

| Mode | CPU 0 workload | Notes |
| --- | --- | --- |
| **Normal** | `sleep 0.01` (10 ms) in a loop | Original low-activity mode |
| **Boost** | `sleep 0.001` (1 ms) in a loop | More frequent wake-ups |
| **Compute Boost** | 1,000 shell arithmetic increments, then `sleep 0.01` (10 ms), repeated | Adds computation between sleeps |

Only one mode runs at a time. **Stop optimization before changing modes.**

These are *requested* sleep intervals, not guaranteed scheduling periods or fixed CPU-load percentages. The 1,000-iteration workload is an experimental implementation and is not guaranteed to reproduce results from earlier manual tests.

### Compatibility

The project targets **Android 8.0+ (API 26+)** at the application level. This is a build requirement, **not a guarantee** that the optimization works on every Android 8.0+ device.

The mechanism is not MediaTek and Samsung Galaxy Tab A11 specific. It may work on other chipsets, or other Android devices, but those platforms have not been verified.

The device must provide a usable `taskset` command and allow the app's worker process to execute on CPU 0. Android CPU affinity restrictions, background policies, and vendor power management may prevent operation or limit its effect.

#### Tested device

| Item | Configuration |
| --- | --- |
| Device | Samsung Galaxy Tab A11 (SM-X130) |
| Chipset | MediaTek Helio G99 |
| Android | Android 16 |
| Streaming client | Moonlight |

This is the development/test device, **not an exclusive compatibility list**. Report results from other devices in GitHub Issues, ideally including the device model, chipset, Android version, mode used, and measured latency.

### Installation

#### From a GitHub Release

1. Open this repository's **Releases** page.
2. Download the APK for the **non-root edition** attached to a published release.
3. Install the APK on your Android device (you may need to allow installation from that source).
4. Open **Moonlight Latency Fix** and allow notification permission if prompted. The app waits for the permission result before initializing its notification. You can then start optimization normally.

#### Build from source

1. Open the **`MoonlightLatencyFix/`** subdirectory of this repository in **Android Studio**.
2. Use **JDK 17** and install **Android SDK Platform 35**.
3. Allow Gradle to synchronize. The project uses Android Gradle Plugin 8.7.3.
4. Choose **Build → Build APK(s)**.
5. Find the debug build at `app/build/outputs/apk/debug/app-debug.apk`.

To install with ADB from PowerShell, run this from the directory containing `adb.exe` (adjust the APK path):

```powershell
.\adb install -r "path\to\app-debug.apk"
```

### How to use

1. Open the app and grant notification permission when prompted. Once permission is granted, the app initializes its persistent notification with controls.
2. Select **Normal**, **Boost**, or **Compute Boost**.
3. Tap **Start optimization**.
4. Open Moonlight and compare decoding latency and perceived frame pacing using consistent stream settings.
5. Tap **Stop** to stop the CPU worker.

#### Notification controls

- **Mode:** cycles Normal → Boost → Compute Boost → Normal, while optimization is stopped.
- **Start / Stop:** toggles the currently selected optimization.
- **Exit:** stops the worker and removes the foreground notification.
- **Tap notification:** opens the app.

The notification remains present after **Stop**, so optimization can be restarted without reopening the app and without quitting Moonlight stream by just swiping down from top. Use **Exit** to remove it. Android may collapse or rearrange notification actions depending on the device.

### Limitations and safety

- Lower *average decoding latency* does not necessarily mean smoother frame pacing; evaluate both.
- Experimental modes can increase CPU activity, battery use, and device temperature.
- The CPU worker may fail if `taskset`, CPU affinity, or the requested scheduling behavior is restricted.
- Android may stop or restrict foreground services under some conditions.
- The notification reports the worker's state, **not proof** that Moonlight performance improved.

## ROOT edition: MoonlightLatencyFix-ROOT

**MoonlightLatencyFix-ROOT** is a separate, experimental version for rooted Android devices. It uses CPU frequency controls rather than the CPU 0 activity loops used by the non-root application. The two editions have different Android package IDs and can be installed side by side. The ROOT edition requires working `su` access.

### ROOT edition features

- **CPU OFF / BOOST:** two mutually exclusive controls. The active control is disabled in the interface.
- **BOOST:** requests a minimum of **1,800,000 kHz (1.8 GHz)** for the Little cluster (`policy0`) and **1,100,000 kHz (1.1 GHz)** for the Big cluster (`policy6`). These are *minimums*, not fixed CPU clock speeds.
- **OFF:** restores the original minimum CPU frequencies saved by the ROOT edition.
- **CPU Governor:** independent **Default / Performance** selector. Default restores the originally captured governors; Performance is available only if supported by the kernel.
- **Display Refresh Rate:** independent **Auto / 60 Hz / 90 Hz** selector. Auto removes the two system refresh-rate overrides present in Galaxy Tab A11; 60 Hz and 90 Hz request matching minimum and peak rates.
- **Persistent notification:** custom two-row controls, **CPU: OFF / BOOST** and **Refresh: AUTO / 60 Hz / 90 Hz**, designed for changing settings without leaving a Moonlight session. The governor is changed inside the app.
- **CLOSE NOTIFICATION & RESTORE DEFAULTS:** restores the saved CPU minimums and governors, clears the refresh-rate overrides (Auto), stops the foreground service, and removes its persistent notification. The app screen may remain open.

The ROOT edition does **not** run the original Normal, Boost, or Compute Boost CPU 0 shell loops. The ROOT edition's **BOOST** refers specifically to raising the minimum CPU frequencies.

### ROOT edition: compatibility and testing

The current CPU paths and target frequencies were chosen for the **Samsung Galaxy Tab A11 (SM-X130), MediaTek Helio G99, Android 16**. Other devices may expose different CPU policies, frequency steps, governors, or display refresh rates. Do not assume the settings are portable without checking the device's kernel interfaces.

In informal Moonlight tests on the Tab A11, the default CPU minimums were associated with approximately **12–18 ms average decoding time**, while raised minimums were associated with approximately **1–2 ms** under the tested conditions. Two five-minute battery-gauge sampling runs measured approximately **2.411 W (default minimums)** and **2.416 W (1.8 GHz Little / 1.1 GHz Big)**. These are observations from a limited test, **not** proof of identical battery life or a guaranteed latency improvement.

During an approximately ten-minute run with raised CPU minimums, available sensors reported maximum temperatures of about **35.0°C (`ap_ntc`)**, **39.2°C (PMIC)**, and **28.7°C (battery)**. Direct CPU-core temperature readings were unavailable. Results may differ with workload, ambient temperature, screen brightness, and device condition.

### ROOT edition: installation and use

1. Open the **`MoonlightLatencyFix-ROOT/`** subdirectory of this same repository in Android Studio and build an APK.
2. Install and open the ROOT edition, grant root access through your root manager, and allow notifications when requested.
3. Select **CPU OFF** or **BOOST**, the desired **CPU Governor**, and **Auto / 60 Hz / 90 Hz** refresh rate.
4. Keep the foreground service active while streaming. Expand the notification to access its CPU and refresh controls without closing Moonlight.
5. Use **CLOSE NOTIFICATION & RESTORE DEFAULTS** when finished, and verify that the original settings were restored.

### ROOT edition: watchdog protection (planned)

The ROOT edition is intended to include an **independent root watchdog** as a safety fallback when the app or its foreground service stops unexpectedly. This is separate from the regular **CLOSE NOTIFICATION & RESTORE DEFAULTS** action.

- While the ROOT service is active, it periodically updates a **heartbeat** that the watchdog monitors.
- If the heartbeat stops for longer than a defined timeout, the watchdog attempts to restore the previously saved **CPU minimum frequencies**, **CPU governors**, and **display refresh-rate defaults**.
- A normal exit restores settings first and then stops the watchdog, avoiding unnecessary recovery actions.
- On the next app launch, the app also checks for an interrupted session and attempts recovery if needed.

**Implementation status:** The independent watchdog is **not yet included in the current ROOT source**. The existing recovery-on-next-launch behavior is not a substitute for it. Even after implementation, an Android **Force Stop**, reboot, or root-process termination may prevent immediate recovery.

### ROOT edition: important safety limitations

**Before uninstalling MoonlightLatencyFix-ROOT, use CLOSE NOTIFICATION & RESTORE DEFAULTS and verify that the CPU settings have been restored.** Android does not guarantee that the app can run cleanup code during uninstall. Removing it while BOOST is active may leave the modified CPU minimums in effect until a reboot or manual restoration. A reboot normally resets kernel CPU policies unless another root service reapplies them.

> [!WARNING]
> Root access can change system-level settings. Test the ROOT edition carefully on your own device before relying on it. Keep the kernel's thermal protections enabled.

- The app captures the original CPU minimum frequencies and governors for restoration. Changes made manually while it is running may be overwritten when exiting.
- **Refresh Auto deletes `peak_refresh_rate` and `min_refresh_rate` overrides.** It does not restore custom values that existed before launching the ROOT edition.
- A crash, Android **Force Stop**, or reboot cannot be guaranteed to execute cleanup code. The current source attempts recovery on the next launch after an interrupted session, but **does not implement an independent root watchdog** or guarantee immediate restoration after Force Stop.
- Android 16 / One UI may collapse, clip, or rearrange the custom notification. Verify both rows and all five controls on the actual device.
- The `performance` governor may be unavailable on some kernels and can increase power use or temperature. Switching governors can also affect CPU frequency minimums.
- CPU frequency writes and governor changes depend on the device and root permissions. If applying or restoring a setting fails, check the actual values before continuing.

## Project status

**Both editions are experimental.** The original non-root and ROOT approach were tested on the Samsung Galaxy Tab A11 described above.

## License

No open-source license has been assigned in this repository yet. If you want to allow reuse and modification, add a `LICENSE` file (for example, MIT) after confirming you have the rights to license the code. Publishing source code publicly does not by itself grant others permission to reuse it.
