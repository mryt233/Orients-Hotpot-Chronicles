// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.client;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.HotPot;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.input.KeyEvent;
// mryt233 改：26.3 使用原版 SDL 按键常量。

/** 火锅小游戏客户端按键绑定（默认 G 挑战/应战，D F J K 为四轨并兼容方向键）。作者：mryt233 */
public final class HotPotKeybinds {
    public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(HotPot.id("main"));

    public static final KeyMapping CHALLENGE = new KeyMapping(
        "key." + HotPot.MOD_ID + ".minigame_challenge", InputConstants.Type.KEYBOARD, InputConstants.KEY_G, CATEGORY);
    public static final KeyMapping LANE_0 = new KeyMapping(
        "key." + HotPot.MOD_ID + ".minigame_lane_0", InputConstants.Type.KEYBOARD, InputConstants.KEY_D, CATEGORY);
    public static final KeyMapping LANE_1 = new KeyMapping(
        "key." + HotPot.MOD_ID + ".minigame_lane_1", InputConstants.Type.KEYBOARD, InputConstants.KEY_F, CATEGORY);
    public static final KeyMapping LANE_2 = new KeyMapping(
        "key." + HotPot.MOD_ID + ".minigame_lane_2", InputConstants.Type.KEYBOARD, InputConstants.KEY_J, CATEGORY);
    public static final KeyMapping LANE_3 = new KeyMapping(
        "key." + HotPot.MOD_ID + ".minigame_lane_3", InputConstants.Type.KEYBOARD, InputConstants.KEY_K, CATEGORY);

    private static final KeyMapping[] ALL = { CHALLENGE, LANE_0, LANE_1, LANE_2, LANE_3 };

    private HotPotKeybinds() { }

    public static KeyMapping[] all() {
        return ALL;
    }

    public static int laneForKey(KeyEvent event) {
        int keyCode = event.key();
        if (LANE_0.matches(event) || keyCode == InputConstants.KEY_LEFT) return 0;
        if (LANE_1.matches(event) || keyCode == InputConstants.KEY_DOWN) return 1;
        if (LANE_2.matches(event) || keyCode == InputConstants.KEY_UP) return 2;
        if (LANE_3.matches(event) || keyCode == InputConstants.KEY_RIGHT) return 3;
        return -1;
    }

    public static String labelForLane(int lane) {
        KeyMapping m = switch (lane) {
            case 0 -> LANE_0;
            case 1 -> LANE_1;
            case 2 -> LANE_2;
            case 3 -> LANE_3;
            default -> null;
        };
        return m == null ? "?" : m.getTranslatedKeyMessage().getString();
    }
}
