export function clampSeek(current: number, delta: number, duration: number): number {
  const base = Number.isFinite(current) ? Math.max(0, current) : 0;
  const next = base + delta;
  const atLeastZero = Math.max(0, next);
  if (!Number.isFinite(duration) || duration <= 0) {
    return atLeastZero;
  }
  return Math.min(atLeastZero, duration);
}
