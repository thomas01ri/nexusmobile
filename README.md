# NEXUS Mobile v1.2.0 — Android Client Foundation

NEXUS is an independent Android launcher foundation for a future GTA San Andreas multiplayer client.

## v1.2.0
- First-launch setup screen
- Detection of the installed Rockstar GTA San Andreas Android package
- Manifest-driven NEXUS component downloader
- HTTPS source allow-list, file-size limits, SHA-256 verification, and download progress
- Local server address list and simplified launcher UI
- Double-press exit confirmation in the NEXUS launcher

## Important limitations
- NEXUS does not download, bundle, or redistribute GTA San Andreas game data. Install the game separately from an official store using your own account.
- The component manifest currently contains no published packages. The downloader only accepts items explicitly marked redistributable with a license identifier and a SHA-256 checksum.
- The server list stores hostnames and ports locally. Actual joining of public SA-MP servers is not enabled yet because a compatible client core has not passed source-license and runtime-compatibility review.
- The double-press exit protection currently applies to the NEXUS launcher. It cannot intercept the exit control inside a separate SA-MP game process until a native client is integrated.
- Keyboard/mouse, mods, chat, voice, and graphics pages remain preference or preview screens; they do not yet control a running game.

## Build APK with GitHub Actions
1. Open the repository Actions tab.
2. Choose **Build NEXUS APK** and run the workflow, or push to **main**.
3. Download the artifact **NEXUS-Mobile-v1.2.0-APK**.
4. Extract NEXUS-Mobile-v1.2.0-debug.apk and install it on Android.

## Build locally
Open the project in Android Studio and run ./gradlew :app:assembleDebug.
