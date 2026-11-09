"""请求带 -PhotpotClientProbe 的开发客户端取证；只写所选 run 目录。作者：mryt233"""
import argparse
import json
from pathlib import Path
import time
import sys

ROOT = Path(__file__).resolve().parent.parent


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--run-dir", required=True, type=Path)
    parser.add_argument("id")
    parser.add_argument("action", choices=("snapshot", "world", "language", "resize", "sauce", "sauceinput", "guide", "overview", "restock", "inventory", "creative", "aim",
                                           "config", "configset", "wear", "view", "camera", "observe", "walk", "pose", "input", "useblock", "close", "disconnect", "stop"))
    parser.add_argument("extra", nargs="*")
    args = parser.parse_args()
    folder = (ROOT / args.run_dir).resolve()
    allowed = {(ROOT / name / "run-client").resolve() for name in ("fabric", "neoforge")}
    if folder not in allowed or not folder.is_dir():
        parser.error("只允许本工程 fabric/run-client 或 neoforge/run-client")
    if not args.id or len(args.id) > 64 or any(c not in "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_-" for c in args.id):
        parser.error("id 只能包含字母、数字、横线和下划线，最长64字符")
    result = folder / f"hotpot-probe-{args.id}.json"
    if result.exists():
        parser.error("该取证 id 已存在，请换一个新 id，防止误读旧结果")
    payload = {"id": args.id, "action": args.action}
    if args.action == "aim":
        if len(args.extra) != 4:
            parser.error("aim 需要 x y z cell 四个整数")
        try:
            x, y, z, cell = (int(value) for value in args.extra)
        except ValueError:
            parser.error("aim 参数必须是整数")
        payload.update(x=x, y=y, z=z, cell=cell)
    # mryt233 改（2026-11-09，r146a）：configset 的参数（advanced / common / expand:N）走 extra 数组
    # mryt233 改：合并实测增加真实按键与独立侧面跟拍输入。
    if args.action in ("configset", "wear", "view", "camera", "resize", "sauceinput", "language", "observe", "walk", "pose", "input", "useblock"):
        payload["extra"] = list(args.extra)
    request = folder / "hotpot-probe-request.json"
    temp = request.with_suffix(".tmp")
    temp.write_bytes(json.dumps(payload).encode())
    temp.replace(request)
    if args.action == "stop":
        print("已请求开发客户端正常退出；以进程退出与日志为准。")
        return
    result = folder / f"hotpot-probe-{args.id}.json"
    deadline = time.monotonic() + 25
    while time.monotonic() < deadline:
        if result.exists():
            print(result.read_text(encoding="utf-8"))
            return
        time.sleep(0.2)
    raise SystemExit("开发客户端探针未响应；检查该实例日志与启动参数")


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    main()
