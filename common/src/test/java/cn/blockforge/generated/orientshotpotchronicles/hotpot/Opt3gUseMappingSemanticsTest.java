// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter.CompoundReader;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter.CompoundWriter;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter.StoredItem;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter.Use;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter.UseOutcome;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 第三批 3G：版本适配小包<b>语义层</b>的独立验收（与 {@link Opt3gAdapterTest} 互补，不重复它的用例）。
 *
 * <p>mryt233 加（2026-11-11）：{@code Opt3gAdapterTest} 钉的是「某个意图换回来的是哪个常量」，
 * 这里钉的是「换回来的常量在原版里到底怎么表现」——用原版枚举自己的 {@code consumesAction()} /
 * {@code result()} / {@code shouldSwing()} / {@code indicateItemUse()} 来判断，不硬编码布尔值。
 * 这样 1.21.1 的语义一旦变化（或有人把两个同 {@code result()} 的常量合并成一个意图），
 * 断言会以「语义不一致」的形式失败，而不是默默漂移。</p>
 *
 * <p>默认值那一条用反射真调了 {@link BlockBehaviour} 的受保护方法（单测环境里 {@code Blocks.STONE}
 * 是普通 {@code Block}、没有覆写这两个入口，反射调用安全），把「1.21.1 的方块默认行为」写死，
 * 防止有人把版本相关的默认值塞进小包当成通用默认。</p>
 */
class Opt3gUseMappingSemanticsTest {
    @BeforeAll
    static void bootstrap() {
        Opt3AttributesTest.bootstrapMinecraft();
    }

    private static HolderLookup.Provider registries() {
        return RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY).freeze();
    }

    // ------------------------------------------------------------ 语义签名

    /**
     * 一个右键结果「原版会怎么表现」的三要素：算不算用到物品、会不会挥手、算不算用了手里物品。
     * 数值全部取自原版枚举方法，测试里不写 true / false 字面量。
     *
     * <p>{@link ItemInteractionResult} 自己没有挥手 / 统计这两个入口，它最终会 {@code result()}
     * 落到一个 {@link InteractionResult} 上，所以按 {@code result()} 那份算——原版
     * {@code ServerPlayerGameMode.useItemOn} 就是拿 {@code result()} 做这些判断的。</p>
     */
    private record Semantics(boolean consumesAction, boolean swings, boolean indicateItemUse) {
    }

    private static Semantics of(ItemInteractionResult result) {
        InteractionResult finalResult = result.result();
        return new Semantics(result.consumesAction(), finalResult.shouldSwing(), finalResult.indicateItemUse());
    }

    private static Semantics of(InteractionResult result) {
        return new Semantics(result.consumesAction(), result.shouldSwing(), result.indicateItemUse());
    }

    // ------------------------------------------------------------ 1. 七个意图的语义

    /**
     * 方块「拿着东西」侧逐意图断语义：换回来的常量要和「原版对应常量」完全一致，
     * 而且要和它最终落到的那档 {@link InteractionResult} 的语义一致（跨类型互证，不是自说自话）。
     */
    @Test
    void blockItemIntentSignaturesFollowVanillaSemantics() {
        for (boolean client : new boolean[]{true, false}) {
            // 7 种意图里只有 PASS 在方块「拿着东西」侧没有对应常量（设计如此），其余逐个核。
            assertSemantics(ItemInteractionResult.SUCCESS, Use.blockItem(UseOutcome.SUCCESS, client));
            assertSemantics(ItemInteractionResult.CONSUME, Use.blockItem(UseOutcome.CONSUME, client));
            assertSemantics(ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION,
                Use.blockItem(UseOutcome.SKIPPED, client));
            assertSemantics(ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION,
                Use.blockItem(UseOutcome.PASS_TO_EMPTY, client));
            assertSemantics(ItemInteractionResult.FAIL, Use.blockItem(UseOutcome.FAIL, client));
            // SUCCESS_SOFT 的客户端 / 服务端两半在下一支用例里单独钉。
        }

        // 跨类型互证：这几对在原版里必须「表现完全一样」，否则意图分类就站不住。
        assertEquals(of(InteractionResult.SUCCESS), of(ItemInteractionResult.SUCCESS));
        assertEquals(of(InteractionResult.CONSUME), of(ItemInteractionResult.CONSUME));
        assertEquals(of(InteractionResult.PASS), of(ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION));
        assertEquals(of(InteractionResult.PASS), of(ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION));
        assertEquals(of(InteractionResult.FAIL), of(ItemInteractionResult.FAIL));

        // 四个「不算用到物品」的常量必须都没有 consumeAction，SUCCESS / CONSUME 必须都有。
        assertTrue(of(ItemInteractionResult.SUCCESS).consumesAction());
        assertTrue(of(ItemInteractionResult.CONSUME).consumesAction());
        assertFalse(of(ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION).consumesAction());
        assertFalse(of(ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION).consumesAction());
        assertFalse(of(ItemInteractionResult.FAIL).consumesAction());
    }

    /**
     * {@code SUCCESS_SOFT} 在「方块拿着东西」侧必须逐字等于原版 {@code sidedSuccess(client)}：
     * 客户端那半是 {@code SUCCESS}、服务端那半是 {@code CONSUME}，语义分别按原版方法算出来对齐。
     * 物品侧（{@code item} / {@code itemUse}）同理。
     *
     * <p>空手侧也遵循同一分端规则，不把 sidedSuccess 误作 SUCCESS_NO_ITEM_USED。</p>
     */
    @Test
    void successSoftBehavesLikeSidedSuccessOnBothSides() {
        for (boolean client : new boolean[]{true, false}) {
            ItemInteractionResult expected = ItemInteractionResult.sidedSuccess(client);
            assertSame(expected, Use.blockItem(UseOutcome.SUCCESS_SOFT, client));
            assertEquals(of(expected), of(Use.blockItem(UseOutcome.SUCCESS_SOFT, client)));

            assertEquals(of(InteractionResult.sidedSuccess(client)), of(Use.item(UseOutcome.SUCCESS_SOFT, client)));

            InteractionResultHolder<ItemStack> held = Use.itemUse(UseOutcome.SUCCESS_SOFT,
                new ItemStack(Items.BOWL), client);
            assertEquals(of(InteractionResult.sidedSuccess(client)), of(held.getResult()));

            // 空手侧：有意地恒等 SUCCESS，不跟 sidedSuccess 走。
            assertSame(InteractionResult.sidedSuccess(client), Use.blockEmpty(UseOutcome.SUCCESS_SOFT, client));
            assertEquals(of(InteractionResult.sidedSuccess(client)), of(Use.blockEmpty(UseOutcome.SUCCESS_SOFT, client)));
        }
        // 客户端那一半是「真成功」（挥手 + 算用到物品），服务端那一半只是「算用到物品」（不挥手）。
        assertEquals(of(InteractionResult.SUCCESS), of(Use.blockItem(UseOutcome.SUCCESS_SOFT, true)));
        assertEquals(of(InteractionResult.CONSUME), of(Use.blockItem(UseOutcome.SUCCESS_SOFT, false)));
        assertNotEquals(of(Use.blockItem(UseOutcome.SUCCESS_SOFT, true)),
            of(Use.blockItem(UseOutcome.SUCCESS_SOFT, false)));

        // 其它六个意图与客户端 / 服务端无关：两侧必须拿同一个常量。
        for (UseOutcome outcome : new UseOutcome[]{UseOutcome.SUCCESS, UseOutcome.CONSUME, UseOutcome.SKIPPED,
            UseOutcome.PASS_TO_EMPTY, UseOutcome.FAIL}) {
            assertSame(Use.blockItem(outcome, true), Use.blockItem(outcome, false), outcome.name());
        }
    }

    /** 空手侧与物品侧的逐意图语义：这两侧没有「跳过空手」这一档，映射要和原版常量表现一致。 */
    @Test
    void blockEmptyAndItemSideFollowVanillaSignatures() {
        for (boolean client : new boolean[]{true, false}) {
            // 空手：SUCCESS_SOFT 按端选择，SKIPPED 按接口约定退化成 CONSUME。
            assertSame(InteractionResult.SUCCESS, Use.blockEmpty(UseOutcome.SUCCESS, client));
            assertSame(InteractionResult.sidedSuccess(client), Use.blockEmpty(UseOutcome.SUCCESS_SOFT, client));
            assertSame(InteractionResult.CONSUME, Use.blockEmpty(UseOutcome.CONSUME, client));
            assertSame(InteractionResult.CONSUME, Use.blockEmpty(UseOutcome.SKIPPED, client));
            assertSame(InteractionResult.PASS, Use.blockEmpty(UseOutcome.PASS_TO_EMPTY, client));
            assertSame(InteractionResult.PASS, Use.blockEmpty(UseOutcome.PASS, client));
            assertSame(InteractionResult.FAIL, Use.blockEmpty(UseOutcome.FAIL, client));

            // 物品侧：SKIPPED / PASS_TO_EMPTY / PASS 都放行；SUCCESS 与 SUCCESS_SOFT 的区别只在挥手。
            assertSame(InteractionResult.SUCCESS, Use.item(UseOutcome.SUCCESS, client));
            assertSame(InteractionResult.CONSUME, Use.item(UseOutcome.CONSUME, client));
            assertSame(InteractionResult.PASS, Use.item(UseOutcome.SKIPPED, client));
            assertSame(InteractionResult.PASS, Use.item(UseOutcome.PASS_TO_EMPTY, client));
            assertSame(InteractionResult.PASS, Use.item(UseOutcome.PASS, client));
            assertSame(InteractionResult.FAIL, Use.item(UseOutcome.FAIL, client));

            // 语义层再核一遍：CONSUME 档「算用到物品但不挥手」，PASS 档三样都不算。
            assertTrue(of(Use.blockEmpty(UseOutcome.CONSUME, client)).consumesAction());
            assertFalse(of(Use.blockEmpty(UseOutcome.CONSUME, client)).swings());
            assertEquals(of(InteractionResult.PASS), of(Use.item(UseOutcome.SKIPPED, client)));
            assertEquals(of(InteractionResult.FAIL), of(Use.item(UseOutcome.FAIL, client)));
        }

        // itemUse 换回来的结果同样对得上，而且手持物原样交回（换手持物只换第二个参数）。
        ItemStack held = new ItemStack(Items.BOWL, 3);
        for (UseOutcome outcome : new UseOutcome[]{UseOutcome.SUCCESS, UseOutcome.CONSUME, UseOutcome.SKIPPED,
            UseOutcome.PASS_TO_EMPTY, UseOutcome.PASS, UseOutcome.FAIL}) {
            InteractionResultHolder<ItemStack> holder = Use.itemUse(outcome, held, false);
            assertSame(held, holder.getObject(), outcome.name());
            assertSame(Use.item(outcome, false), holder.getResult(), outcome.name());
        }
        assertSame(InteractionResultHolder.pass(held).getResult(),
            Use.itemUse(UseOutcome.SKIPPED, held, false).getResult());
    }

    // ------------------------------------------------------------ 2. CONSUME 与 SKIPPED 语义不同

    /**
     * 1.21.1 上 {@code CONSUME} 与 {@code SKIP_DEFAULT_BLOCK_INTERACTION} 的<b>表现</b>不同，
     * 不是「换个名字的同一个东西」：前者 consumesAction 为真、原版直接返回；后者为假、原版还会接着调手里物品。
     * 两者最终落到 {@link InteractionResult} 时也分成 CONSUME 与 PASS 两档。
     */
    @Test
    void consumeAndSkipHaveDifferentActionAndResultSemantics() {
        ItemInteractionResult consume = Use.blockItem(UseOutcome.CONSUME, false);
        ItemInteractionResult skip = Use.blockItem(UseOutcome.SKIPPED, false);

        // 常量本身就不是同一个，也不是「同一个常量换了个名字」。
        assertNotSame(consume, skip);
        assertNotEquals(consume.name(), skip.name());

        // 语义差一：算不算「吃掉这一下」。
        assertTrue(consume.consumesAction());
        assertFalse(skip.consumesAction());
        assertNotEquals(consume.consumesAction(), skip.consumesAction());

        // 语义差二：最终落到 InteractionResult 的两档不同。
        assertSame(InteractionResult.CONSUME, consume.result());
        assertSame(InteractionResult.PASS, skip.result());
        assertNotSame(consume.result(), skip.result());

        // 本来要合并的那个陷阱：CONSUME 档不挥手但算用到物品；PASS 档三样都不算。
        assertEquals(of(InteractionResult.CONSUME), of(consume));
        assertEquals(of(InteractionResult.PASS), of(skip));
        assertFalse(of(consume).swings());
        assertTrue(of(consume).indicateItemUse());
        assertFalse(of(skip).swings());
        assertFalse(of(skip).indicateItemUse());
    }

    // ------------------------------------------------------------ 3. 1.21.1 的三个默认值事实

    /**
     * 1.21.1 的方块默认行为（反射真调 {@link BlockBehaviour} 的受保护入口）：
     * <ol>
     *   <li>{@code useItemOn} 默认返回 {@code PASS_TO_DEFAULT_BLOCK_INTERACTION}（不是 CONSUME、不是 FAIL）；</li>
     *   <li>{@code useWithoutItem} 默认返回 {@code PASS}；</li>
     *   <li>两者的最终 {@link InteractionResult} 都是 PASS、都不 consumesAction——也就是说「默认」在这一层
     *       已经合一，谁都别把「默认」当成一个意图塞进小包：1.20.1 的 {@code use} 默认是 PASS，
     *       26.x 的 {@code useItemOn} 默认是 {@code TRY_WITH_EMPTY_HAND}，各版本不一样。</li>
     * </ol>
     * 用 {@code Blocks.STONE}（单测里就是普通 {@code Block}，不覆写这两个入口）来取默认值；
     * 拿到的是真实方法返回值，不是读注释猜的。
     */
    @Test
    void blockUseDefaultsOn1211ArePassToDefaultBlockInteractionAndPass() throws Exception {
        Method useItemOn = BlockBehaviour.class.getDeclaredMethod("useItemOn", ItemStack.class, BlockState.class,
            Level.class, BlockPos.class, Player.class, InteractionHand.class, BlockHitResult.class);
        Method useWithoutItem = BlockBehaviour.class.getDeclaredMethod("useWithoutItem", BlockState.class,
            Level.class, BlockPos.class, Player.class, BlockHitResult.class);
        useItemOn.setAccessible(true);
        useWithoutItem.setAccessible(true);

        BlockState stoneState = Blocks.STONE.defaultBlockState();
        Object defaultItemOn = useItemOn.invoke(Blocks.STONE, ItemStack.EMPTY, stoneState, null, BlockPos.ZERO,
            null, InteractionHand.MAIN_HAND, null);
        Object defaultEmpty = useWithoutItem.invoke(Blocks.STONE, stoneState, null, BlockPos.ZERO, null, null);

        // 事实一：方块「拿着东西」默认放行给「默认方块交互」。
        assertSame(ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION, defaultItemOn);
        // 事实二：空手默认 PASS。
        assertSame(InteractionResult.PASS, defaultEmpty);
        // 事实三：两个默认值落到 InteractionResult 后语义相同（都不挥手、都不算用到物品、都不 consumeAction），
        // 所以「默认」不能当一个跨版本通用的意图。
        assertSame(((ItemInteractionResult) defaultItemOn).result(), defaultEmpty);
        assertEquals(of(InteractionResult.PASS), of((ItemInteractionResult) defaultItemOn));
        assertEquals(of(InteractionResult.PASS), of((InteractionResult) defaultEmpty));

        // 小包里对应的两个意图必须显式写出来（PASS_TO_EMPTY / SKIPPED），不能靠「默认值」蒙混。
        assertSame(defaultItemOn, Use.blockItem(UseOutcome.PASS_TO_EMPTY, false));
        assertSame(defaultEmpty, Use.blockEmpty(UseOutcome.PASS_TO_EMPTY, false));
        assertSame(defaultEmpty, Use.blockEmpty(UseOutcome.PASS, false));
    }

    // ------------------------------------------------------------ 4. 原语往返

    /** 数值 / 字符串 / 数组的极端值往返：负数、MIN / MAX、NaN、Infinity、空串、空数组、非 ASCII。 */
    @Test
    void numericPrimitiveRoundTripKeepsExtremesAndEmptyArrays() {
        CompoundTag tag = new CompoundTag();
        CompoundWriter writer = new CompoundWriter(tag, registries());
        writer.putInt("IntMin", Integer.MIN_VALUE);
        writer.putInt("IntMax", Integer.MAX_VALUE);
        writer.putInt("IntNeg", -42);
        writer.putFloat("FloatMin", Float.MIN_VALUE);
        writer.putFloat("FloatNegMax", -Float.MAX_VALUE);
        writer.putFloat("FloatNaN", Float.NaN);
        writer.putFloat("FloatInf", Float.POSITIVE_INFINITY);
        writer.putDouble("DoubleMin", Double.MIN_VALUE);
        writer.putDouble("DoubleNegMax", -Double.MAX_VALUE);
        writer.putDouble("DoubleNaN", Double.NaN);
        writer.putDouble("DoubleNegInf", Double.NEGATIVE_INFINITY);
        writer.putBoolean("True", true);
        writer.putBoolean("False", false);
        writer.putString("EmptyString", "");
        writer.putString("Unicode", "\u706b\u9505\ud83c\udf72");
        writer.putIntArray("EmptyInts", new int[0]);
        writer.putByteArray("EmptyBytes", new byte[0]);
        writer.putIntArray("NegativeInts", new int[]{-1, Integer.MIN_VALUE, 0, Integer.MAX_VALUE});
        writer.putByteArray("SignedBytes", new byte[]{-128, -1, 0, 127});

        CompoundReader reader = new CompoundReader(tag, registries());
        assertEquals(Integer.MIN_VALUE, reader.getInt("IntMin", 0));
        assertEquals(Integer.MAX_VALUE, reader.getInt("IntMax", 0));
        assertEquals(-42, reader.getInt("IntNeg", 0));
        assertEquals(Float.MIN_VALUE, reader.getFloat("FloatMin", 0F));
        assertEquals(-Float.MAX_VALUE, reader.getFloat("FloatNegMax", 0F));
        assertEquals(Float.floatToIntBits(Float.NaN), Float.floatToIntBits(reader.getFloat("FloatNaN", 0F)));
        assertEquals(Float.POSITIVE_INFINITY, reader.getFloat("FloatInf", 0F));
        assertEquals(Double.MIN_VALUE, reader.getDouble("DoubleMin", 0D));
        assertEquals(-Double.MAX_VALUE, reader.getDouble("DoubleNegMax", 0D));
        assertEquals(Double.doubleToLongBits(Double.NaN), Double.doubleToLongBits(reader.getDouble("DoubleNaN", 0D)));
        assertEquals(Double.NEGATIVE_INFINITY, reader.getDouble("DoubleNegInf", 0D));
        assertTrue(reader.getBoolean("True", false));
        assertFalse(reader.getBoolean("False", true));
        assertEquals("", reader.getString("EmptyString", "fallback"));
        assertEquals("\u706b\u9505\ud83c\udf72", reader.getString("Unicode", ""));
        assertArrayEquals(new int[0], reader.getIntArray("EmptyInts"));
        assertArrayEquals(new byte[0], reader.getByteArray("EmptyBytes"));
        assertArrayEquals(new int[]{-1, Integer.MIN_VALUE, 0, Integer.MAX_VALUE}, reader.getIntArray("NegativeInts"));
        assertArrayEquals(new byte[]{-128, -1, 0, 127}, reader.getByteArray("SignedBytes"));

        // 空串是「有键」而不是「缺键」：has 必须为真，兜底值不能被拿出来。
        assertTrue(reader.has("EmptyString"));
        assertTrue(reader.has("EmptyInts"));
        assertTrue(reader.has("EmptyBytes"));
    }

    /**
     * 兜底值只在<b>缺键</b>时生效，和原版 {@code CompoundTag.contains(key) ? get : fallback} 的写法一致；
     * 键在但类型不对时给的是 {@code CompoundTag} 自己的默认（数值 0 / 空串），不是调用方给的 fallback。
     */
    @Test
    void fallbacksApplyOnlyWhenTheKeyIsAbsent() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Text", "abc");
        tag.putDouble("Decimal", 1.5D);
        tag.putFloat("Single", 2.5F);
        tag.putBoolean("Flag", true);
        CompoundReader reader = new CompoundReader(tag, registries());

        // 缺键：逐个原语回调用方给的兜底。
        assertEquals(-7, reader.getInt("Missing", -7));
        assertEquals(0.5F, reader.getFloat("Missing", 0.5F));
        assertEquals(1.25D, reader.getDouble("Missing", 1.25D));
        assertTrue(reader.getBoolean("Missing", true));
        assertEquals("fallback", reader.getString("Missing", "fallback"));
        assertEquals(0, reader.getIntArray("Missing").length);
        assertEquals(0, reader.getByteArray("Missing").length);
        assertFalse(reader.has("Missing"));
        assertFalse(reader.hasCompound("Missing"));
        assertFalse(reader.hasNumber("Missing"));
        assertTrue(reader.getItem("Missing").isEmpty());

        // 键在但类型不对：has 为真，取出来是原版 CompoundTag 的默认值。
        assertTrue(reader.has("Text"));
        assertEquals(0, reader.getInt("Text", -7));
        assertEquals(0F, reader.getFloat("Text", 0.5F));
        assertFalse(reader.hasNumber("Text"));
        assertFalse(reader.hasCompound("Text"));
        assertTrue(reader.getItem("Text").isEmpty());
        // 数值型键之间可以互读（原版 getInt / getFloat / getDouble 都按 Number 取）。
        assertEquals(1, reader.getInt("Decimal", -7));
        assertEquals(1.5F, reader.getFloat("Decimal", -7F));
        assertEquals(2.5D, reader.getDouble("Single", -7D));
        assertTrue(reader.hasNumber("Decimal"));
        assertTrue(reader.hasNumber("Single"));
        // 原版把布尔也当数字（TAG_ANY_NUMERIC 覆盖 byte）。
        assertTrue(reader.hasNumber("Flag"));
        assertEquals(1, reader.getInt("Flag", -7));

        // null 标签当空档处理：所有键都缺，兜底值统一。
        CompoundReader empty = new CompoundReader(null, registries());
        assertEquals(-7, empty.getInt("Missing", -7));
        assertFalse(empty.has("Missing"));
        assertEquals(0, empty.items().size());
        assertEquals(0, empty.children("Missing").size());
        assertEquals(-7, empty.child("Missing").getInt("Anything", -7));
    }

    /** 写出空栈要跳过（原版 {@code ContainerHelper} / {@code ItemStack.save} 的写法就是非空才写）。 */
    @Test
    void putItemSkipsEmptyAndKeepsComponents() {
        CompoundTag tag = new CompoundTag();
        CompoundWriter writer = new CompoundWriter(tag, registries());
        writer.putItem("Empty", ItemStack.EMPTY);
        assertFalse(tag.contains("Empty"));

        ItemStack sword = new ItemStack(Items.IRON_SWORD);
        sword.set(DataComponents.DAMAGE, 5);
        writer.putItem("Sword", sword);
        assertTrue(tag.contains("Sword"));
        assertEquals(Tag.TAG_COMPOUND, tag.getTagType("Sword"));

        ItemStack readBack = new CompoundReader(tag, registries()).getItem("Sword");
        assertEquals(Items.IRON_SWORD, readBack.getItem());
        assertEquals(1, readBack.getCount());
        assertEquals(Integer.valueOf(5), readBack.get(DataComponents.DAMAGE));

        // 计数也要原样回来。
        writer.putItem("Bowl", new ItemStack(Items.BOWL, 3));
        assertEquals(3, new CompoundReader(tag, registries()).getItem("Bowl").getCount());
    }

    /**
     * {@code putItems} 写出来的必须是原版 {@code ContainerHelper.saveAllItems} 那份：
     * 键名 {@code Items}、每条带 {@code Slot}、空槽不写、条目顺序按输入顺序。
     */
    @Test
    void putItemsIsIdenticalToContainerHelperSaveAllItems() {
        List<ItemStack> items = new ArrayList<>(Arrays.asList(new ItemStack(Items.BOWL, 3), ItemStack.EMPTY,
            new ItemStack(Items.STICK, 64), ItemStack.EMPTY, new ItemStack(Items.CARROT, 1)));
        CompoundTag ours = new CompoundTag();
        new CompoundWriter(ours, registries()).putItems(items);

        CompoundTag vanilla = new CompoundTag();
        NonNullList<ItemStack> vanillaList = NonNullList.withSize(items.size(), ItemStack.EMPTY);
        for (int i = 0; i < items.size(); i++) {
            vanillaList.set(i, items.get(i));
        }
        ContainerHelper.saveAllItems(vanilla, vanillaList, registries());

        assertEquals(vanilla, ours);
        assertEquals(vanilla.toString(), ours.toString());
        assertEquals(Tag.TAG_LIST, ours.getTagType("Items"));
        assertEquals(Tag.TAG_COMPOUND, ours.getList("Items", Tag.TAG_COMPOUND).getElementType());
        // 空槽不写条目：5 个位置里只有 3 个非空。
        assertEquals(3, ours.getList("Items", Tag.TAG_COMPOUND).size());

        // 全空表也一样要逐字相同（原版三参数重载的空表结果就是 {Items:[]}）。
        CompoundTag oursEmpty = new CompoundTag();
        new CompoundWriter(oursEmpty, registries()).putItems(Arrays.asList(ItemStack.EMPTY, ItemStack.EMPTY));
        CompoundTag vanillaEmpty = new CompoundTag();
        ContainerHelper.saveAllItems(vanillaEmpty, NonNullList.withSize(2, ItemStack.EMPTY), registries());
        assertEquals(vanillaEmpty, oursEmpty);
        assertEquals(vanillaEmpty.toString(), oursEmpty.toString());
    }

    /** 空表也写 {@code Items} 键，类型是 ListTag、元素类型为空（原版 {@code {Items:[]}}），并且读回来是空表。 */
    @Test
    void emptyItemListWritesListTagAndLoadsNothing() {
        CompoundTag tag = new CompoundTag();
        new CompoundWriter(tag, registries()).putItems(Collections.emptyList());

        assertTrue(tag.contains("Items"));
        assertEquals(Tag.TAG_LIST, tag.getTagType("Items"));
        assertEquals(0, tag.getList("Items", Tag.TAG_COMPOUND).size());
        assertEquals("{Items:[]}", tag.toString());

        CompoundReader reader = new CompoundReader(tag, registries());
        assertEquals(0, reader.items().size());
        List<ItemStack> out = new ArrayList<>(Arrays.asList(ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY));
        reader.loadItems(out);
        for (ItemStack stack : out) {
            assertTrue(stack.isEmpty());
        }

        // 空 childList 也一样：键在、是 ListTag，只是没有条目。
        CompoundTag childTag = new CompoundTag();
        new CompoundWriter(childTag, registries()).childList("Plates");
        assertTrue(childTag.contains("Plates"));
        assertEquals(Tag.TAG_LIST, childTag.getTagType("Plates"));
        assertEquals(0, childTag.getList("Plates", Tag.TAG_COMPOUND).size());
        assertEquals(0, new CompoundReader(childTag, registries()).children("Plates").size());
    }

    /** 子节点 / 子节点列表的类型、嵌套、条目顺序，以及缺键 / 类型不符时的空 reader。 */
    @Test
    void childAndChildListKeepTypesNestingAndOrder() {
        CompoundTag tag = new CompoundTag();
        CompoundWriter writer = new CompoundWriter(tag, registries());
        // 注意：child(key) 每次都新建一个空节点并覆盖同名键，所以同一个子节点上的多个键要用同一个 writer 写。
        // （子节点里只有「当前节点」的概念，没有「再取回已有子节点」的入口。）这里拿住 writer 复用它。
        var fuel = writer.child("Fuel");
        fuel.child("Deep").putInt("Level", 3);
        fuel.putItems(List.of(new ItemStack(Items.COAL, 2)));
        writer.child("Empty");
        var plates = writer.childList("Plates");
        plates.addChild().putInt("Order", 1);
        plates.addChild().putInt("Order", 2);
        plates.addChild().putString("Name", "minecraft:oak_planks");

        // 类型：child 是 CompoundTag，childList 是「放 CompoundTag 的 ListTag」。
        assertEquals(Tag.TAG_COMPOUND, tag.getTagType("Fuel"));
        assertEquals(Tag.TAG_COMPOUND, tag.getTagType("Empty"));
        assertEquals(Tag.TAG_LIST, tag.getTagType("Plates"));
        assertEquals(Tag.TAG_COMPOUND, tag.getList("Plates", Tag.TAG_COMPOUND).getElementType());
        assertEquals(3, tag.getList("Plates", Tag.TAG_COMPOUND).size());

        CompoundReader reader = new CompoundReader(tag, registries());
        // 嵌套：Fuel -> Deep 两层都读得到，Fuel 自己也带着物品列表。
        assertTrue(reader.hasCompound("Fuel"));
        assertTrue(reader.child("Fuel").hasCompound("Deep"));
        assertEquals(3, reader.child("Fuel").child("Deep").getInt("Level", -1));
        assertEquals(1, reader.child("Fuel").items().size());
        assertEquals(Items.COAL, reader.child("Fuel").items().get(0).stack().getItem());

        // 条目顺序：children() 按下标顺序交回，不是无序集合。
        List<Object> orders = new ArrayList<>();
        for (var entry : reader.children("Plates")) {
            orders.add(entry.getInt("Order", -1));
        }
        assertEquals(Arrays.asList(1, 2, -1), orders);
        assertEquals("minecraft:oak_planks", reader.children("Plates").get(2).getString("Name", ""));

        // child 是空 CompoundTag 时能读到、但里面什么都没有。
        assertTrue(reader.has("Empty"));
        assertFalse(reader.child("Empty").has("Anything"));
        assertEquals(42, reader.child("Empty").getInt("Anything", 42));

        // 缺键 / 类型不符：child 回空 reader、children 回空列表，不抛异常。
        assertEquals(42, reader.child("Missing").getInt("Anything", 42));
        assertEquals(0, reader.children("Missing").size());
        assertEquals(0, reader.children("Fuel").size()); // 是 compound 不是 list
        assertFalse(reader.child("Plates").has("Order")); // 是 list 不是 compound
        assertEquals(0, reader.children("Empty").size());
    }

    /**
     * {@code StoredItem} 要如实交回「这条有没有 {@code Slot}」：旧存档（{@code SimpleInventory.toNbtList}
     * 生成、没有 Slot）必须能被区分出来，坏条目也要占着位置、物品是空栈。
     */
    @Test
    void storedItemsReportLegacyEntriesWithoutSlot() {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        CompoundTag legacy = (CompoundTag) new ItemStack(Items.BOWL, 2).save(registries());
        list.add(legacy);
        CompoundTag slotted = (CompoundTag) new ItemStack(Items.CARROT, 5).save(registries());
        slotted.putByte("Slot", (byte) 130); // 原版 Slot 存的是 byte，读出时要按无符号处理
        list.add(slotted);
        CompoundTag broken = new CompoundTag();
        broken.putString("id", "minecraft:definitely_not_an_item");
        broken.putInt("count", 1);
        list.add(broken);
        tag.put("Items", list);

        List<StoredItem> stored = new CompoundReader(tag, registries()).items();
        assertEquals(3, stored.size());

        // 旧档：没有 Slot，slot 用 -1 占位，hasSlot 为假。
        assertFalse(stored.get(0).hasSlot());
        assertEquals(-1, stored.get(0).slot());
        assertEquals(Items.BOWL, stored.get(0).stack().getItem());
        assertEquals(2, stored.get(0).stack().getCount());

        // 有 Slot：130 按无符号读出来还是 130。
        assertTrue(stored.get(1).hasSlot());
        assertEquals(130, stored.get(1).slot());
        assertEquals(Items.CARROT, stored.get(1).stack().getItem());

        // 坏条目：位置还在（不能静默丢条目），物品是空栈。
        assertFalse(stored.get(2).hasSlot());
        assertEquals(-1, stored.get(2).slot());
        assertTrue(stored.get(2).stack().isEmpty());

        // Items 键类型不对时按「没有物品」处理。
        CompoundTag wrongType = new CompoundTag();
        wrongType.putString("Items", "not_a_list");
        assertEquals(0, new CompoundReader(wrongType, registries()).items().size());
    }

    /**
     * 读档放回容器：有 Slot 的按槽位放、没有 Slot 的旧档按写入顺序放，
     * 必须和原版 {@code ContainerHelper.loadAllItems} 逐槽相同（这里直接拿原版结果对比，不猜顺序）。
     */
    @Test
    void itemListLoadsIntoContainerBySlotWithLegacyFallback() {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        CompoundTag first = (CompoundTag) new ItemStack(Items.BOWL, 2).save(registries());
        first.putByte("Slot", (byte) 0);
        list.add(first);
        CompoundTag fifth = (CompoundTag) new ItemStack(Items.CARROT, 1).save(registries());
        fifth.putByte("Slot", (byte) 5);
        list.add(fifth);
        // 旧档夹在中间，没有 Slot。
        list.add((CompoundTag) new ItemStack(Items.STICK, 3).save(registries()));
        tag.put("Items", list);

        CompoundReader reader = new CompoundReader(tag, registries());
        List<ItemStack> viaOurs = new ArrayList<>(Collections.nCopies(8, ItemStack.EMPTY));
        reader.loadItems(viaOurs);

        NonNullList<ItemStack> viaVanilla = NonNullList.withSize(8, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, viaVanilla, registries());

        for (int i = 0; i < viaOurs.size(); i++) {
            assertEquals(viaVanilla.get(i).getItem(), viaOurs.get(i).getItem(), "槽 " + i);
            assertEquals(viaVanilla.get(i).getCount(), viaOurs.get(i).getCount(), "槽 " + i);
        }
        // 单独核实一遍最容易出错的那条：旧档（无 Slot）按原版规则进 0 号槽。
        assertEquals(Items.STICK, viaOurs.get(0).getItem());
        assertEquals(Items.CARROT, viaOurs.get(5).getItem());

        // 容器比存档短时不能越界（原版也忽略超出部分的条目），越界的 Slot 5 不能被写出去抛异常。
        List<ItemStack> shortTarget = new ArrayList<>(Collections.nCopies(1, ItemStack.EMPTY));
        reader.loadItems(shortTarget);
        NonNullList<ItemStack> shortVanilla = NonNullList.withSize(1, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, shortVanilla, registries());
        assertEquals(shortVanilla.get(0).getItem(), shortTarget.get(0).getItem());
    }

    /**
     * 缺 {@code Items} 键时 {@link CompoundReader#loadItems} 和原版 {@code ContainerHelper.loadAllItems}
     * 一样：什么都不做，容器保持原样（不是清空）。
     *
     * <p>mryt233 改（2026-11-11）：这条原来是「已知差异」的 characterization——实现里先建等长空表再回抄，
     * 会把缺键时的目标表清空。已按原版写法改成只回填 {@code Items} 里真正写了的槽，所以这里改成
     * 两边逐条一致的正向断言。</p>
     */
    @Test
    void loadItemsWithoutItemsKeyPreservesTheTargetLikeVanilla() {
        List<ItemStack> viaOurs = new ArrayList<>(List.of(new ItemStack(Items.BOWL, 1)));
        new CompoundReader(new CompoundTag(), registries()).loadItems(viaOurs);
        assertEquals(Items.BOWL, viaOurs.get(0).getItem());

        // 原版对同一份缺键的标签：容器同样原封不动。
        NonNullList<ItemStack> viaVanilla = NonNullList.withSize(1, ItemStack.EMPTY);
        viaVanilla.set(0, new ItemStack(Items.BOWL, 1));
        ContainerHelper.loadAllItems(new CompoundTag(), viaVanilla, registries());
        assertEquals(Items.BOWL, viaVanilla.get(0).getItem());

        assertEquals(viaVanilla.get(0).getItem(), viaOurs.get(0).getItem());
    }

    /**
     * <b>使用陷阱（如实钉住）</b>：{@link StoreWriter#child(String)} 每调一次都新建一个空节点并覆盖同名键，
     * 所以对一个子节点写多个键时必须复用第一次拿到的 writer——
     * {@code writer.child("Fuel").putA(); writer.child("Fuel").putB();} 会把 A 冲掉。
     * 业务代码目前都只取一次（如 {@code StoveBlockEntity} 的 {@code w.child("Fuel").putItems(list)}），
     * 这里把这个约定写下来，防止以后拆成多句。
     */
    @Test
    void childOpensAFreshNodeSoTheWriterMustBeReused() {
        CompoundTag tag = new CompoundTag();
        CompoundWriter writer = new CompoundWriter(tag, registries());
        writer.child("Fuel").putInt("A", 1);
        writer.child("Fuel").putInt("B", 2);
        // 第二次 child("Fuel") 换了新节点，A 已经不在了。
        CompoundReader reader = new CompoundReader(tag, registries());
        assertEquals(2, reader.child("Fuel").getInt("B", -1));
        assertEquals(-1, reader.child("Fuel").getInt("A", -1));

        // 复用 writer 才对：同一个子节点上两个键都在。
        CompoundTag reused = new CompoundTag();
        CompoundWriter writer2 = new CompoundWriter(reused, registries());
        var fuel = writer2.child("Fuel");
        fuel.putInt("A", 1);
        fuel.putInt("B", 2);
        CompoundReader reader2 = new CompoundReader(reused, registries());
        assertEquals(1, reader2.child("Fuel").getInt("A", -1));
        assertEquals(2, reader2.child("Fuel").getInt("B", -1));
    }

    // ------------------------------------------------------------ 小工具

    /** 断言「实际换回来的常量」和 {@code expected} 用原版方法算出来的语义完全一致。 */
    private static void assertSemantics(ItemInteractionResult expected, ItemInteractionResult actual) {
        assertEquals(of(expected), of(actual), actual.name());
        assertSame(expected, actual);
    }
}
