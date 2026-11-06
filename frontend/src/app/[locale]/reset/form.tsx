"use client";

import { useTranslations } from "next-intl";
import { useState } from "react";
import { useSearchParams } from "next/navigation";
import { Link } from "@/i18n/navigation";
import { ApiError } from "@/lib/api";
import { useAuth } from "@/lib/auth";
import { fieldErrors, resetSchema } from "@/lib/schemas";
import { Alert, AuthCard, Button, TextField } from "@/components/ui";

export function ResetForm() {
  const t = useTranslations("auth");
  const fields = useTranslations("fields");
  const params = useSearchParams();
  const token = params.get("token") ?? "";
  const { api } = useAuth();
  const [password, setPassword] = useState("");
  const [confirm, setConfirm] = useState("");
  const [errors, setErrors] = useState<Record<string, true>>({});
  const [code, setCode] = useState<string | null>(token ? null : "RESET_TOKEN_INVALID");
  const [pending, setPending] = useState(false);
  const [done, setDone] = useState(false);

  async function submit(event: React.FormEvent) {
    event.preventDefault();
    const parsed = resetSchema.safeParse({ password, confirm });
    if (!parsed.success) {
      setErrors(fieldErrors(parsed.error));
      setCode(null);
      return;
    }
    setErrors({});
    setPending(true);
    try {
      await api.resetPassword({ token, password: parsed.data.password });
      setDone(true);
    } catch (error) {
      setCode(error instanceof ApiError ? error.code : "UNKNOWN");
    } finally {
      setPending(false);
    }
  }

  if (done) {
    return (
      <AuthCard
        title={t("resetDoneTitle")}
        footer={
          <Link href="/login" className="text-ink underline">
            {t("toLogin")}
          </Link>
        }
      >
        <p className="text-sm leading-6 text-muted">{t("resetDone")}</p>
      </AuthCard>
    );
  }

  return (
    <AuthCard
      title={t("resetTitle")}
      footer={
        <Link href="/forgot" className="text-ink underline">
          {t("forgot")}
        </Link>
      }
    >
      <p className="text-sm leading-6 text-muted">{t("resetLead")}</p>
      <form className="space-y-4" onSubmit={submit}>
        <Alert code={code} />
        <TextField
          label={t("password")}
          type="password"
          autoComplete="new-password"
          value={password}
          onChange={(event) => setPassword(event.target.value)}
          error={errors.password ? fields("password") : null}
        />
        <TextField
          label={t("confirm")}
          type="password"
          autoComplete="new-password"
          value={confirm}
          onChange={(event) => setConfirm(event.target.value)}
          error={errors.confirm ? fields("confirm") : null}
        />
        <Button type="submit" className="w-full" disabled={pending || !token}>
          {t("submitReset")}
        </Button>
      </form>
    </AuthCard>
  );
}
