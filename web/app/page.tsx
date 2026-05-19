import Image from "next/image";
import Link from "next/link";

export default function Home() {
  return (
    <main className="mx-auto max-w-5xl px-6 pb-24 pt-16 sm:pt-24">
      <Hero />
      <Features />
      <HowItWorks />
      <Comparison />
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
        how they want it, and walk away with it on their phone. No
        subscription, no monthly bill, no surprise add-ons. One-time
        purchase, runs on the tablet you bring.
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
      title: "Photos straight to a phone",
      body: "Guests scan a QR code or punch in their number and the photo arrives by text. Email works too. Both run through services you connect (Cloudinary and Twilio).",
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
      body: "An on-tablet log shows every text or email that went out, with timestamps and the last four digits of the number.",
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
      title: "Plug in text and photo hosting",
      body: "Paste your Twilio keys for SMS, and a Cloudinary preset so photos can travel by text. Both are optional and we link you to step-by-step guides.",
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

function Comparison() {
  return (
    <section className="mt-28">
      <SectionHeading
        eyebrow="Cost compared"
        title="What a 250-photo event actually costs"
      />
      <p className="mx-auto mt-4 max-w-2xl text-center text-base leading-relaxed text-espresso/75">
        Most photo booth apps charge a monthly fee whether you run one event a
        year or twenty. SnapCabin is a one-time purchase. Texting and hosting
        are billed at cost by Twilio and Cloudinary, with no markup from us.
      </p>

      <div className="mt-10 overflow-x-auto rounded-3xl border border-walnut/15 bg-cream/70">
        <table className="w-full text-left text-sm sm:text-base">
          <thead>
            <tr className="border-b border-walnut/15 text-xs uppercase tracking-widest text-honey-deep">
              <th className="px-5 py-4 font-sans font-semibold">Option</th>
              <th className="px-5 py-4 font-sans font-semibold">
                Up-front
              </th>
              <th className="px-5 py-4 font-sans font-semibold">
                Per month
              </th>
              <th className="px-5 py-4 font-sans font-semibold">
                Per event of 250 photos
              </th>
            </tr>
          </thead>
          <tbody className="divide-y divide-walnut/10">
            <tr className="bg-pine/5">
              <td className="px-5 py-4">
                <p className="font-display text-lg font-semibold text-espresso">
                  SnapCabin
                </p>
                <p className="text-xs text-mist">
                  One-time app, pay-as-you-go text + hosting
                </p>
              </td>
              <td className="px-5 py-4 font-sans text-espresso">
                One-time
              </td>
              <td className="px-5 py-4 font-sans text-pine-deep">$0</td>
              <td className="px-5 py-4 font-sans font-semibold text-pine-deep">
                ~$5 in text fees
              </td>
            </tr>
            <tr>
              <td className="px-5 py-4">
                <p className="font-display text-lg font-semibold text-espresso">
                  Snappic Starter
                </p>
                <p className="text-xs text-mist">Monthly subscription</p>
              </td>
              <td className="px-5 py-4 font-sans text-espresso">$0</td>
              <td className="px-5 py-4 font-sans text-espresso">$69</td>
              <td className="px-5 py-4 font-sans text-espresso">
                $69 (one month minimum)
              </td>
            </tr>
            <tr>
              <td className="px-5 py-4">
                <p className="font-display text-lg font-semibold text-espresso">
                  Snappic Business
                </p>
                <p className="text-xs text-mist">Monthly subscription</p>
              </td>
              <td className="px-5 py-4 font-sans text-espresso">$0</td>
              <td className="px-5 py-4 font-sans text-espresso">$189</td>
              <td className="px-5 py-4 font-sans text-espresso">
                $189 (one month minimum)
              </td>
            </tr>
            <tr>
              <td className="px-5 py-4">
                <p className="font-display text-lg font-semibold text-espresso">
                  Simple Booth HALO
                </p>
                <p className="text-xs text-mist">
                  Weekly or monthly plans, iPad app
                </p>
              </td>
              <td className="px-5 py-4 font-sans text-espresso">$0</td>
              <td className="px-5 py-4 font-sans text-espresso">
                Plans from $9/week
              </td>
              <td className="px-5 py-4 font-sans text-espresso">
                ~$36 if billed weekly all month
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <p className="mx-auto mt-4 max-w-3xl text-center text-xs text-mist">
        Prices listed for the other apps are taken from their public pricing
        pages as of May 2026 and may have changed.{" "}
        <a
          href="https://www.snappic.com/pricing"
          target="_blank"
          rel="noopener noreferrer"
        >
          Snappic pricing
        </a>
        ,{" "}
        <a
          href="https://www.simplebooth.com/plans"
          target="_blank"
          rel="noopener noreferrer"
        >
          Simple Booth plans
        </a>
        . SnapCabin&rsquo;s text cost is based on Twilio&rsquo;s published US
        MMS rate of about $0.02 per message; international rates vary.
      </p>

      <div className="mt-12">
        <h3 className="text-center font-display text-2xl font-medium text-espresso">
          Features compared
        </h3>
        <div className="mt-6 overflow-x-auto rounded-3xl border border-walnut/15 bg-cream/70">
          <table className="w-full text-left text-sm">
            <thead>
              <tr className="border-b border-walnut/15 text-xs uppercase tracking-widest text-honey-deep">
                <th className="px-5 py-4 font-sans font-semibold">Feature</th>
                <th className="px-5 py-4 text-center font-sans font-semibold">
                  SnapCabin
                </th>
                <th className="px-5 py-4 text-center font-sans font-semibold">
                  Snappic Starter
                </th>
                <th className="px-5 py-4 text-center font-sans font-semibold">
                  Simple Booth HALO
                </th>
              </tr>
            </thead>
            <tbody className="divide-y divide-walnut/10 text-espresso/85">
              <FeatureRow label="Single photo, collage, GIF modes" />
              <FeatureRow label="Send photo by text (SMS / MMS)" />
              <FeatureRow label="QR-code on-WiFi download" snapcabin simplebooth={false} snappic={false} />
              <FeatureRow
                label="Photos stored on your own cloud account"
                snapcabin
                snappic={false}
                simplebooth={false}
              />
              <FeatureRow label="Custom border and logo overlay" />
              <FeatureRow label="Works offline once configured" snapcabin snappic={false} simplebooth={false} />
              <FeatureRow
                label="Pay only for what you use after purchase"
                snapcabin
                snappic={false}
                simplebooth={false}
              />
              <FeatureRow
                label="Source available for inspection"
                snapcabin
                snappic={false}
                simplebooth={false}
              />
            </tbody>
          </table>
        </div>
        <p className="mx-auto mt-3 max-w-3xl text-center text-xs text-mist">
          Other app feature lists are best-effort summaries from their public
          marketing pages as of May 2026. We&rsquo;d love to be told what
          we&rsquo;ve missed.
        </p>
      </div>
    </section>
  );
}

function FeatureRow({
  label,
  snapcabin = true,
  snappic = true,
  simplebooth = true,
}: {
  label: string;
  snapcabin?: boolean;
  snappic?: boolean;
  simplebooth?: boolean;
}) {
  return (
    <tr>
      <td className="px-5 py-3">{label}</td>
      <td className="px-5 py-3 text-center">{snapcabin ? <Check /> : <Dash />}</td>
      <td className="px-5 py-3 text-center">{snappic ? <Check /> : <Dash />}</td>
      <td className="px-5 py-3 text-center">{simplebooth ? <Check /> : <Dash />}</td>
    </tr>
  );
}

function Check() {
  return <span className="font-bold text-pine-deep">Yes</span>;
}

function Dash() {
  return <span className="text-mist">No</span>;
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
          name="Twilio"
          tagline="Texts a photo to your guest"
          body="When a guest types their phone number, Twilio sends the photo as a text. You bring the account and we use your keys on the tablet only. Pay Twilio per message at their published rate."
          ctaHref="/setup/twilio"
        />
        <IntegrationCard
          name="Cloudinary"
          tagline="Keeps photos available online"
          body="So the text arrives with the photo attached instead of just a link, the tablet uploads each photo to your Cloudinary account first. The free tier covers a typical event."
          ctaHref="/setup/cloudinary"
        />
      </div>
      <p className="mt-6 text-center text-sm text-mist">
        Cloudinary powers the QR code and the photo attached to text
        messages. Without it, the tablet still saves locally and the system
        share menu still works, you just won&rsquo;t hand off the photo
        over cellular.
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
          q="Do I have to use Twilio and Cloudinary?"
          a="Only if you want photos to travel off the tablet. Cloudinary hosts the photo so a QR code or text can carry it; Twilio is the text-message service. Without them, guests can still save locally, share via the Android system share sheet, or print. There&rsquo;s no local-WiFi QR option any more because modern mobile browsers block plain-HTTP downloads."
        />
        <FaqRow
          q="What kind of tablet do I need?"
          a="An Android tablet, at least eight inches, ideally with a decent front-facing camera. We&rsquo;ve had good results with Samsung Galaxy Tab S6 and newer. Phones aren&rsquo;t supported. The app refuses to launch on devices below an 8-inch class screen so you don&rsquo;t end up with a broken layout at the event."
        />
        <FaqRow
          q="Does SnapCabin send any data back to you?"
          a="The app does not include analytics SDKs, crash reporters, or other libraries that phone home. Whatever your guests send through Twilio or Cloudinary goes only to your account at those services. The privacy policy covers this in more detail."
        />
        <FaqRow
          q="Can I run multiple events?"
          a="Yes. You give each event a name and the app keeps photos, audit logs, and rate limits scoped to it. Today the kiosk handles one active event at a time. Start a new event when you&rsquo;re ready to switch."
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
        SnapCabin is not affiliated with Twilio, Cloudinary, Snappic, Simple
        Booth, Samsung, or Google.
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
