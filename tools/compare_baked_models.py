"""比较全部原有模型的最终几何、实际贴图、粒子与显示参数。作者：mryt233"""
import argparse
import json
from pathlib import Path
from model_bake import compare


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--before", required=True, type=Path)
    parser.add_argument("--after", required=True, type=Path)
    parser.add_argument("--report", type=Path)
    args = parser.parse_args()
    result = compare(args.before, args.after)
    text = json.dumps(result, indent=2, ensure_ascii=False)
    if args.report:
        args.report.parent.mkdir(parents=True, exist_ok=True)
        args.report.write_text(text + "\n", encoding="utf-8", newline="\n")
    print(f"烘焙 {result['models']} 个模型；差异 {len(result['differences'])}")
    for row in result["differences"][:12]:
        print(row)
    return bool(result["differences"])


if __name__ == "__main__":
    raise SystemExit(main())
