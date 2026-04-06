# Example → Test Case Skeleton v1

## 1. 文件目的

本文件用於將 `specification-by-example.md` 中的 example，轉成可直接落地的測試骨架，作為以下工作的依據：

- test-first 任務拆解
- 測試類別 / 方法命名
- 測試責任分工
- regression suite 實作入口

本文件不直接提供完整測試程式碼。  
本文件提供的是：

- 測試應該落在哪一層
- 測試應叫什麼名字
- 測試最少應驗證什麼
- 需要哪些 fixture / fake / sample data

---

## 2. 命名原則

### 2.1 測試類別命名
建議格式：

- Domain Unit Test  
  `{AggregateOrPolicy}Test`

- Application Service Test  
  `{UseCaseName}Test`

- API Integration Test  
  `{ControllerOrApiArea}ApiTest`

- Persistence Test  
  `{RepositoryOrConstraintArea}PersistenceTest`

- Acceptance Test  
  `{BusinessFlowName}AcceptanceTest`

### 2.2 測試方法命名
建議格式：

```text
should_<expected_behavior>_when_<condition>
```

例如：

```text
should_close_case_when_initial_score_is_not_above_threshold
```

---

## 3. Skeleton 欄位定義

| 欄位                    | 說明                               |
| --------------------- | -------------------------------- |
| Example ID            | 來自 `specification-by-example.md` |
| Primary Test Type     | 主要測試層級                           |
| Suggested Test Class  | 建議類別名                            |
| Suggested Test Method | 建議方法名                            |
| Key Assertions        | 核心 assertion                     |
| Suggested Fixtures    | 建議 fixture / sample data         |
| Secondary Coverage    | 建議補充覆蓋層級                         |

---

## 4. Core Skeleton Matrix

| Example ID | Primary Test Type        | Suggested Test Class                       | Suggested Test Method                                                                              | Key Assertions                                                                  | Suggested Fixtures                                                      | Secondary Coverage             |
| ---------- | ------------------------ | ------------------------------------------ | -------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------- | ----------------------------------------------------------------------- | ------------------------------ |
| EX-001     | Domain Unit Test         | `ReviewCaseTest`                           | `should_close_case_when_initial_score_is_not_above_threshold`                                      | case status = CLOSED；不進入 rereview；產生 close-related event                        | case in INITIAL_REVIEW_IN_PROGRESS；qualityThreshold=80；score=75         | Application / API / Acceptance |
| EX-002     | Domain Unit Test         | `ReviewCaseTest`                           | `should_mark_case_pending_rereview_when_initial_score_exceeds_threshold`                           | case status = PENDING_REREVIEW；rereviewRequired=true；產生 escalate event          | case in INITIAL_REVIEW_IN_PROGRESS；threshold=80；score=88.5              | Application / API / Acceptance |
| EX-003     | Domain Unit Test         | `ReviewCaseTest`                           | `should_finalize_score_from_rereview_submission_when_rereview_is_completed`                        | finalScore 取複審分數；status=CLOSED                                                  | case in REREVIEW_IN_PROGRESS；valid rereview submission                  | Application / API / Acceptance |
| EX-004     | Application Service Test | `ReviseInitialReviewUseCaseTest`           | `should_create_new_initial_revision_when_actor_is_original_reviewer_and_case_is_open`              | 建立新 submission；revisionNo+1；舊版 SUPERSEDED；case status 不變                        | existing INITIAL submitted submission；open case；original reviewer actor | Domain / API / Persistence     |
| EX-005     | Application Service Test | `ReviseInitialReviewUseCaseTest`           | `should_reject_initial_revision_when_actor_is_not_original_reviewer`                               | 丟出 actor mismatch；不建立新 revision                                                 | existing INITIAL submitted submission；non-owner actor                   | API                            |
| EX-006     | Application Service Test | `ReviseRereviewUseCaseTest`                | `should_create_new_rereview_revision_when_actor_is_assigned_rereviewer_and_case_is_open`           | 建立新 rereview revision；舊版 SUPERSEDED；case status 不變                              | existing REREVIEW submitted submission；assigned rereviewer actor        | Domain / API / Persistence     |
| EX-007     | Domain Unit Test         | `ReviewSubmissionRevisionPolicyTest`       | `should_reject_revision_when_case_is_closed`                                                       | revision 被拒絕；無新 submission；無 revised event                                      | closed case；submitted submission                                        | Application / API              |
| EX-008     | Domain Unit Test         | `ReviewSubmissionRevisionPolicyTest`       | `should_reject_revision_when_case_is_cancelled`                                                    | revision 被拒絕；無新 submission                                                      | cancelled case；submitted submission                                     | Application / API              |
| EX-009     | Application Service Test | `ExecuteSamplingUseCaseTest`               | `should_create_batch_snapshot_selected_cases_and_review_cases_when_sampling_executes_successfully` | 建立 batch / snapshot / selected cases / review cases；寫出 event                    | active project；published version；candidate data adapter fake            | API / Persistence / Acceptance |
| EX-010     | Persistence Test         | `SamplingBatchSelectedCasePersistenceTest` | `should_enforce_unique_source_case_key_within_same_batch`                                          | `(batch_id, source_case_key)` unique 生效                                         | batch A；same sourceCaseKey twice                                        | Domain / Application           |
| EX-011     | Application Service Test | `ExecuteSamplingUseCaseTest`               | `should_allow_same_source_case_key_in_different_batches`                                           | batch A 與 batch B 可各自成功建立 selected case                                         | two batches；same sourceCaseKey                                          | Persistence                    |
| EX-012     | Application Service Test | `RetrySamplingBatchUseCaseTest`            | `should_create_new_batch_and_keep_retry_reference_when_retrying_failed_batch`                      | 新 batchId；retryOfBatchId 指回舊 batch；舊 batch 不變                                   | failed batch fixture                                                    | Persistence / Acceptance       |
| EX-013     | API Integration Test     | `ReviewCaseDetailApiTest`                  | `should_return_full_detail_view_for_hospital_manager_within_same_hospital`                         | response 含 answers/comments/attachments metadata                                | hospital manager token；same-hospital case                               | Acceptance                     |
| EX-014     | API Integration Test     | `ReviewCaseDetailApiTest`                  | `should_return_metadata_only_view_for_system_admin`                                                | response 不含 answers/full comments/attachment content                            | system admin token；existing case/submission                             | Acceptance                     |
| EX-015     | API Integration Test     | `ReviewCaseDetailApiTest`                  | `should_forbid_auditor_access_outside_authorized_scope`                                            | A 院區成功；B 院區 403/empty                                                           | auditor token with hospital A scope                                     | Acceptance                     |
| EX-016     | Persistence Test         | `ReviewCasePersistenceTest`                | `should_persist_project_batch_version_and_snapshot_references_for_review_case`                     | projectId / batchId / projectVersionId / scoreSheetVersionId / snapshot ref 都可查 | created case from sampling flow                                         | API / Acceptance               |
| EX-017     | Persistence Test         | `ReviewSubmissionSnapshotPersistenceTest`  | `should_persist_score_sheet_snapshot_reference_for_submission`                                     | versionId / checksum / schemaVersion / snapshot ref 可追溯                         | submitted submission with snapshot                                      | API                            |
| EX-018     | Domain Unit Test         | `ReviewProjectVersionPolicyTest`           | `should_reject_direct_modification_of_published_version`                                           | published version 不可 update                                                     | published project version fixture                                       | Application / API              |
| EX-019     | Domain Unit Test         | `ReviewProjectActivationPolicyTest`        | `should_reject_sampling_when_project_is_not_active`                                                | project inactive 時抽選被拒絕                                                         | inactive project                                                        | Application / API              |
| EX-020     | Domain Unit Test         | `ReviewCaseTest`                           | `should_reject_start_initial_review_when_case_is_not_assigned`                                     | 狀態不變；拋 invalid transition                                                       | case in SELECTED_PENDING_ASSIGNMENT                                     | Application / API              |
| EX-021     | Domain Unit Test         | `ReviewCaseTest`                           | `should_reject_start_rereview_when_case_is_not_pending_rereview`                                   | 狀態不變；拋 invalid transition                                                       | case not in PENDING_REREVIEW                                            | Application / API              |
| EX-022     | Application Service Test | `ExecuteSamplingUseCaseTest`               | `should_reject_sampling_when_only_draft_project_version_exists`                                    | 不建立 batch；拋 published version missing / invalid state                           | project with draft only                                                 | API                            |
| EX-023     | API Integration Test     | `ReviewSubmissionRevisionApiTest`          | `should_reject_revision_request_when_api_stage_does_not_match_submission_stage`                    | 422 SUBMISSION_STAGE_MISMATCH                                                   | REREVIEW submission；call INITIAL revise API                             | Application                    |
| EX-024     | API Integration Test     | `ReviewSubmissionApiTest`                  | `should_not_expose_generic_update_for_submitted_submission`                                        | API 不存在或被拒絕；只能 revise                                                           | submitted submission                                                    | Application / Acceptance       |
| EX-025     | API Integration Test     | `ReportApiTest`                            | `should_apply_visibility_and_scope_rules_to_dashboard_and_report_queries`                          | sysadmin 只看 aggregated；auditor 限授權 scope                                        | system admin token；auditor scoped token；report data                     | Acceptance                     |

---

## 5. 建議的測試類別拆分

---

## 5.1 Domain Unit Test 類別建議

```yaml id="qxm4ys"
domain_test_classes:
  - ReviewCaseTest
  - ReviewSubmissionRevisionPolicyTest
  - ReviewProjectVersionPolicyTest
  - ReviewProjectActivationPolicyTest
```

### `ReviewCaseTest`

建議先承接：

* EX-001
* EX-002
* EX-003
* EX-020
* EX-021

### `ReviewSubmissionRevisionPolicyTest`

建議先承接：

* EX-007
* EX-008

### `ReviewProjectVersionPolicyTest`

建議先承接：

* EX-018

### `ReviewProjectActivationPolicyTest`

建議先承接：

* EX-019

---

## 5.2 Application Service Test 類別建議

```yaml id="52xz7h"
application_test_classes:
  - ExecuteSamplingUseCaseTest
  - RetrySamplingBatchUseCaseTest
  - ReviseInitialReviewUseCaseTest
  - ReviseRereviewUseCaseTest
```

### `ExecuteSamplingUseCaseTest`

建議先承接：

* EX-009
* EX-011
* EX-022

### `RetrySamplingBatchUseCaseTest`

建議先承接：

* EX-012

### `ReviseInitialReviewUseCaseTest`

建議先承接：

* EX-004
* EX-005

### `ReviseRereviewUseCaseTest`

建議先承接：

* EX-006

---

## 5.3 API Integration Test 類別建議

```yaml id="jlwm6y"
api_test_classes:
  - ReviewCaseDetailApiTest
  - ReviewSubmissionRevisionApiTest
  - ReviewSubmissionApiTest
  - ReportApiTest
```

### `ReviewCaseDetailApiTest`

建議先承接：

* EX-013
* EX-014
* EX-015

### `ReviewSubmissionRevisionApiTest`

建議先承接：

* EX-023

### `ReviewSubmissionApiTest`

建議先承接：

* EX-024

### `ReportApiTest`

建議先承接：

* EX-025

---

## 5.4 Persistence Test 類別建議

```yaml id="b1u76r"
persistence_test_classes:
  - SamplingBatchSelectedCasePersistenceTest
  - ReviewCasePersistenceTest
  - ReviewSubmissionSnapshotPersistenceTest
```

### `SamplingBatchSelectedCasePersistenceTest`

建議先承接：

* EX-010

### `ReviewCasePersistenceTest`

建議先承接：

* EX-016

### `ReviewSubmissionSnapshotPersistenceTest`

建議先承接：

* EX-017

---

## 5.5 Acceptance Test 類別建議

```yaml id="twe865"
acceptance_test_classes:
  - InitialReviewDirectCloseAcceptanceTest
  - InitialReviewToRereviewAcceptanceTest
  - FailedBatchRetryAcceptanceTest
  - VisibilityScopeAcceptanceTest
```

### 建議承接

* EX-001
* EX-002
* EX-003
* EX-012
* EX-013
* EX-014
* EX-015
* EX-025

---

## 6. 建議的最小骨架清單

如果現在要先落地最小一批測試骨架，我建議先建立以下類別與方法：

```yaml id="ih2m66"
minimum_test_skeletons:
  - class: ReviewCaseTest
    methods:
      - should_close_case_when_initial_score_is_not_above_threshold
      - should_mark_case_pending_rereview_when_initial_score_exceeds_threshold
      - should_finalize_score_from_rereview_submission_when_rereview_is_completed
      - should_reject_start_initial_review_when_case_is_not_assigned
      - should_reject_start_rereview_when_case_is_not_pending_rereview

  - class: ReviewSubmissionRevisionPolicyTest
    methods:
      - should_reject_revision_when_case_is_closed
      - should_reject_revision_when_case_is_cancelled

  - class: ReviseInitialReviewUseCaseTest
    methods:
      - should_create_new_initial_revision_when_actor_is_original_reviewer_and_case_is_open
      - should_reject_initial_revision_when_actor_is_not_original_reviewer

  - class: ReviseRereviewUseCaseTest
    methods:
      - should_create_new_rereview_revision_when_actor_is_assigned_rereviewer_and_case_is_open

  - class: ExecuteSamplingUseCaseTest
    methods:
      - should_create_batch_snapshot_selected_cases_and_review_cases_when_sampling_executes_successfully
      - should_allow_same_source_case_key_in_different_batches
      - should_reject_sampling_when_only_draft_project_version_exists

  - class: SamplingBatchSelectedCasePersistenceTest
    methods:
      - should_enforce_unique_source_case_key_within_same_batch

  - class: ReviewCaseDetailApiTest
    methods:
      - should_return_full_detail_view_for_hospital_manager_within_same_hospital
      - should_return_metadata_only_view_for_system_admin
      - should_forbid_auditor_access_outside_authorized_scope

  - class: ReviewSubmissionRevisionApiTest
    methods:
      - should_reject_revision_request_when_api_stage_does_not_match_submission_stage
```

---

## 7. Fixture 設計建議

### 7.1 共用 fixture builder

建議先準備以下 builder / fixture factory：

```yaml id="5uhjuo"
fixture_builders:
  - ReviewProjectFixture
  - ProjectVersionFixture
  - ScoreSheetVersionFixture
  - SamplingBatchFixture
  - ReviewCaseFixture
  - ReviewSubmissionFixture
  - UserFixture
  - ScopedAuthFixture
```

### 7.2 高頻 fixture 組合

```yaml id="sa032m"
common_fixture_sets:
  - active_project_with_published_version
  - inactive_project
  - case_pending_initial_review
  - case_initial_review_in_progress
  - case_pending_rereview
  - case_rereview_in_progress
  - closed_case_with_submitted_submission
  - cancelled_case_with_submitted_submission
  - initial_submitted_submission_owned_by_reviewer
  - rereview_submitted_submission_owned_by_rereviewer
```

---

## 8. 對應 `test-scenarios.md` 的落地原則

### 8.1 一個 example 不一定只對一個 test method

例如 EX-004 對應：

* happy path revision
* append-only assertion
* state unchanged assertion

所以可拆成多個方法。

### 8.2 一個 test scenario 也可能拆成多層測試

例如 TS-RV-005：

* Domain：closed case revision guard
* Application：closed case command rejection
* API：closed case returns 409/422
