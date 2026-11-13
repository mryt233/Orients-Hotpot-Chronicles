package cn.blockforge.generated.orientshotpotchronicles.hotpot;

// mryt233 改（2026-11-11）：限定类名改用 import，逻辑不变。
// mryt233 改（2026-11-11）：第三批 3G 存读档改走适配小包，键名与结构不变。
import cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter.BeIo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 砧板上放着的那一件东西（2026-10-23）。变了就存档并同步给客户端，CuttingBoardRenderer 画在板面上。
 * 作者：mryt233
 */
// mryt233 改（2026-11-11）：第三批 3F 合并重复实现，保留各入口原有行为。
public class CuttingBoardBlockEntity extends SyncedBlockEntity {
    private ItemStack item = ItemStack.EMPTY;
    /** 一刀一刀切的东西已经切了几刀（换东西时清零）。 */
    private int cuts;
    /**
     * 擀面杖捣了几下（换东西时清零）。
     * mryt233 改（2026-11-07）：与 {@link #cuts} 分开记账——折耳根这类既能切又能捣的食材
     * 敲一两下之后照样能用菜刀切，不再把捣的次数当成已经切过。
     */
    private int pounds;
    /** 板上食物的朝向（放食物时人站的那一边），和砧板自己的 facing 分开（用户 0.1.2-alpha 后要求）。 */
    private Direction itemFacing = Direction.SOUTH;

    public CuttingBoardBlockEntity(BlockPos pos, BlockState state) {
        super(HotPotContent.CUTTING_BOARD_BE, pos, state);
    }

    public ItemStack item() {
        return item;
    }

    public int cuts() {
        return cuts;
    }

    public int pounds() {
        return pounds;
    }

    /** mryt233 改（2026-11-07）：切与捣的进度更新都走统一的同步 helper。 */
    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide) {
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

    public void setItem(ItemStack stack) {
        item = stack;
        cuts = 0;
        pounds = 0;   // mryt233 改（2026-11-07）：换东西或清空时把捣的次数也清零
        sync();
    }

    // mryt233 改（2026-11-11）：第三批 3G 存读档改走适配小包，键名、结构与兜底值不变。
    @Override
    protected void saveAdditional(CompoundTag nbt) {
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
    public void load(CompoundTag nbt) {
        super.load(nbt);
        BeIo.read(nbt, r -> {
            item = r.getItem("Item");
            cuts = r.getInt("Cuts", 0);
            pounds = r.getInt("Pounds", 0);   // mryt233 改（2026-11-07）：老存档没有这个键，按 0 读
            Direction f = Direction.byName(r.getString("ItemFacing", ""));
            itemFacing = f != null && f.getAxis().isHorizontal() ? f : Direction.SOUTH;
        });
    }
}
