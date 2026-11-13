package cn.blockforge.generated.orientshotpotchronicles.hotpot;

// mryt233 改（2026-11-11）：限定类名改用 import，逻辑不变。
// mryt233 改（2026-11-11）：第三批 3G 右键结果改走适配小包（意图 → 原版常量），逻辑不变。
import cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter.Use;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter.UseOutcome;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

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
 *
 * <p>mryt233 改（1.20.1 适配）：悬浮窗入口是 {@code appendHoverText(ItemStack, @Nullable Level, List, TooltipFlag)}；
 * 食物属性用 {@code saturationMod} 与 {@code alwaysEat}。
 * 作者：mryt233
 */
public class SauceBottleItem extends ConfiguredFoodItem implements TableItem {
    private final SauceBottleKind kind;

    /**
     * @param kind       哪种调料瓶
     * @param foodId     食物配置的键（{@code food.<id>.hunger / .saturation}），和原来一样
     * @param nutrition  默认饥饿点数（配置里没写时用它）
     * @param saturation 默认饱和度系数
     */
    public SauceBottleItem(SauceBottleKind kind, String foodId, int nutrition, float saturation) {
        super(foodId, new Item.Properties().food(new FoodProperties.Builder()
            .nutrition(nutrition).saturationMod(saturation).alwaysEat().build()));
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
    public PlateBlock tableBlock(ItemStack stack) {
        return HotPotContent.SAUCE_BOTTLE_BLOCK;
    }

    @Override
    public boolean autoPlace(ItemStack stack) {
        return false;
    }

    @Override
    public void playAddSound(Level world, BlockPos pos, ItemStack stack) {
        world.playSound(null, pos, SoundEvents.GLASS_PLACE, SoundSource.BLOCKS, 0.6F, 1.4F);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        // 拿瓶子上桌只认 Shift+右键（竹筷也是这个规矩）：不潜行时把这次点击让给方块
        if (context.getPlayer() == null || !context.getPlayer().isShiftKeyDown()) {
            return Use.item(UseOutcome.PASS, context.getLevel().isClientSide);
        }
        return Use.item(TableObjects.placeInHolderOrOn(context, this), context.getLevel().isClientSide);
    }

    /**
     * 说明：这一味在蘸料台上的效果（和蘸料碗同一套文案，告诉玩家它有什么用）+ 一行「怎么摆、怎么拿」。
     * mryt233 改（2026-11-09，r146c）：不再显示剩余次数（次数机制已删）。
     */
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag type) {
        Seasoning.appendEffects(List.of(SauceIds.fullId(kind.seasoning().def())), tooltip);
        tooltip.add(Component.translatable(HotPotBlockEntity.key("tooltip.sauce_bottle.hint"))
            .withStyle(ChatFormatting.DARK_GRAY));
    }
}
