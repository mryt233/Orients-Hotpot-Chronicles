"""检查代码分层：哪些类和加载器绑定、哪些是纯逻辑。

背景：以后要做 common / fabric / forge / neoforge 分层，common 层不能 import 任何加载器 API。
这个脚本把现状量化出来，方便一点点往目标挪，也防止新代码往纯逻辑类里塞 Fabric 调用。

用法（工作区根目录）：
    python mod/tools/check_layering.py            # 只报告
    python mod/tools/check_layering.py --fail     # 纯逻辑类里出现加载器 API 时返回非 0（可挂 CI）

判定：
  - 纯逻辑类：hotpot 包下不带 @ExpectPlatform 语义、也不 import net.fabricmc 的类
  - 加载器绑定：import 了 net.fabricmc.* 的类
  - 入口 / 客户端 / 兼容：GeneratedMod、client 包、compat 包，天然依赖加载器，不计入

作者：mryt233
"""
from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
PKG = "cn/blockforge/generated/orientshotpotchronicles"
SRC_ROOTS = [ROOT / "common/src/main/java" / PKG, ROOT / "fabric/src/main/java" / PKG]
SHARED_DIR = "hotpot"

# 这些目录天然依赖加载器，不做纯逻辑要求
PLATFORM_DIRS = {"client", "compat", "mixin", "devtest", "platform"}
# 允许依赖加载器的额外文件（入口）
PLATFORM_FILES = {"GeneratedMod.java"}

LOADER_IMPORT = re.compile(r"^\s*import\s+(net\.fabricmc\.|net\.neoforged\.|net\.minecraftforge\.)", re.M)
# 同文件里直接写全限定名的也算
LOADER_USAGE = re.compile(r"\b(net\.fabricmc\.|net\.neoforged\.|net\.minecraftforge\.)")


def classify(path: Path) -> tuple[str, bool]:
    text = path.read_text(encoding="utf-8", errors="replace")
    depends = bool(LOADER_IMPORT.search(text)) or bool(LOADER_USAGE.search(text))
    return ("platform" if depends else "core", depends)


def main() -> int:
    fail = "--fail" in sys.argv
    core, platform = [], []
    entries = []
    for root in SRC_ROOTS:
        if root.exists():
            entries += [(f, root) for f in sorted(root.rglob("*.java"))]
    for path, root in entries:
        rel = path.relative_to(root)
        parts = rel.parts
        if parts and parts[0] in PLATFORM_DIRS:
            platform.append((rel, "目录天然依赖加载器"))
            continue
        if rel.name in PLATFORM_FILES:
            platform.append((rel, "模组入口"))
            continue
        kind, depends = classify(path)
        if kind == "core":
            core.append(rel)
        else:
            platform.append((rel, "import 了加载器 API"))

    print("源：" + " + ".join(str(r) for r in SRC_ROOTS if r.exists()))
    print(f"纯逻辑 {len(core)} 个，依赖加载器 {len(platform)} 个\n")
    print("== 纯逻辑（可以进 common 层）==")
    for rel in core:
        print(f"  {rel}")
    print("\n== 依赖加载器（要放平台层，或被抽象掉）==")
    for rel, why in platform:
        print(f"  {rel}    ({why})")

    if fail and platform:
        # 只有 hotpot 包里的“本该是纯逻辑”的类依赖加载器时才算违规
        offenders = [rel for rel, why in platform if why == "import 了加载器 API"]
        if offenders:
            print(f"\n有 {len(offenders)} 个 hotpot 下的类依赖加载器 API，没有达到分层要求：")
            for rel in offenders:
                print(f"  {rel}")
            return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
