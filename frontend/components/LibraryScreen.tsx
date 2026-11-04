"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { Art } from "@/components/Art";
import { SkeletonList } from "@/components/Skeleton";
import { ApiError, api } from "@/lib/api";
import type { PodcastSummary } from "@/lib/types";

export function LibraryScreen() {
  const [podcasts, setPodcasts] = useState<PodcastSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [banner, setBanner] = useState<string | null>(null);

  useEffect(() => {
    api<PodcastSummary[]>("/api/podcasts")
      .then(setPodcasts)
      .catch((error: unknown) => {
        setBanner(error instanceof ApiError ? error.message : "Não consegui abrir a biblioteca.");
      })
      .finally(() => setLoading(false));
  }, []);

  return (
    <>
      <header className="topbar">
        <div>
          <p className="kicker">programas</p>
          <h1>biblioteca</h1>
        </div>
      </header>
      {banner && <p className="banner">{banner}</p>}
      {loading ? (
        <SkeletonList />
      ) : podcasts.length === 0 ? (
        <div className="empty">
          <h2>Sua biblioteca está vazia.</h2>
          <p>Os podcasts que você seguir aparecem aqui.</p>
          <Link className="primary" href="/buscar">Buscar um podcast</Link>
        </div>
      ) : (
        <div className="library">
          {podcasts.map((podcast) => (
            <Link className="show-card" key={podcast.id} href={`/podcast?id=${podcast.id}`}>
              <Art src={podcast.artworkUrl} title={podcast.title} />
              {podcast.unplayedCount > 0 && <span className="badge">{podcast.unplayedCount}</span>}
              <span className="show-card-title">{podcast.title}</span>
              <span className="show-card-author">{podcast.author}</span>
            </Link>
          ))}
        </div>
      )}
    </>
  );
}
