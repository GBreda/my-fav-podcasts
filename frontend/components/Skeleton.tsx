export function SkeletonList() {
  return (
    <div className="skeleton" aria-hidden="true">
      {Array.from({ length: 5 }, (_, index) => (
        <div className="skeleton-row" key={index}>
          <div className="skeleton-block" />
          <div>
            <div className="skeleton-line short" />
            <div className="skeleton-line" />
            <div className="skeleton-line short" />
          </div>
        </div>
      ))}
    </div>
  );
}
