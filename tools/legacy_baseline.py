"""旧档基线：用已发布的 0.2.2-alpha jar 真跑游戏建旧档、导出「旧档标准答案」、升级前后对比。作者：mryt233

用法（都在 mod/ 下执行；详细说明见 tools/legacy_baseline说明.md）：
    python tools/legacy_baseline.py setup   fabric|neoforge          # 建运行目录（复制启动器，放入旧版 jar）
    python tools/legacy_baseline.py start   fabric|neoforge [--world 名字] [--run-dir 目录]
    python tools/legacy_baseline.py stop    fabric|neoforge [--run-dir 目录]
    python tools/legacy_baseline.py build   fabric|neoforge          # 在旧版服务端里摆好基线内容
    python tools/legacy_baseline.py dump    --properties server.properties -o 输出.json   # 导出标准答案
    python tools/legacy_baseline.py compare 旧.json 新.json [--allow 允许差异.json]
    python tools/legacy_baseline.py fixtures 标准答案.json -o 夹具目录                      # 导出单测夹具

约定：
- 全部通过 RCON 与原版命令（setblock / data / item / loot / give）操作，不依赖开发命令 hpt，
  所以同一份 dump / compare 也能用在开发服（fabric/run-server）与升级后的试验包服务端上。
- RCON 密码只在各自的 server.properties 里，脚本不打印。
- 关服只用 RCON stop；不按进程名杀进程。
"""
from __future__ import annotations

import argparse
import json
import re
import secrets
import shutil
import subprocess
import sys
import time
from pathlib import Path

TOOLS = Path(__file__).resolve().parent
ROOT = TOOLS.parent
if str(TOOLS) not in sys.path:
    sys.path.insert(0, str(TOOLS))

import rcon_lib  # noqa: E402
from project_env import jdk21  # noqa: E402

BASE = ROOT / "build/opt4/4a"
EXPERIMENTS = ROOT / "experiments"
RELEASE_DIR = ROOT / "build/libs/releases/0.2.2-alpha"
MOD_ID = "orients_hotpot_chronicles"

LOADERS = {
    "fabric": {
        "src": "prod-fabric-server",
        "copy": [".fabric", "fabric-server-launch.jar", "libraries", "versions", "eula.txt"],
        "extra_mods": [("prod-fabric-server/mods/fabric-api-0.116.14+1.21.1.jar", None)],
        "jar": "orients_hotpot_chronicles-1.21.1-fabric-0.2.2-alpha.jar",
        "ports": (25611, 25621),
    },
    "neoforge": {
        "src": "prod-neoforge-server",
        "copy": ["libraries", "eula.txt", "user_jvm_args.txt"],
        "extra_mods": [],
        "jar": "orients_hotpot_chronicles-1.21.1-neoforge-0.2.2-alpha.jar",
        "ports": (25612, 25622),
    },
}

FLAT_GENERATOR = ('{"layers":[{"block":"minecraft:bedrock","height":1},{"block":"minecraft:dirt","height":2},'
                  '{"block":"minecraft:grass_block","height":1}],"biome":"minecraft:plains"}')


def run_dir_of(loader: str, override: str | None = None) -> Path:
    return Path(override) if override else BASE / loader


# ---------------------------------------------------------------------------
# 运行目录、启动、关闭
# ---------------------------------------------------------------------------

def write_properties(run_dir: Path, loader: str, world: str, ports=None) -> None:
    game_port, rcon_port = ports or LOADERS[loader]["ports"]
    path = run_dir / "server.properties"
    old_password = None
    if path.exists():
        for line in path.read_text(encoding="utf-8").splitlines():
            if line.startswith("rcon.password="):
                old_password = line.split("=", 1)[1]
    password = old_password or secrets.token_urlsafe(18)
    props = {
        "accepts-transfers": "false", "allow-flight": "true", "allow-nether": "false",
        "difficulty": "peaceful", "enable-command-block": "false", "enable-rcon": "true",
        "enforce-secure-profile": "false", "gamemode": "creative", "generate-structures": "false",
        "generator-settings": FLAT_GENERATOR.replace(":", "\\:"),
        "hardcore": "false", "level-name": world, "level-type": "minecraft\\:flat",
        "max-players": "2", "motd": "OPT4-4A-BASELINE", "online-mode": "false",
        "op-permission-level": "4", "player-idle-timeout": "0", "pvp": "false",
        "rcon.password": password, "rcon.port": str(rcon_port),
        "server-ip": "127.0.0.1", "server-port": str(game_port),
        "simulation-distance": "4", "spawn-animals": "false", "spawn-monsters": "false",
        "spawn-npcs": "false", "spawn-protection": "0", "sync-chunk-writes": "true",
        "view-distance": "4", "white-list": "false", "enable-status": "false",
        "region-file-compression": "deflate",
    }
    path.write_text("#opt4-4a baseline server\n" + "".join(f"{k}={v}\n" for k, v in props.items()),
                    encoding="utf-8", newline="\n")


def cmd_setup(args) -> int:
    cfg = LOADERS[args.loader]
    run_dir = run_dir_of(args.loader, args.run_dir)
    run_dir.mkdir(parents=True, exist_ok=True)
    src = EXPERIMENTS / cfg["src"]
    for name in cfg["copy"]:
        target = run_dir / name
        if target.exists():
            continue
        if (src / name).is_dir():
            shutil.copytree(src / name, target)
        else:
            shutil.copy2(src / name, target)
    mods = run_dir / "mods"
    mods.mkdir(exist_ok=True)
    for rel, _ in cfg["extra_mods"]:
        shutil.copy2(EXPERIMENTS / rel, mods / Path(rel).name)
    shutil.copy2(RELEASE_DIR / cfg["jar"], mods / cfg["jar"])
    write_properties(run_dir, args.loader, args.world)
    print(f"运行目录就绪：{run_dir}")
    return 0


def server_command(loader: str) -> list[str]:
    java = str(jdk21() / "bin/java.exe")
    if loader == "fabric":
        return [java, "-Xmx2G", "-jar", "fabric-server-launch.jar", "nogui"]
    return [java, "-Xmx2G", "@user_jvm_args.txt", "@libraries/net/neoforged/neoforge/21.1.265/win_args.txt", "nogui"]


def wait_done(log: Path, timeout: float, start_pos: int = 0) -> bool:
    deadline = time.time() + timeout
    while time.time() < deadline:
        if log.exists():
            text = log.read_bytes()[start_pos:].decode("utf-8", "replace")
            if re.search(r"Done \(\d", text) and "RCON running" in text:
                return True
        time.sleep(1.5)
    return False


def cmd_start(args) -> int:
    run_dir = run_dir_of(args.loader, args.run_dir)
    props = run_dir / "server.properties"
    if args.world:
        text = props.read_text(encoding="utf-8")
        text = re.sub(r"^level-name=.*$", f"level-name={args.world}", text, flags=re.M)
        props.write_text(text, encoding="utf-8", newline="\n")
    if rcon_lib.available(props):
        print("这个运行目录的服务端已经在跑，不重复启动。")
        return 1
    log = run_dir / "console.log"
    start_pos = log.stat().st_size if log.exists() else 0
    out = open(log, "ab")
    flags = subprocess.CREATE_NO_WINDOW | subprocess.CREATE_NEW_PROCESS_GROUP
    subprocess.Popen(server_command(args.loader), cwd=run_dir, stdin=subprocess.DEVNULL, stdout=out,
                     stderr=subprocess.STDOUT, creationflags=flags)
    if not wait_done(log, args.timeout, start_pos):
        print("启动超时（日志：%s）" % log)
        return 2
    print("服务端已启动：", run_dir)
    return 0


def cmd_stop(args) -> int:
    run_dir = run_dir_of(args.loader, args.run_dir)
    props = run_dir / "server.properties"
    try:
        rcon_lib.execute(props, "save-all flush")
        rcon_lib.execute(props, "stop")
    except (OSError, ValueError) as exc:
        print("RCON 失败：", exc)
        return 1
    log = run_dir / "console.log"
    for _ in range(120):
        time.sleep(1)
        if not rcon_lib.available(props):
            break
    time.sleep(3)
    print("服务端已停止。")
    return 0


# ---------------------------------------------------------------------------
# 命令执行
# ---------------------------------------------------------------------------

class Server:
    """一个 RCON 会话；每条命令一个返回，长命令自动检查包长度。"""

    def __init__(self, properties: Path):
        self.properties = properties
        self.log: list[tuple[str, str]] = []

    def run(self, command: str) -> str:
        if len(command.encode("utf-8")) > 1400:
            raise ValueError("命令超过 RCON 单包上限：" + command[:80])
        out = rcon_lib.execute(self.properties, command)[0]
        self.log.append((command, out))
        return out


# ---------------------------------------------------------------------------
# 配置文件：旧版生成 + 玩家改过几个值
# ---------------------------------------------------------------------------

# 玩家改过的值（点路径 -> 新值）。基线要覆盖 soup_bowl.* / seasoning.* 两类，另加几条别的做对照。
CONFIG_EDITS = {
    "soup_bowl.spicy.duration": 240,
    "soup_bowl.clear.hunger": 6,
    "soup_bowl.three_fresh.second_duration": 900,
    "seasoning.chili_oil.duration": 400,
    "seasoning.honey.duration": 100,
    "seasoning.msg.hunger": 3,
    "soup_speed.spicy": 1.5,
    "fuel.wood.power": 0.7,
    "cooking.items.potato_slice": 120,
    "food.cooked_beef_loin.hunger": 9,
}


def config_path(run_dir: Path) -> Path:
    return run_dir / "config" / f"{MOD_ID}.json"


def apply_config_edits(path: Path) -> list[str]:
    """按 CONFIG_EDITS 改配置里的值，保持旧版的写法（缩进 2、末尾换行）。返回改动清单。"""
    data = json.loads(path.read_text(encoding="utf-8"))
    changes = []
    for dotted, value in CONFIG_EDITS.items():
        node = data
        parts = dotted.split(".")
        for part in parts[:-1]:
            node = node[part]
        changes.append(f"{dotted}: {node[parts[-1]]} -> {value}")
        node[parts[-1]] = value
    path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n", encoding="utf-8", newline="\n")
    return changes


def cmd_config(args) -> int:
    run_dir = run_dir_of(args.loader, args.run_dir)
    path = config_path(run_dir)
    if not path.exists():
        print("还没有配置文件：先 start 一次让旧版 jar 生成，再 stop。")
        return 1
    out = BASE / "baseline"
    out.mkdir(parents=True, exist_ok=True)
    generated = out / f"config-{args.loader}-generated.json"
    if not generated.exists():
        shutil.copy2(path, generated)
    for line in apply_config_edits(path):
        print("  " + line)
    shutil.copy2(path, out / f"config-{args.loader}-edited.json")
    print("已保存：", generated.name, "（旧版原样生成）和 config-%s-edited.json（玩家改过）" % args.loader)
    return 0


# ---------------------------------------------------------------------------
# 基线内容（坐标固定；dump 和 build 共用这一份表）
# ---------------------------------------------------------------------------

P = MOD_ID + ":"
Y = 4  # 方块摆在 y=4，下面是 y=3 的石头平台

SOUP_NAMES = ["clear", "rich_stock", "mushroom", "spicy", "tomato", "three_fresh"]
# 15 种调料，序号 = Seasoning 枚举序号；原料用旧版的主原料
SEASONINGS = [
    ("msg", "minecraft:wheat"), ("chili_oil", "minecraft:blaze_powder"),
    ("sweet_sour_sauce", "minecraft:sweet_berries"), ("honey", "minecraft:honey_bottle"),
    ("sesame_oil", "minecraft:sunflower"), ("vinegar", "minecraft:apple"), ("sugar", "minecraft:sugar"),
    ("chopped_scallion", P + "scallion"), ("minced_cilantro", P + "cilantro"),
    ("garlic_paste", P + "garlic_paste"), ("oyster_sauce", P + "oyster_sauce"),
    ("sesame_paste", P + "sesame_paste"), ("crushed_peanuts", P + "crushed_peanuts"),
    ("millet_chili", P + "millet_chili"), ("minced_zhe_er_gen", P + "minced_zhe_er_gen"),
]
FULL_MASK = (1 << len(SEASONINGS)) - 1


def comp_text(**kw) -> str:
    """物品组件的命令写法 [a=1,b="x"]；值用 SNBT 文本。"""
    if not kw:
        return ""
    return "[" + ",".join(f"{P}{k}={v}" for k, v in kw.items()) + "]"


def snbt_str(text: str) -> str:
    return '"' + text + '"'


class Spot:
    """一个要摆、要查的地方：build 命令 + dump 查询。"""

    def __init__(self, name: str, note: str):
        self.name = name
        self.note = note
        self.build: list[str] = []
        self.queries: list[tuple[str, str]] = []


def block_spot(name, note, x, z, setup, y=Y, extra_queries=()):
    spot = Spot(name, note)
    spot.build += setup
    spot.queries.append((name, f"data get block {x} {y} {z}"))
    for q_name, cmd in extra_queries:
        spot.queries.append((q_name, cmd))
    return spot


def container_spot(name, note, x, z, kind, items, y=Y):
    """容器（箱子 / 木桶 / 潜影盒）：items = [(槽号, 物品 ID, 组件文本)]；按槽逐个查，便于做夹具。"""
    spot = Spot(name, note)
    spot.build.append(f"setblock {x} {y} {z} minecraft:{kind}")
    for slot, item_id, comps in items:
        spot.build.append(f"item replace block {x} {y} {z} container.{slot} with {item_id}{comps} 1")
    for slot, item_id, comps in items:
        spot.queries.append((f"{name}.slot{slot}", f"data get block {x} {y} {z} Items[{{Slot:{slot}b}}]"))
    return spot


def pot_nbt(**kw) -> str:
    return "{" + ",".join(f"{k}:{v}" for k, v in kw.items()) + "}"


def stack(item_id, comps=None):
    c = ",components:{" + ",".join(f"\"{P}{k}\":{v}" for k, v in comps.items()) + "}" if comps else ""
    return f"id:\"{item_id}\",count:1{c}"


def scenario() -> list[Spot]:
    spots: list[Spot] = []
    # --- 0. 平台 ------------------------------------------------------------
    prep = Spot("prep", "强加载 + 石头平台")
    prep.build += ["forceload add 32 32 63 63", "fill 30 3 30 66 3 66 minecraft:stone",
                   "fill 30 4 30 66 12 66 minecraft:air", "gamerule doMobSpawning false",
                   "gamerule doDaylightCycle false", "gamerule doWeatherCycle false",
                   "time set 6000", "weather clear"]
    spots.append(prep)

    # --- 1. 九宫格锅：六种汤，红汤四档辣度（z=34）-------------------------------
    pots = [
        ("pot_clear", 34, 0, 0), ("pot_rich_stock", 36, 1, 0), ("pot_mushroom", 38, 2, 0),
        ("pot_spicy_l0", 40, 3, 0), ("pot_spicy_l1", 42, 3, 1), ("pot_spicy_l2", 44, 3, 2),
        ("pot_spicy_l3", 46, 3, 3), ("pot_tomato", 48, 4, 0), ("pot_three_fresh", 50, 5, 0),
    ]
    for name, x, base, spicy in pots:
        extra = {"Base": base}
        if spicy:
            extra["Spicy"] = spicy
        spots.append(block_spot(name, f"铁九宫格锅：汤底序号 {base}，辣度 {spicy}", x, 34,
                                [f"setblock {x} {Y} 34 {P}iron_nine_grid_hotpot{pot_nbt(**extra)}"]))

    # 带食材的锅：生食、熟食、过头的豆腐、经验
    food_pot = Spot("pot_with_food", "红汤锅里有生食 / 熟食 / 过头豆腐 / 待领经验")
    food_pot.build += [
        f"setblock 52 {Y} 34 {P}iron_nine_grid_hotpot{{Base:3,Spicy:2,PendingXp:1.5f,"
        f"Items:[{{Slot:0b,id:\"{P}beef_loin_slice\",count:1}},"
        f"{{Slot:1b,id:\"{P}cooked_beef_loin_slice\",count:1}},"
        f"{{Slot:2b,id:\"{P}hot_tofu_cube\",count:1}},"
        f"{{Slot:40b,id:\"{P}cooked_potato_slice\",count:1}}]}}",
        f"data modify block 52 {Y} 34 Doneness[0] set value 30",
        f"data modify block 52 {Y} 34 Doneness[1] set value 50",
        f"data modify block 52 {Y} 34 Cooked[1] set value 1b",
        f"data modify block 52 {Y} 34 Doneness[2] set value 200",
        f"data modify block 52 {Y} 34 Cooked[2] set value 1b",
        f"data modify block 52 {Y} 34 Overcook[2] set value 300",
        f"data modify block 52 {Y} 34 SlotXp[1] set value 70",
        f"data modify block 52 {Y} 34 Doneness[40] set value 100",
        f"data modify block 52 {Y} 34 Cooked[40] set value 1b",
    ]
    food_pot.queries.append(("pot_with_food", f"data get block 52 {Y} 34"))
    spots.append(food_pot)

    # --- 2. 鸳鸯 / 田字锅，多边组合（z=36，独立锅方块）---------------------------
    multi = [
        ("pot_taiji_red_clear", 34, "iron_taiji_yuanyang_hotpot", dict(Base=3, BaseB=0, Spicy=2, SpicyB=0)),
        ("pot_round_tomato_three", 36, "copper_round_yuanyang_hotpot", dict(Base=4, BaseB=5)),
        ("pot_split_rich_mushroom", 38, "iron_split_yuanyang_hotpot", dict(Base=1, BaseB=2)),
        ("pot_square_tf_red3", 40, "copper_square_yuanyang_hotpot", dict(Base=5, BaseB=3, SpicyB=3)),
        ("pot_four_all", 42, "iron_four_grid_hotpot", dict(Base=0, BaseB=3, BaseC=4, BaseD=5, SpicyB=1)),
        ("pot_four_gaps", 44, "copper_four_grid_hotpot", dict(Base=3, BaseB=-1, BaseC=3, BaseD=-1, SpicyC=3)),
        ("pot_taiji_one_side", 46, "iron_taiji_yuanyang_hotpot", dict(Base=2, BaseB=-1)),
        ("pot_empty", 48, "iron_nine_grid_hotpot", {}),
    ]
    for name, x, block, nbt in multi:
        spots.append(block_spot(name, f"{block} {nbt}", x, 36,
                                [f"setblock {x} {Y} 36 {P}{block}{pot_nbt(**nbt)}"]))

    # --- 3. 灶台上的锅（z=38）--------------------------------------------------
    stoves = [
        ("stove_iron_clear", 34, "pot=iron_soup", dict(Base=0)),
        ("stove_yuanyang_s", 36, "pot=copper_yuanyang_s_soup", dict(Base=3, BaseB=1, Spicy=1)),
        ("stove_four_grid", 38, "pot=iron_four_grid_soup", dict(Base=2, BaseB=4, BaseC=5, BaseD=3, SpicyD=2)),
        ("stove_no_pot", 40, "pot=none", dict()),
    ]
    for name, x, state, nbt in stoves:
        spots.append(block_spot(name, f"brick_stove[{state}] {nbt}", x, 38,
                                [f"setblock {x} {Y} 38 {P}brick_stove[{state}]{pot_nbt(**nbt)}"]))
    # 带燃料但不烧（BurnTicks 0，免得几次导出之间数值在变）
    spots.append(block_spot("stove_with_fuel", "灶膛里有煤，没点着", 42, 38,
                            [f"setblock 42 {Y} 38 {P}brick_stove[pot=iron_soup]"
                             f"{{Base:3,Spicy:3,FuelType:1,Fuel:{{Items:[{{Slot:0b,id:\"minecraft:coal\",count:7}}]}}}}"]))
    # 高灶台
    spots.append(block_spot("stove_tall", "高灶台 + 上半截", 44, 38,
                            [f"setblock 44 {Y} 38 {P}tall_brick_stove[pot=copper_soup]{{Base:1}}",
                             f"setblock 44 {Y + 1} 38 {P}tall_brick_stove_top"]))

    # --- 4. 蘸料台（z=40）------------------------------------------------------
    def station(name, note, x, wood, kinds, portions, raws, top=True):
        spot = Spot(name, note)
        parts = []
        if kinds is not None:
            parts.append("Kinds:[I;" + ",".join(map(str, kinds)) + "]")
        parts.append("Portions:[I;" + ",".join(map(str, portions)) + "]")
        if raws:
            parts.append("Items:[" + ",".join(f"{{Slot:{i}b,id:\"{rid}\",count:{cnt}}}" for i, (rid, cnt) in raws.items()) + "]")
        spot.build.append(f"setblock {x} {Y} 40 {P}{wood}_sauce_station[facing=north]{{{','.join(parts)}}}")
        if top:
            spot.build.append(f"setblock {x} {Y + 1} 40 {P}sauce_station_top[facing=north]")
        spot.queries.append((name, f"data get block {x} {Y} 40"))
        return spot

    raw1 = {i: (SEASONINGS[i][1], 1 + i % 3) for i in range(9)}
    spots.append(station("station_kinds_0_8", "蘸料台 A：调料 0～8 各一碗（份数不一，原料格有货）", 34, "oak",
                         list(range(9)), [4, 3, 2, 1, 4, 3, 2, 1, 4], raw1))
    raw2 = {i: (SEASONINGS[9 + i][1], 2) for i in range(6)}
    raw2[0] = (SEASONINGS[9][1], 5)
    spots.append(station("station_kinds_9_14", "蘸料台 B：调料 9～14 各一碗，后三格空", 36, "birch",
                         [9, 10, 11, 12, 13, 14, -1, -1, -1], [4, 0, 2, 0, 1, 3, 0, 0, 0], raw2))
    spots.append(station("station_all_empty", "蘸料台 C：全空碗", 38, "iron", [-1] * 9, [0] * 9, {}))
    spots.append(station("station_portions_no_raw", "蘸料台 D：份数 > 0 但原料格空", 40, "acacia",
                         [2, -1, 7, -1, -1, -1, -1, -1, 14], [6, 0, 3, 0, 0, 0, 0, 0, 1], {}))
    spots.append(station("station_legacy_no_kinds", "蘸料台 E：更老的存档，没有 Kinds（第 i 格 = 第 i 种）", 42, "spruce",
                         None, [3, 0, 2, 0, 0, 0, 0, 0, 1], {}))
    spots.append(station("station_raw_only", "蘸料台 F：只有原料没有份数、Kinds 为空（醋、南瓜子、甜菜根）", 44, "dark_oak",
                         [-1] * 9, [0] * 9, {0: (SEASONINGS[5][1], 4), 4: ("minecraft:pumpkin_seeds", 2),
                                             5: ("minecraft:beetroot", 3)}))

    # --- 5. 桌上餐具（z=42）------------------------------------------------------
    def table(name, note, x, z, table_block, plates, tall=False):
        spot = Spot(name, note)
        spot.build.append(f"setblock {x} {Y} {z} {P}{table_block}")
        flag = "on_tall_table=true" if tall else "on_table=true"
        spot.build.append(f"setblock {x} {Y + 1} {z} {P}plates[{flag}]{{Plates:[{plates[0]}]}}")
        for extra in plates[1:]:
            spot.build.append(f"data modify block {x} {Y + 1} {z} Plates append value {extra}")
        spot.queries.append((name, f"data get block {x} {Y + 1} {z}"))
        return spot

    def plate(plate_block, x, z, axis="x", self_item=None, items=None, chopsticks=None):
        text = f"{{Plate:\"{P}{plate_block}\",X:{x}.0d,Z:{z}.0d,Axis:\"{axis}\""
        if items:
            text += ",Items:[" + ",".join(f"{{Slot:{i}b,{it}}}" for i, it in items.items()) + "]"
        if self_item:
            text += f",Self:{{{self_item}}}"
        if chopsticks:
            text += f",Chopsticks:{{{chopsticks}}}"
        return text + "}"

    sauce_all = {"sauce": f"{{mask:{FULL_MASK},bites:9}}"}
    sauce_two = {"sauce": "{mask:6,bites:5}"}
    spicy_cooked = {"cooked_in_soup": snbt_str("spicy"), "spicy_level": "3", "dipped": "6"}
    chop_food = stack(P + "bamboo_chopsticks", {"held_food": "{" + stack(P + "cooked_beef_loin_slice", spicy_cooked) + "}"})
    spots.append(table("table_oak_mixed", "橡木桌：汤碗、蘸料碗、饮料、杯、小碟(带蘸过的菜)、架着筷子(带菜)", 34, 42, "oak_hotpot_table", [
        plate("bowl", 3, 3, self_item=stack(P + "spicy_red_broth_bowl", {"cooked_in_soup": snbt_str("spicy"), "spicy_level": "2"})),
        plate("bowl", 8, 3, self_item=stack(P + "dipping_sauce", sauce_all)),
        plate("bowl", 13, 3, self_item=stack(P + "dipping_sauce", sauce_two), chopsticks=chop_food),
        plate("drink", 3, 8, self_item=stack(P + "orange_drink")),
        plate("cup", 8, 8, self_item=stack(P + "orange_drink_cup")),
        plate("iron_small_dish", 3, 13, items={
            0: stack(P + "cooked_beef_loin_slice", {"dipped": "17", "cooked_in_soup": snbt_str("clear")}),
            1: stack(P + "cooked_potato_slice", {"dipped": str(FULL_MASK), "cooked_in_soup": snbt_str("three_fresh")}),
            2: stack(P + "greens")}),
    ]))
    spots.append(table("table_iron_bowl_food", "铁桌：装菜的碗 + 汤碗 + 盘子 + 桌上竹筷", 36, 42, "iron_hotpot_table", [
        plate("bowl", 4, 4, items={0: stack(P + "cooked_mutton_roll", {"dipped": "4", "cooked_in_soup": snbt_str("rich_stock")})}),
        plate("bowl", 12, 4, self_item=stack(P + "tomato_soup_bowl", {"cooked_in_soup": snbt_str("tomato")})),
        plate("quartz_two_slot_plate", 8, 11, axis="z", items={0: stack(P + "cooked_fish_ball"), 1: stack(P + "beef_loin_slice")}),
        plate("chopstick_rest", 13, 12, self_item=stack(P + "bamboo_chopsticks", {
            "held_food": "{" + stack(P + "cooked_squid_roll", {"dipped": "8192", "cooked_in_soup": snbt_str("mushroom")}) + "}"})),
    ]))
    spots.append(table("table_andesite_bowls_all_soups", "磨制安山岩桌：六种汤碗各一只", 38, 42, "polished_andesite_hotpot_table", [
        plate("bowl", 3, 3, self_item=stack(P + "clear_broth_bowl", {"cooked_in_soup": snbt_str("clear")})),
        plate("bowl", 8, 3, self_item=stack(P + "rich_stock_bowl")),
        plate("bowl", 13, 3, self_item=stack(P + "mushroom_broth_bowl")),
        plate("bowl", 3, 9, self_item=stack(P + "spicy_red_broth_bowl", {"spicy_level": "0"})),
        plate("bowl", 8, 9, self_item=stack(P + "tomato_soup_bowl")),
        plate("bowl", 13, 9, self_item=stack(P + "three_fresh_soup_bowl")),
    ]))
    spots.append(table("table_tall_quartz", "石英高桌：蘸料碗 + 小碟", 40, 42, "quartz_tall_hotpot_table", [
        plate("bowl", 4, 4, self_item=stack(P + "dipping_sauce", {"sauce": "{mask:16384,bites:9}"})),
        plate("quartz_small_dish", 11, 11, items={0: stack(P + "cooked_enoki_mushroom", {"dipped": "1"})}),
    ], tall=True))

    # --- 6. 容器里的物品（z=46 一排）----------------------------------------------
    sauce_variants = [
        ("sauce_one_msg", {"mask": 1, "bites": 9}),
        ("sauce_one_zhe_er_gen", {"mask": 16384, "bites": 9}),
        ("sauce_all15_full", {"mask": FULL_MASK, "bites": 9}),
        ("sauce_all15_half_eaten", {"mask": FULL_MASK, "bites": 4}),
        ("sauce_14_missing_zhe_er_gen", {"mask": 16383, "bites": 9}),
        ("sauce_14_missing_msg", {"mask": 32766, "bites": 9}),
        ("sauce_two_last_bite", {"mask": 10, "bites": 1}),
        ("sauce_mask0", {"mask": 0, "bites": 9}),
        ("sauce_high_bit", {"mask": 32768 | 3, "bites": 9}),
        ("sauce_bites0", {"mask": 255, "bites": 0}),
    ]
    items = [(i, P + "dipping_sauce", comp_text(sauce=f"{{mask:{v['mask']},bites:{v['bites']}}}"))
             for i, (_, v) in enumerate(sauce_variants)]
    items.append((len(items), P + "dipping_sauce", ""))   # 没有 sauce 组件的蘸料碗
    spots.append(container_spot("chest_sauce_bowls", "箱子：蘸料碗 11 种（槽位顺序：" + "、".join(n for n, _ in sauce_variants) + "、无组件）",
                                34, 46, "chest", items))

    dish_foods = ["cooked_beef_loin_slice", "cooked_potato_slice", "cooked_mutton_roll", "cooked_greens",
                  "cooked_fish_ball", "cooked_squid_roll"]
    dishes = []
    slot = 0
    for i, soup in enumerate(SOUP_NAMES):          # 每种汤煮出来的菜，蘸不同的料
        dishes.append((slot, P + dish_foods[i], comp_text(cooked_in_soup=snbt_str(soup), dipped=str(1 << (i * 2 + 1)))))
        slot += 1
    for level in range(4):                          # 红汤四档辣度的菜
        dishes.append((slot, P + "cooked_beef_loin_slice",
                       comp_text(cooked_in_soup=snbt_str("spicy"), spicy_level=str(level), dipped=str(1 << level))))
        slot += 1
    dishes += [
        (slot, P + "cooked_beef_loin_slice", comp_text(dipped=str(FULL_MASK))),            # 15 种全蘸，没有汤底记录
        (slot + 1, P + "cooked_beef_loin_slice", comp_text(dipped="0")),
        (slot + 2, P + "cooked_potato_slice", comp_text(dipped="32768")),                  # 高位
        (slot + 3, P + "cooked_potato_slice", comp_text(dipped="16384", cooked_in_soup=snbt_str("three_fresh"))),
        (slot + 4, P + "beef_loin_slice", ""),                                             # 生食，无组件
        (slot + 5, P + "cooked_enoki_mushroom", comp_text(cooked_in_soup=snbt_str("minecraft:unknown_future"))),  # 未知汤底名
    ]
    spots.append(container_spot("chest_dipped_dishes", "箱子：蘸过的菜 / 六种汤煮出来的菜 / 辣度 0～3 / 边界值", 36, 46, "chest", dishes))

    bowls = []
    soup_bowl_ids = ["clear_broth_bowl", "rich_stock_bowl", "mushroom_broth_bowl", "spicy_red_broth_bowl",
                     "tomato_soup_bowl", "three_fresh_soup_bowl"]
    for i, bowl in enumerate(soup_bowl_ids):
        bowls.append((i, P + bowl, comp_text(cooked_in_soup=snbt_str(SOUP_NAMES[i]))))
    for level in range(4):
        bowls.append((6 + level, P + "spicy_red_broth_bowl", comp_text(cooked_in_soup=snbt_str("spicy"), spicy_level=str(level))))
    bowls.append((10, P + "clear_broth_bowl", ""))
    bowls.append((11, P + "bamboo_chopsticks", comp_text(held_food="{" + stack(P + "cooked_beef_loin_slice", dict(spicy_cooked)) + "}")))
    bowls.append((12, P + "bamboo_chopsticks", comp_text(held_food="{" + stack(P + "cooked_potato_slice", {"dipped": str(FULL_MASK)}) + "}")))
    bowls.append((13, P + "bamboo_chopsticks", ""))
    bowls.append((14, P + "glass_cup", ""))
    spots.append(container_spot("chest_soup_bowls", "箱子：六种汤碗 + 辣度 0～3 的红汤碗 + 筷子夹菜", 38, 46, "chest", bowls))

    # 装锅物品：用 /loot 从上面的锅方块里取（走旧版自己的 copy_components 掉落，不是手写 NBT）
    pot_loot_sources = ["pot_clear", "pot_rich_stock", "pot_mushroom", "pot_spicy_l0", "pot_spicy_l1", "pot_spicy_l2",
                        "pot_spicy_l3", "pot_tomato", "pot_three_fresh", "pot_with_food", "pot_taiji_red_clear",
                        "pot_round_tomato_three", "pot_split_rich_mushroom", "pot_square_tf_red3", "pot_four_all",
                        "pot_four_gaps", "pot_taiji_one_side", "pot_empty"]
    src_pos = {}
    for sp in spots:
        if sp.name in pot_loot_sources:
            src_pos[sp.name] = sp.queries[0][1].replace("data get block ", "")
    pack = Spot("chest_packed_pots", "两个箱子：18 口锅打包成物品（九宫格 + 鸳鸯 + 田字 + 带食材 + 空锅）")
    pack.build.append(f"setblock 40 {Y} 46 minecraft:chest")
    pack.build.append(f"setblock 42 {Y} 46 minecraft:chest")
    for n, name in enumerate(pot_loot_sources):
        chest_x = 40 if n < 9 else 42
        pack.build.append(f"loot insert {chest_x} {Y} 46 mine {src_pos[name]}")
    for n in range(9):
        pack.queries.append((f"chest_packed_pots.a{n}", f"data get block 40 {Y} 46 Items[{{Slot:{n}b}}]"))
        pack.queries.append((f"chest_packed_pots.b{n}", f"data get block 42 {Y} 46 Items[{{Slot:{n}b}}]"))
    spots.append(pack)

    # 潜影盒：里面有蘸料碗、蘸过的菜、汤碗、筷子；方块形态和物品形态（嵌套 container 组件）都查
    shulker = Spot("shulker_box", "潜影盒：方块 + 装进箱子后的物品形态（嵌套组件）")
    shulker.build += [
        f"setblock 44 {Y} 46 minecraft:shulker_box",
        f"item replace block 44 {Y} 46 container.0 with {P}dipping_sauce{comp_text(sauce=f'{{mask:{FULL_MASK},bites:9}}')} 1",
        f"item replace block 44 {Y} 46 container.1 with {P}dipping_sauce{comp_text(sauce='{mask:5,bites:2}')} 1",
        f"item replace block 44 {Y} 46 container.2 with {P}cooked_beef_loin_slice{comp_text(dipped='21', cooked_in_soup=snbt_str('spicy'), spicy_level='1')} 1",
        f"item replace block 44 {Y} 46 container.3 with {P}three_fresh_soup_bowl{comp_text(cooked_in_soup=snbt_str('three_fresh'))} 1",
        f"item replace block 44 {Y} 46 container.4 with {P}bamboo_chopsticks{comp_text(held_food='{' + stack(P + 'cooked_greens', {'dipped': '3'}) + '}')} 1",
        f"setblock 46 {Y} 46 minecraft:chest",
        f"loot insert 46 {Y} 46 mine 44 {Y} 46",
        f"loot insert 46 {Y} 46 mine {src_pos['pot_spicy_l3']}",
    ]
    shulker.queries.append(("shulker_box.block", f"data get block 44 {Y} 46 Items"))
    shulker.queries.append(("shulker_box.item_in_chest", f"data get block 46 {Y} 46 Items[{{Slot:0b}}]"))
    spots.append(shulker)

    spots.append(container_spot("barrel_misc", "木桶：蘸料碗 + 菜 + 汤碗", 48, 46, "barrel", [
        (0, P + "dipping_sauce", comp_text(sauce="{mask:1023,bites:7}")),
        (13, P + "cooked_lotus_root_slice", comp_text(dipped="1024", cooked_in_soup=snbt_str("mushroom"))),
        (26, P + "mushroom_broth_bowl", comp_text(cooked_in_soup=snbt_str("mushroom"))),
    ]))

    # 掉落物 / 盔甲架（没有玩家也能覆盖「物品放在实体里」）
    ents = Spot("entities", "掉落物（蘸料碗、蘸过的菜）与盔甲架（手持夹着菜的筷子 + 蘸料碗）")
    ents.build += [
        "kill @e[tag=opt4a]",
        f"summon item 36 {Y + 1} 52 {{Tags:[\"opt4a\",\"opt4a_item\"],NoGravity:1b,Age:-32768s,PickupDelay:32767s,"
        f"Item:{{id:\"{P}dipping_sauce\",count:1,components:{{\"{P}sauce\":{{mask:{FULL_MASK},bites:9}}}}}}}}",
        f"summon item 38 {Y + 1} 52 {{Tags:[\"opt4a\",\"opt4a_item2\"],NoGravity:1b,Age:-32768s,PickupDelay:32767s,"
        f"Item:{{id:\"{P}cooked_beef_loin_slice\",count:3,components:{{\"{P}dipped\":21,\"{P}cooked_in_soup\":\"spicy\",\"{P}spicy_level\":2}}}}}}",
        f"summon armor_stand 40 {Y} 52 {{Tags:[\"opt4a\",\"opt4a_stand\"],NoGravity:1b,Invisible:1b,"
        f"HandItems:[{{id:\"{P}bamboo_chopsticks\",count:1,components:{{\"{P}held_food\":{{{stack(P + 'cooked_beef_loin_slice', spicy_cooked)}}}}}}},"
        f"{{id:\"{P}dipping_sauce\",count:1,components:{{\"{P}sauce\":{{mask:6,bites:3}}}}}}]}}",
    ]
    ents.queries.append(("entity_item_sauce", "data get entity @e[tag=opt4a_item,limit=1] Item"))
    ents.queries.append(("entity_item_dish", "data get entity @e[tag=opt4a_item2,limit=1] Item"))
    ents.queries.append(("entity_stand_hands", "data get entity @e[tag=opt4a_stand,limit=1] HandItems"))
    spots.append(ents)
    return spots


def all_queries(spots) -> list[tuple[str, str]]:
    out = []
    for spot in spots:
        out.extend(spot.queries)
    return out


# ---------------------------------------------------------------------------
# build / dump
# ---------------------------------------------------------------------------

SCENARIO_VERSION = 1
ERROR_WORDS = ("unknown", "expected", "incorrect", "no such", "cannot", "could not", "failed", "error", "unable", "invalid")


def cmd_build(args) -> int:
    run_dir = run_dir_of(args.loader, args.run_dir)
    server = Server(run_dir / "server.properties")
    errors = []
    for spot in scenario():
        for command in spot.build:
            out = server.run(command)
            if any(w in out.lower() for w in ERROR_WORDS):
                errors.append((spot.name, command[:160], out[:200]))
        print(f"  {spot.name}: {len(spot.build)} 条命令")
    server.run("save-all flush")
    if errors:
        print("下列命令回执像是出错：")
        for e in errors:
            print("  ", e)
        return 2
    print("基线内容已摆好。")
    return 0


def dump_server(properties: Path) -> dict:
    server = Server(properties)
    entries = []
    for name, command in all_queries(scenario()):
        entries.append({"name": name, "cmd": command, "out": server.run(command).strip()})
    return {"meta": {"tool": "legacy_baseline.py", "scenario": SCENARIO_VERSION,
                     "created": time.strftime("%Y-%m-%d %H:%M:%S")},
            "entries": entries}


def cmd_dump(args) -> int:
    props = Path(args.properties) if args.properties else run_dir_of(args.loader) / "server.properties"
    data = dump_server(props)
    data["meta"]["label"] = args.label or ""
    Path(args.output).parent.mkdir(parents=True, exist_ok=True)
    Path(args.output).write_text(json.dumps(data, indent=1, ensure_ascii=False) + "\n", encoding="utf-8", newline="\n")
    print(f"已导出 {len(data['entries'])} 条查询 -> {args.output}")
    return 0


# ---------------------------------------------------------------------------
# SNBT 解析（只够读 /data get 的输出）与对比
# ---------------------------------------------------------------------------

class Scalar(str):
    """数值 / 布尔的原文（带后缀，如 1b、0.0f），用原文比较，不丢类型。"""


class Arr(list):
    """[I; ...] / [B; ...] / [L; ...]：元素是原文。"""

    def __init__(self, kind, items):
        super().__init__(items)
        self.kind = kind


class SnbtError(ValueError):
    pass


class _Parser:
    def __init__(self, text: str):
        self.s = text
        self.i = 0

    def ws(self):
        while self.i < len(self.s) and self.s[self.i] in " \t\r\n":
            self.i += 1

    def peek(self):
        return self.s[self.i] if self.i < len(self.s) else ""

    def expect(self, ch):
        self.ws()
        if self.peek() != ch:
            raise SnbtError(f"位置 {self.i} 需要 {ch!r}，实际是 {self.s[self.i:self.i + 20]!r}")
        self.i += 1

    def value(self):
        self.ws()
        ch = self.peek()
        if ch == "{":
            return self.compound()
        if ch == "[":
            return self.listing()
        if ch in "\"'":
            return self.quoted()
        return self.bare()

    def quoted(self):
        quote = self.s[self.i]
        self.i += 1
        out = []
        while True:
            if self.i >= len(self.s):
                raise SnbtError("字符串没有结束")
            ch = self.s[self.i]
            if ch == "\\":
                out.append(self.s[self.i + 1])
                self.i += 2
                continue
            if ch == quote:
                self.i += 1
                return "".join(out)
            out.append(ch)
            self.i += 1

    def bare(self):
        start = self.i
        while self.i < len(self.s) and self.s[self.i] not in ",]}:" and self.s[self.i] not in " \t\r\n":
            self.i += 1
        token = self.s[start:self.i]
        if not token:
            raise SnbtError(f"位置 {self.i} 没有值")
        if re.fullmatch(r"-?\d+(\.\d+)?([bBsSlLfFdD])?|true|false", token):
            return Scalar(token)
        return token

    def key(self):
        self.ws()
        if self.peek() in "\"'":
            return self.quoted()
        start = self.i
        while self.i < len(self.s) and self.s[self.i] not in ":":
            self.i += 1
        return self.s[start:self.i].strip()

    def compound(self):
        self.expect("{")
        result = {}
        self.ws()
        if self.peek() == "}":
            self.i += 1
            return result
        while True:
            k = self.key()
            self.expect(":")
            result[k] = self.value()
            self.ws()
            if self.peek() == ",":
                self.i += 1
                continue
            self.expect("}")
            return result

    def listing(self):
        self.expect("[")
        self.ws()
        m = re.match(r"([BIL]);", self.s[self.i:])
        kind = None
        if m:
            kind = m.group(1)
            self.i += 2
        items = []
        self.ws()
        if self.peek() == "]":
            self.i += 1
            return Arr(kind, items) if kind else items
        while True:
            items.append(self.value())
            self.ws()
            if self.peek() == ",":
                self.i += 1
                continue
            self.expect("]")
            return Arr(kind, items) if kind else items


def parse_snbt(text: str):
    parser = _Parser(text.strip())
    node = parser.value()
    parser.ws()
    if parser.i != len(parser.s):
        raise SnbtError(f"尾部还有内容：{parser.s[parser.i:parser.i + 30]!r}")
    return node


_BARE_KEY = re.compile(r"[A-Za-z0-9_.+-]+")


def to_snbt(node) -> str:
    if isinstance(node, Scalar):
        return str(node)
    if isinstance(node, str):
        return '"' + node.replace("\\", "\\\\").replace('"', '\\"') + '"'
    if isinstance(node, dict):
        parts = []
        for k, v in node.items():
            key = k if _BARE_KEY.fullmatch(k) else to_snbt(k)
            parts.append(f"{key}: {to_snbt(v)}")
        return "{" + ", ".join(parts) + "}"
    if isinstance(node, Arr):
        return "[" + node.kind + "; " + ", ".join(node) + "]"
    return "[" + ", ".join(to_snbt(v) for v in node) + "]"


PREFIX = re.compile(r"^.*? has the following (?:block|entity) data: ", re.S)


def entry_value(entry: dict):
    """返回解析后的值；不是 data get 的正常回执（报错 / 没有元素）时返回原文字符串并标记。
    同时认两种条目：dump 的 {out: 回执原文}，和夹具文件的 {snbt: 已去前缀 / raw: 非正常回执}。"""
    if "snbt" in entry:
        try:
            return ("snbt", parse_snbt(entry["snbt"]))
        except SnbtError:
            return ("raw", entry["snbt"])
    if "raw" in entry:
        return ("raw", entry["raw"])
    out = entry["out"].strip()
    m = PREFIX.match(out)
    if not m:
        return ("raw", out)
    try:
        return ("snbt", parse_snbt(out[m.end():]))
    except SnbtError:
        return ("raw", out)


def flatten(node, path="") -> dict:
    """摊平成 路径 -> 叶子文本；数组整体当一个叶子。"""
    if isinstance(node, dict):
        out = {}
        for k, v in node.items():
            out.update(flatten(v, f"{path}.{k}" if path else k))
        return out or {path: "{}"}
    if isinstance(node, Arr):
        return {path: f"[{node.kind}; " + ", ".join(node) + "]"}
    if isinstance(node, list):
        out = {}
        for i, v in enumerate(node):
            out.update(flatten(v, f"{path}[{i}]"))
        return out or {path: "[]"}
    return {path: str(node) if not isinstance(node, Scalar) else str(node)}


def short(text: str, limit: int = 90) -> str:
    text = re.sub(r"(0, ){6,}", "0…", text)
    text = re.sub(r"(0B, ){6,}", "0B…", text)
    return text if len(text) <= limit else text[:limit] + "…"


def diff_entries(old: dict, new: dict) -> list[tuple[str, str, str, str]]:
    """返回 (kind, path, old, new) 列表；kind = added / removed / changed。"""
    kind_o, val_o = entry_value(old)
    kind_n, val_n = entry_value(new)
    if kind_o == "raw" or kind_n == "raw":
        if kind_o == kind_n == "raw" and val_o == val_n:
            return []
        return [("changed", "(整条回执)", short(str(val_o)), short(str(val_n)))]
    fo, fn = flatten(val_o), flatten(val_n)
    result = []
    for path in sorted(set(fo) | set(fn)):
        if path not in fn:
            result.append(("removed", path, fo[path], ""))
        elif path not in fo:
            result.append(("added", path, "", fn[path]))
        elif fo[path] != fn[path]:
            result.append(("changed", path, fo[path], fn[path]))
    return result


def load_dump(path: Path) -> dict:
    """读 dump 文件，或夹具文件（entries 是 名字 -> 条目 的字典），统一成 entries 列表。"""
    data = json.loads(Path(path).read_text(encoding="utf-8"))
    if isinstance(data.get("entries"), dict):
        data["entries"] = [dict(item, name=name) for name, item in data["entries"].items()]
    return data


def load_allow(path: str | None) -> list[dict]:
    if not path:
        return []
    return json.loads(Path(path).read_text(encoding="utf-8"))["rules"]


def rule_allows(rules, name, kind, path) -> str | None:
    for rule in rules:
        if not re.fullmatch(rule.get("entry", ".*"), name):
            continue
        if rule.get("kind", "any") not in ("any", kind):
            continue
        if not re.fullmatch(rule.get("path", ".*"), path):
            continue
        return rule.get("reason", "")
    return None


def compare_dumps(old: dict, new: dict, rules) -> dict:
    old_by = {e["name"]: e for e in old["entries"]}
    new_by = {e["name"]: e for e in new["entries"]}
    report = {"identical": [], "allowed": [], "unexpected": [], "missing": [], "extra": [], "text_differs": []}
    for name, entry in old_by.items():
        if name not in new_by:
            report["missing"].append(name)
            continue
        diffs = diff_entries(entry, new_by[name])
        if not diffs:
            report["identical"].append(name)
            if "out" in entry and "out" in new_by[name] and entry["out"].strip() != new_by[name]["out"].strip():
                report["text_differs"].append(name)   # 内容相同，只是键顺序 / 空白不同
            continue
        bad, ok = [], []
        for kind, path, a, b in diffs:
            reason = rule_allows(rules, name, kind, path)
            (ok if reason is not None else bad).append((kind, path, a, b, reason))
        if bad:
            report["unexpected"].append((name, bad))
        if ok and not bad:
            report["allowed"].append((name, ok))
        elif ok:
            report["allowed"].append((name, ok))
    for name in new_by:
        if name not in old_by:
            report["extra"].append(name)
    return report


def cmd_compare(args) -> int:
    old, new = load_dump(args.old), load_dump(args.new)
    rules = load_allow(args.allow)
    report = compare_dumps(old, new, rules)
    total = len(old["entries"])
    print(f"对比：{args.old} -> {args.new}")
    print(f"  共 {total} 条查询：内容相同 {len(report['identical'])}，仅有允许差异 "
          f"{sum(1 for n, d in report['allowed'] if all(n != u[0] for u in report['unexpected']))}，"
          f"出现不在允许清单里的差异 {len(report['unexpected'])}，新档缺少 {len(report['missing'])}，新档多出 {len(report['extra'])}")
    if report["text_differs"]:
        print(f"  其中 {len(report['text_differs'])} 条回执原文不是逐字相同（键顺序 / 空白不同，内容相同）"
              + ("：" + "、".join(report["text_differs"][:5]) if args.verbose else ""))
        if args.text_exact:
            print("  已指定 --text-exact：按失败处理")
    if args.verbose:
        for name, diffs in report["allowed"]:
            for kind, path, a, b, reason in diffs:
                print(f"  [允许:{reason}] {name} {kind} {path}: {short(a)} -> {short(b)}")
    for name, diffs in report["unexpected"]:
        for kind, path, a, b, _ in diffs:
            print(f"  [差异] {name} {kind} {path}: {short(a)} -> {short(b)}")
    for name in report["missing"]:
        print(f"  [缺少] {name}")
    for name in report["extra"]:
        print(f"  [多出] {name}")
    bad = report["unexpected"] or report["missing"] or (args.text_exact and report["text_differs"])
    print("结论：" + ("有不在允许清单里的差异（见上）" if bad else "没有不在允许清单里的差异"))
    return 1 if bad else 0


# ---------------------------------------------------------------------------
# 夹具导出
# ---------------------------------------------------------------------------

def cmd_fixtures(args) -> int:
    dump = load_dump(args.dump)
    out = {"meta": {"source": "0.2.2-alpha 已发布 jar 真跑建档后用 /data get 导出（tools/legacy_baseline.py）",
                    "loader": args.loader_label, "scenario": dump["meta"].get("scenario"),
                    "dumped": dump["meta"].get("created"),
                    "note": "snbt 字段是 /data get 的原样回执去掉前缀，可用原版 TagParser 读回；item 条目另拆出 id / count / components。Fabric 与 NeoForge 两端导出的回执原文逐字相同。"},
           "entries": {}}
    for entry in dump["entries"]:
        kind, value = entry_value(entry)
        item: dict = {"cmd": entry["cmd"]}
        if kind == "raw":
            item["raw"] = value
        else:
            item["snbt"] = to_snbt(value)
            if isinstance(value, dict) and "id" in value and "count" in value and "Slot" in value or \
                    (isinstance(value, dict) and "id" in value and "count" in value):
                item["item"] = {"id": value["id"], "count": str(value["count"]),
                                "components_snbt": to_snbt(value.get("components", {}))}
        out["entries"][entry["name"]] = item
    Path(args.output).parent.mkdir(parents=True, exist_ok=True)
    Path(args.output).write_text(json.dumps(out, indent=1, ensure_ascii=False) + "\n", encoding="utf-8", newline="\n")
    print(f"夹具 {len(out['entries'])} 条 -> {args.output}")
    return 0


# ---------------------------------------------------------------------------
# 日志检查
# ---------------------------------------------------------------------------

def cmd_check_log(args) -> int:
    log = Path(args.log) if args.log else run_dir_of(args.loader, args.run_dir) / "console.log"
    text = log.read_text(encoding="utf-8", errors="replace")
    if args.last:
        # 只看最近一次启动（最后一个 "Starting minecraft server" 之后）
        idx = text.rfind("Starting minecraft server")
        text = text[idx:] if idx >= 0 else text
    lines = text.splitlines()
    errors = [l for l in lines if re.search(r"/(ERROR|FATAL)\]", l)]
    exceptions = [l for l in lines if "Exception" in l and "/ERROR]" not in l]
    hotpot_warn = [l for l in lines if "/WARN]" in l and ("hotpot" in l.lower() or MOD_ID in l)]
    done = [l for l in lines if re.search(r"Done \(\d", l)]
    print(f"日志 {log}：完成启动 {len(done)} 次；ERROR/FATAL {len(errors)}；含 Exception 的非 ERROR 行 {len(exceptions)}；"
          f"火锅相关 WARN {len(hotpot_warn)}")
    for l in (errors + exceptions + hotpot_warn)[:10]:
        print("  " + l[:200])
    return 0 if not errors else 1


# ---------------------------------------------------------------------------
# 把旧档基线拿去给别的服务端升级（4D / 4E 等步骤复用）
# ---------------------------------------------------------------------------

def cmd_prepare_world(args) -> int:
    """把旧档基线原件复制进目标服务端运行目录，并（可选）放入旧版配置；不改 level-name，只打印要怎么设。"""
    source = BASE / "baseline" / f"world-{args.loader}"
    if not source.exists():
        print("找不到基线世界原件：", source, "（先 make）")
        return 1
    run_dir = Path(args.run_dir)
    if not run_dir.is_absolute():
        run_dir = ROOT / run_dir
    target = run_dir / args.name
    if target.exists():
        print("目标已存在，不覆盖：", target)
        return 1
    shutil.copytree(source, target)
    print("已复制世界：", target)
    if args.config:
        cfg = BASE / "baseline" / f"config-{args.loader}-{args.config}.json"
        dest = config_path(run_dir)
        dest.parent.mkdir(parents=True, exist_ok=True)
        if dest.exists():
            backup = dest.with_suffix(".json.before-legacy")
            shutil.copy2(dest, backup)
            print("原配置已备份：", backup)
        shutil.copy2(cfg, dest)
        print("已放入旧版配置：", dest)
    print(f"接下来：备份 {run_dir / 'server.properties'}，把 level-name 设成 {args.name}，启动该服务端，等 Done 后：")
    print(f"  python tools/legacy_baseline.py dump --properties {run_dir / 'server.properties'} -o <输出.json>")
    return 0


# ---------------------------------------------------------------------------
# 一键建档
# ---------------------------------------------------------------------------

def run_self(*argv) -> int:
    return main(list(argv))


def cmd_make(args) -> int:
    """setup -> 首次启动生成配置 -> 改配置 -> 新世界 -> build -> dump -> 保存关服 -> 复制世界 -> 重开 dump。"""
    loader = args.loader
    run_dir = run_dir_of(loader)
    world = args.world
    steps = [("setup", ["setup", loader, "--world", world])]
    for title, argv in steps:
        if run_self(*argv):
            return 1
    if not config_path(run_dir).exists():
        if run_self("start", loader, "--world", "config-warmup") or run_self("stop", loader):
            return 1
        shutil.rmtree(run_dir / "config-warmup", ignore_errors=True)
    if run_self("config", loader):
        return 1
    shutil.rmtree(run_dir / world, ignore_errors=True)
    out = BASE / "baseline"
    out.mkdir(parents=True, exist_ok=True)
    if run_self("start", loader, "--world", world):
        return 1
    if run_self("build", loader):
        run_self("stop", loader)
        return 2
    time.sleep(3)
    run_self("dump", "--loader", loader, "-o", str(out / f"dump-{loader}-built.json"), "--label", "建档后同一会话")
    run_self("stop", loader)
    pristine = out / f"world-{loader}"
    shutil.rmtree(pristine, ignore_errors=True)
    shutil.copytree(run_dir / world, pristine)
    print("世界原件已存：", pristine)
    return 0


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = parser.add_subparsers(dest="cmd", required=True)

    def loader_args(p, with_world=False):
        p.add_argument("loader", choices=sorted(LOADERS))
        p.add_argument("--run-dir")
        if with_world:
            p.add_argument("--world", default="baseline-world")

    p = sub.add_parser("setup", help="建运行目录")
    loader_args(p, True)
    p.set_defaults(func=cmd_setup)
    p = sub.add_parser("start", help="启动服务端并等到 Done")
    loader_args(p, True)
    p.add_argument("--timeout", type=float, default=420)
    p.set_defaults(func=cmd_start, world=None)
    p = sub.add_parser("stop", help="RCON save-all + stop")
    loader_args(p)
    p.set_defaults(func=cmd_stop)
    p = sub.add_parser("config", help="备份旧版生成的配置并改几个值")
    loader_args(p)
    p.set_defaults(func=cmd_config)
    p = sub.add_parser("build", help="在运行中的旧版服务端里摆好基线内容")
    loader_args(p)
    p.set_defaults(func=cmd_build)
    p = sub.add_parser("make", help="一键：建运行目录、生成并改配置、新世界、摆内容、导出、关服、保存世界原件")
    loader_args(p, True)
    p.set_defaults(func=cmd_make)
    p = sub.add_parser("dump", help="导出标准答案（要求服务端在跑）")
    p.add_argument("--loader", choices=sorted(LOADERS), default="fabric")
    p.add_argument("--properties", help="任意服务端的 server.properties（开发服也行）")
    p.add_argument("-o", "--output", required=True)
    p.add_argument("--label", default="")
    p.set_defaults(func=cmd_dump)
    p = sub.add_parser("compare", help="对比两份导出")
    p.add_argument("old")
    p.add_argument("new")
    p.add_argument("--allow", help="允许差异清单 JSON")
    p.add_argument("-v", "--verbose", action="store_true")
    p.add_argument("--text-exact", action="store_true", help="要求回执原文逐字相同（默认按内容比较，键顺序不同不算差异）")
    p.set_defaults(func=cmd_compare)
    p = sub.add_parser("fixtures", help="把导出整理成单测夹具")
    p.add_argument("dump")
    p.add_argument("-o", "--output", required=True)
    p.add_argument("--loader-label", default="fabric")
    p.set_defaults(func=cmd_fixtures)
    p = sub.add_parser("check-log", help="数日志里的 ERROR / WARN")
    p.add_argument("loader", nargs="?", choices=sorted(LOADERS), default="fabric")
    p.add_argument("--run-dir")
    p.add_argument("--log")
    p.add_argument("--last", action="store_true", help="只看最近一次启动")
    p.set_defaults(func=cmd_check_log)
    p = sub.add_parser("prepare-world", help="把旧档基线原件复制到别的服务端运行目录（升级测试用）")
    p.add_argument("loader", choices=sorted(LOADERS))
    p.add_argument("--run-dir", required=True, help="目标服务端运行目录（开发服 fabric/run-server 或试验包服务端的目录）")
    p.add_argument("--name", required=True, help="复制后的世界文件夹名，建议带步骤号，如 legacy-4d")
    p.add_argument("--config", choices=["generated", "edited"], help="顺便放入旧版配置文件")
    p.set_defaults(func=cmd_prepare_world)
    p = sub.add_parser("rcon", help="发 RCON 命令")
    p.add_argument("loader", choices=sorted(LOADERS))
    p.add_argument("--run-dir")
    p.add_argument("commands", nargs="+")
    p.set_defaults(func=cmd_rcon)
    args = parser.parse_args(argv)
    return args.func(args)


def cmd_rcon(args) -> int:
    server = Server(run_dir_of(args.loader, args.run_dir) / "server.properties")
    for command in args.commands:
        print("> " + command)
        print(server.run(command))
    return 0


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    raise SystemExit(main())
