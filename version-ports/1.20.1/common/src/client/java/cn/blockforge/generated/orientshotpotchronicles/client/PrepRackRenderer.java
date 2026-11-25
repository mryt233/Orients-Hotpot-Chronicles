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
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** 架内货架布局，尺寸从槽的净空与物品模型计算；每格预算与实际库存分离。 */
public final class PrepRackRenderer implements BlockEntityRenderer<PrepRackBlockEntity> {
    private static final int MODEL_CACHE_LIMIT = 256;
    private static final Direction[] FACES = Direction.values();
    private static final double GAP = .0035;
    private static final Bounds CUSTOM = new Bounds(.5,.5,.5,1.15,1.15,1.15,null);
    private static final Bounds ICON = new Bounds(.5,.5,.5,1,1,.09,null);
    private final Map<BakedModel,Bounds> bounds = new IdentityHashMap<>();
    private final RandomSource random = RandomSource.create(42);
    private long atlasGeneration = -1;

    public PrepRackRenderer(BlockEntityRendererProvider.Context ctx) { }

    @Override
    public void render(PrepRackBlockEntity be, float tickDelta, PoseStack pose, MultiBufferSource buffers,
                       int light, int overlay) {
        if (be.isRemoved() || be.getLevel()==null || !(be.getBlockState().getBlock() instanceof PrepRackBlock rack)) return;
        long generation = cn.blockforge.orient.framework.client.BakedQuadRemap.spriteGeneration();
        if (generation != atlasGeneration) { atlasGeneration = generation; bounds.clear(); }
        var state = be.getBlockState();
        double lift = TableDecor.liftOf(be.getLevel(), be.getBlockPos(), state);
        var kind = rack.kind();
        var mount = state.getValue(PrepRackBlock.MOUNT);
        var itemRenderer = Minecraft.getInstance().getItemRenderer();
        pose.pushPose();
        try {
            pose.translate(.5,lift,.5);
            pose.mulPose(Axis.YN.rotationDegrees(state.getValue(PrepRackBlock.FACING).toYRot()));
            for (int i=0;i<kind.slots();i++) {
                ItemStack stack=be.itemAt(i);
                if (!stack.isEmpty()) renderSlot(be,itemRenderer,kind,mount,i,stack,pose,buffers,light);
            }
        } finally { pose.popPose(); }
    }

    private void renderSlot(PrepRackBlockEntity be, ItemRenderer renderer, PrepRackBlock.Kind kind,
                            PrepRackBlock.Mount mount, int slot, ItemStack stack, PoseStack pose,
                            MultiBufferSource buffers, int light) {
        var cell = PrepRackGeometry.cell(kind,mount,slot);
        var patch = RackRules.profile(be.getLevel(),stack,kind);
        Layout layout = RackRules.clientReady() ? RackRules.layout(kind,stack,patch) : Layout.SINGLE;
        BakedModel model = renderer.getModel(stack,be.getLevel(),null,slot);
        boolean custom = model.isCustomRenderer();
        boolean trusted = !custom || BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equals(HotPot.MOD_ID);
        Bounds size = custom ? (trusted ? CUSTOM : null) : measured(model);
        boolean proxy = size == null;
        if (proxy) size = ICON;
        if ((layout==Layout.ROW_DEPTH && (size.d>.24 || model.isGui3d())) || proxy || custom && layout!=Layout.HANGING) layout=Layout.SINGLE;
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
            case ROW_DEPTH -> step(depth*scale, roomD, wantGap, n);
            case FLAT_PILE -> step(height*scale, roomH, wantGap, n);
            case UPRIGHT -> step(width*scale, roomW, wantGap, n);
            default -> 0;
        };
        var rotateTilt = Axis.XP.rotationDegrees(tilt);
        // mryt233 改：吊顶按不透明边缘贴钩，斜画的厨具不会只在透明边框上「接触」。
        double x0 = cell.x();
        double y0 = kind==PrepRackBlock.Kind.HANGING ? cell.y() : cell.y()-cell.halfHeight()+height*scale*.5+GAP;
        if (ceiling) {
            int sign = flipped ? -1 : 1;
            if (anchor!=null) x0 -= sign*(anchor.x-size.x)*scale;
            y0 = cell.y()+cell.halfHeight() - (anchor==null ? height*.5 : sign*(anchor.y-size.y))*scale;
        }
        for (int i=0;i<n;i++) {
            pose.pushPose();
            try {
                double off = (i-(n-1)*.5)*step;
                // 纵深从后往前排，后排略抬起，正面也能看到每件边缘。
                double rise = layout==Layout.ROW_DEPTH ? (n-1-i)*Math.min(.007,Math.max(0,roomH-height*scale-GAP)/Math.max(1,n-1)) : 0;
                pose.translate(x0+(layout==Layout.UPRIGHT ? off : 0),
                    y0+rise+(layout==Layout.FLAT_PILE ? i*step : 0),cell.z()+(layout==Layout.ROW_DEPTH ? off : 0));
                if (flipped) pose.mulPose(Axis.ZP.rotationDegrees(180));
                if (tilt != 0) pose.mulPose(rotateTilt);
                pose.scale((float)scale,(float)scale,(float)scale);
                pose.translate(.5-size.x,.5-size.y,.5-size.z);
                if (proxy) {
                    var sprite = model.getParticleIcon();
                    if (sprite != null) {
                        pose.translate(-.5,-.5,-.5);
                        RendererBoxes.box(pose,buffers.getBuffer(RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS)),
                            sprite,light,0,0,7.3F,16,16,8.7F,0xFFFFFFFF);
                    }
                } else {
                    renderer.render(stack,ItemDisplayContext.NONE,false,pose,buffers,light,OverlayTexture.NO_OVERLAY,model);
                }
            } finally { pose.popPose(); }
        }
    }

    private static double step(double thick, double room, double pref, int count) {
        return count<=1 ? 0 : Math.min((room-thick)/(count-1),Math.max(thick+GAP,pref));
    }

    private record Edge(double x,double y) { }
    private record Rim(double minX,double maxX,Edge bottom,Edge top) { }
    private record Bounds(double x,double y,double z,double w,double h,double d,Rim rim) { }

    private static Edge nearerEdge(Edge current, double left, double right, double y, boolean upper) {
        double x = (left+right)*.5;
        if (current==null || (upper ? y>current.y : y<current.y)
            || y==current.y && Math.abs(x-.5)<Math.abs(current.x-.5)) return new Edge(x,y);
        return current;
    }

    private Bounds measured(BakedModel model) {
        if (bounds.containsKey(model)) return bounds.get(model);
        if (bounds.size()>=MODEL_CACHE_LIMIT) bounds.clear();
        Bounds result;
        try { result=scan(model); }
        catch (RuntimeException | StackOverflowError error) { result=null; }
        bounds.put(model,result);
        return result;
    }

    private Bounds scan(BakedModel model) {
        double x=20,y=20,z=20,X=-20,Y=-20,Z=-20;
        int stride=DefaultVertexFormat.BLOCK.getVertexSize()/4, quads=0;
        boolean flat = !model.isGui3d();
        double minRimX=20,maxRimX=-20;
        Edge bottom=null, top=null;
        for (int f=-1;f<FACES.length;f++) {
            random.setSeed(42);
            for (var quad : model.getQuads(null,f<0?null:FACES[f],random)) {
                if (++quads>2048) return null;
                int[] v=quad.getVertices();
                Direction face=quad.getDirection();
                boolean side=flat && face.getAxis()!=Direction.Axis.Z;
                double left=20,right=-20,low=20,high=-20;
                for (int i=0;i+2<v.length;i+=stride) {
                    double a=Float.intBitsToFloat(v[i]),b=Float.intBitsToFloat(v[i+1]),c=Float.intBitsToFloat(v[i+2]);
                    if (!Double.isFinite(a+b+c) || Math.max(Math.max(Math.abs(a),Math.abs(b)),Math.abs(c))>16) return null;
                    x=Math.min(x,a); y=Math.min(y,b); z=Math.min(z,c);
                    X=Math.max(X,a); Y=Math.max(Y,b); Z=Math.max(Z,c);
                    if (side) {
                        left=Math.min(left,a); right=Math.max(right,a);
                        low=Math.min(low,b); high=Math.max(high,b);
                    }
                }
                if (side) {
                    minRimX=Math.min(minRimX,left); maxRimX=Math.max(maxRimX,right);
                    if (face.getAxis()==Direction.Axis.Y && high-low<1e-5 && right-left>1e-5) {
                        bottom=nearerEdge(bottom,left,right,low,false);
                        top=nearerEdge(top,left,right,high,true);
                    }
                }
            }
        }
        // 生成式物品的正背面是全幅贴图；只有挤出的侧面代表实际不透明轮廓。
        Rim rim=Z-z<=.24 && bottom!=null && top!=null && top.y>bottom.y && maxRimX>minRimX
            ? new Rim(minRimX,maxRimX,bottom,top) : null;
        return quads==0 ? null : new Bounds((x+X)*.5,(y+Y)*.5,(z+Z)*.5,Math.max(.02,X-x),Math.max(.02,Y-y),Math.max(.02,Z-z),rim);
    }
}
