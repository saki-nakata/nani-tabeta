import { afterEach, describe, expect, test, vi } from "vitest";
import {
  searchCategories,
  searchRegionDetails,
  searchShopKinds,
} from "./masters";

// apiFetch だけを偽物に差し替え、どの URL で呼んだかを確かめる
const { apiFetch } = vi.hoisted(() => ({ apiFetch: vi.fn() }));
vi.mock("./client", () => ({ apiFetch }));

afterEach(() => {
  vi.resetAllMocks();
});

describe("マスタの取得", () => {
  test.each([
    ["searchRegionDetails", searchRegionDetails, "/api/areas"],
    ["searchCategories", searchCategories, "/api/categories"],
    ["searchShopKinds", searchShopKinds, "/api/shop-kinds"],
  ])("%s は GET %s を呼び、結果をそのまま返すこと", async (_, search, path) => {
    const expected = [{ id: 1 }];
    apiFetch.mockResolvedValue(expected);

    const actual = await search();

    expect(actual).toBe(expected);
    expect(apiFetch).toHaveBeenCalledWith(path);
  });
});
