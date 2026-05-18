import type { Config } from "tailwindcss";

// Palette ported from app/src/main/java/com/snapcabin/ui/theme/Color.kt
const config: Config = {
  content: ["./app/**/*.{ts,tsx}", "./components/**/*.{ts,tsx}"],
  theme: {
    extend: {
      colors: {
        parchment: "#FAF5EA",
        cream: "#FDFAF1",
        oat: "#EEE5CF",
        pine: "#6B8F73",
        "pine-deep": "#52755A",
        sage: "#B5C6AD",
        walnut: "#8B7558",
        "walnut-deep": "#6B5840",
        honey: "#C9A86A",
        "honey-deep": "#A8804A",
        clay: "#C4866A",
        espresso: "#322619",
        mist: "#B5A892",
      },
      fontFamily: {
        display: ['"Frank Ruhl Libre"', "Georgia", "serif"],
        sans: ['"Hanken Grotesk"', "system-ui", "sans-serif"],
      },
    },
  },
  plugins: [],
};

export default config;
