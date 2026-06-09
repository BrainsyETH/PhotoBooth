# Play Console — Data Safety form answers

This is the exact set of answers to give in **Policy → App content → Data
safety**. Play uses this form to populate the "Data safety" section that
guests see on the Play listing. **It must match what the app actually does**
or Play will reject the submission.

## Section 1 — Data collection and security

**Does your app collect or share any of the required user data types?**
> Yes

**Is all of the user data collected by your app encrypted in transit?**
> Yes
> *(Cloudinary uploads and Resend API calls are both HTTPS-only. The app
>  makes no other outbound network calls.)*

**Do you provide a way for users to request that their data be deleted?**
> Yes, through the event host
> *(The event host is the data controller for any data that leaves the
>  device. Guests contact the host to request deletion from Cloudinary
>  or Resend. The privacy policy explains this.)*

## Section 2 — Data types

Tick **only** these categories. Leave everything else off.

### Photos and videos → Photos

- **Collected**: Yes
- **Shared**: Yes (with Cloudinary, if the host enabled it; and as a JPEG
  attachment via Resend, if the host enabled it)
- **Processed ephemerally**: No, saved at least temporarily
- **Required or optional**: Required for app functionality
- **Purposes**: App functionality
- **Is this data type collected, free-form?** No, structured (image files)
- **User can request data deletion**: Yes (via the host)

### Personal info → Email address

- **Collected**: Yes (only when a guest types one to receive their photo)
- **Shared**: Yes (with Resend, to deliver the email)
- **Processed ephemerally**: Yes. The unmasked address is not retained;
  only a masked version (e.g. `e***@example.com`) appears in the on-device
  audit log.
- **Required or optional**: Optional. The guest chooses to enter it.
- **Purposes**: Communications

## Section 3 — Things to explicitly NOT tick

(These are common false-positives. Be precise. Over-disclosure is a
rejection vector too.)

- ❌ **Location** — the app does not request any location permission.
- ❌ **Device or other IDs** — no advertising ID, no Android ID collection.
- ❌ **App activity / analytics** — no analytics SDKs.
- ❌ **Crash logs / diagnostics** — no Crashlytics, no Sentry, no Firebase.
- ❌ **Performance data** — none collected.
- ❌ **Contacts** — never read.
- ❌ **Calendar** — never read.
- ❌ **Financial info** — none.
- ❌ **Health and fitness** — none.
- ❌ **Phone number** — never collected; the SMS path was removed.
- ❌ **Messages** — we never read SMS / MMS / chat messages.
- ❌ **Audio / files / documents** — only photos taken by the kiosk camera.
  The app declares the `RECORD_AUDIO` permission **but never records,
  stores, or transmits audio**. It's required purely because Android's
  camera service refuses to open a USB (UVC) camera — a composite
  audio+video device — without it. The permission is requested at runtime
  only when an external camera is attached. Because no audio is collected,
  it is NOT disclosed as a collected data type; if the Play Console flags
  the microphone permission, justify it as "required to open external USB
  cameras; no audio is captured." See `camera/CameraManager.kt` and the
  manifest comment on `RECORD_AUDIO`.
- ❌ **Web browsing** — none.

## Section 4 — Why these answers are accurate

Reference the code:

- `share/CloudinaryUploader.kt`: HTTPS upload of photo only, gated on
  host-configured credentials.
- `share/ResendEmailSender.kt`: HTTPS POST of email address plus photo
  attachment (base64 JPEG), gated on host-configured credentials.
- `event/SendLog.kt`: local audit log. Email addresses are masked at
  write time (`SendLog.maskEmail` keeps only the first letter and the
  domain) and never leave the device.
- No analytics, telemetry, or crash-reporting code anywhere in the app.

## Updating this form

Re-do this exercise any time the app gains a new outbound network call or
a new on-device data type. Mismatches between this form and the in-app
behaviour are the most common cause of Play rejections.
