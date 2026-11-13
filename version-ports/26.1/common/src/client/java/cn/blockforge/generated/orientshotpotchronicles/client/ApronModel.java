package cn.blockforge.generated.orientshotpotchronicles.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * 布围裙穿在身上的模型（r146g，2026-11-09 用户定）：从胸口一直垂到膝盖上面，贴身、不鼓。
 *
 * <p>这里自己建一个人形模型：</p>
 * <ul>
 *   <li>躯干：和原版躯干同样大小的方块，只外扩 {@link #BODY_GROW}（比护腿层的 0.5 多一点，
 *       穿着护腿也不会被盖住），贴图 UV 沿用原版胸甲层（u16..40，v16..32）。</li>
 *   <li>裙片：腰下面挂一整片 8×{@value #SKIRT_H}×1 的布，挡在两条腿前面，贴图放在没用到的头部区域（起点 0,0）。
 *       转轴和腿一样在胯部；哪条腿往前迈，裙片就跟着那条腿往前摆，坐下时平铺在大腿上（见 {@link #poseSkirt}）。</li>
 *   <li>头、手臂、腿都是空的，什么都不画。</li>
 * </ul>
 * 贴图是围裙装备资源那一层（{@code textures/entity/equipment/humanoid/<款式>.png}），
 * 由 {@code tools/gen_apron_styles.py} 生成。Fabric 走 {@code ArmorRenderer}，
 * NeoForge 走 {@code IClientItemExtensions#getHumanoidArmorModel}，两边都用这一个类。
 *
 * <p>mryt233 改（2026-11-09，26.1 移植）：26.x 的模型改成「套渲染状态」，姿势在
 * {@link #setupAnim(HumanoidRenderState)} 里按状态摆好、再由调用方提交绘制，不再重写 renderToBuffer。
 * 作者：mryt233
 */
public final class ApronModel extends HumanoidModel<HumanoidRenderState> {
    /** 躯干外扩（像素）：护腿层是 0.5，胸甲层是 1.0；取 0.6 = 贴身又不会被护腿盖住。 */
    public static final float BODY_GROW = 0.6F;
    /** 裙片高（像素）：从腰垂到膝盖上面。和 gen_apron_styles.py 的 SKIRT_H 一致。 */
    public static final int SKIRT_H = 5;
    /** 裙片厚度方向收进去多少（1 像素厚的方块两面各收 0.3，剩 0.4 像素厚，像一层布）。 */
    private static final float SKIRT_THIN = -0.3F;

    private static ApronModel instance;

    private final ModelPart skirt;

    private ApronModel(ModelPart root) {
        super(root);
        this.skirt = root.getChild("apron_skirt");
    }

    /** 共用的一份模型（第一次用到时烘焙，不需要往游戏里登记模型层）。 */
    public static ApronModel get() {
        if (instance == null) {
            instance = new ApronModel(createLayer().bakeRoot());
        }
        return instance;
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        PartDefinition root = mesh.getRoot();
        // 头、帽子、手臂、腿：空的（不画），但位置要留着——裙片跟着腿的转轴走
        root.getChild("head").addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5.0F, 2.0F, 0.0F));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5.0F, 2.0F, 0.0F));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-1.9F, 12.0F, 0.0F));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(1.9F, 12.0F, 0.0F));
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(16, 16)
            .addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(BODY_GROW)), PartPose.ZERO);
        // 裙片：正面贴着躯干正面再往前 0.4 像素，背面正好落在躯干正面那一层（护腿正面在 -2.5，不会穿模）
        float front = -(2.0F + BODY_GROW) - 0.7F;
        root.addOrReplaceChild("apron_skirt", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-4.0F, 0.0F, front, 8.0F, SKIRT_H, 1.0F, new CubeDeformation(BODY_GROW, 0.0F, SKIRT_THIN)),
            PartPose.offset(0.0F, 12.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 32);
    }

    /**
     * 裙片跟着腿：转轴放在胯部（和腿的转轴同一处，蹲下时也跟着腿挪），往前摆的角度取两条腿里往前迈得多的那条；
     * 腿往后时裙片保持竖直，不往后翻。骑乘 / 坐下时腿抬平，裙片也平铺在大腿上。
     */
    private void poseSkirt() {
        skirt.visible = true;
        skirt.x = 0.0F;
        skirt.y = rightLeg.y;
        skirt.z = rightLeg.z;
        skirt.xRot = Math.min(0.0F, Math.min(rightLeg.xRot, leftLeg.xRot));
        skirt.yRot = 0.0F;
        skirt.zRot = 0.0F;
    }

    /**
     * 26.x 的绘制流程是「先 setupAnim 摆姿势、再整棵模型提交」，所以裙片的联动和部件显隐都写在这里：
     * 只画躯干和裙片，其余部件藏起来（部件位置由父类的 setupAnim 摆好，裙片照着腿的转轴走）。
     */
    @Override
    public void setupAnim(HumanoidRenderState state) {
        super.setupAnim(state);
        this.head.visible = false;
        this.hat.visible = false;
        this.rightArm.visible = false;
        this.leftArm.visible = false;
        this.rightLeg.visible = false;
        this.leftLeg.visible = false;
        this.body.visible = true;
        poseSkirt();
    }

    /** 这件围裙的贴图：装备资源里人形那层（不走护腿层）。不是围裙时返回 null。 */
    public static Identifier texture(ItemStack stack) {
        if (!(stack.getItem() instanceof cn.blockforge.generated.orientshotpotchronicles.hotpot.ClothApronItem apron)) {
            return null;
        }
        return Identifier.fromNamespaceAndPath("orients_hotpot_chronicles",
            "textures/entity/equipment/humanoid/" + apron.style().id() + ".png");
    }
}
