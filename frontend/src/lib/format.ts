export function formatClock(total: number): string {
  if (!Number.isFinite(total) || total < 0) {
    total = 0;
  }
  const seconds = Math.floor(total);
  const hours = Math.floor(seconds / 3600);
  const minutes = Math.floor((seconds % 3600) / 60);
  const rest = seconds % 60;
  const paddedMinutes = String(minutes).padStart(2, "0");
  const paddedSeconds = String(rest).padStart(2, "0");
  if (hours > 0) {
    return `${hours}:${paddedMinutes}:${paddedSeconds}`;
  }
  return `${minutes}:${paddedSeconds}`;
}

export function formatDurationLabel(
  seconds: number | null,
  units: { hour: string; minute: string },
): string {
  if (!seconds || seconds <= 0) {
    return "";
  }
  const hours = Math.floor(seconds / 3600);
  const minutes = Math.round((seconds % 3600) / 60);
  if (hours > 0 && minutes > 0) {
    return `${hours} ${units.hour} ${minutes} ${units.minute}`;
  }
  if (hours > 0) {
    return `${hours} ${units.hour}`;
  }
  return `${Math.max(minutes, 1)} ${units.minute}`;
}

export function formatRelative(iso: string, locale: string, now = Date.now()): string {
  const then = new Date(iso).getTime();
  if (Number.isNaN(then)) {
    return "";
  }
  const diffSeconds = Math.round((then - now) / 1000);
  const formatter = new Intl.RelativeTimeFormat(locale, { numeric: "auto" });
  const absolute = Math.abs(diffSeconds);
  if (absolute < 60) {
    return formatter.format(diffSeconds, "second");
  }
  if (absolute < 3600) {
    return formatter.format(Math.round(diffSeconds / 60), "minute");
  }
  if (absolute < 86_400) {
    return formatter.format(Math.round(diffSeconds / 3600), "hour");
  }
  if (absolute < 86_400 * 30) {
    return formatter.format(Math.round(diffSeconds / 86_400), "day");
  }
  return new Intl.DateTimeFormat(locale, { day: "numeric", month: "short" }).format(then);
}

export function greetingKey(hour: number): "morning" | "afternoon" | "evening" {
  if (hour < 12) {
    return "morning";
  }
  if (hour < 18) {
    return "afternoon";
  }
  return "evening";
}
