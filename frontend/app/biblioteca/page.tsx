import type { Metadata } from "next";
import { LibraryScreen } from "@/components/LibraryScreen";

export const metadata: Metadata = { title: "Biblioteca" };

export default function LibraryPage() {
  return <LibraryScreen />;
}
