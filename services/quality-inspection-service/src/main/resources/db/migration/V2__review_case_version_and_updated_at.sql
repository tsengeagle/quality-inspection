-- D-045 第 1 點：案件的樂觀鎖版本欄位；既有資料從 0 開始。
-- D-037 第 1、6 點：案件層級的最後更新時間（UTC）；既有資料沒有可信的歷史時間，保留 NULL，不回填。
alter table review_case add version number(19,0) default 0 not null;
alter table review_case add updated_at timestamp;
