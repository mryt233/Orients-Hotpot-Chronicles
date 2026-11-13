// mryt233 改：26.1 原版标识符与 Trigger 类型。
package cn.blockforge.orient.framework.loader;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Function;
import java.util.function.Supplier;

/** 分域注册能力：每个 registrar 只在其注册表窗口内生效；平台负责窗口与去重预检。作者：mryt233 */
public interface RegistrationApi {
    ItemRegistrar items();

    BlockRegistrar blocks();

    BlockEntityRegistrar blockEntities();

    MenuRegistrar menus();

    ComponentRegistrar components();

    CreativeTabRegistrar creativeTabs();

    EntityRegistrar entityTypes();

    interface ItemRegistrar {
        <T extends Item> T register(Identifier id, T item);

        // mryt233 改：先设置注册键，再交给内容工厂构造物品。
        default <T extends Item> T registerFactory(Identifier id, Function<Item.Properties, T> factory) {
            return register(id, factory.apply(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id))));
        }
    }

    interface BlockRegistrar {
        <T extends Block> T register(Identifier id, T block);

        default <T extends Block> T registerFactory(Identifier id, Function<BlockBehaviour.Properties, T> factory) {
            return registerFactory(id, BlockBehaviour.Properties.of(), factory);
        }

        default <T extends Block> T registerFactory(Identifier id, BlockBehaviour.Properties properties,
                                            Function<BlockBehaviour.Properties, T> factory) {
            return register(id, factory.apply(properties.setId(ResourceKey.create(Registries.BLOCK, id))));
        }
    }

    interface BlockEntityRegistrar {
        <T extends BlockEntity> BlockEntityType<T> register(Identifier id,
                                                            BlockEntityFactory<T> factory,
                                                            Block... blocks);
    }

    /** 平台负责把额外数据（方块坐标）编解码到客户端。 */
    @FunctionalInterface
    interface MenuFactory<T extends AbstractContainerMenu> {
        T create(int syncId, Inventory inventory, BlockPos pos);
    }

    interface MenuRegistrar {
        <T extends AbstractContainerMenu> MenuType<T> register(Identifier id, MenuFactory<T> factory);
    }

    @FunctionalInterface
    interface BlockEntityFactory<T extends BlockEntity> {
        T create(BlockPos pos, BlockState state);
    }

    interface ComponentRegistrar {
        <T> DataComponentType<T> register(Identifier id, DataComponentType<T> type);
    }

    @FunctionalInterface
    interface CreativeTabContent {
        // mryt233 改：不在公开契约中引用原版非公开输出类型。
        void fill(CreativeModeTab.ItemDisplayParameters params, CreativeTabOutput output);
    }

    interface CreativeTabRegistrar {
        CreativeModeTab register(Identifier id, Component title, Supplier<ItemStack> icon,
                                 CreativeTabContent content);
    }

    interface EntityRegistrar {
        <T extends Entity> EntityType<T> register(Identifier id, EntityType<T> type);
    }

    // mryt233 改（2026-11-10）：补充配方、条件触发、声音与粒子注册表域；旧实现保留默认抛出。
    default RecipeSerializerRegistrar recipeSerializers() {
        throw new UnsupportedOperationException("recipeSerializers requires Runtime 0.1.5");
    }

    interface RecipeSerializerRegistrar {
        <T extends net.minecraft.world.item.crafting.RecipeSerializer<?>> T register(Identifier id, T value);
    }

    default RecipeTypeRegistrar recipeTypes() {
        throw new UnsupportedOperationException("recipeTypes requires Runtime 0.1.5");
    }

    interface RecipeTypeRegistrar {
        <T extends net.minecraft.world.item.crafting.RecipeType<?>> T register(Identifier id, T value);
    }

    default CriterionRegistrar criteria() {
        throw new UnsupportedOperationException("criteria requires Runtime 0.1.5");
    }

    interface CriterionRegistrar {
        <T extends net.minecraft.advancements.CriterionTrigger<?>> T register(Identifier id, T value);
    }

    default SoundRegistrar sounds() {
        throw new UnsupportedOperationException("sounds requires Runtime 0.1.5");
    }

    interface SoundRegistrar {
        net.minecraft.sounds.SoundEvent register(Identifier id, net.minecraft.sounds.SoundEvent value);
    }

    default ParticleRegistrar particles() {
        throw new UnsupportedOperationException("particles requires Runtime 0.1.5");
    }

    interface ParticleRegistrar {
        <T extends net.minecraft.core.particles.ParticleType<?>> T register(Identifier id, T value);
    }
}
