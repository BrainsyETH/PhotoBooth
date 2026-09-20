# API 36 readiness

The app now compiles/targets API 36 with AGP 8.10.1, Gradle 8.11.1 and JDK 17.
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

## Email accounting and privacy

Per-address counts now live in a separate app-private DataStore. Recipient identifiers
are event-salted HMACs, not plaintext addresses; these identifiers and counts never leave
the device. Starting a different event's first send replaces the previous counters.
The capped/clearable audit log is not used for enforcement. A request interrupted with
unknown delivery status conservatively consumes its reserved slot; an explicit error
releases it. No background resend queue or automatic retry is introduced.
