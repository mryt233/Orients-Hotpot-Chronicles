package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import net.minecraft.block.MapColor;
import net.minecraft.block.enums.NoteBlockInstrument;
import net.minecraft.sound.BlockSoundGroup;

/**
 * 火锅锅体材质。铜锅与铁锅共用同一套交互逻辑（同一方块实体 / 屏幕），
 * 区别只在合成材料与外观贴图（贴图由各自的 blockstates/models 决定）。
 */
public enum HotPotMaterial {
    /** 重庆九宫格铜锅：硬度同原版铜块。 */
    COPPER("copper_nine_grid_hotpot", MapColor.ORANGE, BlockSoundGroup.COPPER, 3.0F, NoteBlockInstrument.HARP),
    /** 扎实耐用的铁锅：硬度同原版铁块。 */
    IRON("iron_nine_grid_hotpot", MapColor.IRON_GRAY, BlockSoundGroup.METAL, 5.0F, NoteBlockInstrument.IRON_XYLOPHONE);

    private final String idPath;
    private final MapColor mapColor;
    private final BlockSoundGroup sounds;
    private final float hardness;
    private final NoteBlockInstrument instrument;

    HotPotMaterial(String idPath, MapColor mapColor, BlockSoundGroup sounds, float hardness, NoteBlockInstrument instrument) {
        this.instrument = instrument;
        this.idPath = idPath;
        this.mapColor = mapColor;
        this.sounds = sounds;
        this.hardness = hardness;
    }

    /** 方块 / 物品的注册路径名。 */
    public String idPath() {
        return idPath;
    }

    /** 模型里侧壁贴图的名称后缀（block/hot_pot_&lt;小写材质名&gt;_side）。 */
    public String textureKey() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    public NoteBlockInstrument instrument() {
        return instrument;
    }

    public MapColor mapColor() {
        return mapColor;
    }

    public BlockSoundGroup sounds() {
        return sounds;
    }

    public float hardness() {
        return hardness;
    }
}
