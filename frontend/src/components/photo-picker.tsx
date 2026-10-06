"use client";

import { ImagePlusIcon, XIcon } from "lucide-react";
import type { ChangeEvent } from "react";
import { Button, buttonVariants } from "@/components/ui/button";

export const MAX_PHOTOS = 5;

export type SelectedPhoto = {
  file: File;
  previewUrl: string;
};

type PhotoPickerProps = {
  photos: SelectedPhoto[];
  onChange: (photos: SelectedPhoto[]) => void;
};

export function PhotoPicker({ photos, onChange }: PhotoPickerProps) {
  function addPhotos(event: ChangeEvent<HTMLInputElement>) {
    const selected = Array.from(event.target.files ?? [])
      .slice(0, MAX_PHOTOS - photos.length)
      .map((file) => ({ file, previewUrl: URL.createObjectURL(file) }));
    onChange([...photos, ...selected]);
    event.target.value = "";
  }

  function removePhoto(index: number) {
    URL.revokeObjectURL(photos[index].previewUrl);
    onChange(photos.filter((_, i) => i !== index));
  }

  return (
    <div className="flex flex-col gap-3">
      <div className="grid grid-cols-3 gap-2">
        {photos.map((photo, index) => (
          <div key={photo.previewUrl} className="relative">
            {/* eslint-disable-next-line @next/next/no-img-element -- 選んだ直後の写真（一時的な URL）は next/image で最適化できないため */}
            <img
              src={photo.previewUrl}
              alt={`選んだ写真 ${index + 1}枚目`}
              className="aspect-square w-full rounded-md object-cover"
            />
            <Button
              type="button"
              variant="secondary"
              size="icon-sm"
              className="absolute top-1 right-1"
              aria-label={`${index + 1}枚目の写真を外す`}
              onClick={() => removePhoto(index)}
            >
              <XIcon />
            </Button>
          </div>
        ))}
      </div>
      {photos.length < MAX_PHOTOS && (
        <label className={buttonVariants({ variant: "outline" })}>
          <ImagePlusIcon />
          写真を選ぶ
          <input
            type="file"
            accept="image/*"
            multiple
            className="sr-only"
            onChange={addPhotos}
          />
        </label>
      )}
      <p className="text-sm text-muted-foreground">
        {photos.length} / {MAX_PHOTOS} 枚
      </p>
    </div>
  );
}
