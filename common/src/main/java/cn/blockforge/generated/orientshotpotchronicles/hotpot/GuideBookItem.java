package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.level.Level;

/**
 * 游戏内教程书「火锅指南」（2026-10-25 用户：把网页教程做成一本书；菜刀 + 原版书无序合成）。
 * 右键打开书本界面（client/GuideBookScreen，内容是 tools/gen_guide_book.py 从网页教程生成的 guide/&lt;语言&gt;.json）。
 * 作者：mryt233 */
public class GuideBookItem extends Item {
    /** 客户端打开界面的钩子（HotPotClient 里设）；服务端什么都不做。 */
    public static Runnable clientOpen = () -> { };

    public GuideBookItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player user, InteractionHand hand) {
        if (world.isClientSide) {
            clientOpen.run();
        }
        return InteractionResultHolder.sidedSuccess(user.getItemInHand(hand), world.isClientSide);
    }
}
