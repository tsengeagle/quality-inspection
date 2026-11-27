"""前端 service 的回應型別對照 specs/*/http.yaml。唯讀。"""
import re, sys, yaml, json
ROOT = sys.argv[1]
FE = ROOT + "/frontends/quality-inspection-frontend/src/app/features/"
def parse_body(name, body, out):
    """大括號感知的欄位解析；欄位的型別是行內物件時，另外建一個 name$field 的型別。"""
    fields = {}; i = 0; n = len(body)
    while i < n:
        m = re.compile(r"\s*(\w+)(\?)?:\s*").match(body, i)
        if not m: break
        i = m.end(); depth = 0; start = i
        while i < n and not (body[i] == ";" and depth == 0):
            depth += body[i] == "{"; depth -= body[i] == "}"; i += 1
        t = " ".join(body[start:i].split()); i += 1
        if t.startswith("{"):
            close = t.rindex("}"); inner = name + "$" + m.group(1)
            parse_body(inner, t[1:close], out); t = inner + t[close + 1:]
        fields[m.group(1)] = (bool(m.group(2)), t)
    out[name] = fields

def ts_interfaces(path, inline=None):
    s = open(path, encoding="utf-8").read()
    s = re.sub(r"/\*.*?\*/", "", s, flags=re.S); s = re.sub(r"(?m)^\s*//.*$", "", s)
    out = {}
    for m in re.finditer(r"(?:export\s+)?interface\s+(\w+)(?:<\w+>)?\s*\{(.*?)\n\}", s, re.S):
        parse_body(m.group(1), m.group(2), out)
    if inline: parse_body("(行內型別)", inline, out)
    aliases = {m.group(1): " ".join(m.group(2).split()) for m in re.finditer(r"type\s+(\w+)\s*=\s*([^;]+);", s)}
    return out, aliases

def ts_kind(t, ifaces, aliases):
    """回傳 (kind, nullable, inner)。kind: string/number/boolean/array/object/unknown"""
    parts = [p.strip() for p in t.split("|") if p.strip()]
    nullable = "null" in parts or "undefined" in parts
    parts = [p for p in parts if p not in ("null", "undefined")]
    if not parts: return ("null", True, None)
    if all(re.match(r"^'[^']*'$", p) for p in parts): return ("string", nullable, None)
    p = parts[0]
    if p.endswith("[]"): return ("array", nullable, p[:-2])
    if p in aliases: 
        k, n, i = ts_kind(aliases[p], ifaces, aliases); return (k, nullable or n, i)
    if p in ("string", "number", "boolean"): return (p, nullable, None)
    if p in ifaces: return ("object", nullable, p)
    return ("unknown:" + p, nullable, None)

def c_kind(shape):
    """契約的形狀 -> (kind, nullable, always_null)"""
    if isinstance(shape, dict): return ("object", False, False)
    if isinstance(shape, list): return ("array", False, False)
    s = str(shape)
    if s == "null": return ("null", True, True)
    if s in ("true", "false"): return ("boolean", False, False)
    n = s.endswith("?"); b = s[:-1] if n else s
    if b.startswith("enum ") or b in ("id", "text"): return ("string", n, False)
    if b in ("int", "decimal"): return ("number", n, False)
    if b == "boolean": return ("boolean", n, False)
    return ("unknown:" + b, n, False)

def compare(tsname, shape, ifaces, aliases, path, out, variant):
    fields = ifaces[tsname]
    cfields = {}
    for k, v in shape.items():
        nk = k.endswith("?"); cfields[k[:-1] if nk else k] = (v, nk)
    for name, (opt, t) in fields.items():
        tk, tn, inner = ts_kind(t, ifaces, aliases)
        where = "%s.%s" % (path, name)
        if name not in cfields:
            out.append((variant, "前端有、契約沒有", where, "前端型別 %s%s" % (t, "（選填）" if opt else "（必填）"))); continue
        cshape, cobjnull = cfields[name]
        ck, cn, always = c_kind(cshape); cn = cn or cobjnull
        if always:
            out.append((variant, "契約一律 null", where, "前端型別 %s" % t)); continue
        if tk.startswith("unknown") or ck.startswith("unknown"):
            out.append((variant, "無法比對", where, "前端 %s／契約 %s" % (t, cshape))); continue
        if tk != ck:
            out.append((variant, "型別不同", where, "前端 %s／契約 %s" % (t, cshape if not isinstance(cshape, (dict, list)) else ck)))
            continue
        if cn and not (opt or tn):
            out.append((variant, "契約可為 null、前端必填", where, "前端 %s／契約 %s" % (t, cshape if not isinstance(cshape, (dict, list)) else ck + "?")))
        if tk == "object" and inner in ifaces and isinstance(cshape, dict):
            compare(inner, cshape, ifaces, aliases, where, out, variant)
        if tk == "array" and isinstance(cshape, list) and cshape:
            if inner in ifaces and isinstance(cshape[0], dict):
                compare(inner, cshape[0], ifaces, aliases, where + "[]", out, variant)
            elif inner not in ifaces:
                ik = ts_kind(inner, ifaces, aliases)[0]; ek = c_kind(cshape[0])[0]
                if ik != ek: out.append((variant, "型別不同", where + "[]", "前端 %s／契約 %s" % (inner, cshape[0])))
    for name in cfields:
        if name not in fields:
            out.append((variant, "契約有、前端沒有宣告", "%s.%s" % (path, name), ""))

contracts = {}
for d in ("review-case", "review-project", "sampling-batch", "score-sheet"):
    doc = yaml.safe_load(open("%s/specs/%s/http.yaml" % (ROOT, d), encoding="utf-8"))
    for e in doc["endpoints"]:
        contracts[(e["method"], e["path"])] = (d, e)

# (service 檔, 方法, 路徑, 前端型別, 包裝)
CALLS = json.load(open(sys.argv[2], encoding="utf-8"))
used = set(); report = []
for c in CALLS:
    ifaces, aliases = ts_interfaces(FE + c["file"], c.get("inline"))
    if c.get("inline"): c["type"] = "(行內型別)"
    key = (c["method"], c["path"]); used.add(key)
    if key not in contracts:
        report.append({"call": c, "error": "契約裡沒有這個端點"}); continue
    d, e = contracts[key]; succ = e["success"]
    variants = [(v["name"], v["data"]) for v in succ["variants"]] if "variants" in succ else [(None, succ.get("data"))]
    findings = []
    for vname, data in variants:
        t = c["type"]
        if t == "unknown":
            findings.append((vname, "前端不讀回應", "", "契約 data = %s" % (data if not isinstance(data, (dict, list)) else "物件")) ); continue
        shape = data
        if c.get("page"):
            # 前端的 PageResponse<T>
            pf = {"content", "page", "size", "totalElements", "totalPages"}
            if not isinstance(data, dict) or set(k.rstrip("?") for k in data) != pf:
                findings.append((vname, "分頁外殼不同", "", "契約的鍵 %s" % sorted(data)))
            shape = data["content"][0]
        if c.get("array"):
            if not isinstance(data, list): findings.append((vname, "型別不同", "", "前端陣列／契約不是陣列")); continue
            shape = data[0]
        if not isinstance(shape, dict):
            findings.append((vname, "無法比對", "", "契約 data = %r" % (shape,))); continue
        compare(t, shape, ifaces, aliases, t, findings, vname)
    report.append({"call": c, "spec": d, "usecase": e["usecase"], "findings": findings})
json.dump({"report": report, "unused": sorted("%s %s (%s)" % (m, p, contracts[(m, p)][0]) for (m, p) in contracts if (m, p) not in used)}, sys.stdout, ensure_ascii=False, indent=1)
