package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * 看不见的座位：玩家坐条凳时骑在它上面。坐在一格中间时 a == b；坐在两格中间时 a、b 是这两格，
 * 两格都算被占。没人坐了或凳子没了就自己消失，不存档。
 */
public class SeatEntity extends Entity {
    private BlockPos a = BlockPos.ORIGIN;
    private BlockPos b = BlockPos.ORIGIN;
    private Direction facing = Direction.NORTH;
    /** 凳面离地多高（方块单位）：起身时站回地面用。 */
    private double seatY = BenchBlock.SEAT_Y;

    public SeatEntity(EntityType<? extends SeatEntity> type, World world) {
        super(type, world);
        noClip = true;
    }

    public static SeatEntity create(World world, Vec3d at, BlockPos a, BlockPos b, Direction facing, double seatY) {
        SeatEntity seat = new SeatEntity(HotPotContent.SEAT, world);
        seat.a = a.toImmutable();
        seat.b = b.toImmutable();
        seat.facing = facing;
        seat.seatY = seatY;
        seat.refreshPositionAndAngles(at.x, at.y, at.z, facing.asRotation(), 0F);
        return seat;
    }

    @Override
    public void tick() {
        super.tick();
        if (!getWorld().isClient && (!hasPassengers()
            || !(getWorld().getBlockState(a).getBlock() instanceof BenchBlock)
            || !(getWorld().getBlockState(b).getBlock() instanceof BenchBlock))) {
            discard();
        }
    }

    /** 坐的人屁股正好落在凳面上（实体自身就放在凳面高度）。 */
    @Override
    protected Vec3d getPassengerAttachmentPos(Entity passenger, EntityDimensions dimensions, float scaleFactor) {
        return Vec3d.ZERO;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengerList().isEmpty();
    }

    /** 起身：站到凳子后面（背对桌子那一侧）；后面站不下就站到凳面上。 */
    @Override
    public Vec3d updatePassengerForDismount(LivingEntity passenger) {
        Vec3d behind = getPos().add(-facing.getOffsetX() * 0.9, -seatY, -facing.getOffsetZ() * 0.9);
        Box box = passenger.getDimensions(passenger.getPose()).getBoxAt(behind);
        if (getWorld().isSpaceEmpty(passenger, box)) {
            return behind;
        }
        return getPos();
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
    }

    @Override
    public boolean shouldSave() {
        return false;
    }
}
