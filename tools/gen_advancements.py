"""成就（进度）：一页「之晗的火锅物语」，24 条正式成就已实装。

2026-11-05 r136d：把原来 ADVANCEMENTS 里待定的草稿按任务单修正，补足 16 条正式成就并实装（mryt233 改）。
2026-11-06 r139：刀工成就改名「精雕细琢」，并新增 8 条火锅特色成就（九宫掌门 / 阴阳调和 / 调味宗师 /
豆腐四重奏 / 爽脆三绝 / 整装掌勺 / 西南之魂 / 围炉而坐），共 24 条（mryt233 改）。
其中豆腐四重奏、爽脆三绝、整装掌勺、西南之魂依赖 r139 前后才做的冻豆腐 / 鸭肠 / 黄喉 / 布围裙 / 折耳根，
先按现有内容兜底，物品上线后条件自动接上（详见各自的注释）。
跑法：
    python tools/gen_advancements.py      # 写 data/orients_hotpot_chronicles/advancement/*.json（先清空这个文件夹）
文字在语言文件 advancements.orients_hotpot_chronicles.<名字>.title / .description（10 种语言，另加）。
原版触发器能管的用原版（拿到物品），其余用自定义触发器 orients_hotpot_chronicles:hotpot（hotpot/HotPotCriterion）。
喝饮料这类由模组自己 finishUsingItem 结算的动作也走自定义触发器（mryt233 改 2026-11-06：原版 consume_item 不一定触发）。
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

# 名字, 父, 图标, 外框（task / goal / challenge）, 隐藏, 条件（列表里任一条件满足就算达成）
ADVANCEMENTS = [
    ("root", None, "brick_stove", "task", False,
     {"stove": has_any("brick_stove", "tall_brick_stove")}),
    ("first_cooked", "root", "iron_nine_grid_hotpot", "task", False,
     {"cooked": event("pot_cooked")}),
    ("tripe", "first_cooked", "cooked_tripe", "task", False,
     {"tripe": event(f"eat:{MODID}:cooked_tripe")}),
    ("dipped", "first_cooked", "dipping_sauce", "task", False,
     {"dipped": event("eat_dipped")}),
    ("sauce_master", "dipped", "iron_sauce_station", "challenge", False,
     {"full": event("sauce_full")}),
    ("seated", "dipped", "oak_bench", "goal", False,
     {"seated": event("eat_seated")}),
    ("carry_pot", "first_cooked", "copper_nine_grid_hotpot", "task", False,
     {"carry": event("carry_pot")}),
    ("yuanyang", "root", "iron_taiji_yuanyang_hotpot", "goal", False,
     {"pot": has_any(*POTS)}),
    # 刀工：肉片三种 + 火腿肠片
    ("knife_skills", "root", "iron_kitchen_knife", "task", False,
     {"slice": has_any("beef_loin_slice", "fatty_beef_roll", "mutton_roll", "ham_sausage_slice")}),
    # 千锤百炼：肉丸 / 鱼丸 / 虾滑 / 蒜泥
    ("pounding", "knife_skills", "rolling_pin", "task", False,
     {"ball": has_any("beef_meatball", "pork_meatball", "mutton_meatball", "fish_ball", "shrimp_paste", "garlic_paste")}),
    # 作者推荐：四种饮品喝下任一。mryt233 改（2026-11-06，r139）：原来用原版 consume_item 判定，
    # 饮料 / 杯子是自定义 finishUsingItem 结算的，1.21.1 上不一定触发；改走模组自己的 hotpot 触发器，
    # 由 DrinkItem / CupItem 喝下时 fire("drink_author")。
    ("authors_pick", "root", "plant_protein_drink", "task", False,
     {"drink": event("drink_author")}),
    # 偏爱：煮虾 + 持有三种虾制品任一
    ("shrimp_lover", "first_cooked", "cooked_shrimp", "goal", False,
     {"cooked": event("cook_shrimp"),
      "item": has_any("cooked_shrimp", "cooked_peeled_shrimp", "cooked_shrimp_paste")}),
    # 心急吃不了热豆腐：煮老了 + 持有热豆腐块
    ("tofu_overcook", "first_cooked", "hot_tofu_cube", "task", False,
     {"overcook": event("tofu_overcook"),
      "item": has_any("hot_tofu_cube")}),
    # 无辣不欢：红汤加到变态辣
    ("max_spicy", "first_cooked", "chili_pepper", "challenge", False,
     {"spicy": event("max_spicy")}),
    # 干杯：端着满杯饮品碰杯
    ("cheers", "authors_pick", "orient_cola_cup", "goal", False,
     {"cheers": event("cheers")}),
    # 步步高升：把一格高条凳叠起来当梯子爬上去
    ("tall_stool", "root", "oak_bench", "task", False,
     {"stack": event("tall_stool")}),

    # ---------- 2026-11-06 r139 新增 8 条火锅特色成就（mryt233 改） ----------
    # 九宫掌门：手里有铜制或铁制九宫格火锅
    ("nine_grid_master", "first_cooked", "copper_nine_grid_hotpot", "task", False,
     {"pot": has_any("iron_nine_grid_hotpot", "copper_nine_grid_hotpot")}),
    # 阴阳调和：手里有一口太极鸳鸯锅（铜 / 铁都算）
    ("yin_yang_balance", "yuanyang", "iron_taiji_yuanyang_hotpot", "goal", False,
     {"pot": has_any("iron_taiji_yuanyang_hotpot", "copper_taiji_yuanyang_hotpot")}),
    # 调味宗师：吃下一碗同时放了 5 种以上调料的蘸料。判定在 LivingEntityEatMixin（吃的那一刻数 mask 里有几种），
    # 由 HotPotCriterion.fire(player, "sauce_master_5") 上报；不依赖蘸料物品 ID，以后加调料不用改。
    ("sauce_alchemist", "sauce_master", "dipping_sauce", "challenge", False,
     {"five": event("sauce_master_5")}),
    # 豆腐四重奏：冷豆腐块 + 热豆腐块 + 豆干 三样同时带在身上。
    # mryt233 改（2026-11-06）：任务单原本要「冷豆腐、热豆腐、冻豆腐、豆干」四样，但冻豆腐（frozen_tofu_cube）
    # 还没做，引用不存在的物品会让整条进度 JSON 解析失败。先用现有三样兜底（三组都要满足才解锁），
    # 等冻豆腐做好后，在下面的 has_any 里补上 "frozen_tofu_cube" 并把 requirements 改成那一条即可。
    ("tofu_quartet", "tofu_overcook", "dried_tofu", "goal", False,
     {"cold": has_any("cold_tofu_cube"),
      "hot": has_any("hot_tofu_cube"),
      "dried": has_any("dried_tofu")},
     [["cold"], ["hot"], ["dried"]]),
    # 爽脆三绝：手里有毛肚 / 鸭肠 / 黄喉里的任意一样。任务单要的三样里鸭肠、黄喉还没做，
    # 先用已实装的熟毛肚兜底；鸭肠黄喉上线后在 has_any 里加两个物品 ID 即可（没写进 JSON 就不会报错）。
    ("crispy_trio", "tripe", "cooked_tripe", "task", False,
     {"any": has_any("cooked_tripe")}),
    # 整装掌勺：穿上布围裙（mryt233 改 2026-11-06，r139：布围裙已实装，图标换成布围裙本身；
    # 穿戴逻辑在 ClothApronProtection.tickOne，穿上即 fire("wear_apron")）。
    ("chef_apron", "root", "cloth_apron", "task", False,
     {"apron": event("wear_apron")}),
    # 西南之魂：吃下一片折耳根（mryt233 改 2026-11-06，r139：折耳根已实装，图标换成折耳根段；
    # 吃的时候由 LivingEntityEatMixin fire("eat_zhe_er_gen")，整根 / 段 / 熟段都算）。
    ("soul_of_southwest", "first_cooked", "zhe_er_gen_slice", "task", False,
     {"zhe": event("eat_zhe_er_gen")}),
    # 围炉而坐：坐在条凳上吃火锅。判定在 LivingEntityEatMixin（骑在一格高条凳的座位上吃东西时 fire），
    # 和「坐着吃」（seated，骑任何座位都算）错开：这条只认一格高条凳（<wood>_bench）。
    ("warm_gathering", "seated", "oak_bench", "goal", False,
     {"bench": event("eat_seated_bench")}),
]


def main():
    if OUT.exists():
        for old in OUT.glob("*.json"):
            old.unlink()
    if not ADVANCEMENTS:
        print("no advancements yet")
        return
    OUT.mkdir(parents=True, exist_ok=True)
    for entry in ADVANCEMENTS:
        name, parent, icon, frame, hidden, criteria = entry[:6]
        # 第 7 项可选：自定义 requirements（分组列表，每组的条件满足任一即算这组通过）。
        # 不写就是「全部条件任满足一个即可」，写 [["a"], ["b"]] 这种就是「两组都要」。
        requirements = entry[6] if len(entry) > 6 else [list(criteria)]
        key = f"advancements.{MODID}.{name}"
        display = {"icon": {"id": item(icon)}, "title": {"translate": key + ".title"},
                   "description": {"translate": key + ".description"}, "frame": frame,
                   "show_toast": True, "announce_to_chat": True, "hidden": hidden}
        adv = {"display": display, "criteria": criteria, "requirements": requirements}
        if parent:
            adv["parent"] = f"{MODID}:{parent}"
        else:
            display["background"] = "minecraft:textures/block/bricks.png"
            display["announce_to_chat"] = False
        (OUT / f"{name}.json").write_text(json.dumps(adv, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print("ok", len(ADVANCEMENTS))


if __name__ == "__main__":
    main()
