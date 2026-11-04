"use client";

import { useTranslations } from "next-intl";
import { useEffect, useState } from "react";
import { Link } from "@/i18n/navigation";
import { ApiError, type Subscription } from "@/lib/api";
import { useAuth } from "@/lib/auth";
import { RequireAuth } from "@/components/shell";
import { Alert, Artwork, EmptyState } from "@/components/ui";

export default function LibraryPage() {
  return (
    <RequireAuth>
      <LibraryScreen />
    </RequireAuth>
  );
}

function LibraryScreen() {
  const t = useTranslations("library");
  const search = useTranslations("search");
  const { api } = useAuth();
  const [items, setItems] = useState<Subscription[] | null>(null);
  const [code, setCode] = useState<string | null>(null);
  const [pending, setPending] = useState<string | null>(null);

  useEffect(() => {
    api
      .subscriptions()
      .then((response) => setItems(response.items))
      .catch((error: unknown) => setCode(error instanceof ApiError ? error.code : "UNKNOWN"));
  }, [api]);

  async function unfollow(id: string) {
    if (pending === id) {
      await api.unfollow(id);
      setItems((current) => current?.filter((item) => item.id !== id) ?? current);
      setPending(null);
      return;
    }
    setPending(id);
  }

  return (
    <main className="px-5 pt-6">
      <h1 className="font-serif text-4xl tracking-tight">{t("title")}</h1>
      <div className="mt-4">
        <Alert code={code} />
      </div>
      {items?.length === 0 ? (
        <EmptyState
          title={t("empty")}
          body={search("prompt")}
          action={
            <Link href="/search" className="inline-flex h-12 items-center rounded-full bg-accent px-5 text-sm font-semibold text-accent-ink">
              {search("title")}
            </Link>
          }
        />
      ) : null}
      <ul className="mt-4">
        {items?.map((item) => (
          <li key={item.id} className="flex items-center gap-3 border-t border-line py-3">
            <Link href={`/podcast/${item.id}`} className="flex min-w-0 flex-1 items-center gap-3">
              <Artwork src={item.artworkUrl} title={item.title} className="h-14 w-14 shrink-0 rounded-xl" />
              <span className="min-w-0">
                <span className="block truncate font-serif text-lg leading-6">{item.title}</span>
                <span className="block truncate text-xs text-muted">{item.author}</span>
              </span>
            </Link>
            <button type="button" className="text-sm text-muted" onClick={() => unfollow(item.id)}>
              {pending === item.id ? t("confirm") : t("unfollow")}
            </button>
          </li>
        ))}
      </ul>
    </main>
  );
}
