import { LoginButton } from "@/components/login-button";

export default async function LoginPage({ searchParams }: PageProps<"/login">) {
  const { error } = await searchParams;

  return (
    <main className="flex flex-1 flex-col items-center justify-center gap-6">
      <h1 className="text-2xl font-bold">今日なに食べた？</h1>
      {error && (
        <p className="text-sm text-destructive">
          ログインに失敗しました。もう一度お試しください。
        </p>
      )}
      <LoginButton />
    </main>
  );
}
