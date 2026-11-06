"use client";

import { useTranslations } from "next-intl";
import { useState } from "react";
import { Link, useRouter } from "@/i18n/navigation";
import { ApiError } from "@/lib/api";
import { useAuth } from "@/lib/auth";
import { fieldErrors, registerSchema } from "@/lib/schemas";
import { Alert, AuthCard, Button, TextField } from "@/components/ui";

export default function RegisterPage() {
  const t = useTranslations("auth");
  const fields = useTranslations("fields");
  const { api, signIn } = useAuth();
  const router = useRouter();
  const [displayName, setDisplayName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [errors, setErrors] = useState<Record<string, true>>({});
  const [code, setCode] = useState<string | null>(null);
  const [pending, setPending] = useState(false);

  async function submit(event: React.FormEvent) {
    event.preventDefault();
    const parsed = registerSchema.safeParse({ displayName, email, password });
    if (!parsed.success) {
      setErrors(fieldErrors(parsed.error));
      setCode(null);
      return;
    }
    setErrors({});
    setPending(true);
    try {
      signIn(await api.register(parsed.data));
      router.replace("/queue");
    } catch (error) {
      setCode(error instanceof ApiError ? error.code : "UNKNOWN");
    } finally {
      setPending(false);
    }
  }

  return (
    <AuthCard
      title={t("registerTitle")}
      footer={
        <>
          {t("haveAccount")}{" "}
          <Link href="/login" className="text-ink underline">
            {t("toLogin")}
          </Link>
        </>
      }
    >
      <form className="space-y-4" onSubmit={submit}>
        <Alert code={code} />
        <TextField
          label={t("name")}
          autoComplete="nickname"
          value={displayName}
          onChange={(event) => setDisplayName(event.target.value)}
          error={errors.displayName ? fields("displayName") : null}
        />
        <TextField
          label={t("email")}
          type="email"
          autoComplete="email"
          value={email}
          onChange={(event) => setEmail(event.target.value)}
          error={errors.email ? fields("email") : null}
        />
        <TextField
          label={t("password")}
          type="password"
          autoComplete="new-password"
          value={password}
          onChange={(event) => setPassword(event.target.value)}
          error={errors.password ? fields("password") : null}
        />
        <p className="text-xs text-muted">{t("passwordHint")}</p>
        <Button type="submit" className="w-full" disabled={pending}>
          {t("submitRegister")}
        </Button>
      </form>
    </AuthCard>
  );
}
