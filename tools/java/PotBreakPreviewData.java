// 作者：mryt233
import cn.blockforge.generated.orientshotpotchronicles.hotpot.BlockBreakSamples;
import cn.blockforge.generated.orientshotpotchronicles.hotpot.StoveBlock;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.SharedConstants;
import net.minecraft.core.Direction;
import net.minecraft.server.Bootstrap;

/** 只导出真实 Java 采样数据，不创建客户端、窗口或世界。 */
public final class PotBreakPreviewData {
    public static void main(String[] args) throws Exception {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        JsonObject output = new JsonObject();
        output.addProperty("author", "mryt233");
        output.addProperty("source", "StoveBlock.potShapeFor / BlockBreakSamples.destroy");
        for (Direction.Axis axis : new Direction.Axis[]{Direction.Axis.X, Direction.Axis.Z}) {
            JsonArray array = new JsonArray();
            for (var sample : BlockBreakSamples.destroy(StoveBlock.potShapeFor(0, axis))) {
                JsonArray row = new JsonArray();
                for (double value : new double[]{sample.position().x, sample.position().y, sample.position().z,
                        sample.velocity().x, sample.velocity().y, sample.velocity().z}) row.add(value);
                array.add(row);
            }
            output.add(axis.name().toLowerCase(java.util.Locale.ROOT), array);
        }
        output.addProperty("tall_lift", StoveBlock.TALL_LIFT / 16);
        output.addProperty("drop_y", BlockBreakSamples.dropPoint(net.minecraft.core.BlockPos.ZERO,
            StoveBlock.potShapeFor(0, Direction.Axis.X)).y);
        Files.writeString(Path.of(args[0]), new GsonBuilder().create().toJson(output));
    }
}
