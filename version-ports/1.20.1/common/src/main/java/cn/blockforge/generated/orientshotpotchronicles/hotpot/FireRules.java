package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

/** 九种可燃木材及橡木砧板，铁石和下界木均不燃。作者：mryt233 */
final class FireRules {
    private FireRules() { }
    static boolean flammable(Block block) {
        if (block instanceof CuttingBoardBlock) return true;
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
        if (id == null || !id.getNamespace().equals(HotPot.MOD_ID)) return false;
        for (String wood : HotPotContent.TABLE_WOODS) {
            if (!wood.equals("crimson") && !wood.equals("warped") && id.getPath().startsWith(wood + "_")) return true;
        }
        return false;
    }
}
