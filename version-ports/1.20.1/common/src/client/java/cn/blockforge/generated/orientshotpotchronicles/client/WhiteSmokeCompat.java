package cn.blockforge.generated.orientshotpotchronicles.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;

/** 原版白烟的1.20.1客户端替身，不增加粒子注册或网络数据。作者：mryt233 */
public final class WhiteSmokeCompat {
    private WhiteSmokeCompat() { }

    public static void emit(double x, double y, double z, double vx, double vy, double vz) {
        var particle = Minecraft.getInstance().particleEngine.createParticle(ParticleTypes.SMOKE, x, y, z, vx, vy, vz);
        if (particle == null) { return; }
        // mryt233 改：两版烟点的尺寸、寿命和重力相同，白烟只覆盖固定RGB，不再随机颜色。
        particle.setColor(186 / 255F, 177 / 255F, 194 / 255F);
    }
}
