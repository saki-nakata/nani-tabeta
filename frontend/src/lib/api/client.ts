import { createClient } from "@/lib/supabase/server";

type ApiOptions = {
  method?: "GET" | "POST" | "PUT" | "DELETE";
  body?: unknown;
};

export class ApiError extends Error {
  readonly status: number;

  constructor(status: number, message: string) {
    super(message);
    this.status = status;
  }
}

// ログインした人だけが使える API を呼ぶ（JWT がなければ、呼ばずに 401 のエラーにする）
export async function apiFetch<T>(
  path: string,
  options: ApiOptions = {},
): Promise<T> {
  const supabase = await createClient();
  const { data } = await supabase.auth.getSession();
  const token = data.session?.access_token;
  if (!token) {
    throw new ApiError(401, "ログインしていません。");
  }

  const response = await fetch(`${process.env.API_BASE_URL}${path}`, {
    method: options.method ?? "GET",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
    },
    body: options.body === undefined ? undefined : JSON.stringify(options.body),
    cache: "no-store",
  });
  return readJson<T>(response);
}

// ログインしていなくても呼べる GET の API（バックエンドの SecurityConfig で permitAll にしたもの）
const PUBLIC_GET_PATHS = [
  /^\/api\/records\/\d+$/,
  /^\/api\/areas$/,
  /^\/api\/categories$/,
  /^\/api\/shop-kinds$/,
];

export type PublicApiResult<T> = {
  data: T;
  // ログインしていない人として読んだか（JWT を付けずに呼んだ、または JWT が無効で外して読み直した）
  isGuest: boolean;
};

// ログインしていなくても見られる画面のために、GET の API を呼ぶ
// ログインしていれば JWT を付ける。JWT が無効（期限切れなど）で 401 が返ったときだけ、JWT を外して 1 回だけ読み直す
export async function publicApiFetch<T>(
  path: string,
): Promise<PublicApiResult<T>> {
  if (!PUBLIC_GET_PATHS.some((pattern) => pattern.test(path))) {
    throw new Error(`ログインしていなくても呼べる API ではありません：${path}`);
  }
  const supabase = await createClient();
  const { data } = await supabase.auth.getSession();
  const token = data.session?.access_token;

  if (token) {
    const response = await getFromApi(path, token);
    // 401 以外（成功・404・403・500 など）は、読み直さずにそのまま結果かエラーにする
    if (response.status !== 401) {
      return { data: await readJson<T>(response), isGuest: false };
    }
  }
  // ログインしていない、または JWT が無効だった：JWT を付けずに 1 回だけ読む（失敗したら、そのエラーで終わる）
  const response = await getFromApi(path, null);
  return { data: await readJson<T>(response), isGuest: true };
}

// GET で API を呼ぶ（token が null なら JWT を付けない）
function getFromApi(path: string, token: string | null) {
  return fetch(`${process.env.API_BASE_URL}${path}`, {
    method: "GET",
    headers: token === null ? {} : { Authorization: `Bearer ${token}` },
    cache: "no-store",
  });
}

// API の返事を読む（失敗のときは、バックエンドのメッセージとステータスを持つ ApiError を投げる）
async function readJson<T>(response: Response): Promise<T> {
  if (!response.ok) {
    const error = await response.json().catch(() => ({}));
    throw new ApiError(
      response.status,
      error.message ?? `API の呼び出しに失敗しました（${response.status}）`,
    );
  }
  return response.json();
}
