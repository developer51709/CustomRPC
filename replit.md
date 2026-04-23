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
Workflow name: **Build APK**
Command:
```
export ANDROID_HOME=$HOME/android-sdk
export ANDROID_SDK_ROOT=$ANDROID_HOME
export PATH=$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH
./gradlew --no-daemon assembleDebug
```
Run it (or restart it) to (re)produce the debug APK at `app/build/outputs/apk/debug/app-debug.apk`. Install on an Android device by transferring the APK and opening it (allow installs from unknown sources).

## Recent changes
- **Streaming activity URL**: added a `Stream URL` text field to the configuration page that is shown only when `Activity Type = Streaming`. The URL is persisted alongside other settings and is sent to Discord as the `url` field on the activity payload (Discord requires a Twitch / YouTube URL for streaming-type activities). Files touched: `PresenceData.kt`, `MainActivity.kt`, `RpcService.kt`, `DiscordGateway.kt`, `res/layout/activity_main.xml`, `res/values/strings.xml`, `res/values-in/strings.xml`.
