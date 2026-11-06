"use client";

import { useTranslations } from "next-intl";
import { useEffect } from "react";
import { Link, usePathname, useRouter } from "@/i18n/navigation";
import { useAuth } from "@/lib/auth";
import { navOffset, usePlayer } from "./player";
import { cx } from "./ui";

const items = [
  { href: "/queue", key: "queue", icon: QueueIcon },
  { href: "/search", key: "search", icon: SearchIcon },
  { href: "/library", key: "library", icon: LibraryIcon },
  { href: "/settings", key: "settings", icon: SettingsIcon },
] as const;

export function AppFrame({ children }: { children: React.ReactNode }) {
  const t = useTranslations("nav");
  const pathname = usePathname();
  const { track } = usePlayer();

  return (
    <div className={navOffset(track !== null)}>
      {children}
      <nav className="fixed inset-x-0 bottom-0 z-30 mx-auto flex w-full max-w-lg justify-around border-t border-line bg-bg/90 px-2 pt-2 pb-[calc(0.4rem+env(safe-area-inset-bottom))] backdrop-blur">
        {items.map((item) => {
          const active =
            pathname === item.href || (item.href === "/library" && pathname.startsWith("/podcast"));
          const Icon = item.icon;
          return (
            <Link
              key={item.href}
              href={item.href}
              aria-current={active ? "page" : undefined}
              className={cx(
                "flex min-h-11 min-w-16 flex-col items-center gap-1 text-[11px]",
                active ? "text-accent" : "text-muted",
              )}
            >
              <Icon />
              {t(item.key)}
            </Link>
          );
        })}
      </nav>
    </div>
  );
}

export function RequireAuth({ children }: { children: React.ReactNode }) {
  const { session, ready } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (ready && !session) {
      router.replace("/login");
    }
  }, [ready, session, router]);

  if (!ready || !session) {
    return (
      <div className="grid min-h-dvh place-items-center">
        <span className="font-serif text-4xl italic">fila</span>
      </div>
    );
  }

  return <AppFrame>{children}</AppFrame>;
}

function QueueIcon() {
  return (
    <svg viewBox="0 0 24 24" className="h-5 w-5" aria-hidden>
      <path d="M4 7h16M4 12h12M4 17h8" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" />
    </svg>
  );
}

function SearchIcon() {
  return (
    <svg viewBox="0 0 24 24" className="h-5 w-5" aria-hidden>
      <circle cx="11" cy="11" r="6" fill="none" stroke="currentColor" strokeWidth="1.8" />
      <path d="M16 16l4 4" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" />
    </svg>
  );
}

function LibraryIcon() {
  return (
    <svg viewBox="0 0 24 24" className="h-5 w-5" aria-hidden>
      <path d="M6 5h4v14H6zM14 8h4v11h-4z" fill="none" stroke="currentColor" strokeWidth="1.8" />
    </svg>
  );
}

function SettingsIcon() {
  return (
    <svg viewBox="0 0 24 24" className="h-5 w-5" aria-hidden>
      <circle cx="12" cy="12" r="3" fill="none" stroke="currentColor" strokeWidth="1.8" />
      <path
        d="M12 4v2.5M12 17.5V20M4 12h2.5M17.5 12H20M6.4 6.4l1.8 1.8M15.8 15.8l1.8 1.8M17.6 6.4l-1.8 1.8M8.2 15.8l-1.8 1.8"
        stroke="currentColor"
        strokeWidth="1.6"
        strokeLinecap="round"
      />
    </svg>
  );
}
