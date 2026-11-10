"""新物品脚手架：写一份简短描述，一条命令把注册代码、10 种语言、模型、占位贴图、配方、砧板切配、
游戏内指南、教程图标、查看器分类一起搭好，不会漏。作者：mryt233（2026-11-01）

    python tools/new_item.py --example > duck_blood.json   # 打印一份示例描述
    python tools/new_item.py duck_blood.json               # 预演：列出会改哪些文件、各改什么（不落盘）
    python tools/new_item.py duck_blood.json --apply       # 真正写入，然后跑 gen_lang / gen_guide_book / 清单 / 自检
    python tools/new_item.py duck_blood.json --apply --compile   # 另外编译三个模块

描述文件字段（只有 id 和 names 必填）：
  id          物品 ID（英文名，见「ID 跟英文名走」规则）
  names       {语言: 名字}。zh_cn、en_us 必填；ja_jp ko_kr ru_ru de_de fr_fr es_es pt_br 要写全（翻译规则见记忆）；
              zh_tw 不写就用 opencc 从简体转
  hunger / saturation   写了就注册成食物（foodItem，数值自动进配置文件）；不写就是普通物品
  comment     注册代码上方的说明注释（写清用户哪天要的、怎么得到）
  texture     占位贴图：{"like": 原版贴图名, "color": "#RRGGBB"}，把原版那张按亮度换成这个颜色的色阶。
              不写 like 就按分类挑（生肉→beef 等）。已有贴图不会覆盖（--force-texture 才覆盖）
  cooked      熟的版本：{"id", "names", "hunger", "saturation", "cook_seconds", "texture"}。
              自动生成熔炉 / 烟熏炉（减半）/ 营火（3 倍）三条配方；锅里能不能煮就是看这三条配方，所以也能下锅
  cut         砧板切出来：{"from": "minecraft:beef" 或本模组 ID, "count": 3}。
              生成切石机配方、加进 cuttable 标签、写进砧板内置切配表（数据包被删时兜底）
  guide       指南条目：{"section": "ingredients", "zh": {...}, "en": {...}}，条目字段见 guide_content.py 开头。
              不写就生成一条最简单的（图标 + 回几点饥饿），预演时会提醒你改
  flavor      物品栏悬停的灰色趣味小字 {语言: 文字}，要写就 10 种写全（zh_tw 可省）
  viewer      查看器分类 {"category": "ingredients", "keywords": ["鸭血"]}，默认按食物放 ingredients

脚手架不做的（会在最后列出来提醒）：正式贴图（用 pixkit + vanilla_ref + compare_board 画好给用户选）、
网页教程正文、移植同步清单、DEV_LOG。
"""
import json
import re
import subprocess
import sys
from collections import OrderedDict
from pathlib import Path

# mryt233 改（2026-11-10）：版本与 JDK 读取唯一配置源。
from project_env import jdk21

sys.path.insert(0, str(Path(__file__).resolve().parent))
from PIL import Image  # noqa: E402

import vanilla_ref  # noqa: E402
from pixkit import canvas, color, io, model as pm  # noqa: E402
from pixkit.paths import ASSETS, DATA, MOD, MODID, VANILLA  # noqa: E402

JAVA = MOD / "common/src/main/java/cn/blockforge/generated/orientshotpotchronicles/hotpot"
CONTENT = JAVA / "HotPotContent.java"
CUTTING = JAVA / "CuttingBoardBlock.java"
MARK_REG = "// ==== tools/new_item.py：新物品的注册插在这行上面"
MARK_TAB = "// ==== tools/new_item.py：新物品进创造栏插在这行上面"
MARK_CUT = "// ==== tools/new_item.py：新的砧板切配插在这行上面"
LANG_ITEMS = MOD / "tools/lang_items.json"
GUIDE_ITEMS = MOD / "tools/guide_items.json"
GUIDE_SRC = MOD / "tools/guide_content.py"
VIEWER_CATS = MOD / "viewer/categories.json"
ICONS = MOD / "tutorial/img/icons"
UTF8 = dict(__import__("os").environ, PYTHONIOENCODING="utf-8")   # 子脚本输出中文不乱码
OTHER_LANGS = ["zh_tw", "ja_jp", "ko_kr", "ru_ru", "de_de", "fr_fr", "es_es", "pt_br"]
DEFAULT_LIKE = {"raw_meat": "beef", "cooked_meat": "cooked_beef", "raw_fish": "cod", "cooked_fish": "cooked_cod",
                "vegetable": "carrot", "fruit": "apple", "bread_sweet": "bread", "seasoning": "sugar",
                "bottle": "honey_bottle", "tool": "iron_sword"}

EXAMPLE = {
    "id": "duck_blood",
    "names": {"zh_cn": "鸭血", "en_us": "Duck Blood", "ja_jp": "鴨の血豆腐", "ko_kr": "오리 선지",
              "ru_ru": "Утиная кровь", "de_de": "Entenblut", "fr_fr": "Sang de canard", "es_es": "Sangre de pato",
              "pt_br": "Sangue de pato"},
    "hunger": 1, "saturation": 0.2,
    "comment": "鸭血（示例）：砧板切出来，下锅煮熟。",
    "texture": {"like": "beef", "color": "#7A1E1E"},
    "cooked": {"id": "cooked_duck_blood",
               "names": {"zh_cn": "熟鸭血", "en_us": "Cooked Duck Blood", "ja_jp": "煮た鴨の血豆腐", "ko_kr": "익힌 오리 선지",
                         "ru_ru": "Варёная утиная кровь", "de_de": "Gekochtes Entenblut", "fr_fr": "Sang de canard cuit",
                         "es_es": "Sangre de pato cocida", "pt_br": "Sangue de pato cozido"},
               "hunger": 3, "saturation": 0.5, "cook_seconds": 5, "texture": {"color": "#5A2620"}},
    "cut": {"from": "minecraft:chicken", "count": 3},
    "guide": {"section": "ingredients",
              "zh": {"icon": "duck_blood", "text": "示例条目。", "rows": [["duck_blood", "鸭血：回 1"]]},
              "en": {"icon": "duck_blood", "text": "Example entry.", "rows": [["duck_blood", "Duck blood: 1 hunger"]]}},
}


# ------------------------------------------------------------------ 小工具
def const(item_id):
    return item_id.upper()


def item_ref(ref):
    """minecraft:beef → Items.BEEF；本模组 ID → HotPotContent.X。"""
    ns, _, path = ref.partition(":") if ":" in ref else (MODID, "", ref)
    return f"Items.{path.upper()}" if ns == "minecraft" else f"HotPotContent.{path.upper()}"


def full(ref):
    return ref if ":" in ref else f"{MODID}:{ref}"


def zh_tw(text):
    try:
        import opencc
        return opencc.OpenCC("s2twp").convert(text)
    except Exception:
        return None


def read(p):
    return Path(p).read_text(encoding="utf-8")


def write_text(p, text):
    Path(p).parent.mkdir(parents=True, exist_ok=True)
    Path(p).write_bytes(text.encode("utf-8"))


def load_json(p, default):
    return json.loads(read(p), object_pairs_hook=OrderedDict) if Path(p).exists() else default


def dump_json(p, obj):
    write_text(p, json.dumps(obj, ensure_ascii=False, indent=2) + "\n")


def insert_before(text, marker, block):
    i = text.index(marker)
    line_start = text.rfind("\n", 0, i) + 1
    return text[:line_start] + block + text[line_start:]


# ------------------------------------------------------------------ 计划：每一步是（文件, 说明, 动作）
class Plan:
    def __init__(self):
        self.steps, self.todo, self.errors = [], [], []
        self.java = {CONTENT: read(CONTENT), CUTTING: read(CUTTING)}
        self.lang = {"zh_cn": load_json(ASSETS / "lang/zh_cn.json", OrderedDict()),
                     "en_us": load_json(ASSETS / "lang/en_us.json", OrderedDict())}
        self.lang_items = load_json(LANG_ITEMS, OrderedDict())
        self.guide_items = load_json(GUIDE_ITEMS, OrderedDict([("zh", OrderedDict()), ("en", OrderedDict())]))
        self.files = {}          # 路径 → 新内容（bytes 或 PIL 图）
        self.cuttable = None

    def step(self, path, desc):
        self.steps.append((Path(path), desc))


def names_ok(plan, spec, label):
    n = spec.get("names") or {}
    for code in ("zh_cn", "en_us"):
        if not n.get(code):
            plan.errors.append(f"{label}：names 缺 {code}")
    out = dict(n)
    if not out.get("zh_tw") and n.get("zh_cn"):
        tw = zh_tw(n["zh_cn"])
        if tw:
            out["zh_tw"] = tw
    for code in OTHER_LANGS:
        if not out.get(code):
            plan.errors.append(f"{label}：names 缺 {code} 译文（10 种语言要写全）")
    return out


def add_lang(plan, key, texts, what):
    for code in ("zh_cn", "en_us"):
        if texts.get(code) and key not in plan.lang[code]:
            plan.lang[code][key] = texts[code]
    for code in OTHER_LANGS:
        if texts.get(code):
            plan.lang_items.setdefault(code, OrderedDict())[key] = texts[code]
    plan.step(ASSETS / "lang", f"{what}：zh_cn / en_us 加键 {key}，其余 8 种写进 tools/lang_items.json")


def placeholder(spec, item_id, fallback_like=None, base=None):
    tex = spec.get("texture") or {}
    cats = vanilla_ref.category_of(item_id)
    like = tex.get("like") or fallback_like or next((DEFAULT_LIKE[c] for c in cats if c in DEFAULT_LIKE), "beef")
    src = VANILLA / f"textures/item/{like}.png"
    if not src.exists():
        src = VANILLA / "textures/item/beef.png"
        like = "beef"
    c = color.hexc(tex["color"])[:3] if tex.get("color") else (base or (150, 60, 50))
    im = canvas.recolor_by_luma(Image.open(src), color.ramp(c, 6, dark=0.35, light=1.8))
    return im, like, c


def plan_item(plan, spec, label, cooked_from=None, base_like=None, base_color=None):
    iid = spec["id"]
    if not re.fullmatch(r"[a-z0-9_]+", iid or ""):
        plan.errors.append(f"{label}：id「{iid}」只能是小写字母、数字、下划线")
        return None
    src = plan.java[CONTENT]
    names = names_ok(plan, spec, label)
    exists = f'HotPot.id("{iid}")' in src or f'foodItem("{iid}"' in src or f'foodItem(registration, "{iid}"' in src
    if exists:
        plan.todo.append(f"{iid} 已经注册过，跳过注册代码（只补缺的文件）")
    elif re.search(rf"\b{const(iid)}\b", src):
        plan.errors.append(f"{label}：常量名 {const(iid)} 已被占用")
    else:
        doc = spec.get("comment") or (f"{names.get('zh_cn', iid)}：{cooked_from} 煮 / 烤熟（tools/new_item.py 生成）"
                                      if cooked_from else f"{names.get('zh_cn', iid)}（tools/new_item.py 生成，记得补说明）")
        # mryt233 改（2026-11-10）：字段只声明，登记赋值写进 register(registration)。
        line = f'    /** {doc} */\n    public static Item {const(iid)};\n'
        if "hunger" in spec:
            assignment = (f'        {const(iid)} = foodItem(registration, "{iid}", '
                          f'{int(spec["hunger"])}, {float(spec.get("saturation", 0.3))}F);\n')
        else:
            assignment = (f'        {const(iid)} = registration.items().register(HotPot.id("{iid}"),\n'
                          f'            new Item(new Item.Properties()));\n')
        plan.java[CONTENT] = insert_before(plan.java[CONTENT], MARK_REG, line)
        plan.java[CONTENT] = insert_before(plan.java[CONTENT],
            "        // ==== tools/new_item.py：新物品的登记赋值插在这行上面", assignment)
        plan.java[CONTENT] = insert_before(plan.java[CONTENT], MARK_TAB, f"                entries.accept({const(iid)});\n")
        plan.step(CONTENT, f"注册 {const(iid)}" + (f"（食物：回 {spec['hunger']}，饱和 {spec.get('saturation', 0.3)}）"
                                                     if "hunger" in spec else "（普通物品）") + "，并加进创造栏")
    # 语言
    add_lang(plan, f"item.{MODID}.{iid}", names, f"{iid} 名字")
    if spec.get("flavor"):
        fl = dict(spec["flavor"])
        if not fl.get("zh_tw") and fl.get("zh_cn"):
            fl["zh_tw"] = zh_tw(fl["zh_cn"])
        missing = [c for c in ["zh_cn", "en_us"] + OTHER_LANGS if not fl.get(c)]
        if missing:
            plan.errors.append(f"{label}：flavor 缺 {', '.join(missing)}")
        add_lang(plan, f"tooltip.{MODID}.flavor.{iid}", fl, f"{iid} 趣味小字")
    # 贴图 + 模型 + 教程图标
    tex_p = ASSETS / f"textures/item/{iid}.png"
    if not tex_p.exists() or "--force-texture" in sys.argv:
        im, like, c = placeholder(spec, iid, fallback_like=base_like, base=base_color)
        plan.files[tex_p] = im
        plan.step(tex_p, f"占位贴图：原版 {like} 按亮度换成 #{c[0]:02X}{c[1]:02X}{c[2]:02X} 的色阶（正式贴图另画）")
        plan.todo.append(f"给 {iid} 画正式贴图：vanilla_ref.py find --like {like} → pixkit 画几套 → compare_board.py 给用户选")
    else:
        im, like, c = Image.open(tex_p).convert("RGBA"), base_like, base_color
    model_p = ASSETS / f"models/item/{iid}.json"
    if not model_p.exists():
        plan.files[model_p] = pm.item_generated(iid)
        plan.step(model_p, "物品模型（item/generated）")
    icon_p = ICONS / f"{MODID}__{iid}.png"
    plan.files[icon_p] = im.resize((32, 32), Image.NEAREST)
    plan.step(icon_p, "教程图标（贴图放大 2 倍，和查看器导出的一样）")
    return {"id": iid, "names": names, "like": like, "color": c}


def plan_cooked(plan, raw, cooked):
    secs = float(cooked.get("cook_seconds", 5))
    ticks = int(round(secs * 20))
    rec = DATA / "recipe"
    # mryt233 改（2026-11-11）：锅的专用煮法写成配方，不再生成 Java 登记。
    pot = rec / f"pot_boiling/{raw['id']}.json"
    plan.files[pot] = {"_说明": "作者：mryt233", "type": full("pot_boiling"),
                       "ingredient": {"item": full(raw["id"])}, "result": {"id": full(cooked["id"])},
                       "cook_ticks": max(1, ticks // 2)}
    plan.step(pot, "锅专用煮法（自家涮煮时间配置优先）")
    for kind, t, suffix in (("smelting", ticks, ""), ("smoking", max(1, ticks // 2), "_from_smoking"),
                            ("campfire_cooking", ticks * 3, "_from_campfire_cooking")):
        p = rec / f"{cooked['id']}{suffix}.json"
        plan.files[p] = {"type": f"minecraft:{kind}", "category": "food", "ingredient": {"item": full(raw["id"])},
                         "result": {"id": full(cooked["id"])}, "experience": 0.35, "cookingtime": t}
        plan.step(p, f"{kind} 配方 {t} 刻（锅里煮也按这些配方）")


def plan_cut(plan, iid, cut):
    src_item, n = cut["from"], int(cut.get("count", 1))
    out = f"{iid}_from_stonecutting"
    p = DATA / f"recipe/{out}.json"
    plan.files[p] = {"type": "minecraft:stonecutting", "ingredient": {"item": full(src_item)},
                     "result": {"id": full(iid), "count": n}}
    plan.step(p, f"切石机配方：{full(src_item)} → {n} 个 {iid}（砧板优先读这个）")
    tag_p = DATA / "tags/item/cuttable.json"
    tag = load_json(tag_p, OrderedDict([("replace", False), ("values", [])]))
    if full(src_item) not in tag["values"]:
        tag["values"].append(full(src_item))
        plan.files[tag_p] = tag
        plan.step(tag_p, f"cuttable 标签加 {full(src_item)}")
    text = plan.java[CUTTING]
    ref = item_ref(src_item)
    if re.search(rf"Map\.entry\({re.escape(ref)},", text):
        plan.errors.append(f"砧板内置表里 {ref} 已经有产物了，一样东西只能切出一种")
        return
    i = text.index(MARK_CUT)
    line_start = text.rfind("\n", 0, i) + 1
    prev_end = text.rfind(")", 0, line_start)                   # 上一条 Map.entry(...) 的结尾
    text = text[:prev_end + 1] + "," + text[prev_end + 1:line_start] + \
        f"            java.util.Map.entry({ref}, new ItemStack(HotPotContent.{const(iid)}, {n}))\n" + text[line_start:]
    plan.java[CUTTING] = text
    plan.step(CUTTING, f"砧板内置切配表加 {ref} → {n} 个 {const(iid)}（数据包配方被删时兜底）")


def plan_guide(plan, spec, raw, cooked):
    g = spec.get("guide")
    zh_name, en_name = raw["names"]["zh_cn"], raw["names"]["en_us"]
    if not g:
        rows_zh = [[raw["id"], f"{zh_name}：回 {spec.get('hunger', 0)}"]]
        rows_en = [[raw["id"], f"{en_name}: {spec.get('hunger', 0)} hunger"]]
        if cooked:
            rows_zh.append([cooked["id"], f"{cooked['names']['zh_cn']}：回 {spec['cooked'].get('hunger', 0)}"])
            rows_en.append([cooked["id"], f"{cooked['names']['en_us']}: {spec['cooked'].get('hunger', 0)} hunger"])
        g = {"section": "ingredients", "zh": {"icon": raw["id"], "rows": rows_zh}, "en": {"icon": raw["id"], "rows": rows_en}}
        plan.todo.append(f"指南里 {raw['id']} 用的是自动生成的最简条目，补上 get / text（tools/guide_items.json）")
    src = read(GUIDE_SRC)
    if re.search(rf'"icon":\s*"{raw["id"]}"', src) or any(
            e.get("icon") == raw["id"] for e in plan.guide_items.get("zh", {}).get(g["section"], [])):
        plan.todo.append(f"指南里已经有 {raw['id']} 的条目，没再加")
        return
    for lang in ("zh", "en"):
        plan.guide_items.setdefault(lang, OrderedDict()).setdefault(g["section"], []).append(g[lang])
    plan.step(GUIDE_ITEMS, f"指南「{g['section']}」分类加一条 {raw['id']}（gen_guide_book.py 会写进书里）")


def plan_viewer(plan, spec, ids, zh_names):
    v = spec.get("viewer") or {}
    cat_id = v.get("category", "ingredients")
    data = load_json(VIEWER_CATS, None)
    if data is None:
        return
    cat = next((c for c in data["categories"] if c["id"] == cat_id), None)
    if not cat:
        plan.errors.append(f"查看器没有分类 {cat_id}")
        return
    kws = cat.setdefault("keywords", [])
    added = []
    for word in list(ids) + list(zh_names) + list(v.get("keywords", [])):
        if not any(k in word for k in kws if k) and word not in kws:
            kws.append(word)
            added.append(word)
    if added:
        plan.files[VIEWER_CATS] = data
        plan.step(VIEWER_CATS, f"查看器「{cat.get('name', cat_id)}」分类加关键词 {added}")


def build_plan(spec):
    plan = Plan()
    raw = plan_item(plan, spec, "物品")
    cooked = None
    if raw and spec.get("cooked"):
        c = spec["cooked"]
        cooked_like = "cooked_" + raw["like"] if (VANILLA / f"textures/item/cooked_{raw['like']}.png").exists() else raw["like"]
        cooked = plan_item(plan, c, "熟的版本", cooked_from=raw["names"]["zh_cn"], base_like=(c.get("texture") or {}).get("like") or cooked_like,
                           base_color=tuple(color.shade(raw["color"], 0.72)))
        if cooked:
            plan_cooked(plan, raw, c)
    if raw and spec.get("cut"):
        plan_cut(plan, raw["id"], spec["cut"])
    if raw:
        plan_guide(plan, spec, raw, cooked)
        ids = [raw["id"]] + ([cooked["id"]] if cooked else [])
        plan_viewer(plan, spec, ids, [raw["names"]["zh_cn"]])
    # 汇总 Java / 语言改动
    for p, text in plan.java.items():
        if text != read(p):
            plan.files[p] = text
    for code, data in plan.lang.items():
        plan.files[ASSETS / f"lang/{code}.json"] = data
    if plan.lang_items:
        plan.files[LANG_ITEMS] = plan.lang_items
    if any(plan.guide_items.get(k) for k in ("zh", "en")):
        plan.files[GUIDE_ITEMS] = plan.guide_items
    plan.todo += ["网页教程正文（tutorial/content/*.md）和三语同步——归教程流程",
                  "docs/移植同步清单.md「未发布」加一条；DEV_LOG 记一条（mryt233）",
                  "数值确认：hunger / saturation / 烹饪时间写进了代码默认值，玩家可在 config 里改"]
    return plan


def apply(plan):
    for p, content in plan.files.items():
        p = Path(p)
        if isinstance(content, Image.Image):
            io.save_png(content, p)
        elif isinstance(content, str):
            write_text(p, content)
        elif "lang" in p.parts or p in (VIEWER_CATS, LANG_ITEMS, GUIDE_ITEMS):
            dump_json(p, content)           # 这些文件本来就写中文原文（不转义）
        else:
            io.write_json(p, content)       # 配方、模型、标签：和生成器写法一致
    tools = MOD / "tools"
    for script, args in (("gen_lang.py", []), ("gen_guide_book.py", []), ("gen_content_manifest.py", [])):
        r = subprocess.run([sys.executable, str(tools / script), *args], cwd=MOD, capture_output=True, text=True,
                           encoding="utf-8", errors="replace", env=UTF8)
        tail = (r.stdout + r.stderr).strip().splitlines()[-3:]
        print(f"  {script}: {'通过' if r.returncode == 0 else '失败'}  {' / '.join(tail)}")
    if "--compile" in sys.argv:
        import os
        env = dict(os.environ, JAVA_HOME=str(jdk21()))
        gw = str(MOD / ("gradlew.bat" if os.name == "nt" else "gradlew"))
        r = subprocess.run([gw, ":common:compileJava", ":fabric:compileJava", ":neoforge:compileJava", "-q"], cwd=MOD,
                           env=env, capture_output=True, text=True, encoding="utf-8", errors="replace")
        print(f"  编译: {'通过' if r.returncode == 0 else '失败'}")
        if r.returncode:
            print("\n".join((r.stdout + r.stderr).splitlines()[-15:]))
    r = subprocess.run([sys.executable, str(tools / "check_all.py"), "--source-only"], cwd=MOD, capture_output=True,
                       text=True, encoding="utf-8", errors="replace", env=UTF8)
    print("  check_all：")
    for ln in r.stdout.splitlines():
        if ln.startswith("[") or "失败" in ln or "缺" in ln or "新警告" in ln:
            print("    " + ln)


def main(argv):
    if not argv or argv[0] in ("-h", "--help"):
        print(__doc__)
        return 0
    if argv[0] == "--example":
        print(json.dumps(EXAMPLE, ensure_ascii=False, indent=2))
        return 0
    spec = json.loads(Path(argv[0]).read_text(encoding="utf-8"))
    plan = build_plan(spec)
    print(f"新物品 {spec.get('id')}：{'写入' if '--apply' in argv else '预演（不改文件，加 --apply 才写）'}")
    for p, desc in plan.steps:
        try:
            rel = p.relative_to(MOD)
        except ValueError:
            rel = p
        print(f"  · {str(rel).replace(chr(92), '/')}：{desc}")
    if plan.errors:
        print("\n有问题，先改描述文件：")
        for e in plan.errors:
            print("  ✗ " + e)
        return 1
    if "--apply" in argv:
        apply(plan)
    print("\n还要手动做的：")
    for t in plan.todo:
        print("  - " + t)
    return 0


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    raise SystemExit(main(sys.argv[1:]))
