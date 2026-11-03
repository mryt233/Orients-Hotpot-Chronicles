# 作者：mryt233
"""把长凳（*_bench）的斜腿写进方块模型（26.x 用），可以重复跑：每次从只读底本重新生成。

1.21.1 的方块模型只能转 22.5° 的倍数，斜腿（约 8.1°）只好由方块实体渲染器 BenchLegsRenderer 现画；
26.x 的方块模型元素支持任意角度（原版 CuboidRotation），于是把腿直接放进模型，交给原版区块渲染：
开光影也正常（玩家反馈 26.2 开光影凳腿发白发紫），还省掉每帧的方块实体绘制。

几何和 BenchLegsRenderer 完全一样（模型按朝北写，朝向由方块状态的 y 旋转负责）：
- 每头两条腿，上端在 y=14、z=6.5 / 9.5，绕 x 轴往外岔开（atan(2/14)），长 hypot(2,14)，截面 2 × 1.5 像素；
- 单独一张：两头的腿在 x=1.5 / 12.5；连成长凳时两头往里收到 4.5 / 9.5，中间那段没有腿；
- 每个面的贴图取木板贴图左上角、和面一样大的一块（同原来的 quad()）。
同时把物品模型 models/item/<木头>_bench.json 的父模型从 builtin/entity 改成方块模型（腿已经在模型里，不用再现画）。
"""
import json
import math
from pathlib import Path

NS = 'orients_hotpot_chronicles'
ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'common/src/main/resources/assets' / NS / 'models'
BASE = ROOT.parent / 'base/0.2.1-alpha/common/src/main/resources/assets' / NS / 'models'

LEG_TOP, LEG_SPREAD, LEG_W, LEG_T = 14.0, 2.0, 2.0, 1.5
ANGLE = math.degrees(math.atan2(LEG_SPREAD, LEG_TOP))
LENGTH = math.hypot(LEG_SPREAD, LEG_TOP)
# 变体 → 哪几头有腿（和 BenchLegsRenderer：!left → 一头，!right → 另一头；单独一张时往外放）
ENDS = {'': [1.5, 12.5], '_right': [4.5], '_left': [9.5], '_middle': []}


def r(v):
    return round(v, 4)


def leg(end, zc, sign):
    uv = lambda w, h: [0, 0, r(min(16.0, w)), r(min(16.0, h))]
    side = {'uv': uv(LEG_T, LENGTH), 'texture': '#planks'}
    front = {'uv': uv(LEG_W, LENGTH), 'texture': '#planks'}
    cap = {'uv': uv(LEG_W, LEG_T), 'texture': '#planks'}
    return {
        'name': 'bench_leg',
        'from': [r(end), r(LEG_TOP - LENGTH), r(zc - LEG_T / 2)],
        'to': [r(end + LEG_W), LEG_TOP, r(zc + LEG_T / 2)],
        'rotation': {'origin': [r(end), LEG_TOP, zc], 'axis': 'x', 'angle': r(sign * ANGLE)},
        'faces': {'north': dict(front), 'south': dict(front), 'west': dict(side), 'east': dict(side),
                  'up': dict(cap), 'down': dict(cap)},
    }


def main():
    benches = sorted(p.stem for p in (BASE / 'block').glob('*_bench.json'))
    written = 0
    for bench in benches:
        for suffix, ends in ENDS.items():
            src = BASE / 'block' / f'{bench}{suffix}.json'
            model = json.loads(src.read_text(encoding='utf-8'))
            for end in ends:
                for zc, sign in ((6.5, 1), (9.5, -1)):
                    model['elements'].append(leg(end, zc, sign))
            (OUT / 'block' / src.name).write_bytes((json.dumps(model, indent=2, ensure_ascii=False) + '\n').encode('utf-8'))
            written += 1
        item_src = BASE / 'item' / f'{bench}.json'
        item = json.loads(item_src.read_text(encoding='utf-8'))
        item['parent'] = f'{NS}:block/{bench}'
        (OUT / 'item' / item_src.name).write_bytes((json.dumps(item, indent=2, ensure_ascii=False) + '\n').encode('utf-8'))
    print(f'长凳 {len(benches)} 种，写了 {written} 个方块模型（每条腿 {ANGLE:.3f}°、长 {LENGTH:.3f}）和 {len(benches)} 个物品模型')


if __name__ == '__main__':
    main()
