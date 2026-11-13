// mryt233 改：26.2 原版 API；保留 4F 已化开库存 kind_id 持久化与 3G 存读档适配。
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter.BeIo;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter.StoreReader;
import java.util.Arrays;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * 蘸料台的存货：台上 9 个小碗，每个碗对应补货界面的一个原料格。
 * 作者：mryt233
 */
public class SauceStationBlockEntity extends SyncedBlockEntity {
    public static final int BOWLS = 9;
    public static final int PORTIONS_PER_RAW = 4;
    public static final int MAX_RAW = 16;

    private final SimpleContainer raw = new SimpleContainer(BOWLS) {
        @Override
        public void setChanged() {
            if (!loading) {
                SauceStationBlockEntity.this.changed();
            }
        }

        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return accepts(slot, stack);
        }

        @Override
        public int getMaxStackSize() {
            return MAX_RAW;
        }
    };
    private final int[] portions = new int[BOWLS];
    /** mryt233：每碗持久化 ID；空字符串为空，未知 ID 保留。 */
    private final String[] kindIds = new String[BOWLS];
    /** 识别空槽被明确补货，避免无关槽变化抹掉未知 ID。 */
    private final boolean[] rawWasEmpty = new boolean[BOWLS];
    /** mryt233：记录各槽上次见到的原料，以区分玩家换料与配方重载导致的映射变化。 */
    private final ItemStack[] lastRaw = new ItemStack[BOWLS];
    private boolean loading;

    public SauceStationBlockEntity(BlockPos worldPosition, BlockState state) {
        this(HotPotContent.SAUCE_STATION_BE, worldPosition, state);
    }

    SauceStationBlockEntity(BlockEntityType<?> type, BlockPos worldPosition, BlockState state) {
        super(type, worldPosition, state);
        Arrays.fill(kindIds, "");
        Arrays.fill(rawWasEmpty, true);
        Arrays.fill(lastRaw, ItemStack.EMPTY);
    }

    public SimpleContainer rawInventory() {
        return raw;
    }

    public int portions(int i) {
        return portions[i];
    }

    public Seasoning kind(int i) {
        SeasoningDef def = kindDef(i);
        return def == null ? null : def.legacy();
    }

    public SeasoningDef kindDef(int i) {
        ItemStack stack = raw.getItem(i);
        SeasoningDef saved = kindIds[i].isEmpty() ? null : SauceIds.def(kindIds[i]);
        if (!kindIds[i].isEmpty() && saved == null && available(i) > 0) {
            return null;
        }
        if (portions[i] > 0 && saved != null) {
            if (stack.isEmpty() || sameRawKind(lastRaw[i], stack)) {
                return saved;
            }
            SeasoningDef current = Seasonings.ofRaw(stack);
            return current != null ? current : saved;
        }
        if (!stack.isEmpty()) {
            return Seasonings.ofRaw(stack);
        }
        return null;
    }

    /** 会话索引：-1 空，-2 未知，不将未知回写为空。 */
    public int kindIndex(int i) {
        SeasoningDef def = kindDef(i);
        return slotIndex(kindIds[i], available(i), def == null ? -1 : Seasonings.indexOf(def),
            !kindIds[i].isEmpty() && SauceIds.def(kindIds[i]) == null);
    }

    static int slotIndex(String id, int available, int knownIndex, boolean unknown) {
        if (knownIndex >= 0) {
            return knownIndex;
        }
        return available > 0 && !id.isEmpty() && unknown ? -2 : -1;
    }

    public boolean accepts(int i, ItemStack stack) {
        SeasoningDef s = Seasonings.ofRaw(stack);
        SeasoningDef k = kindDef(i);
        return s != null && kindIndex(i) != -2 && (k == null || k == s);
    }

    public int available(int i) {
        return portions[i] + raw.getItem(i).getCount() * PORTIONS_PER_RAW;
    }

    private boolean take(int i) {
        if (portions[i] == 0) {
            ItemStack stack = raw.getItem(i);
            SeasoningDef next = stack.isEmpty() ? null : Seasonings.ofRaw(stack);
            if (next == null) {
                return false;
            }
            kindIds[i] = SauceIds.fullId(next);
            stack.shrink(1);
            portions[i] += PORTIONS_PER_RAW;
        }
        portions[i]--;
        changed();
        return true;
    }

    public void serve(Player player, InteractionHand hand, int i) {
        SeasoningDef s = kindDef(i);
        if (s == null || available(i) == 0) {
            player.sendOverlayMessage(Component.translatable(HotPotBlockEntity.key(
                kindIndex(i) == -2 ? "sauce.unknown" : "msg.bowl_empty")));
            return;
        }
        ItemStack held = player.getItemInHand(hand);
        boolean sauce = held.getItem() instanceof DippingSauceItem;
        if (sauce && DippingSauceItem.contents(held).has(SauceIds.fullId(s))) {
            player.sendOverlayMessage(Component.translatable(HotPotBlockEntity.key("msg.sauce_has"),
                Component.translatable(s.nameKey())));
            return;
        }
        if (!take(i)) {
            return;
        }
        if (sauce) {
            held.set(HotPotContent.SAUCE, DippingSauceItem.contents(held).with(SauceIds.fullId(s)));
            if (SauceIds.hasAllBuiltins(DippingSauceItem.contents(held).ids())) {
                HotPotCriterion.fire(player, "sauce_full");
            }
        } else {
            player.setItemInHand(hand, ItemUtils.createFilledResult(held, player,
                DippingSauceItem.createWithId(SauceIds.fullId(s))));
        }
        level.playSound(null, worldPosition, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 0.6F, 1.4F);
    }

    public void changed() {
        for (int i = 0; i < BOWLS; i++) {
            ItemStack stack = raw.getItem(i);
            boolean hadRaw = !rawWasEmpty[i];
            boolean hasRaw = !stack.isEmpty();
            boolean sameRaw = hasRaw && sameRawKind(lastRaw[i], stack);
            rawWasEmpty[i] = !hasRaw;
            lastRaw[i] = stack.copy();
            SeasoningDef saved = kindIds[i].isEmpty() ? null : SauceIds.def(kindIds[i]);
            SeasoningDef current = hasRaw ? Seasonings.ofRaw(stack) : null;
            kindIds[i] = updatedKindId(kindIds[i], portions[i], hadRaw, hasRaw,
                saved != null, current == null ? "" : SauceIds.fullId(current), sameRaw);
        }
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private static boolean sameRawKind(ItemStack previous, ItemStack current) {
        return !previous.isEmpty() && !current.isEmpty() && ItemStack.isSameItemSameComponents(previous, current);
    }

    static String updatedKindId(String id, int portions, boolean hadRaw, boolean hasRaw,
                                boolean known, String newRawId) {
        return updatedKindId(id, portions, hadRaw, hasRaw, known, newRawId, false);
    }

    static String updatedKindId(String id, int portions, boolean hadRaw, boolean hasRaw,
                                boolean known, String newRawId, boolean sameRaw) {
        boolean newlyFilled = !hadRaw && hasRaw;
        if (!id.isEmpty() && !known && (!newlyFilled || !hasRaw || portions > 0)) {
            return id;
        }
        if (!id.isEmpty() && known && portions > 0 && hasRaw && (newRawId.isEmpty() || sameRaw)) {
            return id;
        }
        if (!hasRaw && portions > 0) {
            return id;
        }
        return hasRaw ? newRawId : "";
    }

    @Override
    protected void saveAdditional(ValueOutput nbt) {
        super.saveAdditional(nbt);
        BeIo.write(nbt, w -> {
            NonNullList<ItemStack> list = NonNullList.withSize(raw.getContainerSize(), ItemStack.EMPTY);
            for (int i = 0; i < raw.getContainerSize(); i++) {
                list.set(i, raw.getItem(i));
            }
            w.putItems(list);
            w.putIntArray("Portions", portions);
            w.putStringList("KindIds", Arrays.asList(kindIds.clone()));
        });
    }

    @Override
    protected void loadAdditional(ValueInput nbt) {
        super.loadAdditional(nbt);
        BeIo.read(nbt, r -> {
            NonNullList<ItemStack> list = NonNullList.withSize(raw.getContainerSize(), ItemStack.EMPTY);
            r.loadItems(list);
            loading = true;
            for (int i = 0; i < raw.getContainerSize(); i++) {
                ItemStack loaded = list.get(i);
                raw.setItem(i, loaded);
                rawWasEmpty[i] = loaded.isEmpty();
                lastRaw[i] = loaded.copy();
            }
            loading = false;
            readKindIds(r, portions, kindIds);
        });
    }

    /** 新键存在优先，坏类型/空列表不复活旧数据，且只保存 ID。 */
    static void readKindIds(StoreReader r, int[] portions, String[] target) {
        int[] p = r.getIntArray("Portions");
        int[] old = r.getIntArray("Kinds");
        List<String> ids = r.getStringList("KindIds");
        boolean hasIds = r.has("KindIds");
        boolean hasKinds = r.has("Kinds");
        for (int i = 0; i < BOWLS; i++) {
            portions[i] = i < p.length ? p[i] : 0;
            if (hasIds) {
                target[i] = i < ids.size() ? ids.get(i) : "";
            } else if (hasKinds) {
                target[i] = i < old.length ? SauceIds.legacyId(old[i]) : (portions[i] > 0 ? SauceIds.legacyId(i) : "");
            } else {
                target[i] = portions[i] > 0 ? SauceIds.legacyId(i) : "";
            }
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return super.getUpdatePacket();
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return super.getUpdateTag(registries);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level != null) {
            Containers.dropContents(level, pos, raw);
        }
    }
}
