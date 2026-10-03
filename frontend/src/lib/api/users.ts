import type { User } from "@/types/user";
import { ApiError, apiFetch } from "./client";

export async function searchMe(): Promise<User | null> {
  try {
    return await apiFetch<User>("/api/users/me");
  } catch (error) {
    if (error instanceof ApiError && error.status === 403) {
      return null;
    }
    throw error;
  }
}

export function registerUser(nickname: string) {
  return apiFetch<User>("/api/users", { method: "POST", body: { nickname } });
}
