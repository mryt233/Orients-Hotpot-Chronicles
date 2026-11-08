/** 作者：mryt233。Jade 编译期签名桩（1.20.1 移植沿用同一套最小签名），成品与源码包均排除。 */
package snownee.jade.api;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
public interface BlockAccessor extends Accessor<BlockHitResult> {
    BlockState getBlockState();
    BlockEntity getBlockEntity();
    BlockPos getPosition();
    boolean isFakeBlock();
    interface Builder {
        Builder from(BlockAccessor accessor);
        Builder serverData(CompoundTag serverData);
        Builder serverConnected(boolean serverConnected);
        Builder hit(BlockHitResult hit);
        Builder blockState(BlockState state);
        Builder blockEntity(Supplier<BlockEntity> blockEntity);
        Builder fakeBlock(ItemStack stack);
        Builder requireVerification();
        BlockAccessor build();
    }
}
