import Image from "next/image";
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
        Photo booth for your event, on your tablet
      </p>
      <Image
        src="/snapcabin-logo.png"
        alt="SnapCabin"
        width={1536}
        height={1024}
        priority
        className="h-auto w-full max-w-xl"
      />
      <p className="max-w-xl text-lg leading-relaxed text-espresso/85">
        A photo booth for an Android tablet. Guests snap their photo, pick
        how they want it, and walk away with it in their inbox. Runs on the
        tablet you bring, with the integrations you connect.
      </p>
      <div className="flex flex-wrap items-center justify-center gap-4">
        <Link
          href="/setup"
          className="rounded-2xl bg-pine px-6 py-3 font-sans text-base font-semibold text-cream no-underline shadow-sm transition hover:bg-pine-deep"
        >
          See how to set it up
        </Link>
        <a
          href="mailto:hello@snapcabin.app"
          className="rounded-2xl border border-walnut/40 px-6 py-3 font-sans text-base font-semibold text-walnut no-underline transition hover:border-walnut hover:text-walnut-deep"
        >
          Ask a question
        </a>
      </div>
    </section>
  );
}

function Features() {
  const items = [
    {
      title: "Runs on an Android tablet",
      body: "Pick any modern Android tablet (at least an 8-inch screen). Install once, take it to the next event, and the next.",
    },
    {
      title: "Three ways to capture a moment",
      body: "Single photo, four-shot collage, or short looping GIF. Guests pick what feels right.",
    },
    {
      title: "Photos straight to a guest's inbox",
      body: "Guests type their email and the photo arrives as an attachment over your venue's WiFi. A QR code option lets them scan and download too. Both run through services you connect (Resend and Cloudinary).",
    },
    {
      title: "Make it look like your event",
      body: "Add the couple's names, a tagline, a watermark, a custom border, or a logo overlay. Change any of it from the kiosk.",
    },
    {
      title: "Your photos stay yours",
      body: "Photos live on the tablet, or in your own Cloudinary account if you wire one up. SnapCabin never holds copies.",
    },
    {
      title: "A history of every send",
      body: "An on-tablet log shows every email that went out, with timestamps and a masked recipient address.",
    },
  ];
  return (
    <section className="mt-28 grid gap-6 sm:grid-cols-2">
      {items.map((f) => (
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
      title: "Install once",
      body: "Buy SnapCabin, install it on your Android tablet. Setup wizard runs you through camera and storage permissions.",
    },
    {
      n: "02",
      title: "Name your event",
      body: "Tap a hidden corner, type the admin PIN, give the event a name and a tagline. The look of the booth updates right away.",
    },
    {
      n: "03",
      title: "Plug in email and photo hosting",
      body: "Paste your Resend API key for email delivery, and a Cloudinary preset for the QR code. Both are optional and we link you to step-by-step guides.",
    },
    {
      n: "04",
      title: "Run the event",
      body: "Turn on Kiosk Mode to lock the tablet to SnapCabin for the night. Guests do the rest.",
    },
  ];
  return (
    <section className="mt-28">
      <SectionHeading
        eyebrow="How it works"
        title="From box to booth in an afternoon"
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
        title="Two services do the heavy lifting"
      />
      <div className="mt-10 grid gap-6 md:grid-cols-2">
        <IntegrationCard
          name="Resend"
          tagline="Emails the photo as an attachment"
          body="When a guest types their email address, Resend delivers a tidy message with the photo attached. You bring the account; the tablet uses your API key only. Resend's free tier covers 3,000 emails per month."
          ctaHref="/setup/resend"
        />
        <IntegrationCard
          name="Cloudinary"
          tagline="Hosts the photo behind a QR code"
          body="If you want a QR-code download option on the share screen, the tablet uploads each photo to your Cloudinary account and renders a code pointing at it. The free tier covers a typical event."
          ctaHref="/setup/cloudinary"
        />
      </div>
      <p className="mt-6 text-center text-sm text-mist">
        The two integrations are independent. Turn on Resend for email
        delivery, Cloudinary for the QR tile, or both. Without either, the
        tablet still saves locally and the system share menu still works.
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
          Read the setup guide
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
          q="Do I have to use Resend and Cloudinary?"
          a="Only if you want photos to travel off the tablet. Resend emails the photo to a guest as an attachment; Cloudinary hosts a copy so a QR code can deliver it. Both are optional and independent. Without them, guests can still save locally, share via the Android system share sheet, or print. There's no local-WiFi QR option any more because modern mobile browsers block plain-HTTP downloads."
        />
        <FaqRow
          q="What kind of tablet do I need?"
          a="An Android tablet, at least eight inches, ideally with a decent front-facing camera. We've had good results with Samsung Galaxy Tab S6 and newer. Phones aren't supported. The app refuses to launch on devices below an 8-inch class screen so you don't end up with a broken layout at the event."
        />
        <FaqRow
          q="Does SnapCabin send any data back to you?"
          a="The app does not include analytics SDKs, crash reporters, or other libraries that phone home. Whatever your guests send through Resend or Cloudinary goes only to your account at those services. The privacy policy covers this in more detail."
        />
        <FaqRow
          q="Can I run multiple events?"
          a="Yes. You give each event a name and the app keeps photos, audit logs, and rate limits scoped to it. Today the kiosk handles one active event at a time. Start a new event when you're ready to switch."
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
      <Image
        src="/snapcabin-logo.png"
        alt="SnapCabin"
        width={1536}
        height={1024}
        className="h-auto w-full max-w-[240px] opacity-80"
      />
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
        SnapCabin is not affiliated with Resend, Cloudinary, Samsung, or
        Google.
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
