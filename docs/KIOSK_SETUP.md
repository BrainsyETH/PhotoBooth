# Kiosk setup guide

How to provision a fresh tablet as a SnapCabin kiosk. Aimed at operators
(event hosts, venue staff). Takes 15–30 minutes.

## What you need

- An Android tablet (Samsung Galaxy Tab series recommended; anything
  Android 8.0+ works).
- A USB cable.
- A Mac or PC with `adb` installed (Android Platform Tools).
- The SnapCabin APK file (provided after purchase).
- *(Optional)* A Twilio account if you want SMS delivery.
- *(Optional)* A Cloudinary account if you want photos to deliver as MMS
  over cellular.

## Step 1 — Factory reset the tablet (for kiosk lockdown)

Skip this step if you're OK with the tablet acting as a normal Android
device that just happens to run SnapCabin — Kiosk Mode is optional.

If you do want Kiosk Mode (recommended for unattended operation):

1. Factory-reset the tablet (Settings → General management → Reset).
2. Skip the setup wizard.
3. **Don't sign into a Google account.** This is critical — Device Owner
   can only be set on a device with no accounts.

## Step 2 — Enable Developer Options + USB Debugging

1. Settings → About tablet → Build number → tap 7 times.
2. Settings → Developer options → enable **USB debugging**.
3. Connect to your computer via USB. Tap "Allow" on the authorization
   prompt.

Verify with `adb devices` — you should see the tablet listed.

## Step 3 — Install SnapCabin

```bash
adb install snapcabin.apk
```

Launch the app once to confirm it starts. Then close it.

## Step 4 *(optional)* — Provision Device Owner for kiosk lockdown

```bash
adb shell dpm set-device-owner com.snapcabin/com.snapcabin.kiosk.DeviceAdminReceiver
```

If you see `Not allowed to set the device owner because there are already
some accounts on the device` — go back to step 1 and reset without
signing in.

## Step 5 — First-run configuration

Open SnapCabin. Long-press the bottom-right corner of the Attract screen
to reach Admin (default PIN: `1234`).

In order:

1. **Change the admin PIN** (KIOSK section). The default-PIN warning at
   the top of Admin won't go away until you do.
2. **Start an event** (EVENT section). Type the event name; the slug is
   auto-generated.
3. **Configure modes** you want to expose (MODES section).
4. **Set the event name on the Attract screen** (BRANDING section).
5. **Configure Twilio + Cloudinary** if you want SMS:
   - Twilio: enter Account SID, Auth Token, From-number from
     console.twilio.com.
   - Cloudinary: create an unsigned upload preset with image-format
     restrictions; enter cloud name and preset name.
6. **Set the camera lens position** (CAPTURE section) so the LOOK HERE
   pointer points at the actual lens.
7. **Enable Kiosk Mode** (KIOSK section) if you provisioned Device Owner
   in step 4.

## Step 6 — Test a complete capture

1. Close Admin → land on Attract → tap to start.
2. Pick a mode → Get Ready → Capture → Review → Share.
3. Confirm the QR works (scan it with your phone on the same WiFi).
4. If Twilio is configured: send a test SMS to your own number.

## Step 7 — Lock it down for production

If you enabled Kiosk Mode:

- The status bar, navigation bar, and recent apps are blocked.
- Exiting the app requires Admin PIN.
- Tablet won't sleep while plugged in.

Set the tablet on its stand, plug it in, walk away.

## Troubleshooting

**`dpm set-device-owner` says "already accounts on device"**:
factory reset again, skip the entire setup wizard, don't add any account.

**Camera preview is rotated 90°**:
the activity is landscape-locked but the emulator/device may be portrait —
rotate the device physically or in the emulator (Ctrl+Right).

**QR code shows a URL that won't load on a phone**:
the phone must be on the same WiFi as the kiosk. Many guest WiFi networks
have client isolation enabled, which blocks this — use a dedicated kiosk
WiFi or set up Cloudinary for off-network delivery.

**SMS doesn't send**:
verify Twilio credentials in Admin; verify the From-number is in E.164
format (`+15551234567`); check Twilio's own console for delivery status.

**Tablet falls asleep mid-event**:
Kiosk Mode includes a "stay on while plugged in" policy — make sure it's
plugged into AC, not just running on battery.

**App crashes / behaves oddly**:
SnapCabin ships with no crash reporting. Capture `adb logcat` output from
your computer while reproducing the issue:

```bash
adb logcat -d > snapcabin-log.txt
```

Email that file to hello@snapcabin.app.
