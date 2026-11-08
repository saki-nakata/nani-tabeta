import { createClient } from "@/lib/supabase/server";
import { PHOTO_BUCKET } from "@/lib/photos/bucket";

// 記録を消したあとに、その写真を Storage から消す（Server Action から呼ぶ。ログイン中の本人の権限で消す）
// 失敗しても投げない。消せたか確認できなかった写真は、あとで回収できるようにサーバーのログに残す
export async function deletePhotosOnServer(paths: string[]): Promise<void> {
  if (paths.length === 0) {
    return;
  }
  try {
    const supabase = await createClient();
    const { data, error } = await supabase.storage
      .from(PHOTO_BUCKET)
      .remove(paths);
    const deleted = new Set(data?.map((deletedPhoto) => deletedPhoto.name));
    const remaining = paths.filter((path) => !deleted.has(path));
    if (remaining.length > 0) {
      console.error("写真を Storage から削除できたか確認できませんでした。", {
        remaining,
        error,
      });
    }
  } catch (error) {
    console.error("写真を Storage から削除できたか確認できませんでした。", {
      remaining: paths,
      error,
    });
  }
}
