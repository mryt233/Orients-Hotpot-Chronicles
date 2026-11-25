"""预览图上写中文。作者：mryt233（2026-11-01，从 gen_sausage.font 抽出）"""
import os
from pathlib import Path

from PIL import ImageFont

_CACHE = {}


def windows_fonts():
    """mryt233 改：从运行环境定位系统字体，不依赖安装盘。"""
    system = os.environ.get("WINDIR") or os.environ.get("SystemRoot")
    return Path(system) / "Fonts" if system else None


def font(size, bold=False):
    """微软雅黑（Windows）或 Noto Sans CJK（Linux）。"""
    key = (size, bold)
    if key not in _CACHE:
        windows = windows_fonts()
        candidates = [Path("/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc")]
        if windows is not None:
            candidates.insert(0, windows / ("msyhbd.ttc" if bold else "msyh.ttc"))
        for path in candidates:
            if path.exists():
                _CACHE[key] = ImageFont.truetype(str(path), size)
                break
        else:
            raise FileNotFoundError("预览文字需要微软雅黑或 Noto Sans CJK 字体")
    return _CACHE[key]
