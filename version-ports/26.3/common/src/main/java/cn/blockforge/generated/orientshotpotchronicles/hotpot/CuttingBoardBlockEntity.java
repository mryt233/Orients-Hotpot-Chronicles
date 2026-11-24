// mryt233 改：26.2 原版 API；第三批 3F/3G 合并与存读档适配。
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter.BeIo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * 砧板上放着的那一件东西。变了就存档并同步给客户端，CuttingBoardRenderer 画在板面上。
 * 作者：mryt233
 */
public class CuttingBoardBlockEntity extends SyncedBlockEntity {
    private ItemStack item = ItemStack.EMPTY;
    private int cuts;
    private int pounds;
    private Direction itemFacing = Direction.SOUTH;

    public CuttingBoardBlockEntity(BlockPos pos, BlockState state) {
        super(HotPotContent.CUTTING_BOARD_BE, pos, state);
    }

    public ItemStack item() {
        return item;
    }

    /** 板上的进度阶段数 = 切了几刀 + 捣了几下，供渲染器选择阶段贴图。 */
    public int cuts() {
        return cuts + pounds;
    }

    /** 纯切了几刀（不含捣的下数）。 */
    public int cutCount() {
        return cuts;
    }

    /** 擀面杖捣了几下。 */
    public int pounds() {
        return pounds;
    }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    public void setCuts(int cuts) {
        this.cuts = cuts;
        sync();
    }

    public void setPounds(int pounds) {
        this.pounds = pounds;
        sync();
    }

    public Direction itemFacing() {
        return itemFacing;
    }

    public void setItem(ItemStack stack, Direction facing) {
        itemFacing = facing;
        setItem(stack);
    }

    public void setItem(ItemStack stack) {
        item = stack;
        cuts = 0;
        pounds = 0;
        sync();
    }

    @Override
    protected void saveAdditional(ValueOutput nbt) {
        super.saveAdditional(nbt);
        BeIo.write(nbt, w -> {
            w.putItem("Item", item);
            w.putInt("Cuts", cuts);
            w.putInt("Pounds", pounds);
            w.putString("ItemFacing", itemFacing.getSerializedName());
        });
    }

    @Override
    protected void loadAdditional(ValueInput nbt) {
        super.loadAdditional(nbt);
        BeIo.read(nbt, r -> {
            item = r.getItem("Item");
            cuts = r.getInt("Cuts", 0);
            pounds = r.getInt("Pounds", 0);
            Direction f = Direction.byName(r.getString("ItemFacing", ""));
            itemFacing = f != null && f.getAxis().isHorizontal() ? f : Direction.SOUTH;
        });
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return super.getUpdatePacket();
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return super.getUpdateTag(registries);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level != null) {
            Containers.dropItemStack(level, pos.getX() + 0.5,
                pos.getY() + CuttingBoardBlock.TOP, pos.getZ() + 0.5, item);
        }
    }
}
