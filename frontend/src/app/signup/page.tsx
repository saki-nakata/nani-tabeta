import { redirect } from "next/navigation";
import { SignupForm } from "@/components/signup-form";
import { searchMe } from "@/lib/api/users";

export default async function SignupPage() {
  const me = await searchMe({ allowGuest: false });
  if (me) {
    redirect("/");
  }

  return (
    <main className="flex flex-1 flex-col items-center justify-center gap-6 px-4">
      <h1 className="text-2xl font-bold">ニックネームを決めてください</h1>
      <div className="flex max-w-md flex-col gap-1 text-center text-sm">
        <p className="text-muted-foreground">
          記録やコメントに表示される名前です。
        </p>
        {/* 記録と記録した人のニックネームは、ログインしていない人も見られる（タイムライン・記録の詳細）ので、名前を決める前に伝える。読み飛ばされないよう、うすい色にしない */}
        <p>
          記録とニックネームは、ログインしていない人も含めて、だれでも見られます。
        </p>
      </div>
      <SignupForm />
    </main>
  );
}
