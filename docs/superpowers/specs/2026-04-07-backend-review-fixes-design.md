# Design Spec: Backend Review Fixes (B1 + B2)

**Date**: 2026-04-07  
**Status**: Approved  
**Based on**: Code Review Report `9f2ba4b → 53399be`  
**Scope**: `services/quality-inspection-service`  
**Authority baseline**: AGENTS.md, docs/01–04 指定文件  

---

## 1. Problem Statement

Code review 識別出 2 Critical、6 High、4 Medium、4 Low issue，加上 4 spec mismatch 及 7 missing test。
最嚴重的問題：

- **C-002**：所有 endpoint 完全無授權保護（security disqualifier）
- **C-001**：D-013 violation — retry 使用當前 project version 而非原 batch snapshot，破壞 audit trail
- **H-001~H-006**：domain invariant、reconstitution、DB constraint 多處缺失

本 spec 定義全部修正的設計決策，供實作 plan 使用。

---

## 2. Implementation Phases

| Phase | 內容 | 依賴 |
|---|---|---|
| Phase 1 | Auth infrastructure + JWT filter | 無 |
| Phase 2 | Domain fixes + reconstitution + exception hierarchy | Phase 1 完成後（use case 簽名需要 ActorContext） |
| Phase 3 | Medium + Low + tests + spec doc fix | Phase 2 完成後（tests 需要新 exception 型別） |

---

## 3. Phase 1 — Auth Infrastructure (C-002)

### 3.1 設計決策

- Gateway 已驗簽（RS512），service 端直接 parse `tz-user` JWT claim，不重複驗簽。
- Actor 資訊以 `ActorContext` value object 傳入 use case，不使用 ThreadLocal 或 Spring SecurityContext 全域存取。
- Hospital scope 本次**預留介面**，不實作驗證邏輯。

### 3.2 JWT 結構

Header: `api-key`  
Payload claim `tz-user`（JSON string）內含：

```json
{
  "user": {
    "user_id": "testuser",
    "user_name": "test_user_name",
    "user_role": [
      { "role_name": "system_admin", "enabled": true },
      { "role_name": "auditor", "enabled": true }
    ]
  }
}
```

`user_role[].role_name`（lowercase）直接對應 domain role（case-insensitive）。  
只取 `enabled: true` 的 role。

### 3.3 元件設計

#### `DomainRole` enum（`application.common`）
```
SYSTEM_ADMIN, HOSPITAL_MANAGER, REVIEWER, REREVIEWER, AUDITOR
```

#### `ActorContext` value object（`application.common`）
| 欄位 | 型別 | 說明 |
|---|---|---|
| userId | String | `user.user_id` |
| userName | String | `user.user_name` |
| roles | `Set<DomainRole>` | enabled roles |
| hospitalId | `Optional<String>` | 本次永遠回傳 `empty()`，預留 |

方法：
- `hasRole(DomainRole)` → boolean
- `requireRole(DomainRole)` → void，失敗拋 `UnauthorizedException`
- `requireAnyRole(DomainRole...)` → void

#### `TzJwtParser`（`infrastructure.security`）
- 接收 raw JWT string（`api-key` header 值）
- Base64URL decode payload（不驗簽）
- 解析 `tz-user` JSON string
- 回傳 `ParsedJwtUser`
- 失敗（malformed / missing claim）拋 `JwtParseException`（→ 401）

#### `ApiKeyAuthFilter`（`infrastructure.security`）
- 實作 `OncePerRequestFilter`
- 讀取 `api-key` header
  - 若缺少：回 401 `{"code":"MISSING_API_KEY"}`
- 呼叫 `TzJwtParser`，失敗：回 401 `{"code":"INVALID_TOKEN"}`
- 成功：將 `ActorContext` 寫入 `HttpServletRequest` attribute（key: `ACTOR_CONTEXT`）

#### `ActorContextArgumentResolver`（`interfaces.rest`）
- 實作 `HandlerMethodArgumentResolver`
- 支援 `@CurrentActor` annotation，controller 方法簽名可直接宣告 `@CurrentActor ActorContext actor`
- 從 `HttpServletRequest` attribute 取出 `ActorContext`

#### `UnauthorizedException`（`application.common`）
→ `GlobalExceptionHandler` 映射到 HTTP 403 + error code `UNAUTHORIZED`

### 3.4 Use case role guard（per permission-matrix.md）

| Use case | Required role |
|---|---|
| CreateReviewProject | HOSPITAL_MANAGER |
| PublishReviewProjectVersion | HOSPITAL_MANAGER |
| CreateScoreSheetDraft | HOSPITAL_MANAGER |
| PublishScoreSheetVersion | HOSPITAL_MANAGER |
| PreviewSamplingPopulation | HOSPITAL_MANAGER |
| ExecuteSampling | HOSPITAL_MANAGER |
| RetrySamplingBatch | HOSPITAL_MANAGER |

每個 use case `execute(ActorContext actor, ...)` 第一行呼叫 `actor.requireRole(HOSPITAL_MANAGER)`。

---

## 4. Phase 2 — Domain Fixes + Reconstitution + Exception Hierarchy

### 4.1 Exception Hierarchy (H-004)

```
RuntimeException
├── DomainRuleViolationException  →  HTTP 422
│     constructor: DomainRuleViolationException(String errorCode, String message)
│     例：throw new DomainRuleViolationException("PROJECT_NOT_ACTIVE", "...")
│
├── DomainStateConflictException  →  HTTP 409
│     constructor: DomainStateConflictException(String errorCode, String message)
│     例：throw new DomainStateConflictException("INVALID_STATE_TRANSITION", "...")
│
└── UnauthorizedException         →  HTTP 403
      constructor: UnauthorizedException(String message)
```

**422 error codes**（對應 api-spec.md §4.2）：
`PROJECT_NOT_ACTIVE`, `BATCH_NOT_RETRYABLE`, `SCORE_SHEET_NOT_PUBLISHABLE`, `REVISION_NOT_ALLOWED`

**409 error codes**（對應 api-spec.md §4.1）：
`CASE_ALREADY_CLOSED`, `DUPLICATE_SELECTED_CASE_IN_BATCH`, `INVALID_STATE_TRANSITION`

`GlobalExceptionHandler` 分別 handle 兩種 exception，回傳 api-spec.md 定義的 error code 字串（非 generic `"CONFLICT"`）。

移除所有 domain 內直接拋 `IllegalStateException` 的地方，改用正確的 custom exception。

### 4.2 C-001 Retry Snapshot Fix

**檔案**：`application/sampling/RetrySamplingBatchUseCase.java`

**修改**：不再呼叫 `project.getCurrentVersion()`；直接：
```java
RuleSnapshot snapshot = previousBatch.getRuleSnapshot();
```
新 retry batch 以原失敗 batch 的 snapshot 建立。

### 4.3 H-001 Cross-section itemId/code Uniqueness

**檔案**：`domain/scoresheet/ScoreSheetDefinition.java`

在 `addSection(ScoreSheetSection section)` 加入：
```java
Set<String> allItemIds = sections.stream()
    .flatMap(s -> s.getItems().stream())
    .map(ScoreItem::getItemId)
    .collect(toSet());
Set<String> allCodes = ...;

section.getItems().forEach(item -> {
    if (allItemIds.contains(item.getItemId())) throw new DomainStateConflictException("DUPLICATE_ITEM_ID", ...);
    if (allCodes.contains(item.getCode()))    throw new DomainStateConflictException("DUPLICATE_ITEM_CODE", ...);
});
```
版本層唯一性 guard，不改動 `ScoreSheetSection` 內的 section 層 guard（保留作 section 內部防線）。

### 4.4 Reconstitution — package-private `reconstitute()` static factory

三個 aggregate 各自加 package-private static factory，僅供同 package 的 repository impl 使用，不暴露公開 API。

**`ReviewProject.reconstitute(...)`**（package-private）：
- 參數：`id, hospitalId, name, recordTypes, status, versions, currentVersionNumber, createdAt, updatedAt`
- 直接 assign 所有欄位，不呼叫 `activate()`、`publishNewVersion()` 等 domain method
- `ReviewProjectRepositoryImpl.toDomain()` 改用此方法，移除 `alignVersion()` / `alignStatus()` 邏輯

**`ScoreSheetDefinition.reconstitute(...)`**（package-private）：
- 參數：`id, projectId, status, version, sections, createdAt, updatedAt`
- 直接 assign `this.version = version`，不再固定為 1
- `ScoreSheetRepositoryImpl.toDomain()` 改用此方法

**`SamplingBatch.reconstitute(...)`**（package-private）：
- 參數：`id, projectId, status, ruleSnapshot, populationCount, eligibleCount, excludedCount, selectedCases, retryOfBatchId, createdAt, finishedAt, failureReason`
- 直接 assign 所有欄位
- `SamplingBatchRepositoryImpl.toDomain()` 改用此方法，移除 `if (count > 0)` guard

### 4.5 H-005 DB Unique Constraint

**檔案**：`infrastructure/persistence/sampling/SelectedCaseRefEntity.java`

新增：
- `@Column(name = "batch_id")` explicit 欄位（與 JPA `@ManyToOne` 關聯對齊）
- `@Table(name = "sampling_batch_selected_case", uniqueConstraints = @UniqueConstraint(columnNames = {"batch_id", "source_case_key"}))`

注意：table name 同時配合 M-003 修正（見下）。

---

## 5. Phase 3 — Medium / Low / Tests / Spec

### 5.1 M-001 SamplingBatch failureReason

- `domain/sampling/SamplingBatch.java`：加 `private String failureReason;`
- `fail(String reason)`：`this.failureReason = reason;`
- `infrastructure/persistence/sampling/SamplingBatchEntity.java`：加 `@Column(name = "failure_reason") private String failureReason;`
- `reconstitute()` 參數含 `failureReason`

### 5.2 M-002 RuleSnapshot TODO

- `domain/sampling/RuleSnapshot.java` 類別 Javadoc 加：
  ```java
  // TODO(D-011): This is a stub. Replace with full immutable snapshot containing
  //              snapshot_json and checksum per decision-log.md D-011 and
  //              persistence-model.md §5.9 (sampling_batch_snapshot table).
  ```

### 5.3 M-003 Table Names

對齊 `docs/03-contracts/persistence-model.md` 定義：

| Entity | 舊 table name | 新 table name |
|---|---|---|
| ReviewProjectEntity | `review_projects` | `review_project` |
| SamplingBatchEntity | `sampling_batches` | `sampling_batch` |
| SelectedCaseRefEntity | `selected_case_refs` | `sampling_batch_selected_case` |
| ScoreSheetEntity | `score_sheet_definitions` | `score_sheet` |
| ReviewCaseEntity | `review_cases` | `review_case` |
| ScoreSheetSectionEntity | `score_sheet_sections` | `score_sheet_section` |
| ScoreItemEntity | `score_items` | `score_sheet_item` |

### 5.4 M-005 Remove sourceCaseKeys from Preview Response

- `application/sampling/SamplingPreviewResult.java`：移除 `sourceCaseKeys: List<String>` 欄位
- 若 use case 內部需要 keys 做邏輯處理，改用 local variable 不寫入 result VO

### 5.5 L-001 Input Validation

- `interfaces/rest/dto/CreateReviewProjectRequest.java`：
  - `hospitalId`、`name` 加 `@NotNull @NotBlank`
- `interfaces/rest/ReviewProjectController.java`：
  - `@RequestBody CreateReviewProjectRequest request` 加 `@Valid`
- `GlobalExceptionHandler` 加 `@ExceptionHandler(MethodArgumentNotValidException.class)` → HTTP 400 + `VALIDATION_ERROR`

### 5.6 L-002 MedicalRecordType Error Message

```java
// 原
.map(MedicalRecordType::valueOf)

// 改
.map(s -> {
    try { return MedicalRecordType.valueOf(s); }
    catch (IllegalArgumentException e) {
        throw new BadRequestException("Invalid medicalRecordType: " + s);
    }
})
```

`BadRequestException` → `GlobalExceptionHandler` → HTTP 400 + `BAD_REQUEST`。

### 5.7 L-003 JoinColumn Name

- `ScoreItemEntity` 中 `@JoinColumn(name = "section_pk")` 改為 `@JoinColumn(name = "section_id")`（FK column 名稱對齊 persistence-model.md 中 `score_sheet_section.section_id` 欄位語意，且不使用帶 `_pk` 的命名）。
- 注意：目前 persistence 實作沒有拆獨立的 `score_sheet_version` 表，`ScoreSheetSectionEntity` 直接 FK 到 `ScoreSheetEntity`；此修正僅改 FK column name，不重構 entity 層級關係。

### 5.8 L-004 ddl-auto Profile

- `application.yml`：將 `spring.jpa.hibernate.ddl-auto: update` 移至：
  ```yaml
  spring:
    config:
      activate:
        on-profile: local
    jpa:
      hibernate:
        ddl-auto: update
  ```
- 預設（非 local）使用 `validate`（或 `none`）。

### 5.9 SD-001 api-spec.md Base Path

- `docs/03-contracts/api-spec.md §3.1`：將 `Base Path: /api/quality-review` 改為 `Base Path: /api/quality-inspection`

### 5.10 Tests T-001 ~ T-007

| ID | 測試類別 | 測試名稱 | 驗證目標 |
|---|---|---|---|
| T-001 | `SamplingUseCaseTest` | `retry_shouldUseOriginalBatchSnapshot_notCurrentProjectVersion` | 新 batch `ruleSnapshot == previousBatch.ruleSnapshot`；與當前 project version 無關 |
| T-002 | `ScoreSheetDefinitionTest` | `addSection_crossSectionDuplicateItemId_shouldThrow` / `..._duplicateCode_shouldThrow` | 跨兩個 section 相同 itemId / code → `DomainStateConflictException` |
| T-003 | `ReviewProjectApiTest` | `inactiveProject_shouldReturn422` / `alreadyPublished_shouldReturn409` | 業務規則禁止 → 422；state conflict → 409（非混用） |
| T-004 | `ReviewProjectIntegrationTest`（新建）| `reconstitution_versionAndStatus_shouldBeCorrect` | @SpringBootTest；save project → publishNewVersion → prepareDraftVersion → save → reload → 確認 version=2, published=false |
| T-005 | `SamplingUseCaseTest` | `preview_inactiveProject_shouldThrow` | `PreviewSamplingPopulationUseCase` inactive project 拋例外 |
| T-006 | `ReviewProjectApiTest` | `createProject_nullMedicalRecordTypes_shouldReturn400` | null / empty `medicalRecordTypes` → HTTP 400 |
| T-007 | `SamplingControllerApiTest`（新建）| preview / execute / retry 各一個成功 + 一個失敗情境 | 完整 HTTP contract 含 response shape |

---

## 6. Out of Scope (this spec)

- M-002 RuleSnapshot 完整實作（D-011 full immutable snapshot）— 僅加 TODO
- Hospital scope validation for HOSPITAL_MANAGER — interface 預留
- B3+ use case（Assignment、Initial Review、Second Review）
- CI pipeline / lint / formatter gate

---

## 7. Files to Create / Modify

### New files
- `application/common/DomainRole.java`
- `application/common/ActorContext.java`
- `application/common/UnauthorizedException.java`
- `domain/common/DomainRuleViolationException.java`
- `domain/common/DomainStateConflictException.java`
- `infrastructure/security/TzJwtParser.java`
- `infrastructure/security/ApiKeyAuthFilter.java`
- `infrastructure/security/ParsedJwtUser.java`
- `interfaces/rest/CurrentActor.java`（annotation）
- `interfaces/rest/ActorContextArgumentResolver.java`
- `interfaces/rest/BadRequestException.java`
- `test/…/ReviewProjectIntegrationTest.java`（新建 integration test）
- `test/…/SamplingControllerApiTest.java`（新建 API test）

### Modified files
- All use case `execute()` signatures（加 `ActorContext actor` 第一個參數）
- `domain/project/ReviewProject.java`（加 `reconstitute()`、改 exception type）
- `domain/scoresheet/ScoreSheetDefinition.java`（加 `reconstitute()`、加 cross-section uniqueness check、改 exception type）
- `domain/sampling/SamplingBatch.java`（加 `reconstitute()`、加 `failureReason`、改 exception type）
- `infrastructure/persistence/*/…RepositoryImpl.java`（全部改用 `reconstitute()`）
- `infrastructure/persistence/sampling/SelectedCaseRefEntity.java`（加 column + uniqueConstraint）
- `infrastructure/persistence/sampling/SamplingBatchEntity.java`（加 `failureReason`）
- 所有 entity `@Table` annotation（M-003 table name 對齊）
- `interfaces/rest/GlobalExceptionHandler.java`（分別 handle 422 / 409 / 403 / 400）
- `interfaces/rest/dto/CreateReviewProjectRequest.java`（加 @NotNull @NotBlank）
- `interfaces/rest/ReviewProjectController.java`（加 @Valid、fix MedicalRecordType error）
- `interfaces/rest/SamplingController.java`（加 @CurrentActor ActorContext）
- `application/sampling/RetrySamplingBatchUseCase.java`（C-001 fix）
- `application/sampling/SamplingPreviewResult.java`（移除 sourceCaseKeys）
- `infrastructure/persistence/scoresheet/ScoreSheetSectionEntity.java`（L-003）
- `src/main/resources/application.yml`（L-004）
- `docs/03-contracts/api-spec.md`（SD-001）
- 現有所有 test（補測試 T-001~T-007）
