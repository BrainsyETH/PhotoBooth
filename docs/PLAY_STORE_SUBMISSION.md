# Play Store submission runbook

Master checklist for getting SnapCabin live on the Play Store. Each numbered
step references a dedicated doc with the exact wording / answers to use.
Follow in order — earlier steps unblock later ones.

## Before you start

You'll need:

- A Google Play Console developer account ($25 one-time).
- The release keystore generated per **[RELEASE_BUILD.md](./RELEASE_BUILD.md)**.
- The privacy policy live at <https://snapcabin.app/privacy>.
- A signed Android App Bundle (`.aab`).

## 1. Build the AAB

Follow [RELEASE_BUILD.md](./RELEASE_BUILD.md). Output:
`app/build/outputs/bundle/release/app-release.aab`.

**Always bump `versionCode`** in `app/build.gradle.kts` before every upload —
the Play Store rejects re-uploads with the same versionCode.

## 2. Create the Play Console app entry

Play Console → Create app:

- **App name**: SnapCabin
- **Default language**: English (United States)
- **App or game**: App
- **Free or paid**: Free (or Paid, if you're selling it directly through Play)
- **Declarations**: confirm both checkboxes (Developer Program Policies, US export laws)

## 3. Upload to Internal testing first

**Always.** Internal testing has a near-zero review window (~1 hour) and is
the cheapest way to catch a malformed bundle or signing mistake.

Play Console → Testing → Internal testing → Create new release → upload AAB.

Once Play accepts the bundle, promote it through the tracks at your pace:
Internal → Closed → Open → Production.

## 4. Fill out App content

Each panel in **Policy → App content** has a separate doc:

| Panel | Doc |
|---|---|
| Privacy policy | URL = `https://snapcabin.app/privacy` |
| App access | [APP_ACCESS.md](./APP_ACCESS.md) |
| Ads | "No" — SnapCabin contains no ads |
| Content rating | [CONTENT_RATING.md](./CONTENT_RATING.md) |
| Target audience | [FAMILY_POLICY.md](./FAMILY_POLICY.md) |
| News app | "No" |
| COVID-19 contact tracing | "No" |
| Data safety | [DATA_SAFETY.md](./DATA_SAFETY.md) |
| Government apps | "No" |
| Financial features | "No" |
| Health features | "No" |
| Device admin permissions | [DEVICE_OWNER_DISCLOSURE.md](./DEVICE_OWNER_DISCLOSURE.md) |

## 5. Store listing

Per [LISTING_ASSETS.md](./LISTING_ASSETS.md):

- Short description (80 chars)
- Full description (4000 chars)
- App icon (512×512)
- Feature graphic (1024×500)
- Screenshots — at least 2 per device class (phone, 7" tablet, 10" tablet)
- Category: **Photography** primary
- Tags: photo booth, kiosk, events
- Contact email / website / phone (optional)

## 6. Pricing and availability

Choose countries. Default to "All available" unless you have a reason to
exclude. Photo apps are generally well-accepted globally.

## 7. Promote through testing tracks

Recommended cadence:

- **Internal testing**: just you and one or two trusted hosts. 1–2 weeks.
- **Closed testing**: 10–100 invited beta operators. 2–4 weeks.
- **Open testing** (optional): public beta if you want broader feedback.
- **Production**: full launch.

Closed testing requires a written description of "what testers will be
testing" — keep it short ("Photo booth kiosk for event hosts; please report
camera, email delivery, and kiosk lockdown bugs").

## 8. What to expect from review

- First Production submission: **3–7 days** review window.
- Subsequent updates: **a few hours to 2 days**.
- Common rejection causes for an app like SnapCabin:
  - Privacy policy not reachable / out of date
  - Data Safety form doesn't match what the app actually does
  - Device admin permission undeclared in App Content
  - Background activity not justified (we use a foreground service —
    declared)
  - Sensitive permission usage not disclosed (CAMERA is fine but our
    `BIND_DEVICE_ADMIN` is the one that draws attention)

If rejected, Play emails you the policy citation and a fix window
(usually 14 days). Address it, bump versionCode, re-upload.

## 9. After approval

- Tag the release in git: `git tag v1.0.0 && git push --tags`
- Save the AAB and the matching ProGuard mapping file
  (`app/build/outputs/mapping/release/mapping.txt`) — uploading the mapping
  to Play Console lets future-you deobfuscate native stack traces.
- Update the privacy policy "Last updated" date if anything changed
  data-flow-wise.
