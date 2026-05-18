# Building a signed release AAB

## 1. Generate the release keystore (one time, ever)

A keystore is a file holding the private key that signs your app. Lose it and
you can never publish updates to the same Play listing again — Play uses the
key fingerprint as identity. **Back this up off-machine immediately.**

```bash
mkdir -p app/keystore
keytool -genkey -v \
  -keystore app/keystore/release.keystore \
  -alias snapcabin \
  -keyalg RSA -keysize 2048 \
  -validity 10000
```

You'll be prompted for:
- A **keystore password** (the `KEYSTORE_PASSWORD` env var below)
- Your name, org, locality, etc. (used in the certificate; cosmetic)
- A **key password** (the `KEY_PASSWORD` env var below) — usually the same as
  the keystore password to keep your life simple

The `.gitignore` excludes `*.keystore` and `keystore/`, so this file stays
out of git. **Copy it to at least two safe places** (1Password, iCloud, an
encrypted drive — anywhere durable that's not just this laptop).

## 2. Set the env vars

The signing config in `app/build.gradle.kts` will **fail-fast** if these are
missing during a release build — no insecure defaults. Add them to your shell
profile:

```bash
export KEYSTORE_PASSWORD='your-keystore-password'
export KEY_ALIAS='snapcabin'
export KEY_PASSWORD='your-key-password'
```

For one-off builds you can prefix the command instead:

```bash
KEYSTORE_PASSWORD=... KEY_ALIAS=snapcabin KEY_PASSWORD=... ./gradlew bundleRelease
```

## 3. Bump versionCode

Every Play upload needs a unique, monotonically-increasing `versionCode`.

Edit `app/build.gradle.kts`:

```kotlin
defaultConfig {
    versionCode = 2          // bump every release
    versionName = "1.0.1"    // user-facing, semver
    ...
}
```

A simple scheme: `versionCode = <integer that always goes up>`. Many teams
use the build date in `YYMMDD` format (`260518`) or just a counter (1, 2, 3).
The Play Store doesn't care which, only that it increments.

## 4. Build

```bash
./gradlew clean bundleRelease
```

Output: `app/build/outputs/bundle/release/app-release.aab`.

The build also produces a ProGuard/R8 mapping file at
`app/build/outputs/mapping/release/mapping.txt`. Save it alongside the AAB —
Play Console accepts it as a separate upload to deobfuscate future crash
stack traces.

## 5. Verify the AAB locally (optional but recommended)

Install [bundletool](https://github.com/google/bundletool/releases):

```bash
brew install bundletool

bundletool build-apks \
  --bundle=app/build/outputs/bundle/release/app-release.aab \
  --output=preview.apks \
  --ks=app/keystore/release.keystore \
  --ks-key-alias=snapcabin
bundletool install-apks --apks=preview.apks
```

This installs the release build on a connected device exactly as Play would
deliver it — splits, language packs, density buckets and all.

## 6. Upload to Play Console

Play Console → Testing → Internal testing → Create new release → drag in
`app-release.aab`. Internal testing is the right first stop for any new
build; once it passes you can promote to Closed / Open / Production from
the same release page.

See [PLAY_STORE_SUBMISSION.md](./PLAY_STORE_SUBMISSION.md) for the full
submission runbook.

## CI signing (future)

For automated builds, store the three env vars in your CI's secret manager.
Never echo them; the failure messages in `build.gradle.kts` are
specifically worded to not include the values.
