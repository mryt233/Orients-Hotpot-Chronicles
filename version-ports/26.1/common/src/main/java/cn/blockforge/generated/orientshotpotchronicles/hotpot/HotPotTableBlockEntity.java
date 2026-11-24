// 作者：mryt233；mryt233 改（2026-11-16）：适配 26.1 ValueInput/ValueOutput、DataComponentGetter 与 Player API。
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter.BeIo;
import cn.blockforge.orient.framework.game.MaterialRenderHost;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

/**
 * 统一火锅桌方块实体：存储桌面、底座（及旧高桌迁移中圈）材质 ID 与防刷标识，
 * 支持物品组件打包、Ctrl+中键去重与客户端烘焙模型刷新。作者：mryt233
 */
public class HotPotTableBlockEntity extends SyncedBlockEntity implements MaterialRenderHost {
    /** 由客户端入口安装的区块重绘回调（common 主代码不引用客户端 API）。 */
    public static BiConsumer<Level, BlockPos> clientRefresh = (level, pos) -> {};

    private static final String[] COMPONENT_NBT_KEYS = {
        "Top", "Base", "LegacyRing", "TopLegacy", "BaseLegacy"
    };

    private TableMaterialData materials = TableMaterialData.EMPTY;

    public HotPotTableBlockEntity(BlockPos pos, BlockState state) {
        super(HotPotContent.HOTPOT_TABLE_BE, pos, state);
    }

    public TableMaterialData materials() {
        return materials;
    }

    public void setMaterials(TableMaterialData next) {
        this.materials = next == null ? TableMaterialData.EMPTY : next;
        sync();
    }

    @Override
    public @Nullable Object materialRenderData() {
        return materials;
    }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_ALL);
        }
    }

    public BlockState topBlockState() {
        return TableMaterials.resolveDisplayState(materials.top(), Blocks.OAK_PLANKS.defaultBlockState());
    }

    public BlockState baseBlockState() {
        return TableMaterials.resolveDisplayState(materials.base(), Blocks.BRICKS.defaultBlockState());
    }

    public boolean applyMaterial(Player player, InteractionHand hand, ItemStack stack,
                                 TableMaterials.Slot slot, Block targetBlock) {
        String newId = TableMaterials.encodeBlock(targetBlock);
        if (newId.isEmpty()) {
            return false;
        }
        String currentRaw = slot == TableMaterials.Slot.TOP ? materials.top() : materials.base();
        boolean currentLegacy = slot == TableMaterials.Slot.TOP ? materials.topLegacy() : materials.baseLegacy();
        String effectiveCurrent = currentRaw.isEmpty()
            ? (slot == TableMaterials.Slot.TOP ? TableMaterials.DEFAULT_TOP_ID : TableMaterials.DEFAULT_BASE_ID)
            : currentRaw;
        boolean hasLegacyRingOverlap = slot == TableMaterials.Slot.BASE
            && getBlockState().getBlock() instanceof TallHotPotTableBlock
            && !materials.legacyRing().isEmpty();
        if (effectiveCurrent.equals(newId) && !hasLegacyRingOverlap) {
            if (level != null && !level.isClientSide()) {
                player.sendOverlayMessage(Component.translatableWithFallback(
                    HotPotBlockEntity.key("msg.table_same_material"), "这一部位已经是这种材质了"));
            }
            return true;
        }
        if (level != null && !level.isClientSide()) {
            boolean creative = player.hasInfiniteMaterials();
            if (!creative) {
                stack.shrink(1);
                if (!currentRaw.isEmpty() && !currentLegacy) {
                    Block oldBlock = TableMaterials.resolveBlock(currentRaw);
                    if (oldBlock != null && oldBlock.asItem() != Items.AIR) {
                        ItemStack refund = new ItemStack(oldBlock.asItem());
                        if (!player.getInventory().add(refund)) {
                            player.drop(refund, false);
                        }
                    }
                }
            }
            materials = slot == TableMaterials.Slot.TOP
                ? materials.withTop(newId, false)
                : materials.withBase(newId, false);
            sync();
            level.playSound(null, worldPosition, targetBlock.defaultBlockState().getSoundType().getPlaceSound(),
                SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        return true;
    }

    public boolean tryPopMaterial(Player player, TableMaterials.Slot slot) {
        String currentRaw = slot == TableMaterials.Slot.TOP ? materials.top() : materials.base();
        boolean currentLegacy = slot == TableMaterials.Slot.TOP ? materials.topLegacy() : materials.baseLegacy();
        if (currentRaw.isEmpty()) {
            return false;
        }
        if (currentLegacy) {
            if (level != null && !level.isClientSide()) {
                player.sendOverlayMessage(Component.translatableWithFallback(
                    HotPotBlockEntity.key("msg.table_legacy_locked"),
                    "旧存档预设材质不可拆出，可直接拿新方块覆盖"));
            }
            return true;
        }
        if (level != null && !level.isClientSide()) {
            if (!player.hasInfiniteMaterials()) {
                Block oldBlock = TableMaterials.resolveBlock(currentRaw);
                if (oldBlock != null && oldBlock.asItem() != Items.AIR) {
                    ItemStack refund = new ItemStack(oldBlock.asItem());
                    if (!player.getInventory().add(refund)) {
                        player.drop(refund, false);
                    }
                }
            }
            materials = slot == TableMaterials.Slot.TOP
                ? materials.withTop("", false)
                : materials.withBase("", false);
            sync();
            level.playSound(null, worldPosition, SoundEvents.WOOD_BREAK, SoundSource.BLOCKS, 0.75F, 1.05F);
        }
        return true;
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        BeIo.write(out, writer -> {
            writer.putString("Top", materials.top());
            writer.putString("Base", materials.base());
            writer.putString("LegacyRing", materials.legacyRing());
            writer.putBoolean("TopLegacy", materials.topLegacy());
            writer.putBoolean("BaseLegacy", materials.baseLegacy());
        });
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        BeIo.read(in, reader -> {
            materials = new TableMaterialData(
                reader.getString("Top", ""),
                reader.getString("Base", ""),
                reader.getString("LegacyRing", ""),
                reader.getBoolean("TopLegacy", false),
                reader.getBoolean("BaseLegacy", false)
            );
        });
        if (level != null && level.isClientSide()) {
            clientRefresh.accept(level, worldPosition);
        }
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        if (materials.hasAny()) {
            builder.set(HotPotContent.TABLE_MATERIALS, materials);
        }
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        TableMaterialData data = components.get(HotPotContent.TABLE_MATERIALS);
        if (data != null) {
            this.materials = data;
        }
    }

    @Override
    public void removeComponentsFromTag(ValueOutput out) {
        super.removeComponentsFromTag(out);
        for (String key : COMPONENT_NBT_KEYS) {
            out.discard(key);
        }
    }
}
