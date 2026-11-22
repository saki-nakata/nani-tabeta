"use client";

import twemoji from "@twemoji/api";
import { useState } from "react";
import { cn } from "@/lib/utils";

// 同梱した Twemoji の画像の場所（public/twemoji/17.0.3/。@twemoji/api の版とそろえる）
const TWEMOJI_PATH = "/twemoji/17.0.3";

// 絵文字を、画像のファイル名にする（例：☕ → 2615、🍽️ → 1f37d）
// @twemoji/api の中と同じ決まり：ZWJ（U+200D）を含まない絵文字は、異体字の印（U+FE0F）を外してから番号にする
export function twemojiFileName(emoji: string) {
  const text = emoji.includes("\u200D") ? emoji : emoji.replace(/\uFE0F/g, "");
  return twemoji.convert.toCodePoint(text);
}

type TwemojiImageProps = {
  // DB に入っている絵文字（☕ など）
  emoji: string;
  className?: string;
};

// 絵文字を Twemoji の画像で出す（どの端末でも同じ絵にするため）
// 名前（「カフェ」など）で意味を伝えている表示か、もともと飾りとして読み上げない表示で使うので、絵文字そのものは読み上げない
// 画像を読めないとき（版にない新しい絵文字など）は、文字の絵文字に戻して出す
export function TwemojiImage({ emoji, className }: TwemojiImageProps) {
  const twemojiSrc = `${TWEMOJI_PATH}/${twemojiFileName(emoji)}.svg`;
  // 最後に読めなかった画像の URL（1 件だけ覚える。別の絵文字に変わったら、URL が変わるので、その画像はもう一度読みにいく）
  const [errorTwemojiSrc, setErrorTwemojiSrc] = useState<string | null>(null);

  if (errorTwemojiSrc === twemojiSrc) {
    return (
      <span aria-hidden="true" className={className}>
        {emoji}
      </span>
    );
  }

  return (
    // eslint-disable-next-line @next/next/no-img-element -- 小さな SVG の絵文字で、Next.js の Image で最適化する必要がないため
    <img
      src={twemojiSrc}
      alt=""
      draggable={false}
      onError={() => setErrorTwemojiSrc(twemojiSrc)}
      className={cn("inline-block size-[1em] align-[-0.125em]", className)}
    />
  );
}
