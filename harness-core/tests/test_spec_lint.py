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
                    "source": [{"doc": "design/model.md", "section": "§1"}]}],
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
        # 路徑刻意不用 quality-inspection 的位置：檢查程式必須從清冊與參數讀，不能寫死
        self.write("law/manifest.txt", "@decision-log records/decisions.md\n@amendments law/amendments/\nlaw/ gate\n")
        self.write("records/decisions.md", "# 決策\n\n## D-001 — 第一條\n")
        self.write("design/model.md", "# 模型\n\n## DD-01 設計決策\n")
        self.write("acceptance/examples.md", "EX-001 與 TS-SM-001\n")
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
        return self.lint_only(report_only)

    def lint_only(self, report_only=(), **options):
        options.setdefault("design_rel", "design")
        options.setdefault("testing_rel", "acceptance")
        passed, report = lint_module.lint(self.tmp, "specs", list(report_only), **options)
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

    def test_observation_steps_are_checked_like_other_steps(self):
        self.examples["examples"][0]["observe"] = [
            {"run": "GetSampleDetial", "as": "m", "input": {}, "expect": {"result": "SUCCESS"}}]
        passed, report = self.run_lint()
        self.assertFalse(passed, report)
        self.assertIn("GetSampleDetial", report)
        self.examples["examples"][0]["needs"] = ["GetSampleDetial"]
        self.assertTrue(self.run_lint()[0])

    def test_unknown_fixture_in_an_observation_step_fails(self):
        self.examples["examples"][0]["observe"] = [{"use": "no_such_fixture"}]
        self.assertFalse(self.run_lint()[0])

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
        passed, report = self.lint_only()
        self.assertFalse(passed, report)

    def test_ids_inside_example_inputs_are_not_rules(self):
        # 範例的輸入裡有 id 欄位很常見，不能被當成規則
        self.examples["examples"][1]["when"]["input"] = {"id": "${s}"}
        passed, report = self.run_lint()
        self.assertTrue(passed, report)
        self.assertIn("規則 5 條", report)

    # --- Copilot 審查（PR #19）補的案例 ---

    def test_decision_log_location_comes_from_the_manifest(self):
        self.write("law/manifest.txt", "@decision-log elsewhere/log.md\n@amendments law/amendments/\nlaw/ gate\n")
        self.write("elsewhere/log.md", "## D-001\n")
        self.assertTrue(self.run_lint()[0])
        os.remove(os.path.join(self.tmp, "elsewhere/log.md"))
        with self.assertRaises(lint_module.NotEvaluated):
            self.run_lint()

    def test_manifest_without_decision_log_is_not_evaluated(self):
        self.write("law/manifest.txt", "@amendments law/amendments/\nlaw/ gate\n")
        with self.assertRaises(lint_module.NotEvaluated):
            self.run_lint()

    def test_unconfigured_document_dirs_fail_the_sources_that_need_them(self):
        self.run_lint()
        passed, report = self.lint_only(design_rel=None, testing_rel=None)
        self.assertFalse(passed, report)
        self.assertIn("沒有設定 --design-docs", report)
        self.assertIn("沒有設定 --testing-docs", report)

    def test_missing_forbidden_vocabulary_is_not_evaluated(self):
        os.remove(os.path.join(self.tmp, "specs/forbidden-vocabulary.txt"))
        with self.assertRaises(lint_module.NotEvaluated):
            self.run_lint()

    def test_non_string_id_fails(self):
        self.usecases["usecases"][0]["rules"].append(
            {"id": 123, "when": "x", "outcome": {"result": "SUCCESS"}, "status": "decided"})
        passed, report = self.run_lint()
        self.assertFalse(passed, report)
        self.assertIn("id 必須是字串", report)

    def test_guard_and_usecase_rule_need_an_outcome(self):
        for target in ("SM-G-01", "SM-ACT-01"):
            self.setUp()
            del self.rule(target)["outcome"]
            passed, report = self.run_lint()
            self.assertFalse(passed, report)
            self.assertIn("%s：前置檢查與 use case 的規則必須有 outcome" % target, report)
        # 不變量與轉移沒有 outcome 是正常的（合格的樣本本來就沒有）
        self.setUp()
        self.assertTrue(self.run_lint()[0])

    def test_malformed_values_are_reported_not_crashed(self):
        cases = [
            lambda: self.examples.__setitem__("fixtures", ["draft"]),
            lambda: self.rule("SM-ACT-01").__setitem__("outcome", {"result": ["SUCCESS", "REJECTED"]}),
            lambda: self.rule("SM-ACT-01").__setitem__("outcome", {"result": "REJECTED", "code": ["A"]}),
            lambda: self.examples["examples"][0].__setitem__("basis", "SM-CRT-S1"),
            lambda: self.examples["examples"][0].__setitem__("basis", [["SM-CRT-S1"]]),
            lambda: self.examples["examples"][1].__setitem__("given", {"use": "draft"}),
            lambda: self.examples["examples"][1]["given"].__setitem__(0, {"use": ["draft"]}),
            lambda: self.examples["examples"][0]["when"].__setitem__("run", ["CreateSample"]),
            lambda: self.examples["examples"][0].__setitem__("needs", [{"x": 1}]),
            lambda: self.usecases.__setitem__("error_codes", 5),
            lambda: self.usecases["error_codes"][0].__setitem__("code", ["ROLE_DENIED"]),
            lambda: self.usecases["usecases"][0].__setitem__("name", ["CreateSample"]),
            lambda: self.usecases["usecases"][0].__setitem__("guards", "SM-G-01"),
            lambda: self.usecases["usecases"][0].__setitem__("guards", [["SM-G-01"]]),
        ]
        for index, mutate in enumerate(cases):
            for folder, report_only, expect_pass in (("specs/sample", (), False),
                                                     ("spec-draft/sample", ["spec-draft"], True)):
                with self.subTest(case=index, folder=folder):
                    self.setUp()
                    os.makedirs(os.path.join(self.tmp, "specs"), exist_ok=True)
                    mutate()
                    passed, report = self.run_lint(folder=folder, report_only=report_only)
                    self.assertEqual(passed, expect_pass, report)
                    self.assertIn("[問題]", report)

    # --- 只列出的目錄 ---

    def test_report_only_folder_lists_problems_without_failing(self):
        os.makedirs(os.path.join(self.tmp, "specs"), exist_ok=True)
        del self.rule("SM-INV-01")["source"]
        passed, report = self.run_lint(folder="spec-draft/sample", report_only=["spec-draft"])
        self.assertTrue(passed, report)
        self.assertIn("spec-draft/sample（只列出）", report)
        self.assertIn("[問題]", report)

    def write_draft_rule(self, rule_id="DR-INV-01", source=None):
        draft = {"spec_version": 1, "kind": "aggregate", "name": "Draft",
                 "invariants": [{"id": rule_id, "text": "草稿規則", "status": "decided",
                                 "source": source or [{"decision": "D-001"}]}]}
        self.write("spec-draft/other/model.yaml", yaml.safe_dump(draft, allow_unicode=True, sort_keys=False))

    def test_strict_spec_cannot_cite_a_draft_only_rule(self):
        self.write_draft_rule()
        self.rule("SM-ACT-01")["source"].append({"rule": "DR-INV-01"})
        passed, report = self.run_lint(report_only=["spec-draft"])
        self.assertFalse(passed, report)
        self.assertIn("source 指向不存在的 rule：DR-INV-01", report)

    def test_strict_example_cannot_use_a_draft_only_rule_as_basis(self):
        self.write_draft_rule()
        self.examples["examples"][0]["basis"].append("DR-INV-01")
        passed, report = self.run_lint(report_only=["spec-draft"])
        self.assertFalse(passed, report)
        self.assertIn("basis 引用了不存在的規則 DR-INV-01", report)

    def test_draft_may_cite_a_strict_rule(self):
        self.write_draft_rule(source=[{"rule": "SM-INV-01"}])
        passed, report = self.run_lint(report_only=["spec-draft"])
        self.assertTrue(passed, report)
        self.assertNotIn("指向不存在的 rule", report)

    def test_proposed_rule_in_another_aggregate_is_noted_and_coverage_counts_across_aggregates(self):
        self.run_lint(folder="specs/one")
        other = copy.deepcopy(MODEL)
        other["transitions"] = [{"id": "OT-T-01", "from": "DRAFT", "by": "ActivateSample", "to": "ACTIVE",
                                 "status": "proposed", "question": "Q-01"}]
        other["invariants"] = []
        self.write("specs/two/OPEN-QUESTIONS.md", "### Q-01\n")
        for name, doc in (("model.yaml", other), ("usecases.yaml", {"spec_version": 1, "kind": "usecases"}),
                          ("examples.yaml", {"spec_version": 1, "kind": "examples"})):
            self.write("specs/two/%s" % name, yaml.safe_dump(doc, allow_unicode=True, sort_keys=False))
        self.examples["examples"][0]["basis"].append("OT-T-01")
        passed, report = self.run_lint(folder="specs/one")
        self.assertTrue(passed, report)
        self.assertIn("basis 含 proposed 規則 OT-T-01", report)
        self.assertNotIn("OT-T-01、", report.split("specs/two")[1].split("\n\n")[0] + "、")
        self.assertNotIn("沒有任何範例涵蓋的規則（1）：OT-T-01", report)

    def test_duplicate_ids_across_aggregates_fail(self):
        self.run_lint(folder="specs/one")
        passed, report = self.run_lint(folder="specs/two")
        self.assertFalse(passed, report)
        self.assertIn("與另一個 aggregate 重複", report)


if __name__ == "__main__":
    unittest.main()
