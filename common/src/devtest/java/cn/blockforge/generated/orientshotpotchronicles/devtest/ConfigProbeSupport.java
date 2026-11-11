// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.devtest;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.Config;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;

/** 隔离配置夹具与实例内鼠标坐标，绝不操作系统输入。 */
final class ConfigProbeSupport {
    private static final String FILE = "orients_hotpot_chronicles.json";
    private static Object store, base, oldDirectory, oldPath, oldServer, oldClient, oldDedicated;
    private static int oldParticle;
    private static Path fixture;
    private static String hoverTarget = "none";
    private static double hoverX, hoverY;
    private ConfigProbeSupport() { }
    private static Field f(Class<?> type, String name) throws ReflectiveOperationException {
        Field f=type.getDeclaredField(name); f.setAccessible(true); return f;
    }
    static void prepare(Minecraft mc, String kind) throws Exception {
        if (mc.level != null || oldDirectory != null) throw new IllegalStateException("须在主菜单且先恢复上一次夹具");
        if (!java.util.Set.of("normal","valid","failed","memory","broken").contains(kind)) throw new IllegalArgumentException(kind);
        String guard=System.getProperty("hotpot.bgclient.dir");
        if (guard == null || guard.isBlank()) throw new IllegalStateException("缺少后台实例证据目录");
        Path evidence=Path.of(guard).toAbsolutePath().normalize().getParent();
        if (evidence == null) throw new IllegalStateException("无效证据目录");
        oldDirectory=f(Config.class,"directory").get(null);
        if (oldDirectory == null) throw new IllegalStateException("配置尚未初始化");
        store=f(Config.class,"STORE").get(null); base=f(Config.class,"BASE").get(null);
        oldPath=f(store.getClass(),"path").get(store); oldServer=f(store.getClass(),"server").get(store);
        oldClient=f(store.getClass(),"client").get(store); oldDedicated=f(base.getClass(),"dedicatedServer").get(base);
        oldParticle=Config.particleIntensity();
        try {
            Path cases=evidence.resolve("save-cases"); Files.createDirectories(cases);
            Path caseDir=cases.resolve(kind); Files.createDirectory(caseDir);
            fixture=caseDir.resolve("config");
            if (kind.equals("memory")) Files.writeString(fixture,"keep"); else Files.createDirectory(fixture);
            Config.init(fixture,false); Config.reload();
            if (kind.equals("failed")) Files.createDirectory(fixture.resolve(FILE));
            if (kind.equals("broken")) Files.writeString(fixture.resolve(FILE),"{broken");
            mc.setScreen(new cn.blockforge.generated.orientshotpotchronicles.client.HotPotConfigScreen());
        } catch (Exception error) { restore(mc); throw error; }
    }
    static void restore(Minecraft mc) throws Exception {
        if (mc.level != null || oldDirectory == null) throw new IllegalStateException("无可恢复的主菜单夹具");
        mc.setScreen(null);
        // 恢复持有的原快照和路径，避免重读或迁移用户配置。
        f(Config.class,"directory").set(null,oldDirectory);
        f(store.getClass(),"path").set(store,oldPath); f(store.getClass(),"server").set(store,oldServer);
        f(store.getClass(),"client").set(store,oldClient); f(base.getClass(),"dedicatedServer").set(base,oldDedicated);
        Config.setParticleIntensity(oldParticle);
        oldDirectory=null; fixture=null;
    }
    static void hover(Minecraft mc, double x, double y, String target) throws ReflectiveOperationException {
        double wx=x*mc.getWindow().getScreenWidth()/mc.getWindow().getGuiScaledWidth();
        double wy=y*mc.getWindow().getScreenHeight()/mc.getWindow().getGuiScaledHeight();
        f(mc.mouseHandler.getClass(),"xpos").setDouble(mc.mouseHandler,wx);
        f(mc.mouseHandler.getClass(),"ypos").setDouble(mc.mouseHandler,wy);
        hoverX=x; hoverY=y; hoverTarget=target;
    }
    static void snapshot(Minecraft mc, JsonObject out) throws Exception {
        out.addProperty("mouse_x",mc.mouseHandler.xpos()*mc.getWindow().getGuiScaledWidth()/mc.getWindow().getScreenWidth());
        out.addProperty("mouse_y",mc.mouseHandler.ypos()*mc.getWindow().getGuiScaledHeight()/mc.getWindow().getScreenHeight());
        out.addProperty("hover_target",hoverTarget); out.addProperty("hover_requested_x",hoverX); out.addProperty("hover_requested_y",hoverY);
        out.addProperty("particle_intensity",Config.particleIntensity());
        Path directory=fixture == null ? (Path)f(Config.class,"directory").get(null) : fixture;
        Path path=directory == null ? null : directory.resolve(FILE);
        out.addProperty("fixture",fixture == null ? "" : fixture.toString());
        out.addProperty("disk_kind",path == null ? "uninitialized" : Files.isRegularFile(path) ? "file" : Files.isDirectory(path) ? "directory" : "missing");
        if (path != null && Files.isRegularFile(path)) out.addProperty("disk_sha256",HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path))));
    }
}
