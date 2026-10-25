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
    IRON("iron_nine_grid_hotpot", MapColor.IRON_GRAY, BlockSoundGroup.METAL, 5.0F, NoteBlockInstrument.IRON_XYLOPHONE),
    // 鸳鸯锅（2026-10-23 用户要，新增）：锅身同九宫格锅，隔板换成 S 形 / O 形，两边各一种汤（PotLayout）
    COPPER_S("copper_s_yuanyang_hotpot", MapColor.ORANGE, BlockSoundGroup.COPPER, 3.0F, NoteBlockInstrument.HARP),
    COPPER_O("copper_o_yuanyang_hotpot", MapColor.ORANGE, BlockSoundGroup.COPPER, 3.0F, NoteBlockInstrument.HARP),
    IRON_S("iron_s_yuanyang_hotpot", MapColor.IRON_GRAY, BlockSoundGroup.METAL, 5.0F, NoteBlockInstrument.IRON_XYLOPHONE),
    IRON_O("iron_o_yuanyang_hotpot", MapColor.IRON_GRAY, BlockSoundGroup.METAL, 5.0F, NoteBlockInstrument.IRON_XYLOPHONE),
    // 中间一道直隔板的鸳鸯锅、田字四宫格锅（用户发照片要的，新增）
    COPPER_SPLIT("copper_split_yuanyang_hotpot", MapColor.ORANGE, BlockSoundGroup.COPPER, 3.0F, NoteBlockInstrument.HARP),
    IRON_SPLIT("iron_split_yuanyang_hotpot", MapColor.IRON_GRAY, BlockSoundGroup.METAL, 5.0F, NoteBlockInstrument.IRON_XYLOPHONE),
    COPPER_FOUR("copper_four_grid_hotpot", MapColor.ORANGE, BlockSoundGroup.COPPER, 3.0F, NoteBlockInstrument.HARP),
    IRON_FOUR("iron_four_grid_hotpot", MapColor.IRON_GRAY, BlockSoundGroup.METAL, 5.0F, NoteBlockInstrument.IRON_XYLOPHONE);

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

    /** 模型里侧壁贴图的名称后缀（block/hot_pot_&lt;copper/iron&gt;_side）：鸳鸯锅和同金属的九宫格锅共用贴图。 */
    public String textureKey() {
        return metal().name().toLowerCase(java.util.Locale.ROOT);
    }

    /** 锅的金属：COPPER 或 IRON（鸳鸯锅归到同金属的九宫格锅）。 */
    public HotPotMaterial metal() {
        return switch (this) {
            case COPPER, COPPER_S, COPPER_O, COPPER_SPLIT, COPPER_FOUR -> COPPER;
            case IRON, IRON_S, IRON_O, IRON_SPLIT, IRON_FOUR -> IRON;
        };
    }

    /** 锅里怎么分格。 */
    public PotLayout layout() {
        return switch (this) {
            case COPPER, IRON -> PotLayout.NINE_GRID;
            case COPPER_S, IRON_S -> PotLayout.YUANYANG_S;
            case COPPER_O, IRON_O -> PotLayout.YUANYANG_O;
            case COPPER_SPLIT, IRON_SPLIT -> PotLayout.SPLIT;
            case COPPER_FOUR, IRON_FOUR -> PotLayout.FOUR_GRID;
        };
    }

    /** 模型名里的锅名：copper / iron / copper_yuanyang_s ……（stove_&lt;它&gt;_hot_pot_shell 等，和 gen_yuanyang.py 一致）。 */
    public String modelKey() {
        return switch (layout()) {
            case NINE_GRID -> textureKey();
            case YUANYANG_S -> textureKey() + "_yuanyang_s";
            case YUANYANG_O -> textureKey() + "_yuanyang_o";
            case SPLIT -> textureKey() + "_yuanyang_split";
            case FOUR_GRID -> textureKey() + "_four_grid";
        };
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
