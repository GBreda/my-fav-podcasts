"use client";

import { Art } from "@/components/Art";
import { IconCheck, IconPause, IconPlay } from "@/components/Icons";
import { progressLabel } from "@/lib/format";
import type { Episode } from "@/lib/types";

export function EpisodeRow({
  episode,
  current,
  playing,
  onPlay,
  onTogglePlayed,
  dimmed = false,
  showPodcast = true,
}: {
  episode: Episode;
  current: boolean;
  playing: boolean;
  onPlay: () => void;
  onTogglePlayed: () => void;
  dimmed?: boolean;
  showPodcast?: boolean;
}) {
  const meta = progressLabel(episode.durationSeconds, episode.positionSeconds);
  const progress = episode.durationSeconds
    ? Math.min(100, (episode.positionSeconds / episode.durationSeconds) * 100)
    : 0;

  return (
    <article className={dimmed ? "ep is-dim" : "ep"}>
      <Art src={episode.artworkUrl || episode.podcast.artworkUrl} title={episode.podcast.title} />
      <div className="ep-body">
        {showPodcast && <p className="ep-show">{episode.podcast.title}</p>}
        <h2 className="ep-title">{episode.title}</h2>
        {meta && <p className="ep-meta">{episode.played ? "ouvido" : meta}</p>}
        {!episode.played && progress > 0 && (
          <div className="ep-track" aria-hidden="true">
            <span style={{ width: `${progress}%` }} />
          </div>
        )}
      </div>
      <button
        className={episode.played ? "ep-check is-played" : "ep-check"}
        type="button"
        onClick={onTogglePlayed}
        aria-label={episode.played ? "Marcar como não ouvido" : "Marcar como ouvido"}
      >
        <IconCheck />
      </button>
      <button
        className={current ? "ep-play is-current" : "ep-play"}
        type="button"
        onClick={onPlay}
        aria-label={current && playing ? "Pausar" : `Tocar ${episode.title}`}
      >
        {current && playing ? <IconPause /> : <IconPlay />}
      </button>
    </article>
  );
}
