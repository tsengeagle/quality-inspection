"""暫時的 v2 檢查：只檢查 15-spec-format-v2-draft.md 第四節已經定義的引用與結構。不是 spec_lint，不進 repo。"""
import itertools, json, pathlib, re, sys
import yaml

root = pathlib.Path(sys.argv[1])
aggs = {}
for d in sorted(p for p in root.iterdir() if p.is_dir()):
    aggs[d.name] = {f: yaml.safe_load((d / f).read_text()) for f in ("model.yaml", "usecases.yaml", "examples.yaml")}

problems, notes = [], []
def problem(where, msg): problems.append("%s：%s" % (where, msg))

# ---- 收集定義 ----
rules, usecases, ports, facets, codes = {}, {}, {}, {}, {}
for name, docs in aggs.items():
    m, u = docs["model.yaml"], docs["usecases.yaml"]
    for key in ("transitions", "invariants"):
        for r in m.get(key) or []: rules[r["id"]] = (name, key, r)
    for g in u.get("guards") or []: rules[g["id"]] = (name, "guards", g)
    for uc in u.get("usecases") or []:
        usecases[(name, uc["name"])] = uc
        for r in uc.get("rules") or []: rules[r["id"]] = (name, "rules", r)
        for r in (uc.get("success") or {}).get("state") or []: rules[r["id"]] = (name, "state", r)
    for p in m.get("ports") or []:
        ports[(name, p["name"])] = {o["name"] for o in p["operations"]}
    for f in m.get("facets") or []:
        facets[(name, f["name"])] = f
    codes[name] = {c["code"] for c in u.get("error_codes") or []}

def visible(agg):
    return [agg] + (aggs[agg]["model.yaml"].get("uses_ports_from") or [])

def resolve(agg, ref, table):
    """'X' 或 'other.X' → 在 table 中找到的 (aggregate, X)。"""
    if "." in ref and ref.split(".", 1)[0] in aggs:
        a, rest = ref.split(".", 1)
        return (a, rest) if (a, rest) in table else None
    for a in visible(agg):
        if (a, ref) in table: return (a, ref)
    return None

def check_port_op(agg, where, ref, processes):
    if ":" in ref:
        proc, ref = ref.split(":", 1)
        if proc not in processes: problem(where, "行程 %s 沒有在 processes 宣告" % proc)
    head, _, op = ref.rpartition(".")
    hit = resolve(agg, head, ports)
    if not hit: problem(where, "埠 %s 不存在" % head); return
    if op not in ports[hit]: problem(where, "埠 %s 沒有操作 %s" % (head, op))

for (agg, fname), f in facets.items():
    q = resolve(agg, f["observed_by"], usecases)
    if not q or usecases[q].get("kind") != "query":
        problem("%s facet %s" % (agg, fname), "observed_by 必須是查詢 use case：%s" % f["observed_by"])

def check_facets(agg, where, fac, processes):
    for key, val in (fac or {}).items():
        if ":" in key:
            proc, key = key.split(":", 1)
            if proc not in processes: problem(where, "行程 %s 沒有在 processes 宣告" % proc)
        hit = resolve(agg, key, facets)
        if not hit: problem(where, "面向 %s 不存在" % key); continue
        if val not in facets[hit]["values"]: problem(where, "面向 %s 沒有值 %s" % (key, val))

# 規則裡的面向
for rid, (agg, kind, r) in rules.items():
    check_facets(agg, "%s %s" % (agg, rid), (r.get("outcome") or {}).get("facets"), [])

# ---- 範例 ----
covered = set()
for agg, docs in aggs.items():
    ex = docs["examples.yaml"]; fixtures = ex.get("fixtures") or {}; expectations = ex.get("expectations") or {}
    def steps_of(steps, seen=()):
        out = []
        for s in steps or []:
            if "use" in s and s["use"] in fixtures and s["use"] not in seen:
                out += steps_of(fixtures[s["use"]].get("steps"), seen + (s["use"],))
            else: out.append(s)
        return out
    def check_step(where, s, processes):
        if "process" in s and s["process"] not in processes: problem(where, "行程 %s 沒有在 processes 宣告" % s["process"])
        if "use" in s and s["use"] not in fixtures: problem(where, "fixture %s 不存在" % s["use"])
        if "run" in s and not resolve(agg, s["run"], usecases): problem(where, "use case %s 不存在" % s["run"])
        if "port" in s: check_port_op(agg, where, s["port"], processes)
        if "pause_at" in s: check_port_op(agg, where, s["pause_at"], processes)
        exp = s.get("expect") or {}
        if exp.get("code") and exp["code"] not in codes[resolve(agg, s["run"], usecases)[0] if s.get("run") and resolve(agg, s["run"], usecases) else agg]:
            problem(where, "代碼 %s 沒有定義" % exp["code"])
    for fname, fx in fixtures.items():
        for s in fx.get("steps") or []: check_step("%s fixture %s" % (agg, fname), s, ["A", "B", "ICCARD", "HCAAPI"])
    expanded = []
    for e in ex.get("examples") or []:
        rows = [None]
        if "matrix" in e:
            cols = e["matrix"]["columns"]
            for i, row in enumerate(e["matrix"]["rows"]):
                if len(row) != len(cols): problem("%s %s" % (agg, e["name"]), "矩陣第 %d 列欄數不符" % (i + 1))
            rows = [dict(zip(cols, row)) for row in e["matrix"]["rows"]]
        for row in rows:
            text = json.dumps(e, ensure_ascii=False)
            name = e["name"]
            if row:
                for k, v in row.items(): text = text.replace("${%s}" % k, str(v))
                name = "%s[%s]" % (e["name"], ",".join(str(v) for v in list(row.values())[:2]))
            expanded.append((name, json.loads(text)))
    for name, e in expanded:
        where = "%s %s" % (agg, name); procs = e.get("processes") or []
        for b in e.get("basis") or []:
            if b not in rules: problem(where, "basis %s 不存在" % b)
            covered.add(b)
            if rules.get(b, (0, 0, {}))[2].get("status") == "proposed": notes.append("%s：basis 含 proposed %s" % (where, b))
        for s in (e.get("given") or []) + (e.get("observe") or []): check_step(where, s, procs)
        when = e.get("when") or {}
        check_step(where + " when", when, procs)
        for s in when.get("during") or []: check_step(where + " during", s, procs)
        then = dict(e.get("then") or {})
        if then.get("use_expect"):
            if then["use_expect"] not in expectations: problem(where, "具名斷言 %s 不存在" % then["use_expect"])
            else: then.update(expectations[then["use_expect"]])
        target = resolve(agg, when.get("run", ""), usecases)
        if then.get("code") and target and then["code"] not in codes[target[0]]: problem(where, "代碼 %s 沒有定義在 %s" % (then["code"], target[0]))
        for key in ("calls", "calls_include"):
            for p, ops in (then.get(key) or {}).items():
                for op in ops: check_port_op(agg, where + " " + key, "%s.%s" % (p, op), procs)
        for ref in then.get("never") or []: check_port_op(agg, where + " never", ref, procs)
        for pair in then.get("order") or []:
            for ref in pair: check_port_op(agg, where + " order", ref, procs)
        check_facets(agg, where, then.get("facets"), procs)
    # v1.1 第 4 點：分辨不了情境的範例（展開 fixture 後）
    seen = {}
    for name, e in expanded:
        key = json.dumps([steps_of(e.get("given")), e.get("when"), e.get("processes")], sort_keys=True, ensure_ascii=False)
        if key in seen and json.dumps(e.get("then"), sort_keys=True) != json.dumps(seen[key][1].get("then"), sort_keys=True):
            problem("%s %s" % (agg, name), "given、when 與 %s 相同，then 卻不同" % seen[key][0])
        seen.setdefault(key, (name, e))
    notes.append("%s：展開後範例 %d 個" % (agg, len(expanded)))

for rid, (agg, kind, r) in sorted(rules.items()):
    if rid not in covered:
        (problems if kind == "transitions" else notes).append("%s %s：沒有範例涵蓋（%s）" % (agg, rid, kind))

for p in problems: print("[問題]", p)
for n in notes: print("[列出]", n)
print("問題 %d 個" % len(problems))
