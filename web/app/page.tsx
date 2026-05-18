import Link from "next/link";

export default function Home() {
  return (
    <main className="mx-auto flex min-h-screen max-w-3xl flex-col items-center justify-center gap-10 px-6 py-20 text-center">
      <h1 className="font-display text-6xl font-bold leading-tight text-espresso sm:text-7xl">
        SnapCabin
      </h1>
      <p className="font-display text-2xl italic text-walnut">
        A photo booth in the woods.
      </p>
      <p className="max-w-xl text-lg text-espresso/80">
        Self-hosted Android photo-booth kiosk for weddings and gatherings. No
        analytics, no telemetry, no monthly fee — pay once and run it on your
        own hardware.
      </p>
      <div className="flex flex-col gap-3 text-base sm:flex-row sm:gap-8">
        <Link href="/privacy" className="font-sans font-semibold">
          Privacy policy →
        </Link>
        <a href="mailto:hello@snapcabin.app" className="font-sans font-semibold">
          Contact →
        </a>
      </div>
    </main>
  );
}
