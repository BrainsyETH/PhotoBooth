# Play Console — Content rating answers

Play uses the IARC (International Age Rating Coalition) questionnaire to
generate per-region ratings (ESRB for North America, PEGI for Europe, etc.).
The questions are blunt — answer them honestly and the result is what it is.

For SnapCabin the expected rating is **Everyone / PEGI 3 / IARC 3+**.

## Where to find it

Play Console → **Policy → App content → Content rating** → Start
questionnaire.

## Answers

**Email**: your developer contact email.

**Category**: **Utility, Productivity, Communication, or Other**.

> SnapCabin is a photo-booth kiosk app for event hosts. It's not a game,
> not social media, not an entertainment app per the IARC taxonomy.

### Violence
- Does your app contain any violence? → **No**
- Realistic-looking violence? → **No**
- Cartoon / fantasy violence? → **No**
- Blood / gore? → **No**

### Sexuality
- Sexual content / nudity? → **No**

### Language
- Profanity? → **No**
- Crude humor? → **No**

### Controlled substances
- References to alcohol, tobacco, drugs? → **No**

### Gambling
- Real or simulated gambling? → **No**

### User-generated content / social
- **Does your app allow users to interact or exchange content?** → **Yes**

> Justify: "Guests can capture photos of themselves and send those photos
> to themselves via email. There is no chat, no public posting, no
> ability for one user to send content to another user via the app — all
> sends are guest-to-self."

- **Are users able to share their physical location with other users?** → **No**
- **Does your app contain user-generated content that may be inappropriate
  for children?** → **No**

> Justify: "Photos are captured of whoever is standing in front of the
> kiosk at an event. The kiosk operator (event host) is responsible for who
> uses the kiosk."

### Miscellaneous
- Does the app allow users to purchase digital goods? → **No**
- Does the app share user location? → **No**
- Internet access? → **Yes** (for Cloudinary uploads and Resend API)
- Digital purchases? → **No**

## Expected outcome

- IARC global rating: **3+**
- ESRB (US): **Everyone**
- PEGI (EU): **3**

If the questionnaire produces a higher rating (e.g. because of how a
question got answered), review your answers and re-take it. Photo-booth
apps with self-portrait capture should land in the lowest tier.

## Re-rating

If the app gains any of the following, re-take the questionnaire:

- Public photo sharing / social features
- Chat / messaging between users
- In-app purchases
- Location features
- Anything resembling user-targeted advertising
