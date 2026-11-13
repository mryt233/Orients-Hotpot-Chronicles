package cn.blockforge.orient.framework.platform.fabric;

// mryt233 改（2026-11-11）：限定类名改用 import，逻辑不变。
// mryt233 改（2026-11-10）：第三批 3C 补充平台能力。

import cn.blockforge.orient.framework.compat.ItemDataKey;
import cn.blockforge.orient.framework.compat.mixin.CriteriaTriggersAccessor;
import cn.blockforge.orient.framework.loader.RegistrationApi;
import cn.blockforge.orient.framework.pure.RegistrationGuard;
import cn.blockforge.orient.framework.pure.RegistrationWindow;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.Objects;

/**
 * Fabric 通用注册实现：注册表不冻结，立即注册；重复 id 预检失败即停。
 * 属于框架 Runtime，不含任何内容业务。作者：mryt233
 */
public final class FabricRegistrationApi implements RegistrationApi {
    private final RegistrationWindow window = new RegistrationWindow();

    /** 首个引擎启动事件在全部内容初始化之后关窗。 */
    public void closeRegistration() {
        window.closeAll();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <T> T put(Registry registry, ResourceLocation id, Object value) {
        String owner = RegistrationGuard.owner(id.getNamespace());
        String key = registry.key().location().toString();
        window.claim(key, id.toString(), owner, registry.containsKey(id));
        try {
            return (T) Registry.register(registry, id, Objects.requireNonNull(value));
        } catch (RuntimeException error) {
            throw RegistrationGuard.failure(key, id.toString(), owner, "register", error);
        }
    }

    @Override
    public ItemRegistrar items() {
        return new ItemRegistrar() {
            @Override
            public <T extends Item> T register(ResourceLocation id, T item) {
                return put(BuiltInRegistries.ITEM, id, item);
            }
        };
    }

    @Override
    public BlockRegistrar blocks() {
        return new BlockRegistrar() {
            @Override
            public <T extends Block> T register(ResourceLocation id, T block) {
                return put(BuiltInRegistries.BLOCK, id, block);
            }
        };
    }

    @Override
    public BlockEntityRegistrar blockEntities() {
        return new BlockEntityRegistrar() {
            @Override
            public <T extends BlockEntity> BlockEntityType<T> register(ResourceLocation id,
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
            public <T extends AbstractContainerMenu> MenuType<T> register(ResourceLocation id, MenuFactory<T> factory) {
                return put(BuiltInRegistries.MENU, id,
                    new ExtendedScreenHandlerType<>((syncId, inventory, buffer) ->
                        factory.create(syncId, inventory, buffer.readBlockPos())));
            }
        };
    }

    @Override
    public ComponentRegistrar components() {
        return new ComponentRegistrar() {
            @Override
            public <T> ItemDataKey<T> register(ResourceLocation id, ItemDataKey<T> type) {
                String owner = RegistrationGuard.owner(id.getNamespace());
                window.claim("orient:item_data", id.toString(), owner, false);
                return Objects.requireNonNull(type);
            }
        };
    }

    @Override
    public CreativeTabRegistrar creativeTabs() {
        return (id, title, icon, content) -> put(BuiltInRegistries.CREATIVE_MODE_TAB, id,
            FabricItemGroup.builder().title(title).icon(icon)
                .displayItems((params, output) -> content.fill(params, output)).build());
    }

    @Override
    public EntityRegistrar entityTypes() {
        return new EntityRegistrar() {
            @Override
            public <T extends Entity> EntityType<T> register(ResourceLocation id, EntityType<T> type) {
                return put(BuiltInRegistries.ENTITY_TYPE, id, type);
            }
        };
    }

    @Override
    public RecipeSerializerRegistrar recipeSerializers() {
        return new RecipeSerializerRegistrar() {
            @Override
            public <T extends RecipeSerializer<?>> T register(ResourceLocation id, T value) {
                return put(BuiltInRegistries.RECIPE_SERIALIZER, id, value);
            }
        };
    }

    @Override
    public RecipeTypeRegistrar recipeTypes() {
        return new RecipeTypeRegistrar() {
            @Override
            public <T extends RecipeType<?>> T register(ResourceLocation id, T value) {
                return put(BuiltInRegistries.RECIPE_TYPE, id, value);
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
                return put(BuiltInRegistries.SOUND_EVENT, id, value);
            }
        };
    }

    @Override
    public ParticleRegistrar particles() {
        return new ParticleRegistrar() {
            @Override
            public <T extends ParticleType<?>> T register(ResourceLocation id, T value) {
                return put(BuiltInRegistries.PARTICLE_TYPE, id, value);
            }
        };
    }
}
