"""装着汤底的锅物品模型：锅身 + 对应汤面，给掉在地上 / 物品栏里的锅显示汤（r94）。

生成 models/item/<铁|铜>_hot_pot_<汤底>.json，并在 <铁|铜>_hot_pot.json 里写好 overrides：
谓词 orients_hotpot_chronicles:soup = (汤底编号 + 1) / 10（客户端 HotPotClient 注册），
自定义模型数据 1（分类页图标）放在最后、优先级最高。两份资源（assets 和内置资源包）一起写。

"""
import json
import os

ROOT = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources')
COPIES = [os.path.join(ROOT, 'assets', 'orients_hotpot_chronicles'),
          os.path.join(ROOT, 'resourcepacks', 'hotpot_assets', 'assets', 'orients_hotpot_chronicles')]
NS = 'orients_hotpot_chronicles'
SOUPS = ['clear', 'milky', 'mushroom', 'spicy']   # 顺序 = SoupBase 编号
METALS = ['iron', 'copper']


def load(base, rel):
    with open(os.path.join(base, rel), encoding='utf-8') as f:
        return json.load(f)


def save(base, rel, data):
    with open(os.path.join(base, rel), 'w', encoding='utf-8', newline='\n') as f:
        json.dump(data, f, indent=2, ensure_ascii=False)


for base in COPIES:
    for metal in METALS:
        item_rel = f'models/item/{metal}_hot_pot.json'
        item = load(base, item_rel)
        shell = load(base, f'models/block/{metal}_hot_pot_shell.json')
        overrides = []
        for i, soup in enumerate(SOUPS):
            surface = load(base, f'models/block/standalone_soup_{soup}.json')
            textures = dict(shell['textures'])
            textures['soup'] = surface['textures']['soup']
            model = {
                'credit': "Orient's Hotpot Chronicles",
                'texture_size': shell.get('texture_size', [16, 16]),
                'textures': textures,
                'elements': shell['elements'] + surface['elements'],
                'display': item['display'],
            }
            name = f'{metal}_hot_pot_{soup}'
            save(base, f'models/item/{name}.json', model)
            overrides.append({'predicate': {f'{NS}:soup': round((i + 1) / 10, 2)},
                              'model': f'{NS}:item/{name}'})
        if metal == 'iron':
            overrides.append({'predicate': {'custom_model_data': 1},
                              'model': f'{NS}:item/iron_hot_pot_tab_icon'})
        item['overrides'] = overrides
        save(base, item_rel, item)
print('ok')
