"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { EpisodeRow } from "@/components/EpisodeRow";
import { IconRefresh } from "@/components/Icons";
import { usePlayer } from "@/components/Player";
import { SkeletonList } from "@/components/Skeleton";
import { ApiError, api } from "@/lib/api";
import { dayKey, dayLabel } from "@/lib/format";
import type { Episode, QueueResponse, RefreshResponse, SortOrder } from "@/lib/types";

export function QueueScreen() {
  const player = usePlayer();
  const { registerQueue, subscribe, play, markPlayed } = player;
  const [sort, setSort] = useState<SortOrder>("newest");
  const [ready, setReady] = useState(false);
  const [episodes, setEpisodes] = useState<Episode[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [banner, setBanner] = useState<{ tone: "quiet" | "alert"; text: string } | null>(null);

  useEffect(() => {
    const saved = localStorage.getItem("fila.sort");
    if (saved === "oldest" || saved === "newest") setSort(saved);
    setReady(true);
  }, []);

  useEffect(() => {
    if (!ready) return;
    localStorage.setItem("fila.sort", sort);
    const controller = new AbortController();
    setLoading(true);
    api<QueueResponse>(`/api/queue?sort=${sort}`)
      .then((queue) => {
        if (controller.signal.aborted) return;
        setEpisodes(queue.episodes);
        setTotal(queue.total);
        setBanner(null);
      })
      .catch((error: unknown) => {
        if (controller.signal.aborted) return;
        setBanner({
          tone: "alert",
          text: error instanceof ApiError ? error.message : "Não consegui carregar a fila.",
        });
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, [ready, sort]);

  useEffect(() => registerQueue(episodes), [episodes, registerQueue]);

  useEffect(() => {
    return subscribe((patch) => {
      setEpisodes((current) => {
        if (patch.played) return current.filter((item) => item.id !== patch.id);
        return current.map((item) => (item.id === patch.id ? { ...item, ...patch } : item));
      });
      if (patch.played) setTotal((value) => Math.max(0, value - 1));
    });
  }, [subscribe]);

  async function refresh() {
    setRefreshing(true);
    try {
      const result = await api<RefreshResponse>("/api/podcasts/refresh", { method: "POST" });
      const queue = await api<QueueResponse>(`/api/queue?sort=${sort}`);
      setEpisodes(queue.episodes);
      setTotal(queue.total);
      setBanner({
        tone: result.failed > 0 ? "alert" : "quiet",
        text: result.failed > 0
          ? `Atualizei ${result.updated}. ${result.failed} não respondeu.`
          : "Fila atualizada.",
      });
    } catch (error) {
      setBanner({
        tone: "alert",
        text: error instanceof ApiError ? error.message : "Não consegui atualizar.",
      });
    } finally {
      setRefreshing(false);
    }
  }

  async function mark(episode: Episode) {
    const previous = episodes;
    setEpisodes((current) => current.filter((item) => item.id !== episode.id));
    try {
      await markPlayed(episode);
    } catch (error) {
      setEpisodes(previous);
      setBanner({
        tone: "alert",
        text: error instanceof ApiError ? error.message : "Não consegui marcar como ouvido.",
      });
    }
  }

  let lastDay = "";

  return (
    <>
      <header className="topbar">
        <div>
          <p className="kicker">por lançamento</p>
          <h1>
            fila
            {!loading && <span className="count">{total}</span>}
          </h1>
        </div>
        <div className="topbar-extra">
          <div className="seg" role="radiogroup" aria-label="Ordem da fila">
            <button type="button" role="radio" aria-checked={sort === "newest"} onClick={() => setSort("newest")}>
              Recentes
            </button>
            <button type="button" role="radio" aria-checked={sort === "oldest"} onClick={() => setSort("oldest")}>
              Antigos
            </button>
          </div>
          <button
            className={refreshing ? "icon-btn spin" : "icon-btn"}
            type="button"
            onClick={() => void refresh()}
            disabled={refreshing}
            aria-label="Atualizar lançamentos"
          >
            <IconRefresh />
          </button>
        </div>
      </header>
      {banner && <p className={banner.tone === "quiet" ? "banner quiet" : "banner"}>{banner.text}</p>}
      {!loading && total > episodes.length && (
        <p className="note">Mostrando os {episodes.length} primeiros de {total}.</p>
      )}
      {loading ? (
        <SkeletonList />
      ) : episodes.length === 0 ? (
        <div className="empty">
          <h2>Nada esperando na fila.</h2>
          <p>Quando você segue um podcast, os episódios entram aqui por ordem de lançamento.</p>
          <Link className="primary" href="/buscar">Buscar um podcast</Link>
        </div>
      ) : (
        episodes.map((episode) => {
          const key = dayKey(episode.publishedAt);
          const header = key === lastDay ? null : (
            <h2 key={`day-${key}`} className="day">{dayLabel(episode.publishedAt)}</h2>
          );
          lastDay = key;
          return (
            <div key={episode.id}>
              {header}
              <EpisodeRow
                episode={episode}
                current={player.episode?.id === episode.id}
                playing={player.playing}
                onPlay={() => play(episode, episodes)}
                onTogglePlayed={() => void mark(episode)}
              />
            </div>
          );
        })
      )}
    </>
  );
}
