/** 作者：mryt233。灶台、锅与桌面餐具按准星部位提供 Jade 视图。 */
package cn.blockforge.generated.orientshotpotchronicles.compat.jade;

// mryt233 改（2026-11-11）：限定类名改用 import，逻辑不变。
import cn.blockforge.generated.orientshotpotchronicles.hotpot.ChopstickRestBlock;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.SauceBottleBlock;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.StoneMillBlock;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.StoneMillBlockEntity;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.StoneMillRecipe;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.WoodenVatBlock;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.WoodenVatBlockEntity;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.WoodenVatRecipe;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPot;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotBlock;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotBlockEntity;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotContent;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotTableBlock;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotTableBlockEntity;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.PrepRackBlock;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.PrepRackBlockEntity;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.RackRules;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.StoveBlock;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.StoveBlockEntity;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.StoveTopBlock;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.TableMaterialData;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.TableMaterials;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.Chopsticks;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.DippingSauceItem;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.PlateHolderBlock;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.PlateHolderBlockEntity;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.SauceContents;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.SauceIds;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.Seasoning;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.SeasoningDef;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.Seasonings;
import cn.blockforge.generated.orientshotpotchronicles.client.ClientTooltips;
import cn.blockforge.generated.orientshotpotchronicles.client.SharedClientSetup;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
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
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.view.ClientViewGroup;
import snownee.jade.api.view.IClientExtensionProvider;
import snownee.jade.api.view.IServerExtensionProvider;
import snownee.jade.api.view.ItemView;
import snownee.jade.api.view.ViewGroup;

@WailaPlugin
public final class HotPotJadePlugin implements IWailaPlugin {
    private static final ResourceLocation STOVE_STORAGE = HotPot.id("stove_item_storage");
    // mryt233 改：Jade 11 以两个独立键承载物品组和客户端扩展 ID。
    private static final String ITEM_STORAGE = "JadeItemStorage";
    private static final String STORAGE_UID = "JadeItemStorageUid";

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerItemStorage(new StoveStorage(), BlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerItemStorageClient(new StoveStorageClient());
        registration.registerBlockComponent(new PlateHolderComponentProvider(), PlateHolderBlock.class);
        registration.registerBlockComponent(new StoneMillComponentProvider(), StoneMillBlock.class);
        registration.registerBlockComponent(new WoodenVatComponentProvider(), WoodenVatBlock.class);
        registration.registerBlockComponent(HotPotTableComponentProvider.INSTANCE, HotPotTableBlock.class);
        registration.registerBlockComponent(PrepRackComponentProvider.INSTANCE, PrepRackBlock.class);
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
                    || !(block.getLevel().getBlockEntity(pos) instanceof StoveBlockEntity stove) || stove.isRemoved()) {
                return previewStorage(registration, block);
            }
            boolean potHit = stove.hasPot() && stove.potMaterial() != null
                && StoveBlock.isPotHit(state, pos, block.getHitResult());
            if (!upper && !potHit) {
                return previewStorage(registration, block);
            }
            BlockHitResult redirectedHit = new BlockHitResult(block.getHitResult().getLocation(),
                block.getHitResult().getDirection(), pos, block.getHitResult().isInside());
            // mryt233 改：保留真实世界命中点、灶台状态及纯净锅图标，服务端才能在下格找到实体。
            BlockAccessor.Builder builder = registration.blockAccessor().from(block).hit(redirectedHit).blockState(state)
                .blockEntity(() -> stove).fakeBlock(potHit
                    ? new ItemStack(HotPotContent.itemFor(stove.potMaterial())) : ItemStack.EMPTY);
            // 原始上格 accessor 只持有数据副本；数据中心此时仍校验上一帧的下格目标。
            CompoundTag data = registration.getServerData();
            if (data != null && data.getInt("x") == pos.getX() && data.getInt("y") == pos.getY()
                    && data.getInt("z") == pos.getZ()) {
                builder.serverData(data);
            }
            return previewStorage(registration, builder.requireVerification().build());
        });
    }

    @Nullable
    private static HotPotBlockEntity aimedPot(Accessor<?> accessor) {
        if (!(accessor instanceof BlockAccessor block)) {
            return null;
        }
        if (block.getBlockEntity() instanceof StoveBlockEntity stove && !stove.isRemoved()) {
            BlockState state = stove.getBlockState();
            return stove.getBlockPos().equals(block.getPosition()) && state.getBlock() instanceof StoveBlock
                && stove.hasPot() && (block.isFakeBlock()
                    || StoveBlock.isPotHit(state, stove.getBlockPos(), block.getHitResult())) ? stove : null;
        }
        return block.getBlockEntity() instanceof HotPotBlockEntity pot
            && !pot.isRemoved() && pot.getBlockPos().equals(block.getPosition())
            && pot.getBlockState().getBlock() instanceof HotPotBlock ? pot : null;
    }

    private static BlockAccessor previewStorage(IWailaClientRegistration registration, BlockAccessor block) {
        if (block.isServerConnected()) return block;
        var pot = aimedPot(block);
        var holder = plateHolderTarget(block);
        if (pot == null && holder == null) return block;
        var data = block.getServerData();
        CompoundTag clean = data == null ? new CompoundTag() : data.copy();
        if (STOVE_STORAGE.toString().equals(clean.getString(STORAGE_UID))) {
            clean.remove(STORAGE_UID);
            clean.remove(ITEM_STORAGE);
        }
        if (!clean.contains(ITEM_STORAGE)) {
            var groups = pot != null ? collectPotGroups(pot) : collectPlateGroups(aimedPlate(block));
            if (ViewGroup.saveList(clean, ITEM_STORAGE, groups, HotPotJadePlugin::writeStorageStack)) {
                clean.putString(STORAGE_UID, STOVE_STORAGE.toString());
            }
        }
        return registration.blockAccessor().from(block).serverData(clean).build();
    }

    private static CompoundTag writeStorageStack(ItemStack stack) {
        CompoundTag tag = stack.save(new CompoundTag());
        if (stack.getCount() > 64) {
            tag.putByte("Count", (byte) 1);
            tag.putInt("NewCount", stack.getCount());
        }
        return tag;
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
            if (ItemStack.isSameItemSameTags(existing, stack)) {
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
            && !holder.isRemoved() && holder.getBlockPos().equals(block.getPosition()) ? holder : null;
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
            if (contents.bites() > 0 && !contents.isEmpty()) {
                for (SeasoningDef seasoning : SauceIds.defs(contents.ids())) {
                    if (!seasoning.raws().isEmpty()) {
                        appendMerged(merged, new ItemStack(seasoning.raw()));
                    }
                }
            }
        } else if (plate.holdsFood()) {
            for (ItemStack stack : plate.items) {
                appendMerged(merged, stack);
            }
        }
        if (plate.plate instanceof ChopstickRestBlock) {
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

    private static final class StoveStorage implements IServerExtensionProvider<BlockEntity, ItemStack> {
        @Override public ResourceLocation getUid() { return STOVE_STORAGE; }

        @Override
        public List<ViewGroup<ItemStack>> getGroups(ServerPlayer player, ServerLevel level, BlockEntity target,
                                                    boolean showDetails) {
            if (target.isRemoved() || target.getLevel() != level) return null;
            if (target instanceof StoveBlockEntity stove) return stove.hasPot() ? collectPotGroups(stove) : null;
            if (target instanceof HotPotBlockEntity pot) return collectPotGroups(pot);
            if (target instanceof PlateHolderBlockEntity holder) {
                var groups = collectAllPlateHolderGroups(holder);
                if (!groups.isEmpty() || holder.plates().isEmpty()) return groups;
                // 旧版空组不会建立物品提示；客户端扩展只渲染瞄准盘，丢弃这个标记。
                return List.of(new ViewGroup<>(List.of(new ItemStack(net.minecraft.world.item.Items.STICK))));
            }
            return null;
        }
    }

    private static final class StoveStorageClient implements IClientExtensionProvider<ItemStack, ItemView> {
        @Override
        public ResourceLocation getUid() {
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
            return ClientViewGroup.map(groups == null ? collectPotGroups(pot) : groups, ItemView::new, null);
        }
    }

    private static final class PlateHolderComponentProvider implements IBlockComponentProvider {
        @Override
        public ResourceLocation getUid() {
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
                    Seasoning.appendEffects(contents.ids(), lines);
                    lines.add(Component.translatable(HotPotBlockEntity.key("tooltip.sauce.bites"), contents.bites())
                        .withStyle(ChatFormatting.GRAY));
                }
            } else if (plate.plate instanceof SauceBottleBlock) {
                // mryt233 改（2026-11-09，r146c）：桌上的芝麻酱 / 蚝油瓶现在只是摆设——Jade 只显示名称
                //（Jade 默认取方块名，PlateHolderBlock#getName 返回的就是这瓶的名字）+ 一行「怎么摆、怎么拿」；
                // 效果说明和剩余次数两行随「桌上加料 / 每瓶次数」一起去掉了。
                lines.add(Component.translatable(HotPotBlockEntity.key("tooltip.sauce_bottle.hint"))
                    .withStyle(ChatFormatting.DARK_GRAY));
            } else if (plate.holdsFood()) {
                List<ItemStack> kinds = new ArrayList<>();
                for (ItemStack food : plate.items) {
                    if (!food.isEmpty() && kinds.stream().noneMatch(
                            existing -> ItemStack.isSameItemSameTags(existing, food))) {
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
            } else if (plate.plate instanceof ChopstickRestBlock) {
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

    private static final class StoneMillComponentProvider implements IBlockComponentProvider {
        @Override
        public ResourceLocation getUid() {
            return HotPot.id("stone_mill_info");
        }

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            if (!(accessor.getBlockEntity() instanceof StoneMillBlockEntity mill)) {
                return;
            }
            if (mill.waterUses() > 0) {
                tooltip.add(Component.translatable("text.orients_hotpot_chronicles.jade.stone_mill.water",
                    mill.waterUses()).withStyle(ChatFormatting.AQUA));
            }
            if (!mill.troughResult().isEmpty()) {
                tooltip.add(Component.translatable("text.orients_hotpot_chronicles.jade.stone_mill.ready",
                    mill.troughResult().getHoverName()).withStyle(ChatFormatting.GREEN));
            } else if (!mill.input().isEmpty()) {
                StoneMillRecipe r = cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotRecipes.stoneMill(
                    accessor.getLevel(), mill.input());
                int goal = r != null ? r.effectiveTurns() : StoneMillRecipe.DEFAULT_TURNS;
                tooltip.add(Component.translatable("text.orients_hotpot_chronicles.jade.stone_mill.progress",
                    mill.input().getHoverName(), mill.input().getCount(), mill.turns(), goal)
                    .withStyle(ChatFormatting.GOLD));
            }
        }
    }

    private static final class WoodenVatComponentProvider implements IBlockComponentProvider {
        @Override
        public ResourceLocation getUid() {
            return HotPot.id("wooden_vat_info");
        }

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            if (!(accessor.getBlockEntity() instanceof WoodenVatBlockEntity vat)) {
                return;
            }
            if (!vat.fluid().isEmpty() && vat.fluidUses() > 0) {
                tooltip.add(Component.translatable("text.orients_hotpot_chronicles.jade.wooden_vat.soymilk")
                    .withStyle(ChatFormatting.WHITE));
            }
            if (!vat.output().isEmpty()) {
                tooltip.add(Component.translatable("text.orients_hotpot_chronicles.jade.wooden_vat.ready",
                    vat.output().getHoverName(), vat.output().getCount()).withStyle(ChatFormatting.GREEN));
            } else if (!vat.input().isEmpty()) {
                WoodenVatRecipe r = cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotRecipes.woodenVat(
                    accessor.getLevel(), vat.fluid(), vat.input());
                if (r != null && r.needsLid() && !vat.hasLid()) {
                    tooltip.add(Component.translatable("text.orients_hotpot_chronicles.jade.wooden_vat.need_lid")
                        .withStyle(ChatFormatting.YELLOW));
                } else if (vat.totalTicks() > 0) {
                    int pct = Math.round(vat.progressRatio() * 100F);
                    tooltip.add(Component.translatable("text.orients_hotpot_chronicles.jade.wooden_vat.progress",
                        vat.input().getHoverName(), pct).withStyle(ChatFormatting.GOLD));
                }
            }
        }
    }

    private static Component materialLabel(String id, Component defaultName) {
        if (id == null || id.isEmpty()) {
            return defaultName;
        }
        Block resolved = TableMaterials.resolveBlock(id);
        return resolved != null
            ? resolved.getName()
            : Component.literal(id).withStyle(ChatFormatting.DARK_GRAY);
    }

    private enum HotPotTableComponentProvider implements IBlockComponentProvider {
        INSTANCE;
        private static final ResourceLocation UID = HotPot.id("table");

        @Override
        public ResourceLocation getUid() {
            return UID;
        }

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor block, IPluginConfig config) {
            if (block.getBlockEntity() instanceof HotPotTableBlockEntity table) {
                TableMaterialData data = table.materials();
                if (!data.top().isEmpty()) {
                    tooltip.add(Component.translatable("text.orients_hotpot_chronicles.tooltip.table_top",
                        materialLabel(data.top(), Component.empty())).withStyle(ChatFormatting.GRAY));
                }
                if (!data.base().isEmpty()) {
                    tooltip.add(Component.translatable("text.orients_hotpot_chronicles.tooltip.table_base",
                        materialLabel(data.base(), Component.empty())).withStyle(ChatFormatting.GRAY));
                }
            }
        }
    }

    private enum PrepRackComponentProvider implements IBlockComponentProvider {
        INSTANCE;
        private static final ResourceLocation UID = HotPot.id("prep_rack");

        @Override
        public ResourceLocation getUid() {
            return UID;
        }

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor block, IPluginConfig config) {
            if (block.getBlockEntity() instanceof PrepRackBlockEntity rack && !rack.isRemoved()
                && rack.getBlockPos().equals(block.getPosition())
                && block.getLevel().getBlockEntity(block.getPosition()) == rack
                && rack.getBlockState() == block.getBlockState()
                && block.getLevel().getBlockState(block.getPosition()) == block.getBlockState()
                && block.getBlockState().getBlock() instanceof PrepRackBlock rackBlock) {
                if (!rack.materials().top().isEmpty()) {
                    tooltip.add(Component.translatable("text.orients_hotpot_chronicles.tooltip.rack_material",
                        materialLabel(rack.materials().top(), Component.empty())).withStyle(ChatFormatting.GRAY));
                }
                int aimed = rackBlock.slotAt(block.getBlockState(), block.getPosition(),
                    block.getHitResult().getLocation(), block.getLevel());
                for (int i = 0; i < rack.slotCount(); i++) {
                    ItemStack st = rack.itemAt(i);
                    boolean isAimed = i == aimed;
                    if (!st.isEmpty() || isAimed) {
                        Component itemLabel = st.isEmpty()
                            ? Component.translatable("text.orients_hotpot_chronicles.jade.rack_empty").withStyle(ChatFormatting.DARK_GRAY)
                            : st.getHoverName().copy().withStyle(isAimed ? ChatFormatting.GOLD : ChatFormatting.WHITE);
                        // mryt233 改：未收到服务端规则时只报真实数量，不猜上限。
                        if (!st.isEmpty()) {
                            var label = itemLabel.copy().append(" ×" + st.getCount());
                            if (RackRules.clientReady()) {
                                int cap = RackRules.capacity(block.getLevel(), st, rackBlock.kind());
                                label.append(" / " + cap);
                            }
                            itemLabel = label;
                        }
                        tooltip.add(Component.translatable("text.orients_hotpot_chronicles.jade.rack_slot",
                            i + 1, itemLabel).withStyle(isAimed ? ChatFormatting.YELLOW : ChatFormatting.GRAY));
                    }
                }
            }
        }
    }
}
