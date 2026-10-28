package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;

/**
 * 一格里装着几样独立东西的方块（灶台 + 锅、盘子格里的几个盘子……）：准星对着哪样，黑色选中框就只框那一样
 * （2026-10-23 用户：选中的是对的，但框把一格里的全框上了，观感不好；规范见 docs/交互规范手册.md）。
 * 客户端 client/PartOutlineRenderer 在画选中框时问这里；返回 null = 照原版框整个形状。
 *
 */
public interface PartOutline {
    VoxelShape aimedPart(BlockState state, BlockView world, BlockPos pos, PlayerEntity player);
}
