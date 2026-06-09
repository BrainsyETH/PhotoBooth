import { ImageResponse } from "next/og";

// Generated (not a product screenshot): branded text card on a parchment
// radial gradient using the SnapCabin palette.
export const runtime = "edge";
export const alt = "SnapCabin — $2.99 photo booth app for Android tablets";
export const size = { width: 1200, height: 630 };
export const contentType = "image/png";

export default function OpengraphImage() {
  return new ImageResponse(
    (
      <div
        style={{
          width: "100%",
          height: "100%",
          display: "flex",
          flexDirection: "column",
          alignItems: "center",
          justifyContent: "center",
          background:
            "radial-gradient(ellipse at top, #FDFAF1 0%, #FAF5EA 55%, #EEE5CF 100%)",
          color: "#322619",
          fontFamily: "Georgia, serif",
          padding: 80,
        }}
      >
        <div
          style={{
            fontSize: 34,
            letterSpacing: 8,
            textTransform: "uppercase",
            color: "#A8804A",
            marginBottom: 24,
          }}
        >
          Photo booth, on your tablet
        </div>
        <div style={{ fontSize: 120, fontWeight: 700, lineHeight: 1 }}>
          SnapCabin
        </div>
        <div
          style={{
            fontSize: 40,
            color: "#322619",
            opacity: 0.8,
            marginTop: 28,
            textAlign: "center",
            maxWidth: 900,
          }}
        >
          The photo booth that lives on your tablet.
        </div>
        <div
          style={{
            display: "flex",
            marginTop: 48,
            background: "#6B8F73",
            color: "#FAF5EA",
            fontSize: 36,
            fontWeight: 600,
            padding: "20px 40px",
            borderRadius: 24,
          }}
        >
          $2.99 on Google Play
        </div>
      </div>
    ),
    { ...size },
  );
}
