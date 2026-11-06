export type User = {
  id: string;
  email: string;
  displayName: string;
};

export type AuthResponse = {
  token: string;
  user: User;
};

export type SearchHit = {
  itunesId: string;
  title: string;
  author: string;
  artworkUrl: string;
  feedUrl: string;
  genre: string;
  followed: boolean;
  podcastId: string | null;
};

export type Episode = {
  id: string;
  title: string;
  summary: string;
  audioUrl: string;
  artworkUrl: string;
  publishedAt: string;
  durationSeconds: number | null;
};

export type Podcast = {
  id: string;
  title: string;
  author: string;
  description: string;
  artworkUrl: string;
  feedUrl: string;
  followed: boolean;
  episodes: Episode[];
};

export type Subscription = {
  id: string;
  title: string;
  author: string;
  artworkUrl: string;
  feedUrl: string;
};

export type QueueEntry = {
  episodeId: string;
  podcastId: string;
  podcastTitle: string;
  title: string;
  summary: string;
  audioUrl: string;
  artworkUrl: string;
  publishedAt: string;
  durationSeconds: number | null;
  positionSeconds: number;
  completed: boolean;
};

export class ApiError extends Error {
  constructor(
    readonly code: string,
    readonly status: number,
  ) {
    super(code);
  }
}

export function apiBase(): string {
  const configured = process.env.NEXT_PUBLIC_API_BASE;
  if (configured === undefined) {
    return "http://localhost:8080";
  }
  return configured.replace(/\/$/, "");
}

export function createApi(token: string | null) {
  async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
    const headers = new Headers(init.headers);
    headers.set("Accept", "application/json");
    if (init.body) {
      headers.set("Content-Type", "application/json");
    }
    if (token) {
      headers.set("Authorization", `Bearer ${token}`);
    }
    let response: Response;
    try {
      response = await fetch(`${apiBase()}${path}`, { ...init, headers });
    } catch {
      throw new ApiError("UNKNOWN", 0);
    }
    if (response.status === 204) {
      return undefined as T;
    }
    const data = (await response.json().catch(() => ({}))) as { code?: string };
    if (!response.ok) {
      throw new ApiError(data.code ?? "UNKNOWN", response.status);
    }
    return data as T;
  }

  return {
    register(body: { email: string; password: string; displayName: string }) {
      return request<AuthResponse>("/api/auth/register", { method: "POST", body: JSON.stringify(body) });
    },
    login(body: { email: string; password: string }) {
      return request<AuthResponse>("/api/auth/login", { method: "POST", body: JSON.stringify(body) });
    },
    changePassword(body: { currentPassword: string; nextPassword: string }) {
      return request<void>("/api/auth/password", { method: "POST", body: JSON.stringify(body) });
    },
    forgotPassword(body: { email: string; locale: string }) {
      return request<{ delivery: "log" | "email" }>("/api/auth/forgot-password", {
        method: "POST",
        body: JSON.stringify(body),
      });
    },
    resetPassword(body: { token: string; password: string }) {
      return request<void>("/api/auth/reset-password", { method: "POST", body: JSON.stringify(body) });
    },
    search(query: string, country: string) {
      const params = new URLSearchParams({ q: query, country });
      return request<{ results: SearchHit[] }>(`/api/catalog/search?${params.toString()}`);
    },
    podcast(id: string) {
      return request<Podcast>(`/api/catalog/podcasts/${id}`);
    },
    follow(body: { feedUrl: string; title?: string; author?: string; artworkUrl?: string }) {
      return request<Podcast>("/api/subscriptions", { method: "POST", body: JSON.stringify(body) });
    },
    unfollow(id: string) {
      return request<void>(`/api/subscriptions/${id}`, { method: "DELETE" });
    },
    subscriptions() {
      return request<{ items: Subscription[] }>("/api/subscriptions");
    },
    queue(filter: "unplayed" | "all", order: "newest" | "oldest") {
      return request<{ entries: QueueEntry[] }>(`/api/queue?filter=${filter}&order=${order}`);
    },
    refresh() {
      return request<{ synced: number; failed: number }>("/api/queue/refresh", { method: "POST" });
    },
    progress(episodeId: string, body: { positionSeconds: number; completed: boolean }) {
      return request<{ positionSeconds: number; completed: boolean }>(
        `/api/queue/episodes/${episodeId}/progress`,
        { method: "POST", body: JSON.stringify(body) },
      );
    },
  };
}

export type Api = ReturnType<typeof createApi>;
