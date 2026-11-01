"""常用路径与模组 ID。作者：mryt233"""
from pathlib import Path

MOD = Path(__file__).resolve().parent.parent.parent          # mod/
MODID = "orients_hotpot_chronicles"
RES = MOD / "common/src/main/resources"
ASSETS = RES / "assets" / MODID                              # 资源只有这一份（r122 起）
DATA = RES / "data" / MODID
ROOTS = [ASSETS]                                             # 兼容旧脚本「for root in ROOTS」的写法
VANILLA = MOD.parent / "viewer/cache/1.21.1/assets/minecraft"  # 原版资源（查看器缓存）
PREVIEW = MOD / "build/preview_assets" / MODID               # 预览用，不装进模组（配合 PREVIEW_ROOT）
OUT = MOD / "tools/out"
