"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { useEffect, useState } from "react";
import { Art } from "@/components/Art";
import { EpisodeRow } from "@/components/EpisodeRow";
import { IconRefresh } from "@/components/Icons";
import { usePlayer } from "@/components/Player";
import { SkeletonList } from "@/components/Skeleton";
import { ApiError, api } from "@/lib/api";
import { updatedLabel } from "@/lib/format";
import type { Episode, PodcastDetail } from "@/lib/types";

export function PodcastScreen() {
  const params = useSearchParams();
  const router = useRouter();
  const id = params.get("id");
  const player = usePlayer();
  const { registerQueue, subscribe, play, markPlayed, markUnplayed } = player;
  const [detail, setDetail] = useState<PodcastDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [confirming, setConfirming] = useState(false);
  const [removing, setRemoving] = useState(false);
  const [banner, setBanner] = useState<string | null>(null);

  useEffect(() => {
    if (!id) {
      setLoading(false);
      return;
    }
    setLoading(true);
    api<PodcastDetail>(`/api/podcasts/${id}`)
      .then(setDetail)
      .catch((error: unknown) => {
        setBanner(error instanceof ApiError ? error.message : "Não encontrei esse podcast.");
      })
      .finally(() => setLoading(false));
  }, [id]);

  useEffect(() => {
    if (!detail) return;
    registerQueue(detail.episodes);
  }, [detail, registerQueue]);

  useEffect(() => {
    return subscribe((patch) => {
      setDetail((current) => {
        if (!current) return current;
        const episodes = current.episodes.map((item) =>
          item.id === patch.id ? { ...item, ...patch, played: patch.played ?? item.played } : item,
        );
        const unplayedCount = episodes.filter((item) => !item.played).length;
        return {
          podcast: { ...current.podcast, unplayedCount, episodeCount: episodes.length },
          episodes,
        };
      });
    });
  }, [subscribe]);

  async function refresh() {
    if (!id) return;
    setRefreshing(true);
    try {
      setDetail(await api<PodcastDetail>(`/api/podcasts/${id}/refresh`, { method: "POST" }));
      setBanner(null);
    } catch (error) {
      setBanner(error instanceof ApiError ? error.message : "Não consegui atualizar esse feed.");
    } finally {
      setRefreshing(false);
    }
  }

  async function remove() {
    if (!id) return;
    setRemoving(true);
    try {
      await api(`/api/podcasts/${id}`, { method: "DELETE" });
      router.push("/biblioteca");
    } catch (error) {
      setBanner(error instanceof ApiError ? error.message : "Não consegui tirar esse podcast.");
      setRemoving(false);
    }
  }

  async function toggle(episode: Episode) {
    try {
      if (episode.played) await markUnplayed(episode);
      else await markPlayed(episode);
    } catch (error) {
      setBanner(error instanceof ApiError ? error.message : "Não consegui atualizar o episódio.");
    }
  }

  if (!id) {
    return (
      <div className="empty">
        <h2>Escolha um podcast.</h2>
        <Link className="primary" href="/biblioteca">Voltar à biblioteca</Link>
      </div>
    );
  }

  const podcast = detail?.podcast;

  return (
    <>
      <header className="topbar">
        <div>
          <p className="kicker">programa</p>
          <h1>episódios</h1>
        </div>
        <button
          className={refreshing ? "icon-btn spin" : "icon-btn"}
          type="button"
          onClick={() => void refresh()}
          disabled={refreshing || loading}
          aria-label="Atualizar este podcast"
        >
          <IconRefresh />
        </button>
      </header>
      {banner && <p className="banner">{banner}</p>}
      {loading ? (
        <SkeletonList />
      ) : podcast ? (
        <>
          <section className="show-hero">
            <Art src={podcast.artworkUrl} title={podcast.title} />
            <div>
              <h2>{podcast.title}</h2>
              <p className="result-author">{podcast.author}</p>
              {updatedLabel(podcast.lastRefreshedAt) && (
                <p className="ep-meta">{updatedLabel(podcast.lastRefreshedAt)}</p>
              )}
            </div>
          </section>
          {confirming ? (
            <div className="confirm">
              <p>Tirar este podcast da biblioteca? Os episódios saem da fila.</p>
              <div className="confirm-actions">
                <button className="primary" type="button" onClick={() => void remove()} disabled={removing}>
                  {removing ? "Tirando…" : "Tirar"}
                </button>
                <button className="ghost" type="button" onClick={() => setConfirming(false)}>Cancelar</button>
              </div>
            </div>
          ) : (
            <div className="show-actions" style={{ padding: "0 20px 12px" }}>
              <button className="ghost" type="button" onClick={() => setConfirming(true)}>
                Deixar de seguir
              </button>
            </div>
          )}
          {detail.episodes.map((episode) => (
            <EpisodeRow
              key={episode.id}
              episode={episode}
              showPodcast={false}
              dimmed={episode.played}
              current={player.episode?.id === episode.id}
              playing={player.playing}
              onPlay={() => play(episode, detail.episodes)}
              onTogglePlayed={() => void toggle(episode)}
            />
          ))}
        </>
      ) : null}
    </>
  );
}
