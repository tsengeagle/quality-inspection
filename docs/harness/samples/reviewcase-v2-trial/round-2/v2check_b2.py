"""暫時的 v2 檢查（B 組，第二輪）。

核對 15-spec-format-v2-draft.md 第四節第 4～8 項，加上第十三節 Q-10～Q-19 建議的寫法。
不是 spec_lint，不屬於 harness-core。執行器沒有跑：這支程式不知道範例會不會通過。
用法：python v2check_b2.py <樣本目錄> <已立法的 specs 目錄>
"""
import json, pathlib, re, sys
import yaml

root, law = pathlib.Path(sys.argv[1]), pathlib.Path(sys.argv[2])
def load(p): return yaml.safe_load(p.read_text(encoding="utf-8")) if p.exists() else {}

problems, notes = [], []
def problem(where, msg): problems.append("%s：%s" % (where, msg))

# ---- 收集：樣本目錄與已立法的規格放在同一個名稱空間（Q-17）----
dirs = {}
for base, trial in ((law, False), (root, True)):
    for d in sorted(p for p in base.iterdir() if p.is_dir()):
        dirs[d.name] = {"trial": trial, "m": load(d / "model.yaml"), "u": load(d / "usecases.yaml"), "e": load(d / "examples.yaml")}

rules, kinds, usecases, fixtures, ports = {}, {}, {}, {}, {}
for name, d in dirs.items():
    m, u, e = d["m"], d["u"], d["e"]
    def add(kind, r):
        if d["trial"] and r["id"] in rules: problem(name, "規則 id %s 重複" % r["id"])
        rules[r["id"]] = r; kinds[r["id"]] = (name, kind)
    for key in ("transitions", "invariants"):
        for r in m.get(key) or []: add(key, r)
    for g in u.get("guards") or []: add("guards", g)
    for uc in u.get("usecases") or []:
        usecases[(name, uc["name"])] = uc
        for r in uc.get("rules") or []: add("rules", r)
        for r in (uc.get("success") or {}).get("state") or []: add("state", r)
    for fname, fx in (e.get("fixtures") or {}).items(): fixtures[(name, fname)] = fx
    for p in m.get("ports") or []: ports[(name, p["name"])] = {o["name"] for o in p["operations"]}

def resolve(here, ref, table):
    """X 先找本目錄；<目錄>.X 找指定的目錄（Q-17）。"""
    if (here, ref) in table: return (here, ref)
    head, _, rest = ref.partition(".")
    return (head, rest) if (head, rest) in table else None

SEG = r"[A-Za-z][A-Za-z0-9]*(\[(\*|\d+|[A-Za-z]\w*=[^\],]+(,[A-Za-z]\w*=[^\],]+)*)\])?"
PATH = re.compile(r"^(\.|%s(\.%s)*)$" % (SEG, SEG))
def check_path(where, path, what, allow_index=False):
    if not isinstance(path, str) or not PATH.match(re.sub(r"\$\{\w+\}", "v", path)):
        problem(where, "%s 的路徑不合 Q-11 的寫法：%s" % (what, path)); return
    if re.search(r"\[\d+\]", path) and not allow_index: problem(where, "%s 不能用 [數字]，只有 save 可以：%s" % (what, path))

ZONED = re.compile(r"^\d{4}-\d\d-\d\dT\d\d:\d\d:\d\d(Z|[+-]\d\d:\d\d)$")
def check_times(where, node):
    if isinstance(node, dict):
        for v in node.values(): check_times(where, v)
    elif isinstance(node, list):
        for v in node: check_times(where, v)
    elif isinstance(node, str) and re.match(r"^\d{4}-\d\d-\d\dT", node) and not ZONED.match(node):
        problem(where, "時間沒有時區（Q-14）：%s" % node)

# ---- 模型：轉移、還沒立法的轉移（Q-16）、跨目錄的 by（Q-17）----
DEFERRED = {"from", "to", "by", "when", "note"}
for name, d in dirs.items():
    if not d["trial"]: continue
    m = d["m"]; enums = {e["name"]: e["values"] for e in m.get("enums") or []}
    def same_enum(t): return any(t["from"] in vs and t["to"] in vs for vs in enums.values())
    seen = set()
    for t in m.get("transitions") or []:
        where = "%s %s" % (name, t["id"])
        if not resolve(name, t["by"], usecases): problem(where, "by %s 不是規格裡的 use case" % t["by"])
        if not same_enum(t): problem(where, "from、to 不屬於同一個列舉")
        key = (t["from"], t["by"], t.get("when"))
        if key in seen: problem(where, "同一個 from、同一支 use case 的轉移要用 when 區分")
        seen.add(key)
    for i, t in enumerate(m.get("deferred_transitions") or []):
        where = "%s deferred_transitions 第 %d 項" % (name, i + 1)
        if set(t) - DEFERRED: problem(where, "不認得的欄位 %s" % sorted(set(t) - DEFERRED))
        if not same_enum(t): problem(where, "from、to 不屬於同一個列舉")
        if resolve(name, t["by"], usecases): problem(where, "%s 已經在規格裡，這條轉移應該搬到 transitions" % t["by"])
    for s in m.get("terminal_states") or []:
        if not any(s in vs for vs in enums.values()): problem(name, "terminal_states 的 %s 不是列舉的值" % s)

# ---- 範例 ----
ASSERT = {"result", "code", "output", "absent", "only", "emits", "emits_include", "use_expect"}
covered = set()
total = expanded_total = 0
for name, d in dirs.items():
    if not d["trial"] or not d["e"]: continue
    ex, u = d["e"], d["u"]
    codes = {c["code"] for c in u.get("error_codes") or []}
    actors, expectations = ex.get("actors") or {}, ex.get("expectations") or {}

    def expand(steps, here, seen=()):
        out = []
        for s in steps or []:
            hit = resolve(here, s["use"], fixtures) if "use" in s else None
            if hit and hit not in seen: out += expand(fixtures[hit].get("steps"), hit[0], seen + (hit,))
            else: out.append(s)
        return out

    def check_assert(where, a, target):
        """then、expect、具名斷言共用（Q-10）。target 是被比對的 use case。"""
        if set(a) - ASSERT: problem(where, "不認得的欄位 %s" % sorted(set(a) - ASSERT))
        if a.get("result") not in ("SUCCESS", "REJECTED"): problem(where, "result 不是 SUCCESS 或 REJECTED")
        if a.get("result") == "REJECTED":
            if not a.get("code"): problem(where, "REJECTED 沒有 code")
            for k in ("output", "absent", "only", "emits", "emits_include"):
                if k in a: problem(where, "REJECTED 不能有 %s（Q-5、Q-15）" % k)
        owner_codes = codes
        if target and dirs[target[0]]["trial"]: owner_codes = {c["code"] for c in dirs[target[0]]["u"].get("error_codes") or []}
        if a.get("code") and a["code"] not in owner_codes: problem(where, "代碼 %s 沒有定義" % a["code"])
        for k in (a.get("output") or {}):
            if "[" in k: check_path(where, k, "output 的鍵")
        for p in a.get("absent") or []: check_path(where, p, "absent")
        for p, fields in (a.get("only") or {}).items():
            check_path(where, p, "only")
            if not isinstance(fields, list) or not fields: problem(where, "only 的 %s 要是欄位清單" % p)
        check_times(where, a.get("output"))
        declared = {}
        if target: declared = {e["event"]: e for e in ((usecases.get(target) or {}).get("success") or {}).get("emits") or []}
        for k in ("emits", "emits_include"):
            for ev in a.get(k) or []:
                if ev["event"] not in declared: problem(where, "事件 %s 沒有在 %s 的 success.emits 宣告" % (ev["event"], target[1] if target else "?"))
                elif declared[ev["event"]].get("status") == "proposed":
                    notes.append("%s：斷言了 proposed 的事件 %s，還不能當驗收依據（殘留 R-1）" % (where, ev["event"]))

    def check_step(where, s, here=name):
        if "use" in s:
            if not resolve(here, s["use"], fixtures): problem(where, "fixture %s 不存在" % s["use"])
            return None
        if "advance" in s:
            if not re.fullmatch(r"P(\d+D)?(T(\d+H)?(\d+M)?(\d+S)?)?", str(s["advance"])): problem(where, "advance 不是 ISO 8601 期間")
            return None
        if "port" in s:                                   # Q-19：只有宣告與 returns
            head, _, op = s["port"].rpartition(".")
            hit = resolve(here, head, ports)
            if not hit: problem(where, "埠 %s 不存在" % head)
            elif op not in ports[hit]: problem(where, "埠 %s 沒有操作 %s" % (head, op))
            if set(s) - {"port", "returns"}: problem(where, "v2.0 的埠只能設定 returns：%s" % sorted(set(s) - {"port", "returns"}))
            return None
        if "run" not in s: problem(where, "步驟沒有 run、use、port 或 advance"); return None
        target = resolve(here, s["run"], usecases)
        if not target: problem(where, "use case %s 不在任何一份規格裡" % s["run"])
        if s.get("as") and "${" not in s["as"] and s["as"] not in actors: problem(where, "操作者 %s 不存在" % s["as"])
        for var, path in (s.get("save") or {}).items(): check_path(where, path, "save", allow_index=True)
        check_times(where, s.get("input"))
        if "expect" in s: check_assert(where + " expect", s["expect"], target)
        return target

    for fname, fx in (ex.get("fixtures") or {}).items():
        for s in fx.get("steps") or []: check_step("%s fixture %s" % (name, fname), s)
    for ename, a in expectations.items(): check_assert("%s 具名斷言 %s" % (name, ename), a, None)

    expanded = []
    for e in ex.get("examples") or []:
        total += 1
        if "matrix" not in e: expanded.append((e["name"], e)); continue
        mx = e["matrix"]; cols = mx["columns"]; key = mx.get("key")
        where = "%s %s" % (name, e["name"])
        if not key or any(k not in cols for k in key): problem(where, "matrix.key 必須列出組成名稱的欄（Q-13）"); key = cols[:1]
        for i, row in enumerate(mx["rows"]):
            if len(row) != len(cols): problem(where, "矩陣第 %d 列欄數不符" % (i + 1)); continue
            vals = dict(zip(cols, row))
            text = json.dumps({k: v for k, v in e.items() if k != "matrix"}, ensure_ascii=False)
            for k, v in vals.items(): text = text.replace("${%s}" % k, str(v))
            expanded.append((".".join([e["name"]] + [str(vals[k]) for k in key]), json.loads(text)))
    expanded_total += len(expanded)

    names = set()
    for ename, e in expanded:
        where = "%s %s" % (name, ename)
        if ename in names: problem(where, "展開後名稱重複（matrix.key 的組合不唯一）")
        names.add(ename)
        if not re.fullmatch(r"[A-Za-z0-9_.\-]+", ename): problem(where, "名稱只能用英數字與 _ . -")
        if e.get("needs"): notes.append("%s：needs %s" % (where, e["needs"]))
        for b in e.get("basis") or []:
            if b not in rules: problem(where, "basis %s 不存在" % b); continue
            covered.add(b)
            if rules[b].get("status") == "proposed": notes.append("%s：basis 含 proposed %s" % (where, b))
        for s in e.get("given") or []: check_step(where, s)
        for s in e.get("observe") or []: check_step(where + " observe", s)
        target = check_step(where + " when", e.get("when") or {})
        then = dict(e.get("then") or {})
        if "use_expect" in then:
            if then["use_expect"] not in expectations: problem(where, "具名斷言 %s 不存在" % then["use_expect"]); continue
            then = dict(expectations[then["use_expect"]], **{k: v for k, v in then.items() if k != "use_expect"})
        check_assert(where + " then", then, target)

    # D-063 第 4 點，依 Q-20 修正：情境相同時，只有 result 或 code 不同才是「不可能同時通過」。
    # 其餘的斷言不同，可能是各驗同一個輸出的不同面向，只列出。
    def verdict(e):
        then = dict(e.get("then") or {})
        if then.get("use_expect") in expectations: then = dict(expectations[then["use_expect"]], **then)
        return (then.get("result"), then.get("code"))
    seen = {}
    for ename, e in expanded:
        key = json.dumps([expand(e.get("given"), name), e.get("when")], sort_keys=True, ensure_ascii=False)
        if key in seen:
            if verdict(e) != seen[key][1]: problem("%s %s" % (name, ename), "given、when 與 %s 相同，result 或 code 卻不同" % seen[key][0])
            else: notes.append("%s %s：given、when 與 %s 相同，斷言不同的面向（Q-20）" % (name, ename, seen[key][0]))
        seen.setdefault(key, (ename, verdict(e)))

trial_rules = [r for r in sorted(rules) if dirs[kinds[r][0]]["trial"]]
for rid in trial_rules:
    if rid in covered: continue
    (problems if kinds[rid][1] == "transitions" else notes).append("%s %s：沒有範例涵蓋（%s）" % (kinds[rid][0], rid, kinds[rid][1]))

for p in problems: print("[問題]", p)
for n in notes: print("[列出]", n)
print("規則 %d 條（proposed %d 條），範例 %d 個（展開後 %d 個）" % (
    len(trial_rules), sum(1 for r in trial_rules if rules[r].get("status") == "proposed"), total, expanded_total))
print("問題 %d 個" % len(problems))
