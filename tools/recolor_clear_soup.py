"""清汤换配色（2026-10-23 用户发了「白开水清汤牛肉火锅」参考图：清亮见底、带锅底的银色，不是浑的土黄色）。

只换颜色、不动纹路和沸腾动画：按每个像素原来的明暗，映射到「清亮浅银灰 → 白色高光」这条色带上；
绿色的葱花点原样保留。原来的贴图备份在 archive/clear_soup_old/（重跑时从备份读，结果不会越跑越偏）。

    python tools/recolor_clear_soup.py
作者：
"""
import shutil
from pathlib import Path

from PIL import Image

MOD = Path(__file__).resolve().parent.parent
RES = MOD / "src/main/resources"
DIRS = [RES / "assets/orients_hotpot_chronicles/textures/block",
        RES / "resourcepacks/hotpot_assets/assets/orients_hotpot_chronicles/textures/block"]
BACKUP = MOD / "archive/clear_soup_old"
FILES = ["hot_pot_top_clear.png", "hot_pot_top_clear_boiling.png"]
# 色带：暗处 = 透过清汤看到的锅底（偏冷的银灰），亮处 = 水面反光（白里带一点暖）
DARK = (150, 160, 162)
MID = (198, 206, 204)
LIGHT = (244, 244, 236)


def ramp(t):
    if t < 0.5:
        a, b, k = DARK, MID, t / 0.5
    else:
        a, b, k = MID, LIGHT, (t - 0.5) / 0.5
    return tuple(int(a[i] + (b[i] - a[i]) * k) for i in range(3))


def recolor(src, dst):
    im = Image.open(src).convert("RGBA")
    px = im.load()
    lum = [0.299 * r + 0.587 * g + 0.114 * b for r, g, b, a in im.getdata() if a]
    lo, hi = min(lum), max(lum)
    for y in range(im.height):
        for x in range(im.width):
            r, g, b, a = px[x, y]
            if not a or (g > r + 25 and g > b + 25):     # 葱花绿点不动
                continue
            t = (0.299 * r + 0.587 * g + 0.114 * b - lo) / max(1, hi - lo)
            px[x, y] = ramp(t) + (a,)
    im.save(dst)


def main():
    BACKUP.mkdir(parents=True, exist_ok=True)
    for name in FILES:
        if not (BACKUP / name).exists():
            shutil.copy(DIRS[0] / name, BACKUP / name)
        for d in DIRS:
            recolor(BACKUP / name, d / name)
    print("ok")


if __name__ == "__main__":
    main()
