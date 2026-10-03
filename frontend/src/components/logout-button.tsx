"use client";

import { useRouter } from "next/navigation";
import { Button } from "@/components/ui/button";
import { createClient } from "@/lib/supabase/client";

export function LogoutButton() {
  const router = useRouter();

  async function signOut() {
    const supabase = createClient();
    await supabase.auth.signOut({ scope: "local" });
    router.push("/login");
  }

  return (
    <Button variant="outline" onClick={signOut}>
      ログアウト
    </Button>
  );
}
