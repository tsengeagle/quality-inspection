#!/usr/bin/env python3
"""law/check.py 的測試：每個案例在暫存的 git repo 裡做一次修改，再跑判決。

執行：python3 -m unittest discover -s law/tests -v
"""
import importlib.util
import json
import os
import subprocess
import tempfile
import unittest

HERE = os.path.dirname(os.path.abspath(__file__))
spec = importlib.util.spec_from_file_location("law_check", os.path.join(HERE, "..", "check.py"))
check = importlib.util.module_from_spec(spec)
spec.loader.exec_module(check)

MANIFEST = """\
@decision-log docs/decision-log.md
@amendments law/amendments/
@allowed-with-amendment docs/**
@allowed-with-amendment **/*.md

docs/decision-log.md   ruling
law/                   gate
rules/                 gate
baseline.txt           baseline
AGENTS.md              tradition
"""

BASE_FILES = {
    "law/manifest.txt": MANIFEST,
    "docs/decision-log.md": "# 決策\n\n## D-001 — 第一條\n\n內容\n",
    "rules/Rule.java": "class Rule { boolean strict = true; }\n",
    "baseline.txt": "violation-a\nviolation-b\nviolation-c\n",
    "AGENTS.md": "不得自行修法\n",
    "src/App.java": "class App {}\n",
    "docs/guide.md": "說明\n",
}


def record(record_id="2026-01-01-change", decision="D-001", paths=("rules/",), reason="調整規則"):
    return ("law/amendments/%s.json" % record_id,
            json.dumps({"id": record_id, "decision": decision, "reason": reason, "paths": list(paths)},
                       ensure_ascii=False, indent=2) + "\n")


class LawCheckTest(unittest.TestCase):

    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.repo = self.tmp.name
        self.git("init", "-q", "-b", "main")
        self.git("config", "user.email", "test@example.invalid")
        self.git("config", "user.name", "test")
        self.git("config", "commit.gpgsign", "false")
        self.write(BASE_FILES)
        self.base = self.commit("base")
        self.git("checkout", "-q", "-b", "change")

    def tearDown(self):
        self.tmp.cleanup()

    def git(self, *args):
        return subprocess.run(["git", "-C", self.repo, *args], check=True, capture_output=True, text=True).stdout

    def write(self, files):
        for path, content in files.items():
            full = os.path.join(self.repo, path)
            if content is None:
                os.remove(full)
                continue
            os.makedirs(os.path.dirname(full), exist_ok=True)
            with open(full, "w", encoding="utf-8") as f:
                f.write(content)

    def commit(self, message):
        self.git("add", "-A")
        self.git("commit", "-q", "-m", message)
        return self.git("rev-parse", "HEAD").strip()

    def judge(self, files):
        """套用修改後跑判決，回傳 (是否通過, 報告全文)。"""
        self.write(files)
        head = self.commit("change")
        passed, report = check.evaluate(self.repo, self.base, head)
        return passed, "\n".join(report)

    # --- 建構 PR ---

    def test_program_change_only_passes_as_build_pr(self):
        passed, report = self.judge({"src/App.java": "class App { int x; }\n"})
        self.assertTrue(passed, report)
        self.assertIn("建構 PR", report)

    def test_baseline_that_only_shrinks_needs_no_amendment(self):
        passed, report = self.judge({"baseline.txt": "violation-a\nviolation-c\n",
                                     "src/App.java": "class App { int x; }\n"})
        self.assertTrue(passed, report)
        self.assertIn("免修法的基線修改", report)

    # --- 需要修法紀錄 ---

    def test_gate_change_without_record_fails(self):
        passed, report = self.judge({"rules/Rule.java": "class Rule { boolean strict = false; }\n"})
        self.assertFalse(passed, report)
        self.assertIn("有且只有一筆新增的修法紀錄", report)

    def test_baseline_with_added_line_fails_without_record(self):
        passed, report = self.judge({"baseline.txt": "violation-a\nviolation-b\nviolation-c\nviolation-new\n"})
        self.assertFalse(passed, report)
        self.assertIn("新增或改寫了行", report)

    def test_baseline_with_rewritten_line_fails_without_record(self):
        passed, report = self.judge({"baseline.txt": "violation-a\nviolation-B-renamed\nviolation-c\n"})
        self.assertFalse(passed, report)

    def test_deleting_a_baseline_file_fails_without_record(self):
        passed, report = self.judge({"baseline.txt": None})
        self.assertFalse(passed, report)
        self.assertIn("刪除整個檔案", report)

    def test_instruction_file_change_fails_without_record(self):
        passed, report = self.judge({"AGENTS.md": "可以自行修法\n"})
        self.assertFalse(passed, report)

    def test_new_file_under_a_law_directory_fails_without_record(self):
        passed, report = self.judge({"rules/NewRule.java": "class NewRule {}\n"})
        self.assertFalse(passed, report)

    # --- 合格的修法 PR ---

    def test_gate_change_with_record_passes(self):
        path, content = record()
        passed, report = self.judge({"rules/Rule.java": "class Rule { boolean strict = false; }\n", path: content})
        self.assertTrue(passed, report)
        self.assertIn("修法 PR", report)
        self.assertIn("核准簽章", report)  # 報告必須列出沒有評估的項目

    def test_amendment_may_add_its_own_decision_and_docs(self):
        path, content = record(decision="D-002", paths=("rules/", "docs/decision-log.md"))
        log = BASE_FILES["docs/decision-log.md"] + "\n## D-002 — 第二條\n\n內容\n"
        passed, report = self.judge({"rules/Rule.java": "class Rule {}\n", "docs/decision-log.md": log,
                                     "docs/guide.md": "更新說明\n", path: content})
        self.assertTrue(passed, report)

    def test_decision_none_is_accepted(self):
        path, content = record(decision="none")
        passed, report = self.judge({"rules/Rule.java": "class Rule {}\n", path: content})
        self.assertTrue(passed, report)

    def test_rewriting_an_existing_ruling_is_flagged(self):
        path, content = record(paths=("docs/decision-log.md",))
        log = BASE_FILES["docs/decision-log.md"].replace("內容", "改寫後的內容")
        passed, report = self.judge({"docs/decision-log.md": log, path: content})
        self.assertTrue(passed, report)
        self.assertIn("刪除或改寫了既有內容", report)

    # --- 不合格的修法紀錄 ---

    def test_record_not_covering_the_changed_path_fails(self):
        path, content = record(paths=("docs/decision-log.md",))
        passed, report = self.judge({"rules/Rule.java": "class Rule {}\n", path: content})
        self.assertFalse(passed, report)
        self.assertIn("超出範圍", report)

    def test_record_pointing_to_unknown_decision_fails(self):
        path, content = record(decision="D-999")
        passed, report = self.judge({"rules/Rule.java": "class Rule {}\n", path: content})
        self.assertFalse(passed, report)

    def test_record_with_malformed_decision_fails(self):
        path, content = record(decision="因為我想")
        passed, report = self.judge({"rules/Rule.java": "class Rule {}\n", path: content})
        self.assertFalse(passed, report)

    def test_record_without_reason_fails(self):
        path, content = record(reason="  ")
        passed, report = self.judge({"rules/Rule.java": "class Rule {}\n", path: content})
        self.assertFalse(passed, report)

    def test_record_id_must_match_file_name(self):
        path, _ = record(record_id="2026-01-01-change")
        _, other = record(record_id="something-else")
        passed, report = self.judge({"rules/Rule.java": "class Rule {}\n", path: other})
        self.assertFalse(passed, report)

    def test_record_that_is_not_json_fails(self):
        passed, report = self.judge({"rules/Rule.java": "class Rule {}\n",
                                     "law/amendments/2026-01-01-change.json": "id: not json\n"})
        self.assertFalse(passed, report)

    def test_record_that_is_json_but_not_an_object_fails(self):
        # null、陣列、字串、數字都是合法的 JSON，但都不是一筆紀錄
        for index, content in enumerate(["null", "[]", '"text"', "42", "true"]):
            with self.subTest(content=content):
                self.git("checkout", "-q", "-B", "change-%d" % index, self.base)
                passed, report = self.judge({"rules/Rule.java": "class Rule { int v = %d; }\n" % index,
                                             "law/amendments/2026-01-01-change.json": content + "\n"})
                self.assertFalse(passed, report)
                self.assertIn("[未通過] 修法紀錄是 JSON 物件", report)

    def test_two_records_in_one_change_fail(self):
        first = record(record_id="2026-01-01-a")
        second = record(record_id="2026-01-01-b")
        passed, report = self.judge({"rules/Rule.java": "class Rule {}\n", first[0]: first[1], second[0]: second[1]})
        self.assertFalse(passed, report)

    def test_modifying_an_existing_record_fails(self):
        path, content = record(record_id="2025-12-31-old")
        self.git("checkout", "-q", "main")
        self.write({path: content})
        self.base = self.commit("base with an old record")
        self.git("checkout", "-q", "-B", "change")
        # 這次修改本身附了合格的新紀錄，唯一的問題是順手改了舊紀錄
        new_path, new_content = record(record_id="2026-01-01-new")
        passed, report = self.judge({"rules/Rule.java": "class Rule {}\n", new_path: new_content,
                                     path: content.replace("調整規則", "事後改寫理由")})
        self.assertFalse(passed, report)
        self.assertIn("[未通過] 既有的修法紀錄沒有被修改或刪除", report)

    # --- P4：修法與建構不混在一起 ---

    def test_amendment_mixed_with_program_change_fails(self):
        path, content = record()
        passed, report = self.judge({"rules/Rule.java": "class Rule {}\n", "src/App.java": "class App { int x; }\n",
                                     path: content})
        self.assertFalse(passed, report)
        self.assertIn("src/App.java", report)

    # --- 判決用的是 base 的清冊 ---

    def test_change_cannot_remove_a_path_from_the_manifest_to_escape(self):
        weakened = MANIFEST.replace("rules/                 gate\n", "")
        path, content = record(paths=("law/",))  # 只宣告改清冊，沒有宣告改規則
        passed, report = self.judge({"law/manifest.txt": weakened,
                                     "rules/Rule.java": "class Rule { boolean strict = false; }\n",
                                     path: content})
        self.assertFalse(passed, report)
        # 依 base 的清冊，rules/ 仍是律法；若誤用 PR 帶來的清冊，它會被當成一般程式
        self.assertIn("[gate] rules/Rule.java", report)

    def test_missing_manifest_on_base_is_not_a_pass(self):
        self.git("checkout", "-q", "main")
        self.write({"law/manifest.txt": None})
        self.base = self.commit("base without manifest")
        with self.assertRaises(check.NotEvaluated):
            check.evaluate(self.repo, self.base, self.base)

    def test_command_line_exit_codes(self):
        self.write({"rules/Rule.java": "class Rule {}\n"})
        head = self.commit("change")
        script = os.path.join(HERE, "..", "check.py")
        run = lambda *a: subprocess.run(["python3", script, "--repo", self.repo, *a], capture_output=True).returncode
        self.assertEqual(1, run("--base", self.base, "--head", head))
        self.assertEqual(0, run("--base", self.base, "--head", self.base))
        self.assertEqual(2, run("--base", "no-such-commit", "--head", head))

    # --- 路徑樣式 ---

    def test_glob_matching(self):
        match = lambda pattern, path: bool(check.glob_to_regex(pattern).match(path))
        self.assertTrue(match("rules/", "rules/a/b/C.java"))
        self.assertFalse(match("rules/", "other/rules/C.java"))
        self.assertTrue(match("**/CLAUDE.md", "a/b/CLAUDE.md"))
        self.assertTrue(match("**/CLAUDE.md", "CLAUDE.md"))
        self.assertTrue(match("*.md", "README.md"))
        self.assertFalse(match("*.md", "docs/README.md"))
        self.assertFalse(match("AGENTS.md", "AGENTS.mdx"))


if __name__ == "__main__":
    unittest.main()
