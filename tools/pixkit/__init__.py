"""pixkit：火锅物语贴图 / 模型生成的公共工具库。作者：mryt233（2026-11-01）

各 gen_*.py 以前各写一份 mix / shade / outline / write，同名函数的算法还不完全一样。
这里把每种算法分开命名保存（例如 mix 与 lerp、outline_inner 与 outline_outer），
迁移旧脚本时选对应的那个，生成结果逐字节不变。用法见同目录 README.md。

    import sys; sys.path.insert(0, str(Path(__file__).parent))   # 脚本在 tools/ 下时可省
    from pixkit import color, canvas, model, anim, io, paths
"""
from . import anim, canvas, color, io, model, paths  # noqa: F401
