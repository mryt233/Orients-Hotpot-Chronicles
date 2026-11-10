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
 * /hpt sounds [clear]                   查看 / 清空实际下发给假玩家的声音包（mryt233 改 r146d）
 * /hpt authorspick                      直接读取假玩家「作者推荐」的真实进度（mryt233 改 r146d）
 * /hpt show                             打印两只手的物品和组件、饥饿值、状态效果
 * /hpt join                             把假玩家真正挂进玩家列表（list / @a 能看到），之后 /recipe give、/give、/advancement 才测得了
 * /hpt join progress                    使用普通服务端玩家配合空连接模拟，避开加载器禁止内置假玩家获得成就的限制（mryt233 改 r146d）
 * /hpt leave                            把假玩家从玩家列表里移除
 * /hpt recipe &lt;配方 ID&gt;              查一条配方在不在、产物是什么（不用玩家也能测）
 * </pre>
 * 作者：mryt233 */
public final class DevTestCommands {
    private DevTestCommands() {
    }

    private static java.util.function.BiFunction<ServerLevel, com.mojang.authlib.GameProfile, ServerPlayer> playerFactory;
    private static ServerPlayer progressPlayer;
    // mryt233 改（2026-11-10）：空连接没有底层 channel，加载器开发适配补发其断线生命周期。
    public static java.util.function.Consumer<ServerPlayer> fakeDisconnect = player -> { };
    public static java.util.function.Supplier<net.minecraft.network.Connection> connectionFactory = DevFakeConnection::new;
    // mryt233 改（r146e）：以四向真实交互取证，不直接往 NBT 填新字段冒充摆放。
    private static float testYaw;

    public static void register(com.mojang.brigadier.CommandDispatcher<CommandSourceStack> dispatcher,
                                net.minecraft.commands.CommandBuildContext registryAccess,
                                java.util.function.BiFunction<ServerLevel, com.mojang.authlib.GameProfile, ServerPlayer> factory) {
        playerFactory = factory;
        // mryt233 改（2026-11-10）：实际运行事件与缓存释放的开发取证。
        Opt3dRuntimeProbe.install();
        dispatcher.register(
            Commands.literal("hpt").requires(src -> src.hasPermission(2))
                // mryt233 改（2026-11-11）：3F 服务端菜单、同步与真实交互取证。
                .then(Commands.literal("opt3f")
                    .then(Commands.argument("action", com.mojang.brigadier.arguments.StringArgumentType.word())
                        .then(Commands.argument("pos", BlockPosArgument.blockPos()).executes(ctx ->
                            report(ctx, Opt3fProbe.run(player(ctx),
                                com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "action"),
                                BlockPosArgument.getBlockPos(ctx, "pos")))))))
                // mryt233 改（2026-11-11）：3G 打包 / 凳子 / 砧板 / 饮料锅的真实入口取证。
                .then(Commands.literal("opt3g")
                    .then(Commands.argument("action", com.mojang.brigadier.arguments.StringArgumentType.word())
                        .then(Commands.argument("pos", BlockPosArgument.blockPos()).executes(ctx ->
                            report(ctx, Opt3gProbe.run(player(ctx),
                                com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "action"),
                                BlockPosArgument.getBlockPos(ctx, "pos")))))))
                .then(Commands.literal("opt3d").then(Commands.argument("action", com.mojang.brigadier.arguments.StringArgumentType.word())
                    .executes(ctx -> report(ctx, Opt3dRuntimeProbe.run(ctx.getSource(),
                        com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "action"))))))
                .then(Commands.literal("facing")
                    .then(Commands.argument("yaw", com.mojang.brigadier.arguments.FloatArgumentType.floatArg())
                        .executes(ctx -> {
                            testYaw = com.mojang.brigadier.arguments.FloatArgumentType.getFloat(ctx, "yaw");
                            return report(ctx, "facing=" + testYaw);
                        })))
                .then(Commands.literal("break")
                    .then(Commands.argument("pos", BlockPosArgument.blockPos()).executes(ctx -> {
                        var pos = BlockPosArgument.getBlockPos(ctx, "pos");
                        var p = player(ctx);
                        if (p.serverLevel().getBlockEntity(pos) instanceof cn.blockforge.generated.orientshotpotchronicles.hotpot.PlateHolderBlockEntity be) {
                            cn.blockforge.generated.orientshotpotchronicles.hotpot.PlateHolderBlock.breakOne(p.serverLevel(), pos, be, p);
                            return report(ctx, "remaining=" + be.plates().size());
                        }
                        return report(ctx, "no holder");
                    })))
                // mryt233 改（2026-11-10）：打印某格的方块状态与方块实体类型（摆测试存档 / 核对用）。
                .then(Commands.literal("block")
                    .then(Commands.argument("pos", BlockPosArgument.blockPos()).executes(ctx -> {
                        var pos = BlockPosArgument.getBlockPos(ctx, "pos");
                        var world = ctx.getSource().getLevel();
                        var state = world.getBlockState(pos);
                        var be = world.getBlockEntity(pos);
                        return report(ctx, "block " + pos.toShortString() + " = "
                            + net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock())
                            + " " + state + " | be=" + (be == null ? "none"
                                : net.minecraft.core.registries.BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(be.getType())));
                    })))
                .then(Commands.literal("shape")
                    .then(Commands.argument("pos", BlockPosArgument.blockPos()).executes(ctx -> {
                        var pos = BlockPosArgument.getBlockPos(ctx, "pos");
                        var world = ctx.getSource().getLevel();
                        return report(ctx, "shape=" + world.getBlockState(pos).getCollisionShape(world, pos).bounds());
                    })))
                // mryt233 改（2026-11-10）：按 SNBT 直接拿一件带组件的物品（摆测试存档的夹具用）。
                .then(Commands.literal("holdsnbt")
                    .then(Commands.argument("snbt", com.mojang.brigadier.arguments.StringArgumentType.greedyString())
                        .executes(ctx -> {
                            com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "snbt");
                            var tag = net.minecraft.nbt.TagParser.parseTag(
                                com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "snbt"));
                            ItemStack stack = ItemStack.parseOptional(registryAccess, tag);
                            ServerPlayer p = player(ctx);
                            p.setItemInHand(InteractionHand.MAIN_HAND, stack);
                            p.getFoodData().setFoodLevel(4);
                            return report(ctx, "holdsnbt ok");
                        })))
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
                .then(Commands.literal("authorspick").executes(ctx -> {
                    var advancement = ctx.getSource().getServer().getAdvancements().get(
                        net.minecraft.resources.ResourceLocation.parse("orients_hotpot_chronicles:authors_pick"));
                    return report(ctx, "authors_pick=" + (advancement != null
                        && player(ctx).getAdvancements().getOrStartProgress(advancement).isDone()));
                }))
                .then(Commands.literal("sounds")
                    .executes(ctx -> report(ctx, "sounds=" + DevFakeConnection.sounds()))
                    .then(Commands.literal("clear").executes(ctx -> {
                        DevFakeConnection.clearSounds();
                        return report(ctx, "sounds cleared");
                    })))
                .then(Commands.literal("join").executes(DevTestCommands::join)
                    .then(Commands.literal("progress").executes(DevTestCommands::joinProgress)))
                .then(Commands.literal("leave").executes(DevTestCommands::leave))
                .then(Commands.literal("recipe")
                    .then(Commands.argument("id", net.minecraft.commands.arguments.ResourceLocationArgument.id())
                        .executes(DevTestCommands::recipe)))
                .then(Commands.literal("show").executes(DevTestCommands::show))
                // mryt233 改（2026-11-10）：重构前后属性 / 注册顺序快照对比（只用于开发环境）
                // mryt233 改（2026-11-11）：全物品规则与展示清单的前后对照。
                .then(Commands.literal("cookingstate")
                    .then(Commands.argument("pos", BlockPosArgument.blockPos()).executes(ctx ->
                        report(ctx, CookingState.read(ctx.getSource().getLevel(), BlockPosArgument.getBlockPos(ctx, "pos"))))))
                .then(Commands.literal("dumpcooking")
                    .then(Commands.argument("name", com.mojang.brigadier.arguments.StringArgumentType.word())
                        .executes(ctx -> report(ctx, "cooking -> " + CookingDump.write(
                            com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "name"),
                            ctx.getSource().getLevel())))))
                .then(Commands.literal("dumpattrs")
                    .then(Commands.argument("name", com.mojang.brigadier.arguments.StringArgumentType.word())
                        .executes(ctx -> report(ctx, "attrs -> " + AttrDump.write(
                            com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "name"),
                            ctx.getSource().getLevel(), player(ctx))))))
                // mryt233 改（2026-11-09，r146b）：打印胸甲槽那件的「算不算布围裙」，
                // 三款防烫 / 特辣不冒火都走 ClothApronItem.wornBy，这个答案就是判定结果。
                .then(Commands.literal("apron").executes(DevTestCommands::apron))
                // mryt233 改（2026-11-09，r146b）：照真实工作台构造输入、找配方、出产物——
                // 用来验「布围裙换款保留耐久 / 附魔 / 自定义名字」。
                .then(Commands.literal("craftwear")
                    .then(Commands.argument("apron", ItemArgument.item(registryAccess))
                        .then(Commands.argument("dye", ItemArgument.item(registryAccess))
                            .executes(DevTestCommands::craftWear))))
                .then(Commands.literal("modules").executes(ctx -> {
                    var host = cn.blockforge.generated.orientshotpotchronicles.HotpotContentModule.HOST;
                    return report(ctx, "modules order=" + host.order() + " declarations=" + host.declarations().size()
                        + " declaration-calls=" + cn.blockforge.generated.orientshotpotchronicles.HotpotContentModule.declarationCalls());
                }))
                .then(Commands.literal("stats").executes(ctx -> report(ctx,
                    "stats " + String.join(" ; ", cn.blockforge.orient.framework.loader.RuntimeStats.get().summary()))))
                // mryt233 改（2026-11-10，r147）：核对登记表冻结状态与晚登记是否报错（3B）
                .then(Commands.literal("freeze").executes(ctx -> {
                    var cooking = cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotCooking.class;
                    String summary = "";
                    try {
                        summary = "cooking=" + (String) cooking.getMethod("registrySummary").invoke(null);
                    } catch (ReflectiveOperationException error) {
                        summary = "cooking=<读不到>";
                    }
                    String rejected;
                    try {
                        cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotCooking.overcook(
                            net.minecraft.world.item.Items.STICK, net.minecraft.world.item.Items.STICK);
                        rejected = "未报错(错)";
                    } catch (IllegalStateException error) {
                        rejected = "已抛异常:" + error.getMessage();
                    }
                    return report(ctx, "registries " + summary
                        + " | frozen=" + (Boolean) cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotHooks
                            .registriesFrozen() + " | late=" + rejected);
                }))
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
                        + " content-hooks=" + cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotHooks.hookCounts()
                        + " heat-sources=" + cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotHeat.hookCount());
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
        if (progressPlayer != null && progressPlayer.serverLevel() == world) {
            return progressPlayer;
        }
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
        double yaw = Math.toRadians(testYaw);
        p.moveTo(hit.x + Math.sin(yaw) * 1.5, hit.y + 1.0 - p.getEyeHeight(), hit.z - Math.cos(yaw) * 1.5,
            testYaw, (float)Math.toDegrees(Math.atan2(1, 1.5)));
        InteractionResult r = p.gameMode.useItemOn(p, p.serverLevel(), p.getMainHandItem(), InteractionHand.MAIN_HAND,
            new BlockHitResult(hit, Direction.UP, pos, false));
        String text = "use -> " + r;
        if (!r.consumesAction()) {
            // 真人客户端：方块和物品的 useOnBlock 都没接，就接着「对空气右键」用物品
            text += " / item -> " + p.gameMode.useItem(p, p.serverLevel(), p.getMainHandItem(), InteractionHand.MAIN_HAND)
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

    /**
     * /hpt apron：打印假玩家胸甲槽里的东西，以及「算不算布围裙」（= {@code ClothApronItem.wornBy}）。
     * 三款防烫、变态辣不冒火，最终都落在这一个判断上。
     */
    private static int apron(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer p = player(ctx);
        var chest = p.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST);
        var hotpot = cn.blockforge.generated.orientshotpotchronicles.hotpot.ClothApronItem.class;
        boolean apron = cn.blockforge.generated.orientshotpotchronicles.hotpot.ClothApronItem.isApron(chest);
        boolean worn = cn.blockforge.generated.orientshotpotchronicles.hotpot.ClothApronItem.wornBy(p);
        return report(ctx, "apron | chest=" + net.minecraft.core.registries.BuiltInRegistries.ITEM
            .getKey(chest.getItem()) + " | isApron=" + apron + " | wornBy=" + worn);
    }

    /**
     * /hpt craftwear &lt;围裙物品&gt; &lt;染料物品&gt;：把这两样摆成 3×3 工作台的输入，按真实配方找一遍、
     * 调 assemble 出产物，放进假玩家主手。用来验换款（耐久 / 附魔 / 自定义名字要保留）。
     */
    private static int craftWear(CommandContext<CommandSourceStack> ctx)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = player(ctx);
        // 用**手里真正拿着的那件**当围裙（带耐久 / 附魔 / 自定义名字），
        // 只有手里不是对应物品时才照参数新建一件——这样测的就是真实换款路径
        ItemStack wanted = ItemArgument.getItem(ctx, "apron").createItemStack(1, false);
        ItemStack held = p.getItemInHand(InteractionHand.MAIN_HAND);
        ItemStack apron = held.getItem() == wanted.getItem() ? held.copy() : wanted;
        ItemStack dye = ItemArgument.getItem(ctx, "dye").createItemStack(1, false);
        // 3×3 工作台必须给满 9 个格子（空位给空栈），只给 2 个会抛异常
        java.util.List<ItemStack> slots = new java.util.ArrayList<>(9);
        for (int i = 0; i < 9; i++) {
            slots.add(ItemStack.EMPTY);
        }
        slots.set(0, apron);
        slots.set(1, dye);
        net.minecraft.world.item.crafting.CraftingInput input =
            net.minecraft.world.item.crafting.CraftingInput.of(3, 3, slots);
        var level = ctx.getSource().getServer().overworld();
        var recipe = level.getRecipeManager()
            .getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, input, level);
        if (recipe.isEmpty()) {
            return report(ctx, "craftwear 没有配得上这种组合的配方");
        }
        ItemStack out = recipe.get().value().assemble(input, level.registryAccess());
        p.setItemInHand(InteractionHand.MAIN_HAND, out);
        return report(ctx, "craftwear ok via " + recipe.get().id());
    }

    private static int eat(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer p = player(ctx);
        ItemStack main = p.getMainHandItem();
        ItemStack after = main.finishUsingItem(p.serverLevel(), p);
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
        var be = player.serverLevel().getBlockEntity(pos);
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

    /** mryt233 改（r146d）：成就测试走真正的进度监听；不手动授予成就，也不改加载器的假玩家策略。 */
    private static int joinProgress(CommandContext<CommandSourceStack> ctx) {
        var server = ctx.getSource().getServer();
        if (progressPlayer == null || progressPlayer.serverLevel() != server.overworld()) {
            if (server.getPlayerList().getPlayer(PROFILE.getId()) != null) {
                return report(ctx, "join progress: 请先 hpt leave");
            }
            progressPlayer = new ServerPlayer(server, server.overworld(), PROFILE,
                net.minecraft.server.level.ClientInformation.createDefault()) {
                @Override
                public void tick() {
                    // 无真实客户端驱动移动，和开发假玩家一样保持静止。
                }
            };
        }
        return join(ctx);
    }

    private static int leave(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer p = player(ctx);
        var manager = ctx.getSource().getServer().getPlayerList();
        if (manager.getPlayer(p.getUUID()) == null) {
            return report(ctx, "leave: 不在玩家列表里");
        }
        // mryt233 改（2026-11-10）：走真实断线处理，覆盖加载器的玩家下线事件与清理回调。
        fakeDisconnect.accept(p);
        p.connection.onDisconnect(new net.minecraft.network.DisconnectionDetails(Component.literal("development logout")));
        if (manager.getPlayer(p.getUUID()) != null) manager.remove(p);
        return report(ctx, "leave ok");
    }

    /** 查一条配方在不在、产物是什么。不需要玩家，比 {@code /recipe give} 稳。 */
    private static int recipe(CommandContext<CommandSourceStack> ctx) {
        net.minecraft.resources.ResourceLocation id = net.minecraft.commands.arguments.ResourceLocationArgument.getId(ctx, "id");
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
