"""及格線實驗：逐一弄壞偵測器（或改需求、手寫違規），跑完整的後端測試，記錄閘門有沒有擋下。只動實驗用的 worktree。"""
import json, os, re, subprocess, sys, time
W = sys.argv[1]; OUT = sys.argv[2]
SVC = os.path.join(W, "services/quality-inspection-service")
T = "services/quality-inspection-service/src/test/java/com/tzuchi/his/quality/inspection/"
M = "services/quality-inspection-service/src/main/java/com/tzuchi/his/quality/inspection/"
A = T + "acceptance/"
env = dict(os.environ, JAVA_HOME="/usr/lib/jvm/java-17-openjdk-amd64")

def sh(*a, cwd=W): return subprocess.run(a, cwd=cwd, capture_output=True, text=True, env=env)
def reset():
    sh("git", "checkout", "-q", "--", "."); sh("git", "clean", "-fdq", "--", "services", "specs", "harness-core", "law")
def rep(path, old, new, count=1, nth=None):
    p = os.path.join(W, path); s = open(p, encoding="utf-8").read()
    assert s.count(old) == count, (path, old[:50], s.count(old))
    if nth is None: s = s.replace(old, new)
    else:
        i = -1
        for _ in range(nth): i = s.index(old, i + 1)
        s = s[:i] + new + s[i + len(old):]
    open(p, "w", encoding="utf-8", newline="").write(s)
def noop(path, sig, ret="return;"):
    rep(path, sig, sig + "\n        if (true) " + ret)
def write(path, text):
    p = os.path.join(W, path); os.makedirs(os.path.dirname(p), exist_ok=True); open(p, "w", encoding="utf-8", newline="\n").write(text)

def gradle():
    r = sh("./gradlew", "test", "--console=plain", cwd=SVC)
    out = r.stdout + r.stderr
    ok = "BUILD SUCCESSFUL" in out
    gate = re.search(r"測試閘門：.*", out)
    i = out.find("測試閘門未通過")
    problems = out[i:i + 1800] if i >= 0 else ""
    compile_err = [l for l in out.splitlines() if "error:" in l][:3]
    failed = sorted(set(re.findall(r"^(\S+ > .+) FAILED$", out, re.M)))
    return {"build": "通過" if ok else "未通過", "gate": gate.group(0) if gate else "", "failed_tests": len(failed),
            "failed_sample": failed[:6], "problems": problems, "compile": compile_err}
def pytests():
    r = sh(sys.executable, "-m", "unittest", "discover", "-s", "harness-core/tests")
    tail = (r.stdout + r.stderr).strip().splitlines()
    fails = [l for l in tail if l.startswith(("FAIL:", "ERROR:"))]
    return {"build": "通過" if r.returncode == 0 else "未通過", "gate": tail[-1] if tail else "", "failed_tests": len(fails), "failed_sample": fails[:6], "problems": "", "compile": []}

EXP = []
def exp(name, kind, what, runner=gradle):
    def deco(fn): EXP.append((name, kind, what, fn, runner)); return fn
    return deco

@exp("J01", "弄壞偵測器", "ARCH-05：規則改成永遠不會違規")
def _(): rep(T + "architecture/ArchitectureRules.java", 'JavaClass.Predicates.resideInAPackage(root + ".application..")', 'JavaClass.Predicates.resideInAPackage(root + ".nosuch..")')
@exp("J02", "弄壞偵測器", "範例執行器：不檢查 SUCCESS／REJECTED 與錯誤代碼（checkResult）")
def _(): noop(A + "ExampleRunner.java", "    private static void checkResult(String where, Map<String, Object> expected, Outcome actual) {")
@exp("J03", "弄壞偵測器", "範例執行器（格式 v1）：不比對輸出（compareOutput）")
def _(): noop(A + "ExampleRunner.java", "    private static void compareOutput(String where, Map<String, Object> expected, Map<String, Object> actual) {")
@exp("J04", "弄壞偵測器", "範例執行器（格式 v2）：輸出斷言（output、absent、only）有問題也不報")
def _(): rep(A + "ExampleRunner.java", '        if (!problems.isEmpty()) {\n            throw new AssertionError(where + "：" + String.join("；", problems));', '        if (false) {\n            throw new AssertionError(where + "：" + String.join("；", problems));', count=2, nth=2)
@exp("J05", "弄壞偵測器", "範例執行器：不檢查事件（checkEvents）")
def _(): noop(A + "ExampleRunner.java", "    private void checkEvents(Map<String, Object> then, Outcome outcome, List<ExampleWorld.EmittedEvent> events,\n                             Map<String, Object> vars, ZoneId zone) {")
@exp("J06", "弄壞偵測器", "範例執行器：執行到沒有設定的埠也不報（failOnUnconfiguredPorts）")
def _(): noop(A + "ExampleRunner.java", "    private void failOnUnconfiguredPorts(String where) {")
@exp("J07", "弄壞偵測器", "範例執行器：不檢查 HTTP 狀態碼（checkHttpStatus）")
def _(): noop(A + "ExampleRunner.java", "    private void checkHttpStatus(Map<String, Object> then, HttpInvoker.Reply reply) {")
@exp("J08", "弄壞偵測器", "HTTP 契約比對器：不看任何回應（observe）")
def _(): noop(A + "http/HttpContractEvaluator.java", "    public void observe(HttpObservation o) {")
@exp("J09", "弄壞偵測器", "HTTP 契約比對器：涵蓋要求永遠沒有問題（coverageProblems）")
def _(): rep(A + "http/HttpContractEvaluator.java", "    public List<String> coverageProblems() {\n        List<String> all = new ArrayList<>();", "    public List<String> coverageProblems() {\n        List<String> all = new ArrayList<>();\n        if (true) return all;")
@exp("J10", "弄壞偵測器", "HTTP 契約比對器：形狀永遠相符（JsonShape.check）")
def _(): rep(A + "http/JsonShape.java", '        if (shape == null) {\n            out.add(path + "：契約沒有這個位置的形狀，無法比對");', '        if (true) return;\n        if (shape == null) {\n            out.add(path + "：契約沒有這個位置的形狀，無法比對");')
@exp("J11", "弄壞偵測器", "HTTP 觀察器：不記錄任何回應（record）")
def _(): rep(A + "http/HttpObservationRecorder.java", "    static void record(HttpObservation observation) {\n        OBSERVATIONS.add(observation);", "    static void record(HttpObservation observation) {")
@exp("J12", "弄壞偵測器", "驗收測試：列出每個範例但不執行（拿掉 runner.run）")
def _(): rep(A + "AcceptanceExamplesTest.java", "                    runner.run(name);", "                    /* 不執行 */")
@exp("J13", "弄壞偵測器", "HTTP 契約報告：問題數永遠寫 0")
def _(): rep(A + "http/HttpContractReport.java", '"\\nenforced-problems: " + enforced.size() + "\\n"', '"\\nenforced-problems: " + 0 + "\\n"')
@exp("R01", "改需求不改程式", "review-case 的範例：稽核 detail 的預期值從 5 改成 6")
def _(): rep("specs/review-case/examples.yaml", """detail: '{"finalScore":5}'""", """detail: '{"finalScore":6}'""")
@exp("R02", "改需求不改程式", "HTTP 契約：提交初審的回應多要求一個欄位 extra")
def _(): rep("specs/review-case/http.yaml", "        caseStatus: enum CaseStatus\n        totalScore: decimal\n", "        caseStatus: enum CaseStatus\n        totalScore: decimal\n        extra: text\n")
@exp("R03", "改需求不改程式", "HTTP 契約：提交初審的回應拿掉 totalScore（程式仍然輸出）")
def _(): rep("specs/review-case/http.yaml", "        caseStatus: enum CaseStatus\n        totalScore: decimal\n", "        caseStatus: enum CaseStatus\n")
@exp("V02", "手寫違規", "程式：提交初審的回應 dto 多輸出一個欄位")
def _(): rep(M + "interfaces/rest/dto/SubmitInitialReviewResponse.java", "    public BigDecimal getTotalScore() { return totalScore; }\n}", '    public BigDecimal getTotalScore() { return totalScore; }\n    public String getExtra() { return "x"; }\n}')
@exp("V03", "手寫違規", "程式：reporting 引用 application.common 以外的 application 類別")
def _(): rep(M + "reporting/DashboardSummary.java", "public class DashboardSummary {\n", "public class DashboardSummary {\n\n    static final Class<?> PROBE = com.tzuchi.his.quality.inspection.application.reviewcase.ListReviewCasesUseCase.class;\n")
@exp("G01", "弄壞偵測器", "建置的測試閘門：不在清單上的失敗不擋（build.gradle），同時放一個會失敗的測試")
def _():
    rep("services/quality-inspection-service/build.gradle", "        if (unexpected) {", "        if (false) {")
    write(T + "GateProbeTest.java", "package com.tzuchi.his.quality.inspection;\n\nimport org.junit.jupiter.api.Test;\nimport static org.junit.jupiter.api.Assertions.fail;\n\nclass GateProbeTest {\n    @Test\n    void alwaysFails() { fail(\"probe\"); }\n}\n")
@exp("P01", "弄壞偵測器", "判決程式：涵蓋清單以外的新範圍不擋（judge_coverage）", pytests)
def _(): rep("harness-core/law_check.py", "        if entry is None:\n            outside.append(path)", "        if entry is None:\n            pass")
@exp("P02", "弄壞偵測器", "判決程式：暫准範圍的修改不提醒", pytests)
def _(): rep("harness-core/law_check.py", "            reminders.append((entry[3], path))", "            pass")
@exp("P03", "弄壞偵測器", "規格檢查：不檢查規則的出處（check_sources）", pytests)
def _(): rep("harness-core/spec_lint.py", "    def check_sources(self, where, sources, all_ids):\n", "    def check_sources(self, where, sources, all_ids):\n        return\n")
@exp("P04", "弄壞偵測器", "規格檢查：不檢查禁用詞彙（check_vocabulary）", pytests)
def _(): rep("harness-core/spec_lint.py", "    def check_vocabulary(self):\n", "    def check_vocabulary(self):\n        return\n")
@exp("P05", "弄壞偵測器", "規格檢查：不檢查範例是否可區分（check_distinguishable）", pytests)
def _(): rep("harness-core/spec_lint.py", '    def check_distinguishable(self, doc, fixtures):\n        """D-063 第 4 點：given（展開 fixture 後）與 when 相同的兩個範例，then 不能不同。"""\n', '    def check_distinguishable(self, doc, fixtures):\n        """D-063 第 4 點：given（展開 fixture 後）與 when 相同的兩個範例，then 不能不同。"""\n        return\n')
@exp("P06", "弄壞偵測器", "規格檢查：不檢查 http 步驟的端點是否在契約裡（check_http_endpoint）", pytests)
def _(): rep("harness-core/spec_lint.py", '    def check_http_endpoint(self, where, method, path):\n        """目錄有 http.yaml 時，method 與 path 必須是其中的端點；路徑參數 {x} 對到 ${變數} 或任何值。"""\n', '    def check_http_endpoint(self, where, method, path):\n        """目錄有 http.yaml 時，method 與 path 必須是其中的端點；路徑參數 {x} 對到 ${變數} 或任何值。"""\n        return\n')

@exp("J14", "弄壞偵測器", "驗收測試：名稱以 rejects 開頭的範例列出但不執行")
def _(): rep(A + "AcceptanceExamplesTest.java", "                    runner.run(name);", "                    if (!name.startsWith(\"rejects\")) runner.run(name);")

only = set(sys.argv[3].split(",")) if len(sys.argv) > 3 else None
results = []
if os.path.exists(OUT): results = json.load(open(OUT, encoding="utf-8"))
done = {r["id"] for r in results}
reset()
if "BASE" not in done and (not only or "BASE" in only):
    t = time.time(); r = gradle(); r.update(id="BASE", kind="對照", what="不做任何修改", secs=int(time.time() - t)); results.append(r)
    json.dump(results, open(OUT, "w", encoding="utf-8"), ensure_ascii=False, indent=1)
for name, kind, what, fn, runner in EXP:
    if name in done or (only and name not in only): continue
    reset(); t = time.time()
    try:
        fn(); r = runner()
    except AssertionError as e:
        r = {"build": "實驗無效", "gate": "修改點找不到：%r" % (e,), "failed_tests": 0, "failed_sample": [], "problems": "", "compile": []}
    r.update(id=name, kind=kind, what=what, secs=int(time.time() - t)); results.append(r)
    json.dump(results, open(OUT, "w", encoding="utf-8"), ensure_ascii=False, indent=1)
    print(name, r["build"], r["failed_tests"], flush=True)
reset()
print("DONE", flush=True)
