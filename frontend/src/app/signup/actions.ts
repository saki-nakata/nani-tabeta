"use server";

import { redirect } from "next/navigation";
import { ApiError } from "@/lib/api/client";
import { registerUser } from "@/lib/api/users";

export type SignupState = { message: string } | null;

export async function signup(
  _previousState: SignupState,
  formData: FormData,
): Promise<SignupState> {
  const nickname = String(formData.get("nickname") ?? "");
  try {
    await registerUser(nickname);
  } catch (error) {
    if (error instanceof ApiError && error.status === 400) {
      return { message: error.message };
    }
    if (!(error instanceof ApiError && error.status === 409)) {
      throw error;
    }
  }
  redirect("/");
}
