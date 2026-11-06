export type ShowRef = {
  id: string;
  title: string;
  artworkUrl: string | null;
};

export type Episode = {
  id: string;
  title: string;
  description: string;
  audioUrl: string;
  artworkUrl: string | null;
  publishedAt: string | null;
  durationSeconds: number | null;
  played: boolean;
  positionSeconds: number;
  podcast: ShowRef;
};

export type QueueResponse = {
  sort: "newest" | "oldest";
  total: number;
  episodes: Episode[];
};

export type PodcastSummary = {
  id: string;
  title: string;
  author: string;
  description: string;
  artworkUrl: string | null;
  feedUrl: string;
  episodeCount: number;
  unplayedCount: number;
  lastRefreshedAt: string | null;
};

export type PodcastDetail = {
  podcast: PodcastSummary;
  episodes: Episode[];
};

export type SearchHit = {
  itunesId: number;
  title: string;
  author: string;
  artworkUrl: string | null;
  feedUrl: string | null;
  genre: string;
  subscribed: boolean;
  podcastId: string | null;
};

export type SearchResponse = {
  results: SearchHit[];
};

export type RefreshResponse = {
  updated: number;
  failed: number;
};

export type SortOrder = "newest" | "oldest";

export type EpisodePatch = {
  id: string;
  played?: boolean;
  positionSeconds?: number;
};
