type IconProps = { className?: string };

export function IconPlay({ className }: IconProps) {
  return (
    <svg className={className} viewBox="0 0 24 24" aria-hidden="true">
      <path d="M8 5.2v13.6L19 12 8 5.2Z" fill="currentColor" />
    </svg>
  );
}

export function IconPause({ className }: IconProps) {
  return (
    <svg className={className} viewBox="0 0 24 24" aria-hidden="true">
      <path d="M7 5h3.2v14H7V5Zm6.8 0H17v14h-3.2V5Z" fill="currentColor" />
    </svg>
  );
}

export function IconCheck({ className }: IconProps) {
  return (
    <svg className={className} viewBox="0 0 24 24" aria-hidden="true">
      <path d="M9.2 16.2 4.8 11.8l1.4-1.4 3 3 8-8 1.4 1.4-9.4 9.4Z" fill="currentColor" />
    </svg>
  );
}

export function IconRefresh({ className }: IconProps) {
  return (
    <svg className={className} viewBox="0 0 24 24" aria-hidden="true">
      <path
        d="M19 12a7 7 0 0 1-11.9 5H9v-2H4v5h2v-1.2A9 9 0 0 0 21 12h-2Zm-14 0a7 7 0 0 1 11.9-5H15v2h5V4h-2v1.2A9 9 0 0 0 3 12h2Z"
        fill="currentColor"
      />
    </svg>
  );
}

export function IconQueue({ className }: IconProps) {
  return (
    <svg className={className} viewBox="0 0 24 24" aria-hidden="true">
      <path d="M4 7h10v2H4V7Zm0 4h16v2H4v-2Zm0 4h7v2H4v-2Z" fill="currentColor" />
    </svg>
  );
}

export function IconSearch({ className }: IconProps) {
  return (
    <svg className={className} viewBox="0 0 24 24" aria-hidden="true">
      <path
        d="M10.5 4a6.5 6.5 0 0 1 5.17 10.45l3.94 3.94-1.42 1.42-3.94-3.95A6.5 6.5 0 1 1 10.5 4Zm0 2a4.5 4.5 0 1 0 0 9 4.5 4.5 0 0 0 0-9Z"
        fill="currentColor"
      />
    </svg>
  );
}

export function IconLibrary({ className }: IconProps) {
  return (
    <svg className={className} viewBox="0 0 24 24" aria-hidden="true">
      <path d="M4 4h7v7H4V4Zm9 0h7v7h-7V4ZM4 13h7v7H4v-7Zm9 0h7v7h-7v-7Z" fill="currentColor" />
    </svg>
  );
}

export function IconDown({ className }: IconProps) {
  return (
    <svg className={className} viewBox="0 0 24 24" aria-hidden="true">
      <path d="m6.7 9.2 5.3 5.3 5.3-5.3 1.4 1.4L12 17.3 5.3 10.6l1.4-1.4Z" fill="currentColor" />
    </svg>
  );
}
