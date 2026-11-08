# EXP-REG 实验报告：注册窗口与持有器

作者：mryt233，2026-10-30。对应《Orient框架计划书 v3.1》§4.2 与《框架API规格》§9。探针为开发期代码（`-Dhotpot.expreg` 开关，devtest 包，不进 jar）。**不改生产行为。**

## 实验方法

- 探针类：`common/.../devtest/RegProbe`、`fabric/.../devtest/FabricRegProbe`、`neoforge/.../devtest/NeoForgeRegProbe`。
- Fabric：在 `onInitialize` 最早期尝试读取注册表、用 `Supplier` 早/晚绑定，按 方块 → 方块实体类型 → 物品 顺序注册，尝试重复注册同一 id。
- NeoForge：mod 构造函数中尝试注册（预期冻结）；监听每个 `RegisterEvent` 记录注册表 key；在 BLOCK 窗口注册方块、并在 BLOCK 窗口注册一个「错窗口物品」；在 ITEM 窗口注册物品并验证早/晚 `Supplier`；重复注册单独开关（默认关闭，触发会崩启动）。
- 运行：两个开发服各跑一次，RCON 正常关服；探针方块放置后 `data get block` 验证方块实体创建。

## 结果（五问）

| 问题 | Fabric | NeoForge |
|---|---|---|
| ① 冻结前能否安全**读取** | 能；注册前查询返回不存在 | 构造函数里**读取**可以（item present=false）；**注册**抛 `IllegalStateException: Registry is already frozen` |
| ② `Supplier` 是否过早求值 | 早绑定字段：注册前 `null`、注册后正常；晚绑定（按 id 查）：注册前返回 `minecraft:air`，注册后正常 | 同左：ITEM 事件内注册前 late=`minecraft:air`，注册后正常；早绑定字段注册前后 `null → 正常` |
| ③ 不同注册表引用何时可解析 | 顺序 方块→BE→物品：BE 类型能引用刚注册的方块；物品（BlockItem）能拿到方块 | 事件顺序为 attribute…block…entity_type…item…block_entity_type…；ITEM 事件里方块已可查；BE 窗口里方块、物品均可用 |
| ④ 方块/物品/方块实体循环初始化 | 静态字段「方块 → BE 类型 → BlockItem」顺序注册可用；BE 构造函数引用后赋值的静态类型，放置后创建成功 | 同左；把探针方块放在世界里 `data get block` 返回 `id: probe_be`，方块实体正常创建 |
| ⑤ 注册失败错误信息 | **重复 id 无报错，静默覆盖**（`NO ERROR`） | 重复 id 抛 `IllegalStateException: Adding duplicate key 'ResourceKey[minecraft:item / …]' to registry`，**并导致启动失败**（事件分发异常） |

补充观察：
- NeoForge 每个注册表**依次**发 `RegisterEvent`（attribute → data_component_type → … → block → entity_type → item → … → block_entity_type → menu …），窗口确实按注册表分发。
- 「错窗口注册」在 NeoForge **不报错**（整个事件期间注册表都未冻结），所以按注册表分发只是**约定与可读性**，不是运行期强制；真正的强制发生在 `freezeData()` 之后。
- 缺失物品的注册表查询返回 `minecraft:air` 而不是 `null`，`Supplier` 早绑定字段未赋值时返回 `null`。

## 对框架的结论（回填 API 规格）

1. **不自研完整注册系统**；`RegistryRef` 取消。采用：
   - Registrar 直接返回注册对象（`block → beType → item` 的自然顺序可用）；
   - 跨模块/跨注册表的晚绑定用框架提供的薄包装 `HolderRef<T>`（内部 `Supplier` + id + `containsKey` 检查）：
     - `isPresent()`：显式检查；
     - `get()`：缺失时抛带 id 的诊断异常（不返回 AIR、不返回 null）。
2. **注册前必须做重复 id 预检**（`containsKey`）：Fabric 会静默覆盖，NeoForge 会崩启动；框架统一抛带 `registryKey + id + 模块` 的异常，两平台行为一致。
3. 生命周期按注册表分发保留（可读、可扩展到更多注册表）；但规格写明：**同一事件窗口内跨注册表注册技术上可行，框架不承诺也不依赖**；需要跨注册表引用时用 `HolderRef`。
4. 缺物品查询的 `AIR` 语义写入规格：禁止用 `get()==AIR` 判断「没注册」，统一用 `containsKey`/`HolderRef.isPresent()`。
5. 客户端初始化、运行时初始化、资源重载（③④⑤）本次未涉及，沿用现有已验证路径；P1 仍需单独验证重载入口。

## 复现

```bash
JAVA_HOME="C:/Users/10536/.jdks/jdk-21.0.12.1+1" ./gradlew :fabric:runServer -PhotpotExpreg=true
JAVA_HOME="C:/Users/10536/.jdks/jdk-21.0.12.1+1" ./gradlew :neoforge:runServer -PhotpotExpreg=true
# 重复注册崩溃实验（仅 NeoForge，会故意崩启动）：
./gradlew :neoforge:runServer -PhotpotExpreg=true -Dhotpot.expreg.duplicate=true  # 需在 gradle 里加 property 转发
```

探针默认关闭；保留在 devtest 供 P1 复测，不进发布 jar。