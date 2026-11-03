import Link from "next/link";
import { LogoutButton } from "@/components/logout-button";
import { Button } from "@/components/ui/button";

// 画面のいちばん上の帯（アプリ名・記録するボタン・ログアウト）。プロトタイプのアプリバー
export function AppHeader() {
  return (
    <header className="border-b">
      <div className="mx-auto flex w-full max-w-5xl items-center justify-between gap-3 px-4 py-3">
        <Link href="/" className="font-heading text-lg font-bold">
          今日なに食べた？
        </Link>
        <div className="flex items-center gap-2">
          <Button asChild>
            <Link href="/records/new">＋ 記録する</Link>
          </Button>
          <LogoutButton />
        </div>
      </div>
    </header>
  );
}
