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
OUT = os.path.join(TUT, "我的世界[之晗的火锅物语]模组-游玩教程（书本样式）.html")
TEX = os.path.join(bt.RES, "assets", bt.MOD, "textures")
VANILLA_ITEM_TEX = os.path.join(bt.ROOT, "..", "viewer", "cache", "1.21.1", "assets", "minecraft", "textures", "item")

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
    ("sauce", 2): "scallion",
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
    ("ingredients", 7): "scallion",
    ("ingredients", 8): "minecraft:cooked_beef",
    ("sauce", 4): "bamboo_chopsticks",
    ("tableware", 0): "quartz_eight_slot_platter",
    ("tableware", 1): "quartz_small_dish",
    ("tableware", 2): "clear_broth_bowl",
    ("tables", 0): "oak_bench",
}
ENTRY_TITLES = {("index", 0): "欢迎"}

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
BASE_RES = os.path.join(bt.ROOT, "..", "version-ports", "base", "0.1.4-alpha", "src", "main", "resources")
BASE_VERSION = "0.1.3-alpha-hotfix1"
VARIANT_PREFIXES = sorted(bt.WOODS + ["quartz", "bamboo", "terracotta", "copper", "iron", "cut_copper", "smooth_stone",
                                      "polished_andesite", "polished_blackstone", "polished_deepslate", "polished_diorite", "polished_granite"],
                          key=len, reverse=True)
NAMES_EXTRA = {}   # 发布版（BASE_RES）语言文件里的名字；发布版有、当前开发版没有的物品靠它补进图鉴语言文件


def load_base_names():
    p = os.path.join(BASE_RES, "assets", bt.MOD, "lang", "zh_cn.json")
    if os.path.exists(p):
        for k, v in json.load(open(p, encoding="utf-8")).items():
            m = re.match(r"(item|block)\.%s\.(.+)" % bt.MOD, k)
            if m:
                NAMES_EXTRA.setdefault("%s:%s" % (bt.MOD, m.group(2)), v)


def nm(i):
    fid = bt.full_id(i)
    n = bt.name_of(fid)
    return NAMES_EXTRA.get(fid, n) if n == fid.split(":")[1] else n


def load_recipes_all():
    """配方 = 已发布版（hotfix1 冻结源码）的全部配方 + 开发版新加的（标「开发版预览」）。"""
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
    t = a[len(a) - k:].lstrip("制木")
    return "%s（%d 种）" % (t or names[0], n)


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
            vary = [i for i in range(9) if len({(v["cells"][i] or (None,))[0] for v in vs}) > 1]
            vj = []
            for v in vs:
                mat = None if b in CHIP_BY_RESULT else next((v["cells"][i] for i in vary if v["cells"][i]), None)
                vj.append({"cells": [ref_key(c) for c in v["cells"]], "out": ikey(v["rid"]) + [v["count"]], "dev": v["dev"],
                           "mat": ref_key(mat) if mat else ikey(v["rid"])})
            gt = GROUP_TITLES.get(b)
            gt = "%s（%d 种）" % (gt, len(vs)) if gt else common_title([nm(v["rid"]) for v in vs], len(vs))
            cards.append({"title": gt, "v": vj})
        if cards:
            sections.append((title, icon, cards))
    if cutting:
        sections.append(("切石机", "minecraft:stonecutter", [{"title": "切石机（%d 种）" % len(cutting), "kind": "cut", "v": cutting}]))
    cook = {}
    for k, r, dev in cooking:
        raw = bt.ing_ref(r["ingredient"])[0]
        kind = {"smelting": "熔炉", "smoking": "烟熏炉", "campfire_cooking": "营火"}[r["type"].split(":")[1]]
        c = cook.setdefault(raw, {"cells": [ikey(raw)], "out": ikey(bt.result_id(r)) + [1], "dev": dev, "mat": ikey(raw), "t": {}})
        c["t"][kind] = round(r.get("cookingtime", 200) / 20, 2)
    cv = sorted(cook.values(), key=lambda v: v["dev"])
    for v in cv:
        v["note"] = "　".join("%s %s 秒" % (k, ("%g" % v["t"][k])) for k in ("熔炉", "烟熏炉", "营火") if k in v["t"])
        del v["t"]
    sections.append(("烹饪", "minecraft:furnace", [{"title": "生 → 熟（%d 种）" % len(cv), "kind": "cook", "v": cv}]))

    out = []
    for si, (title, icon, cards) in enumerate(sections):
        h = '<header class="ehead">%s<h2>%s</h2></header>' % (icon_tag(icon, "ic eic"), title)
        if si == 0:
            h += ('<p class="muted">同一类的配方合成一张卡，材料自动轮换。<b>点卡片下面的材料</b>就停在那一种，右边显示做出来的样子；再点一次恢复轮换。'
                  '标 <span class="tag dev">开发版预览</span> 的是 %s 之后才加的，正式版里还没有，名字和配方以后可能会变。</p>') % BASE_VERSION
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
    intro = re.sub(r"(?ms)^:::tip\n\*\*维护提示.*?\n:::\s*$", "", secs[0])
    preview = [x for x in secs[1:] if x.lstrip().startswith("## 开发版预览")]
    rest = [x for x in secs[1:] if x not in preview]
    out = []
    if preview:
        out.append(("开发版预览", "shrimp", with_head(render_section(preview[0]), "shrimp")))
    out.append(("更新日志", "minecraft:written_book", with_head(render_section(intro + "\n\n" + "\n\n".join(rest)), "minecraft:written_book")))
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
            return m.group(1) + ('<span class="tag dev">开发版预览</span>' if fid not in NAMES_EXTRA else "") + m.group(3)
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


def build():
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
                    if et == "开发版预览":
                        entries.append({"title": et, "icon": icon_tag(eicon), "slug": "changelog", "html": eh})
                continue
            if slug == "@changelog":
                for et, eicon, eh in changelog_entries():
                    if et == "开发版预览":
                        continue
                    if et == "更新日志":
                        slug_first["changelog"] = (ci, len(entries), None)
                    entries.append({"title": et, "icon": icon_tag(eicon), "slug": "changelog", "html": eh})
                continue
            if slug == "@items":
                slug_first["items"] = (ci, len(entries), None)
                entries.append({"title": "物品图鉴", "icon": icon_tag("minecraft:book"), "slug": "items", "html": items_entry()})
                continue
            if slug == "@recipes":
                slug_first["recipes"] = (ci, len(entries), None)
                for rt, ricon, rh in recipe_book_entries():
                    entries.append({"title": rt, "icon": icon_tag(ricon), "slug": "", "html": rh})
                continue
            md = open(os.path.join(bt.CONTENT, slug + ".md"), encoding="utf-8").read()
            for si, sec in enumerate(split_sections(md, by_h2)):
                head = next((l for l in sec.splitlines() if l.startswith("#")), "")
                etitle = ENTRY_TITLES.get((slug, si)) or entry_title(head) or title
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
                body_ = re.sub(r"<header.*?</header>", "", e["html"], count=1, flags=re.S)
                body_ = re.sub(r"<table\b.*?</table>|<div class=\"callout.*?</div>\s*</div>|<div class=\"(?:chips|recipes|rg)\b.*?</div>", "", body_, flags=re.S)
                m_ = re.search(r"<(p|li)\b[^>]*>(.*?)</\1>", body_, flags=re.S)   # 摘要取第一句正文（跳过表格、提示框、配方）
                plain = m_.group(2) if m_ else ""
                plain = re.sub(r"<(script|style)\b.*?</\1>", "", plain, flags=re.S)
                plain = re.sub(r'data-g="[^"]*"', "", plain)
                plain = re.sub(r"</?(?:b|strong|em|i|a|span|code|mark|small)\b[^>]*>", "", plain)
                plain = re.sub(r"<[^>]+>", " ", plain)
                plain = re.sub(r"\s+", " ", html.unescape(plain)).strip()
                snip = plain[:34] + ("…" if len(plain) > 34 else "")
                items += ('<a class="toc-item" href="#" data-go="%d,%d,">%s<span><b>%s</b><small>%s</small></span></a>'
                          % (ci, ei, e["icon"], html.escape(e["title"]), html.escape(snip)))
            entries[0]["html"] += ('<section class="toc"><h3 class="toc-h">本章目录</h3>'
                                   '<p class="toc-tip">点下面的条目直接翻过去；也可以点书顶的书签，或者点右下角的三角一页页往后翻。</p>%s</section>' % items)
        cats.append({"title": title, "icon": icon_tag(icon), "entries": entries, "todo": not sources})

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
            e["html"] = relink(e["html"], e["slug"])

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
        templates.append('<div class="cat-icon" id="ci-%d">%s</div>' % (ci, c["icon"]))
        for ei, e in enumerate(c["entries"]):
            templates.append('<template id="t-%d-%d">%s</template>' % (ci, ei, e["html"]))
            templates.append('<div class="ent-icon" id="ei-%d-%d">%s</div>' % (ci, ei, e["icon"]))

    # 锅形展示要用到全部锅图（铜 / 铁 × 各锅形 × 两个角度），一并内嵌
    if any('class="potviewer"' in e["html"] for c in cats for e in c["entries"]):
        for f in sorted(glob.glob(os.path.join(TUT, "img", "pot_*.png"))):
            IMG.setdefault("img/" + os.path.basename(f), data_uri(f))
    sprite = data_uri(os.path.join(TEX, "gui", "guide_book.png"))
    css = (open(CSS_PATH, encoding="utf-8").read()
           .replace("__SPRITE__", sprite).replace("__LEATHER__", leather_tile()))
    js = open(os.path.join(TUT, "assets", "book.js"), encoding="utf-8").read()
    page = f"""<!doctype html>
<html lang="zh-CN"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>我的世界[之晗的火锅物语]模组-游玩教程</title><style>{css}</style></head>
<body>
<header class="bar"><span class="name">我的世界[之晗的火锅物语]模组-游玩教程</span><button class="snd" id="snd" aria-label="翻页音效"></button><span class="draft">0.1.4-alpha</span></header>
<main class="stage">
  <div class="book" id="book">
    <div class="drawer" id="drawer">
      <nav class="cats" id="cats" aria-label="分类"></nav>
      <nav class="tabs" id="tabs" aria-label="条目"></nav>
      <button class="find-tab" id="find" aria-label="搜索"><img class="cmp" alt="" width="32" height="32"></button>
    </div>
    <div class="mtop"><button class="menu-btn" id="menu" aria-label="目录"><i class="burger"></i><span class="mb-ic" id="menu-ic"></span></button><nav class="mtabs" id="mtabs" aria-label="条目"></nav><button class="mfind" id="mfind" aria-label="搜索"><img class="cmp" alt="" width="32" height="32"></button></div>
    <div class="cover">
      <div class="paper">
        <div class="view" id="view"><article class="flow" id="flow"></article></div>
        <button class="turn prev" id="prev" aria-label="上一页"></button>
        <button class="turn next" id="next" aria-label="下一页"></button>
        <span class="pno" id="pno"></span>
        <i class="spine"></i>
      </div>
    </div>
  </div>
</main>
<div class="backdrop" id="backdrop"></div>
<div class="mctip" id="tip" hidden></div>
<div class="findbox" id="findbox" hidden><div class="fb-head"><img class="cmp" alt="" width="32" height="32"><input id="fq" type="search" placeholder="搜索：物品、玩法、配方……" autocomplete="off"><button id="fclose" aria-label="关闭">×</button></div><div class="fb-list" id="flist"></div></div>
<div hidden id="store">{"".join(templates)}</div>
<script>window.BOOK={json.dumps(meta, ensure_ascii=False)};window.IMG={json.dumps(IMG)};window.SND={json.dumps(page_sounds())};window.SIDX={json.dumps(sidx, ensure_ascii=False)};window.COMPASS={json.dumps(compass_frames())};</script>
<script>{js}</script>
</body></html>"""
    open(OUT, "w", encoding="utf-8").write(page)
    print("指南样式 → %s（%.1f MB）" % (OUT, os.path.getsize(OUT) / 1e6))
    for w in sorted(set(bt.warnings)):
        print("警告：" + w)


if __name__ == "__main__":
    # 预览别处的稿子（比如外部校订的修改版）：--content 目录 --css 文件 --out 输出 html，正式内容不受影响
    args = sys.argv[1:]
    def opt(name):
        return args[args.index(name) + 1] if name in args else None
    if opt("--content"):
        bt.CONTENT = os.path.abspath(opt("--content"))
    if opt("--css"):
        CSS_PATH = os.path.abspath(opt("--css"))
    if opt("--out"):
        OUT = os.path.abspath(opt("--out"))
    build()
