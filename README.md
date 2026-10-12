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


## Visual display understanding (v4.3.0)
1. Open Settings → Visual display understanding and choose 2 s, 1 s, or fast 0.25 s sampling.
2. Tap Start screen capture and approve Android's system consent dialog.
3. Tap Understand latest screen frame when you want Vision to analyze a captured frame. This sends that frame to Google's Gemini API using the API key you entered; screen content can be private, so only analyze when appropriate.
4. Tap Stop screen capture when finished.

Fast sampling uses more battery and can still miss rapid motion. The feature describes captured frames; it is not a guarantee of real-time game control or perfect touch accuracy. Accessibility-tree labels are used for semantic actions when available, and a tap result should be verified by reading the screen again. APK builds are available in GitHub Actions as the Vision-debug-apk artifact after a successful run.
