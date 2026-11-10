// 作者：mryt233
package cn.blockforge.generated.orientshotpotchronicles.hotpot;

import cn.blockforge.orient.framework.pure.ConfigStore;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ConfigSaveTest {
    @TempDir
    Path directory;

    private JsonObject edited() {
        JsonObject values = new JsonObject();
        JsonObject client = new JsonObject();
        client.addProperty("particle_intensity", 37);
        values.add("client", client);
        return values;
    }

    @Test
    void failedWriteReturnsMemoryOnlyWithReason() throws IOException {
        Path blocked = directory.resolve("blocked");
        Files.writeString(blocked, "keep");
        Config.init(blocked, true);
        Config.Loaded saved = Config.saveAndReload(edited());
        assertEquals(ConfigStore.SaveStatus.MEMORY_ONLY, saved.saveStatus());
        assertFalse(saved.reason().isBlank());
        assertEquals(37, Config.particleIntensity());
        assertEquals("keep", Files.readString(blocked));
    }

    @Test
    void invalidOriginalIsBackedUpBeforeNewConfigIsSaved() throws IOException {
        Path file = directory.resolve("orients_hotpot_chronicles.json");
        byte[] original = "{broken".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        Files.write(file, original);
        Config.init(directory, true);
        Config.Loaded saved = Config.saveAndReload(edited());
        assertEquals(ConfigStore.SaveStatus.WRITTEN, saved.saveStatus());
        assertArrayEquals(original, Files.readAllBytes(saved.backup()));
        assertTrue(saved.backup().getFileName().toString().startsWith("orients_hotpot_chronicles.json.broken-"));
        assertEquals(37, Config.particleIntensity());
        assertTrue(Files.readString(file).contains("particle_intensity"));
    }
}
