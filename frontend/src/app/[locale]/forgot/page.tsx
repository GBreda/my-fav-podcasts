"use client";

import { useLocale, useTranslations } from "next-intl";
import { useState } from "react";
import { Link } from "@/i18n/navigation";
import { ApiError } from "@/lib/api";
import { useAuth } from "@/lib/auth";
import { fieldErrors, forgotSchema } from "@/lib/schemas";
import { Alert, AuthCard, Button, TextField } from "@/components/ui";

export default function ForgotPage() {
  const t = useTranslations("auth");
  const fields = useTranslations("fields");
  const locale = useLocale();
  const { api } = useAuth();
  const [email, setEmail] = useState("");
  const [errors, setErrors] = useState<Record<string, true>>({});
  const [code, setCode] = useState<string | null>(null);
  const [pending, setPending] = useState(false);
  const [delivery, setDelivery] = useState<"log" | "email" | null>(null);

  async function submit(event: React.FormEvent) {
    event.preventDefault();
    const parsed = forgotSchema.safeParse({ email });
    if (!parsed.success) {
      setErrors(fieldErrors(parsed.error));
      setCode(null);
      return;
    }
    setErrors({});
    setPending(true);
    try {
      const result = await api.forgotPassword({ email: parsed.data.email, locale });
      setDelivery(result.delivery);
    } catch (error) {
      setCode(error instanceof ApiError ? error.code : "UNKNOWN");
    } finally {
      setPending(false);
    }
  }

  if (delivery) {
    return (
      <AuthCard
        title={t("forgotSentTitle")}
        footer={
          <Link href="/login" className="text-ink underline">
            {t("toLogin")}
          </Link>
        }
      >
        <p className="text-sm leading-6 text-muted">
          {delivery === "log" ? t("forgotSentLog") : t("forgotSentEmail")}
        </p>
      </AuthCard>
    );
  }

  return (
    <AuthCard
      title={t("forgotTitle")}
      footer={
        <Link href="/login" className="text-ink underline">
          {t("toLogin")}
        </Link>
      }
    >
      <p className="text-sm leading-6 text-muted">{t("forgotLead")}</p>
      <form className="space-y-4" onSubmit={submit}>
        <Alert code={code} />
        <TextField
          label={t("email")}
          type="email"
          autoComplete="email"
          value={email}
          onChange={(event) => setEmail(event.target.value)}
          error={errors.email ? fields("email") : null}
        />
        <Button type="submit" className="w-full" disabled={pending}>
          {t("submitForgot")}
        </Button>
      </form>
    </AuthCard>
  );
}
