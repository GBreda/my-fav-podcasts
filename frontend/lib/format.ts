export function dayKey(iso: string | null): string {
  if (!iso) return "none";
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return "none";
  return `${date.getFullYear()}-${date.getMonth()}-${date.getDate()}`;
}

export function dayLabel(iso: string | null, now = new Date()): string {
  if (!iso) return "Sem data";
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return "Sem data";
  const start = (value: Date) => new Date(value.getFullYear(), value.getMonth(), value.getDate()).getTime();
  const diff = Math.round((start(now) - start(date)) / 86_400_000);
  if (diff === 0) return "Hoje";
  if (diff === 1) return "Ontem";
  const sameYear = date.getFullYear() === now.getFullYear();
  return date.toLocaleDateString("pt-BR", {
    day: "numeric",
    month: "long",
    ...(sameYear ? {} : { year: "numeric" }),
  });
}

export function formatDuration(seconds: number | null | undefined): string | null {
  if (seconds == null || seconds <= 0) return null;
  const total = Math.round(seconds);
  const hours = Math.floor(total / 3600);
  const minutes = Math.round((total % 3600) / 60);
  if (hours > 0 && minutes === 60) {
    return `${hours + 1} h`;
  }
  if (hours > 0) {
    return minutes > 0 ? `${hours} h ${minutes} min` : `${hours} h`;
  }
  if (minutes > 0) return `${minutes} min`;
  return `${total} s`;
}

export function formatClock(seconds: number): string {
  const total = Math.max(0, Math.floor(seconds || 0));
  const hours = Math.floor(total / 3600);
  const minutes = Math.floor((total % 3600) / 60);
  const secs = total % 60;
  const ss = String(secs).padStart(2, "0");
  if (hours > 0) return `${hours}:${String(minutes).padStart(2, "0")}:${ss}`;
  return `${minutes}:${ss}`;
}

export function progressLabel(duration: number | null, position: number): string | null {
  if (!duration || position <= 5) return formatDuration(duration);
  const left = Math.max(0, Math.round(duration - position));
  if (left < 30) return "quase no fim";
  const text = formatDuration(left) ?? "1 min";
  return text.startsWith("1 ") ? `${text} restante` : `${text} restantes`;
}

export function formatRate(rate: number): string {
  return `${rate.toLocaleString("pt-BR", { maximumFractionDigits: 2 })}×`;
}

export function updatedLabel(iso: string | null): string | null {
  if (!iso) return null;
  const label = dayLabel(iso);
  if (label === "Sem data") return null;
  return `Atualizado ${label.toLowerCase()}`;
}
