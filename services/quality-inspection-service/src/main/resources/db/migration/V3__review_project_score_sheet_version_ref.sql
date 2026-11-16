-- D-045 Pending（2026-11-16 負責人裁決，PR #42）：專案綁定的評分表參照自 PR #42 起存的就是評分表版本 id，
-- 欄位改名為 score_sheet_version_ref，與 specs/review-project 的 ProjectVersion.scoreSheetVersionRef
-- 及 review_case.score_sheet_version_ref 一致。
-- 不需要資料轉換：導入 Flyway（V1）之前只有記憶體資料庫，沒有以評分表 id 儲存的既有資料。
alter table review_project rename column bound_score_sheet_id to score_sheet_version_ref;
