#!/usr/bin/env python3
"""spec_lint.py — 規格格式的檢查程式（格式 v1，含 D-063 的 v1.1、D-064 的 v1.2；見 specs/README.md）。

逐一檢查 <specs 目錄>/<aggregate>/ 底下的 model.yaml、usecases.yaml、examples.yaml。
判決是確定性的：只讀檔案，不執行任何規格裡的內容。

v1.1（D-063）多了三項：
    封閉結構：每一層只允許欄位表（FILE_SCHEMAS）列出的欄位，型別也要相符；任何一層都可以有 note。
    分辨不了情境的範例：given（展開 fixture 後）與 when 相同、then 卻不同。
    引用完整性：guard_exceptions、transitions 的 by／from／to、terminal_states。

v1.2（D-064）：格式裡屬於專案的部分讀 <specs 目錄>/spec-profile.yaml，不寫死在這裡。
    actor_fields、usecase_actor_fields：操作者允許的欄位。
    error_categories：錯誤分類允許的值。
    guard_order：guard 順序的說明，只給人看，不檢查順序。
    檔案不存在或缺少任何一項，判為無法評估，不使用預設值。

v2.0（D-065）：寫 spec_version: 2 的目錄才適用，spec_version: 1 的目錄判決不變。
    斷言：then、expect、具名斷言（expectations）共用 result、code、output、absent、only、use_expect；then 另有 emits、emits_include。
    路徑：a.b、a[*]、a[k=v]（output 的鍵、absent、only），a[0]（只有 save），.（只有 only）。
    時間：帶時區的 ISO 8601；範例的 clock、步驟的 advance；設定檔的 clock_start、local_time_zone（profile_version: 2）。
    矩陣：matrix 的 columns、key、rows；展開後的名稱是 <name>.<key 的值>。
    轉移：每一條都要有範例；when 區分同一個 from、同一支 use case 的多條；deferred_transitions。
    跨目錄：fixture、use case、轉移的 by、埠都可以寫 <目錄>.<名稱>。
    埠：model.yaml 的 ports；步驟的 port 加 returns。
    情境相同的範例：result 或 code 不同才擋下。
    只有 model.yaml 是必要的檔案。

用法：
    python3 spec_lint.py [--repo .] [--specs specs] [--manifest law/manifest.txt]
                         [--design-docs DIR] [--testing-docs DIR] [--report-only DIR ...]

    --manifest：律法清冊。決策紀錄的位置讀它的 @decision-log。
    --design-docs：design_decision 出處要找的目錄（例如設計決策 DD-03 所在的目錄）。
    --testing-docs：example、scenario 出處要找的目錄。
        這兩個沒有設定時，用到對應出處的規則一律算問題，不會略過。
    --report-only：只列出問題、不讓結束代碼失敗的目錄，例如還在草稿階段的 spec-draft。

結束代碼：0 通過；1 未通過；2 無法評估。無法評估不算通過。
需要 PyYAML。
"""
import argparse
import json
import os
import re
import sys

try:
    import yaml
except ImportError:      # pragma: no cover
    yaml = None

SPEC_VERSION = 1
SPEC_VERSIONS = (1, 2)
KINDS = {"model.yaml": "aggregate", "usecases.yaml": "usecases", "examples.yaml": "examples"}
RESULTS = {"SUCCESS", "REJECTED"}
PROFILE_NAME = "spec-profile.yaml"
ID_PATTERN = re.compile(r"^[A-Z]{2,}-[A-Z0-9]+-[A-Z0-9]+$")
SOURCE_KINDS = {"decision", "design_decision", "doc", "example", "scenario", "rule"}
DEFAULT_MANIFEST = "law/manifest.txt"
EXIT_PASS, EXIT_FAIL, EXIT_NOT_EVALUATED = 0, 1, 2
USECASE_KINDS = {"command", "query"}


# ---------- 欄位表（D-063 第 2 點）----------
#
# 每一層列出允許的欄位與型別。表上沒有的欄位、型別不符的值都是問題。
# 任何一層對應表都可以有 note（自由文字，不參與判決）。
# 欄位表是格式的定義，內建在這裡，不讀專案的 README；README 的欄位表是給人看的同一份內容。
#
# 節點的寫法：
#   "str"、"int"、"bool"：純量
#   "any"：任意值（範例的輸入、預期輸出、屬性的初始值）
#   ("enum", 值的集合)：字串，且必須是其中之一
#   ("list", 項目)：清單
#   ("map", {欄位: 節點})：封閉的對應表
#   ("named", 值)：鍵是自由的名稱（操作者、fixture、變數），值依節點檢查
#   （待改寫）：D-063 第 3 點，v1.1 照樣接受、下一次修法改寫的欄位，以註解標出

def _map(fields):
    return ("map", fields)


def _list(item):
    return ("list", item)


SOURCE_ITEM = _map({"decision": "str", "design_decision": "str", "doc": "str", "section": "str",
                    "example": "str", "scenario": "str", "rule": "str"})
RULE_META = {"status": "str", "source": _list(SOURCE_ITEM), "question": "str"}


def _ruled(fields):
    merged = dict(fields)
    merged.update(RULE_META)
    return _map(merged)


ATTRIBUTE = _ruled({"name": "str", "type": "str", "required": "bool", "initial": "any",
                    "assigned_by": "str", "mutable": "bool", "external": "bool",
                    "range": _list("int")})                       # range（待改寫）
MODEL_SCHEMA = _map({
    "spec_version": "int", "kind": "str", "name": "str", "term": "str",
    "identity": _map({"name": "str", "type": "str", "assigned_by": "str"}),
    "sources": _list(SOURCE_ITEM),
    "attributes": _list(ATTRIBUTE),
    "entities": _list(_map({"name": "str", "term": "str", "attributes": _list(ATTRIBUTE)})),
    "enums": _list(_ruled({"name": "str", "values": _list("str")})),
    "transitions": _list(_ruled({"id": "str", "from": "str", "to": "str", "by": "str"})),
    "terminal_states": _list("str"),
    "invariants": _list(_ruled({"id": "str", "text": "str",
                                "enforced_by": "str"})),         # enforced_by（待改寫）
    "deferred_attributes": _list("str"),
    "deferred_behaviors": _list("str"),
})
OUTCOME = _map({"result": "str", "code": "str", "no_change": "bool"})
INPUT_FIELD = _ruled({"name": "str", "type": "str", "required": "bool"})
USECASE = _map({
    "name": "str", "kind": ("enum", USECASE_KINDS), "summary": "str",
    "actor": ("profile", "usecase_actor_fields"),                 # 欄位來自設定檔（D-064）
    "input": _list(INPUT_FIELD),
    "input_note": "str",                                          # input_note（待改寫）
    "guards": _list("str"),
    "guard_exceptions": _list(_ruled({"guard": "str", "text": "str"})),
    "rules": _list(_ruled({"id": "str", "when": "str", "text": "str", "outcome": OUTCOME})),
    "success": _map({
        "state": _list(_ruled({"id": "str", "text": "str"})),
        "emits": _list(_ruled({"event": "str"})),
        "output": _list(_ruled({"name": "str", "type": "str"})),  # success.output（待改寫）
    }),
    "output": _ruled({"fields": _list(_map({"name": "str", "type": "str"}))}),   # output（待改寫）
})
USECASES_SCHEMA = _map({
    "spec_version": "int", "kind": "str", "aggregate": "str",
    "sources": _list(SOURCE_ITEM),
    "error_codes": _list(_ruled({"code": "str", "category": "str"})),
    "guards": _list(_ruled({"id": "str", "when": "str", "outcome": OUTCOME})),
    "input_types": ("named", _list(INPUT_FIELD)),
    "usecases": _list(USECASE),
})
STEP_FIELDS = {"run": "str", "as": "str", "input": ("named", "any"), "save": ("named", "str"),
               "use": "str", "input_pending": "str"}
STEP = _map(STEP_FIELDS)
RESULT = _map({"result": ("enum", RESULTS), "code": "str"})
OBSERVE_STEP = _map(dict(STEP_FIELDS, expect=_map({"result": ("enum", RESULTS), "code": "str",
                                                     "output": ("named", "any")})))
EXAMPLES_SCHEMA = _map({
    "spec_version": "int", "kind": "str", "aggregate": "str",
    "actors": ("named", ("profile", "actor_fields")),              # 欄位來自設定檔（D-064）
    "fixtures": ("named", _map({"steps": _list(STEP), "needs": _list("str")})),
    "examples": _list(_map({
        "name": "str", "basis": _list("str"), "derived_from": _list("str"), "needs": _list("str"),
        "given": _list(STEP), "when": STEP, "then": RESULT, "observe": _list(OBSERVE_STEP),
    })),
    "not_covered": _list(_map({"rules": _list("str"), "item": "str", "reason": "str"})),
})
FILE_SCHEMAS = {"model.yaml": MODEL_SCHEMA, "usecases.yaml": USECASES_SCHEMA, "examples.yaml": EXAMPLES_SCHEMA}

# ---------- 欄位表：spec_version 2（D-065）----------
#
# 與 v1 的差別：
#   拿掉 D-063 第 3 點的四項待改寫：attributes[].range、invariants[].enforced_by、input_note、success.output。
#   model：transitions[].when、deferred_transitions、ports。
#   usecases：output_types；命令與查詢的輸出都寫 output.fields。
#   examples：expectations、範例的 clock 與 matrix、步驟的 advance／port／returns、then 與 expect 的斷言欄位。
ATTRIBUTE_V2 = _ruled({"name": "str", "type": "str", "required": "bool", "initial": "any",
                       "assigned_by": "str", "mutable": "bool", "external": "bool"})
MODEL_SCHEMA_V2 = _map({
    "spec_version": "int", "kind": "str", "name": "str", "term": "str",
    "identity": _map({"name": "str", "type": "str", "assigned_by": "str"}),
    "sources": _list(SOURCE_ITEM),
    "attributes": _list(ATTRIBUTE_V2),
    "entities": _list(_map({"name": "str", "term": "str", "attributes": _list(ATTRIBUTE_V2)})),
    "enums": _list(_ruled({"name": "str", "values": _list("str")})),
    "transitions": _list(_ruled({"id": "str", "from": "str", "to": "str", "by": "str", "when": "str"})),
    "deferred_transitions": _list(_map({"from": "str", "to": "str", "by": "str", "when": "str"})),
    "terminal_states": _list("str"),
    "invariants": _list(_ruled({"id": "str", "text": "str"})),
    "ports": _list(_map({"name": "str", "term": "str",
                         "operations": _list(_map({"name": "str", "returns": "str"}))})),
    "deferred_attributes": _list("str"),
    "deferred_behaviors": _list("str"),
})
TYPE_FIELD = _map({"name": "str", "type": "str"})
USECASE_V2 = _map({
    "name": "str", "kind": ("enum", USECASE_KINDS), "summary": "str",
    "actor": ("profile", "usecase_actor_fields"),
    "input": _list(INPUT_FIELD),
    "guards": _list("str"),
    "guard_exceptions": _list(_ruled({"guard": "str", "text": "str"})),
    "rules": _list(_ruled({"id": "str", "when": "str", "text": "str", "outcome": OUTCOME})),
    "success": _map({
        "state": _list(_ruled({"id": "str", "text": "str"})),
        "emits": _list(_ruled({"event": "str"})),
    }),
    "output": _ruled({"fields": _list(TYPE_FIELD)}),
})
USECASES_SCHEMA_V2 = _map({
    "spec_version": "int", "kind": "str", "aggregate": "str",
    "sources": _list(SOURCE_ITEM),
    "error_codes": _list(_ruled({"code": "str", "category": "str"})),
    "guards": _list(_ruled({"id": "str", "when": "str", "outcome": OUTCOME})),
    "input_types": ("named", _list(INPUT_FIELD)),
    "output_types": ("named", _list(TYPE_FIELD)),
    "usecases": _list(USECASE_V2),
})
STEP_FIELDS_V2 = dict(STEP_FIELDS, advance="str", port="str", returns="any")
STEP_V2 = _map(STEP_FIELDS_V2)
ASSERT_FIELDS = {"result": ("enum", RESULTS), "code": "str", "output": ("named", "any"),
                 "absent": _list("str"), "only": ("named", _list("str")), "use_expect": "str"}
EMITTED = _list(_map({"event": "str", "with": ("named", "any")}))
THEN_FIELDS = dict(ASSERT_FIELDS, emits=EMITTED, emits_include=EMITTED)
OBSERVE_STEP_V2 = _map(dict(STEP_FIELDS_V2, expect=_map(ASSERT_FIELDS)))
EXAMPLES_SCHEMA_V2 = _map({
    "spec_version": "int", "kind": "str", "aggregate": "str",
    "actors": ("named", ("profile", "actor_fields")),
    "fixtures": ("named", _map({"steps": _list(STEP_V2), "needs": _list("str")})),
    "expectations": ("named", _map(ASSERT_FIELDS)),
    "examples": _list(_map({
        "name": "str", "basis": _list("str"), "derived_from": _list("str"), "needs": _list("str"),
        "clock": _map({"start": "str"}),
        "matrix": _map({"columns": _list("str"), "key": _list("str"), "rows": _list(_list("any"))}),
        "given": _list(STEP_V2), "when": STEP_V2, "then": _map(THEN_FIELDS), "observe": _list(OBSERVE_STEP_V2),
    })),
    "not_covered": _list(_map({"rules": _list("str"), "item": "str", "reason": "str"})),
})
FILE_SCHEMAS_V2 = {"model.yaml": MODEL_SCHEMA_V2, "usecases.yaml": USECASES_SCHEMA_V2,
                   "examples.yaml": EXAMPLES_SCHEMA_V2}
SCHEMAS_BY_VERSION = {1: FILE_SCHEMAS, 2: FILE_SCHEMAS_V2}

# 路徑（D-065 第 3 點）：a.b、a[*]、a[k=v]、a[k=v,k2=v2]、a[0]；. 代表輸出本身
PATH_SEGMENT = r"[A-Za-z_][A-Za-z0-9_]*(\[(\*|\d+|[A-Za-z_]\w*=[^\],=]+(,[A-Za-z_]\w*=[^\],=]+)*)\])?"
PATH_PATTERN = re.compile(r"^%s(\.%s)*$" % (PATH_SEGMENT, PATH_SEGMENT))
INDEX_IN_PATH = re.compile(r"\[\d+\]")
VARIABLE = re.compile(r"\$\{([A-Za-z0-9_]+)\}")
# 時間（D-065 第 5 點）：看起來像時間的字串必須帶時區
TIME_LIKE = re.compile(r"^\d{4}-\d\d-\d\dT\d\d:\d\d")
ZONED_TIME = re.compile(r"^\d{4}-\d\d-\d\dT\d\d:\d\d(:\d\d(\.\d+)?)?(Z|[+-]\d\d:\d\d)$")
DURATION = re.compile(r"^P(?=\d|T\d)(\d+D)?(T(\d+H)?(\d+M)?(\d+S)?)?$")
EXAMPLE_NAME = re.compile(r"^[A-Za-z0-9_.\-]+$")
MATRIX_KEY_VALUE = re.compile(r"^[A-Za-z0-9_\-]+$")
TIME_ZONE = re.compile(r"^(UTC|[A-Za-z_]+(/[A-Za-z0-9_+\-]+)+)$")

# ---------- 專案設定檔（D-064）----------
#
# 操作者的欄位只能用這兩種型別；與欄位表的詞彙相同。
PROFILE_FIELD_TYPES = {"str": "str", "str_list": _list("str")}
PROFILE_SCHEMA = _map({
    "kind": "str", "profile_version": "int",
    "actor_fields": ("named", ("enum", set(PROFILE_FIELD_TYPES))),
    "usecase_actor_fields": ("named", ("enum", set(PROFILE_FIELD_TYPES))),
    "error_categories": _list("str"),
    "guard_order": _map({"text": "str", "source": _list(_map({"decision": "str"}))}),
})
PROFILE_REQUIRED = ("actor_fields", "usecase_actor_fields", "error_categories", "guard_order")
# profile_version 2（D-065 第 5 點）：多兩項，一樣必填。spec_version 2 的規格需要它。
PROFILE_SCHEMA_V2 = _map(dict(PROFILE_SCHEMA[1], clock_start="str", local_time_zone="str"))
PROFILE_REQUIRED_V2 = PROFILE_REQUIRED + ("clock_start", "local_time_zone")


def resolve_profile(node, profile):
    """把欄位表裡的 ("profile", 項目) 換成設定檔決定的封閉對應表。"""
    if isinstance(node, str):
        return node
    kind, inner = node
    if kind == "profile":
        return _map({field: PROFILE_FIELD_TYPES[t] for field, t in profile[inner].items()})
    if kind in ("list", "named"):
        return (kind, resolve_profile(inner, profile))
    if kind == "map":
        return (kind, {key: resolve_profile(value, profile) for key, value in inner.items()})
    return node


def load_profile(specs_dir, decision_log_text):
    path = os.path.join(specs_dir, PROFILE_NAME)
    if not os.path.isfile(path):
        raise NotEvaluated("找不到 %s；格式裡屬於專案的部分（操作者、錯誤分類）沒有定義，不使用預設值（D-064）" % PROFILE_NAME)
    try:
        doc = yaml.safe_load(read(path))
    except yaml.YAMLError as error:
        raise NotEvaluated("%s 讀不進來：%s" % (PROFILE_NAME, str(error).splitlines()[0]))
    if not isinstance(doc, dict):
        raise NotEvaluated("%s 的最上層必須是對應表" % PROFILE_NAME)
    version = doc.get("profile_version")
    second = version == 2 and not isinstance(version, bool)
    problems = ["%s %s" % (where or "最上層", message)
                for where, message in schema_problems(doc, PROFILE_SCHEMA_V2 if second else PROFILE_SCHEMA, "")]
    if doc.get("kind") != "spec-profile":
        problems.append("kind 必須是 spec-profile，實際是 %r" % (doc.get("kind"),))
    if version not in (1, 2) or isinstance(version, bool):
        problems.append("profile_version 必須是 1 或 2，實際是 %r" % (version,))
    for key in PROFILE_REQUIRED_V2 if second else PROFILE_REQUIRED:
        if key not in doc:
            problems.append("缺少 %s" % key)
    if second:
        start, zone = doc.get("clock_start"), doc.get("local_time_zone")
        if isinstance(start, str) and not ZONED_TIME.match(start):
            problems.append("clock_start 必須是帶時區的 ISO 8601 時間，實際是 %r" % (start,))
        if isinstance(zone, str) and not TIME_ZONE.match(zone):
            problems.append("local_time_zone 必須是 UTC 或「地區/城市」形式的時區名稱，實際是 %r" % (zone,))
    for key in ("actor_fields", "usecase_actor_fields", "error_categories"):
        if key in doc and not doc[key]:
            problems.append("%s 不可以是空的" % key)
    categories = doc.get("error_categories")
    if isinstance(categories, list) and len(set(map(str, categories))) != len(categories):
        problems.append("error_categories 有重複的值")
    guard_order = doc.get("guard_order")
    if isinstance(guard_order, dict):
        if not isinstance(guard_order.get("text"), str) or not guard_order["text"]:
            problems.append("guard_order 必須有 text")
        for item in guard_order.get("source") or []:
            key = item.get("decision") if isinstance(item, dict) else None
            if isinstance(key, str) and not heading_exists(decision_log_text, key):
                problems.append("guard_order 的出處 %s 不在決策紀錄裡" % key)
    if problems:
        raise NotEvaluated("%s 不合法：%s" % (PROFILE_NAME, "；".join(problems)))
    return doc
SCALAR_TYPES = {"str": ("字串", lambda v: isinstance(v, str)),
                "int": ("整數", lambda v: isinstance(v, int) and not isinstance(v, bool)),
                "bool": ("布林", lambda v: isinstance(v, bool))}


def schema_problems(node, schema, path):
    """依欄位表檢查，回傳 (路徑, 訊息) 的清單。只檢查欄位與型別，必填與引用由其他檢查負責。

    沒寫的欄位不檢查；寫了但值是 null，除了任意值的欄位，一律是問題（#109 review）。
    """
    if schema == "any":
        return []
    if node is None:
        return [(path, "不可以是 null：不需要就不要寫這個欄位")]
    if isinstance(schema, str):
        label, ok = SCALAR_TYPES[schema]
        return [] if ok(node) else [(path, "必須是%s，實際是 %r" % (label, node))]
    kind, inner = schema
    if kind == "enum":
        if isinstance(node, str) and node in inner:
            return []
        return [(path, "必須是 %s 其中之一，實際是 %r" % ("、".join(sorted(inner)), node))]
    if kind == "list":
        if not isinstance(node, list):
            return [(path, "必須是清單，實際是 %r" % (node,))]
        out = []
        for index, item in enumerate(node):
            label = index
            if isinstance(item, dict):
                for key in ("name", "id", "code"):
                    if isinstance(item.get(key), str):
                        label = item[key]
                        break
            out += schema_problems(item, inner, "%s[%s]" % (path, label))
        return out
    if not isinstance(node, dict):
        return [(path, "必須是對應表，實際是 %r" % (node,))]
    out = []
    if kind == "named":
        for key, value in node.items():
            out += schema_problems(value, inner, "%s.%s" % (path, key))
        return out
    # YAML 1.1 會把 on、yes、no 之類的鍵讀成布林；照讀到的值列出，並說明原因
    unknown = [key if isinstance(key, str) else "%r（YAML 把 on、yes 之類的鍵讀成布林或數字）" % (key,)
               for key in node if key not in inner and key != "note"]
    if unknown:
        out.append((path, "不認得的欄位 %s" % "、".join(unknown)))
    if "note" in node:
        out += schema_problems(node["note"], "str", "%s.note" % path if path else "note")
    for key, value in node.items():
        if key in inner:
            out += schema_problems(value, inner[key], "%s.%s" % (path, key) if path else str(key))
    return out


def strip_notes(node):
    """比較範例時忽略 note。範例的輸入與預期輸出是資料，裡面的 note 也一併忽略：寧可少擋，不可誤擋。"""
    if isinstance(node, dict):
        return {str(k): strip_notes(v) for k, v in node.items() if k != "note"}
    if isinstance(node, list):
        return [strip_notes(v) for v in node]
    return node


def canonical(node):
    return json.dumps(strip_notes(node), sort_keys=True, ensure_ascii=False, default=str)


class NotEvaluated(Exception):
    pass


def use_utf8_output():
    for stream in (sys.stdout, sys.stderr):
        if hasattr(stream, "reconfigure"):
            stream.reconfigure(encoding="utf-8", errors="replace")


def read(path):
    with open(path, encoding="utf-8") as f:
        return f.read()


def read_dir_text(path):
    if not os.path.isdir(path):
        return ""
    parts = []
    for name in sorted(os.listdir(path)):
        if name.endswith(".md"):
            parts.append(read(os.path.join(path, name)))
    return "\n".join(parts)


def heading_exists(text, key):
    return re.search(r"(?m)^#+\s*%s\b" % re.escape(key), text) is not None


def decision_log_path(repo, manifest_rel):
    """從律法清冊讀 @decision-log。專案專屬的路徑一律來自清冊，不寫死在這裡。"""
    manifest = os.path.join(repo, manifest_rel)
    if not os.path.isfile(manifest):
        raise NotEvaluated("找不到律法清冊 %s" % manifest_rel)
    for line in read(manifest).splitlines():
        parts = line.split()
        if len(parts) == 2 and parts[0] == "@decision-log":
            return parts[1]
    raise NotEvaluated("律法清冊 %s 沒有 @decision-log 指示" % manifest_rel)


class Context:
    """跨 aggregate 共用的資料：各種出處的來源文字、禁用詞彙。"""

    def __init__(self, repo, specs_dir, manifest_rel, design_rel, testing_rel):
        self.repo = repo
        log_rel = decision_log_path(repo, manifest_rel)
        log = os.path.join(repo, log_rel)
        if not os.path.isfile(log):
            raise NotEvaluated("找不到決策紀錄 %s（清冊的 @decision-log）" % log_rel)
        self.decision_log = read(log)
        # None 代表專案沒有設定：用到該類出處的規則會被列為問題
        self.design_text = read_dir_text(os.path.join(repo, design_rel)) if design_rel else None
        self.testing_text = read_dir_text(os.path.join(repo, testing_rel)) if testing_rel else None
        self.forbidden = []
        vocab = os.path.join(specs_dir, "forbidden-vocabulary.txt")
        if not os.path.isfile(vocab):
            raise NotEvaluated("找不到 %s；沒有它，實作詞彙的檢查等於沒做" % os.path.relpath(vocab, repo))
        for number, line in enumerate(read(vocab).splitlines(), 1):
            line = line.strip()
            if line and not line.startswith("#"):
                try:
                    self.forbidden.append(re.compile(line, re.IGNORECASE))
                except re.error as error:
                    raise NotEvaluated("forbidden-vocabulary.txt 第 %d 行不是合法的正規表示式：%s" % (number, error))
        self.profile = load_profile(specs_dir, self.decision_log)
        self.categories = set(self.profile["error_categories"])
        self.schemas = {name: resolve_profile(schema, self.profile) for name, schema in FILE_SCHEMAS.items()}
        self.schemas_by_version = {
            version: {name: resolve_profile(schema, self.profile) for name, schema in schemas.items()}
            for version, schemas in SCHEMAS_BY_VERSION.items()}


class Aggregate:
    def __init__(self, ctx, path, label):
        self.ctx, self.path, self.label = ctx, path, label
        self.errors, self.notes = [], []
        self.docs = {}
        self.rules = {}          # id -> (檔案, 規則)
        self.needs_outcome = set()
        self.covered = set()
        self.usecases = {}
        self.error_codes = set()
        self.guards = set()
        self.open_questions = ""
        # D-065：版本，以及跨目錄引用要用的資料
        self.version = 1
        self.dirname = os.path.basename(os.path.normpath(path))
        self.peers = {}          # 目錄名稱 -> 這份規格可以引用的 Aggregate
        self.fixtures = {}
        self.ports = {}          # 埠 -> 操作名稱的集合
        self.transitions = set() # 轉移的 id（v2 要有範例涵蓋）

    def error(self, where, message):
        self.errors.append("%s：%s" % (where, message))

    def as_list(self, where, value):
        """None 視為空清單；其他非清單的值記成問題，回傳空清單。"""
        if value is None:
            return []
        if not isinstance(value, list):
            self.error(where, "必須是清單，實際是 %r" % (value,))
            return []
        return value

    def names(self, where, value):
        """字串清單（needs、basis 之類）。非字串的項目記成問題並略過。"""
        out = []
        for item in self.as_list(where, value):
            if isinstance(item, str):
                out.append(item)
            else:
                self.error(where, "每一項必須是字串，實際是 %r" % (item,))
        return out

    # ---------- 讀檔 ----------

    def load(self):
        loaded, missing = {}, []
        for name in KINDS:
            full = os.path.join(self.path, name)
            if not os.path.isfile(full):
                missing.append(name)
                continue
            try:
                doc = yaml.safe_load(read(full))
            except yaml.YAMLError as error:
                self.error(name, "YAML 讀不進來：%s" % str(error).splitlines()[0])
                continue
            if not isinstance(doc, dict):
                self.error(name, "最上層必須是對應表")
                continue
            loaded[name] = doc
        # 目錄的版本：有任何一個檔案寫 2，整個目錄照 v2 檢查（D-065 第 1 點）
        versions = {name: doc.get("spec_version") for name, doc in loaded.items()}
        if any(v == 2 and not isinstance(v, bool) for v in versions.values()):
            self.version = 2
        for name in missing:
            # v2 的目錄只有 model.yaml 是必要的：一個 aggregate 可以只有狀態機，改變它的 use case 在別的目錄
            if self.version == 1 or name == "model.yaml":
                self.error(name, "缺少這個檔案")
        for name, doc in loaded.items():
            found = versions[name]
            if found not in SPEC_VERSIONS or isinstance(found, bool):
                self.error(name, "spec_version 必須是 1 或 2，實際是 %r" % (found,))
            elif found != self.version:
                self.error(name, "spec_version 是 %d，同一個目錄的其他檔案是 %d：同一個目錄的版本必須相同"
                           % (found, self.version))
            if doc.get("kind") != KINDS[name]:
                self.error(name, "kind 必須是 %s，實際是 %r" % (KINDS[name], doc.get("kind")))
            for where, message in schema_problems(doc, self.ctx.schemas_by_version[self.version][name], ""):
                self.error("%s %s" % (name, where or "最上層"), message)
            self.docs[name] = doc
        questions = os.path.join(self.path, "OPEN-QUESTIONS.md")
        if os.path.isfile(questions):
            self.open_questions = read(questions)
        self.collect_referables()

    def collect_referables(self):
        """別的目錄可以引用的東西：fixture 與埠。use case 由 check_usecases 收集。"""
        fixtures = (self.docs.get("examples.yaml") or {}).get("fixtures")
        if isinstance(fixtures, dict):
            self.fixtures = {name: f for name, f in fixtures.items() if isinstance(name, str) and isinstance(f, dict)}
        for port in (self.docs.get("model.yaml") or {}).get("ports") or [] if self.version == 2 else []:
            if isinstance(port, dict) and isinstance(port.get("name"), str):
                operations = port.get("operations")
                self.ports[port["name"]] = {o["name"] for o in operations if isinstance(o, dict)
                                            and isinstance(o.get("name"), str)} if isinstance(operations, list) else set()

    def resolve(self, ref, table):
        """D-065 第 9 點：名稱先在本目錄找；<目錄>.<名稱> 在指定的目錄找。回傳 (Aggregate, 名稱) 或 None。"""
        if not isinstance(ref, str):
            return None
        if ref in getattr(self, table):
            return self, ref
        head, dot, rest = ref.partition(".")
        peer = self.peers.get(head) if dot else None
        if peer is not None and rest in getattr(peer, table):
            return peer, rest
        return None

    # ---------- 規則 ----------

    def collect_rules(self, name, node, trail):
        if isinstance(node, dict):
            rule_id = node.get("id")
            if "id" in node and not isinstance(rule_id, str):
                self.error("%s %s" % (name, ".".join(trail) or "最上層"), "id 必須是字串，實際是 %r" % (rule_id,))
            elif isinstance(rule_id, str):
                where = "%s %s" % (name, rule_id)
                if not ID_PATTERN.match(rule_id):
                    self.error(where, "id 的格式必須是 <前綴>-<分組>-<序號>，例如 RP-PUB-03")
                if rule_id in self.rules:
                    self.error(where, "id 重複")
                self.rules[rule_id] = (name, node)
                # 前置檢查與 use case 的條件規則必須有結果；不變量與成功狀態的結果是隱含的
                if trail[:1] == ["guards"] or trail[-1:] == ["rules"]:
                    self.needs_outcome.add(rule_id)
            for key, value in node.items():
                self.collect_rules(name, value, trail + [key])
        elif isinstance(node, list):
            for value in node:
                self.collect_rules(name, value, trail)

    def check_rule(self, rule_id, name, rule, all_ids):
        where = "%s %s" % (name, rule_id)
        status = rule.get("status")
        if status == "decided":
            sources = rule.get("source")
            if not sources:
                self.error(where, "decided 的規則必須有 source")
            else:
                self.check_sources(where, sources, all_ids)
        elif status == "proposed":
            question = rule.get("question")
            if not isinstance(question, str) or not re.match(r"^Q-\d+$", question):
                self.error(where, "proposed 的規則必須有 question（例如 Q-07）")
            elif not heading_exists(self.open_questions, question):
                self.error(where, "question %s 不在 OPEN-QUESTIONS.md 的標題裡" % question)
        else:
            self.error(where, "status 必須是 decided 或 proposed，實際是 %r" % (status,))
        outcome = rule.get("outcome")
        if outcome is None:
            if rule_id in self.needs_outcome:
                self.error(where, "前置檢查與 use case 的規則必須有 outcome")
        elif not isinstance(outcome, dict) or not isinstance(outcome.get("result"), str) \
                or outcome["result"] not in RESULTS:
            self.error(where, "outcome.result 必須是 SUCCESS 或 REJECTED")
        elif outcome["result"] == "REJECTED":
            code = outcome.get("code")
            if not code:
                self.error(where, "REJECTED 必須帶 code")
            elif not isinstance(code, str):
                self.error(where, "code 必須是字串，實際是 %r" % (code,))
            elif code not in self.error_codes:
                self.error(where, "code %s 沒有定義在 error_codes" % code)

    def check_sources(self, where, sources, all_ids):
        if not isinstance(sources, list):
            self.error(where, "source 必須是清單")
            return
        for item in sources:
            if not isinstance(item, dict) or len(set(item) & SOURCE_KINDS) != 1:
                self.error(where, "source 的每一項必須是 decision、design_decision、doc、example、scenario、rule 其中一種：%r" % (item,))
                continue
            kind = (set(item) & SOURCE_KINDS).pop()
            value = str(item[kind])
            if kind == "decision":
                ok = bool(re.match(r"^D-\d+$", value)) and heading_exists(self.ctx.decision_log, value)
            elif kind == "design_decision":
                if self.ctx.design_text is None:
                    self.error(where, "沒有設定 --design-docs，無法核對 design_decision：%s" % value)
                    continue
                ok = bool(re.match(r"^DD-\d+$", value)) and heading_exists(self.ctx.design_text, value)
            elif kind == "doc":
                ok = os.path.isfile(os.path.join(self.ctx.repo, value))
            elif kind in ("example", "scenario"):
                if self.ctx.testing_text is None:
                    self.error(where, "沒有設定 --testing-docs，無法核對 %s：%s" % (kind, value))
                    continue
                ok = re.search(r"\b%s\b" % re.escape(value), self.ctx.testing_text) is not None
            else:
                ok = value in all_ids
            if not ok:
                self.error(where, "source 指向不存在的 %s：%s" % (kind, value))

    # ---------- use case ----------

    def check_usecases(self):
        doc = self.docs.get("usecases.yaml")
        if not doc:
            return
        for entry in self.as_list("usecases.yaml error_codes", doc.get("error_codes")):
            if not isinstance(entry, dict) or not isinstance(entry.get("code"), str) or not entry["code"]:
                self.error("usecases.yaml error_codes", "每一項必須有字串的 code")
                continue
            if entry.get("category") not in self.ctx.categories:
                self.error("usecases.yaml error_codes %s" % entry["code"],
                           "category 必須是 %s 其中一種（%s）" % ("、".join(sorted(self.ctx.categories)), PROFILE_NAME))
            self.error_codes.add(entry["code"])
        for guard in self.as_list("usecases.yaml guards", doc.get("guards")):
            if isinstance(guard, dict) and isinstance(guard.get("id"), str):
                self.guards.add(guard["id"])
        for usecase in self.as_list("usecases.yaml usecases", doc.get("usecases")):
            if not isinstance(usecase, dict) or not isinstance(usecase.get("name"), str) or not usecase["name"]:
                self.error("usecases.yaml", "每支 use case 必須有字串的 name")
                continue
            name = usecase["name"]
            self.usecases[name] = usecase
            own_guards = []
            for guard in self.as_list("usecases.yaml %s guards" % name, usecase.get("guards")):
                if not isinstance(guard, str) or guard not in self.guards:
                    self.error("usecases.yaml %s" % name, "guards 引用了不存在的 %s" % guard)
                else:
                    own_guards.append(guard)
            # D-063 第 5 點：例外只能針對這支 use case 自己的前置檢查
            # 型別不對的情形已由欄位表報過，這裡略過，不重複報
            exceptions = usecase.get("guard_exceptions")
            for exception in exceptions if isinstance(exceptions, list) else []:
                if not isinstance(exception, dict):
                    continue
                guard = exception.get("guard")
                if guard not in own_guards:
                    self.error("usecases.yaml %s guard_exceptions" % name,
                               "guard %s 不在這支 use case 的 guards 裡" % (guard,))

    # ---------- 模型的引用（D-063 第 5 點）----------

    def check_model_references(self):
        doc = self.docs.get("model.yaml")
        if not doc:
            return
        # 型別不對的情形已由欄位表報過，這裡只檢查結構正確的部分，不重複報
        def items(key):
            value = doc.get(key)
            return value if isinstance(value, list) else []

        enums = {}
        for enum in items("enums"):
            if isinstance(enum, dict) and isinstance(enum.get("name"), str) and isinstance(enum.get("values"), list):
                enums[enum["name"]] = {v for v in enum["values"] if isinstance(v, str)}
        states = set()
        seen = {}
        for transition in items("transitions"):
            if not isinstance(transition, dict):
                continue
            where = "model.yaml %s" % transition.get("id", "transitions")
            by = transition.get("by")
            if self.version == 2:
                if isinstance(transition.get("id"), str):
                    self.transitions.add(transition["id"])
                if self.resolve(by, "usecases") is None:
                    self.error(where, "by 必須是規格裡的 use case（別的目錄寫 <目錄>.<名稱>），實際是 %r" % (by,))
                # D-065 第 8 點：同一個 from、同一支 use case 的多條轉移，when 必須不同
                key = canonical([transition.get("from"), by, transition.get("when")])
                if key in seen:
                    self.error(where, "與 %s 的 from、by 相同：要用不同的 when 區分" % seen[key])
                seen.setdefault(key, transition.get("id", "?"))
            elif by not in self.usecases:
                self.error(where, "by 必須是同目錄 usecases.yaml 的 use case，實際是 %r" % (by,))
            for key in ("from", "to"):
                if isinstance(transition.get(key), str):
                    states.add(transition[key])
                elif key not in transition:
                    self.error(where, "缺少 %s" % key)
        for index, deferred in enumerate(items("deferred_transitions") if self.version == 2 else [], 1):
            if not isinstance(deferred, dict):
                continue
            where = "model.yaml deferred_transitions 第 %d 項" % index
            for key in ("from", "to", "by"):
                if key not in deferred:
                    self.error(where, "缺少 %s" % key)
            for key in ("from", "to"):
                if isinstance(deferred.get(key), str):
                    states.add(deferred[key])
            if self.resolve(deferred.get("by"), "usecases") is not None:
                self.error(where, "%s 已經在規格裡：這條轉移要搬到 transitions 並給 id" % deferred["by"])
        terminal = [s for s in items("terminal_states") if isinstance(s, str)]
        wanted = states | set(terminal)
        if not wanted:
            return
        if "enums" not in doc:
            # 沒寫 enums 時欄位表不會報，要在這裡報（#109 review）
            self.error("model.yaml", "有轉移或 terminal_states，卻沒有 enums：狀態（%s）無法核對"
                       % "、".join(sorted(wanted)))
            return
        # enums 寫了但不合法時（欄位表已報過），列舉的核對沒有意義，略過以免連帶報錯
        if not isinstance(doc["enums"], list):
            return
        owners = sorted(name for name, values in enums.items() if wanted <= values)
        if not owners:
            self.error("model.yaml transitions", "轉移的狀態與 terminal_states（%s）必須都是同一個列舉的值"
                       % "、".join(sorted(wanted)))

    # ---------- 範例 ----------

    def check_examples(self, all_ids, proposed):
        doc = self.docs.get("examples.yaml")
        if not doc:
            return set()
        fixtures = doc.get("fixtures")
        if fixtures is None:
            fixtures = {}
        elif not isinstance(fixtures, dict):
            self.error("examples.yaml fixtures", "必須是以名稱為鍵的對應表，實際是 %r" % (fixtures,))
            fixtures = {}
        covered = set()

        def check_steps(where, steps, needs):
            for step in self.as_list(where, steps):
                if not isinstance(step, dict):
                    self.error(where, "每個步驟必須是對應表，實際是 %r" % (step,))
                    continue
                if "use" in step and (not isinstance(step["use"], str) or step["use"] not in fixtures):
                    self.error(where, "use 引用了不存在的 fixture %s" % (step["use"],))
                run = step.get("run")
                if run is not None and not isinstance(run, str):
                    self.error(where, "run 必須是 use case 名稱，實際是 %r" % (run,))
                elif run and run not in self.usecases and run not in needs:
                    self.error(where, "執行的 %s 不在 usecases.yaml，也沒有列在 needs" % run)

        for fname, fixture in fixtures.items():
            fwhere = "examples.yaml fixture %s" % fname
            if not isinstance(fixture, dict):
                self.error(fwhere, "必須是對應表")
                continue
            check_steps(fwhere, fixture.get("steps"), set(self.names(fwhere + " needs", fixture.get("needs"))))
        for example in self.as_list("examples.yaml examples", doc.get("examples")):
            if not isinstance(example, dict):
                self.error("examples.yaml examples", "每個範例必須是對應表，實際是 %r" % (example,))
                continue
            name = example.get("name", "?")
            where = "examples.yaml %s" % name
            needs = set(self.names(where + " needs", example.get("needs")))
            basis = self.names(where + " basis", example.get("basis"))
            if not basis:
                self.error(where, "沒有 basis")
            for rule_id in basis:
                if rule_id not in all_ids:
                    self.error(where, "basis 引用了不存在的規則 %s" % rule_id)
                covered.add(rule_id)
            pending = [r for r in basis if r in proposed]
            if pending:
                self.notes.append("%s：basis 含 proposed 規則 %s，還不能當驗收依據" % (where, "、".join(pending)))
            when = example.get("when")
            check_steps(where + " given", example.get("given"), needs)
            check_steps(where + " when", [when] if isinstance(when, dict) else when, needs)
            check_steps(where + " observe", example.get("observe"), needs)
        self.check_distinguishable(doc, fixtures)
        return covered

    # ---------- 範例：spec_version 2（D-065）----------

    def check_path(self, where, what, path, allow_index=False, allow_root=False):
        if path == "." and allow_root:
            return
        if not isinstance(path, str) or not PATH_PATTERN.match(VARIABLE.sub("v", path)):
            self.error(where, "%s 的路徑寫法不對：%r（可以用 a.b、a[*]、a[k=v]）" % (what, path))
        elif INDEX_IN_PATH.search(path) and not allow_index:
            self.error(where, "%s 不能用 [數字]，只有 save 可以：%s" % (what, path))
        elif allow_index and re.search(r"\[(\*|[^\]\d][^\]]*)\]", path):
            self.error(where, "save 只能取一個值，不能用 [*] 或 [k=v]：%s" % path)

    def check_times(self, where, node):
        """看起來像時間的字串必須帶時區（D-065 第 5 點）。"""
        if isinstance(node, dict):
            for value in node.values():
                self.check_times(where, value)
        elif isinstance(node, list):
            for value in node:
                self.check_times(where, value)
        elif isinstance(node, str) and TIME_LIKE.match(node) and not ZONED_TIME.match(node):
            self.error(where, "時間必須帶時區（例如 2026-11-19T09:00:00Z）：%s" % node)

    def check_assertion(self, where, assertion, target, expectations):
        """then、expect 共用（D-065 第 2 點）。target 是被比對的 use case：(Aggregate, 名稱) 或 None。

        回傳展開 use_expect 之後的斷言；無法展開時回傳 None。
        """
        if not isinstance(assertion, dict):
            return None
        merged = dict(assertion)
        if "use_expect" in merged:
            name = merged.pop("use_expect")
            base = expectations.get(name) if isinstance(name, str) else None
            if not isinstance(base, dict):
                self.error(where, "use_expect 引用了不存在的具名斷言 %s" % (name,))
                return None
            merged = dict(base, **merged)
        self.check_assertion_fields(where, merged, target)
        return merged

    def check_assertion_fields(self, where, merged, target):
        result = merged.get("result")
        if result not in RESULTS:
            self.error(where, "必須有 result（SUCCESS 或 REJECTED）")
        code = merged.get("code")
        if result == "REJECTED":
            if not code:
                self.error(where, "REJECTED 必須帶 code")
            extra = [key for key in ("output", "absent", "only", "emits", "emits_include") if key in merged]
            if extra:
                self.error(where, "REJECTED 不能有 %s：被拒絕沒有輸出，也沒有事件" % "、".join(extra))
        if isinstance(code, str) and target is not None and code not in target[0].error_codes:
            self.error(where, "code %s 沒有定義在 %s 的 error_codes" % (code, target[0].dirname))
        output = merged.get("output")
        for key in output if isinstance(output, dict) else []:
            if isinstance(key, str) and ("[" in key or "]" in key):
                self.check_path(where, "output 的鍵", key)
        for path in merged.get("absent") if isinstance(merged.get("absent"), list) else []:
            self.check_path(where, "absent", path)
        only = merged.get("only")
        for path, fields in (only.items() if isinstance(only, dict) else []):
            self.check_path(where, "only", path, allow_root=True)
            if not fields:
                self.error(where, "only 的 %s 沒有列出任何欄位" % path)
        self.check_times(where, output)
        declared = None
        if target is not None:
            usecase = target[0].usecases.get(target[1]) or {}
            success = usecase.get("success") if isinstance(usecase.get("success"), dict) else {}
            declared = {e["event"]: e for e in success.get("emits") or []
                        if isinstance(e, dict) and isinstance(e.get("event"), str)}
        for key in ("emits", "emits_include"):
            for emitted in merged.get(key) if isinstance(merged.get(key), list) else []:
                if not isinstance(emitted, dict):
                    continue
                event = emitted.get("event")
                if not isinstance(event, str):
                    self.error(where, "%s 的每一項必須有 event" % key)
                elif declared is None:
                    self.error(where, "%s：when 執行的 use case 不在規格裡，無從核對事件 %s" % (key, event))
                elif event not in declared:
                    self.error(where, "事件 %s 沒有在 %s 的 success.emits 宣告" % (event, target[1]))
                elif declared[event].get("status") == "proposed":
                    self.notes.append("%s：斷言了 proposed 的事件 %s，還不能當驗收依據" % (where, event))
                self.check_times(where, emitted.get("with"))

    def check_step_v2(self, where, step, needs, actors, expectations):
        """回傳這一步執行的 use case：(Aggregate, 名稱)；不是 run 或找不到時回傳 None。"""
        if not isinstance(step, dict):
            self.error(where, "每個步驟必須是對應表，實際是 %r" % (step,))
            return None
        kinds = [key for key in ("run", "use", "port", "advance") if key in step]
        if len(kinds) != 1:
            self.error(where, "步驟必須是 run、use、port、advance 其中一種，實際有 %s" % ("、".join(kinds) or "無"))
            return None
        kind = kinds[0]
        if "returns" in step and kind != "port":
            self.error(where, "returns 只能和 port 一起用")
        if kind == "use":
            if self.resolve(step["use"], "fixtures") is None:
                self.error(where, "use 引用了不存在的 fixture %s（別的目錄寫 <目錄>.<名稱>）" % (step["use"],))
            return None
        if kind == "advance":
            if not isinstance(step["advance"], str) or not DURATION.match(step["advance"]):
                self.error(where, "advance 必須是 ISO 8601 期間（例如 PT1H、P2D），實際是 %r" % (step["advance"],))
            return None
        if kind == "port":
            ref = step["port"]
            head, dot, operation = ref.rpartition(".") if isinstance(ref, str) else ("", "", "")
            hit = self.resolve(head, "ports") if dot else None
            if hit is None:
                self.error(where, "port 引用了不存在的埠 %s（寫法是 <埠>.<操作>）" % (ref,))
            elif operation not in hit[0].ports[hit[1]]:
                self.error(where, "埠 %s 沒有操作 %s" % (hit[1], operation))
            if "returns" not in step:
                self.error(where, "port 必須設定 returns")
            self.check_times(where, step.get("returns"))
            return None
        run = step["run"]
        target = self.resolve(run, "usecases")
        if target is None and run not in needs:
            self.error(where, "執行的 %s 不在本目錄的 usecases.yaml，也沒有列在 needs"
                              "（別的目錄的 use case 寫 <目錄>.<名稱>）" % (run,))
        actor = step.get("as")
        if isinstance(actor, str) and actor not in actors:
            self.error(where, "as 引用了不存在的操作者 %s" % actor)
        save = step.get("save")
        for path in save.values() if isinstance(save, dict) else []:
            self.check_path(where, "save", path, allow_index=True)
        self.check_times(where, step.get("input"))
        if "expect" in step:
            self.check_assertion(where + " expect", step["expect"], target, expectations)
        return target

    def expand_matrix(self, example):
        """D-065 第 6 點：把有 matrix 的範例展開成每一列一個範例。回傳 [(名稱, 範例)]。"""
        name = example.get("name", "?")
        where = "examples.yaml %s" % name
        matrix = example.get("matrix")
        if not isinstance(matrix, dict):
            return [(name, example)]
        columns, key, rows = matrix.get("columns"), matrix.get("key"), matrix.get("rows")
        if not isinstance(columns, list) or not columns or len(set(map(str, columns))) != len(columns):
            self.error(where, "matrix.columns 必須是不重複的欄名清單")
            return []
        if not isinstance(key, list) or not key or any(k not in columns for k in key):
            self.error(where, "matrix.key 必須列出組成名稱的欄，而且都要在 columns 裡")
            return []
        if not isinstance(rows, list) or not rows:
            self.error(where, "matrix.rows 不可以是空的")
            return []
        template = {k: v for k, v in example.items() if k != "matrix"}

        def saved_names(node, found):
            if isinstance(node, dict):
                if isinstance(node.get("save"), dict):
                    found.update(str(k) for k in node["save"])
                for value in node.values():
                    saved_names(value, found)
            elif isinstance(node, list):
                for value in node:
                    saved_names(value, found)
            return found
        text = json.dumps(template, ensure_ascii=False)
        out, clashes = [], set()
        for index, row in enumerate(rows, 1):
            if not isinstance(row, list) or len(row) != len(columns):
                self.error(where, "matrix 第 %d 列的欄數與 columns 不符" % index)
                continue
            if any(isinstance(v, (dict, list)) or v is None for v in row):
                self.error(where, "matrix 第 %d 列的值必須是字串、數字或布林" % index)
                continue
            values = dict(zip(columns, row))
            bad = [k for k in key if not MATRIX_KEY_VALUE.match(str(values[k]))]
            if bad:
                self.error(where, "matrix 第 %d 列：key 的欄（%s）只能用英數字與 _ -" % (index, "、".join(bad)))
                continue
            filled = text
            for column, value in values.items():
                # 值先轉成 JSON 字串的內容，代入後仍是合法的 JSON
                filled = filled.replace("${%s}" % column, json.dumps(str(value), ensure_ascii=False)[1:-1])
            row_example = json.loads(filled)
            # 欄名與變數都寫成 ${名稱}：同名時不知道代入哪一個。fixture 存的變數也算
            saved = saved_names([self.expand_steps(row_example.get("given")), row_example.get("when"),
                                 row_example.get("observe")], set())
            clashes |= saved & set(map(str, columns))
            out.append((".".join([str(name)] + [str(values[k]) for k in key]), row_example))
        if clashes:
            self.error(where, "matrix 的欄名與 save 的變數同名：%s" % "、".join(sorted(clashes)))
        return out

    def expand_steps(self, steps, seen=frozenset()):
        """把 use 換成 fixture 的步驟，跨目錄的也展開。循環引用照原樣留著。"""
        out = []
        for step in steps if isinstance(steps, list) else []:
            hit = self.resolve(step.get("use"), "fixtures") if isinstance(step, dict) and "use" in step else None
            if hit is None or (hit[0].label, hit[1]) in seen:
                out.append(step)
                continue
            owner, name = hit
            out += owner.expand_steps(owner.fixtures[name].get("steps"), seen | {(owner.label, name)})
        return out

    def check_examples_v2(self, all_ids, proposed):
        doc = self.docs.get("examples.yaml")
        if not doc:
            return set()
        actors = doc.get("actors") if isinstance(doc.get("actors"), dict) else {}
        expectations = doc.get("expectations") if isinstance(doc.get("expectations"), dict) else {}
        covered = set()
        for fname, fixture in self.fixtures.items():
            fwhere = "examples.yaml fixture %s" % fname
            needs = set(self.names(fwhere + " needs", fixture.get("needs")))
            for step in self.as_list(fwhere, fixture.get("steps")):
                self.check_step_v2(fwhere, step, needs, actors, expectations)
        for ename, assertion in expectations.items():
            if isinstance(assertion, dict):
                if "use_expect" in assertion:
                    self.error("examples.yaml 具名斷言 %s" % ename, "具名斷言不能再引用具名斷言")
                self.check_assertion_fields("examples.yaml 具名斷言 %s" % ename, assertion, None)
        expanded = []
        for example in self.as_list("examples.yaml examples", doc.get("examples")):
            if isinstance(example, dict):
                expanded += self.expand_matrix(example)
        names, verdicts = set(), {}
        for name, example in expanded:
            where = "examples.yaml %s" % name
            if not isinstance(name, str) or not EXAMPLE_NAME.match(name):
                self.error(where, "範例名稱只能用英數字與 _ . -")
            if name in names:
                self.error(where, "範例名稱重複（矩陣的話，是 matrix.key 的組合不唯一）")
            names.add(name)
            needs = set(self.names(where + " needs", example.get("needs")))
            basis = self.names(where + " basis", example.get("basis"))
            if not basis:
                self.error(where, "沒有 basis")
            for rule_id in basis:
                if rule_id not in all_ids:
                    self.error(where, "basis 引用了不存在的規則 %s" % rule_id)
                covered.add(rule_id)
            pending = [r for r in basis if r in proposed]
            if pending:
                self.notes.append("%s：basis 含 proposed 規則 %s，還不能當驗收依據" % (where, "、".join(pending)))
            clock = example.get("clock")
            if isinstance(clock, dict):
                start = clock.get("start")
                if not isinstance(start, str) or not ZONED_TIME.match(start):
                    self.error(where, "clock.start 必須是帶時區的 ISO 8601 時間，實際是 %r" % (start,))
            for step in self.as_list(where + " given", example.get("given")):
                self.check_step_v2(where + " given", step, needs, actors, expectations)
            for step in self.as_list(where + " observe", example.get("observe")):
                self.check_step_v2(where + " observe", step, needs, actors, expectations)
            when = example.get("when")
            target = None
            if not isinstance(when, dict) or "run" not in when:
                self.error(where, "when 必須執行一支 use case（run）")
            else:
                target = self.check_step_v2(where + " when", when, needs, actors, expectations)
            then = self.check_assertion(where + " then", example.get("then"), target, expectations)
            if example.get("then") is None:
                self.error(where, "沒有 then")
            if then is None:
                continue
            # D-065 第 12 點：情境相同時，result 或 code 不同才是「不可能同時通過」
            situation = canonical([self.expand_steps(example.get("given")), when, clock])
            verdict = (then.get("result"), then.get("code"))
            rest = canonical([then, example.get("observe")])
            if situation in verdicts:
                other, other_verdict, other_rest = verdicts[situation]
                if verdict != other_verdict:
                    self.error(where, "given、when 與範例 %s 相同，result 或 code 卻不同：兩者不可能同時通過，"
                                      "範例沒有寫出區分兩種情境的前提" % other)
                elif rest == other_rest:
                    self.notes.append("%s：given、when、then、observe 都與範例 %s 相同（可能是刻意的重複）" % (where, other))
                else:
                    self.notes.append("%s：given、when 與範例 %s 相同，斷言的內容不同" % (where, other))
            else:
                verdicts[situation] = (name, verdict, rest)
        for index, entry in enumerate(self.as_list("examples.yaml not_covered", doc.get("not_covered")), 1):
            for rule_id in entry.get("rules") or [] if isinstance(entry, dict) else []:
                if rule_id not in all_ids:
                    self.error("examples.yaml not_covered 第 %d 項" % index,
                               "rules 只能放規則 id，%s 不是；其他的項目寫在 item" % (rule_id,))
        return covered

    def check_distinguishable(self, doc, fixtures):
        """D-063 第 4 點：given（展開 fixture 後）與 when 相同的兩個範例，then 不能不同。"""
        def expand(steps, seen):
            out = []
            for step in steps if isinstance(steps, list) else []:
                if isinstance(step, dict) and isinstance(step.get("use"), str) and step["use"] in fixtures:
                    name = step["use"]
                    if name in seen:          # 循環引用：照原樣比較，不再展開
                        out.append(step)
                        continue
                    fixture = fixtures[name]
                    out += expand(fixture.get("steps") if isinstance(fixture, dict) else None, seen | {name})
                else:
                    out.append(step)
            return out

        first = {}
        for example in doc.get("examples") or []:
            if not isinstance(example, dict):
                continue
            situation = canonical([expand(example.get("given"), frozenset()), example.get("when")])
            name = example.get("name", "?")
            if situation not in first:
                first[situation] = example
                continue
            other = first[situation]
            if canonical(example.get("then")) != canonical(other.get("then")):
                self.error("examples.yaml %s" % name,
                           "given、when 與範例 %s 相同，then 卻不同：兩者不可能同時通過，"
                           "範例沒有寫出區分兩種情境的前提" % other.get("name", "?"))
            elif canonical(example.get("observe")) == canonical(other.get("observe")):
                self.notes.append("examples.yaml %s：given、when、then、observe 都與範例 %s 相同（可能是刻意的重複）"
                                  % (name, other.get("name", "?")))

    # ---------- 實作詞彙 ----------

    def check_vocabulary(self):
        def walk(name, node, trail):
            if isinstance(node, dict):
                for key, value in node.items():
                    walk(name, value, trail + [str(key)])
            elif isinstance(node, list):
                for index, value in enumerate(node):
                    walk(name, value, trail + [str(index)])
            elif isinstance(node, str):
                for pattern in self.ctx.forbidden:
                    hit = pattern.search(node)
                    if hit:
                        self.error("%s %s" % (name, ".".join(trail)), "出現實作詞彙「%s」" % hit.group(0))
        for name, doc in self.docs.items():
            walk(name, doc, [])


def lint(repo, specs_dir_rel, report_only, manifest_rel=DEFAULT_MANIFEST, design_rel=None, testing_rel=None):
    if yaml is None:
        raise NotEvaluated("需要 PyYAML（pip install PyYAML）")
    specs_dir = os.path.join(repo, specs_dir_rel)
    if not os.path.isdir(specs_dir):
        raise NotEvaluated("找不到規格目錄 %s" % specs_dir_rel)
    ctx = Context(repo, specs_dir, manifest_rel, design_rel, testing_rel)
    targets = [(os.path.join(specs_dir, d), "%s/%s" % (specs_dir_rel, d), False)
               for d in sorted(os.listdir(specs_dir)) if os.path.isdir(os.path.join(specs_dir, d))]
    for rel in report_only:
        base = os.path.join(repo, rel)
        if os.path.isdir(base):
            targets += [(os.path.join(base, d), "%s/%s" % (rel, d), True)
                        for d in sorted(os.listdir(base)) if os.path.isdir(os.path.join(base, d))]
    aggregates = []
    for path, label, lenient in targets:
        agg = Aggregate(ctx, path, label)
        agg.lenient = lenient
        agg.load()
        # 規則只定義在模型與 use case 裡；範例的輸入也可能有 id 欄位，不能算進去
        for name, doc in agg.docs.items():
            if name != "examples.yaml":
                agg.collect_rules(name, doc, [])
        aggregates.append(agg)
    all_ids = set()
    for agg in aggregates:
        dupes = all_ids & set(agg.rules)
        for rule_id in sorted(dupes):
            agg.error(rule_id, "id 與另一個 aggregate 重複")
        all_ids |= set(agg.rules)
    # 擋下的規格只能引用擋下的規格裡的規則；草稿可以引用兩者。
    # 否則正式規格可以指向一條只存在於草稿、而且草稿本身有錯也不會擋的規則。
    strict_ids = set()
    for agg in aggregates:
        if not agg.lenient:
            strict_ids |= set(agg.rules)
    proposed_all = {r for agg in aggregates for r, (_, rule) in agg.rules.items() if rule.get("status") == "proposed"}
    proposed_strict = {r for r in proposed_all if r in strict_ids}
    # D-065 第 9 點：跨目錄的引用。擋下的規格只能引用擋下的規格；草稿可以引用兩者，同名時先找草稿。
    strict_peers = {agg.dirname: agg for agg in aggregates if not agg.lenient}
    lenient_peers = dict(strict_peers)
    lenient_peers.update({agg.dirname: agg for agg in aggregates if agg.lenient})
    for agg in aggregates:
        agg.peers = lenient_peers if agg.lenient else strict_peers
    # D-065 第 5 點：spec_version 2 的規格需要 profile_version 2 的設定檔，不使用預設值
    if ctx.profile.get("profile_version") != 2:
        for agg in aggregates:
            if agg.version == 2:
                message = "spec_version 2 的規格需要 profile_version 2 的 %s（clock_start、local_time_zone）" % PROFILE_NAME
                if not agg.lenient:
                    raise NotEvaluated("%s：%s" % (agg.label, message))
                agg.error(PROFILE_NAME, message)
    # use case 要先全部收集完，別的目錄才引用得到
    for agg in aggregates:
        try:
            agg.check_usecases()
        except (TypeError, AttributeError, KeyError, ValueError) as error:
            agg.error("結構", "內容的型別不合法，這個 aggregate 的檢查沒有做完（%s: %s）" % (type(error).__name__, error))
    for agg in aggregates:
        known_ids = all_ids if agg.lenient else strict_ids
        try:
            agg.check_model_references()
            for rule_id, (name, rule) in sorted(agg.rules.items()):
                agg.check_rule(rule_id, name, rule, known_ids)
            check = agg.check_examples_v2 if agg.version == 2 else agg.check_examples
            agg.covered = check(known_ids, proposed_all if agg.lenient else proposed_strict)
            agg.check_vocabulary()
        except (TypeError, AttributeError, KeyError, ValueError) as error:
            # 保險：結構不合法到上面的檢查沒預料到時，記成問題而不是讓整個檢查中止。
            # 擋下的規格因此未通過；只列出的草稿不會讓 CI 失敗。
            agg.error("結構", "內容的型別不合法，這個 aggregate 的檢查沒有做完（%s: %s）" % (type(error).__name__, error))
    # 涵蓋：擋下的規則只算擋下的範例；草稿的規則算所有範例
    covered_strict = {r for agg in aggregates if not agg.lenient for r in agg.covered}
    covered_all = {r for agg in aggregates for r in agg.covered}
    report, failed = [], False
    strict_count = 0
    for agg in aggregates:
        covered = covered_all if agg.lenient else covered_strict
        uncovered = sorted(r for r in agg.rules if r not in covered)
        # D-065 第 8 點：v2 的轉移一定要有範例；其餘規則仍然只列出
        for rule_id in [r for r in uncovered if r in agg.transitions]:
            agg.error("model.yaml %s" % rule_id, "這條轉移沒有任何範例涵蓋")
        uncovered = [r for r in uncovered if r not in agg.transitions]
        if uncovered:
            agg.notes.append("沒有任何範例涵蓋的規則（%d）：%s" % (len(uncovered), "、".join(uncovered)))
        if not agg.lenient:
            strict_count += 1
            failed = failed or bool(agg.errors)
        mode = "只列出" if agg.lenient else "擋下"
        report.append("")
        report.append("%s（%s）：規則 %d 條，問題 %d 個" % (agg.label, mode, len(agg.rules), len(agg.errors)))
        report.extend("  [問題] %s" % e for e in agg.errors)
        report.extend("  [列出] %s" % n for n in agg.notes)
    header = ["規格檢查（格式 v1：含 v1.1、v1.2 的檢查；格式 v2.0：spec_version 2 的目錄）",
              "評估的 aggregate：%d 個要擋下、%d 個只列出" % (strict_count, len(aggregates) - strict_count)]
    if not strict_count:
        header.append("注意：%s 底下還沒有任何 aggregate，擋下的檢查沒有評估到任何東西" % specs_dir_rel)
    report.append("")
    report.append("結果：%s" % ("未通過" if failed else "通過"))
    return not failed, header + report


def main(argv=None):
    use_utf8_output()
    parser = argparse.ArgumentParser(description="規格格式檢查")
    parser.add_argument("--repo", default=".")
    parser.add_argument("--specs", default="specs", help="規格目錄（預設 specs）")
    parser.add_argument("--manifest", default=DEFAULT_MANIFEST, help="律法清冊（預設 %s）" % DEFAULT_MANIFEST)
    parser.add_argument("--design-docs", help="design_decision 出處要找的目錄")
    parser.add_argument("--testing-docs", help="example、scenario 出處要找的目錄")
    parser.add_argument("--report-only", nargs="*", default=[], help="只列出問題、不讓檢查失敗的目錄")
    args = parser.parse_args(argv)
    try:
        passed, report = lint(args.repo, args.specs, args.report_only,
                              args.manifest, args.design_docs, args.testing_docs)
    except NotEvaluated as error:
        print("規格檢查：無法評估 — %s" % error)
        return EXIT_NOT_EVALUATED
    print("\n".join(report))
    return EXIT_PASS if passed else EXIT_FAIL


if __name__ == "__main__":
    sys.exit(main())
