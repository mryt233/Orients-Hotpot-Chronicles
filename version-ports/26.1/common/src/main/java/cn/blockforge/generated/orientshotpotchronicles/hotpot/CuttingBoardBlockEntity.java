// mryt233 改：26.1 原版 API。
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

// mryt233 改（2026-11-11）：限定类名改用 import，逻辑不变。
import net.minecraft.core.Direction;

// mryt233 改（2026-11-11）：第三批 3G 存读档改走适配小包，键名与结构不变。
import cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter.BeIo;

import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.BlockPos;

/**
 * 砧板上放着的那一件东西（2026-10-23）。变了就存档并同步给客户端，CuttingBoardRenderer 画在板面上。
 * 作者：mryt233 */
// mryt233 改（2026-11-11）：第三批 3F 合并重复实现，保留各入口原有行为。
public class CuttingBoardBlockEntity extends SyncedBlockEntity {
    private ItemStack item = ItemStack.EMPTY;
    /**
     * 一刀一刀切的东西已经切了几刀。
     * mryt233 改（2026-11-07）：以前「切了几刀」和「捣了几下」共用一个计数器，折耳根这类既能切又能捣的
     * 食材敲一下就被当成「切过了」，之后菜刀永远切不动（玩家看不到任何提示）。现在两个计数器分开存。
     */
    private int cuts;
    /** 擀面杖已经捣了几下（和 cuts 分开记账，见上）。 */
    private int pounds;
    /** 板上食物的朝向（放食物时人站的那一边），和砧板自己的 facing 分开（用户 0.1.2-alpha 后要求）。 */
    private Direction itemFacing = Direction.SOUTH;

    public CuttingBoardBlockEntity(BlockPos pos, BlockState state) {
        super(HotPotContent.CUTTING_BOARD_BE, pos, state);
    }

    public ItemStack item() {
        return item;
    }

    /**
     * 板上的「进度阶段数」= 切了几刀 + 捣了几下。
     * 客户端渲染器（client/CuttingBoardRenderer）按这个值决定画哪一阶段的贴图，所以必须是两者之和，
     * 不能直接返回纯切刀数，否则捣丸子阶段（捶碎 / 肉浆 / 蒜泥 / 折耳根碎末）就画不出来了（mryt233 改 2026-11-07）。
     */
    public int cuts() {
        return cuts + pounds;
    }

    /** 纯切了几刀（不含捣的下数）：一刀一刀切的东西靠它判断切到第几刀了。 */
    public int cutCount() {
        return cuts;
    }

    /** 擀面杖捣了几下。 */
    public int pounds() {
        return pounds;
    }

    /** 数据变了：存档 + 同步给客户端（渲染器按它画板上的东西和阶段）。作者：mryt233 */
    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    /** 切了几刀（纯切刀数）。mryt233 改（2026-11-07）：走统一的 sync()。 */
    public void setCuts(int cuts) {
        this.cuts = cuts;
        sync();
    }

    /** 擀面杖捣了几下。mryt233 改（2026-11-07）：捣的进度单独记，不再借用 cuts。 */
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

    /** 换东西：两个进度计数一起清零。mryt233 改（2026-11-07）：走统一的 sync()。 */
    public void setItem(ItemStack stack) {
        item = stack;
        cuts = 0;
        pounds = 0;
        sync();
    }

    // mryt233 改（2026-11-11）：第三批 3G 存读档改走适配小包，键名、结构与兜底值不变。
    @Override
    protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput nbt) {
        super.saveAdditional(nbt);
        BeIo.write(nbt, w -> {
            w.putItem("Item", item);
            // 板空了也要写点东西：原版客户端收到空的方块实体数据会直接跳过，板上的东西就一直画着
            // （用户报：捣完丸子、切完肉卷，板上的东西都不消失——凡是把板拿空的都这样；修复）
            w.putInt("Cuts", cuts);
            // mryt233 改（2026-11-07）：捣的下数单独存一个键，老存档没有 Pounds 时按 0 读（等价于没捣过）
            w.putInt("Pounds", pounds);
            w.putString("ItemFacing", itemFacing.getSerializedName());
        });
    }

    @Override
    protected void loadAdditional(net.minecraft.world.level.storage.ValueInput nbt) {
        super.loadAdditional(nbt);
        BeIo.read(nbt, r -> {
            item = r.getItem("Item");
            cuts = r.getInt("Cuts", 0);
            pounds = r.getInt("Pounds", 0);   // mryt233 改（2026-11-07）：老存档没有这个键，按 0 读
            Direction f = Direction.byName(r.getString("ItemFacing", ""));
            itemFacing = f != null && f.getAxis().isHorizontal() ? f : Direction.SOUTH;
        });
    }


    /** 保留公开入口，实际更新格式由公共基类提供。 */
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() { return super.getUpdatePacket(); }

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
