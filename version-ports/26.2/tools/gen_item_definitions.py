# 作者：mryt233
"""生成 26.2 的物品模型定义 assets/<ns>/items/<id>.json（1.21.4 起每个物品都必须有）。

照 1.21.1 的 models/item/<id>.json 逐个翻译，可以重复跑：
- 普通模型                        → {"type": "minecraft:model", "model": "<ns>:item/<id>"}
- 父模型链里有 builtin/entity     → {"type": "orients_hotpot_chronicles:dynamic"}（原来 BEWLR 画的，现由 DynamicItemModel 画）
- overrides 按 custom_model_data   → minecraft:range_dispatch，property minecraft:custom_model_data（index 0）
- overrides 按 orients_hotpot_chronicles:soup → range_dispatch，property orients_hotpot_chronicles:soup
旧 overrides 的规则是「取最后一个满足 值 >= 门槛 的」，range_dispatch 是「取不超过值的最大门槛」，门槛递增时两者一致。
物品清单使用本工程 tools/item_ids.json 固定的 197 个注册物品，不依赖其他版本的测试目录。
"""
import json
import sys
from pathlib import Path

NS = 'orients_hotpot_chronicles'
ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'common/src/main/resources/assets' / NS
MODELS = ASSETS / 'models'
OUT = ASSETS / 'items'
ITEM_LIST = ROOT / 'tools/item_ids.json'


def load_model(ref):
    ns, _, path = ref.partition(':') if ':' in ref else ('minecraft', '', ref)
    if ns != NS:
        return None
    f = MODELS / (path + '.json')
    return json.loads(f.read_text(encoding='utf-8')) if f.exists() else None


def is_builtin_entity(ref, depth=0):
    if depth > 10:
        return False
    if ref.replace('minecraft:', '') == 'builtin/entity':
        return True
    m = load_model(ref)
    if not m or 'parent' not in m:
        return False
    return is_builtin_entity(m['parent'], depth + 1)


def collect_display(ref, depth=0):
    """顺着父模型链合并 display（子模型覆盖父模型），到 builtin/entity 为止。"""
    if depth > 10 or ref.replace('minecraft:', '') == 'builtin/entity':
        return {}
    m = load_model(ref)
    if not m:
        return {}
    merged = collect_display(m['parent'], depth + 1) if 'parent' in m else {}
    merged.update(m.get('display', {}))
    return merged


# mryt233 改（2026-11-05）：_display 模型要继承原模型的 gui_light。
# 模型不写 gui_light 时原版按 side（3D 方块侧光）打光，平面物品（夹着菜的筷子）在物品栏里
# 得不到正面直射光，整个格子看着发暗；这里是 26.2 / 26.1「筷子夹东西物品栏变暗」的修复。
GUI_LIGHT_FORCED = {
    'orients_hotpot_chronicles:item/chopsticks_with_food': 'front',
}


def collect_gui_light(ref, depth=0):
    """顺着父模型链找 gui_light（子模型优先）；对照表里的平面物品固定 front，找不到返回 None。"""
    forced = GUI_LIGHT_FORCED.get(ref)
    if forced:
        return forced
    if depth > 10 or ref.replace('minecraft:', '') == 'builtin/entity':
        return None
    model = load_model(ref)
    if not model:
        return None
    if model.get('gui_light'):
        return model['gui_light']
    return collect_gui_light(model['parent'], depth + 1) if 'parent' in model else None


# mryt233 改：只有 display 变换的模型没有 textures，粒子槽解析不到会报 Missing texture references in model ...: particle。
# 下面这几个动态物品画的是碗 / 盘子，原模型（builtin/entity）自己没写 textures，粒子贴图取同族方块模型里已有的。
PARTICLE_FALLBACKS = {
    'orients_hotpot_chronicles:item/bowl_with_food': 'orients_hotpot_chronicles:block/bowl_wood',
    'orients_hotpot_chronicles:item/dipping_sauce': 'orients_hotpot_chronicles:block/bowl_wood',
    'orients_hotpot_chronicles:item/plate_with_food': 'minecraft:block/quartz_block_side',
}


def collect_particle(ref, depth=0):
    """顺着父模型链找已有的 particle 贴图（子模型优先）。"""
    if depth > 10 or ref.replace('minecraft:', '') == 'builtin/entity':
        return None
    model = load_model(ref)
    if not model:
        return None
    particle = (model.get('textures') or {}).get('particle')
    if particle:
        return particle
    return collect_particle(model['parent'], depth + 1) if 'parent' in model else None


def texture_exists(ref):
    """本工程的贴图文件是否存在（minecraft 命名空间不查）。"""
    ns, _, path = ref.partition(':')
    if ns != NS:
        return True
    return (ROOT / 'common/src/main/resources/assets' / ns / 'textures' / (path + '.png')).exists()


def particle_texture(ref):
    """_display 模型的粒子贴图：原模型父链 → 对照表 → 该物品自己的物品贴图；都没有就报错，不引用不存在的贴图。"""
    item_id = ref.rsplit('/', 1)[-1]
    candidates = (collect_particle(ref), PARTICLE_FALLBACKS.get(ref), f'{NS}:item/{item_id}')
    for candidate in candidates:
        if candidate and texture_exists(candidate):
            return candidate
    raise SystemExit(f'{ref}: 找不到可用的 particle 贴图')


def write_display_model(ref):
    """写 models/<路径>_display.json（只有 display、不带父模型），返回它的模型 ID。"""
    ns, _, path = ref.partition(':')
    target = MODELS / (path + '_display.json')
    # mryt233 改：补上粒子贴图，不然客户端报 Missing texture references in model <ref>_display: particle。
    data = {'textures': {'particle': particle_texture(ref)}, 'display': collect_display(ref)}
    # mryt233 改（2026-11-05）：带上 gui_light，平面物品用正面平光（front），3D 方块模型（锅、碗、盘）照原模型用 side。
    gui_light = collect_gui_light(ref)
    if gui_light:
        data['gui_light'] = gui_light

    target.write_bytes((json.dumps(data, indent=2, ensure_ascii=False) + '\n').encode('utf-8'))
    return f'{ns}:{path}_display'


def model_ref(ref):
    if not ref.startswith(NS + ':') and ':' not in ref:
        ref = 'minecraft:' + ref
    if is_builtin_entity(ref):
        # mryt233 改：带上 base（只含 display 变换的模型），动态物品才按物品栏 / 手持 / 地上的角度摆放。
        # 26.x 已经没有 builtin/entity，不能直接拿原模型当 base（父模型找不到 → 紫黑），所以另写一份不带父模型的 *_display。
        return {'type': f'{NS}:dynamic', 'base': write_display_model(ref)}
    if ref == f'{NS}:item/iron_hot_pot_tab_icon':
        return split_tab_icon(ref)
    return {'type': 'minecraft:model', 'model': ref}


def split_tab_icon(ref):
    """mryt233 改：26.1 单个方块物品模型只能使用同一个纹理图集。"""
    source = load_model(ref)
    textures = source['textures']
    groups = {'blocks': [], 'items': []}

    def resolve(texture):
        seen = set()
        while texture.startswith('#'):
            key = texture[1:]
            if key in seen:
                raise ValueError(f'图标纹理循环引用：{key}')
            seen.add(key)
            texture = textures[key]
        return texture

    for element in source['elements']:
        faces = {'blocks': {}, 'items': {}}
        for side, face in element['faces'].items():
            texture = resolve(face['texture'])
            atlas = 'items' if texture.split(':', 1)[-1].startswith('item/') else 'blocks'
            faces[atlas][side] = face
        for atlas, subset in faces.items():
            if subset:
                groups[atlas].append({**element, 'faces': subset})
    models = []
    for atlas, elements in groups.items():
        if not elements:
            continue
        used = {face['texture'][1:] for e in elements for face in e['faces'].values() if face['texture'].startswith('#')}
        # mryt233 改：固定键顺序及粒子纹理，避免进程间集合遍历导致生成结果变化。
        actual = {key: resolve('#' + key) for key in sorted(used)}
        actual['particle'] = actual[min(actual)]
        model = {k: v for k, v in source.items() if k not in ('textures', 'elements', 'overrides')}
        model.update(textures=actual, elements=elements)
        path = 'item/iron_hot_pot_tab_icon_' + atlas
        (MODELS / (path + '.json')).write_text(json.dumps(model, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
        models.append({'type': 'minecraft:model', 'model': NS + ':' + path})
    return {'type': 'minecraft:composite', 'models': models}


def definition(item_id):
    ref = f'{NS}:item/{item_id}'
    model = load_model(ref)
    if model is None:
        raise SystemExit(f'缺物品模型：models/item/{item_id}.json')
    base = model_ref(ref)
    overrides = model.get('overrides', [])
    if not overrides:
        return {'model': base}
    if any(len(o['predicate']) != 1 for o in overrides):
        raise SystemExit(f'{item_id}: 有一条 override 同时带多个条件，要手工处理')
    # 按条件分组；旧规则里写在后面的条件优先（后面满足的会盖掉前面的），所以先出现的那组放最里层。
    # 例：铁锅先按汤底切换，最后一条 custom_model_data=1 是创造栏图标 → 外层判断图标，不是图标再按汤底。
    order = []
    for o in overrides:
        k = next(iter(o['predicate']))
        if k not in order:
            order.append(k)
    model = base
    for key in order:
        prop = {'custom_model_data': 'minecraft:custom_model_data', f'{NS}:soup': f'{NS}:soup'}.get(key)
        if prop is None:
            raise SystemExit(f'{item_id}: 不认识的 override 条件 {key}')
        group = sorted((o for o in overrides if key in o['predicate']), key=lambda o: o['predicate'][key])
        dispatch = {'type': 'minecraft:range_dispatch', 'property': prop,
                    'entries': [{'threshold': o['predicate'][key], 'model': model_ref(o['model'])} for o in group],
                    'fallback': model}
        if prop == 'minecraft:custom_model_data':
            dispatch['index'] = 0
        model = dispatch
    return {'model': model}


def main():
    items = json.loads(ITEM_LIST.read_text(encoding='utf-8'))['items']
    OUT.mkdir(parents=True, exist_ok=True)
    written = dynamic = dispatched = 0
    for item_id in sorted(items):
        d = definition(item_id)
        text = json.dumps(d, indent=2, ensure_ascii=False) + '\n'
        (OUT / f'{item_id}.json').write_bytes(text.encode('utf-8'))
        written += 1
        dumped = json.dumps(d)
        dynamic += f'{NS}:dynamic' in dumped
        dispatched += 'range_dispatch' in dumped
        # mryt233 改：动态物品回画空盘/空筷子时使用独立基础模型定义。
        source = load_model(f'{NS}:item/{item_id}')
        if source.get('overrides') and not is_builtin_entity(f'{NS}:item/{item_id}'):
            (OUT / f'{item_id}_base.json').write_text(json.dumps({'model': {
                'type':'minecraft:model','model':f'{NS}:item/{item_id}'}},indent=2)+'\n',encoding='utf-8')
    vanilla = ROOT / 'common/src/main/resources/assets/minecraft/items'
    vanilla.mkdir(parents=True, exist_ok=True)
    (vanilla/'bowl_base.json').write_text(json.dumps({'model':{'type':'minecraft:model',
        'model':'minecraft:item/bowl'}},indent=2)+'\n',encoding='utf-8')
    (vanilla/'bowl.json').write_text(json.dumps({'model':{'type':'minecraft:range_dispatch',
        'property':'minecraft:custom_model_data','index':0,
        'entries':[{'threshold':1,'model':{'type':f'{NS}:dynamic','base':write_display_model(f'{NS}:item/bowl_with_food')}}],
        'fallback':{'type':'minecraft:model','model':'minecraft:item/bowl'}}},indent=2)+'\n',encoding='utf-8')
    stale = [p.name for p in OUT.glob('*.json') if p.stem not in items and not (p.stem.endswith('_base') and p.stem[:-5] in items)]
    print(f'写了 {written} 个物品定义（动态 {dynamic}，按条件切换 {dispatched}）；多余的旧文件：{stale}')
    if stale and '--clean' in sys.argv:
        for name in stale:
            (OUT / name).unlink()


if __name__ == '__main__':
    main()
