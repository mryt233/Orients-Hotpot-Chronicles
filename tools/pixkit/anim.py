"""动画贴图（竖着叠多帧 + .png.mcmeta）。作者：mryt233

可乐气泡沿用 tools/fizz.py（这里转一下，方便统一 import）。
"""
from pathlib import Path

from PIL import Image

from .io import save_png, write_json


def strip(frames):
    """多帧（同尺寸）竖着叠成一张动画贴图。"""
    w, h = frames[0].size
    sheet = Image.new("RGBA", (w, h * len(frames)))
    for i, f in enumerate(frames):
        sheet.paste(f, (0, i * h))
    return sheet


def mcmeta(frametime=1, interpolate=None):
    a = {"frametime": frametime}
    if interpolate is not None:
        a["interpolate"] = interpolate
    return {"animation": a}


def save(sheet, meta, path):
    """写动画贴图和同名 .png.mcmeta（LF）。"""
    path = Path(path)
    save_png(sheet, path)
    write_json(path.with_name(path.name + ".mcmeta"), meta)


def fizz_animate(base, zones, bubble, glint=None, seed=7):
    """可乐气泡，参数同 tools/fizz.animate。"""
    import sys
    sys.path.insert(0, str(Path(__file__).resolve().parent.parent))
    import fizz
    return fizz.animate(base, zones, bubble, glint=glint, seed=seed)
