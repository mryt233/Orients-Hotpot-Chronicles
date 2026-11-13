// 作者：mryt233
package cn.blockforge.sample.hotpotsubmod;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.SeasoningDef;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.Seasonings;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.SoupBowlItem;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.SoupDef;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.Soups;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

/** 第四批 4L-1 最小 Java 测试子模组公共入口（零复制火锅源码，仅调用公开登记与桌面接口）。 */
public final class HotpotSubmod {
    public static final String MOD_ID = "hotpotsubmod";
    public static final ResourceLocation GINSENG_BROTH = id("ginseng_broth");
    public static final ResourceLocation YUZU_DIP = id("yuzu_dip");
    public static final ResourceLocation COASTER_ID = id("bamboo_coaster");
    public static final ResourceLocation GINSENG_BOWL_ID = id("ginseng_soup_bowl");

    private static final AtomicBoolean HOTPOT_REGISTERED = new AtomicBoolean();
    private static volatile CoasterBlock coasterBlock;
    private static volatile CoasterItem coasterItem;
    private static volatile SoupBowlItem ginsengBowlItem;

    private HotpotSubmod() {
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    public static synchronized void prepareGameObjects() {
        if (coasterBlock != null) {
            return;
        }
        coasterBlock = new CoasterBlock();
        coasterItem = new CoasterItem();
        ginsengBowlItem = new SoupBowlItem(GINSENG_BROTH);
    }

    public static void registerGameObjects(BiConsumer<ResourceLocation, Block> blocks,
                                           BiConsumer<ResourceLocation, Item> items) {
        prepareGameObjects();
        blocks.accept(COASTER_ID, coasterBlock);
        items.accept(COASTER_ID, coasterItem);
        items.accept(GINSENG_BOWL_ID, ginsengBowlItem);
    }

    /** 必须在 HotPotHooks.freezeRegistries() 之前调用（如模组初始化或 FMLCommonSetup）。 */
    public static void registerHotpotEntries() {
        if (!HOTPOT_REGISTERED.compareAndSet(false, true)) {
            return;
        }
        Soups.register(SoupDef.builder(GINSENG_BROTH)
            .source(() -> Items.GOLDEN_CARROT, Items.BOWL)
            .speed(1.25F)
            .clearsNausea(true)
            .bowlFood(6, 0.8F)
            .mainEffect(MobEffects.REGENERATION, 160)
            .secondEffect(MobEffects.ABSORPTION, 200)
            .screenColors(0xFFE6C87A, 0xFFFFF0B8, 0xFFB8964A, 0xFFD9AE58, 0xFFF7E098)
            .bubbleColors(0xFFF3DFA2, 0xFFFFFFFF)
            .build());

        Seasonings.register(SeasoningDef.builder(YUZU_DIP)
            .raws(() -> Items.HONEYCOMB)
            .color(0xFFE6B83A)
            .effect(MobEffects.MOVEMENT_SPEED, 200, 0)
            .hunger(2)
            .removes(MobEffects.MOVEMENT_SLOWDOWN)
            .garnish(true)
            .build());
    }

    public static CoasterBlock coasterBlock() {
        return coasterBlock;
    }

    public static CoasterItem coasterItem() {
        return coasterItem;
    }

    public static SoupBowlItem ginsengBowlItem() {
        return ginsengBowlItem;
    }
}
