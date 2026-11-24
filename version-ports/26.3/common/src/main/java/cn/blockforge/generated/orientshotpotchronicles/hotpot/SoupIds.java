// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter.StoreReader;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.adapter.StoreWriter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;

/** 4D：锅的汤底 ID 与旧存档序号之间的固定翻译。 */
public final class SoupIds {
    public static final int EMPTY = -1, UNKNOWN = -2;
    private static final String[] SOUP = {"Soup", "SoupB", "SoupC", "SoupD"};
    private static final String[] BASE = {"Base", "BaseB", "BaseC", "BaseD"};
    private static final String[] LEGACY = {"clear", "rich_stock", "mushroom", "spicy", "tomato", "three_fresh"};

    private SoupIds() {
    }

    public static String legacy(int index) {
        return index >= 0 && index < LEGACY.length ? HotPot.MOD_ID + ":" + LEGACY[index] : "";
    }

    public static String key(int side) {
        return SOUP[side];
    }

    public static String oldKey(int side) {
        return BASE[side];
    }

    // 旧键兼容保留到下个大版本；读取时只保存 ID，不查询登记表。
    static String read(StoreReader reader, int side) {
        if (reader.has(key(side))) {
            return reader.getString(key(side), "");
        }
        return legacy(reader.getInt(oldKey(side), -1));
    }

    static void write(StoreWriter writer, int side, String id) {
        writer.putString(key(side), id);
    }

    /** 客户端无法经过锅存储入口；同样兼容旧键到下个大版本。 */
    public static String fromPotTag(CompoundTag tag, int side) {
        if (side < 0 || side >= SOUP.length) {
            return "";
        }
        if (tag.contains(key(side))) {
            return tag.getString(key(side)).orElse("");
        }
        return legacy(tag.getInt(oldKey(side)).orElse(-1));
    }

    public static int index(String id) {
        if (id == null || id.isEmpty()) {
            return EMPTY;
        }
        SoupDef def = Soups.get(id);
        return def == null ? UNKNOWN : Soups.indexOf(def);
    }

    /** 内置食材组件继续写旧短名，其余汤底写完整 ID。 */
    static String cookedKey(Identifier id) {
        if (HotPot.MOD_ID.equals(id.getNamespace())) {
            for (String builtin : LEGACY) {
                if (builtin.equals(id.getPath())) {
                    return builtin;
                }
            }
        }
        return id.toString();
    }

    static boolean same(String a, String b) {
        SoupDef left = Soups.get(a), right = Soups.get(b);
        return left != null && right != null ? left.id().equals(right.id()) : a.equals(b);
    }

    public static String id(int index) {
        SoupDef def = Soups.byIndex(index);
        return def == null ? "" : def.id().toString();
    }
}
