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

- **M1 — Connect + identify (done, needs on-device validation).** Open a PTP
  session and read the camera model. Proves the transport works on a given
  tablet + camera. This is the cheapest thing to fail, so we prove it first.
- **M2 — Remote capture.** Canon EOS: `SetRemoteMode` → `RemoteReleaseOn/Off`,
  watch events for the new object, `GetObject` to download the JPEG. Wire into a
  "take test photo" path, then the guest capture flow.
- **M3 — Live view.** Canon EOS: enable EVF output via a device property, poll
  `GetViewFinderData`, decode each JPEG chunk to a Bitmap (~20–30 fps), feed the
  GetReady/Capture preview.
- **M4 — Integrate + harden.** Make the DSLR a selectable capture source
  alongside CameraX; handle disconnect/reconnect, auto-power-off, full-res vs
  preview JPEG; then expand beyond Canon (Nikon/Sony have their own ops).

## Testing notes (M1)

On the tablet, plug in the Canon, open admin → CAMERA, tap **CONNECT DSLR**,
allow the USB permission prompt. Expected: **"✓ Connected: Canon EOS …"** with
serial + whether it advertises remote capture. Watch logcat tag `DslrManager` /
`PtpTransport` for the transaction trace. Likely first-iteration tuning points:
PTP transaction-ID convention, zero-length-packet handling on bulk-in, and the
`OpenSession` response code for cameras that auto-open a session.
