"use client";

import { useLocale, useTranslations } from "next-intl";
import { useEffect, useState } from "react";
import { ApiError, type SearchHit } from "@/lib/api";
import { useAuth } from "@/lib/auth";
import { feedSchema } from "@/lib/schemas";
import { RequireAuth } from "@/components/shell";
import { Alert, Artwork, Button, TextField, cx } from "@/components/ui";

export default function SearchPage() {
  return (
    <RequireAuth>
      <SearchScreen />
    </RequireAuth>
  );
}

function SearchScreen() {
  const t = useTranslations("search");
  const fields = useTranslations("fields");
  const locale = useLocale();
  const { api } = useAuth();
  const [query, setQuery] = useState("");
  const [results, setResults] = useState<SearchHit[] | null>(null);
  const [code, setCode] = useState<string | null>(null);
  const [busyFeed, setBusyFeed] = useState<string | null>(null);
  const [showRss, setShowRss] = useState(false);
  const [feedUrl, setFeedUrl] = useState("");
  const [feedError, setFeedError] = useState<string | null>(null);
  const [adding, setAdding] = useState(false);

  useEffect(() => {
    const term = query.trim();
    if (term.length < 2) {
      setResults(null);
      return;
    }
    const handle = setTimeout(() => {
      api
        .search(term, locale === "pt-BR" ? "BR" : "US")
        .then((response) => {
          setResults(response.results);
          setCode(null);
        })
        .catch((error: unknown) => setCode(error instanceof ApiError ? error.code : "UNKNOWN"));
    }, 300);
    return () => clearTimeout(handle);
  }, [api, locale, query]);

  async function follow(hit: SearchHit) {
    setBusyFeed(hit.feedUrl);
    try {
      const podcast = await api.follow({
        feedUrl: hit.feedUrl,
        title: hit.title,
        author: hit.author,
        artworkUrl: hit.artworkUrl,
      });
      setResults((current) =>
        current?.map((item) =>
          item.feedUrl === hit.feedUrl ? { ...item, followed: true, podcastId: podcast.id } : item,
        ) ?? current,
      );
      setCode(null);
    } catch (error) {
      setCode(error instanceof ApiError ? error.code : "UNKNOWN");
    } finally {
      setBusyFeed(null);
    }
  }

  async function addFeed(event: React.FormEvent) {
    event.preventDefault();
    const parsed = feedSchema.safeParse({ feedUrl });
    if (!parsed.success) {
      setFeedError(fields("feedUrl"));
      return;
    }
    setFeedError(null);
    setAdding(true);
    try {
      await api.follow({ feedUrl: parsed.data.feedUrl });
      setFeedUrl("");
      setCode(null);
      setShowRss(false);
    } catch (error) {
      setCode(error instanceof ApiError ? error.code : "UNKNOWN");
    } finally {
      setAdding(false);
    }
  }

  return (
    <main className="px-5 pt-6">
      <h1 className="font-serif text-4xl tracking-tight">{t("title")}</h1>
      <label className="mt-5 block">
        <span className="sr-only">{t("placeholder")}</span>
        <input
          value={query}
          onChange={(event) => setQuery(event.target.value)}
          placeholder={t("placeholder")}
          className="h-12 w-full rounded-2xl border border-line bg-surface px-4 text-base outline-none"
        />
      </label>
      <div className="mt-4">
        <Alert code={code} />
      </div>
      {results === null ? <p className="mt-8 text-sm leading-6 text-muted">{t("prompt")}</p> : null}
      {results?.length === 0 ? <p className="mt-8 text-sm text-muted">{t("empty")}</p> : null}
      <ul className="mt-4">
        {results?.map((hit, index) => (
          <li key={`${hit.feedUrl}-${index}`} className="flex items-center gap-3 border-t border-line py-3">
            <Artwork src={hit.artworkUrl} title={hit.title} className="h-14 w-14 shrink-0 rounded-xl" />
            <span className="min-w-0 flex-1">
              <span className="block truncate font-serif text-lg leading-6">{hit.title}</span>
              <span className="block truncate text-xs text-muted">
                {hit.author}
                {hit.genre ? ` · ${hit.genre}` : ""}
              </span>
            </span>
            <button
              type="button"
              disabled={hit.followed || busyFeed === hit.feedUrl}
              onClick={() => follow(hit)}
              className={cx(
                "h-10 shrink-0 rounded-full px-4 text-sm font-semibold",
                hit.followed ? "text-good" : "bg-accent text-accent-ink",
              )}
            >
              {hit.followed ? t("following") : t("subscribe")}
            </button>
          </li>
        ))}
      </ul>
      <button type="button" className="mt-6 text-sm text-accent" onClick={() => setShowRss((value) => !value)}>
        {t("rssToggle")}
      </button>
      {showRss ? (
        <form className="mt-4 space-y-3" onSubmit={addFeed}>
          <p className="text-sm leading-6 text-muted">{t("rssHelp")}</p>
          <TextField
            label={t("feedUrl")}
            value={feedUrl}
            onChange={(event) => setFeedUrl(event.target.value)}
            placeholder="https://"
            error={feedError}
          />
          <Button type="submit" disabled={adding}>
            {adding ? t("adding") : t("add")}
          </Button>
        </form>
      ) : null}
    </main>
  );
}
