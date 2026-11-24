// 作者：mryt233；mryt233 改（2026-11-10）
package cn.blockforge.generated.orientshotpotchronicles.platform;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPot;
import cn.blockforge.orient.framework.loader.Framework;
import cn.blockforge.orient.framework.platform.fabric.FabricFrameworkPlatform;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;

/** 开发运行没有独立前置入口时安装同一平台；正式包依赖独立前置。 */
public final class FabricPlatform {
    private static final TagKey<Biome> HAS_ORANGE_TREE = biomeTag("has_orange_tree");
    private static final TagKey<Biome> HAS_WILD_TEMPERATE_CROPS = biomeTag("has_wild_temperate_crops");
    private static final TagKey<Biome> HAS_WILD_WARM_CROPS = biomeTag("has_wild_warm_crops");
    private static final TagKey<Biome> HAS_WILD_HUMID_CROPS = biomeTag("has_wild_humid_crops");

    private FabricPlatform() { }

    public static void install() {
        if (Framework.isInstalled()) return;
        if (!FabricLoader.getInstance().isDevelopmentEnvironment()) {
            throw new IllegalStateException("orient_framework runtime is required in production");
        }
        FabricFrameworkPlatform.install();
    }

    /** Fabric 的原生 modifyPools 与 BiomeModifications 与 NeoForge 不同，专属适配保留。 */
    public static void afterContentRegistration() {
        // mryt233 改（2026-11-19）：Mixin 仅在加载器源码集接线，正式包与开发环境均安装。
        cn.blockforge.generated.orientshotpotchronicles.hotpot.StoveBlockEntity.installDropSpawner(
            cn.blockforge.generated.orientshotpotchronicles.mixin.BlockDropInvoker::hotpot$popResource);
        ShrimpLoot.register();
        addVegetalFeature(HAS_ORANGE_TREE, "orange_tree_checked");
        for (String crop : new String[]{"scallion", "cilantro", "soybean", "garlic", "greens"}) {
            addVegetalFeature(HAS_WILD_TEMPERATE_CROPS, "wild_" + crop);
        }
        for (String crop : new String[]{"tomato", "chili_pepper", "peanut"}) {
            addVegetalFeature(HAS_WILD_WARM_CROPS, "wild_" + crop);
        }
        addVegetalFeature(HAS_WILD_HUMID_CROPS, "wild_zhe_er_gen");
    }

    private static TagKey<Biome> biomeTag(String path) {
        return TagKey.create(Registries.BIOME, HotPot.id(path));
    }

    private static void addVegetalFeature(TagKey<Biome> biomeTag, String featurePath) {
        BiomeModifications.addFeature(
            BiomeSelectors.tag(biomeTag),
            GenerationStep.Decoration.VEGETAL_DECORATION,
            ResourceKey.create(Registries.PLACED_FEATURE, HotPot.id(featurePath)));
    }
}
