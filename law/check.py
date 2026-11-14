#!/usr/bin/env python3
"""law/check.py — 修法檢查（第 1 期：分類與結構檢查，不驗簽章）。

判斷 base 到 head 之間的修改有沒有動到律法；動到的話，是否附了合格的修法紀錄。
判決是確定性的：只用 git 讀取兩個 commit 的內容，不執行被檢查的那一方的任何程式。

用法：
    python3 law/check.py --base <commit> --head <commit>

律法清冊一律讀 base 上的版本。在 CI 上，這支程式本身也必須取自 base。

結束代碼：0 通過；1 未通過；2 無法評估（例如 base 上沒有清冊）。無法評估不算通過。
只使用 Python 標準函式庫，GitHub Actions 與 GitLab CI 都能直接執行。
"""
import argparse
import json
import re
import subprocess
import sys

MANIFEST_PATH = "law/manifest.txt"
CLASSES = {"ruling", "spec", "gate", "tradition", "baseline"}
EXIT_PASS, EXIT_FAIL, EXIT_NOT_EVALUATED = 0, 1, 2


class NotEvaluated(Exception):
    pass


def git(repo, *args, check=True):
    result = subprocess.run(["git", "-C", repo, *args], capture_output=True)
    if check and result.returncode != 0:
        raise NotEvaluated("git %s 失敗：%s" % (" ".join(args), result.stderr.decode("utf-8", "replace").strip()))
    return result


def show(repo, commit, path):
    """檔案在某個 commit 的內容；不存在時回傳 None。"""
    result = git(repo, "show", "%s:%s" % (commit, path), check=False)
    return result.stdout.decode("utf-8", "replace") if result.returncode == 0 else None


def glob_to_regex(pattern):
    if pattern.endswith("/"):
        pattern += "**"
    out, i = [], 0
    while i < len(pattern):
        if pattern.startswith("**/", i):
            out.append("(?:.*/)?")
            i += 3
        elif pattern.startswith("**", i):
            out.append(".*")
            i += 2
        elif pattern[i] == "*":
            out.append("[^/]*")
            i += 1
        else:
            out.append(re.escape(pattern[i]))
            i += 1
    return re.compile("^" + "".join(out) + "$")


class Manifest:
    def __init__(self, text):
        self.rules = []                 # (樣式, regex, 類別)，依出現順序
        self.decision_log = None
        self.amendments_dir = None
        self.allowed_with_amendment = []
        for number, raw in enumerate(text.splitlines(), 1):
            line = raw.strip()
            if not line or line.startswith("#"):
                continue
            parts = line.split()
            if len(parts) != 2:
                raise NotEvaluated("清冊第 %d 行格式錯誤：%s" % (number, raw))
            key, value = parts
            if key == "@decision-log":
                self.decision_log = value
            elif key == "@amendments":
                self.amendments_dir = value if value.endswith("/") else value + "/"
            elif key == "@allowed-with-amendment":
                self.allowed_with_amendment.append(glob_to_regex(value))
            elif key.startswith("@"):
                raise NotEvaluated("清冊第 %d 行有不認得的指示：%s" % (number, key))
            elif value not in CLASSES:
                raise NotEvaluated("清冊第 %d 行有不認得的類別：%s" % (number, value))
            else:
                self.rules.append((key, glob_to_regex(key), value))
        if not self.rules:
            raise NotEvaluated("清冊沒有列出任何律法路徑")
        if not self.amendments_dir:
            raise NotEvaluated("清冊沒有 @amendments 指示")

    def classify(self, path):
        found = None
        for _, regex, law_class in self.rules:
            if regex.match(path):
                found = law_class
        return found

    def allowed_alongside(self, path):
        return any(regex.match(path) for regex in self.allowed_with_amendment)


def changed_files(repo, start, head):
    """[(狀態, 路徑)]，狀態是 A / M / D。更名視為刪除加新增。"""
    out = git(repo, "diff", "--name-status", "--no-renames", "-z", start, head).stdout.decode("utf-8", "replace")
    fields = [f for f in out.split("\0") if f]
    return [(fields[i][0], fields[i + 1]) for i in range(0, len(fields) - 1, 2)]


def line_counts(repo, start, head, path):
    """(新增行數, 刪除行數)；二進位檔回傳 None。"""
    out = git(repo, "diff", "--numstat", "--no-renames", start, head, "--", path).stdout.decode("utf-8", "replace")
    if not out.strip():
        return (0, 0)
    added, deleted = out.split("\t")[:2]
    if added == "-" or deleted == "-":
        return None
    return (int(added), int(deleted))


def evaluate(repo, base, head):
    """回傳 (是否通過, 報告行的清單)。"""
    report = []
    manifest_text = show(repo, base, MANIFEST_PATH)
    if manifest_text is None:
        raise NotEvaluated("base（%s）上沒有 %s" % (base, MANIFEST_PATH))
    manifest = Manifest(manifest_text)
    start = git(repo, "merge-base", base, head).stdout.decode().strip()
    head_sha = git(repo, "rev-parse", head).stdout.decode().strip()

    needs_amendment = []    # (路徑, 類別, 說明)
    exempt = []             # 只縮短的基線
    records = []            # 修法紀錄檔 (狀態, 路徑)
    alongside = []          # 非律法、但允許和修法放在一起
    program = []            # 非律法的其他修改

    for status, path in changed_files(repo, start, head):
        if path.startswith(manifest.amendments_dir) and path.endswith(".json"):
            records.append((status, path))
            continue
        law_class = manifest.classify(path)
        if law_class is None:
            (alongside if manifest.allowed_alongside(path) else program).append(path)
            continue
        counts = line_counts(repo, start, head, path)
        if law_class == "baseline" and status == "M" and counts is not None and counts[0] == 0:
            exempt.append(path)
            continue
        note = ""
        if law_class == "baseline":
            note = "刪除整個檔案" if status == "D" else "新增或改寫了行"
        elif law_class == "ruling" and status != "A" and (counts is None or counts[1] > 0):
            note = "刪除或改寫了既有內容，核准時請特別檢視"
        needs_amendment.append((path, law_class, note))

    report.append("比較範圍：%s..%s（清冊取自 %s）" % (start[:12], head_sha[:12], base[:12]))
    report.append("")
    if exempt:
        report.append("免修法的基線修改（只刪除行）：")
        report.extend("  - %s" % p for p in exempt)
    changed_records = [p for s, p in records if s != "A"]

    if not needs_amendment and not records:
        report.append("分類：建構 PR（沒有需要修法紀錄的律法修改）")
        report.append("結果：通過")
        return True, report

    report.append("分類：修法 PR")
    if needs_amendment:
        report.append("需要修法紀錄的律法修改：")
        for path, law_class, note in needs_amendment:
            report.append("  - [%s] %s%s" % (law_class, path, "　← " + note if note else ""))

    checks = []     # (名稱, 是否成立, 說明)

    added_records = [p for s, p in records if s == "A"]
    checks.append(("既有的修法紀錄沒有被修改或刪除", not changed_records, "、".join(changed_records)))
    one_record = len(added_records) == 1
    checks.append(("有且只有一筆新增的修法紀錄", one_record,
                   "找到 %d 筆：%s" % (len(added_records), "、".join(added_records) or "無")))

    record = None
    if one_record:
        path = added_records[0]
        try:
            record = json.loads(show(repo, head, path) or "")
        except ValueError as error:
            checks.append(("修法紀錄是合法的 JSON", False, str(error)))
        if record is not None and not isinstance(record, dict):
            checks.append(("修法紀錄是 JSON 物件", False, ""))
            record = None

    if record is not None:
        path = added_records[0]
        stem = path[len(manifest.amendments_dir):-len(".json")]
        checks.append(("紀錄的 id 與檔名一致", record.get("id") == stem,
                       "id=%r，檔名=%r" % (record.get("id"), stem)))
        reason = record.get("reason")
        checks.append(("紀錄有理由（reason）", isinstance(reason, str) and bool(reason.strip()), ""))

        paths = record.get("paths")
        paths_ok = isinstance(paths, list) and bool(paths) and all(isinstance(p, str) and p for p in paths)
        checks.append(("紀錄列出涵蓋的路徑（paths）", paths_ok, ""))
        if paths_ok:
            regexes = [glob_to_regex(p) for p in paths]
            outside = [p for p, _, _ in needs_amendment if not any(r.match(p) for r in regexes)]
            checks.append(("改動的律法路徑都在紀錄的範圍內", not outside, "超出範圍：" + "、".join(outside)))

        decision = record.get("decision")
        if decision == "none":
            checks.append(("紀錄指向的裁決存在", True, "decision 為 none"))
        elif not isinstance(decision, str) or not re.match(r"^D-\d+$", decision):
            checks.append(("紀錄指向的裁決存在", False, "decision 必須是 D-<編號> 或 none，實際是 %r" % (decision,)))
        elif not manifest.decision_log:
            checks.append(("紀錄指向的裁決存在", False, "清冊沒有 @decision-log 指示"))
        else:
            log = show(repo, head, manifest.decision_log) or ""
            exists = re.search(r"(?m)^#+\s*%s\b" % re.escape(decision), log) is not None
            checks.append(("紀錄指向的裁決存在", exists, "%s 在 %s" % (decision, manifest.decision_log)))

    checks.append(("修法 PR 沒有夾帶律法與文件以外的修改（P4）", not program, "夾帶：" + "、".join(program)))

    report.append("")
    report.append("檢查項目：")
    for name, ok, detail in checks:
        report.append("  [%s] %s%s" % ("通過" if ok else "未通過", name, "" if ok or not detail else "　— " + detail))
    report.append("")
    report.append("沒有評估的項目（1）：")
    report.append("  - 核准簽章。第 2 期才會檢查；目前修法紀錄不代表有人核准。")
    report.append("")
    passed = all(ok for _, ok, _ in checks)
    report.append("結果：%s" % ("通過（結構檢查）" if passed else "未通過"))
    return passed, report


def main(argv=None):
    parser = argparse.ArgumentParser(description="修法檢查")
    parser.add_argument("--base", required=True, help="合併的目標 commit；清冊讀這個 commit 上的版本")
    parser.add_argument("--head", required=True, help="要檢查的 commit")
    parser.add_argument("--repo", default=".", help="git 工作目錄（預設：目前目錄）")
    args = parser.parse_args(argv)
    try:
        passed, report = evaluate(args.repo, args.base, args.head)
    except NotEvaluated as error:
        print("修法檢查：無法評估 — %s" % error)
        print("無法評估不算通過。")
        return EXIT_NOT_EVALUATED
    print("修法檢查")
    print("\n".join(report))
    return EXIT_PASS if passed else EXIT_FAIL


if __name__ == "__main__":
    sys.exit(main())
