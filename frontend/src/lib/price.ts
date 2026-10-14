// 価格の欄の文字を、半角の数字だけにそろえる（全角の数字は半角に直し、数字以外は取り除く。7桁まで）
export function normalizePrice(value: string) {
  return value
    .replace(/[０-９]/g, (zenkaku) =>
      String.fromCharCode(zenkaku.charCodeAt(0) - 0xfee0),
    )
    .replace(/[^0-9]/g, "")
    .slice(0, 7);
}
