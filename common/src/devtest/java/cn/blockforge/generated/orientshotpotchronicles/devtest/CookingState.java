// 作者：mryt233；mryt233 改（2026-11-11）
package cn.blockforge.generated.orientshotpotchronicles.devtest;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.*;
import com.google.gson.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/** 只读观察真实交互后的中间阶段、进度和缓存耗时；不写游戏状态。 */
public final class CookingState {
    private CookingState() { }
    public static String read(ServerLevel level, BlockPos pos) {
        JsonObject out = new JsonObject();
        var entity = level.getBlockEntity(pos);
        if (entity instanceof CuttingBoardBlockEntity board) {
            out.addProperty("pounds", board.pounds());
            out.addProperty("item", board.item().toString());
            out.addProperty("display", Pounding.display(level, board.item(), board.cuts()).getComponentsPatch().toString());
        } else if (entity instanceof HotPotBlockEntity pot) {
            try {
                for (String key : new String[]{"doneness", "cookDur", "cooked", "overcook"}) {
                    var field = HotPotBlockEntity.class.getDeclaredField(key); field.setAccessible(true);
                    out.add(key, new Gson().toJsonTree(field.get(pot)));
                }
                out.addProperty("revision", HotPotCooking.cookRevision());
            } catch (ReflectiveOperationException error) { throw new IllegalStateException(error); }
        }
        return out.toString();
    }
}
