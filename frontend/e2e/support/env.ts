// E2E テストで使う、ログインした状態（Cookie）の保存先。.gitignore で Git に入れないようにしてある
export const AUTH_FILE = "playwright/.auth/user.json";

// .env.local の値を読む。ない・空なら、何が足りないかを伝えて止める（ログインできないまま進まないように）
export function requireEnv(name: string): string {
  const value = process.env[name];
  if (!value) {
    throw new Error(
      `${name} が設定されていません。frontend/.env.local に書いてください（名前は .env.sample を参照）。`,
    );
  }
  return value;
}
