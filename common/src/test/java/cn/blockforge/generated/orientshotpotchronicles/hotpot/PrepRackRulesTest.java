// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class PrepRackRulesTest {
    @BeforeAll static void bootstrap() { SharedConstants.tryDetectVersion(); Bootstrap.bootStrap(); }
    private static ResourceLocation id(String id) { return ResourceLocation.parse(id); }
    private static RackRuleDef def(String id,String json) {
        return RackRuleDef.parse(id(id),JsonParser.parseString(json).getAsJsonObject());
    }

    @Test void exactBeatsTagAndCategoryRegardlessOfPriority() {
        var item=id("minecraft:bread");
        var tag=def("example:high", "{\"tag\":\"#example:foods\",\"priority\":900,\"max_stack\":8}");
        var exact=def("example:one", "{\"item\":\"minecraft:bread\",\"layout\":\"row_depth\",\"max_stack\":16,\"overrides\":{\"desktop\":{\"max_stack\":12}}}");
        var fallback=def("example:food", "{\"category\":\"food\",\"max_stack\":3}");
        var tags=new ArrayList<>(List.of(item,id("minecraft:carrot")));
        var snapshot=RackRules.compile(5,List.of(tag,fallback,exact),i->true,i->tags);
        tags.clear();
        var roundTrip=RackRules.Snapshot.fromJson(snapshot.toJson());
        assertEquals(snapshot,roundTrip);
        assertEquals(16,roundTrip.resolve(item,RackRuleDef.Category.FOOD,PrepRackBlock.Kind.MULTI_TIER).maxStack());
        var desk=roundTrip.resolve(item,RackRuleDef.Category.FOOD,PrepRackBlock.Kind.DESKTOP);
        assertEquals(12,desk.maxStack());
        assertEquals(RackRuleDef.Layout.ROW_DEPTH,desk.layout());
        assertEquals(8,roundTrip.resolve(id("minecraft:carrot"),RackRuleDef.Category.FOOD,PrepRackBlock.Kind.MULTI_TIER).maxStack());
        assertEquals(3,roundTrip.resolve(id("minecraft:fish"),RackRuleDef.Category.FOOD,PrepRackBlock.Kind.MULTI_TIER).maxStack());
    }

    @Test void rejectInvalidSyntaxCountsAndIndices() {
        for (String json : List.of("{\"category\":\"other\",\"max_stack\":65}","{\"category\":\"food\",\"max_visual\":17}",
            "{\"category\":\"food\",\"max_stack\":2.5}","{\"category\":\"food\",\"scale\":1e90}",
            "{\"item\":\"minecraft:apple\",\"tag\":\"#example:all\"}","{\"category\":\"food\",\"layout\":\"popup\"}")) {
            assertThrows(RuntimeException.class,()->def("example:test",json));
        }
        assertThrows(IllegalArgumentException.class,()->RackRules.Snapshot.fromJson(
            "{\"revision\":1,\"items\":{\"minecraft:bread\":0},\"profiles\":[],\"categories\":{}}"));
    }

    @Test void clientReplacesAtomicallyButRejectsBrokenOrOldGeneration() {
        RackRules.clearClient();
        try {
            RackRules.acceptClient(new RackRules.Snapshot(4,List.of(),Map.of(),Map.of()).toJson());
            assertTrue(RackRules.clientReady());
            RackRules.acceptClient("{\"revision\":5}");
            RackRules.acceptClient(new RackRules.Snapshot(0,List.of(),Map.of(),Map.of()).toJson());
            assertTrue(RackRules.clientReady());
        } finally { RackRules.clearClient(); }
        assertFalse(RackRules.clientReady());
    }

    @Test void realItemLimitsAndDifferentComponents() {
        ItemStack bread = new ItemStack(Items.BREAD,5);
        ItemStack named = bread.copy();
        named.set(DataComponents.CUSTOM_NAME,Component.literal("Left"));
        var slots = List.of(named,bread.copyWithCount(2),ItemStack.EMPTY,bread.copyWithCount(7));
        assertEquals(3,PrepRackBlockEntity.insertionSlot(slots,4,bread,12,0,i->i==3?0:i));
        assertEquals(2,PrepRackBlockEntity.insertionSlot(slots,4,bread,2,0,i->i));
        assertEquals(0,PrepRackBlockEntity.insertionSlot(slots,4,named,12,0,i->i));
        assertEquals(-1,PrepRackBlockEntity.insertionSlot(List.of(bread),1,bread,4,0,i->0));
        assertEquals(1,RackRules.capacity(null,new ItemStack(Items.IRON_PICKAXE),PrepRackBlock.Kind.HANGING));
    }

    @Test void ceilingCellMeetsHookWithoutReachingWood() {
        var cell=PrepRackGeometry.cell(PrepRackBlock.Kind.HANGING,PrepRackBlock.Mount.CEILING,1);
        assertEquals(7.70,cell.y()*16,1e-8);
        double top=cell.box().maxY*16;
        assertTrue(top>=8.8 && top<9.0);
    }

    @Test void facingCellsRetainGeometryAndStableDistance() {
        var pos=BlockPos.ZERO;
        Vec3 south=PrepRackGeometry.local(Direction.SOUTH,pos,0,new Vec3(.25,.35,.75));
        Vec3 west=PrepRackGeometry.local(Direction.WEST,pos,0,new Vec3(.25,.35,.25));
        assertEquals(south,west);
        var a=PrepRackGeometry.cell(PrepRackBlock.Kind.MULTI_TIER,PrepRackBlock.Mount.WALL,0);
        var b=PrepRackGeometry.cell(PrepRackBlock.Kind.MULTI_TIER,PrepRackBlock.Mount.WALL,1);
        assertFalse(a.box().intersects(b.box()));
        assertTrue(a.distance(south)<b.distance(south));
        var r=Config.defaultsCopy().getAsJsonObject("rack");
        assertEquals(12,r.get("rack_default_max_stack").getAsInt());
        assertEquals(8,r.get("rack_max_visual_items").getAsInt());
    }
}
