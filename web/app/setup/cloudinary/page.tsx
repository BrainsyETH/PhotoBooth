import Link from "next/link";
import { BottomLinks, Callout, Step, Warning } from "../shared";

export const metadata = {
  title: "SnapCabin · Cloudinary setup",
  description:
    "Step-by-step instructions for wiring Cloudinary to a SnapCabin tablet so each photo has a public link.",
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
        Host your event photos online
      </h1>
      <p className="mt-4 max-w-xl text-lg leading-relaxed text-espresso/80">
        Cloudinary is where the tablet uploads each photo so the text
        message can deliver the actual image and not just a link. You own
        the account. The tablet uses an unsigned upload preset, which
        means your API secret never lives on the device.
      </p>

      <Callout title="Before you start">
        <ul className="ml-5 list-disc space-y-1">
          <li>
            A free Cloudinary account covers most events. The free tier
            includes about 25 GB of storage and 25 GB of bandwidth a month.
          </li>
          <li>About five minutes.</li>
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
          From the dashboard at <strong>console.cloudinary.com</strong>,
          look at the &ldquo;Account Details&rdquo; card. The{" "}
          <strong>Cloud name</strong> is usually your account handle. Copy
          it.
        </p>
      </Step>

      <Step n="3" title="Create an unsigned upload preset">
        <p>
          Open <strong>Settings, then Upload, then Upload presets</strong>{" "}
          (or visit{" "}
          <a
            href="https://console.cloudinary.com/settings/upload"
            target="_blank"
            rel="noopener noreferrer"
          >
            console.cloudinary.com/settings/upload
          </a>
          ).
        </p>
        <p>
          Click <strong>Add upload preset</strong> and configure:
        </p>
        <ul className="ml-5 list-disc space-y-1">
          <li>
            <strong>Preset name</strong>: <code>snapcabin_events</code> (or
            any name you like)
          </li>
          <li>
            <strong>Signing Mode</strong>: <strong>Unsigned</strong>
          </li>
          <li>
            <strong>Folder</strong>: leave blank. The tablet sets a folder
            per event.
          </li>
          <li>
            <strong>Allowed formats</strong>: <code>jpg, png, webp</code>
          </li>
          <li>
            <strong>Max file size</strong>: 10000000 (10 MB) so a stranger
            with the preset name can&rsquo;t upload anything huge.
          </li>
        </ul>
        <p>Save the preset and copy the exact name you chose.</p>
        <Warning>
          Unsigned presets let anyone who knows the name upload. Always
          restrict by format and size. If a preset name leaks, you can
          delete it from the same page and create a new one in seconds.
        </Warning>
      </Step>

      <Step n="4" title="Enter the credentials on the tablet">
        <p>On the tablet:</p>
        <ol className="ml-5 list-decimal space-y-1">
          <li>Long-press the bottom-right corner of the Attract screen.</li>
          <li>Enter the admin PIN.</li>
          <li>
            Open <strong>CLOUDINARY PHOTO HOSTING</strong> and turn on
            &ldquo;Upload photos to Cloudinary before SMS&rdquo;.
          </li>
          <li>
            Paste the <strong>Cloud name</strong> and the{" "}
            <strong>upload preset name</strong>. Both are
            case-sensitive.
          </li>
        </ol>
      </Step>

      <Step n="5" title="Test an upload">
        <p>
          Start a test event from the EVENT section, capture a photo, then
          tap the text-message or QR button. The tablet uploads to{" "}
          <code>events/&lt;your-event-name&gt;/</code> in your Cloudinary
          account. Open{" "}
          <a
            href="https://console.cloudinary.com/console/media_library"
            target="_blank"
            rel="noopener noreferrer"
          >
            the Media Library
          </a>{" "}
          to confirm the photo arrived.
        </p>
      </Step>

      <Callout title="Errors you might see">
        <dl className="space-y-3">
          <dt className="font-sans font-semibold text-espresso">
            <code>Upload preset not found</code>
          </dt>
          <dd className="ml-4 text-base text-espresso/80">
            The preset name on the tablet doesn&rsquo;t match the name in
            Cloudinary. Compare them carefully. They are case sensitive.
          </dd>
          <dt className="font-sans font-semibold text-espresso">
            <code>Unsigned upload disabled</code>
          </dt>
          <dd className="ml-4 text-base text-espresso/80">
            The preset is set to Signing Mode = Signed. Edit it and switch
            to Unsigned.
          </dd>
          <dt className="font-sans font-semibold text-espresso">
            <code>Resource type not allowed</code>
          </dt>
          <dd className="ml-4 text-base text-espresso/80">
            The file type isn&rsquo;t in the preset&rsquo;s allowed-formats
            list. Add <code>jpg, png, webp</code> and try again.
          </dd>
        </dl>
      </Callout>

      <BottomLinks current="cloudinary" />
    </main>
  );
}
