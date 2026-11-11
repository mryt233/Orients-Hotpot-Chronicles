// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.devtest;

import cn.blockforge.generated.orientshotpotchronicles.client.HotPotConfigScreen;
import cn.blockforge.generated.orientshotpotchronicles.client.GuideBookScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import java.util.List;
import java.util.Set;
import static cn.blockforge.generated.orientshotpotchronicles.devtest.GuideAt.*;

/** 作者：mryt233；2026-11-11：设置布局及真实界面输入验收，不进入成品包。 */
public final class ConfigAt {
    private static JsonObject result = new JsonObject();
    private ConfigAt() { }
    static JsonObject result() { return result; }
    static void set(Object s,String key,Object value) throws ReflectiveOperationException {
        var f=s.getClass().getDeclaredField(key);f.setAccessible(true);f.set(s,value);
    }
    static Object superclassField(Class<?> type,Object s,String key) throws ReflectiveOperationException { var f=type.getDeclaredField(key);f.setAccessible(true);return f.get(s); }
    static Object editState(Object screen, String key) throws ReflectiveOperationException {
        try { return field(screen, key); }
        catch (NoSuchFieldException missing) { return field(field(screen, "editor"), key); }
    }
    static int n(Object s,String key) throws ReflectiveOperationException { return (int)field(s,key); }
    static Object invoke(Object s,String key) throws ReflectiveOperationException { return call(s,key,new Class<?>[0]); }
    @SuppressWarnings("unchecked")
    public static void run(Minecraft mc,JsonArray args) throws Exception {
        String action=args.get(0).getAsString();
        result=new JsonObject();
        if(action.equals("isolated_save")) {
            ConfigProbeSupport.prepare(mc,args.get(1).getAsString());
        } else if(action.equals("restore_config")) {
            ConfigProbeSupport.restore(mc); ConfigProbeSupport.snapshot(mc,result); return;
        }
        if(action.startsWith("guide_") || action.equals("hover")) {
            if(mc.screen == null) throw new IllegalStateException("需要已打开的界面");
            if(action.equals("hover")) {
                ConfigProbeSupport.hover(mc,args.get(1).getAsDouble(),args.get(2).getAsDouble(),args.size()>3?args.get(3).getAsString():"coordinate");
            } else {
                if(!(mc.screen instanceof GuideBookScreen screen)) throw new IllegalStateException("需要指南界面");
                switch(action) {
                    case "guide_search" -> {
                        screen.keyPressed(47,0,0);
                        for(char c:args.get(1).getAsString().toCharArray()) screen.charTyped(c,0);
                    }
                    case "guide_key" -> screen.keyPressed(args.get(1).getAsInt(),0,0);
                    case "guide_typed" -> { for(char c:args.get(1).getAsString().toCharArray()) screen.charTyped(c,0); }
                    case "guide_click" -> { double x=args.get(1).getAsDouble(),y=args.get(2).getAsDouble(); int button=args.size()>3?args.get(3).getAsInt():0; screen.mouseClicked(x,y,button);screen.mouseReleased(x,y,button); }
                    case "guide_wheel" -> screen.mouseScrolled(args.get(1).getAsDouble(),args.get(2).getAsDouble(),0,args.get(3).getAsDouble());
                    case "guide_state" -> { }
                    default -> throw new IllegalArgumentException(action);
                }
            }
            if(mc.screen instanceof GuideBookScreen screen) guideState(screen,result);
            ConfigProbeSupport.snapshot(mc,result); return;
        }
        if(action.equals("matrix_bounds")) {
            JsonArray matrix=new JsonArray();
            for(int width:new int[]{240,427,640}) for(int tab=0;tab<2;tab++) for(int groups:new int[]{0,1,8}) {
                HotPotConfigScreen s=new HotPotConfigScreen(); s.init(mc,width+16,360);
                s.probeAction(tab==0?"common":"advanced");
                for(int i=0;i<groups;i++) s.probeAction("expand:"+i);
                JsonObject row=new JsonObject(); row.addProperty("guiWidth",width+16); row.addProperty("panelW",n(s,"panelW"));
                row.addProperty("tab",tab); row.addProperty("groups",groups);
                row.addProperty("contentHeight",(int)invoke(s,"contentHeight")); row.addProperty("maxScroll",(int)invoke(s,"maxScroll"));
                matrix.add(row);
            }
            result.add("matrix",matrix); mc.setScreen(new HotPotConfigScreen()); return;
        }
        if(action.equals("matrix")) {
            JsonArray matrix=new JsonArray();
            for(int width:new int[]{320,427,640}) for(int tab=0;tab<2;tab++) for(int groups:new int[]{0,1,8}) {
                HotPotConfigScreen s=new HotPotConfigScreen(); s.init(mc,width,360);
                s.probeAction(tab==0?"common":"advanced");
                for(int i=0;i<groups;i++)s.probeAction("expand:"+i);
                JsonObject row=new JsonObject();row.addProperty("width",width);row.addProperty("tab",tab);row.addProperty("groups",groups);
                row.addProperty("contentHeight",(int)invoke(s,"contentHeight"));row.addProperty("maxScroll",(int)invoke(s,"maxScroll"));
                matrix.add(row);
            }
            result.add("matrix",matrix); mc.setScreen(new HotPotConfigScreen()); return;
        }
        if(!(mc.screen instanceof HotPotConfigScreen))mc.setScreen(new HotPotConfigScreen());
        HotPotConfigScreen s=(HotPotConfigScreen)mc.screen;
        switch(action) {
            case "edit" -> {
                int index=args.get(1).getAsInt(), y=n(s,"listTop")+4-n(s,"scroll");
                List<?> rows=(List<?>)invoke(s,"layout");
                for(int i=0;i<index;i++)y+=(int)call(rows.get(i),"height",new Class<?>[0]);
                int h=(int)call(rows.get(index),"height",new Class<?>[0]);
                int vw=(int)invoke(s,"valueWidth"), x=n(s,"listX")+n(s,"listW")-6-14-2-16-2-vw;
                s.mouseClicked(x+vw/2,y+(h-16)/2+4,0);s.mouseReleased(x+vw/2,y+(h-16)/2+4,0);
                EditBox box=(EditBox)editState(s,"editBox");
                if(box==null)throw new IllegalStateException("数值框未命中");
                box.setValue("");for(char c:args.get(2).getAsString().toCharArray())s.charTyped(c,0);
            }
            case "isolated_save" -> { }
            case "hover_value" -> {
                int index=args.get(1).getAsInt(), y=n(s,"listTop")-n(s,"scroll");
                List<?> rows=(List<?>)invoke(s,"layout");
                for(int i=0;i<index;i++) y+=(int)call(rows.get(i),"height",new Class<?>[0]);
                int h=(int)call(rows.get(index),"height",new Class<?>[0]);
                int vw=(int)invoke(s,"valueWidth"), x=n(s,"listX")+n(s,"listW")-6-14-2-16-2-vw;
                ConfigProbeSupport.hover(mc,x+vw/2.0,y+(h-16)/2.0+8,"config_value:"+index);
            }
            case "slider" -> {
                int percent=args.get(1).getAsInt();
                if(percent<0 || percent>100 || n(s,"tab")!=0) throw new IllegalArgumentException("滑块须在常用页，百分比为 0–100");
                int x=n(s,"px")+10, y=n(s,"py")+48+10, w=n(s,"panelW")-20;
                double start=x+4, end=start+(w-8)*percent/100.0;
                s.mouseClicked(start,y,0);s.mouseDragged(end,y,0,end-start,0);s.mouseReleased(end,y,0);
            }
            case "key" -> s.keyPressed(args.get(1).getAsInt(),0,0);
            case "wheel" -> s.mouseScrolled(n(s,"listX")+10,n(s,"listTop")+10,0,args.get(1).getAsDouble());
            case "outside" -> s.mouseClicked(0,0,0);
            case "bottom" -> {
                int w=n(s,"panelW"),h=n(s,"panelH"),bw=Math.max(70,Math.min(150,(w-36)/3));
                int x=n(s,"px")+(w-(bw*3+12))/2+args.get(1).getAsInt()*(bw+6)+bw/2;
                int y=n(s,"py")+h-20-8+10;s.mouseClicked(x,y,0);s.mouseReleased(x,y,0);
            }
            case "particle" -> {
                int qw=Math.max(36,Math.min(64,(n(s,"panelW")-24)/6));
                int x=n(s,"px")+n(s,"panelW")-10-(3*qw+8)+args.get(1).getAsInt()*(qw+4)+qw/2;
                int y=n(s,"py")+48+22+13+4;s.mouseClicked(x,y,0);s.mouseReleased(x,y,0);
            }
            default -> { if(!action.equals("state"))throw new IllegalArgumentException(action); }
        }
        result.add("edited",((JsonObject)field(s,"edited")).deepCopy());
        result.addProperty("bad",editState(s,"bad").toString());
        result.addProperty("editing",editState(s,"editBox")!=null);
        result.addProperty("same_screen",mc.screen==s);
        result.addProperty("scroll",n(s,"scroll"));
        result.addProperty("confirmReset",(boolean)field(s,"confirmReset"));
        result.addProperty("renderables",((List<?>)superclassField(net.minecraft.client.gui.screens.Screen.class,s,"renderables")).size());
        Object notice=field(s,"saveNotice");
        result.addProperty("notice",String.valueOf(notice));
        if(notice instanceof Component text) {
            result.addProperty("notice_text",text.getString());
            if(text.getContents() instanceof TranslatableContents t) result.addProperty("notice_key",t.getKey());
        }
        ConfigProbeSupport.snapshot(mc,result);
    }
    private static void guideState(GuideBookScreen screen,JsonObject out) throws ReflectiveOperationException {
        Object owner;
        try { field(screen,"cat"); owner=screen; }
        catch(NoSuchFieldException missing) { owner=field(screen,"pager"); }
        for(String key:new String[]{"cat","entry","spread"}) out.addProperty(key,(int)field(owner,key));
        out.addProperty("searchOpen",(boolean)field(screen,"searchOpen"));
        out.addProperty("searchQuery",(String)field(screen,"searchQuery"));
        JsonArray hits=new JsonArray();
        for(Object hit:(List<?>)field(screen,"searchHits")) {
            JsonObject item=new JsonObject();
            item.addProperty("cat",(int)field(hit,"cat")); item.addProperty("entry",(int)field(hit,"entry"));
            item.addProperty("snippet",(String)field(hit,"snippet")); hits.add(item);
        }
        out.add("hits",hits);
    }

}
