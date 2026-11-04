"use client";

import { useTranslations } from "next-intl";
import { createContext, useContext, useEffect, useRef, useState } from "react";
import { useAuth } from "@/lib/auth";
import { formatClock } from "@/lib/format";
import { Artwork, Button, cx } from "./ui";

export type Track = {
  episodeId: string;
  podcastId: string;
  podcastTitle: string;
  title: string;
  summary?: string;
  audioUrl: string;
  artworkUrl: string;
  positionSeconds: number;
  durationSeconds: number | null;
};

type PlayerContextValue = {
  track: Track | null;
  playing: boolean;
  revision: number;
  play: (track: Track) => void;
  toggle: () => void;
};

const PlayerContext = createContext<PlayerContextValue | null>(null);
const RATES = [1, 1.25, 1.5, 1.75, 2];

export function PlayerProvider({ children }: { children: React.ReactNode }) {
  const { session, ready, api } = useAuth();
  const audioRef = useRef<HTMLAudioElement>(null);
  const trackRef = useRef<Track | null>(null);
  const lastSave = useRef(0);
  const [track, setTrack] = useState<Track | null>(null);
  const [playing, setPlaying] = useState(false);
  const [position, setPosition] = useState(0);
  const [duration, setDuration] = useState(0);
  const [rate, setRate] = useState(1);
  const [expanded, setExpanded] = useState(false);
  const [revision, setRevision] = useState(0);
  trackRef.current = track;

  useEffect(() => {
    const savedRate = localStorage.getItem("fila.rate");
    if (savedRate) {
      setRate(Number(savedRate) || 1);
    }
  }, []);

  useEffect(() => {
    if (!ready) {
      return;
    }
    if (!session) {
      audioRef.current?.pause();
      setTrack(null);
      setPlaying(false);
      setExpanded(false);
      sessionStorage.removeItem("fila.now");
      return;
    }
    const saved = sessionStorage.getItem("fila.now");
    if (!saved) {
      return;
    }
    try {
      const restored = JSON.parse(saved) as Track;
      setTrack(restored);
      if (audioRef.current) {
        audioRef.current.src = restored.audioUrl;
      }
    } catch {
      sessionStorage.removeItem("fila.now");
    }
  }, [ready, session]);

  useEffect(() => {
    if (audioRef.current) {
      audioRef.current.playbackRate = rate;
    }
    localStorage.setItem("fila.rate", String(rate));
  }, [rate]);

  async function persist(completed: boolean) {
    const audio = audioRef.current;
    const current = trackRef.current;
    if (!audio || !current || !session) {
      return;
    }
    try {
      await api.progress(current.episodeId, {
        positionSeconds: Math.max(0, Math.floor(audio.currentTime || 0)),
        completed,
      });
      if (completed) {
        setRevision((value) => value + 1);
      }
    } catch {
      // Playback continues even if the progress request fails.
    }
  }

  function remember(next: Track) {
    setTrack(next);
    trackRef.current = next;
    sessionStorage.setItem("fila.now", JSON.stringify(next));
  }

  function play(next: Track) {
    const audio = audioRef.current;
    remember(next);
    setExpanded(false);
    if (!audio) {
      return;
    }
    audio.src = next.audioUrl;
    audio.playbackRate = rate;
    const start = () => {
      if (next.positionSeconds > 1 && Number.isFinite(audio.duration)) {
        audio.currentTime = Math.min(next.positionSeconds, Math.max(audio.duration - 1, 0));
      }
      audio.play().then(() => setPlaying(true)).catch(() => setPlaying(false));
    };
    audio.addEventListener("loadedmetadata", start, { once: true });
  }

  function toggle() {
    const audio = audioRef.current;
    if (!audio || !track) {
      return;
    }
    if (audio.paused) {
      audio.play().then(() => setPlaying(true)).catch(() => setPlaying(false));
    } else {
      audio.pause();
      setPlaying(false);
    }
  }

  function skip(delta: number) {
    const audio = audioRef.current;
    if (!audio) {
      return;
    }
    audio.currentTime = Math.min(Math.max(0, audio.currentTime + delta), Number.isFinite(audio.duration) ? audio.duration : audio.currentTime + delta);
  }

  const value: PlayerContextValue = { track, playing, revision, play, toggle };

  return (
    <PlayerContext.Provider value={value}>
      {children}
      <audio
        ref={audioRef}
        preload="none"
        playsInline
        onTimeUpdate={() => {
          const audio = audioRef.current;
          if (!audio) {
            return;
          }
          setPosition(audio.currentTime || 0);
          if (Number.isFinite(audio.duration)) {
            setDuration(audio.duration);
          }
          if (!audio.paused && audio.currentTime - lastSave.current > 10) {
            lastSave.current = audio.currentTime;
            void persist(false);
          }
        }}
        onPause={() => {
          setPlaying(false);
          void persist(false);
        }}
        onEnded={() => {
          setPlaying(false);
          void persist(true);
        }}
      />
      {track ? (
        <PlayerChrome
          track={track}
          playing={playing}
          position={position}
          duration={duration}
          rate={rate}
          expanded={expanded}
          onToggle={toggle}
          onExpand={() => setExpanded(true)}
          onClose={() => setExpanded(false)}
          onSkip={skip}
          onSeek={(next) => {
            if (audioRef.current) {
              audioRef.current.currentTime = next;
              setPosition(next);
            }
          }}
          onRate={() => setRate(RATES[(RATES.indexOf(rate) + 1) % RATES.length] ?? 1)}
          onPlayed={() => void persist(true)}
        />
      ) : null}
    </PlayerContext.Provider>
  );
}

export function usePlayer() {
  const value = useContext(PlayerContext);
  if (!value) {
    throw new Error("PlayerProvider is missing");
  }
  return value;
}

function PlayerChrome({
  track,
  playing,
  position,
  duration,
  rate,
  expanded,
  onToggle,
  onExpand,
  onClose,
  onSkip,
  onSeek,
  onRate,
  onPlayed,
}: {
  track: Track;
  playing: boolean;
  position: number;
  duration: number;
  rate: number;
  expanded: boolean;
  onToggle: () => void;
  onExpand: () => void;
  onClose: () => void;
  onSkip: (delta: number) => void;
  onSeek: (value: number) => void;
  onRate: () => void;
  onPlayed: () => void;
}) {
  const t = useTranslations("player");
  const progress = duration > 0 ? Math.min(100, (position / duration) * 100) : 0;

  return (
    <>
      <div className="fixed inset-x-0 bottom-[calc(4.25rem+env(safe-area-inset-bottom))] z-30 mx-auto w-full max-w-lg px-3">
        <div className="flex items-center gap-3 rounded-2xl border border-line bg-surface/95 p-2 shadow-lg shadow-black/10 backdrop-blur">
          <button type="button" className="flex min-w-0 flex-1 items-center gap-3 text-left" onClick={onExpand} aria-label={t("expand")}>
            <Artwork src={track.artworkUrl} title={track.title} className="h-12 w-12 shrink-0 rounded-xl" />
            <span className="min-w-0">
              <span className="block truncate text-xs text-muted">{track.podcastTitle}</span>
              <span className="block truncate font-serif text-base leading-5">{track.title}</span>
            </span>
          </button>
          <button
            type="button"
            aria-label={playing ? t("pause") : t("play")}
            onClick={onToggle}
            className="grid h-11 w-11 shrink-0 place-items-center rounded-full bg-accent text-accent-ink"
          >
            {playing ? <PauseIcon /> : <PlayIcon />}
          </button>
        </div>
        <div className="mx-4 mt-1 h-0.5 overflow-hidden rounded-full bg-line">
          <div className="h-full bg-accent" style={{ width: `${progress}%` }} />
        </div>
      </div>
      {expanded ? (
        <div className="fixed inset-0 z-40 mx-auto flex w-full max-w-lg flex-col bg-bg px-5 pb-[calc(1.5rem+env(safe-area-inset-bottom))] pt-6" role="dialog" aria-modal="true">
          <div className="flex justify-end">
            <button type="button" className="text-sm text-muted" onClick={onClose}>
              {t("close")}
            </button>
          </div>
          <Artwork src={track.artworkUrl} title={track.title} className="mx-auto mt-4 aspect-square w-full max-w-sm rounded-[2rem]" />
          <p className="mt-6 text-sm text-muted">{track.podcastTitle}</p>
          <h2 className="font-serif text-3xl leading-tight tracking-tight">{track.title}</h2>
          {track.summary ? <p className="mt-3 line-clamp-3 text-sm leading-6 text-muted">{track.summary}</p> : null}
          <label className="mt-8 block">
            <span className="sr-only">{t("expand")}</span>
            <input
              type="range"
              min={0}
              max={duration || 0}
              step={1}
              value={Math.min(position, duration || 0)}
              onChange={(event) => onSeek(Number(event.target.value))}
              className="w-full"
            />
            <span className="mt-1 flex justify-between text-xs text-muted">
              <span>{formatClock(position)}</span>
              <span>{formatClock(duration)}</span>
            </span>
          </label>
          <div className="mt-4 flex items-center justify-center gap-4">
            <button type="button" aria-label={t("back15")} onClick={() => onSkip(-15)} className="h-12 w-12 rounded-full border border-line text-sm">
              −15
            </button>
            <button type="button" aria-label={playing ? t("pause") : t("play")} onClick={onToggle} className="grid h-16 w-16 place-items-center rounded-full bg-accent text-accent-ink">
              {playing ? <PauseIcon /> : <PlayIcon />}
            </button>
            <button type="button" aria-label={t("forward30")} onClick={() => onSkip(30)} className="h-12 w-12 rounded-full border border-line text-sm">
              +30
            </button>
          </div>
          <div className="mt-6 flex items-center justify-between">
            <button type="button" onClick={onRate} className="rounded-full border border-line px-4 py-2 text-sm" aria-label={t("speed")}>
              {rate.toString().replace(".", ",")}×
            </button>
            <Button variant="ghost" className="h-10" onClick={onPlayed}>
              {t("mark")}
            </Button>
          </div>
        </div>
      ) : null}
    </>
  );
}

function PlayIcon() {
  return (
    <svg viewBox="0 0 24 24" className="h-5 w-5" aria-hidden>
      <path d="M8 5.5v13l11-6.5-11-6.5z" className="fill-current" />
    </svg>
  );
}

function PauseIcon() {
  return (
    <svg viewBox="0 0 24 24" className="h-5 w-5" aria-hidden>
      <path d="M7 5h3.5v14H7zM13.5 5H17v14h-3.5z" className="fill-current" />
    </svg>
  );
}

export function useAppChrome() {
  return usePlayer().track !== null;
}

export function navOffset(hasTrack: boolean) {
  return cx(hasTrack ? "pb-[calc(10.5rem+env(safe-area-inset-bottom))]" : "pb-[calc(6.5rem+env(safe-area-inset-bottom))]");
}
