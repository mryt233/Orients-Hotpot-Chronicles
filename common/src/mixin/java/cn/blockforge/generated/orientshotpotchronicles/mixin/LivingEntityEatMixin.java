package cn.blockforge.generated.orientshotpotchronicles.mixin;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotContent;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.Seasoning;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 蘸过蘸料的菜（物品组件 dipped）不管怎么吃下去——筷子对空气吃、锅界面里吃、放回盘子再拿手吃——
 * 都会得到蘸料的效果（用户：buff 先弄到食物上，吃了后获得）。原版所有吃东西都走 LivingEntity.eatFood。
 * mryt233 改（2026-11-05，r136a）：在哪种汤底里煮出来的菜（COOKED_IN_SOUP 组件）吃下去时按汤底给风味加成，
 * 红汤再按辣度档位（SPICY_LEVEL 组件）加强；同时蘸了蜂蜜 / 醋的菜不再加负面「饥饿」。
 * 作者：mryt233 */
@Mixin(LivingEntity.class)
abstract class LivingEntityEatMixin {
    @org.spongepowered.asm.mixin.Shadow
    private java.util.Optional<net.minecraft.core.BlockPos> lastClimbablePos;

    /**
     * 一格高的条凳（<wood>_bench）上下搭了至少 2 层时，贴着凳塔侧面即可像爬梯子一样上下攀爬；
     * 单张摆在火锅桌边的条凳不触发攀爬（2026-11-05 用户确认，mryt233）。
     *
     * <p>mryt233 改（2026-11-07）：补三处边界——旁观、死亡、骑乘（坐在条凳 / 高脚凳上）都不参与条凳攀爬；
     * 以前坐在凳子上时原版也会走到这里，强行记一个「可攀位置」并返回 true，人会被当成贴墙攀爬，有几率粘在凳侧。
     * 另外只在真的命中可爬叠层条凳时才写 {@code lastClimbablePos}，不再对任意条凳塔都记一笔：
     * 客户端与服务端各写一份、又对不上时，{@code handleOnClimbable} 会算出不同位移，表现为「爬上去一顿一顿」。</p>
     */
    @Inject(method = "onClimbable", at = @At("HEAD"), cancellable = true)
    private void orientsHotpot$climbStackedBench(CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.isSpectator() || self.isPassenger() || !self.isAlive()) {
            return;
        }
        net.minecraft.core.BlockPos pos =
            cn.blockforge.generated.orientshotpotchronicles.hotpot.BenchBlock.findClimbableStackedBench(self);
        if (pos != null) {
            // 真的贴着可爬的叠层条凳才记录；两端都返回 true（否则客户端 travel 不走攀爬分支，就爬不上去）
            this.lastClimbablePos = java.util.Optional.of(pos);
            cir.setReturnValue(true);
        }
    }

    /**
     * mryt233 改（2026-11-06，r139）：布围裙防溅油 / 防沸水烫伤（用户要的两条防护之一）。
     *
     * <p>{@code LivingEntity.hurt} 的 HEAD 处：穿布围裙且身边 4 格内有烧着的灶台 / 篝火 / 火 / 岩浆时，
     * 火焰、营火、岩浆、热地板这几种烫伤直接取消——溅起来的滚油与沸水泼到身上不再掉血。
     * 判定逻辑在 {@link ClothApronProtection#blocksBurn}（核心层，加载器无关）。</p>
     */
    @Inject(method = "hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z", at = @At("HEAD"), cancellable = true)
    private void orientsHotpot$apronBlocksScald(net.minecraft.world.damagesource.DamageSource source, float amount,
                                                 CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof Player player
            && cn.blockforge.generated.orientshotpotchronicles.hotpot.ClothApronProtection
                .blocksBurn(player, source)) {
            cir.setReturnValue(false);
        }
    }

    /**
     * mryt233 改（2026-11-06，r139）：布围裙每刻维护——穿着就补「整装掌勺」成就，
     * 站在热源边把溅到身上的火苗灭掉（见 {@link ClothApronProtection#tickOne}）。
     */
    @Inject(method = "baseTick", at = @At("TAIL"))
    private void orientsHotpot$apronTick(org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        if ((Object) this instanceof Player player) {
            cn.blockforge.generated.orientshotpotchronicles.hotpot.ClothApronProtection.tickOne(player);
        }
    }

    @Inject(method = "eat(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/food/FoodProperties;)Lnet/minecraft/world/item/ItemStack;", at = @At("HEAD"))
    private void orientsHotpot$sauceEffects(Level world, ItemStack stack, FoodProperties food,
                                           CallbackInfoReturnable<ItemStack> cir) {
        Integer mask = stack.get(HotPotContent.DIPPED);
        if (!world.isClientSide && mask != null && mask != 0) {
            Seasoning.applyAll(mask, (LivingEntity) (Object) this);
        }
        // mryt233 改（2026-11-05，r136a）：汤底风味加成。这道菜是在哪种汤里煮出来的，就补一份那种汤的
        // 状态效果；红汤再按它带来的辣度档位加强（0 微辣 ~ 3 变态辣）。
        String soupKey = stack.get(HotPotContent.COOKED_IN_SOUP);
        if (!world.isClientSide && soupKey != null) {
            cn.blockforge.generated.orientshotpotchronicles.hotpot.SoupBase soup =
                cn.blockforge.generated.orientshotpotchronicles.hotpot.SoupBase.byTextureKey(soupKey);
            if (soup != null) {
                LivingEntity self = (LivingEntity) (Object) this;
                int spicy = stack.getOrDefault(HotPotContent.SPICY_LEVEL, 0);
                // 同时蘸了解辣的东西（蜂蜜 / 醋）：负面效果不再叠上去
                boolean calmSpicy = mask != null
                    && ((mask & Seasoning.HONEY.bit()) != 0 || (mask & Seasoning.VINEGAR.bit()) != 0);
                // 清汤、番茄汤本来就解冷锅反胃
                if (soup == cn.blockforge.generated.orientshotpotchronicles.hotpot.SoupBase.CLEAR
                    || soup == cn.blockforge.generated.orientshotpotchronicles.hotpot.SoupBase.TOMATO) {
                    self.removeEffect(net.minecraft.world.effect.MobEffects.CONFUSION);
                }
                for (net.minecraft.world.effect.MobEffectInstance effect : soup.dishBonusEffects(spicy)) {
                    if (calmSpicy && effect.getEffect().is(net.minecraft.world.effect.MobEffects.HUNGER)) {
                        continue;   // 蘸了蜂蜜 / 醋，红汤的饥饿不再往上叠
                    }
                    self.addEffect(effect);
                }
                // 变态辣且没解辣：嘴上 2 秒火苗，纯节目效果（前面刚给了抗火，不扣血）
                // mryt233 改（2026-11-06，r139）：穿着布围裙就不点嘴上这簇火——布围裙挡的是溅起来的滚油与明火。
                if (soup == cn.blockforge.generated.orientshotpotchronicles.hotpot.SoupBase.SPICY
                    && spicy >= 3 && !calmSpicy
                    && !cn.blockforge.generated.orientshotpotchronicles.hotpot.ClothApronItem.wornBy(self)) {
                    self.setRemainingFireTicks(Math.max(self.getRemainingFireTicks(), 40));
                }
            }
        }
        // 成就（2026-10-25）：吃了什么、蘸没蘸、是不是坐在凳子上吃的
        if (!world.isClientSide && (Object) this instanceof net.minecraft.world.entity.player.Player player) {
            cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotCriterion.fire(player,
                "eat:" + net.minecraft.core.registries.BuiltInRegistries.ITEM.getId(stack.getItem()));
            // mryt233 改（2026-11-06，r139「西南之魂」）：吃下折耳根（整根、段、熟段都算）就解锁。
            if (stack.is(cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotContent.ZHE_ER_GEN)
                || stack.is(cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotContent.ZHE_ER_GEN_SLICE)
                || stack.is(cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotContent.COOKED_ZHE_ER_GEN_SLICE)) {
                cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotCriterion.fire(player, "eat_zhe_er_gen");
            }
            if (mask != null && mask != 0) {
                cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotCriterion.fire(player, "eat_dipped");
                // mryt233 改（2026-11-06，r139「调味宗师」）：这一口蘸到了 5 种以上调料才算。
                if (Integer.bitCount(mask) >= 5) {
                    cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotCriterion.fire(player, "sauce_master_5");
                }
            }
            // 座位只取一次：getVehicle() 每帧都可能变，同一段判定里必须用同一个对象
            if (player.getVehicle() instanceof cn.blockforge.generated.orientshotpotchronicles.hotpot.SeatEntity seat) {
                // 所有座位（一格高条凳 <wood>_bench、高脚凳 <wood>_high_stool）都给「坐着吃」
                cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotCriterion.fire(player, "eat_seated");
                // mryt233 改（2026-11-07）：「围炉而坐 / eat_seated_bench」只认一格高条凳的座位。
                // 以前用 getClass() == SeatEntity.class 反推「恰好是条凳座位」——一旦 SeatEntity 被改成 abstract，
                // 或条凳座位换了子类，这里就静默失效 / 静默归错，且没有任何报错。
                // 现在反着写：只要不是高脚凳的座位（HighStoolBlock.Seat）就算条凳座位，
                // 高脚凳以后再加子类也仍然被排除在外。（让座位自己声明 isBenchSeat() 更彻底，留到下次动 SeatEntity 时做）
                if (!(seat instanceof cn.blockforge.generated.orientshotpotchronicles.hotpot.HighStoolBlock.Seat)) {
                    cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotCriterion.fire(player, "eat_seated_bench");
                }
            }
        }
    }

    /**
     * 用筷子吃东西时，吃的碎屑按筷子上那样菜来画（2026-10-23 用户报：吃丸子碎屑是紫黑的「未知方块」——
     * 筷子夹着菜的物品模型是代码画的 builtin/entity，没有碎屑贴图）。新增。
     */
    @org.spongepowered.asm.mixin.injection.ModifyVariable(method = "spawnItemParticles", at = @At("HEAD"), argsOnly = true)
    private ItemStack orientsHotpot$chopsticksCrumbs(ItemStack stack) {
        if (stack.is(HotPotContent.CHOPSTICKS)) {
            ItemStack held = cn.blockforge.generated.orientshotpotchronicles.hotpot.Chopsticks.held(stack);
            if (!held.isEmpty()) {
                return held;
            }
        }
        return stack;
    }
}