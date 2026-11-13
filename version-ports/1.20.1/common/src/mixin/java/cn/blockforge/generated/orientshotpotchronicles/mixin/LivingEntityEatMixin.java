package cn.blockforge.generated.orientshotpotchronicles.mixin;

// mryt233 改（2026-11-11）：限定类名改用 import，逻辑不变。
import cn.blockforge.generated.orientshotpotchronicles.hotpot.BenchBlock;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.Chopsticks;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.ClothApronItem;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.ClothApronProtection;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.Config;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotContent;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPotCriterion;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.SauceIds;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.Seasoning;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.SeatEntity;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.SoupDef;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.Soups;
import cn.blockforge.orient.framework.compat.ItemData;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 蘸过蘸料的菜（物品 NBT dipped）不管怎么吃下去——筷子对空气吃、锅界面里吃、放回盘子再拿手吃——
 * 都会得到蘸料的效果（用户：buff 先弄到食物上，吃了后获得）。原版所有吃东西都走 LivingEntity.eat。
 * mryt233 改（2026-11-05，清单 #117）：在哪种汤底里煮出来的菜（COOKED_IN_SOUP）吃下去时按汤底给风味加成，
 * 红汤再按辣度档位（SPICY_LEVEL）加强；同时蘸了蜂蜜 / 醋的菜不再加负面「饥饿」。
 * mryt233 改（2026-11-06，清单 #127）：布围裙防溅油 / 防沸水烫伤（hurt 取消 + baseTick 灭火苗与成就）；
 * 吃折耳根解锁「西南之魂」。
 * mryt233 改（2026-11-05，清单 #120）：一格高条凳上下搭 2 层以上时贴边即可像爬梯子一样攀爬。
 * <p>mryt233 改（0.2.2-alpha，1.20.1 适配）：
 * ① 1.20.1 的吃入口是 {@code eat(Level, ItemStack)}（两参），1.21.1 多了 {@code FoodProperties} 第三参；
 * ② 物品数据走 {@link ItemData}（NBT），不是 {@code stack.get(DataComponentType)}；
 * ③ 效果用 {@code effect.getEffect()}（1.20.1 直接是 {@code MobEffect}），不是 {@code .getEffect().value()}；
 * ④ 物品 ID 键用 {@code BuiltInRegistries.ITEM.getKey(stack.getItem())}。
 * 作者：mryt233
 */
@Mixin(LivingEntity.class)
abstract class LivingEntityEatMixin {
    @Shadow
    private Optional<BlockPos> lastClimbablePos;

    /**
     * 一格高的条凳（<wood>_bench）上下搭了至少 2 层时，贴着凳塔侧面即可像爬梯子一样上下攀爬；
     * 单张摆在火锅桌边的条凳不触发攀爬。
     */
    @Inject(method = "onClimbable", at = @At("HEAD"), cancellable = true)
    private void orientsHotpot$climbStackedBench(CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.isSpectator() || self.isPassenger() || !self.isAlive()) {
            return;
        }
        BlockPos pos = BenchBlock.findClimbableStackedBench(self);
        if (pos != null) {
            this.lastClimbablePos = Optional.of(pos);
            cir.setReturnValue(true);
        }
    }

    /**
     * mryt233 改（2026-11-06，清单 #127）：布围裙防溅油 / 防沸水烫伤（用户要的两条防护之一）。
     */
    @Inject(method = "hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z", at = @At("HEAD"), cancellable = true)
    private void orientsHotpot$apronBlocksScald(DamageSource source, float amount,
                                                 CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof Player player
            && ClothApronProtection.blocksBurn(player, source)) {
            cir.setReturnValue(false);
        }
    }

    /**
     * mryt233 改（2026-11-06，清单 #127）：布围裙每刻维护——穿着就补「整装掌勺」成就，
     * 站在热源边把溅到身上的火苗灭掉。
     */
    @Inject(method = "baseTick", at = @At("TAIL"))
    private void orientsHotpot$apronTick(CallbackInfo ci) {
        if ((Object) this instanceof Player player) {
            ClothApronProtection.tickOne(player);
        }
    }

    @Inject(method = "eat(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;", at = @At("HEAD"))
    private void orientsHotpot$sauceEffects(Level world, ItemStack stack,
                                            CallbackInfoReturnable<ItemStack> cir) {
        List<String> dipped = ItemData.get(stack, HotPotContent.DIPPED);   // mryt233 改（4E-1）：ID 列表
        if (!world.isClientSide && dipped != null && !dipped.isEmpty()) {
            Seasoning.applyAll(dipped, (LivingEntity) (Object) this);
        }
        // mryt233 改（2026-11-05，清单 #117）：汤底风味加成。这道菜是在哪种汤里煮出来的，就补一份那种汤的
        // 状态效果；红汤再按它带来的辣度档位加强（0 微辣 ~ 3 变态辣）。
        String soupKey = ItemData.get(stack, HotPotContent.COOKED_IN_SOUP);
        if (!world.isClientSide && soupKey != null) {
            SoupDef soup = Soups.get(soupKey);   // mryt233 改（2026-11-11，4B-2）：问登记表
            if (soup != null) {
                LivingEntity self = (LivingEntity) (Object) this;
                Integer storedSpicy = ItemData.get(stack, HotPotContent.SPICY_LEVEL);
                int spicy = storedSpicy == null ? 0 : storedSpicy;
                // 同时蘸了解辣的东西（蜂蜜 / 醋）：负面效果不再叠上去
                boolean calmSpicy = dipped != null
                    && (SauceIds.containsDef(dipped, Seasoning.HONEY) || SauceIds.containsDef(dipped, Seasoning.VINEGAR));
                // 清汤、番茄汤本来就解冷锅反胃
                // mryt233 改（2026-11-10，r147）：「是不是清汤 / 番茄」改成汤底自己的属性 clearsNausea()
                if (soup.clearsNausea()) {
                    self.removeEffect(MobEffects.CONFUSION);
                }
                for (MobEffectInstance effect : soup.dishBonusEffects(spicy)) {
                    // mryt233 改（1.20.1 适配）：1.20.1 的 getEffect() 直接返回 MobEffect
                    if (calmSpicy && effect.getEffect() == MobEffects.HUNGER) {
                        continue;   // 蘸了蜂蜜 / 醋，红汤的饥饿不再往上叠
                    }
                    self.addEffect(effect);
                }
                // 变态辣且没解辣：嘴上冒火苗，纯节目效果（前面刚给了抗火，不扣血）
                // mryt233 改（2026-11-06，r139）：穿着布围裙就不点嘴上这簇火——布围裙挡的是溅起来的滚油与明火。
                // mryt233 改（2026-11-10）：红汤判断改成 spicy() 属性；mryt233 改（第四批 4J）：改读 Config.spicyMouthFire*
                int mouthFireTicks = Config.spicyMouthFireTicks();
                if (soup.spicy()
                    && mouthFireTicks > 0 && spicy >= Config.spicyMouthFireFromLevel()
                    && !calmSpicy
                    && !ClothApronItem.wornBy(self)) {
                    self.setRemainingFireTicks(Math.max(self.getRemainingFireTicks(), mouthFireTicks));
                }
            }
        }
        // 成就（2026-10-25）：吃了什么、蘸没蘸、是不是坐在凳子上吃的
        if (!world.isClientSide && (Object) this instanceof Player player) {
            HotPotCriterion.fire(player,
                "eat:" + BuiltInRegistries.ITEM.getKey(stack.getItem()));
            // mryt233 改（2026-11-06，r139「西南之魂」）：吃下折耳根（整根、段、熟段都算）就解锁。
            if (stack.is(HotPotContent.ZHE_ER_GEN)
                || stack.is(HotPotContent.ZHE_ER_GEN_SLICE)
                || stack.is(HotPotContent.COOKED_ZHE_ER_GEN_SLICE)) {
                HotPotCriterion.fire(player, "eat_zhe_er_gen");
            }
            if (dipped != null && !dipped.isEmpty()) {
                HotPotCriterion.fire(player, "eat_dipped");
                // mryt233 改（2026-11-06，r139「调味宗师」）：这一口蘸到了 5 种以上调料才算。
                if (dipped.size() >= 5) {
                    HotPotCriterion.fire(player, "sauce_master_5");
                }
            }
            // 座位只取一次：getVehicle() 每帧都可能变，同一段判定里必须用同一个对象
            if (player.getVehicle() instanceof SeatEntity seat) {
                // mryt233 改（2026-11-10）：运行时仅保留条凳座位，高凳已归档。
                HotPotCriterion.fire(player, "eat_seated");
                HotPotCriterion.fire(player, "eat_seated_bench");
            }
        }
    }

    /**
     * 用筷子吃东西时，吃的碎屑按筷子上那样菜来画（2026-10-23 用户报：吃丸子碎屑是紫黑的「未知方块」——
     * 筷子夹着菜的物品模型是代码画的 builtin/entity，没有碎屑贴图）。
     */
    @ModifyVariable(method = "spawnItemParticles", at = @At("HEAD"), argsOnly = true)
    private ItemStack orientsHotpot$chopsticksCrumbs(ItemStack stack) {
        if (stack.is(HotPotContent.CHOPSTICKS)) {
            ItemStack held = Chopsticks.held(stack);
            if (!held.isEmpty()) {
                return held;
            }
        }
        return stack;
    }
}
