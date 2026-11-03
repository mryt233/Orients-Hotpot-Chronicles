package cn.blockforge.orient.framework.compat.mixin;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.CriterionTrigger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** 1.20.1 自定义进度触发器注册入口。作者：mryt233 */
@Mixin(CriteriaTriggers.class)
public interface CriteriaTriggersAccessor {
    @Invoker("register")
    static <T extends CriterionTrigger<?>> T register(T trigger) { throw new AssertionError(); }
}
