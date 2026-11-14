#!/usr/bin/env python3
"""law_approve.py — 核准一次修法：對它的內容雜湊簽章，產生核准檔。

由核准者本人在自己的電腦上執行。簽章用的私鑰不會離開那台電腦（或實體金鑰）。

用法（SSH 簽章）：
    python3 harness-core/law_approve.py --signer <核准者名稱> --key <私鑰或金鑰把手的路徑> [--base origin/main]

流程：
    1. 找出目前分支相對於 base 的那一筆新增的修法紀錄。
    2. 列出這次修法改了哪些律法檔案，以及內容雜湊。請先看過這些修改再簽。
    3. 呼叫 ssh-keygen 簽章。使用實體金鑰時，這一步要碰一下金鑰。
    4. 把核准檔寫進修法紀錄的目錄，檔名是 <修法紀錄的 id>.<核准者名稱>.approval.json。
       接著由核准者 commit 並推送。

簽的是「已經 commit 的內容」。簽完之後律法檔案或修法紀錄再改，簽章就失效，要重簽。
"""
import argparse
import importlib.util
import json
import os
import re
import subprocess
import sys
import tempfile

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


def sign_ssh(key, digest):
    with tempfile.TemporaryDirectory() as tmp:
        message = os.path.join(tmp, "message")
        with open(message, "wb") as f:
            f.write(law_check.signed_message(digest))
        result = subprocess.run(["ssh-keygen", "-Y", "sign", "-f", key, "-n", law_check.SIGNATURE_NAMESPACE, message])
        if result.returncode != 0:
            raise law_check.NotEvaluated("ssh-keygen 簽章失敗")
        with open(message + ".sig", encoding="utf-8") as f:
            return f.read()


def main(argv=None):
    parser = argparse.ArgumentParser(description="核准一次修法")
    parser.add_argument("--signer", required=True, help="核准者名稱，要和核准者清單裡的名稱相同")
    parser.add_argument("--key", required=True, help="SSH 私鑰或實體金鑰把手檔的路徑")
    parser.add_argument("--base", default="origin/main", help="合併的目標（預設 origin/main）")
    parser.add_argument("--head", default="HEAD", help="要核准的 commit（預設 HEAD）")
    parser.add_argument("--repo", default=".", help="git 工作目錄（預設：目前目錄）")
    parser.add_argument("--manifest", default=law_check.DEFAULT_MANIFEST_PATH)
    args = parser.parse_args(argv)
    if not re.match(r"^[A-Za-z0-9._@+-]+$", args.signer):
        print("無法核准：核准者名稱只能用英數字與 . _ @ + -", file=sys.stderr)
        return 2
    try:
        dirty = law_check.git(args.repo, "status", "--porcelain").stdout.decode().strip()
        record_path, record_id, law_paths, digest = find_amendment(args.repo, args.base, args.head, args.manifest)
        print("修法紀錄：%s" % record_path)
        print("這次修法改動的律法檔案（%d）：" % len(law_paths))
        for path in law_paths:
            print("  - %s" % path)
        print("內容雜湊：%s" % digest)
        if dirty:
            print("注意：工作目錄有還沒 commit 的修改。簽的是已經 commit 的內容，不包含它們。")
        signature = sign_ssh(args.key, digest)
    except law_check.NotEvaluated as error:
        print("無法核准：%s" % error, file=sys.stderr)
        return 2
    # 每位核准者一個檔案，檔名帶核准者名稱；多位核准者依序簽章時不會互相覆蓋
    target = os.path.join(args.repo, "%s.%s%s" % (record_path[:-len(".json")], args.signer, law_check.APPROVAL_SUFFIX))
    with open(target, "w", encoding="utf-8") as f:
        json.dump({"amendment": record_id, "scheme": "ssh", "signer": args.signer, "digest": digest,
                   "signature": signature}, f, ensure_ascii=False, indent=2)
        f.write("\n")
    print("已寫入核准檔：%s" % target)
    print("接著請 commit 並推送這個檔案。")
    return 0


if __name__ == "__main__":
    sys.exit(main())
