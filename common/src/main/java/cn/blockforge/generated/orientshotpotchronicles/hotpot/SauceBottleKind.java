package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.util.StringRepresentable;

/**
 * 摆在桌上的两种蘸料瓶（r146a 2026-11-08 加：芝麻酱、蚝油能摆上桌）。
 * <p>
 * mryt233 改（2026-11-09，r146c）：<b>瓶子只做摆设</b>——摆到桌上好看，不能再给菜加料（加这一味还是去蘸料台）。
 * 所以这里只留「摆的是哪种瓶子、桌面多高」两件事，去掉了每瓶次数、用完还容器的逻辑。
 * {@link #seasoning()} 仍保留：物品说明里要写「这一味在蘸料台上管什么用」，名字取自这里。
 * <p>
 * <b>注意：这个枚举有意只存调料的 ID、不存 {@link Seasoning} 常量</b>。
 * 它会在 {@link HotPotContent} 的静态初始化里（注册 {@code SAUCE_BOTTLE_BLOCK} 的方块状态）就被加载；
 * 如果构造器里直接引用 {@code Seasoning.SESAME_PASTE}，就会在 HotPotContent 还没给
 * {@code SESAME_PASTE} 赋值的时候把 Seasoning 的静态块拉起来——Seasoning 的表里要取这些物品，
 * 会拿到 null（蘸料台补货界面再 new ItemStack(null) 就崩）。所以调料改成按 ID 现查。
 * 作者：mryt233 */
public enum SauceBottleKind implements StringRepresentable {
    /** 芝麻酱：带标签的矮玻璃罐（6 像素高）。 */
    SESAME_PASTE("sesame_paste", "sesame_paste", 6.0),
    /** 蚝油：细高的深色瓶子（8 像素高）。 */
    OYSTER_SAUCE("oyster_sauce", "oyster_sauce", 8.0);

    private final String id;
    /** 对应调料在 {@link Seasoning} 里的 ID（延迟现查，别在类初始化时碰 Seasoning）。 */
    private final String seasoningId;
    private final double bottleHeight;

    SauceBottleKind(String id, String seasoningId, double bottleHeight) {
        this.id = id;
        this.seasoningId = seasoningId;
        this.bottleHeight = bottleHeight;
    }

    /**
     * 摆在桌上多高（像素）：选中框、碰撞和渲染都按它算，和 {@code tools/gen_sauce_bottle.py} 的 BOTTLES 一致。
     * 芝麻酱是矮玻璃罐 6、蚝油是细高的瓶子 8，比旁边的蘸料碗（3 像素）高、比饮料瓶（15 像素）矮。
     */
    public double bottleHeight() {
        return bottleHeight;
    }

    public String id() {
        return id;
    }

    @Override
    public String getSerializedName() {
        return id;
    }

    /** 这瓶对应蘸料台上的哪种调料（说明里列它管什么用）。运行时现查。 */
    public Seasoning seasoning() {
        for (Seasoning seasoning : Seasoning.ALL) {
            if (seasoning.id().equals(seasoningId)) {
                return seasoning;
            }
        }
        return Seasoning.SESAME_PASTE;
    }
}
