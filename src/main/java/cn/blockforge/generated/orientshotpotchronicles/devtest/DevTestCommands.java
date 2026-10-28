package cn.blockforge.generated.orientshotpotchronicles.devtest;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.ItemStackArgumentType;
import net.minecraft.command.argument.Vec3ArgumentType;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

/**
 * 只在开发环境（runServer / runClient）注册的测试命令：让一个假玩家拿着东西去右键方块、吃东西，
 * 用 RCON 就能测交互（服务端没有真人）。打 jar 时整个 devtest 包被排除（build.gradle）。
 * <pre>
 * /hpt hold &lt;主手物品&gt; [副手物品]      物品写法同 /give，例如 orients_hotpot_chronicles:chopsticks
 * /hpt aim &lt;方块&gt; &lt;小格&gt;             模拟客户端报「这格浮着的是第几小格」（筷子夹菜用）
 * /hpt use &lt;方块&gt; &lt;点击坐标&gt; &lt;潜行&gt;   假玩家主手右键（走原版 interactBlock，和真人一样先方块后物品）
 * /hpt eat                              假玩家把主手的东西「吃完」（筷子夹着菜时 = 按住右键吃完）
 * /hpt show                             打印两只手的物品和组件、饥饿值、状态效果
 * </pre>
 * （2026-10-25）
 */
public final class DevTestCommands {
    private DevTestCommands() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
            CommandManager.literal("hpt").requires(src -> src.hasPermissionLevel(2))
                .then(CommandManager.literal("hold")
                    .then(CommandManager.argument("main", ItemStackArgumentType.itemStack(registryAccess))
                        .executes(ctx -> hold(ctx, false))
                        .then(CommandManager.argument("off", ItemStackArgumentType.itemStack(registryAccess))
                            .executes(ctx -> hold(ctx, true)))))
                .then(CommandManager.literal("use")
                    .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                        .then(CommandManager.argument("hit", Vec3ArgumentType.vec3(false))
                            .then(CommandManager.argument("sneak", BoolArgumentType.bool())
                                .executes(DevTestCommands::use)))))
                .then(CommandManager.literal("aim")
                    .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                        .then(CommandManager.argument("slot", com.mojang.brigadier.arguments.IntegerArgumentType.integer(0, 80))
                            .executes(DevTestCommands::aim))))
                .then(CommandManager.literal("eat").executes(DevTestCommands::eat))
                .then(CommandManager.literal("show").executes(DevTestCommands::show))));
    }

    private static FakePlayer player(CommandContext<ServerCommandSource> ctx) {
        ServerWorld world = ctx.getSource().getServer().getOverworld();
        return FakePlayer.get(world);
    }

    private static int hold(CommandContext<ServerCommandSource> ctx, boolean withOff) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        FakePlayer p = player(ctx);
        p.setStackInHand(Hand.MAIN_HAND, ItemStackArgumentType.getItemStackArgument(ctx, "main").createStack(1, false));
        p.setStackInHand(Hand.OFF_HAND, withOff
            ? ItemStackArgumentType.getItemStackArgument(ctx, "off").createStack(1, false) : ItemStack.EMPTY);
        p.getHungerManager().setFoodLevel(4);
        return report(ctx, "hold ok");
    }

    private static int use(CommandContext<ServerCommandSource> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        FakePlayer p = player(ctx);
        BlockPos pos = BlockPosArgumentType.getBlockPos(ctx, "pos");
        Vec3d hit = Vec3ArgumentType.getVec3(ctx, "hit");
        p.setSneaking(BoolArgumentType.getBool(ctx, "sneak"));
        p.refreshPositionAndAngles(hit.x, hit.y + 1.0, hit.z - 1.5, 0F, 30F);
        ActionResult r = p.interactionManager.interactBlock(p, p.getServerWorld(), p.getMainHandStack(), Hand.MAIN_HAND,
            new BlockHitResult(hit, Direction.UP, pos, false));
        String text = "use -> " + r;
        if (!r.isAccepted()) {
            // 真人客户端：方块和物品的 useOnBlock 都没接，就接着「对空气右键」用物品
            text += " / item -> " + p.interactionManager.interactItem(p, p.getServerWorld(), p.getMainHandStack(), Hand.MAIN_HAND)
                + (p.isUsingItem() ? " (eating)" : "");
            p.stopUsingItem();
        }
        p.setSneaking(false);
        return report(ctx, text);
    }

    /** 模拟客户端的 ChopsticksAimPayload：「我看到这口锅那格浮着的是第 slot 小格」，紧接着的 use 用它。 */
    private static int aim(CommandContext<ServerCommandSource> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        cn.blockforge.generated.orientshotpotchronicles.hotpot.ChopsticksAimPayload.remember(player(ctx),
            BlockPosArgumentType.getBlockPos(ctx, "pos"), com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "slot"));
        return report(ctx, "aim ok");
    }

    private static int eat(CommandContext<ServerCommandSource> ctx) {
        FakePlayer p = player(ctx);
        ItemStack main = p.getMainHandStack();
        ItemStack after = main.finishUsing(p.getServerWorld(), p);
        p.setStackInHand(Hand.MAIN_HAND, after);
        return report(ctx, "eat ok");
    }

    private static int show(CommandContext<ServerCommandSource> ctx) {
        return report(ctx, "show");
    }

    private static int report(CommandContext<ServerCommandSource> ctx, String head) {
        FakePlayer p = player(ctx);
        String text = head + " | main=" + describe(p.getMainHandStack()) + " | off=" + describe(p.getOffHandStack())
            + " | food=" + p.getHungerManager().getFoodLevel() + " | effects=" + p.getStatusEffects().stream()
            .map(e -> e.getEffectType().getIdAsString() + "x" + (e.getAmplifier() + 1)).toList();
        ctx.getSource().sendFeedback(() -> Text.literal(text), false);
        return 1;
    }

    private static String describe(ItemStack stack) {
        return stack.isEmpty() ? "empty" : stack.getCount() + "x" + stack.getRegistryEntry().getIdAsString()
            + stack.getComponentChanges();
    }
}
