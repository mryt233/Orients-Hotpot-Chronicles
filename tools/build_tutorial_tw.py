# -*- coding: utf-8 -*-
"""作者：。生成經用語校訂的 zh_tw 單檔指南。

在 mod/ 下執行：python tools/build_tutorial_tw.py
正文獨立維護；原版名稱取本機 Minecraft 1.21.1 繁中語言檔的裁剪副本。
"""
from pathlib import Path
from collections import Counter
import json
import re

import build_tutorial as guide
from build_tutorial_en import check_translation

TUTORIAL = Path(guide.TUT)
OUTPUT = "Minecraft_Orients_Hotpot_Chronicles_Player_Guide_zh_tw.html"
PAGES = [
    ("index", "歡迎", "開始遊玩"), ("install", "下載、安裝與問題回報", "開始遊玩"),
    ("quickstart", "十分鐘吃上火鍋", "開始遊玩"),
    ("hotpot", "灶台與火鍋", "玩法教學"), ("tables", "桌子與凳子", "玩法教學"),
    ("tableware", "盤子、碗與飲料", "玩法教學"), ("ingredients", "食材與切菜", "玩法教學"),
    ("sauce", "沾醬", "玩法教學"), ("faq", "常見問題", "玩法教學"),
    ("recipes", "合成配方總覽", "資料查詢"), ("items", "物品圖鑑", "資料查詢"),
    ("changelog", "更新紀錄", "資料查詢"),
]
UI = {
    "我的世界[之晗的火锅物语]模组-游玩教程": "Minecraft［之晗的火鍋物語］模組－遊玩指南 · zh_tw",
    "我的世界[之晗的火锅物语]模组": "之晗的火鍋物語",
    "本教程随模组版本更新 · 配方与图鉴由游戏数据自动生成 · 作者：":
        "隨模組版本更新 · 配方與圖鑑依遊戲資料自動產生 · 中文原文及 zh_tw 在地化",
    "这一页由脚本<b>直接读游戏里的配方文件</b>画出来，和游戏永远一致。鼠标放到格子上能看到物品名。":
        "本頁<b>直接依遊戲內的配方檔案產生</b>，方便對照製作。將滑鼠游標移到格子上，就能查看物品名稱。",
    "11 种木头的火锅桌、高火锅桌、板凳、条凳、蘸料台配方都一样，只是把木板换成对应的木板，这里只画<b>橡木</b>的。盘子五种材质（石英、竹、红陶、铜、铁）的<b>小碟</b>各自单列，大盘子只画石英的，其他材质把小碟换成对应材质的小碟。":
        "11 種木材的火鍋桌、高火鍋桌、矮凳、長凳與醬料台共用相同配方，只需換成對應木材的木材方塊；此處以<b>橡木</b>示範。盤子的五種材質（石英、竹、紅陶、銅、鐵）各列出<b>小碟</b>配方，大盤則以石英為例，其他材質請換用同材質的小碟。",
    "这些生食材放进熔炉、烟熏炉、营火都能烤熟，<b>丢进火锅里涮</b>也一样（锅里用烟熏炉的时间，再按火力和汤底折算，见「灶台与火锅」）。":
        "這些生食材可以用熔爐、煙燻爐或營火烹調，也能<b>放入火鍋煮熟</b>。火鍋以煙燻爐的時間為基準，再依火力與湯底倍率換算，詳見「灶台與火鍋」。",
    "模组里所有物品和方块（自动生成）。<code>ID</code> 是指令里用的名字，例如 ":
        "模組內的所有物品與方塊（自動產生）。名稱依遊戲內 zh_tw 語言檔顯示。<code>ID</code> 是指令使用的識別名稱，例如 ",
    "0.1.4 将更新的内容（本教程基于 0.1.3-alpha，这部分还没有发布）":
        "0.1.4 更新預告（本指南以 0.1.3-alpha 為準，以下內容尚未發布）",
    "烹饪（熔炉 / 烟熏炉 / 营火；火锅里按烟熏炉的时间算）": "烹調（熔爐／煙燻爐／營火；火鍋以煙燻爐時間為基準）",
    "烹饪（生 → 熟）": "烹調（生食材 → 熟食）", "木头 / 材质省略规则": "木材與材質變體的顯示方式",
    "灶台、锅与筷子": "灶台、鍋具與筷子", "火锅桌与凳子": "火鍋桌與凳子",
    "厨具与食材": "廚具與食材", "汤碗与饮料": "湯碗與飲料", "灶台与锅": "灶台與鍋具",
    "合成（无序，随便摆）": "不定形合成（材料位置不限）", "合成": "合成",
    "搜索（回车跳到下一处）": "搜尋（按 Enter 跳至下一筆）",
    "0.1.4 将更新": "0.1.4 更新預告", "配方大全": "合成配方總覽", "物品图鉴": "物品圖鑑",
    "小提示": "小提醒", "注意": "注意事項", "切石机": "切石機", "烟熏炉": "煙燻爐", "熔炉": "熔爐",
    "营火": "營火", "<h2>蘸料台</h2>": "<h2>醬料台</h2>", "火锅桌": "火鍋桌", "凳子": "凳子", "盘子": "盤子",
    "工具": "工具", "食材": "食材", "任意原木": "任意原木", "游玩教程": "遊玩指南",
    "反馈：": "問題回報：", "QQ 群": "QQ 交流群", "↑ 回到顶部": "↑ 回到頂端",
    'aria-label="目录"': 'aria-label="目錄"', 'lang="zh-CN"': 'lang="zh-TW"',
    " 秒": " 秒", " 种": " 種",
}


def localize(text):
    # 僅處理生成器固定介面，不改動物品 ID、連結或正文維護標記。
    for source in sorted(UI, key=len, reverse=True):
        text = text.replace(source, UI[source])
    if "</style>" in text:
        text = text.replace("</style>", """
.brand{white-space:normal;min-width:0;line-height:1.4;flex-wrap:wrap}
.rtitle{flex-wrap:wrap}.it{white-space:normal;max-width:100%}.it .ic{flex:none}
.cookgrid{grid-template-columns:repeat(auto-fill,minmax(min(100%,330px),1fr))}
.cards{grid-template-columns:repeat(auto-fill,minmax(min(100%,240px),1fr))}
@media(max-width:820px){.top{flex-wrap:wrap;gap:8px}.brand{flex:1;font-size:14px}
#q{width:100%;max-width:none}nav{top:106px}.chap,h2{scroll-margin-top:120px}}
</style>""")
    return text


def main():
    guide.PAGES = PAGES
    lang = json.loads((Path(guide.RES) / "assets" / guide.MOD / "lang/zh_tw.json").read_text(encoding="utf-8"))
    guide.NAMES = {guide.MOD + ":" + key.split(".", 2)[2]: value for key, value in lang.items()
                   if key.startswith(("item." + guide.MOD + ".", "block." + guide.MOD + "."))}
    guide.VZ = json.loads((TUTORIAL / "data/vanilla_zh_tw.json").read_text(encoding="utf-8"))
    guide.TAG_ICON["minecraft:logs"] = ("minecraft:oak_log", "任意原木")
    guide.BADGE = localize(guide.BADGE)
    bodies = {}
    for slug, _, _ in PAGES:
        if slug == "recipes":
            bodies[slug] = localize(guide.build_recipes_page())
        elif slug == "items":
            bodies[slug] = localize(guide.build_items_page())
        else:
            text = (TUTORIAL / "content-zh-tw" / (slug + ".md")).read_text(encoding="utf-8")
            check_translation(slug, text)
            original = (TUTORIAL / "content" / (slug + ".md")).read_text(encoding="utf-8")
            numbers = lambda value: Counter(re.findall(r"\d+(?:\.\d+)*", value))
            if numbers(original) != numbers(text):
                raise ValueError(f"Numbers differ from source in {slug}")
            bodies[slug] = localize(guide.render_md(text))
    if guide.warnings:
        raise ValueError("\n".join(sorted(set(guide.warnings))))
    guide.build_single(bodies, filename=OUTPUT, transform=localize)
    result = (TUTORIAL / OUTPUT).read_text(encoding="utf-8")
    ids = re.findall(r'\bid="([^"]+)"', result)
    missing = set(re.findall(r'href="#([^"]+)"', result)) - set(ids)
    if len(ids) != len(set(ids)) or missing:
        raise ValueError(f"Duplicate or missing anchors: {missing}")
    if any(term in result for term in ("台灣", "台湾", "繁體中文", "繁体中文")):
        raise ValueError("Use zh_tw for public language labels")
    print(f"Validated {len(bodies)} zh_tw chapters; no missing assets or anchors.")


if __name__ == "__main__":
    main()
