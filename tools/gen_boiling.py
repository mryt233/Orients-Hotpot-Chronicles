"""Generate the rolling-boil soup animations (hot_pot_top_<soup>_boiling.png + .mcmeta).

Each sheet starts from the calm soup texture (hot_pot_top_<soup>.png) so colours match, then,
over a seamless 24-frame loop (1 tick per frame = 1.2 s):
  * churn   - the whole surface is warped by travelling sine waves, so the broth heaves and rolls
  * mounds  - two boil heads over the fire keep surging up, swelling and bursting (ease-out growth)
  * bubbles - ~34 short-lived bubbles, up to 4x4, swell into domes then burst into a ring
              that throws a few droplets outward
  * froth   - pixels crowded by bubbles turn into light foam
Frames are crisp (no interpolate) so everything reads as pixel art.

    python tools/gen_boiling.py
作者：mryt233（2026-11-01 mryt233 改：改用 tools/pixkit 公共库；.mcmeta 统一写 LF，贴图逐字节不变）
"""
import math
import random

from PIL import Image

# mryt233 改（2026-11-10）：材质名单与输出目录统一。
from pixkit.materials import materials, tuples

from pixkit import anim
from pixkit.color import ease_out_cubic, lerp as mix, shade
from pixkit.paths import ASSETS

DIRS = [ASSETS / "textures/block"]
FRAMES = 24
FRAMETIME = 1
BUBBLES = 28

# bubble body / highlight per soup
SOUPS = {e["id"]: (tuples(e["bubble_body"]), tuples(e["bubble_highlight"])) for e in materials()["soups"]}


class Frame:
    def __init__(self, base, f):
        self.px = [[base[(x, y)] for y in range(16)] for x in range(16)]
        # churn: travelling waves warp where each pixel samples the calm surface
        ph = 2 * math.pi * f / FRAMES
        out = [[None] * 16 for _ in range(16)]
        for x in range(16):
            for y in range(16):
                dx = round(1.15 * math.sin(2 * math.pi * y / 7 + ph * 2) + 0.6 * math.sin(2 * math.pi * (x + y) / 11 - ph * 3))
                dy = round(1.15 * math.sin(2 * math.pi * x / 8 - ph * 2) + 0.6 * math.cos(2 * math.pi * (x - y) / 10 + ph))
                sx = min(15, max(0, x + dx))
                sy = min(15, max(0, y + dy))
                # keep the dark pot-wall ring still; only the broth inside moves
                out[x][y] = self.px[x][y] if x in (0, 15) or y in (0, 15) else self.px[sx][sy]
        self.px = out

    def get(self, x, y):
        return self.px[x][y]

    def put(self, x, y, c, t=1.0):
        if 1 <= x <= 14 and 1 <= y <= 14:
            self.px[x][y] = mix(self.px[x][y], c, t)


def dome(fr, x, y, r, body, hi):
    """A bubble dome: body, light catch top-left, shadow bottom-right."""
    dark = shade(fr.get(min(15, x + 1), min(15, y + 1)), 0.6)
    if r == 1:
        cells = [(0, 0), (1, 0), (0, 1), (1, 1)]
        shadow = [(2, 1), (1, 2), (2, 2)]
        lights = [(0, 0)]
    elif r == 2:
        cells = [(dx, dy) for dx in range(-1, 2) for dy in range(-1, 2)]
        shadow = [(2, 0), (2, 1), (0, 2), (1, 2), (2, 2)]
        lights = [(-1, -1), (0, -1), (-1, 0)]
    else:
        cells = [(dx, dy) for dx in range(-1, 3) for dy in range(-1, 3) if (dx, dy) not in ((-1, -1), (2, -1), (-1, 2), (2, 2))]
        shadow = [(3, 0), (3, 1), (2, 2), (0, 3), (1, 3)]
        lights = [(0, -1), (-1, 0), (0, 0)]
    for dx, dy in shadow:
        fr.put(x + dx, y + dy, dark, 0.85)
    for dx, dy in cells:
        fr.put(x + dx, y + dy, body)
    for dx, dy in lights:
        fr.put(x + dx, y + dy, hi)


def burst(fr, x, y, r, p, hi, body, rnd_dirs):
    """Pop: a widening ring plus droplets thrown outward, all fading out."""
    fade = (1 - p) ** 1.4
    ring = r + 1 + round(1.5 * ease_out_cubic(p))
    for dx, dy in [(ring, 0), (-ring, 0), (0, ring), (0, -ring)]:
        fr.put(x + dx, y + dy, hi, 0.9 * fade)
    d = round(ring * 0.7)
    for dx, dy in [(d, d), (-d, d), (d, -d), (-d, -d)]:
        fr.put(x + dx, y + dy, body, 0.6 * fade)
    for ang in rnd_dirs:
        dist = r + 2 + 2.5 * ease_out_cubic(p)
        fr.put(round(x + math.cos(ang) * dist), round(y + math.sin(ang) * dist), hi, fade)
    fr.put(x, y, body, 0.5 * fade)


def events_for(seed):
    rnd = random.Random(seed)
    evs = []
    for i in range(BUBBLES):
        life = rnd.choice([4, 5, 5, 6, 7, 8])
        r = 3 if life >= 7 else rnd.choice([1, 1, 2, 2, 3]) if life >= 5 else 1
        start = (i * 7 + rnd.randrange(4)) % FRAMES
        # triangular distribution: the boil is fiercest over the fire in the middle
        x = round(2 + (rnd.random() + rnd.random()) / 2 * 11)
        y = round(2 + (rnd.random() + rnd.random()) / 2 * 11)
        dirs = [rnd.uniform(0, 2 * math.pi) for _ in range(rnd.choice([2, 3]))]
        evs.append((start, life, r, x, y, dirs))
    return evs


def render(base, f, evs, body, hi):
    fr = Frame(base, f)
    active = []
    for start, life, r, x, y, dirs in evs:
        age = (f - start) % FRAMES
        if age < life:
            active.append((age / (life - 1), r, x, y, dirs))

    # froth where bubbles crowd together
    for x in range(1, 15):
        for y in range(1, 15):
            crowd = sum(1 for u, r, bx, by, _ in active if abs(bx - x) + abs(by - y) <= r + 1)
            if crowd >= 3:
                fr.put(x, y, hi, min(0.4, 0.1 * crowd))

    # two boil heads that keep surging and bursting, half a loop apart
    for cx, cy, off in [(7, 7, 0), (9, 9, FRAMES // 2)]:
        u = ((f + off) % (FRAMES // 2)) / (FRAMES // 2)
        if u < 0.75:
            rad = 0.8 + 1.7 * ease_out_cubic(u / 0.75)
            for x in range(1, 15):
                for y in range(1, 15):
                    dist = math.hypot(x - cx, y - cy)
                    if dist <= rad:
                        fr.put(x, y, body, 0.6)
                        if x - cx + y - cy <= -rad * 0.6:
                            fr.put(x, y, hi, 0.8)
                    elif dist <= rad + 1:
                        fr.put(x, y, shade(fr.get(x, y), 0.7), 0.7)
        else:
            burst(fr, cx, cy, 2, (u - 0.75) / 0.25, hi, body, [0.4, 2.1, 3.7, 5.2])

    for u, r, x, y, dirs in active:
        if u < 0.6:
            g = ease_out_cubic(u / 0.6)
            size = 1 if g < 0.5 else max(1, round(r * g))
            dome(fr, x, y, size, body, hi)
        else:
            burst(fr, x, y, r, (u - 0.6) / 0.4, hi, body, dirs)

    img = Image.new("RGBA", (16, 16))
    for x in range(16):
        for y in range(16):
            img.putpixel((x, y), (*fr.get(x, y), 255))
    return img


def main():
    for name, (body, hi) in SOUPS.items():
        src = Image.open(DIRS[0] / f"hot_pot_top_{name}.png").convert("RGB")
        base = {(x, y): src.getpixel((x, y)) for x in range(16) for y in range(16)}
        # 随机种子按名字算；高汤 2026-10-24 从 milky 改名 rich_stock，种子照旧用 "milky"，动画保持原样
        evs = events_for(sum(map(ord, {"rich_stock": "milky"}.get(name, name))))
        sheet = anim.strip([render(base, f, evs, body, hi) for f in range(FRAMES)])
        for d in DIRS:
            anim.save(sheet, anim.mcmeta(FRAMETIME, interpolate=False), d / f"hot_pot_top_{name}_boiling.png")
        print("wrote", name)


if __name__ == "__main__":
    main()
    # 清汤 2026-10-23 换过配色（recolor_clear_soup.py，用户发了参考图）。这里生成的是换色前的样子，
    # 不接着换色的话重跑本脚本会把清汤变回浑黄色（2026-10-24 全面检查时发现，新增）
    import recolor_clear_soup
    recolor_clear_soup.main()
