// 作者：mryt233
package cn.blockforge.orient.framework.loader;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.Component;
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
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertThrows;

class RegistrationApiCompatibilityTest {
    @Test
    void legacyImplementationThrowsClearMessageForAddedDomains() {
        RegistrationApi legacy = new LegacyRegistrationApi();

        for (Supplier<?> domain : List.<Supplier<?>>of(
            legacy::recipeSerializers, legacy::recipeTypes,
            legacy::criteria, legacy::sounds, legacy::particles)) {
            assertThrows(UnsupportedOperationException.class, domain::get);
        }
    }

    private static final class LegacyRegistrationApi implements RegistrationApi {
        @Override public ItemRegistrar items() { return new ItemRegistrar() {
            @Override public <T extends Item> T register(Identifier id, T item) { return null; }
        }; }
        @Override public BlockRegistrar blocks() { return new BlockRegistrar() {
            @Override public <T extends Block> T register(Identifier id, T block) { return null; }
        }; }
        @Override public BlockEntityRegistrar blockEntities() { return new BlockEntityRegistrar() {
            @Override public <T extends BlockEntity> BlockEntityType<T> register(Identifier id, BlockEntityFactory<T> factory, Block... blocks) { return null; }
        }; }
        @Override public MenuRegistrar menus() { return new MenuRegistrar() {
            @Override public <T extends AbstractContainerMenu> MenuType<T> register(Identifier id, MenuFactory<T> factory) { return null; }
        }; }
        @Override public ComponentRegistrar components() { return new ComponentRegistrar() {
            @Override public <T> DataComponentType<T> register(Identifier id, DataComponentType<T> type) { return null; }
        }; }
        @Override public CreativeTabRegistrar creativeTabs() { return (id, title, icon, content) -> null; }
        @Override public EntityRegistrar entityTypes() { return new EntityRegistrar() {
            @Override public <T extends Entity> EntityType<T> register(Identifier id, EntityType<T> type) { return null; }
        }; }
    }
}
