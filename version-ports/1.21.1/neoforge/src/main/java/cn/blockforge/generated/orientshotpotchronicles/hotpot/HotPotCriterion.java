//  移植：0.1.4-alpha 冻结底本，保留原作者署名。
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import cn.blockforge.generated.orientshotpotchronicles.GeneratedMod;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancement.criterion.AbstractCriterion;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.predicate.entity.EntityPredicate;
import net.minecraft.predicate.entity.LootContextPredicate;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Optional;

/**
 * 成就（进度）用的自定义触发器（2026-10-25 用户：成就系统）：原版触发器管不到的事，代码里 {@link #fire} 一个事件名，
 * 进度 JSON 里写 {"trigger": "orients_hotpot_chronicles:hotpot", "conditions": {"event": "事件名"}}。
 * 现有事件：pot_cooked（从锅里拿到熟菜）、carry_pot（整锅端走 / 从灶台拆下）、sauce_full（一碗蘸料放满所有调料）、
 * eat:&lt;物品 ID&gt;（吃下这样东西，筷子吃也算）、eat_dipped（吃蘸过的菜）、eat_seated（坐在凳子上吃）。
 * 进度文件由 tools/gen_advancements.py 生成。
 */
public class HotPotCriterion extends AbstractCriterion<HotPotCriterion.Conditions> {
    public static final HotPotCriterion INSTANCE = new HotPotCriterion();

    public static void register() {
        NeoRegistrations.queue(Registries.CRITERION, GeneratedMod.id("hotpot"), INSTANCE);
    }

    /** 只在服务端、真玩家身上触发；别处随便调。 */
    public static void fire(PlayerEntity player, String event) {
        if (player instanceof ServerPlayerEntity sp) {
            INSTANCE.trigger(sp, c -> c.event().equals(event));
        }
    }

    @Override
    public Codec<Conditions> getConditionsCodec() {
        return Conditions.CODEC;
    }

    public record Conditions(Optional<LootContextPredicate> player, String event) implements AbstractCriterion.Conditions {
        public static final Codec<Conditions> CODEC = RecordCodecBuilder.create(i -> i.group(
            EntityPredicate.LOOT_CONTEXT_PREDICATE_CODEC.optionalFieldOf("player").forGetter(Conditions::player),
            Codec.STRING.fieldOf("event").forGetter(Conditions::event)
        ).apply(i, Conditions::new));
    }
}
