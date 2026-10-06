# Vision (Android, Kotlin)

## Build on GitHub (no PC needed)
1. Upload everything in this zip to the ROOT of a GitHub repo (settings.gradle.kts must be at the top level).
2. Push to `main` (or Actions tab > "Build APK" > Run workflow).
3. When the run is green: open the run > Artifacts > `Vision-debug-apk` > download, unzip, install `app-debug.apk`.
If `.github/workflows/build.yml` did not upload, create that file in the GitHub web UI and paste the same content.

## First run on the phone
1. Settings tab: paste the Gemini API key.
2. Settings > "Open Accessibility settings" > Vision Control > turn on.
   (Android 13+, sideloaded app: App info > three dots > "Allow restricted settings" first.)
3. Settings > "Display over other apps" > allow.
4. Voice tab: tap the circle and talk.

## Where to edit
- Persona.kt: personality, CREATOR_NAME / CREATOR_PROFILE.
- LiveSession.kt: MODEL name and Live API setup.
- ToolRouter.kt: add tools (declaration + handler).
- VisionAccessibilityService.kt: BLOCK list of apps Vision must never touch.
