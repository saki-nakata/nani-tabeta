import { createServerClient } from "@supabase/ssr";
import { NextResponse, type NextRequest } from "next/server";

export async function proxy(request: NextRequest) {
  let response = NextResponse.next({ request });
  let cacheHeaders: Record<string, string> = {};

  const supabase = createServerClient(
    process.env.NEXT_PUBLIC_SUPABASE_URL!,
    process.env.NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY!,
    {
      cookies: {
        getAll() {
          return request.cookies.getAll();
        },
        setAll(cookiesToSet, headers) {
          // このあと動くページにも、新しい JWT を見せる
          cookiesToSet.forEach(({ name, value }) =>
            request.cookies.set(name, value),
          );
          response = NextResponse.next({ request });
          // ブラウザにも、新しい JWT を保存させる
          cookiesToSet.forEach(({ name, value, options }) =>
            response.cookies.set(name, value, options),
          );
          // JWT の入ったレスポンスを、途中のサーバーに保存（キャッシュ）させない
          cacheHeaders = headers;
          Object.entries(headers).forEach(([key, value]) =>
            response.headers.set(key, value),
          );
        },
      },
    },
  );

  // JWT を検証する。期限が近ければ、ここで新しい JWT になり setAll が呼ばれる
  const { data } = await supabase.auth.getClaims();

  const path = request.nextUrl.pathname;
  const isPublic = path === "/login" || path.startsWith("/auth/");
  if (!data && !isPublic) {
    const url = request.nextUrl.clone();
    url.pathname = "/login";
    const redirectResponse = NextResponse.redirect(url);
    // response に入れた Cookie（削除の指示も含む）とキャッシュのヘッダーを、移動の返事にも移す
    response.cookies
      .getAll()
      .forEach((cookie) => redirectResponse.cookies.set(cookie));
    Object.entries(cacheHeaders).forEach(([key, value]) =>
      redirectResponse.headers.set(key, value),
    );
    return redirectResponse;
  }
  return response;
}

export const config = {
  matcher: [
    "/((?!_next/static|_next/image|favicon.ico|.*\\.(?:svg|png|jpg|jpeg|gif|webp)$).*)",
  ],
};
