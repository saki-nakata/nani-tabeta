package com.nanitabeta.backend.controller;

import static com.nanitabeta.backend.controller.TestUsers.USER_ID;
import static com.nanitabeta.backend.controller.TestUsers.registeredUser;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.nanitabeta.backend.config.ClockConfig;
import com.nanitabeta.backend.config.SecurityConfig;
import com.nanitabeta.backend.config.UserAuthenticationConverter;
import com.nanitabeta.backend.data.FoodRecord;
import com.nanitabeta.backend.data.Item;
import com.nanitabeta.backend.data.Shop;
import com.nanitabeta.backend.domain.RecordDetail;
import com.nanitabeta.backend.exception.InvalidPhotoPathException;
import com.nanitabeta.backend.exception.InvalidShopException;
import com.nanitabeta.backend.exception.ResourceNotFoundException;
import com.nanitabeta.backend.service.RecordService;

@WebMvcTest(RecordController.class)
@Import({SecurityConfig.class, ClockConfig.class})
class RecordControllerTest {

  private static final String PHOTO =
      USER_ID + "/records/0b3c9f2a-1234-4abc-9def-0123456789ab.webp";

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private RecordService service;

  @MockitoBean
  private UserAuthenticationConverter userAuthenticationConverter; // jwt() では使われない

  /**
   * 日本時間 2020-01-01 07:00（＝ UTC 2019-12-31 22:00）で止めた時計。
   * <p>
   * 本物の今日と離れた日付にして、入力チェックが本物の時計を使っていたら結果が変わるようにしている。
   */
  @TestBean
  private Clock clock;

  static Clock clock() {
    return Clock.fixed(Instant.parse("2019-12-31T22:00:00Z"), ClockConfig.ZONE);
  }

  /** 食べた日だけを変えた、登録の本文 */
  private static String requestBody(String eatenOn) {
    return """
        {"shopId": 2, "itemName": "からあげ棒", "categoryId": 4, "isSeasonal": false,
         "eatenOn": %s, "rating": 4, "review": "衣がサクサク", "customNote": null, "price": 220,
         "photoPaths": ["%s"]}
        """.formatted(eatenOn, PHOTO);
  }

  private static RecordDetail recordDetail(LocalDate eatenOn) {
    return new RecordDetail(
        new FoodRecord(1L, USER_ID, 6L, eatenOn, null, "衣がサクサク", 4, 220),
        new Item(6L, 2L, "からあげ棒", 4L, false), new Shop(2L, "セブンイレブン", 20L, 1L, false),
        List.of(PHOTO));
  }

  @Test
  void 記録の取得_記録の詳細がJSONで返ること() throws Exception {
    when(service.searchRecordDetail(1L)).thenReturn(recordDetail(LocalDate.of(2020, 1, 1)));

    mockMvc.perform(get("/api/records/1").with(registeredUser())).andExpect(status().isOk())
        .andExpect(content().json("""
            {"record": {"id": 1, "userId": "%s", "itemId": 6, "eatenOn": "2020-01-01",
                        "customNote": null, "review": "衣がサクサク", "rating": 4, "price": 220},
             "item": {"id": 6, "shopId": 2, "itemName": "からあげ棒", "categoryId": 4,
                      "isSeasonal": false},
             "shop": {"id": 2, "shopName": "セブンイレブン", "areaId": 20, "shopKindId": 1,
                      "isClosed": false},
             "photoPaths": ["%s"]}
            """.formatted(USER_ID, PHOTO)));
  }

  @Test
  void 記録の取得_見つからない場合は404が返ること() throws Exception {
    when(service.searchRecordDetail(999L))
        .thenThrow(new ResourceNotFoundException("記録が見つかりません。id=999"));

    mockMvc.perform(get("/api/records/999").with(registeredUser()))
        .andExpect(status().isNotFound()).andExpect(content().json("""
            {"statusValue": 404, "statusName": "NOT_FOUND", "message": "記録が見つかりません。id=999"}
            """));
  }

  @Test
  void 記録の登録_JWTのsubを記録者として登録し201が返ること() throws Exception {
    LocalDate eatenOn = LocalDate.of(2020, 1, 1);
    when(service.registerRecord(any(), any(), any())).thenReturn(recordDetail(eatenOn));
    FoodRecord expectedRecord =
        new FoodRecord(null, USER_ID, null, eatenOn, null, "衣がサクサク", 4, 220);
    Item expectedItem = new Item(null, 2L, "からあげ棒", 4L, false);

    mockMvc.perform(post("/api/records").with(registeredUser())
        .contentType(MediaType.APPLICATION_JSON).content(requestBody("\"2020-01-01\"")))
        .andExpect(status().isCreated()).andExpect(content().json("""
            {"record": {"id": 1, "userId": "%s", "eatenOn": "2020-01-01"}}
            """.formatted(USER_ID)));

    verify(service).registerRecord(expectedRecord, expectedItem, List.of(PHOTO));
  }

  @Test
  void 記録の登録_日本時間の今日なら登録できること() throws Exception {
    // 時計は日本時間 1/1 07:00。UTC ではまだ 12/31 なので、UTC で判定すると 1/1 は「未来」になる
    when(service.registerRecord(any(), any(), any()))
        .thenReturn(recordDetail(LocalDate.of(2020, 1, 1)));

    mockMvc.perform(post("/api/records").with(registeredUser())
        .contentType(MediaType.APPLICATION_JSON).content(requestBody("\"2020-01-01\"")))
        .andExpect(status().isCreated());
  }

  @Test
  void 記録の登録_昨日の日付なら登録できること() throws Exception {
    when(service.registerRecord(any(), any(), any()))
        .thenReturn(recordDetail(LocalDate.of(2019, 12, 31)));

    mockMvc.perform(post("/api/records").with(registeredUser())
        .contentType(MediaType.APPLICATION_JSON).content(requestBody("\"2019-12-31\"")))
        .andExpect(status().isCreated());
  }

  @Test
  void 記録の登録_明日の日付の場合は400が返ること() throws Exception {
    mockMvc.perform(post("/api/records").with(registeredUser())
        .contentType(MediaType.APPLICATION_JSON).content(requestBody("\"2020-01-02\"")))
        .andExpect(status().isBadRequest()).andExpect(content().json("""
            {"statusValue": 400, "statusName": "BAD_REQUEST",
             "message": "食べた日に未来の日付は指定できません。"}
            """));

    verify(service, never()).registerRecord(any(), any(), any());
  }

  @Test
  void 記録の登録_食べた日が未入力の場合は400が返ること() throws Exception {
    mockMvc.perform(post("/api/records").with(registeredUser())
        .contentType(MediaType.APPLICATION_JSON).content(requestBody("null")))
        .andExpect(status().isBadRequest()).andExpect(content().json("""
            {"statusValue": 400, "statusName": "BAD_REQUEST", "message": "食べた日を入力してください。"}
            """));

    verify(service, never()).registerRecord(any(), any(), any());
  }

  @Test
  void 記録の登録_評価が5を超える場合は400が返ること() throws Exception {
    String body = requestBody("\"2020-01-01\"").replace("\"rating\": 4", "\"rating\": 6");

    mockMvc.perform(post("/api/records").with(registeredUser())
        .contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest()).andExpect(content().json("""
            {"statusValue": 400, "statusName": "BAD_REQUEST", "message": "評価は1〜5で選択してください。"}
            """));

    verify(service, never()).registerRecord(any(), any(), any());
  }

  @Test
  void 記録の登録_写真が6枚以上の場合は400が返ること() throws Exception {
    String sixPhotos = String.join("\", \"", List.of(PHOTO, PHOTO, PHOTO, PHOTO, PHOTO, PHOTO));
    String body = requestBody("\"2020-01-01\"").replace(PHOTO, sixPhotos);

    mockMvc.perform(post("/api/records").with(registeredUser())
        .contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest()).andExpect(content().json("""
            {"statusValue": 400, "statusName": "BAD_REQUEST", "message": "写真は5枚まで登録できます。"}
            """));

    verify(service, never()).registerRecord(any(), any(), any());
  }

  @Test
  void 記録の登録_店が存在しない場合は400が返ること() throws Exception {
    when(service.registerRecord(any(), any(), any()))
        .thenThrow(new InvalidShopException("指定された店が存在しません。shopId=2"));

    mockMvc.perform(post("/api/records").with(registeredUser())
        .contentType(MediaType.APPLICATION_JSON).content(requestBody("\"2020-01-01\"")))
        .andExpect(status().isBadRequest()).andExpect(content().json("""
            {"statusValue": 400, "statusName": "BAD_REQUEST",
             "message": "指定された店が存在しません。shopId=2"}
            """));
  }

  @Test
  void 記録の登録_写真のパスが正しくない場合は400が返ること() throws Exception {
    when(service.registerRecord(any(), any(), any()))
        .thenThrow(new InvalidPhotoPathException("同じ写真が重複しています。"));

    mockMvc.perform(post("/api/records").with(registeredUser())
        .contentType(MediaType.APPLICATION_JSON).content(requestBody("\"2020-01-01\"")))
        .andExpect(status().isBadRequest()).andExpect(content().json("""
            {"statusValue": 400, "statusName": "BAD_REQUEST", "message": "同じ写真が重複しています。"}
            """));
  }
}
