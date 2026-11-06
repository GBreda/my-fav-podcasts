package com.gbreda.fila.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "episodes",
        uniqueConstraints = @UniqueConstraint(name = "uk_episode_guid", columnNames = {"podcast_id", "guid"}),
        indexes = @Index(name = "idx_episode_queue", columnList = "played, published_at"))
public class Episode {

    @Id
    private UUID id = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "podcast_id", nullable = false)
    private Podcast podcast;

    @Column(nullable = false, length = 2048)
    private String guid;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(length = 4000)
    private String description;

    @Column(nullable = false, length = 2000)
    private String audioUrl;

    @Column(length = 1500)
    private String artworkUrl;

    private Instant publishedAt;

    private Integer durationSeconds;

    @Column(nullable = false)
    private boolean played;

    @Column(nullable = false)
    private int positionSeconds;

    private Instant playedAt;

    public UUID getId() {
        return id;
    }

    public Podcast getPodcast() {
        return podcast;
    }

    public void setPodcast(Podcast podcast) {
        this.podcast = podcast;
    }

    public String getGuid() {
        return guid;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAudioUrl() {
        return audioUrl;
    }

    public void setAudioUrl(String audioUrl) {
        this.audioUrl = audioUrl;
    }

    public String getArtworkUrl() {
        return artworkUrl;
    }

    public void setArtworkUrl(String artworkUrl) {
        this.artworkUrl = artworkUrl;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public boolean isPlayed() {
        return played;
    }

    public int getPositionSeconds() {
        return positionSeconds;
    }

    public void setPositionSeconds(int positionSeconds) {
        this.positionSeconds = Math.max(0, positionSeconds);
    }

    public Instant getPlayedAt() {
        return playedAt;
    }

    public void applyPlayed(boolean played) {
        if (played && !this.played) {
            this.playedAt = Instant.now();
            if (durationSeconds != null && durationSeconds > 0) {
                this.positionSeconds = durationSeconds;
            }
        }
        if (!played && this.played) {
            this.playedAt = null;
            if (durationSeconds != null && positionSeconds >= Math.max(0, durationSeconds - 1)) {
                this.positionSeconds = 0;
            }
        }
        this.played = played;
    }
}
