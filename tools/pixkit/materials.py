"""有序材质定义。作者：mryt233"""
import json
from .paths import MOD


def materials():
    return json.loads((MOD / "content/materials.json").read_text(encoding="utf-8"))


def ids(group):
    return [entry["id"] for entry in materials()[group]]


def tuples(value):
    """JSON 数组还原旧脚本使用的嵌套元组。"""
    return tuple(tuples(v) for v in value) if isinstance(value, list) else value


def columns(group, *keys):
    return {entry["id"]: tuple(tuples(entry[key]) for key in keys) for entry in materials()[group]}
