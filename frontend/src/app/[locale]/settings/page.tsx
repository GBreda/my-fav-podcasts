"use client";

import { useLocale, useTranslations } from "next-intl";
import { useTheme } from "next-themes";
import { useEffect, useState } from "react";
import { usePathname, useRouter } from "@/i18n/navigation";
import { ApiError } from "@/lib/api";
import { useAuth } from "@/lib/auth";
import { fieldErrors, passwordSchema } from "@/lib/schemas";
import { RequireAuth } from "@/components/shell";
import { Alert, Button, TextField, cx } from "@/components/ui";

export default function SettingsPage() {
  return (
    <RequireAuth>
      <SettingsScreen />
    </RequireAuth>
  );
}

function SettingsScreen() {
  const t = useTranslations("settings");
  const fields = useTranslations("fields");
  const locale = useLocale();
  const pathname = usePathname();
  const router = useRouter();
  const { theme, setTheme } = useTheme();
  const { session, api, signOut } = useAuth();
  const [currentPassword, setCurrentPassword] = useState("");
  const [nextPassword, setNextPassword] = useState("");
  const [errors, setErrors] = useState<Record<string, true>>({});
  const [code, setCode] = useState<string | null>(null);
  const [saved, setSaved] = useState(false);
  const [mounted, setMounted] = useState(false);

  useEffect(() => {
    setMounted(true);
  }, []);

  async function savePassword(event: React.FormEvent) {
    event.preventDefault();
    const parsed = passwordSchema.safeParse({ currentPassword, nextPassword });
    if (!parsed.success) {
      setErrors(fieldErrors(parsed.error));
      setSaved(false);
      return;
    }
    setErrors({});
    try {
      await api.changePassword(parsed.data);
      setCurrentPassword("");
      setNextPassword("");
      setSaved(true);
      setCode(null);
    } catch (error) {
      setSaved(false);
      setCode(error instanceof ApiError ? error.code : "UNKNOWN");
    }
  }

  return (
    <main className="px-5 pt-6">
      <h1 className="font-serif text-4xl tracking-tight">{t("title")}</h1>

      <section className="mt-8">
        <h2 className="text-sm text-muted">{t("theme")}</h2>
        <div className="mt-3 grid grid-cols-3 gap-2">
          {(["dark", "light", "system"] as const).map((value) => (
            <button
              key={value}
              type="button"
              onClick={() => setTheme(value)}
              className={cx(
                "h-11 rounded-full border text-sm",
                mounted && theme === value ? "border-accent bg-accent text-accent-ink" : "border-line",
              )}
            >
              {t(value)}
            </button>
          ))}
        </div>
      </section>

      <section className="mt-8">
        <h2 className="text-sm text-muted">{t("language")}</h2>
        <div className="mt-3 grid grid-cols-2 gap-2">
          <button
            type="button"
            onClick={() => router.replace(pathname, { locale: "pt-BR" })}
            className={cx("h-11 rounded-full border text-sm", locale === "pt-BR" ? "border-accent bg-accent text-accent-ink" : "border-line")}
          >
            {t("portuguese")}
          </button>
          <button
            type="button"
            onClick={() => router.replace(pathname, { locale: "en-US" })}
            className={cx("h-11 rounded-full border text-sm", locale === "en-US" ? "border-accent bg-accent text-accent-ink" : "border-line")}
          >
            {t("english")}
          </button>
        </div>
      </section>

      <section className="mt-8">
        <h2 className="text-sm text-muted">{t("account")}</h2>
        <p className="mt-2 font-serif text-2xl">{session?.user.displayName}</p>
        <p className="text-sm text-muted">{session?.user.email}</p>
        <form className="mt-5 space-y-4" onSubmit={savePassword}>
          <h3 className="font-serif text-xl">{t("password")}</h3>
          <Alert code={code} />
          {saved ? <p className="text-sm text-good">{t("saved")}</p> : null}
          <TextField
            label={t("current")}
            type="password"
            autoComplete="current-password"
            value={currentPassword}
            onChange={(event) => setCurrentPassword(event.target.value)}
            error={errors.currentPassword ? fields("currentPassword") : null}
          />
          <TextField
            label={t("next")}
            type="password"
            autoComplete="new-password"
            value={nextPassword}
            onChange={(event) => setNextPassword(event.target.value)}
            error={errors.nextPassword ? fields("nextPassword") : null}
          />
          <Button type="submit">{t("save")}</Button>
        </form>
        <Button
          variant="ghost"
          className="mt-6"
          onClick={() => {
            signOut();
            router.replace("/");
          }}
        >
          {t("logout")}
        </Button>
      </section>

      <p className="mt-10 text-sm leading-6 text-muted">{t("about")}</p>
    </main>
  );
}
