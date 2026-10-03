"use client";

import { useEffect } from "react";
import { Button } from "@/components/ui/button";

export default function ErrorPage({
  error,
  retry,
}: {
  error: Error & { digest?: string };
  retry: () => void;
}) {
  useEffect(() => {
    console.error(error);
  }, [error]);

  return (
    <main className="flex flex-1 flex-col items-center justify-center gap-6 px-4">
      <p>うまく処理できませんでした。時間をおいて、もう一度お試しください。</p>
      <Button onClick={() => retry()}>もう一度試す</Button>
    </main>
  );
}
