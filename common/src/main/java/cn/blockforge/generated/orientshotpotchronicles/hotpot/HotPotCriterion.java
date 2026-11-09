package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.world.entity.player.Player;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Registry;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/**
 * 成就（进度）用的自定义触发器（2026-10-25 用户：成就系统）：原版触发器管不到的事，代码里 {@link #fire} 一个事件名，
 * 进度 JSON 里写 {"trigger": "orients_hotpot_chronicles:hotpot", "conditions": {"event": "事件名"}}。
 * 现有事件：pot_cooked（从锅里拿到熟菜）、carry_pot（整锅端走 / 从灶台拆下）、sauce_full（一碗蘸料放满所有调料）、
 * eat:&lt;物品 ID&gt;（吃下这样东西，筷子吃也算）、eat_dipped（吃蘸过的菜）、eat_seated（坐在凳子上吃）、
 * drink_author（只喝植物蛋白饮料，瓶装喝一口或喝下杯装都算；mryt233 改 r146d）、
 * sauce_master_5（吃下一碗放了 5 种以上调料的蘸料）、eat_seated_bench（坐在一格高条凳上吃东西）、
 * wear_apron（穿上布围裙，等布围裙做好后由穿戴逻辑 fire）、eat_zhe_er_gen（吃下折耳根，等折耳根做好后 fire）
 * （后三条 mryt233 改 2026-11-06，r139 成就用）。
 * 进度文件由 tools/gen_advancements.py 生成。作者：mryt233 */
public class HotPotCriterion extends SimpleCriterionTrigger<HotPotCriterion.Conditions> {
    public static final HotPotCriterion INSTANCE = new HotPotCriterion();

    public static void register() {
        Registry.register(BuiltInRegistries.TRIGGER_TYPES, HotPot.id("hotpot"), INSTANCE);
    }

    /** 只在服务端、真玩家身上触发；别处随便调。 */
    public static void fire(Player player, String event) {
        if (player instanceof ServerPlayer sp) {
            INSTANCE.trigger(sp, c -> c.event().equals(event));
        }
    }

    @Override
    public Codec<Conditions> codec() {
        return Conditions.CODEC;
    }

    public record Conditions(Optional<ContextAwarePredicate> player, String event) implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<Conditions> CODEC = RecordCodecBuilder.create(i -> i.group(
            EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(Conditions::player),
            Codec.STRING.fieldOf("event").forGetter(Conditions::event)
        ).apply(i, Conditions::new));
    }
}
