// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.client;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.Config;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPot;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.PrepRackBlock;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.PrepRackBlockEntity;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.PrepRackGeometry;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.RackRuleDef;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.RackRuleDef.Layout;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.RackRules;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.TableDecor;
import cn.blockforge.orient.framework.client.BakedQuadRemap;
import cn.blockforge.orient.framework.client.FrameBlockEntityRenderer;
import cn.blockforge.orient.framework.client.RenderFrame;
import cn.blockforge.orient.framework.client.mixin.ItemStackRenderStateAccessor;
import cn.blockforge.orient.framework.client.mixin.ItemStackRenderStateAccessor.LayerAccessor;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** mryt233 改：提取阶段测量真实物品层并冻结布局，提交阶段不访问世界。 */
public final class PrepRackRenderer extends FrameBlockEntityRenderer<PrepRackBlockEntity> {
    private static final double GAP = .0035;
    private static final int QUAD_LIMIT = 2048;
    private static final int LAYER_LIMIT = 128, CACHE_LIMIT = 256;
    private final Map<List<Object>, Bounds> geometry = new LinkedHashMap<>(16, .75F, true);
    private long atlasGeneration = -1;

    /** 只保存有界的模型身份；绝不把物品数量或完整组件栈用作键。 */
    private static final class MeasuredItem extends ItemStackRenderState {
        final List<Object> identity = new ArrayList<>(8);
        boolean tooLarge;
        @Override public void appendModelIdentityElement(Object element) {
            if (identity.size() < 32 && element != null) identity.add(element);
            else tooLarge = true;
        }
    }
    private static final Bounds CUSTOM = new Bounds(.5,.5,.5,1.15,1.15,1.15,null);
    private static final Bounds ICON = new Bounds(.5,.5,.5,1,1,.09,null);

    public PrepRackRenderer(BlockEntityRendererProvider.Context ctx) { }

    @Override
    protected void extractFrame(PrepRackBlockEntity be, float tickDelta, PoseStack pose, RenderFrame frame,
                                int light, int overlay) {
        if (be.isRemoved() || be.getLevel()==null || !(be.getBlockState().getBlock() instanceof PrepRackBlock rack)) return;
        long generation = BakedQuadRemap.spriteGeneration();
        if (generation != atlasGeneration) { geometry.clear(); atlasGeneration = generation; }
        var state = be.getBlockState();
        double lift = TableDecor.liftOf(be.getLevel(),be.getBlockPos(),state);
        var kind = rack.kind();
        var mount = state.getValue(PrepRackBlock.MOUNT);
        pose.pushPose();
        try {
            pose.translate(.5,lift,.5);
            pose.rotate(Axis.YN.rotationDegrees(state.getValue(PrepRackBlock.FACING).toYRot()));
            for (int i=0;i<kind.slots();i++) {
                ItemStack stack=be.itemAt(i);
                if (!stack.isEmpty()) renderSlot(be,kind,mount,i,stack,pose,frame,light);
            }
        } finally { pose.popPose(); }
    }

    private void renderSlot(PrepRackBlockEntity be, PrepRackBlock.Kind kind, PrepRackBlock.Mount mount,
                                   int slot, ItemStack stack, PoseStack pose, RenderFrame frame, int light) {
        var cell = PrepRackGeometry.cell(kind,mount,slot);
        var patch = RackRules.profile(be.getLevel(),stack,kind);
        Layout layout = RackRules.clientReady() ? RackRules.layout(kind,stack,patch) : Layout.SINGLE;
        // mryt233 改：每槽只解析一次；测量与所有展示副本使用同一个渲染状态。
        MeasuredItem item = new MeasuredItem();
        try {
            Minecraft.getInstance().getItemModelResolver().updateForTopItem(item,stack,ItemDisplayContext.NONE,be.getLevel(),null,slot);
        } catch (RuntimeException | StackOverflowError failure) {
            item = new MeasuredItem(); // mryt233 改：损坏模型退回有界代理，不提交半成品状态。
        }
        int layerKind = inspectLayers(item);
        boolean special = layerKind==1;
        boolean trusted = BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equals(HotPot.MOD_ID);
        Bounds size = layerKind<0 ? null : special ? (trusted ? CUSTOM : null) : measured(item);
        boolean proxy = size==null;
        if (proxy) size=ICON;
        if ((layout==Layout.ROW_DEPTH && (size.d>.24 || layerKind==2)) || proxy || special && layout!=Layout.HANGING) layout=Layout.SINGLE;
        int layoutLimit = switch(layout) {
            case ROW_DEPTH -> 10;
            case FLAT_PILE -> 4;
            case UPRIGHT -> 3;
            case SINGLE,HANGING -> 1;
        };
        int n = Math.min(stack.getCount(), Math.min(RackRuleDef.VISUAL_LIMIT,
            Math.min(Config.rackMaxVisualItems(),layout==Layout.SINGLE || layout==Layout.HANGING ? 1 :
                patch.maxVisual()==null ? layoutLimit : patch.maxVisual())));
        if (n<=0) return;
        float tilt = patch.tilt()!=null ? patch.tilt() : (layout==Layout.ROW_DEPTH ? 8 : 0);
        if (layout==Layout.FLAT_PILE && size.d<.24) tilt+=90;
        if (layout==Layout.HANGING) tilt=0;
        boolean ceiling = kind==PrepRackBlock.Kind.HANGING && mount==PrepRackBlock.Mount.CEILING && n==1;
        boolean flipped = layout==Layout.HANGING;
        Rim rim = ceiling && tilt==0 ? size.rim : null;
        Edge anchor = rim==null ? null : flipped ? rim.bottom : rim.top;
        double ang = Math.toRadians(tilt);
        double width=size.w, height=Math.abs(size.h*Math.cos(ang))+Math.abs(size.d*Math.sin(ang));
        double depth=Math.abs(size.d*Math.cos(ang))+Math.abs(size.h*Math.sin(ang));
        if (anchor!=null) {
            width=Math.max(.02,2*Math.max(anchor.x-rim.minX,rim.maxX-anchor.x));
            height=Math.max(.02,rim.top.y-rim.bottom.y);
        }
        double roomW=2*cell.halfWidth()-GAP, roomH=2*cell.halfHeight()-GAP, roomD=2*cell.halfDepth()-GAP;
        double target = patch.scale()!=null ? patch.scale() : .30;
        double scale = Math.min(target,Math.min(roomW/width,Math.min(roomH/height,roomD/depth)));
        double wantGap = patch.spacing()!=null ? patch.spacing() : .042;
        if (layout==Layout.ROW_DEPTH) scale=Math.min(scale,(roomD-(n-1)*GAP)/(n*depth));
        if (layout==Layout.FLAT_PILE) scale=Math.min(scale,(roomH-(n-1)*GAP)/(n*height));
        if (layout==Layout.UPRIGHT) scale=Math.min(scale,(roomW-(n-1)*GAP)/(n*width));
        if (!Double.isFinite(scale) || scale<.01) return;
        double step = switch(layout) {
            case ROW_DEPTH -> step(depth*scale,roomD,wantGap,n);
            case FLAT_PILE -> step(height*scale,roomH,wantGap,n);
            case UPRIGHT -> step(width*scale,roomW,wantGap,n);
            default -> 0;
        };
        double x0=cell.x();
        double y0=kind==PrepRackBlock.Kind.HANGING ? cell.y() : cell.y()-cell.halfHeight()+height*scale*.5+GAP;
        // 吊顶槽顶为 8.90 px，贴合挤出轮廓而不是透明边框。
        if (ceiling) {
            int sign=flipped ? -1 : 1;
            if (anchor!=null) x0-=sign*(anchor.x-size.x)*scale;
            y0=cell.y()+cell.halfHeight()-(anchor==null ? height*.5 : sign*(anchor.y-size.y))*scale;
        }
        TextureAtlasSprite sprite=null;
        if (proxy) {
            var material=item.pickParticleMaterial(RandomSource.create(42));
            sprite=material==null ? RenderSprites.sprite(MissingTextureAtlasSprite.getLocation()) : material.sprite();
        }
        for (int i=0;i<n;i++) {
            pose.pushPose();
            try {
                double off=(i-(n-1)*.5)*step;
                double rise=layout==Layout.ROW_DEPTH ? (n-1-i)*Math.min(.007,Math.max(0,roomH-height*scale-GAP)/Math.max(1,n-1)) : 0;
                pose.translate(x0+(layout==Layout.UPRIGHT ? off : 0),
                    y0+rise+(layout==Layout.FLAT_PILE ? i*step : 0),cell.z()+(layout==Layout.ROW_DEPTH ? off : 0));
                if (flipped) pose.rotate(Axis.ZP.rotationDegrees(180));
                if (tilt!=0) pose.rotate(Axis.XP.rotationDegrees(tilt));
                pose.scale((float)scale,(float)scale,(float)scale);
                pose.translate(.5-size.x,.5-size.y,.5-size.z);
                if (proxy) {
                    pose.translate(-.5,-.5,-.5);
                    RendererBoxes.box(pose,frame.getBuffer(RenderTypes.entityCutout(sprite.atlasLocation())),
                        sprite,light,0,0,7.3F,16,16,8.7F,0xFFFFFFFF);
                } else {
                    frame.item(item,light,OverlayTexture.NO_OVERLAY,pose);
                }
            } finally { pose.popPose(); }
        }
    }

    private static double step(double thick,double room,double pref,int count) {
        return count<=1 ? 0 : Math.min((room-thick)/(count-1),Math.max(thick+GAP,pref));
    }

    private record Edge(double x,double y) { }
    private record Rim(double minX,double maxX,Edge bottom,Edge top) { }
    private record Bounds(double x,double y,double z,double w,double h,double d,Rim rim) { }

    private static Edge nearerEdge(Edge current,double left,double right,double y,boolean upper) {
        double x=(left+right)*.5;
        if (current==null || (upper ? y>current.y : y<current.y)
            || y==current.y && Math.abs(x-.5)<Math.abs(current.x-.5)) return new Edge(x,y);
        return current;
    }

    // -1：状态不可安全读取；0：平面模型；1：特殊渲染；2：使用方块光照的模型。
    private static int inspectLayers(ItemStackRenderState state) {
        if (!(state instanceof ItemStackRenderStateAccessor access)) return -1;
        int count=access.orient$activeLayerCount();
        if (count<0 || count>LAYER_LIMIT || count>access.orient$layers().length) return -1;
        boolean special=false, blockLight=false;
        for (int i=0;i<count;i++) {
            if (!(access.orient$layers()[i] instanceof LayerAccessor layer)) return -1;
            special |= layer.orient$specialRenderer()!=null;
            blockLight |= layer.orient$usesBlockLight();
        }
        return special ? 1 : blockLight ? 2 : 0;
    }

    private Bounds measured(MeasuredItem state) {
        try {
            if (state.tooLarge || state.identity.isEmpty() || state.isAnimated()) return scan(state);
            List<Object> key = List.copyOf(state.identity);
            if (geometry.containsKey(key)) return geometry.get(key);
            Bounds size = scan(state);
            geometry.put(key, size);
            if (geometry.size() > CACHE_LIMIT) geometry.remove(geometry.keySet().iterator().next());
            return size;
        }
        catch (RuntimeException | StackOverflowError error) { return null; }
    }

    private static Bounds scan(ItemStackRenderState state) {
        if (!(state instanceof ItemStackRenderStateAccessor access)) return null;
        int count=access.orient$activeLayerCount();
        if (count<0 || count>LAYER_LIMIT || count>access.orient$layers().length) return null;
        double x=20,y=20,z=20,X=-20,Y=-20,Z=-20,minRimX=20,maxRimX=-20;
        Edge bottom=null,top=null;
        int quads=0;
        Vector3f[] points={new Vector3f(),new Vector3f(),new Vector3f(),new Vector3f()};
        for (int layerIndex=0;layerIndex<count;layerIndex++) {
            var layer=access.orient$layers()[layerIndex];
            if (!(layer instanceof LayerAccessor accessor) || accessor.orient$specialRenderer()!=null) return null;
            PoseStack layerPose=new PoseStack();
            accessor.orient$applyTransform(layerPose.last());
            Matrix4f transform=layerPose.last().pose();
            for (BakedQuad quad : accessor.orient$quads().all()) {
                if (++quads>QUAD_LIMIT) return null;
                double left=20,right=-20,low=20,high=-20;
                for (int i=0;i<4;i++) {
                    // applyTransform 包含原版的 -0.5；加回 0.5 保持主线坐标约定。
                    Vector3f p=points[i].set(quad.position(i)).mulPosition(transform).add(.5F,.5F,.5F);
                    double a=p.x(),b=p.y(),c=p.z();
                    if (!Double.isFinite(a+b+c) || Math.max(Math.max(Math.abs(a),Math.abs(b)),Math.abs(c))>16) return null;
                    x=Math.min(x,a); y=Math.min(y,b); z=Math.min(z,c);
                    X=Math.max(X,a); Y=Math.max(Y,b); Z=Math.max(Z,c);
                    left=Math.min(left,a); right=Math.max(right,a); low=Math.min(low,b); high=Math.max(high,b);
                }
                Vector3f normal=new Vector3f(points[1]).sub(points[0]).cross(new Vector3f(points[2]).sub(points[0]));
                boolean side=normal.lengthSquared()>1e-12F && Math.abs(normal.z())<1e-5F;
                if (side) {
                    minRimX=Math.min(minRimX,left); maxRimX=Math.max(maxRimX,right);
                    if (high-low<1e-5 && right-left>1e-5) {
                        bottom=nearerEdge(bottom,left,right,low,false);
                        top=nearerEdge(top,left,right,high,true);
                    }
                }
            }
        }
        Rim rim=Z-z<=.24 && bottom!=null && top!=null && top.y>bottom.y && maxRimX>minRimX
            ? new Rim(minRimX,maxRimX,bottom,top) : null;
        return quads==0 ? null : new Bounds((x+X)*.5,(y+Y)*.5,(z+Z)*.5,
            Math.max(.02,X-x),Math.max(.02,Y-y),Math.max(.02,Z-z),rim);
    }
}
