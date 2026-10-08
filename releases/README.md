# Civic: prototype builds

| File | Version | Date | Notes |
|---|---|---|---|
| `Civic-v0.1.0-prototype.apk` | 0.1.0 | 2026-10-08 | First rough prototype (debug build) |

## Install on your Android phone (Android 8.0+)
1. Copy the `.apk` to your phone. Any of these works: a USB cable, emailing it to yourself, Google Drive, or WhatsApp to yourself.
2. Tap the file on your phone. If Android warns about installing from unknown sources, tap **Settings** and allow it for that app (Files, Drive, Chrome…).
3. Tap **Install**. If Play Protect warns about an "unrecognised app", tap **More details → Install anyway**. This happens because it's a debug build that isn't on the Play Store.
4. Open **Civic**. Allow **Camera** and **Location** when asked.

## What to test
- **Report tab:** take a photo. The app tags it with GPS and the time. Turn on phone Location (GPS) for coordinates; first fix outdoors is quickest.
- **New report:** pick a category (Pothole, Fire, Fallen tree…), add a description, then tap **Post**.
- **Feed:** your posts show the photo, location and time. You can upvote, open the location in Maps, or delete a post.
- **Map tab:** a list of located reports. Tap one to open it in Google Maps.
- **Profile tab:** your report counts.

## Known limits (prototype)
- Posts are stored **only on this phone**. There's no server sync or sharing with other users yet.
- There are no accounts or comments yet, and the Map tab is a list, not a real map.
- Uninstalling the app deletes your posts.
- To update, install a newer APK over the old one. Your posts are kept.
