// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.devtest;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPot;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.SauceStationBlockEntity;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.SauceStationScreenHandler;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.SeasoningDef;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.Seasonings;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.SoupBowlItem;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.SoupDef;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.Soups;
import cn.blockforge.orient.framework.loader.RegistrationApi;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 4F-1 开发探针（仅在 {@code -Dhotpot.opt4f=true} 时启用，不进发布包）：
 * 登记 1 个测试汤底、1 个测试蘸料、1 个专属汤碗，用于验证 Java 启动期开放登记全链路。作者：mryt233
 */
public final class Opt4fProbe {
    public static final ResourceLocation SOUP_ID = ResourceLocation.fromNamespaceAndPath("opt4ftest", "herb_broth");
    public static final ResourceLocation SEASONING_ID = ResourceLocation.fromNamespaceAndPath("opt4ftest", "wasabi");
    public static final ResourceLocation BOWL_ID = HotPot.id("opt4ftest_herb_broth_bowl");

    private static boolean installed;
    private static SoupBowlItem customBowl;

    private Opt4fProbe() {
    }

    public static void install(RegistrationApi registration) {
        if (installed) {
            return;
        }
        installed = true;

        customBowl = registration.items().register(BOWL_ID, new SoupBowlItem(SOUP_ID));

        SoupDef herbBroth = SoupDef.builder(SOUP_ID)
            .source(() -> Items.RABBIT_STEW, Items.BOWL)
            .speed(1.5F)
            .bowlFood(6, 0.8F)
            .mainEffect(MobEffects.REGENERATION, 200)
            .clearsNausea(true)
            .build();
        Soups.register(herbBroth);

        SeasoningDef wasabi = SeasoningDef.builder(SEASONING_ID)
            .raw(() -> Items.AMETHYST_SHARD)
            .color(0xFF7CB342)
            .effect(MobEffects.DIG_SPEED, 240, 1)
            .hunger(2)
            .build();
        Seasonings.register(wasabi);

        Item registeredBowl = Soups.bowlItem(herbBroth);
        HotPot.LOGGER.info(
            "[OPT4F] installed soup={} (index={}) seasoning={} (index={}) bowl={} matched={}",
            SOUP_ID, Soups.indexOf(herbBroth),
            SEASONING_ID, Seasonings.indexOf(wasabi),
            BOWL_ID, registeredBowl == customBowl);
    }

    public static String run(ServerPlayer player, String action, BlockPos pos) {
        return switch (action) {
            case "freeze" -> verifyFreeze();
            case "refill" -> refillStation(player, pos);
            case "stowe_sauce" -> storeMainToBag(player);
            case "equip_sauce" -> equipBagToOffhand(player);
            case "p2_put_wheat" -> putItemToStation(player, pos, new ItemStack(Items.WHEAT, 1));
            case "p2_put_sugar" -> putItemToStation(player, pos, new ItemStack(Items.SUGAR, 1));
            case "p2_status" -> stationStatus(player, pos);
            default -> "unknown action=" + action;
        };
    }

    private static String putItemToStation(ServerPlayer player, BlockPos pos, ItemStack stack) {
        if (!(player.serverLevel().getBlockEntity(pos) instanceof SauceStationBlockEntity be)) {
            return "no sauce station at " + pos.toShortString();
        }
        SauceStationScreenHandler menu = new SauceStationScreenHandler(1, player.getInventory(), be);
        player.getInventory().setItem(9, stack);
        menu.quickMoveStack(player, 9);
        be.setChanged();
        return stationStatus(player, pos);
    }

    private static String stationStatus(ServerPlayer player, BlockPos pos) {
        if (!(player.serverLevel().getBlockEntity(pos) instanceof SauceStationBlockEntity be)) {
            return "no sauce station at " + pos.toShortString();
        }
        return "station raw0=" + be.rawInventory().getItem(0).getCount()
            + " kindIndex0=" + be.kindIndex(0) + " portions0=" + be.portions(0)
            + " avail0=" + be.available(0)
            + " def0=" + (be.kindDef(0) != null ? be.kindDef(0).id() : "null")
            + " def1=" + (be.kindDef(1) != null ? be.kindDef(1).id() : "null");
    }

    private static String verifyFreeze() {
        String soupLate = "unrejected";
        try {
            Soups.register(SoupDef.builder(ResourceLocation.fromNamespaceAndPath("opt4ftest", "late_soup"))
                .source(() -> Items.APPLE).build());
        } catch (IllegalStateException expected) {
            soupLate = "rejected";
        }
        String seasoningLate = "unrejected";
        try {
            Seasonings.register(SeasoningDef.builder(ResourceLocation.fromNamespaceAndPath("opt4ftest", "late_sauce"))
                .raw(() -> Items.APPLE).build());
        } catch (IllegalStateException expected) {
            seasoningLate = "rejected";
        }
        return "soupsFrozen=" + Soups.isFrozen() + " seasoningsFrozen=" + Seasonings.isFrozen()
            + " lateSoup=" + soupLate + " lateSeasoning=" + seasoningLate;
    }

    private static String refillStation(ServerPlayer player, BlockPos pos) {
        if (!(player.serverLevel().getBlockEntity(pos) instanceof SauceStationBlockEntity be)) {
            return "no sauce station at " + pos.toShortString();
        }
        SauceStationScreenHandler menu = new SauceStationScreenHandler(1, player.getInventory(), be);
        player.getInventory().setItem(9, new ItemStack(Items.AMETHYST_SHARD, 2));
        // 蘸料台菜单槽位：0..8 为 9 个调料原料槽，第 9 槽起为玩家主背包（Inventory 索引 9）
        menu.quickMoveStack(player, 9);
        be.setChanged();
        return "station raw0=" + be.rawInventory().getItem(0).getCount()
            + " kindIndex0=" + be.kindIndex(0) + " portions0=" + be.available(0)
            + " def0=" + (be.kindDef(0) != null ? be.kindDef(0).id() : "null");
    }

    private static String storeMainToBag(ServerPlayer player) {
        ItemStack main = player.getMainHandItem();
        player.getInventory().setItem(10, main.copy());
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        return "stowed main->bag10";
    }

    private static String equipBagToOffhand(ServerPlayer player) {
        ItemStack saved = player.getInventory().getItem(10);
        player.setItemInHand(InteractionHand.OFF_HAND, saved.copy());
        player.getInventory().setItem(10, ItemStack.EMPTY);
        return "equipped bag10->off";
    }
}
