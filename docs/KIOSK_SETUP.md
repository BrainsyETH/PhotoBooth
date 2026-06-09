# SnapCabin setup guide

This guide has two parts:

- **Part A — Host setup (no computer needed).** Everything a wedding or
  party host does on the tablet itself, in the app. If you just want to
  run the booth, this is all you need. Takes about 15–30 minutes.
- **Part B — Technician lockdown (one-time, needs a computer).** An
  optional, advanced step that hard-locks the tablet to SnapCabin so
  guests can't escape to other apps. Skip it unless you want a fully
  unattended kiosk. You only ever do this once per tablet.

Stuck at any point? Email **hello@snapcabin.app** and we'll help.

---

# Part A — Host setup (no computer needed)

You do all of this on the tablet, by tapping the screen. No cables, no
computer, no developer tools.

## What you need

- An Android tablet (Samsung Galaxy Tab series recommended; anything
  Android 8.0+ works). SnapCabin is built for tablets — it won't run on a
  phone-sized screen.
- A charger, and ideally a stand.
- *(Optional)* A **Resend** account if you want to email photos to guests.
- *(Optional)* A **Cloudinary** account if you want a QR-code "scan to
  download" option on the share screen.

You can create the Resend and Cloudinary accounts later — the app walks
you through it with step-by-step links right inside each section.

## Getting into the admin area

1. Open SnapCabin. You'll land on the **welcome screen** (we call it the
   Attract screen) — your event name and a glowing "tap to start" button.
2. **Press and hold anywhere on the welcome screen** for a couple of
   seconds. (Anywhere on the screen works — you don't have to find a
   special corner.)
3. Enter the **admin PIN**. The default is **`1234`**.

That's it — you're in Settings. A reminder that the PIN is still the
default appears on this entry screen and again inside the app until you
change it.

## The GET STARTED checklist

The first thing you'll see in Settings is **GET STARTED** — a short
checklist that mirrors the three things that actually matter before
guests arrive. Tap any item to jump straight to the section that
finishes it. The checklist is:

1. **Change the admin PIN** — jumps to the **KIOSK** section.
2. **Start your event** — jumps to the **EVENT** section.
3. **Set up photo delivery** — jumps to **EMAIL DELIVERY** (and/or **QR
   DOWNLOADS**).

Below the checklist is a **Delivery methods** summary showing the live
status of Email and QR code. Each shows a small status pill:

- **Off** — that delivery method is turned off.
- **Needs details** — turned on, but you still have to enter your
  account info.
- **Send a test** / **Run a test** — info entered, but you haven't
  confirmed it works yet.
- **Ready · tested** — you ran a test and it succeeded. This is the green
  light you're looking for.

When the PIN is changed, an event is started, and at least one delivery
method is set up, the checklist shows **"You're ready to go."**

Work through the sections in the order below — it matches the side
navigation in the app (top to bottom).

## 1. Change the admin PIN — KIOSK section

Open the **KIOSK** section and find **Admin PIN** at the top. Type a new
PIN (4–8 digits) twice — once to set it, once to confirm — then tap
**SET PIN**. Typing it twice prevents a typo from locking you out of your
own booth.

The default `1234` is fine for poking around, but change it before the
event: anyone who long-presses the welcome screen could otherwise reach
Settings with the default PIN.

> Note: After 5 wrong PIN attempts the entry screen locks for 30 seconds,
> so a curious guest can't just keep guessing.

## 2. Start your event — EVENT section

Open the **EVENT** section and tap **START EVENT**. Type your event name
(for example, "The Hewlett Wedding") and tap **START**.

Starting an event does several things automatically:

- It becomes the **headline on the welcome screen** — you no longer set
  this by hand. (If you want a fancier welcome headline than your event
  name, you can override it later in **BRANDING**.)
- It creates a **photo folder name** (shown as the "slug" — that's just a
  tidy, web-safe version of your event name) so all photos, logs, and
  per-guest email limits are grouped under this event.
- If QR downloads are on, photos upload to the Cloudinary folder
  `events/<your-folder-name>/`.

You can **END EVENT** afterward, or **START NEW EVENT** to begin a fresh
one. Ending an event resets the per-guest email limits.

## 3. Set up photo delivery

There are two independent ways to get photos to guests. You can use
either, both, or neither. Each lives in its own section, and each has a
**built-in test button** plus a status pill so you can confirm it works
*before* the event.

### EMAIL DELIVERY (Resend)

This emails each guest their photo as an attachment, over WiFi. Turn on
**Enable email delivery** and the section shows a numbered, "about 10
minutes" walkthrough with buttons that open the right Resend pages:

1. Create a free Resend account (the free tier covers ~3,000 emails a
   month, 100 a day).
2. Verify a sending domain — or, to test right now, skip this and use
   `onboarding@resend.dev` as the From address.
3. Create an API key with Sending access (it starts with `re_`; Resend
   shows it only once).
4. Paste the **API key** and **From address** into the fields, then
   scroll to **Send a test email**, enter your own address, and tap
   **SEND TEST**.

When the test succeeds, the status pill flips to **Ready · tested**.
There are also optional fields for a reply-to address, subject line, and
message body (type `{event}` and it's replaced with your event name).

### QR DOWNLOADS (Cloudinary)

This hosts each photo online so the share screen can show a **QR code**
guests scan to download. Turn on **Enable QR photo hosting** and the
section shows an "about 5 minutes" walkthrough:

1. Create a free Cloudinary account (the free tier comfortably covers a
   typical event).
2. Copy your **cloud name** from the dashboard.
3. Create an **Unsigned upload preset**. ("Unsigned" simply means the
   tablet can upload photos without storing a secret password on the
   device — safer for a kiosk that strangers can touch.) In Cloudinary:
   Settings → Upload → Add upload preset, set Signing Mode to **Unsigned**,
   allow `jpg`/`png`, and cap the file size around 10 MB.
4. Paste the **cloud name** and **upload preset name** into the fields,
   then tap **TEST UPLOAD**.

When the test upload succeeds, the status pill flips to **Ready · tested**.

## 4. Set the camera — CAMERA section

Open the **CAMERA** section and pick which camera the booth uses (front
or rear), whether to mirror the front camera, and the photo resolution.
Make sure the **LOOK HERE** pointer ends up next to the lens guests
should look at.

### Using an external (USB) camera

For better image quality you can plug a USB webcam — or a camera that
offers a "webcam mode" — into the tablet's USB-C port (use an OTG
adapter or a powered hub if the camera needs more power).

1. Plug the camera in **before the event**, while you're in admin.
2. Open **CAMERA** and check the **External camera (USB)** box. It tells
   you whether the tablet sees the USB device and whether Android is
   exposing it as a camera. Tap **REFRESH CAMERAS** after plugging in.
3. Select the camera. If it appears in the camera list marked
   **— External**, tap it. If it doesn't show up in that list (USB cameras
   don't always enumerate there), tap **USE EXTERNAL USB CAMERA** in the
   External camera box instead — that binds to whatever USB camera is
   connected. Either way, turn **Mirror Image** off (external cameras face
   guests like a rear camera) and run **TEST CAMERA** to confirm the live
   preview.
4. **Allow the microphone prompt.** USB cameras are audio+video devices,
   so Android won't open one unless the app has microphone permission.
   SnapCabin never records audio — it only needs the permission to turn
   the camera on. Tap **Allow**; if you tap Deny the external camera won't
   start. (Built-in cameras never ask for this.)
5. Take one full test photo end-to-end before guests arrive.

> **Sounds:** booth beeps and the shutter play on the tablet's **media**
> volume. If you can't hear them, turn media volume up and make sure the
> tablet isn't in Do Not Disturb.

If the USB device is detected but never shows up as a camera, that
tablet's Android build doesn't support USB (UVC) cameras — not every
tablet does. The booth works fine on the built-in camera; if an external
camera matters, test the exact tablet + camera pair before event day.
Keep the camera plugged in for the whole event: if it disconnects
mid-session the booth automatically falls back to a built-in camera.

## 5. Tune the guest experience (optional)

These sections are all optional — sensible defaults are already set:

- **BRANDING** — watermark, custom border/overlay, and the optional
  welcome-screen headline override.
- **MODES** — turn the Single Photo, Collage, and GIF modes on or off.
- **CAPTURE** — countdown length, flash effect, and review auto-accept.
- **SHARE OPTIONS** — auto-save to gallery, QR sharing, JPEG quality.
- **SOUND** — shutter sound and countdown beep.

## 6. Run a full test capture

1. Tap **CLOSE SETTINGS** to return to the welcome screen.
2. Tap to start, pick a mode, and go through Get Ready → Capture →
   Review → Share.
3. If QR downloads are on: confirm the QR code appears and a phone on the
   same WiFi can open the link.
4. If email is on: on the share screen tap **EMAIL**, enter your own
   address, and confirm the photo arrives as an attachment.

## 7. (Optional) Kiosk Mode

In the **KIOSK** section there's a **Kiosk Mode** toggle. Turning it on
locks the tablet to SnapCabin for the event: the status bar, navigation
bar, and other apps are blocked, and the screen stays on while plugged
in.

- **Exiting Kiosk Mode is easy and safe.** From the **KIOSK** section you
  can either turn the toggle off, or use **Exit kiosk & close app** — both
  are behind the admin PIN, so guests can't get out but you always can.
- For a *fully* hardened lock (so even the swipe-down gesture and reboot
  tricks are blocked), a technician does the one-time setup in **Part B**.
  Without that, Kiosk Mode is still a solid best-effort lock — it's just
  not bulletproof.

Set the tablet on its stand, plug it in, and walk away.

---

# Part B — Technician lockdown (one-time, needs a computer)

> **Optional / advanced.** This section is for someone comfortable with a
> computer and a USB cable. It makes SnapCabin the tablet's **Device Owner**
> — in plain terms, it tells Android "this app is in charge of this tablet,"
> which is what lets Kiosk Mode fully block the home button, recent-apps,
> and the notification shade. You only do this **once** per tablet. If you
> skip it, the booth still works and Kiosk Mode still does a best-effort
> lock; guests just have a slightly easier time poking out of the app.

## What you need

- The tablet, freshly reset (see step 1).
- A USB cable.
- A Mac or PC with **`adb`** installed (Android Platform Tools).
- The SnapCabin APK file (provided after purchase).

## Step 1 — Factory reset, and DON'T sign into Google

Device Owner can only be set on a tablet with **no accounts** on it.

1. Factory-reset the tablet (Settings → General management → Reset).
2. Skip the setup wizard.
3. **Don't sign into a Google account.** This is critical.

## Step 2 — Enable Developer Options + USB Debugging

1. Settings → About tablet → Build number → tap 7 times.
2. Settings → Developer options → enable **USB debugging**.
3. Connect to your computer via USB and tap "Allow" on the prompt.

Verify with `adb devices` — the tablet should be listed.

## Step 3 — Install SnapCabin

```bash
adb install snapcabin.apk
```

Launch it once to confirm it starts, then close it.

## Step 4 — Provision Device Owner

```bash
adb shell dpm set-device-owner com.snapcabin/com.snapcabin.kiosk.DeviceAdminReceiver
```

If you see `Not allowed to set the device owner because there are already
some accounts on the device`, go back to Step 1, reset again, and don't
add any account.

## Step 5 — Hand off to the host

Once Device Owner is set, the rest is done **in the app** — see Part A.
When the host turns on **Kiosk Mode** in the KIOSK section, the full
hardware lockdown kicks in. They can still exit via the KIOSK section
(toggle off, or **Exit kiosk & close app**) behind the admin PIN.

What full lockdown adds over the best-effort lock:

- Status bar, navigation bar, home button, and recent apps are blocked.
- The lock screen (keyguard) is disabled.
- Safe-boot, factory reset, adding users, and mounting external media are
  blocked.
- The tablet stays on while plugged into AC, USB, or wireless power.

---

# Troubleshooting

**I'm locked out / forgot the admin PIN.** Email
**hello@snapcabin.app**. (Recovering from a forgotten PIN means clearing
the app's data, which also wipes your configuration — so set a PIN you'll
remember.)

**The "send a test" / "test upload" button failed.**
- Email: "Resend rejected the API key" → revoke the key in Resend and
  create a new one. "The domain is not verified" → either verify your
  domain (Resend → Domains → Add Domain) or use `onboarding@resend.dev`
  for testing.
- QR: double-check the cloud name and preset name match Cloudinary
  exactly (they're case-sensitive), and that the preset is **Unsigned**
  with `jpg`/`png` allowed.
- Either way, the **AUDIT LOG** section shows the most recent error
  message.

**The QR code shows a link a phone won't open.** The link is a public
Cloudinary URL, so any phone with internet should open it. If it doesn't,
re-check that the upload preset is **Unsigned** and allows `jpg`/`png`.

**The tablet falls asleep mid-event.** Kiosk Mode keeps the screen on
*while plugged in* — make sure it's on AC power, not running on battery.

## Advanced / emulator notes (technicians)

**`dpm set-device-owner` says "already accounts on device".** Factory
reset again, skip the entire setup wizard, and don't add any account.

**Camera preview is rotated 90°.** The app is landscape-locked, but an
emulator or device may be in portrait — rotate it physically, or in the
emulator press **Ctrl+Right**.

**App crashes or behaves oddly.** SnapCabin ships with no crash
reporting. From a computer, capture a log while reproducing the issue:

```bash
adb logcat -d > snapcabin-log.txt
```

Email that file to **hello@snapcabin.app**.
