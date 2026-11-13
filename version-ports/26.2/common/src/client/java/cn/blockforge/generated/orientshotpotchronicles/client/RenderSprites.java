// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.resources.Identifier;

/** 26.2 贴图查询共用工具（走 AtlasManager + SpriteId）。 */
public final class RenderSprites {
    private RenderSprites() {
    }

    public static TextureAtlasSprite sprite(Identifier id) {
        return Minecraft.getInstance().getAtlasManager().get(
            new SpriteId(TextureAtlas.LOCATION_BLOCKS, id));
    }
}
