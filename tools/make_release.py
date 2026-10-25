"""把放进 Releases 文件夹的开发版 jar 整理成正式发布编号（现阶段用 alpha）。

用法：双击 Releases/整理发布版.bat，或在 mod/ 目录运行
    python tools/make_release.py
    python tools/make_release.py --version 0.1.1-alpha   # 用户指定版本号时（2026-10-23 起，新增）
做的事：
  1. 找出 Releases 里还是开发编号（1.0.0-rN）的 jar，按 rN 从小到大排；
  2. 依次编成下一个 alpha 号：0.1.0-alpha.1、0.1.0-alpha.2 ……（接着已有的往后排）；
  3. 改写 jar 里 fabric.mod.json 的 version，另存为
     orients_hotpot_chronicles-1.21.1-fabric-0.1.0-alpha.N.jar，删掉放进来的原文件；
  4. 在 Releases/版本记录.md 里记一条：发布号、对应的开发版 rN、日期、这段时间改了什么（从 DEV_LOG.md 摘）；
  5. docs/移植同步清单.md 的「未发布」一段改名为这个发布版，上面再开一段空的（移植那边按段同步，新增）。
jar 里的其他内容一个字节都不动，所以发布版和当时测的开发版完全一样。
"""
import datetime
import json
import re
import shutil
import sys
import tempfile
import zipfile
from pathlib import Path

MOD = Path(__file__).resolve().parent.parent
REL = MOD / "Releases"
RECORD = REL / "版本记录.md"
STATE = REL / ".releases.json"   # 发布号 ↔ 开发版 rN 的对照，脚本自己维护
BASE = "0.1.0"
STAGE = "alpha"
NAME = "orients_hotpot_chronicles-1.21.1-fabric-{}.jar"
DEV = re.compile(r"^\d+\.\d+\.\d+-r(\d+)$")
HEAD = ("# 版本记录\n\n发布版编号：现阶段是 alpha（`0.1.0-alpha.N`；用户指定时用指定的号，如 `0.1.1-alpha`），按放进来的先后排；"
        "括号里是对应的开发版号，详细原因见 `DEV_LOG.md`。新的在上面。\n\n")


def jar_version(path):
    with zipfile.ZipFile(path) as z:
        return json.loads(z.read("fabric.mod.json").decode("utf-8"))["version"]


def rewrite_version(src, dst, version):
    with zipfile.ZipFile(src) as zin, zipfile.ZipFile(dst, "w") as zout:
        for info in zin.infolist():
            data = zin.read(info.filename)
            if info.filename == "fabric.mod.json":
                meta = json.loads(data.decode("utf-8"))
                meta["version"] = version
                data = (json.dumps(meta, ensure_ascii=False, indent=2) + "\n").encode("utf-8")
            zout.writestr(info, data)


def dev_log_since(prev_r, r):
    """DEV_LOG 里 r(prev_r+1)..r 的条目标题（加粗部分），没有标题就取开头一句。"""
    text = (MOD / "DEV_LOG.md").read_text(encoding="utf-8")
    items = []
    for m in re.finditer(r"^- \*\*r(\d+)([^*]*)\*\*(.*)$", text, re.M):
        n = int(m.group(1))
        if prev_r < n <= r:
            title = m.group(2).strip() or re.split(r"[：。（(]", m.group(3).strip(" ：:"))[0]
            title = re.sub(r"^[/→]\s*r?\d+\s*", "", title)   # 「r45/r46」「r47→r50」这类合并条目
            if title.strip():
                items.append((n, title.strip()))
    return sorted(items)


PORT_LIST = MOD / "docs" / "移植同步清单.md"


def label_port_list(version, r, today):
    """移植同步清单：把「未发布」那一段改名成这个发布版，上面再开一段空的「未发布」（2026-10-23 用户要求，新增）。"""
    if not PORT_LIST.exists():
        return
    text = PORT_LIST.read_text(encoding="utf-8")
    m = re.search(r"^## 未发布.*$", text, re.M)
    if not m:
        return
    head = f"## {version}（开发版 r{r}，{today} 发布）"
    body = re.sub(r"^\n\n> 这一段全部不在 .*?\n", "\n", text[m.end():], count=1)   # 「还没发布」的提示用不上了
    fresh = f"## 未发布（{version} 之后）\n\n（还没有改动）\n\n---\n\n"
    PORT_LIST.write_text(text[:m.start()] + fresh + head + body, encoding="utf-8")
    print(f"移植同步清单：「未发布」已改名为 {version}")


def main(explicit=None):
    """explicit：用户指定的版本号（如 0.1.1-alpha，2026-10-23 用户要求），只给这一次放进来的那个 jar 用。"""
    REL.mkdir(exist_ok=True)
    state = json.loads(STATE.read_text(encoding="utf-8")) if STATE.exists() else {"releases": []}
    dropped = []
    for jar in REL.glob("*.jar"):
        try:
            m = DEV.match(jar_version(jar))
        except (KeyError, zipfile.BadZipFile, json.JSONDecodeError):
            print(f"跳过（不是本模组的 jar）：{jar.name}")
            continue
        if m:
            dropped.append((int(m.group(1)), jar))
    if not dropped:
        print("Releases 里没有新放进来的开发版 jar。")
        return
    for r, jar in sorted(dropped):
        if any(x["dev"] == r for x in state["releases"]):
            print(f"r{r} 已经发布过，删掉重复放进来的 {jar.name}")
            jar.unlink()
            continue
        number = len(state["releases"]) + 1
        version = explicit or f"{BASE}-{STAGE}.{number}"
        if any(x["version"] == version for x in state["releases"]):
            print(f"版本号 {version} 已经用过了，没处理 {jar.name}")
            continue
        out = REL / NAME.format(version)
        with tempfile.TemporaryDirectory() as tmp:
            tmp_jar = Path(tmp) / out.name
            rewrite_version(jar, tmp_jar, version)
            shutil.move(str(tmp_jar), out)
        jar.unlink()
        prev_r = max((x["dev"] for x in state["releases"]), default=0)
        today = datetime.date.today().isoformat()
        state["releases"].append({"version": version, "dev": r, "date": today})
        changes = dev_log_since(prev_r, r)
        lines = [f"## {version}（开发版 r{r}，{today}）", ""]
        lines += [f"- r{n}：{t}" for n, t in changes] or ["- （开发记录里没有找到这段的条目）"]
        old = RECORD.read_text(encoding="utf-8") if RECORD.exists() else HEAD
        body = old[len(HEAD):] if old.startswith(HEAD) else ""
        RECORD.write_text(HEAD + "\n".join(lines) + "\n\n" + body, encoding="utf-8")
        STATE.write_text(json.dumps(state, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        label_port_list(version, r, today)
        print(f"r{r} → {out.name}")


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8")
    # 用法：python tools/make_release.py [--version 0.1.1-alpha]
    args = sys.argv[1:]
    main(args[args.index("--version") + 1] if "--version" in args else None)
