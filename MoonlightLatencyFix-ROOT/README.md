# Moonlight Latency Fix ROOT

**Experimental root-only Android utility for Moonlight game streaming, developed and tested on a Samsung Galaxy Tab A11 with a MediaTek Helio G99.**

Moonlight Latency Fix ROOT experiments with reducing decoding latency and improving frame pacing by adjusting CPU frequency limits, CPU governors, and display refresh rate. Unlike the non-root version, it changes system settings directly and **requires root access**.

> [!IMPORTANT]
> This is an experimental workaround, not an official Moonlight feature. It does not modify Moonlight itself, and lower decoding latency or smoother frame pacing is **not guaranteed**. Root access is required. Changes to CPU and display settings may affect stability, battery use, and temperature.

## About this project

I'm not a developer and have very limited programming knowledge. I built and tested this utility with the help of ChatGPT, which guided me through implementation, testing, debugging, and different optimization approaches.

The project grew out of experiments on my Samsung Galaxy Tab A11, which I bought primarily for streaming games from my PC. After trying a non-root workaround that periodically wakes CPU0, I wanted to explore whether direct CPU and refresh-rate controls could improve Moonlight decoding behavior further.

This ROOT version is a separate experimental approach. It does not use the same CPU0 wake-up loop as the non-root app. Some technical explanations may not be entirely accurate, and results on other devices are unknown. :)

## Features

- CPU BOOST toggle with device-tested minimum CPU frequencies
- Independent CPU governor controls: **Default** and **Performance**
- Display refresh-rate controls: **AUTO**, **60 Hz**, and **90 Hz**
- Persistent notification with quick controls
- Live display of CPU settings and active states
- **CLOSE NOTIFICATION & RESTORE DEFAULTS** to undo overrides and stop the background notification service
- Dark interface with active controls highlighted in green

## CPU and display controls

### CPU BOOST

On the tested Galaxy Tab A11 (SM-X130), the application uses these CPU frequency policies:

| CPU policy | Stock minimum | BOOST minimum | Maximum observed |
| --- | ---: | ---: | ---: |
| `policy0` (Little) | 500 MHz | 1800 MHz | 2000 MHz |
| `policy6` (Big) | 725 MHz | 1100 MHz | 2200 MHz |

BOOST changes the **minimum allowed frequency**, not the maximum. The actual CPU frequency can still rise or fall within the permitted range, depending on the governor and workload. The values above were verified on the development device and **must not be assumed to be appropriate for other models**.

### CPU governor

- **Default** selects the device's tested default governor, `sugov_ext`.
- **Performance** selects `performance` for both CPU policies.

Governor selection is independent of CPU BOOST. Switching governors does not automatically enable or disable BOOST.

### Display refresh rate

- **AUTO** removes the app's explicit `peak_refresh_rate` and `min_refresh_rate` overrides, allowing the system to manage refresh rate.
- **60 Hz** requests 60 Hz for both settings.
- **90 Hz** requests 90 Hz for both settings.

AUTO does **not** necessarily mean the screen is currently displaying 60 Hz. The tested device can operate at 90 Hz while the system is managing the refresh rate automatically.

## Compatibility

> [!WARNING]
> This version is specifically designed for the Samsung Galaxy Tab A11 (SM-X130) with the MediaTek Helio G99. It uses fixed CPU policy paths and BOOST frequencies that may not be supported by other devices. Do not assume compatibility with other MediaTek processors, even if they use a similar CPU architecture.

**Root access is mandatory.** The utility needs permission to write CPU frequency and governor controls and to modify Android refresh-rate settings.

### Tested device

| Item | Configuration |
| --- | --- |
| Device | Samsung Galaxy Tab A11 (SM-X130) |
| Chipset | MediaTek Helio G99 |
| Android | Android 16 |
| CPU policies | `policy0` and `policy6` |
| Default governor | `sugov_ext` |
| Display modes observed | 60 Hz and 90 Hz |
| Streaming client | Moonlight |

## Installation

### From a GitHub Release

1. Download the ROOT APK from a release. Make sure it is clearly labeled as the ROOT version.
2. Install it on a rooted Android device.
3. Open the app and grant root access only if you trust the application and understand what it changes.
4. Grant notification permission if Android requests it.

### Build from source

1. Open the `MoonlightLatencyFix-ROOT` project folder in Android Studio.
2. Install the Android SDK and JDK versions required by the project's Gradle configuration.
3. Allow Gradle to synchronize and choose **Build → Build APK(s)**.
4. Find the debug APK under `app/build/outputs/apk/debug/`.

## How to use

1. Open the ROOT application and grant the requested permissions.
2. Turn **CPU BOOST** on or off according to the behavior you want to test.
3. Choose **Default** or **Performance** governor independently of BOOST.
4. Select **AUTO**, **60 Hz**, or **90 Hz** for the display.
5. Start Moonlight and compare decoding latency, frame pacing, CPU temperature, and battery use with the same streaming settings.
6. When finished, select **CLOSE NOTIFICATION & RESTORE DEFAULTS**.

### Notification controls

The persistent notification provides quick access to optimization controls while Moonlight is running. The main screen exposes the full CPU, governor, and display settings.

**CLOSE NOTIFICATION & RESTORE DEFAULTS** restores the original CPU minimum frequencies, CPU governors, and refresh-rate settings, then stops the background service and removes the persistent notification. It does not necessarily close the app's visible screen.

## Limitations and safety

The controls change real system settings rather than merely affecting the app's own process. Higher minimum CPU frequencies and the `performance` governor can increase power consumption, temperature, or battery drain. Results depend on the device and streaming configuration. Lower average decoding latency does not always mean smoother frame pacing.

**WARNING:** Uninstalling or force-stopping the app while CPU BOOST or other overrides are active may leave modified settings in place. Always use **CLOSE NOTIFICATION & RESTORE DEFAULTS** before uninstalling the app. A reboot will usually reset kernel CPU-frequency and governor changes, but should not be relied upon to restore every Android display setting.

The app does not currently include an independent watchdog to restore settings if it is terminated unexpectedly. This has been left for a possible future update.

## Project status

**Experimental and still being tested.** CPU BOOST, governor switching, and CPU defaults restoration have been verified on the device listed above.

## License

No open-source license has been assigned in this repository yet. If you want to allow reuse and modification, add a `LICENSE` file after confirming you have the rights to license the code. Publishing source code publicly does not by itself grant others permission to reuse it.
