// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.client;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.*;
import cn.blockforge.generated.orientshotpotchronicles.network.PotBreakEffectPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/** 自家复合方块的粒子位置/材质；只在客户端读世界，不参与光照形状计算。mryt233 改（26.2 适配）。 */
public final class BlockPartParticles {
    private record Part(BlockPos origin, VoxelShape shape, BlockState material) { }

    private BlockPartParticles() { }

    public static void potBroken(PotBreakEffectPayload payload) {
        Minecraft client = Minecraft.getInstance();
        ClientLevel level = client.level;
        BlockState stove = Block.stateById(payload.stoveStateId());
        if (level == null || !StoveBlock.isStove(stove) || !stove.getValue(StoveBlock.POT).hasPot()
            || !level.hasChunkAt(payload.pos())) return;
        HotPotMaterial material = stove.getValue(StoveBlock.POT).material();
        BlockState pot = HotPotContent.blockFor(material).defaultBlockState()
            .setValue(HotPotBlock.AXIS, stove.getValue(StoveBlock.POT_AXIS));
        Vec3 origin = Vec3.atLowerCornerOf(payload.pos());
        for (BlockBreakSamples.Sample sample : BlockBreakSamples.destroy(StoveBlock.potShape(stove))) {
            Vec3 p = sample.position().add(origin);
            Vec3 velocity = sample.velocity();
            client.particleEngine.add(new TerrainParticle(level, p.x, p.y, p.z,
                velocity.x, velocity.y, velocity.z, pot, payload.pos()));
        }
        Vec3 sound = StoveBlock.potDropPoint(payload.pos(), stove);
        var type = pot.getSoundType();
        level.playLocalSound(sound.x, sound.y, sound.z, type.getBreakSound(), SoundSource.BLOCKS,
            (type.getVolume() + 1) / 2, type.getPitch() * .8F, false);
    }

    /** 返回 true 才取消原版；不匹配、目标失效或其他模组方块一律放行。 */
    public static boolean hit(ParticleEngine engine, BlockPos pos, net.minecraft.core.Direction face) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null
            || !(client.hitResult instanceof BlockHitResult target) || !target.getBlockPos().equals(pos)) return false;
        Part part = partAt(client.level, pos, target);
        if (part == null || part.shape().isEmpty()) return false;
        Vec3 point = BlockBreakSamples.hit(part.shape().bounds(), face, client.level.getRandom())
            .add(Vec3.atLowerCornerOf(part.origin()));
        engine.add(new TerrainParticle(client.level, point.x, point.y, point.z, 0, 0, 0,
            part.material(), part.origin()).setPower(.2F).scale(.6F));
        return true;
    }

    private static @Nullable Part partAt(ClientLevel level, BlockPos pos, BlockHitResult hit) {
        BlockState state = level.getBlockState(pos);
        boolean top = state.is(HotPotContent.STOVE_TOP);
        BlockPos origin = top ? pos.below() : pos;
        BlockState stove = top ? level.getBlockState(origin) : state;
        if (StoveBlock.isStove(stove)) {
            boolean potHit = StoveBlock.isPotHit(stove, origin, hit);
            VoxelShape shape = potHit ? StoveBlock.potShape(stove) : StoveBlock.baseShape(stove);
            if (top) shape = Shapes.join(shape, Block.box(0, 16, 0, 16, 32, 16), BooleanOp.AND);
            BlockState material = potHit
                ? HotPotContent.blockFor(stove.getValue(StoveBlock.POT).material()).defaultBlockState()
                : Blocks.BRICKS.defaultBlockState();
            return new Part(origin, shape, material);
        }
        if (state.getBlock() instanceof HotPotTableBlock table
            && level.getBlockEntity(pos) instanceof HotPotTableBlockEntity be) {
            TableMaterials.Slot slot = table.slotAt(pos, hit.getLocation());
            return new Part(pos, table.particlePartShape(state, slot),
                slot == TableMaterials.Slot.TOP ? be.topBlockState() : be.baseBlockState());
        }
        if (state.getBlock() instanceof PrepRackBlock && level.getBlockEntity(pos) instanceof PrepRackBlockEntity rack) {
            return new Part(pos, state.getShape(level, pos), rack.frameBlockState());
        }
        return null;
    }

    /** Fabric 的奔跑/落地地形粒子没有 ModelData 入口，按源位置补上真实桌面或架体材质。 */
    public static @Nullable TextureAtlasSprite materialSprite(ClientLevel level, BlockPos pos, BlockState source) {
        BlockState material = null;
        if (source.getBlock() instanceof TallHotPotTableBlock
            && !(level.getBlockEntity(pos) instanceof HotPotTableBlockEntity)
            && level.getBlockState(pos.below()).is(source.getBlock())) {
            pos = pos.below();
        }
        if (source.getBlock() instanceof HotPotTableBlock
            && level.getBlockEntity(pos) instanceof HotPotTableBlockEntity table) {
            material = table.topBlockState();
        } else if (source.getBlock() instanceof PrepRackBlock
            && level.getBlockEntity(pos) instanceof PrepRackBlockEntity rack) {
            material = rack.frameBlockState();
        }
        return material == null ? null
            : Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(material).particleMaterial().sprite();
    }
}
