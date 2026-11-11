# 旧档基线与升级前后对比（第四批 4A）

作者：mryt233。mryt233 改（2026-11-11）。

## 这是什么

用已发布的 0.2.2-alpha 包（1.21.1 的 Fabric 与 NeoForge）**真跑游戏**建出来的一个旧格式存档，加上一套「导出标准答案、升级后对比」的脚本，
给第四批改存档格式的步骤（4D-1、4D-2、4E-1、4E-2 以及后面的总验收）当对照。脚本是 `tools/legacy_baseline.py`，只用 RCON 和原版命令，
不依赖开发命令 `hpt`，所以开发服、试验包服务端都能用。

## 基线里有什么（102 条查询，场景表在脚本的 `scenario()`）

锅方块（standalone）：六种汤、红汤四档辣度、带食材的锅（生食 / 熟食 / 过头豆腐 / 待领经验）、鸳鸯（太极、圆、直隔板、方心）、田字（含空边）、单边有汤的鸳鸯、空锅；
灶台上的锅（含燃料、高灶台）；蘸料台 6 个（15 种调料各一碗、全空、份数>0 但原料格空、缺 `Kinds` 的更老写法、只有原料）；
桌上餐具 4 张桌子（汤碗、蘸料碗、饮料、杯、小碟里蘸过的菜、碗沿架筷子夹菜、桌上竹筷、六种汤碗、高桌）；
箱子 / 木桶 / 潜影盒（方块形态和装进箱子后的嵌套物品形态）里的蘸料碗（含 1 种、15 种满、吃一半、14 种、空掩码、高位、0 口、无组件）、
蘸过的菜（含高位、0、未知汤底名）、六种汤碗、辣度 0～3 的红汤碗和菜、筷子夹菜、18 口打包锅（用 `/loot` 走旧版自己的掉落得到）；掉落物与盔甲架手里的物品。
另有旧版生成的配置文件（原样一份、玩家改过 10 个值的一份，含 `soup_bowl.*`、`seasoning.*`）。

没有覆盖：玩家背包（没有真玩家，物品编码与箱子相同）、刷过材质的桌子（旧包没有）、r146 才有的内容（蘸料瓶、桌面餐具的 `Facing` / `ChopsticksYaw`）、
吃下去的实际效果（用 3A 的 `common/src/test/resources/opt3/attrs-baseline.json` + 开发命令 `/hpt dumpattrs`）。

## 文件在哪

| 东西 | 位置 | 进 git |
| --- | --- | --- |
| 脚本 | `tools/legacy_baseline.py`、本说明、`tools/legacy_baseline_allow.example.json`、`tools/tests/test_legacy_baseline.py` | 是 |
| 单测夹具（标准答案，Fabric 导出；两端内容相同） | `common/src/test/resources/opt4/legacy-0.2.2-alpha/fixtures.json` | 是 |
| 旧版配置 | 同目录 `config-generated.json`（旧版原样生成）、`config-edited.json`（玩家改过） | 是 |
| 夹具自检单测 | `common/src/test/java/.../hotpot/Opt4aLegacyFixtureTest.java` | 是 |
| 基线世界原件（不要直接打开） | `build/opt4/4a/baseline/world-fabric/`、`world-neoforge/` | 否（build/） |
| 各次导出、旧版运行目录 | `build/opt4/4a/baseline/dump-*.json`、`build/opt4/4a/fabric|neoforge/` | 否 |

`fixtures.json` 每条是 `{cmd, snbt}`，物品类另有 `item:{id,count,components_snbt}`；`snbt` 是 `/data get` 的原样回执去掉前缀，可用原版 `TagParser` 读回。
夹具文件也可以直接当「标准答案」喂给 `compare`，所以 `build/` 被清掉也不丢。基线世界原件丢了可以用 `make` 重建（场景表同一份）。

## 怎么用

在 `mod/` 下：

    # 重建基线（会用 build/opt4/4a/fabric|neoforge 的旧版服务端，端口 25611/25621、25612/25622）
    python tools/legacy_baseline.py make fabric
    python tools/legacy_baseline.py make neoforge

    # 升级测试：把基线世界复制进要测的服务端目录（不覆盖已有目录），可顺手放入旧版配置
    python tools/legacy_baseline.py prepare-world fabric --run-dir fabric/run-server --name legacy-4d --config edited
    #   然后备份该目录的 server.properties，把 level-name 改成 legacy-4d，启动，等 Done，再：
    python tools/legacy_baseline.py dump --properties fabric/run-server/server.properties -o build/opt4/4d/upgraded.json
    python tools/legacy_baseline.py compare common/src/test/resources/opt4/legacy-0.2.2-alpha/fixtures.json build/opt4/4d/upgraded.json --allow 允许差异.json -v
    #   保存退出（RCON stop）再启动同一个档，再 dump 一次，两次 compare 应一致 -> “保存重开逐字一致”
    python tools/legacy_baseline.py check-log --log fabric/run-server/logs/latest.log --last

`compare` 按内容对比（键顺序不同不算差异，会另报“原文不逐字相同”的条数；要求原文逐字相同加 `--text-exact`）。
结构变了的键（如 `Base` → 汤底 ID）写进允许差异清单（写法见示例文件），清单外的差异、缺少、多出都会让命令返回非 0。
NeoForge 的旧版对照把 `fabric` 换成 `neoforge`；NeoForge 的 dump 用 NeoForge 的 `server.properties`。

## 已知限制

- 锅、蘸料台等方块实体的内容是用 `setblock {NBT}` 摆进旧版服务端、再由旧版自己存盘 / 读回 / 掉落打包，格式是旧版编码的；不是玩家手动操作出来的。
- 基线是 r145（0.2.2-alpha）的格式；用 r147 试验包实测，旧档打开后唯一的差异是桌面餐具多出 `Facing` / `ChopsticksYaw`（r146e 新增），已写进 `tools/legacy_baseline_allow_r146.json`，4D / 4E 在它的基础上加各自的规则（`--allow` 目前只收一个文件，把 rules 合并进去即可）。
- 配置文件用 `compare-config 旧.json 新.json` 对比：数值键增删 / 变化会报并返回非 0，`_` 开头的说明文字被改写只统计条数（r147 会整段改写这些说明，值不变）。
- 规则可选 `old` / `new` 字段，对旧值 / 新值整串匹配。
- 开发服不要共用别人的运行目录：升级测试用 `prepare-world` 复制出的新世界名，用完恢复 `server.properties`。


## 烹饪探针

`cook-probe` 仅限专用测试世界：会清空坐标 2030..2066、Y 4..12 的区域并改写地面与区块加载状态；禁止用于旧档升级对照副本或其他有用世界。测试完成后会解除 tick 冻结。
