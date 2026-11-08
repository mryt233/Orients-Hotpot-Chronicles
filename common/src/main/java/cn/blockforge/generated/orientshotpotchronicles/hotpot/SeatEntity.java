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
import net.minecraft.world.level.block.state.BlockState;

/**
 * 看不见的座位：玩家坐条凳时骑在它上面。坐在一格中间时 a == b；坐在两格中间时 a、b 是这两格，
 * 两格都算被占。没人坐了或凳子没了就自己消失，不存档。
 */
public class SeatEntity extends Entity {
    private BlockPos a = BlockPos.ZERO;
    private BlockPos b = BlockPos.ZERO;
    /** 座位朝向。mryt233 改（2026-11-07）：升为 protected，子类直接复用这一份，不要再存第二份
     *  （以前子类 HighStoolBlock.Seat 自己存了一份，父类这份恒为 NORTH，是个埋着的坑）。 */
    protected Direction facing = Direction.NORTH;
    /** 凳面离地多高（方块单位）：起身时站回地面用。mryt233 改（2026-11-07）：升为 protected，供子类赋值。 */
    protected double seatY = BenchBlock.SEAT_Y;

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

    /** 这个座位是不是落在这两格上（坐一格时 a、b 相同）。供方块被移除时按格清理。mryt233 改（2026-11-07）新增。 */
    public boolean sitsOn(BlockPos pos) {
        return pos.equals(a) || pos.equals(b);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;                                      // 座位只在服务端判失效
        }
        // mryt233 改（2026-11-07）：坐一格时 a == b，以前 a.above() / b.above() 各查一次，白查一遍还多分配 BlockPos
        BlockState onA = level().getBlockState(a);
        BlockState onB = a.equals(b) ? onA : level().getBlockState(b);
        if (!isVehicle()
            || !(onA.getBlock() instanceof BenchBlock)
            || !(onB.getBlock() instanceof BenchBlock)
            || BenchBlock.isTallBench(level().getBlockState(a.above()))
            || (!a.equals(b) && BenchBlock.isTallBench(level().getBlockState(b.above())))) {
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

    /**
     * 起身落点：站到座位后面（背对凳子朝向的那一侧）；后面站不下就站回座位上。
     * mryt233 改（2026-11-07）：条凳和高凳以前各写一遍一模一样的数学，现在只在父类留一份，子类直接复用。
     * 作者：mryt233
     */
    protected Vec3 dismountBehind(LivingEntity passenger) {
        Vec3 behind = position().add(-facing.getStepX() * 0.9, -seatY, -facing.getStepZ() * 0.9);
        AABB box = passenger.getDimensions(passenger.getPose()).makeBoundingBox(behind);
        return level().noCollision(passenger, box) ? behind : position();
    }

    /** 起身：站到凳子后面（背对桌子那一侧）；后面站不下就站到凳面上。 */
    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        return dismountBehind(passenger);
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
