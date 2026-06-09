import type { Metadata } from "next";
import "./globals.css";
import { PLAY_STORE_URL, SITE_URL } from "@/lib/links";

export const metadata: Metadata = {
  title: {
    default: "SnapCabin — $2.99 Photo Booth App for Android Tablets",
    template: "%s · SnapCabin",
  },
  description:
    "A photo booth that runs on an Android tablet you already own. $2.99 once on Google Play. No accounts, no cloud, no subscription — your photos stay yours.",
  metadataBase: new URL(SITE_URL),
  alternates: {
    canonical: "/",
  },
  openGraph: {
    title: "SnapCabin — $2.99 Photo Booth App for Android Tablets",
    description:
      "A photo booth that lives on your tablet. $2.99 once on Google Play. No accounts, no cloud, no subscription.",
    url: SITE_URL,
    siteName: "SnapCabin",
    type: "website",
    images: [
      {
        url: "/opengraph-image",
        width: 1200,
        height: 630,
        alt: "SnapCabin — $2.99 photo booth app for Android tablets",
      },
    ],
  },
  twitter: {
    card: "summary_large_image",
    title: "SnapCabin — $2.99 Photo Booth App for Android Tablets",
    description:
      "A photo booth that lives on your tablet. $2.99 once on Google Play.",
    images: ["/opengraph-image"],
  },
};

const jsonLd = {
  "@context": "https://schema.org",
  "@type": "SoftwareApplication",
  name: "SnapCabin",
  description:
    "A self-hosted photo booth kiosk app for weddings and parties that runs on an Android tablet you already own.",
  applicationCategory: "MultimediaApplication",
  operatingSystem: "Android 8.0+",
  url: SITE_URL,
  downloadUrl: PLAY_STORE_URL,
  offers: {
    "@type": "Offer",
    price: "2.99",
    priceCurrency: "USD",
    availability: "https://schema.org/InStock",
    url: PLAY_STORE_URL,
  },
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en">
      <body>
        {children}
        <script
          type="application/ld+json"
          // eslint-disable-next-line react/no-danger
          dangerouslySetInnerHTML={{ __html: JSON.stringify(jsonLd) }}
        />
      </body>
    </html>
  );
}
