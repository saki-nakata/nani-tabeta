"use client";

import {
  closestCenter,
  DndContext,
  KeyboardSensor,
  MouseSensor,
  TouchSensor,
  useSensor,
  useSensors,
  type Announcements,
  type DragEndEvent,
  type UniqueIdentifier,
} from "@dnd-kit/core";
import {
  arrayMove,
  rectSortingStrategy,
  SortableContext,
  sortableKeyboardCoordinates,
  useSortable,
} from "@dnd-kit/sortable";
import { CSS } from "@dnd-kit/utilities";
import { ImagePlusIcon, XIcon } from "lucide-react";
import { useId, type ChangeEvent } from "react";
import { Button, buttonVariants } from "@/components/ui/button";
import { cn } from "@/lib/utils";

export const MAX_PHOTOS = 5;

// 写真の欄に並ぶ 1 枚
// saved：保存済みの写真（編集のときの今の写真。Storage のパスと、表示用の URL を持つ）
// new：新しく選んだ写真（まだ上げていないファイルと、プレビュー用の一時的な URL を持つ）
// id は、並べかえで写真を見分けるための名前（saved はパス、new はプレビュー用の URL。どちらも重ならない）
export type SelectedPhoto =
  | { type: "saved"; id: string; path: string; url: string }
  | { type: "new"; id: string; file: File; previewUrl: string };

// 画面に表示する URL（保存済みは公開 URL、新しい写真はプレビュー用の URL）
function photoSrc(photo: SelectedPhoto) {
  return photo.type === "saved" ? photo.url : photo.previewUrl;
}

// キーボードで並べかえるときの説明（読み上げソフトが、写真に移ったときに読む）
const SCREEN_READER_INSTRUCTIONS =
  "写真を並べかえるには、スペースキーでつかみ、矢印キーで動かして、もう一度スペースキーで置きます。Esc キーで取りやめます。";

type PhotoPickerProps = {
  photos: SelectedPhoto[];
  onChange: (photos: SelectedPhoto[]) => void;
};

export function PhotoPicker({ photos, onChange }: PhotoPickerProps) {
  // dnd-kit が読み上げ用に振る名前を、サーバーとブラウザでそろえる（そろわないと、操作の説明が読まれない）
  const dndContextId = useId();

  // つかみ方：マウスは 5px 動かしたら、指は 0.25 秒長押ししたら、キーボードはスペースキーで
  // （押しただけの操作や、画面のスクロールと区別するため）
  const sensors = useSensors(
    useSensor(MouseSensor, { activationConstraint: { distance: 5 } }),
    useSensor(TouchSensor, {
      activationConstraint: { delay: 250, tolerance: 5 },
    }),
    useSensor(KeyboardSensor, {
      coordinateGetter: sortableKeyboardCoordinates,
    }),
  );

  // 写真の名前（id）から、何枚目かを返す（読み上げの案内に使う）
  function positionOf(id: UniqueIdentifier) {
    return photos.findIndex((photo) => photo.id === id) + 1;
  }

  // 読み上げの案内（dnd-kit の決まりの案内は英語なので、日本語にする）
  const announcements: Announcements = {
    onDragStart({ active }) {
      return `${positionOf(active.id)}枚目の写真をつかみました。`;
    },
    onDragOver({ active, over }) {
      return over
        ? `${positionOf(over.id)}枚目の位置です。`
        : `${positionOf(active.id)}枚目の写真は、置ける場所の外にあります。`;
    },
    onDragEnd({ active, over }) {
      return over
        ? `${positionOf(active.id)}枚目の写真を、${positionOf(over.id)}枚目に置きました。`
        : `${positionOf(active.id)}枚目の写真を、元の位置に戻しました。`;
    },
    onDragCancel({ active }) {
      return `並べかえを取りやめました。${positionOf(active.id)}枚目の写真は元の位置のままです。`;
    },
  };

  function addPhotos(event: ChangeEvent<HTMLInputElement>) {
    const selected = Array.from(event.target.files ?? [])
      .slice(0, MAX_PHOTOS - photos.length)
      .map((file): SelectedPhoto => {
        const previewUrl = URL.createObjectURL(file);
        return { type: "new", id: previewUrl, file, previewUrl };
      });
    onChange([...photos, ...selected]);
    event.target.value = "";
  }

  function removePhoto(index: number) {
    const photo = photos[index];
    // 新しい写真だけ、プレビュー用の URL を片付ける（保存済みの写真は Storage の URL なので片付けるものがない）
    if (photo.type === "new") {
      URL.revokeObjectURL(photo.previewUrl);
    }
    onChange(photos.filter((_, i) => i !== index));
  }

  // 写真を置いたとき：つかんだ写真を、置いた場所の写真の位置へ動かす
  function handleDragEnd({ active, over }: DragEndEvent) {
    if (over === null || active.id === over.id) {
      return;
    }
    const from = photos.findIndex((photo) => photo.id === active.id);
    const to = photos.findIndex((photo) => photo.id === over.id);
    onChange(arrayMove(photos, from, to));
  }

  return (
    <div className="flex flex-col gap-3">
      <DndContext
        id={dndContextId}
        sensors={sensors}
        collisionDetection={closestCenter}
        onDragEnd={handleDragEnd}
        accessibility={{
          announcements,
          screenReaderInstructions: { draggable: SCREEN_READER_INSTRUCTIONS },
        }}
      >
        <SortableContext
          items={photos.map((photo) => photo.id)}
          strategy={rectSortingStrategy}
        >
          <div className="grid grid-cols-3 gap-2">
            {photos.map((photo, index) => (
              <SortablePhoto
                key={photo.id}
                photo={photo}
                index={index}
                onRemove={() => removePhoto(index)}
              />
            ))}
          </div>
        </SortableContext>
      </DndContext>
      {photos.length >= 2 && (
        <p className="text-xs text-muted-foreground">
          写真をドラッグ（スマホは長押し）すると、並べかえられます。先頭の写真が代表写真になります。
        </p>
      )}
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

type SortablePhotoProps = {
  photo: SelectedPhoto;
  index: number;
  onRemove: () => void;
};

// 並べかえられる写真 1 枚（写真そのものをつかむ。右上の × で外す）
function SortablePhoto({ photo, index, onRemove }: SortablePhotoProps) {
  const {
    attributes,
    listeners,
    setNodeRef,
    setActivatorNodeRef,
    transform,
    transition,
    isDragging,
  } = useSortable({
    id: photo.id,
    // 読み上げで「sortable」と英語で読まれないようにする
    attributes: { roleDescription: "並べかえできる写真" },
  });

  return (
    <div
      ref={setNodeRef}
      style={{ transform: CSS.Transform.toString(transform), transition }}
      className={cn("relative", isDragging && "z-10 opacity-80")}
    >
      {/* 写真そのものがつかむ所（マウスは押して動かす、指は長押し、キーボードはスペース） */}
      <button
        type="button"
        ref={setActivatorNodeRef}
        {...attributes}
        {...listeners}
        aria-label={`${index + 1}枚目の写真を動かす`}
        className="block w-full cursor-grab touch-manipulation rounded-md outline-none select-none [-webkit-touch-callout:none] focus-visible:ring-3 focus-visible:ring-ring/50 active:cursor-grabbing"
      >
        {/* eslint-disable-next-line @next/next/no-img-element -- 選んだ直後の写真（一時的な URL）は next/image で最適化できないため */}
        <img
          src={photoSrc(photo)}
          alt={`写真 ${index + 1}枚目`}
          draggable={false}
          className="aspect-square w-full rounded-md object-cover"
        />
      </button>
      {index === 0 && (
        <span className="pointer-events-none absolute top-1 left-1 rounded-md bg-primary px-1.5 py-0.5 text-xs text-primary-foreground">
          代表
        </span>
      )}
      <Button
        type="button"
        variant="secondary"
        size="icon-xs"
        className="absolute top-1 right-1"
        aria-label={`${index + 1}枚目の写真を外す`}
        onClick={onRemove}
      >
        <XIcon />
      </Button>
    </div>
  );
}
