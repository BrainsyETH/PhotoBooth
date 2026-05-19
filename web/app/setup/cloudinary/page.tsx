import Link from "next/link";
import { BottomLinks, Callout, Step, Warning } from "../shared";

export const metadata = {
  title: "SnapCabin · Cloudinary setup",
  description:
    "Step-by-step instructions for wiring Cloudinary to a SnapCabin kiosk for public photo hosting.",
};

export default function CloudinarySetup() {
  return (
    <main className="mx-auto max-w-3xl px-6 py-16">
      <Link
        href="/setup"
        className="mb-10 inline-block text-sm font-sans font-semibold uppercase tracking-widest text-walnut no-underline hover:text-walnut-deep"
      >
        ← All setup guides
      </Link>

      <p className="text-xs font-sans font-semibold uppercase tracking-[0.3em] text-honey-deep">
        Integration · Cloudinary
      </p>
      <h1 className="mt-2 font-display text-5xl font-bold text-espresso">
        Host event photos publicly
      </h1>
      <p className="mt-4 max-w-xl text-lg leading-relaxed text-espresso/80">
        Cloudinary is where the kiosk uploads each photo so Twilio (or anyone
        you give the link to) can fetch it over the public internet. You own
        the account; the kiosk uses an <em>unsigned upload preset</em> so your
        API secret never lives on the device.
      </p>

      <Callout title="Before you start">
        <ul className="ml-5 list-disc space-y-1">
          <li>
            A free Cloudinary account is enough for most events — 25 GB
            bandwidth and 25 GB storage per month.
          </li>
          <li>About 5 minutes.</li>
        </ul>
      </Callout>

      <Step n="1" title="Create a Cloudinary account">
        <p>
          Sign up at{" "}
          <a
            href="https://cloudinary.com/users/register_free"
            target="_blank"
            rel="noopener noreferrer"
          >
            cloudinary.com
          </a>
          . You can skip the questions about your stack.
        </p>
      </Step>

      <Step n="2" title="Note your cloud name">
        <p>
          From the dashboard at <strong>console.cloudinary.com</strong>, the
          "Account Details" card shows a <strong>Cloud name</strong> — usually
          your handle. Copy it.
        </p>
      </Step>

      <Step n="3" title="Create an unsigned upload preset">
        <p>
          Open <strong>Settings → Upload → Upload presets</strong> (or visit{" "}
          <a
            href="https://console.cloudinary.com/settings/upload"
            target="_blank"
            rel="noopener noreferrer"
          >
            console.cloudinary.com/settings/upload
          </a>
          ).
        </p>
        <p>Click <strong>Add upload preset</strong> and configure:</p>
        <ul className="ml-5 list-disc space-y-1">
          <li><strong>Preset name</strong>: <code>snapcabin_events</code> (or any name)</li>
          <li><strong>Signing Mode</strong>: <strong>Unsigned</strong></li>
          <li><strong>Folder</strong>: leave blank — the kiosk overrides this per event</li>
          <li><strong>Allowed formats</strong>: <code>jpg, png, webp</code></li>
          <li><strong>Max file size</strong>: 10000000 (10 MB) — defensive against abuse</li>
        </ul>
        <p>Save the preset and copy the exact name.</p>
        <Warning>
          Unsigned presets let anyone with the name upload. Always restrict by
          format + size. If a preset leaks, you can rotate it from the same
          page in seconds.
        </Warning>
      </Step>

      <Step n="4" title="Enter the credentials on the kiosk">
        <p>On the kiosk:</p>
        <ol className="ml-5 list-decimal space-y-1">
          <li>Long-press the bottom-right corner of the Attract screen.</li>
          <li>Enter the admin PIN.</li>
          <li>
            Open <strong>CLOUDINARY PHOTO HOSTING</strong> and toggle "Upload
            photos to Cloudinary before SMS".
          </li>
          <li>Paste the <strong>Cloud name</strong> and the <strong>upload preset name</strong> (case-sensitive).</li>
        </ol>
      </Step>

      <Step n="5" title="Test an upload">
        <p>
          Start a test event from the EVENT section, capture a photo, then tap
          the SMS or QR button. The kiosk uploads to{" "}
          <code>events/&lt;your-event-slug&gt;/</code> in your Cloudinary
          account. Open{" "}
          <a
            href="https://console.cloudinary.com/console/media_library"
            target="_blank"
            rel="noopener noreferrer"
          >
            the Media Library
          </a>{" "}
          and confirm the photo arrived.
        </p>
      </Step>

      <Callout title="Common errors">
        <dl className="space-y-3">
          <dt className="font-sans font-semibold text-espresso">
            <code>Upload preset not found</code>
          </dt>
          <dd className="ml-4 text-base text-espresso/80">
            The preset name on the kiosk doesn't match exactly (case-sensitive).
            Re-copy it from the Cloudinary console.
          </dd>
          <dt className="font-sans font-semibold text-espresso">
            <code>Unsigned upload disabled</code>
          </dt>
          <dd className="ml-4 text-base text-espresso/80">
            The preset was created with Signing Mode = Signed. Edit it and
            switch to Unsigned.
          </dd>
          <dt className="font-sans font-semibold text-espresso">
            <code>Resource type not allowed</code>
          </dt>
          <dd className="ml-4 text-base text-espresso/80">
            The "Allowed formats" list excludes the file type you uploaded.
            Add <code>jpg, png, webp</code> to the preset's allowlist.
          </dd>
        </dl>
      </Callout>

      <BottomLinks current="cloudinary" />
    </main>
  );
}
