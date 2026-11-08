# 作者：mryt233
"""把 26.1 底本的配方 JSON 转成 26.2 写法，可以重复跑（每次都从只读底本重新生成）。

1.21.2 起配料（Ingredient）不再写成对象：
  {"item": "a:b"}           → "a:b"
  {"tag": "c:d"}            → "#c:d"
  [{"item": ...}, ...]      → ["a:b", ...]（多选一）
其余字段（type / group / category / pattern / result / cookingtime / experience）照抄。
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
NS = 'orients_hotpot_chronicles'
BASE = ROOT.parents[1] / 'mod/common/src/main/resources/data' / NS / 'recipe'
OUT = ROOT / 'common/src/main/resources/data' / NS / 'recipe'


def ingredient(x):
    if isinstance(x, str):
        return x
    if isinstance(x, list):
        return [ingredient(i) for i in x]
    if isinstance(x, dict) and set(x) == {'item'}:
        return x['item']
    if isinstance(x, dict) and set(x) == {'tag'}:
        return '#' + x['tag']
    raise SystemExit(f'不认识的配料写法：{x}')


def convert(recipe):
    r = dict(recipe)
    if 'key' in r:
        r['key'] = {k: ingredient(v) for k, v in r['key'].items()}
    if 'ingredients' in r:
        r['ingredients'] = [ingredient(i) for i in r['ingredients']]
    if 'ingredient' in r:
        r['ingredient'] = ingredient(r['ingredient'])
    # 作者：mryt233。26.2只对缺省字段回退，修正原先被容错成misc的无效合成分类。
    if r.get('type') in {'minecraft:crafting_shaped', 'minecraft:crafting_shapeless'} and r.get('category') == 'food':
        r['category'] = 'misc'
    return r


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    n = 0
    for src in sorted(BASE.glob('*.json')):
        data = convert(json.loads(src.read_text(encoding='utf-8')))
        (OUT / src.name).write_bytes((json.dumps(data, indent=2, ensure_ascii=False) + '\n').encode('utf-8'))
        n += 1
    stale = sorted(p.name for p in OUT.glob('*.json') if not (BASE / p.name).exists())
    print(f'转了 {n} 条配方；底本里没有的多余文件：{stale}')


if __name__ == '__main__':
    main()
