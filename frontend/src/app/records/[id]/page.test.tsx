import { cleanup, render, screen } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, test, vi } from "vitest";
import type { RecordDetail } from "@/types/record";
import type { User } from "@/types/user";
import RecordPage from "./page";

// 記録の詳細の画面（サーバーで作る部品）を、ログインの状態ごとに作って、表示を確かめる
// API を呼ぶ関数は偽物にして、「だれが・どう読んだか」をテストごとに決める
const { searchMe, findPublicRecord, notFound, redirect } = vi.hoisted(() => ({
  searchMe: vi.fn(),
  findPublicRecord: vi.fn(),
  // 本物の notFound は、例外を投げて画面の処理を止める
  notFound: vi.fn(() => {
    throw new Error("NEXT_NOT_FOUND");
  }),
  redirect: vi.fn(),
}));
vi.mock("@/lib/api/users", () => ({ searchMe }));
vi.mock("@/lib/api/records", () => ({ findPublicRecord }));
vi.mock("@/lib/api/masters", () => ({
  searchRegionDetails: async () => [
    {
      region: { id: 3, regionName: "中部" },
      areaList: [{ id: 20, regionId: 3, areaName: "長野県" }],
    },
  ],
  searchCategories: async () => [
    { id: 4, categoryName: "肉・揚げ物", categoryEmoji: "🥩" },
  ],
}));
vi.mock("@/lib/photos/url", () => ({ getPhotoUrls: async () => [] }));
vi.mock("next/navigation", () => ({ notFound, redirect }));
// 削除のボタンは Server Action を使うので、ここでは「出ているか」だけがわかる偽物にする
vi.mock("@/components/logout-button", () => ({
  LogoutButton: () => <button type="button">ログアウト</button>,
}));
vi.mock("@/components/delete-record-button", () => ({
  DeleteRecordButton: () => <button type="button">削除</button>,
}));

const SAKI_ID = "20b622c8-dd31-46e8-8f1a-64009c7febe5";
const TEST2_ID = "fa92b95a-22dd-4309-8f76-e53072d5d04c";

const saki: User = {
  id: SAKI_ID,
  nickname: "saki",
  bio: null,
  profilePhotoUrl: null,
  role: "user",
};

// Saki の記録（記録した人のニックネームは saki）
const sakisRecord: RecordDetail = {
  record: {
    id: 12,
    userId: SAKI_ID,
    itemId: 7,
    eatenOn: "2026-11-20",
    customNote: null,
    review: null,
    rating: 4,
    price: null,
    version: 0,
  },
  item: {
    id: 7,
    shopId: 4,
    itemName: "からあげクン",
    categoryId: 4,
    isSeasonal: false,
  },
  shop: {
    id: 4,
    shopName: "ローソン",
    areaId: 20,
    shopKindId: 1,
    isClosed: false,
  },
  photoPaths: [],
  nickname: "saki",
};

// test2 の記録
const test2sRecord: RecordDetail = {
  ...sakisRecord,
  record: { ...sakisRecord.record, id: 13, userId: TEST2_ID },
  nickname: "test2",
};

// 画面を作って表示する（/records/{id} を開いたのと同じ）
async function renderPage(id = "12") {
  const page = await RecordPage({ params: Promise.resolve({ id }) });
  render(page);
}

// 「自分の記録」の札と、編集・削除のボタンが出ていること
function expectMyRecord() {
  expect(screen.getByText("自分の記録")).toBeDefined();
  expect(screen.queryByText(/記録した人：/)).toBeNull();
  expect(screen.getByRole("link", { name: "編集" })).toBeDefined();
  expect(screen.getByRole("button", { name: "削除" })).toBeDefined();
}

// 「記録した人：○○」が出ていて、「自分の記録」の札と、編集・削除のボタンが出ていないこと
function expectSomeoneElsesRecord(nickname: string) {
  expect(screen.getByText(`記録した人：${nickname}`)).toBeDefined();
  expect(screen.queryByText("自分の記録")).toBeNull();
  expect(screen.queryByRole("link", { name: "編集" })).toBeNull();
  expect(screen.queryByRole("button", { name: "削除" })).toBeNull();
}

// ヘッダーが、登録ずみの人向け（記録する）になっていること
function expectHeaderForMember() {
  expect(screen.getByRole("link", { name: "＋ 記録する" })).toBeDefined();
  expect(screen.queryByRole("link", { name: "ログイン・登録" })).toBeNull();
}

// ヘッダーが、ゲスト向け（ログイン・登録）になっていること
function expectHeaderForGuest() {
  expect(screen.getByRole("link", { name: "ログイン・登録" })).toBeDefined();
  expect(screen.queryByRole("link", { name: "＋ 記録する" })).toBeNull();
}

beforeEach(() => {
  findPublicRecord.mockResolvedValue({ data: sakisRecord, isGuest: false });
});

afterEach(() => {
  cleanup();
  vi.clearAllMocks();
});

describe("記録の詳細の画面", () => {
  test("ゲストを許す呼び方で自分を調べ、ログインしていなくても見られる呼び方で記録を読むこと", async () => {
    searchMe.mockResolvedValue(saki);

    await renderPage("12");

    expect(searchMe).toHaveBeenCalledWith({ allowGuest: true });
    expect(findPublicRecord).toHaveBeenCalledWith(12);
  });

  test("登録ずみの本人が自分の記録を見ると、「自分の記録」の札と、編集・削除が出ること", async () => {
    searchMe.mockResolvedValue(saki);

    await renderPage();

    expectMyRecord();
    expectHeaderForMember();
    expect(
      screen.getByRole("link", { name: "編集" }).getAttribute("href"),
    ).toBe("/records/12/edit");
  });

  test("登録ずみの人がほかの人の記録を見ると、記録した人のニックネームが出て、編集・削除は出ないこと", async () => {
    searchMe.mockResolvedValue(saki);
    findPublicRecord.mockResolvedValue({ data: test2sRecord, isGuest: false });

    await renderPage("13");

    expectSomeoneElsesRecord("test2");
    // ほかの人の記録を見ていても、登録ずみの人のヘッダーのまま
    expectHeaderForMember();
  });

  test("ログインしていない人（ゲスト）が見ると、自分の記録でもニックネームが出て、編集・削除は出ないこと", async () => {
    // ログアウトした Saki が、自分の記録を開いた場合。だれが見ているかわからないので、ほかの人と同じ表示
    searchMe.mockResolvedValue(null);
    findPublicRecord.mockResolvedValue({ data: sakisRecord, isGuest: true });

    await renderPage();

    expectSomeoneElsesRecord("saki");
    expectHeaderForGuest();
  });

  test("ログインしているが未登録の人が見ると、/signup へ移さずに記録を見せ、編集・削除は出ないこと", async () => {
    searchMe.mockResolvedValue(null);

    await renderPage();

    expect(redirect).not.toHaveBeenCalled();
    expect(screen.getByText("からあげクン")).toBeDefined();
    expectSomeoneElsesRecord("saki");
    expectHeaderForGuest();
  });

  test("自分の記録でも、ゲストとして読み直した場合（JWT が無効だったなど）は、編集・削除を出さないこと", async () => {
    searchMe.mockResolvedValue(saki);
    findPublicRecord.mockResolvedValue({ data: sakisRecord, isGuest: true });

    await renderPage();

    expectSomeoneElsesRecord("saki");
    // 読み直した結果は、ヘッダーにも反映する（ブラウザではログインしていても、ゲストとして扱う）
    expectHeaderForGuest();
  });

  test("記録が見つからなければ、見つからないの画面にすること", async () => {
    searchMe.mockResolvedValue(null);
    findPublicRecord.mockResolvedValue(null);

    await expect(renderPage("999")).rejects.toThrow("NEXT_NOT_FOUND");
  });

  test("自分を調べるのに失敗したら（500 など）、ゲストとして扱わず、エラーのまま止めること", async () => {
    const expected = new Error("サーバーでエラーが発生しました。");
    searchMe.mockRejectedValue(expected);

    await expect(renderPage()).rejects.toBe(expected);
  });

  test("記録の詳細の店・エリア・食べた日も、だれが見ても出ること", async () => {
    searchMe.mockResolvedValue(null);

    await renderPage();

    expect(screen.getByText("ローソン（長野県）")).toBeDefined();
    expect(screen.getByText("2026年11月20日")).toBeDefined();
  });
});
