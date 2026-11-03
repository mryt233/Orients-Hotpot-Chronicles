// mryt233 改：26.1 原版标识符类型。
package cn.blockforge.orient.framework.platform.fabric;

import cn.blockforge.orient.framework.loader.RegistrationApi;
import cn.blockforge.orient.framework.pure.RegistrationGuard;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * Fabric 通用注册实现：注册表不冻结，立即注册；重复 id 预检失败即停。
 * 属于框架 Runtime，不含任何内容业务。作者：mryt233
 */
public final class FabricRegistrationApi implements RegistrationApi {
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> T put(Registry registry, Identifier id, Object value) {
        RegistrationGuard.requireAbsent(registry.containsKey(id), registry.key().identifier().toString(),
            id.toString(), id.getNamespace());
        return (T) Registry.register(registry, id, value);
    }

    @Override
    public ItemRegistrar items() {
        return new ItemRegistrar() {
            @Override
            public <T extends Item> T register(Identifier id, T item) {
                return put(BuiltInRegistries.ITEM, id, item);
            }
        };
    }

    @Override
    public BlockRegistrar blocks() {
        return new BlockRegistrar() {
            @Override
            public <T extends Block> T register(Identifier id, T block) {
                return put(BuiltInRegistries.BLOCK, id, block);
            }
        };
    }

    @Override
    public BlockEntityRegistrar blockEntities() {
        return new BlockEntityRegistrar() {
            @Override
            public <T extends BlockEntity> BlockEntityType<T> register(Identifier id,
                                                                       BlockEntityFactory<T> factory,
                                                                       Block... blocks) {
                return put(BuiltInRegistries.BLOCK_ENTITY_TYPE, id,
                    FabricBlockEntityTypeBuilder.create(factory::create, blocks).build());
            }
        };
    }

    @Override
    public MenuRegistrar menus() {
        return new MenuRegistrar() {
            @Override
            public <T extends AbstractContainerMenu> MenuType<T> register(Identifier id, MenuFactory<T> factory) {
                return put(BuiltInRegistries.MENU, id,
                    new ExtendedMenuType<>((syncId, inventory, pos) -> factory.create(syncId, inventory, pos),
                        BlockPos.STREAM_CODEC));
            }
        };
    }

    @Override
    public ComponentRegistrar components() {
        return new ComponentRegistrar() {
            @Override
            public <T> DataComponentType<T> register(Identifier id, DataComponentType<T> type) {
                return put(BuiltInRegistries.DATA_COMPONENT_TYPE, id, type);
            }
        };
    }

    @Override
    public CreativeTabRegistrar creativeTabs() {
        return (id, title, icon, content) -> put(BuiltInRegistries.CREATIVE_MODE_TAB, id,
            FabricCreativeModeTab.builder().title(title).icon(icon)
                .displayItems((params, output) -> content.fill(params, output::accept)).build());
    }

    @Override
    public EntityRegistrar entityTypes() {
        return new EntityRegistrar() {
            @Override
            public <T extends Entity> EntityType<T> register(Identifier id, EntityType<T> type) {
                return put(BuiltInRegistries.ENTITY_TYPE, id, type);
            }
        };
    }
}
