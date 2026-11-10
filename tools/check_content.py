# mryt233 改（2026-11-10）：同时识别旧字段写法与传入 registration 的生命周期注册。
"""内容清单的漂移检查。

用途：一条内容在代码、模型、贴图、配方、10 种语言、游戏内指南里都要有对应物，
人工很容易漏。这个脚本按 `mod/content/items.json` 逐项核对，缺什么直接列出来。

判定规则（别用「有没有同名 png / item.xxx 键」这种粗规则，会误报）：
    - 代码：ID 在 `common/src/main/java` 任意一个 Java 文件里出现过（helper / 枚举注册也算）。
    - 模型：`models/item/<id>.json` 存在，且 `overrides` 里引用的模型也都存在。
    - 贴图：顺着模型 parent 链收集 textures，逐个核对 `textures/<路径>.png`。
      方块物品的贴图在 block 模型里，所以不能直接找 `textures/item/<id>.png`。
    - 语言：`item.<ns>.<id>` 或 `block.<ns>.<id>` 任一存在即可。
    - 配方：清单里列的配方文件要存在。
    - 指南：清单标了 guide 的要在 `guide_content.py` 里讲到；
      食物没标 guide 的只作为提醒（不算失败），因为新物品常常最后才补文案。

用法（工作区根目录或 mod/ 下都行）：
    python mod/tools/check_content.py          # 报告
    python mod/tools/check_content.py --strict # 有缺失时返回 1（可挂 CI）

注意：它只检查「有没有」，不检查数值是否正确——数值归 gen_* 脚本和 Config 默认值管。
作者：mryt233
"""
from __future__ import annotations

import json
import re
import sys
from pathlib import Path

MOD = Path(__file__).resolve().parent.parent
MANIFEST = MOD / "content/items.json"
RES = MOD / "common/src/main/resources"
ASSETS = RES / "assets/orients_hotpot_chronicles"
MODELS = ASSETS / "models"
TEXTURES = ASSETS / "textures"
DATA_RECIPES = RES / "data/orients_hotpot_chronicles/recipe"
LANGS = ASSETS / "lang"
COMMON_JAVA = MOD / "common/src/main/java"
GUIDE = MOD / "tools/guide_content.py"
NS = "orients_hotpot_chronicles"
VANILLA = MOD.parent / "viewer/cache/1.21.1/assets/minecraft"


def split_ref(ref: str) -> tuple[str, str]:
    """`ns:path` → (ns, path)；只给 path 时用本模组的命名空间。"""
    ns, _, path = ref.partition(":")
    if not path:
        return NS, ns
    return ns, path


def load_model(ref: str) -> dict | None:
    """读模型；原版（minecraft:）的当作终点，不报缺失。"""
    ns, path = split_ref(ref)
    if ns != NS:
        return {}
    file = MODELS / f"{path}.json"
    if not file.is_file():
        return None
    try:
        return json.loads(file.read_text(encoding="utf-8"))
    except json.JSONDecodeError:
        return None


def texture_exists(ref: str) -> bool:
    """贴图是否存在；原版贴图去查看器的原版资源缓存里核对，缓存缺就跳过。"""
    ns, path = split_ref(ref)
    if ns == NS:
        return (TEXTURES / f"{path}.png").is_file()
    vanilla = VANILLA / "textures" / f"{path}.png"
    return True if not VANILLA.is_dir() else vanilla.is_file()


def resolve_textures(ref: str, seen: set[str] | None = None) -> tuple[set[str], list[str]]:
    """顺着 parent 链收集贴图；返回（贴图引用集合，缺的本模组模型引用）。"""
    seen = seen or set()
    if ref in seen:
        return set(), []
    seen.add(ref)
    model = load_model(ref)
    if model is None:
        return set(), [ref]
    textures = {f"{split_ref(v)[0]}:{split_ref(v)[1]}"
                for v in (model.get("textures") or {}).values() if isinstance(v, str)}
    missing: list[str] = []
    parent = model.get("parent")
    if parent:
        parent_tex, parent_missing = resolve_textures(parent, seen)
        textures |= parent_tex
        missing += parent_missing
    return textures, missing


def check_item(item: dict, code: str, langs: dict[str, dict], guide: str) -> tuple[list[str], list[str]]:
    item_id = item["id"]
    problems: list[str] = []
    notes: list[str] = []

    if item_id not in code:
        problems.append(f"{item_id}: 代码里找不到这个 ID（common/src/main/java 全部文件）")

    model_ref = f"{NS}:item/{item_id}"
    model = load_model(model_ref)
    if model is None:
        problems.append(f"{item_id}: 缺模型 models/item/{item_id}.json")
    else:
        textures, missing = resolve_textures(model_ref)
        for ref in missing:
            problems.append(f"{item_id}: 模型链引用了不存在的模型 {ref}")
        for tex in sorted(textures):
            if not texture_exists(tex):
                ns, path = split_ref(tex)
                folder = "textures" if ns == NS else f"原版 textures/{path}"
                problems.append(f"{item_id}: 缺贴图 {tex}（{folder}）")
        for override in model.get("overrides") or []:
            ref = override.get("model")
            if ref and load_model(ref) is None:
                problems.append(f"{item_id}: overrides 引用了不存在的模型 {ref}")

    keys = (f"item.{NS}.{item_id}", f"block.{NS}.{item_id}")
    for code_name, data in langs.items():
        if not any(k in data for k in keys):
            problems.append(f"{item_id}: 语言 {code_name} 缺 {keys[0]}（或 {keys[1]}）")

    for recipe in item.get("recipes", []):
        if not (DATA_RECIPES / f"{recipe}.json").is_file():
            problems.append(f"{item_id}: 缺配方 recipe/{recipe}.json")

    if "hunger" in item:
        # mryt233 改（2026-11-08，r146a）：蚝油 / 芝麻酱改成 sauceBottle(SauceBottleKind.OYSTER_SAUCE, 饥饿, 饱和) 注册，
        # 数值检查要同时认 foodItem("id", 饥饿, 饱和) 和 sauceBottle(SauceBottleKind.<枚举>, 饥饿, 饱和) 两种写法。
        m = re.search(rf'foodItem\(\s*(?:registration\s*,\s*)?"{re.escape(item_id)}",\s*(\d+),\s*([0-9.]+)', code)
        if not m:
            # 枚举常量是 item_id 全大写（oyster_sauce → OYSTER_SAUCE，和 Seasoning 的写法一致）
            m = re.search(rf'sauceBottle\(\s*(?:registration\s*,\s*)?SauceBottleKind\.{re.escape(item_id.upper())},\s*(\d+),\s*([0-9.]+)', code)
        if not m:
            problems.append(f"{item_id}: 清单写了数值，但代码里没有 foodItem(\"{item_id}\", …) / "
                            f"sauceBottle(SauceBottleKind.{item_id.upper()}, …) 注册")
        else:
            have = (int(m.group(1)), float(m.group(2)))
            want = (int(item["hunger"]), float(item.get("saturation", m.group(2))))
            if have != want:
                problems.append(f"{item_id}: 数值和清单不一致（代码 {have[0]}/{have[1]:g}，"
                                f"清单 {want[0]}/{want[1]:g}）→ 跑 gen_food_numbers.py --apply")

    mentioned = f'"{item_id}"' in guide or f"'{item_id}'" in guide
    if item.get("guide") and not mentioned:
        problems.append(f"{item_id}: 清单标了要写进指南，但 guide_content.py 里没有")
    return problems, notes


# mryt233 改（2026-11-10）：不改 Java 注册顺序，只检查有序名单是否漂移。
def check_materials(code: str) -> list[str]:
    from pixkit.materials import ids
    # mryt233 改（2026-11-11）：全名清理后同时识别导入的 List 和原有全名写法。
    match = re.search(r'TABLE_WOODS\s*=\s*(?:java\.util\.)?List\.of\((.*?)\)', code, re.S)
    woods = re.findall(r'"([^"\n]+)"', match.group(1)) if match else []
    return [] if woods == ids("woods") else ["Java TABLE_WOODS 与 content/materials.json 的木头名单或顺序不一致"]


def main() -> int:
    strict = "--strict" in sys.argv
    manifest = json.loads(MANIFEST.read_text(encoding="utf-8"))
    items = manifest["items"]

    code = "\n".join(
        p.read_text(encoding="utf-8", errors="replace") for p in COMMON_JAVA.rglob("*.java")
    )
    guide = GUIDE.read_text(encoding="utf-8", errors="replace")
    lang_files = sorted(LANGS.glob("*.json"))
    langs = {p.stem: json.loads(p.read_text(encoding="utf-8")) for p in lang_files}

    problems: list[str] = check_materials(code)
    notes: list[str] = []
    for item in items:
        p, n = check_item(item, code, langs, guide)
        problems += p
        notes += n

    print(f"清单 {MANIFEST.name}：{len(items)} 条，语言文件 {len(lang_files)} 个")
    # 指南大多按显示名写（「肥牛卷」而不是 fatty_beef_roll），按 ID 查会大量误报，
    # 所以只给一条汇总，具体哪些没写进去还得人看。
    unseen = [i["id"] for i in items
              if i.get("food") and f'"{i["id"]}"' not in guide and f"'{i['id']}'" not in guide]
    if unseen:
        print(f"指南按 ID 没搜到的食物 {len(unseen)} 个（指南多用显示名，可能已在文字里覆盖）："
              f"{', '.join(unseen[:8])}{' …' if len(unseen) > 8 else ''}")
    if problems:
        print(f"发现 {len(problems)} 处缺失：")
        for line in problems:
            print(f"  - {line}")
    else:
        print("全部齐全，没有发现缺失。")
    if notes:
        print(f"提醒 {len(notes)} 条（不算失败）：")
        for line in notes:
            print(f"  · {line}")
    return 1 if (strict and problems) else 0


if __name__ == "__main__":
    raise SystemExit(main())
