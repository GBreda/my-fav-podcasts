"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useRef,
  useState,
} from "react";
import { Art } from "@/components/Art";
import {
  IconCheck,
  IconDown,
  IconLibrary,
  IconPause,
  IconPlay,
  IconQueue,
  IconSearch,
} from "@/components/Icons";
import { api } from "@/lib/api";
import { formatClock, formatRate } from "@/lib/format";
import type { Episode, EpisodePatch } from "@/lib/types";

const RATES = [1, 1.25, 1.5, 1.75, 2];

type Listener = (patch: EpisodePatch) => void;

type PlayerApi = {
  episode: Episode | null;
  playing: boolean;
  position: number;
  duration: number;
  rate: number;
  expanded: boolean;
  followQueue: boolean;
  error: string | null;
  play: (episode: Episode, queue: Episode[]) => void;
  toggle: () => void;
  seek: (seconds: number) => void;
  skip: (delta: number) => void;
  cycleRate: () => void;
  setExpanded: (value: boolean) => void;
  setFollowQueue: (value: boolean) => void;
  markPlayed: (episode: Episode) => Promise<void>;
  markUnplayed: (episode: Episode) => Promise<void>;
  registerQueue: (queue: Episode[]) => void;
  subscribe: (listener: Listener) => () => void;
};

const PlayerContext = createContext<PlayerApi | null>(null);

export function usePlayer() {
  const value = useContext(PlayerContext);
  if (!value) throw new Error("Player indisponível");
  return value;
}

async function persist(id: string, positionSeconds: number, played?: boolean) {
  await api(`/api/episodes/${id}`, {
    method: "PATCH",
    body: JSON.stringify({
      positionSeconds,
      ...(played == null ? {} : { played }),
    }),
  });
}

export function PlayerProvider({ children }: { children: React.ReactNode }) {
  const audioRef = useRef<HTMLAudioElement | null>(null);
  const queueRef = useRef<Episode[]>([]);
  const episodeRef = useRef<Episode | null>(null);
  const followRef = useRef(true);
  const rateRef = useRef(1);
  const listeners = useRef(new Set<Listener>());
  const lastSaved = useRef(0);
  const playRef = useRef<(episode: Episode, queue: Episode[]) => void>(() => {});
  const finishRef = useRef<() => void>(() => {});
  const suppressSave = useRef(false);
  const skipSaveFor = useRef<string | null>(null);

  const [episode, setEpisode] = useState<Episode | null>(null);
  const [playing, setPlaying] = useState(false);
  const [position, setPosition] = useState(0);
  const [duration, setDuration] = useState(0);
  const [rate, setRate] = useState(1);
  const [expanded, setExpanded] = useState(false);
  const [followQueue, setFollowQueueState] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [hydrated, setHydrated] = useState(false);

  useEffect(() => {
    episodeRef.current = episode;
  }, [episode]);

  useEffect(() => {
    const storedRate = Number(localStorage.getItem("fila.rate"));
    if (RATES.includes(storedRate)) {
      setRate(storedRate);
      rateRef.current = storedRate;
    }
    if (localStorage.getItem("fila.follow") === "0") {
      setFollowQueueState(false);
      followRef.current = false;
    }
    setHydrated(true);
  }, []);

  useEffect(() => {
    if (!hydrated) return;
    localStorage.setItem("fila.rate", String(rate));
    rateRef.current = rate;
    if (audioRef.current) audioRef.current.playbackRate = rate;
  }, [rate, hydrated]);

  useEffect(() => {
    if (!hydrated) return;
    localStorage.setItem("fila.follow", followQueue ? "1" : "0");
    followRef.current = followQueue;
  }, [followQueue, hydrated]);

  const notify = useCallback((patch: EpisodePatch) => {
    listeners.current.forEach((listener) => listener(patch));
  }, []);

  const play = useCallback((next: Episode, queue: Episode[]) => {
    const audio = audioRef.current;
    if (!audio) return;
    queueRef.current = queue;
    setError(null);
    if (episodeRef.current?.id === next.id) {
      if (audio.paused) {
        void audio.play().catch(() => setError("Não consegui reproduzir este áudio."));
      } else {
        audio.pause();
      }
      return;
    }
    const previous = episodeRef.current;
    if (previous && previous.id !== skipSaveFor.current && audio.currentTime > 0) {
      const positionSeconds = Math.floor(audio.currentTime);
      void persist(previous.id, positionSeconds).catch(() => {});
      notify({ id: previous.id, positionSeconds });
    }
    skipSaveFor.current = null;
    episodeRef.current = next;
    setEpisode(next);
    setPosition(next.positionSeconds || 0);
    setDuration(next.durationSeconds || 0);
    suppressSave.current = true;
    audio.src = next.audioUrl;
    audio.playbackRate = rateRef.current;
    const start = next.positionSeconds || 0;
    const seekToStart = () => {
      if (start > 0 && (!Number.isFinite(audio.duration) || start < audio.duration - 1)) {
        audio.currentTime = start;
        setPosition(start);
      }
      audio.removeEventListener("loadedmetadata", seekToStart);
    };
    audio.addEventListener("loadedmetadata", seekToStart);
    void audio.play()
      .catch(() => setError("Não consegui reproduzir este áudio."))
      .finally(() => {
        suppressSave.current = false;
      });
  }, [notify]);

  const finishCurrent = useCallback(() => {
    const current = episodeRef.current;
    const audio = audioRef.current;
    if (!current || !audio) return;
    const queue = queueRef.current;
    const index = queue.findIndex((item) => item.id === current.id);
    const next = queue.slice(index + 1).find((item) => !item.played && item.id !== current.id) ?? null;
    const positionSeconds = Math.floor(audio.currentTime || current.durationSeconds || 0);
    void persist(current.id, positionSeconds, true)
      .catch(() => {})
      .finally(() => {
        notify({ id: current.id, played: true, positionSeconds });
        if (followRef.current && next) {
          playRef.current(next, queue);
        } else {
          setPlaying(false);
        }
      });
  }, [notify]);

  useEffect(() => {
    playRef.current = play;
    finishRef.current = finishCurrent;
  }, [play, finishCurrent]);

  useEffect(() => {
    const audio = new Audio();
    audio.preload = "metadata";
    audioRef.current = audio;
    const onTime = () => {
      setPosition(audio.currentTime || 0);
      const current = episodeRef.current;
      if (!current || audio.paused) return;
      const now = Date.now();
      if (now - lastSaved.current < 5000) return;
      lastSaved.current = now;
      const positionSeconds = Math.floor(audio.currentTime);
      void persist(current.id, positionSeconds).catch(() => {});
      notify({ id: current.id, positionSeconds });
    };
    const onDuration = () => {
      if (Number.isFinite(audio.duration)) setDuration(audio.duration);
    };
    const onPlay = () => setPlaying(true);
    const onPause = () => {
      setPlaying(false);
      if (suppressSave.current) return;
      const current = episodeRef.current;
      if (!current || !audio.currentTime) return;
      const positionSeconds = Math.floor(audio.currentTime);
      void persist(current.id, positionSeconds).catch(() => {});
      notify({ id: current.id, positionSeconds });
    };
    const onEnded = () => finishRef.current();
    const onError = () => {
      if (episodeRef.current && audio.src) setError("Não consegui reproduzir este áudio.");
    };
    audio.addEventListener("timeupdate", onTime);
    audio.addEventListener("loadedmetadata", onDuration);
    audio.addEventListener("durationchange", onDuration);
    audio.addEventListener("play", onPlay);
    audio.addEventListener("pause", onPause);
    audio.addEventListener("ended", onEnded);
    audio.addEventListener("error", onError);
    return () => {
      audio.pause();
      audio.src = "";
      audio.removeEventListener("timeupdate", onTime);
      audio.removeEventListener("loadedmetadata", onDuration);
      audio.removeEventListener("durationchange", onDuration);
      audio.removeEventListener("play", onPlay);
      audio.removeEventListener("pause", onPause);
      audio.removeEventListener("ended", onEnded);
      audio.removeEventListener("error", onError);
    };
  }, [notify]);

  const toggle = useCallback(() => {
    const audio = audioRef.current;
    if (!audio || !episodeRef.current) return;
    if (audio.paused) {
      void audio.play().catch(() => setError("Não consegui reproduzir este áudio."));
    } else {
      audio.pause();
    }
  }, []);

  const seek = useCallback((seconds: number) => {
    const audio = audioRef.current;
    if (!audio) return;
    const limit = Number.isFinite(audio.duration) ? audio.duration : seconds;
    const next = Math.min(Math.max(0, seconds), limit);
    audio.currentTime = next;
    setPosition(next);
  }, []);

  const skip = useCallback((delta: number) => {
    const audio = audioRef.current;
    if (!audio) return;
    const limit = Number.isFinite(audio.duration) ? audio.duration : audio.currentTime + delta;
    const next = Math.min(Math.max(0, audio.currentTime + delta), limit);
    audio.currentTime = next;
    setPosition(next);
  }, []);

  const cycleRate = useCallback(() => {
    setRate((current) => RATES[(RATES.indexOf(current) + 1) % RATES.length] ?? 1);
  }, []);

  const markPlayed = useCallback(async (target: Episode) => {
    const queue = queueRef.current;
    const index = queue.findIndex((item) => item.id === target.id);
    const next = queue.slice(Math.max(index, 0) + (index >= 0 ? 1 : 0)).find((item) => !item.played && item.id !== target.id) ?? null;
    const positionSeconds = Math.floor(
      episodeRef.current?.id === target.id ? audioRef.current?.currentTime || target.durationSeconds || 0 : target.durationSeconds || 0,
    );
    await persist(target.id, positionSeconds, true);
    notify({ id: target.id, played: true, positionSeconds });
    if (episodeRef.current?.id !== target.id) return;
    skipSaveFor.current = target.id;
    if (followRef.current && next) {
      playRef.current(next, queue);
      return;
    }
    audioRef.current?.pause();
  }, [notify]);

  const markUnplayed = useCallback(async (target: Episode) => {
    await persist(target.id, 0, false);
    notify({ id: target.id, played: false, positionSeconds: 0 });
    if (episodeRef.current?.id === target.id) {
      setEpisode({ ...target, played: false, positionSeconds: 0 });
    }
  }, [notify]);

  const registerQueue = useCallback((queue: Episode[]) => {
    queueRef.current = queue;
  }, []);

  const subscribe = useCallback((listener: Listener) => {
    listeners.current.add(listener);
    return () => listeners.current.delete(listener);
  }, []);

  useEffect(() => {
    const onKey = (event: KeyboardEvent) => {
      if (event.code !== "Space") return;
      const target = event.target as HTMLElement | null;
      if (target && (target.tagName === "INPUT" || target.tagName === "TEXTAREA" || target.isContentEditable)) return;
      if (!episodeRef.current) return;
      event.preventDefault();
      toggle();
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [toggle]);

  useEffect(() => {
    if (!episode || !("mediaSession" in navigator)) return;
    const artwork = episode.artworkUrl
      ? [{ src: episode.artworkUrl, sizes: "512x512", type: "image/jpeg" }]
      : [];
    navigator.mediaSession.metadata = new MediaMetadata({
      title: episode.title,
      artist: episode.podcast.title,
      artwork,
    });
    navigator.mediaSession.playbackState = playing ? "playing" : "paused";
    try {
      navigator.mediaSession.setActionHandler("play", () => void audioRef.current?.play());
      navigator.mediaSession.setActionHandler("pause", () => audioRef.current?.pause());
      navigator.mediaSession.setActionHandler("seekbackward", () => skip(-15));
      navigator.mediaSession.setActionHandler("seekforward", () => skip(30));
    } catch {
      // Some browsers reject handlers they don't support.
    }
  }, [episode, playing, skip]);

  useEffect(() => {
    if (!expanded) return;
    const previous = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    return () => {
      document.body.style.overflow = previous;
    };
  }, [expanded]);

  const value: PlayerApi = {
    episode,
    playing,
    position,
    duration,
    rate,
    expanded,
    followQueue,
    error,
    play,
    toggle,
    seek,
    skip,
    cycleRate,
    setExpanded,
    setFollowQueue: setFollowQueueState,
    markPlayed,
    markUnplayed,
    registerQueue,
    subscribe,
  };

  return (
    <PlayerContext.Provider value={value}>
      <div className="stage">
        <aside className="stage-copy">
          <p className="stage-brand">fila</p>
          <p className="stage-lead">Vários podcasts. Uma fila só, na ordem em que foram lançados.</p>
        </aside>
        <div className={`app-shell${episode ? " has-player" : ""}${expanded ? " is-expanded" : ""}`}>
          {children}
          {episode && <MiniPlayer />}
          <TabBar />
          {expanded && episode && <FullPlayer />}
        </div>
      </div>
    </PlayerContext.Provider>
  );
}

function MiniPlayer() {
  const { episode, playing, position, duration, toggle, setExpanded } = usePlayer();
  if (!episode) return null;
  const pct = duration > 0 ? Math.min(100, (position / duration) * 100) : 0;
  return (
    <div className="mini">
      <span className="mini-progress" style={{ width: `${pct}%` }} />
      <button className="mini-open" type="button" onClick={() => setExpanded(true)}>
        <Art src={episode.artworkUrl} title={episode.title} />
        <span>
          <strong>{episode.title}</strong>
          <small>{episode.podcast.title}</small>
        </span>
      </button>
      <button className="mini-toggle" type="button" onClick={toggle} aria-label={playing ? "Pausar" : "Tocar"}>
        {playing ? <IconPause /> : <IconPlay />}
      </button>
    </div>
  );
}

function FullPlayer() {
  const player = usePlayer();
  const episode = player.episode;
  const [marking, setMarking] = useState(false);
  if (!episode) return null;
  const shown = player.position;
  const limit = player.duration || episode.durationSeconds || 0;

  return (
    <div className="sheet" role="dialog" aria-modal="true" aria-label="Reproduzindo">
      <div className="sheet-inner">
        <button className="sheet-close" type="button" onClick={() => player.setExpanded(false)}>
          <IconDown />
          <span className="sr-only">Fechar player</span>
        </button>
        <Art className="sheet-art" src={episode.artworkUrl} title={episode.podcast.title} />
        <Link
          className="sheet-show"
          href={`/podcast?id=${episode.podcast.id}`}
          onClick={() => player.setExpanded(false)}
        >
          {episode.podcast.title}
        </Link>
        <h2 className="sheet-title">{episode.title}</h2>
        <div className="scrub">
          <input
            type="range"
            min={0}
            max={limit || 0}
            step={1}
            value={Math.min(shown, limit || shown)}
            aria-label="Posição do episódio"
            onChange={(event) => player.seek(Number(event.currentTarget.value))}
          />
          <div className="scrub-times">
            <span>{formatClock(shown)}</span>
            <span>{formatClock(limit)}</span>
          </div>
        </div>
        <div className="transport">
          <button className="transport-skip" type="button" onClick={() => player.skip(-15)} aria-label="Voltar 15 segundos">
            −15
          </button>
          <button className="transport-play" type="button" onClick={player.toggle} aria-label={player.playing ? "Pausar" : "Tocar"}>
            {player.playing ? <IconPause /> : <IconPlay />}
          </button>
          <button className="transport-skip" type="button" onClick={() => player.skip(30)} aria-label="Avançar 30 segundos">
            +30
          </button>
        </div>
        <div className="sheet-actions">
          <button className="pill" type="button" onClick={player.cycleRate} aria-label="Velocidade">
            {formatRate(player.rate)}
          </button>
          <button
            className="pill"
            type="button"
            aria-pressed={player.followQueue}
            onClick={() => player.setFollowQueue(!player.followQueue)}
          >
            Tocar o próximo
          </button>
        </div>
        <button
          className="text-btn mark"
          type="button"
          disabled={marking}
          onClick={() => {
            setMarking(true);
            void player.markPlayed(episode).finally(() => setMarking(false));
          }}
        >
          <IconCheck /> Marcar como ouvido
        </button>
        {player.error && <p className="banner">{player.error}</p>}
        {episode.description && (
          <section className="notes">
            <h3>Notas</h3>
            <p>{episode.description}</p>
          </section>
        )}
      </div>
    </div>
  );
}

function TabBar() {
  const path = usePathname();
  const items = [
    { href: "/", label: "Fila", icon: <IconQueue />, current: path === "/" },
    { href: "/buscar", label: "Buscar", icon: <IconSearch />, current: path.startsWith("/buscar") },
    {
      href: "/biblioteca",
      label: "Biblioteca",
      icon: <IconLibrary />,
      current: path.startsWith("/biblioteca") || path.startsWith("/podcast"),
    },
  ];
  return (
    <nav className="tabbar" aria-label="Principal">
      {items.map((item) => (
        <Link key={item.href} href={item.href} className="tab" aria-current={item.current ? "page" : undefined}>
          {item.icon}
          {item.label}
        </Link>
      ))}
    </nav>
  );
}
