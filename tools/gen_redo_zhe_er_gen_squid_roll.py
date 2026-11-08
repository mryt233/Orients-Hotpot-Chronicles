"""折耳根与生鱿鱼卷的三套候选、对比展板及教程图标同步。

作者：mryt233
在 mod 目录运行：python tools/gen_redo_zhe_er_gen_squid_roll.py
加 --preview-only 仅生成展板与候选，不安装贴图。
"""
from __future__ import annotations

import argparse
import hashlib
import json
import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

from check_textures import check_image
from pixkit import io
from pixkit.paths import ASSETS, MOD
from pixkit.texstats import stats


TEXTURES = ASSETS / "textures"
ITEMS = TEXTURES / "item"
BOARD = MOD / "build/boards/redo_zhe_er_gen_squid_roll.png"
SOURCES = BOARD.parent / "redo_zhe_er_gen_squid_roll_sources"
REPORT = BOARD.with_suffix(".json")
NAMES = ("squid_roll", "zhe_er_gen")
ALLOWED = {ITEMS / f"{name}.png" for name in NAMES}
SELECTED = {"squid_roll": "A", "zhe_er_gen": "A"}
LABELS = {"squid_roll": "生鱿鱼卷", "zhe_er_gen": "折耳根"}
DESCRIPTIONS = {
    "squid_roll": {"A": "原样保留熟卷轮廓与花刀", "B": "略舒展的湿润卷口", "C": "较细长的浅粉卷筒"},
    "zhe_er_gen": {"A": "双根微弯与双叶嫩芽", "B": "三根错落与紫红节环", "C": "分叉根茎与紫红芽尖"},
}
SQUID_COLORS = (
    "#6e3f4d", "#9b6072", "#b87b8c", "#d09aa9", "#d6a3b1", "#e0b4c1",
    "#e5bfc9", "#f2dce2", "#f7e6ea", "#fbedf0", "#fef2f4", "#fff8f8",
)
ROOT = {
    "outline": "#6b4945", "shadow": "#c9bda6", "body": "#e6decb", "light": "#f7f2e7",
    "shine": "#fffbf3", "ring": "#9e5a52", "ring_light": "#b57665",
    "bud": "#a84a62", "bud_light": "#c76b7e", "leaf_dark": "#4c7348",
    "leaf": "#6da85b", "leaf_light": "#92c47c",
}


def fingerprints() -> dict[str, str]:
    return {p.relative_to(TEXTURES).as_posix(): hashlib.sha256(p.read_bytes()).hexdigest()
            for p in sorted(TEXTURES.rglob("*.png"))}


def protected_check(before: dict[str, str]) -> dict[str, str]:
    after = fingerprints()
    allowed = {p.relative_to(TEXTURES).as_posix() for p in ALLOWED}
    changed = [p for p in before.keys() | after.keys()
               if p not in allowed and before.get(p) != after.get(p)]
    if changed:
        raise RuntimeError(f"白名单以外贴图指纹变化：{changed}")
    if before["item/cooked_squid_roll.png"] != after["item/cooked_squid_roll.png"]:
        raise RuntimeError("熟鱿鱼卷保护校验失败")
    return after


def squid_candidates(cooked: Image.Image) -> dict[str, Image.Image]:
    # mryt233 改：逐色一一映射，A 的轮廓和全部花刀色块位置与熟卷严格一致。
    source_colors = sorted({c for c in cooked.get_flattened_data() if c[3]},
                           key=lambda c: sum(a * b for a, b in zip(c, (0.2126, 0.7152, 0.0722))))
    if len(source_colors) != len(SQUID_COLORS):
        raise ValueError("熟卷色阶已变化，需要重新确认生卷的花刀映射")
    palette = {src: tuple(bytes.fromhex(dst[1:])) + (255,)
               for src, dst in zip(source_colors, SQUID_COLORS)}
    a = Image.new("RGBA", (16, 16))
    a.putdata([palette.get(c, (0, 0, 0, 0)) for c in cooked.get_flattened_data()])
    b, c = a.copy(), a.copy()
    bd, cd = ImageDraw.Draw(b), ImageDraw.Draw(c)
    bd.line([(10, 2), (12, 3), (13, 5)], fill="#fff8f8")
    bd.line([(11, 3), (12, 4), (12, 5)], fill="#d6a3b1")
    bd.line([(12, 6), (13, 7), (13, 8)], fill="#b87b8c")
    bd.line([(13, 5), (14, 6), (14, 7)], fill="#e5bfc9")
    bd.point((14, 8), fill="#6e3f4d")
    for point in ((1, 8), (1, 9), (2, 11), (2, 12), (3, 12), (5, 14)):
        cd.point(point, fill=(0, 0, 0, 0))
    cd.line([(2, 8), (2, 9), (3, 10), (3, 11), (4, 12), (5, 13)], fill="#9b6072")
    cd.line([(3, 8), (4, 9), (5, 10)], fill="#fff8f8")
    cd.line([(10, 3), (11, 4), (12, 5)], fill="#fef2f4")
    return {"A": a, "B": b, "C": c}


def roots(variant: str) -> Image.Image:
    im = Image.new("RGBA", (16, 16))
    draw = ImageDraw.Draw(im)
    paths = {
        "A": ([(2, 12), (4, 11), (5, 8), (8, 6), (10, 3)],
              [(5, 13), (7, 11), (8, 9), (11, 7), (12, 4)]),
        "B": ([(2, 12), (3, 9), (6, 6), (8, 4)],
              [(3, 13), (6, 11), (8, 8), (11, 6), (12, 4)],
              [(7, 13), (9, 11), (10, 8), (11, 7)]),
        "C": ([(2, 13), (4, 11), (5, 8), (8, 6), (10, 3)],
              [(5, 13), (7, 11), (8, 8), (11, 6), (12, 4)],
              [(5, 8), (4, 6), (6, 4)]),
    }[variant]
    # 先画背后根茎，再覆盖前根；象牙白亮线朝向左上。
    for path in paths:
        draw.line(path, fill=ROOT["outline"], width=3)
        draw.line(path, fill=ROOT["body"], width=2)
        draw.line([(x, y - 1) for x, y in path], fill=ROOT["light"])
        draw.point(path[0], fill=ROOT["shadow"])
    rings = {
        "A": ([(4, 9), (5, 10)], [(7, 6), (8, 7)], [(7, 10), (8, 11)], [(10, 6), (11, 7)]),
        "B": ([(3, 9), (4, 10)], [(6, 6), (7, 7)], [(6, 10), (7, 11)], [(9, 7), (10, 8)], [(9, 11), (10, 12)]),
        "C": ([(4, 10), (5, 11)], [(7, 6), (8, 7)], [(7, 10), (8, 11)], [(10, 6), (11, 7)]),
    }[variant]
    for ring in rings:
        draw.line(ring, fill=ROOT["ring"])
        draw.point(ring[0], fill=ROOT["ring_light"])
    hairs = {"A": ((3, 10), (6, 7), (9, 11)),
             "B": ((2, 10), (5, 7), (8, 12)),
             "C": ((3, 10), (6, 7), (9, 11))}[variant]
    for x, y in hairs:
        draw.line([(x, y), (x - 1, y + 1)], fill=ROOT["shadow"])
    draw.polygon([(9, 3), (9, 2), (11, 1), (12, 2), (12, 3), (11, 5)], fill=ROOT["bud"])
    draw.line([(10, 3), (11, 2), (11, 3), (10, 4)], fill=ROOT["leaf_light"])
    draw.point((12, 2), fill=ROOT["bud_light"])
    draw.polygon([(12, 4), (13, 3), (14, 3), (14, 5), (12, 6), (11, 5)], fill=ROOT["leaf_dark"])
    draw.polygon([(12, 4), (14, 3), (13, 5), (12, 5)], fill=ROOT["leaf"])
    draw.line([(12, 4), (13, 4)], fill=ROOT["leaf_light"])
    if variant == "C":
        draw.line([(6, 4), (7, 3), (7, 2)], fill=ROOT["bud"])
        draw.point((7, 3), fill=ROOT["bud_light"])
    draw.point((5, 8), fill=ROOT["shine"])
    return im


def measurements(im: Image.Image) -> dict:
    s = stats(im)
    solid = {(x, y) for y in range(16) for x in range(16) if im.getpixel((x, y))[3]}
    s["mass_offset"] = [round(sum(p[a] for p in solid) / len(solid) - 7.5, 4) for a in (0, 1)]
    seen = set()
    components = []
    for start in sorted(solid):
        if start in seen:
            continue
        todo, group = [start], set()
        while todo:
            p = todo.pop()
            if p in seen:
                continue
            seen.add(p)
            group.add(p)
            todo.extend((p[0] + dx, p[1] + dy) for dx, dy in
                        ((-1, 0), (1, 0), (0, -1), (0, 1))
                        if (p[0] + dx, p[1] + dy) in solid - seen)
        components.append(len(group))
    s["components"] = components
    return s


def validate(images: dict[str, dict[str, Image.Image]], cooked: Image.Image) -> None:
    a = images["squid_roll"]["A"]
    if list(a.getchannel("A").get_flattened_data()) != list(cooked.getchannel("A").get_flattened_data()):
        raise ValueError("生卷 A 与熟卷轮廓不一致")
    for name, variants in images.items():
        for variant, im in variants.items():
            s, issues = measurements(im), check_image(im, f"item/{name}")
            if im.size != (16, 16) or im.mode != "RGBA" or s["semi"] or issues:
                raise ValueError(f"{name}/{variant}：{issues or '尺寸、模式或透明度不合规'}")
            if not 85 <= s["opaque"] <= 130 or not 6 <= s["colors"] <= 14:
                raise ValueError(f"{name}/{variant}：像素数 {s['opaque']}，颜色数 {s['colors']}")
            if min(s["bbox"][:2]) < 1 or max(s["bbox"][2:]) > 15:
                raise ValueError(f"{name}/{variant}：不足 1px 透明边距")
            if any(abs(v) > 1 for v in s["mass_offset"]):
                raise ValueError(f"{name}/{variant}：像素重心偏移 {s['mass_offset']}")
            if len(s["components"]) != 1:
                raise ValueError(f"{name}/{variant}：存在未相连像素 {s['components']}")


def font(size: int) -> ImageFont.ImageFont:
    for p in (Path("C:/Windows/Fonts/msyh.ttc"), Path("C:/Windows/Fonts/simhei.ttf"),
              Path("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf")):
        if p.is_file():
            return ImageFont.truetype(str(p), size)
    return ImageFont.load_default()


def board(images: dict[str, dict[str, Image.Image]], old: dict[str, Image.Image], cooked: Image.Image) -> Image.Image:
    out = Image.new("RGB", (1680, 1220), "#171b22")
    d = ImageDraw.Draw(out)
    title, normal, small = font(28), font(17), font(14)
    d.text((28, 20), "生鱿鱼卷 / 折耳根 · 重绘候选与同步", font=title, fill="#fff1d7")
    d.text((28, 65), "作者：mryt233  |  每格均为最近邻 1× / 4× / 8×  |  上：#373737 槽位  下：#f1e7d7 书页", font=normal, fill="#bac2cb")
    selected_label = " / ".join(f"{LABELS[n]} {SELECTED[n]}（{DESCRIPTIONS[n][SELECTED[n]]}）" for n in NAMES)
    d.text((28, 96), f"选定：{selected_label}", font=normal, fill="#f5d395")
    for row, name in enumerate(NAMES):
        y = 142 + row * 525
        d.text((28, y), f"{LABELS[name]}  /  {name}.png", font=normal, fill="#f0e5d5")
        entries = [("熟鱿鱼卷 · 只读基准", cooked), ("修改前 · 旧贴图", old[name])]
        entries += [(f"{v} · {DESCRIPTIONS[name][v]}" + (" ★采用" if SELECTED[name] == v else ""), im)
                    for v, im in images[name].items()]
        for col, (label, im) in enumerate(entries):
            x, top = 26 + col * 330, y + 35
            chosen = col >= 2 and "ABC"[col - 2] == SELECTED[name]
            d.rounded_rectangle((x, top, x + 318, top + 466), radius=8, fill="#242b34",
                                outline="#dbb877" if chosen else "#414953", width=2)
            d.text((x + 10, top + 10), label, font=small, fill="#f5d395" if chosen else "#d4d9df")
            for bi, bg in enumerate(("#373737", "#f1e7d7")):
                by = top + 57 + bi * 163
                bx = x + 10
                for scale, area in ((1, 34), (4, 86), (8, 158)):
                    d.rectangle((bx, by, bx + area - 1, by + 139), fill=bg)
                    side = scale * 16
                    big = im.resize((side, side), Image.Resampling.NEAREST)
                    out.paste(big, (bx + (area - side) // 2, by + (140 - side) // 2), big)
                    d.text((bx + 2, by - 21), f"{scale}×", font=small, fill="#aeb9c6")
                    bx += area + 9
            s = measurements(im)
            d.text((x + 10, top + 386), f"{s['opaque']} px / {s['colors']} 色 / 半透明 {s['semi']}", font=small, fill="#bec8d4")
            d.text((x + 10, top + 410), f"重心偏移 {s['mass_offset']}", font=small, fill="#bec8d4")
            d.text((x + 10, top + 434), f"范围 {s['bbox']} / 连通块 {len(s['components'])}", font=small, fill="#bec8d4")
    d.text((28, 1190), "熟鱿鱼卷保持原文件不变；白名单外全部 PNG 逐文件 SHA-256 校验。教程图标使用 32×32 最近邻放大。", font=small, fill="#bac2cb")
    return out


def save_if_changed(im: Image.Image, path: Path) -> None:
    if path.is_file():
        with Image.open(path) as current:
            if (current.mode, current.size, current.tobytes()) == (im.mode, im.size, im.tobytes()):
                return
    io.save_png(im, path)


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--preview-only", action="store_true")
    args = parser.parse_args()
    before = fingerprints()
    SOURCES.mkdir(parents=True, exist_ok=True)
    cooked = Image.open(ITEMS / "cooked_squid_roll.png").convert("RGBA")
    old = {}
    for name in NAMES:
        backup = SOURCES / f"{name}_before.png"
        if not backup.exists():
            backup.write_bytes((ITEMS / f"{name}.png").read_bytes())
        old[name] = Image.open(backup).convert("RGBA")
    images = {"squid_roll": squid_candidates(cooked), "zhe_er_gen": {v: roots(v) for v in "ABC"}}
    validate(images, cooked)
    comparison = board(images, old, cooked)
    for name, variants in images.items():
        for variant, im in variants.items():
            save_if_changed(im, SOURCES / f"{name}_{variant}.png")
    save_if_changed(comparison, BOARD)
    if not args.preview_only:
        outputs = {}
        for name in NAMES:
            im = images[name][SELECTED[name]]
            outputs[ITEMS / f"{name}.png"] = im
            big = im.resize((32, 32), Image.Resampling.NEAREST)
            outputs[MOD / "tutorial/assets/items" / f"{name}.png"] = big
            for folder in ("tutorial/img/icons", "tutorial/site/img/icons"):
                outputs[MOD / folder / f"orients_hotpot_chronicles__{name}.png"] = big
        if {p for p in outputs if p.is_relative_to(TEXTURES)} != ALLOWED or len(outputs) != 8:
            raise ValueError("输出路径超出两项贴图及六项图标白名单")
        try:
            for path, im in outputs.items():
                save_if_changed(im, path)
        finally:
            protected_check(before)
    after = protected_check(before)
    report = {
        "author": "mryt233", "selected": SELECTED, "installed": not args.preview_only,
        "statistics": {n: {v: measurements(im) for v, im in variants.items()} for n, variants in images.items()},
        "before_sha256": before, "after_sha256": after,
        "protected_count": len(before) - len(ALLOWED), "protected_unchanged": True,
    }
    io.write_json(REPORT, report, ensure_ascii=False)
    print("作者：mryt233；六张候选 check_image 均为 0 错误、0 警告。")
    for name, variants in images.items():
        for variant, im in variants.items():
            s = measurements(im)
            print(f"{name}/{variant}: {s['opaque']} px, {s['colors']} 色, 重心 {s['mass_offset']}, 连通块 {s['components']}")
    print(f"白名单以外 {len(before) - 2} 张贴图 SHA-256 全部一致。")
    print(f"cooked_squid_roll.png: {after['item/cooked_squid_roll.png']}")
    print(f"展板：{BOARD}")


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    main()
