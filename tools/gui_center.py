"""物品栏图标在格子里居不居中（2026-10-26，用户：图标都要在物品栏正中）。

    python tools/gui_center.py 物品ID 物品ID ...      # 例如 oak_sauce_station cutting_board orient_cola
按游戏画物品栏图标的方式（物品模型 display.gui 的 缩放 → XYZ 旋转 → 平移）把模型所有长方块投到格子上，
打印左右 / 上下范围和中心；中心离 0 超过 0.5 像素就该改 gui 的 translation（往上 = y 加）。砖灶台算出来正好居中，和查看器一致。
只认本模组的模型，不算元素自带的 rotation。
作者：mryt233（2026-11-01 mryt233 改：路径跟上多模块工程；可被 check_textures.py 导入）
"""
import json, math, sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parent))
from pixkit.paths import ASSETS
ROOT = ASSETS / "models"
def load(ref):
    ns,p=ref.split(":") if ":" in ref else ("minecraft",ref)
    if ns!="orients_hotpot_chronicles": return {}
    return json.load(open(ROOT/(p+".json")))
def elements(ref):
    m=load(ref)
    if "elements" in m: return m["elements"]
    return elements(m["parent"]) if "parent" in m else []
def rot(v,deg):
    x,y,z=v; a,b,c=[math.radians(d) for d in deg]
    # rotationXYZ: v' = Rx * Ry * Rz * v
    x,y = x*math.cos(c)-y*math.sin(c), x*math.sin(c)+y*math.cos(c)
    x,z = x*math.cos(b)+z*math.sin(b), -x*math.sin(b)+z*math.cos(b)
    y,z = y*math.cos(a)-z*math.sin(a), y*math.sin(a)+z*math.cos(a)
    return x,y,z
def bbox(item):
    m=load("orients_hotpot_chronicles:item/"+item); gui=m.get("display",{}).get("gui") or load(m.get("parent","")).get("display",{}).get("gui",{})
    els=elements(m["parent"]); s=gui.get("scale",[1,1,1]); r=gui.get("rotation",[0,0,0]); t=gui.get("translation",[0,0,0])
    xs=[];ys=[]
    for e in els:
        for X in (e["from"][0],e["to"][0]):
            for Y in (e["from"][1],e["to"][1]):
                for Z in (e["from"][2],e["to"][2]):
                    v=((X-8)*s[0],(Y-8)*s[1],(Z-8)*s[2]); v=rot(v,r)
                    xs.append(v[0]+t[0]); ys.append(v[1]+t[1])
    return min(xs),max(xs),min(ys),max(ys),t
if __name__ == "__main__":
  for it in sys.argv[1:]:
    x0,x1,y0,y1,t=bbox(it)
    print(f"{it}: x {x0:.2f}..{x1:.2f} (center {(x0+x1)/2:.2f})  y {y0:.2f}..{y1:.2f} (center {(y0+y1)/2:.2f})  size {x1-x0:.1f}x{y1-y0:.1f}  translation {t}")
