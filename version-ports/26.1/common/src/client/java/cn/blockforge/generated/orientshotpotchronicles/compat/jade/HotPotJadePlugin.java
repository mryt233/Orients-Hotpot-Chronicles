/** 作者：mryt233。灶台、锅与桌面餐具按准星部位提供 Jade 视图。 */
package cn.blockforge.generated.orientshotpotchronicles.compat.jade;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPot;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotBlock;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotBlockEntity;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotContent;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.StoveBlock;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.StoveBlockEntity;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.StoveTopBlock;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.Chopsticks;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.DippingSauceItem;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.PlateHolderBlock;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.PlateHolderBlockEntity;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.SauceContents;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.Seasoning;
import cn.blockforge.generated.orientshotpotchronicles.client.ClientTooltips;
import cn.blockforge.generated.orientshotpotchronicles.client.SharedClientSetup;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import snownee.jade.api.Accessor;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.JadeIds;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.view.ClientViewGroup;
import snownee.jade.api.view.IClientExtensionProvider;
import snownee.jade.api.view.IServerExtensionProvider;
import snownee.jade.api.view.ItemView;
import snownee.jade.api.view.ViewGroup;

@WailaPlugin
public final class HotPotJadePlugin implements IWailaPlugin {
    private static final Identifier STOVE_STORAGE = HotPot.id("stove_item_storage");
    private static final String ITEM_STORAGE = JadeIds.UNIVERSAL_ITEM_STORAGE.toString();

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerItemStorage(new StoveStorage(), BlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerItemStorageClient(new StoveStorageClient());
        registration.registerBlockComponent(new PlateHolderComponentProvider(), PlateHolderBlock.class);
        registration.addBeforeTooltipCollectCallback((theme, accessor) -> {
            if (!accessor.isServerConnected()) {
                HotPotBlockEntity pot = aimedPot(accessor);
                if (pot != null && !accessor.getServerData().contains(ITEM_STORAGE) && !pot.isEmpty()) {
                    accessor.getServerData().put(ITEM_STORAGE, accessor.encodeAsNbt(
                        StorageCodec.STREAM_CODEC, Map.entry(STOVE_STORAGE, collectPotGroups(pot))));
                }
                PlateHolderBlockEntity.Placed plate = aimedPlate(accessor);
                if (plate != null && !accessor.getServerData().contains(ITEM_STORAGE)) {
                    List<ViewGroup<ItemStack>> groups = collectPlateGroups(plate);
                    if (!groups.isEmpty()) {
                        accessor.getServerData().put(ITEM_STORAGE, accessor.encodeAsNbt(
                            StorageCodec.STREAM_CODEC, Map.entry(STOVE_STORAGE, groups)));
                    }
                }
            }
            return true;
        });
        registration.addRayTraceCallback((hit, accessor, original) -> {
            if (!(accessor instanceof BlockAccessor block)) {
                return accessor;
            }
            BlockPos pos = block.getPosition();
            BlockState state = block.getBlockState();
            boolean upper = state.getBlock() instanceof StoveTopBlock;
            if (upper) {
                pos = pos.below();
                state = block.getLevel().getBlockState(pos);
            }
            if (!(state.getBlock() instanceof StoveBlock)
                    || !(block.getLevel().getBlockEntity(pos) instanceof StoveBlockEntity stove)) {
                return accessor;
            }
            boolean potHit = stove.hasPot() && stove.potMaterial() != null
                && StoveBlock.isPotHit(state, pos, block.getHitResult());
            if (!upper && !potHit) {
                return accessor;
            }
            BlockHitResult redirectedHit = new BlockHitResult(block.getHitResult().getLocation(),
                block.getHitResult().getDirection(), pos, block.getHitResult().isInside());
            // mryt233 改：保留真实世界命中点、灶台状态及纯净锅图标，服务端才能在下格找到实体。
            BlockAccessor.Builder builder = registration.blockAccessor().from(block).hit(redirectedHit).blockState(state)
                .blockEntity(() -> stove).fakeBlock(potHit
                    ? new ItemStack(HotPotContent.itemFor(stove.potMaterial())) : ItemStack.EMPTY);
            // 原始上格 accessor 只持有数据副本；数据中心此时仍校验上一帧的下格目标。
            CompoundTag data = registration.getServerData();
            // mryt233 改（26.1）：NBT 取值改成返回 Optional 的写法（getInt 不再直接给 int）。
            if (data != null && data.getIntOr("x", Integer.MIN_VALUE) == pos.getX()
                    && data.getIntOr("y", Integer.MIN_VALUE) == pos.getY()
                    && data.getIntOr("z", Integer.MIN_VALUE) == pos.getZ()) {
                builder.serverData(data);
            }
            return builder.requireVerification().build();
        });
    }

    @Nullable
    private static HotPotBlockEntity aimedPot(Accessor<?> accessor) {
        if (!(accessor instanceof BlockAccessor block)) {
            return null;
        }
        if (block.getBlockEntity() instanceof StoveBlockEntity stove) {
            BlockState state = stove.getBlockState();
            return stove.getBlockPos().equals(block.getPosition()) && state.getBlock() instanceof StoveBlock
                && stove.hasPot() && (block.isFakeBlock()
                    || StoveBlock.isPotHit(state, stove.getBlockPos(), block.getHitResult())) ? stove : null;
        }
        return block.getBlockEntity() instanceof HotPotBlockEntity pot
            && pot.getBlockPos().equals(block.getPosition())
            && pot.getBlockState().getBlock() instanceof HotPotBlock ? pot : null;
    }

    @Nullable
    private static HotPotBlockEntity potTarget(Accessor<?> accessor) {
        if (!(accessor instanceof BlockAccessor block)) {
            return null;
        }
        if (block.getBlockEntity() instanceof HotPotBlockEntity pot && !(pot instanceof StoveBlockEntity)) {
            return pot;
        }
        BlockPos pos = block.getPosition();
        var entity = block.getBlockEntity();
        if (!(entity instanceof StoveBlockEntity)) {
            entity = block.getLevel().getBlockEntity(pos);
        }
        if (!(entity instanceof StoveBlockEntity)
                && block.getLevel().getBlockState(pos).getBlock() instanceof StoveTopBlock) {
            entity = block.getLevel().getBlockEntity(pos.below());
        }
        // 服务端按实体提供完整数据；准星部位仅在客户端过滤，避免同格灶身缓存空食材。
        return entity instanceof StoveBlockEntity stove && stove.hasPot() ? stove : null;
    }

    private static List<ViewGroup<ItemStack>> collectPotGroups(HotPotBlockEntity pot) {
        List<ItemStack> merged = new ArrayList<>();
        for (int slot = 0; slot < pot.getContainerSize(); slot++) {
            ItemStack stack = pot.getItem(slot);
            appendMerged(merged, stack);
        }
        return merged.isEmpty() ? List.of() : List.of(new ViewGroup<>(merged));
    }

    private static void appendMerged(List<ItemStack> merged, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        for (ItemStack existing : merged) {
            if (ItemStack.isSameItemSameComponents(existing, stack)) {
                existing.grow(stack.getCount());
                return;
            }
        }
        if (merged.size() < 54) {
            merged.add(stack.copy());
        }
    }

    @Nullable
    private static PlateHolderBlockEntity plateHolderTarget(Accessor<?> accessor) {
        return accessor instanceof BlockAccessor block
            && block.getBlockEntity() instanceof PlateHolderBlockEntity holder
            && holder.getBlockPos().equals(block.getPosition()) ? holder : null;
    }

    @Nullable
    private static PlateHolderBlockEntity.Placed aimedPlate(Accessor<?> accessor) {
        PlateHolderBlockEntity holder = plateHolderTarget(accessor);
        return holder != null && accessor instanceof BlockAccessor block
            && block.getBlockState().getBlock() instanceof PlateHolderBlock
                ? PlateHolderBlock.aimedAt(block.getPosition(), block.getHitResult(), holder) : null;
    }

    private static List<ViewGroup<ItemStack>> collectPlateGroups(@Nullable PlateHolderBlockEntity.Placed plate) {
        if (plate == null) {
            return List.of();
        }
        List<ItemStack> merged = new ArrayList<>();
        if (plate.self.getItem() instanceof DippingSauceItem) {
            SauceContents contents = DippingSauceItem.contents(plate.self);
            if (contents.bites() > 0 && contents.mask() != 0) {
                for (Seasoning seasoning : Seasoning.ALL) {
                    if ((contents.mask() & seasoning.bit()) != 0 && !seasoning.raws().isEmpty()) {
                        appendMerged(merged, new ItemStack(seasoning.raw()));
                    }
                }
            }
        } else if (plate.holdsFood()) {
            for (ItemStack stack : plate.items) {
                appendMerged(merged, stack);
            }
        }
        if (plate.self.is(HotPotContent.CHOPSTICKS)) {
            appendMerged(merged, Chopsticks.held(plate.self));
        }
        if (!plate.chopsticks.isEmpty()) {
            appendMerged(merged, Chopsticks.held(plate.chopsticks));
        }
        return merged.isEmpty() ? List.of() : List.of(new ViewGroup<>(merged));
    }

    private static List<ViewGroup<ItemStack>> collectAllPlateHolderGroups(PlateHolderBlockEntity holder) {
        List<ItemStack> merged = new ArrayList<>();
        for (PlateHolderBlockEntity.Placed plate : holder.plates()) {
            for (ViewGroup<ItemStack> group : collectPlateGroups(plate)) {
                for (ItemStack stack : group.views) {
                    appendMerged(merged, stack);
                }
            }
        }
        return merged.isEmpty() ? List.of() : List.of(new ViewGroup<>(merged));
    }

    // 专用服只注册扩展；编码器留到客户端实际补数据时初始化。
    private static final class StorageCodec {
        private static final StreamCodec<RegistryFriendlyByteBuf,
            Map.Entry<Identifier, List<ViewGroup<ItemStack>>>> STREAM_CODEC =
                ViewGroup.listCodec(ItemStack.OPTIONAL_STREAM_CODEC);
    }

    private static final class StoveStorage implements IServerExtensionProvider<ItemStack> {
        @Override
        public Identifier getUid() {
            return STOVE_STORAGE;
        }

        @Override
        public List<ViewGroup<ItemStack>> getGroups(Accessor<?> accessor) {
            HotPotBlockEntity pot = potTarget(accessor);
            if (pot != null) {
                return collectPotGroups(pot);
            }
            // mryt233 改：整格非空即激活存储扩展，避免先指空盘后在限流窗口内漏显其他餐具。
            PlateHolderBlockEntity holder = plateHolderTarget(accessor);
            return holder == null ? null : collectAllPlateHolderGroups(holder);
        }

        @Override
        public boolean shouldRequestData(Accessor<?> accessor) {
            return potTarget(accessor) != null || plateHolderTarget(accessor) != null;
        }
    }

    private static final class StoveStorageClient implements IClientExtensionProvider<ItemStack, ItemView> {
        @Override
        public Identifier getUid() {
            return STOVE_STORAGE;
        }

        @Override
        public List<ClientViewGroup<ItemView>> getClientGroups(Accessor<?> accessor, List<ViewGroup<ItemStack>> groups) {
            // mryt233 改：同格餐具逐帧读取本地同步数据，空餐具也必须覆盖上一帧的服务端汇总。
            if (plateHolderTarget(accessor) != null) {
                return ClientViewGroup.map(collectPlateGroups(aimedPlate(accessor)), ItemView::new, null);
            }
            // 同格从锅移向灶身时立即隐藏，防止上一次服务端响应在刷新间隔内残留。
            HotPotBlockEntity pot = aimedPot(accessor);
            if (pot == null) {
                return List.of();
            }
            return ClientViewGroup.map(groups == null ? List.of() : groups, ItemView::new, null);
        }
    }

    private static final class PlateHolderComponentProvider implements IBlockComponentProvider {
        @Override
        public Identifier getUid() {
            return HotPot.id("plate_holder_info");
        }

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            PlateHolderBlockEntity.Placed plate = aimedPlate(accessor);
            if (plate == null) {
                return;
            }
            List<Component> lines = new ArrayList<>();
            if (plate.self.getItem() instanceof DippingSauceItem) {
                SauceContents contents = DippingSauceItem.contents(plate.self);
                if (contents.bites() > 0) {
                    Seasoning.appendEffects(contents.mask(), lines);
                    lines.add(Component.translatable(HotPotBlockEntity.key("tooltip.sauce.bites"), contents.bites())
                        .withStyle(ChatFormatting.GRAY));
                }
            } else if (plate.holdsFood()) {
                List<ItemStack> kinds = new ArrayList<>();
                for (ItemStack food : plate.items) {
                    if (!food.isEmpty() && kinds.stream().noneMatch(
                            existing -> ItemStack.isSameItemSameComponents(existing, food))) {
                        kinds.add(food);
                    }
                }
                for (ItemStack kind : kinds) {
                    List<Component> extra = new ArrayList<>();
                    ClientTooltips.appendDipped(kind, extra);
                    ClientTooltips.appendCookedSoup(kind, extra);
                    if (!extra.isEmpty()) {
                        if (kinds.size() > 1) {
                            lines.add(kind.getHoverName().copy().withStyle(ChatFormatting.GRAY));
                        }
                        lines.addAll(extra);
                    }
                }
            } else if (plate.self.is(HotPotContent.CHOPSTICKS)) {
                SharedClientSetup.appendPlateTooltip(plate.self, lines);
            }
            if (!plate.chopsticks.isEmpty()) {
                SharedClientSetup.appendPlateTooltip(plate.chopsticks, lines);
            }
            for (Component line : lines) {
                tooltip.add(line);
            }
        }
    }
}
