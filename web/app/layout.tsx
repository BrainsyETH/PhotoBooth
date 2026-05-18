import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "SnapCabin — a photo booth in the woods",
  description:
    "SnapCabin is a self-hosted Android photo-booth kiosk. Sage-and-cream aesthetic. No analytics, no telemetry.",
  metadataBase: new URL("https://snapcabin.app"),
  openGraph: {
    title: "SnapCabin",
    description: "A photo booth in the woods.",
    url: "https://snapcabin.app",
    siteName: "SnapCabin",
    type: "website",
  },
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en">
      <body>{children}</body>
    </html>
  );
}
