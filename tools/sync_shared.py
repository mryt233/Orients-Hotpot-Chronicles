"""把「之晗的火锅物语」的共享代码和资源同步到子模组。

用法（工作区根目录执行）：
    python mod/tools/sync_shared.py mod2            # 只列出差异，不动文件
    python mod/tools/sync_shared.py mod2 --apply    # 真的覆盖过去
    python mod/tools/sync_shared.py mod2 --apply --force   # 忽略本地改动，强推

目录分层（2026-10-29 起）：
  - `hotpot/` 是核心，住在 `common/src/main/java/...`
  - `platform/`、`client/`、`compat/`、`mixin/` 是平台层，住在 `fabric/src/main/java/...`
  两边目录结构一致，所以同步按包选根目录。

同步内容（2026-10-29 扩）：除了 Java，还包括共享内容用到的**全部资源**——
`assets/orients_hotpot_chronicles/` 与 `data/orients_hotpot_chronicles/` 整棵树
（贴图、模型、语言、配方、标签、战利品表）。子模组自己的内容放在自己的命名空间下，
不会被碰。这一条是为了堵住「同步了代码却没同步贴图，进游戏是紫黑方块」的坑。

不同步：`devtest/`（子模组有自己的测试）、`GeneratedMod.java`（各模组入口不同）、
子模组自己的命名空间资源。

安全措施：
  - 目标目录下有 `.sync-manifest.json` 记录上次同步时每个文件的哈希。
  - 只有目标文件仍等于上次同步的哈希（或从没同步过）才允许覆盖。
  - 被本地改过 → 判为冲突，默认不覆盖并给 diff。
  - 删除只看 manifest 记过的文件。
  - 写入先写临时文件再原子替换。

作者：mryt233
"""
from __future__ import annotations

import difflib
import hashlib
import json
import shutil
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent          # mod/
WORKSPACE = ROOT.parent
PKG = "cn/blockforge/generated/orientshotpotchronicles"
PACKAGES = ("hotpot", "network", "platform", "client", "compat", "mixin")
SKIP_FILES = {"GeneratedMod.java"}
MANIFEST = ".sync-manifest.json"
COMMON_PACKAGES = {"hotpot", "network", "compat"}
RESOURCE_TREES = ("assets/orients_hotpot_chronicles", "data/orients_hotpot_chronicles")
# 这两个目录两边内容本来就不同，不能整文件覆盖：
#   lang/  —— 子模组用的是同一个模组 ID（orients_hotpot_chronicles），但 modmenu.* 描述是它自己的
#   guide/ —— 由各自的 tools/gen_guide_book.py + guide_content.py 生成，子模组有自己的条目
RESOURCE_MERGE_LANG = "assets/orients_hotpot_chronicles/lang"
RESOURCE_SKIP = ("assets/orients_hotpot_chronicles/lang/", "assets/orients_hotpot_chronicles/guide/")


def java_roots(base: Path, pkg: str) -> Path:
    if pkg == "client":
        folder = "common/src/client/java"
    elif pkg == "mixin":
        folder = "common/src/mixin/java"
    else:
        folder = "common/src/main/java" if pkg in COMMON_PACKAGES else "fabric/src/main/java"
    return base / folder / PKG / pkg


def sync_pairs(target: str) -> list[tuple[str, Path, Path]]:
    """返回 [(manifest 键, 源文件, 目标文件)]。"""
    pairs: list[tuple[str, Path, Path]] = []
    other = WORKSPACE / target
    for pkg in PACKAGES:
        source = java_roots(ROOT, pkg)
        if not source.exists():
            continue
        for src in sorted(source.rglob("*.java")):
            if src.name in SKIP_FILES:
                continue
            rel = f"{pkg}/{src.relative_to(source).as_posix()}"
            pairs.append((rel, src, java_roots(other, pkg) / src.relative_to(source)))
    for tree in RESOURCE_TREES:
        source = ROOT / "common/src/main/resources" / tree
        if not source.exists():
            continue
        for src in sorted(p for p in source.rglob("*") if p.is_file()):
            rel_tree = src.relative_to(source).as_posix()
            if any(f"{tree}/{rel_tree}".startswith(prefix) for prefix in RESOURCE_SKIP):
                continue
            rel = f"resources/{tree}/{rel_tree}"
            pairs.append((rel, src, other / "common/src/main/resources" / tree / src.relative_to(source)))
    return pairs


def merge_langs(target: str, apply: bool) -> list[str]:
    """语言文件只补「火锅有、子模组没有」的键，绝不覆盖已有值（modmenu.* 是它自己的）。"""
    other = WORKSPACE / target
    source_dir = ROOT / "common/src/main/resources" / RESOURCE_MERGE_LANG
    dest_dir = other / "common/src/main/resources" / RESOURCE_MERGE_LANG
    report = []
    for src in sorted(source_dir.glob("*.json")):
        dst = dest_dir / src.name
        upstream = json.loads(src.read_text(encoding="utf-8"))
        if dst.exists():
            local = json.loads(dst.read_text(encoding="utf-8"))
            missing = {k: v for k, v in upstream.items() if k not in local}
            if not missing:
                continue
            report.append(f"{src.name}: 补 {len(missing)} 个键（{', '.join(sorted(missing)[:3])}…）")
            if apply:
                merged = dict(local)
                merged.update(missing)
                text = json.dumps(merged, ensure_ascii=False, indent=2) + "\n"
                atomic_write(dst, text.encode("utf-8"))
        else:
            report.append(f"{src.name}: 目标端没有，整份复制")
            if apply:
                atomic_write(dst, src.read_bytes())
    return report


def digest(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def atomic_write(path: Path, data: bytes) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    handle, temp = tempfile.mkstemp(dir=str(path.parent), prefix=".sync-", suffix=".tmp")
    try:
        with open(handle, "wb") as stream:
            stream.write(data)
        shutil.move(temp, path)
    finally:
        if Path(temp).exists():
            Path(temp).unlink()


def load_manifest(path: Path) -> dict:
    if not path.exists():
        return {}
    try:
        data = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, ValueError):
        return {}
    # 旧版本在 Windows 上把键写成了反斜杠，统一成 / 再比较，否则会误判成「源端已没有」
    return {key.replace("\\", "/"): value for key, value in data.items()}


def validate_target(other: Path) -> str | None:
    """先验证布局/映射，绝不将 Mojang 源码同步进旧 Yarn 工程。"""
    if other.resolve() == ROOT.resolve():
        return "不能把主工程自身作为同步目标"
    for folder in ("common/src/main/java", "common/src/main/resources", "fabric/src/main/java"):
        if not (other / folder).is_dir():
            return f"目标仍是旧布局（缺 {folder}），先迁移工程；没有写入或删除任何文件"
    for module in ("common", "fabric"):
        build = other / module / "build.gradle"
        if not build.is_file() or "officialMojangMappings" not in build.read_text(encoding="utf-8"):
            return f"无法确认 {module} 使用 Mojang 官方映射，禁止同步（--force 也不绕过）"
    return None


def main() -> int:
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    apply = "--apply" in sys.argv
    force = "--force" in sys.argv
    if not args:
        print(__doc__)
        return 1
    target = args[0]
    if not (WORKSPACE / target).exists():
        print(f"找不到目标：{WORKSPACE / target}")
        return 1

    error = validate_target(WORKSPACE / target)
    if error:
        print(f"不能同步：{error}")
        return 1

    manifest_path = WORKSPACE / target / "src/.sync-manifest.json"
    manifest = load_manifest(manifest_path)

    added, changed, extra, conflicts = [], [], [], []
    same = 0
    next_manifest = {}

    pairs = sync_pairs(target)
    sources = {rel: src for rel, src, _ in pairs}
    for rel, src, dst_file in pairs:
        source_hash = digest(src)
        next_manifest[rel] = source_hash
        if not dst_file.exists():
            added.append(rel)
            if apply:
                atomic_write(dst_file, src.read_bytes())
            continue
        if digest(dst_file) == source_hash:
            same += 1
            continue
        known = manifest.get(rel)
        if known is not None and digest(dst_file) != known and not force:
            conflicts.append(rel)
            continue
        changed.append(rel)
        if apply:
            atomic_write(dst_file, src.read_bytes())

    # 目标端有、源端已没有的（火锅删掉的文件）：只清理上次同步过的
    destinations = {rel: dst for rel, _, dst in pairs}
    for rel in sorted(manifest):
        if rel in sources:
            continue
        dst_file = destinations.get(rel)
        if dst_file is None:
            # 源端整个目录都没了，按 manifest 键反推目标路径
            if rel.startswith("resources/"):
                dst_file = WORKSPACE / target / "common/src/main/resources" / rel[len("resources/"):]
            else:
                pkg, _, rest = rel.partition("/")
                dst_file = java_roots(WORKSPACE / target, pkg) / rest
        if dst_file.exists():
            extra.append(rel)
            if apply:
                dst_file.unlink()

    print(f"目标：{target}")
    print(f"一致 {same} 个，新增 {len(added)} 个，有差异 {len(changed)} 个，"
          f"多余 {len(extra)} 个，本地改动冲突 {len(conflicts)} 个")
    lang_report = merge_langs(target, apply)
    for p in added:
        print(f"  + {p}")
    for p in changed:
        print(f"  ~ {p}")
    for p in extra:
        print(f"  - {p}（源端已没有）")
    for p in conflicts:
        print(f"  ! {p}（目标端在上次同步后被改过，未覆盖）")
        if not apply and p in sources and sources[p].suffix == ".java":
            local = destinations[p]
            local_lines = local.read_text(encoding="utf-8", errors="replace").splitlines()
            src_lines = sources[p].read_text(encoding="utf-8", errors="replace").splitlines()
            for line in list(difflib.unified_diff(local_lines, src_lines, "目标", "源", lineterm="", n=1))[:20]:
                print("      " + line)

    for line in lang_report:
        print(f"  ≈ 语言 {line}")

    if conflicts:
        print("\n有本地改动被拦下：先确认那处是不是直接改了火锅代码。"
              "是的话改成钩子；确实要覆盖再加 --force。")
    elif not apply and (added or changed or extra):
        print("\n这是预览，加 --apply 才会真的覆盖 / 删除。")
    if apply:
        atomic_write(manifest_path, json.dumps(next_manifest, indent=2, sort_keys=True).encode("utf-8"))
        print("\n已同步，manifest 已更新。记得两边各跑一次 compileJava 验证。")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
