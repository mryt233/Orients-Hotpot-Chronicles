"""把游戏里实际用的方块模型（blockstate 拼出来的几部分）导出成一个 Blockbench .bbmodel。

用法（在 mod/ 目录）：
    python tools/export_bbmodel.py
默认导出「灶台点火 + 铜锅 + 红汤」到 model-previews/stove_hotpot_combined.bbmodel。
贴图直接嵌进文件（动画贴图只取第一帧），Blockbench 和查看器的「模型文件」页都能直接打开。
改了灶台/锅的模型之后重新跑一次即可。
"""
import base64
import io
import json
import uuid
from pathlib import Path

from PIL import Image

MOD = Path(__file__).resolve().parent.parent
MODID = "orients_hotpot_chronicles"
ROOTS = [
    MOD / "src/main/resources/resourcepacks/hotpot_assets/assets",
    MOD / "src/main/resources/assets",
    MOD.parent / "viewer/cache/1.21.1/assets",
]

# (分组名, 模型 ID)
PARTS = [
    ("灶台", f"{MODID}:block/stove_lit"),
    ("铜锅", f"{MODID}:block/stove_copper_hot_pot_shell"),
    ("红汤", f"{MODID}:block/stove_soup_spicy"),
]
OUT = MOD / "model-previews/stove_hotpot_combined.bbmodel"
NAME = "灶台 + 铜锅 + 红汤（由游戏模型导出）"


def split_id(i, kind):
    ns, _, p = i.partition(":") if ":" in i else ("minecraft", "", i)
    return ns, p


def find(ns, sub):
    for r in ROOTS:
        f = r / ns / sub
        if f.exists():
            return f
    raise FileNotFoundError(f"{ns}/{sub}")


def load_model(mid):
    ns, p = split_id(mid, "models")
    j = json.loads(find(ns, f"models/{p}.json").read_text(encoding="utf-8"))
    if "parent" in j and not j["parent"].startswith("builtin/"):
        parent = load_model(j["parent"])
        j = {
            "textures": {**parent.get("textures", {}), **j.get("textures", {})},
            "elements": j.get("elements", parent.get("elements", [])),
        }
    return j


def resolve(textures, ref):
    for _ in range(16):
        if not ref.startswith("#"):
            return ref
        ref = textures.get(ref[1:], "")
    return None


def default_uv(face, f, t):
    x0, y0, z0 = f
    x1, y1, z1 = t
    return {
        "north": [16 - x1, 16 - y1, 16 - x0, 16 - y0],
        "south": [x0, 16 - y1, x1, 16 - y0],
        "east": [16 - z1, 16 - y1, 16 - z0, 16 - y0],
        "west": [z0, 16 - y1, z1, 16 - y0],
        "up": [x0, z0, x1, z1],
        "down": [x0, 16 - z1, x1, 16 - z0],
    }[face]


def texture_entry(tid, index):
    ns, p = split_id(tid, "textures")
    img = Image.open(find(ns, f"textures/{p}.png")).convert("RGBA")
    if img.height > img.width:  # 动画贴图：只取第一帧
        img = img.crop((0, 0, img.width, img.width))
    buf = io.BytesIO()
    img.save(buf, "PNG")
    return {
        "path": "", "name": p.split("/")[-1] + ".png", "folder": "block", "namespace": ns,
        "id": str(index), "width": img.width, "height": img.height,
        "uv_width": 16, "uv_height": 16, "particle": False, "render_mode": "default",
        "visible": True, "internal": True, "saved": True, "uuid": str(uuid.uuid4()),
        "source": "data:image/png;base64," + base64.b64encode(buf.getvalue()).decode(),
    }


def main():
    tex_index = {}
    textures = []
    elements = []
    outliner = []
    for group_name, mid in PARTS:
        model = load_model(mid)
        children = []
        for n, el in enumerate(model.get("elements", [])):
            faces = {}
            for face, fd in el.get("faces", {}).items():
                tid = resolve(model.get("textures", {}), fd.get("texture", ""))
                if tid is None:
                    continue
                tid = tid if ":" in tid else "minecraft:" + tid
                if tid not in tex_index:
                    tex_index[tid] = len(textures)
                    textures.append(texture_entry(tid, len(textures)))
                face_out = {"uv": fd.get("uv") or default_uv(face, el["from"], el["to"]),
                            "texture": tex_index[tid]}
                if fd.get("rotation"):
                    face_out["rotation"] = fd["rotation"]
                faces[face] = face_out
            rot = el.get("rotation")
            rotation = [0, 0, 0]
            origin = [8, 8, 8]
            if rot:
                origin = rot.get("origin", origin)
                rotation["xyz".index(rot["axis"])] = rot["angle"]
            uid = str(uuid.uuid4())
            elements.append({
                "name": el.get("name") or f"{group_name}_{n}", "box_uv": False, "rescale": False,
                "locked": False, "render_order": "default", "allow_mirror_modeling": True,
                "from": el["from"], "to": el["to"], "autouv": 0, "color": len(outliner),
                "origin": origin, "rotation": rotation, "faces": faces, "type": "cube", "uuid": uid,
            })
            children.append(uid)
        outliner.append({"name": group_name, "origin": [8, 8, 8], "color": 0, "uuid": str(uuid.uuid4()),
                         "export": True, "isOpen": True, "locked": False, "visibility": True,
                         "autouv": 0, "children": children})
    bb = {
        "meta": {"format_version": "4.10", "model_format": "java_block", "box_uv": False},
        "name": NAME, "parent": "", "ambientocclusion": True, "front_gui_light": False,
        "visible_box": [1, 1, 0], "variable_placeholders": "", "variable_placeholder_buttons": [],
        "timeline_setups": [], "unhandled_root_fields": {},
        "resolution": {"width": 16, "height": 16},
        "elements": elements, "outliner": outliner, "textures": textures,
    }
    OUT.write_text(json.dumps(bb, ensure_ascii=False, indent=1), encoding="utf-8")
    print(f"{OUT.relative_to(MOD)}: {len(elements)} 个方块, {len(textures)} 张贴图")


if __name__ == "__main__":
    main()
