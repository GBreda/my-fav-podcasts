"use client";

import { useTranslations } from "next-intl";
import type { ButtonHTMLAttributes, InputHTMLAttributes, ReactNode } from "react";

export function cx(...parts: Array<string | false | null | undefined>) {
  return parts.filter(Boolean).join(" ");
}

const ERROR_CODES = new Set([
  "EMAIL_TAKEN",
  "INVALID_CREDENTIALS",
  "VALIDATION",
  "ALREADY_FOLLOWING",
  "FEED_UNREADABLE",
  "FEED_URL_BLOCKED",
  "DIRECTORY_UNAVAILABLE",
  "NOT_FOLLOWING",
  "PODCAST_NOT_FOUND",
  "EPISODE_NOT_FOUND",
  "RESET_TOKEN_INVALID",
  "UNKNOWN",
]);

export function errorText(code: string | null, t: (key: string) => string) {
  if (!code) {
    return null;
  }
  return t(ERROR_CODES.has(code) ? code : "UNKNOWN");
}

export function Logo({ compact = false }: { compact?: boolean }) {
  return (
    <span className="inline-flex items-center gap-2">
      <svg viewBox="0 0 32 32" className="h-8 w-8" aria-hidden>
        <rect width="32" height="32" rx="9" className="fill-ink" />
        <rect x="7" y="9" width="4" height="14" rx="1" className="fill-accent" />
        <rect x="14" y="13" width="4" height="10" rx="1" className="fill-surface" />
        <rect x="21" y="7" width="4" height="16" rx="1" className="fill-accent" />
      </svg>
      {compact ? null : <span className="font-serif text-2xl italic tracking-tight">fila</span>}
    </span>
  );
}

export function Artwork({
  src,
  title,
  className,
}: {
  src?: string | null;
  title: string;
  className?: string;
}) {
  if (!src) {
    return (
      <div className={cx("grid place-items-center bg-accent/15 font-serif text-lg text-accent", className)}>
        {title.slice(0, 1).toUpperCase()}
      </div>
    );
  }
  return <img src={src} alt="" className={cx("object-cover", className)} />;
}

export function Button({
  variant = "primary",
  className,
  ...props
}: ButtonHTMLAttributes<HTMLButtonElement> & { variant?: "primary" | "ghost" | "quiet" }) {
  return (
    <button
      className={cx(
        "inline-flex h-12 items-center justify-center rounded-full px-5 text-sm font-semibold transition active:scale-[0.98] disabled:opacity-50",
        variant === "primary" && "bg-accent text-accent-ink",
        variant === "ghost" && "border border-line bg-surface text-ink",
        variant === "quiet" && "h-10 bg-transparent px-3 text-muted",
        className,
      )}
      {...props}
    />
  );
}

export function TextField({
  label,
  error,
  ...props
}: InputHTMLAttributes<HTMLInputElement> & { label: string; error?: string | null }) {
  return (
    <label className="block">
      <span className="mb-2 block text-sm text-muted">{label}</span>
      <input
        className="h-12 w-full rounded-2xl border border-line bg-surface px-4 text-base outline-none placeholder:text-muted/70"
        {...props}
      />
      {error ? (
        <span className="mt-2 block text-sm text-accent" role="alert">
          {error}
        </span>
      ) : null}
    </label>
  );
}

export function Alert({ code }: { code: string | null }) {
  const t = useTranslations("errors");
  const message = errorText(code, t);
  if (!message) {
    return null;
  }
  return (
    <p role="alert" className="rounded-2xl bg-accent/10 px-4 py-3 text-sm text-accent">
      {message}
    </p>
  );
}

export function EmptyState({
  title,
  body,
  action,
}: {
  title: string;
  body: string;
  action?: ReactNode;
}) {
  return (
    <div className="mt-16 px-2 text-center">
      <h2 className="font-serif text-3xl tracking-tight">{title}</h2>
      <p className="mx-auto mt-3 max-w-xs text-sm leading-6 text-muted">{body}</p>
      {action ? <div className="mt-6">{action}</div> : null}
    </div>
  );
}

export function AuthCard({
  title,
  children,
  footer,
}: {
  title: string;
  children: ReactNode;
  footer: ReactNode;
}) {
  return (
    <main className="flex min-h-dvh flex-col justify-between px-5 py-8">
      <div>
        <Logo />
        <h1 className="mt-10 max-w-[12ch] font-serif text-5xl leading-[0.95] tracking-tight">{title}</h1>
        <div className="mt-8 space-y-4">{children}</div>
      </div>
      <p className="pt-10 text-sm text-muted">{footer}</p>
    </main>
  );
}
