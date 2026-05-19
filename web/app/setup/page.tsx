import Link from "next/link";

export const metadata = {
  title: "SnapCabin · Setup guides",
  description:
    "Step-by-step guides for wiring SnapCabin to Twilio (text messages) and Cloudinary (photo hosting).",
};

export default function SetupIndex() {
  return (
    <main className="mx-auto max-w-3xl px-6 py-16">
      <Link
        href="/"
        className="mb-10 inline-block text-sm font-sans font-semibold uppercase tracking-widest text-walnut no-underline hover:text-walnut-deep"
      >
        ← Back to SnapCabin
      </Link>
      <h1 className="font-display text-5xl font-bold text-espresso">
        Setup guides
      </h1>
      <p className="mt-4 max-w-xl text-lg leading-relaxed text-espresso/80">
        SnapCabin works offline by default. These two optional integrations
        are what let a guest get the photo straight to their phone over
        cellular instead of WiFi.
      </p>

      <section className="mt-12 grid gap-6 md:grid-cols-2">
        <SetupCard
          title="Twilio"
          subtitle="Send the photo by text"
          href="/setup/twilio"
          minutes="About 10 minutes"
        />
        <SetupCard
          title="Cloudinary"
          subtitle="Host the photo so the text can include it"
          href="/setup/cloudinary"
          minutes="About 5 minutes"
        />
      </section>

      <section className="mt-16 rounded-3xl border border-walnut/15 bg-cream/70 p-6">
        <h2 className="font-display text-2xl font-medium text-espresso">
          Recommended order
        </h2>
        <ol className="mt-4 list-decimal space-y-2 pl-6 text-base text-espresso/85">
          <li>Set up Cloudinary first. It gives you a public photo URL.</li>
          <li>Then Twilio. It uses that URL when sending the picture.</li>
          <li>
            Test each one from the admin screen on the tablet before your
            event.
          </li>
        </ol>
      </section>
    </main>
  );
}

function SetupCard({
  title,
  subtitle,
  href,
  minutes,
}: {
  title: string;
  subtitle: string;
  href: string;
  minutes: string;
}) {
  return (
    <Link
      href={href}
      className="block rounded-3xl border border-walnut/20 bg-cream/80 p-6 no-underline transition hover:border-pine hover:bg-cream"
    >
      <p className="font-sans text-xs font-semibold uppercase tracking-widest text-pine">
        {subtitle}
      </p>
      <h2 className="mt-1 font-display text-3xl font-bold text-espresso">
        {title}
      </h2>
      <p className="mt-3 text-sm text-mist">{minutes}, step by step</p>
    </Link>
  );
}
