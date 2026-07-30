# Provisioning & Hosting Runbook (KD5 go-live)

The one-time, no-ambiguity setup a **provider/operator** does so a shop owner can
enroll phones from a phone browser with no PC. Until every step here is done and
`php artisan enrollment:check` passes, **no phone can enroll**. See the plan
`docs/plans/2026-07-31-002-feat-enrollment-flow-no-pc-plan.md` (KD5) and the
device matrix `docs/decisions/device-matrix.md`.

> Audience: the operator (you), once per deployment. NOT the shop owner — after
> this is done, the shop owner only opens the panel, generates a QR, and scans.

## 1. Build the signed release APK

```bash
cd lock
./gradlew assembleRelease        # or bundleRelease
```
Sign it with a **stable release keystore** you keep safe (the same key must sign
every update, or devices can't update the DPC). Configure signing in
`app/build.gradle.kts` (release `signingConfig`) or sign the output with
`apksigner`.

## 2. Host the APK at a public HTTPS URL

Upload `app-release.apk` to a durable public HTTPS location (object storage / CDN
/ your server). Note the URL, e.g. `https://dpc.myshop.com/lock.apk`. It must be:
- **HTTPS** (the setup wizard refuses plain HTTP),
- reachable from the phone's network at enrollment time,
- stable (same URL for updates).

## 3. Derive the signature checksum

The QR carries `PROVISIONING_DEVICE_ADMIN_SIGNATURE_CHECKSUM` so the setup wizard
can verify the downloaded APK. Derive it from the **signing certificate** (see
the derivation already documented in `docs/decisions/device-matrix.md`):

```
# url-safe base64 of the SHA-256 of the signing certificate (no padding)
apksigner verify --print-certs app-release.apk    # confirm the signer cert
# then: base64url( sha256( DER-encoded signing certificate ) )
```

## 4. Deploy the panel at a public HTTPS URL

Deploy `lock_admin` to a public HTTPS host (VPS / PaaS). Run:
```bash
php artisan migrate --force
php artisan keys:generate-signing        # server Ed25519 command-signing key
php artisan db:seed --class=AdminUserSeeder --force   # set ADMIN_* in .env first
```
The panel URL, e.g. `https://panel.myshop.com`, is what the DPC calls back to.

## 5. Configure the enrollment env

In `lock_admin/.env`:
```
DPC_DOWNLOAD_LOCATION=https://dpc.myshop.com/lock.apk
DPC_SIGNATURE_CHECKSUM=<from step 3>
DPC_SERVER_URL=https://panel.myshop.com
DPC_COMPONENT=com.example.lock/com.example.lock.DeviceAdminReceiver
```
Also set the Android build's `SERVER_URL`/`CERT_PIN_SHA256` for the release APK
(see `lock/app/build.gradle.kts`) to the same panel host.

## 6. Verify with the go-live gate

```bash
php artisan enrollment:check
```
Every line must read **PASS**. If anything is FAIL, fix it before enrolling a
real device — this command is the deploy gate (U3).

## 7. One test enrollment

Do a full factory-reset test enrollment on a matrix device (per
`docs/decisions/device-matrix.md`): register a device in the panel → generate QR
→ provision → confirm the device shows "managed and active" and appears online.

---

## Shop-side operating rules (put these in owner training)

- **Enroll BEFORE any Google account is added** — Device Owner needs a phone
  with zero accounts. If a Google account is added first, the phone must be
  factory-reset (KD4).
- **Enroll before handover** — the shop preps the phone, then hands the customer
  an already-managed, unlocked, normal phone (R7).
- **Shop Wi-Fi is required** during enrollment — the phone downloads the app over
  it (KD6).
- **Used phones** must be factory-reset first; this erases them, so only reset
  shop-owned/pre-handover devices.
