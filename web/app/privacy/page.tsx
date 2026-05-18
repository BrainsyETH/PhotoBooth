import Link from "next/link";

export const metadata = {
  title: "SnapCabin · Privacy Policy",
  description:
    "How SnapCabin handles photos, phone numbers, and other data. We collect nothing. Photos and SMS recipients flow only through services the kiosk operator configures.",
};

const LAST_UPDATED = "May 18, 2026";

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
          SnapCabin is an Android kiosk app for hosts running photo booths at
          weddings, parties, and similar events. This policy explains exactly
          what data the app touches, what we — the SnapCabin developer —
          collect, and where any data goes if it leaves the kiosk.
        </p>
        <p className="rounded-2xl border border-pine/30 bg-cream px-6 py-5 text-base">
          <strong className="font-sans font-semibold text-pine-deep">
            The short version:
          </strong>{" "}
          We collect nothing — no analytics, no crash reports, no usage
          telemetry, no identifiers. Photos and phone numbers flow only through
          services that the operator of the kiosk has explicitly configured
          (Cloudinary for hosting, Twilio for SMS), and the operator is the
          data controller for those flows. SnapCabin itself never receives any
          of that data.
        </p>
      </section>

      <Section title="Who's who">
        <p>
          This policy uses three distinct terms:
        </p>
        <ul className="ml-6 list-disc space-y-2 mt-3">
          <li>
            <strong>SnapCabin</strong> (also "we") — the developer of the
            Android app. We publish the software and the website you're reading
            now.
          </li>
          <li>
            <strong>Operator</strong> — the person or business who installs
            SnapCabin on a tablet and runs it at an event. The operator
            configures any third-party services and is responsible for
            disclosing data handling to their guests.
          </li>
          <li>
            <strong>Guest</strong> — someone who walks up to a kiosk at an
            event and takes a photo.
          </li>
        </ul>
      </Section>

      <Section title="What SnapCabin (the developer) collects">
        <p>
          <strong>Nothing.</strong> The app does not include any analytics
          SDKs, crash-reporting SDKs, advertising SDKs, or telemetry of any
          kind. We do not receive any data when the app is used. We do not
          have any user accounts.
        </p>
        <p>
          If you contact us by email, we receive the contents of that email.
          That's it.
        </p>
      </Section>

      <Section title="What the kiosk processes locally">
        <p>
          Everything below stays on the device unless the operator has
          configured one of the optional integrations covered later:
        </p>
        <ul className="ml-6 list-disc space-y-2 mt-3">
          <li>
            <strong>Camera frames</strong> — the live preview is rendered
            on-screen and never transmitted. Captured photos are written to
            app-private storage on the device.
          </li>
          <li>
            <strong>Audit log</strong> — when the kiosk sends an SMS or email,
            it records a timestamp, the event slug, the channel, the success
            status, and a masked recipient (last four digits of a phone
            number, or the first letter of an email local-part). The full
            recipient is never stored. The log lives on-device and can be
            cleared from the admin screen.
          </li>
          <li>
            <strong>Admin settings</strong> — event name, branding choices,
            Twilio / Cloudinary credentials (when the operator enters them),
            kiosk preferences. Stored in the app's private data partition.
          </li>
        </ul>
      </Section>

      <Section title="Optional: local WiFi photo download">
        <p>
          When a guest reaches the share screen, SnapCabin can start a small
          web server on the kiosk's local WiFi (typically{" "}
          <code className="rounded bg-oat/60 px-1 py-0.5 text-sm">
            http://192.168.x.y:8080
          </code>
          ) so a phone on the same network can scan the QR code and download
          the photo. This server is reachable only from the local network and
          stops when the share screen is dismissed. Nothing about this flow
          touches the public internet.
        </p>
      </Section>

      <Section title="Optional: photo upload (Cloudinary)">
        <p>
          If the operator has entered Cloudinary credentials in the admin
          screen and enabled the toggle, captured photos are uploaded to the
          operator's Cloudinary account before SMS delivery. The URL Cloudinary
          returns is unguessable and used to deliver the photo to the
          recipient.
        </p>
        <ul className="ml-6 list-disc space-y-2 mt-3">
          <li>The operator is the data controller for their Cloudinary account.</li>
          <li>
            Retention is governed by the operator's Cloudinary configuration —
            see{" "}
            <a
              href="https://cloudinary.com/privacy"
              target="_blank"
              rel="noopener noreferrer"
            >
              Cloudinary's privacy policy
            </a>
            .
          </li>
          <li>
            SnapCabin never receives a copy of the photo or the upload URL.
          </li>
        </ul>
      </Section>

      <Section title="Optional: SMS delivery (Twilio)">
        <p>
          If the operator has entered Twilio credentials and the guest taps
          the SMS button, the guest's phone number is sent to Twilio along
          with the Cloudinary photo URL so Twilio can deliver an MMS to that
          number.
        </p>
        <ul className="ml-6 list-disc space-y-2 mt-3">
          <li>The phone number is transmitted over HTTPS to Twilio's API.</li>
          <li>The phone number is not retained beyond the masked audit log.</li>
          <li>
            The operator is the data controller for their Twilio account — see{" "}
            <a
              href="https://www.twilio.com/legal/privacy"
              target="_blank"
              rel="noopener noreferrer"
            >
              Twilio's privacy policy
            </a>
            .
          </li>
          <li>
            SnapCabin never receives the phone number or the message contents.
          </li>
        </ul>
      </Section>

      <Section title="Permissions the app requests">
        <ul className="ml-6 list-disc space-y-2">
          <li>
            <strong>Camera</strong> — required to take photos. The live feed
            never leaves the device unless the guest accepts and the operator
            has configured upload.
          </li>
          <li>
            <strong>Internet / WiFi / Network state</strong> — required for
            Cloudinary uploads, Twilio API calls, and the local QR-download
            server.
          </li>
          <li>
            <strong>Boot completed</strong> — so the kiosk relaunches
            automatically when a tablet is power-cycled. Operator-controlled.
          </li>
          <li>
            <strong>Foreground service / notifications</strong> — required to
            keep the LAN photo server alive while the share screen is active.
          </li>
          <li>
            <strong>USB host</strong> — optional, used to detect external USB
            webcams.
          </li>
        </ul>
      </Section>

      <Section title="Kiosk lockdown (Device Owner)">
        <p>
          Operators can provision SnapCabin as the Android Device Owner via
          ADB on a factory-reset tablet. When active, the app uses Lock Task
          Mode to keep the tablet locked into the kiosk. This is configured
          locally on the device; no device data is transmitted to SnapCabin or
          to any third party as part of that flow.
        </p>
      </Section>

      <Section title="Children's privacy">
        <p>
          SnapCabin is designed for adult event hosts to operate. Guests who
          interact with the kiosk are typically adults attending the event,
          but the operator is responsible for any guest under 13 who may use
          the booth. We do not knowingly collect data from anyone under 13;
          since we collect nothing at all, this is true by design.
        </p>
      </Section>

      <Section title="Data your operator may share with you on request">
        <p>
          If you took a photo at an event using SnapCabin and want it deleted
          from the operator's photo host, contact the operator (the event
          host, venue, or whoever provided the kiosk). They control retention
          and deletion. SnapCabin (the developer) cannot access or delete
          photos in an operator's account.
        </p>
      </Section>

      <Section title="Changes to this policy">
        <p>
          If material changes happen, we'll update the "Last updated" date at
          the top of this page. The current version is always available at{" "}
          <a href="https://snapcabin.app/privacy">snapcabin.app/privacy</a>.
        </p>
      </Section>

      <Section title="Contact">
        <p>
          Questions about this policy or the app itself:{" "}
          <a href="mailto:hello@snapcabin.app">hello@snapcabin.app</a>.
        </p>
      </Section>

      <p className="mt-16 text-sm text-mist">
        SnapCabin is not affiliated with Cloudinary, Twilio, Samsung, Google,
        or any other named service.
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
