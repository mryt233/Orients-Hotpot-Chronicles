"""颜色运算。作者：mryt233

颜色一律是元组：RGB (r, g, b) 或 RGBA (r, g, b, a)。
注意 mix 与 lerp 是两种插值写法，t 落在 .5 附近时取整可能差 1，迁移旧脚本时照原写法选。
"""


def clamp(c):
    """每个通道截到 0..255，**向零取整**（int）。旧 gen_ingredients / gen_cutting / gen_meatballs 的写法。"""
    return tuple(max(0, min(255, int(v))) for v in c)


def shade(c, k):
    """按比例变亮 / 变暗，**四舍五入**后截到 0..255。k<1 变暗，k>1 变亮。"""
    return tuple(max(0, min(255, round(v * k))) for v in c)


def shade_floor(c, k):
    """同 shade，但**向零取整**（= clamp(v*k)）。旧 gen_ingredients / gen_cutting / gen_meatballs 的写法。"""
    return clamp(tuple(v * k for v in c))


def mix(a, b, t, channels=3):
    """a + (b - a) * t，取前 channels 个通道，不截 t。旧 gen_cup / gen_drink_cauldron / gen_guide_gui 的写法。"""
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(channels))


def lerp(a, b, t, channels=3):
    """a * (1 - t) + b * t，t 先截到 0..1。旧 gen_boiling / gen_stove_fire 的写法。"""
    t = max(0.0, min(1.0, t))
    return tuple(round(a[i] * (1 - t) + b[i] * t) for i in range(channels))


def rgba(c, a=255):
    """RGB 补上透明度；已经是 RGBA 的替换透明度。"""
    return tuple(c[:3]) + (a,)


def hexc(s, a=255):
    """'#3F76E4' / '3f76e4' → (63, 118, 228, 255)。"""
    s = s.lstrip("#")
    return tuple(int(s[i:i + 2], 16) for i in (0, 2, 4)) + (a,)


def luma(c):
    """感知亮度 0..255（Rec. 601）。"""
    return 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]


def ramp(base, steps=5, dark=0.55, light=1.25):
    """从一个基色生成由暗到亮的色阶（steps 级，基色在中间附近）。原版食物通常 4～6 级。"""
    if steps < 2:
        return [tuple(base[:3])]
    return [shade(base[:3], dark + (light - dark) * i / (steps - 1)) for i in range(steps)]


def ease_out_cubic(x):
    return 1 - (1 - x) ** 3
