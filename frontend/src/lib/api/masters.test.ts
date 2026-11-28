import { afterEach, describe, expect, test, vi } from "vitest";
import {
  searchCategories,
  searchRegionDetails,
  searchShopKinds,
} from "./masters";

// publicApiFetch だけを偽物に差し替え、どの URL で呼んだかを確かめる
const { publicApiFetch } = vi.hoisted(() => ({ publicApiFetch: vi.fn() }));
vi.mock("./client", () => ({ publicApiFetch }));

afterEach(() => {
  vi.resetAllMocks();
});

describe("マスタの取得", () => {
  test.each([
    ["searchRegionDetails", searchRegionDetails, "/api/areas"],
    ["searchCategories", searchCategories, "/api/categories"],
    ["searchShopKinds", searchShopKinds, "/api/shop-kinds"],
  ])(
    "%s はログインしていなくても読める呼び方で GET %s を呼び、データだけを返すこと",
    async (_, search, path) => {
      const expected = [{ id: 1 }];
      publicApiFetch.mockResolvedValue({ data: expected, isGuest: true });

      const actual = await search();

      // isGuest は使わず、今までと同じ形（一覧そのもの）で返す
      expect(actual).toBe(expected);
      expect(publicApiFetch).toHaveBeenCalledWith(path);
    },
  );
});
