import { act, cleanup, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, test, vi } from "vitest";
import type { SelectedPhoto } from "@/components/photo-picker";
import type { SaveRecordResult } from "@/app/records/[id]/edit/actions";
import type { RecordDetail } from "@/types/record";
import { RecordForm } from "./record-form";

// ここでは「保存する」を押したあとの順番（写真を置く → 保存 → 後片付け → 移動）を確かめる
// 写真の欄・店や商品の欄は、中身のない偽物にする（写真の欄と、店を選んだあとの表示は、それぞれの部品のテストで確かめている。
// 店・商品を探して選ぶ操作とフォームのつながりは、ここでは確かめない。主な流れは E2E の担当）
const { push, saveRecord, uploadPhotos, deletePhotos, photoPicker } =
  vi.hoisted(() => ({
    push: vi.fn(),
    saveRecord: vi.fn(),
    uploadPhotos: vi.fn(),
    deletePhotos: vi.fn(),
    // 写真の欄の偽物で「写真を変える」を押したときに、渡す写真の並び
    photoPicker: { nextPhotos: [] as SelectedPhoto[] },
  }));
vi.mock("next/navigation", () => ({ useRouter: () => ({ push }) }));
vi.mock("@/app/records/[id]/edit/actions", () => ({ saveRecord }));
vi.mock("@/app/records/new/actions", () => ({
  registerNewShop: vi.fn(),
  submitRecord: vi.fn(),
  suggestItems: vi.fn(),
}));
vi.mock("@/lib/photos/upload", () => ({ uploadPhotos, deletePhotos }));
vi.mock("@/lib/last-area", () => ({ saveLastAreaId: vi.fn() }));
vi.mock("@/components/area-picker", () => ({ AreaPicker: () => null }));
vi.mock("@/components/shop-picker", () => ({ ShopPicker: () => null }));
vi.mock("@/components/item-picker", () => ({
  EMPTY_NEW_ITEM: { itemName: "", categoryId: null, isSeasonal: false },
  ItemPicker: () => null,
}));
// 写真の欄：今の並び（id）を出し、ボタンで photoPicker.nextPhotos に変える
vi.mock("@/components/photo-picker", () => ({
  PhotoPicker: ({
    photos,
    onChange,
  }: {
    photos: SelectedPhoto[];
    onChange: (photos: SelectedPhoto[]) => void;
  }) => (
    <div>
      <p data-testid="photo-order">
        {photos.map((photo) => photo.id).join(",")}
      </p>
      <button type="button" onClick={() => onChange(photoPicker.nextPhotos)}>
        写真を変える
      </button>
    </div>
  ),
}));
// キャンセル：確かめの小窓は CancelEditButton のテストで確かめているので、渡された値だけ出す
vi.mock("@/components/cancel-edit-button", () => ({
  CancelEditButton: ({
    href,
    isChanged,
  }: {
    href: string;
    isChanged: boolean;
  }) => <p data-testid="cancel">{`${href} isChanged=${isChanged}`}</p>,
}));

const USER = "20b622c8";
const A = `${USER}/records/a.webp`;
const B = `${USER}/records/b.webp`;
const UPLOADED_X = `${USER}/records/x.webp`;
const UPLOADED_Y = `${USER}/records/y.webp`;

const CONFLICT_MESSAGE =
  "ほかの画面で先に保存されています。\n詳細画面を開き直してから、もう一度編集してください。";
const UNKNOWN_SAVE_MESSAGE =
  "保存できたか確認できませんでした。詳細画面で保存されたか確かめてから、もう一度保存してください。";

// 編集する記録（版 3、写真は A・B）
const editingRecord: RecordDetail = {
  record: {
    id: 12,
    userId: USER,
    itemId: 7,
    eatenOn: "2026-11-08",
    customNote: null,
    review: "濃くておいしい",
    rating: 4,
    price: 220,
    version: 3,
  },
  item: {
    id: 7,
    shopId: 4,
    itemName: "抹茶ラテ",
    categoryId: 10,
    isSeasonal: false,
  },
  shop: {
    id: 4,
    shopName: "スタバ",
    areaId: 20,
    shopKindId: 3,
    isClosed: false,
  },
  photoPaths: [A, B],
};

// 保存済みの写真（編集画面と同じく、パスを id にする）
function savedPhoto(path: string): SelectedPhoto {
  return { type: "saved", id: path, path, url: `https://example.com/${path}` };
}

// 新しく選んだ写真（file を取り出せるように、「新しい写真」の型で返す）
function newPhoto(name: string): Extract<SelectedPhoto, { type: "new" }> {
  const previewUrl = `blob:${name}`;
  return {
    type: "new",
    id: previewUrl,
    file: new File(["photo"], `${name}.jpg`, { type: "image/jpeg" }),
    previewUrl,
  };
}

function renderForm() {
  return render(
    <RecordForm
      today="2026-11-17"
      regionDetails={[]}
      shopKinds={[]}
      categories={[]}
      initialAreaId={20}
      editingRecord={editingRecord}
      initialPhotos={[savedPhoto(A), savedPhoto(B)]}
    />,
  );
}

function saveButton() {
  return screen.getByRole("button", { name: /保存する|保存しています…/ });
}

function isDisabled(element: HTMLElement) {
  // 保存の途中は、まわりの fieldset ごと押せなくしている
  return element.matches(":disabled");
}

beforeEach(() => {
  // 新しい写真のプレビュー用の URL は、画面を離れるときに片付ける（jsdom にはないので偽物にする）
  URL.revokeObjectURL = vi.fn();
  photoPicker.nextPhotos = [];
  uploadPhotos.mockImplementation(async (files: File[]) =>
    files.map((file) => (file.name === "x.jpg" ? UPLOADED_X : UPLOADED_Y)),
  );
  deletePhotos.mockResolvedValue(undefined);
});

afterEach(() => {
  cleanup();
  vi.resetAllMocks();
});

describe("RecordForm（編集）の保存", () => {
  test("新しい写真だけを置き、保存済みと新しい写真を、欄に並んだ順のパスで送ること", async () => {
    const user = userEvent.setup();
    saveRecord.mockResolvedValue({ status: "success" });
    renderForm();
    // 並び：新しい X → 保存済み B → 新しい Y → 保存済み A
    const x = newPhoto("x");
    const y = newPhoto("y");
    photoPicker.nextPhotos = [x, savedPhoto(B), y, savedPhoto(A)];
    await user.click(screen.getByRole("button", { name: "写真を変える" }));

    await user.click(saveButton());

    // 保存済みの A・B は、もう Storage にあるので置き直さない
    expect(uploadPhotos).toHaveBeenCalledWith([x.file, y.file], "records");
    expect(saveRecord).toHaveBeenCalledWith(
      12,
      3,
      expect.objectContaining({ photoPaths: [UPLOADED_X, B, UPLOADED_Y, A] }),
    );
  });

  test("編集画面を開いたときの版（version）を、そのまま saveRecord に渡すこと", async () => {
    const user = userEvent.setup();
    saveRecord.mockResolvedValue({ status: "success" });
    renderForm();

    await user.click(saveButton());

    expect(saveRecord).toHaveBeenCalledWith(12, 3, expect.anything());
  });

  test("保存できたら詳細画面へ移り、写真は消さず、ボタンは押せないままにすること", async () => {
    const user = userEvent.setup();
    saveRecord.mockResolvedValue({ status: "success" });
    renderForm();

    await user.click(saveButton());

    expect(push).toHaveBeenCalledWith("/records/12");
    expect(deletePhotos).not.toHaveBeenCalled();
    // 移るまでにもう一度押されて、二重に保存しないように
    expect(saveButton().textContent).toBe("保存しています…");
    expect(isDisabled(saveButton())).toBe(true);
  });

  test("ほかの画面で先に保存されていた（409 → failed）ら、今回置いた新しい写真だけを消し、案内を出して押せる状態に戻すこと", async () => {
    const user = userEvent.setup();
    saveRecord.mockResolvedValue({
      status: "failed",
      message: CONFLICT_MESSAGE,
    });
    renderForm();
    photoPicker.nextPhotos = [savedPhoto(A), newPhoto("x"), savedPhoto(B)];
    await user.click(screen.getByRole("button", { name: "写真を変える" }));

    await user.click(saveButton());

    // 保存済みの A・B は、先に保存した画面の記録でも使っているかもしれないので消さない
    expect(deletePhotos).toHaveBeenCalledWith([UPLOADED_X]);
    expect(screen.getByRole("alert").textContent).toContain(CONFLICT_MESSAGE);
    expect(push).not.toHaveBeenCalled();
    expect(saveButton().textContent).toBe("保存する");
    expect(isDisabled(saveButton())).toBe(false);
  });

  test("新しい写真がないまま failed になったら、消す写真は空のままにすること", async () => {
    const user = userEvent.setup();
    saveRecord.mockResolvedValue({
      status: "failed",
      message: CONFLICT_MESSAGE,
    });
    renderForm();

    await user.click(saveButton());

    expect(deletePhotos).toHaveBeenCalledWith([]);
  });

  test("保存できたかわからない（unknown）ときは、写真を消さずに、詳細で確かめる案内を出すこと", async () => {
    const user = userEvent.setup();
    saveRecord.mockResolvedValue({ status: "unknown" });
    renderForm();
    photoPicker.nextPhotos = [savedPhoto(A), newPhoto("x")];
    await user.click(screen.getByRole("button", { name: "写真を変える" }));

    await user.click(saveButton());

    // 保存されていれば、新しい写真 X も記録で使っているので消さない
    expect(deletePhotos).not.toHaveBeenCalled();
    expect(screen.getByRole("alert").textContent).toContain(
      UNKNOWN_SAVE_MESSAGE,
    );
    expect(push).not.toHaveBeenCalled();
    expect(isDisabled(saveButton())).toBe(false);
  });

  test("saveRecord の返事が届かなかった（投げた）ときも、写真を消さずに unknown と同じ案内を出すこと", async () => {
    const user = userEvent.setup();
    saveRecord.mockRejectedValue(new TypeError("fetch failed"));
    renderForm();
    photoPicker.nextPhotos = [newPhoto("x")];
    await user.click(screen.getByRole("button", { name: "写真を変える" }));

    await user.click(saveButton());

    expect(deletePhotos).not.toHaveBeenCalled();
    expect(screen.getByRole("alert").textContent).toContain(
      UNKNOWN_SAVE_MESSAGE,
    );
  });

  test("保存している途中は「保存しています…」になって押せず、もう一度押しても保存は 1 回だけであること", async () => {
    const user = userEvent.setup();
    // 返事をまだ返さない保存
    let finishSave: (result: SaveRecordResult) => void = () => {};
    saveRecord.mockReturnValue(
      new Promise<SaveRecordResult>((resolve) => {
        finishSave = resolve;
      }),
    );
    renderForm();

    await user.click(saveButton());
    await waitFor(() => expect(saveRecord).toHaveBeenCalledTimes(1));

    expect(saveButton().textContent).toBe("保存しています…");
    expect(isDisabled(saveButton())).toBe(true);
    await user.click(saveButton());
    expect(saveRecord).toHaveBeenCalledTimes(1);

    // 後始末：保存を終わらせる
    await act(async () => finishSave({ status: "success" }));
  });

  test("価格の「0220」は、220 として送ること", async () => {
    const user = userEvent.setup();
    saveRecord.mockResolvedValue({ status: "success" });
    renderForm();
    const price = screen.getByLabelText("価格");
    await user.clear(price);
    await user.type(price, "0220");
    await user.tab();

    await user.click(saveButton());

    expect(saveRecord).toHaveBeenCalledWith(
      12,
      3,
      expect.objectContaining({ price: 220 }),
    );
  });
});

describe("RecordForm（編集）の変更の有無", () => {
  function cancelText() {
    return screen.getByTestId("cancel").textContent;
  }

  test("開いたばかりなら、変更なしとしてキャンセルに詳細の URL を渡すこと", () => {
    renderForm();

    expect(cancelText()).toBe("/records/12 isChanged=false");
  });

  test("感想を変えたら変更あり、元に戻したら変更なしになること", async () => {
    const user = userEvent.setup();
    renderForm();
    const review = screen.getByLabelText("感想");

    await user.type(review, "！");
    expect(cancelText()).toContain("isChanged=true");

    await user.type(review, "{Backspace}");
    expect(cancelText()).toContain("isChanged=false");
  });

  test("価格を「0220」にしても、数としては 220 のままなので変更なしであること", async () => {
    const user = userEvent.setup();
    renderForm();
    const price = screen.getByLabelText("価格");

    await user.clear(price);
    await user.type(price, "0220");

    expect(cancelText()).toContain("isChanged=false");
  });

  test("価格を変えたら変更ありになること", async () => {
    const user = userEvent.setup();
    renderForm();
    const price = screen.getByLabelText("価格");

    await user.clear(price);
    await user.type(price, "300");

    expect(cancelText()).toContain("isChanged=true");
  });

  test("評価を変えたら変更ありになること", async () => {
    const user = userEvent.setup();
    renderForm();

    await user.click(screen.getByRole("button", { name: "5点" }));

    expect(cancelText()).toContain("isChanged=true");
  });

  test("写真を並べかえただけでも変更ありになること", async () => {
    const user = userEvent.setup();
    renderForm();
    photoPicker.nextPhotos = [savedPhoto(B), savedPhoto(A)];

    await user.click(screen.getByRole("button", { name: "写真を変える" }));

    expect(screen.getByTestId("photo-order").textContent).toBe(`${B},${A}`);
    expect(cancelText()).toContain("isChanged=true");
  });
});
