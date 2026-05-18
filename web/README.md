# SnapCabin web (snapcabin.app)

Next.js 15 + Tailwind site that serves the marketing landing page and the
public privacy policy referenced from inside the Android app
(`R.string.privacy_view_online_url`).

## Local development

```
cd web
pnpm install   # or npm/yarn
pnpm dev
```

Visit `http://localhost:3000` and `http://localhost:3000/privacy`.

## Deploying

Vercel auto-deploys from the repo. The repo root has a `vercel.json` that
sets `web/` as the project root so the Android source above doesn't confuse
the build. The `snapcabin.app` domain points to this project.

## Editing the privacy policy

The hosted policy lives at `app/privacy/page.tsx`. Any material change there
should be mirrored in the in-app summary at
`../app/src/main/res/values/strings.xml` (`privacy_body`) and the
"Last updated" date bumped.
