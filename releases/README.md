# Civic: prototype builds

| File | Version | Date | Notes |
|---|---|---|---|
| `Civic-v0.4.0-accounts.apk` | 0.4.0 | 2026-10-09 | Profiles, accounts, authored feed and comments, one-vote upvotes. On-device build (no server configured) |
| `Civic-v0.2.0-prototype.apk` | 0.2.0 | 2026-10-09 | Real map with pins, comments, status + filters, bug fixes |
| `Civic-v0.1.0-prototype.apk` | 0.1.0 | 2026-10-08 | First rough prototype (debug build) |

## Install on your Android phone (Android 8.0+)
1. Copy the `.apk` to your phone. Any of these works: a USB cable, emailing it to yourself, Google Drive, or WhatsApp to yourself.
2. Tap the file on your phone. If Android warns about installing from unknown sources, tap **Settings** and allow it for that app (Files, Drive, Chrome…).
3. Tap **Install**. If Play Protect warns about an "unrecognised app", tap **More details → Install anyway**. This happens because it's a debug build that isn't on the Play Store.
4. Open **Civic**. Allow **Camera** and **Location** when asked.

Installing over an older Civic keeps your existing posts (the database migrates; old safety reports are marked private).

## What to test in 0.4.0
- **First launch:** you're asked what to call you. That name becomes your profile and appears on everything you post. There's no password and no network involved.
- **Profile tab:** your avatar initials, name, `@handle` and stats. **Edit** changes your display name and bio — your existing posts and comments update to the new name. The card below says plainly where your data lives.
- **Feed:** every card now shows who posted it. Upvote is **one per person** and tapping again takes it back. The refresh button pulls from the server when one is configured.
- **Report page:** comments show who wrote them, with an avatar and a relative time.
- **Safety reports** stay private — they never enter the feed, and in a server build the database itself refuses to show them to anyone else.

## Known limits (this build)
- **This APK has no server**, so accounts, sharing and sync are switched off and the sign-in screens are hidden — everything stays on this phone. That's deliberate: the app is usable before any backend exists. Follow `docs/BACKEND_SETUP.md` (about 15 minutes, free) and rebuild to turn on accounts, the shared feed and photo storage.
- Pins at the exact same spot overlap; only the top one can be tapped.
- Uninstalling the app deletes posts that haven't synced.
- Sync (in a server build) runs on launch and on the refresh button; there's no background sync yet.

## Rebuilding
```
JAVA_HOME=/path/to/jdk21 ./gradlew :frontend:assembleDebug
cp frontend/build/outputs/apk/debug/frontend-debug.apk releases/Civic-vX.Y.Z-name.apk
```
`.apk` files are git-ignored, so a build is never committed — rebuild from the tag instead.
