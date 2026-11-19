#!/usr/bin/env python3
"""spec_lint.py — 規格格式的檢查程式（格式 v1，含 D-063 的 v1.1 檢查；見 specs/README.md）。

逐一檢查 <specs 目錄>/<aggregate>/ 底下的 model.yaml、usecases.yaml、examples.yaml。
判決是確定性的：只讀檔案，不執行任何規格裡的內容。

v1.1（D-063）多了三項：
    封閉結構：每一層只允許欄位表（FILE_SCHEMAS）列出的欄位，型別也要相符；任何一層都可以有 note。
    分辨不了情境的範例：given（展開 fixture 後）與 when 相同、then 卻不同。
    引用完整性：guard_exceptions、transitions 的 by／from／to、terminal_states。

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
KINDS = {"model.yaml": "aggregate", "usecases.yaml": "usecases", "examples.yaml": "examples"}
RESULTS = {"SUCCESS", "REJECTED"}
CATEGORIES = {"denied", "not_found", "invalid_input", "conflict", "rule_violation"}
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
    "actor": _map({"roles": _list("str")}),
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
    "actors": ("named", _map({"user": "str", "roles": _list("str"), "hospital": "str"})),
    "fixtures": ("named", _map({"steps": _list(STEP), "needs": _list("str")})),
    "examples": _list(_map({
        "name": "str", "basis": _list("str"), "derived_from": _list("str"), "needs": _list("str"),
        "given": _list(STEP), "when": STEP, "then": RESULT, "observe": _list(OBSERVE_STEP),
    })),
    "not_covered": _list(_map({"rules": _list("str"), "item": "str", "reason": "str"})),
})
FILE_SCHEMAS = {"model.yaml": MODEL_SCHEMA, "usecases.yaml": USECASES_SCHEMA, "examples.yaml": EXAMPLES_SCHEMA}
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
        for name, kind in KINDS.items():
            full = os.path.join(self.path, name)
            if not os.path.isfile(full):
                self.error(name, "缺少這個檔案")
                continue
            try:
                doc = yaml.safe_load(read(full))
            except yaml.YAMLError as error:
                self.error(name, "YAML 讀不進來：%s" % str(error).splitlines()[0])
                continue
            if not isinstance(doc, dict):
                self.error(name, "最上層必須是對應表")
                continue
            if doc.get("spec_version") != SPEC_VERSION:
                self.error(name, "spec_version 必須是 %d，實際是 %r" % (SPEC_VERSION, doc.get("spec_version")))
            if doc.get("kind") != kind:
                self.error(name, "kind 必須是 %s，實際是 %r" % (kind, doc.get("kind")))
            for where, message in schema_problems(doc, FILE_SCHEMAS[name], ""):
                self.error("%s %s" % (name, where or "最上層"), message)
            self.docs[name] = doc
        questions = os.path.join(self.path, "OPEN-QUESTIONS.md")
        if os.path.isfile(questions):
            self.open_questions = read(questions)

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
            if entry.get("category") not in CATEGORIES:
                self.error("usecases.yaml error_codes %s" % entry["code"],
                           "category 必須是 %s 其中一種" % "、".join(sorted(CATEGORIES)))
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
        for transition in items("transitions"):
            if not isinstance(transition, dict):
                continue
            where = "model.yaml %s" % transition.get("id", "transitions")
            by = transition.get("by")
            if by not in self.usecases:
                self.error(where, "by 必須是同目錄 usecases.yaml 的 use case，實際是 %r" % (by,))
            for key in ("from", "to"):
                if isinstance(transition.get(key), str):
                    states.add(transition[key])
                elif key not in transition:
                    self.error(where, "缺少 %s" % key)
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
    for agg in aggregates:
        known_ids = all_ids if agg.lenient else strict_ids
        try:
            agg.check_usecases()
            agg.check_model_references()
            for rule_id, (name, rule) in sorted(agg.rules.items()):
                agg.check_rule(rule_id, name, rule, known_ids)
            agg.covered = agg.check_examples(known_ids, proposed_all if agg.lenient else proposed_strict)
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
    header = ["規格檢查（格式 v%d，含 v1.1 的檢查）" % SPEC_VERSION,
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
