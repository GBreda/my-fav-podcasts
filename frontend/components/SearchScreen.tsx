"use client";

import { useEffect, useState } from "react";
import { Art } from "@/components/Art";
import { ApiError, api } from "@/lib/api";
import type { SearchHit, SearchResponse } from "@/lib/types";

export function SearchScreen() {
  const [query, setQuery] = useState("");
  const [results, setResults] = useState<SearchHit[]>([]);
  const [searching, setSearching] = useState(false);
  const [followed, setFollowed] = useState<Record<number, boolean>>({});
  const [pending, setPending] = useState<number | null>(null);
  const [banner, setBanner] = useState<string | null>(null);
  const [touched, setTouched] = useState(false);

  useEffect(() => {
    const term = query.trim();
    if (term.length < 2) {
      setResults([]);
      setSearching(false);
      return;
    }
    const controller = new AbortController();
    const timer = setTimeout(() => {
      setSearching(true);
      setTouched(true);
      api<SearchResponse>(`/api/podcasts/search?q=${encodeURIComponent(term)}`)
        .then((response) => {
          if (!controller.signal.aborted) {
            setResults(response.results);
            setBanner(null);
          }
        })
        .catch((error: unknown) => {
          if (controller.signal.aborted) return;
          setResults([]);
          setBanner(error instanceof ApiError ? error.message : "A busca falhou.");
        })
        .finally(() => {
          if (!controller.signal.aborted) setSearching(false);
        });
    }, 350);
    return () => {
      controller.abort();
      clearTimeout(timer);
    };
  }, [query]);

  async function follow(hit: SearchHit) {
    setPending(hit.itunesId);
    setBanner(null);
    try {
      await api("/api/podcasts", {
        method: "POST",
        body: JSON.stringify({ itunesId: hit.itunesId }),
      });
      setFollowed((current) => ({ ...current, [hit.itunesId]: true }));
    } catch (error) {
      setBanner(error instanceof ApiError ? error.message : "Não consegui seguir esse podcast.");
    } finally {
      setPending(null);
    }
  }

  return (
    <>
      <header className="topbar">
        <div>
          <p className="kicker">catálogo</p>
          <h1>buscar</h1>
        </div>
      </header>
      <label className="field">
        <span className="sr-only">Nome do podcast</span>
        <input
          value={query}
          onChange={(event) => setQuery(event.currentTarget.value)}
          placeholder="Qual podcast?"
          enterKeyHint="search"
          autoCapitalize="none"
          autoCorrect="off"
        />
      </label>
      {banner && <p className="banner">{banner}</p>}
      {searching && <p className="note">Procurando…</p>}
      {!searching && query.trim().length < 2 && (
        <div className="empty">
          <h2>Um nome basta.</h2>
          <p>Os episódios de tudo o que você seguir entram na mesma fila.</p>
        </div>
      )}
      {!searching && touched && query.trim().length >= 2 && results.length === 0 && !banner && (
        <div className="empty">
          <h2>Nenhum podcast com esse nome.</h2>
          <p>Tente o título do programa, não o episódio.</p>
        </div>
      )}
      {results.map((hit) => {
        const on = hit.subscribed || followed[hit.itunesId];
        return (
          <article className="result" key={hit.itunesId}>
            <Art src={hit.artworkUrl} title={hit.title} />
            <div>
              <h2 className="result-title">{hit.title}</h2>
              <p className="result-author">{[hit.author, hit.genre].filter(Boolean).join(" · ")}</p>
            </div>
            <button
              className={on ? "follow is-on" : "follow"}
              type="button"
              disabled={on || !hit.feedUrl || pending === hit.itunesId}
              onClick={() => void follow(hit)}
            >
              {!hit.feedUrl ? "Sem feed" : pending === hit.itunesId ? "Entrando…" : on ? "Seguindo" : "Seguir"}
            </button>
          </article>
        );
      })}
    </>
  );
}
