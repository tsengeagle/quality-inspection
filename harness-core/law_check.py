#!/usr/bin/env python3
"""law_check.py — 修法檢查。

判斷 base 到 head 之間的修改有沒有動到律法；動到的話，是否附了合格的修法紀錄，
以及（清冊有要求時）是否有核准者對這次修改的內容簽章。
判決是確定性的：只用 git 讀取兩個 commit 的內容，不執行被檢查的那一方的任何程式。

用法：
    python3 law_check.py --base <commit> --head <commit> [--manifest law/manifest.txt]

律法清冊與核准者清單一律讀 base 上的版本。在 CI 上，這支程式本身也必須取自 base，或取自專案之外。

這支程式屬於 harness 的核心，不含任何專案專屬的設定；專案的設定全部寫在清冊裡。
它刻意維持單一檔案：CI 用 git show 從 base 把它抽出來執行，不能依賴旁邊的其他檔案。

結束代碼：0 通過；1 未通過；2 無法評估（例如 base 上沒有清冊、需要驗章卻沒有 ssh-keygen）。
無法評估不算通過。只使用 Python 標準函式庫；驗證 SSH 簽章時另外需要 OpenSSH 的 ssh-keygen。
"""
import argparse
import hashlib
import json
import os
import re
import subprocess
import sys
import tempfile

DEFAULT_MANIFEST_PATH = "law/manifest.txt"
CLASSES = {"ruling", "spec", "gate", "tradition", "baseline"}
EXIT_PASS, EXIT_FAIL, EXIT_NOT_EVALUATED = 0, 1, 2
APPROVAL_SUFFIX = ".approval.json"
SIGNATURE_NAMESPACE = "law-amendment"       # 簽章的用途標記，避免拿別處的簽章來充當
DIGEST_HEADER = b"law-amendment-v1\n"


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
        self.approval_schemes = []      # 接受哪些簽章方案；空的代表不要求核准簽章
        self.approvers = {}             # 方案 -> 核准者清單在 repo 裡的路徑
        self.approvals_required = 1
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
            elif key == "@approval-schemes":
                self.approval_schemes = [scheme for scheme in value.split(",") if scheme]
            elif key.startswith("@approvers-") and len(key) > len("@approvers-"):
                self.approvers[key[len("@approvers-"):]] = value
            elif key == "@approvals-required":
                if not value.isdigit() or int(value) < 1:
                    raise NotEvaluated("清冊第 %d 行：@approvals-required 必須是正整數" % number)
                self.approvals_required = int(value)
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
        for scheme in self.approval_schemes:
            if scheme not in VERIFIERS:
                raise NotEvaluated("清冊要求的簽章方案 %s 這個版本不支援" % scheme)
            if scheme not in self.approvers:
                raise NotEvaluated("清冊接受 %s 簽章，卻沒有 @approvers-%s 指出核准者清單" % (scheme, scheme))

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


def blob(repo, commit, path):
    """檔案在某個 commit 的原始內容（bytes）；不存在時回傳 None。"""
    result = git(repo, "cat-file", "blob", "%s:%s" % (commit, path), check=False)
    return result.stdout if result.returncode == 0 else None


def amendment_digest(repo, head, record_path, law_paths):
    """這次修法的內容雜湊：修法紀錄本身，加上每個被改動的律法檔案在 head 的內容。

    核准者簽的就是這個值。簽完之後，紀錄或任何一個律法檔案再改一個字，值就不同，簽章失效。
    核准檔本身不算在內，所以加上核准檔不會改變這個值。
    """
    digest = hashlib.sha256(DIGEST_HEADER)
    for path in sorted(set(law_paths) | {record_path}):
        content = blob(repo, head, path)
        state = "deleted" if content is None else hashlib.sha256(content).hexdigest()
        digest.update(("%s\t%s\n" % (path, state)).encode("utf-8"))
    return "sha256:" + digest.hexdigest()


def signed_message(digest):
    return (digest + "\n").encode("ascii")


def verify_ssh(approvers_text, signer, message, signature):
    """用 OpenSSH 驗證簽章。回傳 (是否有效, 說明)。核准者清單是 allowed_signers 格式。"""
    with tempfile.TemporaryDirectory() as tmp:
        allowed = os.path.join(tmp, "allowed_signers")
        sig = os.path.join(tmp, "message.sig")
        with open(allowed, "w", encoding="utf-8") as f:
            f.write(approvers_text)
        with open(sig, "w", encoding="utf-8") as f:
            f.write(signature if signature.endswith("\n") else signature + "\n")
        try:
            result = subprocess.run(
                ["ssh-keygen", "-Y", "verify", "-f", allowed, "-I", signer, "-n", SIGNATURE_NAMESPACE, "-s", sig],
                input=message, capture_output=True, timeout=30)
        except FileNotFoundError:
            raise NotEvaluated("需要驗證 SSH 簽章，但找不到 ssh-keygen")
        except subprocess.TimeoutExpired:
            raise NotEvaluated("ssh-keygen 驗章逾時")
    if result.returncode == 0:
        return True, ""
    detail = (result.stderr or result.stdout).decode("utf-8", "replace").strip().splitlines()
    return False, detail[-1] if detail else "ssh-keygen 回報驗章失敗"


# 簽章方案 -> 驗證函式。新增方案（OpenPGP、X.509）時在這裡登記。
VERIFIERS = {"ssh": verify_ssh}


def check_approvals(repo, base, head, manifest, record_id, digest, approval_paths):
    """回傳 (有效的核准者集合, 無效核准的說明清單)。核准者清單讀 base 上的版本。"""
    valid, problems = set(), []
    message = signed_message(digest)
    for path in approval_paths:
        try:
            approval = json.loads(show(repo, head, path) or "")
        except ValueError:
            approval = None
        if not isinstance(approval, dict):
            problems.append("%s：不是 JSON 物件" % path)
            continue
        scheme, signer, signature = approval.get("scheme"), approval.get("signer"), approval.get("signature")
        if approval.get("amendment") != record_id:
            problems.append("%s：核准的是另一筆修法（%r）" % (path, approval.get("amendment")))
        elif scheme not in manifest.approval_schemes:
            problems.append("%s：清冊不接受 %r 這種簽章" % (path, scheme))
        elif not isinstance(signer, str) or not re.match(r"^[A-Za-z0-9._@+-]+$", signer):
            problems.append("%s：signer 欄位不合格" % path)
        elif not isinstance(signature, str) or not signature.strip():
            problems.append("%s：沒有 signature" % path)
        elif approval.get("digest") != digest:
            problems.append("%s：簽的是另一份內容（核准之後律法或紀錄被改過）" % path)
        else:
            approvers_text = show(repo, base, manifest.approvers[scheme])
            if approvers_text is None:
                raise NotEvaluated("base 上沒有核准者清單 %s" % manifest.approvers[scheme])
            ok, detail = VERIFIERS[scheme](approvers_text, signer, message, signature)
            if ok:
                valid.add((scheme, signer))
            else:
                problems.append("%s：簽章無效（%s）" % (path, detail))
    return valid, problems


def evaluate(repo, base, head, manifest_path=DEFAULT_MANIFEST_PATH):
    """回傳 (是否通過, 報告行的清單)。"""
    report = []
    manifest_text = show(repo, base, manifest_path)
    if manifest_text is None:
        raise NotEvaluated("base（%s）上沒有 %s" % (base, manifest_path))
    manifest = Manifest(manifest_text)
    start = git(repo, "merge-base", base, head).stdout.decode().strip()
    head_sha = git(repo, "rev-parse", head).stdout.decode().strip()

    needs_amendment = []    # (路徑, 類別, 說明)
    exempt = []             # 只縮短的基線
    records = []            # 修法紀錄檔 (狀態, 路徑)
    approvals = []          # 核准檔 (狀態, 路徑)
    alongside = []          # 非律法、但允許和修法放在一起
    program = []            # 非律法的其他修改

    for status, path in changed_files(repo, start, head):
        if path.startswith(manifest.amendments_dir) and path.endswith(APPROVAL_SUFFIX):
            approvals.append((status, path))
            continue
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

    if not needs_amendment and not records and not approvals:
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
    changed_records += [p for st, p in approvals if st != "A"]
    checks.append(("既有的修法紀錄與核准檔沒有被修改或刪除", not changed_records, "、".join(changed_records)))
    one_record = len(added_records) == 1
    checks.append(("有且只有一筆新增的修法紀錄", one_record,
                   "找到 %d 筆：%s" % (len(added_records), "、".join(added_records) or "無")))

    record = None
    if one_record:
        path = added_records[0]
        try:
            parsed = json.loads(show(repo, head, path) or "")
        except ValueError as error:
            checks.append(("修法紀錄是合法的 JSON", False, str(error)))
        else:
            # null、陣列、字串、數字都是合法的 JSON，但都不是一筆紀錄
            if isinstance(parsed, dict):
                record = parsed
            else:
                checks.append(("修法紀錄是 JSON 物件", False, "實際是 %s" % type(parsed).__name__))

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

    # 核准簽章。內容雜湊只在紀錄本身合格時才有意義。
    digest = None
    record_id = record.get("id") if record is not None else None
    if record is not None and isinstance(record_id, str):
        digest = amendment_digest(repo, head_sha, added_records[0], [p for p, _, _ in needs_amendment])
    added_approvals = [p for st, p in approvals if st == "A"]
    prefix = added_records[0][:-len(".json")] if one_record else None
    stray = [p for p in added_approvals if prefix is None or not p.startswith(prefix + ".")]
    checks.append(("核准檔都對應到這次新增的修法紀錄", not stray, "、".join(stray)))

    not_evaluated = []
    if not manifest.approval_schemes:
        not_evaluated.append("核准簽章。清冊沒有設定 @approval-schemes，所以修法紀錄不代表有人核准。")
    elif digest is None:
        checks.append(("有 %d 個有效的核准簽章" % manifest.approvals_required, False, "修法紀錄不合格，無從核對"))
    else:
        mine = [p for p in added_approvals if p not in stray]
        valid, problems = check_approvals(repo, base, head_sha, manifest, record_id, digest, mine)
        detail = "有效 %d 個" % len(valid)
        if problems:
            detail += "；" + "；".join(problems)
        if not mine:
            detail += "；還沒有核准檔。核准者請對下面的內容雜湊簽章"
        checks.append(("有 %d 個有效的核准簽章" % manifest.approvals_required,
                       len(valid) >= manifest.approvals_required, detail))
        if valid:
            report.append("核准者：" + "、".join("%s（%s）" % (signer, scheme) for scheme, signer in sorted(valid)))
    if digest is not None:
        report.append("內容雜湊：%s" % digest)

    report.append("")
    report.append("檢查項目：")
    for name, ok, detail in checks:
        report.append("  [%s] %s%s" % ("通過" if ok else "未通過", name, "" if ok or not detail else "　— " + detail))
    report.append("")
    if not_evaluated:
        report.append("沒有評估的項目（%d）：" % len(not_evaluated))
        report.extend("  - %s" % item for item in not_evaluated)
        report.append("")
    passed = all(ok for _, ok, _ in checks)
    report.append("結果：%s" % ("未通過" if not passed else "通過（結構檢查）" if not_evaluated else "通過"))
    return passed, report


def main(argv=None):
    parser = argparse.ArgumentParser(description="修法檢查")
    parser.add_argument("--base", required=True, help="合併的目標 commit；清冊讀這個 commit 上的版本")
    parser.add_argument("--head", required=True, help="要檢查的 commit")
    parser.add_argument("--repo", default=".", help="git 工作目錄（預設：目前目錄）")
    parser.add_argument("--manifest", default=DEFAULT_MANIFEST_PATH,
                        help="律法清冊在 repo 裡的路徑（預設：%s）" % DEFAULT_MANIFEST_PATH)
    args = parser.parse_args(argv)
    try:
        passed, report = evaluate(args.repo, args.base, args.head, args.manifest)
    except NotEvaluated as error:
        print("修法檢查：無法評估 — %s" % error)
        print("無法評估不算通過。")
        return EXIT_NOT_EVALUATED
    print("修法檢查")
    print("\n".join(report))
    return EXIT_PASS if passed else EXIT_FAIL


if __name__ == "__main__":
    sys.exit(main())
