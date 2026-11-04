import type { Metadata, Viewport } from "next";
import { Fraunces, Outfit } from "next/font/google";
import { PlayerProvider } from "@/components/Player";
import "./globals.css";

const outfit = Outfit({
  subsets: ["latin"],
  weight: ["400", "500", "600"],
  variable: "--font-outfit",
  display: "swap",
});

const fraunces = Fraunces({
  subsets: ["latin"],
  style: ["normal", "italic"],
  variable: "--font-fraunces",
  display: "swap",
});

export const metadata: Metadata = {
  title: { default: "Fila", template: "%s · Fila" },
  description: "Uma fila só para os podcasts que você acompanha, em ordem de lançamento.",
  applicationName: "Fila",
  manifest: "/manifest.webmanifest",
  icons: { icon: "/icon.svg", apple: "/icon.svg" },
  appleWebApp: { capable: true, title: "Fila", statusBarStyle: "black-translucent" },
};

export const viewport: Viewport = {
  width: "device-width",
  initialScale: 1,
  viewportFit: "cover",
  themeColor: "#141210",
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="pt-BR">
      <body className={`${outfit.variable} ${fraunces.variable}`}>
        <PlayerProvider>{children}</PlayerProvider>
      </body>
    </html>
  );
}
