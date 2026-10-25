package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.util.math.BlockPos;

/**
 * 砧板上放着的那一件东西（2026-10-23）。变了就存档并同步给客户端，CuttingBoardRenderer 画在板面上。
 * 作者：
 */
public class CuttingBoardBlockEntity extends BlockEntity {
    private ItemStack item = ItemStack.EMPTY;
    /** 一刀一刀切的东西已经切了几刀 / 捣丸子已经捣了几下（换东西时清零）。 */
    private int cuts;
    /** 板上食物的朝向（放食物时人站的那一边），和砧板自己的 facing 分开（用户 0.1.2-alpha 后要求）。 */
    private net.minecraft.util.math.Direction itemFacing = net.minecraft.util.math.Direction.SOUTH;

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
        markDirty();
        // 捣丸子的阶段要画出来（捶碎 / 肉浆），同步给客户端
        if (world != null && !world.isClient) {
            world.updateListeners(pos, getCachedState(), getCachedState(), Block.NOTIFY_LISTENERS);
        }
    }

    public net.minecraft.util.math.Direction itemFacing() {
        return itemFacing;
    }

    public void setItem(ItemStack stack, net.minecraft.util.math.Direction facing) {
        itemFacing = facing;
        setItem(stack);
    }

    public void setItem(ItemStack stack) {
        item = stack;
        cuts = 0;
        markDirty();
        if (world != null && !world.isClient) {
            world.updateListeners(pos, getCachedState(), getCachedState(), Block.NOTIFY_LISTENERS);
        }
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        if (!item.isEmpty()) {
            nbt.put("Item", item.writeNbt(new NbtCompound()));
        }
        // 板空了也要写点东西：原版客户端收到空的方块实体数据会直接跳过，板上的东西就一直画着
        // （用户报：捣完丸子、切完肉卷，板上的东西都不消失——凡是把板拿空的都这样； 修）
        nbt.putInt("Cuts", cuts);
        nbt.putString("ItemFacing", itemFacing.asString());
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        item = nbt.contains("Item") ? ItemStack.fromNbt(nbt.getCompound("Item"))
            : ItemStack.EMPTY;
        cuts = nbt.getInt("Cuts");
        net.minecraft.util.math.Direction f = net.minecraft.util.math.Direction.byName(nbt.getString("ItemFacing"));
        itemFacing = f != null && f.getAxis().isHorizontal() ? f : net.minecraft.util.math.Direction.SOUTH;
    }

    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        return createNbt();
    }
}
