import { cleanup, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, test, vi } from "vitest";
import type { ShopKind } from "@/types/master";
import type { Shop } from "@/types/shop";
import { ShopPicker, type SelectedShop } from "./shop-picker";

// ここでは、店を選んだあとの表示（店名・業態・「変更」）を確かめる
// 候補を探す処理は、選んだあとも裏で動くので、空の候補を返す偽物にする
const { suggestShops } = vi.hoisted(() => ({ suggestShops: vi.fn() }));
vi.mock("@/app/records/new/actions", () => ({ suggestShops }));

const shopKinds: ShopKind[] = [
  { id: 1, shopKindName: "コンビニ", shopKindEmoji: "🏪" },
  { id: 3, shopKindName: "カフェ", shopKindEmoji: "☕" },
];

const starbucks: Shop = {
  id: 4,
  shopName: "スタバ",
  areaId: 20,
  shopKindId: 3,
  isClosed: false,
};

function renderPicker(
  selectedShop: SelectedShop | null,
  onChange: (selectedShop: SelectedShop | null) => void = vi.fn(),
) {
  return render(
    <ShopPicker
      areaId={20}
      shopKinds={shopKinds}
      selectedShop={selectedShop}
      onChange={onChange}
    />,
  );
}

// 「業態：」の行に、業態の Twemoji の画像（ファイル名）と、業態の名前が出ていること
function expectShopKind(twemojiFileName: string, shopKindName: string) {
  const line = screen.getByText(/業態：/);
  expect(line.textContent).toBe(`業態： ${shopKindName}`);
  expect(line.querySelector("img")?.getAttribute("src")).toBe(
    `/twemoji/17.0.3/${twemojiFileName}.svg`,
  );
}

beforeEach(() => {
  suggestShops.mockResolvedValue([]);
});

afterEach(() => {
  cleanup();
  vi.resetAllMocks();
});

describe("ShopPicker（店を選んだあと）", () => {
  test("登録済みの店を選んだら、店名と、その店の業態を出すこと", () => {
    renderPicker({ type: "registered", shop: starbucks });

    expect(screen.getByText("スタバ")).toBeDefined();
    expectShopKind("2615", "カフェ");
    // 登録済みの店には「（新しい店）」を付けない
    expect(screen.queryByText("（新しい店）")).toBeNull();
  });

  test("新しい店を選んだら、店名・「（新しい店）」と、選んだ業態を出すこと", () => {
    renderPicker({ type: "new", shopName: "ローソン", shopKindId: 1 });

    expect(screen.getByText("ローソン")).toBeDefined();
    expect(screen.getByText("（新しい店）")).toBeDefined();
    expectShopKind("1f3ea", "コンビニ");
  });

  test("店の業態が業態の一覧になければ、業態の行は出さず、店名だけを出すこと", () => {
    renderPicker({
      type: "registered",
      shop: { ...starbucks, shopKindId: 99 },
    });

    expect(screen.getByText("スタバ")).toBeDefined();
    expect(screen.queryByText(/業態：/)).toBeNull();
  });

  test("「変更」を押すと、選んだ店を外す（null を渡す）こと", async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    renderPicker({ type: "registered", shop: starbucks }, onChange);

    await user.click(screen.getByRole("button", { name: "変更" }));

    expect(onChange).toHaveBeenCalledWith(null);
  });
});
