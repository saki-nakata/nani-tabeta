// "2026-11-03" の形の日付の、前の日を同じ形で返す（「これまで」の欄の、昨日までの範囲に使う）
// 月の初め・年の初め・うるう年でも正しく出せるよう、日付として計算する
// Date.UTC（世界共通の時刻）で作るので、動かしているサーバーのタイムゾーンで日付がずれない
export function dayBefore(date: string): string {
  const [year, month, day] = date.split("-").map(Number);
  // JavaScript の月は 0 から数えるので month - 1。日が 0 になると、前の月の最後の日にくり上がる
  return new Date(Date.UTC(year, month - 1, day - 1))
    .toISOString()
    .slice(0, 10);
}
