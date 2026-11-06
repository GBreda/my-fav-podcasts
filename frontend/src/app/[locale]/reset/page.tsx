import { Suspense } from "react";
import { ResetForm } from "./form";

export default function ResetPage() {
  return (
    <Suspense fallback={<div className="grid min-h-dvh place-items-center font-serif text-4xl italic">fila</div>}>
      <ResetForm />
    </Suspense>
  );
}
