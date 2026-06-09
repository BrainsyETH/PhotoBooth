# SnapCabin

A photo booth in the woods — self-hosted Android kiosk app for weddings,
parties, and gatherings. Wedding-friendly sage/cream/champagne palette;
no analytics, no telemetry; runs entirely on hardware you bring.
$2.99 once per device on Google Play.

## Repo layout

```
.
├── app/             Android app (Kotlin + Jetpack Compose)
├── web/             Next.js site for snapcabin.app (landing + privacy policy)
├── docs/            Operator + Play Store submission docs
├── gradle/          Gradle wrapper + version catalog
└── README.md        you are here
```

## Quick links

- **Operator setup** → [docs/KIOSK_SETUP.md](./docs/KIOSK_SETUP.md)
- **Release build** → [docs/RELEASE_BUILD.md](./docs/RELEASE_BUILD.md)
- **Play Store submission** → [docs/PLAY_STORE_SUBMISSION.md](./docs/PLAY_STORE_SUBMISSION.md)
- **Threat model + privacy** → [docs/THREAT_MODEL.md](./docs/THREAT_MODEL.md)
- **Hosted privacy policy** → <https://snapcabin.app/privacy>

## Developing the Android app

```bash
./gradlew :app:installDebug
adb shell am start -n com.snapcabin.debug/com.snapcabin.MainActivity
```

The debug variant has `applicationIdSuffix = ".debug"` so it installs
side-by-side with any release build.

## Developing the website

```bash
cd web
npm install
npm run dev   # http://localhost:3000
```

For Vercel deployment, set the project's **Root Directory** to `web` in
the Vercel dashboard (Settings → General → Root Directory). That points
the build at the Next.js subproject so the Android source above doesn't
get fed into the web build. See [web/README.md](./web/README.md).

## Releasing

See [docs/RELEASE_BUILD.md](./docs/RELEASE_BUILD.md) for keystore
generation, env-var setup, and `./gradlew bundleRelease`. The signing
config is **fail-fast** — release builds without the env vars will
abort with an explicit error, never with the legacy `snapcabin123`
default.
