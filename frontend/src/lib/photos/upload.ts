import { createClient } from "@/lib/supabase/client";
import { compressToWebp } from "@/lib/photos/compress";

const BUCKET = "photos";

export type PhotoFolder = "records" | "shops";

// 置き場所ごとの、長い辺の最大の長さ（店のメニュー写真は文字を読むため大きめ）
const MAX_SIDE: Record<PhotoFolder, number> = { records: 1200, shops: 1600 };
// 1枚の大きさの上限（Supabase のバケットの設定 1MB と合わせる）
const MAX_FILE_SIZE = 1024 * 1024;

// 写真を自分のフォルダに置いて、置いた場所（パス）を返す
export async function uploadPhoto(
  photo: Blob,
  folder: PhotoFolder,
): Promise<string> {
  if (photo.size > MAX_FILE_SIZE) {
    throw new Error(
      "写真の容量が大きすぎてアップロードできませんでした。別の写真を選んでください。",
    );
  }

  const supabase = createClient();
  const { data } = await supabase.auth.getClaims();
  if (!data) {
    throw new Error("ログインしていません。");
  }

  const path = `${data.claims.sub}/${folder}/${crypto.randomUUID()}.webp`;
  const { error } = await supabase.storage
    .from(BUCKET)
    .upload(path, photo, { contentType: "image/webp" });

  if (error) {
    throw new Error("写真をアップロードできませんでした。");
  }
  return path;
}

// 選んだ写真を1枚ずつ小さくして置き、置いた場所を選んだ順に返す。途中で失敗したら、置いた分を消してから投げ直す
export async function uploadPhotos(
  files: File[],
  folder: PhotoFolder,
): Promise<string[]> {
  const paths: string[] = [];
  try {
    for (const file of files) {
      const photo = await compressToWebp(file, MAX_SIDE[folder]);
      paths.push(await uploadPhoto(photo, folder));
    }
    return paths;
  } catch (error) {
    await deletePhotos(paths);
    throw error;
  }
}

// 置いた写真を消す（記録の登録に失敗したときの後片付け）。失敗しても投げず、消せたか確認できなかったものは記録に残す
export async function deletePhotos(paths: string[]): Promise<void> {
  if (paths.length === 0) {
    return;
  }
  try {
    const supabase = createClient();
    const { data, error } = await supabase.storage.from(BUCKET).remove(paths);
    const deleted = new Set(data?.map((deletedPhoto) => deletedPhoto.name));
    const remaining = paths.filter((path) => !deleted.has(path));
    if (remaining.length > 0) {
      console.warn("写真の削除を確認できませんでした。", remaining, error);
    }
  } catch (error) {
    console.warn("写真の削除を確認できませんでした。", paths, error);
  }
}
