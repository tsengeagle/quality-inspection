#!/usr/bin/env python3
"""law_approve.py — 核准一次修法：對它的內容雜湊簽章，產生核准檔。

由核准者本人在自己的電腦上執行。簽章用的私鑰不會離開那台電腦（或實體金鑰）。

**一定要從受信任的版本執行，不要簽出修法的分支再執行。** 修法可以改到這支程式；
被改過的版本可以顯示一個雜湊，卻讓金鑰簽另一個。所以做法是：工作目錄停在合併目標（例如 main），
用 --head 指定要核准的分支。程式只用 git 讀那個分支的內容，不執行其中任何檔案。
工作目錄停在修法的分支上時，程式會拒絕執行。

用法（SSH 簽章）：
    git switch main && git pull && git fetch origin
    python3 -I harness-core/law_approve.py --head origin/<修法的分支> --signer <核准者名稱> --key <金鑰把手檔>

流程：
    1. 找出 --head 相對於 --base 的那一筆新增的修法紀錄。
    2. 列出這次修法改了哪些律法檔案，以及內容雜湊。請先看過這些修改再簽。
    3. 呼叫 ssh-keygen 簽章。使用實體金鑰時，這一步要碰一下金鑰。
    4. 把核准檔寫到 .git/law-approvals/，並印出把它放進修法分支的指令。

簽的是 --head 那個 commit 的內容。簽完之後律法檔案或修法紀錄再改，簽章就失效，要重簽。
"""
import argparse
import importlib.util
import json
import os
import re
import subprocess
import sys
import tempfile

# 不在工具目錄留下 Python 的快取檔：這個目錄要保持和 git 裡的內容完全相同
sys.dont_write_bytecode = True

HERE = os.path.dirname(os.path.abspath(__file__))
spec = importlib.util.spec_from_file_location("law_check", os.path.join(HERE, "law_check.py"))
law_check = importlib.util.module_from_spec(spec)
spec.loader.exec_module(law_check)


def find_amendment(repo, base, head, manifest_path):
    """回傳 (修法紀錄的路徑, id, 需要修法的律法路徑, 內容雜湊)。"""
    text = law_check.show(repo, base, manifest_path)
    if text is None:
        raise law_check.NotEvaluated("base（%s）上沒有 %s" % (base, manifest_path))
    manifest = law_check.Manifest(text)
    start = law_check.git(repo, "merge-base", base, head).stdout.decode().strip()
    records, law_paths = [], []
    for status, path in law_check.changed_files(repo, start, head):
        if path.startswith(manifest.amendments_dir) and path.endswith(law_check.APPROVAL_SUFFIX):
            continue
        if path.startswith(manifest.amendments_dir) and path.endswith(".json"):
            if status == "A":
                records.append(path)
            continue
        law_class = manifest.classify(path)
        if law_class is None:
            continue
        counts = law_check.line_counts(repo, start, head, path)
        if law_class == "baseline" and status == "M" and counts is not None and counts[0] == 0:
            continue
        law_paths.append(path)
    if len(records) != 1:
        raise law_check.NotEvaluated("要有且只有一筆新增的修法紀錄，找到 %d 筆" % len(records))
    record_id = records[0][len(manifest.amendments_dir):-len(".json")]
    digest = law_check.amendment_digest(repo, head, records[0], law_paths)
    return records[0], record_id, sorted(law_paths), digest


def refuse_untrusted_copy(repo, base):
    """這支程式若位在被檢查的 repo 裡，工作目錄簽出的必須是 base 已經包含的 commit。

    否則執行中的這份程式可能來自待核准的修改，不能信任它顯示的內容。
    """
    top = law_check.git(repo, "rev-parse", "--show-toplevel").stdout.decode().strip()
    try:
        inside = os.path.commonpath([os.path.realpath(top), os.path.realpath(HERE)]) == os.path.realpath(top)
    except ValueError:
        inside = False      # Windows：兩者在不同的磁碟機
    if not inside:
        return
    # 只看 git 追蹤的檔案有沒有被改過。沒被追蹤的檔案（例如快取）不會被這支程式載入
    if law_check.git(repo, "status", "--porcelain", "--untracked-files=no", "--", HERE).stdout.strip():
        raise law_check.NotEvaluated("核准工具所在的目錄有還沒 commit 的修改，不能信任。請用乾淨的 main 執行")
    trusted = law_check.git(repo, "merge-base", "--is-ancestor", "HEAD", base, check=False).returncode == 0
    if not trusted:
        raise law_check.NotEvaluated(
            "工作目錄簽出的不是 %s 已經包含的版本，這份核准工具可能來自待核准的修改。"
            "請先 git switch main，再用 --head 指定要核准的分支" % base)


def sign_ssh(key, digest):
    with tempfile.TemporaryDirectory() as tmp:
        message = os.path.join(tmp, "message")
        with open(message, "wb") as f:
            f.write(law_check.signed_message(digest))
        result = subprocess.run(["ssh-keygen", "-Y", "sign", "-f", key, "-n", law_check.SIGNATURE_NAMESPACE, message])
        if result.returncode != 0:
            raise law_check.NotEvaluated("ssh-keygen 簽章失敗")
        with open(message + ".sig", encoding="utf-8", newline="") as f:
            return f.read().replace("\r\n", "\n")


def main(argv=None):
    # 要在 argparse 之前：--help 與參數錯誤的訊息是在解析時印出的
    law_check.use_utf8_output()
    parser = argparse.ArgumentParser(description="核准一次修法")
    parser.add_argument("--signer", required=True, help="核准者名稱，要和核准者清單裡的名稱相同")
    parser.add_argument("--key", required=True, help="SSH 私鑰或實體金鑰把手檔的路徑")
    parser.add_argument("--base", default="origin/main", help="合併的目標（預設 origin/main）")
    parser.add_argument("--head", required=True, help="要核准的 commit，例如 origin/<修法的分支>")
    parser.add_argument("--repo", default=".", help="git 工作目錄（預設：目前目錄）")
    parser.add_argument("--manifest", default=law_check.DEFAULT_MANIFEST_PATH)
    args = parser.parse_args(argv)
    if not re.match(r"^[A-Za-z0-9._@+-]+$", args.signer):
        print("無法核准：核准者名稱只能用英數字與 . _ @ + -", file=sys.stderr)
        return 2
    try:
        refuse_untrusted_copy(args.repo, args.base)
        head = law_check.git(args.repo, "rev-parse", "--verify", args.head + "^{commit}").stdout.decode().strip()
        record_path, record_id, law_paths, digest = find_amendment(args.repo, args.base, head, args.manifest)
        print("修法紀錄：%s" % record_path)
        print("這次修法改動的律法檔案（%d）：" % len(law_paths))
        for path in law_paths:
            print("  - %s" % path)
        print("內容雜湊：%s" % digest)
        print("核准的 commit：%s" % head)
        signature = sign_ssh(args.key, digest)
    except law_check.NotEvaluated as error:
        print("無法核准：%s" % error, file=sys.stderr)
        return 2
    # 每位核准者一個檔案，檔名帶核准者名稱；多位核准者依序簽章時不會互相覆蓋。
    # 先寫到 .git 底下：不受切換分支影響，也不會被誤 commit 到別的分支。
    name = "%s.%s%s" % (os.path.basename(record_path)[:-len(".json")], args.signer, law_check.APPROVAL_SUFFIX)
    git_dir = law_check.git(args.repo, "rev-parse", "--absolute-git-dir").stdout.decode().strip()
    out_dir = os.path.join(git_dir, "law-approvals")
    os.makedirs(out_dir, exist_ok=True)
    target = os.path.join(out_dir, name)
    with open(target, "w", encoding="utf-8", newline="\n") as f:
        json.dump({"amendment": record_id, "scheme": "ssh", "signer": args.signer, "digest": digest,
                   "signature": signature}, f, ensure_ascii=False, indent=2)
        f.write("\n")
    destination = os.path.join(os.path.dirname(record_path), name)
    print("已寫入核准檔：%s" % target)
    for line in next_steps(args.head, target, destination, record_id):
        print(line)
    return 0


def next_steps(head, target, destination, record_id):
    """簽完之後要執行的指令，填好實際的分支與檔名，一行一個指令。

    不用 && 串起來：串起來時前一個指令失敗（例如沒有東西可以 commit），後面的會被略過，畫面上卻不明顯。
    """
    branch = head[len("origin/"):] if head.startswith("origin/") else "<修法的分支>"
    return [
        "",
        "接著把核准檔放進修法的分支並推送。請一行一行複製執行，每一行都確認沒有錯誤再執行下一行：",
        "",
        "  git switch %s" % branch,
        "  git pull",
        '  cp "%s" "%s"' % (target, destination),
        '  git add "%s"' % destination,
        '  git commit -m "approve: %s"' % record_id,
        "  git push",
        "  git switch main",
        "",
        "git commit 若顯示 nothing to commit，代表複製的核准檔和分支上的一模一樣，通常是複製錯了檔案。",
    ]


if __name__ == "__main__":
    sys.exit(main())
