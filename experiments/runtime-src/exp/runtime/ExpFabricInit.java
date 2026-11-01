package exp.runtime;

import net.fabricmc.api.ModInitializer;

public final class ExpFabricInit implements ModInitializer {
    @Override public void onInitialize() { ExpRuntime.init("fabric"); }
}
