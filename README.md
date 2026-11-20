# NEXUS Mobile v1.1.0 — Android UI Prototype

NEXUS is an independent Android interface prototype for a future GTA San Andreas multiplayer client concept.

## Included
- Home dashboard
- Local server list management
- Local nickname profile
- Keyboard and mouse preference settings
- Control binding preferences and radial menu preview
- Local mod entry management
- Custom chat preferences and preview
- Voice-related preference settings
- Display and graphics preferences

## Important limitations
This version is an Android UI prototype. It does not include the GTA San Andreas game engine, SA-MP multiplayer core, actual server connection, native mod loader, voice transport, or working in-game keyboard/mouse input. Settings currently save preferences locally and do not modify an actual game engine.

## Build APK using GitHub Actions
1. Push this project to the `main` branch of a GitHub repository.
2. Open the repository's Actions tab.
3. Select **Build NEXUS APK**.
4. Choose **Run workflow**.
5. Wait for the workflow to finish successfully.
6. Download the artifact **NEXUS-Mobile-v1.1.0-APK**.
7. Extract the artifact ZIP to obtain `NEXUS-Mobile-v1.1.0-debug.apk`.

The APK is not included in this source ZIP. It must be built by GitHub Actions or Android Studio.

## Build locally
Open the project in Android Studio, sync Gradle, then run the `assembleDebug` task.

The expected APK output is:
`app/build/outputs/apk/debug/app-debug.apk`
