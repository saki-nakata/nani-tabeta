// 店名・商品名を、バックエンド（NameNormalizer）と同じ形に整える
// 全角スペースを半角に → 続くスペースを 1 つに → 前後の空白を取る
export function normalizeName(name: string) {
  return name
    .replace(/\u3000/g, " ")
    .replace(/ +/g, " ")
    .trim();
}
