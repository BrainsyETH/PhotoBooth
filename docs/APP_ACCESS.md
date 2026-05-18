# Play Console — App Access

Play uses the **App access** panel to tell reviewers how to get past any
authentication or restricted areas. SnapCabin doesn't have user accounts,
but the **admin screen** is gated behind a PIN — reviewers need that PIN
to inspect Admin / Privacy / Gallery during review.

## Where to find it

Play Console → **Policy → App content → App access**.

## What to enter

**All or some functionality is restricted?** → **All functionality is
available without special access**, *with notes*.

> Wait — that's not quite right because Admin is PIN-gated.

The closest match Play offers is:

**Some functionality is restricted (e.g., login required, captcha, etc.)**
→ check this box and add instructions.

### Instructions text (paste verbatim)

> SnapCabin has no user accounts. All guest-facing flows (camera, photo
> capture, share, QR download, SMS, email) are available without any login.
>
> The **Admin** screen — accessible by long-pressing the bottom-right
> corner of the Attract screen — is protected by a numeric PIN to prevent
> guests at an event from accessing operator-only settings (Twilio
> credentials, Cloudinary credentials, kiosk lockdown toggle, event
> configuration, audit log).
>
> **Default PIN for review**: `1234`
>
> When opening the app for review:
> 1. Tap the screen once to leave the Attract screen.
> 2. To reach Admin: return to Attract, then long-press the lower-right
>    corner where it says "Long-press for settings".
> 3. Enter PIN `1234`.
> 4. All restricted screens (Admin, Privacy Policy, Gallery) are reachable
>    from there.
>
> The app surfaces a visible warning inside Admin if the PIN is still the
> default `1234` to encourage operators to change it before deploying at a
> public event.

### Why we ship with a default PIN

Two reasons:

1. **First-run ergonomics** — the host of an event needs to be able to
   open the app, configure it, and run an event without being locked out
   on the first boot. A randomly-generated PIN on first install would
   require either a paper-trail to recover or extra UX friction.
2. **Threat model** — the kiosk admin is meant to be reachable only by
   the operator. Once Kiosk Mode is enabled and Device Owner provisioning
   is active (see DEVICE_OWNER_DISCLOSURE.md), exiting the app to do
   anything malicious is blocked at the OS level. The PIN is the
   secondary gate, and the in-app warning nudges the operator to change
   it before opening the kiosk to the public.

This combination is industry-standard for kiosk-class Android apps.

## Login credentials (not applicable)

- **Username**: leave blank
- **Password**: leave blank

## URL to access restricted area

> Not applicable — restricted areas are reached by long-pressing the
> bottom-right corner of the Attract screen, not via a URL.

## Any other access instructions

> The kiosk lockdown features (Lock Task Mode, Device Owner) are documented
> separately under the Device Admin section. They are not active in a
> standard install — they require manual ADB provisioning by the operator.
> The default install behaves like a standard Android app and can be exited
> normally for review.
