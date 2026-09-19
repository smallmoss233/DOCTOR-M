package mosslib.client.bedrock;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class MossBedrockReloader implements SimpleSynchronousResourceReloadListener {

    public static void register() {
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES)
                .registerReloadListener(new MossBedrockReloader());
    }

    @Override
    public Identifier getFabricId() {
        return new Identifier("doctor_m", "moss_bedrock");
    }

    @Override
    public void reload(ResourceManager manager) {
        MossBedrock.clearCache();
        MossBedrockRenderer.clearModelCache();
    }
}