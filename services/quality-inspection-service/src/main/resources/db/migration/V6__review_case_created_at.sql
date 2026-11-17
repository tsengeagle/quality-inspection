-- D-057 第 1 點：案件建立時間（UTC），案件建立時寫入，之後不得修改；報表與匯出依它過濾時間區間（含起不含迄）。
-- 可為 NULL：V6 之前已存在的案件沒有可信的建立時間，保留 NULL、不回填，也不會落在任何時間區間內。
-- 正式環境導入前沒有資料（D-045）。
alter table review_case add created_at timestamp;
create index ix_review_case_hospital_created on review_case (hospital_id, created_at);
