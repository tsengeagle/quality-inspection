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
            "profile_version 必須是 1": PROFILE.replace("profile_version: 1", "profile_version: 2"),
            "讀不進來": "kind: [\n",
            "最上層必須是對應表": "- a\n",
        }
        for expected, text in cases.items():
            with self.subTest(expected=expected):
                self.write("specs/spec-profile.yaml", text)
                with self.assertRaises(lint_module.NotEvaluated) as caught:
                    self.run_lint()
                self.assertIn(expected, str(caught.exception))


if __name__ == "__main__":
    unittest.main()
