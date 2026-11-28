import type { User } from "@/types/user";
import { ApiError, apiFetch } from "./client";

// 自分（ログインしている人）を調べる
// 未登録（403）のときは null を返す
// allowGuest が true のとき（だれでも見られる画面）は、ログインしていない（401）ときも、ゲストとして null を返す（/signup へは移さない）
// allowGuest が false のとき（ログインが必要な画面）は、ログインしていない（401）ときはエラーを投げる
// それ以外の失敗（500・通信の失敗など）は、どちらでもそのまま投げる（エラーの画面になる）
export async function searchMe({
  allowGuest,
}: {
  allowGuest: boolean;
}): Promise<User | null> {
  try {
    return await apiFetch<User>("/api/users/me");
  } catch (error) {
    if (
      error instanceof ApiError &&
      (error.status === 403 || (allowGuest && error.status === 401))
    ) {
      return null;
    }
    throw error;
  }
}

export function registerUser(nickname: string) {
  return apiFetch<User>("/api/users", { method: "POST", body: { nickname } });
}
