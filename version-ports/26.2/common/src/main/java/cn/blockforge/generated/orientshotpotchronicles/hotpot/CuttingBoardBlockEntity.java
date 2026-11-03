// mryt233 改：26.1 原版 API。
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.BlockPos;

/**
 * 砧板上放着的那一件东西（2026-10-23）。变了就存档并同步给客户端，CuttingBoardRenderer 画在板面上。
 * 作者：mryt233 */
public class CuttingBoardBlockEntity extends BlockEntity {
    private ItemStack item = ItemStack.EMPTY;
    /** 一刀一刀切的东西已经切了几刀 / 捣丸子已经捣了几下（换东西时清零）。 */
    private int cuts;
    /** 板上食物的朝向（放食物时人站的那一边），和砧板自己的 facing 分开（用户 0.1.2-alpha 后要求）。 */
    private net.minecraft.core.Direction itemFacing = net.minecraft.core.Direction.SOUTH;

    public CuttingBoardBlockEntity(BlockPos pos, BlockState state) {
        super(HotPotContent.CUTTING_BOARD_BE, pos, state);
    }

    public ItemStack item() {
        return item;
    }

    public int cuts() {
        return cuts;
    }

    public void setCuts(int cuts) {
        this.cuts = cuts;
        setChanged();
        // 捣丸子的阶段要画出来（捶碎 / 肉浆），同步给客户端
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    public net.minecraft.core.Direction itemFacing() {
        return itemFacing;
    }

    public void setItem(ItemStack stack, net.minecraft.core.Direction facing) {
        itemFacing = facing;
        setItem(stack);
    }

    public void setItem(ItemStack stack) {
        item = stack;
        cuts = 0;
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput nbt) {
        super.saveAdditional(nbt);
        if (!item.isEmpty()) {
            nbt.store("Item", ItemStack.CODEC, item);
        }
        // 板空了也要写点东西：原版客户端收到空的方块实体数据会直接跳过，板上的东西就一直画着
        // （用户报：捣完丸子、切完肉卷，板上的东西都不消失——凡是把板拿空的都这样；修复）
        nbt.putInt("Cuts", cuts);
        nbt.putString("ItemFacing", itemFacing.getSerializedName());
    }

    @Override
    protected void loadAdditional(net.minecraft.world.level.storage.ValueInput nbt) {
        super.loadAdditional(nbt);
        item = nbt.read("Item", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        cuts = nbt.getIntOr("Cuts", 0);
        net.minecraft.core.Direction f = net.minecraft.core.Direction.byName(nbt.getStringOr("ItemFacing", "south"));
        itemFacing = f != null && f.getAxis().isHorizontal() ? f : net.minecraft.core.Direction.SOUTH;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return StorageSupport.snapshot(this, registries);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level != null) {
            net.minecraft.world.Containers.dropItemStack(level, pos.getX() + 0.5,
                pos.getY() + CuttingBoardBlock.TOP, pos.getZ() + 0.5, item);
        }
    }
}