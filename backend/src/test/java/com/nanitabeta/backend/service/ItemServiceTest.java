package com.nanitabeta.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nanitabeta.backend.data.Item;
import com.nanitabeta.backend.exception.DuplicateItemNameException;
import com.nanitabeta.backend.exception.InvalidCategoryException;
import com.nanitabeta.backend.exception.ResourceNotFoundException;
import com.nanitabeta.backend.repository.ItemRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

  @Mock
  private ItemRepository repository;

  @Mock
  private ShopService shopService;

  @Mock
  private CategoryService categoryService;

  @Mock
  private NameReadingService nameReadingService;

  @InjectMocks
  private ItemService sut;

  @Test
  void 商品の取得_リポジトリの結果をそのまま返すこと() {
    Item expected = new Item(1L, 1L, "からあげ棒", 4L, false);
    when(repository.searchItem(1L)).thenReturn(expected);

    Item actual = sut.searchItem(1L);

    assertThat(actual).isEqualTo(expected);
  }

  @Test
  void 商品の取得_見つからない場合は例外を投げること() {
    when(repository.searchItem(999L)).thenReturn(null);

    assertThatThrownBy(() -> sut.searchItem(999L)).isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("商品が見つかりません。id=999");
  }

  @Test
  void 商品の候補_リポジトリの結果をそのまま返すこと() {
    List<Item> expected = List.of(new Item(1L, 1L, "からあげ棒", 4L, false));
    when(repository.searchItemSuggestions(any(), any(), any(), anyInt())).thenReturn(expected);

    List<Item> actual = sut.searchItemSuggestions(1L, "から");

    assertThat(actual).isEqualTo(expected);
  }

  @Test
  void 商品の候補_店が存在しない場合は例外を投げて検索しないこと() {
    doThrow(new ResourceNotFoundException("店が見つかりません。id=999")).when(shopService).searchShop(999L);

    assertThatThrownBy(() -> sut.searchItemSuggestions(999L, null))
        .isInstanceOf(ResourceNotFoundException.class).hasMessage("店が見つかりません。id=999");

    verify(repository, never()).searchItemSuggestions(any(), any(), any(), anyInt());
  }

  @Test
  void 商品の候補_キーワードを正規化してから渡すこと() {
    sut.searchItemSuggestions(1L, "　から　あげ　"); // 前後と途中に全角スペース

    verify(repository).searchItemSuggestions(eq(1L), eq("から あげ"), eq("から あげ"), anyInt());
  }

  @Test
  void 商品の候補_部分一致用はワイルドカードとバックスラッシュをエスケープし完全一致用はエスケープしないこと() {
    sut.searchItemSuggestions(1L, "　100%_\\OFF　");

    // 部分一致用（LIKE）：100\%\_\\OFF、完全一致用：100%_\OFF（正規化だけ）
    verify(repository).searchItemSuggestions(eq(1L), eq("100\\%\\_\\\\OFF"), eq("100%_\\OFF"),
        anyInt());
  }

  @Test
  void 商品の候補_件数の上限を10件として渡すこと() {
    sut.searchItemSuggestions(1L, "から");

    verify(repository).searchItemSuggestions(eq(1L), eq("から"), eq("から"), eq(10));
  }

  @Test
  void 商品の候補_キーワードが未指定の場合は部分一致用も完全一致用もnullのまま渡すこと() {
    sut.searchItemSuggestions(1L, null);

    verify(repository).searchItemSuggestions(eq(1L), isNull(), isNull(), anyInt());
  }

  @Test
  void 商品の候補_キーワードが空文字の場合は部分一致用も完全一致用も空文字で渡すこと() {
    sut.searchItemSuggestions(1L, "");

    verify(repository).searchItemSuggestions(eq(1L), eq(""), eq(""), anyInt());
  }

  @Test
  void 商品の候補_キーワードが空白だけの場合は正規化して空文字で渡すこと() {
    sut.searchItemSuggestions(1L, " 　 ");

    verify(repository).searchItemSuggestions(eq(1L), eq(""), eq(""), anyInt());
  }

  @Test
  void 商品の候補_商品名で10件見つかった場合は読みでは探さないこと() {
    List<Item> expected = createItems(1L, 10);
    when(repository.searchItemSuggestions(any(), any(), any(), anyInt())).thenReturn(expected);

    List<Item> actual = sut.searchItemSuggestions(1L, "まっちゃ");

    assertThat(actual).isEqualTo(expected);
    verify(nameReadingService, never()).createKeywordForms(any());
    verify(repository, never()).searchAllItemSuggestions(any());
    verify(nameReadingService, never()).matchesReading(any(), any());
  }

  @Test
  void 商品の候補_キーワードが未指定の場合は読みでは探さないこと() {
    sut.searchItemSuggestions(1L, null);

    verify(nameReadingService, never()).createKeywordForms(any());
    verify(repository, never()).searchAllItemSuggestions(any());
  }

  @Test
  void 商品の候補_比べる形がない場合は全部を読み出さないこと() {
    List<Item> expected = List.of(new Item(1L, 1L, "からあげ棒", 4L, false));
    when(repository.searchItemSuggestions(any(), any(), any(), anyInt())).thenReturn(expected);
    when(nameReadingService.createKeywordForms(" 　 ")).thenReturn(List.of());

    List<Item> actual = sut.searchItemSuggestions(1L, " 　 ");

    assertThat(actual).isEqualTo(expected);
    verify(repository, never()).searchAllItemSuggestions(any());
    verify(nameReadingService, never()).matchesReading(any(), any());
  }

  @Test
  void 商品の候補_比べる形はエスケープも正規化もしていない入力から作ること() {
    when(nameReadingService.createKeywordForms(any())).thenReturn(List.of());

    sut.searchItemSuggestions(1L, "　100%_\\OFF　");

    verify(nameReadingService).createKeywordForms("　100%_\\OFF　");
  }

  @Test
  void 商品の候補_商品名で見つからない場合は読みが当たる商品を商品名順に返すこと() {
    Item hojichaLatte = new Item(3L, 1L, "ほうじ茶ラテ", 2L, false);
    Item coffee = new Item(4L, 1L, "コーヒー", 2L, false);
    Item matchaLatte = new Item(5L, 1L, "抹茶ラテ", 2L, false);
    List<String> keywordForms = List.of("ラテ");
    when(nameReadingService.createKeywordForms("らて")).thenReturn(keywordForms);
    when(repository.searchAllItemSuggestions(1L))
        .thenReturn(List.of(hojichaLatte, coffee, matchaLatte));
    stubMatchingNames(Set.of("ほうじ茶ラテ", "抹茶ラテ"));

    List<Item> actual = sut.searchItemSuggestions(1L, "らて");

    List<Item> expected = List.of(hojichaLatte, matchaLatte);
    assertThat(actual).isEqualTo(expected);
    verify(nameReadingService).matchesReading("コーヒー", keywordForms);
    // 比べる形づくりと全部の読み出しは、1 回の検索で 1 回だけ
    verify(nameReadingService, times(1)).createKeywordForms(any());
    verify(repository, times(1)).searchAllItemSuggestions(any());
  }

  @Test
  void 商品の候補_読みで当たる商品がない場合は商品名の候補をそのまま返すこと() {
    Item latte = new Item(6L, 1L, "ラテ", 2L, false);
    Item matchaLatte = new Item(5L, 1L, "抹茶ラテ", 2L, false);
    Item coffee = new Item(4L, 1L, "コーヒー", 2L, false);
    when(repository.searchItemSuggestions(any(), any(), any(), anyInt()))
        .thenReturn(List.of(latte, matchaLatte));
    when(nameReadingService.createKeywordForms("ラテ")).thenReturn(List.of("ラテ"));
    when(repository.searchAllItemSuggestions(1L)).thenReturn(List.of(coffee));
    stubMatchingNames(Set.of()); // どの名前も読みで当たらない

    List<Item> actual = sut.searchItemSuggestions(1L, "ラテ");

    List<Item> expected = List.of(latte, matchaLatte);
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  void 商品の候補_商品名の候補を先に元の順で並べ読みの候補をうしろに足すこと() {
    Item matchaLatte = new Item(5L, 1L, "抹茶ラテ", 2L, false);
    Item latte = new Item(6L, 1L, "ラテ", 2L, false);
    Item hojichaLatte = new Item(3L, 1L, "ほうじ茶ラテ", 2L, false);
    when(repository.searchItemSuggestions(any(), any(), any(), anyInt()))
        .thenReturn(List.of(latte, matchaLatte)); // LOWER 一致の「ラテ」が先頭
    when(nameReadingService.createKeywordForms("ラテ")).thenReturn(List.of("ラテ"));
    when(repository.searchAllItemSuggestions(1L)).thenReturn(List.of(hojichaLatte));
    stubMatchingNames(Set.of("ほうじ茶ラテ"));

    List<Item> actual = sut.searchItemSuggestions(1L, "ラテ");

    List<Item> expected = List.of(latte, matchaLatte, hojichaLatte);
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  void 商品の候補_商品名で見つかった商品はIDで見分けて読みの候補に重ねて足さないこと() {
    Item latte = new Item(6L, 1L, "ラテ", 2L, false);
    // 全部を読み出した結果の中の同じ商品。2 回の SELECT の間に名前や分類が変わっても、ID が同じなら同じ商品とみなす
    Item sameIdLatte = new Item(6L, 1L, "らて", 3L, true);
    Item matchaLatte = new Item(5L, 1L, "抹茶ラテ", 2L, false);
    List<String> keywordForms = List.of("ラテ");
    when(repository.searchItemSuggestions(any(), any(), any(), anyInt()))
        .thenReturn(List.of(latte));
    when(nameReadingService.createKeywordForms("ラテ")).thenReturn(keywordForms);
    when(repository.searchAllItemSuggestions(1L)).thenReturn(List.of(matchaLatte, sameIdLatte));
    stubMatchingNames(Set.of("ラテ", "らて", "抹茶ラテ"));

    List<Item> actual = sut.searchItemSuggestions(1L, "ラテ");

    List<Item> expected = List.of(latte, matchaLatte);
    assertThat(actual).isEqualTo(expected);
    // すでに候補にある商品は、読みも作らない
    verify(nameReadingService, times(1)).matchesReading(any(), any());
    verify(nameReadingService).matchesReading("抹茶ラテ", keywordForms);
  }

  @Test
  void 商品の候補_商品名で9件見つかった場合は読みの候補を1件だけ足しそれ以上は読みを作らないこと() {
    List<Item> nameMatches = createItems(1L, 9); // ID 1〜9
    List<Item> readingMatches = createItems(101L, 3); // ID 101〜103
    when(repository.searchItemSuggestions(any(), any(), any(), anyInt())).thenReturn(nameMatches);
    when(nameReadingService.createKeywordForms("まっちゃ")).thenReturn(List.of("マッチャ"));
    when(repository.searchAllItemSuggestions(1L)).thenReturn(readingMatches);
    when(nameReadingService.matchesReading(any(), any())).thenReturn(true);

    List<Item> actual = sut.searchItemSuggestions(1L, "まっちゃ");

    List<Item> expected = new ArrayList<>(nameMatches);
    expected.add(readingMatches.get(0));
    assertThat(actual).isEqualTo(expected);
    verify(nameReadingService, times(1)).matchesReading(any(), any());
  }

  @Test
  void 商品の候補_商品名で見つからない場合は読みの候補を10件まで返すこと() {
    List<Item> readingMatches = createItems(101L, 12); // ID 101〜112
    when(nameReadingService.createKeywordForms("まっちゃ")).thenReturn(List.of("マッチャ"));
    when(repository.searchAllItemSuggestions(1L)).thenReturn(readingMatches);
    when(nameReadingService.matchesReading(any(), any())).thenReturn(true);

    List<Item> actual = sut.searchItemSuggestions(1L, "まっちゃ");

    List<Item> expected = readingMatches.subList(0, 10);
    assertThat(actual).isEqualTo(expected);
    verify(nameReadingService, times(10)).matchesReading(any(), any());
  }

  /**
   * テスト用の商品を、ID が続いた番号で作ります。
   *
   * @param firstId 最初の ID
   * @param count 件数
   * @return 商品の一覧（商品名は「商品{ID}」）
   */
  private List<Item> createItems(Long firstId, int count) {
    List<Item> items = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      long id = firstId + i;
      items.add(new Item(id, 1L, "商品" + id, 2L, false));
    }
    return items;
  }

  /**
   * 読みが当たる商品名を決めます（それ以外の商品名は当たらない）。
   *
   * @param matchingNames 読みが当たる商品名
   */
  private void stubMatchingNames(Set<String> matchingNames) {
    when(nameReadingService.matchesReading(any(), any()))
        .thenAnswer(invocation -> matchingNames.contains(invocation.getArgument(0)));
  }

  @Test
  void 商品の登録_同じ商品があれば登録せずにその商品を返すこと() {
    Item item = new Item(null, 1L, "からあげ棒", 4L, false);
    Item expected = new Item(1L, 1L, "からあげ棒", 4L, false);
    when(repository.searchItemByShopAndName(any())).thenReturn(expected);

    Item actual = sut.registerItem(item);

    assertThat(actual).isEqualTo(expected);
    verify(repository, never()).insertItem(any());
  }

  @Test
  void 商品の登録_同じ商品がなければ登録すること() {
    Item item = new Item(null, 1L, "からあげ棒", 4L, false);
    Item expected = new Item(1L, 1L, "からあげ棒", 4L, false);
    when(repository.searchItemByShopAndName(any())).thenReturn(null);
    doAnswer(invocation -> {
      Item inserted = invocation.getArgument(0);
      inserted.setId(1L); // DB が振った ID を書き戻す（useGeneratedKeys の代わり）
      return null;
    }).when(repository).insertItem(any());
    when(repository.searchItem(1L)).thenReturn(expected);

    Item actual = sut.registerItem(item);

    assertThat(actual).isEqualTo(expected);
    verify(repository).searchItem(1L); // INSERT で振られた ID で取り直すこと
  }

  @Test
  void 商品の登録_同時に登録された場合は既存の商品を返すこと() {
    Item item = new Item(null, 1L, "からあげ棒", 4L, false);
    Item expected = new Item(1L, 1L, "からあげ棒", 4L, false);
    when(repository.searchItemByShopAndName(any())).thenReturn(null);
    when(repository.searchItemByShopAndNameForShare(any())).thenReturn(expected);
    doThrow(new DuplicateKeyException("重複")).when(repository).insertItem(any());

    Item actual = sut.registerItem(item);

    assertThat(actual).isEqualTo(expected);
  }

  @Test
  void 商品の登録_商品名を正規化してから探すこと() {
    Item item = new Item(null, 1L, "　からあげ　棒　", 4L, false); // 前後と途中に全角スペース
    Item expectedArgument = new Item(null, 1L, "からあげ 棒", 4L, false);

    sut.registerItem(item);

    verify(repository).searchItemByShopAndName(expectedArgument);
    verify(repository).insertItem(expectedArgument);
    assertThat(item.getItemName()).isEqualTo("　からあげ　棒　"); // 受け取った引数は変更しないこと
  }

  @Test
  void 商品の更新_商品名を正規化して更新し更新後の商品を返すこと() {
    Item item = new Item(1L, null, "　からあげ　棒　", 4L, false); // 前後と途中に全角スペース
    Item expectedArgument = new Item(1L, null, "からあげ 棒", 4L, false);
    Item expected = new Item(1L, 1L, "からあげ 棒", 4L, false);
    when(repository.searchItem(1L)).thenReturn(expected);

    Item actual = sut.updateItem(item);

    assertThat(actual).isEqualTo(expected);
    verify(repository).updateItem(expectedArgument);
    assertThat(item.getItemName()).isEqualTo("　からあげ　棒　"); // 受け取った引数は変更しないこと
  }

  @Test
  void 商品の更新_商品が見つからない場合は例外を投げて更新しないこと() {
    Item item = new Item(999L, null, "からあげ棒", 4L, false);
    when(repository.searchItem(999L)).thenReturn(null);

    assertThatThrownBy(() -> sut.updateItem(item)).isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("商品が見つかりません。id=999");

    verify(categoryService, never()).searchCategory(any()); // 商品の確認で止まり、分類は確認しない
    verify(repository, never()).updateItem(any());
  }

  @Test
  void 商品の更新_分類が存在しない場合は例外を投げて更新しないこと() {
    Item item = new Item(1L, null, "からあげ棒", 999L, false);
    Item expected = new Item(1L, 1L, "からあげ棒", 4L, false);
    when(repository.searchItem(1L)).thenReturn(expected);
    doThrow(new InvalidCategoryException("指定された分類が存在しません。categoryId=999")).when(categoryService)
        .searchCategory(999L);

    assertThatThrownBy(() -> sut.updateItem(item)).isInstanceOf(InvalidCategoryException.class)
        .hasMessage("指定された分類が存在しません。categoryId=999");

    verify(repository, never()).updateItem(any());
  }

  @Test
  void 商品の更新_商品名が重複する場合は商品専用の例外に変換すること() {
    Item item = new Item(1L, null, "おにぎり 鮭", 1L, false);
    Item expected = new Item(1L, 1L, "からあげ棒", 4L, false);
    when(repository.searchItem(1L)).thenReturn(expected);
    doThrow(new DuplicateKeyException("重複")).when(repository).updateItem(any());

    assertThatThrownBy(() -> sut.updateItem(item)).isInstanceOf(DuplicateItemNameException.class)
        .hasMessage("同じ商品名の商品が、その店にすでに登録されています。").hasCauseInstanceOf(DuplicateKeyException.class);
  }

  @Test
  void 記録がない商品の削除_リポジトリに削除を依頼すること() {
    sut.deleteItemIfNoRecords(6L);

    verify(repository).deleteItemIfNoRecords(6L);
  }
}
