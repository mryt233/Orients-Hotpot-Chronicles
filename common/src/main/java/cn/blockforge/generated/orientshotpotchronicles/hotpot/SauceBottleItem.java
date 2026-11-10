package cn.blockforge.generated.orientshotpotchronicles.hotpot;

// mryt233 改（2026-11-11）：限定类名改用 import，逻辑不变。
// mryt233 改（2026-11-11）：第三批 3G 右键结果改走适配小包（意图 → 原版常量），逻辑不变。
import cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter.Use;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter.UseOutcome;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.food.FoodProperties;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;

import java.util.List;

/**
 * 能摆上桌的蘸料瓶（r146a 2026-11-08 加，r146c 2026-11-09 按用户意见改成「只摆放」）：芝麻酱、蚝油。
 *
 * <p>和原来一样能吃（营养值照旧走配置 {@code food.<id>}），额外只做一件事：
 * <b>Shift+右键桌面 / 地面</b>把这瓶摆到桌上（借 {@link PlateHolderBlock} 的「盘子格」摆，方块是
 * {@link SauceBottleBlock}），空手右键拿回来、对着瓶子挖只挖这一瓶。摆上去就是个摆设。
 *
 * <p>mryt233 改（2026-11-09，r146c）：<b>去掉桌上加料</b>。摆在桌上的瓶子不再能给菜加料，
 * 想加这一味仍然和以前一样去蘸料台打蘸料；同时去掉了每瓶次数（组件 {@code sauce_bottle_used}、
 * 配置 {@code sauce_bottle.*.uses}、用完还容器、加料的声光粒子），物品恢复成可正常堆叠。
 * 所以这个类现在只负责「摆放 + 说明文字」，加料那套逻辑全部删掉。
 *
 * <p>照竹筷 {@link ChopsticksItem} 的规矩：不潜行时把这次点击让给方块自己的交互，
 * 瓶子上桌只认 Shift，免得手滑把桌上的菜盘当成摆放目标。
 * 作者：mryt233 */
public class SauceBottleItem extends ConfiguredFoodItem {
    private final SauceBottleKind kind;

    /**
     * @param kind       哪种调料瓶
     * @param foodId     食物配置的键（{@code food.<id>.hunger / .saturation}），和原来一样
     * @param nutrition  默认饥饿点数（配置里没写时用它）
     * @param saturation 默认饱和度系数
     */
    public SauceBottleItem(SauceBottleKind kind, String foodId, int nutrition, float saturation) {
        super(foodId, new Item.Properties().food(new FoodProperties.Builder()
            .nutrition(nutrition).saturationModifier(saturation).alwaysEdible().build()));
        this.kind = kind;
        Config.rememberFood(foodId, nutrition, saturation);
    }

    public SauceBottleKind kind() {
        return kind;
    }

    /** 这叠物品是哪种瓶子；不是蘸料瓶（凌空、存档里被改坏）返回 null。 */
    public static SauceBottleKind kindOf(ItemStack stack) {
        return stack.getItem() instanceof SauceBottleItem bottle ? bottle.kind() : null;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        // 拿瓶子上桌只认 Shift+右键（竹筷也是这个规矩）：不潜行时把这次点击让给方块
        if (context.getPlayer() == null || !context.getPlayer().isShiftKeyDown()) {
            return Use.item(UseOutcome.PASS, context.getLevel().isClientSide);
        }
        return placeBottle(context);
    }

    /**
     * 摆在桌面 / 地面上：点在桌面时射线打到的是那格 {@link PlateHolderBlock}，要摆进眼前这一格；
     * 点在普通方块顶面时照 {@link PlateItem#placeOn} 的规矩摆到上面一格。
     * 和竹筷 {@link ChopsticksItem} 一样走「整件摆上去」（keepSelf），所以拿回来时原样还回这瓶。
     */
    private InteractionResult placeBottle(UseOnContext context) {
        Level world = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (context.getClickedFace() == Direction.UP
            && world.getBlockState(pos).is(HotPotContent.PLATE_HOLDER)
            && world.getBlockEntity(pos) instanceof PlateHolderBlockEntity holder) {
            if (world.isClientSide) {
                return Use.item(UseOutcome.SUCCESS, world.isClientSide);
            }
            double hx = (context.getClickLocation().x - pos.getX()) * 16;
            double hz = (context.getClickLocation().z - pos.getZ()) * 16;
            Direction.Axis axis = HotPotBlock.handleAxisFor(context.getHorizontalDirection());
            if (holder.tryAdd(HotPotContent.SAUCE_BOTTLE_BLOCK, axis, hx, hz, context.getItemInHand(), true)) {
                world.playSound(null, pos, SoundEvents.GLASS_PLACE,
                    SoundSource.BLOCKS, 0.6F, 1.4F);
                context.getItemInHand().consume(1, context.getPlayer());
                return Use.item(UseOutcome.SUCCESS, world.isClientSide);
            }
            if (context.getPlayer() != null) {
                context.getPlayer().displayClientMessage(
                    Component.translatable(HotPotBlockEntity.key("msg.plate_no_room")), true);
            }
            return Use.item(UseOutcome.FAIL, world.isClientSide);
        }
        return PlateItem.placeOn(context, HotPotContent.SAUCE_BOTTLE_BLOCK, true);
    }

    /**
     * 说明：这一味在蘸料台上的效果（和蘸料碗同一套文案，告诉玩家它有什么用）+ 一行「怎么摆、怎么拿」。
     * mryt233 改（2026-11-09，r146c）：不再显示剩余次数（次数机制已删）。
     */
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag type) {
        Seasoning.appendEffects(kind.seasoning().bit(), tooltip);
        tooltip.add(Component.translatable(HotPotBlockEntity.key("tooltip.sauce_bottle.hint"))
            .withStyle(ChatFormatting.DARK_GRAY));
    }
}
