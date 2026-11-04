"use client";

import { useEffect, useState } from "react";

function tone(title: string) {
  let hash = 0;
  for (const char of title) hash = (hash * 31 + char.charCodeAt(0)) >>> 0;
  const hue = 16 + (hash % 32);
  return `linear-gradient(160deg, hsl(${hue} 42% 32%), #1a1612 72%)`;
}

export function Art({
  src,
  title,
  className = "",
}: {
  src?: string | null;
  title: string;
  className?: string;
}) {
  const [failed, setFailed] = useState(false);
  useEffect(() => setFailed(false), [src]);
  const letter = title.trim().charAt(0).toUpperCase() || "•";

  if (!src || failed) {
    return (
      <span className={`art fallback ${className}`} style={{ background: tone(title) }} aria-hidden="true">
        {letter}
      </span>
    );
  }

  return <img className={`art ${className}`} src={src} alt="" onError={() => setFailed(true)} />;
}
