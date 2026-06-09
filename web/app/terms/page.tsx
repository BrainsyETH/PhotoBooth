import Link from "next/link";

export const metadata = {
  title: "SnapCabin · Terms of Use",
  description:
    "The terms that apply when you install and use SnapCabin. Plain English, no surprises.",
};

const LAST_UPDATED = "May 19, 2026";

export default function TermsPage() {
  return (
    <main className="mx-auto max-w-3xl px-6 py-16">
      <Link
        href="/"
        className="mb-10 inline-block text-sm font-sans font-semibold uppercase tracking-widest text-walnut no-underline hover:text-walnut-deep"
      >
        ← Back to SnapCabin
      </Link>

      <h1 className="font-display text-5xl font-bold text-espresso">
        Terms of use
      </h1>
      <p className="mt-4 text-sm uppercase tracking-widest text-mist">
        Last updated · {LAST_UPDATED}
      </p>

      <section className="mt-12 space-y-6 text-lg leading-relaxed text-espresso/90">
        <p>
          These terms apply when you install SnapCabin from Google Play and
          use it to run a photo booth at an event. They&rsquo;re kept short
          on purpose. If something here is unclear, write to us and
          we&rsquo;ll explain.
        </p>
        <p className="rounded-2xl border border-pine/30 bg-cream px-6 py-5 text-base">
          <strong className="font-sans font-semibold text-pine-deep">
            The short version:
          </strong>{" "}
          You buy a per-device license to use SnapCabin. We may release
          updates from time to time but don&rsquo;t guarantee a fixed
          cadence. Refunds go through Google Play. We&rsquo;re a small team
          and the app is provided as-is, so our liability is limited to
          what you paid.
        </p>
      </section>

      <Section title="Your license">
        <p>
          When you purchase SnapCabin on Google Play, you get a
          non-transferable, non-exclusive license to install and use the
          app on a single Android device per purchase. Running the app on
          more than one tablet at the same time requires one purchase per
          tablet.
        </p>
        <p>
          You may use the app at events you host or operate. You may not
          repackage, resell, sublicense, or distribute the app or its
          assets. Reverse-engineering for the purpose of compatibility or
          security research is fine.
        </p>
      </Section>

      <Section title="Updates">
        <p>
          We may release updates to SnapCabin from time to time. Updates
          may add features, fix bugs, change behavior, or remove features
          that are no longer maintained. Some updates may be required to
          keep the app working with current Android releases or with the
          third-party services you connect (Resend, Cloudinary).
        </p>
        <p>
          We don&rsquo;t commit to a fixed update schedule and we
          don&rsquo;t guarantee that any specific feature will be
          maintained indefinitely. If we discontinue the app or a
          significant feature, we&rsquo;ll do our best to give reasonable
          notice through Google Play and this website.
        </p>
        <p>
          Google Play controls how updates are delivered to your device.
          You can adjust auto-update behavior in the Play Store settings.
        </p>
      </Section>

      <Section title="Refunds">
        <p>
          Refunds are handled through Google Play&rsquo;s standard refund
          policy. To request one, open the Play Store, find your purchase,
          and follow Google&rsquo;s refund flow. We don&rsquo;t process
          refunds directly outside of Play.
        </p>
      </Section>

      <Section title="Third-party services you connect">
        <p>
          SnapCabin lets you connect your own Resend and Cloudinary
          accounts for email delivery and photo hosting. Anything sent
          through those services is governed by your agreements with them
          and is billed at their rates. We&rsquo;re not a party to those
          arrangements and don&rsquo;t see your traffic or data with them.
        </p>
      </Section>

      <Section title="As-is, no warranty">
        <p>
          SnapCabin is provided as-is, without warranties of any kind
          beyond those that can&rsquo;t be disclaimed by law. We
          don&rsquo;t warrant that the app will be uninterrupted, free of
          defects, or fit for any particular event or purpose.
        </p>
        <p>
          To the maximum extent allowed by law, our total liability to you
          for anything related to SnapCabin is limited to the amount you
          paid for the app. We aren&rsquo;t responsible for lost photos,
          missed sends, network outages, third-party service downtime,
          or other indirect or consequential damages.
        </p>
        <p>
          You&rsquo;re responsible for testing the app, the integrations,
          and the tablet before your event. We strongly recommend running
          a full dry run end-to-end at least 24 hours before guests
          arrive.
        </p>
      </Section>

      <Section title="Your conduct and your guests">
        <p>
          You&rsquo;re responsible for how SnapCabin is used at your
          event, including whether guests under 13 may use it and how
          photos and email addresses captured at the event are handled
          afterward.
        </p>
      </Section>

      <Section title="Changes to these terms">
        <p>
          If we change these terms in a material way, we&rsquo;ll update
          the &ldquo;Last updated&rdquo; date at the top of this page.
          Continued use of the app after a change means you accept the
          updated terms. The current version is always available at{" "}
          <a href="https://snapcabin.app/terms">snapcabin.app/terms</a>.
        </p>
      </Section>

      <Section title="Contact">
        <p>
          Questions, problems, or anything else:{" "}
          <a href="mailto:hello@snapcabin.app">hello@snapcabin.app</a>.
        </p>
      </Section>

      <p className="mt-16 text-sm text-mist">
        SnapCabin is not affiliated with Resend, Cloudinary, Samsung, or
        Google.
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
