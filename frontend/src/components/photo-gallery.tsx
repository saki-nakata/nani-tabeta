"use client";

import { useState } from "react";
import { cn } from "@/lib/utils";

type PhotoGalleryProps = {
  photoUrls: string[];
  // 写真の説明（例：「からあげクンの写真」）。何枚目かは、この部品が足す
  altText: string;
};

// 写真を 1 枚表示する
function Photo({
  src,
  alt,
  className,
}: {
  src: string;
  alt: string;
  className?: string;
}) {
  return (
    // eslint-disable-next-line @next/next/no-img-element -- 写真は置くときに WebP・長辺 1200px に縮めてあるので、Next.js の Image は使わない
    <img
      src={src}
      alt={alt}
      className={cn("aspect-square w-full object-cover", className)}
    />
  );
}

// 選んだ写真を大きく表示し、その下に全部の写真を小さく並べる（押すと、大きい写真が切り替わる）
export function PhotoGallery({ photoUrls, altText }: PhotoGalleryProps) {
  // 大きく表示している写真が何枚目か（0 が 1 枚目）
  const [selectedIndex, setSelectedIndex] = useState(0);

  return (
    <div className="flex flex-col gap-2">
      <Photo
        src={photoUrls[selectedIndex]}
        alt={
          photoUrls.length > 1
            ? `${altText}（${selectedIndex + 1}枚目）`
            : altText
        }
        className="rounded-lg"
      />
      {photoUrls.length > 1 && (
        <div className="grid grid-cols-5 gap-2">
          {photoUrls.map((photoUrl, index) => (
            <button
              key={photoUrl}
              type="button"
              aria-label={`${index + 1}枚目の写真を表示`}
              aria-pressed={index === selectedIndex}
              onClick={() => setSelectedIndex(index)}
              className={cn(
                "overflow-hidden rounded-md",
                index === selectedIndex && "ring-2 ring-foreground",
              )}
            >
              <Photo src={photoUrl} alt="" />
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
