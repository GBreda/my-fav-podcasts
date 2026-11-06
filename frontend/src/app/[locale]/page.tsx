"use client";

import { useTranslations } from "next-intl";
import { useEffect } from "react";
import { Link, useRouter } from "@/i18n/navigation";
import { useAuth } from "@/lib/auth";
import { Logo } from "@/components/ui";

export default function LandingPage() {
  const t = useTranslations("landing");
  const { session, ready } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (ready && session) {
      router.replace("/queue");
    }
  }, [ready, session, router]);

  const rows = [
    { show: t("show1"), title: t("title1"), meta: t("meta1") },
    { show: t("show2"), title: t("title2"), meta: t("meta2") },
    { show: t("show3"), title: t("title3"), meta: t("meta3") },
  ];

  return (
    <main className="px-5 py-8">
      <Logo />
      <p className="mt-10 text-xs font-semibold tracking-[0.18em] text-muted uppercase">{t("kicker")}</p>
      <h1 className="mt-3 max-w-[12ch] font-serif text-5xl leading-[0.95] tracking-tight">{t("title")}</h1>
      <p className="mt-4 max-w-sm text-base leading-7 text-muted">{t("subtitle")}</p>
      <div className="mt-8 flex flex-col gap-3 sm:flex-row">
        <Link href="/register" className="inline-flex h-12 items-center justify-center rounded-full bg-accent px-5 text-sm font-semibold text-accent-ink">
          {t("register")}
        </Link>
        <Link href="/login" className="inline-flex h-12 items-center justify-center rounded-full border border-line bg-surface px-5 text-sm font-semibold">
          {t("login")}
        </Link>
      </div>

      <section className="mt-10 rounded-[1.75rem] border border-line bg-surface p-4" aria-label={t("preview")}>
        <p className="px-2 pb-2 text-xs tracking-[0.16em] text-muted uppercase">{t("preview")}</p>
        <ol>
          {rows.map((row) => (
            <li key={row.title} className="flex items-center gap-3 border-t border-line px-2 py-3 first:border-t-0">
              <span className="grid h-12 w-12 shrink-0 place-items-center rounded-xl bg-accent/15 font-serif text-accent">
                {row.show.slice(0, 1)}
              </span>
              <span className="min-w-0 flex-1">
                <span className="block truncate text-xs text-muted">{row.show}</span>
                <span className="block truncate font-serif text-lg leading-6">{row.title}</span>
                <span className="block text-xs text-muted">{row.meta}</span>
              </span>
              <span className="grid h-10 w-10 place-items-center rounded-full bg-ink text-bg" aria-hidden>
                ▶
              </span>
            </li>
          ))}
        </ol>
      </section>

      <ul className="mt-8 space-y-5">
        {[
          [t("point1Title"), t("point1Body")],
          [t("point2Title"), t("point2Body")],
          [t("point3Title"), t("point3Body")],
        ].map(([title, body]) => (
          <li key={title}>
            <h2 className="font-serif text-2xl">{title}</h2>
            <p className="mt-1 text-sm leading-6 text-muted">{body}</p>
          </li>
        ))}
      </ul>
      <div className="h-8" />
    </main>
  );
}
