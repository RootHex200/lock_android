# U1 — Pilot Device Matrix & Device Owner Provisioning Spike

`lock_android#1` · **HITL** (requires physical hardware) · Release Gate 1

> **Related:** the one-time provider hosting/config steps that make QR
> enrollment work live are in [`provisioning-runbook.md`](provisioning-runbook.md)
> (gated by `php artisan enrollment:check`).

> **Status: template + procedure.** This decision doc defines the supported
> matrix, the exact QR provisioning payload, the distribution/update method, and
> the pass/fail checklist. The result columns MUST be filled in against real,
> factory-resettable pilot handsets before any Android device-behaviour unit
> (U20/U22/U23) is signed off. Runtime proof on hardware > unit tests for this
> spike (it is deliberately not shippable code).

## Decisions fixed by this spike

- **minSdk = 26** (KTD-7). Confirm reliable Device Owner `startLockTask`
  behaviour on every candidate; raise per-model if a device misbehaves.
- **compileSdk / targetSdk = 36.**
- **Provisioning method:** QR from the setup wizard (factory-reset → tap the
  welcome screen 6×  → scan). The DPC is **not** preinstalled, so the payload
  carries the APK download location + signature checksum.
- **Distribution/update:** signed APK hosted at an HTTPS URL
  (`DPC_DOWNLOAD_LOCATION`); updates via the same URL, no ADB required.

## QR provisioning payload (frozen keys — feeds U6)

The `lock_admin` enrollment QR (`EnrollmentTokenService::provisioningPayload`)
encodes exactly:

```json
{
  "android.app.extra.PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME": "com.example.lock/com.example.lock.DeviceAdminReceiver",
  "android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_LOCATION": "https://dpc.example.com/lock.apk",
  "android.app.extra.PROVISIONING_DEVICE_ADMIN_SIGNATURE_CHECKSUM": "<url-safe-base64 SHA-256 of signing cert>",
  "android.app.extra.PROVISIONING_SKIP_ENCRYPTION": false,
  "android.app.extra.PROVISIONING_LEAVE_ALL_SYSTEM_APPS_ENABLED": true,
  "android.app.extra.PROVISIONING_ADMIN_EXTRAS_BUNDLE": {
    "enrollment_token": "<single-use token>",
    "server_url": "https://dpc.example.com",
    "device_id": 123,
    "checksum": "<integrity checksum>"
  }
}
```

To derive `PROVISIONING_DEVICE_ADMIN_SIGNATURE_CHECKSUM` from the release APK:

```
# url-safe base64 of the SHA-256 of the signing certificate
keytool -printcert -jarfile app-release.apk    # note the signer cert
apksigner verify --print-certs app-release.apk
# then compute: base64url( sha256( DER-encoded signing certificate ) )
```

Set it via `DPC_SIGNATURE_CHECKSUM` in the `lock_admin` env.

## Validation matrix (fill per model)

Acquire 2–3 candidate models that match the pilot shop's actual financed OEMs
(NFR-006 blocks unsupported devices from enrollment).

| Check | Model A | Model B | Model C |
|-------|---------|---------|---------|
| OEM / model / Android version | | | |
| Factory reset → QR provisions as Device Owner | ☐ | ☐ | ☐ |
| `startLockTask` blocks Home / Recent / Back / shade / Settings | ☐ | ☐ | ☐ |
| `DISALLOW_SAFE_BOOT` — safe-mode boot blocked | ☐ | ☐ | ☐ |
| `DISALLOW_FACTORY_RESET` — recovery/factory-reset blocked | ☐ | ☐ | ☐ |
| `DISALLOW_ADD_USER` — secondary user blocked | ☐ | ☐ | ☐ |
| Reboot while LOCKED → returns to lock screen | ☐ | ☐ | ☐ |
| **Boot-to-relock latency** (target: seconds, record actual) | ___s | ___s | ___s |
| Unlock restores launcher, apps + data intact | ☐ | ☐ | ☐ |
| APK install/update without ADB (HTTPS URL) | ☐ | ☐ | ☐ |

**Release Gate 1 passes when ≥2 models pass all five behavioural checks**
(provision / lock / safe-boot+factory-reset / reboot-to-relock / unlock) and the
distribution/update path is confirmed repeatable without ADB.

## Supported matrix (output — fill after validation)

| OEM | Model | Min Android | Provisioning | Distribution/Update | Verdict |
|-----|-------|-------------|--------------|---------------------|---------|
| _pending hardware_ | | 8.0 (API 26) | QR (setup wizard) | HTTPS APK | ☐ |
