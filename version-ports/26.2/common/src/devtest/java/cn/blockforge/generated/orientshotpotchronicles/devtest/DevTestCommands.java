// mryt233 改：26.1 原版标识符类型。
package cn.blockforge.generated.orientshotpotchronicles.devtest;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.world.item.ItemStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

/**
 * 只在开发环境（runServer / runClient）注册的测试命令：让一个假玩家拿着东西去右键方块、吃东西，
 * 用 RCON 就能测交互（服务端没有真人）。打 jar 时整个 devtest 包被排除（build.gradle）。
 * <pre>
 * /hpt hold &lt;主手物品&gt; [副手物品]      物品写法同 /give，例如 orients_hotpot_chronicles:chopsticks
 * /hpt aim &lt;方块&gt; &lt;小格&gt;             模拟客户端报「这格浮着的是第几小格」（筷子夹菜用）
 * /hpt use &lt;方块&gt; &lt;点击坐标&gt; &lt;潜行&gt;   假玩家主手右键（走原版 interactBlock，和真人一样先方块后物品）
 * /hpt eat                              假玩家把主手的东西「吃完」（筷子夹着菜时 = 按住右键吃完）
 * /hpt show                             打印两只手的物品和组件、饥饿值、状态效果
 * /hpt join                             把假玩家真正挂进玩家列表（list / @a 能看到），之后 /recipe give、/give、/advancement 才测得了
 * /hpt leave                            把假玩家从玩家列表里移除
 * /hpt recipe &lt;配方 ID&gt;              查一条配方在不在、产物是什么（不用玩家也能测）
 * </pre>
 * 作者：mryt233 */
public final class DevTestCommands {
    private DevTestCommands() {
    }

    private static java.util.function.BiFunction<ServerLevel, com.mojang.authlib.GameProfile, ServerPlayer> playerFactory;
    public static java.util.function.Supplier<net.minecraft.network.Connection> connectionFactory = DevFakeConnection::new;

    public static void register(com.mojang.brigadier.CommandDispatcher<CommandSourceStack> dispatcher,
                                net.minecraft.commands.CommandBuildContext registryAccess,
                                java.util.function.BiFunction<ServerLevel, com.mojang.authlib.GameProfile, ServerPlayer> factory) {
        playerFactory = factory;
        dispatcher.register(
            Commands.literal("hpt").requires(src -> src.hasPermission(2))
                .then(Commands.literal("hold")
                    .then(Commands.argument("main", ItemArgument.item(registryAccess))
                        .executes(ctx -> hold(ctx, false))
                        .then(Commands.argument("off", ItemArgument.item(registryAccess))
                            .executes(ctx -> hold(ctx, true)))))
                .then(Commands.literal("use")
                    .then(Commands.argument("pos", BlockPosArgument.blockPos())
                        .then(Commands.argument("hit", Vec3Argument.vec3(false))
                            .then(Commands.argument("sneak", BoolArgumentType.bool())
                                .executes(DevTestCommands::use)))))
                .then(Commands.literal("aim")
                    .then(Commands.argument("pos", BlockPosArgument.blockPos())
                        .then(Commands.argument("slot", com.mojang.brigadier.arguments.IntegerArgumentType.integer(0, 80))
                            .executes(DevTestCommands::aim))))
                .then(Commands.literal("eat").executes(DevTestCommands::eat))
                .then(Commands.literal("join").executes(DevTestCommands::join))
                .then(Commands.literal("leave").executes(DevTestCommands::leave))
                .then(Commands.literal("recipe")
                    .then(Commands.argument("id", net.minecraft.commands.arguments.IdentifierArgument.id())
                        .executes(DevTestCommands::recipe)))
                .then(Commands.literal("show").executes(DevTestCommands::show))
                .then(Commands.literal("modules").executes(ctx -> {
                    var host = cn.blockforge.generated.orientshotpotchronicles.HotpotContentModule.HOST;
                    return report(ctx, "modules order=" + host.order() + " declarations=" + host.declarations().size()
                        + " declaration-calls=" + cn.blockforge.generated.orientshotpotchronicles.HotpotContentModule.declarationCalls());
                }))
                .then(Commands.literal("stats").executes(ctx -> report(ctx,
                    "stats " + String.join(" ; ", cn.blockforge.orient.framework.loader.RuntimeStats.get().summary()))))
                .then(Commands.literal("hooks").executes(ctx -> {
                    var host = new cn.blockforge.orient.framework.game.Hooks<java.util.function.Consumer<String>>();
                    java.util.List<String> seen = new java.util.ArrayList<>();
                    var a = host.add(1, value -> seen.add("p1:" + value));
                    host.add(5, value -> seen.add("p5:" + value));
                    host.add(3, value -> seen.add("p3:" + value));
                    host.add(9, value -> { throw new IllegalStateException("boom"); });
                    host.forEach(listener -> listener.accept("x"), (listener, error) -> seen.add("isolated:" + error.getMessage()));
                    a.unregister();
                    int after = host.size();
                    return report(ctx, "hooks order=" + seen + " after-unregister=" + after
                        + " content-hooks=" + cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotHooks.hookCounts());
                }))
                .then(Commands.literal("openscreen")
                    .then(Commands.argument("player", com.mojang.brigadier.arguments.StringArgumentType.word())
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                            .executes(DevTestCommands::openScreen)
                            .then(Commands.literal("fuel").executes(ctx -> openScreen(ctx, true)))))));
    }

    /**
     * 假玩家的档案（起用自定义名字）。
     *
     * <p>Fabric 默认档案叫 {@code [Minecraft]}，开头是方括号，命令里当玩家名写会被当成选择器解析，
     * 所以 {@code /give [Minecraft] ...} 一定失败。换成普通名字 {@code DevPlayer} 之后，
     * {@code /give DevPlayer ...}、{@code /recipe give DevPlayer ...} 都能直接写。</p>
     */
    private static final com.mojang.authlib.GameProfile PROFILE = new com.mojang.authlib.GameProfile(
        java.util.UUID.nameUUIDFromBytes("orients-hotpot-devplayer".getBytes(java.nio.charset.StandardCharsets.UTF_8)),
        "DevPlayer");

    private static ServerPlayer player(CommandContext<CommandSourceStack> ctx) {
        ServerLevel world = ctx.getSource().getServer().overworld();
        return playerFactory.apply(world, PROFILE);
    }

    private static int hold(CommandContext<CommandSourceStack> ctx, boolean withOff) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = player(ctx);
        p.setItemInHand(InteractionHand.MAIN_HAND, ItemArgument.getItem(ctx, "main").createItemStack(1, false));
        p.setItemInHand(InteractionHand.OFF_HAND, withOff
            ? ItemArgument.getItem(ctx, "off").createItemStack(1, false) : ItemStack.EMPTY);
        p.getFoodData().setFoodLevel(4);
        return report(ctx, "hold ok");
    }

    private static int use(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = player(ctx);
        BlockPos pos = BlockPosArgument.getBlockPos(ctx, "pos");
        Vec3 hit = Vec3Argument.getVec3(ctx, "hit");
        p.setShiftKeyDown(BoolArgumentType.getBool(ctx, "sneak"));
        p.snapTo(hit.x, hit.y + 1.0, hit.z - 1.5, 0F, 30F);
        InteractionResult r = p.gameMode.useItemOn(p, p.level(), p.getMainHandItem(), InteractionHand.MAIN_HAND,
            new BlockHitResult(hit, Direction.UP, pos, false));
        String text = "use -> " + r;
        if (!r.consumesAction()) {
            // 真人客户端：方块和物品的 useOnBlock 都没接，就接着「对空气右键」用物品
            text += " / item -> " + p.gameMode.useItem(p, p.level(), p.getMainHandItem(), InteractionHand.MAIN_HAND)
                + (p.isUsingItem() ? " (eating)" : "");
            p.stopUsingItem();
        }
        p.setShiftKeyDown(false);
        return report(ctx, text);
    }

    /** 模拟客户端的 ChopsticksAimPayload：「我看到这口锅那格浮着的是第 slot 小格」，紧接着的 use 用它。 */
    private static int aim(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        cn.blockforge.generated.orientshotpotchronicles.hotpot.ChopsticksAim.remember(player(ctx),
            BlockPosArgument.getBlockPos(ctx, "pos"), com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "slot"));
        return report(ctx, "aim ok");
    }

    private static int eat(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer p = player(ctx);
        ItemStack main = p.getMainHandItem();
        ItemStack after = main.finishUsingItem(p.level(), p);
        p.setItemInHand(InteractionHand.MAIN_HAND, after);
        return report(ctx, "eat ok");
    }

    /**
     * 给**真实玩家**打开方块对应的界面。
     *
     * <p>服务端验证「界面能不能打开」时用：真人客户端没有自动化手段去右键，这条命令走的是
     * 和潜行右键完全一样的那条路（{@code HotpotScreens}），所以能验证界面包 + 客户端 Screen 注册都通。</p>
     */
    private static int openScreen(CommandContext<CommandSourceStack> ctx) {
        return openScreen(ctx, false);
    }

    private static int openScreen(CommandContext<CommandSourceStack> ctx, boolean fuel) {
        String name = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "player");
        var server = ctx.getSource().getServer();
        var player = server.getPlayerList().getPlayerByName(name);
        if (player == null) {
            return report(ctx, "openscreen: 找不到在线玩家 " + name);
        }
        BlockPos pos = BlockPosArgument.getBlockPos(ctx, "pos");
        var be = player.level().getBlockEntity(pos);
        // 调整：灶台继承锅实体，显式 fuel 子命令避免始终命中锅分支。
        if (fuel && be instanceof cn.blockforge.generated.orientshotpotchronicles.hotpot.StoveBlockEntity stove) {
            cn.blockforge.generated.orientshotpotchronicles.hotpot.api.HotpotScreens.openFuel(player, stove);
            return report(ctx, "openscreen: 灶膛");
        }
        if (be instanceof cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotBlockEntity pot) {
            cn.blockforge.generated.orientshotpotchronicles.hotpot.api.HotpotScreens.openPot(player, pot);
            return report(ctx, "openscreen: 锅");
        }
        if (be instanceof cn.blockforge.generated.orientshotpotchronicles.hotpot.StoveBlockEntity stove) {
            cn.blockforge.generated.orientshotpotchronicles.hotpot.api.HotpotScreens.openFuel(player, stove);
            return report(ctx, "openscreen: 灶台");
        }
        if (be instanceof cn.blockforge.generated.orientshotpotchronicles.hotpot.SauceStationBlockEntity sauce) {
            cn.blockforge.generated.orientshotpotchronicles.hotpot.api.HotpotScreens.openSauce(player, sauce);
            return report(ctx, "openscreen: 蘸料台");
        }
        return report(ctx, "openscreen: 这个坐标不是本模组的方块实体（" + be + "）");
    }

    private static int show(CommandContext<CommandSourceStack> ctx) {
        return report(ctx, "show");
    }

    /**
     * 把假玩家真正挂进玩家列表。
     *
     * <p>Fabric 的 {@code FakePlayer} 默认只是个「像玩家」的实体，不在 {@code PlayerList} 的列表里，
     * 所以 {@code list} 看不到、{@code @a} 选不到，{@code /recipe give} 这类命令会回「No player was found」。
     * 这里用 {@link DevFakeConnection} 走一遍原版的登录流程，就能当真人玩家用。</p>
     */
    private static int join(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer p = player(ctx);
        var manager = ctx.getSource().getServer().getPlayerList();
        if (manager.getPlayer(p.getUUID()) != null) {
            return report(ctx, "join: 已经在玩家列表里");
        }
        manager.placeNewPlayer(connectionFactory.get(), p,
            net.minecraft.server.network.CommonListenerCookie.createInitial(p.getGameProfile(), false));
        return report(ctx, "join ok");
    }

    private static int leave(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer p = player(ctx);
        var manager = ctx.getSource().getServer().getPlayerList();
        if (manager.getPlayer(p.getUUID()) == null) {
            return report(ctx, "leave: 不在玩家列表里");
        }
        manager.remove(p);
        return report(ctx, "leave ok");
    }

    /** 查一条配方在不在、产物是什么。不需要玩家，比 {@code /recipe give} 稳。 */
    private static int recipe(CommandContext<CommandSourceStack> ctx) {
        net.minecraft.resources.Identifier id = net.minecraft.commands.arguments.IdentifierArgument.getId(ctx, "id");
        var server = ctx.getSource().getServer();
        var found = server.getRecipeManager().byKey(id);
        if (found.isEmpty()) {
            return report(ctx, "recipe: 不存在 " + id);
        }
        var entry = found.get();
        ItemStack result = entry.value().getResultItem(server.registryAccess());
        return report(ctx, "recipe ok " + id + " | type=" + entry.value().getType() + " | result=" + describe(result));
    }

    private static int report(CommandContext<CommandSourceStack> ctx, String head) {
        ServerPlayer p = player(ctx);
        String text = head + " | main=" + describe(p.getMainHandItem()) + " | off=" + describe(p.getOffhandItem())
            + " | food=" + p.getFoodData().getFoodLevel() + " | effects=" + p.getActiveEffects().stream()
            .map(e -> e.getEffect().getRegisteredName() + "x" + (e.getAmplifier() + 1)).toList();
        ctx.getSource().sendSuccess(() -> Component.literal(text), false);
        return 1;
    }

    private static String describe(ItemStack stack) {
        return stack.isEmpty() ? "empty" : stack.getCount() + "x" + stack.getItemHolder().getRegisteredName()
            + stack.getComponentsPatch();
    }
}
