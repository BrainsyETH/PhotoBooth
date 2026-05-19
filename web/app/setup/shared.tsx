import Link from "next/link";

export function BottomLinks({
  current,
}: {
  current: "twilio" | "cloudinary";
}) {
  return (
    <div className="mt-16 flex flex-col gap-3 sm:flex-row sm:justify-between">
      <Link
        href="/setup"
        className="text-sm font-sans font-semibold uppercase tracking-widest text-walnut no-underline hover:text-walnut-deep"
      >
        ← All setup guides
      </Link>
      {current === "twilio" ? (
        <Link
          href="/setup/cloudinary"
          className="text-sm font-sans font-semibold uppercase tracking-widest text-pine no-underline hover:text-pine-deep"
        >
          Next: Cloudinary →
        </Link>
      ) : (
        <Link
          href="/setup/twilio"
          className="text-sm font-sans font-semibold uppercase tracking-widest text-pine no-underline hover:text-pine-deep"
        >
          Next: Twilio →
        </Link>
      )}
    </div>
  );
}

export function Step({
  n,
  title,
  children,
}: {
  n: string;
  title: string;
  children: React.ReactNode;
}) {
  return (
    <section className="mt-12">
      <div className="flex items-baseline gap-4">
        <span className="font-display text-3xl font-semibold text-honey-deep">
          {n}
        </span>
        <h2 className="font-display text-2xl font-medium text-espresso">
          {title}
        </h2>
      </div>
      <div className="mt-3 space-y-3 text-base leading-relaxed text-espresso/85">
        {children}
      </div>
    </section>
  );
}

export function Callout({
  title,
  children,
}: {
  title: string;
  children: React.ReactNode;
}) {
  return (
    <aside className="mt-10 rounded-3xl border border-pine/30 bg-cream p-6">
      <h3 className="font-sans text-xs font-semibold uppercase tracking-widest text-pine-deep">
        {title}
      </h3>
      <div className="mt-3 space-y-2 text-base leading-relaxed text-espresso/85">
        {children}
      </div>
    </aside>
  );
}

export function Warning({ children }: { children: React.ReactNode }) {
  return (
    <p className="rounded-2xl border border-clay/40 bg-cream px-4 py-3 text-sm text-clay">
      {children}
    </p>
  );
}
