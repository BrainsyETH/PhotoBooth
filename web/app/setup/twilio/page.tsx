import Link from "next/link";
import { BottomLinks, Callout, Step, Warning } from "../shared";

export const metadata = {
  title: "SnapCabin · Twilio setup",
  description:
    "Step-by-step instructions for wiring a Twilio account to a SnapCabin kiosk for SMS and MMS photo delivery.",
};

export default function TwilioSetup() {
  return (
    <main className="mx-auto max-w-3xl px-6 py-16">
      <Link
        href="/setup"
        className="mb-10 inline-block text-sm font-sans font-semibold uppercase tracking-widest text-walnut no-underline hover:text-walnut-deep"
      >
        ← All setup guides
      </Link>

      <p className="text-xs font-sans font-semibold uppercase tracking-[0.3em] text-honey-deep">
        Integration · Twilio
      </p>
      <h1 className="mt-2 font-display text-5xl font-bold text-espresso">
        Send the photo by SMS / MMS
      </h1>
      <p className="mt-4 max-w-xl text-lg leading-relaxed text-espresso/80">
        Twilio is what delivers the photo to a guest's phone over cellular. You
        bring the account; SnapCabin holds the credentials locally on the
        kiosk and never sends them anywhere else.
      </p>

      <Callout title="Before you start">
        <ul className="ml-5 list-disc space-y-1">
          <li>
            A credit card to upgrade past Twilio's trial (trial accounts can
            only send to verified numbers — fine for testing, painful at an
            event).
          </li>
          <li>About 10 minutes.</li>
        </ul>
      </Callout>

      <Step n="1" title="Create a Twilio account">
        <p>
          Sign up at{" "}
          <a
            href="https://www.twilio.com/try-twilio"
            target="_blank"
            rel="noopener noreferrer"
          >
            twilio.com/try-twilio
          </a>
          . Verify your email and phone number.
        </p>
      </Step>

      <Step n="2" title="Buy a phone number with SMS + MMS">
        <p>
          From the Twilio Console, open{" "}
          <strong>Phone Numbers → Manage → Buy a number</strong>. Filter for{" "}
          <strong>SMS</strong> capability — and{" "}
          <strong>MMS</strong> if you want to deliver the photo as an inline
          image rather than a link. US/CA local numbers are typically $1.15/mo.
        </p>
        <p className="text-sm text-mist">
          Toll-free numbers also work but require carrier verification, which
          takes 1–3 weeks. For a one-off event, a local number is fastest.
        </p>
      </Step>

      <Step n="3" title="Copy your Account SID + Auth Token">
        <p>
          Go to the Console homepage. The{" "}
          <strong>Account SID</strong> (starts with <code>AC</code>) and{" "}
          <strong>Auth Token</strong> are in the "Account Info" card. Reveal
          the token and copy it.
        </p>
        <Warning>
          The Auth Token is a master credential. Treat it like a password.
          SnapCabin stores it in the kiosk's private app sandbox — never type
          it on a shared computer.
        </Warning>
      </Step>

      <Step n="4" title="Enter the credentials on the kiosk">
        <p>
          On the kiosk:
        </p>
        <ol className="ml-5 list-decimal space-y-1">
          <li>Long-press the bottom-right corner of the Attract screen.</li>
          <li>Enter the admin PIN (default <code>1234</code> — change it).</li>
          <li>
            Open <strong>TWILIO SMS</strong>, toggle "Enable Twilio SMS
            sending".
          </li>
          <li>Paste the Account SID, Auth Token, and From number (E.164, e.g. <code>+15551234567</code>).</li>
        </ol>
      </Step>

      <Step n="5" title="(Optional) Wire up MMS via Cloudinary">
        <p>
          For Twilio to attach an image rather than a link, the kiosk needs a
          public URL for the photo. The easiest path is{" "}
          <Link href="/setup/cloudinary">Cloudinary</Link> — enable it under
          the kiosk's Cloudinary section and SnapCabin will upload each photo
          before handing the URL to Twilio.
        </p>
      </Step>

      <Step n="6" title="Test from your own phone">
        <p>
          End the current event (or start a fresh test event), capture a
          photo, hit the SMS button, and enter your phone number. If something
          fails the kiosk surfaces the Twilio error inline and logs it in the
          Audit Log section.
        </p>
      </Step>

      <Callout title="Common errors">
        <dl className="space-y-3">
          <dt className="font-sans font-semibold text-espresso">
            <code>401 Unauthorized</code>
          </dt>
          <dd className="ml-4 text-base text-espresso/80">
            Account SID or Auth Token is wrong. Double-check by re-revealing
            the token in the Twilio Console.
          </dd>
          <dt className="font-sans font-semibold text-espresso">
            <code>21408 Permission to send an SMS has not been enabled</code>
          </dt>
          <dd className="ml-4 text-base text-espresso/80">
            Your Twilio number can't send to that country. Open{" "}
            <strong>Messaging → Settings → Geo permissions</strong> and enable
            the destination.
          </dd>
          <dt className="font-sans font-semibold text-espresso">
            <code>21610 Unsubscribed recipient</code>
          </dt>
          <dd className="ml-4 text-base text-espresso/80">
            The guest's number has previously replied <code>STOP</code> to
            your Twilio number. Have them text <code>START</code> to re-opt in.
          </dd>
        </dl>
      </Callout>

      <BottomLinks current="twilio" />
    </main>
  );
}

