import type { Metadata } from "next";
import {
  Geist_Mono,
  Shippori_Mincho,
  Zen_Kaku_Gothic_New,
} from "next/font/google";
import { ThemeProvider } from "@/components/theme-provider";
import "./globals.css";

// 本文の文字（プロトタイプと同じ Zen Kaku Gothic New）
const zenKakuGothicNew = Zen_Kaku_Gothic_New({
  variable: "--font-zen-kaku-gothic-new",
  weight: ["400", "500", "700"],
  subsets: ["latin"],
});

// アプリ名・見出しの文字（プロトタイプと同じ Shippori Mincho）
const shipporiMincho = Shippori_Mincho({
  variable: "--font-shippori-mincho",
  weight: ["500", "700"],
  subsets: ["latin"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

export const metadata: Metadata = {
  title: "今日なに食べた？",
  description: "食べたものを写真で記録するアプリ",
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    // next-themes が表示の前に <html> の class（dark）を書きかえるため、ここだけ違いの警告を出さない
    <html
      lang="ja"
      className={`${zenKakuGothicNew.variable} ${shipporiMincho.variable} ${geistMono.variable} h-full antialiased`}
      suppressHydrationWarning
    >
      <body className="flex min-h-full flex-col">
        {/* 端末の設定（ライト／ダーク）に合わせて、<html> に dark のクラスを付ける */}
        <ThemeProvider
          attribute="class"
          defaultTheme="system"
          enableSystem
          disableTransitionOnChange
        >
          {children}
        </ThemeProvider>
      </body>
    </html>
  );
}
