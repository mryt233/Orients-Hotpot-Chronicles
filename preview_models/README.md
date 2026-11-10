<!-- 作者：mryt233；mryt233 改（2026-11-10）。 -->
# 完整模型预览覆盖层

这里是工具用的模型，**不能作为资源包装进游戏或打进 JAR**。1.21.1 游戏模型不支持约 8° 的元素旋转；游戏仍由原来的代码绘制斜腿。

目录按资源包排列：`assets/<命名空间>/models/<block|item>/<名称>.json`。同路径模型在静态查看器、出图、导出时优先覆盖游戏原模型；没有预览就回退到原资源。动画场景读原资源，继续自行绘制运行时部件。

- `python tools/gen_benches.py`：保留原有板凳生成，同次生成 11 木种 × 4 连接条凳方块预览和 11 个条凳物品预览。
- `python tools/gen_tall.py`：保留高桌 / 高灶台 / 条凳生成，最后调用同一个预览步骤。
- `--out build/bench_preview/regen/resources`：源资源写进指定目录，预览写到其旁的 `resources-preview_models/`，清单写到 `resources-generated_manifest.json`；不回写真实资源、预览或主清单。
- `python tools/check_all.py --source-only --strict`：包括预览 / Java 参数一致性与生成物哈希检查；单独检查用 `python tools/check_bench_preview.py`。

方块预览完整展开父链，包含凳面、横撑、斜腿、贴图及显示属性，不依赖预览父模型。物品预览使用同一张独立条凳几何，并保留游戏物品的 display。LEG_TOP / LEG_SPREAD / LEG_W / LEG_T 每次从 Java 源码读取，读取失败立即终止。

朝北坐标与原方块状态相同：`left=false,right=true` 对应 `_right`（摆在长凳左端）；`left=true,right=false` 对应 `_left`（摆在长凳右端）；双 true 为无腿中段。

生成物均登记在 `content/generated_manifest.json`。原方块 JSON 的 `_说明` 仅指向这里，不影响游戏烘焙。`Gradle` 的资源输入限各模块 `src/main/resources`，正式打包入口不读取本目录，最终包检查也拒绝 `preview_models/`。
