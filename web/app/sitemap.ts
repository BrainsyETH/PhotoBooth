import type { MetadataRoute } from "next";
import { SITE_URL } from "@/lib/links";

export default function sitemap(): MetadataRoute.Sitemap {
  const routes = [
    "",
    "/setup",
    "/setup/resend",
    "/setup/cloudinary",
    "/setup/kiosk",
    "/privacy",
    "/terms",
  ];
  const lastModified = new Date();
  return routes.map((path) => ({
    url: `${SITE_URL}${path}`,
    lastModified,
    changeFrequency: "monthly",
    priority: path === "" ? 1 : 0.6,
  }));
}
