// 作者：mryt233
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

    /** 往准星选中格放入 1 件物品；目标格已有物时自动寻找最近空槽，放满则返回 false。 */
    public boolean tryInsert(Player player, InteractionHand hand, ItemStack held, int preferredSlot) {
        int count = slotCount();
        int target = findEmptySlot(preferredSlot, count);
        if (target < 0 || held.isEmpty()) {
            return false;
        }
        if (level != null && !level.isClientSide()) {
            items.set(target, held.copyWithCount(1));
            if (!player.hasInfiniteMaterials()) {
                held.shrink(1);
            }
            level.playSound(null, worldPosition, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 0.7F, 1.05F);
            sync();
        }
        return true;
    }

    /** 空手右键优先从准星格取回物品；准星格为空则取最近非空槽。 */
    public boolean tryTake(Player player, int preferredSlot) {
        int count = slotCount();
        int target = findOccupiedSlot(preferredSlot, count);
        if (target < 0) {
            return false;
        }
        if (level != null && !level.isClientSide()) {
            ItemStack taken = items.get(target).copy();
            items.set(target, ItemStack.EMPTY);
            if (!player.getInventory().add(taken)) {
                player.drop(taken, false, net.minecraft.util.Prediction.SERVER_ONLY); // mryt233 改：26.3 服务端掉落
            }
            level.playSound(null, worldPosition, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 0.7F, 1.05F);
            sync();
        }
        return true;
    }

    private int findEmptySlot(int preferred, int count) {
        int p = Math.max(0, Math.min(count - 1, preferred));
        if (items.get(p).isEmpty()) {
            return p;
        }
        for (int i = 0; i < count; i++) {
            if (items.get(i).isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    private int findOccupiedSlot(int preferred, int count) {
        int p = Math.max(0, Math.min(count - 1, preferred));
        if (!items.get(p).isEmpty()) {
            return p;
        }
        for (int i = count - 1; i >= 0; i--) {
            if (!items.get(i).isEmpty()) {
                return i;
            }
        }
        return -1;
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
                            player.drop(refund, false, net.minecraft.util.Prediction.SERVER_ONLY); // mryt233 改：26.3 服务端掉落
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
                        player.drop(refund, false, net.minecraft.util.Prediction.SERVER_ONLY); // mryt233 改：26.3 服务端掉落
                    }
                }
            }
            materials = TableMaterialData.EMPTY;
            sync();
            level.playSound(null, worldPosition, SoundEvents.WOOD_BREAK, SoundSource.BLOCKS, 0.75F, 1.05F);
        }
        return true;
    }

    public void dropStoredItems() {
        if (level == null || level.isClientSide()) {
            return;
        }
        for (int i = 0; i < items.size(); i++) {
            ItemStack st = items.get(i);
            if (!st.isEmpty()) {
                Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.4,
                    worldPosition.getZ() + 0.5, st.copy());
                items.set(i, ItemStack.EMPTY);
            }
        }
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        dropStoredItems();
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
    public void removeComponentsFromTag(ValueOutput output) {
        super.removeComponentsFromTag(output);
        output.discard("MatTop");
    }
}
