#!/usr/bin/env python3
"""spec_lint.py — 規格格式的檢查程式（格式 v1，見 specs/README.md）。

逐一檢查 <specs 目錄>/<aggregate>/ 底下的 model.yaml、usecases.yaml、examples.yaml。
判決是確定性的：只讀檔案，不執行任何規格裡的內容。

用法：
    python3 spec_lint.py [--repo .] [--specs specs] [--report-only DIR ...]

    --report-only：只列出問題、不讓結束代碼失敗的目錄，例如還在草稿階段的 spec-draft。

結束代碼：0 通過；1 未通過；2 無法評估。無法評估不算通過。
需要 PyYAML。
"""
import argparse
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
DECISION_LOG = "docs/01-requirements/decision-log.md"
DESIGN_DIR = "docs/02-ddd"
TESTING_DIR = "docs/04-testing"
EXIT_PASS, EXIT_FAIL, EXIT_NOT_EVALUATED = 0, 1, 2


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


class Context:
    """跨 aggregate 共用的資料：各種出處的來源文字、所有規則 id、禁用詞彙。"""

    def __init__(self, repo, specs_dir):
        self.repo = repo
        log = os.path.join(repo, DECISION_LOG)
        self.decision_log = read(log) if os.path.isfile(log) else ""
        self.design_text = read_dir_text(os.path.join(repo, DESIGN_DIR))
        self.testing_text = read_dir_text(os.path.join(repo, TESTING_DIR))
        self.forbidden = []
        vocab = os.path.join(specs_dir, "forbidden-vocabulary.txt")
        if os.path.isfile(vocab):
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
        self.usecases = {}
        self.error_codes = set()
        self.guards = set()
        self.open_questions = ""

    def error(self, where, message):
        self.errors.append("%s：%s" % (where, message))

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
            self.docs[name] = doc
        questions = os.path.join(self.path, "OPEN-QUESTIONS.md")
        if os.path.isfile(questions):
            self.open_questions = read(questions)

    # ---------- 規則 ----------

    def collect_rules(self, name, node, trail):
        if isinstance(node, dict):
            rule_id = node.get("id")
            if isinstance(rule_id, str):
                where = "%s %s" % (name, rule_id)
                if not ID_PATTERN.match(rule_id):
                    self.error(where, "id 的格式必須是 <前綴>-<分組>-<序號>，例如 RP-PUB-03")
                if rule_id in self.rules:
                    self.error(where, "id 重複")
                self.rules[rule_id] = (name, node)
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
        if outcome is not None:
            if not isinstance(outcome, dict) or outcome.get("result") not in RESULTS:
                self.error(where, "outcome.result 必須是 SUCCESS 或 REJECTED")
            elif outcome.get("result") == "REJECTED":
                code = outcome.get("code")
                if not code:
                    self.error(where, "REJECTED 必須帶 code")
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
                ok = bool(re.match(r"^DD-\d+$", value)) and heading_exists(self.ctx.design_text, value)
            elif kind == "doc":
                ok = os.path.isfile(os.path.join(self.ctx.repo, value))
            elif kind in ("example", "scenario"):
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
        for entry in doc.get("error_codes") or []:
            if not isinstance(entry, dict) or not entry.get("code"):
                self.error("usecases.yaml error_codes", "每一項必須有 code")
                continue
            if entry.get("category") not in CATEGORIES:
                self.error("usecases.yaml error_codes %s" % entry["code"],
                           "category 必須是 %s 其中一種" % "、".join(sorted(CATEGORIES)))
            self.error_codes.add(entry["code"])
        for guard in doc.get("guards") or []:
            if isinstance(guard, dict) and guard.get("id"):
                self.guards.add(guard["id"])
        for usecase in doc.get("usecases") or []:
            if not isinstance(usecase, dict) or not usecase.get("name"):
                self.error("usecases.yaml", "每支 use case 必須有 name")
                continue
            name = usecase["name"]
            self.usecases[name] = usecase
            for guard in usecase.get("guards") or []:
                if guard not in self.guards:
                    self.error("usecases.yaml %s" % name, "guards 引用了不存在的 %s" % guard)

    # ---------- 範例 ----------

    def check_examples(self, all_ids, proposed):
        doc = self.docs.get("examples.yaml")
        if not doc:
            return set()
        fixtures = doc.get("fixtures") or {}
        covered = set()

        def check_steps(where, steps, needs):
            for step in steps or []:
                if not isinstance(step, dict):
                    continue
                if "use" in step and step["use"] not in fixtures:
                    self.error(where, "use 引用了不存在的 fixture %s" % step["use"])
                run = step.get("run")
                if run and run not in self.usecases and run not in needs:
                    self.error(where, "執行的 %s 不在 usecases.yaml，也沒有列在 needs" % run)

        for fname, fixture in fixtures.items():
            if isinstance(fixture, dict):
                check_steps("examples.yaml fixture %s" % fname, fixture.get("steps"), set(fixture.get("needs") or []))
        for example in doc.get("examples") or []:
            if not isinstance(example, dict):
                continue
            name = example.get("name", "?")
            where = "examples.yaml %s" % name
            needs = set(example.get("needs") or [])
            basis = example.get("basis") or []
            if not basis:
                self.error(where, "沒有 basis")
            for rule_id in basis:
                if rule_id not in all_ids:
                    self.error(where, "basis 引用了不存在的規則 %s" % rule_id)
                covered.add(rule_id)
            pending = [r for r in basis if r in proposed]
            if pending:
                self.notes.append("%s：basis 含 proposed 規則 %s，還不能當驗收依據" % (where, "、".join(pending)))
            steps = list(example.get("given") or [])
            when = example.get("when")
            steps += [when] if isinstance(when, dict) else list(when or [])
            steps += list(example.get("observe") or [])
            check_steps(where, steps, needs)
        return covered

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


def lint(repo, specs_dir_rel, report_only):
    if yaml is None:
        raise NotEvaluated("需要 PyYAML（pip install PyYAML）")
    specs_dir = os.path.join(repo, specs_dir_rel)
    if not os.path.isdir(specs_dir):
        raise NotEvaluated("找不到規格目錄 %s" % specs_dir_rel)
    ctx = Context(repo, specs_dir)
    if not ctx.decision_log:
        raise NotEvaluated("找不到 %s" % DECISION_LOG)
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
    report, failed = [], False
    strict_count = 0
    for agg in aggregates:
        agg.check_usecases()
        proposed = {r for r, (_, rule) in agg.rules.items() if rule.get("status") == "proposed"}
        known_ids = all_ids if agg.lenient else strict_ids
        for rule_id, (name, rule) in sorted(agg.rules.items()):
            agg.check_rule(rule_id, name, rule, known_ids)
        covered = agg.check_examples(known_ids, proposed)
        agg.check_vocabulary()
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
    header = ["規格檢查（格式 v%d）" % SPEC_VERSION,
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
    parser.add_argument("--report-only", nargs="*", default=[], help="只列出問題、不讓檢查失敗的目錄")
    args = parser.parse_args(argv)
    try:
        passed, report = lint(args.repo, args.specs, args.report_only)
    except NotEvaluated as error:
        print("規格檢查：無法評估 — %s" % error)
        return EXIT_NOT_EVALUATED
    print("\n".join(report))
    return EXIT_PASS if passed else EXIT_FAIL


if __name__ == "__main__":
    sys.exit(main())
