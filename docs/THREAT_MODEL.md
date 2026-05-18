# Threat model

One-page summary of what SnapCabin assumes about its operating environment,
what attacks it defends against, and which threats live with the operator.
Doubles as customer-facing reassurance and a pre-emptive answer to anything
Play review might raise.

## Assets

1. **Photos** taken at events — both as in-memory bitmaps and as files
   optionally uploaded to the operator's Cloudinary account.
2. **Phone numbers** entered by guests for SMS delivery.
3. **Operator credentials** — Twilio Account SID + Auth Token + From-number;
   Cloudinary Cloud Name + Upload Preset; admin PIN.
4. **Event metadata** — current event name, slug, start time, audit log.

## Trust boundaries

| Boundary | What's on each side |
|---|---|
| Device sandbox ↔ other apps | DataStore lives in the app-private sandbox; other apps can't read it without root. |
| Kiosk admin ↔ guests | Admin PIN gates Admin, Privacy, Gallery. Long-press hotspot is small, dim, and bottom-right. |
| Kiosk ↔ Twilio | HTTPS only. Basic auth header constructed locally. Auth Token never logged. |
| Kiosk ↔ Cloudinary | HTTPS only. Unsigned upload — no API secret on-device. |
| Kiosk LAN ↔ guest phones | HTTP server on LAN only. Not reachable from the internet. |
| Operator ↔ guest | The operator is the data controller for any data that leaves the kiosk. SnapCabin (the developer) receives nothing. |

## Threats and mitigations

### T1. Physical access to the kiosk

**Threat**: someone at an event picks up the tablet and tries to access
operator-only settings, extract credentials, or send fraudulent SMS.

**Mitigations**:
- Admin PIN (default `1234`, default-PIN warning surfaces in Admin).
- **Kiosk Mode via Device Owner** — when provisioned, the OS prevents
  exiting the app, opening other apps, or reaching system settings.
  See [DEVICE_OWNER_DISCLOSURE.md](./DEVICE_OWNER_DISCLOSURE.md).
- Per-session and per-phone SMS rate limits cap blast radius if an
  attacker does get to the SMS UI.

**Residual risk**: an attacker who knows the PIN and is on an unlocked
(non-Device-Owner) kiosk can change settings or exfiltrate credentials.
**Operator action**: change the default PIN, enable Kiosk Mode before
deployment.

### T2. Credential exfiltration via ADB

**Threat**: someone with USB access enables ADB and pulls Twilio /
Cloudinary credentials out of the app's DataStore.

**Mitigations**:
- Device Owner mode disables developer options and ADB by policy.
- Without Device Owner, ADB requires unlocked developer mode + USB
  authorization — both require physical access plus interactive
  approval.

**Residual risk**: a determined attacker with prolonged physical access
to a non-Device-Owner kiosk can extract credentials.
**Operator action**: enable Kiosk Mode.

### T3. SMS abuse / spam

**Threat**: an attacker repeatedly enters phone numbers to send unwanted
SMS, racking up the operator's Twilio bill or harassing recipients.

**Mitigations**:
- `twilioMaxPerSession` (default 10) — caps SMS per photo.
- `twilioMaxPerNumber` (default 3) — caps SMS to the same number per
  event.
- Phone numbers normalized to E.164 + rejected if invalid before any
  network call.
- Per-event reset prevents cross-event blast.

**Residual risk**: an attacker rapidly rotating phone numbers can still
spam up to `twilioMaxPerSession` × N photos.
**Operator action**: monitor Twilio's own dashboard (which has its own
abuse detection); tighten the per-session cap in Admin.

### T4. Unsigned Cloudinary preset abuse

**Threat**: an attacker who learns the Cloudinary cloud name + preset
name uploads arbitrary content to the operator's bucket.

**Mitigations** (server-side, in Cloudinary):
- Configure the preset as **Unsigned**.
- Restrict allowed formats to `jpg, png` only.
- Set a max file size (~10 MB).
- Restrict folder to `events/`.

The admin UI in SnapCabin links operators to Cloudinary's settings page
and lists the constraints to set.

**Residual risk**: bounded to "image files of < 10 MB in the events/
folder of the operator's Cloudinary account". Annoying but not
catastrophic. Operator can rotate the preset name at any time without
data loss.

### T5. Inappropriate photo content

**Threat**: a guest takes an offensive photo and uploads it to the
operator's Cloudinary account or sends it via the operator's Twilio.

**Mitigations**:
- This is a content-moderation problem, not a technical one. SnapCabin
  doesn't moderate; the operator does (or doesn't).
- Audit log records every send with a masked recipient + timestamp +
  event so the operator can audit after the fact.

**Operator action**: communicate event norms to guests; supervise the
kiosk at sensitive events.

### T6. Network-based attacks on the LAN server

**Threat**: someone on the venue WiFi enumerates the kiosk's IP, hits
`http://x.x.x.x:8080/photo.jpg`, and pulls the latest photo.

**Mitigations**:
- The server only ever holds the **most recent** photo and shuts down
  when Share screen is dismissed.
- The URL is the *intended* delivery mechanism (the QR points there).
- Operators concerned about this should run a dedicated kiosk WiFi
  network or disable QR sharing entirely (Admin toggle).

**Residual risk**: bounded to "one photo per session, accessible on
the LAN for the duration of the share screen."

## What's intentionally NOT defended

- **Determined OS-level attacker with rooted device + physical access**:
  outside scope. If your threat model includes nation-state actors, this
  isn't the app for you.
- **Cloudinary or Twilio compromise**: operator's responsibility — those
  vendors have their own security programs.
- **Operator misconfiguration** (e.g. setting Twilio From-number to a
  shared toll-free that can be spoofed): outside scope; documented in
  the admin UI as the operator's responsibility.

## What's collected centrally by SnapCabin (the developer)

**Nothing.** No analytics. No crash reports. No identifiers. No telemetry.
This is enforced by *not having* any analytics or crash-reporting SDKs in
the app. Verifiable in `app/build.gradle.kts` (no Firebase, no Sentry, no
amplitude, no mixpanel, etc.).
