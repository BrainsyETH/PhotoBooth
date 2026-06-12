# Native DSLR support (PTP over USB)

Goal: control a tethered DSLR (Canon first) directly over USB — live view +
remote capture — **without** an HDMI-to-USB capture stick, the way dedicated
booth apps (e.g. Touchpix) do. Pure-Kotlin on Android's USB Host API; no NDK.

## Why this is needed

A DSLR plugged straight into the tablet enumerates as a **PTP** (still-image
class, USB class 6) device. Android's camera framework (CameraX) only accepts
**UVC** (video class) devices, so the camera HAL never exposes the DSLR — it
can't be a CameraX camera. The only ways to use it are an HDMI capture stick
(makes it look like a UVC webcam) or speaking PTP to it ourselves. This is the
second path.

## Architecture (`com.snapcabin.dslr`)

- **`Ptp.kt`** — protocol constants (operation/response codes, container types,
  vendor IDs), a little-endian `PtpReader` for datasets, and `PtpBytes` helpers.
- **`PtpTransport.kt`** — USB bulk transport. Finds the PTP interface + bulk
  endpoints, claims it, and runs the `command → (data) → response` transaction
  using the PTP USB container framing.
- **`PtpDeviceInfo.kt`** — parses `GetDeviceInfo` (manufacturer, model, serial,
  vendor extension, supported operations).
- **`DslrManager.kt`** — `@Singleton`: detects PTP devices, handles the USB
  permission prompt, opens a session, reads device info, and holds the
  connection open for later milestones. Exposes a `StateFlow<State>` the admin
  UI observes.

Admin surfacing: when a PTP device is detected, the **CAMERA** section shows an
experimental "CONNECT DSLR" block (`DslrConnectBlock` in `CameraSection.kt`).

## Milestones

- **M1 — Connect + identify (done; connects to Canon on-device).** Open a PTP
  session and read the camera model. Proved the transport works.
- **M2 — Remote capture (built; needs on-device validation).** Canon EOS:
  `SetRemoteMode`/`SetEventMode` → fire the shutter → poll `GetEvent` for
  ObjectAddedEx / RequestObjectTransfer (handle = first u32 of the record
  payload) → `GetPartialObject` chunks → `TransferComplete`. Surfaced as a
  **TAKE DSLR PHOTO** button that shows the downloaded image. The shutter is
  fired per the body's advertised ops: staged half/full press
  (`RemoteReleaseOn` 0x9128: On(1)→settle→On(2)→Off(2)→Off(1)) when available,
  falling back to the classic one-shot `RemoteRelease` 0x910F (the only release
  op older Rebels have, and also tried when the staged full-press is refused).
  The on-screen capture trace + per-event hex dump in logcat (`DslrManager`)
  decode body-specific layouts. **Known gotcha: with the lens on AF and no
  focus lock, EOS bodies in AF-priority silently refuse to fire** — set the
  lens to MF (and pre-focus) for booth use.
- **M2.5 — Hybrid booth mode (built).** Admin → Camera → "Use DSLR for
  photos": the tablet camera remains the live preview, but each booth shot is
  fired on the DSLR (`DslrManager.captureBoothPhoto`) and the downloaded JPEG
  (sampled down to the configured photo resolution) is what enters the
  review/share pipeline. GIF mode always uses the tablet camera (DSLR cadence
  is too slow), and any DSLR failure falls back to the tablet camera so guests
  are never stranded. The session auto-reconnects if the camera power-cycled
  (USB permission persists until unplug).
- **M3 — Live view (not built).** Canon EOS: enable EVF output via a device
  property, poll `GetViewFinderData`, decode each JPEG chunk to a Bitmap
  (~20–30 fps), feed the GetReady/Capture preview.
- **M4 — Harden.** Disconnect/reconnect edge cases, auto-power-off, full-res vs
  preview JPEG; then expand beyond Canon (Nikon/Sony have their own ops).

## Testing notes (M1)

On the tablet, plug in the Canon, open admin → CAMERA, tap **CONNECT DSLR**,
allow the USB permission prompt. Expected: **"✓ Connected: Canon EOS …"** with
serial + whether it advertises remote capture. Watch logcat tag `DslrManager` /
`PtpTransport` for the transaction trace. Likely first-iteration tuning points:
PTP transaction-ID convention, zero-length-packet handling on bulk-in, and the
`OpenSession` response code for cameras that auto-open a session.
