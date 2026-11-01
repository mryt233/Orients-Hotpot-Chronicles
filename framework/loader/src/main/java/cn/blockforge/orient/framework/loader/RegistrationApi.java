package cn.blockforge.orient.framework.loader;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

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
        <T extends Item> T register(ResourceLocation id, T item);
    }

    interface BlockRegistrar {
        <T extends Block> T register(ResourceLocation id, T block);
    }

    interface BlockEntityRegistrar {
        <T extends BlockEntity> BlockEntityType<T> register(ResourceLocation id,
                                                            BlockEntityFactory<T> factory,
                                                            Block... blocks);
    }

    /** 平台负责把额外数据（方块坐标）编解码到客户端。 */
    @FunctionalInterface
    interface MenuFactory<T extends AbstractContainerMenu> {
        T create(int syncId, Inventory inventory, BlockPos pos);
    }

    interface MenuRegistrar {
        <T extends AbstractContainerMenu> MenuType<T> register(ResourceLocation id, MenuFactory<T> factory);
    }

    @FunctionalInterface
    interface BlockEntityFactory<T extends BlockEntity> {
        T create(BlockPos pos, BlockState state);
    }

    interface ComponentRegistrar {
        <T> DataComponentType<T> register(ResourceLocation id, DataComponentType<T> type);
    }

    @FunctionalInterface
    interface CreativeTabContent {
        void fill(CreativeModeTab.ItemDisplayParameters params, CreativeModeTab.Output output);
    }

    interface CreativeTabRegistrar {
        CreativeModeTab register(ResourceLocation id, Component title, Supplier<ItemStack> icon,
                                 CreativeTabContent content);
    }

    interface EntityRegistrar {
        <T extends Entity> EntityType<T> register(ResourceLocation id, EntityType<T> type);
    }
}