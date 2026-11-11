// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.devtest;

import cn.blockforge.generated.orientshotpotchronicles.client.GuideBookScreen;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import java.lang.reflect.Method;
import java.util.List;

/** 作者：mryt233；2026-11-11：只读导出指南排版，定位仅作用于测试实例；不进入成品包。 */
public final class GuideAt {
    private static JsonObject result = new JsonObject();
    private GuideAt() { }
    static JsonObject result() { return result; }
    static Object field(Object target, String name) throws ReflectiveOperationException {
        var f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(target);
    }
    static Object call(Object target, String name, Class<?>[] types, Object... args) throws ReflectiveOperationException {
        Method m = target.getClass().getDeclaredMethod(name, types);
        m.setAccessible(true);
        return m.invoke(target, args);
    }
    public static void run(Minecraft mc, JsonArray args) throws ReflectiveOperationException {
        if (!(mc.screen instanceof GuideBookScreen)) mc.setScreen(new GuideBookScreen());
        GuideBookScreen screen = (GuideBookScreen) mc.screen;
        result = new JsonObject();
        result.addProperty("language", mc.getLanguageManager().getSelected());
        JsonArray entries = new JsonArray();
        List<?> cats = (List<?>) field(screen, "cats");
        if(args.isEmpty()) for (int c = 0; c < cats.size(); c++) {
            List<?> items = (List<?>) field(cats.get(c), "entries");
            for (int e = 0; e < items.size(); e++) {
                call(screen, "open", new Class<?>[]{int.class,int.class}, c,e);
                JsonObject entry = new JsonObject();
                entry.addProperty("cat",c); entry.addProperty("entry",e);
                JsonArray columns = new JsonArray();
                for (Object col : (List<?>) field(screen,"columns")) {
                    JsonArray elements = new JsonArray();
                    for (Object el : (List<?>) col) {
                        JsonObject element = new JsonObject();
                        for (String key : new String[]{"kind","height","color","number","goCat","goEntry","indent","recipes"})
                            element.addProperty(key,String.valueOf(field(el,key)));
                        JsonArray lines = new JsonArray();
                        for (Object line : (List<?>)field(el,"lines")) {
                            StringBuilder text = new StringBuilder();
                            JsonArray styles = new JsonArray();
                            ((FormattedCharSequence)line).accept((index,style,code) -> {
                                text.appendCodePoint(code); styles.add(style.toString()); return true;
                            });
                            JsonObject lineData = new JsonObject();
                            lineData.addProperty("text",text.toString()); lineData.add("styles",styles); lines.add(lineData);
                        }
                        element.add("lines",lines);
                        element.addProperty("icon",((ItemStack)field(el,"icon")).toString());
                        JsonArray icons = new JsonArray();
                        for(Object icon:(List<?>)field(el,"icons")) icons.add(icon.toString());
                        element.add("icons",icons); elements.add(element);
                    }
                    columns.add(elements);
                }
                entry.add("columns",columns); entries.add(entry);
            }
        }
        result.add("entries",entries);
        int c=args.size()>0?args.get(0).getAsInt():0, e=args.size()>1?args.get(1).getAsInt():0;
        call(screen,"open",new Class<?>[]{int.class,int.class},c,e);
        if(args.size()>2) {
            Object owner=screen;
            java.lang.reflect.Field f;
            try { f=owner.getClass().getDeclaredField("spread"); }
            catch (NoSuchFieldException missing) {
                owner=field(screen,"pager");
                f=owner.getClass().getDeclaredField("spread");
            }
            f.setAccessible(true);f.setInt(owner,args.get(2).getAsInt());
        }
    }
}
