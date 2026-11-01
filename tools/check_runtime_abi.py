"""前置 Runtime 二进制兼容检查：旧内容包用到的前置类 / 字段 / 方法，新 Runtime 里是否都还在。

作者：mryt233（2026-11-01，前置检查：0.1.x 之内只加不删，旧内容包要能配新前置）

用法（在 mod/ 下，先编译）：
    python tools/check_runtime_abi.py <旧内容包.jar>
        默认拿 NeoForge 版比：NeoForge 成品 jar 和编译产物都用 Mojang 官方名，可以直接对照；
        Fabric 成品是 intermediary 名，源码层面的改动两端相同，比 NeoForge 就够了。
    python tools/check_runtime_abi.py <旧内容包.jar> --classes <目录或 jar> [...]
        指定新 Runtime 的类来源（默认 framework/*/build/classes/java/main + neoforge 平台实现编译产物）。

做法：直接解析 class 文件常量池，取出旧内容包里所有指向 cn/blockforge/orient/framework/ 的
类引用、字段引用、方法引用（名字 + 描述符），在新 Runtime 的类里逐个查找（沿父类 / 接口在 Runtime 内部向上找）。
退出码：0 = 全部找到；1 = 有缺失（逐条列出）。
"""
from __future__ import annotations

import struct
import sys
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
PREFIX = "cn/blockforge/orient/framework/"
DEFAULT_CLASSES = [
    ROOT / "framework/pure/build/classes/java/main",
    ROOT / "framework/loader/build/classes/java/main",
    ROOT / "framework/game/build/classes/java/main",
    ROOT / "framework/client/build/classes/java/main",
    ROOT / "neoforge/build/classes/java/main",
]


def parse(data: bytes):
    """返回 (类名, 父类, 接口列表, 成员集合 {(名, 描述符)}, 引用列表 [(种类, 类, 名, 描述符)])。"""
    if data[:4] != b"\xca\xfe\xba\xbe":
        raise ValueError("not a class file")
    count = struct.unpack(">H", data[8:10])[0]
    pool: list = [None] * count
    i, pos = 1, 10
    while i < count:
        tag = data[pos]
        if tag == 1:
            length = struct.unpack(">H", data[pos + 1:pos + 3])[0]
            pool[i] = ("utf8", data[pos + 3:pos + 3 + length].decode("utf-8", "replace"))
            pos += 3 + length
        elif tag in (3, 4):
            pos += 5
        elif tag in (5, 6):
            pos += 9
            i += 1
        elif tag in (7, 8, 16, 19, 20):
            pool[i] = (tag, struct.unpack(">H", data[pos + 1:pos + 3])[0])
            pos += 3
        elif tag in (9, 10, 11, 12, 17, 18):
            pool[i] = (tag, *struct.unpack(">HH", data[pos + 1:pos + 5]))
            pos += 5
        elif tag == 15:
            pos += 4
        else:
            raise ValueError(f"unknown constant tag {tag}")
        i += 1

    def utf(index):
        return pool[index][1]

    def cls(index):
        return utf(pool[index][1])

    refs = []
    for entry in pool:
        if not entry or entry[0] == "utf8":
            continue
        if entry[0] == 7:
            name = utf(entry[1])
            if name.startswith(PREFIX):
                refs.append(("class", name, "", ""))
        elif entry[0] in (9, 10, 11):
            owner = cls(entry[1])
            if owner.startswith(PREFIX):
                nat = pool[entry[2]]
                kind = "field" if entry[0] == 9 else "method"
                refs.append((kind, owner, utf(nat[1]), utf(nat[2])))
    access, this, super_ = struct.unpack(">HHH", data[pos:pos + 6])
    pos += 6
    n = struct.unpack(">H", data[pos:pos + 2])[0]
    pos += 2
    interfaces = [cls(struct.unpack(">H", data[pos + 2 * k:pos + 2 * k + 2])[0]) for k in range(n)]
    pos += 2 * n
    members = set()
    for _ in range(2):  # fields, methods
        n = struct.unpack(">H", data[pos:pos + 2])[0]
        pos += 2
        for _ in range(n):
            _, name, desc, attrs = struct.unpack(">HHHH", data[pos:pos + 8])
            pos += 8
            members.add((utf(name), utf(desc)))
            for _ in range(attrs):
                length = struct.unpack(">I", data[pos + 2:pos + 6])[0]
                pos += 6 + length
    return cls(this), (cls(super_) if super_ else None), interfaces, members, refs


def read_classes(source: Path) -> dict[str, bytes]:
    out = {}
    if source.is_dir():
        for f in source.rglob("*.class"):
            out[f.relative_to(source).as_posix()] = f.read_bytes()
    elif source.suffix == ".jar":
        with zipfile.ZipFile(source) as jar:
            for name in jar.namelist():
                if name.endswith(".class"):
                    out[name] = jar.read(name)
    return out


def main(argv: list[str]) -> int:
    if not argv:
        print(__doc__)
        return 2
    old_jar = Path(argv[0])
    sources = [Path(a) for a in argv[argv.index("--classes") + 1:]] if "--classes" in argv else DEFAULT_CLASSES

    runtime: dict[str, tuple] = {}
    for source in sources:
        for name, data in read_classes(source).items():
            if name.startswith(PREFIX):
                this, super_, interfaces, members, _ = parse(data)
                runtime[this] = (super_, interfaces, members)
    if not runtime:
        print("新 Runtime 里没找到任何框架类：先编译（gradlew :framework:*:compileJava :neoforge:compileJava）")
        return 2

    def has(owner: str, name: str, desc: str, seen=None) -> bool:
        seen = seen or set()
        if owner in seen or owner not in runtime:
            return owner not in runtime and not owner.startswith(PREFIX)  # 继承到 JDK / MC 的成员视为存在
        seen.add(owner)
        super_, interfaces, members = runtime[owner]
        if (name, desc) in members:
            return True
        return any(has(parent, name, desc, seen) for parent in [super_, *interfaces] if parent)

    used = set()
    for name, data in read_classes(old_jar).items():
        if name.startswith(PREFIX):
            continue  # 旧包自己带的框架类（不该有）
        for ref in parse(data)[4]:
            used.add(ref)
    missing = []
    for kind, owner, name, desc in sorted(used):
        if owner not in runtime:
            missing.append(f"缺类 {owner}" + (f"（被引用成员 {name}{desc}）" if name else ""))
        elif kind != "class" and not has(owner, name, desc):
            missing.append(f"缺{ '字段' if kind == 'field' else '方法' } {owner}.{name} {desc}")
    classes = len({r[1] for r in used})
    print(f"旧内容包 {old_jar.name}：引用前置 {classes} 个类、{len([r for r in used if r[0] != 'class'])} 个成员；"
          f"新 Runtime {len(runtime)} 个类")
    if missing:
        print(f"不兼容 {len(set(missing))} 处：")
        for line in sorted(set(missing)):
            print("  " + line)
        return 1
    print("全部找到：旧内容包可以配这个 Runtime 运行（二进制层面）")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
