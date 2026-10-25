# -*- coding: utf-8 -*-
"""
把 mod/tutorial/content/*.md 编成一个静态教程网站（mod/tutorial/site/），双击 index.html 就能看。
作者：（2026-10-24）

做了三件事：
1. Markdown -> HTML，加了几个写文章用的小指令（见下），配方图直接读游戏里的配方 JSON 画，所以配方永远和游戏一致。
2. 自动生成「配方大全」和「物品图鉴」两页（读 lang + 配方，不用手写）。
3. 检查教程有没有跟上游戏：新物品没人提、配方没页面讲、移植同步清单「未发布」里的条目更新日志没写，都会在最后警告。

写文章的小指令（在 .md 里写）：
  {{i:ID}}            行内小图标 + 中文名；ID 可省略模组前缀（模组物品），原版写 minecraft:xxx
  {{recipe:ID}}       显示以 ID 为产物的全部配方（合成 / 切石 / 烹饪）
  {{icons:a,b,c}}     一排物品图标（带名字）
  {{img:文件名|说明}}   插入 tutorial/img/ 里的截图
  {{since:0.1.2-alpha}} 「自 x 版本」标签；{{dev}} 「开发版（未发布）」标签
  :::tip / :::warn / :::dev ... :::   提示框 / 注意框 / 开发版框

用法（在 mod/ 下）：python tools/build_tutorial.py
"""
import glob
import html
import json
import os
import re
import shutil
import sys

import markdown

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
TUT = os.path.join(ROOT, "tutorial")
CONTENT = os.path.join(TUT, "content")
SITE = os.path.join(TUT, "site")
RES = os.path.join(ROOT, "src", "main", "resources")
MOD = "orients_hotpot_chronicles"
RECIPES = os.path.join(RES, "data", MOD, "recipe")
ICONS = os.path.join(TUT, "img", "icons")

WOODS = ["oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry", "bamboo", "crimson", "warped"]
WOOD_ZH = {"oak": "橡木", "spruce": "云杉木", "birch": "白桦木", "jungle": "丛林木", "acacia": "金合欢木", "dark_oak": "深色橡木",
           "mangrove": "红树木", "cherry": "樱花木", "bamboo": "竹", "crimson": "绯红木", "warped": "诡异木"}
TAG_ICON = {"minecraft:logs": ("minecraft:oak_log", "任意原木")}

PAGES = [  # (文件名, 导航标题, 分组)
    ("index", "欢迎", "开始"),
    ("install", "下载、安装与反馈", "开始"),
    ("quickstart", "十分钟吃上火锅", "开始"),
    ("hotpot", "灶台与火锅", "玩法"),
    ("tables", "桌子与凳子", "玩法"),
    ("tableware", "盘子、碗与饮料", "玩法"),
    ("ingredients", "食材与切菜", "玩法"),
    ("sauce", "蘸料", "玩法"),
    ("faq", "常见问题", "玩法"),
    ("recipes", "配方大全", "查阅"),
    ("items", "物品图鉴", "查阅"),
    ("changelog", "更新日志", "查阅"),
]
VERSION = "见更新日志"

warnings = []


# ---------------- 名字 ----------------
def load_names():
    names = {}
    for kind in ("item", "block"):
        pass
    lang = json.load(open(os.path.join(RES, "assets", MOD, "lang", "zh_cn.json"), encoding="utf-8"))
    for k, v in lang.items():
        m = re.match(r"(item|block)\.%s\.(.+)" % MOD, k)
        if m:
            names.setdefault("%s:%s" % (MOD, m.group(2)), v)
    # 原版中文名：存一份裁剪过的在 tutorial/data，缺了再从 Loom 缓存里取
    vz_path = os.path.join(TUT, "data", "vanilla_zh.json")
    vz = json.load(open(vz_path, encoding="utf-8")) if os.path.exists(vz_path) else {}
    return names, lang, vz, vz_path


NAMES, LANG, VZ, VZ_PATH = load_names()


def refresh_vanilla(needed):
    """needed: 需要中文名的原版 id 列表；缺的从 Loom 的 zh_cn.json 补进 vanilla_zh.json。"""
    missing = [i for i in needed if i not in VZ]
    if not missing:
        return
    idx_dir = os.path.expanduser("~/.gradle/caches/fabric-loom/assets")
    try:
        index = json.load(open(os.path.join(idx_dir, "indexes", "1.21.1-17.json"), encoding="utf-8"))["objects"]
        h = index["minecraft/lang/zh_cn.json"]["hash"]
        z = json.load(open(os.path.join(idx_dir, "objects", h[:2], h), encoding="utf-8"))
    except Exception as e:  # noqa: BLE001
        warnings.append("取不到原版中文名（%s），以下用英文 ID：%s" % (e, ", ".join(missing)))
        return
    for i in missing:
        n = i.split(":")[1]
        v = z.get("item.minecraft." + n) or z.get("block.minecraft." + n)
        if v:
            VZ[i] = v
        else:
            warnings.append("原版 %s 没有中文名" % i)
    os.makedirs(os.path.dirname(VZ_PATH), exist_ok=True)
    json.dump(VZ, open(VZ_PATH, "w", encoding="utf-8"), ensure_ascii=False, indent=1, sort_keys=True)


def full_id(i):
    return i if ":" in i else "%s:%s" % (MOD, i)


def name_of(i):
    i = full_id(i)
    if i.startswith("minecraft:"):
        return VZ.get(i) or i.split(":")[1]
    return NAMES.get(i) or i.split(":")[1]


def icon_file(i):
    return "img/icons/%s.png" % full_id(i).replace(":", "__")


used_icons = set()


ICON_FALLBACK = {"%s:dipping_sauce" % MOD: "minecraft:bowl"}  # 蘸料由代码画，查看器画不出图标，借碗的


def icon_img(i, cls="ic"):
    i = full_id(i)
    i = ICON_FALLBACK.get(i, i)
    used_icons.add(i)
    if not os.path.exists(os.path.join(ICONS, i.replace(":", "__") + ".png")):
        warnings.append("缺图标：%s（在查看器里导出）" % i)
        return '<span class="ic missing">?</span>'
    return '<img class="%s" src="%s" alt="%s" width="32" height="32">' % (cls, icon_file(i), html.escape(name_of(i)))


def chip(i):
    i = full_id(i)
    return '<span class="it" title="%s">%s<span>%s</span></span>' % (html.escape(i), icon_img(i), html.escape(name_of(i)))


# ---------------- 配方 ----------------
def load_recipes():
    out = {}
    for f in sorted(glob.glob(os.path.join(RECIPES, "*.json"))):
        out[os.path.basename(f)[:-5]] = json.load(open(f, encoding="utf-8"))
    return out


RECIPE_DB = load_recipes()


def result_id(r):
    res = r.get("result")
    if isinstance(res, dict):
        return full_id(res.get("id") or res.get("item"))
    return full_id(res)


def result_count(r):
    res = r.get("result")
    return res.get("count", 1) if isinstance(res, dict) else r.get("count", 1)


def ing_ref(ing):
    """配料 -> (图标 id, 提示文字)"""
    if isinstance(ing, list):
        ing = ing[0]
    if "tag" in ing:
        icon, label = TAG_ICON.get(ing["tag"], (None, ing["tag"]))
        return icon, label
    iid = full_id(ing["item"])
    return iid, name_of(iid)


def slot(ref, count=None):
    if ref is None:
        return '<div class="slot"></div>'
    iid, label = ref
    cnt = '<b class="cnt">%s</b>' % count if count and count > 1 else ""
    if iid is None:
        return '<div class="slot" title="%s"><span class="ic missing">#</span></div>' % html.escape(label)
    return '<div class="slot" title="%s">%s%s</div>' % (html.escape(label), icon_img(iid), cnt)


# 已发布版里还没有、要等下一版才发布的物品（教程基于已发布的 0.1.3-alpha，这些在配方 / 图鉴里加「0.1.4 将更新」角标）。
# 发版时把已发布的从这里删掉。
UPCOMING = set()   # 0.1.3-alpha（2026-10-25）发布时清空：牛脊肉 / 吊龙、鸳鸯锅 / 田字锅都已发布（）
BADGE = '<span class="tag dev">0.1.4 将更新</span>'


def badge(i):
    return BADGE if full_id(i).split(":")[1] in UPCOMING else ""


ARROW = ('<svg class="arrow" viewBox="0 0 22 15" width="44" height="30" shape-rendering="crispEdges">'
         '<path d="M0 5h13V0h3v2h2v2h2v2h2v3h-2v2h-2v2h-3v2h-3v-5H0z" transform="translate(0 -1) scale(1 .93)" fill="#6b6b6b"/></svg>')


def grid_recipe(r):
    shaped = r["type"].endswith("crafting_shaped")
    cells = [None] * 9
    if shaped:
        pat = [row for row in r["pattern"] if row != ""] if False else r["pattern"]
        h = len(pat)
        w = max(len(row) for row in pat)
        # 空行 / 不齐的行照样放在格子里（原样画出来，和游戏里合成时摆法一致）
        for y, row in enumerate(pat[:3]):
            for x, ch in enumerate(row[:3]):
                if ch != " " and ch in r["key"]:
                    cells[y * 3 + x] = ing_ref(r["key"][ch])
    else:
        for n, ing in enumerate(r["ingredients"][:9]):
            cells[n] = ing_ref(ing)
    # 裁掉整行 / 整列都空的边，格子小一点；但保留相对位置
    rows = [cells[0:3], cells[3:6], cells[6:9]]
    keep_r = [i for i in range(3) if any(rows[i])] or [0]
    keep_c = [j for j in range(3) if any(rows[i][j] for i in range(3))] or [0]
    grid = ""
    for i in range(keep_r[0], keep_r[-1] + 1):
        for j in range(keep_c[0], keep_c[-1] + 1):
            grid += slot(rows[i][j])
    cols = keep_c[-1] - keep_c[0] + 1
    rid = result_id(r)
    label = "合成" if shaped else "合成（无序，随便摆）"
    return ('<div class="rcp"><div class="rlabel">%s</div><div class="rbody"><div class="grid" style="--cols:%d">%s</div>%s%s</div></div>'
            % (label, cols, grid, ARROW, slot((rid, name_of(rid)), result_count(r))))


def cook_block(raw_id, entries):
    """entries: [(类型, 秒, 产物id)]"""
    rows = ""
    prod = entries[0][2]
    for kind, secs in [(e[0], e[1]) for e in entries]:
        rows += '<span class="ck">%s %s 秒</span>' % (kind, secs)
    rlabel_badge = badge(prod) or badge(raw_id)
    return ('<div class="rcp cook"><div class="rlabel">%s烹饪（熔炉 / 烟熏炉 / 营火；火锅里按烟熏炉的时间算）</div><div class="rbody">%s%s%s<div class="cks">%s</div></div></div>'
            % (rlabel_badge, slot((raw_id, name_of(raw_id))), ARROW, slot((prod, name_of(prod))), rows))


def stonecut(r):
    ing = ing_ref(r["ingredient"])
    rid = result_id(r)
    return ('<div class="rcp"><div class="rlabel">切石机</div><div class="rbody">%s%s%s</div></div>'
            % (slot(ing), ARROW, slot((rid, name_of(rid)), result_count(r))))


def cooking_map():
    """原料 id -> {类型: 秒, '产物': id}"""
    m = {}
    for rid, r in RECIPE_DB.items():
        t = r["type"].replace("minecraft:", "")
        if t in ("smelting", "smoking", "campfire_cooking"):
            raw = ing_ref(r["ingredient"])[0]
            kind = {"smelting": "熔炉", "smoking": "烟熏炉", "campfire_cooking": "营火"}[t]
            m.setdefault(raw, {"产物": result_id(r), "秒": {}})
            m[raw]["秒"][kind] = round(r.get("cookingtime", 200) / 20, 2)
    return m


COOK = cooking_map()


def recipes_for(item_id):
    item_id = full_id(item_id)
    out = []
    for rid, r in RECIPE_DB.items():
        t = r["type"].replace("minecraft:", "")
        if t in ("crafting_shaped", "crafting_shapeless", "stonecutting") and result_id(r) == item_id:
            out.append(r)
    return out


def recipe_html(item_id):
    item_id = full_id(item_id)
    parts = []
    for r in recipes_for(item_id):
        t = r["type"].replace("minecraft:", "")
        parts.append(stonecut(r) if t == "stonecutting" else grid_recipe(r))
    for raw, c in COOK.items():
        if c["产物"] == item_id:
            order = ["熔炉", "烟熏炉", "营火"]
            parts.append(cook_block(raw, [(k, c["秒"][k], c["产物"]) for k in order if k in c["秒"]]))
    if not parts:
        warnings.append("{{recipe:%s}}：没有找到这个物品的配方" % item_id)
        return ""
    return '<div class="recipes"><div class="rtitle">%s %s%s</div>%s</div>' % (icon_img(item_id), html.escape(name_of(item_id)), badge(item_id), "".join(parts))


# ---------------- 指令展开 ----------------
def expand_inline(text):
    def i_(m):
        return chip(m.group(1).strip())

    def icons_(m):
        return '<div class="chips">%s</div>' % "".join(chip(x.strip()) for x in m.group(1).split(","))

    def recipe_(m):
        return "\n\n" + "".join(recipe_html(x.strip()) for x in m.group(1).split(",")) + "\n\n"

    def img_(m):
        f, _, cap = m.group(1).partition("|")
        if not os.path.exists(os.path.join(TUT, "img", f)):
            warnings.append("缺插图：tutorial/img/%s" % f)
        return '\n\n<figure><img src="img/%s" alt="%s"><figcaption>%s</figcaption></figure>\n\n' % (f, html.escape(cap), html.escape(cap))

    # 标题写成「## 标题 {#锚点}{{since:..}}」时，把标签挪到锚点前面（attr_list 要求 {#锚点} 在行尾）
    text = re.sub(r"^(#+ .*?)\s*(\{#[\w-]+\})(\{\{(?:since:[^}]+|dev)\}\})\s*$", lambda m: "%s%s %s" % (m.group(1), m.group(3), m.group(2)), text, flags=re.M)
    text = re.sub(r"\{\{i:([^}]+)\}\}", i_, text)
    text = re.sub(r"\{\{icons:([^}]+)\}\}", icons_, text)
    text = re.sub(r"\{\{recipe:([^}]+)\}\}", recipe_, text)
    text = re.sub(r"\{\{img:([^}]+)\}\}", img_, text)
    text = re.sub(r"\{\{since:([^}]+)\}\}", lambda m: '<span class="tag since">自 %s</span>' % m.group(1), text)
    text = text.replace("{{dev}}", '<span class="tag dev">0.1.4 将更新</span>')
    return text


MD = markdown.Markdown(extensions=["tables", "attr_list", "sane_lists"])


def render_md(text):
    def callout(m):
        kind, body = m.group(1), m.group(2)
        title = {"tip": "小提示", "warn": "注意", "dev": "0.1.4 将更新的内容（本教程基于 0.1.3-alpha，这部分还没有发布）"}[kind]
        inner = render_md_plain(body)
        return '\n\n<div class="callout %s"><div class="ct">%s</div>%s</div>\n\n' % (kind, title, inner)

    text = re.sub(r"^:::(tip|warn|dev)\n(.*?)\n:::\s*$", callout, text, flags=re.S | re.M)
    return render_md_plain(text)


def render_md_plain(text):
    MD.reset()
    return MD.convert(expand_inline(text))


# ---------------- 自动页面 ----------------
def cat_of(rid):
    for w in WOODS:
        if rid.startswith(w + "_"):
            return "wood"
    return None


RECIPE_SECTIONS = [
    ("灶台、锅与筷子", lambda i: i in ("brick_stove", "tall_brick_stove", "bamboo_chopsticks") or i.endswith("_hotpot") and "grid" in i or "yuanyang" in i or i.endswith("four_grid_hotpot")),
    ("火锅桌与凳子", lambda i: i.endswith("hotpot_table") or i.endswith("_stool") or i.endswith("_bench")),
    ("盘子", lambda i: any(s in i for s in ("small_dish", "plate", "platter"))),
    ("蘸料台", lambda i: "sauce_station" in i),
    ("厨具与食材", lambda i: True),
]


def build_recipes_page():
    shown = {}
    by_result = {}
    for rid, r in RECIPE_DB.items():
        t = r["type"].replace("minecraft:", "")
        if t in ("crafting_shaped", "crafting_shapeless", "stonecutting"):
            by_result.setdefault(result_id(r).split(":")[1], []).append(r)
    ids = sorted(by_result)
    out = ["<h1>配方大全</h1>",
           "<p>这一页由脚本<b>直接读游戏里的配方文件</b>画出来，和游戏永远一致。鼠标放到格子上能看到物品名。</p>",
           '<div class="callout tip"><div class="ct">木头 / 材质省略规则</div>'
           "<p>11 种木头的火锅桌、高火锅桌、板凳、条凳、蘸料台配方都一样，只是把木板换成对应的木板，这里只画<b>橡木</b>的。"
           "盘子五种材质（石英、竹、红陶、铜、铁）的<b>小碟</b>各自单列，大盘子只画石英的，其他材质把小碟换成对应材质的小碟。</p></div>"]
    placed = set()
    for title, pred in RECIPE_SECTIONS:
        sec = []
        for i in ids:
            if i in placed:
                continue
            if not pred(i):
                continue
            if cat_of(i) and not i.startswith("oak_"):
                placed.add(i)
                continue
            if any(s in i for s in ("two_slot_plate", "three_slot", "four_slot", "five_slot", "eight_slot")) and not i.startswith("quartz_"):
                placed.add(i)
                continue
            if i.startswith("cilantro_") and "meatball" not in i:
                pass
            placed.add(i)
            sec.append(i)
        if not sec:
            continue
        out.append("<h2>%s</h2>" % title)
        for i in sec:
            out.append(recipe_html(i))
    # 烹饪
    out.append("<h2>烹饪（生 → 熟）</h2><p>这些生食材放进熔炉、烟熏炉、营火都能烤熟，<b>丢进火锅里涮</b>也一样（锅里用烟熏炉的时间，再按火力和汤底折算，见「灶台与火锅」）。</p><div class='cookgrid'>")
    for raw, c in COOK.items():
        order = ["熔炉", "烟熏炉", "营火"]
        out.append(cook_block(raw, [(k, c["秒"][k], c["产物"]) for k in order if k in c["秒"]]))
    out.append("</div>")
    return "\n".join(out)


def build_items_page():
    groups = [
        ("灶台与锅", lambda i: i in ("brick_stove", "tall_brick_stove") or "hotpot" in i and "table" not in i),
        ("火锅桌", lambda i: i.endswith("hotpot_table")),
        ("凳子", lambda i: i.endswith("_stool") or i.endswith("_bench")),
        ("盘子", lambda i: any(s in i for s in ("small_dish", "plate", "platter"))),
        ("蘸料台", lambda i: "sauce_station" in i),
        ("汤碗与饮料", lambda i: "broth_bowl" in i or "drink" in i or i == "dipping_sauce"),
        ("工具", lambda i: i in ("bamboo_chopsticks", "iron_kitchen_knife", "rolling_pin", "cutting_board")),
        ("食材", lambda i: True),
    ]
    all_ids = sorted({k.split(":")[1] for k in NAMES} - {"plates"})
    hide = {"plates", "sauce_station_top", "tall_brick_stove_top", "bowl", "drink"}
    all_ids = [i for i in all_ids if i not in hide]
    seen = set()
    out = ["<h1>物品图鉴</h1>", "<p>模组里所有物品和方块（自动生成）。<code>ID</code> 是指令里用的名字，例如 <code>/give @s orients_hotpot_chronicles:brick_stove</code>。</p>"]
    for title, pred in groups:
        sel = [i for i in all_ids if i not in seen and pred(i)]
        if not sel:
            continue
        seen.update(sel)
        out.append("<h2>%s <small>%d 种</small></h2><div class='cards'>" % (title, len(sel)))
        for i in sel:
            out.append('<div class="card">%s<div><b>%s</b>%s<code>%s</code></div></div>' % (icon_img(full_id(i)), html.escape(name_of(i)), badge(i), i))
        out.append("</div>")
    return "\n".join(out)


# ---------------- 页面外壳 ----------------
def shell(slug, title, body, nav_html, search_index_json):
    return f"""<!doctype html>
<html lang="zh-CN"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>{html.escape(title)} · 我的世界[之晗的火锅物语]模组-游玩教程</title>
<link rel="stylesheet" href="style.css"></head>
<body>
<header class="top"><button id="menu" aria-label="菜单">☰</button>
<a class="brand" href="index.html"><img src="img/icons/{MOD}__brick_stove.png" width="28" height="28" alt=""> 我的世界[之晗的火锅物语]模组 <small>游玩教程</small></a>
<input id="q" type="search" placeholder="搜索教程…" autocomplete="off"></header>
<div class="wrap"><nav id="nav">{nav_html}</nav>
<main><div id="results" hidden></div><article id="doc">{body}</article>
<footer>反馈：<a href="https://github.com/mryt233/Orients-Hotpot-Chronicles/issues/new/choose">GitHub Issues</a> · QQ 群 248314791 · <a href="https://www.curseforge.com/minecraft/mc-mods/orients-hotpot-chronicles">CurseForge</a> · <a href="https://modrinth.com/mod/orients-hotpot-chronicles">Modrinth</a><br>本教程随模组版本更新 · 配方与图鉴由游戏数据自动生成 · 作者：</footer></main></div>
<script>window.SEARCH={search_index_json};</script><script src="app.js"></script></body></html>"""


def main():
    # 把教程提到的原版物品名先准备好
    vanilla = set()
    for r in RECIPE_DB.values():
        def walk(o):
            if isinstance(o, dict):
                for k, v in o.items():
                    if k in ("item", "id") and isinstance(v, str) and v.startswith("minecraft:"):
                        vanilla.add(v)
                    else:
                        walk(v)
            elif isinstance(o, list):
                for v in o:
                    walk(v)
        walk(r)
    for f in glob.glob(os.path.join(CONTENT, "*.md")):
        for m in re.finditer(r"\{\{(?:i|icons):([^}]+)\}\}", open(f, encoding="utf-8").read()):
            for x in m.group(1).split(","):
                if x.strip().startswith("minecraft:"):
                    vanilla.add(x.strip())
    vanilla.add("minecraft:oak_log")
    refresh_vanilla(sorted(vanilla))

    os.makedirs(SITE, exist_ok=True)
    if os.path.isdir(os.path.join(SITE, "img")):
        shutil.rmtree(os.path.join(SITE, "img"))

    bodies = {}
    for slug, title, _ in PAGES:
        p = os.path.join(CONTENT, slug + ".md")
        if slug == "recipes":
            body = build_recipes_page()
            intro = os.path.join(CONTENT, "recipes.md")
            if os.path.exists(intro):
                body = render_md(open(intro, encoding="utf-8").read()) + body
        elif slug == "items":
            body = build_items_page()
        else:
            if not os.path.exists(p):
                warnings.append("缺页面 content/%s.md" % slug)
                continue
            body = render_md(open(p, encoding="utf-8").read())
        bodies[slug] = body

    nav = ""
    group = None
    for slug, title, g in PAGES:
        if g != group:
            nav += ('</ul>' if group else '') + '<h3>%s</h3><ul>' % g
            group = g
        nav += '<li><a href="%s.html" data-slug="%s">%s</a></li>' % (slug, slug, title)
    nav += "</ul>"

    search = []
    for slug, title, _ in PAGES:
        if slug in bodies:
            text = re.sub(r"<[^>]+>", " ", bodies[slug])
            text = re.sub(r"\s+", " ", html.unescape(text))
            search.append({"slug": slug, "title": title, "text": text})
    sj = json.dumps(search, ensure_ascii=False).replace("</", "<\\/")
    for slug, title, _ in PAGES:
        if slug not in bodies:
            continue
        open(os.path.join(SITE, slug + ".html"), "w", encoding="utf-8").write(shell(slug, title, bodies[slug], nav, sj))

    shutil.copy(os.path.join(TUT, "assets", "style.css"), os.path.join(SITE, "style.css"))
    shutil.copy(os.path.join(TUT, "assets", "app.js"), os.path.join(SITE, "app.js"))
    os.makedirs(os.path.join(SITE, "img", "icons"), exist_ok=True)
    for i in used_icons:
        src = os.path.join(ICONS, i.replace(":", "__") + ".png")
        if os.path.exists(src):
            shutil.copy(src, os.path.join(SITE, "img", "icons"))
    for f in glob.glob(os.path.join(TUT, "img", "*")):
        if os.path.isfile(f):
            shutil.copy(f, os.path.join(SITE, "img"))
    check_coverage(bodies)
    build_single(bodies)
    print("已生成 %d 页 → %s" % (len(bodies), os.path.join(SITE, "index.html")))
    for w in sorted(set(warnings)):
        print("警告：" + w)
    return 0


def check_coverage(bodies):
    """教程有没有跟上游戏。"""
    text_all = "".join(open(f, encoding="utf-8").read() for f in glob.glob(os.path.join(CONTENT, "*.md")))
    hide = {"plates", "sauce_station_top", "tall_brick_stove_top", "bowl", "drink"}
    for full, zh in NAMES.items():
        i = full.split(":")[1]
        if i in hide:
            continue
        # 木头 / 材质变种由「一类」代表，只要求代表出现
        base = i
        for w in WOODS:
            if i.startswith(w + "_"):
                base = None if w != "oak" else i
        if base is None:
            continue
        if i.startswith(("copper_", "iron_", "bamboo_", "terracotta_")) and ("plate" in i or "dish" in i or "platter" in i):
            continue
        if i.startswith("quartz_") and "plate" in i:
            continue
        # 熟的版本跟着生的走；铁鸳鸯锅跟着铜的走；香菜丸子由「香菜丸子」一节统一讲；内部物品不用讲
        if i == "pounded_meat":
            continue
        if i.startswith("cooked_") and (NAMES.get(MOD + ":" + i[7:], "") in text_all or i[7:] in text_all):
            continue
        if i.startswith("iron_") and ("yuanyang" in i or "four_grid" in i):
            continue
        if "cilantro_" in i and ("meatball" in i or "fish_ball" in i) and "香菜丸子" in text_all:
            continue
        if zh not in text_all and i not in text_all:
            warnings.append("教程正文没提到：%s（%s）——去 tutorial/content/ 补上" % (zh, i))
    # 移植同步清单「未发布」条目 vs 更新日志
    sync = os.path.join(ROOT, "docs", "移植同步清单.md")
    log = os.path.join(CONTENT, "changelog.md")
    if os.path.exists(sync) and os.path.exists(log):
        s = open(sync, encoding="utf-8").read()
        m = re.search(r"## 未发布.*?\n(.*?)\n## ", s, flags=re.S)
        if m:
            nums = [int(x) for x in re.findall(r"^#### (\d+)\.", m.group(1), flags=re.M)]
            lg = open(log, encoding="utf-8").read()
            done = {int(x) for x in re.findall(r"清单#(\d+)", lg)}
            for n in nums:
                if n not in done:
                    warnings.append("移植同步清单「未发布」#%d 还没写进更新日志（在对应条目后面加 <!--清单#%d--> 标记）" % (n, n))




# ---------------- 单文件长页面版（新增，2026-10-24） ----------------
def build_single(bodies, filename=None, transform=None):
    """所有页面合成一个 HTML：左侧目录（章 + 小节），右侧一直往下翻；图片全部内嵌，一个文件就能看。"""
    import base64
    import mimetypes

    sections, toc = [], ""
    group = None
    for slug, title, g in PAGES:
        if slug not in bodies:
            continue
        body = bodies[slug]
        # id 加章前缀，页内 / 跨页链接改成锚点
        body = re.sub(r'\bid="([^"]+)"', lambda m: 'id="%s-%s"' % (slug, m.group(1)), body)
        body = re.sub(r'href="#([^"]+)"', lambda m: 'href="#%s-%s"' % (slug, m.group(1)), body)
        body = re.sub(r'href="([a-z]+)\.html#([^"]+)"', lambda m: 'href="#%s-%s"' % (m.group(1), m.group(2)), body)
        body = re.sub(r'href="([a-z]+)\.html"', lambda m: 'href="#page-%s"' % m.group(1), body)
        heads = re.findall(r'<h2 id="([^"]+)">(.*?)</h2>', body)
        # 没写锚点的 h2 补一个
        n = [0]

        def add_id(m):
            n[0] += 1
            return '<h2 id="%s-s%d">' % (slug, n[0])
        body = re.sub(r"<h2>", add_id, body)
        heads = re.findall(r'<h2 id="([^"]+)">(.*?)</h2>', body)
        if g != group:
            toc += ("</ul>" if group else "") + "<h3>%s</h3><ul>" % g
            group = g
        sub = "".join('<li class="sub"><a href="#%s" data-t="%s">%s</a></li>' % (i, i, re.sub(r"<[^>]+>", "", t)) for i, t in heads)
        toc += '<li><a href="#page-%s" data-t="page-%s">%s</a></li>%s' % (slug, slug, title, sub)
        sections.append('<article id="page-%s" class="chap">%s</article>' % (slug, body))
    toc += "</ul>"
    page = "".join(sections)

    # 图片：同一张只内嵌一次，页面里用 data-k 引用
    imgs = {}

    def grab(m):
        src = m.group(1)
        path = os.path.join(TUT, src.replace("/", os.sep))
        if not os.path.exists(path):
            return m.group(0)
        if src not in imgs:
            mime = mimetypes.guess_type(path)[0] or "image/png"
            imgs[src] = "data:%s;base64,%s" % (mime, base64.b64encode(open(path, "rb").read()).decode())
        return 'data-k="%s"' % src
    page = re.sub(r'src="(img/[^"]+)"', grab, page)
    css = open(os.path.join(TUT, "assets", "style.css"), encoding="utf-8").read()
    extra = """
.chap{margin-bottom:22px;scroll-margin-top:70px}h2{scroll-margin-top:70px}
nav li.sub a{padding:2px 10px 2px 24px;font-size:13px;color:var(--sub)}nav li.sub a.on{color:var(--ink)}
nav ul>li:not(.sub)>a{font-weight:600}
#top{position:fixed;right:18px;bottom:18px;z-index:30;background:#3b342d;color:#fff;border:2px solid #1d1a17;padding:8px 12px;cursor:pointer;font:inherit;display:none}
mark{background:#ffd54a;color:#000}
"""
    js = """
(function(){
var IMG=%s;
document.querySelectorAll('img[data-k]').forEach(function(i){i.src=IMG[i.dataset.k];});
window.addEventListener('load',function(){if(location.hash){var t=document.getElementById(decodeURIComponent(location.hash.slice(1)));if(t)t.scrollIntoView()}});
var nav=document.getElementById('nav');
document.getElementById('menu').onclick=function(){nav.classList.toggle('open')};
nav.addEventListener('click',function(e){if(e.target.tagName==='A')nav.classList.remove('open')});
var links=[].slice.call(nav.querySelectorAll('a[data-t]')),targets=links.map(function(a){return document.getElementById(a.dataset.t)});
function spy(){var y=window.scrollY+90,cur=0;targets.forEach(function(t,i){if(t&&t.getBoundingClientRect().top+window.scrollY<=y)cur=i});
 links.forEach(function(a,i){a.classList.toggle('on',i===cur)});var a=links[cur];if(a){var r=a.getBoundingClientRect(),n=nav.getBoundingClientRect();if(r.top<n.top||r.bottom>n.bottom)nav.scrollTop+=r.top-n.top-60}
 document.getElementById('top').style.display=window.scrollY>600?'block':'none'}
window.addEventListener('scroll',spy,{passive:true});spy();
document.getElementById('top').onclick=function(){window.scrollTo({top:0,behavior:'smooth'})};
var q=document.getElementById('q');
q.addEventListener('keydown',function(e){if(e.key!=='Enter')return;var s=q.value.trim();if(!s)return;
 window.find&&window.find(s,false,false,true)});
q.placeholder='搜索（回车跳到下一处）';
})();
""" % json.dumps(imgs)
    html_out = f"""<!doctype html>
<html lang="zh-CN"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>我的世界[之晗的火锅物语]模组-游玩教程</title><style>{css}{extra}</style></head>
<body>
<header class="top"><button id="menu" aria-label="目录">☰</button>
<a class="brand" href="#page-index">我的世界[之晗的火锅物语]模组 <small>游玩教程</small></a>
<input id="q" type="search" autocomplete="off"></header>
<div class="wrap"><nav id="nav">{toc}</nav>
<main>{page}<footer>反馈：<a href="https://github.com/mryt233/Orients-Hotpot-Chronicles/issues/new/choose">GitHub Issues</a> · QQ 群 248314791 · <a href="https://www.curseforge.com/minecraft/mc-mods/orients-hotpot-chronicles">CurseForge</a> · <a href="https://modrinth.com/mod/orients-hotpot-chronicles">Modrinth</a><br>本教程随模组版本更新 · 配方与图鉴由游戏数据自动生成 · 作者：</footer></main></div>
<button id="top">↑ 回到顶部</button>
<script>{js}</script></body></html>"""
    # 调整：允许英文构建复用布局、内嵌图片和交互，不覆盖中文成品。
    if transform is not None:
        html_out = transform(html_out)
    out = os.path.join(TUT, filename or "我的世界[之晗的火锅物语]模组-游玩教程.html")
    open(out, "w", encoding="utf-8").write(html_out)
    print("单文件版 → %s（%.1f MB）" % (out, os.path.getsize(out) / 1e6))


if __name__ == "__main__":
    sys.exit(main())
