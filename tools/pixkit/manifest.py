"""记录生成器实际写入的资源及哈希，支持隔离输出。作者：mryt233"""
import atexit
import hashlib
import inspect
import json
from pathlib import Path
import sys

_started = False
_declared_merges = {}


def declare_merge(path, order, reason):
    """mryt233 改（2026-11-10）：允许生成入口显式声明有序的说明补写。"""
    _declared_merges[Path(path).resolve()] = {"order": order, "reason": reason}


def start_session(mod: Path, resources: Path):
    global _started
    mod, resources = Path(mod).resolve(), Path(resources).resolve()
    entry = Path(sys.argv[0]).name
    if _started or not (entry.startswith("gen_") and entry.endswith(".py") or entry == "recolor_clear_soup.py"):
        return
    _started = True
    written = {}
    failed = [False]
    source = mod / "common/src/main/resources"
    # mryt233 改（2026-11-10）：预览是独立输出根，隔离试跑也只登记到隔离清单。
    preview = mod / "preview_models" if resources == source else resources.with_name(resources.name + "-preview_models")
    roots = {"common/src/main/resources": resources, "preview_models": preview}
    destination = (mod / "content/generated_manifest.json" if resources == source else
                   resources.parent / (resources.name + "-generated_manifest.json"))
    original_bytes, original_text = Path.write_bytes, Path.write_text

    def record(path):
        try:
            path = Path(path).resolve()
            rel = next(prefix + "/" + path.relative_to(root).as_posix()
                       for prefix, root in roots.items() if path.is_relative_to(root))
        except (TypeError, ValueError, StopIteration):
            return
        owner = "tools/" + entry
        for frame in inspect.stack():
            file = Path(frame.filename)
            if file.parent == mod / "tools" and file.name.startswith("gen_"):
                owner = "tools/" + file.name
                break
        written.setdefault(owner, set()).add(rel)

    def write_bytes(path, data):
        result = original_bytes(path, data)
        record(path)
        return result

    def write_text(path, *args, **kwargs):
        result = original_text(path, *args, **kwargs)
        record(path)
        return result

    Path.write_bytes, Path.write_text = write_bytes, write_text
    from PIL import Image
    original_save = Image.Image.save
    def save(image, fp, *args, **kwargs):
        result = original_save(image, fp, *args, **kwargs)
        record(fp)
        return result
    Image.Image.save = save
    old_hook = sys.excepthook
    def error_hook(*args):
        failed[0] = True
        old_hook(*args)
    sys.excepthook = error_hook

    def finish():
        if failed[0] or not written:
            return
        manifest = json.loads(destination.read_text(encoding="utf-8")) if destination.exists() else {
            "_说明": "作者：mryt233；实际生成文件及 SHA-256。合并文件以 merges 的最终哈希为准。",
            "generators": {}, "merges": {},
        }
        for owner, files in written.items():
            hashes = {}
            for rel in sorted(files):
                prefix = next(prefix for prefix in roots if rel.startswith(prefix + "/"))
                file = roots[prefix] / Path(rel).relative_to(prefix)
                if file.is_file():
                    hashes[rel] = hashlib.sha256(file.read_bytes()).hexdigest()
                    if file in _declared_merges:
                        manifest["merges"][rel] = {**_declared_merges[file], "sha256": hashes[rel]}
                    if rel in manifest["merges"]:
                        manifest["merges"][rel]["sha256"] = hashes[rel]
            # mryt233 改（2026-11-10）：另一入口只调用某生成器的预览步骤时，保留它已登记的其余产物。
            manifest["generators"].setdefault(owner, {"files": {}})["files"].update(hashes)
        destination.parent.mkdir(parents=True, exist_ok=True)
        original_text(destination, json.dumps(manifest, indent=2, ensure_ascii=False) + "\n", encoding="utf-8", newline="\n")
    atexit.register(finish)
