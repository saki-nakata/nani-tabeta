"use client";

import { StarIcon } from "lucide-react";
import { cn } from "@/lib/utils";

const STARS = [1, 2, 3, 4, 5];

type StarRatingProps = {
  value: number;
  onChange: (value: number) => void;
};

export function StarRating({ value, onChange }: StarRatingProps) {
  return (
    <div className="flex gap-1">
      {STARS.map((star) => (
        <button
          key={star}
          type="button"
          aria-label={`${star}点`}
          aria-pressed={star === value}
          className="rounded-md p-1"
          onClick={() => onChange(star)}
        >
          <StarIcon
            className={cn(
              "size-7",
              star <= value
                ? "fill-amber-400 text-amber-400"
                : "text-muted-foreground",
            )}
          />
        </button>
      ))}
    </div>
  );
}
