import { NextResponse, type NextRequest } from "next/server";
import { createClient } from "@/lib/supabase/server";

export async function GET(request: NextRequest) {
  const code = request.nextUrl.searchParams.get("code");
  if (code) {
    const supabase = await createClient();
    const { error } = await supabase.auth.exchangeCodeForSession(code);
    if (!error) {
      return redirectWithoutCache(new URL("/", request.url));
    }
  }
  return redirectWithoutCache(new URL("/login?error=auth", request.url));
}

// 入館証を含む返事を、途中のサーバーに保存させない
function redirectWithoutCache(url: URL) {
  const response = NextResponse.redirect(url);
  response.headers.set("Cache-Control", "private, no-store");
  return response;
}
