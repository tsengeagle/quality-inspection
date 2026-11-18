// 驗證 ruleset 用的探針，不得合併。
// 目的：確認 frontend-check 失敗時，main 的 ruleset 會擋下合併。驗證完關閉 PR，刪除分支。
describe('ruleset probe（不得合併）', () => {
  it('故意失敗，讓 frontend-check 變紅', () => {
    expect('frontend-check 應該擋下合併').toBe('這個測試永遠不會通過');
  });
});
