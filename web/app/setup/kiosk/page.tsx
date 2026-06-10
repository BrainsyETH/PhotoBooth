import Link from "next/link";
import { Callout, Step, Warning } from "../shared";

export const metadata = {
  title: "SnapCabin · Kiosk mode setup",
  description:
    "Lock an Android tablet to SnapCabin for the night — the in-app kiosk toggle, getting back out, and optional full lockdown for technicians.",
};

export default function KioskSetup() {
  return (
    <main className="mx-auto max-w-3xl px-6 py-16">
      <Link
        href="/setup"
        className="mb-10 inline-block text-sm font-sans font-semibold uppercase tracking-widest text-walnut no-underline hover:text-walnut-deep"
      >
        ← All setup guides
      </Link>
      <p className="font-sans text-xs font-semibold uppercase tracking-widest text-pine">
        Lock the tablet to the booth
      </p>
      <h1 className="mt-2 font-display text-5xl font-bold text-espresso">
        Kiosk mode
      </h1>
      <p className="mt-4 max-w-xl text-lg leading-relaxed text-espresso/80">
        Kiosk Mode locks the tablet to SnapCabin for the event: the status
        bar, navigation buttons, and other apps are blocked, and the screen
        stays on while it&rsquo;s plugged in. Guests stay in the booth; you
        get back out with your admin PIN.
      </p>

      <Step n="1" title="Set your own admin PIN first">
        <p>
          On the tablet, press and hold anywhere on the welcome screen, enter
          the PIN (the default is <strong>1234</strong>), and open the{" "}
          <strong>KIOSK</strong> section. Type a new PIN twice and tap{" "}
          <strong>SET PIN</strong> before anything else.
        </p>
        <Warning>
          Do this before turning Kiosk Mode on. Recovering from a forgotten
          PIN means clearing the app&rsquo;s data, which also wipes your
          configuration.
        </Warning>
      </Step>

      <Step n="2" title="Turn on Kiosk Mode">
        <p>
          In the same <strong>KIOSK</strong> section, flip the{" "}
          <strong>Kiosk Mode</strong> switch. From now on the tablet stays in
          SnapCabin: hardware back is swallowed, the status bar is hidden,
          and the screen stays awake while on power.
        </p>
        <p>
          Keep the tablet plugged into AC power for the event &mdash; the
          keep-awake only applies while charging.
        </p>
      </Step>

      <Step n="3" title="Getting back out">
        <p>
          Press and hold anywhere on the welcome screen, enter your PIN, and
          open <strong>KIOSK</strong>. Either flip the switch off or tap{" "}
          <strong>EXIT KIOSK MODE</strong> &mdash; both unlock the tablet so
          you can leave the app or press home.
        </p>
      </Step>

      <Callout title="Optional: full lockdown (needs a computer, one time)">
        <p>
          Out of the box this is a <em>best-effort</em> lock &mdash; on some
          tablets a determined guest can still find a way out. For events
          where that matters, a technician can provision the tablet as{" "}
          <strong>Device Owner</strong> over USB, which adds: no home button
          or recent apps, lock screen disabled, safe-boot and factory reset
          blocked.
        </p>
        <p>
          The short version: factory-reset the tablet, skip every sign-in
          during setup (no Google account), enable USB debugging, install
          SnapCabin, then run{" "}
          <code className="rounded bg-oat px-1.5 py-0.5 text-sm">
            adb shell dpm set-device-owner
            com.snapcabin/com.snapcabin.kiosk.DeviceAdminReceiver
          </code>{" "}
          from a computer. The full walkthrough ships with the app in{" "}
          <code className="rounded bg-oat px-1.5 py-0.5 text-sm">
            docs/KIOSK_SETUP.md
          </code>
          , or email{" "}
          <a href="mailto:hello@snapcabin.app">hello@snapcabin.app</a> and
          we&rsquo;ll walk you through it.
        </p>
      </Callout>

      <Callout title="Troubleshooting">
        <p>
          <strong>The tablet falls asleep mid-event.</strong> Kiosk Mode
          keeps the screen on <em>while plugged in</em> &mdash; check
          it&rsquo;s on AC power, not battery.
        </p>
        <p>
          <strong>I&rsquo;m locked out / forgot the PIN.</strong> Email{" "}
          <a href="mailto:hello@snapcabin.app">hello@snapcabin.app</a>.
        </p>
        <p>
          <strong>
            &ldquo;dpm set-device-owner&rdquo; says there are already
            accounts on the device.
          </strong>{" "}
          Factory reset again and skip the entire setup wizard without adding
          any account.
        </p>
      </Callout>

      <div className="mt-16 flex flex-col gap-3 sm:flex-row sm:justify-between">
        <Link
          href="/setup"
          className="text-sm font-sans font-semibold uppercase tracking-widest text-walnut no-underline hover:text-walnut-deep"
        >
          ← All setup guides
        </Link>
        <Link
          href="/setup/resend"
          className="text-sm font-sans font-semibold uppercase tracking-widest text-pine no-underline hover:text-pine-deep"
        >
          Next: Email delivery →
        </Link>
      </div>
    </main>
  );
}
