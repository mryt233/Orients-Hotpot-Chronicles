"""方案对比板：几套候选排成一张图，每套标编号，用户回「2 号」就行。作者：mryt233（2026-11-01）

每一列一个方案，从上到下：
    原尺寸（1× / 2×）→ 放大 10 倍带像素格 → 物品栏（夹在两个原版同类物品中间，看风格合不合群）
    → 摆在桌上 / 世界里的样子 → 自检结果（tools/check_textures.py 的规则）
最右一列是原版同类参考（tools/vanilla_ref.py 自动找色相最接近的，也可以 --like 指定）。

    # 物品贴图（PNG）候选
    python tools/compare_board.py 火腿肠片 --as item/ham_sausage_slice --png a.png b.png c.png
    python tools/compare_board.py 火腿肠片 --as item/ham_sausage_slice --png a.png b.png --labels 横排 叠放 --like cooked_porkchop
    # 方块模型候选：每个候选一个 PREVIEW_ROOT 目录（assets/<modid> 结构，不装进模组），同一个模型引用
    python tools/compare_board.py 玻璃杯 --model orients_hotpot_chronicles:block/cup --root dirA dirB
    # 用 JSON 描述（候选可混用 png / root）
    python tools/compare_board.py --spec board.json
    （任何写法都可以加 --out 路径.png 指定输出位置）

输出 build/boards/<名字>.png。别的脚本：from compare_board import board; board(name, candidates, as_rel, like)
candidates = [{"label": "1 号", "png": 路径或 PIL 图} 或 {"label": ..., "root": 目录, "model": 模型引用}]
"""
import json
import os
import subprocess
import sys
import tempfile
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from PIL import Image, ImageDraw  # noqa: E402

import vanilla_ref  # noqa: E402
from check_textures import LABEL, check_image  # noqa: E402
from pixkit.paths import MOD, VANILLA  # noqa: E402
from pixkit.texstats import first_frame, hls, stats  # noqa: E402
from pixkit.text import font  # noqa: E402

COL_W = 300
BG = (49, 49, 47, 255)
INK, DIM, WARN_C, OK_C, ERR_C = (247, 240, 220), (176, 172, 160), (240, 190, 90), (140, 210, 120), (240, 110, 100)
PANEL = (198, 198, 198, 255)


# ------------------------------------------------------------------ 画小部件
def slot(img, x, y, k=4):
    """原版物品栏格子（18×18：左上暗边、右下亮边、中间 #8B8B8B），放大 k 倍。返回放物品的左上角。"""
    d = ImageDraw.Draw(img)
    s = 18 * k
    d.rectangle((x, y, x + s - 1, y + s - 1), fill=(139, 139, 139, 255))
    d.rectangle((x, y, x + s - 1, y + k - 1), fill=(55, 55, 55, 255))
    d.rectangle((x, y, x + k - 1, y + s - 1), fill=(55, 55, 55, 255))
    d.rectangle((x, y + s - k, x + s - 1, y + s - 1), fill=(255, 255, 255, 255))
    d.rectangle((x + s - k, y, x + s - 1, y + s - 1), fill=(255, 255, 255, 255))
    return x + k, y + k


def zoom_grid(im, k=10):
    """放大 k 倍，画浅色像素格，底下垫棋盘格方便看透明。"""
    w, h = im.size
    out = Image.new("RGBA", (w * k, h * k))
    d = ImageDraw.Draw(out)
    for y in range(h):
        for x in range(w):
            c = (92, 92, 90, 255) if (x + y) % 2 else (80, 80, 78, 255)
            d.rectangle((x * k, y * k, x * k + k - 1, y * k + k - 1), fill=c)
    out.alpha_composite(im.resize((w * k, h * k), Image.NEAREST))
    for i in range(w + 1):
        d.line((i * k, 0, i * k, h * k), fill=(255, 255, 255, 28))
    for i in range(h + 1):
        d.line((0, i * k, w * k, i * k), fill=(255, 255, 255, 28))
    return out


def on_table(sprite, size=230):
    """物品平放在原版橡木板桌面上，从斜上方看（和摆在盘子 / 桌上时的视角接近）。"""
    plank = Image.open(VANILLA / "textures/block/oak_planks.png").convert("RGBA")
    k = 8
    top = Image.new("RGBA", (32 * k, 32 * k))          # 2×2 块木板，物品占约半格宽（和游戏里摆在桌上差不多）
    for gy in range(2):
        for gx in range(2):
            top.paste(plank.resize((16 * k, 16 * k), Image.NEAREST), (gx * 16 * k, gy * 16 * k))
    # 物品放正中，往右下投一点影子
    item = sprite.resize((16 * k, 16 * k), Image.NEAREST)
    shadow = Image.new("RGBA", item.size, (0, 0, 0, 0))
    shadow.putalpha(item.getchannel("A").point(lambda a: 70 if a else 0))
    top.alpha_composite(shadow, (8 * k + k, 8 * k + k))
    top.alpha_composite(item, (8 * k, 8 * k))
    iso = top.rotate(45, resample=Image.NEAREST, expand=True)
    iso = iso.resize((iso.width, iso.height // 2), Image.NEAREST)
    iso.thumbnail((size, size), Image.NEAREST)
    return iso


def render_model(root, ref, size=230):
    """用 tools/render_preview.py 把方块模型渲染出来（PREVIEW_ROOT 指向候选目录）。"""
    with tempfile.TemporaryDirectory() as d:
        out = Path(d) / "m.png"
        env = dict(os.environ, PREVIEW_ROOT=str(root))
        subprocess.run([sys.executable, str(MOD / "tools/render_preview.py"), str(out), ref], env=env, check=True,
                       capture_output=True)
        im = Image.open(out).convert("RGBA")
    bg = im.getpixel((0, 0))                      # render_preview 的底色：抠成透明
    px = im.load()
    for y in range(im.height):
        for x in range(im.width):
            if px[x, y][:3] == bg[:3]:
                px[x, y] = (0, 0, 0, 0)
    bbox = im.getbbox()
    if bbox:
        im = im.crop(bbox)
    im.thumbnail((size, size), Image.NEAREST)
    return im


def model_texture(root, ref):
    """取模型用到的第一张本模组贴图（给自检和小图用）。"""
    ns, path = ref.split(":", 1)
    m = json.loads((Path(root) / f"models/{path}.json").read_text(encoding="utf-8"))
    for v in (m.get("textures") or {}).values():
        if isinstance(v, str) and not v.startswith("#"):
            tns, tpath = v.split(":", 1) if ":" in v else ("minecraft", v)
            p = Path(root) / f"textures/{tpath}.png"
            if p.exists():
                mp = p.with_name(p.name + ".mcmeta")
                meta = json.loads(mp.read_text(encoding="utf-8")) if mp.exists() else None
                return Image.open(p).convert("RGBA"), tpath, meta
    return None, None, None


# ------------------------------------------------------------------ 选原版参考
def pick_peers(as_rel, cand_img, like=None, n=3):
    data = vanilla_ref.load()
    if like:
        keys = [k if "/" in k else f"item/{k}" for k in like]
        return [k for k in keys if k in data][:n]
    folder, name = as_rel.split("/", 1)
    cats = vanilla_ref.category_of(name, folder)
    pool = {}
    for c in cats:
        pool.update(vanilla_ref.peers(c, data))
    if not pool:
        pool = {k: v for k, v in data.items() if k.startswith("item/") and v["opaque"] and v["frames"] == 1}
    main = stats(cand_img)["main"] if cand_img is not None else None
    if not main:
        return sorted(pool)[:n]
    mh, ms, ml = hls(main)

    def dist(k):
        c = pool[k]["main"]
        if not c:
            return 999
        h, s, l = hls(c)
        dh = min(abs(h - mh), 360 - abs(h - mh)) / 180
        return dh * 2 + abs(s - ms) + abs(l - ml)
    return sorted(pool, key=dist)[:n]


def vanilla_img(k):
    return first_frame(Image.open(VANILLA / f"textures/{k}.png").convert("RGBA"))


# ------------------------------------------------------------------ 主体
def board(name, candidates, as_rel="item/candidate", like=None, out=None):
    cols = []
    for i, c in enumerate(candidates):
        label = c.get("label") or f"{i + 1} 号"
        if "png" in c:
            img = c["png"] if isinstance(c["png"], Image.Image) else Image.open(c["png"])
            img = img.convert("RGBA")
            tex, rel, world, icon = img, as_rel, None, None
            meta_p = Path(c["png"]).with_name(Path(c["png"]).name + ".mcmeta") if isinstance(c["png"], (str, Path)) else None
            meta = json.loads(meta_p.read_text(encoding="utf-8")) if meta_p and meta_p.exists() else None
        else:
            tex, tpath, meta = model_texture(c["root"], c["model"])
            rel = tpath or as_rel
            world = render_model(c["root"], c["model"])
            icon = world                                   # 方块模型：物品栏、原尺寸都用渲染出来的样子
        problems = check_image(tex, rel, meta) if tex is not None else []
        cols.append({"label": label, "tex": first_frame(tex) if tex is not None else None, "world": world,
                     "icon": icon, "problems": problems})
    models = all(c["icon"] is not None for c in cols)
    # 方块模型按颜色找原版没有意义：只有 --like 指定了才放参考列
    peers = pick_peers(as_rel, cols[0]["tex"], like) if (like or not models) else []

    n = len(cols) + (1 if peers else 0)
    rows_y = {"title": 0, "label": 70, "small": 118, "zoom": 160, "slots": 340, "world": 440, "check": 690}
    H = 860
    GUT = 60                       # 左边留一条写行名
    W = GUT + n * COL_W + 10
    img = Image.new("RGBA", (W, H), BG)
    d = ImageDraw.Draw(img)
    d.text((20, 16), f"{name} · 方案对比", font=font(26, True), fill=INK)
    d.text((20 + d.textlength(f"{name} · 方案对比", font=font(26, True)) + 18, 26),
           "选一个编号回复即可（例：2 号，或 2 号再深一点）", font=font(15), fill=DIM)
    side = ["原尺寸", "放大", "物品栏", "摆放", "自检"]
    peer_imgs = [vanilla_img(k) for k in peers]
    for ci in range(n):
        x0 = GUT + ci * COL_W
        is_ref = ci == len(cols)
        icon = None if is_ref else cols[ci]["icon"]
        d.rounded_rectangle((x0 + 4, 60, x0 + COL_W - 4, H - 10), radius=8,
                            fill=(60, 60, 58, 255) if not is_ref else (54, 58, 66, 255))
        if is_ref:
            title = "原版参考"
            tex = peer_imgs[0] if peer_imgs else None
        else:
            title = cols[ci]["label"]
            tex = cols[ci]["tex"]
        tw = d.textlength(title, font=font(24, True))
        d.text((x0 + (COL_W - tw) / 2, rows_y["label"]), title, font=font(24, True), fill=INK)
        if tex is None:
            continue
        # 原尺寸
        cx = x0 + COL_W // 2
        if icon is not None:
            for size, dx in ((16, -30), (32, 0)):
                th = icon.copy()
                th.thumbnail((size, size), Image.LANCZOS)
                img.alpha_composite(th, (cx + dx, rows_y["small"] + 32 - th.height))
        else:
            img.alpha_composite(tex, (cx - 30, rows_y["small"] + 8))
            img.alpha_composite(tex.resize((32, 32), Image.NEAREST), (cx, rows_y["small"]))
        # 放大
        z = zoom_grid(tex, max(1, 160 // max(tex.size)))
        img.alpha_composite(z, (cx - z.width // 2, rows_y["zoom"]))
        # 物品栏：原版 | 候选 | 原版
        k = 4
        strip_w = 3 * 18 * k
        sx = cx - strip_w // 2
        d.rectangle((sx - 6, rows_y["slots"] - 6, sx + strip_w + 5, rows_y["slots"] + 18 * k + 5), fill=PANEL)
        if is_ref:
            trio = (peer_imgs + [None, None, None])[:3]
        else:
            trio = [peer_imgs[0] if peer_imgs else None, icon if icon is not None else tex,
                    peer_imgs[1] if len(peer_imgs) > 1 else None]
        for j, t in enumerate(trio):
            ix, iy = slot(img, sx + j * 18 * k, rows_y["slots"], k)
            if t is None:
                continue
            if t is icon:
                th = t.copy()
                th.thumbnail((16 * k, 16 * k), Image.LANCZOS)
                img.alpha_composite(th, (ix + (16 * k - th.width) // 2, iy + (16 * k - th.height) // 2))
            else:
                img.alpha_composite(t.resize((16 * k, 16 * k), Image.NEAREST), (ix, iy))
        # 摆放
        world = cols[ci]["world"] if not is_ref else None
        if world is None:
            world = on_table(tex.resize((16, 16), Image.NEAREST) if tex.size[0] != 16 else tex)
        img.alpha_composite(world, (cx - world.width // 2, rows_y["world"] + (230 - world.height) // 2))
        # 自检 / 参考名字
        y = rows_y["check"]
        if is_ref:
            for k_ in peers:
                d.text((x0 + 16, y), k_.split("/", 1)[1], font=font(15), fill=DIM)
                y += 22
            prof = None
            folder, nm = as_rel.split("/", 1)
            cats = vanilla_ref.category_of(nm, folder)
            if cats:
                prof = vanilla_ref.profile(cats[0])
                d.text((x0 + 16, y + 4), f"原版{vanilla_ref.NAME[cats[0]]}：{prof['colors']['min']}～{prof['colors']['max']} 种颜色",
                       font=font(14), fill=DIM)
        else:
            probs = cols[ci]["problems"]
            if not probs:
                d.text((x0 + 16, y), "自检通过", font=font(17, True), fill=OK_C)
            for lvl, code, msg in probs[:5]:
                col = ERR_C if lvl == "错误" else WARN_C
                lines = wrap(f"{LABEL[code]}：{msg}", 17)
                for ln in lines:
                    d.text((x0 + 16, y), ln, font=font(14), fill=col)
                    y += 19
                y += 3
            s = stats(tex)
            d.text((x0 + 16, H - 34), f"{s['colors']} 种颜色 · 对比 {s['contrast']} · 描边{'深' if s['outline'] and s['outline']['dark'] else '无'}",
                   font=font(13), fill=DIM)
    # 每行左侧的小标签
    for key, txt in zip(("small", "zoom", "slots", "world", "check"), side):
        d.text((12, rows_y[key] + (6 if key != "zoom" else 70)), txt, font=font(14), fill=(150, 146, 136))
    out = Path(out) if out else MOD / "build/boards" / f"{name}.png"
    out.parent.mkdir(parents=True, exist_ok=True)
    img.convert("RGB").save(out)
    print("已出对比板", out)
    return out


def wrap(text, n):
    return [text[i:i + n] for i in range(0, len(text), n)] or [""]


def _after(argv, flag):
    if flag not in argv:
        return []
    i = argv.index(flag) + 1
    vals = []
    while i < len(argv) and not argv[i].startswith("--"):
        vals.append(argv[i])
        i += 1
    return vals


def main(argv):
    if not argv or argv[0] in ("-h", "--help"):
        print(__doc__)
        return 0
    if "--spec" in argv:
        spec = json.loads(Path(_after(argv, "--spec")[0]).read_text(encoding="utf-8"))
        board(spec["name"], spec["candidates"], spec.get("as", "item/candidate"), spec.get("like"), spec.get("out"))
        return 0
    name = argv[0]
    as_rel = (_after(argv, "--as") or ["item/candidate"])[0]
    labels = _after(argv, "--labels")
    like = _after(argv, "--like") or None
    if "--png" in argv:
        cands = [{"png": p} for p in _after(argv, "--png")]
    else:
        ref = _after(argv, "--model")[0]
        cands = [{"root": r, "model": ref} for r in _after(argv, "--root")]
    for c, lb in zip(cands, labels):
        c["label"] = lb
    board(name, cands, as_rel, like, (_after(argv, "--out") or [None])[0])
    return 0


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    raise SystemExit(main(sys.argv[1:]))
