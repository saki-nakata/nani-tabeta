"use client";

import { useEffect, useRef, useState } from "react";
import { PhotoPicker, type SelectedPhoto } from "@/components/photo-picker";

export function RecordForm() {
  const [photos, setPhotos] = useState<SelectedPhoto[]>([]);

  // 画面を離れるときに片付けられるよう、今の写真の一覧を控えておく
  const photosRef = useRef(photos);
  useEffect(() => {
    photosRef.current = photos;
  }, [photos]);

  // 画面を離れるときに、残っているプレビュー用の URL を片付ける
  useEffect(
    () => () => {
      photosRef.current.forEach((photo) =>
        URL.revokeObjectURL(photo.previewUrl),
      );
    },
    [],
  );

  return (
    <div className="flex w-full max-w-md flex-col gap-6">
      <PhotoPicker photos={photos} onChange={setPhotos} />
    </div>
  );
}
