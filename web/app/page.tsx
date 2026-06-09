import Image from "next/image";
import Link from "next/link";
import { PLAY_STORE_URL, PRICE, SUPPORT_MAILTO } from "@/lib/links";

export default function Home() {
  return (
    <main className="mx-auto max-w-5xl px-6 pb-24 pt-16 sm:pt-24">
      <Hero />
      <Features />
      <Screenshots />
      <PrivacyBand />
      <HowItWorks />
      <Integrations />
      <Faq />
      <Footer />
    </main>
  );
}

/** Primary Play Store call-to-action, used in the hero, after How-It-Works,
 *  and in the footer. Price lives on the button itself. */
function PlayStoreButton({ className = "" }: { className?: string }) {
  return (
    <a
      href={PLAY_STORE_URL}
      className={`rounded-2xl bg-pine px-6 py-3 text-center font-sans text-base font-semibold text-white no-underline shadow-sm transition hover:bg-pine-deep ${className}`}
    >
      Get SnapCabin — {PRICE} on Google Play
    </a>
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
      <h1 className="font-display text-4xl font-bold text-espresso sm:text-5xl">
        The photo booth that lives on your tablet.
      </h1>
      <p className="max-w-xl text-lg leading-relaxed text-espresso/85">
        Guests snap their photo, pick how they want it, and walk away with it in
        their inbox. Runs on the tablet you already own. Photos go straight to
        your guests.
      </p>
      <div className="flex flex-wrap items-center justify-center gap-4">
        <PlayStoreButton />
        <Link
          href="/setup"
          className="rounded-2xl border border-walnut/40 px-6 py-3 font-sans text-base font-semibold text-walnut no-underline transition hover:border-walnut hover:text-walnut-deep"
        >
          See how to set it up
        </Link>
      </div>
      <p className="max-w-xl text-base italic leading-relaxed text-espresso/70">
        Like a little cabin: everything it needs is inside, and nobody else has
        a key.
      </p>
      <HeroDevice />
    </section>
  );
}

/** Tablet-on-a-stand framing for the hero screenshot. */
function HeroDevice() {
  return (
    <div className="mt-6 flex w-full max-w-md flex-col items-center">
      <div className="w-full rounded-[2rem] border-[10px] border-espresso/85 bg-espresso/85 p-1 shadow-xl">
        <ScreenshotFrame
          src="/screenshots/hero-tablet.png"
          label="hero-tablet.png"
          alt="SnapCabin running on a tablet"
          className="aspect-[4/5] rounded-[1.5rem]"
          priority
        />
      </div>
      {/* simple stand */}
      <div className="h-10 w-2 bg-espresso/70" />
      <div className="h-2 w-40 rounded-full bg-espresso/70" />
    </div>
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
      body: "Guests type their email and the photo arrives as an attachment. A QR code option lets them scan and download too.",
    },
    {
      title: "Make it look like your event",
      body: "Add the couple's names, a tagline, a watermark, a custom border, or a logo overlay. Change any of it from the kiosk.",
    },
    {
      title: "Your photos stay yours",
      body: "Photos live on the tablet, or in your own photo-hosting account if you wire one up. SnapCabin never holds copies.",
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

/** Three-screenshot strip: attract → capture → share. */
function Screenshots() {
  const shots = [
    {
      src: "/screenshots/attract.png",
      label: "attract.png",
      caption: "Attract",
      alt: "SnapCabin attract screen inviting guests to tap to start",
    },
    {
      src: "/screenshots/capture.png",
      label: "capture.png",
      caption: "Capture",
      alt: "SnapCabin capture screen with photo, collage, and GIF options",
    },
    {
      src: "/screenshots/share.png",
      label: "share.png",
      caption: "Share",
      alt: "SnapCabin share screen with email entry and a QR code",
    },
  ];
  return (
    <section className="mt-20" aria-label="What guests see">
      <div className="grid gap-6 sm:grid-cols-3">
        {shots.map((s) => (
          <figure key={s.src} className="flex flex-col items-center gap-3">
            <ScreenshotFrame
              src={s.src}
              label={s.label}
              alt={s.alt}
              className="aspect-[2/3] w-full rounded-3xl border border-walnut/15"
            />
            <figcaption className="font-sans text-sm font-semibold uppercase tracking-widest text-honey-deep">
              {s.caption}
            </figcaption>
          </figure>
        ))}
      </div>
    </section>
  );
}

/** Renders a screenshot with a clearly-labeled "REPLACE" overlay so the
 *  parchment placeholders read as placeholders until real PNGs arrive. */
function ScreenshotFrame({
  src,
  label,
  alt,
  className = "",
  priority = false,
}: {
  src: string;
  label: string;
  alt: string;
  className?: string;
  priority?: boolean;
}) {
  return (
    <div className={`relative overflow-hidden bg-parchment ${className}`}>
      <Image
        src={src}
        alt={alt}
        fill
        sizes="(max-width: 640px) 100vw, 33vw"
        priority={priority}
        className="object-cover"
      />
      <span
        aria-hidden
        className="pointer-events-none absolute inset-0 flex items-center justify-center p-4 text-center font-sans text-sm font-semibold uppercase tracking-widest text-walnut/70"
      >
        REPLACE: {label}
      </span>
    </div>
  );
}

function PrivacyBand() {
  return (
    <section className="mt-20 rounded-3xl border border-pine/20 bg-pine/5 px-8 py-12 text-center">
      <h2 className="font-display text-3xl font-bold text-espresso sm:text-4xl">
        No accounts. No cloud. No subscription.
      </h2>
      <p className="mx-auto mt-4 max-w-2xl text-lg leading-relaxed text-espresso/75">
        Photos stay yours. They live on the tablet, or in your own photo-hosting
        account if you connect one — SnapCabin never holds copies and never
        phones home.
      </p>
      <div className="mt-6">
        <Link
          href="/privacy"
          className="font-sans text-base font-semibold text-pine-deep"
        >
          Read the privacy policy
        </Link>
      </div>
    </section>
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
      body: "Long-press a corner, type the admin PIN, give the event a name and a tagline. The look of the booth updates right away.",
    },
    {
      n: "03",
      title: "Plug in email and photo hosting",
      body: "Add a key for email delivery and a preset for the QR code. Both are optional and we link you to step-by-step guides.",
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
            <h3 className="mt-3 font-display text-xl font-medium text-espresso">
              {s.title}
            </h3>
            <p className="mt-2 text-sm leading-relaxed text-espresso/75">
              {s.body}
            </p>
          </li>
        ))}
      </ol>
      <div className="mt-12 flex justify-center">
        <PlayStoreButton />
      </div>
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
      <p className="mx-auto mt-6 max-w-2xl text-center text-base leading-relaxed text-espresso/70">
        The two integrations are independent. Turn on Resend for email delivery,
        Cloudinary for the QR tile, or both. Without either, the tablet still
        saves locally and the system share menu still works.
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
        <p className="font-sans text-xs font-semibold uppercase tracking-widest text-pine-deep">
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
          className="inline-block rounded-xl bg-pine px-5 py-2.5 font-sans text-sm font-semibold text-white no-underline transition hover:bg-pine-deep"
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
          q="What does it cost?"
          a="$2.99 on Google Play, per device. Pay once, install on the tablet you're using as the booth. If you run more than one tablet at the same event, each tablet is a separate $2.99 install. Whatever you spend at Resend or Cloudinary for delivery and hosting is between you and them — we don't take a cut. Refunds follow Google Play's standard refund policy."
        />
        <FaqRow
          q="Do I have to use Resend and Cloudinary?"
          a="Only if you want photos to travel off the tablet. Resend emails the photo to a guest as an attachment; Cloudinary hosts a copy so a QR code can deliver it. Both are optional and independent. Without them, guests can still save locally, share via the Android system share sheet, or print."
        />
        <FaqRow
          q="What kind of tablet do I need?"
          a="An Android tablet, at least eight inches, ideally with a decent front-facing camera. We've had good results with Samsung Galaxy Tab S6 and newer. Phones aren't supported. SnapCabin is built for 8-inch tablets and larger, so the layout always looks right at the event."
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
      <summary className="flex cursor-pointer list-none items-center justify-between gap-4 font-display text-xl font-medium text-espresso group-open:text-pine-deep">
        <span>{q}</span>
        <svg
          aria-hidden
          viewBox="0 0 24 24"
          className="h-5 w-5 flex-shrink-0 text-honey-deep transition-transform duration-200 group-open:rotate-180"
          fill="none"
          stroke="currentColor"
          strokeWidth="2"
          strokeLinecap="round"
          strokeLinejoin="round"
        >
          <polyline points="6 9 12 15 18 9" />
        </svg>
      </summary>
      <p className="mt-3 text-base leading-relaxed text-espresso/80">{a}</p>
    </details>
  );
}

function Footer() {
  return (
    <footer className="mt-28 flex flex-col items-center gap-6 border-t border-walnut/15 pt-10 text-sm text-espresso/70">
      <Image
        src="/snapcabin-logo.png"
        alt=""
        width={1536}
        height={1024}
        className="h-auto w-full max-w-[240px] opacity-80"
      />
      <PlayStoreButton />
      <div className="flex flex-wrap items-center justify-center gap-6 font-sans">
        <Link href="/setup" className="font-semibold no-underline">
          Setup
        </Link>
        <Link href="/privacy" className="font-semibold no-underline">
          Privacy
        </Link>
        <Link href="/terms" className="font-semibold no-underline">
          Terms
        </Link>
        <a href={SUPPORT_MAILTO} className="font-semibold no-underline">
          hello@snapcabin.app
        </a>
      </div>
      <p className="text-xs text-mist">
        SnapCabin is not affiliated with Resend, Cloudinary, Samsung, or Google.
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
