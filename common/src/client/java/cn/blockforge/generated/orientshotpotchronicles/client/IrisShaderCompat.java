// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.client;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotContent;
import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 光影（Iris + BSL 等按固定方块 ID 表识别材质的光影包）运行时方块映射兼容：
 * 当检测到光影包加载/重载生成了新的 {@code WorldRenderingSettings#blockStateIds} 时，
 * 若光影包尚未显式或通过标签为本模组橙子树叶、树苗与作物映射 ID，则按对应原版模板补齐，
 * 使树叶正确获得风吹飘动与次表面散射；零 Iris 编译期依赖，未装光影模组时自动静默跳过。
 * 作者：mryt233
 */
public final class IrisShaderCompat {
    private static boolean initialized;
    private static boolean unavailable;
    private static Object worldSettingsInstance;
    private static Method getBlockStateIds;
    private static Method setBlockStateIds;
    private static Method clearReloadRequired;
    private static Object2IntMap<BlockState> lastPublishedMap;

    private IrisShaderCompat() {
    }

    @SuppressWarnings("unchecked")
    public static void onClientTick() {
        if (!ensureReflect()) {
            return;
        }
        try {
            Object raw = getBlockStateIds.invoke(worldSettingsInstance);
            if (!(raw instanceof Object2IntMap<?> mapRaw)) {
                lastPublishedMap = null;
                return;
            }
            Object2IntMap<BlockState> current = (Object2IntMap<BlockState>) mapRaw;
            if (current == lastPublishedMap || current.isEmpty()) {
                return;
            }
            Object2IntLinkedOpenHashMap<BlockState> updated = new Object2IntLinkedOpenHashMap<>(current);
            int defVal = current.defaultReturnValue();
            updated.defaultReturnValue(defVal);

            int added = 0;
            added += injectMissing(current, updated, HotPotContent.ORANGE_LEAVES,
                Blocks.AZALEA_LEAVES, Blocks.OAK_LEAVES, Blocks.JUNGLE_LEAVES);
            // mryt233 改（2026-11-22）：BSL 等光影包将 oak_sapling 归入仅透光不摆动的 10900 材质组，
            // 优先借用 Blocks.AZALEA（10700）让橙子树苗与杜鹃一样保持透光并随风轻晃。
            added += injectMissing(current, updated, HotPotContent.ORANGE_SAPLING,
                Blocks.AZALEA, Blocks.OAK_SAPLING);
            for (Block crop : HotPotContent.cropBlocks()) {
                added += injectMissing(current, updated, crop, Blocks.WHEAT, Blocks.CARROTS);
            }
            for (Block wild : HotPotContent.WILD_CROPS) {
                added += injectMissing(current, updated, wild, Blocks.SHORT_GRASS, Blocks.FERN, Blocks.POPPY);
            }

            if (added <= 0) {
                lastPublishedMap = current;
                return;
            }
            lastPublishedMap = updated;
            setBlockStateIds.invoke(worldSettingsInstance, updated);
            clearReloadRequired.invoke(worldSettingsInstance);
            Minecraft mc = Minecraft.getInstance();
            if (mc.levelRenderer != null) {
                mc.levelRenderer.allChanged();
            }
        } catch (ReflectiveOperationException | LinkageError ignored) {
            unavailable = true;
        }
    }

    /** 按原版模板方块的首个已映射光影材质 ID，为尚未映射到光影表的模组方块全部状态补齐条目。 */
    static int injectMissing(Object2IntMap<BlockState> source, Object2IntMap<BlockState> target,
                             Block modBlock, Block... templates) {
        if (source == null || target == null || modBlock == null || templates == null) {
            return 0;
        }
        int defVal = source.defaultReturnValue();
        int templateId = defVal;
        for (Block tpl : templates) {
            if (tpl == null) {
                continue;
            }
            for (BlockState state : tpl.getStateDefinition().getPossibleStates()) {
                int id = source.getInt(state);
                if (id != defVal) {
                    templateId = id;
                    break;
                }
            }
            if (templateId != defVal) {
                break;
            }
        }
        if (templateId == defVal) {
            return 0;
        }
        int targetDef = target.defaultReturnValue();
        int added = 0;
        for (BlockState state : modBlock.getStateDefinition().getPossibleStates()) {
            if (target.getInt(state) == targetDef) {
                target.put(state, templateId);
                added++;
            }
        }
        return added;
    }

    private static boolean ensureReflect() {
        if (unavailable) {
            return false;
        }
        if (initialized) {
            return true;
        }
        initialized = true;
        try {
            Class<?> cls = Class.forName("net.irisshaders.iris.shaderpack.materialmap.WorldRenderingSettings");
            Field instanceField = cls.getField("INSTANCE");
            worldSettingsInstance = Objects.requireNonNull(instanceField.get(null));
            getBlockStateIds = cls.getMethod("getBlockStateIds");
            setBlockStateIds = cls.getMethod("setBlockStateIds", Object2IntMap.class);
            clearReloadRequired = cls.getMethod("clearReloadRequired");
            return true;
        } catch (ClassNotFoundException | NoSuchFieldException | NoSuchMethodException
                 | IllegalAccessException | LinkageError ignored) {
            unavailable = true;
            return false;
        }
    }
}
