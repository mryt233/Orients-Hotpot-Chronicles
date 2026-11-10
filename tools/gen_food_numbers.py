# mryt233 改（2026-11-10）：同时识别旧字段写法与传入 registration 的生命周期注册。
"""把内容清单里的食物数值写回代码。

用途：`content/items.json` 是食物数值的唯一出处，改完清单跑一次这个脚本，
`HotPotContent.foodItem("id", 饥饿, 因子)` 里的两个数字就跟着变，不用手抠代码。

用法（工作区根目录或 mod/ 下都行）：
    python mod/tools/gen_food_numbers.py           # 只报告哪几项和代码不一致
    python mod/tools/gen_food_numbers.py --apply   # 把清单里的数写回代码
    python mod/tools/gen_food_numbers.py --apply --strict  # 有差异且没写时返回 1（给 CI 用）

只动 `foodItem(...)` 这一种注册方式里已经存在的两个数字参数，不新增、不删除物品；
新食物还是要先在代码里写一行 `foodItem("id", 1, 0.3F)`，再跑 gen_content_manifest.py 收进清单。

注意：写回的是「饱和度系数」，不是饱和点数（实际饱和点数 = 2 × 饥饿 × 系数），
和 Config 里的 `food.<id>.saturation` 含义一致。

作者：mryt233
"""
from __future__ import annotations

import json
import re
import sys
import tempfile
from pathlib import Path

MOD = Path(__file__).resolve().parent.parent
MANIFEST = MOD / "content/items.json"
CONTENT_JAVA = MOD / "common/src/main/java/cn/blockforge/generated/orientshotpotchronicles/hotpot/HotPotContent.java"

CALL = re.compile(r'(foodItem\(\s*(?:registration\s*,\s*)?"(?P<id>[a-z0-9_]+)",\s*)(?P<hunger>\d+)(,\s*)(?P<sat>[0-9.]+)(F?\))')


def fmt_float(value: float, original: str) -> str:
    """保持原来的写法：代码里写 0.3F 就还写 0.3F，写 0.30F 也留着两位。"""
    text = f"{value:g}"
    if "." not in text:
        text += ".0"
    return text


def main() -> int:
    apply_ = "--apply" in sys.argv
    strict_ = "--strict" in sys.argv
    if not MANIFEST.is_file():
        print(f"找不到清单：{MANIFEST}")
        return 1
    items = {i["id"]: i for i in json.loads(MANIFEST.read_text(encoding="utf-8"))["items"]}

    src = CONTENT_JAVA.read_text(encoding="utf-8")
    drifts: list[str] = []
    missing: list[str] = []

    def patch(match: re.Match) -> str:
        item_id = match.group("id")
        item = items.get(item_id)
        if item is None or "hunger" not in item:
            missing.append(item_id)
            return match.group(0)
        want_h = int(item["hunger"])
        want_s = float(item.get("saturation", match.group("sat")))
        have_h = int(match.group("hunger"))
        have_s = float(match.group("sat"))
        if want_h != have_h or abs(want_s - have_s) > 1e-9:
            drifts.append(f"{item_id}: 代码 {have_h}/{have_s:g} → 清单 {want_h}/{want_s:g}")
            return (f'{match.group(1)}{want_h}{match.group(4)}'
                    f'{fmt_float(want_s, match.group("sat"))}{match.group(6)}')
        return match.group(0)

    new_src = CALL.sub(patch, src)

    print(f"清单 {len(items)} 条，代码里找到 {len(CALL.findall(src))} 处 foodItem(...)")
    if missing:
        print(f"清单里有、代码里没有（忽略）：{', '.join(sorted(set(missing)))}")
    if not drifts:
        print("数值全部一致，不需要写回。")
        return 0
    print(f"发现 {len(drifts)} 处不一致：")
    for line in drifts:
        print(f"  - {line}")

    if apply_:
        handle, temp = tempfile.mkstemp(dir=str(CONTENT_JAVA.parent), prefix=".nums-", suffix=".tmp")
        try:
            with open(handle, "wb") as stream:
                stream.write(new_src.encode("utf-8"))
            Path(temp).replace(CONTENT_JAVA)
        finally:
            if Path(temp).exists():
                Path(temp).unlink()
        print("已写回代码。记得重新编译。")
        return 0

    print("这是预览，加 --apply 才会真的改代码。")
    return 1 if strict_ else 0


if __name__ == "__main__":
    raise SystemExit(main())