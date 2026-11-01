"""预览图上写中文。作者：mryt233（2026-11-01，从 gen_sausage.font 抽出）"""
from pathlib import Path

from PIL import ImageFont

_CACHE = {}


def font(size, bold=False):
    """微软雅黑（Windows）或 Noto Sans CJK（Linux）。"""
    key = (size, bold)
    if key not in _CACHE:
        windows = Path("C:/Windows/Fonts") / ("msyhbd.ttc" if bold else "msyh.ttc")
        for path in (windows, Path("/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc")):
            if path.exists():
                _CACHE[key] = ImageFont.truetype(str(path), size)
                break
        else:
            raise FileNotFoundError("预览文字需要微软雅黑或 Noto Sans CJK 字体")
    return _CACHE[key]
