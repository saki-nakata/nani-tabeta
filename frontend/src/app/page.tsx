import { redirect } from "next/navigation";
import { LogoutButton } from "@/components/logout-button";
import { searchMe } from "@/lib/api/users";

export default async function Home() {
  const me = await searchMe();
  if (!me) {
    redirect("/signup");
  }

  return (
    <main className="flex flex-1 flex-col items-center justify-center gap-6">
      <p>{me.nickname} さん、こんにちは</p>
      <LogoutButton />
    </main>
  );
}
