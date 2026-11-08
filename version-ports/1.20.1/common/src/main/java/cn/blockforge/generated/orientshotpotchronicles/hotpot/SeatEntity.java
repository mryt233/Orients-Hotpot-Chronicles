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

    /**
     * mryt233 改（2026-11-05，清单 #120）：上面又被叠了一张条凳时自动起身。
     * 以前只判「坐着的那两格还是不是条凳」，凳塔叠高以后人会坐在凳塔中间层里（头卡在上一层的凳面下）。
     * 现在额外判「这一格头顶是不是也成了条凳」，是就丢弃座位，让玩家落回地面。
     */
    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && (!isVehicle()
            || !(level().getBlockState(a).getBlock() instanceof BenchBlock)
            || !(level().getBlockState(b).getBlock() instanceof BenchBlock)
            || BenchBlock.isTallBench(level().getBlockState(a.above()))
            || BenchBlock.isTallBench(level().getBlockState(b.above())))) {
            discard();
        }
    }

    /**
     * 这个座位是不是落在 pos 这一格上（含两格交界时的一侧）。
     * mryt233 改（2026-11-05，清单 #120）：{@link BenchBlock#clearSeatsAt} 拆凳时用，
     * 以前只能靠座位实体自己下一 tick 发现凳子没了。
     */
    public boolean sitsOn(BlockPos pos) {
        return a.equals(pos) || b.equals(pos);
    }

    /** 坐的人屁股正好落在凳面上（实体自身就放在凳面高度）。 */
    @Override
    public double getPassengersRidingOffset() {
        return -0.25;
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
    protected void defineSynchedData() {
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
