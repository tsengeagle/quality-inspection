-- D-054 第 2 點：初審固定記在案件上，不新增案件狀態；initial_fixed_at 有值即已固定。
-- 三個欄位都可為 NULL：尚未固定的案件（含既有資料）維持 NULL，不回填。
alter table review_case add initial_fixed_at timestamp;
alter table review_case add initial_fixed_by varchar2(255 char);
alter table review_case add initial_fixed_submission_id varchar2(255 char);
