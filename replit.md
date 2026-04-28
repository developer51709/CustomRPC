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

## Features implemented (as of last build)
- Login via Discord (WebView token sniffer) or manual token entry
- RPC service with Gateway WebSocket (auto-reconnect, foreground service, wake/wifi locks)
- Config page with live preview, all presence fields, asset picker, import/export/reset
- Token copy & revoke (via Discord API)
- Logs view (AppLogger singleton, real-time updates)
- **RPC Rotation Mode** — up to N presets cycle on a configurable interval (30 s – 10 min). Managed via the rotation card on the dashboard or "Save as Rotation Preset" in config Advanced section.
- **Device App Tracker** — shows the currently active foreground app in the presence card when connected. Requires Usage Access permission (`PACKAGE_USAGE_STATS`). Tapping the label prompts the user to grant it if not already granted.

## Key files
- `MainActivity.kt` — single-activity, 7 views (login, dashboard, settings, about, logs, presets, app-tracker)
- `RpcService.kt` — foreground service with rotation timer and gateway lifecycle
- `RotationPreset.kt` — rotation preset data class with full field set + JSON serialization
- `AppLogger.kt` — in-memory log ring buffer with listener callbacks
- `DiscordGateway.kt` — WebSocket gateway to Discord
- `activity_main.xml` — all 7 views in a single FrameLayout

## Recent changes (latest first)
- **Preset Manager page (VIEW 6)**: Full-screen `/view_presets` accessible via the "Manage Presets" button on the rotation card. Contains: 5 static templates (Gaming Session, Music Break, Code Session, Movie Time, AFK) that auto-fill a new-preset dialog; a "MY PRESETS" section showing dynamically-generated cards for each saved preset with Edit and Delete buttons; an "+ Add" button for creating presets from scratch. Editing opens the same dialog pre-filled. Presets saved to SharedPreferences as JSON.
- **App Activity Tracker page (VIEW 7)**: Full-screen `/view_app_tracker` accessible via the "Configure" button on the new dashboard compact card. Settings: App ID, update interval (5s/10s/30s/1min), "show app name as" (Activity Name / Details / State), prefix text, fixed details/state fields. When enabled, polls the foreground app and sends a presence update whenever the app changes. Mutually exclusive with Rotation Mode. Permission card shown when Usage Access is not granted.
- **File import/export**: Export and Import buttons in the Settings → Advanced section now show a dialog ("Copy to clipboard" / "Save to file"). File ops use Android Storage Access Framework (no extra permission needed). Export JSON format upgraded to `customrpc/v2` — includes all settings plus a `rotationPresets` array. Import parses both v1 (clipboard-only) and v2 (with presets).
- **Streaming status button fix**: Removed the `&& assets.length() > 0` condition in `DiscordGateway.kt` that prevented buttons from being sent when no images were set. Buttons now always send when label + URL are provided.
- **RotationPreset expanded**: Added `smallImageText`, `streamUrl`, `button1Label`, `button1Url`, `button2Label`, `button2Url` fields (all defaulting to `""` for backward compatibility).

## Previous changes
- **Token copy**: Lock icon on dashboard top bar → dialog shows masked token, "Copy token" button copies full token to clipboard.
- **Token revoke**: Same dialog has "Revoke token…" which POSTs to `POST /api/v9/auth/logout` with the user's Authorization header to permanently invalidate the token, then logs the user out.
- **Logs page**: Log icon (top-left of dashboard) opens a scrollable, timestamped activity log (info/warn/error). `AppLogger` singleton captures service events; "Clear" button wipes entries.
- **Config page UI overhaul**: All fields are now grouped inside `MaterialCardView` section cards (Identity, Activity, Content, Party & Timing, Appearance, Buttons, Advanced). Section headers are styled in purple. Back arrow added to config header.
- **Dashboard RPC management**: Presence info card (purple border) shows "NOW ACTIVE" with current activity name and details when connected, hidden when offline. Token options and log icons added to the top bar.
- **AppLogger** (`AppLogger.kt`): new singleton keeping up to 500 in-memory log entries, with real-time listener callbacks used by the logs page.

## Previous changes
- **Streaming activity URL**: added a `Stream URL` text field shown only when `Activity Type = Streaming`. The URL is persisted alongside other settings and sent to Discord as the activity `url` field.
- **Custom Party ID**: optional input under the party size/max row. Persisted and forwarded to Discord (skips auto-generated UUID when set).
- **Live preview card**: top of the configuration screen renders a Discord-style activity card that updates as the user types name/details/state and changes activity type.
- **Advanced actions row**: Reset (clears all presence fields with confirmation, keeps token), Export (copies a versioned JSON of all settings to clipboard), Import (parses such a JSON back).
- **Consistent EN/ID strings**: rebuilt both `values/strings.xml` and `values-in/strings.xml` so every key exists in both languages with matching meaning. New keys: `section_*`, `preview_*`, `btn_export/import/reset_settings`, `msg_settings_*`, `dialog_reset_*`, `hint_party_id`, `btn_yes/no`.
- **Layout fixes**: corrected four `text=` typos to `android:text=` on the large/small image and button labels.
- **Release signing**: generated 10000-day RSA-2048 keystore at `keystore/customrpc-release.keystore`. Release build type emits `app-release-unsigned.apk`; the build script then zip-aligns and signs a copy with `apksigner` (V1+V2+V3) to produce `app-release-signed.apk`.
