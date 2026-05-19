import Link from "next/link";
import { BottomLinks, Callout, Step, Warning } from "../shared";

export const metadata = {
  title: "SnapCabin · Twilio setup",
  description:
    "Step-by-step instructions for wiring a Twilio account to a SnapCabin tablet so guests can receive their photo by text.",
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
        Send the photo by text
      </h1>
      <p className="mt-4 max-w-xl text-lg leading-relaxed text-espresso/80">
        Twilio is the service that delivers the photo to a guest&rsquo;s
        phone over cellular. You bring the account. SnapCabin keeps the
        credentials on the tablet only and uses them to call Twilio&rsquo;s
        API when a guest taps the text-message button.
      </p>

      <Callout title="Before you start">
        <ul className="ml-5 list-disc space-y-1">
          <li>
            A credit card so you can move past Twilio&rsquo;s trial. (Trial
            accounts can only send to numbers you have verified, which is
            fine for testing and frustrating during a live event.)
          </li>
          <li>About ten minutes.</li>
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
          . Verify your email and phone number when asked.
        </p>
      </Step>

      <Step n="2" title="Buy a phone number with SMS and MMS">
        <p>
          From the Twilio Console, open{" "}
          <strong>Phone Numbers, then Manage, then Buy a number</strong>.
          Filter for <strong>SMS</strong> capability. Also check{" "}
          <strong>MMS</strong> if you want the text to arrive with the
          photo attached rather than a link. US and Canadian local numbers
          are typically about a dollar a month.
        </p>
        <p className="text-sm text-mist">
          Toll-free numbers work too, but they require carrier verification
          that can take one to three weeks. For a one-off event, a local
          number is the fastest path.
        </p>
      </Step>

      <Step n="3" title="Copy your Account SID and Auth Token">
        <p>
          On the Console home page, find the &ldquo;Account Info&rdquo;
          card. The <strong>Account SID</strong> starts with{" "}
          <code>AC</code>. The <strong>Auth Token</strong> is hidden until
          you click to reveal it. Copy both.
        </p>
        <Warning>
          The Auth Token is a master credential. Treat it like a password.
          SnapCabin stores it in the tablet&rsquo;s private app data and
          never sends it anywhere besides Twilio. Avoid typing it on a
          shared computer.
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
            Open <strong>TWILIO SMS</strong> and turn on &ldquo;Enable
            Twilio SMS sending&rdquo;.
          </li>
          <li>
            Paste the Account SID, the Auth Token, and the From number in
            E.164 format, for example <code>+15551234567</code>.
          </li>
        </ol>
      </Step>

      <Step n="5" title="Optional: send the photo as an attachment using Cloudinary">
        <p>
          For Twilio to attach the actual image instead of a link, the
          tablet needs a public URL for the photo. The easiest path is{" "}
          <Link href="/setup/cloudinary">Cloudinary</Link>. Turn it on
          under the Cloudinary section in the admin screen and SnapCabin
          will upload each photo and hand the URL to Twilio.
        </p>
      </Step>

      <Step n="6" title="Test by sending to your own phone">
        <p>
          End the current event (or start a fresh test event), capture a
          photo, tap the text-message button, and enter your own phone
          number. If something goes wrong, the tablet shows the Twilio
          error on screen and writes it to the Audit Log section.
        </p>
      </Step>

      <Callout title="Errors you might see">
        <dl className="space-y-3">
          <dt className="font-sans font-semibold text-espresso">
            <code>401 Unauthorized</code>
          </dt>
          <dd className="ml-4 text-base text-espresso/80">
            The Account SID or Auth Token is wrong. Reveal the token again
            in the Twilio Console and copy it carefully.
          </dd>
          <dt className="font-sans font-semibold text-espresso">
            <code>21408 Permission to send an SMS has not been enabled</code>
          </dt>
          <dd className="ml-4 text-base text-espresso/80">
            Your Twilio number is not allowed to send to that country.
            Open <strong>Messaging, then Settings, then Geo permissions</strong>{" "}
            and enable the destination.
          </dd>
          <dt className="font-sans font-semibold text-espresso">
            <code>21610 Unsubscribed recipient</code>
          </dt>
          <dd className="ml-4 text-base text-espresso/80">
            The guest&rsquo;s number once replied <code>STOP</code> to
            your Twilio number. Ask them to text <code>START</code> to opt
            back in.
          </dd>
        </dl>
      </Callout>

      <BottomLinks current="twilio" />
    </main>
  );
}
