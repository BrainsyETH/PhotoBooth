# SnapCabin web (snapcabin.app)

Next.js 15 + Tailwind site that serves the marketing landing page and the
public privacy policy referenced from inside the Android app
(`R.string.privacy_view_online_url`).

## Local development

```
cd web
npm install
npm run dev
```

Visit `http://localhost:3000` and `http://localhost:3000/privacy`.

## Vercel setup — one-time

Because this repo is a monorepo (Android app at the root, web app in `/web`),
you must point Vercel at the `web/` subdirectory:

1. Vercel Dashboard → your project → **Settings → General → Root Directory**
2. Click **Edit**, set the value to: `web`
3. Save.

After that, Vercel auto-detects Next.js, runs `npm install` + `next build`
inside `web/`, and deploys. No `vercel.json` is required.

The `snapcabin.app` domain points to this project.

## Updating the privacy policy

The hosted policy lives at `app/privacy/page.tsx`. Any material change there
should be mirrored in the in-app summary at
`../app/src/main/res/values/strings.xml` (`privacy_body`) and the
`LAST_UPDATED` constant in `app/privacy/page.tsx` bumped.

## Keeping Next.js patched

Run `npm audit` periodically. If a Next.js CVE drops, bump the version in
`package.json` and redeploy. The current pin (`^15.5.5`) covers
CVE-2025-66478.
