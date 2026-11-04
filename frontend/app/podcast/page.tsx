import type { Metadata } from "next";
import { Suspense } from "react";
import { PodcastScreen } from "@/components/PodcastScreen";
import { SkeletonList } from "@/components/Skeleton";

export const metadata: Metadata = { title: "Episódios" };

export default function PodcastPage() {
  return (
    <Suspense fallback={<SkeletonList />}>
      <PodcastScreen />
    </Suspense>
  );
}
