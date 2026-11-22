// 画面のいちばん下の帯（使っている素材の帰属表示）
// 分類・業態の絵文字は Twemoji の画像（CC-BY 4.0）を使っているので、その表示を出す
export function AppFooter() {
  return (
    <footer className="border-t">
      <p className="mx-auto w-full max-w-5xl px-4 pt-4 pb-[max(1rem,env(safe-area-inset-bottom))] text-xs text-muted-foreground">
        絵文字：
        <a
          href="https://github.com/jdecked/twemoji"
          target="_blank"
          rel="noopener noreferrer"
          className="underline underline-offset-2 hover:text-foreground"
        >
          Twemoji
        </a>
        （
        <a
          href="https://creativecommons.org/licenses/by/4.0/"
          target="_blank"
          rel="noopener noreferrer"
          className="underline underline-offset-2 hover:text-foreground"
        >
          CC-BY 4.0
        </a>
        ）
      </p>
    </footer>
  );
}
