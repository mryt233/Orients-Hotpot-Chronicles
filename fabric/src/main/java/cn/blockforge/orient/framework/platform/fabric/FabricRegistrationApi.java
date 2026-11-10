package cn.blockforge.orient.framework.platform.fabric;

// mryt233 改（2026-11-10）：第三批 3C 补充平台能力。

import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import cn.blockforge.orient.framework.pure.RegistrationWindow;

import cn.blockforge.orient.framework.loader.RegistrationApi;
import cn.blockforge.orient.framework.pure.RegistrationGuard;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
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
            return (T) Registry.register(registry, id, java.util.Objects.requireNonNull(value));
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
                    new ExtendedScreenHandlerType<>((syncId, inventory, pos) -> factory.create(syncId, inventory, pos),
                        BlockPos.STREAM_CODEC));
            }
        };
    }

    @Override
    public ComponentRegistrar components() {
        return new ComponentRegistrar() {
            @Override
            public <T> DataComponentType<T> register(ResourceLocation id, DataComponentType<T> type) {
                return put(BuiltInRegistries.DATA_COMPONENT_TYPE, id, type);
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
            public <T extends net.minecraft.world.item.crafting.RecipeSerializer<?>> T register(ResourceLocation id, T value) {
                return put(BuiltInRegistries.RECIPE_SERIALIZER, id, value);
            }
        };
    }

    @Override
    public RecipeTypeRegistrar recipeTypes() {
        return new RecipeTypeRegistrar() {
            @Override
            public <T extends net.minecraft.world.item.crafting.RecipeType<?>> T register(ResourceLocation id, T value) {
                return put(BuiltInRegistries.RECIPE_TYPE, id, value);
            }
        };
    }

    @Override
    public CriterionRegistrar criteria() {
        return new CriterionRegistrar() {
            @Override
            public <T extends net.minecraft.advancements.CriterionTrigger<?>> T register(ResourceLocation id, T value) {
                return put(BuiltInRegistries.TRIGGER_TYPES, id, value);
            }
        };
    }

    @Override
    public ArmorMaterialRegistrar armorMaterials() {
        return new ArmorMaterialRegistrar() {
            @Override
            public net.minecraft.core.Holder<net.minecraft.world.item.ArmorMaterial> register(ResourceLocation id, net.minecraft.world.item.ArmorMaterial value) {
                String owner = RegistrationGuard.owner(id.getNamespace());
                String key = BuiltInRegistries.ARMOR_MATERIAL.key().location().toString();
                window.claim(key, id.toString(), owner, BuiltInRegistries.ARMOR_MATERIAL.containsKey(id));
                try {
                    return Registry.registerForHolder(BuiltInRegistries.ARMOR_MATERIAL, id, value);
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
            public net.minecraft.sounds.SoundEvent register(ResourceLocation id, net.minecraft.sounds.SoundEvent value) {
                return put(BuiltInRegistries.SOUND_EVENT, id, value);
            }
        };
    }

    @Override
    public ParticleRegistrar particles() {
        return new ParticleRegistrar() {
            @Override
            public <T extends net.minecraft.core.particles.ParticleType<?>> T register(ResourceLocation id, T value) {
                return put(BuiltInRegistries.PARTICLE_TYPE, id, value);
            }
        };
    }
}
