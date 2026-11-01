"""方块 / 物品模型 JSON。作者：mryt233

坐标单位是模型像素（一格 = 16）。只生成字典，写文件用 io.write_json。
"""
from .paths import MODID

FACES = ("north", "east", "south", "west", "up", "down")


def box(frm, to, tex, faces=FACES, uv=None):
    """一个长方块。tex 如 "#wood"；faces 选要画的面；uv 可给 {面: [u0, v0, u1, v1]} 写明部分面的贴图区域。"""
    out = {}
    for d in faces:
        f = {"texture": tex}
        if uv and d in uv:
            f = {"uv": uv[d], "texture": tex}
        out[d] = f
    return {"from": list(frm), "to": list(to), "faces": out}


def item_generated(name, folder="item", modid=MODID):
    """原版 item/generated 薄物品模型（一张贴图挤出厚度）。"""
    return {"parent": "minecraft:item/generated", "textures": {"layer0": f"{modid}:{folder}/{name}"}}


def item_from_block(block_model, display=None, modid=MODID):
    """物品直接用某个方块模型的样子，可另给 display。"""
    m = {"parent": f"{modid}:block/{block_model}"}
    if display:
        m["display"] = display
    return m


def display_entry(rotation=(0, 0, 0), translation=(0, 0, 0), scale=1.0):
    s = scale if isinstance(scale, (list, tuple)) else (scale, scale, scale)
    return {"rotation": list(rotation), "translation": list(translation), "scale": list(s)}


# 原版 block/block.json 的 display（方块拿在手里、放物品栏的默认姿势），抄自 1.21.1
VANILLA_BLOCK_DISPLAY = {
    "gui": display_entry((30, 225, 0), (0, 0, 0), 0.625),
    "ground": display_entry((0, 0, 0), (0, 3, 0), 0.25),
    "fixed": display_entry((0, 0, 0), (0, 0, 0), 0.5),
    "thirdperson_righthand": display_entry((75, 45, 0), (0, 2.5, 0), 0.375),
    "firstperson_righthand": display_entry((0, 45, 0), (0, 0, 0), 0.40),
    "firstperson_lefthand": display_entry((0, 225, 0), (0, 0, 0), 0.40),
}


def small_block_display(height, gui_scale=1.4):
    """矮小方块（杯、碗、小碟）放物品栏时居中：往上挪 height 像素并放大。参照 gen_cup 的 CUP_DISPLAY。"""
    return {
        "gui": display_entry((30, 225, 0), (0, height, 0), gui_scale),
        "ground": display_entry((0, 0, 0), (0, 3, 0), 0.6),
        "fixed": display_entry((0, 0, 0), (0, 0, 0), 1.2),
        "thirdperson_righthand": display_entry((0, 45, 0), (0, 2, 1), 0.6),
        "thirdperson_lefthand": display_entry((0, 225, 0), (0, 2, 1), 0.6),
        "firstperson_righthand": display_entry((0, 45, 0), (0, 3, 0), 0.7),
        "firstperson_lefthand": display_entry((0, 225, 0), (0, 3, 0), 0.7),
    }


def block_model(elements, textures, parent="minecraft:block/block"):
    """方块模型：textures 如 {"d": "orients_hotpot_chronicles:block/cup"}；没给 particle 时用第一张。"""
    tex = dict(textures)
    tex.setdefault("particle", next(iter(textures.values())))
    return {"parent": parent, "textures": tex, "elements": elements}
