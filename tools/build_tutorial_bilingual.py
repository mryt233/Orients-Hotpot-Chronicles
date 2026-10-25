# -*- coding: utf-8 -*-
"""作者：。把简中、zh_tw 及英文教程合成带语言选择的离线 HTML。

用法（mod/ 下）：python tools/build_tutorial_bilingual.py
先更新三种语言教程；本脚本只读取成品，共享内嵌图片，不需要联网。
"""
from pathlib import Path
import json
import re

TUTORIAL = Path(__file__).resolve().parents[1] / "tutorial"
OUTPUT = TUTORIAL / "Minecraft_Orients_Hotpot_Chronicles_Player_Guide.html"


def read_page(filename):
    text = (TUTORIAL / filename).read_text(encoding="utf-8")
    def extract(pattern):
        match = re.search(pattern, text, re.S)
        if match is None:
            raise ValueError(f"Unexpected tutorial format: {filename}: {pattern}")
        return match.group(1)
    return {
        "title": extract(r"<title>(.*?)</title>"),
        "brand": extract(r'<a class="brand"[^>]*>(.*?)</a>'),
        "nav": extract(r'<nav id="nav">(.*?)</nav>'),
        "main": extract(r"<main>(.*?)</main>"),
    }, json.loads(extract(r"var IMG=(.*?);\s*document\.querySelectorAll")), extract(r"<style>(.*?)</style>")


def main():
    zh, images, _ = read_page("我的世界[之晗的火锅物语]模组-游玩教程.html")
    en, english_images, css = read_page("Minecraft_Orients_Hotpot_Chronicles_Player_Guide_EN.html")
    tw, taiwan_images, _ = read_page("Minecraft_Orients_Hotpot_Chronicles_Player_Guide_zh_tw.html")
    for locale, page, localized_images in (("en", en, english_images), ("zh-TW", tw, taiwan_images)):
        if images != localized_images:
            raise ValueError(f"Image sets differ for {locale}; rebuild the tutorials first")
        for name in ("nav", "main"):
            if re.findall(r'\bid="([^"]+)"', zh[name]) != re.findall(r'\bid="([^"]+)"', page[name]):
                raise ValueError(f"Anchors differ for {locale}: {name}")
    payload = json.dumps({"zh-CN": zh, "zh-TW": tw, "en": en}, ensure_ascii=False).replace("</", "<\\/")
    image_json = json.dumps(images).replace("</", "<\\/")
    js = (TUTORIAL / "assets" / "bilingual.js").read_text(encoding="utf-8")
    page = f'''<!doctype html>

<html lang="zh-CN"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>{zh['title']}</title><style>{css}
#language{{flex:none;background:#1d1a17;color:#fff;border:2px solid #7a6f60;padding:6px 10px;cursor:pointer;font:inherit;font-size:14px}}
.brand{{flex:1}}#q{{margin-left:0}}
@media(max-width:820px){{.top{{gap:8px}}#q{{order:4}}.brand{{font-size:13px}}#language{{font-size:13px;padding:6px}}}}
@media(max-width:820px){{nav{{top:var(--guide-header-height,106px)}}}}
</style></head><body>
<header class="top"><button id="menu" aria-label="目录">☰</button>
<a class="brand" href="#page-index">{zh['brand']}</a>
<input id="q" type="search" aria-label="搜索教程" autocomplete="off">
<select id="language" aria-label="语言选择">
<option value="zh-CN" lang="zh-CN">简体中文</option>
<option value="zh-TW" lang="zh-TW">zh_tw</option>
<option value="en" lang="en">English</option></select></header>
<div class="wrap"><nav id="nav" aria-label="目录"></nav><main></main></div>
<noscript>请启用 JavaScript 查看教程及切换语言。請啟用 JavaScript 閱讀指南及切換語言。 Enable JavaScript to view this guide and switch languages.</noscript>
<button id="top">↑ 回到顶部</button>
<script type="application/json" id="translations">{payload}</script>
<script type="application/json" id="images">{image_json}</script>
<script>{js}</script></body></html>'''
    if any(term in page for term in ("台灣", "台湾", "繁體中文", "繁体中文")):
        raise ValueError("Use zh_tw for public language labels")
    OUTPUT.write_text(page, encoding="utf-8")
    print(f"Trilingual guide: {OUTPUT} ({OUTPUT.stat().st_size:,} bytes; {len(images)} shared images)")


if __name__ == "__main__":
    main()
