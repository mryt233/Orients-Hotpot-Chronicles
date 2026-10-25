# 作者：  收集教程要画图标的物品 ID
import json,glob,re,os
M='orients_hotpot_chronicles'
base='src/main/resources'
lang=json.load(open(f'{base}/assets/{M}/lang/zh_cn.json',encoding='utf-8'))
ids=set()
for k in lang:
    m=re.match(r'(item|block)\.'+M+r'\.(.+)',k)
    if m and m.group(2) not in('plates','sauce_station_top','tall_brick_stove_top','bowl','drink'): ids.add(f'{M}:{m.group(2)}')
def walk(o):
    if isinstance(o,dict):
        for k,v in o.items():
            if k in('item','id') and isinstance(v,str): ids.add(v if ':' in v else 'minecraft:'+v)
            else: walk(v)
    elif isinstance(o,list):
        for v in o: walk(v)
for f in glob.glob(f'{base}/data/{M}/recipe/*.json'): walk(json.load(open(f,encoding='utf-8')))
for v in 'bowl bucket water_bucket milk_bucket lava_bucket mushroom_stew beef mutton porkchop cod salmon blaze_powder sweet_berries honey_bottle sunflower pumpkin_seeds beetroot apple sugar wheat potato stick coal charcoal oak_planks furnace cobblestone brick bricks campfire soul_campfire cow_spawn_egg mooshroom_spawn_egg short_grass tall_grass shears red_mushroom brown_mushroom'.split(): ids.add('minecraft:'+v)
ids={i for i in ids if not i.startswith('#')}
json.dump(sorted(ids),open('tutorial/_icon_ids.json','w'))
print(len(ids))
