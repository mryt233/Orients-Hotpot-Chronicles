# -*- coding: utf-8 -*-
"""
把玩家教程排成游戏内「火锅指南」那本书的样子（摊开的红皮书、左边分类书签、顶上条目书签、书角三角翻页），
输出一个单文件 HTML。正文、配方图、物品图标照旧来自 content/*.md 和 build_tutorial.py（只换外观）。
作者：mryt233

现在是样稿：只排「入门」「蘸料」两个分类，其他分类的书签先占位（用户要先看样式，确认后再全部铺开）。

用法（在 mod/ 下）：python tools/build_tutorial_book.py
输出：tutorial/我的世界[之晗的火锅物语]模组-游玩教程（指南样式样稿）.html
贴图：直接用游戏的 textures/gui/guide_book.png（书签、物品格、箭头、翻页三角），书皮和纸页按 tools/gen_guide_gui.py 的颜色用 CSS 画。
"""
import base64
import glob
import html
import json
import mimetypes
import os
import re
import sys

sys.path.insert(0, os.path.dirname(__file__))
import build_tutorial as bt  # noqa: E402

TUT = bt.TUT
OUT_NAME = "我的世界[之晗的火锅物语]模组-游玩教程（书本样式）.html"
OUT = os.path.join(TUT, OUT_NAME)
CUSTOM_OUT = False
TEX = os.path.join(bt.RES, "assets", bt.MOD, "textures")
VANILLA_ITEM_TEX = os.path.join(bt.ROOT, "..", "viewer", "cache", "1.21.1", "assets", "minecraft", "textures", "item")


def read_gradle_versions():
    """mryt233 改（2026-11-06）：从 gradle.properties 读取当前 MC 版本与模组版本，使书本教程按版本自动归档保留。"""
    props = os.path.join(bt.ROOT, "gradle.properties")
    mc, ver = "1.21.1", "0.2.2-alpha"
    if os.path.exists(props):
        text = open(props, encoding="utf-8").read()
        m_mc = re.search(r"^minecraft_version\s*=\s*(\S+)", text, re.M)
        m_ver = re.search(r"^mod_version\s*=\s*(\S+)", text, re.M)
        if m_mc:
            mc = m_mc.group(1)
        if m_ver:
            ver = m_ver.group(1)
    return mc, ver


MC_VERSION, MOD_VERSION = read_gradle_versions()

# 分类（左边书签）：(标题, 图标, 内容来源)；内容来源是 [(content 文件名, 是否按二级标题拆成条目)]，空 = 样稿里还没排
CATEGORIES = [
    ("入门", "hotpot_guide", [("index", True), ("quickstart", False)]),
    ("火锅", "tomato_boiling_pot", [("hotpot", True)]),   # 图标：煮着番茄汤、沸腾的铁锅（查看器逐帧渲染的动图，用户 2026-10-27）
    ("食材", "iron_kitchen_knife", [("ingredients", True)]),
    ("蘸料", "oak_sauce_station", [("sauce", True)]),
    ("餐具", "bamboo_chopsticks", [("tableware", True)]),
    ("桌凳", "oak_bench", [("tables", True)]),   # 图标：条凳（用户 2026-10-27）
    ("查阅", "minecraft:knowledge_book", [("@recipes", True), ("@items", True)]),
    ("开发版预览", "shrimp", [("preview", False)]),      # 用户 2026-10-27：单独一个大类书签，放在查阅（合成表）下面
    ("关于", "minecraft:writable_book", [("install", True), ("faq", False), ("@changelog", True)]),
]
PLANNED = {  # 占位分类以后会放进来的内容（样稿里写在占位页上）
    "火锅": "灶台、燃料、锅、汤底、下菜和捞菜、火锅界面、篝火、鸳鸯锅",
    "食材": "毛肚、牛脊肉和吊龙、砧板 / 菜刀 / 擀面杖、切菜、丸子、豆腐、小葱香菜",
    "餐具": "盘子、碗和汤碗、饮料、竹筷",
    "桌凳": "矮火锅桌、高火锅桌、板凳、条凳",
    "关于": "下载与安装、反馈和交流、常见问题、更新日志、开发版预览",
    "查阅": "物品图鉴",
}
# 条目图标：(文件, 第几段) → 图标；没写的取这一段里第一个提到的物品，再没有就用分类图标
ENTRY_ICONS = {
    ("index", 0): "hotpot_guide",
    ("index", 1): "iron_nine_grid_hotpot",
    ("index", 2): "minecraft:writable_book",
    ("quickstart", 0): "brick_stove",
    ("sauce", 0): "dipping_sauce",
    ("sauce", 2): "millet_chili",   # mryt233 改，2026-11-06：这一节现在是「14 种小料」，图标换成小米辣
    ("install", 0): "minecraft:writable_book",
    ("install", 1): "minecraft:name_tag",
    ("install", 2): "minecraft:map",
    ("install", 3): "minecraft:paper",
    ("install", 4): "minecraft:spyglass",
    ("install", 5): "minecraft:feather",
    ("faq", 0): "minecraft:book",
    ("preview", 0): "minecraft:spyglass",
    ("preview", 7): "hotpot_guide",
    ("preview", 8): "bamboo_chopsticks",
    ("preview", 9): "garlic_paste",
    ("hotpot", 0): "tomato_boiling_pot",
    ("hotpot", 2): "copper_nine_grid_hotpot",
    ("hotpot", 4): "minecraft:campfire",
    ("hotpot", 5): "minecraft:golden_apple",
    ("hotpot", 7): "minecraft:iron_pickaxe",
    ("ingredients", 0): "iron_kitchen_knife",
    ("ingredients", 1): "tripe",
    ("ingredients", 3): "cutting_board",
    ("ingredients", 7): "ham_sausage_slice",   # mryt233 改，2026-11-02：插入火腿肠一节后序号后移，图标跟着改（用户指出）
    ("ingredients", 9): "crispy_pork",           # mryt233 改，2026-11-06：r137 新增 5 节，序号后移，图标同步
    ("ingredients", 10): "dried_tofu",
    ("ingredients", 11): "enoki_mushroom",
    ("ingredients", 12): "egg_fried_rice",
    ("ingredients", 13): "chili_pepper",
    ("ingredients", 14): "zhe_er_gen",          # mryt233 改，2026-11-06：r139 插入折耳根一节，后续序号后移
    ("ingredients", 15): "scallion",
    ("ingredients", 16): "minecraft:food_full",   # 饥饿速查：原版饱食度图标（鸡腿），tutorial/img/icons/minecraft__food_full.png
    ("sauce", 4): "bamboo_chopsticks",
    ("tableware", 0): "quartz_eight_slot_platter",
    ("tableware", 1): "quartz_small_dish",
    ("tableware", 2): "clear_broth_bowl",
    ("tableware", 4): "cloth_apron",
    ("tables", 0): "oak_bench",
}
ENTRY_TITLES = {("index", 0): "欢迎"}

# ---------------- 多语言（mryt233 改，2026-11-02：用户要书本样式出 zh_tw、英文，一个网页里用地球按钮切换） ----------------
# 正文各语言独立维护（content / content-zh-tw / content-en，标题结构一致）；生成器自己写的中文界面文字靠下面的对照表换掉。
LANGS = [("zh_cn", "简体中文"), ("zh_tw", "繁體中文"), ("en", "English")]
LANG = "zh_cn"
T = {}   # 当前语言：生成器里的中文原文 → 译文；简体为空
BOOK_UI = {   # 书本样式自己的界面文字（分类名、合成表分组、目录提示、页面外框、book.js 里的提示）
    "en": {
        "入门": "Getting Started", "火锅": "Hotpot", "食材": "Ingredients", "蘸料": "Dipping Sauces", "餐具": "Tableware",
        "桌凳": "Tables & Seating", "查阅": "Reference", "开发版预览": "Development Preview", "关于": "About", "欢迎": "Welcome",
        "灶台与锅": "Stoves & Pots", "盘子": "Plates", "蘸料台": "Sauce Stations", "厨具与食材": "Kitchenware & Ingredients",
        "切石机": "Stonecutter", "烹饪": "Cooking", "鸳鸯锅 / 田字锅": "Yuanyang / Four-Grid Pots", "香菜丸子": "Cilantro Meatballs",
        "熔炉": "Furnace", "烟熏炉": "Smoker", "营火": "Campfire", "秒": "s", "生 → 熟": "Raw → Cooked", "种": "types",
        "物品图鉴": "Item Catalog", "更新日志": "Changelog", "本章目录": "In this chapter",
        "点下面的条目直接翻过去；也可以点书顶的书签，或者点右下角的三角一页页往后翻。":
            "Click an entry below to jump to it; you can also use the bookmarks along the top, or the triangle at the bottom right to turn page by page.",
        "同一类的配方合成一张卡，材料自动轮换。<b>点卡片下面的材料</b>就停在那一种，右边显示做出来的样子；再点一次恢复轮换。":
            "Recipes of one kind share a card that cycles through the materials. <b>Click a material under the card</b> to stop on it and see the result on the right; click again to resume cycling.",
        "标 <span class=\"tag dev\">开发版预览</span> 的是 %s 之后才加的，正式版里还没有，名字和配方以后可能会变。":
            "Entries marked <span class=\"tag dev\">Development Preview</span> were added after %s and are not in the released build yet; names and recipes may change.",
        "我的世界[之晗的火锅物语]模组-游玩教程": "Minecraft [Orient's Hotpot Chronicles] Mod — Player Guide",
        "翻页音效": "Page-turn sound", "分类": "Categories", "条目": "Entries", "搜索": "Search", "目录": "Contents",
        "上一页": "Previous page", "下一页": "Next page", "关闭": "Close", "语言": "Language",
        "搜索：物品、玩法、配方……": "Search: items, gameplay, recipes…",
        "目录：": "Contents: ", "点一下换个角度": "Click to switch the view angle", "铜制": "Copper ", "铁制": "Iron ",
        "做法：": "How to make: ", "铜锅": "Copper", "铁锅": "Iron",
        "翻页音效：开（点一下关掉）": "Page-turn sound: on (click to turn off)", "翻页音效：关（点一下打开）": "Page-turn sound: off (click to turn on)",
        "输入想找的东西，比如「毛肚」「鸳鸯锅」「蘸料」「配方」。": "Type what you are looking for, e.g. “tripe”, “yuanyang”, “sauce”, or “recipe”.",
        "没有找到「": "Nothing found for “", "」。": "”.",
        "开发版预览（还没正式发布，名字和数值以后可能会变）": "Development Preview (not released yet; names and values may change)",
    },
    "zh_tw": {
        "模组里所有物品和方块（自动生成）。<code>ID</code> 是指令里用的名字，例如 ":
            "模組內的所有物品與方塊（自動產生）。<code>ID</code> 是指令使用的識別名稱，例如 ",   # 不写 zh_tw 字样（用户 2026-11-02）
        "入门": "入門", "火锅": "火鍋", "食材": "食材", "蘸料": "沾醬", "餐具": "餐具", "桌凳": "桌凳", "查阅": "查閱",
        "开发版预览": "開發版預覽", "关于": "關於", "欢迎": "歡迎",
        "灶台与锅": "灶台與鍋具", "盘子": "盤子", "蘸料台": "醬料台", "厨具与食材": "廚具與食材",
        "切石机": "切石機", "烹饪": "烹調", "鸳鸯锅 / 田字锅": "鴛鴦鍋／田字鍋", "香菜丸子": "香菜丸子",
        "熔炉": "熔爐", "烟熏炉": "煙燻爐", "营火": "營火", "秒": "秒", "生 → 熟": "生 → 熟", "种": "種",
        "物品图鉴": "物品圖鑑", "更新日志": "更新日誌", "本章目录": "本章目錄",
        "点下面的条目直接翻过去；也可以点书顶的书签，或者点右下角的三角一页页往后翻。":
            "點下面的條目直接翻過去；也可以點書頂的書籤，或者點右下角的三角一頁頁往後翻。",
        "同一类的配方合成一张卡，材料自动轮换。<b>点卡片下面的材料</b>就停在那一种，右边显示做出来的样子；再点一次恢复轮换。":
            "同一類的配方合成一張卡，材料自動輪換。<b>點卡片下面的材料</b>就停在那一種，右邊顯示做出來的樣子；再點一次恢復輪換。",
        "标 <span class=\"tag dev\">开发版预览</span> 的是 %s 之后才加的，正式版里还没有，名字和配方以后可能会变。":
            "標 <span class=\"tag dev\">開發版預覽</span> 的是 %s 之後才加的，正式版裡還沒有，名稱和配方以後可能會變。",
        "我的世界[之晗的火锅物语]模组-游玩教程": "我的世界[之晗的火鍋物語]模組-遊玩教程",
        "翻页音效": "翻頁音效", "分类": "分類", "条目": "條目", "搜索": "搜尋", "目录": "目錄",
        "上一页": "上一頁", "下一页": "下一頁", "关闭": "關閉", "语言": "語言",
        "搜索：物品、玩法、配方……": "搜尋：物品、玩法、配方……",
        "目录：": "目錄：", "点一下换个角度": "點一下換個角度", "铜制": "銅製", "铁制": "鐵製",
        "做法：": "做法：", "铜锅": "銅鍋", "铁锅": "鐵鍋",
        "翻页音效：开（点一下关掉）": "翻頁音效：開（點一下關掉）", "翻页音效：关（点一下打开）": "翻頁音效：關（點一下打開）",
        "输入想找的东西，比如「毛肚」「鸳鸯锅」「蘸料」「配方」。": "輸入想找的東西，比如「毛肚」「鴛鴦鍋」「沾醬」「配方」。",
        "没有找到「": "沒有找到「", "」。": "」。",
        "开发版预览（还没正式发布，名字和数值以后可能会变）": "開發版預覽（還沒正式釋出，名稱和數值以後可能會變）",
    },
}
# book.js 里用到的界面文字（按简体原文查）
JS_UI = ["目录：", "开发版预览", "点一下换个角度", "铜制", "铁制", "做法：", "铜锅", "铁锅", "翻页音效：开（点一下关掉）",
         "翻页音效：关（点一下打开）", "输入想找的东西，比如「毛肚」「鸳鸯锅」「蘸料」「配方」。", "没有找到「", "」。"]
FRAME_UI = ["我的世界[之晗的火锅物语]模组-游玩教程", "翻页音效", "分类", "条目", "搜索", "目录", "上一页", "下一页", "关闭", "语言",
            "搜索：物品、玩法、配方……"]
POTS_L = {   # 锅形展示的锅名、说明、做法
    "en": {
        "nine_grid_hotpot": ("Nine-Grid Hotpot", "One broth for the whole pot, nine large cells.", "Copper / iron ingots + a cauldron (recipe below)"),
        "taiji_yuanyang_hotpot": ("Taiji Yuanyang Hotpot", "An S-shaped divider like the taiji symbol, one broth on each side.", "Nine-grid pot + 1 ingot to the left of the pot"),
        "round_yuanyang_hotpot": ("Round-Center Yuanyang Hotpot", "A ring in the middle: one broth outside, one inside.", "Nine-grid pot + 1 ingot below the pot"),
        "split_yuanyang_hotpot": ("Straight Yuanyang Hotpot", "A straight divider down the middle, one broth on each side.", "Nine-grid pot + 1 ingot above the pot"),
        "four_grid_hotpot": ("Four-Grid Hotpot", "A cross divider makes four parts, one broth each.", "Nine-grid pot + 2 ingots, one on each side"),
        "square_yuanyang_hotpot": ("Square-Center Yuanyang Hotpot", "Like the round-center pot, but with a square in the middle.", "Nine-grid pot + 2 ingots, above and below"),
    },
    "zh_tw": {
        "nine_grid_hotpot": ("九宮格火鍋", "一整鍋一種湯，9 個大格。", "銅錠／鐵錠 + 鍋釜（配方見下面）"),
        "taiji_yuanyang_hotpot": ("太極鴛鴦鍋", "太極圖那樣的 S 形隔板，兩邊各一種湯。", "九宮格鍋 + 1 塊錠，錠放在鍋的左邊"),
        "round_yuanyang_hotpot": ("圓心鴛鴦鍋", "中間一圈：圈外、圈裡各一種湯。", "九宮格鍋 + 1 塊錠，錠放在鍋的下面"),
        "split_yuanyang_hotpot": ("直隔鴛鴦鍋", "中間一道直隔板，左右各一種湯。", "九宮格鍋 + 1 塊錠，錠放在鍋的上面"),
        "four_grid_hotpot": ("田字火鍋", "十字隔板分成四塊，每塊一種湯。", "九宮格鍋 + 2 塊錠，鍋的左右各一塊"),
        "square_yuanyang_hotpot": ("方心鴛鴦鍋", "圓心鴛鴦鍋中間換成正方形。", "九宮格鍋 + 2 塊錠，鍋的上下各一塊"),
    },
}
_ORIG = {}


def L(s):
    return T.get(s, s)


def loc(text):
    """把生成器写死的中文换成当前语言（正文本来就是译文，不受影响）。"""
    if not T:
        return text
    for k in sorted(T, key=len, reverse=True):
        text = text.replace(k, T[k])
    if LANG == "en":
        text = re.sub(r'(<span class="tag since">)自 ', r"\1Since ", text)
    return text


def set_lang(lang):
    """切换生成语言：正文目录、物品名、原版名、界面文字。"""
    global LANG, T
    if not _ORIG:
        _ORIG.update(CONTENT=bt.CONTENT, NAMES=dict(bt.NAMES), VZ=dict(bt.VZ), TAG=bt.TAG_ICON["minecraft:logs"],
                     BADGE=bt.BADGE, POTS=[dict(p) for p in bt.POTS])
    LANG = lang
    bt.CONTENT, bt.NAMES, bt.VZ = _ORIG["CONTENT"], dict(_ORIG["NAMES"]), dict(_ORIG["VZ"])
    bt.TAG_ICON["minecraft:logs"], bt.BADGE, bt.POTS = _ORIG["TAG"], _ORIG["BADGE"], [dict(p) for p in _ORIG["POTS"]]
    NAMES_EXTRA.clear()
    if lang == "zh_cn":
        T = {}
        return
    import build_tutorial_en as en
    import build_tutorial_tw as tw
    mod, lang_file, folder = (en, "en_us.json", "content-en") if lang == "en" else (tw, "zh_tw.json", "content-zh-tw")
    T = dict(mod.UI)
    T.pop("注意", None)   # 只换提示框标题，别把 zh_tw 正文里的「注意」也换掉
    T['<div class="ct">注意</div>'] = '<div class="ct">%s</div>' % mod.UI["注意"]
    T['<div class="ct">小提示</div>'] = '<div class="ct">%s</div>' % mod.UI["小提示"]
    T.pop("小提示", None)
    T.update(BOOK_UI[lang])
    bt.CONTENT = os.path.join(TUT, folder)
    names = json.load(open(os.path.join(bt.RES, "assets", bt.MOD, "lang", lang_file), encoding="utf-8"))
    # mryt233 改：忽略物品语言子键（如 soup_bowl.named / soup_bowl.unknown）
    bt.NAMES = {bt.MOD + ":" + k.split(".", 2)[2]: v for k, v in names.items()
                if k.startswith(("item." + bt.MOD + ".", "block." + bt.MOD + ".")) and "." not in k.split(".", 2)[2]}
    if lang == "en":
        bt.VZ = {k: k.split(":")[1].replace("_", " ").title() for k in _ORIG["VZ"]}
        bt.VZ.update({"minecraft:beef": "Raw Beef", "minecraft:mutton": "Raw Mutton", "minecraft:quartz": "Nether Quartz"})
        bt.TAG_ICON["minecraft:logs"] = ("minecraft:oak_log", "Any Log")
    else:
        bt.VZ = json.load(open(os.path.join(TUT, "data", "vanilla_zh_tw.json"), encoding="utf-8"))
    bt.BADGE = loc(bt.BADGE)
    for p in bt.POTS:
        p["n"], p["d"], p["r"] = POTS_L[lang][p["k"]]


def check_lang():
    """译文和简体原文的结构要一致（引用、配方、锚点、标题数；zh_tw 还核对数字）。"""
    import build_tutorial_en as en
    from collections import Counter
    for slug in ("index", "install", "quickstart", "hotpot", "tables", "tableware", "ingredients", "sauce", "faq", "preview", "changelog"):
        text = open(os.path.join(bt.CONTENT, slug + ".md"), encoding="utf-8").read()
        try:
            en.check_translation(slug, text)
        except ValueError as e:
            bt.warnings.append("[%s] %s" % (LANG, e))
        if LANG == "zh_tw":
            src = open(os.path.join(_ORIG["CONTENT"], slug + ".md"), encoding="utf-8").read()
            num = lambda v: Counter(re.findall(r"\d+(?:\.\d+)*", re.sub(r"<!--.*?-->", "", v, flags=re.S)))  # noqa: E731
            if num(src) != num(text):
                bt.warnings.append("[zh_tw] %s.md 的数字和简体不一致" % slug)
            if any(w in text for w in ("台灣", "台湾")):   # mryt233 改，2026-11-02：用户定写「繁体中文」，不写地区名、不写 zh_tw
                bt.warnings.append("[zh_tw] %s.md 出现了地区名，语言一律写「繁体中文」" % slug)

CSS_PATH = os.path.join(TUT, "assets", "book.css")
IMG = {}   # 页面里用到的图片：相对路径 → data URI（同一张只嵌一次）


def data_uri(path):
    mime = mimetypes.guess_type(path)[0] or "image/png"
    return "data:%s;base64,%s" % (mime, base64.b64encode(open(path, "rb").read()).decode())


def icon_path(i):
    """图标文件：先找查看器导出的，再找模组 / 原版的平面物品贴图。"""
    fid = bt.full_id(i)
    fid = bt.ICON_FALLBACK.get(fid, fid)
    p = os.path.join(bt.ICONS, fid.replace(":", "__") + ".png")
    if os.path.exists(p):
        return p
    # mryt233 改（2026-11-09，r146b）：标签名（不一定是 «ns:name» 形式）找不到平面贴图，
    # 直接返回 None，交给调用方报「缺图标」，别在这里 split 崩掉。
    if fid.count(":") != 1:
        return None
    ns, name = fid.split(":")
    for d in ([os.path.join(TEX, "item")] if ns == bt.MOD else [VANILLA_ITEM_TEX]):
        q = os.path.join(d, name + ".png")
        if os.path.exists(q):
            return q
    return None


def icon_tag(i, cls="ic"):
    p = icon_path(i)
    if not p:
        bt.warnings.append("缺图标：%s" % i)
        return '<span class="ic missing">?</span>'
    key = "icon:" + bt.full_id(i)
    IMG.setdefault(key, data_uri(p))
    return '<img class="%s" data-k="%s" alt="">' % (cls, html.escape(key))


def entry_title(md_heading):
    t = re.sub(r"\{#[^}]*\}", "", md_heading)
    t = re.sub(r"\{\{[^}]*\}\}", "", t)
    return t.strip("# ").strip()


def first_item(md):
    m = re.search(r"\{\{(?:i|icons|recipe):([^},]+)", md)
    return m.group(1).strip() if m else None


def split_sections(md, by_h2):
    """按二级标题拆段（提示框 ::: 里面的不算）。第一段是一级标题和导语。"""
    if not by_h2:
        return [md]
    out, cur, inbox = [], [], False
    for line in md.splitlines():
        if line.startswith(":::"):
            inbox = line.strip() != ":::"
        if line.startswith("## ") and not inbox and cur:
            out.append("\n".join(cur))
            cur = []
        cur.append(line)
    out.append("\n".join(cur))
    return [s for s in out if s.strip()]


def render_section(md):
    return embed_imgs(bt.render_md(md))


def embed_imgs(body):
    # 箭头换成指南贴图里的箭头
    body = body.replace(bt.ARROW, '<i class="arr"></i>')
    # 图片：img/xxx → data-k
    def grab(m):
        src = m.group(1)
        if src.startswith("img/icons/"):
            fid = src[len("img/icons/"):-4].replace("__", ":")
            p = icon_path(fid)
            key = "icon:" + fid
        else:
            p = os.path.join(TUT, src.replace("/", os.sep))
            key = src
        if not p or not os.path.exists(p):
            return m.group(0)
        IMG.setdefault(key, data_uri(p))
        return 'data-k="%s"' % key
    return re.sub(r'src="(img/[^"]+)"', grab, body)


# ---------------- 合成表大全（同一类的配方合成一张卡，材料自动轮换，点材料看做出来的东西） ----------------
BASE_RES = bt.RES   # mryt233 改，2026-11-08：0.2.2-alpha 正式版 = 当前主工程
BASE_VERSION = "0.2.2-alpha"
VARIANT_PREFIXES = sorted(bt.WOODS + ["quartz", "bamboo", "terracotta", "copper", "iron", "cut_copper", "smooth_stone",
                                      "polished_andesite", "polished_blackstone", "polished_deepslate", "polished_diorite", "polished_granite"],
                          key=len, reverse=True)
NAMES_EXTRA = {}   # 发布版（BASE_RES）语言文件里的名字；发布版有、当前开发版没有的物品靠它补进图鉴语言文件


def load_base_names():
    p = os.path.join(BASE_RES, "assets", bt.MOD, "lang", {"zh_cn": "zh_cn.json", "zh_tw": "zh_tw.json", "en": "en_us.json"}[LANG])
    if os.path.exists(p):
        for k, v in json.load(open(p, encoding="utf-8")).items():
            m = re.match(r"(item|block)\.%s\.(.+)" % bt.MOD, k)
            # mryt233 改：忽略物品语言子键
            if m and "." not in m.group(2):
                NAMES_EXTRA.setdefault("%s:%s" % (bt.MOD, m.group(2)), v)


def nm(i):
    fid = bt.full_id(i)
    n = bt.name_of(fid)
    return NAMES_EXTRA.get(fid, n) if n == fid.split(":")[1] else n


def load_recipes_all():
    """配方 = 已发布版的全部配方 + 开发版新加的（标「开发版预览」）；0.2.0-alpha 起已发布版就是主工程。"""
    out = []
    base_dir = os.path.join(BASE_RES, "data", bt.MOD, "recipe")
    base = {os.path.basename(f)[:-5]: json.load(open(f, encoding="utf-8")) for f in sorted(glob.glob(os.path.join(base_dir, "*.json")))}
    if not base:
        bt.warnings.append("找不到已发布版的配方（%s），全部按开发版算" % base_dir)
    for k, r in base.items():
        out.append((k, r, False))
    cur_dir = bt.RECIPES
    for f in sorted(glob.glob(os.path.join(cur_dir, "*.json"))):
        k = os.path.basename(f)[:-5]
        if k not in base:
            out.append((k, json.load(open(f, encoding="utf-8")), bool(base)))
    return out


def ikey(i, label=None):
    """物品 → [图标 key, 名字]，图标登记进 IMG。label 用来给标签写「任意原木」这种名字。"""
    fid = bt.full_id(i)
    p = icon_path(fid)
    if not p:
        bt.warnings.append("缺图标：%s" % fid)
        return [None, label or nm(fid)]
    key = "icon:" + fid
    IMG.setdefault(key, data_uri(p))
    return [key, label or nm(fid)]


def ref_key(ref):
    """bt.ing_ref 的结果 (图标 id, 提示文字) → [key, 名字]；物品就用中文名，标签用提示文字。"""
    if not ref or not ref[0]:
        return None
    iid, label = ref
    return ikey(iid, None if label == bt.name_of(iid) else label)


def recipe_cells(r):
    cells = [None] * 9
    if "pattern" in r:
        for y, row in enumerate(r["pattern"][:3]):
            for x, ch in enumerate(row[:3]):
                if ch != " " and ch in r["key"]:
                    cells[y * 3 + x] = bt.ing_ref(r["key"][ch])
    else:
        for n, ing in enumerate((r.get("ingredients") or [])[:9]):
            cells[n] = bt.ing_ref(ing)
    return cells


def base_of(name):
    if "yuanyang" in name or "four_grid" in name:
        return "split_pot"      # 各种鸳鸯锅、田字锅合成一张卡
    for pre in VARIANT_PREFIXES:
        if name.startswith(pre + "_") and len(name) > len(pre) + 1:
            return name[len(pre) + 1:]
    if re.match(r"cilantro_\w+_(meatball|ball)$", name):
        return "cilantro_meatball"
    return name


GROUP_TITLES = {"split_pot": "鸳鸯锅 / 田字锅", "cilantro_meatball": "香菜丸子"}
CHIP_BY_RESULT = {"split_pot", "cilantro_meatball"}   # 这几类下面那排按「做出来的东西」挑，不按材料


def common_title(names, n):
    if n == 1:
        return names[0]
    a = names[0]
    k = 0
    while k < len(a) and all(len(x) > k and x[-1 - k] == a[-1 - k] for x in names):
        k += 1
    t = a[len(a) - k:]
    if LANG == "en":
        t = t[t.index(" ") + 1:] if " " in t and not t.startswith(" ") else t.strip()
    t = t.lstrip("制製木")
    return count_title(t or names[0], n)


def count_title(t, n):
    return "%s (%d %s)" % (t, n, L("种")) if LANG == "en" else "%s（%d %s）" % (t, n, L("种"))


RG_SECTIONS = [
    ("灶台与锅", "brick_stove", lambda b: b in ("brick_stove", "tall_brick_stove", "chopsticks", "split_pot") or ("hotpot" in b and "table" not in b and "guide" not in b) or "soup_bucket" in b),
    ("桌凳", "oak_hotpot_table", lambda b: "table" in b or b in ("stool", "bench")),
    ("盘子", "quartz_small_dish", lambda b: any(x in b for x in ("dish", "plate", "platter"))),
    ("蘸料台", "oak_sauce_station", lambda b: "sauce_station" in b),
    ("厨具与食材", "iron_kitchen_knife", lambda b: True),
]


def recipe_book_entries():
    """合成表大全的条目：每段几张轮换卡片，返回 [(标题, 图标, html)]。"""
    load_base_names()
    groups = {}
    cooking, cutting = [], []
    for k, r, dev in load_recipes_all():
        t = r["type"].split(":")[1]
        if t in ("smelting", "smoking", "campfire_cooking"):
            cooking.append((k, r, dev))
            continue
        rid = bt.result_id(r)
        if t == "stonecutting":
            src = ref_key(bt.ing_ref(r["ingredient"]))
            cutting.append({"cells": [src], "out": ikey(rid) + [bt.result_count(r)], "dev": dev, "mat": src})
            continue
        # mryt233 改（2026-11-09，r146b）：布围裙换款配方（apron_recolor）字段是 apron / dye，
        # 当成「两格无序」进合成表大全的「厨具与食材」段。
        if t == "apron_recolor":
            # cells 存的是 ing_ref 结果（(图标 id, 名字)），渲染时统一经 ref_key 转换一次
            cells = [bt.ing_ref(r["apron"]), bt.ing_ref(r["dye"])]
            groups.setdefault(base_of(rid.split(":")[1]), []).append(
                {"rid": rid, "cells": cells, "count": bt.result_count(r), "dev": dev})
            continue
        if not ("pattern" in r or "ingredients" in r):
            bt.warnings.append("看不懂的配方类型：%s" % k)
            continue
        groups.setdefault(base_of(rid.split(":")[1]), []).append(
            {"rid": rid, "cells": recipe_cells(r), "count": bt.result_count(r), "dev": dev})

    sections, used = [], set()
    for title, icon, pred in RG_SECTIONS:
        cards = []
        order = ["brick_stove", "tall_brick_stove", "nine_grid_hotpot", "split_pot", "chopsticks",
                 "hotpot_table", "tall_hotpot_table", "stool", "bench", "small_dish"]
        for b in sorted(groups, key=lambda x: (all(v["dev"] for v in groups[x]), order.index(x) if x in order else 99, x)):
            if b in used or not pred(b):
                continue
            used.add(b)
            vs = groups[b]
            # 已发布的排前面（橡木打头），开发版的排后面
            vs.sort(key=lambda v: (v["dev"], 0 if v["rid"].split(":")[1].startswith("oak_") else 1, v["rid"]))
            # mryt233 改（2026-11-09，r146b）：换款配方只有 2 格（围裙 + 染料），
            # 轮换格数按各卡片实际格子数取最小值，免得越界。
            n_cells = min(len(v["cells"]) for v in vs)
            vary = [i for i in range(n_cells) if len({(v["cells"][i] or (None,))[0] for v in vs}) > 1]
            vj = []
            for v in vs:
                mat = None if b in CHIP_BY_RESULT else next((v["cells"][i] for i in vary if v["cells"][i]), None)
                vj.append({"cells": [ref_key(c) for c in v["cells"]], "out": ikey(v["rid"]) + [v["count"]], "dev": v["dev"],
                           "mat": ref_key(mat) if mat else ikey(v["rid"])})
            gt = GROUP_TITLES.get(b)
            gt = count_title(L(gt), len(vs)) if gt else common_title([nm(v["rid"]) for v in vs], len(vs))
            cards.append({"title": gt, "v": vj})
        if cards:
            sections.append((L(title), icon, cards))
    if cutting:
        sections.append((L("切石机"), "minecraft:stonecutter", [{"title": count_title(L("切石机"), len(cutting)), "kind": "cut", "v": cutting}]))
    cook = {}
    for k, r, dev in cooking:
        raw = bt.ing_ref(r["ingredient"])[0]
        kind = L({"smelting": "熔炉", "smoking": "烟熏炉", "campfire_cooking": "营火"}[r["type"].split(":")[1]])
        c = cook.setdefault(raw, {"cells": [ikey(raw)], "out": ikey(bt.result_id(r)) + [1], "dev": dev, "mat": ikey(raw), "t": {}})
        c["t"][kind] = round(r.get("cookingtime", 200) / 20, 2)
    cv = sorted(cook.values(), key=lambda v: v["dev"])
    for v in cv:
        v["note"] = "　".join("%s %s %s" % (k, ("%g" % v["t"][k]), L("秒")) for k in map(L, ("熔炉", "烟熏炉", "营火")) if k in v["t"])
        del v["t"]
    sections.append((L("烹饪"), "minecraft:furnace", [{"title": count_title(L("生 → 熟"), len(cv)), "kind": "cook", "v": cv}]))

    out = []
    for si, (title, icon, cards) in enumerate(sections):
        h = '<header class="ehead">%s<h2>%s</h2></header>' % (icon_tag(icon, "ic eic"), title)
        if si == 0:
            h += '<p class="muted">%s%s</p>' % (L("同一类的配方合成一张卡，材料自动轮换。<b>点卡片下面的材料</b>就停在那一种，右边显示做出来的样子；再点一次恢复轮换。"),
                                                 L("标 <span class=\"tag dev\">开发版预览</span> 的是 %s 之后才加的，正式版里还没有，名字和配方以后可能会变。") % BASE_VERSION)
        for g in cards:
            h += '<div class="rg" data-g="%s"></div>' % html.escape(json.dumps(g, ensure_ascii=False), quote=True)
        out.append((title, icon, h))
    return out


def with_head(body, icon):
    return re.sub(r"^\s*<(h[12])([^>]*)>(.*?)</\1>",
                  lambda m: '<header class="ehead">%s<%s%s>%s</%s></header>' % (icon_tag(icon, "ic eic"), m.group(1), m.group(2), m.group(3), m.group(1)),
                  body, count=1, flags=re.S)


def changelog_entries():
    """更新日志拆两条：「开发版预览」单独一条，各正式版合成「更新日志」一条（维护提示框不进书）。"""
    md = open(os.path.join(bt.CONTENT, "changelog.md"), encoding="utf-8").read()
    secs = split_sections(md, True)
    intro = re.sub(r"(?ms)^:::tip\n\*\*(?:维护提示|維護提示|Maintenance note).*?\n:::\s*$", "", secs[0])
    preview = [x for x in secs[1:] if x.lstrip().startswith("## " + L("开发版预览"))]
    rest = [x for x in secs[1:] if x not in preview]
    out = []
    if preview:
        out.append((L("开发版预览"), "shrimp", with_head(render_section(preview[0]), "shrimp")))
    out.append((L("更新日志"), "minecraft:written_book", with_head(render_section(intro + "\n\n" + "\n\n".join(rest)), "minecraft:written_book")))
    return out


def items_entry():
    """物品图鉴：照 build_tutorial 的图鉴页，开发版才有的物品加「开发版预览」标签。"""
    load_base_names()
    # 发布版有、开发版没有的物品也列上，图鉴以发布版为准
    for fid, n in NAMES_EXTRA.items():
        bt.NAMES.setdefault(fid, n)
    body = embed_imgs(bt.build_items_page())
    if NAMES_EXTRA:
        def tag(m):
            fid = "%s:%s" % (bt.MOD, m.group(2))
            return m.group(1) + ('<span class="tag dev">%s</span>' % L("开发版预览") if fid not in NAMES_EXTRA else "") + m.group(3)
        body = re.sub(r"(</b>)(?:<span class=\"tag dev\">[^<]*</span>)?(<code>)([a-z0-9_]+)(</code>)", lambda m: tag(type("M", (), {"group": lambda self, k: {1: m.group(1), 2: m.group(3), 3: m.group(2) + m.group(3) + m.group(4)}[k]})()), body)
    return with_head(body, "minecraft:book")


def page_sounds():
    """翻页声：和游戏里指南一样用原版「翻书」声（item.book.page_turn = open_flip1～3），从 Loom 缓存的原版资源取。"""
    root = os.path.expanduser("~/.gradle/caches/fabric-loom/assets")
    try:
        idx = json.load(open(os.path.join(root, "indexes", "1.21.1-17.json"), encoding="utf-8"))["objects"]
    except Exception as e:  # noqa: BLE001
        bt.warnings.append("取不到原版翻书声（%s），网页没有音效" % e)
        return []
    out = []
    for n in (1, 2, 3):
        h = idx.get("minecraft/sounds/item/book/open_flip%d.ogg" % n, {}).get("hash")
        f = h and os.path.join(root, "objects", h[:2], h)
        if f and os.path.exists(f):
            out.append("data:audio/ogg;base64," + base64.b64encode(open(f, "rb").read()).decode())
    return out


def compass_frames():
    """原版指南针的 32 帧（compass_00～31，查看器缓存的原版贴图），给搜索书签用。"""
    out = []
    for i in range(32):
        p = os.path.join(VANILLA_ITEM_TEX, "compass_%02d.png" % i)
        if not os.path.exists(p):
            bt.warnings.append("缺原版指南针贴图：%s" % p)
            return []
        out.append(data_uri(p))
    return out


def leather_tile():
    """书皮皮革纹理：照 gen_guide_gui.py 的 leather()，皮面色上撒明暗颗粒，16×16 放大 3 倍（一个贴图像素 = 3 个网页像素）。"""
    import io
    import random
    from PIL import Image
    base = (0x8a, 0x22, 0x1a)
    rnd = random.Random(7)
    im = Image.new("RGB", (16, 16))
    for y in range(16):
        for x in range(16):
            r = rnd.random()
            k = 0.94 if r < 0.18 else 1.05 if r > 0.86 else 1.0
            im.putpixel((x, y), tuple(max(0, min(255, round(c * k))) for c in base))
    im = im.resize((48, 48), Image.NEAREST)
    buf = io.BytesIO()
    im.save(buf, "PNG")
    return "data:image/png;base64," + base64.b64encode(buf.getvalue()).decode()


def use_release_recipes():
    bt.use_release_recipes()   # 正文里的 {{recipe:…}} 照已发布版的配方画


def build_lang(lang):
    """排一种语言的书：返回 (分类目录, 搜索索引, 模板 html)。模板 id 带语言前缀，三种语言放进同一个网页。"""
    set_lang(lang)
    if lang != "zh_cn":
        check_lang()
    use_release_recipes()
    load_base_names()
    cats = []
    anchors = {}   # 锚点 → (分类, 条目)；页面之间的链接靠它跳
    slug_first = {}
    for ci, (title, icon, sources) in enumerate(CATEGORIES):
        entries = []
        for slug, by_h2 in sources:
            if slug == "@preview":
                for et, eicon, eh in changelog_entries():
                    if et == L("开发版预览"):
                        entries.append({"title": et, "icon": icon_tag(eicon), "slug": "changelog", "html": eh})
                continue
            if slug == "@changelog":
                for et, eicon, eh in changelog_entries():
                    if et == L("开发版预览"):
                        continue
                    if et == L("更新日志"):
                        slug_first["changelog"] = (ci, len(entries), None)
                    entries.append({"title": et, "icon": icon_tag(eicon), "slug": "changelog", "html": eh})
                continue
            if slug == "@items":
                slug_first["items"] = (ci, len(entries), None)
                entries.append({"title": L("物品图鉴"), "icon": icon_tag("minecraft:book"), "slug": "items", "html": items_entry()})
                continue
            if slug == "@recipes":
                slug_first["recipes"] = (ci, len(entries), None)
                for rt, ricon, rh in recipe_book_entries():
                    entries.append({"title": rt, "icon": icon_tag(ricon), "slug": "", "html": rh})
                continue
            md = open(os.path.join(bt.CONTENT, slug + ".md"), encoding="utf-8").read()
            for si, sec in enumerate(split_sections(md, by_h2)):
                head = next((l for l in sec.splitlines() if l.startswith("#")), "")
                etitle = L(ENTRY_TITLES.get((slug, si), "")) or entry_title(head) or L(title)
                eicon = ENTRY_ICONS.get((slug, si)) or first_item(sec) or icon
                body = render_section(sec)
                # 标题前加图标，做成指南里「图标 + 标题 + 一道线」的样子
                body = re.sub(r"^\s*<(h[12])([^>]*)>(.*?)</\1>",
                              lambda m: '<header class="ehead">%s<%s%s>%s</%s></header>' % (icon_tag(eicon, "ic eic"), m.group(1), m.group(2), m.group(3), m.group(1)),
                              body, count=1, flags=re.S)
                ei = len(entries)
                for a in re.findall(r'\bid="([^"]+)"', body):
                    anchors.setdefault("%s#%s" % (slug, a), (ci, ei, a))
                slug_first.setdefault(slug, (ci, ei, None))
                entries.append({"title": etitle, "icon": icon_tag(eicon), "slug": slug, "html": body})
        if not entries:
            body = ('<header class="ehead">%s<h1>%s</h1></header><p class="muted">样稿只排了「入门」和「蘸料」两个分类，这一章先占个书签。</p>'
                    '<p>确认样式后，这里会放：<b>%s</b>。</p>') % (icon_tag(icon, "ic eic"), title, PLANNED.get(title, ""))
            entries.append({"title": title, "icon": icon_tag(icon), "slug": "", "html": body, "todo": True})
        # 每章第一页末尾加「本章目录」：列出后面各条（图标 + 标题 + 一句开头），点了直接翻过去；
        # 不然像「下载、安装与反馈」这种开头只有标题的，第一页是空的，读者不知道要点书顶的书签（用户 2026-10-27）
        if len(entries) > 1:
            items = ""
            for ei, e in enumerate(entries[1:], 1):
                # mryt233 改（2026-11-06）：摘要不能在这里就截，否则会钉在生成脚本写死的中文上，
                # 英文 / 繁体页面里条目摘要会漏出中文。先留占位，等 e["html"] 换完语言后再按译文截。
                items += ('<a class="toc-item" href="#" data-go="%d,%d,">%s<span><b>%s</b><small>{{TOC%02d-%02d}}</small></span></a>'
                          % (ci, ei, e["icon"], html.escape(e["title"]), ci, ei))
            entries[0]["html"] += ('<section class="toc"><h3 class="toc-h">%s</h3><p class="toc-tip">%s</p>%s</section>'
                                   % (L("本章目录"), L("点下面的条目直接翻过去；也可以点书顶的书签，或者点右下角的三角一页页往后翻。"), items))
        cats.append({"title": L(title), "icon": icon_tag(icon), "entries": entries, "todo": not sources})

    # 链接改成书里的跳转：x.html#id / x.html / #id
    def relink(body, slug):
        def fix(m):
            href = m.group(1)
            mm = re.match(r"^([a-z]+)\.html(?:#(.+))?$", href) or re.match(r"^#(.+)$", href)
            if not mm:
                return m.group(0)
            if href.startswith("#"):
                tgt = anchors.get("%s#%s" % (slug, mm.group(1)))
            else:
                tgt = anchors.get("%s#%s" % (mm.group(1), mm.group(2))) if mm.group(2) else slug_first.get(mm.group(1))
                if not tgt and mm.group(1) in slug_first:
                    tgt = slug_first[mm.group(1)]
            if not tgt:
                return 'href="#" data-todo="1"'
            return 'href="#" data-go="%d,%d,%s"' % (tgt[0], tgt[1], tgt[2] or "")
        return re.sub(r'href="([^"]+)"', fix, body)

    for c in cats:
        for e in c["entries"]:
            e["html"] = loc(relink(e["html"], e["slug"]))
            e["title"] = loc(e["title"])
    # mryt233 改（2026-11-06）：「本章目录」的条目摘要原来在换语言之前就截好了，英文 / 繁体页会漏中文。
    # 现在按换完语言的正文重新截一遍，嵌回占位符。
    for ci_, ci_e in enumerate(cats):
        for ei_, e in enumerate(ci_e["entries"]):
            if "{{TOC%02d-%02d}}" % (ci_, ei_) not in e["html"]:
                continue
            body_ = re.sub(r"<header.*?</header>", "", e["html"], count=1, flags=re.S)
            body_ = re.sub(r"<table\b.*?</table>|<div class=\"callout.*?</div>\s*</div>|<div class=\"(?:chips|recipes|rg)\b.*?</div>", "", body_, flags=re.S)
            m_ = re.search(r"<(p|li)\b[^>]*>(.*?)</\1>", body_, flags=re.S)
            plain = m_.group(2) if m_ else ""
            plain = re.sub(r"<(script|style)\b.*?</\1>", "", plain, flags=re.S)
            plain = re.sub(r'data-g="[^"]*"', "", plain)
            plain = re.sub(r"</?(?:b|strong|em|i|a|span|code|mark|small)\b[^>]*>", "", plain)
            plain = re.sub(r"<[^>]+>", " ", plain)
            plain = re.sub(r"\s+", " ", html.unescape(plain)).strip()
            snip = plain[:34] + ("…" if len(plain) > 34 else "")
            e["html"] = e["html"].replace("{{TOC%02d-%02d}}" % (ci_, ei_), html.escape(snip))

    templates, meta, sidx = [], [], []
    for ci, c in enumerate(cats):
        meta.append({"t": c["title"], "todo": c["todo"], "e": [x["title"] for x in c["entries"]]})
        for ei, e in enumerate(c["entries"]):
            extra = []   # 合成表大全的卡片是网页上现画的，把卡片标题和各变体名字也放进搜索
            for g in re.findall(r'data-g="([^"]*)"', e["html"]):
                try:
                    gj = json.loads(html.unescape(g))
                    extra.append(gj.get("title", ""))
                    extra += [(v.get("out") or [None, ""])[1] for v in gj.get("v", [])]
                except ValueError:
                    pass
            plain = re.sub(r"</?(?:b|strong|em|i|a|span|code|mark|small)\b[^>]*>", "", e["html"])   # 行内标签直接去掉，句子不被空格打断
            plain = re.sub(r"<[^>]+>", " ", plain) + " " + " ".join(x for x in extra if x)
            plain = re.sub(r"\s+", " ", html.unescape(plain)).strip()
            sidx.append({"c": ci, "e": ei, "x": plain})
        templates.append('<div class="cat-icon" id="%s-ci-%d">%s</div>' % (lang, ci, c["icon"]))
        for ei, e in enumerate(c["entries"]):
            templates.append('<template id="%s-t-%d-%d">%s</template>' % (lang, ci, ei, e["html"]))
            templates.append('<div class="ent-icon" id="%s-ei-%d-%d">%s</div>' % (lang, ci, ei, e["icon"]))
    pots = any('class="potviewer"' in e["html"] for c in cats for e in c["entries"])
    ui = {k: L(k) for k in JS_UI}
    ui.update({"_" + k: L(k) for k in FRAME_UI})
    return {"meta": meta, "sidx": sidx, "ui": ui}, templates, pots


# 换语言按钮的地球（16×16 像素画）：o 描边、b 海、l 浅海、g 陆地、d 深色陆地
GLOBE = [
    ".....oooooo.....",
    "...oolbbbggoo...",
    "..olbbbggggbbo..",
    ".olbbbgggggbbbo.",
    ".oggbbggdbbbggo.",
    "obgggbbbbbbgggbo",
    "obggggbbbbggdgbo",
    "obbgdgbbbbbgggbo",
    "obbbggbbbbbbggbo",
    "obbbbgbbbggbbbbo",
    "obbbbbbbggdgbbbo",
    ".obbbbbbgggbbbo.",
    ".obbbbbbbggbbbo.",
    "..obbbbbbbbbbo..",
    "...oobbbbbbboo..",
    ".....oooooo.....",
]


# 翻页音效按钮的喇叭（16×16）：o 描边、w 喇叭身、a 声波（开着时显示）、x 红叉（关掉时显示）
SPEAKER = [
    "................",
    "................",
    "......oo........",
    ".....owo....a...",
    "....owwo..a..a..",
    "ooooowwo...a..a.",
    "owwwwwwo.a..a.a.",
    "owwwwwwo.a..a.a.",
    "owwwwwwo.a..a.a.",
    "owwwwwwo.a..a.a.",
    "ooooowwo...a..a.",
    "....owwo..a..a..",
    ".....owo....a...",
    "......oo........",
    "................",
    "................",
]
SPEAKER_X = [(9, 5), (10, 6), (11, 7), (12, 8), (13, 9), (14, 10), (14, 5), (13, 6), (11, 8), (10, 9), (9, 10),
             (10, 5), (14, 6), (13, 10), (9, 9), (12, 7)]


def speaker_svg():
    col = {"o": "#1d1d1d", "w": "#efe2c8"}
    body = "".join('<rect x="%d" y="%d" width="1" height="1" fill="%s"/>' % (x, y, col[ch])
                   for y, row in enumerate(SPEAKER) for x, ch in enumerate(row) if ch in col)
    waves = "".join('<rect x="%d" y="%d" width="1" height="1"/>' % (x, y)
                    for y, row in enumerate(SPEAKER) for x, ch in enumerate(row) if ch == "a")
    cross = "".join('<rect x="%d" y="%d" width="1" height="1"/>' % xy for xy in SPEAKER_X)
    return ('<svg viewBox="0 0 16 16" width="20" height="20" shape-rendering="crispEdges" aria-hidden="true">%s'
            '<g class="w" fill="#efe2c8">%s</g><g class="x" fill="#ff5555">%s</g></svg>' % (body, waves, cross))


def globe_svg():
    col = {"o": "#1d2b4a", "b": "#3b6fd1", "l": "#7fb0f0", "g": "#4caa3a", "d": "#2e7a24"}
    rects = "".join('<rect x="%d" y="%d" width="1" height="1" fill="%s"/>' % (x, y, col[ch])
                    for y, row in enumerate(GLOBE) for x, ch in enumerate(row) if ch in col)
    return '<svg viewBox="0 0 16 16" width="20" height="20" shape-rendering="crispEdges" aria-hidden="true">%s</svg>' % rects


def build():
    data, templates, pots = {}, [], False
    for lang, _ in LANGS:
        d, t, p = build_lang(lang)
        data[lang] = d
        templates += t
        pots = pots or p
    set_lang("zh_cn")

    # 锅形展示要用到全部锅图（铜 / 铁 × 各锅形 × 两个角度），一并内嵌
    if pots:
        for f in sorted(glob.glob(os.path.join(TUT, "img", "pot_*.png"))):
            IMG.setdefault("img/" + os.path.basename(f), data_uri(f))
    sprite = data_uri(os.path.join(TEX, "gui", "guide_book.png"))
    css = (open(CSS_PATH, encoding="utf-8").read()
           .replace("__SPRITE__", sprite).replace("__LEATHER__", leather_tile()))
    js = open(os.path.join(TUT, "assets", "book.js"), encoding="utf-8").read()
    langs = [{"k": k, "n": n} for k, n in LANGS]
    version_badge = f"MC {MC_VERSION} · {BASE_VERSION}" if MOD_VERSION == BASE_VERSION else f"MC {MC_VERSION} · {BASE_VERSION}（{MOD_VERSION}）"
    page = f"""<!doctype html>
<html lang="zh-CN"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>我的世界[之晗的火锅物语]模组-游玩教程（{MC_VERSION} · {MOD_VERSION}）</title><style>{css}</style></head>
<body>
<header class="bar"><span class="name" data-ui="我的世界[之晗的火锅物语]模组-游玩教程">我的世界[之晗的火锅物语]模组-游玩教程</span><button class="snd" id="snd" data-ui-label="翻页音效" aria-label="翻页音效">{speaker_svg()}</button><span class="langw"><button class="lang-btn" id="lang" data-ui-label="语言" aria-label="语言" aria-haspopup="true">{globe_svg()}</button><span class="lang-menu" id="lang-menu" hidden></span></span><span class="draft">{version_badge}</span></header>
<main class="stage">
  <div class="book" id="book">
    <div class="drawer" id="drawer">
      <nav class="cats" id="cats" data-ui-label="分类" aria-label="分类"></nav>
      <nav class="tabs" id="tabs" data-ui-label="条目" aria-label="条目"></nav>
      <button class="find-tab" id="find" data-ui-label="搜索" aria-label="搜索"><img class="cmp" alt="" width="32" height="32"></button>
    </div>
    <div class="mtop"><button class="menu-btn" id="menu" aria-label="目录"><i class="burger"></i><span class="mb-ic" id="menu-ic"></span></button><nav class="mtabs" id="mtabs" data-ui-label="条目" aria-label="条目"></nav><button class="mfind" id="mfind" data-ui-label="搜索" aria-label="搜索"><img class="cmp" alt="" width="32" height="32"></button></div>
    <div class="cover">
      <div class="paper">
        <div class="view" id="view"><article class="flow" id="flow"></article></div>
        <button class="turn prev" id="prev" data-ui-label="上一页" aria-label="上一页"></button>
        <button class="turn next" id="next" data-ui-label="下一页" aria-label="下一页"></button>
        <span class="pno" id="pno"></span>
        <i class="spine"></i>
      </div>
    </div>
  </div>
</main>
<div class="backdrop" id="backdrop"></div>
<div class="mctip" id="tip" hidden></div>
<div class="findbox" id="findbox" hidden><div class="fb-head"><img class="cmp" alt="" width="32" height="32"><input id="fq" type="search" data-ui-ph="搜索：物品、玩法、配方……" placeholder="搜索：物品、玩法、配方……" autocomplete="off"><button id="fclose" data-ui-label="关闭" aria-label="关闭">×</button></div><div class="fb-list" id="flist"></div></div>
<div hidden id="store">{"".join(templates)}</div>
<script>window.BOOKS={json.dumps(data, ensure_ascii=False)};window.LANGS={json.dumps(langs, ensure_ascii=False)};window.IMG={json.dumps(IMG)};window.SND={json.dumps(page_sounds())};window.COMPASS={json.dumps(compass_frames())};</script>
<script>{js}</script>
</body></html>"""
    open(OUT, "w", encoding="utf-8").write(page)
    print("指南样式（当前最新入口）→ %s（%.1f MB）" % (OUT, os.path.getsize(OUT) / 1e6))
    # mryt233 改（2026-11-06，用户要求）：每次生成书本教程时，按 MC 版本与模组版本同步留存一份带版本号的归档副本，
    # 避免每次更新都只覆盖单文件而丢掉历史版本教程。
    if not CUSTOM_OUT:
        tier = "dev" if re.search(r"-r\d+", MOD_VERSION) else "release"
        ver_dir = os.path.join(TUT, "versions", MC_VERSION, tier)
        os.makedirs(ver_dir, exist_ok=True)
        ver_name = f"我的世界[之晗的火锅物语]模组-游玩教程（书本样式）-{MC_VERSION}-{MOD_VERSION}.html"
        ver_out = os.path.join(ver_dir, ver_name)
        open(ver_out, "w", encoding="utf-8").write(page)
        print("指南样式（版本归档副本）→ %s" % ver_out)
    for w in sorted(set(bt.warnings)):
        print("警告：" + w)


if __name__ == "__main__":
    # 预览别处的稿子（比如 translate/…/修改版）：--content 目录 --css 文件 --out 输出 html，正式内容不受影响
    # 也可传 --mc <MC版本> --ver <模组版本> 指定归档版本号
    args = sys.argv[1:]
    def opt(name):
        return args[args.index(name) + 1] if name in args else None
    if opt("--content"):
        bt.CONTENT = os.path.abspath(opt("--content"))
    if opt("--css"):
        CSS_PATH = os.path.abspath(opt("--css"))
    if opt("--mc"):
        MC_VERSION = opt("--mc")
    if opt("--ver"):
        MOD_VERSION = opt("--ver")
    if opt("--out"):
        OUT = os.path.abspath(opt("--out"))
        CUSTOM_OUT = True
    build()
