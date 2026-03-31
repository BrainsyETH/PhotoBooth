# Google Stitch Design Prompts — SnapCabin PhotoBooth

> **How to use**: Copy the **Global Design Brief** first, then append any screen-specific prompt below it when pasting into [stitch.withgoogle.com](https://stitch.withgoogle.com). This keeps the visual language consistent across all screens.

---

## Global Design Brief

Paste this at the **start** of every Stitch session:

```
Design a sleek, modern Jetpack Compose Material3 UI for an Android photo booth kiosk app called "SnapCabin". The app runs on tablets (8.7"–12.4") in LANDSCAPE orientation only.

Design system:
- Theme: Dark, premium, warm cabin-lodge aesthetic
- Background: Dark Charcoal #1A1A17
- Surface/cards: Warm Dark #2C2C26
- Primary action color: Forest Green #2E7D32
- Secondary action color: Warm Wood Brown #8D6E63
- Accent/highlight: Golden Amber #C8A96E
- Text: Warm Off-White #F5F0E8
- Muted text: #F5F0E8 at 50% opacity

Typography: Bold display headings (48–96sp), large body text (20–24sp). Everything must be readable from 3 feet away.

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

Layout: Full-screen, centered content, dark background (#1A1A17).

Elements:
- App logo/icon (96dp) and app name "SnapCabin" in large bold text (64sp) in a horizontal row, centered
- A large call-to-action "Tap Anywhere to Start" in Golden Amber (#C8A96E) with a smooth pulsing opacity animation (fades between 50% and 100% over 2 seconds, infinite loop)
- Below the CTA, a subtle subtitle "EXPLORE THREE MODES" in muted white (50% opacity, 18sp, wide letter-spacing)
- In the bottom-right corner, a barely visible hint "Hold for settings" (12sp, 20% opacity white)

Interactions:
- Tapping anywhere on the screen navigates to Mode Select
- Long-pressing (3 seconds) navigates to Admin Settings

Style: Cinematic, minimal, dramatic. Think premium event photo booth. The screen should feel alive with the pulsing animation but not busy.
```

---

## Screen 2: Mode Select

```
Design a mode selection screen where users choose between three photo booth modes.

Layout: Centered column. Title at top, three large cards in a horizontal row below.

Elements:
- Title: "Choose Your Mode" (36sp, SemiBold, centered, Off-White)
- Three mode cards side by side (180dp wide each, 24dp gap between):

Card 1 — "Single Photo":
  - Icon area: Small colored box with camera icon, Warm Wood Brown (#8D6E63) accent
  - Title: "Single Photo" (SemiBold)
  - Description: "Strike a pose" (muted text)

Card 2 — "Collage":
  - Icon area: Forest Green (#2E7D32) accent
  - Title: "Collage"
  - Description: "Multiple shots, one frame"

Card 3 — "GIF":
  - Icon area: Golden Amber (#C8A96E) accent
  - Title: "GIF"
  - Description: "Animated memories"

Card styling: Dark surface (#2C2C26) background, rounded 16dp corners, subtle elevation or border. Each card should have a hover/press state with a slight scale or glow effect.

Note: Any of the three cards may be hidden (disabled by admin), so design them as independent elements. If only one card is visible, it should still look balanced.
```

---

## Screen 3: Capture (Camera)

```
Design the camera capture screen for a photo booth.

Layout: Full-screen camera preview with minimal overlay controls.

Elements:
- Full-screen camera preview (fills entire screen, no borders)
- A large "TAKE PHOTO" button at bottom center (80dp height, Warm Wood Brown #8D6E63 background, white bold text, rounded 16dp, elevated). Position 48dp from bottom edge.
- The button should disappear during countdown and capture

Overlay states:
- COUNTDOWN: A massive number (3, 2, 1) centered on screen over the camera preview. Each number should be 200sp+ bold white text with a slight scale-down animation as it transitions. Semi-transparent dark vignette behind the number for readability.
- FLASH: After capture, a brief full-screen white flash overlay (fades in and out quickly, ~200ms)
- ERROR: If camera unavailable, show centered error message on dark background

Style: The camera preview is the hero. Keep the UI minimal — just the capture button and overlays. No chrome, no headers, no borders around the preview.
```

---

## Screen 4: Review

```
Design a photo review screen where the user decides to keep or retake their photo.

Layout: Full-screen photo display with floating controls.

Elements:
- Captured photo displayed full-screen (aspect-fit, centered, dark background behind any letterboxing)
- Auto-accept timer bar at top center: Semi-transparent dark pill (#000000 at 60% opacity, rounded 20dp), containing Golden Amber text "Auto-accepting in 8s" (the number counts down). Only visible when auto-accept is enabled.
- Two action buttons at bottom center in a row (48dp from bottom, 24dp gap):
  - "RETAKE" — Surface color (#2C2C26), white text, rounded 16dp, 80dp height
  - "ACCEPT" — Warm Wood Brown (#8D6E63), white text, rounded 16dp, 80dp height

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

Right column (360dp, dark surface #2C2C26, full height, 24dp padding):
- Section title "FILTERS" (TitleMedium, Golden Amber, uppercase)
- Horizontal scrolling row of filter thumbnails:
  - Each thumbnail: 80x80dp, rounded 8dp
  - Selected thumbnail: 3dp Golden Amber border
  - Filter name below each (11sp, Golden Amber if selected, muted white otherwise)

- Section title "OVERLAYS" (TitleMedium, Golden Amber, uppercase)
- Horizontal scrolling row of overlay chip/pills:
  - Pill shape (rounded 20dp)
  - Selected: Forest Green (#2E7D32) background
  - Unselected: Surface (#2C2C26) background with border
  - White text

- Bottom action buttons (Row, spaced):
  - "BACK" — Surface color
  - "DONE" — Warm Wood Brown (#8D6E63)

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
- Title: "EVENT BRANDING" (TitleMedium, Golden Amber, uppercase)
- Scrollable list of branding templates:
  - Each template: Surface (#2C2C26) card, rounded 12dp, 16dp padding
  - Selected template: Forest Green (#2E7D32) background with 2dp Golden Amber border
  - Template name in white (BodyLarge)
  - 6dp vertical spacing between items

- "Event Name" outlined text field:
  - Golden Amber border when focused, muted border when unfocused
  - White text, single line

- "Event Date" outlined text field (same styling)

- Bottom action buttons:
  - "SKIP" — Surface color
  - "APPLY" — Warm Wood Brown (#8D6E63)

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
- Section title "LAYOUT" (TitleMedium, Golden Amber)
- Horizontal scrolling row of layout option chips:
  - Each chip: rounded 12dp, 16dp horizontal padding, 12dp vertical
  - Selected: Forest Green background, 2dp Golden Amber border
  - Unselected: Surface (#2C2C26) background
  - Layout name + photo count (e.g., "Grid — 4 photos")

- Photo counter: "Photos: 2 / 4" (BodyLarge, white)

- "TAKE PHOTO (2 more)" button — Golden Amber (#C8A96E) background, full width, bold text. Only shown if more photos needed.
- "UNDO LAST" button — Surface color, full width. Only shown if photos exist.

- Bottom action buttons:
  - "CANCEL" — Surface color
  - "USE COLLAGE" — Warm Wood Brown (#8D6E63), disabled state if no preview ready

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
- Title: "CREATE GIF" (HeadlineSmall, white, bold)
- Frame counter: "Frames: 3 / 6" (BodyLarge, white)

- "TAKE FRAME" button — Golden Amber (#C8A96E), full width. Hidden when max frames reached.
- "UNDO LAST" button — Surface, full width. Hidden when no frames.

- Speed control section:
  - Label: "Speed: 500ms per frame" (BodyMedium, white)
  - Slider: range 100–2000ms
  - Slider thumb: Golden Amber (#C8A96E)
  - Slider track: Forest Green (#2E7D32)

- Bottom action buttons:
  - "CANCEL" — Surface
  - "CREATE GIF" — Warm Wood Brown (#8D6E63), disabled if fewer than 2 frames

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
- Title: "Share Your Photo" (HeadlineSmall, white, bold)

- QR code section:
  - Label: "Scan to Download" (BodyMedium, muted white)
  - QR code image: 160dp square, white background, rounded 8dp, 8dp padding
  - Clickable URL text below (Golden Amber, small)

- Share action buttons (full width each, 12dp vertical spacing, rounded 12dp, 56dp+ height):
  - "SAVE TO GALLERY" — Forest Green (#2E7D32)
  - "SHARE" — Warm Wood Brown (#8D6E63)
  - "PRINT" — Dark Brown (#5D4037)
  - "EMAIL" — Blue (#2980B9)
  - "SMS" — Green (#27AE60)

- "DONE" button at bottom — Surface color, full width

- Snackbar/toast area for feedback messages ("Saved!" etc.)

Style: Organized and inviting. Each share button should feel distinct with its color. The QR code should be prominent and clearly scannable. Consider subtle icons on each button.
```

---

## Screen 10: Thank You

```
Design a simple thank-you screen shown after the user finishes sharing their photo.

Layout: Full-screen, perfectly centered content, dark background (#1A1A17).

Elements:
- "Thank You!" in large bold display text (64sp, Warm Off-White #F5F0E8)
- Below it (16dp spacing): "Your photo is ready" in Golden Amber (#C8A96E), 24sp
- Optional: A subtle decorative element — a thin golden line, a soft glow, or a gentle fade-in animation

Behavior: This screen auto-dismisses after 5 seconds and returns to the Attract screen.

Style: Elegant, warm, grateful. Think luxury brand confirmation screen. Simple but polished. Consider a gentle scale-up or fade-in entrance animation.
```

---

## Screen 11: Admin Settings

```
Design a comprehensive admin settings panel for a photo booth kiosk. This screen is PIN-protected.

PART A — PIN Entry Modal:
- Centered modal card (Surface #2C2C26, rounded 24dp, elevation)
- Title: "Admin PIN" (HeadlineMedium, white)
- Password input field (OutlinedTextField, numeric keypad, masked dots)
  - Golden Amber border when focused
- Error text in red if PIN is wrong
- Two buttons: "CANCEL" (Surface) and "ENTER" (Forest Green #2E7D32)
- Dark semi-transparent scrim behind modal

PART B — Settings Panel (after PIN verified):
Full-screen scrollable list with grouped sections. Each section has an uppercase Golden Amber header.

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
- "PHOTO GALLERY" button (Forest Green)

Section: ABOUT
- "PRIVACY POLICY" button (Surface)
- Version display "v1.0.0" (muted text)

Bottom: "CLOSE SETTINGS" button (Warm Wood Brown, full width, prominent)

Component styling:
- Setting rows: Surface (#2C2C26) cards, rounded 12dp, 16dp padding
- Toggle switches: Golden Amber thumb/track when ON, gray when OFF
- Sliders: Golden Amber thumb, Forest Green track
- Text fields: Golden Amber border when focused
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

Loading state: Golden Amber spinner centered
Empty state: "No photos yet" in muted white, centered

Right column (300dp, 24dp padding):
- Title: "GALLERY" (HeadlineSmall, white)
- Photo count: "24 photos" (BodyMedium, muted white)

When photo selected:
- "USE THIS PHOTO" button — Golden Amber, full width
- "BACK TO GRID" button — Surface, full width

Always visible:
- "CLOSE" button — Warm Wood Brown (#8D6E63), full width, at bottom

Style: Clean gallery aesthetic. Thumbnails should feel like a curated collection. The detail view transition should be smooth and elegant.
```

---

## Screen 13: Privacy Policy

```
Design a simple privacy policy display screen.

Layout: Full-screen centered column, vertically scrollable, 32dp padding all sides.

Elements:
- Title: "Privacy Policy" (HeadlineSmall, white, bold)
- Body text: Long-form privacy policy content (BodyLarge, white at 85% opacity, comfortable line height)
- "CLOSE" button at the bottom — Warm Wood Brown (#8D6E63), centered

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
| Token | Hex | Usage |
|-------|-----|-------|
| CabinBackground | `#1A1A17` | Screen backgrounds |
| CabinSurface | `#2C2C26` | Cards, panels, inputs |
| CabinPrimary | `#2E7D32` | Primary buttons, selected states |
| CabinSecondary | `#8D6E63` | Action buttons (Accept, Done, Close) |
| CabinAccent | `#C8A96E` | Highlights, selected borders, CTAs |
| CabinOnSurface | `#F5F0E8` | Text, icons |
| CabinPrimaryVariant | `#1B5E20` | Pressed/dark primary states |
