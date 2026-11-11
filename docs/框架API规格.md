# 框架 API 规格（v0.3）

作者：mryt233　　日期：2026-11-11　　对应 Runtime：0.2.0

v0.3 按当前代码（主工程 HEAD `6a3308d`，Minecraft 1.21.1）整体重写：描述框架现在实际提供什么，删掉设计稿阶段从未落地的接口，让读者照着本文就能写出能编译、能注册内容、能收发网络包的内容模组。

**读法**：本文只描述 `cn.blockforge.orient.framework.{pure,loader,game,client}` 与两个平台实现的实际行为。代码与本文不一致时以代码为准。标「待核实」的条目是尚未在游戏里确认的现象；标「待定事项」（附录 15.2）的是需要作者决定、本次不打算改的行为。

---

## 1. 版本与常量

### 1.1 三个版本号各自的含义

| 号 | 常量 / 位置 | 当前值 | 含义 |
|---|---|---|---|
| API 标签 | `cn.blockforge.orient.framework.loader.ApiVersion.CURRENT` | `0.1.0-pre` | 预发布 API 标签。独立于 Runtime 号，不随 Runtime 递增，表示「公开接口仍在预发布期，1.0.0 之前不承诺跨版本稳定」 |
| Runtime 号 | `ApiVersion.RUNTIME` | `0.2.0` | 前置 `orient_framework` 自身的递增版本号，是打包脚本生成内容包依赖范围的唯一来源 |
| 网络协议 | `cn.blockforge.orient.framework.loader.NetworkApi.PROTOCOL_VERSION` | `1` | 接口常量（`int`），与上面两个号独立：Fabric 由 `FrameworkHello` 携带，NeoForge 用作整批 payload registrar 的版本字符串 |
| 内容版本 | `gradle.properties` 的 `mod_version` | `1.0.0-r147` | 内容模组自己的版本，与框架解耦 |

- 三个号互相不推导：`CURRENT` 不随 `RUNTIME` 走，`RUNTIME` 也不改 `CURRENT`。0.2.0 只是 Runtime 的次版本号，API 仍是预发布标签。
- `ApiVersion.RUNTIME` 这个**字面量**只出现在 `ApiVersion.java` 与 `HolderRefTest` 的断言里；Python 工具统一经 `tools/project_env.py` 的 `runtime_version()` 读它（`build_candidates.py`、`check_final_jar.py`、`prod_client_check.py`、`prod_neo_client_check.py`、`run_matrix.py`、`tools/tests/test_bench_preview.py` 都是如此）。
- 包名是 `cn.blockforge.orient.framework.loader`，不是 `...framework.api`。

### 1.2 0.1.x 只加不删

- 0.1.x 内只新增（新类、新 default 方法、新嵌套接口），不删已有类、字段、方法，不改已有签名与行为；破坏性变更一律进 0.2.0。
- 新增接口一律给 default 实现，保证只实现了老方法的旧平台仍能实例化；未实现时抛 `UnsupportedOperationException`，信息里写明需要的 Runtime 版本（文本见第 12 章）。
- 弃用用 `@Deprecated(since = "0.1.2", forRemoval = true)` 标记，0.1.x 内不删，留到 0.2.0。这是 0.1.x 时期的规则；0.2.0 删完弃用副本后，现行 framework 里已经没有 `@Deprecated` 成员，只剩三个移植版的框架副本还能看到样本。
- 0.2.0 的实际动作只有「删弃用副本、把两个公开字段改私有」，没有改接口行为、网络协议或存档格式。

### 1.3 Runtime 版本演进

| Runtime | 加了 / 删了什么 |
|---|---|
| 0.1.0 / 0.1.1 | 框架四层落地。0.1.1 与 0.1.0-hotfix1 代码相同，只是改用递增数字版本（Fabric 与 NeoForge 对 `hotfixN` 字符串排序相反，数字两端一致） |
| 0.1.2 | 逐行整修：`FrameworkHello` 握手、`ClientItemRenderers` / `ClientNetwork` 增加 `install()` / `send()` 并弃用公开静态字段（两类的**包位置上移发生在 0.1.0**）、配置警告改语言键、`ConfigBase.setClientThreadCheck`、`RuntimeStats` 按通道缓存、`ModuleHost` 全局声明表、`tools/check_runtime_abi.py`；也是 `ApiVersion.RUNTIME` 这个常量首次出现的版本（0.1.0 / 0.1.1 时 Runtime 号写在打包脚本里） |
| 0.1.3 | `ConfigStore` 快照扁平表：`real` / `integer` / `contains` / `Snapshot` |
| 0.1.4 | `ConfigStore` 保存回执：`SaveStatus`、`Saved`、`saveEdited` |
| 0.1.5 | 六个新注册分域、`RuntimeEvents`、`Platform.events()`、`ClientContent`、`LootEventBridge` 平台接管标记、`pure` 的 `RegistrationWindow` / `RegistrationGuard.withOwner` / `DeferredRegistrations`、前置自带 10 种语言各 14 键 |
| 0.1.6 | `RuntimeEvents` 的 `onServerStarting` / `onServerStopped` / `beforeBlockBreak`；`ClientContent` 的 `cutout` / `screens` / `blockOutline`；`Platform.isDedicatedServer()`（0.1.6 新增，default 返回 false）及两端真实实现 |
| 0.2.0（当前） | 删除框架侧 `client.GuiKit` 弃用副本；`ClientItemRenderers.registrar`、`ClientNetwork.sender` 改私有。现存接口行为、协议、存档格式不变 |

弃用与已删接口的完整清单见附录 15.1。

---

## 2. 分层与依赖

### 2.1 模块

| Gradle 模块 | 包根 | 职责 |
|---|---|---|
| `framework:pure` | `...framework.pure` | 不依赖 Minecraft（只 `compileOnly` gson）：`ConfigStore`、`NumericConfig`、`ConfigWarning`、`RegistrationGuard`、`RegistrationWindow`、`DeferredRegistrations` |
| `framework:loader` | `...framework.loader` | 平台能力契约与生命周期：`Platform`、`Framework`、`RegistrationApi`、`NetworkApi`、`PayloadHandler`、`ProtocolPolicy`、`MenuOpener`、`MenuProviderLike`、`LootEventBridge`、`HolderRef`、`RuntimeEvents`、`RuntimeHooks`、`ClientHooks`、`ContentModule`、`ModuleDeclaration`、`ModuleHost`、`RuntimeStats`、`ApiVersion`；资源带 `assets/orient_framework/lang/` 10 份语言文件 |
| `framework:game` | `...framework.game` | 通用游戏层：`ConfigBase`、`Hooks`、`CompatModule`、`CompatRunner`、`PartOutline` |
| `framework:client` | `...framework.client` | 仅客户端：`ClientContent`、`ClientItemRenderers`、`ClientNetwork`、`PartOutlineDrawing` |
| `common` | `cn.blockforge.generated.orientshotpotchronicles` | 火锅内容（过渡态，见 2.4） |
| `fabric` / `neoforge` | `...platform.fabric` / `...platform.neoforge` | 两端框架平台实现 + 火锅业务接线 + 打包入口 |
| `experiments:server-fabric` / `experiments:server-neoforge` | — | EXP-PKG 空服务端，只在 `run/mods` 里加载真实 jar 验证解析 |

### 2.2 允许依赖表

`tools/check_architecture.py` 的允许表（逐项与脚本一致）：

| 模块 | 允许依赖 |
|---|---|
| `framework:pure` | （无） |
| `framework:loader` | `framework:pure` |
| `framework:game` | `framework:pure`、`framework:loader` |
| `framework:client` | `framework:pure`、`framework:loader`、`framework:game` |
| `common` | 框架四层（pure / loader / game / client） |
| `fabric` / `neoforge` | `common` + 框架四层 |
| `experiments:server-fabric` / `experiments:server-neoforge` | （无） |

同一脚本还执行两条源码级禁令，外加一条依赖图检查：

1. `framework/**/*.java` 不得 import `cn.blockforge.generated`、`net.fabricmc.`、`net.neoforged.`、`net.minecraftforge.`。
2. `common/src/main/java`、`framework/loader/src/main/java`、`framework/game/src/main/java` 不得 import 客户端类（`...orientshotpotchronicles.client.`、`...framework.client.`、`net.minecraft.client.`）。
3. 依赖图检查（读各模块 `build.gradle`）：不得成环；依赖不在 `settings.gradle` 里、或不在允许表里，都算失败。

`tools/check_all.py` 第 1 项「分层」检查 `common` 不 import 加载器，第 2 项「架构」直接调 `check_architecture.py --strict`。`check_all.py` 默认跑 10 项（9 项源码检查 + 第 10 项「中间产物」静态结构检查）；`--source-only` 时只跑 9 项；显式加 `--jar` 时第 10 项换成最终单包验收。

### 2.3 对内容侧的实际含义

- 内容模块只 import 框架契约（`loader` / `game` / `client`）与 Minecraft 原版类型；不得直接 import 平台实现 `...platform.fabric` / `...platform.neoforge`。唯一的例外是开发环境回退安装的两个内容侧类（Fabric 的 `generated...platform.FabricPlatform`、NeoForge 的 `...neoforge.NeoForgePlatform`），它们只在 `Framework.isInstalled()` 为 false 且非生产环境时调用平台安装。
- `loader` 不 import `game` / `client` / 内容；`game` 不 import `client` / 内容；`client` 不 import 内容。
- 平台实现可以 import `common`，火锅的业务接线就在 `fabric` / `neoforge` 里。

### 2.4 `common` 是 content/hotpot 的过渡态

允许表里写的是 `common`，脚本注释写明「过渡：content/hotpot」；仓库根的 `content/` 只是 `items.json`、`materials.json`、`generated_manifest.json` 这类数据，不是 Gradle 模块。规格早先写的 `content/*` 内容层与 `modules/<series>-<loader>` 目录都没有落地——现在没有这两个目录。多系列拆分现状见附录 15.2 第 9 条。

### 2.5 框架 Runtime 的入口

| 端 | 入口类 | 位置 | 说明 |
|---|---|---|---|
| Fabric | `FabricFrameworkRuntime`（`ModInitializer`，mod id `orient_framework`） | `fabric/src/main/java/...platform/fabric/` | 只调 `FabricFrameworkPlatform.install()` |
| NeoForge | `NeoForgeFrameworkRuntime`（`@Mod("orient_framework")`） | `neoforge/src/runtime/java/...platform/neoforge/` | 单独源集，不参与开发环境的 mod 扫描（否则会被当成内容模组的悬空入口点），只由打包脚本编译进 Runtime jar |

两个平台类的 `install()` 都是幂等安装：已装过直接返回；`Framework.isInstalled()` 为 false 时才 `Framework.install(platform)`。开发运行没有独立前置时由内容侧回退安装同一平台。

---

## 3. loader 契约

### 3.1 `Platform`

平台能力契约，不含任何内容业务。内容侧只通过 `Framework.platform()` 拿到它。

| 方法 | 说明 |
|---|---|
| `RegistrationApi registration()` | 分域注册入口 |
| `RuntimeEvents events()` | **default**：未实现时抛 `UnsupportedOperationException("runtime events require Runtime 0.1.5")` |
| `NetworkApi network()` | 网络入口 |
| `MenuOpener menus()` | 打开界面 |
| `LootEventBridge lootEvents()` | 必须返回 `Framework.lootEvents()` 的同一实例 |
| `Path configDir()` | 配置目录 |
| `boolean isModLoaded(String modId)` | 第三方模组在场查询 |
| `boolean isDevelopmentEnvironment()` | 开发环境 |
| `boolean isDedicatedServer()` | **default 返回 `false`**，两端实现用加载器环境覆盖 |

两个 default 方法的降级语义不同：`events()` 缺失是**抛异常**（一调就失败），`isDedicatedServer()` 缺失是**静默 `false`**（专用服会被当成客户端）。后者的影响与前置条件见第 11 章 L-13。

### 3.2 `Framework`

| 成员 | 说明 |
|---|---|
| `static LootEventBridge lootEvents()` | 规范战利品桥实例，内容登记与平台消费都用它 |
| `static void install(Platform implementation)` | 平台在初始化最早期调用一次；重复调用抛 `IllegalStateException("Platform already installed")`；成功后记一次 `RuntimeStats.platformInstalled` |
| `static Platform platform()` | 取平台；未安装抛 `IllegalStateException("Platform not installed")`（不含缺哪个桥） |
| `static boolean isInstalled()` | 开发期查询，不抛异常 |

没有 `installBridges(...)` 之类的旧入口；`ScreenBridge`、`ContentBridge`、`ScreenOpeners`、`NeoForgeScreenOpeners` 早已拆完删除，**本工程 framework 内**不存在（`mod2/` 是另一个项目，另有同名类）。

### 3.3 `MenuOpener` / `MenuProviderLike`

```java
@FunctionalInterface
public interface MenuOpener {
    void open(ServerPlayer player, MenuProviderLike menu, BlockPos pos);
}

public interface MenuProviderLike {
    Component title();
    AbstractContainerMenu createMenu(int syncId, Inventory inventory, Player player);
}
```

- `pos` 由平台编解码到客户端：Fabric 走 `ExtendedScreenHandlerFactory<BlockPos>` 的 `getScreenOpeningData`，NeoForge 走 `player.openMenu(provider, pos)`。菜单不需要额外数据时也要给一个有效坐标。
- 具体界面（锅 / 灶膛 / 蘸料台）由内容侧 `hotpot.api.HotpotScreens` 组装 `MenuProviderLike` 后调 `Framework.platform().menus().open(...)`；loader 不认识这些类型（`HotpotScreens` 只有 `openPot` / `openFuel` / `openSauce` 三个公开方法）。

### 3.4 `LootEventBridge`

实例能力，`Framework.lootEvents()` 提供唯一规范实例。

| 成员 | 说明 |
|---|---|
| `interface Modification` | `void apply(ResourceKey<LootTable> key, LootTable.Builder builder, boolean builtin, HolderLookup.Provider registries)` |
| `void register(Modification)` | 内容侧登记；内部 `CopyOnWriteArrayList` |
| `List<Modification> all()` | 平台侧遍历 |
| `boolean isPlatformManaged()` / `void markPlatformManaged()` | 平台挂上原生事件后标记；旧内容侧接线曾据此跳过自己的通用消费段。3D 已把火锅的两份旧消费段删掉，现在只有开发探针还读它 |

两端拿到的东西不一样：Fabric 给的是真正的 `LootTable.Builder`（可 `modifyPools`），NeoForge 只支持追加池（`withPool`）。见第 11 章 L-06。

### 3.5 `HolderRef<T>`

跨注册表引用的薄封装（不自研注册系统，不引入 `RegistryRef`）。

| 成员 | 说明 |
|---|---|
| `static <T> HolderRef<T> of(String id, BooleanSupplier present, Supplier<T> supplier)` | 构造 |
| `String id()` | 诊断用 id |
| `boolean isPresent()` | 是否存在 |
| `T get()` | 缺失抛 `IllegalStateException("unresolved holder: " + id)`；supplier 返回 null 抛 `IllegalStateException("holder resolved to null: " + id)`。**不返回 AIR / null** |

缺物品查询返回 `minecraft:air`，禁止用 `get() == AIR` 判断「没注册」，统一用 `containsKey` 或 `HolderRef.isPresent()`。

### 3.6 `RuntimeStats`

行为计数与摘要，开发期诊断用，不依赖加载器或客户端类。

| 成员 | 说明 |
|---|---|
| `static RuntimeStats get()` | 单例 |
| `setSink(Consumer<String>)` / `setProcess(String)` / `process()` | 平台设置日志出口与进程名（`client` / `server`） |
| `runtimeInit()` / `initCount()` / `installCount()` | 框架 Runtime 初始化、平台安装次数 |
| `platformInstalled(String implementation)` | 由 `Framework.install` 调用 |
| `modulePhase(String moduleId, String phase)` | `ModuleHost` 记 `declaration` / `register` / `runtime` / `client` / `reload` |
| `channel(CustomPacketPayload.Type<?>)` | 取 `ChannelStats`（首次创建后缓存） |
| `count(String)` / `summary()` / `logSummary(String trigger)` | 计数查询与摘要输出 |

嵌套类型 2 个：枚举 `Direction { S2C, C2S }` 与静态终态类 `ChannelStats`。`ChannelStats` 按方向分别记 `registered(direction, policyName)`、`sent`、`handled`、`skipped`。摘要行格式固定：

```text
[ORIENT-FW] summary trigger=<trigger>
[ORIENT-FW] process=<p> platform=<类名> init=<n> installs=<n>
[ORIENT-FW] module process=<p> id=<模块> declaration=<n> register=<n> runtime=<n> client=<n> reload=<n>
[ORIENT-FW] net process=<p> channel=<通道> direction=<S2C|C2S> registered=<n> policy=<REQUIRED|OPTIONAL|?> sent=<n> handled=<n> skipped=<n>
```

---

## 4. 注册

### 4.1 `RegistrationApi` 全部分域

13 个分域：7 个 0.1.0 起就有（items / blocks / blockEntities / menus / components / creativeTabs / entityTypes），6 个 0.1.5 加（recipeSerializers / recipeTypes / criteria / armorMaterials / sounds / particles）。后 6 个是 **default 方法**，旧平台没实现时抛 `UnsupportedOperationException("... requires Runtime 0.1.5")`。

| 分域方法 | `register` 签名 | 返回 | Fabric 落地 | NeoForge 落地 |
|---|---|---|---|---|
| `items()` | `<T extends Item> T register(ResourceLocation id, T item)` | 传入的对象 | `BuiltInRegistries.ITEM` | `Registries.ITEM` 队列 |
| `blocks()` | `<T extends Block> T register(ResourceLocation id, T block)` | 传入的对象 | `BuiltInRegistries.BLOCK` | `Registries.BLOCK` 队列 |
| `blockEntities()` | `<T extends BlockEntity> BlockEntityType<T> register(ResourceLocation id, BlockEntityFactory<T> factory, Block... blocks)` | 新建的 `BlockEntityType` | `BuiltInRegistries.BLOCK_ENTITY_TYPE` | `Registries.BLOCK_ENTITY_TYPE` 队列 |
| `menus()` | `<T extends AbstractContainerMenu> MenuType<T> register(ResourceLocation id, MenuFactory<T> factory)` | 新建的 `MenuType` | `BuiltInRegistries.MENU`，`ExtendedScreenHandlerType` + `BlockPos.STREAM_CODEC` | `Registries.MENU` 队列，`IMenuTypeExtension.create` |
| `components()` | `<T> DataComponentType<T> register(ResourceLocation id, DataComponentType<T> type)` | 传入的对象 | `BuiltInRegistries.DATA_COMPONENT_TYPE` | `Registries.DATA_COMPONENT_TYPE` 队列 |
| `creativeTabs()` | `CreativeModeTab register(ResourceLocation id, Component title, Supplier<ItemStack> icon, CreativeTabContent content)` | 新建的 `CreativeModeTab` | `BuiltInRegistries.CREATIVE_MODE_TAB`，`FabricItemGroup.builder()` | `Registries.CREATIVE_MODE_TAB` 队列，`CreativeModeTab.builder(Row.TOP, 0)` |
| `entityTypes()` | `<T extends Entity> EntityType<T> register(ResourceLocation id, EntityType<T> type)` | 传入的对象 | `BuiltInRegistries.ENTITY_TYPE` | `Registries.ENTITY_TYPE` 队列 |
| `recipeSerializers()` | `<T extends RecipeSerializer<?>> T register(ResourceLocation id, T value)` | 传入的对象 | `BuiltInRegistries.RECIPE_SERIALIZER` | `Registries.RECIPE_SERIALIZER` 队列 |
| `recipeTypes()` | `<T extends RecipeType<?>> T register(ResourceLocation id, T value)` | 传入的对象 | `BuiltInRegistries.RECIPE_TYPE` | `Registries.RECIPE_TYPE` 队列 |
| `criteria()` | `<T extends CriterionTrigger<?>> T register(ResourceLocation id, T value)` | 传入的对象 | `BuiltInRegistries.TRIGGER_TYPES` | `Registries.TRIGGER_TYPE`（落地写入 `TRIGGER_TYPES`） |
| `armorMaterials()` | `Holder<ArmorMaterial> register(ResourceLocation id, ArmorMaterial value)` | **Fabric 真实 `Holder`；NeoForge `DeferredHolder`** | `Registry.registerForHolder` | `Registries.ARMOR_MATERIAL` 队列 |
| `sounds()` | `SoundEvent register(ResourceLocation id, SoundEvent value)` | 传入的对象 | `BuiltInRegistries.SOUND_EVENT` | `Registries.SOUND_EVENT` 队列 |
| `particles()` | `<T extends ParticleType<?>> T register(ResourceLocation id, T value)` | 传入的对象 | `BuiltInRegistries.PARTICLE_TYPE` | `Registries.PARTICLE_TYPE` 队列 |

嵌套类型 16 个：`ItemRegistrar`、`BlockRegistrar`、`BlockEntityRegistrar`、`BlockEntityFactory`、`MenuRegistrar`、`MenuFactory`、`ComponentRegistrar`、`CreativeTabRegistrar`、`CreativeTabContent`、`EntityRegistrar`、`RecipeSerializerRegistrar`、`RecipeTypeRegistrar`、`CriterionRegistrar`、`ArmorMaterialRegistrar`、`SoundRegistrar`、`ParticleRegistrar`。

```java
@FunctionalInterface interface BlockEntityFactory<T extends BlockEntity> { T create(BlockPos pos, BlockState state); }
@FunctionalInterface interface MenuFactory<T extends AbstractContainerMenu> { T create(int syncId, Inventory inventory, BlockPos pos); }
@FunctionalInterface interface CreativeTabContent { void fill(CreativeModeTab.ItemDisplayParameters params, CreativeModeTab.Output output); }
```

内容侧用法（火锅 `HotPotContent.register(RegistrationApi)`）：注册阶段只调这些分域方法，把结果赋给已有的 `public static` 字段（字段类型不变，注册前为 `null`），不要在注册之前缓存对象。

### 4.2 窗口模型

「窗口」两端不是一回事：

| | Fabric | NeoForge |
|---|---|---|
| 窗口粒度 | **全局一次性**：`RegistrationWindow.closeAll()` | **逐表**：每张表的 `RegisterEvent` 落地后 `close(<registryKey>)` |
| 关窗点 | `FabricFrameworkPlatform.install()` 里注册的 `ServerLifecycleEvents.SERVER_STARTING`（服务端）、`FabricClientNetworkHandlers.installLifecycle()` 里的 `ClientLifecycleEvents.CLIENT_STARTED`（客户端） | `NeoForgeRegistrationApi.onRegisterEvent`：每个 `Registries.*` 表 flush 完立刻关 |
| 可注册期 | 从 mod 初始化一直到「服务端即将开始 / 客户端已启动」 | 只在对应 `RegisterEvent` 之前；内容侧实际用的是模组构造期的第一个 `RegisterEvent` 解冻窗口 |
| 关窗后再登记 | `RegistrationWindow.claim` 抛 `phase=closed` 异常；网络没有窗口检查 | 同样抛异常；网络另有 `payload registration window already closed: <id>（请在模组构造 / 注册阶段登记）` |
| 重复 id 预检 | 登记前 `registry.containsKey(id)` 加内存去重表 | 入队时同样预检；flush 时再查一次注册表（覆盖入队后别的模组抢占 id 的情况） |

- Fabric `closeAll()` 幂等：集成服再触发一次没有副作用。
- Fabric 关窗选在这两个事件，代码注释的说法是「首个引擎启动事件在全部内容初始化之后」；这两个事件相对其他模组初始化的实际先后**未实测**（见 15.3）。
- 内容侧跨表引用（方块 → 方块实体类型 → 物品）只能按自然顺序拿已返回的对象；需要晚绑定时用 `HolderRef`。

### 4.3 失败诊断

所有注册失败都带四个字段：`registry`、`id`、`owner`、`phase`。`owner` 默认是 id 的命名空间；走 `ModuleHost.registerAll()` 时由 `RegistrationGuard.withOwner(模块名, ...)` 换成模块名（支持嵌套，异常后恢复原值）。

| 场景 | 异常类型 | 真实消息（逐字） |
|---|---|---|
| 窗口已关 | `IllegalStateException` | `registration failed: registry=<registryKey>, id=<id>, owner=<owner>, phase=closed (registration window already closed)` |
| 内存去重表里已有同一 id | 同上 | `registration failed: registry=<key>, id=<id>, owner=<owner>, phase=declare`，cause 为 `duplicate registration, previous owner=<前一个 owner 或 registry>` |
| 注册表里已有该 id（NeoForge flush 时） | 同上 | `registration failed: registry=<key>, id=<id>, owner=<owner>, phase=register`，cause 为 `duplicate registration: registry=<key>, id=<id>, owner=<owner>` |
| 实际写入注册表抛异常 | 同上 | `registration failed: registry=<key>, id=<id>, owner=<owner>, phase=register`，cause 为原始异常 |
| NeoForge 入队值为 null（只有走 `requireValue` 的 7 个取值型分域：recipeSerializers / recipeTypes / criteria / armorMaterials / sounds / particles 与护甲材质） | 同上 | `registration failed: registry=<key>, id=<id>, owner=<owner>, phase=declare`，cause 为 `NullPointerException: null entry`；items / blocks 这类直接入队的分域要到 flush 时才失败，`phase=register` |

`phase` 的取值：`closed`、`declare`（登记阶段）、`register`（落地阶段）。`closed` 分支才会在消息末尾追加 ` (registration window already closed)`。核心注册失败一律立即终止加载，不保留「部分注册后继续启动」。

### 4.4 关于 `minecraft:air`

只有原版的**默认注册表**（`BuiltInRegistries.ITEM` 这类 `DefaultedRegistry`）在 id 缺失时回退到默认值 `minecraft:air`；普通 `Registry.get` 返回 `null`。判断「没注册」必须用 `containsKey`（火锅 `HotPotCompat.itemOf` 自 3F 起就是 `BuiltInRegistries.ITEM.containsKey(id) ? get(id) : null`），或用 `HolderRef.isPresent()`。

---

## 5. 生命周期

### 5.1 `ContentModule`

实际就 5 个方法：2 个抽象（`declaration` / `register`）+ 3 个 default（`runtime` / `client` / `onReload`）：

```java
public interface ContentModule {
    ModuleDeclaration declaration();
    void register(RegistrationApi registration);
    default void runtime(RuntimeHooks hooks) {}
    default void client(ClientHooks hooks) {}
    default void onReload() {}
}
```

- 没有 `registerItems` / `registerBlocks` 之类的分注册表方法，也没有 `onReload(ReloadContext)`（`ReloadContext` 类型不存在）——都是早期设计稿，代码里从来没有。
- `onReload()` 无参、可被任意次调用；处理器不得保留一次性状态。

### 5.2 `ModuleDeclaration`

```java
public record ModuleDeclaration(String modId, String contentNamespace,
                                Set<String> dependsOn, List<String> configFiles) {
    // 紧凑构造器：dependsOn = Set.copyOf(...)，configFiles = List.copyOf(...)
}
```

- `configFiles` 是 `List<String>`，不是 `List<ConfigKey>`（`ConfigKey` 类型不存在）。**本仓库代码里没有读取方**（`mod/`、`mod2/`、`version-ports/` 检索只命中声明本身与文档），保留还是删除见附录 15.2 第 1 条。
- `contentNamespace` 目前也没有读取方。
- 是公开 record：新增字段会改规范构造器签名，旧内容二进制可能直接 `new ModuleDeclaration(...)`，所以只能在主版本变更时动。

### 5.3 `ModuleHost`

内容包真正调用的宿主类。一个模组建一个宿主，所有宿主共用一份**静态全局声明表**（`GLOBAL`，`ConcurrentHashMap.newKeySet()`）。

| 成员 | 说明 |
|---|---|
| `ModuleHost(List<ContentModule> modules)` | 构造时逐个调 `declaration()`，只采集一次并缓存；按 `dependsOn` 拓扑排序；缺依赖 / 重复 id / 环一律抛异常即停。全局表的 id 在**排序成功之后**才写入，失败的宿主不占 id |
| `static boolean isDeclared(String moduleId)` | 某模块是否已在任意宿主里声明（跨模组可选联动查询用） |
| `List<ModuleDeclaration> declarations()` | 声明缓存（只读副本），**按声明顺序**；拓扑序只有 `order()` |
| `List<String> order()` | 排序后的模块 id |
| `void registerAll()` | 按 `ordered` 顺序，在每个模块外包一层 `RegistrationGuard.withOwner(模块名, ...)`，内部调 `module.register(Framework.platform().registration())` |
| `void runtime(RuntimeHooks hooks)` | 按 `ordered` 顺序调 `module.runtime(hooks)` |
| `void client(ClientHooks hooks)` | 按 `ordered` 顺序调 `module.client(hooks)` |
| `void reload()` | 可重复调用任意次，按 `ordered` 顺序调 `module.onReload()` |

异常文本（逐字）：

| 场景 | 消息 |
|---|---|
| 声明缺 modId | `content module declaration missing modId` |
| 同宿主或跨宿主重复 id | `duplicate content module id: <id>` |
| 依赖环 | `content module dependency cycle at <id>` |
| 依赖不存在（本宿主和更早建的宿主里都没有） | `missing content module dependency: <id> -> <dep> (not declared in this host or any host created earlier)` |

顺序规则：**四个阶段（register / runtime / client / reload）全部按 `dependsOn` 拓扑排序遍历**，不是只有前两个阶段有序；同宿主内两个模块没有依赖关系时保持声明顺序。

### 5.4 实际调用时机归谁

框架只负责「按顺序遍历并转发」，**具体时机由内容侧用加载器原生事件接进来**。这是本版最容易被误读的一点。

| 阶段 | 火锅现在的接法（Fabric） | 火锅现在的接法（NeoForge） | 归属 |
|---|---|---|---|
| ① 声明 | `GeneratedMod.onInitialize()` 里首次引用 `HOST` 触发类初始化，`ModuleHost` 构造器里调 `declaration()` | 构造器只挂 `RegisterEvent` 监听；`HOST` 的首次引用在事件回调里，所以声明与注册在**同一时刻**（第一个 `RegisterEvent` 到达时）发生 | 内容侧（类初始化时机由加载器入口决定） |
| ② 注册 | `GeneratedMod.onInitialize()` → `HOST.registerAll()` | `HotPotNeoForge` 构造器里挂 `RegisterEvent` 监听，第一个事件到达时 `HOST.registerAll()` | 内容侧触发，框架实现落表 |
| ③ 运行时 | `HOST.runtime(callback -> ServerLifecycleEvents.SERVER_STARTED.register(server -> callback.run()))` | `HOST.runtime(callback -> NeoForge.EVENT_BUS.addListener((ServerStartedEvent e) -> callback.run()))` | 内容侧 |
| ④ 客户端 | `ClientModInitializer`：`HOST.client(Runnable::run)` | `FMLClientSetupEvent` + `event.enqueueWork(() -> HotPotNeoForge.clientHooks(Runnable::run))` | 内容侧 |
| ⑤ 重载 | `ResourceManagerHelper(PackType.SERVER_DATA)` 的 `SimpleSynchronousResourceReloadListener` → `HOST.reload()` | `AddReloadListenerEvent` 加一个 `SimplePreparableReloadListener` → `HOST.reload()` | 内容侧 |

`RuntimeHooks` / `ClientHooks` 都是只有一个抽象方法的接口（**没有** `@FunctionalInterface` 标注），框架里**没有任何实现类**，两端都由内容侧直接传 lambda；框架不管时机（规格 v0.2 写的「RuntimeHooks 自身保证一次注册、多次有效」不成立）：

```java
public interface RuntimeHooks {
    default RuntimeEvents events() { return Framework.platform().events(); }
    void onServerStarted(Runnable callback);
}

public interface ClientHooks {
    void onClientSetup(Runnable callback);
}
```

- `RuntimeHooks.onServerStarted` 只接收一个 `Runnable`：火锅两端都把它接到各自的原生「服务端已启动」事件上，`Runnable::run` 表示「立即执行」（客户端入口用）。
- `ClientHooks.onClientSetup` 同样只有 `Runnable`；服务端类里不要 import 客户端类型。
- 声明阶段的框架约束：声明只做声明，不得修改注册表，不得把「尚未完成注册的对象值」当作后续注册依据；只读诊断查询允许，但结果不得用于决定注册顺序或内容。这条**只有约定、代码里没有检查**（`ModuleDeclaration` 的类注释写「不产生注册副作用」）。

---

## 6. 运行期事件

### 6.1 `RuntimeEvents` 全集

由 `RuntimeHooks.events()`（转发 `Platform.events()`）取得。

| 方法 | 起始版本 | 签名 |
|---|---|---|
| `onCommands(CommandRegistration)` | 0.1.5 | `void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context, Commands.CommandSelection selection)` |
| `onPlayerJoin(Consumer<ServerPlayer>)` | 0.1.5 | |
| `onPlayerLeave(Consumer<ServerPlayer>)` | 0.1.5 | |
| `onServerStopping(Consumer<MinecraftServer>)` | 0.1.5 | |
| `flammable(Block, int encouragement, int flammability)` | 0.1.5 | 语义同原版 `FireBlock`：引燃难度、燃烧速度 |
| `onServerStarting(Consumer<MinecraftServer>)` | 0.1.6（default，未实现抛 `server starting requires Runtime 0.1.6`） | |
| `onServerStopped(Consumer<MinecraftServer>)` | 0.1.6（default，`server stopped requires Runtime 0.1.6`） | |
| `beforeBlockBreak(BlockBreak)` | 0.1.6（default，`block breaking requires Runtime 0.1.6`） | |

```java
@FunctionalInterface
interface BlockBreak {
    /** 返回 true 取消原版破坏；回调在方块移除之前执行。 */
    boolean cancel(Level level, Player player, BlockPos pos, BlockState state, BlockEntity entity);
}
```

**没有**实体事件、通用玩家交互事件、方块放置 / 使用事件。`RuntimeEvents` 全库只有上面这些方法。

### 6.2 两端事件映射

| 能力 | Fabric 原生事件 | NeoForge 原生事件 | 差异 |
|---|---|---|---|
| 命令 | `CommandRegistrationCallback.EVENT` | `RegisterCommandsEvent` | 一致 |
| 玩家进 | `ServerPlayConnectionEvents.JOIN`（连接层） | `PlayerEvent.PlayerLoggedInEvent`（实体进世界） | 时机不同，见 L-08 |
| 玩家出 | `ServerPlayConnectionEvents.DISCONNECT` | `PlayerEvent.PlayerLoggedOutEvent` | 同上 |
| 停止中 | `ServerLifecycleEvents.SERVER_STOPPING` | `ServerStoppingEvent` | 一致 |
| 服务端启动中 | `ServerLifecycleEvents.SERVER_STARTING` | `ServerStartingEvent` | 一致 |
| 服务端已停 | `ServerLifecycleEvents.SERVER_STOPPED` | `ServerStoppedEvent` | 一致 |
| 破坏前 | `PlayerBlockBreakEvents.BEFORE`（直接给 BlockEntity） | `BlockEvent.BreakEvent`（`world.getBlockEntity(pos)` 现场重查） | 实体参数可能为 null，见 L-09 |
| 可燃 | `FlammableBlockRegistry.getDefaultInstance().add(...)` | `((FireBlock) Blocks.FIRE).setFlammable(...)` | 机制不同，见 L-10 |

- `flammable` 两端都对负数抛 `IllegalArgumentException("negative fire value")`，对 null 方块抛 NPE。
- `beforeBlockBreak` 的回调返回 `true` 表示取消破坏；Fabric 侧包成 `!cancel(...)` 交给原版事件，NeoForge 侧包成 `e.setCanceled(true)`。

---

## 7. 网络

### 7.1 `NetworkApi`

```java
public interface NetworkApi {
    int PROTOCOL_VERSION = 1;
    <T extends CustomPacketPayload> void registerS2C(CustomPacketPayload.Type<T> type,
        StreamCodec<? super RegistryFriendlyByteBuf, T> codec, PayloadHandler<T> clientHandler,
        ProtocolPolicy policy);
    <T extends CustomPacketPayload> void registerC2S(CustomPacketPayload.Type<T> type,
        StreamCodec<? super RegistryFriendlyByteBuf, T> codec, PayloadHandler<T> serverHandler,
        ProtocolPolicy policy);
    void sendToPlayer(ServerPlayer player, CustomPacketPayload payload);
    void sendToServer(CustomPacketPayload payload);
    boolean canSendTo(ServerPlayer player, CustomPacketPayload.Type<?> type);
    int protocolVersion();
}

public enum ProtocolPolicy { REQUIRED, OPTIONAL }

@FunctionalInterface
public interface PayloadHandler<T extends CustomPacketPayload> {
    void handle(T payload, Context context);
    interface Context { Object player(); }
}
```

- `PROTOCOL_VERSION` 是 `NetworkApi` 上的**接口常量**，全局单值 `1`；两个平台实现都直接引用它，没有各自的私有字面量，也**没有按通道 / 按 payload 组定版本**的机制。
- `canSendTo` 的 `type` 参数是 `CustomPacketPayload.Type<?>`。
- `PayloadHandler.Context.player()` 返回 `Object`：服务端是 `ServerPlayer`；客户端是客户端玩家，取自 `Minecraft.getInstance().player`，**断线或没进世界时为 null**。平台实现负责具体类型，内容侧自己转型（火锅的 C2S 处理器写 `(ServerPlayer) context.player()`）。

### 7.2 两个平台的落地与协商

| | Fabric | NeoForge |
|---|---|---|
| 通道落地 | `registerS2C/C2S` 立即 `PayloadTypeRegistry.playS2C()/playC2S().register(...)`，**没有窗口检查** | 先入队；`RegisterPayloadHandlersEvent` 时统一落地，之后 `closed = true`，再登记抛 `IllegalStateException` |
| 重复通道 | 去重键 = `通道 id + "\|" + 方向`，重复只 `LOGGER.warn("[ORIENT-FW] duplicate channel registration ignored: {} {}", direction, id)` 并忽略 | 同样的去重键，重复 `LOGGER.warn("[ORIENT-FW] duplicate channel registration ignored: {}", id)` |
| REQUIRED 的拒绝时机 | 玩家 **JOIN 之后**，服务端发 `orient_framework:hello`（S2C），客户端核对失败就带原因断开 | 交给 NeoForge registrar 版本协商，**连接阶段**拒绝 |
| 版本字段 | `FrameworkHello(version, required)`：`version` 是 `PROTOCOL_VERSION`，`required` 是全部 REQUIRED 通道 id（排序后发送） | 整批 registrar 共用一个版本字符串 `Integer.toString(PROTOCOL_VERSION)` |
| OPTIONAL | 客户端注册了才发；`canSendTo` 为 false 时 `sendToPlayer` 直接 `skipped` 计数并返回 | 走 `registrar.optional()` |
| 主线程 | 接收处理器由 `ServerPlayNetworking.registerGlobalReceiver` / `ClientPlayNetworking.registerGlobalReceiver` 在主线程执行 | `PayloadRegistrar.playToClient/playToServer` 默认 `HandlerThread.MAIN`，框架没有调用 `executesOn(NETWORK)` |

Fabric 握手细节（`FrameworkHello`）：

- 服务端在 JOIN 时先看 `required` 集合：为空就**根本不发握手**。非空时再 `ServerPlayNetworking.canSend(player, FrameworkHello.TYPE)`；对端没有这个通道（没装前置，或 0.1.1 及更早）就**跳过检查、不踢人**，只记一条 `[ORIENT-FW] ... cannot receive the framework handshake; skipping channel check` 的 debug 日志。
- `required` 集合按通道 id 去重：同一 id 的 S2C 与 C2S 只算一条。客户端核对时只看「这个 id 有没有在任一方向登记」，不区分方向。
- 客户端核对两条：协议版本不等 → 断开原因 `orient_framework.disconnect.protocol`；缺必需通道 → `orient_framework.disconnect.missing_channels`。两条都用 `Component.translatableWithFallback`；键在**前置 Runtime 自带的** `assets/orient_framework/lang/` 10 份语言文件里（内容包另有一份副本），没有翻译时显示英文兜底。
- 断开原因文案（**代码里的兜底英文**，逐字；前置语言文件里这两条键的文案是 `Please install the same mod version as the server.`，少一个 `s`，两处都按现状保留）：
  - `Orient Framework network protocol mismatch (server %s, client %s). Install the same mod versions as the server.`
  - `This server requires network channels your client does not have: %s. Install the same mod versions as the server.`

### 7.3 降级与授权

- **协商不等于授权**：每个 C2S 处理器仍须自行校验距离、目标方块实体类型、槽位范围、权限与状态。火锅三个通道的做法可作模板：`ChopsticksAimPayload.handle` 校验 `slot < 0 || slot >= 81`（9×9）、`distanceToSqr(pos.getCenter()) > 64`（约 8 格）、`hasChunkAt(pos)`、方块实体类型；`OpenSauceRestockPayload.handle` 校验距离、区块与 `SauceStationBlockEntity` 类型。
- 发送前必须检查 `canSendTo(player, type)`；`sendToPlayer` 内部也会检查并计一次 `skipped`。
- `sendToServer` 只在客户端可用：两端都抛 `IllegalStateException("sendToServer is client-only")`。
- 火锅现有三个通道（内容侧登记）：`config`（S2C，OPTIONAL）、`chopsticks_aim`（C2S，REQUIRED）、`open_sauce_restock`（C2S，REQUIRED）。

### 7.4 断线清理归谁

框架只提供客户端断开回调 `ClientContent.onDisconnect(Runnable)`，**框架内没有任何清协议版本 / 功能开关的代码**；`game` 层虽然有一个 `ConfigBase.clearClient()`，但框架不会自动调用它。真正的清理是内容侧接的：火锅 `HotPotClientContent.install()` 里 `client.onDisconnect(Config::clearClient)`。

---

## 8. 客户端层

### 8.1 `ClientContent`

客户端能力入口，只有 `client` 层引用它。`ClientContent.get()` 取实现（未安装抛 `IllegalStateException("client content platform not installed")`），`ClientContent.install(ClientContent)` 由平台安装（已装过别的实现抛 `IllegalStateException("client content platform already installed")`，同一实例重复安装无副作用）。

| 方法 | 起始版本 | 说明 |
|---|---|---|
| `menu(MenuType<M>, ScreenFactory<M,S>)` | 0.1.5 | 注册界面工厂；Fabric `MenuScreens.register`，NeoForge `RegisterMenuScreensEvent.register` |
| `blockEntityRenderer(BlockEntityType<T>, BlockEntityRendererProvider<T>)` | 0.1.5 | 方块实体渲染器（NeoForge 走 `EntityRenderersEvent.RegisterRenderers`） |
| `entityRenderer(EntityType<T>, EntityRendererProvider<T>)` | 0.1.5 | 实体渲染器 |
| `tooltip(Tooltip)` | 0.1.5 | 物品提示；Fabric `ItemTooltipCallback`，NeoForge `ItemTooltipEvent` |
| `armorModels(ArmorModel, Item... items)` | 0.1.5 | 护甲模型；模型使用物品原有 `ArmorMaterial` 的贴图层。**姿势与槽位可见性的归属两端不同**：Fabric 由平台手动复刻（`copyPropertiesTo` + `setAllVisible(false)` + 按槽位设可见 + 逐层贴图），NeoForge 只注册 `IClientItemExtensions.getHumanoidArmorModel`，其余交给原版渲染器（一致性见 15.3 第 11 条） |
| `onDisconnect(Runnable)` | 0.1.5 | 客户端断线回调 |
| `cutout(Block... blocks)` | 0.1.6（default 抛 `cutout requires Runtime 0.1.6`） | 渲染层 cutout；Fabric `BlockRenderLayerMap.INSTANCE.putBlocks`，NeoForge `ItemBlockRenderTypes.setRenderLayer` |
| `screens(ScreenInit)` | 0.1.6（`screen events require Runtime 0.1.6`） | 屏幕生命周期挂钩 |
| `blockOutline(BlockOutline)` | 0.1.6（`block outline requires Runtime 0.1.6`） | 自定义方块选中框 |

7 个公开嵌套类型（另有一个私有单例持有类 `Installed`，持 `private static volatile ClientContent instance`）：

```java
@FunctionalInterface interface ScreenFactory<M extends AbstractContainerMenu, S extends Screen & MenuAccess<M>> {
    S create(M menu, Inventory inventory, Component title); }
@FunctionalInterface interface Tooltip {
    void append(ItemStack stack, Item.TooltipContext context, TooltipFlag flag, List<Component> lines); }
@FunctionalInterface interface ArmorModel {
    HumanoidModel<?> model(LivingEntity entity, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> original); }
@FunctionalInterface interface Scroll { boolean allow(double x, double y, double delta); }
record ScreenHooks(Runnable beforeRender, Scroll scroll) { }
@FunctionalInterface interface ScreenInit {
    ScreenHooks init(Screen screen, Consumer<AbstractWidget> add); }
@FunctionalInterface interface BlockOutline {
    boolean render(BlockState state, BlockPos pos, double cameraX, double cameraY, double cameraZ,
                   PoseStack pose, VertexConsumer vertices); }
```

- `ScreenHooks` 可以为 null（表示这个屏幕不需要挂钩）；`screens` 的 `initializer` 在 `AFTER_INIT` / `ScreenEvent.Init.Post` 时调用。
- `BlockOutline.render` 返回 `true` 表示替代原版轮廓（Fabric 侧实现取反交给事件）。
- `armorModels` 只接受 `ArmorItem`：空数组抛 `IllegalArgumentException("no armor items")`，非护甲物品抛 `IllegalArgumentException("armor item required: " + item)`。
- 内容侧要在 `ContentModule.client(hooks)` 里取 `ClientContent.get()`，但**不能把客户端 import 放进服务端类**：火锅的做法是 `common/src/client` 里一个 `HotPotClientContent.install()`，`ContentModule.client()` 里在 `hooks.onClientSetup(...)` 的**回调内**用 `Class.forName(...).getMethod("install").invoke(null)` 反射进入（失败包成 `IllegalStateException("hotpot client initialization failed")`）。

### 8.2 客户端登记的落地时机

- **Fabric**：调用即登记（mod 初始化期）；菜单、渲染器都是立即写注册表。
- **NeoForge**：只有 `menu` / `blockEntityRenderer` / `entityRenderer` / `armorModels` 走三个 `DeferredRegistrations` 队列（菜单 / 渲染器 / 客户端扩展），原生事件到达时绑定并重放全部已登记项；`tooltip` / `onDisconnect` / `screens` / `blockOutline` 直接挂 `NeoForge.EVENT_BUS`，`cutout` 立即调 `ItemBlockRenderTypes.setRenderLayer`。事件已到之后再登记也会立即执行。内容侧还要等 `FMLClientSetupEvent` + `enqueueWork`，所以同一段内容代码在两端「什么时候安全」不同（见 L-05）。

### 8.3 `ClientItemRenderers` / `ClientNetwork` / `PartOutlineDrawing`

```java
public final class ClientItemRenderers {
    @FunctionalInterface public interface Renderer {
        void render(ItemStack stack, ItemDisplayContext context, PoseStack pose,
                    MultiBufferSource buffers, int light, int overlay); }
    public static void install(BiConsumer<Item, Renderer> platformRegistrar);
    public static void register(Item item, Renderer renderer);
}

public final class ClientNetwork {
    public static void install(Consumer<CustomPacketPayload> send);
    public static void send(CustomPacketPayload payload);
}
```

- 0.2.0 起 `ClientItemRenderers` 的私有字段 `registrar`、`ClientNetwork` 的私有字段 `sender` 都是 `private static volatile`，初始值是抛异常的空实现（`Client item renderer platform not installed` / `Client network platform not installed`）；内容侧只能用 `install` / `register` / `send`。
- 动态物品渲染的平台适配由框架装：Fabric 用 `BuiltinItemRendererRegistry`，NeoForge 用 `IClientItemExtensions` + `BlockEntityWithoutLevelRenderer`（复用扩展事件队列，不依赖原生事件先后）。
- `PartOutlineDrawing.render(BlockState, BlockPos, double x, double y, double z, PoseStack, VertexConsumer)` 是「一格里几样东西」的选中框共同画法：方块实现 `game.PartOutline` 且 `aimedPart` 返回非空形状时画那一部分的黑框，返回 `true` 表示替代原版轮廓；否则返回 `false`。

---

## 9. game 层

### 9.1 `ConfigBase`

配置门面：服务端 / 客户端快照隔离与线程判定；文件格式、校验、读写都在 `pure.ConfigStore`。

| 成员 | 说明 |
|---|---|
| `ConfigBase(ConfigStore store)` | 唯一构造器，内容侧自己 new 一个门面并持有内核 |
| `static void setClientThreadCheck(BooleanSupplier check)` | 平台客户端入口调用一次，写入 `private static volatile` 的**全进程唯一**判定；专用服不调用（默认恒 `false`）。多个 `ConfigBase` 实例共享这一份判定 |
| `void init(Path directory, boolean dedicated)` | 记下 dedicated，`store.init(directory)` 后 `load()` |
| `void load()` | 首次读取 |
| `ConfigStore.Loaded reload()` | 重新读文件并换服务端快照 |
| `void setServer(MinecraftServer value)` | 设服务端；传 null 时把服务端快照重置为默认值，非 null 时 `store.reload()` + `store.saveDefaultsIfNeeded()` |
| `JsonObject current()` | 当前线程该用的快照：专用服恒服务端快照；否则服务端在跑、且（当前是服务端主线程 `isSameThread()`，或当前不是客户端主线程）时读服务端快照，其余读客户端快照 |
| `JsonObject serverSnapshot()` | 服务端快照的深拷贝 |
| `void acceptServer(JsonObject data)` | **注意：写的是客户端快照**，内部调 `store.acceptClient(data)`（参数名校验后落到客户端） |
| `void clearClient()` | 客户端快照清回默认值 |
| `ConfigStore store()` | 底层内核 |

规格 v0.2 写的 `snapshot()` / `acceptClientSnapshot()` 不存在。

### 9.2 `Hooks<T>`

通用钩子容器：优先级排序（**大者先**，同级按登记顺序）、可注销、单钩子异常隔离。

| 成员 | 说明 |
|---|---|
| `interface Registration { void unregister(); }` | 注销句柄，重复调用无副作用 |
| `Registration add(T listener)` | 优先级 0 |
| `Registration add(int priority, T listener)` | 指定优先级 |
| `boolean remove(T listener)` | 按引用移除（`entry.listener() == listener`） |
| `List<T> all()` | 已排序的不可变快照，热路径直接遍历（不复制不排序） |
| `int size()` / `boolean isEmpty()` | |
| `void forEach(Consumer<T> action, BiConsumer<T, Throwable> onError)` | 依次调用；单钩子抛错只交给 `onError` 并继续 |

补充：`add` / `remove` 都对 `this` 加锁，`all()` 只读 `volatile` 快照；`Registration.unregister()` 无返回值；`forEach` 不判 `onError` 为空，传 null 时单钩子抛错会变成 NPE、原异常丢失。

没有命名钩子表，没有 `register(name, priority, callback)`——名字参数从来不存在。异常隔离要调用方自己传 `onError`。

### 9.3 `CompatModule` / `CompatRunner`

```java
public interface CompatModule {
    default String name() { return modIds().isEmpty() ? "compat" : modIds().get(0); }
    List<String> modIds();
    void register();
}

public final class CompatRunner {
    public static int run(List<CompatModule> modules, Predicate<String> isModLoaded,
                          BiConsumer<String, Throwable> warn);
}
```

- `run` 对每个模块取第一个在场的 mod id；都不在场就跳过；在场就 `register()`，成功计入返回值；单个模块抛错只 `warn.accept(module.name() + "（" + hit + "）", error)` 并继续下一个。
- 注入点由内容 / 平台决定：火锅用 `HotPotCompat.install(Predicate<String>)`（`HotPotRuntime` 里传 `platform::isModLoaded`），框架层只收 `run(modules, isModLoaded, warn)` 的三个参数。框架里没有 `CompatRegistry`（那是火锅自己的模块清单）。
- 注入**不在 Runtime install 期**：火锅在 `events.onServerStarting` 回调里用 `compatInitialized` 守卫只跑一次（`HotPotCompat.install(...)` + `CompatRegistry.init()`），时机是全部注册完成、冻结之前。

### 9.4 `PartOutline`

```java
public interface PartOutline {
    VoxelShape aimedPart(BlockState state, BlockGetter world, BlockPos pos, Player player);
}
```

方块实现它 + 客户端注册 `ClientContent.blockOutline(PartOutlineDrawing::render)`，准星对着哪一样就只框哪一样；返回 `null` 或空形状都表示照原版框整个形状（`PartOutlineDrawing` 判 `shape == null || shape.isEmpty()`）。配套的 `PartOutlineDrawing` 在 client 层（8.3）。

---

## 10. pure 层

### 10.1 `ConfigStore`

不依赖 Minecraft 的配置内核（只 `compileOnly` gson）。每内容模块一个实例，状态互不共享。

```java
public ConfigStore(String fileName, int format, long maxBytes, String note,
                   NumericConfig.Rules rules, Supplier<JsonObject> fieldHelp, Consumer<String> warn);
```

参数含义：`fileName` 是配置目录下的文件名；`format` 写进 `_format`；`maxBytes` 是 `too_large` 阈值；`note` 写进 `_说明`；`rules` 是逐路径校验规则；`fieldHelp` 写进 `_字段说明`；`warn` 收下所有英文警告文本。

| 成员 | 说明 |
|---|---|
| `enum SaveStatus { WRITTEN, MEMORY_ONLY, FAILED }` | 保存回执状态 |
| `record Saved(Loaded loaded, SaveStatus status, String reason, Path backup)` | `saveEdited` 的返回 |
| `record Loaded(JsonObject data, List<ConfigWarning> details)` | 一次读取结果；框架产出的 `data` 是独立副本，但 record 的规范构造器只 `List.copyOf(details)`、不防御拷贝 `data`，约定调用方只读 |
| `Loaded.warnings()` | 英文文本列表（`details.stream().map(ConfigWarning::text)`），保留给 0.1.1 时期编译的旧内容包；0.2.0 **未收紧、仍然保留**，见 15.2 第 6 条。新代码用 `details()` |
| `void init(Path directory)` | 记下文件路径（`directory.resolve(fileName)`） |
| `JsonObject defaults()` | 默认值表（可变，内容侧注册默认值时用） |
| `void load()` | 首次读取；默认值表为空时抛 `IllegalStateException("必须先注册数值默认值，再加载配置")`；读取成功才置「已加载」，失败可重试 |
| `Loaded reload()` | 未 `init` 抛 `IllegalStateException("配置目录未初始化：先调用 init(directory)")`；读文件、换服务端快照、把每条警告交给 `warn` |
| `Loaded read(Path file)` | 直接读某个文件（文件不存在返回默认值副本） |
| `Loaded parse(String text)` | 用 `NumericConfig` 解析 |
| `boolean isValidDocument(String text)` | 文档是否合法 |
| `JsonObject serverData()` / `JsonObject clientData()` | 当前快照（只读，不要改） |
| `double real(JsonObject snapshot, String path, double fallback)` | 扁平路径取数，一次哈希查找；认不出的副本退回逐层遍历 |
| `int integer(JsonObject snapshot, String path, int fallback)` | 同上取整（截断方向与原 `getAsInt()` 一致） |
| `boolean contains(JsonObject snapshot, String path)` | 这一项在不在（不看类型） |
| `void resetServerToDefaults()` | 服务端快照回默认值 |
| `void replaceServer(JsonObject data)` | 换服务端快照（传入会深拷贝） |
| `void acceptClient(JsonObject data)` | 客户端快照 = 解析后的传入数据 |
| `void clearClient()` | 客户端快照回默认值 |
| `Saved saveEdited(JsonObject edited)` | 设置界面保存。先确认原文件可保留：非常规文件 / 读取失败 / 超过 `maxBytes` / 不是合法文档 → 备份成 `<文件名>.broken-<yyyyMMdd-HHmmss-SSS>[-n]`（后两种）；保留失败则**不改内存快照**并返回 `FAILED`，warn `Could not preserve original config: <消息>`。成功路径输出 `merge(merge(defaults, existing), edited)` 并重写 `_format` / `_说明` / `_字段说明`；写盘失败降级 `MEMORY_ONLY`，warn `Could not write config (<reason>); changes applied in memory only`；未 `init` 时 `reason` 是 `Config directory is not initialized` 且不 warn |
| `void saveDefaultsIfNeeded()` | 补全有效文件，保留用户值与未知键；无效文档不重写 |
| `static JsonObject merge(JsonObject defaults, JsonObject input)` | 递归填充缺项，不覆盖已有值 |
| `static JsonObject read(JsonObject root, String... path)` | 逐层取对象 |
| `static JsonObject make(JsonObject root, String... path)` | 逐层建对象 |
| `static int intAt(JsonObject node, String key, int fallback)` | |
| `static float floatAt(JsonObject node, String key, float fallback)` | |

关键语义：

- 树与「路径 → 数值」扁平表**成对换引用**（`volatile Snapshot`），单人游戏里集成服线程与渲染线程并发读不会读到「新树配旧表」。
- 客户端快照加载后即为默认值（不是空对象）。
- 写入统一 `writeAtomically`（临时文件 + `ATOMIC_MOVE`，不支持时回退普通 move）。
- `Loaded.warnings()` 的英文文本是「0.1.1 时期旧内容包」的二进制兼容入口，见附录 15.2 第 6 条。

### 10.2 `NumericConfig`

纯数值解析器：不访问游戏、加载器或文件。

```java
record Rule(BigDecimal min, BigDecimal max, boolean integer)   // 另有 Rule(double,double,boolean) 构造器
interface Rules { Rule forPath(String path); }
record Result(JsonObject data, List<ConfigWarning> warnings, boolean validDocument)
static Result parse(String text, JsonObject defaults, Rules rules, int format)
```

校验顺序与警告（`ConfigWarning.PREFIX = "orient_framework.config."`，键名即后缀）：

| 键 | 英文兜底（逐字） | 触发条件 |
|---|---|---|
| `json_invalid` | `Config is not valid JSON; using defaults, original file kept` | 解析失败 |
| `root_not_object` | `Config root must be an object; using defaults, original file kept` | 根不是对象 |
| `format_unsupported` | `Unsupported config format version; using defaults, original file kept` | `_format` 不是数字，或整数值与期望不符 |
| `format_invalid` | `Invalid config format version; using defaults, original file kept` | `_format` 取整数值时抛异常（小数、超出 int 范围等） |
| `unknown_key` | `Unknown config entry %s, ignored` | 默认值表里没有这个键（忽略，不加进结果） |
| `not_object` | `%s must be an object; default kept` | 该是对象却不是 |
| `not_number` | `%s must be a number; default used` | 不是数字 |
| `not_integer` | `%s must be a whole number; default used`（前置语言文件里这条键写的是 `%s must be an integer; default used`，措辞不同，按现状保留） | 整数规则却是小数 |
| `out_of_range` | `%s must be between %s and %s; default used` | 越界（两端包含） |

- 另有 3 条警告由 `ConfigStore` 自己产生（不在上表里）：`too_large`（`Config file is too large; using defaults, original file kept`）、`read_failed`（`Could not read config (%s); using defaults, original file kept`）、`write_failed`（`Could not write config (%s); original file kept`）。
- `_format` **缺失**时完全不校验格式；`json_invalid` / `root_not_object` / `format_unsupported` / `format_invalid` 返回默认值副本且 `validDocument=false`，而 `unknown_key` / `not_*` / `out_of_range` 只回退该项、仍是 `validDocument=true`。
- `out_of_range` 的两个边界参数经 `stripTrailingZeros().toPlainString()` 格式化后填入。
- 以 `_` 开头的键（`_format`、`_说明`、`_字段说明`）在每一层都被跳过。
- 默认值表里有、但没有校验规则的字段，抛 `IllegalStateException("配置字段缺少校验规则：" + path)`。
- 单测覆盖：范围端点包含、`7.0` 这类整数的十进制写法可接受、数字代替对象保留整个默认对象、坏 JSON / 非对象根、未知 `_format` 不应用任何用户值、缺规则报字段路径、结果与默认值互相独立。

### 10.3 `ConfigWarning`

```java
public record ConfigWarning(String key, String fallback, List<String> args) {
    public static final String PREFIX = "orient_framework.config.";
    public String text();   // 用英文兜底按 %s 顺序填入，日志用
}
```

- 包级私有工厂 `static ConfigWarning of(String name, String fallback, Object... args)`：key 自动加 `PREFIX`，参数一律 `String.valueOf`。
- `toString()` 返回 `text()`。
- 纯内核不碰 Minecraft；上层用 `Component.translatableWithFallback(key, fallback, args...)` 按玩家语言显示（火锅 `Config.describe`）。
- 前置自带 `assets/orient_framework/lang/` 10 种语言各 14 个键（12 个配置警告 + 2 个断开原因）；内容包里的同名副本仍保留。

### 10.4 `RegistrationGuard` / `RegistrationWindow`

```java
public final class RegistrationGuard {
    public static String owner(String fallback);
    public static void withOwner(String owner, Runnable action);        // ThreadLocal，支持嵌套，finally 恢复
    public static IllegalStateException failure(String registry, String id, String owner,
                                                String phase, Throwable cause);
    public static void requireAbsent(boolean present, String registryKey, String id, String ownerModule);
}

public final class RegistrationWindow {
    public synchronized void claim(String registry, String id, String owner, boolean present);
    public synchronized void close(String registry);
    public synchronized void closeAll();
}
```

- `RegistrationWindow` 不引用游戏类：已关注册表 / 全局关窗 → `phase=closed`；内存去重表已占位或 `present` 为真 → `phase=declare`，cause 是 `IllegalStateException("duplicate registration, previous owner=" + (previous == null ? "registry" : previous))`。内部去重键是 `registry + "/" + id`。
- `failure(...)` 在 `phase=closed` 时消息末尾追加 ` (registration window already closed)`，并把 `cause` 挂成原因。
- `requireAbsent` 抛 `duplicate registration: registry=<key>, id=<id>, owner=<ownerModule>`。
- `RegistrationGuard.owner(id.getNamespace())` 是「绕过 ModuleHost 直接注册」时的回退归属。

### 10.5 `DeferredRegistrations<E>`

兼容「内容回调先到、加载器事件后到」的顺序问题；NeoForge 客户端三类登记用它，后续同类事件可重放。

| 成员 | 说明 |
|---|---|
| `synchronized void add(Consumer<E> registration)` | 事件已到就立即执行一次，然后入队（供后续事件重放） |
| `synchronized void bind(E event)` | 记下当前事件（存为 `current`），对 `List.copyOf(registrations)` 快照按登记顺序逐个执行；bind 执行期间新增的登记不在本次执行，但之后 `add` 会立刻用这个 `current` 执行一次 |

`add` / `bind` 都 `Objects.requireNonNull`；单测还覆盖「执行时抛错不被吞」。

---

## 11. 两个加载器的行为差异（L-01～L-13）

同一份内容代码在两端要注意的地方。全部按 HEAD `6a3308d` 的代码核对。

| 编号 | 项目 | Fabric | NeoForge | 对内容侧的影响 |
|---|---|---|---|---|
| L-01 | 注册窗口 | `FabricRegistrationApi.closeRegistration()` → `RegistrationWindow.closeAll()`；关窗点在 `FabricFrameworkPlatform.install()` 注册的 `SERVER_STARTING`（服务端）与 `FabricClientNetworkHandlers.installLifecycle()` 的 `CLIENT_STARTED`（客户端） | `NeoForgeRegistrationApi` 每张表 `flushAndClose` 后 `close(key)`，分派见 `onRegisterEvent` | Fabric 可注册期一直延续到「服务端即将开始 / 客户端已启动」；NeoForge 在 `RegisterEvent` 期间就关完。规格 v0.2 的「每个注册表窗口」只在 NeoForge 成立 |
| L-02 | 创造栏排位 | `FabricItemGroup.builder()`，不指定 row / column，交给 Fabric API 的分页逻辑重排（非原版组从第 2 页起、每页 10 个、上下两行） | `CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)`（坐标写死） | 玩家两端看到的组位置不同；目前内容侧没有任何手段指定排位，平台也没暴露 `tabsBefore` / `tabsAfter`。Fabric 落在第 2 页是读 Fabric API 源码推的，**未实机核对**；NeoForge `TOP,0` 是否与原版第一格冲突**未核实**（本机没有 patched 的 `net/minecraft` 源码） |
| L-03 | REQUIRED 拒绝时机 | 玩家 JOIN 后发 `orient_framework:hello`（S2C），客户端校验失败才断开；`required` 集合为空时**根本不发握手、也不校验协议版本** | 交给 registrar 版本协商，连接阶段就拒绝 | 「协议不匹配」的体验不同：Fabric 是进服后掉线，NeoForge 是连不上。Fabric 对收不到握手的旧客户端**放行**（`canSend` 为 false 时跳过检查） |
| L-04 | 通道晚注册 | 立即落地，**没有窗口检查**；重复只 warn | `RegisterPayloadHandlersEvent` 之后抛 `IllegalStateException("payload registration window already closed: <id>（请在模组构造 / 注册阶段登记）")`，带 id | 同样写错时机，Fabric 静默、NeoForge 崩 |
| L-05 | 客户端登记落地时机 | `FabricClientContent`：调用即注册（mod 初始化期） | `NeoForgeClientContent`：三个 `DeferredRegistrations` 队列，事件到了才落地；内容侧还要等 `FMLClientSetupEvent` + `enqueueWork` | 同一段内容代码在两端「什么时候安全」不同 |
| L-06 | 战利品桥的能力 | `FabricLootEvents` 用 `source.isBuiltin()`；给的是真正的 `LootTable.Builder`，可 `modifyPools` | `NeoForgeLootEvents` 用 `"minecraft".equals(name.getNamespace())` 自算 `builtin`；给的是 `PoolCollector extends LootTable.Builder`，**只 override 了 `withPool`** | 数据包改写过的 `minecraft:` 表两端 `builtin` 判定可能相反，直接影响 `GarnishLoot` / `OrangeLoot` / `TripeLoot` 里 `if (!builtin) return` 的分支；NeoForge 侧只能用 `withPool` 追加池 |
| L-07 | `armorMaterials` 返回值 | `Registry.registerForHolder` 立即返回真实 `Holder`，可 `value()` | 排队后返回 `NeoForge` 的 `DeferredHolder.create(...)`；据 NeoForge 的 `DeferredHolder` 语义，**注册落地前读 `value()` 会抛**（具体异常与文本未在本仓库内确认，见 15.3） | 内容侧不要在对象构造期缓存 `Holder.value()`；火锅只保存 `Holder`、构造围裙时不读 |
| L-08 | 玩家进 / 出事件 | `ServerPlayConnectionEvents.JOIN` / `DISCONNECT`（连接层） | `PlayerEvent.PlayerLoggedInEvent` / `PlayerLoggedOutEvent`（实体进 / 出世界） | `onPlayerJoin` 里发配置包的时机两端不同 |
| L-09 | `beforeBlockBreak` 的实体参数 | 直接用事件给的 `BlockEntity` | `world.getBlockEntity(pos)` 现场重查，且先判 `e.getLevel() instanceof Level` | 回调里拿到的实体可能是 null 或已移除 |
| L-10 | `flammable` 机制 | `FlammableBlockRegistry.getDefaultInstance().add(...)`（独立表） | `((FireBlock) Blocks.FIRE).setFlammable(...)`（直接改原版 `FireBlock` 的 map） | 生效时机与可重复调用语义不同；NeoForge 侧若 `Blocks.FIRE` 被替换会 `ClassCastException` |
| L-11 | 客户端断线摘要触发次数 | `ClientPlayConnectionEvents.DISCONNECT`，只在真正断开时打一次 | `NeoForgeClientEvents` 的 `ClientPlayerNetworkEvent.LoggingOut` **不过滤玩家 / 连接**，进世界前的空连接也会打一次（注意 `NeoForgeClientContent.onDisconnect` 的内容回调是过滤过的：`e.getPlayer() != null && e.getConnection() != null`） | 「单次发包 / 各处理一次」的行为核对基线两端不等价 |
| L-12 | 服务端停止摘要时机 | `SERVER_STOPPED`（已停） | `ServerStoppingEvent`（正在停） | 同名指标时机不同，两次摘要的内容可能不一样 |
| L-13 | `isDedicatedServer()` 的降级风险 | 真实判断 `getEnvironmentType() == EnvType.SERVER` | 真实判断 `FMLEnvironment.dist.isDedicatedServer()` | 接口默认实现是 `false`：只装旧 Runtime 或第三方 `Platform` 时专用服会被判成客户端。受影响的内容调用点是 `Config.init(platform.configDir(), platform.isDedicatedServer())`（dedicated 参数决定 `current()` 走哪份快照）；`MinecraftServer.isDedicatedServer()` 不受影响 |

---

## 12. 错误与诊断

### 12.1 按代码原样抄的错误文本

| 出处 | 消息（逐字） |
|---|---|
| `Framework.install` 重复安装 | `Platform already installed` |
| `Framework.platform()` 未安装 | `Platform not installed` |
| `Platform.events()` 默认实现 | `runtime events require Runtime 0.1.5` |
| `RuntimeEvents.onServerStarting` 默认实现 | `server starting requires Runtime 0.1.6` |
| `RuntimeEvents.onServerStopped` 默认实现 | `server stopped requires Runtime 0.1.6` |
| `RuntimeEvents.beforeBlockBreak` 默认实现 | `block breaking requires Runtime 0.1.6` |
| `RegistrationApi.recipeSerializers()` 默认实现 | `recipeSerializers requires Runtime 0.1.5` |
| `RegistrationApi.recipeTypes()` 默认实现 | `recipeTypes requires Runtime 0.1.5` |
| `RegistrationApi.criteria()` 默认实现 | `criteria requires Runtime 0.1.5` |
| `RegistrationApi.armorMaterials()` 默认实现 | `armorMaterials requires Runtime 0.1.5` |
| `RegistrationApi.sounds()` 默认实现 | `sounds requires Runtime 0.1.5` |
| `RegistrationApi.particles()` 默认实现 | `particles requires Runtime 0.1.5` |
| `ClientContent.get()` 未安装 | `client content platform not installed` |
| `ClientContent.install()` 已被别的实现占用 | `client content platform already installed` |
| `ClientContent.cutout` 默认实现 | `cutout requires Runtime 0.1.6` |
| `ClientContent.screens` 默认实现 | `screen events require Runtime 0.1.6` |
| `ClientContent.blockOutline` 默认实现 | `block outline requires Runtime 0.1.6` |
| `ClientItemRenderers.register` 平台未安装 | `Client item renderer platform not installed` |
| `ClientNetwork.send` 平台未安装 | `Client network platform not installed` |
| `FrameworkHello` 协议不一致（**英文兜底**，10 种语言各有译文，键 `orient_framework.disconnect.protocol`） | `Orient Framework network protocol mismatch (server %s, client %s). Install the same mod versions as the server.` |
| `FrameworkHello` 缺通道（**英文兜底**，键 `orient_framework.disconnect.missing_channels`） | `This server requires network channels your client does not have: %s. Install the same mod versions as the server.` |
| `NeoForgeNetworkApi.queue` 关窗后登记 | `payload registration window already closed: <通道 id>（请在模组构造 / 注册阶段登记）` |
| `FabricNetworkApi.sendToServer` / `NeoForgeNetworkApi.sendToServer` 在服务端调用 | `sendToServer is client-only` |
| `RuntimeEvents.flammable` 负数 | `IllegalArgumentException: negative fire value` |
| `ClientContent.armorModels` 空数组 | `IllegalArgumentException: no armor items` |
| `ClientContent.armorModels` 非护甲物品 | `IllegalArgumentException: armor item required: <item>` |
| `ConfigStore.load` 默认值表为空 | `必须先注册数值默认值，再加载配置` |
| `ConfigStore.reload` 未 init | `配置目录未初始化：先调用 init(directory)` |
| `ConfigStore.saveEdited` 路径不是常规文件 | `IOException: Config path is not a regular file: <path>` |
| `ConfigStore.saveEdited` 无法保留原文件（warn） | `Could not preserve original config: <消息>` |
| `ConfigStore.saveEdited` 写盘失败（warn） | `Could not write config (<消息>); changes applied in memory only` |
| `ConfigStore.backupBrokenDocument` 备份成功（warn） | `Invalid config backed up to <备份路径>` |
| `NumericConfig.overlay` 缺规则 | `IllegalStateException: 配置字段缺少校验规则：<path>` |
| `HolderRef.get` 缺失 | `unresolved holder: <id>` |
| `HolderRef.get` 解析为 null | `holder resolved to null: <id>` |
| `ModuleHost` 缺 modId | `content module declaration missing modId` |
| `ModuleHost` 重复 id | `duplicate content module id: <id>` |
| `ModuleHost` 依赖环 | `content module dependency cycle at <id>` |
| `ModuleHost` 缺依赖 | `missing content module dependency: <id> -> <dep> (not declared in this host or any host created earlier)` |
| `RegistrationGuard.requireAbsent` | `duplicate registration: registry=<key>, id=<id>, owner=<owner>` |
| 注册失败（统一格式） | `registration failed: registry=<key>, id=<id>, owner=<owner>, phase=<phase>`（`phase=closed` 时末尾追加 ` (registration window already closed)`） |
| NeoForge 空值入队 | 同上，`phase=declare`，cause `NullPointerException: null entry` |
| 内容侧回退安装，生产环境缺前置（Fabric） | `IllegalStateException: orient_framework runtime is required in production` |
| 内容侧回退安装，生产环境缺前置（NeoForge） | 同上（同一条文本） |

### 12.2 诊断字段

- 注册失败一律四个字段：`registry`、`id`、`owner`、`phase`；`cause` 保留原始异常链。
- 「注册失败即停」是唯一策略：核心注册失败不捕获、不继续启动，也不允许「部分注册后继续」。
- 只有可选第三方兼容模块（`CompatRunner`）允许隔离：单模块抛错只 `warn` 并继续。
- 每个平台 `install()` 都往 `RuntimeStats` 的 sink 输出一行 `[ORIENT-FW] init process=<client|server> platform=<类名> count=<initCount> installs=<installCount>`。
- 摘要由 `RuntimeStats.logSummary(trigger)` 输出：先一行 `[ORIENT-FW] summary trigger=<trigger>`，再逐行 `[ORIENT-FW] ` + 模块行 / 通道行。实际 trigger 只有三种：`server-started`、`server-stopped`、`client-disconnect`（类注释里列的 `command` 没有调用点）。触发次数两端不一致，见 L-11 / L-12。
- 重复通道的 warn 两端格式不同：Fabric 带方向与 id 两个占位符（`... ignored: {} {}`），NeoForge 只带 id（`... ignored: {}`）。
- 开发期排查命令：`/hpt`（`DevTestCommands`）里有 `modules`、`hooks`、`stats`、`dumpattrs`、`dumpcooking`、`cookingstate`、`freeze` 等；**这些命令在 `common/src/devtest` 源集里，成品 jar 与源码 jar 都排除**，不能在发布包里用。

---

## 13. 版本兼容与打包

### 13.1 内容包依赖范围由打包脚本生成

`tools/build_candidates.py` 的**依赖范围只由 `ApiVersion.RUNTIME` 算**（经 `tools/project_env.py` 的 `runtime_version()` 读 `RUNTIME = "..."`）；同一脚本另外读 `ApiVersion.CURRENT` 只用于打印横幅，不参与范围计算：

```python
_MAJOR, _MINOR = RUNTIME_VERSION.split(".")[:2]
_NEXT_MINOR = f"{_MAJOR}.{int(_MINOR) + 1}.0"
RUNTIME_RANGE = f"[{RUNTIME_VERSION},{_NEXT_MINOR})"        # NeoForge 内容依赖 + JarJar range
FABRIC_RUNTIME_RANGE = f">={RUNTIME_VERSION} <{_NEXT_MINOR}"  # Fabric depends
```

Runtime 0.2.0 时实际生成：Fabric `>=0.2.0 <0.3.0`；NeoForge 内容依赖与 `META-INF/jarjar/metadata.json` 的 `version.range` 都是 `[0.2.0,0.3.0)`，`artifactVersion` 为 `0.2.0`。

- 没有 `frameworkApiRange` 这个元数据键，也没有「框架加载时检查 API 范围」的代码；兼容边界完全靠加载器自己的依赖声明（Fabric `depends`、NeoForge `versionRange` + JarJar）。
- 打包脚本目前直接覆盖 Fabric 的 `jars` 列表与 NeoForge 的整份 JarJar metadata；以后若再嵌别的依赖会丢原有条目（见附录 15.2 第 8 条）。
- 所有交付包都是**内嵌 Runtime 的单 JAR**：Fabric 写 `META-INF/jars/<runtime>.jar` + `fabric.mod.json.jars`，NeoForge 写同目录文件 + `META-INF/jarjar/metadata.json`。
- Runtime jar 本身带 `assets/orient_framework/lang/` 10 份语言文件；内容包不带这份副本。拆分前缀两侧不对称：Runtime 只收 `cn/blockforge/orient/framework/`（`FRAMEWORK_PACKAGE`）与 `assets/orient_framework/`；内容包则排除整个 `cn/blockforge/orient/` 与 `assets/orient_framework/`。
- NeoForge Runtime 的 `@Mod` 入口单独编译（`neoforge/src/runtime/java`），只进 Runtime jar。

### 13.2 已发布的旧包不会选到 0.2.0

本地 `build/libs/releases/0.2.2-alpha/` 的两份 1.21.1 包实测元数据：

| 加载器 | 依赖写法 | 内嵌前置 |
|---|---|---|
| Fabric | `orient_framework: ">=0.1.3 <0.2.0"` | `META-INF/jars/orient_framework-fabric-0.1.3.jar` |
| NeoForge | `versionRange="[0.1.3,0.2.0)"` + JarJar `range "[0.1.3,0.2.0)"` / `artifactVersion "0.1.3"` | `META-INF/jars/orient_framework-neoforge-0.1.3.jar` |

结论：已发布的 0.2.2-alpha 内容包声明的是 `[0.1.3,0.2.0)`，**不会选到 0.2.0**；本次不改已发布包的元数据，也不建议手工拆嵌套包。其未重编译的 NeoForge 包用到的 43 个前置类、77 个成员（3C / 3H 前半的报告记录值，本次未复跑）在新编译产物里仍全部找到——那是「所引用符号的 ABI 检查通过」，不等于加载器允许越过旧范围，也不代表所有外部旧内容兼容。

多内容包共存仍只共享一份 Runtime：Fabric 按硬依赖求解一个版本；NeoForge 要求同 group + artifact、范围有交集且实际有候选，可能不是最高版本。一个要求 `<0.2.0`、另一个要求 `>=0.2.0` 时范围无交集，应明确拒绝；两份内嵌前置不能提供各自隔离的运行环境。

### 13.3 `tools/check_runtime_abi.py`

```bash
python tools/check_runtime_abi.py <旧内容包.jar>
python tools/check_runtime_abi.py <旧内容包.jar> --classes <目录或 jar> [...]
```

- 做法：直接解析 class 文件常量池，取出旧包里所有指向 `cn/blockforge/orient/framework/` 的类 / 字段 / 方法引用（名字 + 描述符），在新 Runtime 的类里沿父类 / 接口向上查找。
- 默认比 NeoForge 编译产物（`framework/*/build/classes/java/main` + `neoforge/build/classes/java/main`）：NeoForge 成品与编译产物都用 Mojang 官方名，可以直接对照；Fabric 成品是 intermediary 名，源码层面改动两端相同，比 NeoForge 就够。
- 退出码：`0` 全部找到；`1` 有缺失（逐条列出，先输出 `不兼容 N 处：`，再逐行 `缺类 <owner>` / `缺字段 …` / `缺方法 …`）；`2` 是无参数（打印模块说明）或新 Runtime 一个框架类都没找 到（提示先编译）。
- 输出一行统计：`旧内容包 <jar>：引用前置 <n> 个类、<m> 个成员；新 Runtime <k> 个类`；成功时再打一行 `全部找到：旧内容包可以配这个 Runtime 运行（二进制层面）`。
- 注意 `has()` 的判断：引用方的 owner 不在新 Runtime 里时一律视为存在（注释写「继承到 JDK / MC 的成员视为存在」），所以「全部找到」不等于每个成员都在框架里，只等于没有指向框架的缺失符号。
- ABI 检查只证明二进制成员还在，**不等于行为回归**，也不等于加载器允许越过依赖范围。

### 13.4 版本字符串的坑

Fabric 与 NeoForge（Maven 比较）对同一版本串排序不同：`0.1.0-hotfix1` 在 Fabric 上小于 `0.1.0`、在 NeoForge 上大于；`0.2.0-pre` 在 Fabric 上小于 `0.2.0`、在 NeoForge 上大于。所以 Runtime 一律用递增数字版本，不用 `hotfixN`。

「要排除下一系列的预发布，Fabric 用 `<0.2.0-`」目前只写在 `前置内嵌与多版本兼容方案` 里，**打包脚本没有对应实现**（只生成 `>=<本版本> <<下个次版本>`）。

---

## 14. 测试矩阵

### 14.1 现有单元测试覆盖了什么

`./gradlew :framework:pure:test :framework:loader:test :framework:game:test :fabric:test :neoforge:test :common:test`。框架五模块与 `common` 分开统计，数字取自各模块 `build/test-results/test/*.xml`：

| 模块 | 测试类 | 用例数 | 覆盖的契约 |
|---|---|---:|---|
| `framework:pure` | `ConfigStoreTest` | 18 | 缺文件返回默认值 / 缺文件时按默认值写盘 / 超限文件 / 补全 / 不重写坏 JSON / 空默认值表 / 未 init / 失败后重试 / 客户端初始为默认值 / `acceptClient` 校验 / `merge` 递归与类型冲突 / 旧 `warnings()` 与新 `details()` 文本一致 / `saveEdited` 三种状态 / 坏文档与超限文档备份 / 无法保留原文件时不应用 |
| | `NumericConfigTest` | 15 个方法（11 个 `@Test` + 4 个 `@ParameterizedTest`，参数化展开 13 条）/ 24 条用例 | 覆盖默认值、嵌套路径、`_` 元数据跳过、未知键、非数字、整数规则拒小数、`7.0` 接受、越界、端点包含、数字代替对象、坏 JSON、非对象根、未知 `_format`、缺规则报字段、结果与默认值互相独立 |
| | `ConfigWarningTest` | 3 | 参数顺序、`orient_framework.config.` 前缀、参数不可变 |
| | `RegistrationWindowTest` | 6 | 关窗消息含 registry/id/owner/phase、全局关窗幂等、去重报双方 owner、注册表既有 id、`withOwner` 嵌套与异常恢复、原始 cause 保留 |
| | `DeferredRegistrationsTest` | 3 | 事件前后登记、异常不被吞、null 拒绝 |
| `framework:loader` | `ModuleHostTest` | 8 | 依赖先行、缺依赖、环、同宿主重复、依赖更早宿主的模块、依赖更晚宿主失败、跨宿主重复、失败宿主不占 id |
| | `HolderRefTest` | 4 | 缺失时不读兜底值、登记后延迟解析、存在却返回 null 报 id、`RUNTIME` / `PROTOCOL_VERSION` 断言 |
| | `RegistrationApiCompatibilityTest` | 2 | 旧平台只实现 7 个 getter 仍可实例化；`RuntimeHooks` / `ClientHooks` 仍是函数式接口 |
| | `RuntimeEventsCompatibilityTest` | 1 | 旧 `RuntimeEvents` 实现不需要新增方法 |
| | `RuntimeStatsTest` | 1 | 通道计数缓存与摘要 |
| `framework:game` | `HooksTest` | 8 | 优先级大者先、同级按登记序、注销只影响自己且幂等、`remove` 按引用、`forEach` 异常隔离并继续、快照不可变、未变更时返回同一缓存快照、增删后旧快照不变 |
| | `CompatRunnerTest` | 4 | 缺失跳过且不 warn、在场只执行一次且只计成功、别名可启用、失败模块 warn 且不影响后续 |
| `framework:client` | — | 0 | 无单元测试 |
| `fabric` | `NetworkApiTest` | 4 | 握手 codec 往返（空 / 多通道）、通道列表防御性复制、REQUIRED / OPTIONAL 按实际登记判定、缺必需通道拒绝 / 缺可选接受 |
| `neoforge` | `NetworkApiTest` | 2 | codec 双向往返且保留策略、关窗后两个方向都拒绝 |

按 JUnit 结果统计，框架五模块共 **88** 条用例（pure 54、loader 16、game 12、fabric 4、neoforge 2）；JUnit 方法声明数是 79（75 个 `@Test` + 4 个参数化方法），差额来自 `NumericConfigTest` 的 4 个参数化方法展开成 13 条（11 + 13 = 24）。`common` 另有 80 个内容侧用例（`Opt3*`、`Config*`、`IngredientRecipes` 等），不属于框架契约；两者相加 168，与 3H 前半报告的计数一致。

### 14.2 仍需人工验证的集成项

| 项 | 现状 |
|---|---|
| 两端真实开局（注册数量与关窗无异常、统计摘要格式） | 3C / 3D / 3H 前半都在**开发运行配置**的服务端实测过；成品单包只做过静态检查（`check_all --jar`），没有把试验包放进游戏 |
| 真人客户端进服（Fabric 握手） | 3C 用开发客户端实测过；**真人客户端 + 生产包**仍按交接文档的说明需另行确认 |
| 客户端观感（界面、模型、选中框、渲染层） | 3D 用开发客户端截图验收过；3H 前半没有碰客户端，浏览器窗口焦点问题仍处阻断状态 |
| 第三方模组联测 | 未做（只有 Jade / JEI / REI / ModMenu 等插件入口的离线或单独验证） |
| `NetworkApi` 主线程保证的实测时序 | 结论来自各自源码注释与包装实现，**未实测** |
| 两端创造栏实际排位 | 未实机核对（见 L-02） |
| 成品单包 + Runtime 0.2.0 的运行基线 | 现有成品包服务端实测记录是 0.2.0-alpha 内容包 + Runtime 0.1.2 时期，**不是** 0.2.0；本次不另立基线 |
| 数据包改写过的战利品表在两端的 `builtin` 差异 | 只从代码推断，**未实跑** |

---

## 15. 附录

### 15.1 弃用与已删接口

| 符号 | 位置 | 现状 | 删除条件 / 替代入口 |
|---|---|---|---|
| `cn.blockforge.orient.framework.client.GuiKit` | `framework/client`（已删） | **0.2.0 已删除**。火锅自己的 `cn.blockforge.generated.orientshotpotchronicles.client.GuiKit` 是另一个同名不同包的类，保留不动 | 已删；内容侧用自己的绘图工具。三份移植版（`version-ports/1.20.1`、`26.1`、`26.2`）各自的框架副本是独立编译的，本次未同步删除 |
| `ClientItemRenderers.registrar`（公开静态字段） | `framework/client/ClientItemRenderers.java` | **0.2.0 改私有**，保留 `volatile` 与原初始化行为；`install()` 是直接覆盖、没有冲突检查（后装者生效） | 已收紧；改用 `install(BiConsumer<Item, Renderer>)` + `register(item, renderer)` |
| `ClientNetwork.sender`（公开静态字段） | `framework/client/ClientNetwork.java` | **0.2.0 改私有**，保留 `volatile` 与原初始化行为；`install()` 同样是直接覆盖、没有冲突检查 | 已收紧；改用 `install(Consumer<CustomPacketPayload>)` + `send(payload)` |
| `ConfigStore.Loaded.warnings()` | `framework/pure/ConfigStore.java` | 保留，返回英文文本列表；注释写「保留给 Runtime 0.1.1 时编译的旧内容包，0.2.0 再收紧」 | 本次（0.2.0）保留未收紧，见待定事项第 6 条；新代码用 `details()` |
| `HotPotCompat.customFuel(ItemStack)` | 内容侧 `common/.../compat/HotPotCompat.java` | 标了 `@Deprecated`，火锅本体 0 调用；`version-ports` 里的旧副本仍在调 | 不属于框架范围，各移植版自己决定 |

已删除的旧设计（**主工程 `mod/` 与 `version-ports/` 里都不存在**，不要再按 v0.2 去找；`mod2/` 是另一个项目，另有同名类，不要混）：`ScreenBridge`、`ContentBridge`、`ScreenOpeners`、`NeoForgeScreenOpeners`、`HotPotContentPlatform`、`NeoForgeContentPlatform`、`TagKeys`、`BuiltinRecipeFallback`、`RegistryRef`、`ReloadContext`、`ConfigKey`、`frameworkApiRange`，以及设计稿里给 `content/hotpot` 列的六个业务扩展接口中从未落地的五个（`SoupBaseSource`、`FuelSource`、`ExtraPotItem`、`EatIntercept`、`PreSettleNotify`）。

内容侧现存的 `HotPotHeat.HeatSource` 是火锅自己的嵌套接口，不在框架里。

### 15.2 待定事项

以下都是**本次不改代码、也没被用户决定**的事项。每条写清现状、可选方向、为什么要等。

**1. `ModuleDeclaration.configFiles` 留还是删（需要用户定）**

- 现状：字段是 `List<String> configFiles`，全库无人读取（只在声明行与构造器里出现）；`contentNamespace` 同样没有读取方。
- 可选方向：① 保留并明确用途（谁读、读来做什么，例如由框架校验配置文件名冲突或生成模板）；② 删字段——但那会改公开 record 的规范构造器签名，只能进主版本。
- 为什么要等：用途只有作者知道；本次不替用户决定。

**2. 要不要给 `RuntimeEvents` 加实体事件（需要用户定）**

- 现状：`RuntimeEvents` 只有命令、玩家进 / 出、服务端起 / 停 / 停止中、可燃、破坏前；没有实体事件，也没有通用玩家交互事件。v0.2 规格写的「实体事件」是设计稿泛称，代码从来没有。
- 可选方向：纯新增 default 方法，但需要先定「哪些实体事件、给谁用」；现在做也要按 0.2.x 的规则评估（`ApiVersion.RUNTIME` 已是 `0.2.0`，0.1.x 已成历史）。
- 为什么要等：需求不明确，加了会扩大公开面；本次不扩大范围。

**3. 注册窗口粒度（Fabric 全局关 / NeoForge 逐表关）**

- 现状：见第 4.2 节与 L-01。Fabric 一次 `closeAll()` 关掉全部注册表；NeoForge 逐表关。
- 可选方向：让 Fabric 也按表关（需要平台在合适的时机精确知道每张表何时冻结，Fabric 没有对应事件）、或在文档与工具里只提醒不统一。
- 为什么要等：任何改动都是**改既有行为**而不是加接口，会影响现有内容包的注册时机，只能进主版本并两端回归。

**4. 协议不匹配的拒绝时机（Fabric 进服后握手 / NeoForge 连接阶段）**

- 现状：见 L-03。Fabric 是玩家 JOIN 之后发 `orient_framework:hello`，校验失败才断开；并且对收不到握手的旧客户端放行。
- 可选方向：把 Fabric 侧校验提前到连接阶段（需要 Fabric networking 的 configuration 阶段支持，或另注册 `configuration` 通道）、或保持「进服后校验」并写清旧客户端放行策略。
- 为什么要等：改实现属行为变更，会影响未装前置 / 旧前置客户端的进服体验，0.1.x 内不宜做。

**5. ③④（runtime / client）时机的归属**

- 现状：`RuntimeHooks` / `ClientHooks` 只是单方法函数式接口，框架只按 `ModuleHost` 的 `ordered` 顺序遍历转发；真正的时机由内容侧用加载器原生事件接（见 5.4）。v0.2 规格写的「RuntimeHooks 自身保证一次注册、多次有效」不成立。
- 可选方向：让框架平台侧实现 `RuntimeHooks` / `ClientHooks`（纯新增，但要两端各写一份原生接法）、或保持现状并在文档里写清「内容侧负责时机」。现在做按 0.2.x 规则评估。
- 为什么要等：改成框架负责会与现有内容侧接法重复触发，需要先统一两端的接入方式。

**6. `ConfigStore.Loaded.warnings()` 是否继续保留**

- 现状：保留，返回英文文本列表；mod 内只有单元测试用它，内容侧早已改用内容自己的 `Config.Loaded`。
- 可选方向：删除（配合 ABI 检查确认无外部旧包引用）、或继续保留。
- 为什么要等：删任何公开成员都要先进主版本，而且「有没有已发布的外部内容包在用」属于作者的产品决定。

**7. NeoForge 战利品桥只支持追加池**

- 现状：`NeoForgeLootEvents` 给内容的 `LootTable.Builder` 是 `PoolCollector`，只 override 了 `withPool`；Fabric 给的是真正的 builder，可 `modifyPools`。`builtin` 判定两端算法也不同（L-06）。
- 可选方向：给 NeoForge 补一个能改既有池的窄接口（需要 NeoForge 侧的访问器，Fabric 钓鱼出虾就是同类特例）、或明确「框架通用桥只支持追加池」。
- 为什么要等：要更宽契约得先定接口形状并两端回归；本次只按现状写清限制。

**8. 打包脚本会覆盖 `jars` / JarJar metadata**

- 现状：`build_candidates.py` 直接写 `fabric.mod.json` 的 `jars` 列表与 NeoForge 的整份 `META-INF/jarjar/metadata.json`；以后若再嵌别的依赖会丢失原有条目。
- 可选方向：改成「读取已有声明、追加 Runtime 条目」；或在嵌套依赖增加时另立方案。
- 为什么要等：当前只嵌一个 Runtime，改成合并逻辑属工具改造，需与多系列（第 9 条）一起定。

**9. 多系列与 `-slim` 包**

- 现状：没有 `content/<series>` 层与 `modules/<series>-<loader>` 目录；`common` 是 content/hotpot 的过渡态。所有交付包都是内嵌 Runtime 的单 JAR，没有 `-slim` 包，也没有能力查询接口。
- 可选方向：按 `前置内嵌与多版本兼容方案` 做共享前置 + slim 内容包，并补运行时能力查询；或继续单系列单包。
- 为什么要等：多系列共存要先实测两端加载器的求解与 JarJar 选择（NeoForge 是 `findFirst()`，不保证最高版本），且要动目录结构与打包脚本。

**10. 两端诊断摘要时机不统一**

- 现状：L-11（客户端断线摘要触发次数）与 L-12（server-stopped 摘要时机）两端不等价。
- 可选方向：统一一端的触发点（不碰公开签名）、或在规格里把两端差异当既定事实。
- 为什么要等：改统计口径属行为变更，会动到依赖摘要的行为断言脚本。

**11. 创造栏排位无法指定**

- 现状：L-02。Fabric 不指定坐标、交给分页逻辑；NeoForge 写死 `Row.TOP, 0`。平台没暴露 `tabsBefore` / `tabsAfter`。
- 可选方向：给 `CreativeTabRegistrar` 增加可选排位参数（新增 default 方法即可，但需要平台两端都能落地）、或保持现状。
- 为什么要等：先要实机确认两端现状的实际位置（见待核实第 1、2 条），否则改了也不知道改对没有。

**12. 内容侧 `HotPotCompat.customFuel(ItemStack)` 的弃用入口**

- 现状：标 `@Deprecated`，火锅本体 0 调用，`version-ports` 旧副本仍在调。
- 可选方向：各移植版自行删除，或保留。
- 为什么要等：不属于框架 0.2.0 范围，本次不动内容侧代码。

### 15.3 待核实

以下条目在写作时**无法从代码或本地资料确认**，按「待核实」列出，不要在实现里当成既定结论：

1. **NeoForge 创造栏 `Row.TOP, 0` 会不会和原版第一格冲突**：本机的 NeoForge 源码包不含 patched 的 `net/minecraft`，无法确认原版创造栏的重复位置检查在 NeoForge 上被改成了什么。需要进游戏看一眼创造栏。
2. **Fabric 创造栏落在第 2 页第 1 格**：这是读 Fabric API 的 `ItemGroupsMixin` 源码推出来的，**没有实机截图**。
3. **战利品桥 `builtin` 判定差异的实际影响**：只从代码推断「被数据包改写的 `minecraft:` 表两端判定可能相反」，没有真跑过被打包改写的战利品表，不知道三条掉落会不会一边生效一边不生效。
4. **两端网络处理器的主线程保证**：结论来自 Fabric `ServerPlayNetworking` 注释与 NeoForge `PayloadRegistrar` 默认 `HandlerThread.MAIN` 的包装实现，**没有实测时序**。
5. **真人客户端 + 生产包进服（Fabric 握手）**：3C 用开发客户端验证过握手链路，生产包 + 真人客户端仍需另行确认。
6. **`ModuleDeclaration.configFiles` / `contentNamespace` 是否被外部内容包读取**：只检索了工作区内的 `mod`、`mod2`、`version-ports`，不能覆盖项目外的内容包。
7. **移植版框架副本要不要跟到 0.2.0**：`version-ports/1.20.1`、`26.1`、`26.2` 各自的框架副本只读核对过「没有调用三个被删 / 收紧的符号」，升级计划不在本次范围。
8. **`check_runtime_abi.py` 对 Fabric 成品包的对照**：工具默认比 NeoForge 编译产物；Fabric 成品是 intermediary 名，本次没有单独比过 Fabric 成品包。
9. **`DeferredHolder.value()` 落地前抛的具体异常与文本**：L-07 的「注册前读 `value()` 会抛」来自 NeoForge 自己的 `DeferredHolder` 语义，本仓库里只有 `DeferredHolder.create(...)` 这一处调用，没有它的实现源码；未绑定时 `getKey()` 是否可用同样未确认。
10. **NeoForge registrar 拒绝连接的阶段与玩家表现**：7.2 / L-03 的「连接阶段拒绝」依据是 NeoForge `PayloadRegistrar` 的 javadoc（连接尝试时缺非可选 payload 会失败），本仓库只有 `event.registrar(Integer.toString(PROTOCOL_VERSION))` 这一处调用；实际发生在哪个阶段、玩家看到什么界面与日志，都没有实测。
11. **两端护甲模型的一致性**：Fabric 是平台手动复刻姿势与可见性，NeoForge 交给原版渲染器（见 8.1）；同一 `ArmorModel` 在两端外观是否一致**未实机对比**。
12. **`RegisterClientExtensionsEvent` 与 `FMLClientSetupEvent` 的实际先后**：8.2 / 8.3 说 NeoForge 复用扩展事件队列「不依赖原生事件先后」，代码层面成立（先到先执行、后到重放），但两个事件在游戏里的真实顺序没有实测确认。
13. **Fabric 关窗相对其他模组初始化的先后**：4.2 与 L-01 的「关窗点都在所有模组初始化之后，不会截断后面模组的初始化」依据是 `FabricRegistrationApi` 的一行注释（「首个引擎启动事件在全部内容初始化之后关窗」），事件与其他模组初始化的真实先后**未实测**。

### 15.4 修订记录

| 版本 | 日期 | 说明 |
|---|---|---|
| v0.2（草案） | 2026-10-30 | 设计稿阶段的接口边界与契约；含大量当时尚未落地的设想（分注册表生命周期方法、命名钩子表、`frameworkApiRange`、`content/hotpot` 六个业务接口、`RegistryRef`），对应 Runtime 0.1.0 |
| v0.3 | 2026-11-11 | 按 HEAD `6a3308d`（Runtime 0.2.0）的代码整体重写：补齐 0.1.5 / 0.1.6 新增的注册分域、`RuntimeEvents`、`ClientContent`、`RuntimeHooks` / `ClientHooks` 的真实归属；按代码逐字对齐全部错误文本；收进两个加载器的 13 处行为差异；写清三个版本号与打包生成依赖范围的机制、弃用与已删接口、现有单元测试覆盖；删掉全部从未落地的设计稿接口 |
