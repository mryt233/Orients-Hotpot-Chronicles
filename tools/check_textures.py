"""贴图自检：画完先自己查出问题，不等用户发现。作者：mryt233（2026-11-01）

    python tools/check_textures.py                   # 查本模组全部贴图和模型
    python tools/check_textures.py --strict          # 有错误或白名单外的警告时返回 1（check_all 用这个）
    python tools/check_textures.py --file a.png --as item/duck_blood   # 查一张还没装进模组的候选图
    python tools/check_textures.py --baseline        # 把当前所有警告写进白名单（只在建立基线时用，之后逐条补原因）

检查项（等级：错误 = 进游戏会坏 / 警告 = 和原版风格或惯例不符，可以有理由地例外）：
  尺寸     贴图宽要是 16 的倍数（gui/、misc/ 不查）                                    错误
  动画     高图必须有 .mcmeta，高要是宽的整数倍，mcmeta 里的帧号不能越界                错误
  动画     有 .mcmeta 但图不是竖叠多帧                                                 警告
  半透明   物品贴图只能全透明或不透明，半透明像素在物品栏会发灰                         警告
  居中     物品贴图可见部分偏离格子中心超过 1 像素                                     警告
  居中     方块样式的物品图标（display.gui）投影中心偏离超过 0.5 像素（用户 #63 的规矩）  警告
  风格     颜色数 / 对比度超出原版同类范围（tools/vanilla_ref.py 的分类）               警告
  描边     原版同类 80% 以上有深色描边，这张没有                                       警告
  引用     模型引用了不存在的贴图                                                      错误
  引用     贴图没有任何模型、方块状态或代码用到                                        警告

白名单 tools/texture_allowlist.json：{"贴图路径": {"检查代号": "原因"}}，原因必须写，空原因不算数。
"""
import json
import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from PIL import Image  # noqa: E402

import vanilla_ref  # noqa: E402
from pixkit.paths import ASSETS, MOD, MODID, VANILLA  # noqa: E402
from pixkit.texstats import stats  # noqa: E402

TEX = ASSETS / "textures"
MODELS = ASSETS / "models"
ALLOW = Path(__file__).resolve().parent / "texture_allowlist.json"
SKIP_DIRS = ("gui/", "misc/")
CODE_DIRS = [MOD / "common/src", MOD / "fabric/src", MOD / "neoforge/src", MOD / "framework"]

ERR, WARN = "错误", "警告"
PBR_SUFFIXES = ("_n", "_s")
LABEL = {"size": "尺寸", "anim": "动画", "anim_meta": "动画", "semi": "半透明", "center": "居中", "gui_center": "居中",
         "colors": "风格", "contrast": "风格", "outline": "描边", "missing_tex": "引用", "unused": "引用",
         "pbr_mode": "PBR", "pbr_orphan": "PBR", "pbr_pair": "PBR", "pbr_size": "PBR", "pbr_meta": "PBR",
         "pbr_alpha": "PBR", "pbr_normal": "PBR", "pbr_f0": "PBR"}


def split_pbr_rel(rel: str):
    """若 rel 以 _n / _s 结尾，返回 (base_rel, suffix)，否则返回 (rel, None)。"""
    for suf in PBR_SUFFIXES:
        if rel.endswith(suf) and len(rel) > len(suf):
            return rel[:-len(suf)], suf
    return rel, None


def check_pbr_channels(im, suffix, base_im=None, mcmeta=None, base_meta=None):
    """LabPBR 1.3 副图规范检查（尺寸/动画一致、Alpha 全零、XY 法线长度、F0 保留段）。"""
    out = []
    if base_im is not None and im.size != base_im.size:
        out.append((ERR, "pbr_size", f"尺寸 {im.size[0]}×{im.size[1]} 与基础贴图 {base_im.size[0]}×{base_im.size[1]} 不一致"))
    if base_im is not None and ((mcmeta is not None) != (base_meta is not None) or (mcmeta is not None and mcmeta != base_meta)):
        out.append((ERR, "pbr_meta", ".mcmeta 存在或动画参数与基础贴图不一致"))
    data = list(im.get_flattened_data()) if hasattr(im, "get_flattened_data") else list(im.getdata())
    if all(px[3] == 0 for px in data):
        out.append((ERR, "pbr_alpha", "Alpha 通道全为 0（法线高度全塌陷或高光自发光字段异常）"))
    base_data = None
    if base_im is not None and base_im.size == im.size:
        b = base_im.convert("RGBA")
        base_data = list(b.get_flattened_data()) if hasattr(b, "get_flattened_data") else list(b.getdata())
    if suffix == "_n":
        bad_xy = 0
        for idx, (r, g, _b, _a) in enumerate(data):
            if base_data is not None and base_data[idx][3] == 0:
                continue
            nx = (r / 255.0) * 2.0 - 1.0
            ny = (g / 255.0) * 2.0 - 1.0
            if nx * nx + ny * ny >= 1.015:
                bad_xy += 1
        if bad_xy:
            out.append((ERR, "pbr_normal", f"{bad_xy} 个像素 XY 法线向量长度超限（>=1 会导致着色器 Z=NaN）"))
    elif suffix == "_s":
        reserved_f0 = 0
        for idx, (_r, g, _b, _a) in enumerate(data):
            if base_data is not None and base_data[idx][3] == 0:
                continue
            if 238 <= g <= 254:
                reserved_f0 += 1
        if reserved_f0:
            out.append((ERR, "pbr_f0", f"{reserved_f0} 个像素高光 G 通道落入 LabPBR 未定义保留段 238..254"))
    return out


def check_image(im, rel, mcmeta=None, base_im=None, base_meta=None):
    """查一张图。rel 形如 "item/duck_blood"（决定按物品还是方块的规矩查、归哪一类原版）。
    mcmeta：对应的 .mcmeta 字典（没有就 None）。返回 [(等级, 代号, 说明)]。"""
    out = []
    _base_rel, pbr_suffix = split_pbr_rel(rel)
    folder, name = rel.split("/", 1) if "/" in rel else ("item", rel)
    w, h = im.size
    if w % 16:
        out.append((ERR, "size", f"宽 {w} 不是 16 的倍数"))
    tall = h > w
    if tall and mcmeta is None:
        out.append((ERR, "anim", f"{w}×{h} 是高图但没有 .mcmeta，游戏里会显示成紫黑格"))
    if tall and h % w:
        out.append((ERR, "anim", f"高 {h} 不是宽 {w} 的整数倍"))
    if mcmeta is not None:
        if not tall:
            out.append((WARN, "anim_meta", "有 .mcmeta 但图不是竖叠多帧"))
        frames = (mcmeta.get("animation") or {}).get("frames")
        n = h // w if w and tall else 1
        for f in frames or []:
            idx = f["index"] if isinstance(f, dict) else f
            if not 0 <= idx < n:
                out.append((ERR, "anim", f".mcmeta 帧号 {idx} 越界（只有 {n} 帧）"))
                break
    if pbr_suffix is not None:
        out.extend(check_pbr_channels(im, pbr_suffix, base_im=base_im, mcmeta=mcmeta, base_meta=base_meta))
        return out
    s = stats(im)
    if folder == "item" and s["opaque"]:
        if s["semi"]:
            out.append((WARN, "semi", f"{s['semi']} 个半透明像素（物品栏里会发灰）"))
        cx, cy = s["center"]
        if abs(cx) > 1 or abs(cy) > 1:
            out.append((WARN, "center", f"可见部分偏离中心 ({cx:+.1f}, {cy:+.1f}) 像素"))
        for cat in vanilla_ref.category_of(name, "item"):
            prof = vanilla_ref.profile(cat)
            if not prof:
                continue
            zh = vanilla_ref.NAME[cat]
            lo, hi = prof["colors"]["min"], prof["colors"]["max"]
            if s["colors"] > hi * 1.6 or s["colors"] < max(2, lo // 2):
                out.append((WARN, "colors", f"{s['colors']} 种颜色，原版{zh}是 {lo}～{hi} 种"))
            if s["contrast"] < prof["contrast"]["min"] * 0.7:
                out.append((WARN, "contrast", f"对比度 {s['contrast']}，原版{zh}最低 {prof['contrast']['min']}（看着发平）"))
            if prof["outline_share"] >= 0.8 and not s["outline"]["dark"]:
                out.append((WARN, "outline", f"原版{zh} {prof['outline_share']:.0%} 有深色描边，这张没有"))
            break   # 只按第一个分类比
    return out


def _load_json(p):
    try:
        return json.loads(p.read_text(encoding="utf-8"))
    except (OSError, ValueError):
        return None


def model_refs():
    """所有本模组模型 → 引用的贴图。返回 {模型相对路径: [ns:path, ...]}。"""
    refs = {}
    for p in MODELS.rglob("*.json"):
        m = _load_json(p) or {}
        vals = [v for v in (m.get("textures") or {}).values() if isinstance(v, str) and not v.startswith("#")]
        refs[str(p.relative_to(MODELS)).replace("\\", "/")] = [v if ":" in v else f"minecraft:{v}" for v in vals]
    return refs


def texture_exists(ref):
    ns, path = ref.split(":", 1)
    if ns == MODID:
        return (TEX / f"{path}.png").is_file()
    if ns == "minecraft":
        return (VANILLA / "textures" / f"{path}.png").is_file() or True   # 原版缓存不全时不报
    return True


def _code_text():
    parts = []
    for d in CODE_DIRS:
        if d.is_dir():
            for p in d.rglob("*"):
                if p.suffix in (".java", ".json", ".js") and "build" not in p.parts:
                    try:
                        parts.append(p.read_text(encoding="utf-8", errors="replace"))
                    except OSError:
                        pass
    return "\n".join(parts)


def gui_centers():
    """方块样式物品图标的物品栏投影中心（复用 gui_center.py 的算法）。返回 {物品: (cx, cy)}。"""
    import gui_center
    out = {}
    for p in (MODELS / "item").glob("*.json"):
        m = _load_json(p) or {}
        parent = m.get("parent", "")
        if not parent.startswith(f"{MODID}:block/"):
            continue
        # mryt233 改（2026-11-10）：*_item 模板是原先直接写几何的物品；保持原有检查范围。
        if parent.startswith(f"{MODID}:block/template/") and parent.endswith("_item"):
            continue
        try:
            x0, x1, y0, y1, _ = gui_center.bbox(p.stem)
        except (KeyError, ValueError, OSError, TypeError):
            continue
        if x1 > x0:
            out[p.stem] = ((x0 + x1) / 2, (y0 + y1) / 2)
    return out


def scan():
    """查整个模组，返回 [(贴图或模型路径, 等级, 代号, 说明)]。"""
    found = []
    refs = model_refs()
    used = set()
    for model, rs in refs.items():
        for r in rs:
            if r.startswith(MODID + ":"):
                used.add(r.split(":", 1)[1])
            if not texture_exists(r):
                found.append((f"models/{model}", ERR, "missing_tex", f"引用的贴图 {r} 不存在"))
    code = _code_text()
    # 代码里拼接出来的贴图名（如 "block/sauce_" + id）：带这个前缀的贴图都算用到
    prefixes = set(re.findall(r'"((?:block|item)/[a-z0-9_]+_)"\s*\+', code))
    for p in sorted(TEX.rglob("*.png")):
        rel = str(p.relative_to(TEX)).replace("\\", "/")[:-4]
        if rel.startswith(SKIP_DIRS):
            continue
        meta_p = p.with_name(p.name + ".mcmeta")
        meta = _load_json(meta_p) if meta_p.exists() else None
        if meta_p.exists() and meta is None:
            found.append((rel, ERR, "anim", ".mcmeta 不是合法 JSON"))
        raw_im = Image.open(p)
        base_rel, pbr_suffix = split_pbr_rel(rel)
        base_im = None
        base_meta = None
        if pbr_suffix is not None:
            if raw_im.mode != "RGBA":
                found.append((rel, ERR, "pbr_mode", f"LabPBR 副图必须使用 RGBA 格式保留 Alpha 通道（当前 {raw_im.mode}）"))
            base_p = TEX / f"{base_rel}.png"
            partner_suffix = "_s" if pbr_suffix == "_n" else "_n"
            partner_p = TEX / f"{base_rel}{partner_suffix}.png"
            if not base_p.is_file():
                found.append((rel, ERR, "pbr_orphan", f"找不到对应的基础贴图 {base_rel}.png"))
            else:
                base_im = Image.open(base_p).convert("RGBA")
                base_meta_p = base_p.with_name(base_p.name + ".mcmeta")
                base_meta = _load_json(base_meta_p) if base_meta_p.exists() else None
            if not partner_p.is_file():
                found.append((rel, ERR, "pbr_pair", f"缺少配对贴图 {base_rel}{partner_suffix}.png"))
        for lvl, code_, msg in check_image(raw_im.convert("RGBA"), rel, meta, base_im=base_im, base_meta=base_meta):
            found.append((rel, lvl, code_, msg))
        usage_rel = base_rel if pbr_suffix is not None else rel
        name = usage_rel.split("/", 1)[1]
        if pbr_suffix is not None and not (TEX / f"{base_rel}.png").is_file():
            continue
        if usage_rel not in used and not any(usage_rel.startswith(x) for x in prefixes) and not re.search(r"[\"/:]" + re.escape(name) + r"[\"/.]", code) \
                and not re.search(re.escape(usage_rel), code):
            if pbr_suffix is None:
                found.append((rel, WARN, "unused", "没有模型、方块状态或代码用到这张贴图"))
    for item, (cx, cy) in gui_centers().items():
        if abs(cx) > 0.5 or abs(cy) > 0.5:
            found.append((f"models/item/{item}", WARN, "gui_center",
                          f"物品栏投影中心 ({cx:+.2f}, {cy:+.2f})，超过 0.5 像素就该调 display.gui.translation"))
    return found


def load_allow():
    data = _load_json(ALLOW) or {}
    return {k: {c: r for c, r in v.items() if str(r).strip()} for k, v in data.items() if not k.startswith("_")}


def main(argv):
    if "--file" in argv:
        path = Path(argv[argv.index("--file") + 1])
        rel = argv[argv.index("--as") + 1] if "--as" in argv else f"item/{path.stem}"
        meta_p = path.with_name(path.name + ".mcmeta")
        res = check_image(Image.open(path).convert("RGBA"), rel, _load_json(meta_p) if meta_p.exists() else None)
        for lvl, c, msg in res:
            print(f"[{lvl}] {LABEL[c]}：{msg}")
        print("没有问题" if not res else "")
        return 1 if any(l == ERR for l, _, _ in res) else 0
    found = scan()
    if "--baseline" in argv:
        old = _load_json(ALLOW) or {}
        allow = {k: v for k, v in old.items()}
        allow.setdefault("_说明", "贴图自检白名单：{路径: {检查代号: 原因}}。原因为空的条目不算数，要补上。")
        for rel, lvl, c, msg in found:
            if lvl == WARN:
                allow.setdefault(rel, {}).setdefault(c, "")
        ALLOW.write_bytes((json.dumps(allow, ensure_ascii=False, indent=2) + "\n").encode())
        print(f"已写 {ALLOW}")
        return 0
    allow = load_allow()
    errors = [f for f in found if f[1] == ERR]
    new_warn = [f for f in found if f[1] == WARN and f[2] not in allow.get(f[0], {})]
    known = [f for f in found if f[1] == WARN and f[2] in allow.get(f[0], {})]
    n_tex = sum(1 for p in TEX.rglob("*.png") if not str(p.relative_to(TEX)).replace("\\", "/").startswith(SKIP_DIRS))
    print(f"查了 {n_tex} 张贴图、{len(model_refs())} 个模型；错误 {len(errors)}，新警告 {len(new_warn)}，白名单内 {len(known)}")
    for rel, lvl, c, msg in errors + new_warn:
        print(f"  [{lvl}] {LABEL[c]} {rel}：{msg}")
    if "--verbose" in argv:
        for rel, lvl, c, msg in known:
            print(f"  [已知] {LABEL[c]} {rel}：{msg}（{allow[rel][c]}）")
    if "--strict" in argv and (errors or new_warn):
        return 1
    return 0


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    raise SystemExit(main(sys.argv[1:]))
