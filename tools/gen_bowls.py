"""碗的模型（r99，用户要原版碗能放桌上装菜、盛汤）：先出两种形状给用户在查看器里挑。

    python tools/gen_bowls.py
生成 models/block/bowl_<形状>.json（空碗）和 bowl_<形状>_soup_<汤底>.json（盛着汤）。中心在方块中央、底在 y=0，
和盘子一样由盘子格的渲染器平移、贴着桌面画。贴图 bowl_wood / bowl_wood_inner 是按原版碗物品的颜色给橡木木板重新上色的。
  a：直壁碗——4×4 碗底脚，6×6 碗身，壁厚 1，高 3
  b：敞口碗——4×4 碗脚，下圈 6×6、上圈 7×7 往外张半像素，高 3（8×8 的话一格并排两只会连成一条槽）
作者：
"""
import json
from pathlib import Path

MOD = Path(__file__).resolve().parent.parent
MODID = "orients_hotpot_chronicles"
RES = MOD / "src/main/resources"
ASSET_ROOTS = [RES / "assets" / MODID, RES / "resourcepacks/hotpot_assets/assets" / MODID]
SOUPS = ["clear", "rich_stock", "mushroom", "spicy"]
FACES = ["north", "east", "south", "west", "up", "down"]


def box(f, t, tex, faces=FACES):
    return {"from": f, "to": t, "faces": {d: {"texture": tex} for d in faces}}


def ring(half_out, y0, y1, tex="#wood"):
    """方环：外边半宽 half_out，壁厚 1。"""
    a, b = 8 - half_out, 8 + half_out
    return [
        box([a, y0, a], [b, y1, a + 1], tex),
        box([a, y0, b - 1], [b, y1, b], tex),
        box([a, y0, a + 1], [a + 1, y1, b - 1], tex),
        box([b - 1, y0, a + 1], [b, y1, b - 1], tex),
    ]


SHAPES = {
    # 形状: (元素, 汤面高度, 汤面半宽)
    "a": ([box([6, 0, 6], [10, 0.5, 10], "#wood"), box([5, 0.5, 5], [11, 1, 11], "#inner")] + ring(3, 1, 3),
          2.5, 2),
    "b": ([box([6, 0, 6], [10, 1, 10], "#wood")] + ring(3, 1, 2) + ring(3.5, 2, 3), 2.5, 2.5),
}


def surface(y, half):
    a, b = 8 - half, 8 + half
    return {"from": [a, y, a], "to": [b, y + 0.01, b],
            "faces": {"up": {"texture": "#soup", "uv": [a, a, b, b]}}}


def write(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n", encoding="utf-8")


def main():
    for root in ASSET_ROOTS:
        for name, (els, soup_y, soup_half) in SHAPES.items():
            textures = {"wood": f"{MODID}:block/bowl_wood", "inner": f"{MODID}:block/bowl_wood_inner",
                        "particle": f"{MODID}:block/bowl_wood"}
            # 碗底朝上的那一面（碗里面看得见的底）用暗的内壁贴图
            els = json.loads(json.dumps(els))
            for e in els:
                if e["from"][0] == 6 and e["to"][0] == 10 or e["faces"]["up"]["texture"] == "#inner":
                    e["faces"]["up"]["texture"] = "#inner"
            write(root / f"models/block/bowl_{name}.json",
                  {"parent": "minecraft:block/block", "textures": textures, "elements": els})
            for soup in SOUPS:
                t = dict(textures, soup=f"{MODID}:block/hot_pot_top_{soup}")
                write(root / f"models/block/bowl_{name}_soup_{soup}.json",
                      {"parent": "minecraft:block/block", "textures": t,
                       "elements": els + [surface(soup_y, soup_half)]})
    print("ok")


if __name__ == "__main__":
    main()


# ---------------------------------------------------------------- r99 定稿：游戏里用 b（a 的模型留在仓库，用户要求）
GAME_SHAPE = "b"
BOWL_DISPLAY = {
    "gui": {"rotation": [30, 225, 0], "translation": [0, 6, 0], "scale": [1.3, 1.3, 1.3]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.6, 0.6, 0.6]},
    "fixed": {"rotation": [0, 0, 0], "translation": [0, 4, 0], "scale": [1.2, 1.2, 1.2]},
    "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 3, 2], "scale": [0.6, 0.6, 0.6]},
    "thirdperson_lefthand": {"rotation": [75, 225, 0], "translation": [0, 3, 2], "scale": [0.6, 0.6, 0.6]},
    "firstperson_righthand": {"rotation": [30, 45, 0], "translation": [0, 5, 0], "scale": [0.7, 0.7, 0.7]},
    "firstperson_lefthand": {"rotation": [30, 225, 0], "translation": [0, 5, 0], "scale": [0.7, 0.7, 0.7]},
}


def game_files():
    """摆在桌上的碗（方块状态 bowl：汤底 × 方向）、四种汤碗物品、装菜的碗（原版 bowl 的自定义模型数据 1）。"""
    for root in ASSET_ROOTS:
        variants = {}
        for axis in ("x", "z"):
            for soup in ["none"] + SOUPS:
                model = f"{MODID}:block/bowl_{GAME_SHAPE}" + ("" if soup == "none" else f"_soup_{soup}")
                v = {"model": model}
                if axis == "z":
                    v["y"] = 90
                variants[f"axis={axis},soup={soup}"] = v
        write(root / "blockstates/bowl.json", {"variants": variants})
        for soup in SOUPS:
            bowl_id = {"clear": "clear_broth_bowl", "rich_stock": "rich_stock_bowl", "mushroom": "mushroom_broth_bowl",
                       "spicy": "spicy_red_broth_bowl"}[soup]   # 物品 ID（r102 起和显示名对得上）
            write(root / f"models/item/{bowl_id}.json",
                  {"parent": f"{MODID}:block/bowl_{GAME_SHAPE}_soup_{soup}", "display": BOWL_DISPLAY})
        write(root / "models/item/bowl_with_food.json", {"parent": "minecraft:builtin/entity", "display": BOWL_DISPLAY})
        # 原版的碗：平时照原版，装着菜（自定义模型数据 1）切到上面那个，由 HeldFoodItemRenderer 画立体的碗和菜
        mc = root.parent / "minecraft"
        write(mc / "models/item/bowl.json", {
            "parent": "minecraft:item/generated",
            "textures": {"layer0": "minecraft:item/bowl"},
            "overrides": [{"predicate": {"custom_model_data": 1}, "model": f"{MODID}:item/bowl_with_food"}],
        })


game_files()
