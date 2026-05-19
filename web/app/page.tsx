import Link from "next/link";

export default function Home() {
  return (
    <main className="mx-auto max-w-5xl px-6 pb-24 pt-16 sm:pt-24">
      <Hero />
      <Features />
      <HowItWorks />
      <Integrations />
      <Faq />
      <Footer />
    </main>
  );
}

function Hero() {
  return (
    <section className="flex flex-col items-center gap-8 text-center">
      <p className="text-xs font-sans font-semibold uppercase tracking-[0.3em] text-honey-deep">
        Photo Booth · Self-hosted · Android
      </p>
      <h1 className="font-display text-6xl font-bold leading-[1.05] text-espresso sm:text-7xl">
        SnapCabin
      </h1>
      <p className="font-display text-2xl italic text-walnut sm:text-3xl">
        A photo booth in the woods.
      </p>
      <p className="max-w-xl text-lg leading-relaxed text-espresso/80">
        Self-hosted Android photo-booth kiosk for weddings and gatherings. No
        analytics, no telemetry, no monthly fee. Pay once, run it on your own
        hardware, keep every photo on your operator's storage.
      </p>
      <div className="flex flex-wrap items-center justify-center gap-4">
        <Link
          href="/setup"
          className="rounded-2xl bg-pine px-6 py-3 font-sans text-base font-semibold text-cream no-underline shadow-sm transition hover:bg-pine-deep"
        >
          Setup guides
        </Link>
        <a
          href="mailto:hello@snapcabin.app"
          className="rounded-2xl border border-walnut/40 px-6 py-3 font-sans text-base font-semibold text-walnut no-underline transition hover:border-walnut hover:text-walnut-deep"
        >
          Contact
        </a>
      </div>
    </section>
  );
}

function Features() {
  return (
    <section className="mt-28 grid gap-6 sm:grid-cols-2">
      {[
        {
          title: "Yours, on your hardware",
          body: "Runs on any modern Android tablet. Photos live on the device unless you wire it to your own cloud storage.",
        },
        {
          title: "Single Photo · Collage · GIF",
          body: "Three capture modes a guest can pick from, each with countdown coaching, framing guide, and pose prompts.",
        },
        {
          title: "SMS + MMS delivery",
          body: "Bring your own Twilio account and SnapCabin will send the photo straight to a guest's phone.",
        },
        {
          title: "Cloudinary photo hosting",
          body: "Unsigned uploads to a folder scoped per event. The kiosk never sees your API secret.",
        },
        {
          title: "Branding per event",
          body: "Event name, attract tagline, watermark, custom border, and logo overlay — all configurable from the admin screen.",
        },
        {
          title: "Audit log, on-device",
          body: "Every send is logged with timestamp, channel, masked recipient, and event slug. Last 500 entries, exportable on request.",
        },
      ].map((f) => (
        <FeatureCard key={f.title} title={f.title} body={f.body} />
      ))}
    </section>
  );
}

function FeatureCard({ title, body }: { title: string; body: string }) {
  return (
    <article className="rounded-3xl border border-walnut/15 bg-cream/70 p-6">
      <h3 className="font-display text-2xl font-medium text-espresso">{title}</h3>
      <p className="mt-2 text-base leading-relaxed text-espresso/75">{body}</p>
    </article>
  );
}

function HowItWorks() {
  const steps = [
    {
      n: "01",
      title: "Install on a tablet",
      body: "Sideload the APK (or install once it's on Play). One-time setup; no monthly fee.",
    },
    {
      n: "02",
      title: "Configure your event",
      body: "Tap a hidden corner, enter the admin PIN, name the event, and dial in branding.",
    },
    {
      n: "03",
      title: "Wire up SMS + hosting",
      body: "Paste Twilio credentials and a Cloudinary preset. We link to the setup guides right from the admin screen.",
    },
    {
      n: "04",
      title: "Lock it down + host",
      body: "Optional Android Device Owner kiosk mode keeps the app locked to the tablet for the evening.",
    },
  ];
  return (
    <section className="mt-28">
      <SectionHeading
        eyebrow="How it works"
        title="A weekend's worth of setup, not a quarter's"
      />
      <ol className="mt-10 grid gap-6 sm:grid-cols-2 lg:grid-cols-4">
        {steps.map((s) => (
          <li
            key={s.n}
            className="rounded-3xl border border-walnut/15 bg-cream/60 p-6"
          >
            <span className="font-display text-3xl font-semibold text-honey-deep">
              {s.n}
            </span>
            <h4 className="mt-3 font-display text-xl font-medium text-espresso">
              {s.title}
            </h4>
            <p className="mt-2 text-sm leading-relaxed text-espresso/75">
              {s.body}
            </p>
          </li>
        ))}
      </ol>
    </section>
  );
}

function Integrations() {
  return (
    <section className="mt-28">
      <SectionHeading
        eyebrow="Bring your own keys"
        title="Two integrations, both optional"
      />
      <div className="mt-10 grid gap-6 md:grid-cols-2">
        <IntegrationCard
          name="Twilio"
          tagline="SMS + MMS delivery"
          body="Send the captured photo as an SMS link or an MMS image. SnapCabin authenticates with your Account SID + Auth Token; per-session and per-phone rate limits keep one phone from spamming the line."
          ctaHref="/setup/twilio"
        />
        <IntegrationCard
          name="Cloudinary"
          tagline="Public photo hosting"
          body="Photos upload to a folder scoped to the current event via an unsigned preset. The kiosk never holds your API secret, and you control retention, transforms, and access on your end."
          ctaHref="/setup/cloudinary"
        />
      </div>
      <p className="mt-6 text-center text-sm text-mist">
        Skip both and the kiosk still runs — guests download photos by scanning
        the QR code on the same WiFi.
      </p>
    </section>
  );
}

function IntegrationCard({
  name,
  tagline,
  body,
  ctaHref,
}: {
  name: string;
  tagline: string;
  body: string;
  ctaHref: string;
}) {
  return (
    <article className="flex flex-col gap-4 rounded-3xl border border-walnut/20 bg-cream/80 p-8">
      <div>
        <p className="font-sans text-xs font-semibold uppercase tracking-widest text-pine">
          {tagline}
        </p>
        <h3 className="mt-1 font-display text-3xl font-bold text-espresso">
          {name}
        </h3>
      </div>
      <p className="text-base leading-relaxed text-espresso/80">{body}</p>
      <div>
        <Link
          href={ctaHref}
          className="inline-block rounded-xl bg-pine px-5 py-2.5 font-sans text-sm font-semibold text-cream no-underline transition hover:bg-pine-deep"
        >
          Open setup guide →
        </Link>
      </div>
    </article>
  );
}

function Faq() {
  return (
    <section className="mt-28">
      <SectionHeading eyebrow="FAQ" title="Quick answers" />
      <div className="mt-10 space-y-4">
        <FaqRow
          q="Do I need a backend?"
          a="No. SnapCabin runs entirely on the tablet. Twilio and Cloudinary are optional — they're how guests receive their photo by text. Without them, guests download via the QR code on the local WiFi."
        />
        <FaqRow
          q="Does SnapCabin see any data?"
          a="No analytics, no telemetry, no crash reporting, no accounts. Your operator is the data controller for anything sent through Twilio or Cloudinary."
        />
        <FaqRow
          q="What tablet do I need?"
          a="Any modern Android tablet (Android 8+, 8.7–12.4 inch screen, landscape). We've tested on Samsung Tab S6 / S8 / S9."
        />
        <FaqRow
          q="Can I run multiple events?"
          a="Today the kiosk handles one active event at a time. Start a new event to roll over branding, Cloudinary folder, and rate limits."
        />
      </div>
    </section>
  );
}

function FaqRow({ q, a }: { q: string; a: string }) {
  return (
    <details className="group rounded-2xl border border-walnut/15 bg-cream/70 p-5 open:bg-cream/90">
      <summary className="cursor-pointer list-none font-display text-xl font-medium text-espresso group-open:text-pine-deep">
        {q}
      </summary>
      <p className="mt-3 text-base leading-relaxed text-espresso/80">{a}</p>
    </details>
  );
}

function Footer() {
  return (
    <footer className="mt-28 flex flex-col items-center gap-4 border-t border-walnut/15 pt-10 text-sm text-mist">
      <p className="font-display text-base italic text-walnut">
        SnapCabin · A photo booth in the woods
      </p>
      <div className="flex flex-wrap items-center justify-center gap-6 font-sans">
        <Link href="/setup" className="font-semibold no-underline">
          Setup
        </Link>
        <Link href="/privacy" className="font-semibold no-underline">
          Privacy
        </Link>
        <a
          href="mailto:hello@snapcabin.app"
          className="font-semibold no-underline"
        >
          hello@snapcabin.app
        </a>
      </div>
      <p className="text-xs text-mist">
        Not affiliated with Twilio, Cloudinary, Samsung, or Google.
      </p>
    </footer>
  );
}

function SectionHeading({
  eyebrow,
  title,
}: {
  eyebrow: string;
  title: string;
}) {
  return (
    <div className="flex flex-col items-center gap-3 text-center">
      <p className="text-xs font-sans font-semibold uppercase tracking-[0.3em] text-honey-deep">
        {eyebrow}
      </p>
      <h2 className="font-display text-4xl font-bold text-espresso sm:text-5xl">
        {title}
      </h2>
    </div>
  );
}
