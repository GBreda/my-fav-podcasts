"use client";

import { useTranslations } from "next-intl";
import { createContext, useContext, useEffect, useRef, useState } from "react";
import { useAuth } from "@/lib/auth";
import { formatClock } from "@/lib/format";
import { clampSeek } from "@/lib/seek";
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
  const playGeneration = useRef(0);
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
      setPosition(restored.positionSeconds || 0);
      if (restored.durationSeconds) {
        setDuration(restored.durationSeconds);
      }
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
    setPosition(next.positionSeconds || 0);
    if (next.durationSeconds && next.durationSeconds > 0) {
      setDuration(next.durationSeconds);
    }
    sessionStorage.setItem("fila.now", JSON.stringify(next));
  }

  function syncTimeline(audio: HTMLAudioElement) {
    if (Number.isFinite(audio.duration) && audio.duration > 0) {
      setDuration(audio.duration);
    }
    setPosition(Number.isFinite(audio.currentTime) ? audio.currentTime : 0);
  }

  function play(next: Track) {
    const audio = audioRef.current;
    remember(next);
    setExpanded(false);
    if (!audio) {
      return;
    }
    const generation = ++playGeneration.current;
    const resume = () => {
      if (generation !== playGeneration.current) {
        return;
      }
      const known = Number.isFinite(audio.duration) ? audio.duration : 0;
      if (known > 0) {
        setDuration(known);
      }
      if (next.positionSeconds > 1 && known > 0 && audio.currentTime < 1) {
        const nextPos = Math.min(next.positionSeconds, Math.max(known - 1, 0));
        try {
          audio.currentTime = nextPos;
        } catch {
          return;
        }
        setPosition(nextPos);
      }
    };
    audio.addEventListener("loadedmetadata", resume, { once: true });
    const nextSrc = new URL(next.audioUrl, window.location.href).href;
    if (!(audio.readyState >= 1 && audio.src === nextSrc)) {
      audio.src = next.audioUrl;
    }
    audio.playbackRate = rate;
    if (audio.readyState >= 1) {
      resume();
    }
    audio.play().then(() => {
      if (generation === playGeneration.current) {
        setPlaying(true);
      }
    }).catch(() => {
      if (generation === playGeneration.current) {
        setPlaying(false);
      }
    });
  }

  function dismiss() {
    playGeneration.current += 1;
    void persist(false);
    const audio = audioRef.current;
    setTrack(null);
    trackRef.current = null;
    setPlaying(false);
    setExpanded(false);
    setPosition(0);
    setDuration(0);
    sessionStorage.removeItem("fila.now");
    if (audio) {
      audio.pause();
      audio.removeAttribute("src");
      audio.load();
    }
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
    const known = Number.isFinite(audio.duration) ? audio.duration : 0;
    const next = clampSeek(audio.currentTime || 0, delta, known);
    try {
      audio.currentTime = next;
    } catch {
      return;
    }
    setPosition(next);
  }

  const value: PlayerContextValue = { track, playing, revision, play, toggle };

  return (
    <PlayerContext.Provider value={value}>
      {children}
      <audio
        ref={audioRef}
        preload="none"
        playsInline
        onLoadedMetadata={() => {
          if (audioRef.current) {
            syncTimeline(audioRef.current);
          }
        }}
        onDurationChange={() => {
          if (audioRef.current) {
            syncTimeline(audioRef.current);
          }
        }}
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
          onCollapse={() => setExpanded(false)}
          onDismiss={dismiss}
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
  onCollapse,
  onDismiss,
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
  onCollapse: () => void;
  onDismiss: () => void;
  onSkip: (delta: number) => void;
  onSeek: (value: number) => void;
  onRate: () => void;
  onPlayed: () => void;
}) {
  const t = useTranslations("player");
  const knownDuration = duration > 0 ? duration : track.durationSeconds && track.durationSeconds > 0 ? track.durationSeconds : 0;

  return (
    <>
      <div className="fixed inset-x-0 bottom-[calc(4.25rem+env(safe-area-inset-bottom))] z-30 mx-auto w-full max-w-lg px-3">
        <div className="rounded-2xl border border-line bg-surface/95 p-2 shadow-lg shadow-black/10 backdrop-blur">
          <div className="flex items-center gap-3">
            <button type="button" className="flex min-w-0 flex-1 items-center gap-3 text-left" onClick={onExpand} aria-label={t("expand")}>
              <Artwork src={track.artworkUrl} title={track.title} className="h-12 w-12 shrink-0 rounded-xl" />
              <span className="min-w-0">
                <span className="block truncate text-xs text-muted">{track.podcastTitle}</span>
                <span className="block truncate font-serif text-base leading-5">{track.title}</span>
              </span>
            </button>
            <button
              type="button"
              aria-label={t("close")}
              onClick={onDismiss}
              className="grid h-11 w-11 shrink-0 place-items-center rounded-full text-muted"
            >
              <CloseIcon />
            </button>
            <button
              type="button"
              aria-label={playing ? t("pause") : t("play")}
              onClick={onToggle}
              className="grid h-12 w-12 shrink-0 place-items-center rounded-full bg-accent text-accent-ink"
            >
              {playing ? <PauseIcon /> : <PlayIcon />}
            </button>
          </div>
          <div className="mt-2 flex items-center gap-2 px-1 pb-1">
            <SkipButton label={t("back15")} onClick={() => onSkip(-15)}>
              −15
            </SkipButton>
            <SeekBar position={position} duration={knownDuration} label={t("seek")} onSeek={onSeek} />
            <SkipButton label={t("forward15")} onClick={() => onSkip(15)}>
              +15
            </SkipButton>
          </div>
        </div>
      </div>
      {expanded ? (
        <div className="fixed inset-0 z-40 mx-auto flex w-full max-w-lg flex-col bg-bg" role="dialog" aria-modal="true">
          <div className="flex min-h-0 flex-1 flex-col overflow-y-auto px-5 pt-6">
            <div className="flex items-center justify-between">
              <button type="button" className="text-sm text-muted" onClick={onCollapse}>
                {t("collapse")}
              </button>
              <button
                type="button"
                aria-label={t("close")}
                onClick={onDismiss}
                className="grid h-11 w-11 place-items-center rounded-full text-muted"
              >
                <CloseIcon />
              </button>
            </div>
            <Artwork src={track.artworkUrl} title={track.title} className="mx-auto mt-4 aspect-square w-full max-h-[42vh] max-w-sm rounded-[2rem]" />
            <p className="mt-6 text-sm text-muted">{track.podcastTitle}</p>
            <h2 className="font-serif text-3xl leading-tight tracking-tight">{track.title}</h2>
            {track.summary ? <p className="mt-3 line-clamp-3 text-sm leading-6 text-muted">{track.summary}</p> : null}
          </div>
          <div className="shrink-0 border-t border-line bg-bg px-5 pt-4 pb-[calc(1.25rem+env(safe-area-inset-bottom))]">
            <SeekBar position={position} duration={knownDuration} label={t("seek")} onSeek={onSeek} />
            <div className="mt-3 flex items-center justify-center gap-4">
              <SkipButton label={t("back15")} onClick={() => onSkip(-15)} large>
                −15
              </SkipButton>
              <button type="button" aria-label={playing ? t("pause") : t("play")} onClick={onToggle} className="grid h-16 w-16 place-items-center rounded-full bg-accent text-accent-ink">
                {playing ? <PauseIcon /> : <PlayIcon />}
              </button>
              <SkipButton label={t("forward15")} onClick={() => onSkip(15)} large>
                +15
              </SkipButton>
            </div>
            <div className="mt-5 flex items-center justify-between">
              <button type="button" onClick={onRate} className="rounded-full border border-line px-4 py-2 text-sm" aria-label={t("speed")}>
                {rate.toString().replace(".", ",")}×
              </button>
              <Button variant="ghost" className="h-10" onClick={onPlayed}>
                {t("mark")}
              </Button>
            </div>
          </div>
        </div>
      ) : null}
    </>
  );
}

function SeekBar({
  position,
  duration,
  label,
  onSeek,
}: {
  position: number;
  duration: number;
  label: string;
  onSeek: (value: number) => void;
}) {
  const max = duration > 0 ? duration : 0;
  const value = max > 0 ? Math.min(Math.max(0, position), max) : 0;
  const progress = max > 0 ? Math.min(100, (value / max) * 100) : 0;

  return (
    <label className="block min-w-0 flex-1">
      <span className="sr-only">{label}</span>
      <input
        type="range"
        min={0}
        max={max > 0 ? max : 1}
        step={1}
        value={value}
        aria-valuemin={0}
        aria-valuemax={Math.round(max)}
        aria-valuenow={Math.round(value)}
        aria-valuetext={`${formatClock(value)} / ${formatClock(max)}`}
        onChange={(event) => {
          if (max > 0) {
            onSeek(Number(event.target.value));
          }
        }}
        className="seek"
        style={{ ["--seek" as string]: `${progress}%` }}
      />
      <span className="flex justify-between text-xs font-medium tabular-nums text-ink">
        <span>{formatClock(value)}</span>
        <span>{formatClock(max)}</span>
      </span>
    </label>
  );
}

function SkipButton({
  label,
  onClick,
  large = false,
  children,
}: {
  label: string;
  onClick: () => void;
  large?: boolean;
  children: React.ReactNode;
}) {
  return (
    <button
      type="button"
      aria-label={label}
      onClick={onClick}
      className={cx(
        "grid shrink-0 place-items-center rounded-full border border-line bg-surface text-sm font-semibold text-ink",
        large ? "h-14 w-14" : "h-12 w-12",
      )}
    >
      {children}
    </button>
  );
}

function CloseIcon() {
  return (
    <svg viewBox="0 0 24 24" className="h-5 w-5" aria-hidden>
      <path d="M6 6l12 12M18 6L6 18" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" />
    </svg>
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
  return cx(hasTrack ? "pb-[calc(13.5rem+env(safe-area-inset-bottom))]" : "pb-[calc(6.5rem+env(safe-area-inset-bottom))]");
}
