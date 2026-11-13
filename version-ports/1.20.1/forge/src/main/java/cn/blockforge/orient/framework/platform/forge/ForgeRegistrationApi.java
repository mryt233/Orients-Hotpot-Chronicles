package cn.blockforge.orient.framework.platform.forge;

// mryt233 改（2026-11-11）：限定类名改用 import，逻辑不变。
// mryt233 改（2026-11-10）：第三批 3C 补充平台能力。

import cn.blockforge.orient.framework.compat.ItemDataKey;
import cn.blockforge.orient.framework.compat.mixin.CriteriaTriggersAccessor;
import cn.blockforge.orient.framework.loader.RegistrationApi;
import cn.blockforge.orient.framework.pure.RegistrationGuard;
import cn.blockforge.orient.framework.pure.RegistrationWindow;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.network.IContainerFactory;
import net.minecraftforge.registries.RegisterEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Forge 通用注册实现：对象立即构造、按注册表排队，在各自 RegisterEvent 窗口内落地。
 * 重复 id 预检失败即停（与 Fabric 行为统一）。属于框架 Runtime，不含内容业务。作者：mryt233
 *
 * <p>mryt233 改（2026-11-01，前置检查 F1）：某个注册表的 RegisterEvent 过了以后再登记，以前会进队列但永远不注册、
 * 也不报错；现在每个注册表落地后关窗，之后再登记直接抛出带 id 的异常（与「注册失败即停」一致）。</p>
 */
public final class ForgeRegistrationApi implements RegistrationApi {
    private record Entry<T>(ResourceLocation id, T value, String owner) {
    }

    private final RegistrationWindow window = new RegistrationWindow();

    private void requireOpen(ResourceKey<?> registry, ResourceLocation id) {
        Registry<?> target = BuiltInRegistries.REGISTRY.get(registry.location());
        window.claim(registry.location().toString(), id.toString(),
            RegistrationGuard.owner(id.getNamespace()), target != null && target.containsKey(id));
    }

    private static <T> T requireValue(ResourceKey<?> registry, ResourceLocation id, T value) {
        if (value == null) {
            throw RegistrationGuard.failure(registry.location().toString(), id.toString(),
                RegistrationGuard.owner(id.getNamespace()), "declare", new NullPointerException("null entry"));
        }
        return value;
    }

    private <T> void flushAndClose(RegisterEvent event, ResourceKey<?> key, Registry<T> registry, List<Entry<T>> queue) {
        flush(event, registry, queue);
        window.close(key.location().toString());
    }

    void closeNonRegistryWindows() {
        window.close("minecraft:trigger_type");
        window.close("orient:item_data");
    }

    private final List<Entry<Item>> items = new ArrayList<>();
    private final List<Entry<Block>> blocks = new ArrayList<>();
    private final List<Entry<BlockEntityType<?>>> blockEntities = new ArrayList<>();
    private final List<Entry<MenuType<?>>> menus = new ArrayList<>();
    private final List<Entry<CreativeModeTab>> tabs = new ArrayList<>();
    private final List<Entry<EntityType<?>>> entities = new ArrayList<>();
    private final List<Entry<RecipeSerializer<?>>> recipeSerializers = new ArrayList<>();
    private final List<Entry<RecipeType<?>>> recipeTypes = new ArrayList<>();
    private final List<Entry<SoundEvent>> sounds = new ArrayList<>();
    private final List<Entry<ParticleType<?>>> particles = new ArrayList<>();

    void queueRecipeSerializer(ResourceLocation id, RecipeSerializer<?> serializer) {
        recipeSerializers().register(id, serializer);
    }

    /**
     * mryt233 改（2026-11-02）：改用 {@code RegisterEvent.register}，不再直接调原版 {@code Registry.register}。
     * Forge 47.0.0 在 RegisterEvent 里仍锁着原版注册表，直接写会抛「Can not register to a locked registry」；
     * 47.4.x 才放宽。用事件自带的注册方法后，最低能支持到 Forge 47.0.0。
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> void flush(RegisterEvent event, Registry<T> registry, List<Entry<T>> queue) {
        for (Entry<T> entry : queue) {
            try {
                RegistrationGuard.requireAbsent(registry.containsKey(entry.id()),
                    registry.key().location().toString(), entry.id().toString(), entry.owner());
                T value = entry.value();
                event.register((ResourceKey) registry.key(), entry.id(), () -> value);
            } catch (RuntimeException error) {
                throw RegistrationGuard.failure(registry.key().location().toString(),
                    entry.id().toString(), entry.owner(), "register", error);
            }
        }
        queue.clear();
    }

    void onRegisterEvent(RegisterEvent event) {
        ResourceKey<?> key = event.getRegistryKey();
        if (key.equals(Registries.ITEM)) {
            flushAndClose(event, key, BuiltInRegistries.ITEM, items);
        } else if (key.equals(Registries.BLOCK)) {
            flushAndClose(event, key, BuiltInRegistries.BLOCK, blocks);
        } else if (key.equals(Registries.BLOCK_ENTITY_TYPE)) {
            flushAndClose(event, key, BuiltInRegistries.BLOCK_ENTITY_TYPE, blockEntities);
        } else if (key.equals(Registries.MENU)) {
            flushAndClose(event, key, BuiltInRegistries.MENU, menus);
        } else if (key.equals(Registries.CREATIVE_MODE_TAB)) {
            flushAndClose(event, key, BuiltInRegistries.CREATIVE_MODE_TAB, tabs);
        } else if (key.equals(Registries.ENTITY_TYPE)) {
            flushAndClose(event, key, BuiltInRegistries.ENTITY_TYPE, entities);
        } else if (key.equals(Registries.RECIPE_SERIALIZER)) {
            flushAndClose(event, key, BuiltInRegistries.RECIPE_SERIALIZER, recipeSerializers);
        } else if (key.equals(Registries.RECIPE_TYPE)) {
            flushAndClose(event, key, BuiltInRegistries.RECIPE_TYPE, recipeTypes);
        } else if (key.equals(Registries.SOUND_EVENT)) {
            flushAndClose(event, key, BuiltInRegistries.SOUND_EVENT, sounds);
        } else if (key.equals(Registries.PARTICLE_TYPE)) {
            flushAndClose(event, key, BuiltInRegistries.PARTICLE_TYPE, particles);
        }
    }

    @Override
    public ItemRegistrar items() {
        return new ItemRegistrar() {
            @Override
            public <T extends Item> T register(ResourceLocation id, T item) {
                requireOpen(Registries.ITEM, id);
                T checked = requireValue(Registries.ITEM, id, item);
                items.add(new Entry<>(id, checked, RegistrationGuard.owner(id.getNamespace())));
                return checked;
            }
        };
    }

    @Override
    public BlockRegistrar blocks() {
        return new BlockRegistrar() {
            @Override
            public <T extends Block> T register(ResourceLocation id, T block) {
                requireOpen(Registries.BLOCK, id);
                T checked = requireValue(Registries.BLOCK, id, block);
                blocks.add(new Entry<>(id, checked, RegistrationGuard.owner(id.getNamespace())));
                return checked;
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
                blockEntities.add(new Entry<>(id, type, RegistrationGuard.owner(id.getNamespace())));
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
                menus.add(new Entry<>(id, type, RegistrationGuard.owner(id.getNamespace())));
                return type;
            }
        };
    }

    @Override
    public ComponentRegistrar components() {
        return new ComponentRegistrar() {
            @Override
            public <T> ItemDataKey<T> register(ResourceLocation id, ItemDataKey<T> type) {
                window.claim("orient:item_data", id.toString(), RegistrationGuard.owner(id.getNamespace()), false);
                return Objects.requireNonNull(type);
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
            tabs.add(new Entry<>(id, tab, RegistrationGuard.owner(id.getNamespace())));
            return tab;
        };
    }

    @Override
    public EntityRegistrar entityTypes() {
        return new EntityRegistrar() {
            @Override
            public <T extends Entity> EntityType<T> register(ResourceLocation id, EntityType<T> type) {
                requireOpen(Registries.ENTITY_TYPE, id);
                EntityType<T> checked = requireValue(Registries.ENTITY_TYPE, id, type);
                entities.add(new Entry<>(id, checked, RegistrationGuard.owner(id.getNamespace())));
                return checked;
            }
        };
    }

    @Override
    public RecipeSerializerRegistrar recipeSerializers() {
        return new RecipeSerializerRegistrar() {
            @Override
            public <T extends RecipeSerializer<?>> T register(ResourceLocation id, T value) {
                requireOpen(Registries.RECIPE_SERIALIZER, id);
                T checked = requireValue(Registries.RECIPE_SERIALIZER, id, value);
                recipeSerializers.add(new Entry<>(id, checked, RegistrationGuard.owner(id.getNamespace())));
                return checked;
            }
        };
    }

    @Override
    public RecipeTypeRegistrar recipeTypes() {
        return new RecipeTypeRegistrar() {
            @Override
            public <T extends RecipeType<?>> T register(ResourceLocation id, T value) {
                requireOpen(Registries.RECIPE_TYPE, id);
                T checked = requireValue(Registries.RECIPE_TYPE, id, value);
                recipeTypes.add(new Entry<>(id, checked, RegistrationGuard.owner(id.getNamespace())));
                return checked;
            }
        };
    }

    @Override
    public CriterionRegistrar criteria() {
        return new CriterionRegistrar() {
            @Override
            public <T extends CriterionTrigger<?>> T register(ResourceLocation id, T value) {
                String owner = RegistrationGuard.owner(id.getNamespace());
                String key = "minecraft:trigger_type";
                window.claim(key, id.toString(), owner, CriteriaTriggers.getCriterion(id) != null);
                try {
                    return CriteriaTriggersAccessor.register(Objects.requireNonNull(value));
                } catch (RuntimeException error) {
                    throw RegistrationGuard.failure(key, id.toString(), owner, "register", error);
                }
            }
        };
    }

    @Override
    public SoundRegistrar sounds() {
        return new SoundRegistrar() {
            @Override
            public SoundEvent register(ResourceLocation id, SoundEvent value) {
                requireOpen(Registries.SOUND_EVENT, id);
                SoundEvent checked = requireValue(Registries.SOUND_EVENT, id, value);
                sounds.add(new Entry<>(id, checked, RegistrationGuard.owner(id.getNamespace())));
                return checked;
            }
        };
    }

    @Override
    public ParticleRegistrar particles() {
        return new ParticleRegistrar() {
            @Override
            public <T extends ParticleType<?>> T register(ResourceLocation id, T value) {
                requireOpen(Registries.PARTICLE_TYPE, id);
                T checked = requireValue(Registries.PARTICLE_TYPE, id, value);
                particles.add(new Entry<>(id, checked, RegistrationGuard.owner(id.getNamespace())));
                return checked;
            }
        };
    }
}
