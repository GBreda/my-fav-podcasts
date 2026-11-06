"use client";

import { useTranslations } from "next-intl";
import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import { Link } from "@/i18n/navigation";
import { ApiError, type Podcast } from "@/lib/api";
import { useAuth } from "@/lib/auth";
import { EpisodeRow } from "@/components/episode-row";
import { usePlayer } from "@/components/player";
import { RequireAuth } from "@/components/shell";
import { Alert, Artwork, Button } from "@/components/ui";

export default function PodcastPage() {
  return (
    <RequireAuth>
      <PodcastScreen />
    </RequireAuth>
  );
}

function PodcastScreen() {
  const params = useParams<{ id: string }>();
  const t = useTranslations("podcast");
  const { api } = useAuth();
  const player = usePlayer();
  const [podcast, setPodcast] = useState<Podcast | null>(null);
  const [code, setCode] = useState<string | null>(null);
  const [pending, setPending] = useState(false);

  useEffect(() => {
    api
      .podcast(params.id)
      .then(setPodcast)
      .catch((error: unknown) => setCode(error instanceof ApiError ? error.code : "UNKNOWN"));
  }, [api, params.id]);

  async function follow() {
    if (!podcast) {
      return;
    }
    setPending(true);
    try {
      await api.follow({
        feedUrl: podcast.feedUrl,
        title: podcast.title,
        author: podcast.author,
        artworkUrl: podcast.artworkUrl,
      });
      setPodcast({ ...podcast, followed: true });
    } catch (error) {
      setCode(error instanceof ApiError ? error.code : "UNKNOWN");
    } finally {
      setPending(false);
    }
  }

  return (
    <main className="px-5 pt-6">
      <Link href="/library" className="text-sm text-muted">
        {t("back")}
      </Link>
      <Alert code={code} />
      {podcast ? (
        <>
          <Artwork src={podcast.artworkUrl} title={podcast.title} className="mt-4 aspect-square w-full rounded-[2rem]" />
          <h1 className="mt-5 font-serif text-4xl tracking-tight">{podcast.title}</h1>
          <p className="mt-1 text-sm text-muted">{podcast.author}</p>
          {podcast.description ? <p className="mt-4 text-sm leading-6 text-muted">{podcast.description}</p> : null}
          <Button className="mt-5" variant={podcast.followed ? "ghost" : "primary"} disabled={podcast.followed || pending} onClick={follow}>
            {podcast.followed ? t("following") : t("follow")}
          </Button>
          <h2 className="mt-8 font-serif text-2xl">{t("episodes")}</h2>
          <ol>
            {podcast.episodes.map((episode) => (
              <EpisodeRow
                key={episode.id}
                episode={{
                  episodeId: episode.id,
                  podcastId: podcast.id,
                  podcastTitle: podcast.title,
                  title: episode.title,
                  summary: episode.summary,
                  audioUrl: episode.audioUrl,
                  artworkUrl: episode.artworkUrl || podcast.artworkUrl,
                  publishedAt: episode.publishedAt,
                  durationSeconds: episode.durationSeconds,
                  positionSeconds: 0,
                  completed: false,
                }}
                onPlay={(item) =>
                  player.play({
                    episodeId: item.episodeId,
                    podcastId: item.podcastId,
                    podcastTitle: item.podcastTitle,
                    title: item.title,
                    summary: item.summary,
                    audioUrl: item.audioUrl,
                    artworkUrl: item.artworkUrl,
                    positionSeconds: 0,
                    durationSeconds: item.durationSeconds,
                  })
                }
              />
            ))}
          </ol>
        </>
      ) : null}
    </main>
  );
}
