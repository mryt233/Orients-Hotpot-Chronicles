# 框架 API 规格（草案 v0.2）

作者：mryt233，2026-10-30。对应《Orient框架计划书 v3.1》。**只定义接口边界与契约，不实现代码。** v0.2 按评审意见 P1-01/02/03 修订。标 🧪 的条目由 EXP-PKG / P1 实验决定。

## 1. 版本与包

| 版本 | 常量位置 | 说明 |
|---|---|---|
| Framework API | `orient.framework.api.ApiVersion.CURRENT = "0.1.0-pre"`（`framework/loader`） | **预发布**：P1–P3 期间不承诺稳定；1.0.0 只在 P3 打包实验通过后发布 |
| Content Mod | 各 `modules/<series>-<loader>` 的 mod 元数据 | 各系列独立 |
| Network Protocol | `NetworkApi.PROTOCOL_VERSION = 1`（每个 payload 可带） | 与上面两者独立 |

包根：`cn.blockforge.orient.framework.{pure,loader,game,client}`；内容：`cn.blockforge.orient.<series>`。
公开 API 只允许暴露原版类型与框架契约类型；**禁止**暴露任何内容模块实现类。

---

## 2. 依赖与可见性

```text
pure  ← loader ← game ← client
content/* → pure, loader, game, client
platform/{fabric,neoforge} → loader, game, client, 平台 API
modules/<series>-<loader> → content/<series>, platform/<loader>
```

- `loader` 不 import `game/client/content`；`game` 不 import `client/content`；`client` 不 import `content`。
- 平台层不得被内容模块 import。
- 硬校验：`check_all` 读 build.gradle 依赖图断言无环 + 方向正确。

---

## 3. framework/loader（平台能力契约）

```java
public interface Platform {
    RegistrationApi registration();
    NetworkApi network();
    MenuOpener menus();
    LootEventBridge lootEvents();      // 通用战利品修改入口；平台负责挂到自己的事件上
    java.nio.file.Path configDir();
    boolean isModLoaded(String modId);
    boolean isDevelopmentEnvironment();
}

public interface MenuOpener {
    /** 打开一个界面；只带原版 MenuProvider 语义，不含任何具体界面/内容类型。 */
    void open(ServerPlayer player, MenuProviderLike menu, BlockPos pos);
}
```

- **不提供 `installBridges(ScreenBridge, LootBridge, ContentBridge)`**（评审意见：这三个是旧版混合体，其中 Screen/Content 含火锅业务）。平台实现通过框架入口注入：
  `Framework.install(Platform)`（在 mod 初始化最早期调用一次；重复注入抛异常）。
- 业务界面（锅/灶膛/蘸料）由 `content/hotpot` 的 `HotpotScreens` 组装 `MenuProviderLike` 后调用 `Platform.menus().open(...)`；loader 不感知这些类型。
- **`LootEventBridge` 是实例能力**（评审修正）：`Framework.lootEvents()` 提供规范实例，平台实现的 `Platform.lootEvents()` 必须返回同一实例；内容登记与平台消费共用它，禁止静态旁路。
```

- **Registrar 方法只在对应注册窗口内有效**；窗口外调用 → `IllegalStateException("registration window closed: ITEM")`。
- 所有 registrar 返回已注册对象（或 🧪 持有器），**不允许**返回可变的 `Registry` 本体。
- 错误处理：注册失败必须带 `registryKey + entryId + 原因`；批量注册中途失败不得静默吞掉。
- **注册前重复 id 预检**（EXP-REG 结论）：Fabric 静默覆盖、NeoForge 崩溃启动，框架统一用 `containsKey` 预检并抛 `registryKey + id + 模块` 异常。
- **跨注册表引用**（EXP-REG 结论）：Registrar 直接返回对象，`方块 → 方块实体类型 → 物品` 的自然顺序可用；跨模块/晚绑定用框架薄包装 `HolderRef<T>`（内部 `Supplier` + id + `containsKey`），`isPresent()` 显式判断，`get()` 缺失时抛带 id 的诊断异常。**不自研完整注册系统，不引入 RegistryRef。**
- 缺物品查询返回 `minecraft:air`：禁止用 `get()==AIR` 判断「没注册」，统一 `containsKey`/`HolderRef.isPresent()`。

```java
public enum ProtocolPolicy { REQUIRED, OPTIONAL }

public interface NetworkApi {
    <T extends CustomPacketPayload> void registerS2C(Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
                                                     PayloadHandler<T> clientHandler, ProtocolPolicy policy);
    <T extends CustomPacketPayload> void registerC2S(Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
                                                     PayloadHandler<T> serverHandler, ProtocolPolicy policy);
    void sendToPlayer(ServerPlayer player, CustomPacketPayload payload);
    void sendToServer(CustomPacketPayload payload);
    boolean canSendTo(ServerPlayer player, Type<?> type);   // 未协商 = false
    int protocolVersion();                                  // 本端协议版本，握手期交换
}
```

- C2S 与 S2C **统一用 `ProtocolPolicy`**，不再一边布尔一边缺省（评审意见）。
- `required` 语义即 `ProtocolPolicy.REQUIRED`；`optional` 即 `OPTIONAL`。

- `required=true`：协议不匹配时在连接阶段**明确拒绝**（错误信息含 payload id + 双方版本）。
- `required=false`：不匹配时跳过，不踢人；对应功能降级，客户端不得假定状态一致。
- **握手阶段**：协议版本在连接建立期交换——NeoForge 用 configuration 阶段（`configurationToServer`/`configurationToClient`）或 `registrar.versioned(...)`；Fabric 无内建机制，用配置阶段自定义 payload 或首个 play 包 🧪。版本字段与 payload 组一起定义，不做全局隐式默认。
- **断线清理**：客户端断开时清空所有「服务端下发」的缓存（配置快照、协议版本、功能开关）；重连重新握手（现有 `Config.clearClient` 行为 ✅）。
- **协商不等于授权**（P1 约束 3）：每个 C2S 处理器仍须在服务端校验距离、目标方块实体类型、槽位范围、权限与状态；不得因「已协商」跳过校验。现有 `ChopsticksAimPayload` / `OpenSauceRestockPayload` 的距离与类型检查就是模板 ✅。
- **功能降级矩阵**：每个 optional payload 在规格里登记「缺失时客户端/服务端各自行为」，避免状态分歧；required payload 缺失即拒绝连接。
- 接收处理器统一在主线程执行（平台负责）。

**平台桥的归属（P1-03 修订，禁止只改包名）**：
- `framework/loader` 只放**通用平台能力**：
  - `MenuOpener { void open(ServerPlayer player, MenuProviderLike menu, BlockPos pos); }`（不包含具体界面语义）
  - `LootEventBridge`（`LootBridge` 现有签名本身就是通用的：`ResourceKey<LootTable> + LootTable.Builder + boolean builtin + HolderLookup.Provider`，可直接上移）
  - 各 Registrar（第 3 节上半）、`NetworkApi`、平台事件抽象
- **火锅业务接口留在 `content/hotpot`**：
  - `HotpotScreens { openPot(ServerPlayer, HotPotBlockEntity); openFuel(...); openSauce(...); }`——内部调用通用 `MenuOpener` 与自己的 `MenuProvider`；`openPot/openFuel/openSauce` **不得出现在 loader**
  - 火锅的菜单类型、方块实体类型、创造栏内容由 `content/hotpot` 通过通用 Registrar 声明；`ContentBridge` 不再整体上移，拆成「通用注册器 + 内容声明」
- 平台实现（`framework/platform/*`）只实现通用桥；火锅的 `MenuProvider` 组装在 `content/hotpot`，平台不 import 内容类型。

**平台事件抽象**（loader）：玩家交互、实体事件、生命周期事件的统一回调接口；不含任何业务语义。

---

## 4. framework/game（通用游戏层）

- `ConfigBase`：`Path` 由平台注入；`load()/reload()/snapshot()/acceptClientSnapshot()/clearClient()`；各模块用 `remember*` 登记默认值；错误聚合为 `List<String> warnings`。
- `Hooks`（机制）：命名钩子表，`register(name, priority, callback)` → `Registration`（可注销）；调用方按优先级执行，单钩子异常只记录并跳过。
- `ContentModule` 与各 Registrar 见第 5 节（接口定义在 `loader`，内容用 `game` 的辅助实现）。
- 通用组件 `HeldFood` 模式：组件值必须不可变、实现 `equals/hashCode`。
- `TagKeys`、`BuiltinRecipeFallback`、`PartOutline`、`HotPotHeat`、`TableDecor` 通用化。
- **Integration API（第三方）**：数据驱动优先 → `Hooks` 机制 → `CompatModule` 插件（`isModLoaded` 平台注入）。

`content/hotpot/api`（不进框架）：`SoupBaseSource`、`FuelSource`、`HeatSource`、`ExtraPotItem`、`EatIntercept`、`PreSettleNotify` 等火锅业务扩展；面条可选依赖，不强制。

---

## 5. 生命周期契约（能力模型）

```java
public interface ContentModule {
    ModuleDeclaration declaration();                       // ① 一次；不得碰注册表
    default void registerItems(ItemRegistrar r) {}         // ② ITEM 窗口
    default void registerBlocks(BlockRegistrar r) {}       // ② BLOCK
    default void registerBlockEntities(BlockEntityRegistrar r) {}
    default void registerMenus(MenuRegistrar r) {}
    default void registerComponents(ComponentRegistrar r) {}
    default void registerCreativeTab(CreativeTabRegistrar r) {}
    default void runtime(RuntimeHooks h) {}                // ③ 每进程一次；可注册可重复回调
    default void client(ClientHooks h) {}                  // ④ 客户端一次；与③无强制先后
    default void onReload(ReloadContext c) {}              // ⑤ 可重复
}

public record ModuleDeclaration(String modId, String contentNamespace,
                                Set<String> dependsOn,
                                List<ConfigKey> configSchema) {}
```

规则：
1. ① 由框架在 mod 构造期调用。**平台事实**：NeoForge 构造期读取注册表可以成功（EXP-REG ✅），注册会因冻结抛异常。**框架约束（人为规定，不是平台限制）**：声明阶段只做声明——禁止修改注册表；禁止把「尚未完成注册的对象值」作为后续注册依据；允许只读诊断查询，但读取结果不得用于决定注册顺序或内容。框架不承诺构造期读取的语义稳定。
2. ② 每个 registrar 只在其注册表窗口内被调用（NeoForge 按 `RegisterEvent` 的 registryKey 分发；Fabric 在初始化期顺序调用）。窗口外调用立即抛异常。
3. ③ 可在服务器启动/进入世界等时机调用；`RuntimeHooks` 自身保证一次注册、多次有效。
4. ④ 与 ③ 互不依赖先后；客户端入口只在 `dist=CLIENT` 加载。
5. ⑤ 资源/数据重载可重复任意次；处理器不得保留一次性状态。
6. 模块间顺序：`dependsOn` 只约束 ①–② 的调用顺序；③–⑤ 不保证模块间顺序（幂等）。

---

## 6. 错误处理与诊断

| 场景 | 行为 |
|---|---|
| 平台实现未安装（开发期） | 首次调用桥时抛 `IllegalStateException`，信息含缺哪个桥 |
| 注册窗口外调用 | 抛异常，含 registryKey 与调用方法 |
| **核心注册失败（P1-02）** | **立即终止加载**，保留原始异常链，诊断含 `registryKey + entryId + ownerModule + phase + cause`；**不允许**「部分注册后继续启动」 |
| 注册 id 冲突（框架声明冲突或 Registry 已存在） | 注册前预检即失败终止，错误含冲突双方 owner；冲突测试必须失败即停 |
| 可选第三方兼容模块异常 | 只跳过该模块并记录（`CompatRegistry` 现有行为 ✅），不得掩盖核心注册错误 |
| 可选依赖缺失 | 功能降级，`isModLoaded=false` 路径正常 |
| 必要网络协议不匹配 | 拒绝连接，错误含 payload id / 双方版本 |
| 配置坏文件 | 保留原文件 + 默认值 + 日志原因（现有四兜底 ✅） |

---

## 7. 版本兼容规则

1. Framework API：当前 **`0.1.0-pre` 预发布**，P1–P3 期间不承诺兼容；1.0.0 只在 P3 打包与多系列实验通过后发布。发布后规则：MAJOR 变更 = 删除/改签名；MINOR = 纯新增（实现默认方法）；PATCH = 修复。弃用保留 ≥1 MINOR。
2. 内容模块声明 `frameworkApiRange`：**0.1.0-pre 开发期为 `"[0.1,0.2)"`（示例，不作为稳定承诺）**；1.x 的 `"[1.0,2.0)"` 只在 1.0.0 发布后使用。加载时检查，不满足 → 明确报错（🧪 EXP-PKG 场景）。
3. 网络协议：`PROTOCOL_VERSION` 按 payload 组管理；required/optional 见 §3。
4. 存档/资源：注册 id、组件 id 是公开契约；alpha 可改名（用户规则），beta 起冻结；组件编码变更必须兼容旧数据（`HeldFood` 已保持 JSON 形状 ✅）。

---

## 8. 每个 API 的测试场景

| API | 单元测试 | 集成/回归 |
|---|---|---|
| ConfigBase | 四兜底、钳制、缺键、坏文件 | 双端 reload + 客户端快照 3→7→3 |
| Registrars | 窗口外抛异常、重复 id 报错 | 双端起服、注册数量、`/hpt` 查询 |
| 注册失败即停 | 抛异常不吞、异常链完整（registryKey/entryId/module/phase） | 故意制造冲突 → 启动必须失败并给出诊断 |
| 跨模块重复 ID | 框架声明冲突与 Registry 已存在两种都失败 | 两模块声明同 id → 启动失败、错误标出双方模块 |
| NetworkApi | codec 往返、required/optional 判定 | 真连接收发、未协商不发、协议不匹配拒绝、C2S 越权被服务端校验拦下 |
| Hooks | 排序、注销、异常隔离 | 下锅/蘸料钩子行为不变 |
| Compat | 缺失跳过、异常隔离 | 装/不装第三方 |
| ContentModule | 生命周期调用顺序 | 火锅单/面条单/同装 × 双端 |
| HolderRef | `isPresent()`、`get()` 缺失抛带 id 诊断、早绑定不因未注册返回 AIR | 跨注册表引用（方块↔物品↔BE）注册顺序变化仍可解析 |

---

## 9. 实验结论回填

1. ✅ **EXP-REG 已完成**（报告：`docs/实验_EXP-REG.md`）：按注册表分发成立；早绑定字段注册前为 null、晚绑定查询返回 AIR；重复 id Fabric 静默覆盖 / NeoForge 崩溃，必须框架预检；跨表引用用直接返回 + `HolderRef` 薄包装。取消自研 `RegistryRef`。
2. EXP-PKG：`frameworkApiRange` 元数据写法（Fabric/NeoForge 各自）待实验。
3. Fabric 协议协商：配置阶段 payload vs 首包；版本字段放置待实验。
4. 客户端/服务端重载入口的精确映射（Fabric/NeoForge 各自事件）待 P1 验证。

## 10. 修订摘要（v0.2，回应评审意见 P1-01/02/03）与 P1 可执行条件

### 10.1 本次修改
1. **P1-01**：声明阶段规则改为「框架约束」表述——构造期读取可行（EXP-REG ✅），禁止注册表修改、禁止依赖未完成注册的对象值；区分平台限制与框架人为约束。
2. **P1-02**：删除「单模块失败后继续」；核心注册失败（含 id 冲突）立即终止加载，异常链与诊断字段（registryKey/entryId/ownerModule/phase/cause）写入规格；仅可选兼容模块允许隔离。
3. **P1-03**：明确 Loader 不含火锅业务接口；`openPot/openFuel/openSauce` 移入 `content/hotpot/api/HotpotScreens`；`LootBridge` 签名通用可直接上移；`ContentBridge` 拆成通用 Registrar + 内容声明。
4. API 版本改为 `0.1.0-pre` 预发布。
5. 网络 API 增加握手阶段、降级矩阵、断线清理、「协商不等于授权」条款。
6. 测试矩阵删除 `RegistryRef`，补 `HolderRef`、注册失败即停、异常链、跨模块重复 ID 测试。

### 10.2 与现有代码的冲突核查
| 契约 | 现有代码 | 冲突/待办 |
|---|---|---|
| Loader 不含火锅业务 | `ScreenBridge` 三个方法都是火锅实体 | P1 拆出 `HotpotScreens` + 通用 `MenuOpener`；平台 `ScreenOpeners`/`NeoForgeScreenOpeners` 改为实现 `MenuOpener` |
| Loader 不含火锅业务 | `ContentBridge` 含 `HotPotScreenHandler`/`StoveScreenHandler`/`SauceStationScreenHandler` 与 6 个火锅 BE 类型 | P1 拆成通用 Registrar；火锅类型由 content 声明 |
| 通用战利品桥 | `LootBridge` 签名本身通用 | 直接上移，无冲突 |
| 注册失败即停 | Fabric 入口无 try/catch；NeoForge 事件异常已导致启动失败（EXP-REG 观察） | 框架统一包装诊断；需保证不捕获核心异常 |
| 重复 id 预检 | Fabric 静默覆盖、NeoForge 崩溃（EXP-REG ✅） | 框架注册前 `containsKey` 预检并失败终止 |
| HolderRef | 不存在 | P1 新增薄封装，语义按 §3 |

### 10.3 仍需验证的问题（不阻塞规格，阻塞对应实现）
1. `ContentBridge` 拆成 Registrar 后，菜单类型/方块实体类型在 NeoForge 各注册表窗口内的创建时机与现有静态初始化顺序是否兼容 🧪（P1 第一批验证）。
2. Fabric 配置阶段握手 payload 的可行性 🧪（P1/P3）。
3. 客户端/服务端资源重载入口映射 🧪（P1）。
4. `Hooks` 机制的单钩子异常隔离与排序语义 🧪（P1）。

### 10.4 P1 可执行条件
- 规格已按上述修订；`HolderRef` 薄封装与重复 id 预检为 P1 第一实现项；
- 第一批迁移只做「包移动 + 桥拆分」，不做玩法改动，双端回归对比 `f06898c` 基线；
- 任何注册时序问题按第 10.3 停止并回到规格，不自行扩大改动。