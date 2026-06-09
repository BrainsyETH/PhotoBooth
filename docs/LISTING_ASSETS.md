# Play Store listing assets

Everything Play requires for the public store listing. Sizes are exact —
Play rejects off-spec images. Capture screenshots from the actual app
running on an emulator or device, not from mocks.

## Text copy

### Short description (max 80 chars)

> A photo booth in the woods — self-hosted kiosk for weddings and gatherings.

### Long description (max 4000 chars)

Suggested copy:

> **SnapCabin turns any Android tablet into a private, self-hosted photo
> booth for weddings, parties, and gatherings.** $2.99 once, per device.
> No analytics. No telemetry. Runs entirely on hardware you bring.
>
> **Three modes for any moment**
>   • **Single** — one perfect shot. Tap, smile, send.
>   • **Collage** — four shots arranged into a 2×2 keepsake.
>   • **GIF** — six rapid frames stitched into a looping animation.
>
> **A real photo-booth experience**
>   • Friendly countdown coaching ("Look right here", "Big smile")
>   • Customizable pose prompts between shots ("Group hug!", "Goofy face")
>   • A clear "↑ LOOK HERE" pointer to the lens so every photo catches eyes
>   • Soft auto-accept review with a pause-on-tap progress sweep
>
> **Sharing that doesn't feel like 2010**
>   • Email the photo as an attachment via your own Resend account (optional)
>   • QR-code download via your own Cloudinary account (optional)
>   • Android system share sheet for AirDrop-style handoff
>   • Print to any AirPrint / Mopria-compatible printer
>
> **Built for hosts who actually use it**
>   • True kiosk lockdown via Android Device Owner mode
>   • Per-event Cloudinary folders for clean photo organization
>   • On-device audit log of every send (recipients masked for privacy)
>   • Per-address and per-session email rate limiting
>   • Wedding-friendly upscale palette — ivory, sage, champagne, taupe
>
> **Honest about your data**
>   • We (SnapCabin) collect nothing — no analytics, crash reports, or IDs
>   • Photos and email addresses flow only through services *you* configure
>   • Full privacy policy at https://snapcabin.app/privacy
>
> Built for hosts running 1–10 events per year who want a real photo
> booth they fully control. Refunds follow Google Play's standard
> refund policy.

## Graphics

### App icon
- **Size**: 512 × 512 px
- **Format**: PNG, 32-bit (with alpha)
- **Asset**: re-use `app/src/main/res/drawable/snapcabin_icon.png` if sized
  correctly; otherwise export a fresh version of just the cabin glyph (no
  wordmark) with ~20% transparent padding.

### Feature graphic
- **Size**: 1024 × 500 px
- **Format**: PNG or JPG, no alpha
- **Suggested composition**:
  - Background: ivory (`#FAF5EA`) with a subtle radial gradient toward cream
  - Left third: the full `snapcabin_logov2.png` (cabin + wordmark)
  - Right two-thirds: the headline "A photo booth in the woods" in Frank
    Ruhl Libre 56pt + a one-liner in Hanken Grotesk
  - No app screenshots inside the feature graphic — Play's guidelines
    discourage this

### Phone screenshots (2–8 required)
- **Size**: any size where the shorter edge is between 320–3840 px and the
  aspect ratio is between 16:9 and 9:16
- **Format**: PNG or JPG, 24-bit, no alpha
- Capture in landscape (the app is landscape-locked)

### 7-inch tablet screenshots (1–8 required)
- Same format rules; at landscape resolutions like 1024×600 or 1280×800

### 10-inch tablet screenshots (1–8 required)
- Same format rules; at landscape resolutions like 1920×1200 or 2560×1600

## Which screens make the best screenshots

In rough priority order:

1. **Attract** with the event name set — the front door. Bonus points for
   capturing the breathing CTA mid-bright.
2. **Mode select** — shows the animated mode previews
   (`AnimatedModePreview.kt`). Capture mid-animation so each card shows
   different state.
3. **Get Ready** — the framing oval + "You're in frame" pill + bottom
   instruction card all visible.
4. **Capture mid-count** — huge "2" with the LOOK HERE pointer pulsing.
5. **Review (Collage)** — assembled 2×2 with the action row.
6. **Share** — QR code + Email / Save / Print stack visible.
7. **Admin** showing the EVENT block at top — proves the per-event scoping
   to reviewers without making the listing itself look technical.

For each shot, capture from a 10" landscape tablet emulator running at
1920×1200 — gives Play one image you can crop down for the 7" and phone
sets.

## Capturing screenshots

```bash
# From a running emulator or connected device, in landscape:
adb shell screencap -p /sdcard/snap.png
adb pull /sdcard/snap.png ./play-assets/01-attract.png
```

Avoid status-bar artifacts — the kiosk's `windowFullscreen=true` theme hides
the bar, so they should be clean.

## Categorization

- **Primary category**: Photography
- **Tags**: photo booth, kiosk, events, weddings
- **App type**: App (not Game)

## Optional promo video

Skip for v1. If you do one later: 30 seconds, 16:9, hosted on YouTube as
unlisted, paste the URL into Play Console.
