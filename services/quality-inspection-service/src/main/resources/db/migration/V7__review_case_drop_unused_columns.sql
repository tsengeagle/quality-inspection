-- D-084 第 9 點（接 D-083 第 10 點）：rereview_required、final_conclusion、rule_snapshot_ref 沒有任何行為讀取，移除。
-- 是否進複審由案件狀態表示（D-054）；正式環境導入前沒有資料（D-045）。
alter table review_case drop column rereview_required;
alter table review_case drop column final_conclusion;
alter table review_case drop column rule_snapshot_ref;
