"""成就（进度）：一页「之晗的火锅物语」。

用户 2026-10-25：「成就系统先做，不加成就，等我想好了再加」——所以下面 ADVANCEMENTS 是空的，DRAFTS 是计划里的草稿（备选），
用户定了哪几条，就把它们挪进 ADVANCEMENTS（或照格式新写），再补语言文件、跑一遍：
    python tools/gen_advancements.py      # 写 data/orients_hotpot_chronicles/advancement/*.json（先清空这个文件夹）
文字在语言文件 advancements.orients_hotpot_chronicles.<名字>.title / .description（10 种语言，另加）。
原版触发器能管的用原版（拿到物品、喝饮料），其余用自定义触发器 orients_hotpot_chronicles:hotpot（hotpot/HotPotCriterion）。
作者：mryt233
"""
import json
from pathlib import Path

MOD = Path(__file__).resolve().parent.parent
MODID = "orients_hotpot_chronicles"
OUT = MOD / "common/src/main/resources/data" / MODID / "advancement"


def item(i):
    return i if ":" in i else f"{MODID}:{i}"


def has_any(*items):
    return {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": [item(i) for i in items]}]}}


def event(e):
    return {"trigger": f"{MODID}:hotpot", "conditions": {"event": e}}


POTS = [f"{m}_{k}" for m in ("iron", "copper") for k in ("taiji_yuanyang_hotpot", "round_yuanyang_hotpot", "square_yuanyang_hotpot",
                                                          "split_yuanyang_hotpot", "four_grid_hotpot")]

# 名字, 父, 图标, 外框（task / goal / challenge）, 隐藏, 条件
DRAFTS = [
    ("root", None, "brick_stove", "task", False, {"stove": has_any("brick_stove", "tall_brick_stove")}),
    ("first_cooked", "root", "iron_nine_grid_hotpot", "task", False, {"cooked": event("pot_cooked")}),
    ("tripe", "first_cooked", "cooked_tripe", "task", False, {"tripe": event(f"eat:{MODID}:cooked_tripe")}),
    ("dipped", "first_cooked", "dipping_sauce", "task", False, {"dipped": event("eat_dipped")}),
    ("sauce_master", "dipped", "iron_sauce_station", "challenge", False, {"full": event("sauce_full")}),
    ("seated", "dipped", "oak_bench", "goal", True, {"seated": event("eat_seated")}),
    ("carry_pot", "first_cooked", "copper_nine_grid_hotpot", "task", False, {"carry": event("carry_pot")}),
    ("yuanyang", "root", "iron_taiji_yuanyang_hotpot", "goal", False, {"pot": has_any(*POTS)}),
    ("knife_skills", "root", "iron_kitchen_knife", "task", False,
     {"slice": has_any("beef_loin_slice", "fatty_beef_roll", "mutton_roll")}),
    ("pounding", "knife_skills", "rolling_pin", "task", False,
     {"ball": has_any("beef_meatball", "pork_meatball", "mutton_meatball", "fish_ball", "garlic_paste")}),
    ("authors_pick", "root", "plant_protein_drink", "task", False,
     {"drink": {"trigger": "minecraft:consume_item", "conditions": {"item": {"items": [item("plant_protein_drink")]}}},
      "cup": {"trigger": "minecraft:consume_item", "conditions": {"item": {"items": [item("plant_protein_drink_cup")]}}}}),
]


# 正式加进游戏的成就（用户定了再加；第一条要是 root：没有父，带背景图）
ADVANCEMENTS = []


def main():
    if OUT.exists():
        for old in OUT.glob("*.json"):
            old.unlink()
    if not ADVANCEMENTS:
        print("no advancements yet")
        return
    OUT.mkdir(parents=True, exist_ok=True)
    for name, parent, icon, frame, hidden, criteria in ADVANCEMENTS:
        key = f"advancements.{MODID}.{name}"
        display = {"icon": {"id": item(icon)}, "title": {"translate": key + ".title"},
                   "description": {"translate": key + ".description"}, "frame": frame,
                   "show_toast": True, "announce_to_chat": True, "hidden": hidden}
        adv = {"display": display, "criteria": criteria,
               "requirements": [list(criteria)]}   # 列表里任一条件满足就算
        if parent:
            adv["parent"] = f"{MODID}:{parent}"
        else:
            display["background"] = "minecraft:textures/block/bricks.png"
            display["announce_to_chat"] = False
        (OUT / f"{name}.json").write_text(json.dumps(adv, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print("ok", len(ADVANCEMENTS))


if __name__ == "__main__":
    main()
