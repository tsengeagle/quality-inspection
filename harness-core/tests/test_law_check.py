#!/usr/bin/env python3
"""law_check.py 的測試：每個案例在暫存的 git repo 裡做一次修改，再跑判決。

測試只用自己建立的暫存 repo，不依賴所在專案的任何內容。

執行：python3 -m unittest discover -s harness-core/tests -v
"""
import importlib.util
import json
import os
import subprocess
import sys
import tempfile
import unittest

HERE = os.path.dirname(os.path.abspath(__file__))
spec = importlib.util.spec_from_file_location("law_check", os.path.join(HERE, "..", "law_check.py"))
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


def temporary_directory():
    """Windows 上 .git 裡的唯讀檔會讓清理失敗；清理失敗不該讓測試失敗。"""
    if sys.version_info >= (3, 10):
        return tempfile.TemporaryDirectory(ignore_cleanup_errors=True)
    return tempfile.TemporaryDirectory()


class RepoCase(unittest.TestCase):
    """共用的準備工作：一個暫存的 git repo，base 在 main，修改在 change 分支。"""

    def setUp(self):
        self.tmp = temporary_directory()
        self.repo = self.tmp.name
        self.git("init", "-q", "-b", "main")
        self.git("config", "core.autocrlf", "false")    # Windows 的 git 預設會轉換換行
        self.git("config", "user.email", "test@example.invalid")
        self.git("config", "user.name", "test")
        self.git("config", "commit.gpgsign", "false")
        self.write(BASE_FILES)
        self.base = self.commit("base")
        self.git("checkout", "-q", "-b", "change")

    def tearDown(self):
        self.tmp.cleanup()

    def git(self, *args):
        return subprocess.run(["git", "-C", self.repo, *args], check=True, capture_output=True, text=True,
                              encoding="utf-8", errors="replace").stdout

    def write(self, files):
        for path, content in files.items():
            full = os.path.join(self.repo, path)
            if content is None:
                os.remove(full)
                continue
            os.makedirs(os.path.dirname(full), exist_ok=True)
            with open(full, "w", encoding="utf-8", newline="\n") as f:
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


class LawCheckTest(RepoCase):
    """清冊沒有要求核准簽章時的行為（結構檢查）。"""

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
        self.assertIn("[未通過] 既有的修法紀錄與核准檔沒有被修改或刪除", report)

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
        script = os.path.join(HERE, "..", "law_check.py")
        run = lambda *a: subprocess.run([sys.executable, script, "--repo", self.repo, *a], capture_output=True).returncode
        self.assertEqual(1, run("--base", self.base, "--head", head))
        self.assertEqual(0, run("--base", self.base, "--head", self.base))
        self.assertEqual(2, run("--base", "no-such-commit", "--head", head))

    def test_manifest_location_can_be_set_on_the_command_line(self):
        # 專案可以把清冊放在別的位置；判決程式不寫死專案的路徑
        self.git("checkout", "-q", "main")
        self.git("mv", "law/manifest.txt", "governance.txt")
        self.base = self.commit("base with the manifest elsewhere")
        self.git("checkout", "-q", "-B", "change")
        self.write({"rules/Rule.java": "class Rule { boolean strict = false; }\n"})
        head = self.commit("change")
        script = os.path.join(HERE, "..", "law_check.py")
        run = lambda *a: subprocess.run([sys.executable, script, "--repo", self.repo, "--base", self.base,
                                         "--head", head, *a], capture_output=True).returncode
        self.assertEqual(2, run())                                   # 預設位置找不到清冊：無法評估
        self.assertEqual(1, run("--manifest", "governance.txt"))     # 指定位置後照常判決

    def test_help_and_usage_errors_print_on_a_non_utf8_console(self):
        # Windows 的主控台常見 cp1252 之類的編碼；--help 與參數錯誤都在 argparse 裡印出中文
        env = dict(os.environ, PYTHONIOENCODING="cp1252")
        for name in ("law_check.py", "law_approve.py"):
            script = os.path.join(HERE, "..", name)
            for args, expected in ((["--help"], 0), ([], 2)):
                with self.subTest(script=name, args=args):
                    result = subprocess.run([sys.executable, script, *args], capture_output=True, env=env)
                    self.assertEqual(expected, result.returncode, result.stderr.decode("utf-8", "replace"))
                    self.assertNotIn(b"UnicodeEncodeError", result.stderr)

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


class ApprovalTest(RepoCase):
    """清冊要求核准簽章時的行為。測試用臨時產生的 SSH 金鑰；驗章的路徑和實體金鑰相同。"""

    SIGNED_MANIFEST = MANIFEST + """
@approval-schemes ssh
@approvers-ssh law/allowed_signers
law/allowed_signers    gate
"""

    def setUp(self):
        super().setUp()
        self.keys = temporary_directory()
        self.owner_key = self.new_key("owner")
        self.intruder_key = self.new_key("intruder")
        # base：清冊要求簽章，核准者只有 owner
        self.git("checkout", "-q", "main")
        self.write({"law/manifest.txt": self.SIGNED_MANIFEST,
                    "law/allowed_signers": self.allowed_line("owner", self.owner_key)})
        self.base = self.commit("base that requires signed approval")
        self.git("checkout", "-q", "-B", "change")

    def tearDown(self):
        self.keys.cleanup()
        super().tearDown()

    def new_key(self, name):
        path = os.path.join(self.keys.name, name)
        subprocess.run(["ssh-keygen", "-q", "-t", "ed25519", "-N", "", "-C", name, "-f", path], check=True)
        return path

    def allowed_line(self, principal, key):
        with open(key + ".pub", encoding="utf-8") as f:
            kind, material = f.read().split()[:2]
        return '%s namespaces="law-amendment" %s %s\n' % (principal, kind, material)

    def run_approve(self, script, signer="owner", key=None):
        head = self.git("rev-parse", "HEAD").strip()
        return subprocess.run([sys.executable, script, "--repo", self.repo, "--base", self.base, "--head", head,
                               "--signer", signer, "--key", key or self.owner_key], capture_output=True, text=True,
                              encoding="utf-8", errors="replace")

    def approve(self, signer="owner", key=None):
        """用核准工具（放在被檢查的 repo 之外）對目前的 HEAD 簽章，並把核准檔 commit 進去。"""
        result = self.run_approve(os.path.join(HERE, "..", "law_approve.py"), signer, key)
        self.assertEqual(0, result.returncode, result.stdout + result.stderr)
        out_dir = os.path.join(self.repo, ".git", "law-approvals")
        for name in os.listdir(out_dir):
            os.replace(os.path.join(out_dir, name), os.path.join(self.repo, "law/amendments", name))
        return self.commit("approve")

    def amend(self, files=None):
        path, content = record()
        self.write(dict({"rules/Rule.java": "class Rule { boolean strict = false; }\n", path: content}, **(files or {})))
        return self.commit("amend")

    def verdict(self):
        head = self.git("rev-parse", "HEAD").strip()
        passed, report = check.evaluate(self.repo, self.base, head)
        return passed, "\n".join(report)

    def test_amendment_without_approval_fails_and_shows_the_digest(self):
        self.amend()
        passed, report = self.verdict()
        self.assertFalse(passed, report)
        self.assertIn("[未通過] 有 1 個有效的核准簽章", report)
        self.assertIn("內容雜湊：sha256:", report)

    def test_amendment_approved_by_a_listed_signer_passes(self):
        self.amend()
        self.approve()
        passed, report = self.verdict()
        self.assertTrue(passed, report)
        self.assertIn("核准者：owner（ssh）", report)
        self.assertIn("結果：通過", report)
        self.assertNotIn("沒有評估的項目", report)

    def test_changing_a_law_file_after_approval_invalidates_it(self):
        self.amend()
        self.approve()
        self.write({"rules/Rule.java": "class Rule { boolean strict = false; int sneaked = 1; }\n"})
        self.commit("change after approval")
        passed, report = self.verdict()
        self.assertFalse(passed, report)
        self.assertIn("簽的是另一份內容", report)

    def test_changing_the_record_after_approval_invalidates_it(self):
        self.amend()
        self.approve()
        path, content = record(reason="事後改寫的理由")
        self.write({path: content})
        self.commit("rewrite the reason after approval")
        passed, report = self.verdict()
        self.assertFalse(passed, report)
        self.assertIn("簽的是另一份內容", report)

    def test_adding_another_law_file_after_approval_invalidates_it(self):
        self.amend()
        self.approve()
        self.write({"rules/Extra.java": "class Extra {}\n"})
        self.commit("add a law file after approval")
        passed, report = self.verdict()
        self.assertFalse(passed, report)

    def test_signature_by_an_unlisted_key_fails(self):
        self.amend()
        self.approve(signer="owner", key=self.intruder_key)   # 冒用名稱，但用的是別的金鑰
        passed, report = self.verdict()
        self.assertFalse(passed, report)
        self.assertIn("簽章無效", report)

    def test_change_cannot_add_its_own_key_to_the_approvers(self):
        # 修改本身把入侵者加進核准者清單，再用入侵者的金鑰簽。清單要讀 base 的版本。
        self.amend({"law/allowed_signers": self.allowed_line("owner", self.owner_key)
                                           + self.allowed_line("intruder", self.intruder_key)})
        path, content = record(paths=("rules/", "law/"))
        self.write({path: content})
        self.commit("cover the approvers file in the record")
        self.approve(signer="intruder", key=self.intruder_key)
        passed, report = self.verdict()
        self.assertFalse(passed, report)
        self.assertIn("簽章無效", report)

    def test_tampered_signature_fails(self):
        self.amend()
        self.approve()
        path = os.path.join(self.repo, "law/amendments/2026-01-01-change.owner.approval.json")
        with open(path, encoding="utf-8") as f:
            approval = json.load(f)
        lines = approval["signature"].splitlines()
        lines[2] = lines[2][:10] + ("A" if lines[2][10] != "A" else "B") + lines[2][11:]
        approval["signature"] = "\n".join(lines) + "\n"
        with open(path, "w", encoding="utf-8") as f:
            json.dump(approval, f)
        self.commit("tamper with the signature")
        passed, report = self.verdict()
        self.assertFalse(passed, report)

    def test_approval_copied_from_another_amendment_fails(self):
        self.amend()
        self.approve()
        approval = self.git("show", "HEAD:law/amendments/2026-01-01-change.owner.approval.json")
        # 另起一個修改，沿用同一個紀錄檔名，但改的是別的律法檔案
        self.git("checkout", "-q", "-B", "other", self.base)
        path, content = record()
        self.write({"rules/Other.java": "class Other {}\n", path: content,
                    "law/amendments/2026-01-01-change.owner.approval.json": approval})
        self.commit("reuse an approval")
        passed, report = self.verdict()
        self.assertFalse(passed, report)
        self.assertIn("簽的是另一份內容", report)

    def test_signature_made_for_another_purpose_fails(self):
        # 同一把金鑰、同一段內容，但簽章的用途標記不是 law-amendment
        self.amend()
        head = self.git("rev-parse", "HEAD").strip()
        digest = check.amendment_digest(self.repo, head, "law/amendments/2026-01-01-change.json", ["rules/Rule.java"])
        message = os.path.join(self.keys.name, "message")
        with open(message, "wb") as f:
            f.write(check.signed_message(digest))
        subprocess.run(["ssh-keygen", "-q", "-Y", "sign", "-f", self.owner_key, "-n", "git", message],
                       check=True, capture_output=True)
        with open(message + ".sig", encoding="utf-8") as f:
            signature = f.read()
        self.write({"law/amendments/2026-01-01-change.owner.approval.json": json.dumps(
            {"amendment": "2026-01-01-change", "scheme": "ssh", "signer": "owner", "digest": digest,
             "signature": signature})})
        self.commit("approval with the wrong namespace")
        passed, report = self.verdict()
        self.assertFalse(passed, report)
        self.assertIn("簽章無效", report)

    def test_scheme_not_accepted_by_the_manifest_fails(self):
        self.amend()
        self.approve()
        path = os.path.join(self.repo, "law/amendments/2026-01-01-change.owner.approval.json")
        with open(path, encoding="utf-8") as f:
            approval = json.load(f)
        approval["scheme"] = "x509"
        with open(path, "w", encoding="utf-8") as f:
            json.dump(approval, f)
        self.commit("claim another scheme")
        passed, report = self.verdict()
        self.assertFalse(passed, report)
        self.assertIn("清冊不接受", report)

    def test_approval_for_an_older_amendment_is_a_stray(self):
        self.amend()
        self.approve()
        self.write({"law/amendments/2020-01-01-old.approval.json": "{}"})
        self.commit("stray approval")
        passed, report = self.verdict()
        self.assertFalse(passed, report)
        self.assertIn("[未通過] 核准檔都對應到這次新增的修法紀錄", report)

    def test_build_change_needs_no_approval(self):
        self.write({"src/App.java": "class App { int x; }\n"})
        self.commit("build change")
        passed, report = self.verdict()
        self.assertTrue(passed, report)
        self.assertIn("建構 PR", report)

    def test_shrinking_a_baseline_after_approval_keeps_it_valid(self):
        self.amend()
        self.approve()
        self.write({"baseline.txt": "violation-a\nviolation-c\n"})
        self.commit("shrink a baseline")
        passed, report = self.verdict()
        self.assertTrue(passed, report)

    def test_manifest_naming_an_unsupported_scheme_is_not_a_pass(self):
        self.git("checkout", "-q", "main")
        self.write({"law/manifest.txt": self.SIGNED_MANIFEST.replace("@approval-schemes ssh", "@approval-schemes pgp")})
        self.base = self.commit("base with an unsupported scheme")
        with self.assertRaises(check.NotEvaluated):
            check.evaluate(self.repo, self.base, self.base)

    def install_tool_in_repo(self, tamper=False):
        """把核准工具放進被檢查的 repo（模擬專案安裝了 harness-core），回傳它在 repo 裡的路徑。"""
        files = {}
        for name in ("law_approve.py", "law_check.py"):
            with open(os.path.join(HERE, "..", name), encoding="utf-8") as f:
                files["harness-core/" + name] = f.read() + ("\n# tampered\n" if tamper else "")
        self.write(files)
        return os.path.join(self.repo, "harness-core", "law_approve.py")

    def test_tool_refuses_to_run_from_the_branch_under_review(self):
        # 修法的分支帶著一份被改過的核准工具；核准者若簽出這個分支再執行，工具要拒絕
        script = self.install_tool_in_repo(tamper=True)
        self.amend()
        result = self.run_approve(script)
        self.assertEqual(2, result.returncode, result.stdout + result.stderr)
        self.assertIn("可能來自待核准的修改", result.stderr)
        self.assertFalse(os.path.exists(os.path.join(self.repo, ".git", "law-approvals")))

    def test_tool_runs_from_the_base_checkout_and_reads_the_branch_through_git(self):
        # 正確的做法：工具在 base 上，工作目錄停在 base，用 --head 指定修法的分支
        self.git("checkout", "-q", "main")
        script = self.install_tool_in_repo()
        self.base = self.commit("base with the tool installed")
        self.git("checkout", "-q", "-B", "change")
        self.amend()
        head = self.git("rev-parse", "HEAD").strip()
        self.git("checkout", "-q", "main")
        result = subprocess.run([sys.executable, script, "--repo", self.repo, "--base", self.base, "--head", head,
                                 "--signer", "owner", "--key", self.owner_key], capture_output=True, text=True,
                              encoding="utf-8", errors="replace")
        self.assertEqual(0, result.returncode, result.stdout + result.stderr)
        self.assertIn("核准的 commit：" + head, result.stdout)
        self.git("checkout", "-q", "change")
        out_dir = os.path.join(self.repo, ".git", "law-approvals")
        for name in os.listdir(out_dir):
            os.replace(os.path.join(out_dir, name), os.path.join(self.repo, "law/amendments", name))
        self.commit("approve")
        passed, report = self.verdict()
        self.assertTrue(passed, report)

    def test_untracked_files_next_to_the_tool_do_not_block_it(self):
        # 例如 Python 的快取目錄。它們不被 git 追蹤，也不會被工具載入
        self.git("checkout", "-q", "main")
        script = self.install_tool_in_repo()
        self.base = self.commit("base with the tool installed")
        self.git("checkout", "-q", "-B", "change")
        self.amend()
        head = self.git("rev-parse", "HEAD").strip()
        self.git("checkout", "-q", "main")
        self.write({"harness-core/__pycache__/law_check.cpython-312.pyc": "cache"})
        result = subprocess.run([sys.executable, script, "--repo", self.repo, "--base", self.base, "--head", head,
                                 "--signer", "owner", "--key", self.owner_key], capture_output=True, text=True,
                              encoding="utf-8", errors="replace")
        self.assertEqual(0, result.returncode, result.stdout + result.stderr)

    def test_tool_refuses_when_its_own_files_have_uncommitted_changes(self):
        self.git("checkout", "-q", "main")
        script = self.install_tool_in_repo()
        self.base = self.commit("base with the tool installed")
        self.git("checkout", "-q", "-B", "change")
        self.amend()
        head = self.git("rev-parse", "HEAD").strip()
        self.git("checkout", "-q", "main")
        with open(script, "a", encoding="utf-8") as f:
            f.write("\n# local edit\n")
        result = subprocess.run([sys.executable, script, "--repo", self.repo, "--base", self.base, "--head", head,
                                 "--signer", "owner", "--key", self.owner_key], capture_output=True, text=True,
                              encoding="utf-8", errors="replace")
        self.assertEqual(2, result.returncode, result.stdout + result.stderr)
        self.assertIn("還沒 commit 的修改", result.stderr)

    def test_two_approvals_required(self):
        self.git("checkout", "-q", "main")
        self.write({"law/manifest.txt": self.SIGNED_MANIFEST + "@approvals-required 2\n",
                    "law/allowed_signers": self.allowed_line("owner", self.owner_key)
                                           + self.allowed_line("second", self.intruder_key)})
        self.base = self.commit("base that requires two approvals")
        self.git("checkout", "-q", "-B", "change")
        self.amend()
        self.approve()
        passed, report = self.verdict()
        self.assertFalse(passed, report)
        self.assertIn("有效 1 個", report)
        # 第二位核准者照同樣的流程簽，不能蓋掉第一位的核准檔
        self.approve(signer="second", key=self.intruder_key)
        passed, report = self.verdict()
        self.assertTrue(passed, report)
        self.assertIn("owner（ssh）", report)
        self.assertIn("second（ssh）", report)

    def test_same_signer_approving_twice_counts_once(self):
        self.git("checkout", "-q", "main")
        self.write({"law/manifest.txt": self.SIGNED_MANIFEST + "@approvals-required 2\n"})
        self.base = self.commit("base that requires two approvals")
        self.git("checkout", "-q", "-B", "change")
        self.amend()
        self.approve()
        approval = self.git("show", "HEAD:law/amendments/2026-01-01-change.owner.approval.json")
        self.write({"law/amendments/2026-01-01-change.copy.approval.json": approval})
        self.commit("duplicate the approval under another file name")
        passed, report = self.verdict()
        self.assertFalse(passed, report)
        self.assertIn("有效 1 個", report)

    def test_making_a_law_file_executable_after_approval_invalidates_it(self):
        self.amend()
        self.approve()
        # 直接改 git 索引裡的模式：Windows 的檔案系統沒有可執行位元
        os.chmod(os.path.join(self.repo, "rules/Rule.java"), 0o755)
        self.git("update-index", "--chmod=+x", "rules/Rule.java")
        self.git("commit", "-q", "-m", "flip the executable bit")
        passed, report = self.verdict()
        self.assertFalse(passed, report)
        self.assertIn("簽的是另一份內容", report)

    def test_replacing_a_law_file_with_a_symlink_after_approval_invalidates_it(self):
        # 連結的內容（指向的路徑字串）和原本的檔案內容相同，只有類型不同
        self.git("checkout", "-q", "main")
        self.write({"rules/pointer": "Rule.java"})
        self.base = self.commit("base with a file whose content is a path")
        self.git("checkout", "-q", "-B", "change")
        path, content = record()
        self.write({"rules/pointer": "Other.java", path: content})
        self.commit("amend")
        self.approve()
        # 直接在 git 索引裡把類型改成符號連結，沿用同一個內容物件：
        # Windows 上建立符號連結需要額外權限
        blob_id = self.git("rev-parse", "HEAD:rules/pointer").strip()
        self.git("update-index", "--cacheinfo", "120000,%s,rules/pointer" % blob_id)
        self.git("commit", "-q", "-m", "turn the file into a symlink with the same bytes")
        passed, report = self.verdict()
        self.assertFalse(passed, report)
        self.assertIn("簽的是另一份內容", report)


if __name__ == "__main__":
    unittest.main()
