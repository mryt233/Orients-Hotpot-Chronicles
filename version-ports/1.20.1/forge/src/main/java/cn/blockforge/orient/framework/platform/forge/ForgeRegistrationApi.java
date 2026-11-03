package cn.blockforge.orient.framework.platform.forge;

import cn.blockforge.orient.framework.loader.RegistrationApi;
import cn.blockforge.orient.framework.pure.RegistrationGuard;
import net.minecraft.core.Registry;
import cn.blockforge.orient.framework.compat.ItemDataKey;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
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
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.network.IContainerFactory;
import net.minecraftforge.registries.RegisterEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Forge 通用注册实现：对象立即构造、按注册表排队，在各自 RegisterEvent 窗口内落地。
 * 重复 id 预检失败即停（与 Fabric 行为统一）。属于框架 Runtime，不含内容业务。作者：mryt233
 *
 * <p>mryt233 改（2026-11-01，前置检查 F1）：某个注册表的 RegisterEvent 过了以后再登记，以前会进队列但永远不注册、
 * 也不报错；现在每个注册表落地后关窗，之后再登记直接抛出带 id 的异常（与「注册失败即停」一致）。</p>
 */
public final class ForgeRegistrationApi implements RegistrationApi {
    private record Entry<T>(ResourceLocation id, T value) {
    }

    /** 已经落地（关窗）的注册表。 */
    private final java.util.Set<ResourceKey<?>> closed = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private void requireOpen(ResourceKey<?> registry, ResourceLocation id) {
        if (closed.contains(registry)) {
            throw new IllegalStateException("registration window already closed: registry="
                + registry.location() + ", id=" + id + "（请在模组构造 / 注册阶段登记）");
        }
    }

    @SuppressWarnings("unchecked")
    private <T> void flushAndClose(RegisterEvent event, Registry<T> registry, List<Entry<T>> queue) {
        flush(event, registry, queue);
        closed.add(event.getRegistryKey());
    }

    private final List<Entry<Item>> items = new ArrayList<>();
    private final List<Entry<Block>> blocks = new ArrayList<>();
    private final List<Entry<BlockEntityType<?>>> blockEntities = new ArrayList<>();
    private final List<Entry<MenuType<?>>> menus = new ArrayList<>();
    private final java.util.Set<ResourceLocation> components = new java.util.HashSet<>();
    private final List<Entry<CreativeModeTab>> tabs = new ArrayList<>();
    private final List<Entry<EntityType<?>>> entities = new ArrayList<>();
    private final List<Entry<net.minecraft.world.item.crafting.RecipeSerializer<?>>> recipeSerializers = new ArrayList<>();

    void queueRecipeSerializer(ResourceLocation id, net.minecraft.world.item.crafting.RecipeSerializer<?> serializer) {
        requireOpen(Registries.RECIPE_SERIALIZER, id);
        recipeSerializers.add(new Entry<>(id, serializer));
    }

    /**
     * mryt233 改（2026-11-02）：改用 {@code RegisterEvent.register}，不再直接调原版 {@code Registry.register}。
     * Forge 47.0.0 在 RegisterEvent 里仍锁着原版注册表，直接写会抛「Can not register to a locked registry」；
     * 47.4.x 才放宽。用事件自带的注册方法后，最低能支持到 Forge 47.0.0。
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> void flush(RegisterEvent event, Registry<T> registry, List<Entry<T>> queue) {
        for (Entry<T> entry : queue) {
            RegistrationGuard.requireAbsent(registry.containsKey(entry.id()),
                registry.key().location().toString(), entry.id().toString(), entry.id().getNamespace());
            T value = entry.value();
            event.register((ResourceKey) registry.key(), entry.id(), () -> value);
        }
        queue.clear();
    }

    void onRegisterEvent(RegisterEvent event) {
        ResourceKey<?> key = event.getRegistryKey();
        if (key.equals(Registries.ITEM)) {
            flushAndClose(event, BuiltInRegistries.ITEM, items);
        } else if (key.equals(Registries.BLOCK)) {
            flushAndClose(event, BuiltInRegistries.BLOCK, blocks);
        } else if (key.equals(Registries.BLOCK_ENTITY_TYPE)) {
            flushAndClose(event, BuiltInRegistries.BLOCK_ENTITY_TYPE, blockEntities);
        } else if (key.equals(Registries.MENU)) {
            flushAndClose(event, BuiltInRegistries.MENU, menus);

        } else if (key.equals(Registries.CREATIVE_MODE_TAB)) {
            flushAndClose(event, BuiltInRegistries.CREATIVE_MODE_TAB, tabs);
        } else if (key.equals(Registries.ENTITY_TYPE)) {
            flushAndClose(event, BuiltInRegistries.ENTITY_TYPE, entities);
        } else if (key.equals(Registries.RECIPE_SERIALIZER)) {
            flushAndClose(event, BuiltInRegistries.RECIPE_SERIALIZER, recipeSerializers);
        }
    }

    @Override
    public ItemRegistrar items() {
        return new ItemRegistrar() {
            @Override
            public <T extends Item> T register(ResourceLocation id, T item) {
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
            public <T extends Block> T register(ResourceLocation id, T block) {
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
            public <T extends BlockEntity> BlockEntityType<T> register(ResourceLocation id,
                                                                       BlockEntityFactory<T> factory,
                                                                       Block... blockArray) {
                requireOpen(Registries.BLOCK_ENTITY_TYPE, id);
                BlockEntityType<T> type = BlockEntityType.Builder.of(factory::create, blockArray).build(null);
                blockEntities.add(new Entry<BlockEntityType<?>>(id, type));
                return type;
            }
        };
    }

    @Override
    public MenuRegistrar menus() {
        return new MenuRegistrar() {
            @Override
            public <T extends AbstractContainerMenu> MenuType<T> register(ResourceLocation id, MenuFactory<T> factory) {
                requireOpen(Registries.MENU, id);
                MenuType<T> type = IForgeMenuType.create((IContainerFactory<T>)
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
            public <T> ItemDataKey<T> register(ResourceLocation id, ItemDataKey<T> type) {
                requireOpen(Registries.ITEM, id);
                RegistrationGuard.requireAbsent(!components.add(id), "item_data", id.toString(), id.getNamespace());
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
                .displayItems((params, output) -> content.fill(params, output)).build();
            tabs.add(new Entry<>(id, tab));
            return tab;
        };
    }

    @Override
    public EntityRegistrar entityTypes() {
        return new EntityRegistrar() {
            @Override
            public <T extends Entity> EntityType<T> register(ResourceLocation id, EntityType<T> type) {
                requireOpen(Registries.ENTITY_TYPE, id);
                entities.add(new Entry<EntityType<?>>(id, type));
                return type;
            }
        };
    }
}
