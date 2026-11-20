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

# 測試用的專案設定檔：操作者欄位刻意與 quality-inspection 相同，另外的測試再換成別的專案
PROFILE = """kind: spec-profile
profile_version: 1
actor_fields: { user: str, roles: str_list, hospital: str }
usecase_actor_fields: { roles: str_list }
error_categories: [denied, not_found, invalid_input, conflict, rule_violation]
guard_order:
  text: 角色 → 領域規則
  source: [{ decision: D-001 }]
"""
# profile_version 2（D-065 第 5 點）：spec_version 2 的規格需要它
PROFILE_V2 = PROFILE.replace("profile_version: 1", "profile_version: 2") + \
    'clock_start: "2026-11-19T09:00:00Z"\nlocal_time_zone: Asia/Taipei\n'
# 安裝 harness-core 的另一個專案：操作者是程式，錯誤分類也不同（D-064 第 6 點）
DEVICE_PROFILE = """kind: spec-profile
profile_version: 1
actor_fields: { program: str, user: str }
usecase_actor_fields: { program: str }
error_categories: [conflict, device_error]
guard_order: { text: 依呼叫順序 }
"""

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
        self.write("specs/spec-profile.yaml", PROFILE)
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
        # 轉移的 by 要是同目錄的 use case（D-063 第 5 點）
        two_usecases = {"spec_version": 1, "kind": "usecases", "usecases": [{"name": "ActivateSample"}]}
        for name, doc in (("model.yaml", other), ("usecases.yaml", two_usecases),
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

    # --- D-063 第 2 點：封閉結構 ---

    def test_unknown_field_in_then_fails(self):
        # 試寫時的寫法：then 多出來的鍵，v1 的檢查與執行器都不理，讀的人卻會以為驗證過
        self.examples["examples"][1]["then"]["not_called"] = ["sendPin"]
        passed, report = self.run_lint()
        self.assertFalse(passed, report)
        self.assertIn("examples.yaml examples[activates_twice].then：不認得的欄位 not_called", report)

    def test_unknown_field_in_a_step_fails(self):
        self.examples["examples"][1]["given"].append({"clock": {"advance": "15s"}})
        passed, report = self.run_lint()
        self.assertFalse(passed, report)
        self.assertIn("given[2]：不認得的欄位 clock", report)

    def test_unknown_field_in_each_file_fails(self):
        for target, mutate in (
                ("model.yaml", lambda: self.rule("SM-INV-01").__setitem__("severity", "high")),
                ("usecases.yaml", lambda: self.rule("SM-ACT-01")["outcome"].__setitem__("resource", "FAULTED")),
                ("usecases.yaml", lambda: self.usecases["usecases"][0].__setitem__("timeout", "15s")),
                ("examples.yaml", lambda: self.examples.__setitem__("ports", {})),
                ("model.yaml", lambda: self.rule("SM-INV-01")["source"][0].__setitem__("page", 3))):
            with self.subTest(target=target):
                self.setUp()
                mutate()
                passed, report = self.run_lint()
                self.assertFalse(passed, report)
                self.assertIn("[問題] %s" % target, report)
                self.assertIn("不認得的欄位", report)

    def test_note_is_allowed_at_any_level(self):
        self.model["note"] = "最上層"
        self.rule("SM-INV-01")["note"] = "規則"
        self.rule("SM-INV-01")["source"][0]["note"] = "出處"
        self.examples["examples"][0]["note"] = "範例"
        self.examples["examples"][0]["when"]["note"] = "步驟"
        self.examples["examples"][0]["then"]["note"] = "結果"
        passed, report = self.run_lint()
        self.assertTrue(passed, report)

    def test_note_must_be_text(self):
        self.rule("SM-INV-01")["note"] = {"x": 1}
        passed, report = self.run_lint()
        self.assertFalse(passed, report)
        self.assertIn("note：必須是字串", report)

    def test_wrong_types_fail(self):
        for mutate, message in (
                (lambda: self.model.__setitem__("attributes", [{"name": "n", "type": "text", "required": "yes"}]),
                 "required：必須是布林"),
                (lambda: self.usecases["usecases"][0].__setitem__("kind", "event"), "必須是 command、query 其中之一"),
                (lambda: self.examples["examples"][0]["then"].__setitem__("result", "OK"),
                 "必須是 REJECTED、SUCCESS 其中之一"),
                (lambda: self.model.__setitem__("enums", {"SampleStatus": ["DRAFT", "ACTIVE"]}), "enums：必須是清單"),
                (lambda: self.usecases["usecases"][0].__setitem__("input", {"name": "text"}), "input：必須是清單")):
            with self.subTest(message=message):
                self.setUp()
                mutate()
                passed, report = self.run_lint()
                self.assertFalse(passed, report)
                self.assertIn(message, report)

    def test_fields_already_in_use_are_accepted(self):
        # D-063 第 2、3 點：照現有規格收錄的欄位（含標為「待改寫」的四項），v1.1 照樣接受
        self.model.update({
            "term": "樣本", "identity": {"name": "sampleId", "type": "id", "assigned_by": "system"},
            "sources": [{"decision": "D-001"}], "terminal_states": ["ACTIVE"],
            "deferred_attributes": ["x"], "deferred_behaviors": ["y"],
            "attributes": [{"name": "score", "type": "int", "required": False, "initial": 0, "mutable": False,
                            "external": False, "range": [1, 10], "assigned_by": "system"}],
            "entities": [{"name": "Part", "term": "部分", "attributes": [{"name": "n", "type": "int", "initial": []}]}],
        })
        self.rule("SM-INV-01")["enforced_by"] = "另一支 use case"
        usecase = self.usecases["usecases"][0]
        usecase.update({"summary": "建立", "actor": {"roles": ["MANAGER"]}, "input_note": "說明",
                        "input": [{"name": "title", "type": "text", "required": True}],
                        "guard_exceptions": [{"guard": "SM-G-01", "text": "例外", "status": "decided",
                                              "source": [{"decision": "D-001"}]}],
                        "success": {"state": [], "emits": [{"event": "SampleCreated", "status": "decided",
                                                            "source": [{"decision": "D-001"}]}],
                                    "output": [{"name": "sampleId", "type": "id"}]}})
        self.usecases["usecases"].append({"name": "GetSample", "kind": "query", "guards": [],
                                          "output": {"status": "decided", "source": [{"decision": "D-001"}],
                                                     "fields": [{"name": "sampleId", "type": "id"}]}})
        self.usecases["input_types"] = {"ItemInput": [{"name": "code", "type": "text", "required": True}]}
        self.rule("SM-ACT-01")["outcome"]["no_change"] = True
        self.examples["examples"][0]["derived_from"] = ["EX-001"]
        self.examples["not_covered"] = [{"rules": ["SM-INV-01"], "reason": "x"}, {"item": "emits", "reason": "y"}]
        passed, report = self.run_lint()
        self.assertTrue(passed, report)

    def test_present_null_values_fail_except_where_any_value_is_allowed(self):
        # #109 review：寫了但值是 null，會繞過型別檢查
        for mutate, where in (
                (lambda: self.rule("SM-T-01").__setitem__("from", None), "transitions[SM-T-01].from"),
                (lambda: self.usecases["usecases"][0].__setitem__("kind", None), "usecases[CreateSample].kind"),
                (lambda: self.model.__setitem__("attributes", [{"name": "n", "type": "text", "required": None}]),
                 "attributes[n].required"),
                (lambda: self.examples["examples"][1].__setitem__("given", None), "examples[activates_twice].given"),
                (lambda: self.rule("SM-INV-01").__setitem__("note", None), "invariants[SM-INV-01].note")):
            with self.subTest(where=where):
                self.setUp()
                mutate()
                passed, report = self.run_lint()
                self.assertFalse(passed, report)
                self.assertIn("%s：不可以是 null" % where, report)

    def test_null_is_still_allowed_where_any_value_is(self):
        self.model["attributes"] = [{"name": "n", "type": "int?", "initial": None}]
        self.examples["examples"][0]["observe"] = [
            {"run": "CreateSample", "as": "m", "input": {"title": None},
             "expect": {"result": "SUCCESS", "output": {"threshold": None}}}]
        passed, report = self.run_lint()
        self.assertTrue(passed, report)

    def test_yaml_boolean_keys_are_explained(self):
        self.run_lint()
        path = os.path.join(self.tmp, "specs/sample/examples.yaml")
        with open(path, encoding="utf-8") as f:
            text = f.read()
        with open(path, "w", encoding="utf-8") as f:
            f.write(text.replace("  given:\n", "  given:\n  - port: CardModule\n    on: closeModule\n", 1))
        passed, report = self.lint_only()
        self.assertFalse(passed, report)
        self.assertIn("True（YAML 把 on、yes 之類的鍵讀成布林或數字）", report)
        self.assertNotIn("結構：內容的型別不合法", report)

    # --- D-063 第 4 點：分辨不了情境的範例 ---

    def duplicate_of_example(self, index, **changes):
        twin = copy.deepcopy(self.examples["examples"][index])
        twin["name"] = "twin"
        twin["basis"] = ["SM-INV-01"]
        twin.update(changes)
        self.examples["examples"].append(twin)

    def test_same_situation_with_different_then_fails(self):
        self.duplicate_of_example(1, then={"result": "SUCCESS"})
        passed, report = self.run_lint()
        self.assertFalse(passed, report)
        self.assertIn("examples.yaml twin：given、when 與範例 activates_twice 相同，then 卻不同", report)

    def test_fixtures_are_expanded_before_comparing(self):
        # 一個用 fixture、一個把同樣的步驟寫開：情境相同
        steps = copy.deepcopy(self.examples["fixtures"]["draft"]["steps"]) + \
            [copy.deepcopy(self.examples["examples"][1]["given"][1])]
        self.duplicate_of_example(1, given=steps, then={"result": "SUCCESS"})
        passed, report = self.run_lint()
        self.assertFalse(passed, report)
        self.assertIn("then 卻不同", report)

    def test_notes_and_names_do_not_make_situations_different(self):
        self.duplicate_of_example(0, then={"result": "REJECTED", "code": "ROLE_DENIED"},
                                  derived_from=["EX-001"], note="不同的說明")
        self.examples["examples"][-1]["when"]["note"] = "步驟的說明"
        passed, report = self.run_lint()
        self.assertFalse(passed, report)
        self.assertIn("then 卻不同", report)

    def test_different_input_is_a_different_situation(self):
        self.duplicate_of_example(0, then={"result": "REJECTED", "code": "ROLE_DENIED"})
        self.examples["examples"][-1]["when"]["input"] = {"title": "另一個"}
        passed, report = self.run_lint()
        self.assertTrue(passed, report)

    def test_identical_examples_are_listed_not_blocked(self):
        self.duplicate_of_example(0)
        passed, report = self.run_lint()
        self.assertTrue(passed, report)
        self.assertIn("twin：given、when、then、observe 都與範例 creates 相同", report)

    def test_same_then_with_different_observation_is_not_listed(self):
        self.duplicate_of_example(0, observe=[{"run": "CreateSample", "as": "m", "input": {},
                                               "expect": {"result": "SUCCESS"}}])
        passed, report = self.run_lint()
        self.assertTrue(passed, report)
        self.assertNotIn("twin：", report)

    def test_fixture_cycles_do_not_hang(self):
        self.examples["fixtures"]["loop"] = {"steps": [{"use": "loop"}]}
        self.duplicate_of_example(0, given=[{"use": "loop"}], then={"result": "SUCCESS"})
        self.examples["examples"][0]["given"] = [{"use": "loop"}]
        self.examples["examples"][-1]["then"] = {"result": "REJECTED", "code": "ROLE_DENIED"}
        passed, report = self.run_lint()
        self.assertFalse(passed, report)
        self.assertIn("then 卻不同", report)

    # --- D-063 第 5 點：引用完整性 ---

    def test_guard_exception_must_name_one_of_the_usecases_own_guards(self):
        self.usecases["guards"].append({"id": "SM-G-02", "when": "院區不符",
                                        "outcome": {"result": "REJECTED", "code": "ROLE_DENIED"},
                                        "status": "decided", "source": [{"decision": "D-001"}]})
        usecase = self.usecases["usecases"][0]
        usecase["guard_exceptions"] = [{"guard": "SM-G-02", "text": "例外", "status": "decided",
                                        "source": [{"decision": "D-001"}]}]
        passed, report = self.run_lint()
        self.assertFalse(passed, report)
        self.assertIn("guard SM-G-02 不在這支 use case 的 guards 裡", report)
        usecase["guards"].append("SM-G-02")
        passed, report = self.run_lint()
        self.assertTrue(passed, report)

    def test_transition_by_must_be_a_usecase_in_the_same_folder(self):
        self.rule("SM-T-01")["by"] = "PublishSample"
        passed, report = self.run_lint()
        self.assertFalse(passed, report)
        self.assertIn("model.yaml SM-T-01：by 必須是同目錄 usecases.yaml 的 use case", report)

    def test_transition_states_must_belong_to_one_enum(self):
        self.model["enums"].append({"name": "Other", "values": ["ARCHIVED"]})
        self.rule("SM-T-01")["to"] = "ARCHIVED"
        passed, report = self.run_lint()
        self.assertFalse(passed, report)
        self.assertIn("必須都是同一個列舉的值", report)

    def test_terminal_states_must_belong_to_the_state_enum(self):
        self.model["terminal_states"] = ["CLOSED"]
        passed, report = self.run_lint()
        self.assertFalse(passed, report)
        self.assertIn("必須都是同一個列舉的值", report)
        self.model["terminal_states"] = ["ACTIVE"]
        self.assertTrue(self.run_lint()[0])

    def test_states_without_any_enums_fail(self):
        # #109 review：沒寫 enums 時，欄位表不會報，狀態的核對不能就此略過
        del self.model["enums"]
        passed, report = self.run_lint()
        self.assertFalse(passed, report)
        self.assertIn("有轉移或 terminal_states，卻沒有 enums", report)
        self.model["transitions"] = []
        self.rule("SM-ACT-01")["source"] = [{"scenario": "TS-SM-001"}]
        self.examples["examples"][1]["basis"] = ["SM-ACT-01"]
        self.model["terminal_states"] = ["ACTIVE"]
        passed, report = self.run_lint()
        self.assertFalse(passed, report)
        self.assertIn("卻沒有 enums", report)
        self.model["terminal_states"] = []
        passed, report = self.run_lint()
        self.assertTrue(passed, report)            # 沒有狀態要核對時，不需要 enums

    def test_transition_without_from_fails(self):
        del self.rule("SM-T-01")["from"]
        passed, report = self.run_lint()
        self.assertFalse(passed, report)
        self.assertIn("SM-T-01：缺少 from", report)

    # --- D-063 第 6 點：回歸樣本（docs/harness/10-spec-format.md 第五節的試寫）---

    def lint_trial(self, version):
        source = os.path.join(HERE, "fixtures", "spec-format-trial", version)
        shutil.copytree(source, os.path.join(self.tmp, "trial"))
        # 試寫是本專案的草稿，用與本專案相同的設定檔（D-064）
        self.write("trial/spec-profile.yaml", PROFILE)
        passed, report = lint_module.lint(self.tmp, "trial", [])
        return passed, "\n".join(report)

    def test_trial_strict_draft_is_blocked_for_its_hollow_examples(self):
        # v1 判它零問題：成功與被拒絕的範例 given、when 一字不差
        passed, report = self.lint_trial("strict")
        self.assertFalse(passed, report)
        self.assertEqual(report.count("then 卻不同"), 2, report)
        self.assertIn("rejects_when_another_process_holds_the_lock：given、when 與範例 signs_once_and_releases_the_lock 相同", report)
        self.assertIn("mismatched_card_never_sends_pin：given、when 與範例 signs_once_and_releases_the_lock 相同", report)
        self.assertNotIn("結構：內容的型別不合法", report)

    def test_trial_extended_draft_is_blocked_for_each_invented_field(self):
        # 試寫結果二的前 7 列；第 8 列（實作詞彙）不在 v1.1 的範圍
        passed, report = self.lint_trial("ext")
        self.assertFalse(passed, report)
        for expected in (
                "outcome：必須是對應表",                                    # 一條規則兩個結果（v1 已擋）
                "rules[HC-USE-03].outcome：不認得的欄位 resource",            # 第二個面向
                "then：不認得的欄位 calls、not_called、resource",             # 呼叫的順序、未呼叫
                "then：不認得的欄位 resource、call_count",                    # 次數
                "examples[ext_lock_held_by_other_process].then：不認得的欄位 output",   # 拒絕時帶資料
                "given[0]：不認得的欄位 port",                               # 外部的替身
                "given[0]：不認得的欄位 process、holds",                      # 第二個行程
                "given[1]：不認得的欄位 clock"):                             # 固定時鐘
            with self.subTest(expected=expected):
                self.assertIn(expected, report)
        self.assertNotIn("結構：內容的型別不合法", report)


    # --- 專案設定檔（D-064）---

    def use_device_profile(self):
        """iccard 的寫法：操作者是程式，沒有角色與院區。"""
        self.write("specs/spec-profile.yaml", DEVICE_PROFILE)
        self.examples["actors"] = {"m": {"program": "iccard-localserver", "user": "doctor-1"}}
        self.usecases["error_codes"] = [{"code": "ROLE_DENIED", "category": "device_error"},
                                        {"code": "INVALID_STATE_TRANSITION", "category": "conflict"}]

    def test_program_actor_is_rejected_by_this_projects_profile(self):
        self.examples["actors"] = {"m": {"program": "iccard-localserver", "user": "doctor-1"}}
        passed, report = self.run_lint()
        self.assertFalse(passed, report)
        self.assertIn("actors.m：不認得的欄位 program", report)

    def test_another_projects_profile_accepts_its_own_actors_and_categories(self):
        self.use_device_profile()
        self.usecases["usecases"][0]["actor"] = {"program": "iccard-localserver"}
        passed, report = self.run_lint()
        self.assertTrue(passed, report)

    def test_another_projects_profile_rejects_fields_it_does_not_list(self):
        self.use_device_profile()
        self.examples["actors"]["m"]["hospital"] = "H-01"
        self.usecases["usecases"][0]["actor"] = {"roles": ["MANAGER"]}
        self.usecases["error_codes"][1]["category"] = "denied"
        passed, report = self.run_lint()
        self.assertFalse(passed, report)
        self.assertIn("actors.m：不認得的欄位 hospital", report)
        self.assertIn("actor：不認得的欄位 roles", report)
        self.assertIn("category 必須是 conflict、device_error 其中一種", report)

    def test_profile_field_types_are_checked(self):
        self.examples["actors"]["m"]["roles"] = "MANAGER"
        passed, report = self.run_lint()
        self.assertFalse(passed, report)
        self.assertIn("actors.m.roles：必須是清單", report)

    def test_missing_profile_is_not_evaluated(self):
        os.remove(os.path.join(self.tmp, "specs/spec-profile.yaml"))
        with self.assertRaises(lint_module.NotEvaluated) as caught:
            self.run_lint()
        self.assertIn("找不到 spec-profile.yaml", str(caught.exception))

    def test_each_missing_profile_item_is_not_evaluated(self):
        for key in ("actor_fields", "usecase_actor_fields", "error_categories", "guard_order"):
            with self.subTest(key=key):
                doc = yaml.safe_load(PROFILE)
                del doc[key]
                self.write("specs/spec-profile.yaml", yaml.safe_dump(doc, allow_unicode=True))
                with self.assertRaises(lint_module.NotEvaluated) as caught:
                    self.run_lint()
                self.assertIn("缺少 %s" % key, str(caught.exception))

    def test_invalid_profiles_are_not_evaluated(self):
        cases = {
            "不認得的欄位": PROFILE + "extra: 1\n",
            "必須是 str、str_list 其中之一": PROFILE.replace("hospital: str", "hospital: int"),
            "error_categories 不可以是空的": PROFILE.replace("[denied, not_found, invalid_input, conflict, rule_violation]", "[]"),
            "error_categories 有重複的值": PROFILE.replace("[denied, not_found", "[denied, denied"),
            "guard_order 的出處 D-999 不在決策紀錄裡": PROFILE.replace("D-001", "D-999"),
            "guard_order 必須有 text": PROFILE.replace("text: 角色 → 領域規則", "text: ''"),
            "kind 必須是 spec-profile": PROFILE.replace("kind: spec-profile", "kind: profile"),
            "profile_version 必須是 1 或 2": PROFILE.replace("profile_version: 1", "profile_version: 3"),
            # profile_version 2 多兩項必填（D-065 第 5 點）
            "缺少 clock_start；缺少 local_time_zone": PROFILE.replace("profile_version: 1", "profile_version: 2"),
            "clock_start 必須是帶時區的 ISO 8601 時間": PROFILE_V2.replace("09:00:00Z", "09:00:00"),
            "local_time_zone 必須是 UTC 或": PROFILE_V2.replace("Asia/Taipei", "+08:00"),
            # profile_version 1 不認得這兩項：要用就要升版
            "不認得的欄位 clock_start": PROFILE + 'clock_start: "2026-11-19T09:00:00Z"\n',
            "讀不進來": "kind: [\n",
            "最上層必須是對應表": "- a\n",
        }
        for expected, text in cases.items():
            with self.subTest(expected=expected):
                self.write("specs/spec-profile.yaml", text)
                with self.assertRaises(lint_module.NotEvaluated) as caught:
                    self.run_lint()
                self.assertIn(expected, str(caught.exception))

    def test_version_1_specs_pass_under_a_version_2_profile(self):
        self.write("specs/spec-profile.yaml", PROFILE_V2)
        passed, report = self.run_lint()
        self.assertTrue(passed, report)
        self.assertIn("規則 5 條，問題 0 個", report)


@unittest.skipIf(yaml is None, "需要 PyYAML")
class SpecLintV2Test(unittest.TestCase):
    """D-065（格式 v2.0）。回歸樣本是 docs/harness/samples/reviewcase-v2-trial/round-2 的第二輪試寫：
    三個 spec_version 2 的目錄，加上一個 spec_version 1 的 review-project 供跨目錄引用。

    原樣必須零問題；每個案例改壞一處，確認被擋下（D-065 第 15 點）。
    """

    FIXTURE = os.path.join(HERE, "fixtures", "spec-format-v2-trial")

    def setUp(self):
        self.tmp = tempfile.mkdtemp()
        shutil.copytree(self.FIXTURE, os.path.join(self.tmp, "specs"))
        self.write("specs/spec-profile.yaml", PROFILE_V2)
        self.write("law/manifest.txt", "@decision-log records/decisions.md\n@amendments law/amendments/\nlaw/ gate\n")
        # 樣本引用的裁決與文件：只要存在就好，內容不重要
        import re
        text = ""
        for root, _, files in os.walk(os.path.join(self.tmp, "specs")):
            for name in files:
                with open(os.path.join(root, name), encoding="utf-8") as f:
                    text += f.read()
        decisions = sorted(set(re.findall(r"decision: (D-\d+)", text)))
        self.write("records/decisions.md", "# 決策\n\n" + "".join("## %s — 測試用\n\n" % d for d in decisions))
        for doc in sorted(set(re.findall(r"doc: ([\w./-]+\.md)", text))):
            self.write(doc, "# 測試用\n")

    def tearDown(self):
        shutil.rmtree(self.tmp, ignore_errors=True)

    def write(self, rel, text):
        path = os.path.join(self.tmp, rel)
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "w", encoding="utf-8", newline="\n") as f:
            f.write(text)

    def read(self, rel):
        with open(os.path.join(self.tmp, rel), encoding="utf-8") as f:
            return f.read()

    def change(self, rel, old, new, count=1):
        text = self.read(rel)
        self.assertGreaterEqual(text.count(old), 1, "樣本裡找不到要改的文字：%s" % old)
        self.write(rel, text.replace(old, new, count))

    def lint(self, report_only=()):
        passed, report = lint_module.lint(self.tmp, "specs", list(report_only))
        return passed, "\n".join(report)

    def assert_blocked(self, expected):
        passed, report = self.lint()
        self.assertFalse(passed, report)
        self.assertIn(expected, report)

    # --- 合格 ---

    def test_trial_passes_unchanged(self):
        passed, report = self.lint()
        self.assertTrue(passed, report)
        for label, rules in (("review-case", 29), ("review-submission", 2), ("sampling-batch", 6), ("review-project", 5)):
            self.assertIn("specs/%s（擋下）：規則 %d 條，問題 0 個" % (label, rules), report)

    def test_a_folder_may_hold_only_a_model(self):
        # review-submission 只有狀態機；改變它的 use case 在 review-case
        self.assertFalse(os.path.exists(os.path.join(self.tmp, "specs/review-submission/usecases.yaml")))
        passed, report = self.lint()
        self.assertTrue(passed, report)

    def test_version_2_folder_still_needs_a_model(self):
        os.remove(os.path.join(self.tmp, "specs/sampling-batch/model.yaml"))
        self.assert_blocked("model.yaml：缺少這個檔案")

    def test_version_1_folder_still_needs_all_three_files(self):
        os.remove(os.path.join(self.tmp, "specs/review-project/examples.yaml"))
        self.assert_blocked("examples.yaml：缺少這個檔案")

    # --- 第 1 點：版本 ---

    def test_files_in_one_folder_must_share_a_version(self):
        self.change("specs/sampling-batch/usecases.yaml", "spec_version: 2", "spec_version: 1")
        self.assert_blocked("同一個目錄的版本必須相同")

    def test_version_2_spec_needs_a_version_2_profile(self):
        self.write("specs/spec-profile.yaml", PROFILE)
        with self.assertRaises(lint_module.NotEvaluated) as caught:
            self.lint()
        self.assertIn("需要 profile_version 2", str(caught.exception))

    def test_version_2_draft_under_a_version_1_profile_is_reported_not_fatal(self):
        shutil.move(os.path.join(self.tmp, "specs/review-submission"), os.path.join(self.tmp, "draft/review-submission"))
        shutil.move(os.path.join(self.tmp, "specs/review-case"), os.path.join(self.tmp, "draft/review-case"))
        shutil.move(os.path.join(self.tmp, "specs/sampling-batch"), os.path.join(self.tmp, "draft/sampling-batch"))
        self.write("specs/spec-profile.yaml", PROFILE)
        passed, report = self.lint(report_only=["draft"])
        self.assertTrue(passed, report)
        self.assertIn("需要 profile_version 2", report)

    def test_fields_dropped_from_version_2_are_rejected(self):
        # D-063 第 3 點的四項待改寫，在 spec_version 2 不再接受（D-065 第 14 點）
        cases = [
            ("specs/review-case/usecases.yaml", "    summary: 指派初審醫師與期限\n",
             "    summary: 指派初審醫師與期限\n    input_note: 舊寫法\n", "不認得的欄位 input_note"),
            ("specs/review-case/usecases.yaml", "    success:\n      state:\n        - { id: RC-ASG-S1,",
             "    success:\n      output:\n        - { name: caseId, type: id }\n      state:\n        - { id: RC-ASG-S1,",
             "不認得的欄位 output"),
            ("specs/review-case/model.yaml", "    text: SYSTEM_ADMIN 只取得 metadata",
             "    enforced_by: 查詢\n    text: SYSTEM_ADMIN 只取得 metadata", "不認得的欄位 enforced_by"),
            ("specs/review-case/model.yaml", "required: false }      # Q-14", "required: false, range: [1, 2] }  #",
             "不認得的欄位 range"),
        ]
        for rel, old, new, expected in cases:
            with self.subTest(expected=expected):
                original = self.read(rel)
                self.change(rel, old, new)
                self.assert_blocked(expected)
                self.write(rel, original)

    def test_not_covered_rules_must_be_rule_ids(self):
        self.change("specs/review-case/examples.yaml", "  - item: 逾期\n", "  - rules: [emits]\n")
        self.assert_blocked("rules 只能放規則 id，emits 不是")

    # --- 第 2～4 點：斷言與路徑 ---

    def test_rejected_cannot_carry_output(self):
        self.change("specs/review-case/examples.yaml", "then: { result: REJECTED, code: NO_CHANGES }",
                    "then: { result: REJECTED, code: NO_CHANGES, output: { a: 1 } }")
        self.assert_blocked("REJECTED 不能有 output")

    def test_then_code_must_be_defined_where_the_use_case_lives(self):
        self.change("specs/review-case/examples.yaml", "then: { result: REJECTED, code: NO_CHANGES }",
                    "then: { result: REJECTED, code: NO_SUCH_CODE }")
        self.assert_blocked("code NO_SUCH_CODE 沒有定義在 review-case 的 error_codes")

    def test_then_needs_a_result(self):
        self.change("specs/review-case/examples.yaml", "      result: SUCCESS\n      only:", "      only:")
        self.assert_blocked("必須有 result")

    def test_path_syntax_is_checked(self):
        cases = [
            ('save: { c: "content[0].caseId" }', 'save: { c: "content[first].caseId" }', "save 的路徑寫法不對"),
            ('save: { c: "content[0].caseId" }', 'save: { c: "content[*].caseId" }', "save 只能取一個值"),
            ('absent: ["entries[*].totalScore", "entries[*].comment"]', 'absent: ["entries[0].totalScore"]',
             "absent 不能用 [數字]"),
            ('"entries[*]": [submissionId, stage, revisionNo, status, submittedAt]',
             '"entries[*": [submissionId]', "only 的路徑寫法不對"),
            ('"content[caseId=${c}]": { reviewerId: u-r1, assignedAt', '"content[caseId>${c}]": { reviewerId: u-r1, assignedAt',
             "output 的鍵 的路徑寫法不對"),
        ]
        rel = "specs/review-case/examples.yaml"
        for old, new, expected in cases:
            with self.subTest(expected=expected):
                original = self.read(rel)
                self.change(rel, old, new)
                self.assert_blocked(expected)
                self.write(rel, original)

    def test_unknown_named_expectation_fails(self):
        self.change("specs/review-case/examples.yaml", 'then: { use_expect: "${expect}" }', 'then: { use_expect: "${expect}_x" }')
        self.assert_blocked("use_expect 引用了不存在的具名斷言 full_content_x")

    # --- 第 5 點：時間 ---

    def test_times_need_a_zone(self):
        self.change("specs/review-case/examples.yaml", 'dueAt: "2026-11-27T07:59:59+08:00"', 'dueAt: "2026-11-27T07:59:59"')
        self.assert_blocked("時間必須帶時區")

    def test_advance_must_be_a_duration(self):
        self.change("specs/review-case/examples.yaml", "      - advance: PT2H", "      - advance: 2h")
        self.assert_blocked("advance 必須是 ISO 8601 期間")

    def test_example_clock_needs_a_zone(self):
        self.change("specs/review-case/examples.yaml", "  - name: saves_the_draft\n",
                    '  - name: saves_the_draft\n    clock: { start: "2026-11-19T09:00:00" }\n')
        self.assert_blocked("clock.start 必須是帶時區的 ISO 8601 時間")

    # --- 第 6 點：矩陣 ---

    def test_matrix_expands_to_one_named_example_per_row(self):
        self.change("specs/review-case/examples.yaml", "basis: [RC-VIS-01, RC-VIS-03, RC-RVH-01, RC-RVH-02, RC-INV-02]",
                    "basis: [RC-VIS-01, RC-NO-99]")
        self.assert_blocked("examples.yaml revision_history_visibility.admin.INITIAL_REVIEW_COMPLETED：basis 引用了不存在的規則 RC-NO-99")

    def test_matrix_key_must_make_names_unique(self):
        self.change("specs/review-case/examples.yaml", "      key: [actor, state]", "      key: [expect]")
        self.assert_blocked("範例名稱重複")

    def test_matrix_needs_a_key(self):
        self.change("specs/review-case/examples.yaml", "      key: [actor, state]   ", "      # no key")
        self.assert_blocked("matrix.key 必須列出組成名稱的欄")

    def test_matrix_rows_must_match_columns(self):
        self.change("specs/review-case/examples.yaml", "        - [dual_role_h1,  INITIAL_REVIEW_COMPLETED,   denied]",
                    "        - [dual_role_h1,  INITIAL_REVIEW_COMPLETED]")
        self.assert_blocked("matrix 第 10 列的欄數與 columns 不符")

    def test_matrix_key_values_are_restricted(self):
        self.change("specs/review-case/examples.yaml", "        - [dual_role_h1,  INITIAL_REVIEW_COMPLETED,   denied]",
                    '        - ["dual role",   INITIAL_REVIEW_COMPLETED,   denied]')
        self.assert_blocked("只能用英數字與 _ -")

    def test_matrix_column_cannot_share_a_name_with_a_saved_variable(self):
        # 準備步驟把案件的 id 存成 c；欄名也叫 c 的話，${c} 不知道代入哪一個
        rel = "specs/review-case/examples.yaml"
        self.change(rel, "      columns: [actor, state, expect]", "      columns: [c, state, expect]")
        self.change(rel, "      key: [actor, state]", "      key: [c, state]")
        self.assert_blocked("matrix 的欄名與 save 的變數同名：c")

    # --- 第 7 點：事件 ---

    def test_asserted_event_must_be_declared(self):
        self.change("specs/review-case/examples.yaml", "{ event: InitialReviewStarted, with", "{ event: InitialReviewBegun, with")
        self.assert_blocked("事件 InitialReviewBegun 沒有在 StartInitialReview 的 success.emits 宣告")

    def test_asserting_a_proposed_event_is_noted(self):
        passed, report = self.lint()
        self.assertTrue(passed, report)
        self.assertIn("斷言了 proposed 的事件 InitialReviewCompleted", report)

    # --- 第 8 點：轉移 ---

    def test_every_transition_needs_an_example(self):
        self.change("specs/review-case/examples.yaml", "basis: [RC-DRF-S1, RS-T-01]", "basis: [RC-DRF-S1]")
        self.assert_blocked("model.yaml RS-T-01：這條轉移沒有任何範例涵蓋")

    def test_transitions_sharing_from_and_by_need_different_conditions(self):
        rel = "specs/review-submission/model.yaml"
        self.change(rel, "deferred_transitions:",
                    "  - { id: RS-T-03, from: DRAFT, to: SUPERSEDED, by: review-case.SubmitInitialReview, status: decided, "
                    "source: [{ doc: docs/02-ddd/state-machine.md }] }\ndeferred_transitions:")
        self.assert_blocked("與 RS-T-02 的 from、by 相同：要用不同的 when 區分")

    def test_a_condition_tells_two_transitions_apart(self):
        rel = "specs/review-submission/model.yaml"
        self.change(rel, "deferred_transitions:",
                    "  - { id: RS-T-03, from: DRAFT, to: SUPERSEDED, by: review-case.SubmitInitialReview, when: 另一種情況, "
                    "status: decided, source: [{ doc: docs/02-ddd/state-machine.md }] }\ndeferred_transitions:")
        self.change("specs/review-case/examples.yaml", "basis: [RC-DRF-S1, RS-T-01]", "basis: [RC-DRF-S1, RS-T-01, RS-T-03]")
        passed, report = self.lint()
        self.assertTrue(passed, report)

    def test_deferred_transition_states_are_checked(self):
        self.change("specs/review-case/model.yaml",
                    "{ from: PENDING_REREVIEW,            to: REREVIEW_IN_PROGRESS,       by: StartRereview }",
                    "{ from: PENDING_REREVIEW, to: NO_SUCH_STATE, by: StartRereview }")
        self.assert_blocked("必須都是同一個列舉的值")

    def test_deferred_transition_must_move_once_its_use_case_is_specified(self):
        self.change("specs/review-case/model.yaml", "by: StartRereview }", "by: StartInitialReview }")
        self.assert_blocked("StartInitialReview 已經在規格裡：這條轉移要搬到 transitions 並給 id")

    # --- 第 9 點：跨目錄 ---

    def test_transition_may_be_driven_by_a_use_case_in_another_folder(self):
        self.change("specs/review-submission/model.yaml", "by: review-case.SubmitInitialReview,", "by: review-case.SubmitReview,")
        self.assert_blocked("by 必須是規格裡的 use case")

    def test_unknown_fixture_in_another_folder_fails(self):
        self.change("specs/review-case/examples.yaml", "      - use: review-project.active_project  ",
                    "      - use: review-project.live_project  ")
        self.assert_blocked("use 引用了不存在的 fixture review-project.live_project")

    def test_use_case_in_another_folder_must_be_qualified(self):
        self.change("specs/review-case/examples.yaml", "      - run: sampling-batch.ExecuteSampling", "      - run: ExecuteSampling")
        self.assert_blocked("執行的 ExecuteSampling 不在本目錄的 usecases.yaml，也沒有列在 needs")

    def test_strict_spec_cannot_reach_into_a_draft_folder(self):
        shutil.move(os.path.join(self.tmp, "specs/sampling-batch"), os.path.join(self.tmp, "draft/sampling-batch"))
        passed, report = self.lint(report_only=["draft"])
        self.assertFalse(passed, report)
        self.assertIn("執行的 sampling-batch.ExecuteSampling 不在本目錄", report)

    def test_unknown_actor_fails(self):
        self.change("specs/sampling-batch/examples.yaml", "when: { run: ExecuteSampling, as: reviewer_h1,",
                    "when: { run: ExecuteSampling, as: nobody,")
        self.assert_blocked("as 引用了不存在的操作者 nobody")

    # --- 第 11 點：最小的埠 ---

    def test_port_operation_must_be_declared(self):
        self.change("specs/review-case/examples.yaml", "port: sampling-batch.CandidateSource.queryCandidates",
                    "port: sampling-batch.CandidateSource.listAll")
        self.assert_blocked("埠 CandidateSource 沒有操作 listAll")

    def test_port_must_be_declared(self):
        self.change("specs/sampling-batch/examples.yaml", "port: CandidateSource.queryCandidates", "port: Elsewhere.queryCandidates")
        self.assert_blocked("port 引用了不存在的埠 Elsewhere.queryCandidates")

    def test_port_step_needs_returns_and_nothing_from_version_2_1(self):
        rel = "specs/sampling-batch/examples.yaml"
        original = self.read(rel)
        self.change(rel, "      - port: CandidateSource.queryCandidates\n        returns:\n",
                    "      - port: CandidateSource.queryCandidates\n        fails:\n")
        self.assert_blocked("不認得的欄位 fails")
        self.write(rel, original)
        self.change(rel, "      - port: CandidateSource.queryCandidates\n        returns:\n"
                         "          - { sourceCaseKey: MR-001, hospitalId: H-01, medicalRecordType: OUTPATIENT }\n"
                         "          - { sourceCaseKey: MR-002, hospitalId: H-01, medicalRecordType: OUTPATIENT }\n",
                    "      - port: CandidateSource.queryCandidates\n")
        self.assert_blocked("port 必須設定 returns")

    # --- 第 12 點：情境相同的範例 ---

    def test_same_situation_with_different_assertions_is_listed_not_blocked(self):
        passed, report = self.lint()
        self.assertTrue(passed, report)
        self.assertIn("admin_history_is_a_whitelist：given、when 與範例 "
                      "revision_history_visibility.admin.INITIAL_REVIEW_COMPLETED 相同，斷言的內容不同", report)

    def test_same_situation_with_different_result_is_blocked(self):
        self.change("specs/review-case/examples.yaml",
                    "    then:\n      result: SUCCESS\n      only:", "    then:\n      result: REJECTED\n      code: ACCESS_DENIED\n      skipped_only:")
        passed, report = self.lint()
        self.assertFalse(passed, report)
        self.assertIn("相同，result 或 code 卻不同", report)


if __name__ == "__main__":
    unittest.main()
