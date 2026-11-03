package cn.blockforge.orient.framework.compat;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import java.util.Objects;
import java.util.function.BiConsumer;

/** 配方序列化器在平台规定的注册窗口落地。作者：mryt233 */
public final class RecipeRegistrations {
    private static BiConsumer<ResourceLocation, RecipeSerializer<?>> registrar =
        (id, serializer) -> Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, id, serializer);
    private RecipeRegistrations() { }
    public static void install(BiConsumer<ResourceLocation, RecipeSerializer<?>> platformRegistrar) {
        registrar = Objects.requireNonNull(platformRegistrar);
    }
    public static void register(ResourceLocation id, RecipeSerializer<?> serializer) {
        registrar.accept(id, serializer);
    }
}
