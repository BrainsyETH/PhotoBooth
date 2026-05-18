# Play Console — Device admin / Device Owner disclosure

Google Play flags `BIND_DEVICE_ADMIN` as a sensitive permission. Reviewers
want to know why an app declares a `DeviceAdminReceiver`, how it's used, and
how the user can disable it. Use the text below as the canonical answer.

## Where to put this

Play Console → **Policy → App content → Sensitive declarations →
Device administrator policies** (the panel name varies slightly by year;
it's the one that surfaces when you upload an app declaring
`<receiver android:name="...DeviceAdminReceiver">`).

## Justification (paste this)

> SnapCabin is an Android kiosk app designed for unattended operation at
> weddings, parties, and similar events. The `DeviceAdminReceiver` exists so
> that the kiosk operator can optionally provision the app as the device's
> **Device Owner** to enable **Lock Task Mode** — Android's built-in kiosk
> lockdown that prevents guests from exiting the app, opening other
> applications, or accessing system settings.
>
> Provisioning is performed manually by the operator via ADB on a
> factory-reset device:
>
> ```
> adb shell dpm set-device-owner com.snapcabin/com.snapcabin.kiosk.DeviceAdminReceiver
> ```
>
> The app itself **does not, and cannot, set itself as Device Owner**.
> Device Owner can only be granted before any user accounts are added to
> the device, which means it requires a factory reset and ADB access. This
> is enforced by Android, not by SnapCabin.
>
> When the operator has provisioned Device Owner *and* enabled "Kiosk Mode"
> in the in-app admin screen, SnapCabin applies the following policies:
>
> - **Lock Task whitelist** — limits Lock Task to SnapCabin's package only.
> - **Lock Task features** — disables the status bar, navigation bar, and
>   recent apps overlay while locked.
> - **Keyguard disabled** — bypasses the lock screen so the kiosk is always
>   immediately usable.
> - **Stay on while plugged in** — the screen remains active while the
>   tablet is on AC, USB, or wireless power.
> - **User restrictions** — `DISALLOW_SAFE_BOOT`, `DISALLOW_FACTORY_RESET`,
>   `DISALLOW_ADD_USER`, `DISALLOW_MOUNT_PHYSICAL_MEDIA`, and (on Android
>   9+) `DISALLOW_SYSTEM_ERROR_DIALOGS`.
>
> All of these are standard Android Device Owner APIs and are documented
> at developer.android.com/work/dpc/dedicated-devices.
>
> **How users disable it**: a user with the admin PIN can enter the in-app
> admin screen and toggle "Kiosk Mode" off; the app immediately calls
> `stopLockTask()`. To remove Device Owner status entirely, the operator
> must factory-reset the device — this is an Android security guarantee
> that we don't override.

## Code references for reviewers

- `app/src/main/java/com/snapcabin/kiosk/KioskManager.kt` — all policy
  application is in `configureDeviceOwner`; the receiver is only consulted
  when `isDeviceOwner(context) == true`, so a non-provisioned install
  applies no policies.
- `app/src/main/java/com/snapcabin/kiosk/DeviceAdminReceiver.kt` — the
  receiver class itself; minimal — just the standard Android contract.
- `app/src/main/res/xml/device_admin.xml` — declares only the
  `force-lock` policy (the minimum Android requires for the receiver to
  load).

## Privacy implications

The Device Owner provisioning flow does not transmit any device data —
neither to SnapCabin (we collect nothing, see DATA_SAFETY.md) nor to any
third party. All policies are applied locally via Android's
DevicePolicyManager APIs.

This is also covered in the public privacy policy at
<https://snapcabin.app/privacy> ("Kiosk lockdown (Device Owner)" section).
