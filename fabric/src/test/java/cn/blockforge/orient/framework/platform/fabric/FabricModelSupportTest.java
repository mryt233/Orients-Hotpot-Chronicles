// 作者：mryt233
package cn.blockforge.orient.framework.platform.fabric;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelModifier;
import net.fabricmc.fabric.api.renderer.v1.Renderer;
import net.fabricmc.fabric.api.renderer.v1.material.MaterialFinder;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** 无窗口帮助逻辑回归；不替代加载器、区块构网及真实渲染验证。 */
class FabricModelSupportTest {
    @Test void registeredStandardDoesNotAllocateFinder() {
        RenderMaterial standard = material();
        Renderer renderer = proxy(Renderer.class, (object, method, args) -> {
            assertEquals("materialById", method.getName());
            assertEquals(RenderMaterial.MATERIAL_STANDARD, args[0]);
            return standard;
        });
        assertSame(standard, FabricClientContent.standardMaterial(renderer));
    }

    @Test void missingRegistrationUsesCurrentRendererFinder() {
        RenderMaterial fallback = material();
        MaterialFinder finder = proxy(MaterialFinder.class, (object, method, args) -> {
            assertEquals("find", method.getName());
            return fallback;
        });
        Renderer renderer = renderer(null, finder);
        assertSame(fallback, FabricClientContent.standardMaterial(renderer));
    }

    @Test void absentRendererFailsWithUsefulDiagnostic() {
        IllegalStateException error = assertThrows(IllegalStateException.class,
            () -> FabricClientContent.standardMaterial(null));
        assertTrue(error.getMessage().contains("渲染器尚未就绪"));
    }

    @Test void rendererCannotReturnNullMaterialToEmitter() {
        MaterialFinder finder = proxy(MaterialFinder.class, (object, method, args) -> null);
        IllegalStateException error = assertThrows(IllegalStateException.class,
            () -> FabricClientContent.standardMaterial(renderer(null, finder)));
        assertTrue(error.getMessage().contains("标准模型材质"));
    }

    @Test void standardMaterialIsNotCachedAcrossRenderers() {
        RenderMaterial first = material();
        RenderMaterial second = material();
        assertSame(first, FabricClientContent.standardMaterial(renderer(first, null)));
        assertSame(second, FabricClientContent.standardMaterial(renderer(second, null)));
    }

    @Test void ambientOcclusionMaterialForcesTriStateTrueAndDoesNotCacheAcrossRenderers() {
        RenderMaterial first = material();
        RenderMaterial second = material();
        assertSame(first, FabricClientContent.ambientOcclusionMaterial(renderer(null, aoFinder(first))));
        assertSame(second, FabricClientContent.ambientOcclusionMaterial(renderer(null, aoFinder(second))));
    }

    @Test void ambientOcclusionMaterialRejectsAbsentRendererOrNullMaterial() {
        IllegalStateException missingRenderer = assertThrows(IllegalStateException.class,
            () -> FabricClientContent.ambientOcclusionMaterial(null));
        assertTrue(missingRenderer.getMessage().contains("渲染器尚未就绪"));
        IllegalStateException missingMaterial = assertThrows(IllegalStateException.class,
            () -> FabricClientContent.ambientOcclusionMaterial(renderer(null, aoFinder(null))));
        assertTrue(missingMaterial.getMessage().contains("平滑环境光遮蔽模型材质"));
    }

    @Test void atlasLookupRetainsGetterNotReusedContext() {
        List<Material> lookedUp = new ArrayList<>();
        AtomicReference<Function<Material, TextureAtlasSprite>> current = new AtomicReference<>(value -> {
            lookedUp.add(value);
            return null;
        });
        ModelModifier.AfterBake.Context context = proxy(ModelModifier.AfterBake.Context.class,
            (object, method, args) -> {
                assertEquals("textureGetter", method.getName());
                return current.get();
            });
        Function<ResourceLocation, TextureAtlasSprite> lookup = FabricClientContent.atlasLookup(context);
        current.set(value -> { throw new AssertionError("访问了后续模型的查找器"); });
        ResourceLocation texture = ResourceLocation.parse("test:block/slot");
        assertNull(lookup.apply(texture));
        assertEquals(List.of(new Material(TextureAtlas.LOCATION_BLOCKS, texture)), lookedUp);
    }

    @Test void transformIsPoppedAfterNormalEmission() {
        List<String> calls = new ArrayList<>();
        RenderContext context = context(calls);
        FabricClientContent.emitTransformed(context, quad -> true, () -> calls.add("emit"));
        assertEquals(List.of("pushTransform", "emit", "popTransform"), calls);
    }

    @Test void transformIsPoppedAndOriginalFailurePropagates() {
        List<String> calls = new ArrayList<>();
        RenderContext context = context(calls);
        RuntimeException failure = new RuntimeException("原模型输出失败");
        RuntimeException actual = assertThrows(RuntimeException.class,
            () -> FabricClientContent.emitTransformed(context, quad -> true, () -> {
                calls.add("emit");
                throw failure;
            }));
        assertSame(failure, actual);
        assertEquals(List.of("pushTransform", "emit", "popTransform"), calls);
    }

    private static RenderContext context(List<String> calls) {
        return proxy(RenderContext.class, (object, method, args) -> {
            calls.add(method.getName());
            return null;
        });
    }

    private static MaterialFinder aoFinder(RenderMaterial target) {
        List<String> calls = new ArrayList<>();
        return proxy(MaterialFinder.class, (self, method, args) -> switch (method.getName()) {
            case "ambientOcclusion" -> {
                assertEquals(TriState.TRUE, args[0]);
                calls.add("ambientOcclusion");
                yield self;
            }
            case "find" -> {
                assertEquals(List.of("ambientOcclusion"), calls);
                yield target;
            }
            default -> throw new AssertionError(method.getName());
        });
    }

    private static Renderer renderer(RenderMaterial standard, MaterialFinder finder) {
        return proxy(Renderer.class, (object, method, args) -> switch (method.getName()) {
            case "materialById" -> standard;
            case "materialFinder" -> finder;
            default -> throw new AssertionError(method.getName());
        });
    }

    private static RenderMaterial material() {
        return proxy(RenderMaterial.class, (object, method, args) -> {
            throw new AssertionError("测试不应读取材质内部状态：" + method.getName());
        });
    }

    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler));
    }
}
