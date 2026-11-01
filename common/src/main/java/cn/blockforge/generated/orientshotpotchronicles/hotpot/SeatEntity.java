package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;

/**
 * 看不见的座位：玩家坐条凳时骑在它上面。坐在一格中间时 a == b；坐在两格中间时 a、b 是这两格，
 * 两格都算被占。没人坐了或凳子没了就自己消失，不存档。
 */
public class SeatEntity extends Entity {
    private BlockPos a = BlockPos.ZERO;
    private BlockPos b = BlockPos.ZERO;
    private Direction facing = Direction.NORTH;
    /** 凳面离地多高（方块单位）：起身时站回地面用。 */
    private double seatY = BenchBlock.SEAT_Y;

    public SeatEntity(EntityType<? extends SeatEntity> type, Level world) {
        super(type, world);
        noPhysics = true;
    }

    public static SeatEntity create(Level world, Vec3 at, BlockPos a, BlockPos b, Direction facing, double seatY) {
        SeatEntity seat = new SeatEntity(HotPotContent.SEAT, world);
        seat.a = a.immutable();
        seat.b = b.immutable();
        seat.facing = facing;
        seat.seatY = seatY;
        seat.moveTo(at.x, at.y, at.z, facing.toYRot(), 0F);
        return seat;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && (!isVehicle()
            || !(level().getBlockState(a).getBlock() instanceof BenchBlock)
            || !(level().getBlockState(b).getBlock() instanceof BenchBlock))) {
            discard();
        }
    }

    /** 坐的人屁股正好落在凳面上（实体自身就放在凳面高度）。 */
    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scaleFactor) {
        return Vec3.ZERO;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().isEmpty();
    }

    /** 起身：站到凳子后面（背对桌子那一侧）；后面站不下就站到凳面上。 */
    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        Vec3 behind = position().add(-facing.getStepX() * 0.9, -seatY, -facing.getStepZ() * 0.9);
        AABB box = passenger.getDimensions(passenger.getPose()).makeBoundingBox(behind);
        if (level().noCollision(passenger, box)) {
            return behind;
        }
        return position();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag nbt) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag nbt) {
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }
}
