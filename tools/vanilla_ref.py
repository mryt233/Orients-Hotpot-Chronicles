"""原版贴图参考库：画新东西之前，先查原版同类东西怎么画、用了哪些颜色。作者：mryt233（2026-11-01）

    python tools/vanilla_ref.py build                    # 重建索引 tools/out/vanilla_ref.json（原版更新后跑一次）
    python tools/vanilla_ref.py cats                     # 所有分类和数量
    python tools/vanilla_ref.py find 生肉                 # 按分类找（中文名或英文键）
    python tools/vanilla_ref.py find --like cooked_beef  # 和某张原版贴图同类的
    python tools/vanilla_ref.py show beef cooked_beef    # 单张的色板、明暗级数、描边
    python tools/vanilla_ref.py profile 熟肉              # 同类原版的典型范围（颜色数、明暗级数、对比度……）
    python tools/vanilla_ref.py sheet 熟肉                # 放大拼图 build/vanilla_ref/熟肉.png
    python tools/vanilla_ref.py ramp 生肉                 # 同类原版最常用的颜色，按明暗排好（画新东西直接取色）

数据来自查看器缓存的原版 1.21.1 贴图（viewer/cache）。分类按文件名规则（CATEGORIES），一张图可以属于多个分类；
本模组物品用同一套规则归类（category_of），贴图自检和方案对比板据此找原版对照。
别的工具这样用：from vanilla_ref import load, category_of, peers, profile
"""
import json
import re
import sys
from collections import Counter
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from pixkit.color import luma  # noqa: E402
from pixkit.paths import MOD, OUT, VANILLA  # noqa: E402
from pixkit.texstats import stats  # noqa: E402

INDEX = OUT / "vanilla_ref.json"
TEX = VANILLA / "textures"

# (英文键, 中文名, 目录, 文件名正则, 是否排除 cooked_ 开头)
CATEGORIES = [
    ("raw_meat", "生肉", "item",
     r"^(beef|porkchop|mutton|chicken|rabbit|rotten_flesh)$|loin|_roll$|minced|^tripe$|meatball$|blood|liver|kidney|intestine|gizzard|^raw_ham_sausage$", True),
    ("cooked_meat", "熟肉", "item",
     r"^cooked_(beef|porkchop|mutton|chicken|rabbit)$|^cooked_.*(beef|pork|mutton|tripe|meatball|roll|loin|blood|liver|kidney|intestine|gizzard)|^(cooked_)?ham_sausage", False),
    ("raw_fish", "生鱼海鲜", "item", r"^(cod|salmon|tropical_fish|pufferfish)$|shrimp|fish_ball$|squid", True),
    ("cooked_fish", "熟鱼海鲜", "item", r"^cooked_(cod|salmon)$|^cooked_.*(shrimp|fish_ball|squid)", False),
    ("vegetable", "蔬菜", "item",
     r"^(carrot|potato|baked_potato|poisonous_potato|beetroot|golden_carrot|dried_kelp)$|potato_slice|scallion|cilantro$|garlic$|tomato$|soybean|kelp", False),
    ("fruit", "水果", "item",
     r"^(apple|golden_apple|enchanted_golden_apple|melon_slice|glistering_melon_slice|sweet_berries|glow_berries|chorus_fruit|popped_chorus_fruit)$|orange$", False),
    ("bread_sweet", "面食甜点豆制品", "item", r"^(bread|cookie|pumpkin_pie|cake)$|tofu", False),
    ("bowl_food", "碗装", "item", r"_stew$|_soup$|^bowl$", False),
    ("bottle", "瓶装饮料", "item",
     r"^(potion|splash_potion|lingering_potion|honey_bottle|glass_bottle|experience_bottle|dragon_breath)$|bottle|drink|cola", False),
    ("seasoning", "调料粉末", "item",
     r"^(sugar|gunpowder|glowstone_dust|redstone|blaze_powder|bone_meal)$|_dye$|paste$|hotpot_base$", False),
    ("plant", "植物种子", "item", r"seeds$|^(wheat|sugar_cane|bamboo|stick|nether_wart|cocoa_beans)$", False),
    ("tool", "工具", "item",
     r"_(sword|pickaxe|axe|shovel|hoe)$|^(shears|flint_and_steel|fishing_rod|brush|spyglass)$|knife|chopsticks|rolling_pin", False),
    ("ingot_gem", "锭与宝石", "item",
     r"_ingot$|^(diamond|emerald|amethyst_shard|lapis_lazuli|quartz|coal|charcoal|netherite_scrap|raw_iron|raw_gold|raw_copper|brick|nether_brick)$", False),
    ("container", "桶与容器", "item", r"bucket$|^(bowl|bundle)$", False),
    ("book", "书", "item", r"^(book|written_book|writable_book|enchanted_book|knowledge_book)$|guide$", False),
    ("block_stone", "石质方块", "block", r"stone|brick|deepslate|andesite|diorite|granite|blackstone|tuff|cobble", False),
    ("block_wood", "木质方块", "block", r"planks$|_log$|_log_top$|stripped_", False),
    ("block_metal", "金属方块", "block",
     r"^(iron_block|gold_block|copper_block|cut_copper|exposed_|weathered_|oxidized_|raw_.*_block)", False),
    ("liquid", "液体", "block", r"^(water|lava)_(still|flow)$|cauldron", False),
]
ZH = {zh: key for key, zh, *_ in CATEGORIES}
NAME = {key: zh for key, zh, *_ in CATEGORIES}


def category_of(name, folder="item"):
    """物品 / 方块贴图名 → 分类键列表（原版与本模组共用这套规则）。"""
    out = []
    for key, _, f, pat, no_cooked in CATEGORIES:
        if f != folder or (no_cooked and name.startswith("cooked_")):
            continue
        if re.search(pat, name):
            out.append(key)
    return out


def build():
    data = {}
    for folder in ("item", "block"):
        for p in sorted((TEX / folder).glob("*.png")):
            try:
                s = stats(p)
            except Exception as e:  # 少数特殊格式
                print("跳过", p.name, e)
                continue
            s["cats"] = category_of(p.stem, folder)
            data[f"{folder}/{p.stem}"] = s
    INDEX.parent.mkdir(parents=True, exist_ok=True)
    INDEX.write_bytes(json.dumps(data, ensure_ascii=False, separators=(",", ":")).encode())
    print(f"已写 {INDEX}（{len(data)} 张）")
    return data


_CACHE = {}


def load():
    if "d" not in _CACHE:
        _CACHE["d"] = json.loads(INDEX.read_text(encoding="utf-8")) if INDEX.exists() else build()
    return _CACHE["d"]


def key_of(cat):
    return ZH.get(cat, cat)


def peers(cat, data=None):
    """某分类下的原版贴图 {"item/名字": 统计}。cat 可写中文名。"""
    data = data or load()
    key = key_of(cat)
    return {k: v for k, v in data.items() if key in v["cats"]}


def profile(cat, data=None):
    """某分类原版贴图的典型范围（min / p10 / median / p90 / max），以及有深色描边的比例。"""
    ps = [p for p in peers(cat, data).values() if p["opaque"]]
    if not ps:
        return None

    def rng(f):
        v = sorted(f(p) for p in ps)
        n = len(v)
        return {"min": v[0], "p10": v[n // 10], "median": v[n // 2], "p90": v[min(n - 1, (n * 9) // 10)], "max": v[-1]}
    return {"n": len(ps), "colors": rng(lambda p: p["colors"]), "shades": rng(lambda p: p["shades"]),
            "contrast": rng(lambda p: p["contrast"]), "fill": rng(lambda p: p["fill"]),
            "outline_share": round(sum(1 for p in ps if p["outline"]["dark"]) / len(ps), 2)}


def ramp(cat, n=8, data=None):
    """同类原版贴图里最常用的 n 种颜色（按像素数加权），由暗到亮。"""
    cnt = Counter()
    for p in peers(cat, data).values():
        for c, k in p["palette"]:
            cnt[tuple(c)] += k
    return sorted((c for c, _ in cnt.most_common(n)), key=luma)


def hexc(c):
    return "#%02X%02X%02X" % tuple(c[:3])


def describe(k, s):
    o = s["outline"]
    anim = f" 动画{s['frames']}帧" if s["frames"] > 1 else ""
    lines = [f"{k}  分类 {[NAME[c] for c in s['cats']] or '-'}  {s['size'][0]}×{s['size'][1]}{anim}"]
    if s["opaque"]:
        lines.append(f"  颜色 {s['colors']} 种，主色相 {s['shades']} 级明暗，亮度 {s['luma'][0]}～{s['luma'][1]}（对比 {s['contrast']}），"
                     f"填充 {s['fill']:.0%}，描边{'：深色' if o['dark'] else '：无'}（边/内亮度比 {o['edge_ratio']}）")
        lines.append("  色板（暗→亮）：" + " ".join(f"{hexc(c)}×{n}" for c, n in s["palette"]))
    return "\n".join(lines)


def show(names, data=None):
    data = data or load()
    for n in names:
        k = n if "/" in n else (f"item/{n}" if f"item/{n}" in data else f"block/{n}")
        print(describe(k, data[k]) if k in data else f"{n}：没找到")


def sheet(cat, data=None, scale=6):
    """放大拼图：每张原版贴图放大 scale 倍，下面写名字。"""
    from PIL import Image, ImageDraw
    data = data or load()
    items = sorted(peers(cat, data))
    if not items:
        print("这个分类没有贴图：", cat)
        return None
    cell, cols = 16 * scale + 12, 10
    rows = (len(items) + cols - 1) // cols
    out = Image.new("RGBA", (cols * cell, rows * (cell + 12)), (198, 198, 198, 255))
    d = ImageDraw.Draw(out)
    for i, k in enumerate(items):
        im = Image.open(TEX / f"{k}.png").convert("RGBA")
        w = im.size[0]
        im = im.crop((0, 0, w, w)).resize((16 * scale, 16 * scale), Image.NEAREST)
        x, y = (i % cols) * cell + 6, (i // cols) * (cell + 12) + 6
        out.alpha_composite(im, (x, y))
        d.text((x, y + 16 * scale + 1), k.split("/")[1][:17], fill=(40, 40, 40, 255))
    path = MOD / "build/vanilla_ref" / f"{NAME.get(key_of(cat), cat)}.png"
    path.parent.mkdir(parents=True, exist_ok=True)
    out.save(path)
    print("已出图", path)
    return path


def main(argv):
    if not argv or argv[0] in ("-h", "--help"):
        print(__doc__)
        return
    cmd, rest = argv[0], argv[1:]
    data = build() if cmd == "build" else load()
    if cmd == "cats":
        for key, zh, folder, *_ in CATEGORIES:
            print(f"{zh}（{key}，{folder}）：{len(peers(key, data))} 张")
    elif cmd == "find":
        if rest[:1] == ["--like"]:
            k = rest[1] if "/" in rest[1] else f"item/{rest[1]}"
            cats = data[k]["cats"]
            print(f"{k} 属于 {[NAME[c] for c in cats]}")
            names = sorted({n for c in cats for n in peers(c, data)})
        else:
            names = sorted(peers(rest[0], data))
            print(f"分类 {rest[0]}：{len(names)} 张")
        show(names, data)
    elif cmd == "show":
        show(rest, data)
    elif cmd == "profile":
        print(json.dumps(profile(rest[0], data), ensure_ascii=False, indent=1))
    elif cmd == "ramp":
        print(" ".join(hexc(c) for c in ramp(rest[0], int(rest[1]) if len(rest) > 1 else 8, data)))
    elif cmd == "sheet":
        sheet(rest[0], data)
    elif cmd != "build":
        print("不认识的命令：", cmd)
        print(__doc__)


if __name__ == "__main__":
    main(sys.argv[1:])
