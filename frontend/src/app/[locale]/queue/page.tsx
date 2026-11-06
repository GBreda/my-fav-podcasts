"use client";

import { useTranslations } from "next-intl";
import { useEffect, useState } from "react";
import { Link } from "@/i18n/navigation";
import { ApiError, type QueueEntry } from "@/lib/api";
import { useAuth } from "@/lib/auth";
import { greetingKey } from "@/lib/format";
import { EpisodeRow } from "@/components/episode-row";
import { usePlayer } from "@/components/player";
import { RequireAuth } from "@/components/shell";
import { Alert, Button, EmptyState, cx } from "@/components/ui";

export default function QueuePage() {
  return (
    <RequireAuth>
      <QueueScreen />
    </RequireAuth>
  );
}

function QueueScreen() {
  const t = useTranslations("queue");
  const greetings = useTranslations("greetings");
  const { session, api } = useAuth();
  const player = usePlayer();
  const [filter, setFilter] = useState<"unplayed" | "all">("unplayed");
  const [order, setOrder] = useState<"newest" | "oldest">("newest");
  const [entries, setEntries] = useState<QueueEntry[] | null>(null);
  const [code, setCode] = useState<string | null>(null);
  const [refreshing, setRefreshing] = useState(false);
  const [failed, setFailed] = useState(0);
  const [hour, setHour] = useState(20);

  useEffect(() => {
    setHour(new Date().getHours());
  }, []);

  useEffect(() => {
    let cancelled = false;
    setEntries(null);
    api
      .queue(filter, order)
      .then((response) => {
        if (!cancelled) {
          setEntries(response.entries);
          setCode(null);
        }
      })
      .catch((error: unknown) => {
        if (!cancelled) {
          setCode(error instanceof ApiError ? error.code : "UNKNOWN");
          setEntries([]);
        }
      });
    return () => {
      cancelled = true;
    };
  }, [api, filter, order, player.revision]);

  async function refresh() {
    setRefreshing(true);
    try {
      const result = await api.refresh();
      setFailed(result.failed);
      const response = await api.queue(filter, order);
      setEntries(response.entries);
    } catch (error) {
      setCode(error instanceof ApiError ? error.code : "UNKNOWN");
    } finally {
      setRefreshing(false);
    }
  }

  async function markPlayed(episode: QueueEntry) {
    try {
      await api.progress(episode.episodeId, { positionSeconds: episode.positionSeconds, completed: true });
      const response = await api.queue(filter, order);
      setEntries(response.entries);
    } catch (error) {
      setCode(error instanceof ApiError ? error.code : "UNKNOWN");
    }
  }

  const greeting = greetings(greetingKey(hour));

  return (
    <main className="px-5 pt-6">
      <div className="flex items-start justify-between gap-3">
        <div>
          <h1 className="font-serif text-4xl tracking-tight">
            {greeting}, {session?.user.displayName}
          </h1>
          <p className="mt-2 text-sm text-muted">
            {entries === null ? t("refreshing") : t("count", { count: entries.length })}
          </p>
        </div>
        <Button variant="quiet" onClick={refresh} disabled={refreshing}>
          {refreshing ? t("refreshing") : t("refresh")}
        </Button>
      </div>
      <div className="mt-5 grid grid-cols-2 rounded-full bg-ink/5 p-1 text-sm">
        <Segment active={order === "newest"} onClick={() => setOrder("newest")}>{t("newest")}</Segment>
        <Segment active={order === "oldest"} onClick={() => setOrder("oldest")}>{t("oldest")}</Segment>
      </div>
      <div className="mt-3 flex gap-2">
        <Chip active={filter === "unplayed"} onClick={() => setFilter("unplayed")}>{t("unplayed")}</Chip>
        <Chip active={filter === "all"} onClick={() => setFilter("all")}>{t("all")}</Chip>
      </div>
      {failed > 0 ? <p className="mt-4 text-sm text-accent">{t("refreshFailed", { failed })}</p> : null}
      <div className="mt-4">
        <Alert code={code} />
      </div>
      {entries === null ? (
        <p className="mt-10 text-sm text-muted">{t("refreshing")}</p>
      ) : entries.length === 0 ? (
        <EmptyState
          title={t("emptyTitle")}
          body={t("emptyBody")}
          action={
            <Link href="/search" className="inline-flex h-12 items-center rounded-full bg-accent px-5 text-sm font-semibold text-accent-ink">
              {t("emptyCta")}
            </Link>
          }
        />
      ) : (
        <ol className="mt-2">
          {entries.map((episode) => (
            <EpisodeRow
              key={episode.episodeId}
              episode={episode}
              onPlay={(item) =>
                player.play({
                  episodeId: item.episodeId,
                  podcastId: item.podcastId,
                  podcastTitle: item.podcastTitle,
                  title: item.title,
                  summary: item.summary,
                  audioUrl: item.audioUrl,
                  artworkUrl: item.artworkUrl,
                  positionSeconds: item.positionSeconds,
                  durationSeconds: item.durationSeconds,
                })
              }
              onPlayed={markPlayed}
            />
          ))}
        </ol>
      )}
    </main>
  );
}

function Segment({ active, children, onClick }: { active: boolean; children: React.ReactNode; onClick: () => void }) {
  return (
    <button type="button" onClick={onClick} className={cx("h-10 rounded-full", active ? "bg-surface font-semibold" : "text-muted")}>
      {children}
    </button>
  );
}

function Chip({ active, children, onClick }: { active: boolean; children: React.ReactNode; onClick: () => void }) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={cx("h-9 rounded-full px-4 text-sm", active ? "bg-ink text-bg" : "border border-line text-muted")}
    >
      {children}
    </button>
  );
}
