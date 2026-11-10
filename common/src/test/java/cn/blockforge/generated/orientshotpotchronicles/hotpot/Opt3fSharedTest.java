// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import cn.blockforge.generated.orientshotpotchronicles.compat.HotPotCompat;
import cn.blockforge.generated.orientshotpotchronicles.compat.HotPotRecipeViewerData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Proxy;
import java.util.Locale;
import static org.junit.jupiter.api.Assertions.*;

class Opt3fSharedTest {
    @BeforeAll
    static void bootstrap() { Opt3AttributesTest.bootstrapMinecraft(); }

    private static final class Menu extends PlayerInventoryMenu {
        final SimpleContainer contents = new SimpleContainer(1);
        final Inventory inventory = new Inventory(null);
        int emptied;
        Menu() {
            super(MenuType.GENERIC_9x1, 1);
            addSlot(new Slot(contents, 0, 10, 10));
            addPlayerInventory(inventory, 8, 84, 142);
        }
        @Override protected int containerSlots() { return 1; }
        @Override protected boolean acceptsQuickMove(ItemStack stack) { return stack.is(Items.COAL); }
        @Override protected void onQuickEmptied(Slot slot, boolean fromContainer) { if (fromContainer) emptied++; }
        @Override public boolean stillValid(Player player) { return true; }
    }

    @Test void inventoryCoordinatesAndIndicesStayInVanillaOrder() {
        var menu = new Menu();
        assertEquals(37, menu.slots.size());
        for (int i = 0; i < 36; i++) {
            var slot = menu.slots.get(i + 1);
            assertEquals(i < 27 ? i + 9 : i - 27, slot.getContainerSlot());
            assertEquals(8 + i % 9 * 18, slot.x);
            assertEquals(i < 27 ? 84 + i / 9 * 18 : 142, slot.y);
        }
    }
    @Test void invalidQuickMovesDoNotTouchItems() {
        var menu = new Menu(); menu.contents.setItem(0, new ItemStack(Items.COAL, 3));
        for (int i : new int[]{-1, 37, Integer.MAX_VALUE}) assertTrue(menu.quickMoveStack(null, i).isEmpty());
        assertEquals(3, menu.contents.getItem(0).getCount());
    }
    @Test void quickMovePreservesBothDirectionsAndEmptyNotification() {
        var menu = new Menu(); menu.inventory.setItem(9, new ItemStack(Items.COAL, 3));
        assertEquals(3, menu.quickMoveStack(null, 1).getCount());
        assertEquals(3, menu.contents.getItem(0).getCount());
        assertEquals(3, menu.quickMoveStack(null, 0).getCount());
        assertEquals(3, menu.inventory.getItem(8).getCount());
        assertEquals(1, menu.emptied);
    }
    @Test void invalidIngredientIsRejectedWithoutLoss() {
        var menu = new Menu(); menu.inventory.setItem(9, new ItemStack(Items.STONE, 3));
        assertTrue(menu.quickMoveStack(null, 1).isEmpty());
        assertEquals(3, menu.inventory.getItem(9).getCount());
        assertTrue(menu.contents.isEmpty());
    }
    @Test void fullInventoryDoesNotSwallowContainerContents() {
        var menu = new Menu();
        for (int i = 0; i < 36; i++) menu.inventory.setItem(i, new ItemStack(Items.STONE, 64));
        menu.contents.setItem(0, new ItemStack(Items.COAL, 3));
        assertTrue(menu.quickMoveStack(null, 0).isEmpty());
        assertEquals(3, menu.contents.getItem(0).getCount());
        assertEquals(0, menu.emptied);
    }
    @Test void dataBindingKeepsClientDefaultsAndServerReadOnlyValues() {
        var menu = new Menu(); int[] defaults = {-1, 0, -1};
        var client = menu.bindData(defaults, null);
        assertEquals(-1, client[0].get()); client[1].set(71); assertEquals(71, defaults[1]);
        var server = menu.bindData(defaults, index -> 100 + index);
        server[1].set(5); assertEquals(101, server[1].get()); assertEquals(71, defaults[1]);
    }
    private static final class Entity extends SyncedBlockEntity {
        int value = 23;
        Entity() { super(BlockEntityType.CHEST, BlockPos.ZERO, Blocks.CHEST.defaultBlockState()); }
        @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.saveAdditional(tag, registries); tag.putInt("fixture", value);
        }
        @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.loadAdditional(tag, registries); value = tag.getInt("fixture");
        }
    }
    @Test void updateTagAndPacketPreserveSubclassDataAndReadBack() {
        var registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        var entity = new Entity(); var expected = entity.saveWithoutMetadata(registries);
        assertEquals(expected, entity.getUpdateTag(registries));
        var copy = new Entity(); copy.value = 0; copy.loadWithComponents(expected, registries);
        assertEquals(23, copy.value); assertEquals(expected, copy.getUpdateTag(registries));
    }
    @Test void originalPublicSyncAndTopMethodsRemainAvailable() throws Exception {
        for (Class<?> type : new Class<?>[]{HotPotBlockEntity.class, CuttingBoardBlockEntity.class,
                PlateHolderBlockEntity.class, SauceStationBlockEntity.class}) {
            assertNotNull(type.getDeclaredMethod("getUpdatePacket"));
            assertNotNull(type.getDeclaredMethod("getUpdateTag", HolderLookup.Provider.class));
        }
        for (Class<?> type : new Class<?>[]{StoveTopBlock.class, SauceStationTopBlock.class}) {
            assertNotNull(type.getDeclaredMethod("aimedPart", net.minecraft.world.level.block.state.BlockState.class,
                BlockGetter.class, BlockPos.class, Player.class));
        }
    }
    @Test void registryLookupDistinguishesMissingIdFromRegisteredAir() {
        assertNull(HotPotCompat.itemOf("opt3f_missing", "missing"));
        assertSame(Items.AIR, HotPotCompat.itemOf("minecraft", "air"));
        assertSame(Items.BEEF, HotPotCompat.itemOf("minecraft", "beef"));
    }
    @Test void viewerSecondsStayLocaleIndependent() {
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.FRANCE);
            assertEquals("0.0", HotPotRecipeViewerData.seconds(0));
            assertEquals("1.3", HotPotRecipeViewerData.seconds(25));
            assertEquals("20.0", HotPotRecipeViewerData.seconds(400));
        } finally { Locale.setDefault(original); }
    }
    @Test void viewerTextKeepsTranslationKeyAndStringArguments() {
        var recipe = new HotPotRecipeViewerData.CookingEntry(null, ItemStack.EMPTY, ItemStack.EMPTY,
            25, 0, new ItemStack(Items.BEEF), 20);
        var text = (net.minecraft.network.chat.contents.TranslatableContents) HotPotRecipeViewerData.cookingTime(recipe).getContents();
        assertEquals("text.orients_hotpot_chronicles.recipe.cooking.time", text.getKey());
        assertArrayEquals(new Object[]{"1.3", "25"}, text.getArgs());
        var overcook = (net.minecraft.network.chat.contents.TranslatableContents) HotPotRecipeViewerData.overcookHint(recipe).getContents();
        assertEquals("0.0", overcook.getArgs()[0]);
    }
}
