import Link from "next/link";
import { LogoutButton } from "@/components/logout-button";
import { Button } from "@/components/ui/button";

type AppHeaderProps = {
  // ゲストか（ログインしていない、未登録、またはゲストとして読み直した）
  isGuest: boolean;
};

// 画面のいちばん上の帯（アプリ名と、右側のボタン）。プロトタイプのアプリバー
// 登録ずみの人には「記録する」「ログアウト」、ゲストには「ログイン・登録」を出す
export function AppHeader({ isGuest }: AppHeaderProps) {
  return (
    <header className="border-b">
      <div className="mx-auto flex w-full max-w-5xl items-center justify-between gap-3 px-4 py-3">
        <Link href="/" className="font-heading text-lg font-bold">
          今日なに食べた？
        </Link>
        <div className="flex items-center gap-2">
          {isGuest ? (
            // トップ（/）へ行くと、ログインしていない人は /login へ、未登録の人は /signup へ移る
            <Button asChild>
              <Link href="/">ログイン・登録</Link>
            </Button>
          ) : (
            <>
              <Button asChild>
                <Link href="/records/new">＋ 記録する</Link>
              </Button>
              <LogoutButton />
            </>
          )}
        </div>
      </div>
    </header>
  );
}
