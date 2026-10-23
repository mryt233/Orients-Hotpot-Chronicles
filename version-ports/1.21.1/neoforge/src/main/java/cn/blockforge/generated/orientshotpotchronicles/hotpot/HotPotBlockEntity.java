// 1.21.1 NeoForge 移植：Codex 改。
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import cn.blockforge.generated.orientshotpotchronicles.GeneratedMod;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * 火锅库存与烹饪数据。
 *
 * <p>独立摆放的锅使用本类；坐上灶台后的组合方块由
 * {@link StoveBlockEntity} 继承本类。因此两种形态共用完全相同的 81 格
 * 九宫格、汤底、烹饪进度和经验逻辑。</p>
 */
public class HotPotBlockEntity extends BlockEntity
    implements Inventory, NamedScreenHandlerFactory {
    /** 9x9 = 81 个食材槽，每 3x3 为一个大格。 */
    public static final int SLOTS = 81;
    /** 屏幕属性包使用 short，熟食位图每组只占 15 位，避免高位被截断。 */
    public static final int COOKED_WORD_BITS = 15;
    public static final int COOKED_WORDS = (SLOTS + COOKED_WORD_BITS - 1) / COOKED_WORD_BITS;
    /** 0汤底 1燃料类型 2余燃 3总燃 4..84熟度 85..90熟食位图。 */
    public static final int PROP_COUNT = 4 + SLOTS + COOKED_WORDS;

    private final SimpleInventory ingredients = new SimpleInventory(SLOTS);
    private final int[] doneness = new int[SLOTS];
    private final int[] cookDur = new int[SLOTS];
    private final float[] xpValue = new float[SLOTS];
    private final boolean[] cooked = new boolean[SLOTS];
    private final ItemStack[] tracked = new ItemStack[SLOTS];
    private float pendingXp = 0F;
    private int base = -1;
    /** 客户端缓动热度，用于让食材起伏和蒸汽自然启停。 */
    private float clientHeat = 0F;
    /** 新旧存档首次进入服务端 tick 时修复新增的汤底方块状态。 */
    private boolean needsVisualRepair = true;
    /** 玩家破坏回调与状态替换回调之间的一次性防重复标记。 */
    private boolean breakHandled = false;

    /** 独立火锅的方块实体构造器。 */
    public HotPotBlockEntity(BlockPos pos, BlockState state) {
        this(HotPotContent.HOT_POT_BE, pos, state);
    }

    /** 供组合灶台子类使用的构造器。 */
    protected HotPotBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        java.util.Arrays.fill(tracked, ItemStack.EMPTY);
    }

    // ------------------------------------------------------------------ tick

    public static void tick(World world, BlockPos pos, BlockState state, HotPotBlockEntity be) {
        if (world.isClient) {
            be.clientTick(world, pos, state);
            return;
        }
        be.serverTick(world);
    }

    /** 服务端烹饪：只有有锅、有汤并且热源在燃烧时推进熟度。 */
    protected void serverTick(World world) {
        if (needsVisualRepair) {
            needsVisualRepair = false;
            repairVisualState();
        }
        trackSlotChanges();
        StoveBlockEntity stove = stoveBelow();
        boolean stoveHeat = stove != null && stove.burning();
        // r102：独立锅摆在点着的篝火（热源标签）上也能煮，速度同灶台烧煤（用户定「和灶台一样快」），不耗燃料
        boolean openFire = stove == null && heatSourceBelow();
        updateStandaloneBoilingVisual((stoveHeat || openFire) && base >= 0);
        if (!hasPot() || !(stoveHeat || openFire) || base < 0) {
            return;
        }
        if (openFire && world.getTime() % StoveBlockEntity.WARMTH_INTERVAL == 0L) {
            StoveBlockEntity.giveWarmth(world, pos);   // 围锅回血，同灶台（用户要）
        }
        SoupBase soup = SoupBase.byId(base);
        float mul = soup == null ? 1F : soup.speedMul();
        float power = stoveHeat ? stove.power() : OPEN_FIRE_POWER;
        boolean dirty = false;
        for (int i = 0; i < SLOTS; i++) {
            if (cooked[i]) {
                continue;
            }
            ItemStack stack = ingredients.getStack(i);
            if (stack.isEmpty()) {
                continue;
            }
            doneness[i] = Math.min(HotPotCooking.FULL_DONE,
                doneness[i] + HotPotCooking.progressPerTick(cookDur[i], power, mul));
            dirty = true;
            if (doneness[i] >= HotPotCooking.FULL_DONE) {
                completeSlot(world, i, soup, stack);
            }
        }
        if (dirty && world.getTime() % 10L == 0L) {
            markDirty();
        }
    }

    /** 煮熟瞬间原地换成熟食，成品等待玩家从同一槽位取走。 */
    private void completeSlot(World world, int slot, SoupBase soup, ItemStack raw) {
        xpValue[slot] = HotPotCooking.xpFor(world, raw);
        ItemStack result = HotPotCooking.finish(world, raw, soup);
        // 库存与变化快照各持一份独立拷贝，任何一方被清空都不会连带抹掉另一方。
        ItemStack stored = result.copy();
        ingredients.setStack(slot, stored);
        tracked[slot] = stored.copy();
        cooked[slot] = true;
        doneness[slot] = HotPotCooking.FULL_DONE;
        markDirtyAndSync();
    }

    /** 客户端粒子表现：灶火旺时翻滚冒汽，熄火后热度平滑回落。 */
    protected void clientTick(World world, BlockPos pos, BlockState state) {
        boolean stoveShape = StoveBlock.isStove(state);
        double lift = StoveBlock.liftOf(state);
        boolean lit = hasFire();
        boolean soupVisible = hasSoup() && hasPot();
        clientHeat += ((lit && soupVisible ? 1F : 0F) - clientHeat) * 0.08F;

        if (stoveShape && lit && world.random.nextInt(2) == 0) {
            net.minecraft.util.math.Direction facing = state.get(StoveBlock.FACING);
            double side = (world.random.nextDouble() - 0.5) * 0.36;
            double x = pos.getX() + 0.5 + facing.getOffsetX() * 0.43 + facing.getOffsetZ() * side;
            double z = pos.getZ() + 0.5 + facing.getOffsetZ() * 0.43 - facing.getOffsetX() * side;
            world.addParticle(net.minecraft.particle.ParticleTypes.SMALL_FLAME,
                x, pos.getY() + 0.27, z, 0.0, 0.035, 0.0);
        }
        // 气泡由汤面的沸腾贴图表现（原版 BUBBLE_POP 是蓝色水泡，放在红汤上不对）；
        // 这里只从汤面（与模型同高：灶上 14.32/16、独立 5.6/16）缓缓冒出稀疏的热气。
        // 火星子：从锅和灶口砖墙之间那圈缝（四个面随机）往上冒；没装锅时从整个灶口里冒
        if (stoveShape && lit && world.random.nextFloat() < 0.35F) {
            double x;
            double z;
            double y;
            if (hasPot()) {
                double gap = 1.125 / 16.0;               // 灶口内沿 1/16 与锅身 1.25/16 之间
                double along = (1 + world.random.nextDouble() * 14) / 16.0;
                switch (world.random.nextInt(4)) {
                    case 0 -> { x = along; z = gap; }
                    case 1 -> { x = along; z = 1 - gap; }
                    case 2 -> { x = gap; z = along; }
                    default -> { x = 1 - gap; z = along; }
                }
                y = 11.0 / 16.0 + lift + 0.01;           // 灶台顶面（高灶台整体抬高）
            } else {
                x = (2 + world.random.nextDouble() * 12) / 16.0;
                z = (2 + world.random.nextDouble() * 12) / 16.0;
                y = 8.0 / 16.0 + lift;                   // 锅槽底上方
            }
            world.addParticle(net.minecraft.particle.ParticleTypes.SMALL_FLAME,
                pos.getX() + x, pos.getY() + y, pos.getZ() + z,
                (x - 0.5) * 0.01, 0.02 + world.random.nextDouble() * 0.02, (z - 0.5) * 0.01);
        }
        // 大滚时热气又密又急：火越旺，冒得越多、升得越快
        double surface = pos.getY() + lift + (stoveShape ? 14.32 : 5.6) / 16.0;
        if (soupVisible && clientHeat > 0.08F) {
            int puffs = world.random.nextFloat() < clientHeat * 0.6F ? 1 + (world.random.nextFloat() < clientHeat * 0.5F ? 1 : 0) : 0;
            for (int i = 0; i < puffs; i++) {
                double ox = (world.random.nextDouble() - 0.5) * 0.55;
                double oz = (world.random.nextDouble() - 0.5) * 0.55;
                world.addParticle(net.minecraft.particle.ParticleTypes.WHITE_SMOKE,
                    pos.getX() + 0.5 + ox, surface + 0.03, pos.getZ() + 0.5 + oz,
                    (world.random.nextDouble() - 0.5) * 0.01, 0.03 + clientHeat * 0.045, (world.random.nextDouble() - 0.5) * 0.01);
            }
        }
        // 咕嘟咕嘟：火越旺越密。客户端必须用不带玩家参数的重载，传 null 玩家在客户端不会出声
        if (soupVisible && clientHeat > 0.3F && world.random.nextInt(Math.max(3, Math.round(16 - 12 * clientHeat))) == 0) {
            world.playSound(pos.getX() + 0.5, surface, pos.getZ() + 0.5,
                SoundEvents.BLOCK_BUBBLE_COLUMN_BUBBLE_POP, SoundCategory.BLOCKS,
                0.18F + 0.17F * clientHeat, 0.55F + world.random.nextFloat() * 0.35F, false);
        }
        if (soupVisible && lit && world.random.nextInt(60) == 0) {
            world.playSound(pos.getX() + 0.5, surface, pos.getZ() + 0.5,
                SoundEvents.BLOCK_LAVA_POP, SoundCategory.BLOCKS,
                0.25F, 0.8F + world.random.nextFloat() * 0.4F, false);
        }
    }

    // ------------------------------------------------------------------ 灶台联动

    /**
     * 返回实际热源。组合灶台自身就是热源；独立火锅则查找正下方的组合灶台。
     */
    public StoveBlockEntity stoveBelow() {
        if (this instanceof StoveBlockEntity stove) {
            return stove;
        }
        if (world == null) {
            return null;
        }
        BlockEntity below = world.getBlockEntity(pos.down());
        return below instanceof StoveBlockEntity stove ? stove : null;
    }

    /** 当前方块是否承载一个锅。 */
    public boolean hasPot() {
        BlockState state = world == null ? getCachedState() : world.getBlockState(pos);
        if (StoveBlock.isStove(state)) {
            return state.get(StoveBlock.POT).hasPot();
        }
        return state.getBlock() instanceof HotPotBlock;
    }

    /** 组合灶台的锅状态必须与库存实体保持一致，避免旧存档出现“有库存但无锅”状态。 */
    public void repairVisualState() {
        if (world == null || world.isClient) {
            return;
        }
        BlockState state = world.getBlockState(pos);
        if (StoveBlock.isStove(state)) {
            StovePot pot = state.get(StoveBlock.POT);
            if (!pot.hasPot()) {
                // 旧版灶台没有锅库存，不能凭空猜材质；清理无法显示的幽灵内容与汤底。
                if (filledSlots() > 0) {
                    clear();
                }
                if (base >= 0) {
                    clearSoupState();
                }
                return;
            }
            SoupVisual visual = SoupVisual.fromBase(SoupBase.byId(base));
            if (pot.hasSoup() != visual.hasSoup() || state.get(StoveBlock.SOUP) != visual) {
                world.setBlockState(pos, state.with(StoveBlock.POT, pot.withSoup(visual.hasSoup()))
                    .with(StoveBlock.SOUP, visual), Block.NOTIFY_LISTENERS);
            }
        } else if (state.getBlock() instanceof HotPotBlock) {
            SoupVisual visual = SoupVisual.fromBase(SoupBase.byId(base));
            if (state.get(HotPotBlock.SOUP) != visual
                || state.get(HotPotBlock.HAS_SOUP) != visual.hasSoup()) {
                world.setBlockState(pos, state.with(HotPotBlock.HAS_SOUP, visual.hasSoup())
                    .with(HotPotBlock.SOUP, visual), Block.NOTIFY_LISTENERS);
            }
        }
    }

    /** 当前锅材质；没有锅时返回 null。 */
    public HotPotMaterial potMaterial() {
        BlockState state = world == null ? getCachedState() : world.getBlockState(pos);
        if (StoveBlock.isStove(state)) {
            return state.get(StoveBlock.POT).material();
        }
        return state.getBlock() instanceof HotPotBlock pot ? pot.material() : null;
    }

    /** 篝火上煮的火力：同灶台烧煤炭 / 木炭（HotPotFuel.COAL）。 */
    public static final float OPEN_FIRE_POWER = 1.0F;

    /** 独立锅下面是不是点着的篝火这类热源（{@link HotPotHeat}，标签 heat_sources）。 */
    public boolean heatSourceBelow() {
        return world != null && !(this instanceof StoveBlockEntity) && HotPotHeat.isHeatSource(world, pos.down());
    }

    /** 灶火是否正旺。 */
    public boolean hasFire() {
        if (world == null) {
            return false;
        }
        if (this instanceof StoveBlockEntity) {
            return world.getBlockState(pos).get(StoveBlock.LIT);
        }
        StoveBlockEntity stove = stoveBelow();
        return (stove != null && stove.burning()) || heatSourceBelow();
    }

    /** 清空汤底并同步空锅外观，拆锅或重新安装时使用。 */
    protected void clearSoupState() {
        base = -1;
        updateSoupVisual();
        markDirtyAndSync();
    }

    /** 让锅的外观状态与汤底种类保持一致。 */
    protected void updateSoupVisual() {
        if (world == null) {
            return;
        }
        SoupVisual visual = SoupVisual.fromBase(SoupBase.byId(base));
        BlockState state = world.getBlockState(pos);
        if (StoveBlock.isStove(state)) {
            StovePot pot = state.get(StoveBlock.POT);
            BlockState updated = state.with(StoveBlock.POT, pot.withSoup(visual.hasSoup()))
                .with(StoveBlock.SOUP, visual);
            world.setBlockState(pos, updated, Block.NOTIFY_LISTENERS);
        } else if (state.getBlock() instanceof HotPotBlock) {
            BlockState updated = state.with(HotPotBlock.HAS_SOUP, visual.hasSoup())
                .with(HotPotBlock.SOUP, visual)
                .with(HotPotBlock.BOILING, hasFire() && visual.hasSoup());
            world.setBlockState(pos, updated, Block.NOTIFY_LISTENERS);
        }
    }

    private void updateStandaloneBoilingVisual(boolean boiling) {
        if (world == null) {
            return;
        }
        BlockState state = world.getBlockState(pos);
        if (state.getBlock() instanceof HotPotBlock && state.get(HotPotBlock.BOILING) != boiling) {
            world.setBlockState(pos, state.with(HotPotBlock.BOILING, boiling), Block.NOTIFY_LISTENERS);
        }
    }

    // ------------------------------------------------------------------ 交互

    /** 筷子任何时候都能打开当前锅的面板。 */
    public void useWithChopsticks(PlayerEntity player) {
        if (world == null || world.isClient || !hasPot()) {
            return;
        }
        if (player instanceof ServerPlayerEntity sp) {
            sp.openMenu(this, buf -> buf.writeBlockPos(pos));
        }
    }

    /** 一锅只能有一种汤底。食材可以先放，汤底只决定后续是否加热。 */
    /**
     * 拿装汤底的空容器能不能舀：空桶舀水/牛奶/岩浆；菌汤是蘑菇煲，要用碗舀，
     * 而碗平时是捞熟食的，所以只在锅里已经没有食物时才用碗舀菌汤。
     */
    public boolean canScoopWith(ItemStack stack) {
        // r99 起碗是盛汤 / 盛熟菜（serveWithBowl），舀走汤底只用空桶；菌汤用桶是倒掉（用户定的）
        return stack.isOf(net.minecraft.item.Items.BUCKET);
    }

    /**
     * 用空容器把汤底舀出来：倒进去的是什么就还什么（水桶、牛奶桶、岩浆桶、蘑菇煲）。
     * 锅里还有食物的话，先全部掉到地上，再清空汤底。
     */
    public boolean scoopSoup(PlayerEntity player, Hand hand) {
        if (world == null || world.isClient || !hasPot()) {
            return false;
        }
        ItemStack stack = player.getStackInHand(hand);
        SoupBase soup = SoupBase.byId(base);
        if (soup == null) {
            player.sendMessage(Text.translatable(key("msg.no_soup_to_scoop")), true);
            return false;
        }
        if (!stack.isOf(soup.remainder())) {
            // 菌汤：桶装不了蘑菇煲，就是把汤倒掉，桶还是空桶（r99）
            dropContents();
            base = -1;
            updateSoupVisual();
            world.playSound(null, pos, SoundEvents.ENTITY_GENERIC_SPLASH, SoundCategory.BLOCKS, 0.6F, 1.0F);
            markDirtyAndSync();
            return true;
        }
        dropContents();
        base = -1;
        updateSoupVisual();
        player.setStackInHand(hand, net.minecraft.item.ItemUsage.exchangeStack(stack, player, new ItemStack(soup.source())));
        net.minecraft.sound.SoundEvent sound = soup == SoupBase.SPICY ? SoundEvents.ITEM_BUCKET_FILL_LAVA
            : soup.remainder() == net.minecraft.item.Items.BOWL ? SoundEvents.ITEM_BOTTLE_FILL : SoundEvents.ITEM_BUCKET_FILL;
        world.playSound(null, pos, sound, SoundCategory.BLOCKS, 1.0F, 1.0F);
        markDirtyAndSync();
        return true;
    }

    /**
     * 拿空碗点锅（r99）：锅里有熟菜就盛进碗里（先盛准星对着那一大格的，最多 3 样，碗变成装着菜的碗）；
     * 没熟菜但有汤，就盛一碗汤（锅里的汤不会少，用户定的「盛不完」）。
     */
    public boolean serveWithBowl(PlayerEntity player, Hand hand, int bigCell) {
        if (world == null || world.isClient || !hasPot()) {
            return false;
        }
        ItemStack held = player.getStackInHand(hand);
        java.util.List<ItemStack> scooped = new java.util.ArrayList<>();
        float xp = 0;
        java.util.List<Integer> order = new java.util.ArrayList<>();
        for (int i = 0; i < 9; i++) {
            order.add((bigCell + i) % 9);
        }
        outer:
        for (int cell : order) {
            int slot;
            while ((slot = firstCookedSlotInCell(cell)) >= 0) {
                ItemStack food = ingredients.getStack(slot).copy();
                xp += xpValue[slot] * food.getCount();
                ingredients.setStack(slot, ItemStack.EMPTY);
                resetSlot(slot, ItemStack.EMPTY);
                // 一格里只有 1 个；万一有多个，多出来的放回去
                scooped.add(food.copyWithCount(1));
                if (food.getCount() > 1) {
                    ingredients.setStack(slot, food.copyWithCount(food.getCount() - 1));
                }
                if (scooped.size() >= 3) {
                    break outer;
                }
            }
        }
        ItemStack result;
        if (!scooped.isEmpty()) {
            result = new ItemStack(net.minecraft.item.Items.BOWL);
            result.set(net.minecraft.component.DataComponentTypes.CONTAINER,
                net.minecraft.component.type.ContainerComponent.fromStacks(scooped));
            PlateItem.syncPlateModel(result);
            if (xp > 0) {
                net.minecraft.entity.ExperienceOrbEntity.spawn((net.minecraft.server.world.ServerWorld) world,
                    Vec3d.ofCenter(pos).add(0, 0.6, 0), Math.max(1, Math.round(xp)));
            }
            world.playSound(null, pos, SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.BLOCKS, 0.35F, 1.2F);
        } else if (SoupBase.byId(base) != null) {
            result = new ItemStack(HotPotContent.SOUP_BOWLS.get(base));
            world.playSound(null, pos, SoundEvents.ITEM_BOTTLE_FILL, SoundCategory.BLOCKS, 1.0F, 1.0F);
        } else {
            player.sendMessage(Text.translatable(key("msg.bowl_nothing")), true);
            return false;
        }
        player.setStackInHand(hand, net.minecraft.item.ItemUsage.exchangeStack(held, player, result));
        markDirtyAndSync();
        return true;
    }

    public boolean pourSoup(PlayerEntity player, Hand hand) {
        if (world == null || world.isClient || !hasPot()) {
            return false;
        }
        ItemStack stack = player.getStackInHand(hand);
        SoupBase soup = SoupBase.fromStack(stack);
        if (soup == null) {
            return false;
        }
        if (base >= 0) {
            player.sendMessage(Text.translatable(key("msg.already_soup")), true);
            return false;
        }
        base = soup.ordinal();
        consumeHand(player, hand, soup.remainder());
        updateSoupVisual();
        world.playSound(null, pos, SoundEvents.ITEM_BUCKET_EMPTY,
            SoundCategory.BLOCKS, 1.0F, 1.15F);
        markDirtyAndSync();
        return true;
    }

    /** 从手中直接放入第一个空槽，面板内点击也使用同样的槽位规则。 */
    /** 筷子上夹着的食物放进锅里指定的小格（熟食、汤底原料放不进）。 */
    public boolean insertHeld(PlayerEntity player, ItemStack chopsticks, int slot) {
        if (world == null || world.isClient || !hasPot()) {
            return false;
        }
        ItemStack food = Chopsticks.held(chopsticks);
        if (!HotPotCooking.acceptable(world, food)) {
            player.sendMessage(Text.translatable(key("msg.cannot_cook")), true);
            return false;
        }
        if (slot < 0 || slot >= SLOTS) {
            player.sendMessage(Text.translatable(key("msg.cell_full")), true);
            return false;
        }
        if (!ingredients.getStack(slot).isEmpty()) {
            player.sendMessage(Text.translatable(key("msg.slot_occupied")), true);
            return false;
        }
        ingredients.setStack(slot, food.copyWithCount(1));
        Chopsticks.hold(chopsticks, ItemStack.EMPTY);
        onSlotStackChanged(slot);
        world.playSound(null, pos, SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.BLOCKS, 0.35F, 1.35F);
        return true;
    }

    /**
     * 拿着装了菜的盘子点锅：能下锅的生食材随机倒进九个大格的空位，熟食和放不下的留在盘子里。
     * 一叠盘子只倒最上面那一个。
     */
    public boolean pourPlate(PlayerEntity player, Hand hand) {
        if (world == null || world.isClient || !hasPot()) {
            return false;
        }
        ItemStack held = player.getStackInHand(hand);
        PlateBlock plateBlock = PlateItem.placedFor(held);
        if (plateBlock == null || PlateItem.keepsSelf(held)) {
            return false;
        }
        net.minecraft.component.type.ContainerComponent contents = held.getOrDefault(
            net.minecraft.component.DataComponentTypes.CONTAINER, net.minecraft.component.type.ContainerComponent.DEFAULT);
        DefaultedList<ItemStack> foods = DefaultedList.ofSize(plateBlock.size().capacity(), ItemStack.EMPTY);
        contents.copyTo(foods);
        if (foods.stream().allMatch(ItemStack::isEmpty)) {
            player.sendMessage(Text.translatable(key("msg.plate_empty")), true);
            return false;
        }
        int moved = 0;
        for (int i = 0; i < foods.size(); i++) {
            ItemStack food = foods.get(i);
            if (food.isEmpty() || !HotPotCooking.acceptable(world, food)) {
                continue;
            }
            int slot = randomEmptySlot();
            if (slot < 0) {
                break;
            }
            ingredients.setStack(slot, food.copyWithCount(1));
            onSlotStackChanged(slot);
            foods.set(i, ItemStack.EMPTY);
            moved++;
        }
        if (moved == 0) {
            player.sendMessage(Text.translatable(key("msg.plate_nothing_to_pour")), true);
            return false;
        }
        ItemStack plate = held.getCount() > 1 ? held.split(1) : held;
        plate.set(net.minecraft.component.DataComponentTypes.CONTAINER,
            net.minecraft.component.type.ContainerComponent.fromStacks(foods));
        PlateItem.syncPlateModel(plate);
        if (plate != held && !player.getInventory().insertStack(plate)) {
            player.dropItem(plate, false);
        }
        world.playSound(null, pos, SoundEvents.ENTITY_GENERIC_SPLASH, SoundCategory.BLOCKS, 0.4F, 1.4F);
        return true;
    }

    public boolean insertIngredient(PlayerEntity player, Hand hand) {
        int slot = firstEmptySlot();
        if (slot < 0) {
            player.sendMessage(Text.translatable(key("msg.pot_full")), true);
            return false;
        }
        return insertIngredient(player, hand, slot);
    }

    /** 将一份食材放入准星对应槽位；占用时不挪动旧物品。 */
    public boolean insertIngredient(PlayerEntity player, Hand hand, int slot) {
        if (world == null || world.isClient || !hasPot()) {
            return false;
        }
        ItemStack handStack = player.getStackInHand(hand);
        if (!HotPotCooking.acceptable(world, handStack)) {
            return false;
        }
        if (slot < 0 || slot >= SLOTS) {
            player.sendMessage(Text.translatable(key("msg.cell_full")), true);
            return false;
        }
        if (!ingredients.getStack(slot).isEmpty()) {
            player.sendMessage(Text.translatable(key("msg.slot_occupied")), true);
            return false;
        }
        ingredients.setStack(slot, handStack.copyWithCount(1));
        consumeHand(player, hand, net.minecraft.item.Items.AIR);
        onSlotStackChanged(slot);
        world.playSound(null, pos, SoundEvents.ENTITY_ITEM_PICKUP,
            SoundCategory.BLOCKS, 0.35F, 1.35F);
        return true;
    }

    /**
     * 服务端原子取件：先从熟食槽移出整份，再尝试放入背包，余量安全掉在锅边。
     * 只有实际移出的数量会结算经验，调用者重复发包也不会复制物品。
     */
    public boolean collectCookedSlot(PlayerEntity player, int slot) {
        int delivered = transferCookedSlot(player, slot);
        if (delivered > 0) {
            finishCollection();
        }
        return delivered > 0;
    }

    /** 双击面板时一次性收集全部同类熟食，超出背包容量的部分安全掉落。 */
    public int collectMatchingCooked(PlayerEntity player, ItemStack target) {
        if (world == null || world.isClient || target.isEmpty()) {
            return 0;
        }
        ItemStack key = target.copyWithCount(1);
        int total = 0;
        for (int slot = 0; slot < SLOTS; slot++) {
            ItemStack stack = ingredients.getStack(slot);
            if (cooked[slot] && !stack.isEmpty()
                && ItemStack.areItemsAndComponentsEqual(stack, key)) {
                total += transferCookedSlot(player, slot);
            }
        }
        if (total > 0) {
            finishCollection();
        }
        return total;
    }

    private int transferCookedSlot(PlayerEntity player, int slot) {
        if (world == null || world.isClient || slot < 0 || slot >= SLOTS || !cooked[slot]) {
            return 0;
        }
        ItemStack stored = ingredients.getStack(slot);
        if (stored.isEmpty()) {
            resetSlot(slot, ItemStack.EMPTY);
            return 0;
        }

        float slotXp = xpValue[slot];
        // 不用 copyAndEmpty()：它会把库存里那个 ItemStack 对象自己清成 0 个。
        // 只要该对象被别处共享（旧存档、第三方配方缓存等），就会连带抹掉其它槽位。
        // 这里只接管一份拷贝，再显式把锅格置空。
        ItemStack moved = stored.copy();
        ingredients.setStack(slot, ItemStack.EMPTY);
        resetSlot(slot, ItemStack.EMPTY);

        int total = moved.getCount();
        ItemStack remainder = moved.copy();
        player.getInventory().insertStack(remainder);
        int delivered = total - remainder.getCount();
        if (!remainder.isEmpty()) {
            ItemEntity dropped = new ItemEntity(world,
                pos.getX() + 0.5, pos.getY() + 1.15, pos.getZ() + 0.5,
                remainder.copy(), 0.0, 0.16, 0.0);
            dropped.setToDefaultPickupDelay();
            dropped.setOwner(player.getUuid());
            if (world.spawnEntity(dropped)) {
                delivered += remainder.getCount();
            } else {
                ingredients.setStack(slot, remainder.copy());
                tracked[slot] = remainder.copy();
                cooked[slot] = true;
                doneness[slot] = HotPotCooking.FULL_DONE;
                xpValue[slot] = slotXp;
                cookDur[slot] = HotPotCooking.cookTicks(world, remainder);
            }
        }
        if (delivered > 0) {
            grantTakeXp(slotXp, delivered);
        }
        return delivered;
    }

    private void finishCollection() {
        world.playSound(null, pos, SoundEvents.ENTITY_ITEM_PICKUP,
            SoundCategory.BLOCKS, 0.35F, 1.2F);
        markDirtyAndSync();
    }

    /** 取出成品时结算经验，经验球在锅的位置生成。 */
    public void grantTakeXp(PlayerEntity player, int slot, int count) {
        if (world == null || world.isClient || count <= 0
            || slot < 0 || slot >= SLOTS || !cooked[slot]) {
            return;
        }
        grantTakeXp(xpValue[slot], count);
        tracked[slot] = ingredients.getStack(slot).copy();
        markDirty();
    }

    private void grantTakeXp(float perItem, int count) {
        pendingXp += perItem * count;
        int whole = (int) pendingXp;
        if (whole > 0) {
            pendingXp -= whole;
            if (world instanceof ServerWorld sw) {
                ExperienceOrbEntity.spawn(sw, Vec3d.ofCenter(pos).add(0.0, 0.7, 0.0), whole);
            }
        }
    }

    // ------------------------------------------------------------------ 槽位管理

    private void trackSlotChanges() {
        for (int i = 0; i < SLOTS; i++) {
            ItemStack stack = ingredients.getStack(i);
            ItemStack snapshot = tracked[i];
            if (snapshot == null || !ItemStack.areEqual(snapshot, stack)) {
                resetSlot(i, stack);
                markDirtyAndSync();
            }
        }
    }

    private void resetSlot(int slot, ItemStack stack) {
        tracked[slot] = stack.copy();
        doneness[slot] = 0;
        cooked[slot] = false;
        xpValue[slot] = 0F;
        cookDur[slot] = stack.isEmpty() ? 0 : HotPotCooking.cookTicks(world, stack);
    }

    public void onSlotStackChanged(int slot) {
        if (slot >= 0 && slot < SLOTS) {
            resetSlot(slot, ingredients.getStack(slot));
            markDirtyAndSync();
        }
    }

    /** 成品被取空后立即重置该槽，允许下一次放入食材。 */
    public void onSlotEmptied(int slot) {
        if (slot >= 0 && slot < SLOTS && ingredients.getStack(slot).isEmpty()) {
            resetSlot(slot, ItemStack.EMPTY);
            markDirtyAndSync();
        }
    }

    // ------------------------------------------------------------------ 查询

    public int base() {
        return base;
    }

    public SoupVisual soupVisual() {
        return SoupVisual.fromBase(SoupBase.byId(base));
    }

    public float clientHeat() {
        return clientHeat;
    }

    public boolean hasSoup() {
        return base >= 0;
    }

    public int filledSlots() {
        int n = 0;
        for (int i = 0; i < SLOTS; i++) {
            if (!ingredients.getStack(i).isEmpty()) {
                n++;
            }
        }
        return n;
    }

    public int firstEmptySlot() {
        for (int slot = 0; slot < SLOTS; slot++) {
            if (ingredients.getStack(slot).isEmpty()) {
                return slot;
            }
        }
        return -1;
    }

    /** 锅里是否还有没煮熟的食材，供灶台判断要不要继续添燃料。 */
    public boolean hasRawIngredient() {
        for (int i = 0; i < SLOTS; i++) {
            if (!cooked[i] && !ingredients.getStack(i).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /** 锅里有食物，而且全部都熟了：灶台据此进入保温，不再消耗燃料。 */
    public boolean onlyCookedFood() {
        boolean any = false;
        for (int i = 0; i < SLOTS; i++) {
            if (!ingredients.getStack(i).isEmpty()) {
                if (!cooked[i]) {
                    return false;
                }
                any = true;
            }
        }
        return any;
    }

    /**
     * 整盘倒菜用：每一样随机挑一个还有空位的大格（九宫格之一），放进那格的第一个空小格，
     * 所以每个大格分到几样是随机的（r97，用户要求，Claude）。锅满了返回 -1。
     */
    private int randomEmptySlot() {
        int[] open = new int[9];
        int n = 0;
        for (int cell = 0; cell < 9; cell++) {
            if (firstEmptySlotInCell(cell) >= 0) {
                open[n++] = cell;
            }
        }
        return n == 0 ? -1 : firstEmptySlotInCell(open[world.random.nextInt(n)]);
    }

    public int firstEmptySlotInCell(int bigCell) {
        return firstSlotInCell(bigCell, slot -> ingredients.getStack(slot).isEmpty());
    }

    public int firstCookedSlotInCell(int bigCell) {
        return firstSlotInCell(bigCell, slot -> cooked[slot] && !ingredients.getStack(slot).isEmpty());
    }

    private int firstSlotInCell(int bigCell, java.util.function.IntPredicate matches) {
        if (bigCell < 0 || bigCell > 8) {
            return -1;
        }
        int baseRow = (bigCell / 3) * 3;
        int baseCol = (bigCell % 3) * 3;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int slot = (baseRow + row) * 9 + baseCol + col;
                if (matches.test(slot)) {
                    return slot;
                }
            }
        }
        return -1;
    }

    public int donenessOf(int slot) {
        return slot >= 0 && slot < SLOTS ? doneness[slot] : 0;
    }

    public boolean isCookedSlot(int slot) {
        return slot >= 0 && slot < SLOTS && cooked[slot];
    }

    public int propertyAt(int index) {
        if (index == 0) {
            return base;
        }
        if (index >= 4 && index < 4 + SLOTS) {
            return doneness[index - 4];
        }
        int cookedStart = 4 + SLOTS;
        if (index >= cookedStart && index < PROP_COUNT) {
            int word = index - cookedStart;
            int bits = 0;
            for (int b = 0; b < COOKED_WORD_BITS; b++) {
                int slot = word * COOKED_WORD_BITS + b;
                if (slot < SLOTS && cooked[slot]) {
                    bits |= 1 << b;
                }
            }
            return bits;
        }
        StoveBlockEntity stove = stoveBelow();
        return switch (index) {
            case 1 -> stove == null ? -1 : stove.fuelType();
            case 2 -> stove == null ? 0 : stove.displayBurnTicks();
            default -> stove == null ? 0 : stove.displayBurnTotal();
        };
    }

    public int comparatorLevel() {
        int total = 0;
        for (int i = 0; i < SLOTS; i++) {
            if (!ingredients.getStack(i).isEmpty()) {
                total++;
            }
        }
        return Math.min(15, total * 15 / SLOTS + (total > 0 ? 1 : 0));
    }

    public Text statusText() {
        SoupBase soup = SoupBase.byId(base);
        Text soupText = soup == null
            ? Text.translatable(key("soup.none"))
            : Text.translatable(soup.nameKey());
        StoveBlockEntity stove = stoveBelow();
        Text fireText;
        if (stove == null && heatSourceBelow()) {
            fireText = Text.translatable(key("heat.open_fire"));
        } else if (stove == null) {
            fireText = Text.translatable(key("stove.none"));
        } else if (stove.idle()) {
            fireText = Text.translatable(key("fuel.idle"));
        } else if (stove.burning()) {
            HotPotFuel fuel = HotPotFuel.byId(stove.fuelType());
            fireText = Text.translatable(fuel == null ? key("fuel.none") : fuel.nameKey())
                .append(Text.literal(" " + (stove.burnTicks() / 20) + "s"));
        } else {
            fireText = Text.translatable(key("fuel.none"));
        }
        return Text.translatable(key("msg.status"), soupText, filledSlots(), fireText);
    }

    /** 标记本次破坏已经由玩家回调处理，避免 onStateReplaced 重复掉落。 */
    public void markBreakHandled() {
        breakHandled = true;
    }

    /** 消费一次性破坏标记；没有标记时返回 false。 */
    public boolean consumeBreakHandled() {
        boolean handled = breakHandled;
        breakHandled = false;
        return handled;
    }

    /** 掉落所有食材，不改变锅/灶台本体状态。 */
    public void dropContents() {
        if (world == null || world.isClient) {
            return;
        }
        for (int i = 0; i < SLOTS; i++) {
            ItemStack stack = ingredients.getStack(i);
            if (!stack.isEmpty()) {
                Block.dropStack(world, pos, stack);
                ingredients.setStack(i, ItemStack.EMPTY);
                resetSlot(i, ItemStack.EMPTY);
            }
        }
        markDirtyAndSync();
    }

    // ------------------------------------------------------------------ 整锅打包 / 冷热

    /** 存进锅物品里的字段名，复制方块（Ctrl+中键）时要从方块实体数据里去掉，免得重复。 */
    private static final String[] POT_DATA_KEYS = {"Base", "Doneness", "Cooked", "SlotXp", "PendingXp", "Items"};

    /** 锅里有没有东西（汤底或食材）。 */
    public boolean hasPotContents() {
        return base >= 0 || !ingredients.isEmpty();
    }

    /** 把整锅（汤底 + 食材 + 熟度）打包成一个锅物品；空锅就是普通锅物品。 */
    public ItemStack toPotItem(HotPotMaterial material) {
        ItemStack stack = new ItemStack(HotPotContent.itemFor(material));
        if (world != null && hasPotContents()) {
            NbtCompound data = new NbtCompound();
            writePotData(data, world.getRegistryManager());
            stack.set(HotPotContent.POT_CONTENTS, NbtComponent.of(data));
        }
        return stack;
    }

    /** 摆出装着东西的锅时，把物品里打包的汤底和食材原样放回来。 */
    public void loadPotContents(ItemStack stack) {
        NbtComponent data = stack.get(HotPotContent.POT_CONTENTS);
        if (data == null || world == null || world.isClient) {
            return;
        }
        readPotData(data.copyNbt(), world.getRegistryManager());
        updateSoupVisual();
        markDirtyAndSync();
    }

    /** 清空锅里的数据（已经打包进物品之后调用），不碰方块状态。 */
    public void clearPotContents() {
        ingredients.clear();
        for (int i = 0; i < SLOTS; i++) {
            resetSlot(i, ItemStack.EMPTY);
        }
        pendingXp = 0F;
        base = -1;
        markDirty();
    }

    /**
     * 锅现在是不是热的：坐在烧着的灶台上（含保温），或者摆在能加热的方块上。
     * 能加热的方块由 {@link HotPotHeat} 决定（标签 + 联动注册），以后农夫乐事的炉灶从那里接入。
     */
    public boolean isHeated() {
        if (world == null || !hasPot()) {
            return false;
        }
        StoveBlockEntity stove = stoveBelow();
        if (stove != null && stove.burning()) {
            return true;
        }
        return !(this instanceof StoveBlockEntity) && HotPotHeat.isHeatSource(world, pos.down());
    }

    /** 方块被破坏掉落时（战利品表 copy_components），锅里的东西跟着锅物品走。 */
    @Override
    protected void addComponents(ComponentMap.Builder builder) {
        super.addComponents(builder);
        if (!(this instanceof StoveBlockEntity) && world != null && hasPotContents()) {
            NbtCompound data = new NbtCompound();
            writePotData(data, world.getRegistryManager());
            builder.add(HotPotContent.POT_CONTENTS, NbtComponent.of(data));
        }
    }

    /** 直接摆出独立锅时，原版 BlockItem 会调用这里读物品组件。 */
    @Override
    protected void readComponents(BlockEntity.ComponentsAccess components) {
        super.readComponents(components);
        NbtComponent data = components.get(HotPotContent.POT_CONTENTS);
        if (data != null && world != null && !(this instanceof StoveBlockEntity)) {
            readPotData(data.copyNbt(), world.getRegistryManager());
            markDirtyAndSync();
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void removeFromCopiedStackNbt(NbtCompound nbt) {
        super.removeFromCopiedStackNbt(nbt);
        for (String key : POT_DATA_KEYS) {
            nbt.remove(key);
        }
    }

    private static void consumeHand(PlayerEntity player, Hand hand, Item remainder) {
        ItemStack stack = player.getStackInHand(hand);
        if (player.getAbilities().creativeMode) {
            return;
        }
        if (stack.getCount() > 1) {
            stack.decrement(1);
            if (remainder != net.minecraft.item.Items.AIR) {
                ItemStack back = new ItemStack(remainder);
                if (!player.getInventory().insertStack(back)) {
                    player.dropItem(back, false);
                }
            }
        } else {
            player.setStackInHand(hand,
                remainder == net.minecraft.item.Items.AIR ? ItemStack.EMPTY : new ItemStack(remainder));
        }
    }

    public static String key(String name) {
        return "text." + GeneratedMod.MOD_ID + "." + name;
    }

    // ------------------------------------------------------------------ Inventory 委托

    @Override
    public int size() {
        return SLOTS;
    }

    @Override
    public boolean isEmpty() {
        return ingredients.isEmpty();
    }

    @Override
    public ItemStack getStack(int slot) {
        return ingredients.getStack(slot);
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        ItemStack result = ingredients.removeStack(slot, amount);
        if (!result.isEmpty()) {
            markDirtyAndSync();
        }
        return result;
    }

    @Override
    public ItemStack removeStack(int slot) {
        ItemStack result = ingredients.removeStack(slot);
        if (!result.isEmpty()) {
            markDirtyAndSync();
        }
        return result;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        if (slot < 0 || slot >= SLOTS) {
            return;
        }
        // 面板的每个格子代表一份食材。无论调用方是普通点击、Shift
        // 快速移动还是旧存档恢复，都不允许把多份物品压进同一个物理槽。
        ItemStack single = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
        ingredients.setStack(slot, single);
        onSlotStackChanged(slot);
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return !isRemoved() && player.canInteractWithBlockAt(pos, 5.0);
    }

    @Override
    public void clear() {
        ingredients.clear();
        for (int i = 0; i < SLOTS; i++) {
            resetSlot(i, ItemStack.EMPTY);
        }
        markDirtyAndSync();
    }

    // ------------------------------------------------------------------ 屏幕工厂

    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new HotPotScreenHandler(syncId, playerInventory, this);
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable(key("screen.title"));
    }

    // ------------------------------------------------------------------ 同步与 NBT

    /** 保存并立即把当前方块实体 NBT 推送给附近客户端。 */
    protected void markDirtyAndSync() {
        markDirty();
        if (world != null && !world.isClient) {
            BlockState state = world.getBlockState(pos);
            world.updateListeners(pos, state, state, Block.NOTIFY_LISTENERS);
            if (world instanceof ServerWorld serverWorld) {
                serverWorld.getChunkManager().markForUpdate(pos);
            }
        }
    }

    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registries) {
        return createNbt(registries);
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.writeNbt(nbt, registries);
        writePotData(nbt, registries);
    }

    /** 锅里的东西（汤底、81 格食材、熟度、经验）；方块存档和"装着东西的锅"物品共用这一份格式。 */
    private void writePotData(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        nbt.putInt("Base", base);
        nbt.putIntArray("Doneness", doneness);
        byte[] cookedBytes = new byte[SLOTS];
        for (int i = 0; i < SLOTS; i++) {
            cookedBytes[i] = (byte) (cooked[i] ? 1 : 0);
        }
        nbt.putByteArray("Cooked", cookedBytes);
        int[] xp = new int[SLOTS];
        for (int i = 0; i < SLOTS; i++) {
            xp[i] = Math.round(xpValue[i] * 100F);
        }
        nbt.putIntArray("SlotXp", xp);
        nbt.putFloat("PendingXp", pendingXp);
        // 用带 Slot 索引的列表保存 81 格，避免 SimpleInventory.toNbtList 只按顺序
        // 写出、恢复时把物品重排到槽 0（表现为“物品乱换位置/全部挤到左上角”）。
        DefaultedList<ItemStack> list = DefaultedList.ofSize(SLOTS, ItemStack.EMPTY);
        for (int i = 0; i < SLOTS; i++) {
            list.set(i, ingredients.getStack(i));
        }
        Inventories.writeNbt(nbt, list, registries);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.readNbt(nbt, registries);
        readPotData(nbt, registries);
    }

    private void readPotData(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        base = nbt.contains("Base") ? nbt.getInt("Base") : -1;
        java.util.Arrays.fill(doneness, 0);
        int[] loaded = nbt.getIntArray("Doneness");
        System.arraycopy(loaded, 0, doneness, 0, Math.min(loaded.length, SLOTS));
        java.util.Arrays.fill(cooked, false);
        byte[] cookedBytes = nbt.getByteArray("Cooked");
        for (int i = 0; i < SLOTS; i++) {
            cooked[i] = i < cookedBytes.length && cookedBytes[i] != 0;
        }
        java.util.Arrays.fill(xpValue, 0F);
        int[] xp = nbt.getIntArray("SlotXp");
        for (int i = 0; i < Math.min(xp.length, SLOTS); i++) {
            xpValue[i] = xp[i] / 100F;
        }
        pendingXp = nbt.getFloat("PendingXp");
        // 按 Slot 索引恢复，不再依赖 SimpleInventory.readNbtList 的顺序重排。
        loadIngredients(nbt, registries);
        // 兼容处理：旧版本或异常网络包可能曾把堆叠数量写入锅槽；加载时按物理槽语义截断，
        // 但保留每份物品自身的 NBT/Data Component，避免生熟状态被粗暴合并。
        for (int i = 0; i < SLOTS; i++) {
            ItemStack stack = ingredients.getStack(i);
            tracked[i] = stack.copy();
            cookDur[i] = stack.isEmpty() ? 0 : HotPotCooking.cookTicks(world, stack);
        }
        needsVisualRepair = true;
        repairVisualState();
    }

    /**
     * 从 NBT 恢复 81 格食材。新版存档每条都带 {@code Slot} 索引，按索引放回原槽；
     * 旧版（{@code SimpleInventory.toNbtList} 生成、无 {@code Slot} 字段）退化为按写入顺序
     * 依次放入 0..N，保持与旧行为一致。这样无论走哪条路径，物品都不会被重排到左上角。
     */
    private void loadIngredients(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        ingredients.clear();
        NbtList list = nbt.getList("Items", NbtElement.COMPOUND_TYPE);
        int sequential = 0;
        for (int i = 0; i < list.size(); i++) {
            NbtCompound entry = list.getCompound(i);
            int slot = entry.contains("Slot") ? (entry.getByte("Slot") & 0xFF) : sequential;
            ItemStack stack = ItemStack.fromNbt(registries, entry).orElse(ItemStack.EMPTY);
            if (slot >= 0 && slot < SLOTS && !stack.isEmpty()) {
                if (stack.getCount() > 1) {
                    stack = stack.copyWithCount(1);
                }
                ingredients.setStack(slot, stack);
            }
            sequential++;
        }
    }
}
