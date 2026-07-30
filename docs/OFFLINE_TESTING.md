# Offline feature test script

Manual replication steps for Queuemark's offline-first behavior. Run top to
bottom — the order matters, because the reader tests (4–5) need a bookmark
that was saved **online** first. Works on a device (Quick Settings airplane
toggle) or emulator (`adb shell cmd connectivity airplane-mode enable|disable`).

## Prep

- Install the latest debug build (`./gradlew installDebug`).
- Fresh-install state: `adb shell pm clear com.bookmarkapp.queuemark`
  (or Settings → Apps → Queuemark → Clear data).

## 1. Enter the app with zero internet

Airplane ON → wipe data → launch → allow notifications → **Continue as guest**.

✅ Dashboard ("Hello, Guest") opens immediately, no error.
*Proves: lazy anonymous auth — entry doesn't depend on a Firebase network call.*

## 2. Save a bookmark offline

Still offline: **+** → `example.com` → Save.

✅ Saved with the **URL as title**, 1 min read (no network = no scrape; the
save is never lost). Header shows amber **"Changes waiting to sync."**
*Proves: offline-first capture + honest sync indicator.*

## 3. Reconnect and watch it heal

Airplane OFF → close and reopen the app → wait ≤ 60 s.

✅ Indicator flips to **"All caught up"** on its own. (Console check:
Firestore → `users/{uid}/bookmarks` now contains the offline save; the uid
was created lazily in the background.)
*Proves: lazy account creation + WorkManager network-constrained sync.*

## 4. Capture an article for offline reading (online)

Still online: save `en.wikipedia.org/wiki/Reading` (or share any article
from Chrome). Open it once.

✅ Opens in **Reader view**: serif text, "domain • N min read" byline,
scroll progress bar.
*Proves: article text extracted and stored at save time.*

## 5. Read with no internet

Airplane ON → reopen the article → then tap the globe icon (Web view) →
then **"Read offline copy."**

✅ Reader shows the full article offline. Web view shows the friendly
**"You're offline — an offline copy is available"** state (never the raw
`net::ERR` browser page). The button returns to Reader.
*Proves: offline reading + graceful web fallback.*

## 6. The timer can't lie

Still offline, open the content-less `example.com` bookmark (offline empty
state appears). Wait ~90 s → back.

✅ **No** "Mark as complete?" prompt — error states never count as reading.
(Contrast: reading the article in Reader past ~80% of its estimate → prompt.)
*Proves: reading sessions start only when real content is shown.*

## 7. Logout protects unsynced work

Still offline: swipe a bookmark right (complete) → indicator goes amber →
tap logout (sign in / link an account first if on a guest session).

✅ Warning: *"1 change hasn't synced yet and will be lost if you sign out
now."* Cancel keeps the session and the data.
*Proves: the logout wipe can't silently destroy offline changes.*

## 8. Stale notification deep link (adb)

```bash
adb shell am force-stop com.bookmarkapp.queuemark
adb shell am start -n com.bookmarkapp.queuemark/.MainActivity --es extra_bookmark_id dead-id
```

✅ "This bookmark is no longer available" screen with a back button — not a
blank page.
*Proves: stale/cross-account notification handling.*

## Known behavior (by design, not bugs)

- Offline saves get the URL as title and 1-min estimate (no scrape possible);
  re-saving while online fetches the real title, read time, and reader text.
- Bookmarks synced **down** from the cloud have no offline reader copy until
  re-saved on this device (article text is local-only, never uploaded).
- After reconnecting, sync can take up to ~60 s if a retry backoff was
  pending; any new save/complete triggers an immediate attempt.
