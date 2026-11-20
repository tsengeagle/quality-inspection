"""暫時的 v2 檢查（B 組）：只核對 15-spec-format-v2-draft.md 第四節第 4～8 項已經定義的引用與結構。

不是 spec_lint，不屬於 harness-core。執行器沒有跑：這支程式不知道範例會不會通過。
用法：python v2check_b.py <樣本目錄> [<已立法的 specs 目錄>]
"""
import json, pathlib, re, sys
import yaml

root = pathlib.Path(sys.argv[1])
law = pathlib.Path(sys.argv[2]) if len(sys.argv) > 2 else None
load = lambda p: yaml.safe_load(p.read_text(encoding="utf-8"))

problems, notes = [], []
def problem(where, msg): problems.append("%s：%s" % (where, msg))

# 已立法的 use case：needs 列出的名稱要嘛在這裡，要嘛算「未滿足」
legislated = set()
if law:
    for f in law.glob("*/usecases.yaml"):
        legislated |= {u["name"] for u in load(f).get("usecases") or []}

for d in sorted(p for p in root.iterdir() if p.is_dir()):
    agg = d.name
    m, u, ex = (load(d / f) for f in ("model.yaml", "usecases.yaml", "examples.yaml"))

    rules, kinds = {}, {}
    def add(kind, r): rules[r["id"]] = r; kinds[r["id"]] = kind
    for key in ("transitions", "invariants"):
        for r in m.get(key) or []: add(key, r)
    for g in u.get("guards") or []: add("guards", g)
    usecases = {uc["name"]: uc for uc in u.get("usecases") or []}
    for uc in usecases.values():
        for r in uc.get("rules") or []: add("rules", r)
        for r in (uc.get("success") or {}).get("state") or []: add("state", r)
    codes = {c["code"] for c in u.get("error_codes") or []}
    events = {name: {e["event"] for e in (uc.get("success") or {}).get("emits") or []} for name, uc in usecases.items()}
    enum_values = {e["name"]: e["values"] for e in m.get("enums") or []}

    fixtures = ex.get("fixtures") or {}
    expectations = ex.get("expectations") or {}
    actors = ex.get("actors") or {}

    def expand(steps, seen=()):
        out = []
        for s in steps or []:
            if "use" in s and s["use"] in fixtures and s["use"] not in seen:
                out += expand(fixtures[s["use"]].get("steps"), seen + (s["use"],))
            else: out.append(s)
        return out

    def fixture_needs(name, seen=()):
        fx = fixtures.get(name) or {}
        out = set(fx.get("needs") or [])
        for s in fx.get("steps") or []:
            if "use" in s and s["use"] not in seen: out |= fixture_needs(s["use"], seen + (name,))
        return out

    def check_step(where, s, needs):
        if "use" in s:
            if s["use"] not in fixtures: problem(where, "fixture %s 不存在" % s["use"])
            return
        if "advance" in s:
            if not re.fullmatch(r"P(\d+D)?(T(\d+H)?(\d+M)?(\d+S)?)?", str(s["advance"])): problem(where, "advance 不是 ISO 8601 期間：%s" % s["advance"])
            return
        run = s.get("run")
        if not run: problem(where, "步驟沒有 run、use 或 advance"); return
        if run not in usecases and run not in legislated and run not in needs:
            problem(where, "use case %s 不在本規格、已立法的規格，也不在 needs" % run)
        if s.get("as") and "${" not in s["as"] and s["as"] not in actors: problem(where, "操作者 %s 不存在" % s["as"])
        code = (s.get("expect") or {}).get("code")
        if code and code not in codes: problem(where, "代碼 %s 沒有定義" % code)

    for name, fx in fixtures.items():
        own = fixture_needs(name)
        for s in fx.get("steps") or []: check_step("%s fixture %s" % (agg, name), s, own)

    # 第 6 項：矩陣展開
    expanded = []
    for e in ex.get("examples") or []:
        if "matrix" not in e: expanded.append((e["name"], e)); continue
        cols = e["matrix"]["columns"]
        for i, row in enumerate(e["matrix"]["rows"]):
            if len(row) != len(cols): problem("%s %s" % (agg, e["name"]), "矩陣第 %d 列欄數不符" % (i + 1)); continue
            text = json.dumps({k: v for k, v in e.items() if k != "matrix"}, ensure_ascii=False)
            for k, v in zip(cols, row): text = text.replace("${%s}" % k, str(v))
            expanded.append(("%s[%s]" % (e["name"], ",".join(str(v) for v in row[:2])), json.loads(text)))

    covered, unmet, names = set(), [], set()
    for name, e in expanded:
        where = "%s %s" % (agg, name)
        if name in names: problem(where, "展開後名稱重複")
        names.add(name)
        if not re.fullmatch(r"[A-Za-z0-9_.\-]+", name): notes.append("%s：名稱含有 v1 不允許的字元（缺口 B-05）" % where)
        needs = set(e.get("needs") or [])
        for s in e.get("given") or []:
            if "use" in s: needs |= fixture_needs(s["use"])
        missing = sorted(n for n in needs if n not in usecases and n not in legislated)
        if missing: unmet.append((name, missing))
        for b in e.get("basis") or []:
            if b not in rules: problem(where, "basis %s 不存在" % b); continue
            covered.add(b)
            if rules[b].get("status") == "proposed": notes.append("%s：basis 含 proposed %s" % (where, b))
        for s in (e.get("given") or []) + (e.get("observe") or []): check_step(where, s, needs)
        when = e.get("when") or {}
        check_step(where + " when", when, needs)
        then = dict(e.get("then") or {})
        if "use_expect" in then:
            if then["use_expect"] not in expectations: problem(where, "具名斷言 %s 不存在" % then["use_expect"])
        if then.get("result") not in ("SUCCESS", "REJECTED"): problem(where, "then.result 不是 SUCCESS 或 REJECTED")
        if then.get("result") == "REJECTED" and not then.get("code"): problem(where, "REJECTED 沒有 code")
        if then.get("code") and then["code"] not in codes: problem(where, "代碼 %s 沒有定義" % then["code"])
        # 第 7 項：事件必須在 when 那支 use case 的 success.emits 宣告
        for ev in then.get("emits") or []:
            declared = events.get(when.get("run"))
            if declared is None: problem(where, "emits：%s 不在本規格，無從核對事件" % when.get("run"))
            elif ev["event"] not in declared: problem(where, "事件 %s 沒有在 %s 的 success.emits 宣告" % (ev["event"], when.get("run")))
        # 第 4 項：absent 的路徑
        for holder in [then] + [s.get("expect") or {} for s in e.get("observe") or []] + ([expectations.get(then.get("use_expect")) or {}]):
            for path in holder.get("absent") or []:
                if not isinstance(path, str) or not path.strip(): problem(where, "absent 的路徑是空的")
                elif "[" in path: notes.append("%s：absent 路徑 %s 用了第 4 項沒有定義的清單寫法（缺口 B-01）" % (where, path))

    # v1.1 第 4 點：分辨不了情境的範例
    seen = {}
    for name, e in expanded:
        key = json.dumps([expand(e.get("given")), e.get("when")], sort_keys=True, ensure_ascii=False)
        then = json.dumps(e.get("then"), sort_keys=True, ensure_ascii=False)
        if key in seen and then != seen[key][1]: problem("%s %s" % (agg, name), "given、when 與 %s 相同，then 卻不同" % seen[key][0])
        seen.setdefault(key, (name, then))

    # 引用完整性（v1.1）與第 8 項：轉移的涵蓋
    for t in m.get("transitions") or []:
        if t["by"] not in usecases: problem("%s %s" % (agg, t["id"]), "by %s 不是本規格的 use case" % t["by"])
        owner = [n for n, vs in enum_values.items() if t["from"] in vs and t["to"] in vs]
        if not owner: problem("%s %s" % (agg, t["id"]), "from、to 不屬於同一個列舉")
    for rid in sorted(rules):
        if rid in covered: continue
        (problems if kinds[rid] == "transitions" else notes).append("%s %s：沒有範例涵蓋（%s）" % (agg, rid, kinds[rid]))

    notes.append("%s：規則 %d 條，範例 %d 個（展開後 %d 個）" % (agg, len(rules), len(ex.get("examples") or []), len(expanded)))
    notes.append("%s：needs 未滿足的範例 %d 個（v1 第四節：算未通過）；缺的 use case：%s" % (
        agg, len(unmet), "、".join(sorted({n for _, ms in unmet for n in ms})) or "無"))

for p in problems: print("[問題]", p)
for n in notes: print("[列出]", n)
print("問題 %d 個" % len(problems))
