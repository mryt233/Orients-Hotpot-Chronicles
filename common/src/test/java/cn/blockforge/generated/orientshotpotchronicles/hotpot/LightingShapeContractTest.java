// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** 防止把“包含一立方米”误当成原版“形状严格等于整块”；不替代光照实机取证。 */
class LightingShapeContractTest {
    @BeforeAll static void bootstrap() { Opt3AttributesTest.bootstrapMinecraft(); }

    @Test void containingFullCubeDoesNotMakeExtendedShapeAFullBlock() {
        var tall = Block.box(0, 0, 0, 16, 21, 16);
        assertFalse(Shapes.joinIsNotEmpty(Shapes.block(), tall, BooleanOp.ONLY_FIRST));
        assertFalse(Block.isShapeFullBlock(tall));
        assertTrue(Block.isShapeFullBlock(Shapes.block()));
    }

    @Test void allStoveHeightsAndHandleAxesRemainNonFullShapes() {
        for (double lift : new double[]{0, StoveBlock.TALL_LIFT}) {
            for (boolean pot : new boolean[]{false, true}) {
                for (Direction.Axis axis : new Direction.Axis[]{Direction.Axis.X, Direction.Axis.Z}) {
                    assertFalse(Block.isShapeFullBlock(StoveBlock.shapeFor(lift, pot, axis)));
                }
            }
        }
    }

    @Test void tallProxyGeometryCanBeComputedWithoutWorldAccess() {
        for (boolean pot : new boolean[]{false, true}) {
            for (Direction.Axis axis : new Direction.Axis[]{Direction.Axis.X, Direction.Axis.Z}) {
                var proxy = StoveBlock.shapeFor(StoveBlock.TALL_LIFT, pot, axis).move(0, -1, 0);
                assertFalse(Block.isShapeFullBlock(proxy));
                assertTrue(proxy.bounds().minY < 0);
                assertTrue(proxy.bounds().maxY < 1);
            }
        }
    }

    @Test void stoveOuterFlushFacesCullWhileUpperTallWallFacesDoNot() throws Exception {
        for (String name : new String[]{"stove", "stove_lit", "tall_stove", "tall_stove_lit"}) {
            String path = "/assets/orients_hotpot_chronicles/models/block/" + name + ".json";
            JsonObject model;
            try (InputStream in = LightingShapeContractTest.class.getResourceAsStream(path)) {
                assertNotNull(in, "缺失灶台模型资源: " + path);
                model = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            }
            for (JsonElement rawEl : model.getAsJsonArray("elements")) {
                JsonObject el = rawEl.getAsJsonObject();
                var from = el.getAsJsonArray("from");
                var to = el.getAsJsonArray("to");
                double x0 = from.get(0).getAsDouble(), y0 = from.get(1).getAsDouble(), z0 = from.get(2).getAsDouble();
                double x1 = to.get(0).getAsDouble(), y1 = to.get(1).getAsDouble(), z1 = to.get(2).getAsDouble();
                for (Map.Entry<String, JsonElement> entry : el.getAsJsonObject("faces").entrySet()) {
                    String face = entry.getKey();
                    JsonObject faceObj = entry.getValue().getAsJsonObject();
                    String expectedCull = expectedStoveCullface(face, x0, y0, z0, x1, y1, z1);
                    if (expectedCull == null) {
                        assertFalse(faceObj.has("cullface"), name + " 不应有 cullface: " + face + " y1=" + y1);
                    } else {
                        assertEquals(expectedCull, faceObj.get("cullface").getAsString(), name + " 外砖缺少 cullface");
                    }
                }
            }
        }
    }

    private static String expectedStoveCullface(String face, double x0, double y0, double z0,
                                                double x1, double y1, double z1) {
        if (y1 > 16.0) {
            return null;
        }
        return switch (face) {
            case "north" -> z0 == 0.0 ? "north" : null;
            case "south" -> z1 == 16.0 ? "south" : null;
            case "west" -> x0 == 0.0 ? "west" : null;
            case "east" -> x1 == 16.0 ? "east" : null;
            case "down" -> y0 == 0.0 ? "down" : null;
            default -> null;
        };
    }
}
