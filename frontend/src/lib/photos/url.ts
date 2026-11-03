import { PHOTO_BUCKET } from "@/lib/photos/bucket";
import { createClient } from "@/lib/supabase/server";

// 写真の場所（パス）から、画面に表示するための公開 URL を作る（photos バケットは公開設定）
export async function getPhotoUrls(paths: string[]): Promise<string[]> {
  const supabase = await createClient();
  return paths.map(
    (path) =>
      supabase.storage.from(PHOTO_BUCKET).getPublicUrl(path).data.publicUrl,
  );
}

// 写真の場所（パス）から公開 URL を作る。写真がない（null）ところは null のまま返す（一覧の代表写真に使う）
export async function getPhotoUrlsOrNull(
  paths: (string | null)[],
): Promise<(string | null)[]> {
  const supabase = await createClient();
  const bucket = supabase.storage.from(PHOTO_BUCKET);
  return paths.map((path) =>
    path === null ? null : bucket.getPublicUrl(path).data.publicUrl,
  );
}
