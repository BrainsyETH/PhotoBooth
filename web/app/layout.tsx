import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "SnapCabin",
  description:
    "A photo booth that runs on an Android tablet you already own. No subscription, no monthly fee.",
  metadataBase: new URL("https://snapcabin.app"),
  openGraph: {
    title: "SnapCabin",
    description:
      "A photo booth that runs on an Android tablet you already own.",
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
