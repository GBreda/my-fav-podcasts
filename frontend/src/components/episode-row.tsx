"use client";

import { useLocale, useTranslations } from "next-intl";
import type { QueueEntry } from "@/lib/api";
import { formatDurationLabel, formatRelative } from "@/lib/format";
import { Artwork, cx } from "./ui";

export function EpisodeRow({
  episode,
  onPlay,
  onPlayed,
}: {
  episode: QueueEntry;
  onPlay: (episode: QueueEntry) => void;
  onPlayed?: (episode: QueueEntry) => void;
}) {
  const locale = useLocale();
  const t = useTranslations();
  const duration = formatDurationLabel(episode.durationSeconds, {
    hour: t("common.hour"),
    minute: t("common.minute"),
  });

  return (
    <li className={cx("flex items-center gap-3 border-t border-line py-3", episode.completed && "opacity-55")}>
      <button type="button" onClick={() => onPlay(episode)} className="flex min-w-0 flex-1 items-center gap-3 text-left">
        <Artwork src={episode.artworkUrl} title={episode.podcastTitle} className="h-14 w-14 shrink-0 rounded-xl" />
        <span className="min-w-0">
          <span className="block truncate text-xs text-muted">{episode.podcastTitle}</span>
          <span className="block truncate font-serif text-lg leading-6">{episode.title}</span>
          <span className="block truncate text-xs text-muted">
            {formatRelative(episode.publishedAt, locale)}
            {duration ? ` · ${duration}` : ""}
          </span>
        </span>
      </button>
      <button
        type="button"
        aria-label={t("player.play")}
        onClick={() => onPlay(episode)}
        className="grid h-11 w-11 shrink-0 place-items-center rounded-full bg-ink text-bg"
      >
        <svg viewBox="0 0 24 24" className="h-4 w-4" aria-hidden>
          <path d="M8 5.5v13l11-6.5-11-6.5z" className="fill-current" />
        </svg>
      </button>
      {onPlayed && !episode.completed ? (
        <button
          type="button"
          aria-label={t("queue.markPlayed")}
          onClick={() => onPlayed(episode)}
          className="grid h-11 w-11 shrink-0 place-items-center rounded-full border border-line text-muted"
        >
          <svg viewBox="0 0 24 24" className="h-4 w-4" aria-hidden>
            <path d="M5 12.5l4.2 4.2L19 7.8" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" />
          </svg>
        </button>
      ) : null}
    </li>
  );
}
