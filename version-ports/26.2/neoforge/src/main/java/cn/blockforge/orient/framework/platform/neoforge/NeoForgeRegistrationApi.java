// mryt233 改：26.1 原版标识符类型。
package cn.blockforge.orient.framework.platform.neoforge;

import cn.blockforge.orient.framework.loader.RegistrationApi;
import cn.blockforge.orient.framework.pure.RegistrationGuard;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.network.IContainerFactory;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * NeoForge 通用注册实现：对象立即构造、按注册表排队，在各自 RegisterEvent 窗口内落地。
 * 重复 id 预检失败即停（与 Fabric 行为统一）。属于框架 Runtime，不含内容业务。作者：mryt233
 *
 * <p>mryt233 改（2026-11-01，前置检查 F1）：某个注册表的 RegisterEvent 过了以后再登记，以前会进队列但永远不注册、
 * 也不报错；现在每个注册表落地后关窗，之后再登记直接抛出带 id 的异常（与「注册失败即停」一致）。</p>
 */
public final class NeoForgeRegistrationApi implements RegistrationApi {
    private record Entry<T>(Identifier id, T value) {
    }

    /** 已经落地（关窗）的注册表。 */
    private final java.util.Set<ResourceKey<?>> closed = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private void requireOpen(ResourceKey<?> registry, Identifier id) {
        if (closed.contains(registry)) {
            throw new IllegalStateException("registration window already closed: registry="
                + registry.identifier() + ", id=" + id + "（请在模组构造 / 注册阶段登记）");
        }
    }

    private <T> void flushAndClose(ResourceKey<?> key, Registry<T> registry, List<Entry<T>> queue) {
        flush(registry, queue);
        closed.add(key);
    }

    private final List<Entry<Item>> items = new ArrayList<>();
    private final List<Entry<Block>> blocks = new ArrayList<>();
    private final List<Entry<BlockEntityType<?>>> blockEntities = new ArrayList<>();
    private final List<Entry<MenuType<?>>> menus = new ArrayList<>();
    private final List<Entry<DataComponentType<?>>> components = new ArrayList<>();
    private final List<Entry<CreativeModeTab>> tabs = new ArrayList<>();
    private final List<Entry<EntityType<?>>> entities = new ArrayList<>();

    private static <T> void flush(Registry<T> registry, List<Entry<T>> queue) {
        for (Entry<T> entry : queue) {
            RegistrationGuard.requireAbsent(registry.containsKey(entry.id()),
                registry.key().identifier().toString(), entry.id().toString(), entry.id().getNamespace());
            Registry.register(registry, entry.id(), entry.value());
        }
        queue.clear();
    }

    void onRegisterEvent(RegisterEvent event) {
        ResourceKey<?> key = event.getRegistryKey();
        if (key.equals(Registries.ITEM)) {
            flushAndClose(key, BuiltInRegistries.ITEM, items);
        } else if (key.equals(Registries.BLOCK)) {
            flushAndClose(key, BuiltInRegistries.BLOCK, blocks);
        } else if (key.equals(Registries.BLOCK_ENTITY_TYPE)) {
            flushAndClose(key, BuiltInRegistries.BLOCK_ENTITY_TYPE, blockEntities);
        } else if (key.equals(Registries.MENU)) {
            flushAndClose(key, BuiltInRegistries.MENU, menus);
        } else if (key.equals(Registries.DATA_COMPONENT_TYPE)) {
            flushAndClose(key, BuiltInRegistries.DATA_COMPONENT_TYPE, components);
        } else if (key.equals(Registries.CREATIVE_MODE_TAB)) {
            flushAndClose(key, BuiltInRegistries.CREATIVE_MODE_TAB, tabs);
        } else if (key.equals(Registries.ENTITY_TYPE)) {
            flushAndClose(key, BuiltInRegistries.ENTITY_TYPE, entities);
        }
    }

    @Override
    public ItemRegistrar items() {
        return new ItemRegistrar() {
            @Override
            public <T extends Item> T register(Identifier id, T item) {
                requireOpen(Registries.ITEM, id);
                items.add(new Entry<Item>(id, item));
                return item;
            }
        };
    }

    @Override
    public BlockRegistrar blocks() {
        return new BlockRegistrar() {
            @Override
            public <T extends Block> T register(Identifier id, T block) {
                requireOpen(Registries.BLOCK, id);
                blocks.add(new Entry<Block>(id, block));
                return block;
            }
        };
    }

    @Override
    public BlockEntityRegistrar blockEntities() {
        return new BlockEntityRegistrar() {
            @Override
            public <T extends BlockEntity> BlockEntityType<T> register(Identifier id,
                                                                       BlockEntityFactory<T> factory,
                                                                       Block... blockArray) {
                requireOpen(Registries.BLOCK_ENTITY_TYPE, id);
                BlockEntityType<T> type = new BlockEntityType<>(factory::create, blockArray); // mryt233 改：26.1 去掉了 Builder
                blockEntities.add(new Entry<BlockEntityType<?>>(id, type));
                return type;
            }
        };
    }

    @Override
    public MenuRegistrar menus() {
        return new MenuRegistrar() {
            @Override
            public <T extends AbstractContainerMenu> MenuType<T> register(Identifier id, MenuFactory<T> factory) {
                requireOpen(Registries.MENU, id);
                MenuType<T> type = IMenuTypeExtension.create((IContainerFactory<T>)
                    (syncId, inventory, data) -> factory.create(syncId, inventory, data.readBlockPos()));
                menus.add(new Entry<>(id, type));
                return type;
            }
        };
    }

    @Override
    public ComponentRegistrar components() {
        return new ComponentRegistrar() {
            @Override
            public <T> DataComponentType<T> register(Identifier id, DataComponentType<T> type) {
                requireOpen(Registries.DATA_COMPONENT_TYPE, id);
                components.add(new Entry<>(id, type));
                return type;
            }
        };
    }

    @Override
    public CreativeTabRegistrar creativeTabs() {
        return (id, title, icon, content) -> {
            requireOpen(Registries.CREATIVE_MODE_TAB, id);
            CreativeModeTab tab = CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                .title(title).icon(icon)
                .displayItems((params, output) -> content.fill(params, output::accept)).build();
            tabs.add(new Entry<>(id, tab));
            return tab;
        };
    }

    @Override
    public EntityRegistrar entityTypes() {
        return new EntityRegistrar() {
            @Override
            public <T extends Entity> EntityType<T> register(Identifier id, EntityType<T> type) {
                requireOpen(Registries.ENTITY_TYPE, id);
                entities.add(new Entry<EntityType<?>>(id, type));
                return type;
            }
        };
    }
}
