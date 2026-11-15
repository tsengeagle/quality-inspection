#!/usr/bin/env python3
"""spec_lint.py 的測試：每個案例在暫存目錄裡放一份最小的合格規格，再改壞一處。

執行：python3 -m unittest discover -s harness-core/tests -v
"""
import copy
import importlib.util
import os
import shutil
import sys
import tempfile
import unittest

HERE = os.path.dirname(os.path.abspath(__file__))
spec = importlib.util.spec_from_file_location("spec_lint", os.path.join(HERE, "..", "spec_lint.py"))
lint_module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(lint_module)

try:
    import yaml
except ImportError:      # pragma: no cover
    yaml = None

MODEL = {
    "spec_version": 1, "kind": "aggregate", "name": "Sample",
    "enums": [{"name": "SampleStatus", "values": ["DRAFT", "ACTIVE"]}],
    "transitions": [{"id": "SM-T-01", "from": "DRAFT", "by": "ActivateSample", "to": "ACTIVE",
                     "status": "decided", "source": [{"decision": "D-001"}]}],
    "invariants": [{"id": "SM-INV-01", "text": "名稱不可為空", "status": "decided",
                    "source": [{"doc": "docs/02-ddd/model.md", "section": "§1"}]}],
}
USECASES = {
    "spec_version": 1, "kind": "usecases", "aggregate": "Sample",
    "error_codes": [{"code": "ROLE_DENIED", "category": "denied"},
                    {"code": "INVALID_STATE_TRANSITION", "category": "conflict"}],
    "guards": [{"id": "SM-G-01", "when": "角色不符", "outcome": {"result": "REJECTED", "code": "ROLE_DENIED"},
                "status": "decided", "source": [{"design_decision": "DD-01"}]}],
    "usecases": [
        {"name": "CreateSample", "kind": "command", "guards": ["SM-G-01"],
         "rules": [{"id": "SM-CRT-S1", "when": "成功", "outcome": {"result": "SUCCESS"},
                    "status": "decided", "source": [{"example": "EX-001"}]}]},
        {"name": "ActivateSample", "kind": "command", "guards": ["SM-G-01"],
         "rules": [{"id": "SM-ACT-01", "when": "已啟用", "outcome": {"result": "REJECTED", "code": "INVALID_STATE_TRANSITION"},
                    "status": "decided", "source": [{"scenario": "TS-SM-001"}, {"rule": "SM-T-01"}]}]},
    ],
}
EXAMPLES = {
    "spec_version": 1, "kind": "examples", "aggregate": "Sample",
    "actors": {"m": {"user": "u", "roles": ["MANAGER"], "hospital": "H-01"}},
    "fixtures": {"draft": {"steps": [{"run": "CreateSample", "as": "m", "input": {}, "save": {"s": "sampleId"}}]}},
    "examples": [
        {"name": "creates", "basis": ["SM-CRT-S1", "SM-G-01", "SM-INV-01"],
         "when": {"run": "CreateSample", "as": "m", "input": {}}, "then": {"result": "SUCCESS"}},
        {"name": "activates_twice", "basis": ["SM-ACT-01", "SM-T-01"],
         "given": [{"use": "draft"}, {"run": "ActivateSample", "as": "m", "input": {"id": "${s}"}}],
         "when": {"run": "ActivateSample", "as": "m", "input": {"id": "${s}"}},
         "then": {"result": "REJECTED", "code": "INVALID_STATE_TRANSITION"}},
    ],
}


@unittest.skipIf(yaml is None, "需要 PyYAML")
class SpecLintTest(unittest.TestCase):

    def setUp(self):
        self.tmp = tempfile.mkdtemp()
        self.write("docs/01-requirements/decision-log.md", "# 決策\n\n## D-001 — 第一條\n")
        self.write("docs/02-ddd/model.md", "# 模型\n\n## DD-01 設計決策\n")
        self.write("docs/04-testing/examples.md", "EX-001 與 TS-SM-001\n")
        self.write("specs/forbidden-vocabulary.txt", "# 測試用\n/api/\n\\w+Controller\\b\n")
        self.model, self.usecases, self.examples = map(copy.deepcopy, (MODEL, USECASES, EXAMPLES))

    def tearDown(self):
        shutil.rmtree(self.tmp, ignore_errors=True)

    def write(self, rel, text):
        path = os.path.join(self.tmp, rel)
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "w", encoding="utf-8", newline="\n") as f:
            f.write(text)

    def run_lint(self, folder="specs/sample", report_only=()):
        for name, doc in (("model.yaml", self.model), ("usecases.yaml", self.usecases), ("examples.yaml", self.examples)):
            self.write("%s/%s" % (folder, name), yaml.safe_dump(doc, allow_unicode=True, sort_keys=False))
        passed, report = lint_module.lint(self.tmp, "specs", list(report_only))
        return passed, "\n".join(report)

    def rule(self, rule_id):
        for doc in (self.model, self.usecases):
            stack = [doc]
            while stack:
                node = stack.pop()
                if isinstance(node, dict):
                    if node.get("id") == rule_id:
                        return node
                    stack.extend(node.values())
                elif isinstance(node, list):
                    stack.extend(node)
        raise KeyError(rule_id)

    # --- 合格 ---

    def test_valid_spec_passes(self):
        passed, report = self.run_lint()
        self.assertTrue(passed, report)
        self.assertIn("規則 5 條，問題 0 個", report)

    def test_empty_specs_dir_passes_but_says_nothing_was_evaluated(self):
        os.makedirs(os.path.join(self.tmp, "specs"), exist_ok=True)
        passed, report = lint_module.lint(self.tmp, "specs", [])
        report = "\n".join(report)
        self.assertTrue(passed, report)
        self.assertIn("沒有評估到任何東西", report)

    # --- 每一條格式規則 ---

    def test_wrong_spec_version_fails(self):
        self.model["spec_version"] = "0.1-draft"
        self.assertFalse(self.run_lint()[0])

    def test_duplicate_id_fails(self):
        self.rule("SM-INV-01")["id"] = "SM-T-01"
        passed, report = self.run_lint()
        self.assertFalse(passed, report)
        self.assertIn("id 重複", report)

    def test_malformed_id_fails(self):
        self.rule("SM-INV-01")["id"] = "invariant-1"
        self.assertFalse(self.run_lint()[0])

    def test_decided_rule_without_source_fails(self):
        del self.rule("SM-INV-01")["source"]
        self.assertFalse(self.run_lint()[0])

    def test_free_text_source_fails(self):
        self.rule("SM-INV-01")["source"] = ["domain-model.md §5.1"]
        self.assertFalse(self.run_lint()[0])

    def test_each_kind_of_dangling_source_fails(self):
        for kind, value in (("decision", "D-999"), ("design_decision", "DD-99"), ("doc", "docs/none.md"),
                            ("example", "EX-999"), ("scenario", "TS-XX-999"), ("rule", "SM-NONE-01")):
            with self.subTest(kind=kind):
                self.setUp()
                self.rule("SM-INV-01")["source"] = [{kind: value}]
                passed, report = self.run_lint()
                self.assertFalse(passed, report)
                self.assertIn("source 指向不存在的 %s" % kind, report)

    def test_unknown_status_fails(self):
        self.rule("SM-INV-01")["status"] = "draft"
        self.assertFalse(self.run_lint()[0])

    def test_proposed_rule_needs_an_existing_question(self):
        rule = self.rule("SM-ACT-01")
        rule["status"] = "proposed"
        del rule["source"]
        self.assertFalse(self.run_lint()[0])                       # 沒有 question
        rule["question"] = "Q-01"
        self.assertFalse(self.run_lint()[0])                       # 沒有 OPEN-QUESTIONS.md
        self.write("specs/sample/OPEN-QUESTIONS.md", "# 待決\n\n### Q-01 已經啟用時怎麼辦？\n")
        passed, report = self.run_lint()
        self.assertTrue(passed, report)
        self.assertIn("basis 含 proposed 規則 SM-ACT-01", report)  # 只列出，不擋

    def test_rejected_without_code_or_with_undefined_code_fails(self):
        outcome = self.rule("SM-ACT-01")["outcome"]
        del outcome["code"]
        self.assertFalse(self.run_lint()[0])
        outcome["code"] = "NOT_DEFINED"
        passed, report = self.run_lint()
        self.assertFalse(passed, report)
        self.assertIn("沒有定義在 error_codes", report)

    def test_unknown_result_fails(self):
        self.rule("SM-CRT-S1")["outcome"] = {"result": "SUCCESS_OR_REJECTED"}
        self.assertFalse(self.run_lint()[0])

    def test_error_code_category_must_be_known(self):
        self.usecases["error_codes"][0]["category"] = "403"
        self.assertFalse(self.run_lint()[0])

    def test_usecase_referencing_unknown_guard_fails(self):
        self.usecases["usecases"][0]["guards"] = ["SM-G-99"]
        self.assertFalse(self.run_lint()[0])

    def test_forbidden_vocabulary_fails(self):
        self.rule("SM-INV-01")["text"] = "由 SampleController 檢查"
        passed, report = self.run_lint()
        self.assertFalse(passed, report)
        self.assertIn("出現實作詞彙「SampleController」", report)

    def test_example_basis_must_exist(self):
        self.examples["examples"][0]["basis"].append("SM-NONE-01")
        self.assertFalse(self.run_lint()[0])

    def test_example_without_basis_fails(self):
        self.examples["examples"][0]["basis"] = []
        self.assertFalse(self.run_lint()[0])

    def test_running_an_unspecified_usecase_needs_a_needs_entry(self):
        self.examples["examples"][0]["given"] = [{"run": "BindScoreSheet", "as": "m", "input": {}}]
        self.assertFalse(self.run_lint()[0])
        self.examples["examples"][0]["needs"] = ["BindScoreSheet"]
        self.assertTrue(self.run_lint()[0])

    def test_unknown_fixture_fails(self):
        self.examples["examples"][1]["given"][0] = {"use": "no_such_fixture"}
        self.assertFalse(self.run_lint()[0])

    def test_uncovered_rules_are_listed_not_blocked(self):
        self.examples["examples"].pop(1)
        passed, report = self.run_lint()
        self.assertTrue(passed, report)
        self.assertIn("沒有任何範例涵蓋的規則（2）：SM-ACT-01、SM-T-01", report)

    def test_missing_file_fails(self):
        self.run_lint()
        os.remove(os.path.join(self.tmp, "specs/sample/examples.yaml"))
        passed, report = lint_module.lint(self.tmp, "specs", [])
        self.assertFalse(passed, report)

    def test_ids_inside_example_inputs_are_not_rules(self):
        # 範例的輸入裡有 id 欄位很常見，不能被當成規則
        self.examples["examples"][1]["when"]["input"] = {"id": "${s}"}
        passed, report = self.run_lint()
        self.assertTrue(passed, report)
        self.assertIn("規則 5 條", report)

    # --- 只列出的目錄 ---

    def test_report_only_folder_lists_problems_without_failing(self):
        os.makedirs(os.path.join(self.tmp, "specs"), exist_ok=True)
        del self.rule("SM-INV-01")["source"]
        passed, report = self.run_lint(folder="spec-draft/sample", report_only=["spec-draft"])
        self.assertTrue(passed, report)
        self.assertIn("spec-draft/sample（只列出）", report)
        self.assertIn("[問題]", report)

    def test_duplicate_ids_across_aggregates_fail(self):
        self.run_lint(folder="specs/one")
        passed, report = self.run_lint(folder="specs/two")
        self.assertFalse(passed, report)
        self.assertIn("與另一個 aggregate 重複", report)


if __name__ == "__main__":
    unittest.main()
