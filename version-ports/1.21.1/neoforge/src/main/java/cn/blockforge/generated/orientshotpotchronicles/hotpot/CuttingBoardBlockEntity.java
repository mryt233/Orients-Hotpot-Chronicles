package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;

/**
 * 砧板上放着的那一件东西（2026-10-23）。变了就存档并同步给客户端，CuttingBoardRenderer 画在板面上。
 * 作者：Claude
 */
public class CuttingBoardBlockEntity extends BlockEntity {
    private ItemStack item = ItemStack.EMPTY;
    /** 一刀一刀切的东西已经切了几刀 / 捣丸子已经捣了几下（换东西时清零）。 */
    private int cuts;

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

    public void setItem(ItemStack stack) {
        item = stack;
        cuts = 0;
        markDirty();
        if (world != null && !world.isClient) {
            world.updateListeners(pos, getCachedState(), getCachedState(), Block.NOTIFY_LISTENERS);
        }
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.writeNbt(nbt, registries);
        if (!item.isEmpty()) {
            nbt.put("Item", item.encode(registries));
            nbt.putInt("Cuts", cuts);
        }
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.readNbt(nbt, registries);
        item = nbt.contains("Item") ? ItemStack.fromNbt(registries, nbt.getCompound("Item")).orElse(ItemStack.EMPTY)
            : ItemStack.EMPTY;
        cuts = nbt.getInt("Cuts");
    }

    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registries) {
        return createNbt(registries);
    }
}
