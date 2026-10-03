"use client";

import { useActionState } from "react";
import { signup } from "@/app/signup/actions";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";

export function SignupForm() {
  const [state, formAction, pending] = useActionState(signup, null);

  return (
    <form action={formAction} className="flex w-full max-w-xs flex-col gap-4">
      <div className="flex flex-col gap-2">
        <Label htmlFor="nickname">ニックネーム</Label>
        <Input id="nickname" name="nickname" required maxLength={30} />
      </div>
      {state && <p className="text-sm text-destructive">{state.message}</p>}
      <Button type="submit" disabled={pending}>
        {pending ? "登録中…" : "はじめる"}
      </Button>
    </form>
  );
}
