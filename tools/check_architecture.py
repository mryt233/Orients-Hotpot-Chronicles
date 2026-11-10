"""框架分层与 Gradle 依赖方向检查（P1 第一批）。

规则（《框架API规格》§2）：
  pure   → 无
  loader → pure
  game   → pure, loader
  client → pure, loader, game
  content/* → pure, loader, game, client
  platform/* → loader, game, client
  modules/* → content, platform
另外：
  - framework 源码不得 import 内容包（cn.blockforge.generated）、不得 import 加载器。
  - 依赖图不得有环。

当前 migration 过渡：common 暂视为 content/hotpot，fabric/neoforge 暂视为平台入口。

用法：python tools/check_architecture.py [--strict]
作者：mryt233
"""
from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent

# mryt233 改（2026-11-11）：移除重复键，规则不变。
ALLOWED = {
    "framework:pure": set(),
    "framework:loader": {"framework:pure"},
    "framework:game": {"framework:pure", "framework:loader"},
    "framework:client": {"framework:pure", "framework:loader", "framework:game"},
    "common": {"framework:pure", "framework:loader", "framework:game", "framework:client"},      # 过渡：content/hotpot
    "fabric": {"common", "framework:pure", "framework:loader", "framework:game", "framework:client"},
    "neoforge": {"common", "framework:pure", "framework:loader", "framework:game", "framework:client"},
    # 实验空服只验证 run/mods 里的真实 jar 解析，不依赖工程内模块
    "experiments:server-fabric": set(),
    "experiments:server-neoforge": set(),
}
FORBIDDEN_IMPORTS = re.compile(r"\b(cn\.blockforge\.generated|net\.fabricmc\.|net\.neoforged\.|net\.minecraftforge\.)")


def includes() -> list[str]:
    text = (ROOT / "settings.gradle").read_text(encoding="utf-8")
    return re.findall(r"include\s+['\"]([^'\"]+)['\"]", text)


def project_dir(project_id: str) -> Path:
    return ROOT / project_id.replace(":", "/")


def deps_of(project_id: str) -> set[str]:
    build = project_dir(project_id) / "build.gradle"
    if not build.is_file():
        return set()
    text = build.read_text(encoding="utf-8")
    return set(re.findall(r"project\('\:?([^']+)'\)", text)) | set(
        re.findall(r'project\("\:?([^"]+)"\)', text))


def check_edges(projects: list[str]) -> list[str]:
    problems: list[str] = []
    graph = {p: deps_of(p) for p in projects}
    for project, deps in graph.items():
        if project not in ALLOWED:
            problems.append(f"{project}: 未登记在允许依赖表中")
            continue
        for dep in deps:
            if dep not in projects:
                problems.append(f"{project}: 依赖 {dep} 不在 settings.gradle 里")
            elif dep not in ALLOWED[project]:
                problems.append(f"{project} → {dep}：不允许的依赖方向")
    # 环检测
    state: dict[str, int] = {}

    def visit(node: str, stack: list[str]) -> None:
        if state.get(node) == 1:
            problems.append("依赖环：" + " → ".join(stack + [node]))
            return
        if state.get(node) == 2:
            return
        state[node] = 1
        for dep in graph.get(node, ()):
            if dep in graph:
                visit(dep, stack + [node])
        state[node] = 2

    for project in graph:
        visit(project, [])
    return problems


def check_framework_sources() -> list[str]:
    problems: list[str] = []
    root = ROOT / "framework"
    if not root.is_dir():
        return problems
    for path in root.rglob("*.java"):
        for lineno, line in enumerate(path.read_text(encoding="utf-8", errors="replace").splitlines(), 1):
            code = line.split("//", 1)[0]
            if code.lstrip().startswith("*"):
                continue
            if FORBIDDEN_IMPORTS.search(code):
                problems.append(f"{path.relative_to(ROOT).as_posix()}:{lineno}: {line.strip()[:90]}")
    return problems


CLIENT_IMPORT = re.compile(r"import\s+(cn\.blockforge\.generated\.orientshotpotchronicles\.client\.|cn\.blockforge\.orient\.framework\.client\.|net\.minecraft\.client\.)")


def check_client_isolation() -> list[str]:
    """服务端内容与框架契约不得引用客户端类（第四批约束）。"""
    problems: list[str] = []
    roots = [ROOT / "common/src/main/java", ROOT / "framework/loader/src/main/java",
             ROOT / "framework/game/src/main/java"]
    for root in roots:
        if not root.is_dir():
            continue
        for path in root.rglob("*.java"):
            for lineno, line in enumerate(path.read_text(encoding="utf-8", errors="replace").splitlines(), 1):
                code = line.split("//", 1)[0]
                if CLIENT_IMPORT.search(code):
                    problems.append(f"{path.relative_to(ROOT).as_posix()}:{lineno}: 客户端类进了服务端代码")
    return problems


def main() -> int:
    strict = "--strict" in sys.argv
    projects = includes()
    problems = check_edges(projects) + check_framework_sources() + check_client_isolation()
    print(f"工程 {len(projects)} 个：{', '.join(projects)}")
    if problems:
        for line in problems[:20]:
            print("  ! " + line)
        print(f"架构检查失败：{len(problems)} 项")
        return 1 if strict else 0
    print("依赖方向与环检查通过；framework 源码无内容/加载器 import。")
    return 0


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    raise SystemExit(main())