"""按生成清单哈希检查手改与未声明的重复生成者，不重跑生成器。作者：mryt233"""
import hashlib
import json
from pathlib import Path
from pixkit.paths import MOD


def check_manifest(root: Path = MOD, manifest=None):
    manifest = manifest if manifest is not None else json.loads((root / "content/generated_manifest.json").read_text(encoding="utf-8"))
    owners, problems = {}, []
    for generator, info in manifest["generators"].items():
        for name, digest in info["files"].items():
            owners.setdefault(name, {})[generator] = digest
    for name, claims in sorted(owners.items()):
        merge = manifest.get("merges", {}).get(name)
        if len(claims) > 1 and (not merge or set(merge.get("order", [])) != set(claims) or not merge.get("reason")):
            problems.append(f"未声明合并关系：{name}：{', '.join(claims)}")
        expected = merge["sha256"] if merge else next(iter(claims.values()))
        file = root / name
        if not file.is_file() or hashlib.sha256(file.read_bytes()).hexdigest() != expected:
            problems.append(f"生成物缺失或被手改：{name}")
    return not problems, [f"{len(manifest['generators'])} 个生成器，{len(owners)} 个文件，{len(manifest.get('merges', {}))} 个显式合并项"] + problems


if __name__ == "__main__":
    ok, lines = check_manifest()
    print("\n".join(lines))
    raise SystemExit(not ok)
