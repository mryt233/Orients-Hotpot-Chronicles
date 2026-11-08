// mryt233 改：26.1 原版 API。
package cn.blockforge.generated.orientshotpotchronicles.mixin;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotContent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 火锅玩法对 {@code LivingEntity} 的三处注入。
 *
 * <p>mryt233 改（2026-11-08）：26.x 的进食效果统一挂在物品自带的 {@code CONSUMABLE} 上，
 * 蘸料效果、汤底风味加成与「吃了什么」这类成就全在 {@link FoodConsumeMixin}
 * （注入 {@code Consumable.onConsume}），这里不再注入 26.x 已经删掉的 {@code eat}。
 * 本类只留三件事：条凳叠搭攀爬、布围裙防烫伤与灭火苗、吃筷子夹的菜时碎渣按夹着的那样菜画。</p>
 * 作者：mryt233
 */
@Mixin(LivingEntity.class)
abstract class LivingEntityEatMixin {
    @Shadow
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
     * <p>{@code LivingEntity.hurtServer} 的 HEAD 处（26.x 的 hurt 已改成 final，注入点在 hurtServer）：穿布围裙且身边 4 格内有烧着的灶台 / 篝火 / 火 / 岩浆时，
     * 火焰、营火、岩浆、热地板这几种烫伤直接取消——溅起来的滚油与沸水泼到身上不再掉血。
     * 判定逻辑在 {@link ClothApronProtection#blocksBurn}（核心层，加载器无关）。</p>
     */
    // mryt233 改（2026-11-08）：26.x 把 Entity.hurt 改成 final，真正可注入的是 LivingEntity.hurtServer；
    // 以前注入的 hurt(...)Z 在 26.x 里根本不存在，客户端加载 mixin 时就会报目标方法找不到。
    @Inject(method = "hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z",
        at = @At("HEAD"), cancellable = true)
    private void orientsHotpot$apronBlocksScald(net.minecraft.server.level.ServerLevel level,
                                                 net.minecraft.world.damagesource.DamageSource source, float amount,
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
