"""0.3.1-alpha Phase 6：基于 pbr_materials.json 为重点硬表面、汤面与器皿生成 LabPBR 1.3 副图（_n / _s）。

用法：
    python mod/tools/gen_pbr.py                 # 生成全部重点 LabPBR 副图、同步动画 .mcmeta，并在 tools/out/pbr/ 出预览图版
    python mod/tools/gen_pbr.py --check         # 仅检查配置引用、现有副图与 LabPBR 合规性（不改写文件）
    python mod/tools/gen_pbr.py --out <dir>     # 隔离资源目录输出

作者：mryt233
"""
from __future__ import annotations

import json
import math
import shutil
import sys
from pathlib import Path
from PIL import Image, ImageDraw

from pixkit.io import save_png
from pixkit.paths import ASSETS, MOD, RES

TOOLS = Path(__file__).resolve().parent
CFG_PATH = TOOLS / "pbr_materials.json"
OUT_DIR = TOOLS / "out/pbr"
TEX_ROOT = ASSETS / "textures"


def _clamp_byte(v: float, lo: int = 0, hi: int = 255) -> int:
    return max(lo, min(hi, round(v)))


def _lerp_range(pair: list[int], t: float) -> int:
    lo, hi = pair
    return _clamp_byte(lo + (hi - lo) * max(0.0, min(1.0, t)))


def _luma01(rgb: tuple[int, int, int]) -> float:
    return (0.299 * rgb[0] + 0.587 * rgb[1] + 0.114 * rgb[2]) / 255.0


def _sample_height(heights: list[list[float]], alpha_ok: list[list[bool]], w: int, h: int, x: int, y: int, wrap: bool, fallback: float) -> float:
    if wrap:
        sx, sy = x % w, y % h
    else:
        sx, sy = max(0, min(w - 1, x)), max(0, min(h - 1, y))
    if not alpha_ok[sy][sx]:
        return fallback
    return heights[sy][sx]


def _render_frame(frame: Image.Image, preset: dict) -> tuple[Image.Image, Image.Image]:
    """将单帧 W×W RGBA 基础图转为 LabPBR 1.3 的 (_n, _s) 图像对。"""
    w, h = frame.size
    src = frame.load()
    wrap = bool(preset.get("wrap_uv", False))
    h_lo, h_hi = preset.get("height", [180, 255])
    n_strength = float(preset.get("normal_strength", 1.5))
    ao_strength = float(preset.get("ao_strength", 0.25))

    lumas = [[_luma01(src[x, y][:3]) for x in range(w)] for y in range(h)]
    alpha_ok = [[src[x, y][3] > 0 for x in range(w)] for y in range(h)]
    opaque_lumas = [lumas[y][x] for y in range(h) for x in range(w) if alpha_ok[y][x]]
    mean_luma = sum(opaque_lumas) / len(opaque_lumas) if opaque_lumas else 0.5

    heights = [[0.0] * w for _ in range(h)]
    for y in range(h):
        for x in range(w):
            if not alpha_ok[y][x]:
                heights[y][x] = 1.0
                continue
            centered = 0.5 + (lumas[y][x] - mean_luma) * 1.45
            heights[y][x] = max(0.0, min(1.0, centered))

    norm_img = Image.new("RGBA", (w, h), (128, 128, 255, 255))
    spec_img = Image.new("RGBA", (w, h), (0, 0, 0, 255))
    npix = norm_img.load()
    spix = spec_img.load()

    split = preset.get("split_rule") or {}
    emissive_mask = preset.get("emissive_mask")
    base_smooth = preset["smoothness"]
    base_f0 = int(preset["f0"])
    base_porosity = int(preset.get("porosity", 0))
    base_emissive = int(preset.get("emissive", 255))

    for y in range(h):
        for x in range(w):
            r, g, b, a = src[x, y]
            if a == 0:
                npix[x, y] = (128, 128, 255, 255)
                spix[x, y] = (0, 0, 0, 255)
                continue

            hc = heights[y][x]
            # 3x3 Sobel 高度导数（wrap 复用平铺边缘，非 wrap 钳位边界并回退透明邻域）
            h_tl = _sample_height(heights, alpha_ok, w, h, x - 1, y - 1, wrap, hc)
            h_tc = _sample_height(heights, alpha_ok, w, h, x,     y - 1, wrap, hc)
            h_tr = _sample_height(heights, alpha_ok, w, h, x + 1, y - 1, wrap, hc)
            h_ml = _sample_height(heights, alpha_ok, w, h, x - 1, y,     wrap, hc)
            h_mr = _sample_height(heights, alpha_ok, w, h, x + 1, y,     wrap, hc)
            h_bl = _sample_height(heights, alpha_ok, w, h, x - 1, y + 1, wrap, hc)
            h_bc = _sample_height(heights, alpha_ok, w, h, x,     y + 1, wrap, hc)
            h_br = _sample_height(heights, alpha_ok, w, h, x + 1, y + 1, wrap, hc)

            dx = ((h_tr + 2.0 * h_mr + h_br) - (h_tl + 2.0 * h_ml + h_bl)) * 0.25
            dy = ((h_bl + 2.0 * h_bc + h_br) - (h_tl + 2.0 * h_tc + h_tr)) * 0.25

            # LabPBR 1.3 DirectX top-down：R=左(-)->右(+), G=上(-)->下(+)
            nx = -dx * n_strength
            ny = -dy * n_strength
            mag = math.hypot(nx, ny)
            if mag > 0.94:
                scale = 0.94 / mag
                nx *= scale
                ny *= scale
            nr = _clamp_byte((nx * 0.5 + 0.5) * 255.0)
            ng = _clamp_byte((ny * 0.5 + 0.5) * 255.0)
            nb = _clamp_byte(255.0 - (1.0 - hc) * ao_strength * 255.0)
            na = _clamp_byte(h_lo + (h_hi - h_lo) * hc, 1, 255)
            npix[x, y] = (nr, ng, nb, na)

            lum = lumas[y][x]
            chroma = max(r, g, b) - min(r, g, b)
            smooth_pair = base_smooth
            f0 = base_f0
            porosity = base_porosity
            emissive = base_emissive

            kind = split.get("kind")
            if kind == "lowsat_metal":
                is_metal = chroma <= int(split.get("max_chroma", 32)) and lum >= float(split.get("min_luma", 0.2))
                if not is_metal:
                    f0 = int(split.get("fallback_f0", 10))
                    smooth_pair = split.get("fallback_smoothness", base_smooth)
                    porosity = int(split.get("fallback_porosity", 24))
            elif kind == "glass_or_liquid":
                is_glass = chroma <= int(split.get("max_chroma", 34)) and lum >= float(split.get("min_luma", 0.4))
                if not is_glass:
                    f0 = int(split.get("liquid_f0", 11))
                    smooth_pair = split.get("liquid_smoothness", base_smooth)
                    porosity = int(split.get("liquid_porosity", 120))

            if emissive_mask is not None:
                u0, v0, u1, v1 = emissive_mask["uv_rect"]
                u = (x + 0.5) / w
                v = (y + 0.5) / h
                if u0 <= u <= u1 and v0 <= v <= v1 and r >= int(emissive_mask["min_r"]) and (r - b) >= int(emissive_mask["min_warmth"]):
                    strength = min(1.0, max(0.0, (r - emissive_mask["min_r"]) / max(1.0, 255.0 - emissive_mask["min_r"])))
                    elo, ehi = emissive_mask["emissive_range"]
                    emissive = _clamp_byte(elo + (ehi - elo) * strength, 1, 254)
                    smooth_pair = emissive_mask.get("fire_smoothness", smooth_pair)
                    f0 = int(emissive_mask.get("fire_f0", f0))
                    porosity = int(emissive_mask.get("fire_porosity", porosity))

            sr = _lerp_range(smooth_pair, lum)
            spix[x, y] = (sr, max(0, min(255, f0)), max(0, min(255, porosity)), max(0, min(255, emissive)))

    return norm_img, spec_img


def generate_pair(base_path: Path, preset: dict) -> tuple[Image.Image, Image.Image]:
    """按单帧或竖叠动画贴图逐帧生成完整的 (_n, _s) LabPBR 对。"""
    base = Image.open(base_path).convert("RGBA")
    w, h = base.size
    if h <= w or h % w != 0:
        return _render_frame(base, preset)
    frames = h // w
    norm_sheet = Image.new("RGBA", (w, h))
    spec_sheet = Image.new("RGBA", (w, h))
    for i in range(frames):
        sub = base.crop((0, i * w, w, (i + 1) * w))
        fn, fs = _render_frame(sub, preset)
        norm_sheet.paste(fn, (0, i * w))
        spec_sheet.paste(fs, (0, i * w))
    return norm_sheet, spec_sheet


def _sync_mcmeta(base_path: Path, norm_path: Path, spec_path: Path) -> int:
    meta_src = base_path.with_name(base_path.name + ".mcmeta")
    count = 0
    for target in (norm_path, spec_path):
        meta_dst = target.with_name(target.name + ".mcmeta")
        if meta_src.is_file():
            meta_dst.write_bytes(meta_src.read_bytes())
            count += 1
        elif meta_dst.is_file():
            meta_dst.unlink()
    return count


def _build_preview_sheet(samples: list[tuple[str, Image.Image, Image.Image, Image.Image]], out_path: Path) -> None:
    cols = 4  # base, normal, spec(R/G/B), spec-A(emissive)
    cell = 32
    pad = 4
    rows = len(samples)
    w = 220 + cols * (cell + pad)
    h = 12 + rows * (cell + pad)
    sheet = Image.new("RGBA", (w, h), (22, 24, 30, 255))
    draw = ImageDraw.Draw(sheet)
    for idx, (label, base, norm, spec) in enumerate(samples):
        y = 8 + idx * (cell + pad)
        draw.text((8, y + 10), label[:30], fill=(225, 228, 236, 255))
        bw = base.size[0]
        b0 = base.crop((0, 0, bw, bw)).resize((cell, cell), Image.Resampling.NEAREST)
        n0 = norm.crop((0, 0, bw, bw)).convert("RGB").resize((cell, cell), Image.Resampling.NEAREST).convert("RGBA")
        s0 = spec.crop((0, 0, bw, bw)).convert("RGB").resize((cell, cell), Image.Resampling.NEAREST).convert("RGBA")
        sa = spec.crop((0, 0, bw, bw)).getchannel("A").convert("RGB").resize((cell, cell), Image.Resampling.NEAREST).convert("RGBA")
        for col_i, panel in enumerate((b0, n0, s0, sa)):
            sheet.paste(panel, (220 + col_i * (cell + pad), y))
    out_path.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(out_path)


def run(check_only: bool = False) -> int:
    cfg = json.loads(CFG_PATH.read_text(encoding="utf-8"))
    presets = cfg["presets"]
    mapping = cfg["textures"]
    if not mapping:
        raise SystemExit("pbr_materials.json textures 为空")

    missing = []
    previews = []
    seen_presets = set()
    written_png = 0
    written_meta = 0

    for rel, preset_name in sorted(mapping.items()):
        if preset_name not in presets:
            raise SystemExit(f"{rel} 引用了未知材质预设 {preset_name}")
        base_path = TEX_ROOT / f"{rel}.png"
        if not base_path.is_file():
            missing.append(rel)
            continue
        norm_path = TEX_ROOT / f"{rel}_n.png"
        spec_path = TEX_ROOT / f"{rel}_s.png"
        if check_only:
            if not norm_path.is_file() or not spec_path.is_file():
                missing.append(f"{rel}(_n/_s 缺失)")
            continue
        norm_img, spec_img = generate_pair(base_path, presets[preset_name])
        save_png(norm_img, norm_path)
        save_png(spec_img, spec_path)
        written_png += 2
        written_meta += _sync_mcmeta(base_path, norm_path, spec_path)
        if preset_name not in seen_presets or "stove_" in rel or "hot_pot_copper_side" == rel or "iron_kitchen_knife" in rel:
            seen_presets.add(preset_name)
            previews.append((rel, Image.open(base_path).convert("RGBA"), norm_img, spec_img))

    if missing:
        raise SystemExit(f"基础或副图缺失 {len(missing)} 项：{', '.join(missing[:8])}")
    if not check_only:
        _build_preview_sheet(previews, OUT_DIR / "preview_sheet.png")
    print(f"LabPBR {'检查通过' if check_only else '生成完成'}：目标 {len(mapping)} 组基础贴图（写入 {written_png} 张 _n/_s PNG，{written_meta} 份 .mcmeta）")
    return 0


def main() -> int:
    check_only = "--check" in sys.argv
    return run(check_only=check_only)


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    raise SystemExit(main())
