import type { Metadata, Viewport } from "next";
import { Roboto } from "next/font/google";
import { NextIntlClientProvider } from "next-intl";
import { getLocale, getMessages, getTranslations } from "next-intl/server";
import { ThemeProvider } from "next-themes";
import { AuthProvider } from "@/lib/auth";
import { PlayerProvider } from "@/components/player";
import { routing } from "@/i18n/routing";
import "../globals.css";

const roboto = Roboto({
  subsets: ["latin"],
  weight: ["400", "500", "600", "700"],
  style: ["normal", "italic"],
  variable: "--font-roboto",
  display: "swap",
});

export async function generateMetadata(): Promise<Metadata> {
  const t = await getTranslations("meta");
  return {
    title: t("title"),
    description: t("description"),
    applicationName: "Fila",
    manifest: "/manifest.webmanifest",
    icons: { icon: "/icon.svg" },
    appleWebApp: {
      capable: true,
      title: "Fila",
      statusBarStyle: "black-translucent",
    },
  };
}

export const viewport: Viewport = {
  width: "device-width",
  initialScale: 1,
  viewportFit: "cover",
  themeColor: [
    { media: "(prefers-color-scheme: light)", color: "#f3eee6" },
    { media: "(prefers-color-scheme: dark)", color: "#100e0c" },
  ],
};

export function generateStaticParams() {
  return routing.locales.map((locale) => ({ locale }));
}

export default async function LocaleLayout({ children }: { children: React.ReactNode }) {
  const locale = await getLocale();
  const messages = await getMessages();

  return (
    <html lang={locale} suppressHydrationWarning className={`${roboto.variable} h-full`}>
      <body className="min-h-dvh font-sans antialiased">
        <NextIntlClientProvider locale={locale} messages={messages}>
          <ThemeProvider attribute="class" defaultTheme="dark" enableSystem disableTransitionOnChange>
            <AuthProvider>
              <PlayerProvider>
                <div className="mx-auto min-h-dvh w-full max-w-lg">{children}</div>
              </PlayerProvider>
            </AuthProvider>
          </ThemeProvider>
        </NextIntlClientProvider>
      </body>
    </html>
  );
}
