# Play Console — Target audience and content

This panel determines whether your app falls under Play's
"Designed for Families" program or its broader audience-targeting rules.
SnapCabin is operated by adults at events, not designed for children, and
should be set accordingly.

## Where to find it

Play Console → **Policy → App content → Target audience and content**.

## Step 1 — Target age groups

Select the age groups your app targets. **Select 18 and over only**.

> Justification: SnapCabin is operated by adult event hosts and venue
> staff. Guests at events may include any age, but the app's *target
> audience* (the people who install and configure it) is adults.

If Play warns "Selecting 18+ means you confirm the app is not designed
for children" — yes, that's correct.

## Step 2 — Appeal to children

**"Could children also be interested in your app?"** → **No**

> Justification: The app's UI (admin settings, Resend configuration, kiosk
> lockdown via ADB, event slug management) is operator-facing and not
> designed to appeal to children. Children may *appear* in photos taken
> at events, but the app itself is not marketed to or designed for
> children.

## Step 3 — Store listing presence

You'll be asked if any of the following are in your listing:

- Imagery / language designed to appeal to children → **No**
- Child celebrities → **No**
- Cartoon / animated characters appealing to children → **No**

The wedding-palette landing page, the cabin logo, and the event-host
marketing copy are all clearly adult-targeted.

## Step 4 — Content guidelines

**Does your app provide privacy and security disclosures for children?**
→ **No** (not applicable — app does not target children).

## Step 5 — Family Policy compliance

Because the app is **18+** and not designed to appeal to children, the
Families Policy section does not apply. Play will accept the absence of
COPPA / GDPR-K-style disclosures.

## What if a reviewer pushes back?

Occasionally a reviewer flags photo apps as "may appeal to children"
because of the camera. If that happens:

1. Reply citing this doc.
2. Point to the listing copy, which is explicitly written for "weddings,
   parties, and gatherings" with no child-targeted language.
3. Confirm that no in-app UI uses cartoon characters, gamification, or
   child-friendly imagery (it doesn't — the design system is the
   wedding sage / cream / champagne palette documented in `web/`).

## Ads

**Does your app contain ads?** → **No**

SnapCabin has no advertising of any kind. This makes the audience
questionnaire simpler — no ad-network compliance to address.

## In-app purchases

**Does your app contain in-app purchases?** → **No**

The business model is a one-time app purchase (or sideload license).
Cloudinary and Resend are configured per-operator with their own
billing relationships outside the app.
