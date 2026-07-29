# U21 — Lock Screen Content & Per-Model Emergency Actions

`lock_android#8` · **HITL** (design review + per-model emergency validation)

The lock screen (`res/layout/activity_lock.xml` + `LockActivity`) binds shop
identity/support/instruction/reconnect from the settings synced at enrollment
(`SettingsStore`, populated from the U8 activate response's `settings` block,
refreshed on heartbeat). Status is text-labelled ("LOCKED"), not colour-only
(NFR-009).

## Content shown (all from synced settings — U3)

- Shop name (`shop_name`)
- LOCKED status label
- Lock-screen message (`lock_screen_message`)
- Support phone (`support_phone`) + a **Call support** action (`ACTION_DIAL`,
  no runtime permission required)
- Reconnect instructions (`reconnect_instructions`)

Because the content is cached in `SettingsStore`, a locked **offline** device
still shows current shop contact info.

## Manual validation (fill per matrix model — from U1)

| Check | Model A | Model B | Model C |
|-------|---------|---------|---------|
| All settings fields render | ☐ | ☐ | ☐ |
| Content updates after a settings change + heartbeat | ☐ | ☐ | ☐ |
| Emergency/support dial works from the locked kiosk | ☐ | ☐ | ☐ |
| Incoming-call behaviour acceptable (call UI vs kiosk) | ☐ | ☐ | ☐ |
| Status legible, not colour-only | ☐ | ☐ | ☐ |

## Design-review sign-off

- [ ] Design reviewer approved the lock-screen layout and copy.
- [ ] Emergency action validated on **each** matrix model.

> Emergency/incoming-call handling is the per-model risk (some OEMs route calls
> differently under LockTask). Record any model needing a whitelist tweak in the
> U1 matrix.
