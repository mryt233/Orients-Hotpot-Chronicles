# 框架 API 兼容性契约

> 历史预发布接口契约，当前实现以 `framework/` 源码为准。

作者：mryt233，2026-10-30。对应《框架API规格 v0.2》。**当前为预发布，不构成稳定承诺。**

## 1. 版本

| 版本 | 当前值 | 规则 |
|---|---|---|
| Framework API | `0.1.0-pre` | P1–P3 期间允许破坏性调整；1.0.0 只在 P3 打包实验与多系列组合测试通过后发布 |
| Content Mod | 各系列独立（当前 `1.0.0-r130` 开发版） | 与框架 API 版本解耦 |
| Network Protocol | `PROTOCOL_VERSION = 1` | 与上面两者解耦；按 payload 组协商 |

公开 API 包根：`cn.blockforge.orient.framework.{pure,loader,game,client}`。内容模块实现类不得出现在公开 API 签名中。

## 2. 稳定性规则（1.0 之后生效）

- **MAJOR**：删除/修改公开签名、语义破坏；**MINOR**：纯新增（默认方法、新接口）；**PATCH**：修复且不改签名。
- 弃用：`@Deprecated(forRemoval = true)` + 替代入口说明，至少保留 **1 个 MINOR** 后才可移除。
- 破坏性变更必须记入本文「变更记录」；预发布期同样记录。
- 内容模块声明 `frameworkApiRange`（开发期示例 `[0.1,0.2)`）；不满足时**明确报错并拒绝加载**。

## 3. 公开 API 边界

- **loader**：`Platform`、`RegistrationApi`（7 个分域 Registrar + `BlockEntityFactory`/`MenuFactory`/`CreativeTabContent`）、`NetworkApi`/`ProtocolPolicy`/`PayloadHandler`、`MenuOpener`/`MenuProviderLike`、`LootEventBridge`、`HolderRef`、`ContentModule`/`ModuleDeclaration`/`ModuleHost`/`RuntimeHooks`/`ClientHooks`、`Framework`、`ApiVersion`。
- **game**：`ConfigBase`、`Hooks`、`CompatModule`/`CompatRunner`。
- **pure**：`NumericConfig`、`ConfigStore`、`RegistrationGuard`。
- **client**：`GuiKit`。
- **内容侧公开 API**（不属于框架，允许子系列选择性依赖）：`content/hotpot/api/HotpotScreens`、`HotPotHooks`、`HotPotCompat`、`SoupBase.registerSource`、`HotPotFuel.registerCustomSource`。**面条可以依赖火锅的 API，但不得反向要求火锅依赖面条。**

## 4. 可选依赖与缺失降级

- 第三方模组不存在 → `CompatRunner` 跳过该模块，不加载其类，不报错。
- 框架/内容版本不满足 → 启动日志明确说明并拒绝该模块；不静默降级。
- 多内容模块：任何模块缺失时其余模块正常运行；`ModuleHost` 对缺依赖/重复 id/环失败即停。

## 5. 网络协议

- 每个 payload 标注 `ProtocolPolicy.REQUIRED/OPTIONAL`。
- REQUIRED 不匹配 → 连接阶段拒绝并给出双方版本；OPTIONAL 不匹配 → 功能降级，不踢人。
- 断线清理：客户端清空服务端下发缓存（配置快照、协议版本、功能开关），重连重新握手（现有 `Config.clearClient` 行为）。
- **协商不等于授权**：每个 C2S 处理器仍校验距离、目标方块实体、槽位与权限。

## 6. 资源与存档

- 注册 id、组件 id、数据包路径视为公开契约；alpha 阶段允许改名（用户规则），beta 起冻结。
- 组件编码变更必须兼容旧数据（`held_food` 使用 `HeldFood` 包装，持久化 JSON 形状与旧版一致）。
- 配置文件名与格式由内容模块声明（`ModuleDeclaration.configFiles`）；字段含义与范围由内容层维护，框架不得改变既有语义。
- 存档升级目前无 datafix；跨大版本不承诺（1.20.x 由 `version-ports/` 独立移植）。

## 7. 变更记录

| 日期 | 版本 | 变更 |
|---|---|---|
| 2026-10-30 | 0.1.0-pre | 框架分层落地：pure/loader/game/client；RegistrationApi 分域与注册窗口；ContentModule/ModuleHost 生命周期；Hooks 排序/注销/异常隔离；ConfigStore/ConfigBase；GuiKit 上移；LootEventBridge 实例能力 |
| 2026-10-30 | 0.1.0-pre | 预发布声明：不承诺跨 P1–P3 兼容；每批改动记录在《计划书》与 DEV_LOG |

## 8. 评审与例外

- 公开 API 新增需满足「两个实际消费者或明确第三方需求」。
- 例外需在本文登记理由、影响范围与回退方式。