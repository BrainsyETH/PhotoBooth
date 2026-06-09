# Google Stitch Design Prompts — SnapCabin PhotoBooth

> **How to use**: Copy the **Global Design Brief** first, then append any screen-specific prompt below it when pasting into [stitch.withgoogle.com](https://stitch.withgoogle.com). This keeps the visual language consistent across all screens.

---

## Global Design Brief

Paste this at the **start** of every Stitch session:

```
Design a sleek, modern Jetpack Compose Material3 UI for an Android photo booth kiosk app called "SnapCabin". The app runs on tablets (8.7"–12.4") in LANDSCAPE orientation only.

Design system:
- Theme: Warm, light, upscale-wedding aesthetic — ivory & champagne, soft sage, gentle warmth. NOT dark.
- Background: Parchment #FAF5EA (ivory / linen)
- Surface/cards: Cream #FDFAF1 (near-white); recessed surfaces Oat #EEE5CF (soft sand)
- Primary action color: Pine #6B8F73 (fresh sage)
- Secondary action color: Walnut #8B7558 (warm taupe)
- Accent/highlight: Honey #C9A86A (champagne gold)
- Text: Espresso #322619 (warm ink)
- Muted text: Mist #B5A892 (soft warm grey)

Typography: Frank Ruhl Libre (serif) for display headings; Hanken Grotesk (sans) for body and UI labels. Bold display headings (48–96sp), large body text (20–24sp). Everything must be readable from 3 feet away.

Constraints:
- All tap targets minimum 56dp, prefer 80dp+ for primary actions
- Rounded corners on all interactive elements (12–16dp)
- No thin fonts, no small text, no dense layouts
- Landscape tablet only — never portrait phone
- Full-screen, no system bars visible
- Subtle animations welcome (fades, scales, pulses)
- Use Kotlin Jetpack Compose with Material3 components
```

---

## Screen 1: Attract (Welcome / Idle)

```
Design the idle welcome screen for a photo booth kiosk. This is the first thing users see — it must be inviting and obvious.

Layout: Full-screen, centered content, light Parchment background (#FAF5EA).

Elements:
- App logo/icon (96dp) and app name "SnapCabin" in large bold text (64sp) in a horizontal row, centered
- A large call-to-action "Tap Anywhere to Start" — a Pine (#6B8F73) rounded pill with white text, with a smooth pulsing opacity animation (fades between 55% and 100% over 2 seconds, infinite loop)
- Below the CTA, a subtle subtitle "EXPLORE THREE MODES" in muted text (Mist #B5A892) (50% opacity, 18sp, wide letter-spacing)
- In the bottom-right corner, a barely visible hint "Hold for settings" (12sp, 20% opacity Espresso)

Interactions:
- Tapping anywhere on the screen navigates to Mode Select
- Long-pressing anywhere on the screen navigates to Admin Settings (PIN-gated)

Style: Warm, elegant, inviting — upscale-wedding, not dark or cinematic. Think a tasteful welcome card on linen. The screen should feel alive with the gentle pulsing CTA but never busy.
```

---

## Screen 2: Mode Select

```
Design a mode selection screen where users choose between three photo booth modes.

Layout: Centered column. Title at top, three large cards in a horizontal row below.

Elements:
- Title: "Choose Your Mode" (36sp, SemiBold, centered, Espresso #322619)
- Three mode cards side by side (180dp wide each, 24dp gap between):

Card 1 — "Single Photo":
  - Icon area: Small colored box with camera icon, Walnut (#8B7558) accent
  - Title: "Single Photo" (SemiBold)
  - Description: "Strike a pose" (muted text)

Card 2 — "Collage":
  - Icon area: Pine (#6B8F73) accent
  - Title: "Collage"
  - Description: "Multiple shots, one frame"

Card 3 — "GIF":
  - Icon area: Honey (#C9A86A) accent
  - Title: "GIF"
  - Description: "Animated memories"

Card styling: Cream surface (#FDFAF1) background, rounded 16dp corners, subtle elevation or border. Each card should have a hover/press state with a slight scale or glow effect.

Note: Any of the three cards may be hidden (disabled by admin), so design them as independent elements. If only one card is visible, it should still look balanced.
```

---

## Screen 3: Capture (Camera)

```
Design the camera capture screen for a photo booth.

Layout: Full-screen camera preview with minimal overlay controls.

Elements:
- Full-screen camera preview (fills entire screen, no borders)
- A large "TAKE PHOTO" button at bottom center (80dp height, Walnut #8B7558 background, white bold text, rounded 16dp, elevated). Position 48dp from bottom edge.
- The button should disappear during countdown and capture

Overlay states:
- COUNTDOWN: A massive number (3, 2, 1) centered on screen over the camera preview. Each number should be 200sp+ bold white text with a slight scale-down animation as it transitions. Semi-transparent dark vignette behind the number for readability.
- FLASH: After capture, a brief full-screen white flash overlay (fades in and out quickly, ~200ms)
- ERROR: If camera unavailable, show centered error message on the Parchment background

Style: The camera preview is the hero. Keep the UI minimal — just the capture button and overlays. No chrome, no headers, no borders around the preview.
```

---

## Screen 4: Review

```
Design a photo review screen where the user decides to keep or retake their photo.

Layout: Full-screen photo display with floating controls.

Elements:
- Captured photo displayed full-screen (aspect-fit, centered, soft Oat (#EEE5CF) behind any letterboxing)
- Auto-accept timer bar at top center: Semi-transparent dark pill (#000000 at 60% opacity, rounded 20dp), containing Honey text "Auto-accepting in 8s" (the number counts down). Only visible when auto-accept is enabled.
- Two action buttons at bottom center in a row (48dp from bottom, 24dp gap):
  - "RETAKE" — Surface color (Oat #EEE5CF), white text, rounded 16dp, 80dp height
  - "ACCEPT" — Walnut (#8B7558), white text, rounded 16dp, 80dp height

Style: The photo is the star. Buttons should float over the image with subtle shadows. Clean, confident, minimal.
```

---

## Screen 5: Filters

```
Design a photo filter selection screen with a two-column layout.

Layout: Landscape two-column — photo preview on the left (flexible width), control panel on the right (360dp fixed width).

Left column:
- Large photo preview with the selected filter applied
- Rounded corners (12dp), centered in available space
- Loading spinner overlay when processing

Right column (360dp, Cream surface #FDFAF1, full height, 24dp padding):
- Section title "FILTERS" (TitleMedium, Honey, uppercase)
- Horizontal scrolling row of filter thumbnails:
  - Each thumbnail: 80x80dp, rounded 8dp
  - Selected thumbnail: 3dp Honey border
  - Filter name below each (11sp, Honey if selected, muted text (Mist #B5A892) otherwise)

- Section title "OVERLAYS" (TitleMedium, Honey, uppercase)
- Horizontal scrolling row of overlay chip/pills:
  - Pill shape (rounded 20dp)
  - Selected: Pine (#6B8F73) background
  - Unselected: Surface (Cream #FDFAF1) background with border
  - White text

- Bottom action buttons (Row, spaced):
  - "BACK" — Surface color
  - "DONE" — Walnut (#8B7558)

Style: Clean editorial layout. The preview should be large and dominant. The right panel should feel like a refined toolbar, not cluttered.
```

---

## Screen 6: Branding

```
Design an event branding screen where users add event name and date to their photo.

Layout: Landscape two-column — photo preview left (flexible), controls right (360dp fixed).

Left column:
- Large photo preview showing applied branding template in real-time
- Rounded corners (12dp), centered
- Loading spinner if processing

Right column (360dp, 24dp padding):
- Title: "EVENT BRANDING" (TitleMedium, Honey, uppercase)
- Scrollable list of branding templates:
  - Each template: Surface (Cream #FDFAF1) card, rounded 12dp, 16dp padding
  - Selected template: Pine (#6B8F73) background with 2dp Honey border
  - Template name in white (BodyLarge)
  - 6dp vertical spacing between items

- "Event Name" outlined text field:
  - Honey border when focused, muted border when unfocused
  - White text, single line

- "Event Date" outlined text field (same styling)

- Bottom action buttons:
  - "SKIP" — Surface color
  - "APPLY" — Walnut (#8B7558)

Style: Professional, event-oriented. The preview updates live as the user selects templates and types.
```

---

## Screen 7: Collage

```
Design a collage builder screen where users select a layout and capture multiple photos.

Layout: Landscape two-column — collage preview left (flexible), controls right (360dp fixed).

Left column:
- Collage preview image (assembled photos in chosen layout)
- Rounded corners (12dp), centered
- Placeholder text "Select a layout and take photos" when empty
- Loading spinner when assembling

Right column (360dp, 24dp padding):
- Section title "LAYOUT" (TitleMedium, Honey)
- Horizontal scrolling row of layout option chips:
  - Each chip: rounded 12dp, 16dp horizontal padding, 12dp vertical
  - Selected: Pine background, 2dp Honey border
  - Unselected: Surface (Cream #FDFAF1) background
  - Layout name + photo count (e.g., "Grid — 4 photos")

- Photo counter: "Photos: 2 / 4" (BodyLarge, Espresso)

- "TAKE PHOTO (2 more)" button — Honey (#C9A86A) background, full width, bold text. Only shown if more photos needed.
- "UNDO LAST" button — Surface color, full width. Only shown if photos exist.

- Bottom action buttons:
  - "CANCEL" — Surface color
  - "USE COLLAGE" — Walnut (#8B7558), disabled state if no preview ready

Style: Creative and playful but organized. The layout chips should visually hint at the collage arrangement if possible.
```

---

## Screen 8: GIF

```
Design a GIF creation screen where users capture multiple frames and adjust animation speed.

Layout: Landscape two-column — frame preview left (flexible), controls right (360dp fixed).

Left column:
- Animated preview cycling through captured frames
- Rounded corners (12dp), centered
- Placeholder text "Capture frames to preview" when empty
- Encoding overlay: Semi-transparent dark backdrop with centered spinner and "Creating GIF..." text

Right column (360dp, 24dp padding):
- Title: "CREATE GIF" (HeadlineSmall, Espresso, bold)
- Frame counter: "Frames: 3 / 6" (BodyLarge, Espresso)

- "TAKE FRAME" button — Honey (#C9A86A), full width. Hidden when max frames reached.
- "UNDO LAST" button — Surface, full width. Hidden when no frames.

- Speed control section:
  - Label: "Speed: 500ms per frame" (BodyMedium, Espresso)
  - Slider: range 100–2000ms
  - Slider thumb: Honey (#C9A86A)
  - Slider track: Pine (#6B8F73)

- Bottom action buttons:
  - "CANCEL" — Surface
  - "CREATE GIF" — Walnut (#8B7558), disabled if fewer than 2 frames

Style: Fun and dynamic. The animated preview should feel lively. Consider a subtle film-strip or frame-counter visual treatment.
```

---

## Screen 9: Share

```
Design a photo sharing screen with multiple sharing options and a QR code.

Layout: Landscape two-column — photo preview left (flexible), share options right (360dp fixed).

Left column:
- Final photo/collage/GIF displayed large
- Rounded corners (12dp), centered

Right column (360dp, 24dp padding, vertically scrollable):
- Title: "Share Your Photo" (HeadlineSmall, Espresso, bold)

- QR code section:
  - Label: "Scan to Download" (BodyMedium, muted text (Mist #B5A892))
  - QR code image: 160dp square, white background, rounded 8dp, 8dp padding
  - Clickable URL text below (Honey, small)

- Share action buttons (full width each, 12dp vertical spacing, rounded 12dp, 56dp+ height):
  - "SAVE TO GALLERY" — Pine (#6B8F73)
  - "SHARE" — Walnut (#8B7558)
  - "PRINT" — WalnutDeep (#6B5840)
  - "EMAIL" — Slate Blue (ShareDenim #6B96B0)

- "DONE" button at bottom — Surface color, full width

- Snackbar/toast area for feedback messages ("Saved!" etc.)

Style: Organized and inviting. Each share button should feel distinct with its color. The QR code should be prominent and clearly scannable. Consider subtle icons on each button.
```

---

## Screen 10: Thank You

```
Design a simple thank-you screen shown after the user finishes sharing their photo.

Layout: Full-screen, perfectly centered content, light Parchment background (#FAF5EA).

Elements:
- "Thank You!" in large bold display text (64sp, Espresso #322619)
- Below it (16dp spacing): "Your photo is ready" in Honey (#C9A86A), 24sp
- Optional: A subtle decorative element — a thin golden line, a soft glow, or a gentle fade-in animation

Behavior: This screen auto-dismisses after 5 seconds and returns to the Attract screen.

Style: Elegant, warm, grateful. Think luxury brand confirmation screen. Simple but polished. Consider a gentle scale-up or fade-in entrance animation.
```

---

## Screen 11: Admin Settings

```
Design a comprehensive admin settings panel for a photo booth kiosk. This screen is PIN-protected.

PART A — PIN Entry Modal:
- Centered modal card (Cream #FDFAF1, rounded 24dp, elevation)
- Title: "Admin PIN" (HeadlineMedium, Espresso)
- Password input field (OutlinedTextField, numeric keypad, masked dots)
  - Honey border when focused
- Error text in red if PIN is wrong
- Two buttons: "CANCEL" (Surface) and "ENTER" (Pine #6B8F73)
- Dark semi-transparent scrim behind modal

PART B — Settings Panel (after PIN verified):
Full-screen scrollable list with grouped sections. Each section has an uppercase Honey header.

Section: CAMERA
- "Use Front Camera" toggle switch
- "Mirror Front Camera" toggle switch
- Camera selection list (radio buttons for each detected camera)
- Photo Resolution selector (Low / Medium / Full as chips)

Section: MODES
- "Single Photo Mode" toggle
- "Collage Mode" toggle
- "GIF Mode" toggle

Section: CAPTURE
- "Countdown" slider (1–10 seconds) with value label
- "Flash Effect" toggle
- "Review Auto-Accept" slider (0–30 seconds, 0 = off) with value label

Section: SOUND
- "Sound Enabled" master toggle
- "Shutter Sound" toggle (disabled when sound off)
- "Countdown Beep" toggle (disabled when sound off)

Section: SHARING
- "Auto-Save to Gallery" toggle
- "QR Code Sharing" toggle
- "JPEG Quality" slider (50–100%) with value label

Section: KIOSK
- "Admin PIN" text field (4–8 digits)
- "Kiosk Mode" toggle
- "Idle Timeout" slider (15–300 seconds)
- "Screen Brightness" slider (10–100%)

Section: BRANDING
- "Watermark" toggle
- "Watermark Text" text field (visible when watermark on)
- "Custom Border" upload button with preview thumbnail
- "Custom Overlay" upload button with preview thumbnail

Section: TOOLS
- "PHOTO GALLERY" button (Pine)

Section: ABOUT
- "PRIVACY POLICY" button (Surface)
- Version display "v1.0.0" (muted text)

Bottom: "CLOSE SETTINGS" button (Walnut, full width, prominent)

Component styling:
- Setting rows: Surface (Cream #FDFAF1) cards, rounded 12dp, 16dp padding
- Toggle switches: Honey thumb/track when ON, gray when OFF
- Sliders: Honey thumb, Pine track
- Text fields: Honey border when focused
- Section spacing: 24dp between sections, 8dp between items within

Style: Clean, professional admin dashboard. Dense but readable. Clearly organized sections with visual hierarchy.
```

---

## Screen 12: Gallery

```
Design a photo gallery browser for reviewing previously captured photos.

Layout: Landscape two-column — photo grid/viewer left (flexible), controls right (300dp fixed).

Left column — Grid view (default):
- LazyVerticalGrid of photo thumbnails
- Adaptive columns (minimum 150dp per item)
- Each thumbnail: 4:3 aspect ratio, rounded 8dp, 4dp spacing
- Subtle hover/press effect

Left column — Detail view (when photo selected):
- Large photo display, Fit scale, rounded 12dp
- Smooth transition from grid to detail

Loading state: Honey spinner centered
Empty state: "No photos yet" in muted text (Mist #B5A892), centered

Right column (300dp, 24dp padding):
- Title: "GALLERY" (HeadlineSmall, Espresso)
- Photo count: "24 photos" (BodyMedium, muted text (Mist #B5A892))

When photo selected:
- "USE THIS PHOTO" button — Honey, full width
- "BACK TO GRID" button — Surface, full width

Always visible:
- "CLOSE" button — Walnut (#8B7558), full width, at bottom

Style: Clean gallery aesthetic. Thumbnails should feel like a curated collection. The detail view transition should be smooth and elegant.
```

---

## Screen 13: Privacy Policy

```
Design a simple privacy policy display screen.

Layout: Full-screen centered column, vertically scrollable, 32dp padding all sides.

Elements:
- Title: "Privacy Policy" (HeadlineSmall, Espresso, bold)
- Body text: Long-form privacy policy content (BodyLarge, Espresso #322619 at 85% opacity, comfortable line height)
- "CLOSE" button at the bottom — Walnut (#8B7558), centered

Style: Clean, readable, legal-document appropriate. Generous padding and line spacing. The text should be easy to read on a large tablet screen. No decorative elements — keep it serious and professional.
```

---

## Screen 14: Countdown Overlay (Component)

```
Design a fullscreen countdown overlay component that appears over the camera preview.

Layout: Full-screen overlay, content centered.

Elements:
- Semi-transparent dark vignette background (radial gradient from transparent center to dark edges)
- Massive countdown number (3, 2, 1) in bold white, 250sp+
- Each number transition: Scale from 1.5x to 1.0x with a fade-in, then fade-out to next number
- Optional: Subtle ring or circle animation around the number
- Optional: A progress arc that depletes as each second passes

Style: Dramatic, cinematic. Think movie countdown or photography timer. The numbers should command attention. High contrast against the camera preview behind.
```

---

## Integration Guide

After generating designs in Stitch, follow these steps to integrate:

### 1. Export the Compose code
- Copy the generated `@Composable` function from Stitch

### 2. Replace screen composables
- Each screen lives in `/app/src/main/java/com/snapcabin/ui/screens/<name>/`
- Replace the UI layout code inside the existing `@Composable` function
- **Keep** all ViewModel references, state collection (`collectAsState`), and callback lambdas

### 3. Apply the SnapCabin theme
- Stitch may generate its own colors — replace with references to `MaterialTheme.colorScheme` or direct color constants from `Color.kt`
- Replace any typography with `MaterialTheme.typography` references from `Type.kt`
- Wrap everything in `SnapCabinTheme { }` (already done in `NavGraph.kt`)

### 4. Preserve business logic
These should NOT change:
- `ViewModel` classes and `UiState` data classes
- `NavGraph.kt` navigation routes
- `CameraManager`, `GifEncoder`, `CollageTemplate`
- `SettingsManager`, `PhotoSaver`, `QrCodeGenerator`
- `KioskManager`, `SoundManager`

### 5. Test on target hardware
- Test on actual tablet sizes (8.7"–12.4")
- Verify landscape orientation
- Check all tap targets are easily pressable
- Verify text readability from 3 feet distance

### Color Quick Reference

`app/src/main/java/com/snapcabin/ui/theme/Color.kt` is the source of truth — if
this table ever disagrees with it, the file wins.

| Constant | Hex | Usage |
|----------|-----|-------|
| Parchment | `#FAF5EA` | Screen backgrounds (ivory / linen) |
| Cream | `#FDFAF1` | Cards, panels, inputs (near-white) |
| Oat | `#EEE5CF` | Recessed surfaces, secondary buttons (soft sand) |
| Pine | `#6B8F73` | Primary buttons, selected states (fresh sage) |
| PineDeep | `#52755A` | Pressed primary |
| Walnut | `#8B7558` | Action buttons (Accept, Done, Close) (warm taupe) |
| WalnutDeep | `#6B5840` | Secondary text / pressed taupe |
| Honey | `#C9A86A` | Highlights, selected borders, CTAs (champagne gold) |
| HoneyDeep | `#A8804A` | High-contrast champagne on light |
| Clay | `#C4866A` | Warm pop / errors (soft terracotta) |
| Espresso | `#322619` | Text, icons (warm ink) |
| Mist | `#B5A892` | Muted text / hints (soft warm grey) |
| ShareDenim | `#6B96B0` | Email share button (slate blue) |

The legacy `Cabin*` aliases (`CabinBackground`, `CabinSurface`, `CabinPrimary`,
`CabinSecondary`, `CabinAccent`, `CabinOnSurface`, …) still exist in `Color.kt`
and now point at the warm constants above — so older screen code keeps working,
but prefer the named constants for new work.

Fonts: `FrankRuhlLibre` (serif display) and `HankenGrotesk` (sans body/UI), both
defined in `Type.kt`.
