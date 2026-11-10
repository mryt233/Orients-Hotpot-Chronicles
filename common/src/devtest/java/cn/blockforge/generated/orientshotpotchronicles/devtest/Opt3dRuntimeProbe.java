// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.devtest;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.*;
import cn.blockforge.orient.framework.loader.Framework;
import com.google.gson.JsonObject;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.Map;
import java.util.UUID;

/** 只在开发命令存在时安装，检查实际事件顺序与缓存释放。 */
public final class Opt3dRuntimeProbe {
    private static boolean installed;
    private static int joins, leaves, stopping, stopped;
    private static UUID seeded;
    private static boolean leaveClean, stopClean, startReady;
    private Opt3dRuntimeProbe() { }

    public static void install() {
        if (installed) return;
        installed = true;
        var events = Framework.platform().events();
        events.onServerStarting(server -> {
            startReady = SoupBase.sourcesFrozen() && HotPotFuel.sourcesFrozen();
            log("starting frozen=" + startReady);
        });
        events.onPlayerJoin(player -> joins++);
        events.onPlayerLeave(player -> {
            leaves++;
            leaveClean = !cache(ClothApronProtection.class, "HEAT_CACHE").containsKey(player.getUUID())
                && cache(CuttingBoardBlock.class, "CUT_CACHE").isEmpty();
            log("leave clean=" + leaveClean);
        });
        events.onServerStopping(server -> {
            stopping++;
            stopClean = cache(ClothApronProtection.class, "HEAT_CACHE").isEmpty()
                && cache(CuttingBoardBlock.class, "CUT_CACHE").isEmpty();
            log("stopping clean=" + stopClean);
        });
        events.onServerStopped(server -> { stopped++; log("stopped=" + stopped); });
    }

    @SuppressWarnings("unchecked")
    private static Map<Object, Object> cache(Class<?> type, String name) {
        try {
            var field = type.getDeclaredField(name);
            field.setAccessible(true);
            return (Map<Object, Object>) field.get(null);
        } catch (ReflectiveOperationException error) { throw new IllegalStateException(error); }
    }

    public static String run(CommandSourceStack source, String action) {
        boolean redirected = false;
        if (action.equals("break")) {
            var player = source.getServer().getPlayerList().getPlayerByName("DevPlayer");
            var pos = new BlockPos(174, 4, 174);
            player.serverLevel().setBlock(pos, HotPotContent.PLATE_HOLDER.defaultBlockState(), 3);
            boolean removed = player.gameMode.destroyBlock(pos);
            redirected = !removed && player.serverLevel().getBlockState(pos).is(HotPotContent.PLATE_HOLDER);
        }
        if (action.equals("seed")) {
            var player = source.getServer().getPlayerList().getPlayerByName("DevPlayer");
            if (player == null) player = source.getServer().getPlayerList().getPlayers().getFirst();
            seeded = player.getUUID();
            try {
                Class<?> type = Class.forName(ClothApronProtection.class.getName() + "$HeatStamp");
                var constructor = type.getDeclaredConstructors()[0];
                constructor.setAccessible(true);
                Object stamp = constructor.newInstance(0L, player.level().dimension(), BlockPos.ZERO, false);
                cache(ClothApronProtection.class, "HEAT_CACHE").put(seeded, stamp);
                cache(CuttingBoardBlock.class, "CUT_CACHE").put(Items.BEEF, new ItemStack(Items.COOKED_BEEF));
            } catch (ReflectiveOperationException error) { throw new IllegalStateException(error); }
        }
        JsonObject status = new JsonObject();
        status.addProperty("joins", joins);
        status.addProperty("leaves", leaves);
        status.addProperty("stopping", stopping);
        status.addProperty("stopped", stopped);
        status.addProperty("start_ready", startReady);
        status.addProperty("leave_clean", leaveClean);
        status.addProperty("stop_clean", stopClean);
        status.addProperty("apron_entries", cache(ClothApronProtection.class, "HEAT_CACHE").size());
        status.addProperty("board_entries", cache(CuttingBoardBlock.class, "CUT_CACHE").size());
        status.addProperty("ham_hunger", Config.foodHunger("ham_sausage", -1));
        var registries = net.minecraft.core.registries.BuiltInRegistries.RECIPE_SERIALIZER;
        boolean registrations = registries.get(HotPot.id("soup_bucket")) == SoupBucketRecipe.SERIALIZER
            && registries.get(HotPot.id("apron_recolor")) == ApronRecolorRecipe.SERIALIZER
            && net.minecraft.core.registries.BuiltInRegistries.TRIGGER_TYPES.get(HotPot.id("hotpot")) == HotPotCriterion.INSTANCE;
        for (var item : HotPotContent.CLOTH_APRONS) {
            var armor = (net.minecraft.world.item.ArmorItem) item;
            registrations &= armor.getMaterial().value() == net.minecraft.core.registries.BuiltInRegistries.ARMOR_MATERIAL.get(
                armor.getMaterial().unwrapKey().orElseThrow().location());
            registrations &= armor.getDefense() == 3 && new ItemStack(item).getMaxDamage() == 80;
        }
        status.addProperty("registrations", registrations);
        status.addProperty("break_redirect", redirected);
        return status.toString();
    }
    private static void log(String message) { HotPot.LOGGER.info("[OPT3D] {}", message); }
}
