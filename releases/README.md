# Civic: prototype builds

| File | Version | Date | Notes |
|---|---|---|---|
| `Civic-v0.2.0-prototype.apk` | 0.2.0 | 2026-10-09 | Real map with pins, comments, status + filters, bug fixes |
| `Civic-v0.1.0-prototype.apk` | 0.1.0 | 2026-10-08 | First rough prototype (debug build) |

## Install on your Android phone (Android 8.0+)
1. Copy the `.apk` to your phone. Any of these works: a USB cable, emailing it to yourself, Google Drive, or WhatsApp to yourself.
2. Tap the file on your phone. If Android warns about installing from unknown sources, tap **Settings** and allow it for that app (Files, Drive, Chrome…).
3. Tap **Install**. If Play Protect warns about an "unrecognised app", tap **More details → Install anyway**. This happens because it's a debug build that isn't on the Play Store.
4. Open **Civic**. Allow **Camera** and **Location** when asked.

## What to test
- **Report tab:** take a photo. The app tags it with GPS and the time. Turn on phone Location (GPS) for coordinates; first fix outdoors is quickest.
- **New report:** pick a category (Pothole, Fire, Fallen tree…), add a description, then tap **Post**.
- **Feed:** your posts show the photo, status, location and time. You can upvote, open the location in Maps, or delete a post (it asks first). Use the chips at the top to filter by status or category.
- **Report page:** tap a post (or a map pin → **View report**) to change its status (Reported → Resolved) and add comments.
- **Map tab:** an OpenStreetMap map with a pin per report (needs internet for map tiles). Tap a pin for a summary.
- **Profile tab:** your report counts.

## Known limits (prototype)
- Posts are stored **only on this phone**. There's no server sync or sharing with other users yet.
- There are no accounts yet, so comments are posted as "Guest".
- Pins at the exact same spot overlap; only the top one can be tapped.
- Uninstalling the app deletes your posts.
- To update, install a newer APK over the old one. Your posts are kept.
