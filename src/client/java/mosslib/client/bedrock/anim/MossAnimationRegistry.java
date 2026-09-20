package mosslib.client.bedrock.anim;

import mosslib.MossLib;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;

import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

@Environment(EnvType.CLIENT)
public final class MossAnimationRegistry implements SimpleSynchronousResourceReloadListener {

    private static final MossAnimationRegistry INSTANCE = new MossAnimationRegistry();

    /** "namespace:path" -> MossAnimation。path 里可以带 "/"，比如 "toyota_rotor/spin"。 */
    private final Map<Identifier, MossAnimation> animations = new HashMap<>();

    private MossAnimationRegistry() {
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(this);
    }

    public static MossAnimationRegistry getInstance() {
        return INSTANCE;
    }

    public MossAnimation get(Identifier id) {
        return animations.get(id);
    }

    @Override
    public Identifier getFabricId() {
        return MossLib.id("bedrock_animations");
    }

    @Override
    public void reload(ResourceManager manager) {
        animations.clear();

        var found = manager.findResources("bedrock",
                p -> p.getPath().endsWith(".animation.json"));

        for (Identifier rawId : found.keySet()) {
            try (var in = manager.getResource(rawId).orElseThrow().getInputStream()) {
                var reader = new InputStreamReader(in);

                // 文件名从 "bedrock/xxx.animation.json" 提取出 "xxx"
                String path = rawId.getPath();
                String baseName = path.substring("bedrock/".length(),
                        path.length() - ".animation.json".length());

                Map<String, MossAnimation> parsed = MossAnimationLoader.loadAll(reader);
                for (Map.Entry<String, MossAnimation> e : parsed.entrySet()) {
                    // "animation.toyota_rotor.spin" -> 只保留最后一段 "spin"
                    String animName = e.getKey();
                    if (animName.startsWith("animation.")) {
                        animName = animName.substring("animation.".length());
                    }
                    int dot = animName.lastIndexOf('.');
                    if (dot >= 0) animName = animName.substring(dot + 1);

                    Identifier key = Identifier.of(rawId.getNamespace(),
                            baseName + "/" + animName);

                    animations.put(key, e.getValue());
                }
            } catch (Exception ex) {
                MossLib.LOGGER.error("Failed to load animation {}", rawId, ex);
            }
        }

        MossLib.LOGGER.info("Loaded {} bedrock animations", animations.size());
    }
}