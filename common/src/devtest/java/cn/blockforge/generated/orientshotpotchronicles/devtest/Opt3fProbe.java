// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.devtest;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.*;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

/** 第三批 3F：只在开发服务端运行的交互取证。 */
public final class Opt3fProbe {
    private Opt3fProbe() { }

    public static String run(ServerPlayer player, String action, BlockPos pos) {
        var level = player.serverLevel();
        JsonObject out = new JsonObject();
        switch (action) {
            case "menus" -> {
                var be = level.getBlockEntity(pos);
                AbstractContainerMenu menu = be instanceof StoveBlockEntity stove
                    ? new StoveScreenHandler(1, player.getInventory(), stove)
                    : be instanceof HotPotBlockEntity pot ? new HotPotScreenHandler(1, player.getInventory(), pot)
                    : new SauceStationScreenHandler(1, player.getInventory(), (SauceStationBlockEntity) be);
                player.getInventory().clearContent();
                int n = menu.slots.size() - 36;
                ItemStack input = new ItemStack(be instanceof StoveBlockEntity ? Items.COAL
                    : be instanceof HotPotBlockEntity ? Items.BEEF : HotPotContent.SCALLION, 3);
                player.getInventory().setItem(9, input);
                JsonArray slots = new JsonArray();
                for (int i = n; i < menu.slots.size(); i++) {
                    var s = menu.slots.get(i);
                    slots.add(s.getContainerSlot() + ":" + s.x + ":" + s.y);
                }
                out.add("slots", slots);
                out.addProperty("in", menu.quickMoveStack(player, n).toString());
                out.addProperty("out", menu.quickMoveStack(player, 0).toString());
                out.addProperty("negative", bounds(menu, player, -1));
                out.addProperty("end", bounds(menu, player, menu.slots.size()));
                out.addProperty("large", bounds(menu, player, Integer.MAX_VALUE));
                menu.removed(player);
            }
            case "sync" -> {
                var be = level.getBlockEntity(pos);
                var tag = be.getUpdateTag(level.registryAccess());
                var packet = (net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket) be.getUpdatePacket();
                out.addProperty("tag", tag.toString());
                out.addProperty("packet", packet.getTag().toString());
                var copy = be.getType().create(pos, be.getBlockState());
                copy.loadWithComponents(tag, level.registryAccess());
                out.addProperty("readback", copy.getUpdateTag(level.registryAccess()).toString());
            }
            case "shapes" -> {
                var block = level.getBlockState(pos).getBlock();
                if (block.hasDynamicShape()) throw new AssertionError("dynamic shape");
                var forbidden = (net.minecraft.world.level.BlockGetter) java.lang.reflect.Proxy.newProxyInstance(
                    net.minecraft.world.level.BlockGetter.class.getClassLoader(),
                    new Class<?>[]{net.minecraft.world.level.BlockGetter.class},
                    (proxy, method, args) -> { throw new AssertionError("shape read world: " + method); });
                JsonArray shapes = new JsonArray();
                for (var state : block.getStateDefinition().getPossibleStates()) {
                    shapes.add(state + ":" + state.getShape(forbidden, pos).toAabbs()
                        + ":" + state.getCollisionShape(forbidden, pos).toAabbs());
                }
                out.add("shapes", shapes);
            }
            case "hook" -> {
                int[] calls = {0};
                var before = level.getBlockEntity(pos).saveWithoutMetadata(level.registryAccess());
                var held = player.getMainHandItem().copy();
                var registration = HotPotHooks.registerBlockUse((world, p, hand, stack, at, state, hit) -> {
                    if (!at.equals(pos)) return null;
                    calls[0]++; return net.minecraft.world.ItemInteractionResult.SUCCESS;
                });
                try {
                    player.gameMode.useItemOn(player, level, player.getMainHandItem(), net.minecraft.world.InteractionHand.MAIN_HAND,
                        new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos),
                            net.minecraft.core.Direction.UP, pos, false));
                } finally { registration.unregister(); }
                out.addProperty("calls", calls[0]);
                out.addProperty("unchanged", before.equals(level.getBlockEntity(pos).saveWithoutMetadata(level.registryAccess()))
                    && ItemStack.matches(held, player.getMainHandItem()));
            }
            case "drops" -> {
                java.util.Map<String, Integer> counts = new java.util.TreeMap<>();
                for (var entity : level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                        new net.minecraft.world.phys.AABB(pos).inflate(8))) {
                    ItemStack item = entity.getItem();
                    String key = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item.getItem()).toString();
                    counts.merge(key, item.getCount(), Integer::sum);
                }
                counts.forEach(out::addProperty);
            }
            case "full" -> {
                for (int i = 0; i < 36; i++) player.getInventory().setItem(i, new ItemStack(Items.STONE, 64));
                out.addProperty("full", true);
            }
            case "clear" -> { player.getInventory().clearContent(); player.removeAllEffects(); }
            case "break", "breakpot" -> {
                player.setGameMode(GameType.SURVIVAL);
                var aim = net.minecraft.world.phys.Vec3.atCenterOf(pos);
                if (action.equals("breakpot")) aim = aim.add(0, -0.1, 0);
                player.moveTo(aim.x, aim.y - player.getEyeHeight(), aim.z - 2, 0, 0);
                if (action.equals("breakpot")) {
                    var ray = player.pick(player.blockInteractionRange(), 1.0F, false);
                    out.addProperty("before", level.getBlockState(pos).toString());
                    out.addProperty("beforeBelow", level.getBlockState(pos.below()).toString());
                    out.addProperty("ray", ray.getLocation().toString());
                    out.addProperty("rayBlock", ray instanceof net.minecraft.world.phys.BlockHitResult h ? h.getBlockPos().toString() : "miss");
                    out.addProperty("aims", StoveBlock.aimsAtPot(level.getBlockState(pos.below()), pos.below(), player));
                }
                out.addProperty("removed", player.gameMode.destroyBlock(pos));
                out.addProperty("below", level.getBlockState(pos.below()).toString());
                out.addProperty("at", level.getBlockState(pos).toString());
            }
            case "drink", "consume" -> {
                player.removeAllEffects();
                player.getFoodData().setFoodLevel(4);
                player.getFoodData().setSaturation(0);
                player.gameMode.useItem(player, level, player.getMainHandItem(), net.minecraft.world.InteractionHand.MAIN_HAND);
                try {
                    var tick = net.minecraft.world.entity.LivingEntity.class.getDeclaredMethod("updatingUsingItem");
                    tick.setAccessible(true);
                    int duration = player.getUseItemRemainingTicks();
                    if (duration <= 0) throw new AssertionError("item use did not start");
                    for (int i = 0; i < duration; i++) tick.invoke(player);
                } catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
                out.addProperty("food", player.getFoodData().getFoodLevel());
                out.addProperty("saturation", player.getFoodData().getSaturationLevel());
                JsonArray effects = new JsonArray();
                player.getActiveEffects().forEach(e -> effects.add(e.getEffect().getRegisteredName()+":"+e.getDuration()+":"+e.getAmplifier()));
                out.add("effects", effects);
                out.addProperty("hand", player.getMainHandItem().save(level.registryAccess()).toString());
            }
            default -> throw new IllegalArgumentException(action);
        }
        return out.toString();
    }

    private static String bounds(AbstractContainerMenu menu, ServerPlayer p, int index) {
        try { return menu.quickMoveStack(p, index).toString(); }
        catch (IndexOutOfBoundsException expected) { return "IndexOutOfBoundsException"; }
    }
}
