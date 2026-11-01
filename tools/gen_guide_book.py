"""游戏内「火锅指南」的内容文件（2026-10-25 用户要一本游戏内教程书；2026-10-26 用户：改成真的指南那样——
左边分类书签、顶上条目书签、每条一个跨页，内容简洁高效，所以不再整篇搬网页教程）。

    python tools/gen_guide_book.py
读 tools/guide_content.py（简体 ZH、英文 EN），写 assets/orients_hotpot_chronicles/guide/<语言>.json（两份资源目录都写）：
  {"categories": [{"id", "icon", "title", "entries": [{"icon", "title"?, "get"?, "text"?, "rows"?, "recipes"?,
                                                      "steps"?, "tip"?}, ...]}, ...]}
物品 ID 都补全命名空间。繁体 zh_tw 由简体用 opencc（s2twp）转换；其他语言游戏里退回英文。
作者：mryt233
"""
import json
from pathlib import Path

import opencc

from guide_content import EN, ZH

MOD = Path(__file__).resolve().parent.parent
MODID = "orients_hotpot_chronicles"
ROOTS = [MOD / "common/src/main/resources/assets" / MODID]


def full(i):
    return i if ":" in i else f"{MODID}:{i}"


def normalize(book, conv=None):
    t = conv or (lambda s: s)
    out = []
    for cat in book:
        entries = []
        for e in cat["entries"]:
            n = {"icon": full(e["icon"])}
            for k in ("title", "get", "text", "tip"):
                if k in e:
                    n[k] = t(e[k])
            if "rows" in e:
                n["rows"] = [[full(i), t(s)] for i, s in e["rows"]]
            if "recipes" in e:
                n["recipes"] = [full(r) for r in e["recipes"]]
            if "steps" in e:
                n["steps"] = [t(s) for s in e["steps"]]
            entries.append(n)
        out.append({"id": cat["id"], "icon": full(cat["icon"]), "title": t(cat["title"]), "entries": entries})
    return {"categories": out}


def check(book):
    """物品 ID 都要在模组资源里有（本模组的）——拼错了游戏里会显示成空图标。"""
    names = {p.stem for p in (ROOTS[0] / "models/item").glob("*.json")}
    bad = set()
    for cat in book["categories"]:
        ids = [cat["icon"]] + [e["icon"] for e in cat["entries"]]
        for e in cat["entries"]:
            ids += [r[0] for r in e.get("rows", [])] + e.get("recipes", [])
        bad |= {i for i in ids if i.startswith(MODID + ":") and i.split(":")[1].split("#")[0] not in names}
    for i in sorted(bad):
        print("警告：没有这个物品", i)


def main():
    tw = opencc.OpenCC("s2twp").convert
    books = {"zh_cn": normalize(ZH), "zh_tw": normalize(ZH, tw), "en_us": normalize(EN)}
    for lang, book in books.items():
        check(book)
        for root in ROOTS:
            out = root / "guide" / f"{lang}.json"
            out.parent.mkdir(parents=True, exist_ok=True)
            out.write_text(json.dumps(book, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")
        cats = book["categories"]
        print(lang, len(cats), "分类", sum(len(c["entries"]) for c in cats), "条")


if __name__ == "__main__":
    main()
