# -*- coding: utf-8 -*-
"""游戏内「火锅指南」的内容文件（2026-11-05 用户：书里的章节、条目、排版和文字要跟网页端书本教程完全一样）。

    python tools/gen_guide_book.py

内容不再单独维护：直接复用 tools/build_tutorial.py（正文、物品名、配方、图鉴）和 tools/build_tutorial_book.py
（分类 CATEGORIES、条目划分、条目图标 ENTRY_ICONS）读取 tutorial/content（简体）、content-zh-tw（繁体）、
content-en（英文），把每个条目的 Markdown 按源文件顺序拆成块，写 assets/orients_hotpot_chronicles/guide/
<语言>.json（zh_cn / zh_tw / en_us）。

JSON 结构（GuideBookScreen 读取；旧版的 text / rows / recipes / steps 字段仍然认，这里不再写）：
  {"categories": [{"id", "icon", "title", "entries": [{"icon", "title", "blocks": [...]}, ...]}, ...]}
块的种类（按源文件顺序排列，进游戏后从左页流到右页、再翻到下一个跨页）：
  {"k": "sub", "t": 小标题, "l": 标题级数}          网页里的 <h3>（h2 是条目本身）
  {"k": "p", "t": 段落}                            网页里的段落
  {"k": "note", "t": 标题, "b": [行...], "s": 种类}  提示框（tip / warn / dev / quote）
  {"k": "th", "t": 表头}                            表格第一行
  {"k": "tr", "icon": 物品ID, "t": 一行, "go": [分类, 条目]}  表格 / 对照表的一行，可带图标和跳转
  {"k": "bul", "t": 一项, "d": 缩进级}              无序列表
  {"k": "num", "t": 一项, "n": 序号, "d": 缩进级}   有序列表
  {"k": "icons", "ids": [物品ID...]}                一排物品图标
  {"k": "recipes", "ids": [产物ID...]}              配方块（工作台 / 切石机 / 熔炉，几个就轮换）
  {"k": "code", "l": [行...]}                       命令等原文
  {"k": "rule"}                                     分隔线

图标里的「锅ID#汤底」= 装着那种汤的锅（如 iron_nine_grid_hotpot#tomato），「…#lit」= 烧着的灶台。
网页专用的伪图标（tomato_boiling_pot、minecraft:food_full）换成游戏里真实存在的物品。
作者：mryt233
"""
import html
import json
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import build_tutorial as bt          # noqa: E402  正文、物品名、配方、图鉴
import build_tutorial_book as btb    # noqa: E402  网页书本教程的分类 / 条目划分

MODID = bt.MOD
ROOTS = [os.path.join(bt.RES, "assets", MODID)]
LANGS = [("zh_cn", "zh_cn"), ("zh_tw", "zh_tw"), ("en_us", "en")]

warnings = []

# 网页上的伪图标 → 游戏里真实存在的物品（mryt233，2026-11-05）
ICON_MAP = {
    "tomato_boiling_pot": "iron_nine_grid_hotpot#tomato",   # 网页上是逐帧渲染的番茄锅动图
    "minecraft:food_full": "minecraft:cooked_beef",         # 网页上是原版鸡腿图标
}
# 教程正文里偶尔还会写改名前的老 ID（正文由教程维护，这里只做显示上的兼容，不悄悄换成别的物品）
ALIAS = {
    "tofu": "cold_tofu",            # r134 起豆腐分冷热，老的「豆腐」= 豆腐（冷）
    "cup": "glass_cup",             # 玻璃杯
}
# 汤底名（GuideBookScreen 里按 SoupBase 枚举查，写错了锅图标是空的）
SOUP_NAMES = {"clear", "rich_stock", "mushroom", "spicy", "tomato", "three_fresh"}
# {{potviewer}}（网页上的三维锅形展示）在游戏里换成六个锅的物品图标
POT_IDS = ["copper_nine_grid_hotpot", "copper_taiji_yuanyang_hotpot", "copper_round_yuanyang_hotpot",
           "copper_split_yuanyang_hotpot", "copper_four_grid_hotpot", "copper_square_yuanyang_hotpot"]

CAT_IDS = ["start", "hotpot", "ingredients", "sauce", "tableware", "tables", "reference", "preview", "about"]
# 提示框标题：build_tutorial 的界面表为了不影响正文，把「小提示」「注意」从替换表里拿掉了，
# 书本网页是换成 <div class="ct"> 再替换的，这里直接取各语言的界面文字
NOTE_TITLE = {}


# ---------------------------------------------------------------- 基础
def full(i):
    """补全命名空间；「锅ID#汤底」只补前面的 ID。"""
    i = ICON_MAP.get(i.strip(), i.strip())
    base, sep, tail = i.partition("#")
    if ":" not in base and base in ALIAS:
        base = ALIAS[base]
    return bt.full_id(base) + sep + tail


def base_id(i):
    return full(i).split("#", 1)[0]


def item_name(i):
    """物品显示名（按当前语言）。"""
    return bt.name_of(base_id(i))


def split_ids(s):
    return [x.strip() for x in s.split(",") if x.strip()]


def L(s):
    """生成器里的固定中文 → 当前语言（正文本身来自各语言的 content 目录）。"""
    return btb.L(s)


# ---------------------------------------------------------------- 行内
def inline(text, items):
    """Markdown 行内语法 → 一行纯文本；用到的物品 ID 追加进 items。"""
    def one(x):
        fid = full(x)
        items.append(fid)
        return item_name(fid)

    text = re.sub(r"\{\{i:([^}]+)\}\}", lambda m: "、".join(one(x) for x in split_ids(m.group(1))), text)
    text = re.sub(r"\{\{icons:([^}]+)\}\}", lambda m: "、".join(one(x) for x in split_ids(m.group(1))), text)
    text = re.sub(r"\{\{recipe:[^}]*\}\}", "", text)
    text = re.sub(r"\{\{img:[^}]*\}\}", "", text)
    text = re.sub(r"\{\{since:[^}]*\}\}", "", text)
    text = text.replace("{{potviewer}}", "")
    text = text.replace("{{dev}}", "（%s）" % L("开发版预览"))
    text = re.sub(r"<((?:https?|mailto):[^>]+)>", r"\1", text)          # 自动链接 <https://…>
    text = re.sub(r"\[([^\]]*)\]\([^)]*\)", r"\1", text)                 # 站内 / 站外链接只留文字
    text = re.sub(r"</?[A-Za-z][^>]*>", "", text)                        # <b> <code> <span …>
    text = text.replace("**", "").replace("`", "")
    text = re.sub(r"(?<![\w*])\*(?=\S)(.+?)(?<=\S)\*(?![\w*])", r"\1", text)
    text = html.unescape(text)
    text = re.sub(r"[ \t]+", " ", text)
    return text.strip()


# ---------------------------------------------------------------- 块
LIST_RE = re.compile(r"^(\s*)(?:([-*+])|(\d+)[.)])\s+(.*)$")
HEAD_RE = re.compile(r"^(#{1,6})\s+(.*)$")
FENCE_RE = re.compile(r"^\s*`")
RULE_RE = re.compile(r"^\s*(?:-{3,}|\*{3,}|_{3,})\s*$")
SEP_CELL = re.compile(r"^:?-{3,}:?$")


def heading_text(s):
    s = re.sub(r"\{#[^}]*\}", "", s)
    s = re.sub(r"\{\{[^}]*\}\}", "", s)
    return s.strip().rstrip("#").strip()


def standalone(text):
    """整行是一个指令 → 对应一个块；不是指令返回 False。"""
    t = text.strip()
    if t == "{{potviewer}}":
        return {"k": "icons", "ids": [full(p) for p in POT_IDS]}
    m = re.match(r"^\{\{icons:([^}]+)\}\}$", t)
    if m:
        return {"k": "icons", "ids": [full(x) for x in split_ids(m.group(1))]}
    m = re.match(r"^\{\{recipe:([^}]+)\}\}$", t)
    if m:
        return {"k": "recipes", "ids": [full(x) for x in split_ids(m.group(1))]}
    if t.startswith("{{img:") or t.startswith("{{BTimg:"):      # 网页截图不在模组资源里，游戏内不显示
        return None
    return False


def paragraph_block(lines):
    items = []
    text = inline(" ".join(x.strip() for x in lines), items)
    if not text:
        return []
    out = []
    ids = []
    for i in items:
        if i not in ids:
            ids.append(i)
    if ids:                          # 正文里提到的物品，先在旁边摆一排图标
        out.append({"k": "icons", "ids": ids})
    out.append({"k": "p", "t": text})
    return out


def table_blocks(lines):
    rows = []
    for ln in lines:
        t = ln.strip()
        if not t.startswith("|"):
            continue
        rows.append([c.strip() for c in re.split(r"(?<!\\)\|", t.strip("|"))])
    rows = [r for r in rows if any(c for c in r)]
    if not rows:
        return []
    out = []
    header = None
    for cells in rows:
        if len(cells) > 1 and all(SEP_CELL.match(c or "-") for c in cells):
            continue
        if header is None:
            header = cells
            continue
        its, texts = [], []
        for n, c in enumerate(cells):
            cell_items = []
            txt = inline(c, cell_items)
            if n == 0 and cell_items and txt == "、".join(item_name(x) for x in cell_items):
                txt = ""             # 第一格只有物品：图标已经表示，不用再写一遍名字
            its.extend(cell_items)
            if txt:
                texts.append(txt)
        row = {"k": "tr", "t": "　".join(texts)}
        if its:
            row["icon"] = its[0]
        if row["t"] or row.get("icon"):
            out.append(row)
    if header:
        out.insert(0, {"k": "th", "t": "　".join(inline(c, []) for c in header)})
    return out


def flatten(blocks):
    """提示框里的块拍平成几行文字（游戏里的提示框只有一条竖条 + 文字）。"""
    lines = []
    for b in blocks:
        k = b["k"]
        if k in ("p", "sub"):
            lines.append(b["t"])
        elif k in ("bul", "num"):
            lines.append(("• " if k == "bul" else "%d. " % b.get("n", 1)) + b["t"])
        elif k == "tr":
            lines.append(((item_name(b["icon"]) + " ") if b.get("icon") else "") + b["t"])
        elif k == "icons":
            lines.append("、".join(item_name(i) for i in b["ids"]))
        elif k == "code":
            lines.extend(b["l"])
        elif k == "note":
            lines.append(b["t"])
            lines.extend(b["b"])
    return [x for x in lines if x]


def md_blocks(md):
    """一段条目正文的 Markdown → 按源文件顺序排列的块。"""
    md = re.sub(r"<!--.*?-->", "", md, flags=re.S)
    lines = md.splitlines()
    out = []
    i, n = 0, len(lines)
    while i < n:
        raw = lines[i]
        s = raw.strip()
        if not s:
            i += 1
            continue
        # 提示框
        if s.startswith(":::") and s != ":::":
            kind = s[3:].strip() or "tip"
            body = []
            i += 1
            while i < n and lines[i].strip() != ":::":
                body.append(lines[i])
                i += 1
            i += 1
            title = {"tip": NOTE_TITLE.get("tip", "小提示"), "warn": NOTE_TITLE.get("warn", "注意"),
                     "dev": L("开发版预览（还没正式发布，名字和数值以后可能会变）")}.get(
                         kind, NOTE_TITLE.get("tip", "小提示"))
            out.append({"k": "note", "t": title, "b": flatten(md_blocks("\n".join(body))), "s": kind})
            continue
        # 代码块
        if FENCE_RE.match(raw):
            body = []
            i += 1
            while i < n and not FENCE_RE.match(lines[i]):
                body.append(lines[i].rstrip())
                i += 1
            i += 1
            if body:
                out.append({"k": "code", "l": body})
            continue
        # 整行的指令
        b = standalone(s)
        if b is not False:
            if b:
                out.append(b)
            i += 1
            continue
        # 表格
        if s.startswith("|"):
            body = []
            while i < n and lines[i].strip().startswith("|"):
                body.append(lines[i])
                i += 1
            out.extend(table_blocks(body))
            continue
        # 标题
        m = HEAD_RE.match(s)
        if m:
            level = len(m.group(1))
            text = inline(heading_text(m.group(2)), [])
            if text:
                out.append({"k": "sub", "t": ("◆ " if level >= 3 else "") + text, "l": level})
            i += 1
            continue
        # 引用
        if s.startswith(">"):
            body = []
            while i < n and lines[i].strip().startswith(">"):
                body.append(lines[i].strip()[1:].strip())
                i += 1
            out.append({"k": "note", "t": "", "b": [inline(x, []) for x in body if x], "s": "quote"})
            continue
        # 分隔线
        if RULE_RE.match(raw):
            out.append({"k": "rule"})
            i += 1
            continue
        # 列表
        m = LIST_RE.match(raw)
        if m:
            counters = {}
            while i < n:
                mm = LIST_RE.match(lines[i])
                if not mm:
                    break
                indent = len(mm.group(1).replace("\t", "  ")) // 2
                its = []
                text = inline(mm.group(4), its)
                if mm.group(2):
                    blk = {"k": "bul", "t": text, "d": indent}
                else:
                    num = counters.get(indent, int(mm.group(3)) - 1) + 1
                    counters[indent] = num
                    blk = {"k": "num", "t": text, "n": num, "d": indent}
                if its:                     # 这一项提到的物品，在左边摆一个小图标
                    blk["icon"] = its[0]
                out.append(blk)
                i += 1
            continue
        # 段落：一直收到空行 / 另一个块开头
        body = [raw]
        i += 1
        while i < n:
            nxt = lines[i]
            t = nxt.strip()
            if (not t or t.startswith(("|", ">", ":::", "`")) or HEAD_RE.match(t) or LIST_RE.match(nxt)
                    or RULE_RE.match(nxt) or standalone(t) is not False):
                break
            body.append(nxt)
            i += 1
        out.extend(paragraph_block(body))
    return out


# ---------------------------------------------------------------- 条目
def section_heading(sec):
    for ln in sec.splitlines():
        if ln.startswith("#"):
            return ln
    return ""


def toc_blocks(entries):
    """每章第一页末尾的「本章目录」：后面每条的图标、标题、开头一句（和网页书本一样）。"""
    blocks = [{"k": "sub", "t": L("本章目录"), "l": 3},
              {"k": "p", "t": L("点下面的条目直接翻过去；也可以点书顶的书签，或者点右下角的三角一页页往后翻。")}]
    for ei, e in enumerate(entries[1:], 1):
        snip = ""
        for b in e["blocks"]:
            if b["k"] in ("p", "bul", "num"):
                snip = b["t"]
            elif b["k"] == "note":
                snip = " ".join(b.get("b", []))
            if snip:
                break
        snip = re.sub(r"\s+", " ", snip).strip()
        snip = snip[:34] + ("…" if len(snip) > 34 else "")
        blocks.append({"k": "tr", "icon": e["icon"], "t": "%s　%s" % (e["title"], snip), "go": None})
    return blocks


def build_entries(lang, cat_title, cat_icon, sources, cat_i):
    """一个分类下的条目：和 build_tutorial_book.build_lang 同样的划分、顺序、标题和图标。"""
    entries = []
    for slug, by_h2 in sources:
        if slug == "@recipes":
            for title, icon, blocks in recipe_sections():
                entries.append({"icon": full(icon), "title": title, "blocks": blocks})
            continue
        if slug == "@items":
            entries.append({"icon": full("minecraft:book"), "title": L("物品图鉴"), "blocks": items_blocks()})
            continue
        if slug == "@changelog":
            for title, icon, blocks in changelog_entries():
                entries.append({"icon": full(icon), "title": title, "blocks": blocks})
            continue
        md = open(os.path.join(bt.CONTENT, slug + ".md"), encoding="utf-8").read()
        for si, sec in enumerate(btb.split_sections(md, by_h2)):
            head = section_heading(sec)
            title = L(btb.ENTRY_TITLES.get((slug, si), "")) or btb.entry_title(head) or L(cat_title)
            icon = btb.ENTRY_ICONS.get((slug, si)) or btb.first_item(sec) or cat_icon
            blocks = md_blocks(sec)
            # 条目标题在书里由「图标 + 标题 + 一道线」画，正文开头的一级 / 二级标题去掉
            if blocks and blocks[0]["k"] == "sub" and blocks[0].get("l", 3) <= 2:
                blocks = blocks[1:]
            entries.append({"icon": full(icon), "title": title, "blocks": blocks})
    # 本章目录：第一条末尾列出后面的条目，点了直接翻过去（和网页书本一样）
    if len(entries) > 1:
        toc = toc_blocks(entries)
        nxt = 0
        for b in toc:
            if b["k"] == "tr":
                b["go"] = [cat_i, nxt + 1]
                nxt += 1
        entries[0]["blocks"] = entries[0]["blocks"] + toc
    for e in entries:
        if not e["blocks"]:
            warnings.append("%s 分类「%s」/ %s 是空的" % (lang, cat_title, e["title"]))
    return entries


# ---------------------------------------------------------------- 查阅：合成表大全
def recipe_sections():
    """「查阅」分类的合成表大全：读游戏配方，按网页书本同样的分组和顺序生成。"""
    groups, cooking, cutting = {}, [], []
    for k, r, dev in btb.load_recipes_all():
        t = r["type"].split(":")[1]
        if t in ("smelting", "smoking", "campfire_cooking"):
            raw = bt.ing_ref(r["ingredient"])[0]
            cooking.append((raw, bt.result_id(r), t, round(r.get("cookingtime", 200) / 20, 2)))
            continue
        rid = bt.result_id(r)
        if t == "stonecutting":
            cutting.append(rid)
            continue
        # mryt233 改（2026-11-09，r146b）：布围裙换款配方（apron_recolor）也进合成表大全
        if t == "apron_recolor":
            groups.setdefault(btb.base_of(rid.split(":")[1]), []).append((rid, dev))
            continue
        if not ("pattern" in r or "ingredients" in r):
            warnings.append("看不懂的配方类型：%s" % k)
            continue
        groups.setdefault(btb.base_of(rid.split(":")[1]), []).append((rid, dev))

    order = ["brick_stove", "tall_brick_stove", "nine_grid_hotpot", "split_pot", "chopsticks",
             "hotpot_table", "tall_hotpot_table", "stool", "bench", "small_dish"]
    out, used = [], set()
    for title, icon, pred in btb.RG_SECTIONS:
        cards = []
        for b in sorted(groups, key=lambda x: (all(v[1] for v in groups[x]), order.index(x) if x in order else 99, x)):
            if b in used or not pred(b):
                continue
            used.add(b)
            vs = groups[b]
            vs.sort(key=lambda v: (v[1], 0 if v[0].split(":")[1].startswith("oak_") else 1, v[0]))
            ids = [v[0] for v in vs]
            gt = btb.GROUP_TITLES.get(b)
            ct = btb.count_title(L(gt), len(ids)) if gt else btb.common_title([item_name(i) for i in ids], len(ids))
            cards.append((ct, ids))
        if not cards:
            continue
        blocks = []
        for ct, ids in cards:
            blocks.append({"k": "sub", "t": ct, "l": 3})
            blocks.append({"k": "recipes", "ids": ids})
        out.append((L(title), full(icon), blocks))
    if cutting:
        cut = sorted(set(cutting))
        out.append((L("切石机"), full("minecraft:stonecutter"),
                    [{"k": "sub", "t": btb.count_title(L("切石机"), len(cut)), "l": 3},
                     {"k": "recipes", "ids": cut}]))
    # 烹饪：生 → 熟，按原料一行，写清熔炉 / 烟熏炉 / 营火各要几秒
    if cooking:
        by_raw = {}
        for raw, out_id, kind, secs in cooking:
            by_raw.setdefault(raw, {"out": out_id, "t": {}})["t"][L(
                {"smelting": "熔炉", "smoking": "烟熏炉", "campfire_cooking": "营火"}[kind])] = secs
        rows = [{"k": "sub", "t": btb.count_title(L("生 → 熟"), len(by_raw)), "l": 3}]
        for raw, c in sorted(by_raw.items()):
            times = "　".join("%s %g %s" % (k, c["t"][k], L("秒")) for k in (L("熔炉"), L("烟熏炉"), L("营火"))
                             if k in c["t"])
            rows.append({"k": "tr", "icon": full(raw), "t": "%s　%s" % (item_name(c["out"]), times)})
        out.append((L("烹饪"), full("minecraft:furnace"), rows))
    return out


# ---------------------------------------------------------------- 查阅：物品图鉴
def items_blocks():
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
    all_ids = sorted({k.split(":")[1] for k in bt.NAMES} - {"plates"})
    # mryt233 改：饮料炼药锅只有方块状态，没有可列入图鉴的独立物品。
    hide = {"plates", "sauce_station_top", "tall_brick_stove_top", "bowl", "drink", "drink_cauldron", "sauce_bottle"}
    all_ids = [i for i in all_ids if i not in hide]
    ex = "/give @s %s:brick_stove" % MODID
    blocks = [{"k": "p", "t": inline(L("模组里所有物品和方块（自动生成）。<code>ID</code> 是指令里用的名字，例如 ")
                                    + "<code>" + ex + "</code>。", [])}]
    seen = set()
    for title, pred in groups:
        sel = [i for i in all_ids if i not in seen and pred(i)]
        if not sel:
            continue
        seen.update(sel)
        blocks.append({"k": "sub", "t": btb.count_title(L(title), len(sel)), "l": 2})
        for i in sel:
            fid = full(i)
            name = item_name(fid)
            if i in bt.UPCOMING:
                name += "（%s）" % L("开发版预览")
            blocks.append({"k": "tr", "icon": fid, "t": "%s　%s" % (name, i)})
    return blocks


# ---------------------------------------------------------------- 关于：更新日志
def changelog_entries():
    """和网页书本一样：各正式版合成「更新日志」一条（维护提示框不进书）。"""
    md = open(os.path.join(bt.CONTENT, "changelog.md"), encoding="utf-8").read()
    secs = btb.split_sections(md, True)
    intro = re.sub(r"(?ms)^:::tip\n\*\*(?:维护提示|維護提示|Maintenance note).*?\n:::\s*$", "", secs[0])
    preview_title = "## " + L("开发版预览")
    preview = [x for x in secs[1:] if x.lstrip().startswith(preview_title)]
    rest = [x for x in secs[1:] if x not in preview]
    blocks = md_blocks(intro + "\n\n" + "\n\n".join(rest))
    if blocks and blocks[0]["k"] == "sub" and blocks[0].get("l", 3) <= 2:
        blocks = blocks[1:]
    return [(L("更新日志"), full("minecraft:written_book"), blocks)]


# ---------------------------------------------------------------- 组装
def build_lang(lang):
    btb.set_lang(lang)
    NOTE_TITLE.clear()
    if lang != "zh_cn":
        mod = __import__("build_tutorial_en" if lang == "en" else "build_tutorial_tw")
        NOTE_TITLE.update(tip=mod.UI["小提示"], warn=mod.UI["注意"])
    cats = []
    for ci, (title, icon, sources) in enumerate(btb.CATEGORIES):
        cid = CAT_IDS[ci] if ci < len(CAT_IDS) else "cat%d" % ci
        # 游戏内指南不收录开发版预览和关于（下载、安装与外部版本信息，用户 2026-11-06 定）
        if cid in ("preview", "about"):
            continue
        entries = build_entries(lang, title, icon, sources, len(cats))
        cats.append({"id": cid,
                     "icon": full(icon), "title": L(title), "entries": entries})
    return {"categories": cats}


# ---------------------------------------------------------------- 自检
def vanilla_ids():
    """原版物品 ID 全集：查看器缓存里导出的原版物品模型（含方块物品），没有就用教程里的原版名表。"""
    out = set()
    d = os.path.join(bt.ROOT, "..", "viewer", "cache", "1.21.1", "assets", "minecraft", "models", "item")
    if os.path.isdir(d):
        out |= {"minecraft:" + f[:-5] for f in os.listdir(d) if f.endswith(".json")}
    for p in ("vanilla_zh.json", "vanilla_zh_tw.json"):
        f = os.path.join(bt.TUT, "data", p)
        if os.path.exists(f):
            out |= set(json.load(open(f, encoding="utf-8")))
    return out


def check(book, vanilla):
    """物品 ID 都要真实存在——拼错了游戏里就是空图标；配方得找得到；汤底名要对。"""
    names = {p[:-5] for p in os.listdir(os.path.join(ROOTS[0], "models", "item")) if p.endswith(".json")}
    results = {bt.result_id(r) for r in bt.RECIPE_DB.values()}

    def check_id(i, where):
        base, sep, tail = i.partition("#")
        if base.startswith(MODID + ":"):
            if base.split(":")[1] not in names:
                warnings.append("没有这个物品：%s（%s）" % (i, where))
        elif base.startswith("minecraft:"):
            if vanilla and base not in vanilla:
                warnings.append("原版没有这个物品：%s（%s）" % (i, where))
        else:
            warnings.append("不认识这个物品：%s（%s）" % (i, where))
        if sep:
            if tail == "lit":
                return
            if tail not in SOUP_NAMES:
                warnings.append("没有这种汤底：%s（%s）" % (i, where))

    for ci, cat in enumerate(book["categories"]):
        check_id(cat["icon"], "分类 %d 图标" % ci)
        if not cat["entries"]:
            warnings.append("分类 %s 没有条目" % cat["title"])
        for ei, e in enumerate(cat["entries"]):
            check_id(e["icon"], "%s 第 %d 条图标" % (cat["title"], ei + 1))
            if not e["blocks"]:
                warnings.append("%s / %s 没有内容" % (cat["title"], e["title"]))
            for bi, b in enumerate(e["blocks"]):
                where = "%s / %s 第 %d 块" % (cat["title"], e["title"], bi + 1)
                if b.get("icon"):
                    check_id(b["icon"], where)
                for i in b.get("ids", []):
                    check_id(i, where)
                if b["k"] == "recipes":
                    for i in b["ids"]:
                        if i not in results:
                            warnings.append("找不到 %s 的配方（%s）" % (i, where))
                if b["k"] == "tr" and b.get("go"):
                    c, en = b["go"]
                    if not (0 <= c < len(book["categories"]) and 0 <= en < len(book["categories"][c]["entries"])):
                        warnings.append("目录跳转越界（%s）" % where)


# ---------------------------------------------------------------- 页数估算
NARROW = set("ilI.,:;'!|()[]{}`-")


def text_width(s):
    w = 0
    for ch in s:
        if ch == " ":
            w += 4
        elif ch in NARROW:
            w += 3
        elif ord(ch) < 128:
            w += 6
        else:
            w += 9
    return w


def wrap_width(s, maxw):
    """按汉字逐个断行、西文按词断行，和游戏里 font.split 的效果接近（只用来估算页数）。"""
    lines, cur = [], ""
    for tok in re.findall(r"[⺀-￿]|\s+|[^\s⺀-￿]+", s):
        if cur and text_width(cur + tok) > maxw:
            lines.append(cur.rstrip())
            cur = tok.strip()
        else:
            cur += tok
    if cur.strip():
        lines.append(cur)
    return lines or [""]


def list_width(b):
    """列表项的文字宽度：和 GuideBookScreen.block 里 bul / num 的 textX 对应。"""
    text_x = {(True, "bul"): 15, (True, "num"): 22, (False, "bul"): 8, (False, "num"): 11}[
        (bool(b.get("icon")), b["k"])]
    return 112 - text_x - int(b.get("d", 0)) * 4


def block_height(b):
    k = b["k"]
    if k == "sub":
        return len(wrap_width(b["t"], 112)) * 9 + 6 + 3
    if k == "p":
        return len(wrap_width(b["t"], 112)) * 9
    if k == "note":
        return (len(wrap_width(b["t"], 106)) + len(wrap_width(" ".join(b["b"]), 106))) * 9 + 6
    if k == "th":
        return len(wrap_width(b["t"], 112)) * 9 + 3
    if k == "tr":
        return max(14, len(wrap_width(b["t"], 97)) * 9 + 5)
    if k in ("bul", "num"):
        return max(12 if b.get("icon") else 0, len(wrap_width(b["t"], list_width(b))) * 9 + 3)
    if k == "icons":
        n = len(b["ids"])
        return max(1, (n + 5) // 6) * 18 + 2     # 一排 6 个图标（和 GuideBookScreen.ICON_PER_ROW 一致）
    if k == "recipes":
        return 66
    if k == "code":
        return len(b["l"]) * 9 + 6
    return 7


def split_block(b):
    """一块比一页还高（英文长段落、长提示框）就按行切成几块（GuideBookScreen.splitBlock）。"""
    bh = block_height(b)
    if bh <= 144:
        return [bh]
    if b["k"] == "icons":
        per = max(1, (144 - 2) // 18 * 6)
        n = len(b["ids"])
        return [max(1, (min(n, per + i) - i + 5) // 6) * 18 + 2 for i in range(0, n, per)]
    if b["k"] == "note":
        pad = 6
    elif b["k"] == "tr":
        pad = 5
    elif b["k"] in ("bul", "num"):
        pad = 3
    else:
        pad = 0
    if b["k"] == "note":
        lines = len(wrap_width(b["t"], 106)) + len(wrap_width(" ".join(b["b"]), 106))
    elif b["k"] == "tr":
        lines = len(wrap_width(b["t"], 97))
    elif b["k"] in ("bul", "num"):
        lines = len(wrap_width(b["t"], list_width(b)))
    else:
        lines = len(wrap_width(b["t"], 112))
    step = max(1, (144 - pad) // 9)
    return [pad + min(step, lines - i) * 9 for i in range(0, lines, step)]


def estimate_pages(book):
    """按 GuideBookScreen 的排版（两页一个跨页、每页 144 高、装不下就翻页）估算页数。"""
    cols = 0
    for cat in book["categories"]:
        for e in cat["entries"]:
            c, used = 1, 18
            for b in e["blocks"]:
                for bh in split_block(b):
                    if used + bh > 144 and used > 0:
                        c += 1
                        used = 0
                    used += bh
            cols += c
    return cols, (cols + 1) // 2


# ---------------------------------------------------------------- 主流程
def main():
    btb.use_release_recipes()      # 配方照已发布版画（和网页教程一致）
    vanilla = vanilla_ids()
    books = {}
    for lang_json, lang in LANGS:
        book = build_lang(lang)
        check(book, vanilla)
        books[lang_json] = book
    btb.set_lang("zh_cn")
    for lang_json, _ in LANGS:
        book = books[lang_json]
        out = os.path.join(ROOTS[0], "guide", lang_json + ".json")
        os.makedirs(os.path.dirname(out), exist_ok=True)
        with open(out, "w", encoding="utf-8") as f:   # 和原来的生成脚本一样：Windows 下就是 CRLF
            f.write(json.dumps(book, ensure_ascii=False, indent=1) + "\n")
        cats = book["categories"]
        entries = sum(len(c["entries"]) for c in cats)
        blocks = sum(len(e["blocks"]) for c in cats for e in c["entries"])
        cols, pages = estimate_pages(book)
        print("%-6s %d 分类 %3d 条目 %4d 块 %5d 页（%d 列）" % (lang_json, len(cats), entries, blocks, pages, cols))
    print("物品图标 / 配方 / 汤底名检查完毕。")
    for w in sorted(set(warnings)):
        print("警告：" + w)


if __name__ == "__main__":
    main()
