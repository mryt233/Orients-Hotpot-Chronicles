# -*- coding: utf-8 -*-
"""作者：。复用中文教程的配方、图片和页面布局，生成独立英文单文件。

在 mod/ 下运行：python tools/build_tutorial_en.py
"""
from collections import Counter
from pathlib import Path
import hashlib
import json
import re

import build_tutorial as guide

TUTORIAL = Path(guide.TUT)
ENGLISH = TUTORIAL / "content-en"
OUTPUT = "Minecraft_Orients_Hotpot_Chronicles_Player_Guide_EN.html"

PAGES = [
    ("index", "Welcome", "Getting Started"),
    ("install", "Downloads, Installation & Feedback", "Getting Started"),
    ("quickstart", "Your First Hotpot in Ten Minutes", "Getting Started"),
    ("hotpot", "Stoves & Hotpots", "Gameplay"),
    ("tables", "Tables & Seating", "Gameplay"),
    ("tableware", "Tableware & Drinks", "Gameplay"),
    ("ingredients", "Ingredients & Food Preparation", "Gameplay"),
    ("sauce", "Dipping Sauces", "Gameplay"),
    ("faq", "Frequently Asked Questions", "Gameplay"),
    ("recipes", "Recipe Collection", "Reference"),
    ("items", "Item Catalog", "Reference"),
    ("changelog", "Changelog", "Reference"),
]

# 只替换原生成器的固定界面文字；正文由 content-en 完整翻译。
UI = {
    "我的世界[之晗的火锅物语]模组-游玩教程": "Minecraft [Orient's Hotpot Chronicles] Mod — Player Guide",
    "我的世界[之晗的火锅物语]模组": "Orient's Hotpot Chronicles",
    "本教程随模组版本更新 · 配方与图鉴由游戏数据自动生成 · 作者：":
        "Updated with the mod · Recipes and catalog generated from game data · Guide and English translation",
    "这一页由脚本<b>直接读游戏里的配方文件</b>画出来，和游戏永远一致。鼠标放到格子上能看到物品名。":
        "These diagrams are generated <b>directly from the game's recipe files</b> to match the game. Hover over a slot to see its item name.",
    "11 种木头的火锅桌、高火锅桌、板凳、条凳、蘸料台配方都一样，只是把木板换成对应的木板，这里只画<b>橡木</b>的。盘子五种材质（石英、竹、红陶、铜、铁）的<b>小碟</b>各自单列，大盘子只画石英的，其他材质把小碟换成对应材质的小碟。":
        "Hotpot tables, tall tables, stools, benches, and sauce stations share recipes across all 11 woods; just use matching planks. Only <b>oak</b> examples are shown. <b>Small dishes</b> are listed for all five materials (quartz, bamboo, terracotta, copper, iron). Larger plates show quartz examples; substitute matching dishes for other materials.",
    "这些生食材放进熔炉、烟熏炉、营火都能烤熟，<b>丢进火锅里涮</b>也一样（锅里用烟熏炉的时间，再按火力和汤底折算，见「灶台与火锅」）。":
        "Cook these raw ingredients in a furnace, smoker, or campfire, or <b>add them to a hotpot</b>. Pots use smoker times adjusted by heat and broth; see Stoves &amp; Hotpots.",
    "模组里所有物品和方块（自动生成）。<code>ID</code> 是指令里用的名字，例如 ":
        "All items and blocks in the mod (automatically generated). <code>ID</code> is the name used in commands, for example ",
    "0.1.4 将更新的内容（本教程基于 0.1.3-alpha，这部分还没有发布）":
        "Coming in 0.1.4 (this guide covers 0.1.3-alpha; this content is not released yet)",
    "烹饪（熔炉 / 烟熏炉 / 营火；火锅里按烟熏炉的时间算）":
        "Cooking (furnace / smoker / campfire; hotpots use smoker times)",
    "烹饪（生 → 熟）": "Cooking (raw → cooked)",
    "木头 / 材质省略规则": "Wood and material variants",
    "灶台、锅与筷子": "Stoves, Pots &amp; Chopsticks",
    "火锅桌与凳子": "Tables &amp; Seating",
    "厨具与食材": "Kitchenware &amp; Ingredients",
    "汤碗与饮料": "Soup Bowls &amp; Drinks",
    "灶台与锅": "Stoves &amp; Pots",
    "合成（无序，随便摆）": "Crafting (shapeless; any arrangement)",
    "搜索（回车跳到下一处）": "Search (Enter for next match)",
    "0.1.4 将更新": "Coming in 0.1.4",
    "配方大全": "Recipe Collection", "物品图鉴": "Item Catalog",
    "小提示": "Tip", "注意": "Note", "合成": "Crafting", "切石机": "Stonecutter",
    "烟熏炉": "Smoker", "熔炉": "Furnace", "营火": "Campfire",
    "蘸料台": "Sauce Stations", "火锅桌": "Hotpot Tables", "凳子": "Seating",
    "盘子": "Plates", "工具": "Tools", "食材": "Ingredients",
    "任意原木": "Any Log", "游玩教程": "Player Guide", "反馈：": "Feedback: ",
    "QQ 群": "QQ group", "↑ 回到顶部": "↑ Back to top", 'aria-label="目录"': 'aria-label="Contents"',
    'lang="zh-CN"': 'lang="en"', " 秒": " s", " 种": " types",
}


def localize(text):
    for source in sorted(UI, key=len, reverse=True):
        text = text.replace(source, UI[source])
    text = re.sub(r'(<span class="tag since">)自 ', r'\1Since ', text)
    # 英文标签更长，窄屏允许换行；不改中文页面的公共样式。
    if "</style>" in text:
        text = text.replace("</style>", """
.brand{white-space:normal;min-width:0;line-height:1.4;flex-wrap:wrap}
.rtitle{flex-wrap:wrap}.it{white-space:normal;max-width:100%}.it .ic{flex:none}
.cookgrid{grid-template-columns:repeat(auto-fill,minmax(min(100%,330px),1fr))}
.cards{grid-template-columns:repeat(auto-fill,minmax(min(100%,240px),1fr))}
@media(max-width:820px){.top{flex-wrap:wrap;gap:8px}.brand{flex:1;font-size:14px}
#q{width:100%;max-width:none}nav{top:106px}.rtitle{font-size:15px}}
</style>""")
    return text


def check_translation(slug, english):
    """核对引用、配方、图片、版本标记及显式锚点，避免翻译漏项。"""
    chinese = (TUTORIAL / "content" / (slug + ".md")).read_text(encoding="utf-8")
    patterns = [r"\{\{[^}]+\}\}", r"\{#[\w-]+\}", r"\]\(([^)]+)\)", r"清单#\d+"]
    # 图注需翻译，只比较图片路径。
    def refs(text, pattern):
        return Counter(re.sub(r"(\{\{img:[^|}]+)\|.*?\}\}", r"\1}}", value)
                       for value in re.findall(pattern, text))
    for pattern in patterns:
        if refs(chinese, pattern) != refs(english, pattern):
            raise ValueError(f"Translation reference mismatch in {slug}: {pattern}")
    if len(re.findall(r"^#{1,3} ", chinese, re.M)) != len(re.findall(r"^#{1,3} ", english, re.M)):
        raise ValueError(f"Translation heading count mismatch in {slug}")


def main():
    guide.PAGES = PAGES
    lang = json.loads((Path(guide.RES) / "assets" / guide.MOD / "lang" / "en_us.json").read_text(encoding="utf-8"))
    guide.NAMES = {guide.MOD + ":" + key.split(".", 2)[2]: value
                   for key, value in lang.items() if key.startswith(("item." + guide.MOD + ".", "block." + guide.MOD + "."))}
    guide.LANG = lang
    # 原版 ID 的常规英文名；特殊显示名与 Minecraft 英文名称对齐。
    guide.VZ = {key: key.split(":")[1].replace("_", " ").title() for key in guide.VZ}
    guide.VZ.update({"minecraft:beef": "Raw Beef", "minecraft:mutton": "Raw Mutton",
                     "minecraft:quartz": "Nether Quartz"})
    guide.TAG_ICON["minecraft:logs"] = ("minecraft:oak_log", "Any Log")
    guide.BADGE = localize(guide.BADGE)
    bodies = {}
    source_hashes = {}
    for slug, _, _ in PAGES:
        if slug == "recipes":
            bodies[slug] = localize(guide.build_recipes_page())
        elif slug == "items":
            bodies[slug] = localize(guide.build_items_page())
        else:
            path = ENGLISH / (slug + ".md")
            text = path.read_text(encoding="utf-8")
            check_translation(slug, text)
            bodies[slug] = localize(guide.render_md(text))
            source_hashes[slug] = hashlib.sha256((TUTORIAL / "content" / path.name).read_bytes()).hexdigest()
    if guide.warnings:
        raise ValueError("\n".join(sorted(set(guide.warnings))))
    guide.build_single(bodies, filename=OUTPUT, transform=localize)
    result = (TUTORIAL / OUTPUT).read_text(encoding="utf-8")
    ids = re.findall(r'\bid="([^"]+)"', result)
    if len(ids) != len(set(ids)):
        raise ValueError("Duplicate HTML IDs")
    missing = set(re.findall(r'href="#([^"]+)"', result)) - set(ids)
    if missing:
        raise ValueError(f"Missing anchors: {missing}")
    # 记录翻译所依据的中文版本，以便后续更新时核对。
    (ENGLISH / "source-hashes.json").write_text(json.dumps({"author": "", "sha256": source_hashes}, indent=2) + "\n", encoding="utf-8")
    print(f"Validated {len(bodies)} chapters, {len(ids)} HTML IDs; no missing assets or anchors.")


if __name__ == "__main__":
    main()
