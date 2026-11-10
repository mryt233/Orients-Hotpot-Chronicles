"""项目版本与 JDK 21 路径。作者：mryt233"""
from __future__ import annotations

import os
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
DEFAULT_JDK21 = "C:/Users/10536/.jdks/jdk-21.0.12.1+1"


def jdk21() -> Path:
    """只用显式 JDK，不回退到系统 PATH。"""
    return Path(os.environ.get("HOTPOT_JDK21") or os.environ.get("JAVA_HOME") or DEFAULT_JDK21)


def property_version(name: str, root: Path = ROOT) -> str:
    text = (root / "gradle.properties").read_text(encoding="utf-8")
    match = re.search(rf"^{re.escape(name)}\s*=\s*(\S+)", text, re.MULTILINE)
    if not match:
        raise ValueError(f"gradle.properties 里找不到 {name}")
    return match.group(1)


def mod_version(root: Path = ROOT) -> str:
    return property_version("mod_version", root)


def runtime_version(root: Path = ROOT) -> str:
    source = root / "framework/loader/src/main/java/cn/blockforge/orient/framework/loader/ApiVersion.java"
    match = re.search(r'\bRUNTIME\s*=\s*"([^"]+)"', source.read_text(encoding="utf-8"))
    if not match:
        raise ValueError("ApiVersion.java 里找不到 RUNTIME")
    return match.group(1)
