// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.devtest;

import cn.blockforge.orient.framework.loader.*;
import net.minecraft.advancements.critereon.ImpossibleTrigger;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import static net.minecraft.commands.Commands.literal;

/** 全部假内容只由显式开发参数启用，验证第二个内容模块独立消费框架能力。 */
public final class Opt3cProbe implements ContentModule {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("opt3c", "probe");
    public static ProbeBlock block;
    public static BlockEntityType<ProbeBE> blockEntity;
    public static EntityType<ItemEntity> entity;
    public static MenuType<ProbeMenu> menu;
    public static Item item;
    public static ArmorItem armor;
    public static Holder<ArmorMaterial> material;
    public static RecipeSerializer<ProbeRecipe> serializer;
    public static RecipeType<CraftingRecipe> recipeType;
    public static ImpossibleTrigger trigger;
    public static SoundEvent sound;
    public static SimpleParticleType particle;
    public static final AtomicInteger joins = new AtomicInteger();
    public static final AtomicInteger leaves = new AtomicInteger();
    public static final AtomicInteger lootLoads = new AtomicInteger();
    private static ModuleHost host;

    public static synchronized void install(RuntimeHooks hooks) {
        if (host != null) return;
        host = new ModuleHost(List.of(new Opt3cProbe()));
        host.registerAll();
        host.runtime(hooks);
    }

    public static void clientModules(ClientHooks hooks) {
        if (host != null) host.client(hooks);
    }

    public static void log(String message) {
        System.out.println("[OPT3C] " + message);
    }

    @Override
    public ModuleDeclaration declaration() {
        return new ModuleDeclaration("opt3c", "opt3c", Set.of(), List.of());
    }

    @Override
    public void register(RegistrationApi api) {
        block = api.blocks().register(ID, new ProbeBlock());
        blockEntity = api.blockEntities().register(ID, ProbeBE::new, block);
        item = api.items().register(ID, new BlockItem(block, new Item.Properties()));
        menu = api.menus().register(ID, ProbeMenu::new);
        entity = api.entityTypes().register(ID,
            EntityType.Builder.<ItemEntity>of(ItemEntity::new, MobCategory.MISC).sized(.25F, .25F).noSave().build(ID.toString()));
        serializer = api.recipeSerializers().register(ID, new SimpleCraftingRecipeSerializer<>(ProbeRecipe::new));
        recipeType = api.recipeTypes().register(ID, new RecipeType<CraftingRecipe>() { });
        trigger = api.criteria().register(ID, new ImpossibleTrigger());
        material = api.armorMaterials().register(ID, new ArmorMaterial(
            Map.of(ArmorItem.Type.CHESTPLATE, 1), 0, SoundEvents.ARMOR_EQUIP_LEATHER,
            () -> Ingredient.of(Items.STRING), List.of(new ArmorMaterial.Layer(ResourceLocation.withDefaultNamespace("leather"))), 0, 0));
        armor = api.items().register(ResourceLocation.fromNamespaceAndPath("opt3c", "armor"),
            new ArmorItem(material, ArmorItem.Type.CHESTPLATE, new Item.Properties().durability(80)));
        sound = api.sounds().register(ID, SoundEvent.createVariableRangeEvent(ID));
        particle = api.particles().register(ID, new SimpleParticleType(false) { });
        Framework.lootEvents().register((key, builder, builtin, registries) -> {
            if (key.location().equals(ResourceLocation.withDefaultNamespace("blocks/cobblestone"))) {
                lootLoads.incrementAndGet();
                builder.withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1))
                    .add(LootItem.lootTableItem(Items.STICK)));
            }
        });
    }

    @Override
    public void runtime(RuntimeHooks hooks) {
        RuntimeEvents events = hooks.events();
        events.flammable(block, 300, 300);
        events.onPlayerJoin(player -> log("join=" + joins.incrementAndGet()));
        events.onPlayerLeave(player -> log("leave=" + leaves.incrementAndGet()));
        events.onServerStopping(server -> log("stopping lootLoads=" + lootLoads.get()));
        events.onCommands((dispatcher, context, selection) -> {
            if (context == null || selection == null) throw new IllegalStateException("missing command context");
            dispatcher.register(literal("opt3c").requires(source -> source.hasPermission(2))
                .then(literal("verify").executes(ctx -> {
                    String status = verify();
                    ctx.getSource().sendSuccess(() -> Component.literal(status), false);
                    return 1;
                }))
                .then(literal("menu").executes(ctx -> {
                    var player = ctx.getSource().getPlayerOrException();
                    Framework.platform().menus().open(player, new MenuProviderLike() {
                        @Override public Component title() { return Component.literal("3C 开发界面"); }
                        @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player who) {
                            return new ProbeMenu(id, inventory, BlockPos.ZERO);
                        }
                    }, BlockPos.ZERO);
                    return 1;
                })));
            log("commands selection=" + selection);
        });
        hooks.onServerStarted(() -> {
            log(verify());
            try {
                Framework.platform().registration().sounds().register(
                    ResourceLocation.fromNamespaceAndPath("opt3c", "late"), sound);
                throw new AssertionError("late registration accepted");
            } catch (IllegalStateException expected) {
                for (String field : List.of("registry=", "id=opt3c:late", "owner=opt3c", "phase=closed")) {
                    if (!expected.getMessage().contains(field)) throw new AssertionError(expected);
                }
                log("late-registration PASS " + expected.getMessage());
            }
        });
    }

    public static String verify() {
        boolean valid = BuiltInRegistries.RECIPE_SERIALIZER.get(ID) == serializer
            && BuiltInRegistries.RECIPE_TYPE.get(ID) == recipeType
            && BuiltInRegistries.TRIGGER_TYPES.get(ID) == trigger
            && BuiltInRegistries.ARMOR_MATERIAL.get(ID) == material.value()
            && material.unwrapKey().orElseThrow().location().equals(ID)
            && BuiltInRegistries.SOUND_EVENT.get(ID) == sound
            && BuiltInRegistries.PARTICLE_TYPE.get(ID) == particle
            && BuiltInRegistries.BLOCK_ENTITY_TYPE.get(ID) == blockEntity
            && BuiltInRegistries.ENTITY_TYPE.get(ID) == entity
            && BuiltInRegistries.MENU.get(ID) == menu;
        if (!valid) throw new IllegalStateException("probe registries missing");
        return "registries PASS protocol=" + Framework.platform().network().protocolVersion()
            + " managed=" + Framework.lootEvents().isPlatformManaged() + " lootLoads=" + lootLoads.get();
    }

    @Override
    public void client(ClientHooks hooks) {
        try {
            Class.forName("cn.blockforge.generated.orientshotpotchronicles.devtest.Opt3cClientProbe")
                .getMethod("install").invoke(null);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException(error);
        }
    }

    public static final class ProbeBlock extends Block implements EntityBlock {
        public ProbeBlock() { super(Properties.of().strength(1)); }
        @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new ProbeBE(pos, state); }
    }

    public static final class ProbeBE extends BlockEntity {
        public ProbeBE(BlockPos pos, BlockState state) { super(blockEntity, pos, state); }
    }

    public static final class ProbeMenu extends AbstractContainerMenu {
        public ProbeMenu(int id, Inventory inventory, BlockPos pos) { super(menu, id); }
        @Override public ItemStack quickMoveStack(Player player, int slot) { return ItemStack.EMPTY; }
        @Override public boolean stillValid(Player player) { return true; }
    }

    public static final class ProbeRecipe extends CustomRecipe {
        public ProbeRecipe(CraftingBookCategory category) { super(category); }
        @Override public boolean matches(CraftingInput input, Level level) { return input.size() == 1 && input.getItem(0).is(Items.DIRT); }
        @Override public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) { return new ItemStack(item); }
        @Override public boolean canCraftInDimensions(int width, int height) { return width * height >= 1; }
        @Override public RecipeSerializer<?> getSerializer() { return serializer; }
        @Override public RecipeType<CraftingRecipe> getType() { return recipeType; }
    }
}
