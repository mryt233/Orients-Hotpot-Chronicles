"""可乐气泡动画（2026-10-26 用户：之晗可乐和一杯之晗可乐做动效，自下而上的气泡，像真的可乐一样）。

给 gen_orient_cola.py / gen_cup.py 用：在一张静态贴图上，指定几段「可乐看得见的区域」，每段里几列气泡从下往上冒，
一帧往上走 1 个贴图像素，冒到顶就消失，下面再冒新的；各列错开起点和速度，不会整齐划一。
输出竖着叠起来的多帧贴图 + .png.mcmeta（原版动画贴图格式），方块、物品、摆在桌上都会动。
作者：mryt233
"""
import json
import random

FRAMES = 24          # 一圈 24 帧
FRAMETIME = 2        # 每帧 2 刻（0.1 秒），一圈 2.4 秒


def animate(base, zones, bubble, glint=None, seed=7):
    """base：静态贴图（RGBA）。zones：[(列列表, 最下一行, 最上一行), ...]，每列一串气泡在这几行之间往上冒。
    bubble：气泡颜色；glint：气泡上方一格更淡的拖尾色（可省）。返回 (多帧贴图, mcmeta 字典)。"""
    from PIL import Image
    w, h = base.size
    rnd = random.Random(seed)
    tracks = []
    for cols, bottom, top in zones:
        length = bottom - top + 1
        for x in cols:
            # 每列 1~2 个气泡，起点错开；有的列走得快（一帧一格），有的慢（两帧一格）
            speed = rnd.choice((1, 1, 2)) if length * 2 + 2 <= FRAMES else 1
            want = length * speed + rnd.randint(2, 6)              # 冒完之后停一会儿再冒
            period = min(d for d in (4, 6, 8, 12, 24) if d >= min(want, FRAMES))   # 取 24 的约数，动画首尾无缝
            for k in range(rnd.choice((1, 1, 2))):
                tracks.append((x, bottom, top, speed, period, rnd.randint(0, period - 1) + k * period // 2))
    sheet = Image.new("RGBA", (w, h * FRAMES))
    for f in range(FRAMES):
        frame = base.copy()
        px = frame.load()
        for x, bottom, top, speed, period, phase in tracks:
            t = (f + phase) % period
            y = bottom - t // speed
            if top <= y <= bottom:
                px[x, y] = bubble
                if glint and y + 1 <= bottom and t % speed == 0:
                    px[x, y + 1] = glint
        sheet.paste(frame, (0, f * h))
    return sheet, {"animation": {"frametime": FRAMETIME}}


def save(sheet, meta, path):
    sheet.save(path)
    path.with_name(path.name + ".mcmeta").write_bytes((json.dumps(meta, indent=2) + "\n").encode("utf-8"))
