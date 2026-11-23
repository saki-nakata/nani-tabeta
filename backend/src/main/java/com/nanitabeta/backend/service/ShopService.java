package com.nanitabeta.backend.service;

import com.nanitabeta.backend.data.Shop;
import com.nanitabeta.backend.domain.ShopRegistration;
import com.nanitabeta.backend.exception.DuplicateShopNameException;
import com.nanitabeta.backend.exception.InvalidShopException;
import com.nanitabeta.backend.exception.ResourceNotFoundException;
import com.nanitabeta.backend.repository.ShopRepository;
import com.nanitabeta.backend.util.LikeEscaper;
import com.nanitabeta.backend.util.NameNormalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 店を扱うサービスです。
 */
@Service
public class ShopService {

  /** 店の候補として返す最大件数 */
  private static final int SUGGESTION_LIMIT = 10;

  private ShopRepository repository;
  private AreaService areaService;
  private ShopKindService shopKindService;
  private NameReadingService nameReadingService;

  @Autowired
  public ShopService(ShopRepository repository, AreaService areaService,
      ShopKindService shopKindService, NameReadingService nameReadingService) {
    this.repository = repository;
    this.areaService = areaService;
    this.shopKindService = shopKindService;
    this.nameReadingService = nameReadingService;
  }

  /**
   * 店を1件取得します。
   *
   * @param id 店ID
   * @return 店
   * @throws ResourceNotFoundException 店が見つからない場合
   */
  public Shop searchShop(Long id) {
    Shop shop = repository.searchShop(id);
    if (shop == null) {
      throw new ResourceNotFoundException("店が見つかりません。id=" + id);
    }
    return shop;
  }

  /**
   * 記録などを紐づける先として、リクエストの本文で指定された店を1件取得します。
   * <p>
   * URL で指定された店を探す {@link #searchShop} とは違い、見つからない場合は 400 になる例外を投げます。
   *
   * @param id 店ID
   * @return 店
   * @throws InvalidShopException 店が見つからない場合
   */
  public Shop searchShopToLink(Long id) {
    Shop shop = repository.searchShop(id);
    if (shop == null) {
      throw new InvalidShopException("指定された店が存在しません。shopId=" + id);
    }
    return shop;
  }

  /**
   * 店を登録します。
   * <p>
   * 店名を正規化したうえで、同じ店名とエリアの店がすでにあれば、新しく作らずにその店を返します。 同時に同じ店が登録された場合も、あとから登録したほうは既存の店を返します。
   *
   * @param input 登録する店（リクエストの内容。この引数は変更しない）
   * @return 登録結果
   */
  @Transactional
  public ShopRegistration registerShop(Shop input) {
    areaService.searchArea(input.getAreaId()); // 存在しない場合は InvalidAreaException
    shopKindService.searchShopKind(input.getShopKindId()); // 存在しない場合は InvalidShopKindException
    Shop shop = new Shop(input.getId(), NameNormalizer.normalize(input.getShopName()),
        input.getAreaId(), input.getShopKindId(), input.getIsClosed());
    Shop matchedShop = repository.searchShopByNameAndArea(shop);
    if (matchedShop != null) {
      return new ShopRegistration(matchedShop, false);
    }
    try {
      repository.insertShop(shop);
    } catch (DuplicateKeyException ex) {
      Shop registered = repository.searchShopByNameAndAreaForShare(shop);
      if (registered == null) {
        throw ex;
      }
      return new ShopRegistration(registered, false);
    }
    Shop registerShop = repository.searchShop(shop.getId());
    return new ShopRegistration(registerShop, true);
  }

  /**
   * 店を更新します。
   * <p>
   * 店名は正規化してから保存します。変更後の店名とエリアが他の店と重複する場合は、 DuplicateShopNameException が発生します。
   *
   * @param input 更新する店（リクエストの内容。この引数は変更しない）
   * @return 更新後の店
   * @throws ResourceNotFoundException 店が見つからない場合
   * @throws DuplicateShopNameException 店名とエリアが他の店と重複する場合
   */
  @Transactional
  public Shop updateShop(Shop input) {
    searchShop(input.getId()); // 存在しない場合は ResourceNotFoundException
    areaService.searchArea(input.getAreaId()); // 存在しない場合は InvalidAreaException
    shopKindService.searchShopKind(input.getShopKindId()); // 存在しない場合は InvalidShopKindException
    Shop shop = new Shop(input.getId(), NameNormalizer.normalize(input.getShopName()),
        input.getAreaId(), input.getShopKindId(), input.getIsClosed());
    try {
      repository.updateShop(shop);
    } catch (DuplicateKeyException ex) {
      throw new DuplicateShopNameException("同じ店名の店が、そのエリアにすでに登録されています。", ex);
    }
    return repository.searchShop(shop.getId());
  }

  /**
   * 店の登録時に表示する候補を取得します。
   * <p>
   * 閉店した店は候補に出しません。キーワードがない場合（null・空文字・スペースだけ）は、ログイン中の利用者がよく使う順に並べます
   * （自分の記録が多い店、同じなら最後に食べた日が新しい店、そのあとは店名順。自分の記録がない店はうしろに店名順）。
   * <p>
   * キーワードがある場合は、まず店名で探します（キーワードを含む店。LOWER 一致の店名を先頭に、残りは店名順）。
   * 件数の上限に足りない場合は、読みがキーワードに当たる店を、店名順にうしろへ足します （「まつや」で「松屋」、「starbucks」で「スターバックス」を見つけるため）。
   * 店名で上限まで見つかった場合は、読みでは探しません。
   *
   * @param userId 利用者のID（JWT の sub。キーワードがない場合の並びに使う）
   * @param areaId エリアID
   * @param keyword 店名の一部（null・空文字・スペースだけの場合は絞り込まず、よく使う順に並べる）
   * @return 店の一覧（該当がない場合は空のリスト）
   */
  public List<Shop> searchShopSuggestions(String userId, Long areaId, String keyword) {
    areaService.searchArea(areaId); // 存在しない場合は InvalidAreaException
    // 完全一致・かなと大文字小文字の違いを吸収した一致用（先頭に並べる）
    String sameKeyword = keyword == null ? null : NameNormalizer.normalize(keyword);
    // キーワードがない（未指定・空文字・スペースだけ）ときは、自分の記録で並べる
    if (sameKeyword == null || sameKeyword.isEmpty()) {
      return repository.searchShopSuggestionsByMyRecords(areaId, userId, SUGGESTION_LIMIT);
    }
    String likeKeyword = LikeEscaper.escape(sameKeyword); // 部分一致用
    List<Shop> suggestions =
        repository.searchShopSuggestions(areaId, likeKeyword, sameKeyword, SUGGESTION_LIMIT);

    // 店名で上限まで見つかった場合は、読みでは探さない
    if (suggestions.size() >= SUGGESTION_LIMIT) {
      return suggestions;
    }
    // 読みと比べる形は、LIKE のエスケープ前のキーワードから作る（% や _ を普通の文字として比べるため）
    List<String> keywordForms = nameReadingService.createKeywordForms(keyword);
    if (keywordForms.isEmpty()) {
      return suggestions;
    }

    List<Shop> results = new ArrayList<>(suggestions);
    // すでに候補にある店は、ID で見分けて足さない
    Set<Long> foundIds = new HashSet<>();
    for (Shop shop : suggestions) {
      foundIds.add(shop.getId());
    }
    // そのエリアの閉店していない店を店名順に見て、読みが当たるものを足す（上限になったら止める）
    List<Shop> allShops = repository.searchAllShopSuggestions(areaId);
    for (Shop shop : allShops) {
      if (results.size() >= SUGGESTION_LIMIT) {
        break;
      }
      if (!foundIds.contains(shop.getId())
          && nameReadingService.matchesReading(shop.getShopName(), keywordForms)) {
        results.add(shop);
      }
    }
    return results;
  }

}
