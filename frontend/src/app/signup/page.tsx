import { redirect } from "next/navigation";
import { SignupForm } from "@/components/signup-form";
import { searchMe } from "@/lib/api/users";

export default async function SignupPage() {
  const me = await searchMe();
  if (me) {
    redirect("/");
  }

  return (
    <main className="flex flex-1 flex-col items-center justify-center gap-6 px-4">
      <h1 className="text-2xl font-bold">ニックネームを決めてください</h1>
      <p className="text-sm text-muted-foreground">
        記録やコメントに表示される名前です。
      </p>
      <SignupForm />
    </main>
  );
}
