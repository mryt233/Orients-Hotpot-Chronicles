// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import static org.junit.jupiter.api.Assertions.*;
import java.util.function.Supplier;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** 4C 蘸料内置顺序、兼容入口及独立登记表规则。作者：mryt233 */
class Opt4cSeasoningsTest {
    @BeforeAll static void bootstrap() { Opt3AttributesTest.bootstrapMinecraft(); }

    @Test void preservesBuiltinOrderAndValues() {
        assertEquals(15, Seasoning.ALL.length);
        for (int i = 0; i < Seasoning.ALL.length; i++) {
            Seasoning old = Seasoning.ALL[i]; SeasoningDef def = old.def();
            assertSame(old, def.legacy());
            assertEquals(i, def.ordinal()); assertEquals(1 << i, def.bit());
            assertEquals(old.id(), def.id()); assertEquals(old.color(), def.color());
        }
    }

    @Test void pointsAreDefensiveAndSizesRemainUnchanged() {
        for (int i = 0; i < 15; i++) {
            SeasoningDef def = Seasoning.ALL[i].def(); float[][] dots = def.dots();
            float before = def.dotX(0); dots[0][0] = before + 1;
            assertEquals(before, def.dotX(0));
            assertEquals(i == 7 || i == 8 ? 0.7F : 0.5F, def.dotSize());
        }
        SeasoningDef standalone = sample("immutable", 15, Items.WHEAT);
        assertThrows(UnsupportedOperationException.class, () -> standalone.raws().add(Items.STICK));
    }

    @Test void independentRegistryKeepsFirstRawAndFreezes() {
        Seasonings.Registry registry = new Seasonings.Registry();
        SeasoningDef first = sample("first", 15, Items.WHEAT);
        SeasoningDef second = sample("second", 16, Items.WHEAT);
        registry.register(first); registry.register(second);
        assertSame(first, registry.raw(Items.WHEAT));
        assertSame(first, registry.byIndex(0)); assertSame(second, registry.byIndex(1));
        assertNull(registry.byIndex(-1)); assertNull(registry.byIndex(2)); assertNull(registry.byId("missing"));
        assertThrows(IllegalArgumentException.class, () -> registry.register(first));
        registry.freeze();
        assertThrows(IllegalStateException.class, () -> registry.register(sample("late", 17, Items.STICK)));
        assertEquals(2, registry.all().size());
    }

    @Test void lookupAndFreezeAreSafeTogether() throws Exception {
        Seasonings.Registry registry = new Seasonings.Registry();
        SeasoningDef first = sample("parallel", 15, Items.WHEAT);
        registry.register(first);
        java.util.concurrent.CountDownLatch start = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.atomic.AtomicReference<Throwable> failure = new java.util.concurrent.atomic.AtomicReference<>();
        Thread reader = new Thread(() -> {
            try { start.await(); for (int i = 0; i < 10000; i++) assertSame(first, registry.byId("parallel")); }
            catch (Throwable error) { failure.set(error); }
        });
        reader.start(); start.countDown(); registry.freeze(); reader.join();
        assertNull(failure.get()); assertSame(first, registry.byId("parallel"));
    }

    @SuppressWarnings("unchecked")
    @Test void rejectsInvalidDefinitions() {
        Holder<MobEffect>[] removals = (Holder<MobEffect>[]) new Holder<?>[0];
        Supplier<Item>[] sources = (Supplier<Item>[]) new Supplier<?>[]{(Supplier<Item>) () -> Items.WHEAT};
        assertThrows(NullPointerException.class, () -> new SeasoningDef(Seasoning.MSG, null, 0, 0, null, 0, 0, 0, removals, sources));
        assertThrows(NullPointerException.class, () -> new SeasoningDef(Seasoning.MSG, "x", 0, 0, null, 0, 0, 0, null, sources));
        assertThrows(NullPointerException.class, () -> new SeasoningDef(Seasoning.MSG, "x", 0, 0, null, 0, 0, 0, removals, null));
        assertThrows(IllegalArgumentException.class, () -> new SeasoningDef(Seasoning.MSG, "x", -1, 0, null, 0, 0, 0, removals, sources));
        assertThrows(IllegalArgumentException.class, () -> new SeasoningDef(Seasoning.MSG, "x", 0, 0, null, 0, 0, 0, removals, (Supplier<Item>[]) new Supplier<?>[0]));
    }

    @Test void oldPublicSignaturesRemain() throws Exception {
        assertEquals(Seasoning.class, Seasoning.class.getMethod("ofRaw", ItemStack.class).getReturnType());
        assertEquals(Seasoning.class, SauceStationBlockEntity.class.getMethod("kind", int.class).getReturnType());
        assertEquals(Seasoning.class, SauceStationScreenHandler.class.getMethod("kind", int.class).getReturnType());
    }

    @SuppressWarnings("unchecked")
    private static SeasoningDef sample(String id, int index, Item source) {
        return new SeasoningDef(Seasoning.MSG, id, index, 0xff000000, null, 0, 0, 0,
            (Holder<MobEffect>[]) new Holder<?>[0], (Supplier<Item>[]) new Supplier<?>[]{(Supplier<Item>) () -> source});
    }
}
