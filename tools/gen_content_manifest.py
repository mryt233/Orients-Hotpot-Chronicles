"""从代码和资源生成内容清单 mod/content/items.json。

用途：内容清单是「一条内容应该有哪些文件」的唯一记录。手工维护 60 多个物品必然漏，
所以这里从实际代码扫出物品 ID，再从配方文件反查每个物品的产物配方，生成清单初稿；
之后清单就是准绳，改内容时先改清单、再跑 check_content.py 核对。

生成规则：
    id       HotPotContent 里注册到 Registries.ITEM 的全部 ID（含方块物品）
    block    该物品是不是方块物品（有对应的 models/block 模型）
    food     foodItem(...) 注册的食物
    recipes  以它作为产物的配方文件
    guide    当前 guide_content.py 里讲到了没有

用法：
    python mod/tools/gen_content_manifest.py          # 写回 content/items.json
    python mod/tools/gen_content_manifest.py --check  # 只报告差异，不写文件
作者：mryt233
"""
from __future__ import annotations

import json
import re
import sys
from pathlib import Path

MOD = Path(__file__).resolve().parent.parent
MANIFEST = MOD / "content/items.json"
ASSETS = MOD / "common/src/main/resources/assets/orients_hotpot_chronicles"
DATA = MOD / "common/src/main/resources/data/orients_hotpot_chronicles"
RECIPES = DATA / "recipe"
CONTENT_JAVA = MOD / "common/src/main/java/cn/blockforge/generated/orientshotpotchronicles/hotpot/HotPotContent.java"
GUIDE = MOD / "tools/guide_content.py"
NS = "orients_hotpot_chronicles"

# 手工补充：产物不在本物品名下、但属于这条链的配方（如砧板切、烧炼）
EXTRA_RECIPES = {
    "raw_ham_sausage": ["raw_ham_sausage", "ham_sausage", "ham_sausage_from_smoking",
                        "ham_sausage_from_campfire_cooking"],
    "ham_sausage": ["ham_sausage_slice_from_stonecutting"],
    "ham_sausage_slice": ["cooked_ham_sausage_slice", "cooked_ham_sausage_slice_from_smoking",
                          "cooked_ham_sausage_slice_from_campfire_cooking"],
    "minced_pork": ["minced_pork_from_stonecutting"],
}


def registered_items(src: str) -> list[str]:
    """取出所有注册到 Registries.ITEM 的 ID（允许跨行）。

    来源四路取并集，避免漏掉用 helper / 枚举 / 循环注册的物品：
      1. 直接的 Registry.register(Registries.ITEM, HotPot.id("x"))
      2. foodItem("x") —— 内部自己注册
      3. SoupBase 的 bowlId（switch 里的汤碗 ID）
      4. textures/item 下的贴图名（非方块物品必有贴图）
    """
    ids = set(re.findall(r"Registries\.ITEM,\s*HotPot\.id\(\"([a-z0-9_]+)\"\)", src))
    # P1 第三批起走分域 Registrar：reg().items().register(HotPot.id("x"), ...)
    ids |= set(re.findall(r'\.items\(\)\.register\(\s*HotPot\.id\("([a-z0-9_]+)"\)', src))
    ids |= set(re.findall(r'foodItem\("([a-z0-9_]+)"', src))
    soup = (MOD / "common/src/main/java/cn/blockforge/generated/orientshotpotchronicles/hotpot/SoupBase.java")
    if soup.is_file():
        text = soup.read_text(encoding="utf-8")
        # mryt233 改（2026-11-10，r147）：碗 ID 从 bowlId() 的 switch 搬到了枚举构造参数
        # （第二个参数就是碗物品 ID）。旧写法（switch）仍兼容，两种都能扫出来。
        ids |= set(re.findall(r'^\s*[A-Z_]+\("soup\.[a-z0-9_]+",\s*"([a-z0-9_]+)"', text, re.M))
        for block in re.findall(r"public String bowlId\(\)\s*\{(.*?)\n    \}", text, re.S):
            ids |= set(re.findall(r'->\s*"([a-z0-9_]+)"', block))
    ids |= {p.stem for p in (ASSETS / "textures/item").glob("*.png")}
    return sorted(ids)


def food_items(src: str) -> set[str]:
    return set(re.findall(r'foodItem\("([a-z0-9_]+)"', src))


def recipe_results() -> dict[str, list[str]]:
    """产物 ID → [配方文件名]，扫描 data/<ns>/recipe/*.json。"""
    out: dict[str, list[str]] = {}
    for path in sorted(RECIPES.glob("*.json")):
        try:
            data = json.loads(path.read_text(encoding="utf-8"))
        except json.JSONDecodeError:
            continue
        result = data.get("result")
        rid = None
        if isinstance(result, dict):
            rid = str(result.get("id", "")).split(":")[-1] or None
        elif isinstance(result, str):
            rid = result.split(":")[-1]
        if rid:
            out.setdefault(rid, []).append(path.stem)
    return out


def food_numbers(src: str) -> dict[str, tuple[int, float]]:
    """`foodItem("id", 饥饿, 饱和度系数)` → {id: (饥饿, 饱和度系数)}。"""
    out = {}
    for m in re.finditer(r'foodItem\("([a-z0-9_]+)",\s*(\d+),\s*([0-9.]+)F?\)', src):
        out[m.group(1)] = (int(m.group(2)), float(m.group(3)))
    return out


def cooking_times() -> dict[str, int]:
    """产物 ID → 烧炼 / 烟熏 / 营火配方的 cookingtime（取第一个能找到的）。"""
    out: dict[str, int] = {}
    for path in sorted(RECIPES.glob("*.json")):
        try:
            data = json.loads(path.read_text(encoding="utf-8"))
        except json.JSONDecodeError:
            continue
        if not str(data.get("type", "")).startswith("minecraft:") or "cooking" not in str(data.get("type")):
            continue
        result = data.get("result")
        rid = (result.get("id") if isinstance(result, dict) else result) or ""
        rid = str(rid).split(":")[-1]
        ticks = data.get("cookingtime")
        if rid and isinstance(ticks, int):
            out[rid] = min(ticks, out.get(rid, ticks))
    return out


def build_entry(item_id: str, foods: set[str], numbers: dict, cook: dict, results: dict, guide_src: str) -> dict:
    entry: dict = {"id": item_id, "block": (ASSETS / "models/block" / f"{item_id}.json").is_file(),
                   "food": item_id in foods}
    if item_id in numbers:
        hunger, saturation = numbers[item_id]
        entry["hunger"] = hunger
        entry["saturation"] = saturation
    if item_id in cook:
        entry["cook_ticks"] = cook[item_id]
    entry["recipes"] = sorted(set(results.get(item_id, [])) | set(EXTRA_RECIPES.get(item_id, [])))
    entry["guide"] = bool(re.search(rf"['\"]{re.escape(item_id)}['\"]", guide_src))
    return entry


def main() -> int:
    check_only = "--check" in sys.argv
    src = CONTENT_JAVA.read_text(encoding="utf-8")
    ids = registered_items(src)
    foods = food_items(src)
    numbers = food_numbers(src)
    cook = cooking_times()
    results = recipe_results()
    guide_src = GUIDE.read_text(encoding="utf-8", errors="replace")
    langs = {p.stem for p in (ASSETS / "lang").glob("*.json")}

    # 清单里**已有的条目原样保留**（hunger / saturation 以清单为准，可手工改），
    # 只补新物品、删掉代码里已经没有的物品——否则每次重跑都会把手工调的数值冲掉。
    previous: dict[str, dict] = {}
    if MANIFEST.is_file():
        try:
            previous = {i["id"]: i for i in json.loads(MANIFEST.read_text(encoding="utf-8"))["items"]}
        except (ValueError, KeyError):
            previous = {}

    items = []
    for item_id in ids:
        if item_id in previous:
            entry = dict(previous[item_id])
            # 结构字段（是不是方块 / 食物、配方、指南）仍按代码刷新，数值保持清单里的
            fresh = build_entry(item_id, foods, numbers, cook, results, guide_src)
            for key in ("hunger", "saturation"):
                if key in previous[item_id]:
                    fresh[key] = previous[item_id][key]
            entry = fresh
        else:
            entry = build_entry(item_id, foods, numbers, cook, results, guide_src)
        items.append(entry)

    added = [i["id"] for i in items if i["id"] not in previous]
    removed = [k for k in previous if k not in ids]

    manifest = {
        "_说明": (
            f"内容清单（mryt233 建，gen_content_manifest.py 生成）：{len(items)} 个物品。"
            "每条记录描述一个物品，check_content.py 用它核对贴图 / 模型 / 配方 / 语言 / 指南是否齐全，"
            "并核对 hunger / saturation 与代码是否一致。改数值可以直接改这里的 hunger / saturation，"
            "再跑 gen_food_numbers.py --apply 写回代码。"
        ),
        "_字段": {
            "id": "物品 ID（不含命名空间）",
            "block": "是不是方块物品（有 block 模型）",
            "food": "是不是食物（foodItem 注册）",
            "hunger": "食物饥饿点数（2 点 = 一个完整图标）",
            "saturation": "饱和度系数（注意不是饱和点数；实际饱和点数 = 2 × 饥饿 × 系数）",
            "cook_ticks": "烧炼 / 烟熏 / 营火的 cookingtime（20 tick = 1 秒）",
            "recipes": "这条链用到的配方文件名（data/<ns>/recipe/<名字>.json，不含 .json）",
            "guide": "游戏内指南里有没有讲到（guide_content.py 里出现这个 ID）",
        },
        "items": items,
    }
    text = json.dumps(manifest, ensure_ascii=False, indent=2) + "\n"

    if check_only:
        current = MANIFEST.read_text(encoding="utf-8") if MANIFEST.is_file() else ""
        if current == text:
            print(f"清单已是最新：{len(items)} 个物品。")
            return 0
        print("清单与代码不同步，请运行 gen_content_manifest.py 更新：")
        if added:
            print(f"  代码里有、清单里没有：{', '.join(added)}")
        if removed:
            print(f"  清单里有、代码里已经没有了：{', '.join(removed)}")
        if not added and not removed:
            print("  物品没变，但结构字段（方块 / 食物 / 配方 / 指南）有刷新")
        return 1

    MANIFEST.parent.mkdir(parents=True, exist_ok=True)
    MANIFEST.write_bytes(text.encode("utf-8"))
    print(f"已写入 {MANIFEST.relative_to(MOD.parent)}：{len(items)} 个物品"
          f"（方块 {sum(1 for i in items if i['block'])}，食物 {sum(1 for i in items if i['food'])}），"
          f"语言文件 {len(langs)} 个。")
    if added:
        print(f"  新增：{', '.join(added)}")
    if removed:
        print(f"  移除：{', '.join(removed)}")
    print("  提示：hunger / saturation 以清单为准，改完跑 gen_food_numbers.py --apply 写回代码。")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
