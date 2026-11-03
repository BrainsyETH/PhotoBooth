# API 36 readiness

The app now compiles/targets API 36 with AGP 8.10.1 and Gradle 8.11.1. Any JDK 17+ works
(Android Studio's bundled JDK 21 locally, JDK 17 in CI); the Gradle daemon JDK is deliberately not pinned.
The API 36 large-screen compatibility opt-out preserves landscape kiosk behavior.
This is temporary: before targeting API 37, implement/test fully adaptive layouts.

## Automated checks

`bash gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug`

GitHub Actions runs these checks without release credentials. Event email-limit
regressions exercise real file-backed DataStore persistence, concurrent reservations,
address normalization, failure recovery and new-event isolation. The existing GIF
encoder tests also run. Debug validation is not a signed production AAB test.

## Physical tablet release gate (not yet completed)

- Install a signed release using `RELEASE_BUILD.md`; verify minified code and Play splits.
- Android 16 tablet: rotate, resize/multi-window, background/resume; check camera framing,
  edge-to-edge controls, keyboard, Back, and preservation/recovery of the current session.
- Android 8/9 if still supported: launch, system bars, storage permission, kiosk entry/exit.
- Run repeated Single/Collage/GIF sessions including retakes and automatic timeout resets.
- Enable only one mode: Get Ready Back must return to the welcome screen.
- Repeated taps on Email while sending must create only one request. Reach the per-address
  cap across separate guest sessions, restart the app, and confirm the cap remains.
- Start a new event (including reusing its name) and verify a fresh email allowance.
- Disconnect Wi-Fi during email/upload, retry, and test API rejection and low storage.
- Setup: permission alone must not pass the camera check. Capture a real test photo;
  change camera/resolution or unplug USB and verify readiness is invalidated.
- Setup: credentials alone must not pass delivery. Test successfully, edit credentials
  during a test, repeat a failed test, and verify stale success cannot mark new settings ready.
- Test printing/system sharing both with screen pinning and Device Owner lockdown.
- Verify any native libraries in the final AAB on a 16 KB page-size device/emulator.

## All-night camera + guest checks (added with the camera hardening pass)

CI uploads a minified, CI-signed release APK (`snapcabin-release-ci-signed`
artifact) for these. It is test-only: never upload it to Play.

- **Memory:** at Photo resolution = Full, run 30+ back-to-back GIF and collage
  sessions without restarting. The app must not crash or slow down.
- **Hung camera:** cover/unplug a USB camera mid-countdown. The shot fails within
  ~10 s, the guest sees the normal retry path, and the next session works.
- **USB camera unplug/replug:** with an external camera selected, unplug it at
  Get Ready → preview switches to a built-in camera within seconds; plug it back
  → the external camera returns without leaving the screen.
- **Permissions:** with a DSLR (or any non-camera USB device) plugged in and mic
  permission denied, guest sessions must show NO system dialog.
- **Multi-tap:** hammer Get Ready, a mode card, Accept on Review, and admin's
  close button with several fingers. Exactly one screen opens; admin close never
  leaves a blank screen.
- **Power button (kiosk on):** press power, press again → the booth shows over
  the lock screen, camera live.
- **Crash restart:** force a crash (e.g. `adb shell am crash com.snapcabin`) →
  booth relaunches within a few seconds; three crashes in 2 min stop relaunching.
  Check with and without Device Owner (Android 10+ may block the relaunch
  without it).
- **DSLR budget/pause:** set the lens to AF and point at a blank wall so focus
  fails → each shot falls back to the tablet within ~8 s of the flash; after the
  2nd miss, shots go straight to the tablet camera and admin shows the pause
  with TRY DSLR AGAIN NOW.
- **DSLR power cycle:** tick "Always open SnapCabin" on the USB prompt, switch
  the camera off and on mid-event → the next shot uses the DSLR again with no
  admin visit. Also check whether that USB prompt appears in kiosk/lock-task mode.
- **DSLR heartbeat:** pull the DSLR cable while idle → within ~30 s admin shows
  it as detected/not connected rather than Connected.

## Email accounting and privacy

Per-address counts now live in a separate app-private DataStore. Recipient identifiers
are event-salted HMACs, not plaintext addresses; these identifiers and counts never leave
the device. Starting a different event's first send replaces the previous counters.
The capped/clearable audit log is not used for enforcement. A request interrupted with
unknown delivery status conservatively consumes its reserved slot; an explicit error
releases it. No background resend queue or automatic retry is introduced.
