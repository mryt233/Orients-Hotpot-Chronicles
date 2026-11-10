"""写文件。作者：mryt233

一律写 LF 换行：Windows 上 Path.write_text 会把 \n 写成 CRLF，仓库里是 LF（旧 gen_boiling / gen_ingredients 等用了 write_text）。
"""
import json
from pathlib import Path


def write_json(path, obj, ensure_ascii=True):
    """写 JSON（缩进 2、末尾换行、LF）。自动建目录。"""
    path = Path(path)
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes((json.dumps(obj, indent=2, ensure_ascii=ensure_ascii) + "\n").encode("utf-8"))


# mryt233 改（2026-11-10）：迁移旧生成器时保留资源的换行、末尾换行和转义约定。
def write_legacy_json(path, obj, ensure_ascii=True, newline=None, trailing=True):
    path = Path(path)
    if newline is None:
        newline = "\r\n" if path.exists() and b"\r\n" in path.read_bytes() else "\n"
    text = json.dumps(obj, indent=2, ensure_ascii=ensure_ascii) + ("\n" if trailing else "")
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(text.replace("\n", newline).encode("utf-8"))


def write_text(path, text):
    """复制文本时保留目标资源已有换行，新文件使用 LF。"""
    path = Path(path)
    newline = "\r\n" if path.exists() and b"\r\n" in path.read_bytes() else "\n"
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(text.replace("\r\n", "\n").replace("\n", newline).encode("utf-8"))


def save_png(im, path):
    """保存 PNG，自动建目录。"""
    path = Path(path)
    path.parent.mkdir(parents=True, exist_ok=True)
    im.save(path)


def install_item(root, name, im, folder="item", model=True):
    """装一个薄物品：textures/<folder>/<name>.png + models/item/<name>.json（item/generated）。"""
    from .model import item_generated
    save_png(im, Path(root) / f"textures/{folder}/{name}.png")
    if model:
        write_json(Path(root) / f"models/item/{name}.json", item_generated(name, folder))
