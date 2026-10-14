// 前回選んだエリアを覚えておく Cookie の名前
export const LAST_AREA_COOKIE = "lastAreaId";

// 選んだエリアを、ブラウザの Cookie に1年間覚えておく（ブラウザで動く部品から呼ぶ）
export function saveLastAreaId(areaId: number) {
  document.cookie = `${LAST_AREA_COOKIE}=${areaId}; path=/; max-age=31536000; samesite=lax`;
}
