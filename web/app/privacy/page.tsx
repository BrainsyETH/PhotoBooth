import Link from "next/link";

export const metadata = {
  title: "SnapCabin · Privacy Policy",
  description:
    "How SnapCabin handles photos, phone numbers, and other data. What we collect, what stays on the tablet, and what flows through services the event host configures.",
};

const LAST_UPDATED = "May 19, 2026";

export default function PrivacyPage() {
  return (
    <main className="mx-auto max-w-3xl px-6 py-16">
      <Link
        href="/"
        className="mb-10 inline-block text-sm font-sans font-semibold uppercase tracking-widest text-walnut no-underline hover:text-walnut-deep"
      >
        ← Back to SnapCabin
      </Link>

      <h1 className="font-display text-5xl font-bold text-espresso">
        Privacy policy
      </h1>
      <p className="mt-4 text-sm uppercase tracking-widest text-mist">
        Last updated · {LAST_UPDATED}
      </p>

      <section className="mt-12 space-y-6 text-lg leading-relaxed text-espresso/90">
        <p>
          SnapCabin is an Android app a host can install on a tablet and run
          as a photo booth at a wedding, party, or similar event. This page
          describes what data the app touches, what we (the people who make
          the app) see, and what flows through services the host chooses to
          turn on. We aim to be accurate. If you spot something here that
          doesn&rsquo;t match what the app actually does, please write to
          us at the address at the bottom.
        </p>
        <p className="rounded-2xl border border-pine/30 bg-cream px-6 py-5 text-base">
          <strong className="font-sans font-semibold text-pine-deep">
            The short version:
          </strong>{" "}
          The app, as published, does not include analytics, advertising,
          crash-reporting, or tracking libraries from third parties. We have
          no user accounts and no server that the app talks to. Photos and
          phone numbers travel through the services the host enables
          (typically Cloudinary for hosting and Twilio for text messages),
          and those services have their own privacy policies. SnapCabin
          itself does not receive that data.
        </p>
      </section>

      <Section title="Who&rsquo;s who">
        <p>This page uses three terms:</p>
        <ul className="ml-6 list-disc space-y-2 mt-3">
          <li>
            <strong>SnapCabin</strong> (also &ldquo;we&rdquo;): the people
            who make the Android app and run this website.
          </li>
          <li>
            <strong>Host</strong>: the person or business who installs
            SnapCabin on a tablet and runs it at an event. The host picks
            which third-party services to wire up and is responsible for
            telling their guests how the photos will be handled.
          </li>
          <li>
            <strong>Guest</strong>: someone who walks up to a tablet at an
            event and takes a photo.
          </li>
        </ul>
      </Section>

      <Section title="What we receive at SnapCabin">
        <p>
          When you use the app, it doesn&rsquo;t open a connection to any
          server we control. The currently published build does not bundle
          analytics, crash reporting, advertising, or telemetry SDKs from
          third parties. We don&rsquo;t have user accounts.
        </p>
        <p>
          We do receive whatever you put in an email if you write to us. We
          may keep that email in our mailbox so we can follow up.
        </p>
        <p>
          We may also see standard web server logs for this website (the
          marketing site and this privacy page), which is run on Vercel.
          Vercel&rsquo;s privacy practices apply to those logs. We
          don&rsquo;t use those logs to track individuals.
        </p>
      </Section>

      <Section title="What the tablet keeps locally">
        <p>
          Unless the host turns on one of the optional integrations below,
          the items below stay on the tablet:
        </p>
        <ul className="ml-6 list-disc space-y-2 mt-3">
          <li>
            <strong>Camera frames.</strong> The live preview is rendered on
            screen. Captured photos are written to app-private storage on
            the device.
          </li>
          <li>
            <strong>Audit log.</strong> When the app sends a text or email,
            it records a timestamp, the event name, the channel, the success
            status, and a masked recipient (for example, the last four
            digits of a phone number). The full recipient is not stored.
            The log stays on the tablet and can be cleared from the admin
            screen. It is capped at the most recent 500 entries.
          </li>
          <li>
            <strong>Admin settings.</strong> Event name, branding choices,
            Twilio and Cloudinary credentials if the host enters them,
            kiosk preferences. Stored in the app&rsquo;s private data area
            on the device.
          </li>
        </ul>
        <p className="text-sm text-mist">
          On Android, app-private storage is not visible to other apps
          installed on the device. It can be read by anyone with physical
          access to the device and the right developer tools, which is why
          we recommend setting an admin PIN and turning on Kiosk Mode for
          events.
        </p>
      </Section>

      <Section title="Optional: photo download over WiFi">
        <p>
          When a guest reaches the share screen, the app can start a small
          web server on the tablet&rsquo;s local WiFi (for example,{" "}
          <code className="rounded bg-oat/60 px-1 py-0.5 text-sm">
            http://192.168.x.y:8080
          </code>
          ) so a phone on the same network can scan the QR code and
          download the photo. This server is reachable only from devices on
          the same network and stops when the share screen is dismissed.
          The photo does not leave the tablet during this flow.
        </p>
      </Section>

      <Section title="Optional: photo hosting through Cloudinary">
        <p>
          If the host has entered Cloudinary credentials and turned on the
          Cloudinary toggle, captured photos are uploaded to that host&rsquo;s
          Cloudinary account before the photo URL is sent on. Cloudinary
          returns a URL the app uses to deliver the photo to the guest.
        </p>
        <ul className="ml-6 list-disc space-y-2 mt-3">
          <li>The host is the data controller for their Cloudinary account.</li>
          <li>
            How long the photo stays online depends on the host&rsquo;s
            Cloudinary settings. See{" "}
            <a
              href="https://cloudinary.com/privacy"
              target="_blank"
              rel="noopener noreferrer"
            >
              Cloudinary&rsquo;s privacy policy
            </a>
            .
          </li>
          <li>
            SnapCabin doesn&rsquo;t receive a copy of the photo or the
            upload URL.
          </li>
        </ul>
      </Section>

      <Section title="Optional: text delivery through Twilio">
        <p>
          If the host has entered Twilio credentials and a guest taps the
          text-message button, the guest&rsquo;s phone number is sent to
          Twilio along with the photo URL so Twilio can deliver a text
          message. The phone number travels to Twilio&rsquo;s API over
          HTTPS.
        </p>
        <ul className="ml-6 list-disc space-y-2 mt-3">
          <li>
            The full phone number is not kept on the tablet beyond the
            masked entry in the audit log described above.
          </li>
          <li>
            The host is the data controller for their Twilio account. See{" "}
            <a
              href="https://www.twilio.com/legal/privacy"
              target="_blank"
              rel="noopener noreferrer"
            >
              Twilio&rsquo;s privacy policy
            </a>
            .
          </li>
          <li>
            SnapCabin doesn&rsquo;t receive the phone number or the message
            contents.
          </li>
        </ul>
      </Section>

      <Section title="Permissions the app asks for">
        <ul className="ml-6 list-disc space-y-2">
          <li>
            <strong>Camera.</strong> Needed to take photos. The live feed
            doesn&rsquo;t leave the device unless the guest accepts and the
            host has enabled upload to Cloudinary.
          </li>
          <li>
            <strong>Internet, WiFi, and network state.</strong> Needed for
            Cloudinary uploads, Twilio text delivery, and the WiFi-only
            photo download server.
          </li>
          <li>
            <strong>Boot completed.</strong> So the tablet can relaunch the
            app after a power cycle. The host turns this on if they want it.
          </li>
          <li>
            <strong>Foreground service and notifications.</strong> Used to
            keep the WiFi photo server alive while the share screen is
            active.
          </li>
          <li>
            <strong>USB host.</strong> Optional, used to detect a USB
            webcam if the host plugs one in.
          </li>
        </ul>
      </Section>

      <Section title="Kiosk lockdown (Device Owner)">
        <p>
          Hosts can set SnapCabin as the Android Device Owner using ADB on
          a factory-reset tablet. When that&rsquo;s active, the app uses
          Lock Task Mode to keep the tablet locked to the booth experience.
          This is configured locally on the device. No device data is
          transmitted to SnapCabin or to any third party as part of that
          setup.
        </p>
      </Section>

      <Section title="Children&rsquo;s privacy">
        <p>
          SnapCabin is meant to be set up and run by adults. Guests who
          interact with the booth at an event may include people of any
          age, and the host is responsible for any guest under 13 who uses
          the booth. We don&rsquo;t knowingly collect data from anyone
          under 13. Because the app does not send anything back to us
          either way, this is true regardless of the guest&rsquo;s age.
        </p>
      </Section>

      <Section title="Data a host may share with you on request">
        <p>
          If you took a photo at an event using SnapCabin and want it
          removed from the host&rsquo;s photo storage, please contact the
          host directly (the event organizer, venue, or whoever set up the
          tablet). They control how long photos stay online. SnapCabin
          doesn&rsquo;t have access to a host&rsquo;s Cloudinary account
          and can&rsquo;t delete photos there on your behalf.
        </p>
      </Section>

      <Section title="Changes to this page">
        <p>
          If something material changes, we&rsquo;ll update the &ldquo;Last
          updated&rdquo; date at the top of this page. The current version
          is always available at{" "}
          <a href="https://snapcabin.app/privacy">snapcabin.app/privacy</a>.
        </p>
      </Section>

      <Section title="Contact">
        <p>
          Questions, corrections, or anything you think we&rsquo;ve gotten
          wrong: <a href="mailto:hello@snapcabin.app">hello@snapcabin.app</a>.
        </p>
      </Section>

      <p className="mt-16 text-sm text-mist">
        SnapCabin is not affiliated with Cloudinary, Twilio, Samsung,
        Google, or any other named service.
      </p>
    </main>
  );
}

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <section className="mt-12">
      <h2 className="font-display text-3xl font-medium text-espresso">{title}</h2>
      <div className="mt-4 space-y-4 text-base leading-relaxed text-espresso/85">
        {children}
      </div>
    </section>
  );
}
