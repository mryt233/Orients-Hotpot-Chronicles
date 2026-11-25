// 作者：mryt233；mryt233 改：同步备菜架存取与安全掉落，保留 26.3 接口。
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter.BeIo;
import cn.blockforge.orient.framework.game.MaterialRenderHost;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

/**
 * 食材架 / 备菜架通用方块实体：管理至多 6 格摆盘 / 食材 / 悬挂厨具槽位及统一换材质状态。
 * 作者：mryt233
 */
public class PrepRackBlockEntity extends SyncedBlockEntity implements MaterialRenderHost {
    public static final int MAX_SLOTS = 6;
    public static BiConsumer<Level, BlockPos> clientRefresh = (level, pos) -> {};

    private final NonNullList<ItemStack> items = NonNullList.withSize(MAX_SLOTS, ItemStack.EMPTY);
    private TableMaterialData materials = TableMaterialData.EMPTY;

    public PrepRackBlockEntity(BlockPos pos, BlockState state) {
        super(HotPotContent.PREP_RACK_BE, pos, state);
    }

    public int slotCount() {
        return getBlockState().getBlock() instanceof PrepRackBlock rack ? rack.kind().slots() : MAX_SLOTS;
    }

    public ItemStack itemAt(int index) {
        return index >= 0 && index < items.size() ? items.get(index) : ItemStack.EMPTY;
    }

    public TableMaterialData materials() {
        return materials;
    }

    public BlockState frameBlockState() {
        return TableMaterials.resolveDisplayState(materials.top(), Blocks.OAK_PLANKS.defaultBlockState());
    }

    @Override
    public @Nullable Object materialRenderData() {
        return materials;
    }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_ALL);
        }
    }

    private PrepRackBlock.Kind kind() {
        return getBlockState().getBlock() instanceof PrepRackBlock rack ? rack.kind() : PrepRackBlock.Kind.MULTI_TIER;
    }

    private PrepRackGeometry.Cell cell(int slot) { return PrepRackGeometry.cell(kind(),getBlockState().getValue(PrepRackBlock.MOUNT),slot); }

    private Vec3 center(int slot) {
        var cell=cell(Math.max(0,Math.min(slotCount()-1,slot)));
        return new Vec3(cell.x(),cell.y(),cell.z());
    }

    public boolean tryInsert(Player player, InteractionHand hand, ItemStack held, int preferredSlot) {
        return tryInsert(player,hand,held,preferredSlot,center(preferredSlot));
    }

    public boolean tryInsert(Player player, InteractionHand hand, ItemStack held, int preferredSlot, Vec3 hit) {
        if (held.isEmpty() || isRemoved() || level == null || level.isClientSide()) return false;
        int capacity = RackRules.capacity(level,held,kind());
        int target = insertionSlot(items,slotCount(),held,capacity,preferredSlot,i -> cell(i).distance(hit));
        if (target < 0) return false;
        ItemStack old = items.get(target);
        items.set(target,old.isEmpty() ? held.copyWithCount(1) : old.copyWithCount(old.getCount()+1));
        if (!player.hasInfiniteMaterials()) held.shrink(1);
        level.playSound(null,worldPosition,SoundEvents.ITEM_FRAME_ADD_ITEM,SoundSource.BLOCKS,.7F,1.05F);
        sync();
        return true;
    }

    /** mryt233 改：准星格优先，其次最近的可合并格，再其次最近空格。 */
    static int insertionSlot(java.util.List<ItemStack> slots, int count, ItemStack held, int capacity, int preferred,
                             java.util.function.IntToDoubleFunction distance) {
        if (held.isEmpty() || capacity<1) return -1;
        int s = Math.max(0,Math.min(count-1,preferred));
        if (slots.get(s).isEmpty() || canMerge(slots.get(s),held,capacity)) return s;
        int nearest = nearest(count, distance, i -> canMerge(slots.get(i),held,capacity));
        return nearest >= 0 ? nearest : nearest(count,distance,i -> slots.get(i).isEmpty());
    }

    private static boolean canMerge(ItemStack old, ItemStack held, int capacity) {
        return !old.isEmpty() && old.getCount()<capacity && ItemStack.isSameItemSameComponents(old,held);
    }

    private static int nearest(int count, java.util.function.IntToDoubleFunction distance, java.util.function.IntPredicate test) {
        double best=Double.POSITIVE_INFINITY;
        int slot=-1;
        for(int i=0;i<count;i++) if (test.test(i)) {
            double next=distance.applyAsDouble(i);
            if (next<best) { best=next; slot=i; }
        }
        return slot;
    }

    boolean hasStoredItems() {
        for (int i = 0, count = slotCount(); i < count; i++) if (!items.get(i).isEmpty()) return true;
        return false;
    }

    public boolean tryTake(Player player,int preferredSlot) { return tryTake(player,preferredSlot,center(preferredSlot)); }

    public boolean tryTake(Player player,int preferredSlot,Vec3 hit) {
        if (isRemoved() || level == null || level.isClientSide()) return false;
        int p=Math.max(0,Math.min(slotCount()-1,preferredSlot));
        int slot=items.get(p).isEmpty() ? nearest(slotCount(), i -> cell(i).distance(hit), i -> !items.get(i).isEmpty()) : p;
        if (slot<0) return false;
        ItemStack old=items.get(slot);
        ItemStack taken=old.copyWithCount(1);
        items.set(slot,old.getCount()==1 ? ItemStack.EMPTY : old.copyWithCount(old.getCount()-1));
        if (!player.getInventory().add(taken) && !taken.isEmpty()) player.drop(taken, false, net.minecraft.util.Prediction.SERVER_ONLY);
        level.playSound(null,worldPosition,SoundEvents.ITEM_FRAME_REMOVE_ITEM,SoundSource.BLOCKS,.7F,1.05F);
        sync();
        return true;
    }

    public boolean applyMaterial(Player player, ItemStack stack, Block targetBlock) {
        String newId = TableMaterials.encodeBlock(targetBlock);
        if (newId.isEmpty()) {
            return false;
        }
        String currentRaw = materials.top();
        String effectiveCurrent = currentRaw.isEmpty() ? TableMaterials.DEFAULT_TOP_ID : currentRaw;
        if (effectiveCurrent.equals(newId)) {
            if (level != null && !level.isClientSide()) {
                player.sendOverlayMessage(Component.translatableWithFallback(
                    HotPotBlockEntity.key("msg.table_same_material"), "这一部位已经是这种材质了"));
            }
            return true;
        }
        if (level != null && !level.isClientSide()) {
            boolean creative = player.hasInfiniteMaterials();
            if (!creative) {
                stack.shrink(1);
                if (!currentRaw.isEmpty()) {
                    Block oldBlock = TableMaterials.resolveBlock(currentRaw);
                    if (oldBlock != null && oldBlock.asItem() != Items.AIR) {
                        ItemStack refund = new ItemStack(oldBlock.asItem());
                        if (!player.getInventory().add(refund)) {
                            player.drop(refund, false, net.minecraft.util.Prediction.SERVER_ONLY);
                        }
                    }
                }
            }
            materials = new TableMaterialData(newId, "", "", false, false);
            sync();
            level.playSound(null, worldPosition, targetBlock.defaultBlockState().getSoundType().getPlaceSound(),
                SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        return true;
    }

    public boolean tryPopMaterial(Player player) {
        if (materials.top().isEmpty()) {
            return false;
        }
        if (level != null && !level.isClientSide()) {
            if (!player.hasInfiniteMaterials()) {
                Block oldBlock = TableMaterials.resolveBlock(materials.top());
                if (oldBlock != null && oldBlock.asItem() != Items.AIR) {
                    ItemStack refund = new ItemStack(oldBlock.asItem());
                    if (!player.getInventory().add(refund)) {
                        player.drop(refund, false, net.minecraft.util.Prediction.SERVER_ONLY);
                    }
                }
            }
            materials = TableMaterialData.EMPTY;
            sync();
            level.playSound(null, worldPosition, SoundEvents.WOOD_BREAK, SoundSource.BLOCKS, 0.75F, 1.05F);
        }
        return true;
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        // mryt233 改：26.3 由本回调转移并掉落库存。
        dropStoredItems();
    }

    public void dropStoredItems() {
        if (level == null || level.isClientSide()) {
            return;
        }
        // mryt233 改：外部掉落实体回调可同步重入；先转移每格所有权，再派发掉落。
        Level dropLevel = level;
        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.set(i, ItemStack.EMPTY);
            if (stack.isEmpty()) continue;
            int limit = Math.max(1, Math.min(64, stack.getMaxStackSize()));
            int left = stack.getCount();
            while (left > 0) {
                int count = Math.min(left, limit);
                left -= count;
                Containers.dropItemStack(dropLevel, worldPosition.getX() + .5, worldPosition.getY() + .4,
                    worldPosition.getZ() + .5, stack.copyWithCount(count));
            }
        }
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        BeIo.write(out, writer -> {
            writer.putString("MatTop", materials.top());
            for (int i = 0; i < MAX_SLOTS; i++) {
                writer.putItem("Slot" + i, items.get(i));
            }
        });
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        BeIo.read(in, reader -> {
            materials = new TableMaterialData(reader.getString("MatTop", ""), "", "", false, false);
            for (int i = 0; i < MAX_SLOTS; i++) {
                items.set(i, reader.getItem("Slot" + i));
            }
        });
        if (level != null && level.isClientSide()) {
            clientRefresh.accept(level, worldPosition);
        }
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        if (materials.hasAny()) {
            builder.set(HotPotContent.TABLE_MATERIALS, materials);
        }
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        TableMaterialData data = components.get(HotPotContent.TABLE_MATERIALS);
        if (data != null) {
            this.materials = new TableMaterialData(data.top(), "", "", false, false);
        }
    }

    @Override
    public void removeComponentsFromTag(ValueOutput out) {
        super.removeComponentsFromTag(out);
        out.discard("MatTop");
    }
}
