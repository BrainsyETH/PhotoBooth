# Threat model

One-page summary of what SnapCabin assumes about its operating environment,
what attacks it defends against, and which threats live with the operator.
Doubles as customer-facing reassurance and a pre-emptive answer to anything
Play review might raise.

## Assets

1. **Photos** taken at events — both as in-memory bitmaps and as files
   optionally uploaded to the operator's Cloudinary account.
2. **Email addresses** entered by guests for email delivery.
3. **Operator credentials** — Resend API Key + From-address;
   Cloudinary Cloud Name + Upload Preset; admin PIN.
4. **Event metadata** — current event name, slug, start time, audit log.

## Trust boundaries

| Boundary | What's on each side |
|---|---|
| Device sandbox ↔ other apps | DataStore lives in the app-private sandbox; other apps can't read it without root. |
| Kiosk admin ↔ guests | Admin PIN gates Admin, Privacy, Gallery. Long-press hotspot is small, dim, and bottom-right. |
| Kiosk ↔ Resend | HTTPS only. Bearer token header constructed locally. API key never logged. |
| Kiosk ↔ Cloudinary | HTTPS only. Unsigned upload — no API secret on-device. |
| Operator ↔ guest | The operator is the data controller for any data that leaves the kiosk. SnapCabin (the developer) receives nothing. |

## Threats and mitigations

### T1. Physical access to the kiosk

**Threat**: someone at an event picks up the tablet and tries to access
operator-only settings, extract credentials, or send fraudulent email.

**Mitigations**:
- Admin PIN (default `1234`, default-PIN warning surfaces in Admin).
- **Kiosk Mode via Device Owner** — when provisioned, the OS prevents
  exiting the app, opening other apps, or reaching system settings.
  See [DEVICE_OWNER_DISCLOSURE.md](./DEVICE_OWNER_DISCLOSURE.md).
- Per-session and per-address email rate limits cap blast radius if an
  attacker does get to the email UI.

**Residual risk**: an attacker who knows the PIN and is on an unlocked
(non-Device-Owner) kiosk can change settings or exfiltrate credentials.
**Operator action**: change the default PIN, enable Kiosk Mode before
deployment.

### T2. Credential exfiltration via ADB

**Threat**: someone with USB access enables ADB and pulls Resend /
Cloudinary credentials out of the app's DataStore.

**Mitigations**:
- Device Owner mode disables developer options and ADB by policy.
- Without Device Owner, ADB requires unlocked developer mode + USB
  authorization — both require physical access plus interactive
  approval.

**Residual risk**: a determined attacker with prolonged physical access
to a non-Device-Owner kiosk can extract credentials.
**Operator action**: enable Kiosk Mode.

### T3. Email abuse / spam

**Threat**: an attacker repeatedly enters email addresses to send unwanted
mail, eating into the operator's Resend quota or harassing recipients.

**Mitigations**:
- `resendMaxPerSession` (default 10) — caps emails per photo.
- `resendMaxPerAddress` (default 3) — caps emails to the same address per
  event.
- Email addresses validated against a basic RFC-5322 pattern before any
  network call.
- Per-event reset prevents cross-event blast.

**Residual risk**: an attacker rapidly rotating email addresses can still
spam up to `resendMaxPerSession` × N photos.
**Operator action**: Resend's free tier caps daily sends at 100 — even
worst-case the blast radius is bounded by the operator's plan limits.
Tighten the per-session cap in Admin if abuse is anticipated.

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
operator's Cloudinary account or emails it via the operator's Resend
account.

**Mitigations**:
- This is a content-moderation problem, not a technical one. SnapCabin
  doesn't moderate; the operator does (or doesn't).
- Audit log records every send with a masked recipient + timestamp +
  event so the operator can audit after the fact.

**Operator action**: communicate event norms to guests; supervise the
kiosk at sensitive events.

## What's intentionally NOT defended

- **Determined OS-level attacker with rooted device + physical access**:
  outside scope. If your threat model includes nation-state actors, this
  isn't the app for you.
- **Cloudinary or Resend compromise**: operator's responsibility — those
  vendors have their own security programs.
- **Operator misconfiguration** (e.g. setting a Resend From-address on a
  domain they don't actually control): outside scope; the Resend API
  itself rejects unverified domains, and the rejection is surfaced
  verbatim in the admin UI.

## What's collected centrally by SnapCabin (the developer)

**Nothing.** No analytics. No crash reports. No identifiers. No telemetry.
This is enforced by *not having* any analytics or crash-reporting SDKs in
the app. Verifiable in `app/build.gradle.kts` (no Firebase, no Sentry, no
amplitude, no mixpanel, etc.).
