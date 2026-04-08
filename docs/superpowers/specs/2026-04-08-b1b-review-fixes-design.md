# B1b Review Fixes Design Spec

## 1. 背景

B1b (ScoreSheetDefinition CRUD + Query) 已完成實作並通過 final code review。  
Review 發現 C-1 (Critical) + I-1 (Important) 已修正（commit `58a681c`）。  
本文件定義剩餘 6 項 findings 的修正策略。

---

## 2. 修正項目

### T1: S-2 — Domain events 消除 ActorContext 依賴

**問題：** 9 個 domain event class 的 constructor 接受 `ActorContext`（application layer），違反 hexagonal architecture 的依賴方向。

**策略：** 改為接受 `String actorUserId, String actorUserName`，由 application layer use case 在 fire event 時從 `ActorContext` 取出值傳入。

**影響範圍：**

Domain event files (改 constructor signature，移除 `import ActorContext`)：
- `domain/project/event/ReviewProjectCreatedEvent.java`
- `domain/project/event/ReviewProjectActivatedEvent.java`
- `domain/project/event/ReviewProjectDeactivatedEvent.java`
- `domain/project/event/ReviewProjectVersionPublishedEvent.java`
- `domain/project/event/ReviewProjectDraftUpdatedEvent.java`
- `domain/project/event/ScoreSheetBoundToProjectEvent.java`
- `domain/scoresheet/event/ScoreSheetDraftCreatedEvent.java`
- `domain/scoresheet/event/ScoreSheetVersionPublishedEvent.java`
- `domain/scoresheet/event/ScoreSheetDraftUpdatedEvent.java`

Application use case files (改 event construction call site，用 `actor.getUserId()`, `actor.getUserName()` 傳入)：
- 所有發布 domain event 的 use case

Test files：
- 任何直接 construct domain event 的 test

**設計：**

Before:
```java
public class ReviewProjectCreatedEvent extends DomainEvent {
    public ReviewProjectCreatedEvent(Object source, ActorContext actor,
                                      String projectId, String hospitalId) {
        super(source, actor.getUserId(), actor.getUserName(),
              hospitalId, "ReviewProject", projectId, "CREATED", null);
    }
}
```

After:
```java
public class ReviewProjectCreatedEvent extends DomainEvent {
    public ReviewProjectCreatedEvent(Object source, String actorUserId, String actorUserName,
                                      String projectId, String hospitalId) {
        super(source, actorUserId, actorUserName,
              hospitalId, "ReviewProject", projectId, "CREATED", null);
    }
}
```

Call site change (use case):
```java
// Before:
eventPublisher.publish(new ReviewProjectCreatedEvent(this, actor, projectId, hospitalId));
// After:
eventPublisher.publish(new ReviewProjectCreatedEvent(this, actor.getUserId(), actor.getUserName(), projectId, hospitalId));
```

---

### T2: S-3 — Controller enum valueOf 驗證

**問題：** `ScoreSheetController.toCommand()` 中的 `ScoreType.valueOf()` 和 `RequiredRule.valueOf()` 沒有 try-catch。不合法值會拋 `IllegalArgumentException` → 500 而非 400。

**策略：** 在 `toCommand()` 和 `toAddItemCmd()` 中加 try-catch，catch `IllegalArgumentException` 並 rethrow 為 `BadRequestException`，附帶清楚的錯誤訊息。

**設計：**

新增 helper method：
```java
private <E extends Enum<E>> E parseEnum(Class<E> enumClass, String value, String fieldName) {
    try {
        return Enum.valueOf(enumClass, value);
    } catch (IllegalArgumentException | NullPointerException e) {
        throw new BadRequestException(
            String.format("Invalid %s: '%s'. Allowed values: %s",
                fieldName, value,
                Arrays.stream(enumClass.getEnumConstants())
                    .map(Enum::name)
                    .collect(Collectors.joining(", "))));
    }
}
```

Replace all `ScoreType.valueOf(...)` → `parseEnum(ScoreType.class, ..., "scoreType")`
Replace all `RequiredRule.valueOf(...)` → `parseEnum(RequiredRule.class, ..., "requiredRule")`

新增 API test：驗證 bad enum value → 400 + 含 allowed values 的 error message。

---

### T3: I-2 + I-4 — 更新 persistence-model.md

**問題：**

- I-2: `score_sheet_item.section_id` spec 定義為 varchar FK to section business key，但 JPA 實作用 surrogate PK (BIGINT) 作為 section→items join column。
- I-4: `score_sheet_item.required_rule_json` spec 定義為 JSON，但實作拆成 `required_rule` (varchar) + `condition_expression` (varchar) 兩欄。

**策略：** 更新 persistence-model.md § 5.6 和 § 5.7 對齊實作。

**設計 — § 5.6 score_sheet_section：**

加入 `id: bigint PK (surrogate)`，保留 `section_id: varchar` 作為 business key。
說明 JPA 使用 surrogate PK 管理 section→items parent-child 關聯。

**設計 — § 5.7 score_sheet_item：**

- 移除 `required_rule_json: json`，改為 `required_rule: varchar` + `condition_expression: varchar nullable`
- 移除 `visibility_rule_json: json`（尚未實作，見 T4）
- 將 `section_id` 的 FK 說明改為參照 section surrogate PK
- 加入 `id: bigint PK (surrogate)` 管理 section→items parent-child，保留 `item_row_id: bigint PK` 用於全域唯一

**備註：** 保持 `(score_sheet_version_id, item_id)` 和 `(score_sheet_version_id, item_code)` unique 約束不變。

---

### T4: I-3 — 記錄 visibilityRule 延後

**策略：** 在 decision-log.md 新增一筆 decision，記錄 `visibilityRule` 延後至 B3/B4 實作。
同時在 persistence-model.md 中將 `visibility_rule_json` 標記為 reserved/未實作。

---

### T5: I-5 — 更新 api-spec.md POST /score-sheets

**問題：** api-spec.md § 8.1 `POST /score-sheets` 的 request body 包含完整 name/sections/items，但 B1b 設計決定 create-then-update 模式，create 只需 `{ projectId }`。

**策略：** 更新 api-spec.md § 8.1 對齊實作。新增 decision-log 記錄此設計決策。

**設計 — 新 request body：**
```json
{
  "projectId": "RP-001"
}
```

**設計 — 新 response：**
```json
{
  "code": "SUCCESS",
  "data": {
    "scoreSheetId": "SS-xxx"
  }
}
```

同時補充 § 8.2 `PUT /score-sheets/{scoreSheetId}` 的 request body（updateDraft 六相位 command）。

---

### T6: 驗證

全量 `.\gradlew test` 確認 209+ tests 通過，無 regression。

---

## 3. 執行順序

1. **T1**: Domain events 消除 ActorContext 依賴（code）
2. **T2**: Controller enum valueOf 驗證（code）
3. **T3**: 更新 persistence-model.md（docs）
4. **T4**: 記錄 visibilityRule 延後（docs）
5. **T5**: 更新 api-spec.md（docs）
6. **T6**: 全量 test 驗證

先做 code 修改（T1→T2），確認 test pass，再做 docs 修改（T3→T4→T5）。

---

## 4. 不在範圍

- S-1 (ScoreSheetSummary 缺 hospitalId) — 不影響正確性，後續優化
- S-4 (InMemoryScoreSheetRepository deep copy) — test utility，不影響 production
- 任何新功能或新 endpoint
- 任何 scope 以外的 refactor
