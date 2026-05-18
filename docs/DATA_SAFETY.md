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
> *(Cloudinary uploads and Twilio API calls are both HTTPS-only. The local
>  WiFi photo server is HTTP, but never leaves the LAN and is disclosed in
>  the policy.)*

**Do you provide a way for users to request that their data be deleted?**
> Yes — through the operator
> *(The kiosk operator is the data controller for any data that leaves the
>  device. Guests contact the operator to request deletion from Cloudinary
>  or Twilio. The privacy policy explains this.)*

## Section 2 — Data types

Tick **only** these categories. Leave everything else off.

### Photos and videos → Photos

- **Collected**: Yes
- **Shared**: Yes (with Cloudinary, if the operator enabled it)
- **Processed ephemerally**: No — saved at least temporarily
- **Required or optional**: Required for app functionality
- **Purposes**: App functionality
- **Is this data type collected, free-form?** No, structured (image files)
- **User can request data deletion**: Yes (via operator)

### Personal info → Phone number

- **Collected**: Yes (only when a guest enters one to receive an SMS)
- **Shared**: Yes (with Twilio, to deliver the SMS)
- **Processed ephemerally**: Yes — the unmasked number is not retained;
  only a masked last-4-digits version appears in the on-device audit log
- **Required or optional**: Optional — the guest chooses to enter it
- **Purposes**: Communications

### Personal info → Email address

- **Collected**: Yes (only when a guest taps the email button)
- **Shared**: Handed off to a third-party email app via Android Intent;
  SnapCabin doesn't transmit it directly
- **Processed ephemerally**: Yes — not retained
- **Required or optional**: Optional
- **Purposes**: Communications

## Section 3 — Things to explicitly NOT tick

(These are common false-positives. Be precise — over-disclosure is a
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
- ❌ **Messages** — we never read SMS / MMS / chat messages.
- ❌ **Audio / files / documents** — only photos taken by the kiosk camera.
- ❌ **Web browsing** — none.

## Section 4 — Why these answers are accurate

Reference the code:

- `share/CloudinaryUploader.kt` — HTTPS upload of photo only, gated on
  operator-configured credentials.
- `share/TwilioSmsSender.kt` — HTTPS POST of phone number + photo URL,
  gated on operator-configured credentials.
- `event/SendLog.kt` — local audit log; phone numbers are masked at write
  time (`SendLog.maskPhone` keeps only the last four digits) and never
  leave the device.
- `share/LocalPhotoServer.kt` — LAN-only HTTP server, never reachable from
  the internet.
- No analytics, telemetry, or crash-reporting code anywhere in the app.

## Updating this form

Re-do this exercise any time the app gains a new outbound network call or a
new on-device data type. Mismatches between this form and the in-app
behaviour are the #1 cause of Play rejections.
