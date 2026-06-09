import Link from "next/link";
import { BottomLinks, Callout, Step, Warning } from "../shared";

export const metadata = {
  title: "SnapCabin · Resend setup",
  description:
    "Step-by-step instructions for wiring a Resend account to a SnapCabin tablet so guests can receive their photo by email.",
};

export default function ResendSetup() {
  return (
    <main className="mx-auto max-w-3xl px-6 py-16">
      <Link
        href="/setup"
        className="mb-10 inline-block text-sm font-sans font-semibold uppercase tracking-widest text-walnut no-underline hover:text-walnut-deep"
      >
        ← All setup guides
      </Link>

      <p className="text-xs font-sans font-semibold uppercase tracking-[0.3em] text-honey-deep">
        Integration · Resend
      </p>
      <h1 className="mt-2 font-display text-5xl font-bold text-espresso">
        Send the photo by email
      </h1>
      <p className="mt-4 max-w-xl text-lg leading-relaxed text-espresso/80">
        Resend is the service that emails the photo to a guest. You bring the
        account. The tablet keeps the API key on-device and calls
        Resend&rsquo;s HTTP API when a guest taps the email button. The photo
        rides as a JPEG attachment, so the email is self-contained.
      </p>

      <Callout title="Before you start">
        <ul className="ml-5 list-disc space-y-1">
          <li>
            A free Resend account. The free tier covers 3,000 emails per
            month and 100 per day &mdash; plenty for a typical event.
          </li>
          <li>
            A domain you can edit DNS records for. (For testing only, Resend
            lets you send from <code>onboarding@resend.dev</code>.)
          </li>
          <li>About ten minutes.</li>
        </ul>
      </Callout>

      <Step n="1" title="Create a Resend account">
        <p>
          Sign up at{" "}
          <a
            href="https://resend.com/signup"
            target="_blank"
            rel="noopener noreferrer"
          >
            resend.com
          </a>
          . Verify your email when asked.
        </p>
      </Step>

      <Step n="2" title="Verify a sending domain">
        <p>
          In the Resend dashboard, open <strong>Domains, then Add Domain</strong>.
          Enter a domain you own (for example, <code>snapcabin.example.com</code>).
          Resend will list a handful of DNS records (SPF, DKIM, sometimes DMARC).
          Add them at your DNS provider and wait for Resend to mark the
          domain as <strong>Verified</strong> &mdash; this usually takes a
          few minutes.
        </p>
        <p className="text-sm text-mist">
          If you&rsquo;re just trying things out, skip this step and use{" "}
          <code>onboarding@resend.dev</code> as the From address. Emails will
          send but will arrive from a generic Resend address.
        </p>
      </Step>

      <Step n="3" title="Create an API key">
        <p>
          Open <strong>API Keys, then Create API Key</strong>. Give it a name
          like <code>snapcabin-kiosk</code> and set permission to{" "}
          <strong>Sending access</strong> (so it can send mail but not change
          account settings). Copy the key &mdash; it starts with{" "}
          <code>re_</code> and is shown only once.
        </p>
        <Warning>
          The API key lets anyone with it send email as your account. Treat
          it like a password. SnapCabin stores it in the tablet&rsquo;s
          private app data and never sends it anywhere besides Resend. If
          the key leaks, revoke it in the Resend dashboard and create a new
          one.
        </Warning>
      </Step>

      <Step n="4" title="Enter the credentials on the tablet">
        <p>On the tablet:</p>
        <ol className="ml-5 list-decimal space-y-1">
          <li>Long-press the bottom-right corner of the Attract screen.</li>
          <li>
            Enter the admin PIN. (Default is <code>1234</code>. Change it
            before your event.)
          </li>
          <li>
            Open <strong>EMAIL (RESEND)</strong> and turn on &ldquo;Enable
            email delivery via Resend&rdquo;.
          </li>
          <li>
            Paste the API key and a From address. The From address should
            look like{" "}
            <code>SnapCabin &lt;booth@yourdomain.com&gt;</code> using a
            domain you verified in step 2.
          </li>
          <li>
            Optionally adjust the subject line, the per-session cap, and the
            per-address cap.
          </li>
        </ol>
      </Step>

      <Step n="5" title="Test by sending to your own inbox">
        <p>
          End the current event (or start a fresh test event), capture a
          photo, tap <strong>EMAIL</strong>, and enter your own email
          address. The photo should arrive within a few seconds. If
          something goes wrong, the tablet shows the Resend error on screen
          and writes it to the Audit Log section.
        </p>
      </Step>

      <Callout title="Errors you might see">
        <dl className="space-y-3">
          <dt className="font-sans font-semibold text-espresso">
            <code>Resend rejected the API key</code>
          </dt>
          <dd className="ml-4 text-base text-espresso/80">
            The key is wrong or has been revoked. Create a new one in the
            Resend dashboard and paste it again.
          </dd>
          <dt className="font-sans font-semibold text-espresso">
            <code>The domain is not verified</code>
          </dt>
          <dd className="ml-4 text-base text-espresso/80">
            The From address uses a domain that isn&rsquo;t verified in
            Resend. Either complete domain verification in step 2 or switch
            the From address to <code>onboarding@resend.dev</code>.
          </dd>
          <dt className="font-sans font-semibold text-espresso">
            <code>Resend rate limit hit</code>
          </dt>
          <dd className="ml-4 text-base text-espresso/80">
            The free tier allows 100 emails per day. Upgrade the plan in
            Resend or wait until tomorrow.
          </dd>
        </dl>
      </Callout>

      <BottomLinks current="resend" />
    </main>
  );
}
