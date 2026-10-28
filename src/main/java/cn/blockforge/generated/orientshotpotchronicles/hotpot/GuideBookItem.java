package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

/**
 * 游戏内教程书「火锅指南」（2026-10-25 用户：把网页教程做成一本书；菜刀 + 原版书无序合成）。
 * 右键打开书本界面（client/GuideBookScreen，内容是 tools/gen_guide_book.py 从网页教程生成的 guide/&lt;语言&gt;.json）。
 *
 */
public class GuideBookItem extends Item {
    /** 客户端打开界面的钩子（HotPotClient 里设）；服务端什么都不做。 */
    public static Runnable clientOpen = () -> { };

    public GuideBookItem() {
        super(new Item.Settings().maxCount(1));
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if (world.isClient) {
            clientOpen.run();
        }
        return TypedActionResult.success(user.getStackInHand(hand), world.isClient);
    }
}
