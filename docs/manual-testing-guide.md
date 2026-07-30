# Real Manual Testing Guide — Device Financing Lock

The fastest reliable loop uses an **AOSP emulator + `adb dpm set-device-owner`**
(no repeated factory resets). A real phone works too (Device Owner via QR from a
factory-reset state, or the same adb command over USB).

> Requires: Android SDK platform-tools (`adb`), an emulator or USB device on
> **Android 8+ (API 26)**, and the Laravel panel running. Device Owner can only
> be set on a device with **no accounts** — use a plain AOSP emulator image
> (not a Google Play one), freshly wiped.

---

## 1. Start the panel

```bash
cd lock_admin
php artisan migrate:fresh --seed --force     # seeds the single admin (.env ADMIN_*)
php artisan keys:generate-signing            # server Ed25519 signing keypair
php artisan serve --host=0.0.0.0 --port=8000
# optional 2nd terminal — processes the FCM-wake job (not needed for polling):
php artisan queue:work
```

Make it reachable from the device — pick one:

- **ngrok (real HTTPS):** `ngrok http 8000` → copy the `https://….ngrok-free.app` URL.
- **adb reverse (local, emulator/USB):** `adb reverse tcp:8000 tcp:8000` → use `http://127.0.0.1:8000`.

## 2. Create the target device in the panel

Browser → log in → **Customers → New** → **Devices → Register** (link to that
customer) → open the device → **Generate enrollment QR**. On the QR page, copy
the `enrollment_token` value from the payload JSON. Leave this tab open.

## 3. Build + install the debug app (pointed at your server)

```bash
cd lock
# ngrok:
./gradlew installDebug -PserverUrl=https://XXXX.ngrok-free.app
# or local:
adb reverse tcp:8000 tcp:8000
./gradlew installDebug -PserverUrl=http://127.0.0.1:8000
```
Debug builds skip cert pinning and allow cleartext to localhost, so either works.

## 4. Make the app Device Owner (no factory reset)

```bash
adb shell dpm set-device-owner com.example.lock/.DeviceAdminReceiver
```
Expect `Success: Device owner set…`. This fires `onEnabled` → sets
`DISALLOW_SAFE_BOOT/FACTORY_RESET/ADD_USER`, blocks uninstall, whitelists
LockTask (U25). If it errors about accounts, wipe the emulator and retry.

## 5. Enroll (activate)

```bash
adb shell am start -n com.example.lock/.enroll.EnrollmentActivity \
  -e enrollment_token "PASTE_TOKEN_HERE"
```
Screen shows "Device enrolled successfully." Panel device page → **enrollment
ACTIVE, reported UNLOCKED, last-seen now**.  ✅ *register → provision → online UNLOCKED*

## 6. Lock

Panel → **Lock device** → confirm password. `desired` → `LOCK_REQUESTED`.
Force an immediate poll (instead of waiting up to 15 min for the periodic one):

```bash
adb shell am broadcast -a com.example.lock.DEBUG_SYNC_NOW -p com.example.lock
```
Device enters the kiosk lock screen (shop name / message / support / call). Panel
→ **reported LOCKED**; timeline shows the command + a **SUCCESS** receipt. Try
Home / Recents / Back — all blocked.  ✅ *signed Lock end-to-end*

## 7. Reboot while locked

```bash
adb reboot
```
After boot the device returns to the lock screen (BootReceiver →
RelockForegroundService). Panel stays LOCKED.  ✅ *reboot persistence*

## 8. Unlock

Panel → **Unlock device** → confirm. Then:
```bash
adb shell am broadcast -a com.example.lock.DEBUG_SYNC_NOW -p com.example.lock
```
Kiosk exits, launcher + apps + data restored. Panel → **reported UNLOCKED**.
✅ *separate manual Unlock restores access, data intact*

## 9. Other checks

- **Diagnostics:** open the app → *Diagnostics* → enrollment id / state / sequence /
  versions, no secrets (AND-018).
- **Offline:** stop the tunnel / airplane-mode; after the threshold
  (`config/devices.php`, 45 min — lower it to test fast) the dashboard **Offline**
  card counts it via derivation; stored state is unchanged. Reconnect → it runs the
  latest valid command.
- **Duplicate command:** re-broadcast SYNC_NOW after a LOCK is already applied →
  exactly one transition, a DUPLICATE receipt.
- **DPC protection:** Settings → the app can't be uninstalled/disabled;
  `adb shell dpm remove-active-admin com.example.lock/.DeviceAdminReceiver` is blocked.
- **Payment note:** panel → add a payment on the device page → device state
  unchanged (no command created).

## Teardown / re-test

Easiest reset: **wipe the emulator** (AVD → Cold Boot / Wipe Data), then repeat
from step 3. (`dpm` device-owner state persists across app reinstalls, so a wipe
is the clean reset.)

## Gotchas

- Device Owner needs **zero accounts** on the device — plain AOSP emulator, freshly wiped.
- ngrok free URLs change on restart → rebuild with the new `-PserverUrl`, or use
  `adb reverse` for a stable local URL.
- **FCM won't wake the app** (no `google-services.json` yet) — expected. Use the
  `DEBUG_SYNC_NOW` broadcast, or wait for the 15-min periodic poll. Wire Firebase +
  apply the `google-services` plugin to test the ≤60 s FCM path.
- Can't reach the server from the device → verify ngrok/`adb reverse` and that
  `serve` binds `0.0.0.0`.
