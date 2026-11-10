"""常用路径与模组 ID。作者：mryt233"""
from pathlib import Path
import shutil
import sys

MOD = Path(__file__).resolve().parent.parent.parent          # mod/
MODID = "orients_hotpot_chronicles"
SOURCE_RES = MOD / "common/src/main/resources"
# mryt233 改（2026-11-10）：--out 是隔离资源根目录；复制已有输入，绝不回写源资源。
RES = SOURCE_RES
if "--out" in sys.argv:
    index = sys.argv.index("--out")
    if index + 1 >= len(sys.argv):
        raise SystemExit("--out 后缺少资源输出目录")
    RES = Path(sys.argv[index + 1]).resolve()
    if RES == SOURCE_RES or SOURCE_RES in RES.parents:
        raise SystemExit("--out 必须使用源资源目录以外的独立目录")
    if not RES.exists():
        shutil.copytree(SOURCE_RES, RES)
ASSETS = RES / "assets" / MODID                              # 资源只有这一份（r122 起）
DATA = RES / "data" / MODID
ROOTS = [ASSETS]                                             # 兼容旧脚本「for root in ROOTS」的写法
VANILLA = MOD.parent / "viewer/cache/1.21.1/assets/minecraft"  # 原版资源（查看器缓存）
PREVIEW = MOD / "build/preview_assets" / MODID               # 预览用，不装进模组（配合 PREVIEW_ROOT）
OUT = MOD / "tools/out"

from .manifest import start_session
start_session(MOD, RES)
