# CustomRPC (Codename: Archangel)

A native Android app (Kotlin + Gradle) for Discord Rich Presence. This is **not** a web project — there is no server or browser preview. The "workflow" in this Replit builds the APK.

## Project layout
- `app/` — Android module (Kotlin source, resources, `AndroidManifest.xml`)
- `gradle/libs.versions.toml` — version catalog (AGP 8.13.2, Kotlin 2.0.21)
- `build.gradle.kts` / `settings.gradle.kts` — top-level Gradle build
- `app/build/outputs/apk/debug/app-debug.apk` — built APK (after running the build workflow)

## Toolchain (installed in this environment)
- Java: GraalVM CE 22.3.1 (OpenJDK 19) via Replit module `java-graalvm22.3`
- Android SDK at `$HOME/android-sdk` with: `platform-tools`, `platforms;android-36`, `build-tools;36.0.0`
- System: `unzip`

## Build workflow
Workflow name: **Build APK** — runs `bash scripts/build-apks.sh`.

The script builds debug + release APKs sequentially (separate Gradle invocations to avoid Kotlin daemon storage conflicts), then zip-aligns and signs the release APK with `apksigner` using the keystore at `keystore/customrpc-release.keystore` (alias `customrpc`, both passwords `customrpc`).

Outputs (copied into `outputs/`):
- `outputs/app-debug.apk` — debuggable build
- `outputs/app-release-unsigned.apk` — release build, unsigned (use this if you want to sign with your own key)
- `outputs/app-release-signed.apk` — release build, signed with the bundled keystore (ready to install)

Install on an Android device by transferring the desired APK and opening it (allow installs from unknown sources).

## Versioning
- `versionCode` and `versionName` live in `app/build.gradle.kts` — current: `versionCode 4`, `versionName "1.1"`.

## Recent changes
- **Streaming activity URL**: added a `Stream URL` text field shown only when `Activity Type = Streaming`. The URL is persisted alongside other settings and sent to Discord as the activity `url` field.
- **Custom Party ID**: optional input under the party size/max row. Persisted and forwarded to Discord (skips auto-generated UUID when set).
- **Live preview card**: top of the configuration screen renders a Discord-style activity card that updates as the user types name/details/state and changes activity type.
- **Advanced actions row**: Reset (clears all presence fields with confirmation, keeps token), Export (copies a versioned JSON of all settings to clipboard), Import (parses such a JSON back).
- **Consistent EN/ID strings**: rebuilt both `values/strings.xml` and `values-in/strings.xml` so every key exists in both languages with matching meaning. New keys: `section_*`, `preview_*`, `btn_export/import/reset_settings`, `msg_settings_*`, `dialog_reset_*`, `hint_party_id`, `btn_yes/no`.
- **Layout fixes**: corrected four `text=` typos to `android:text=` on the large/small image and button labels.
- **Release signing**: generated 10000-day RSA-2048 keystore at `keystore/customrpc-release.keystore`. Release build type emits `app-release-unsigned.apk`; the build script then zip-aligns and signs a copy with `apksigner` (V1+V2+V3) to produce `app-release-signed.apk`.
