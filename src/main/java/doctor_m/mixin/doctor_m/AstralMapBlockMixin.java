package doctor_m.mixin.doctor_m;

import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.amble.ait.core.blocks.AstralMapBlock;
import net.minecraft.world.gen.structure.Structure;

import java.util.HashSet;
import java.util.Set;

@Mixin(AstralMapBlock.class)
public class AstralMapBlockMixin {

    private static final Identifier BLACKLIST_TAG = new Identifier("doctor_m", "astral_map_blacklist");

    @Inject(
            method = "handleStructureRequest",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )

    private static void onHandleStructureRequest(ServerPlayerEntity player, Identifier target, CallbackInfo ci) {
        ServerWorld world = player.getServerWorld();
        Registry<Structure> registry = world.getRegistryManager().get(RegistryKeys.STRUCTURE);
        RegistryKey<Structure> key = RegistryKey.of(RegistryKeys.STRUCTURE, target);
        var optional = registry.getEntry(key);

        if (optional.isEmpty()) {
            return;
        }

        RegistryEntry<Structure> entry = optional.get();

        if (isInBlacklist(registry, entry, new HashSet<>())) {
            player.sendMessage(Text.translatable("block.ait.astral_map.finder.structure_not_found"), false);
            ci.cancel();
        }
    }

    private static boolean isInBlacklist(Registry<Structure> registry, RegistryEntry<Structure> entry, Set<TagKey<Structure>> visited) {
        TagKey<Structure> blacklistTag = TagKey.of(RegistryKeys.STRUCTURE, BLACKLIST_TAG);
        var tagOptional = registry.getEntryList(blacklistTag);
        if (tagOptional.isEmpty()) {
            return false;
        }

        RegistryEntryList.Named<Structure> blacklist = tagOptional.get();

        for (RegistryEntry<Structure> blacklistEntry : blacklist) {

            if (blacklistEntry.getKey().isPresent() && blacklistEntry.getKey().get().getValue().getPath().startsWith("#")) {

            }
        }

        if (blacklist.contains(entry)) {
            return true;
        }

        for (RegistryEntry<Structure> blacklistEntry : blacklist) {
            var tagKey = blacklistEntry.getKey()
                    .map(key -> TagKey.of(RegistryKeys.STRUCTURE, key.getValue()))
                    .orElse(null);

            if (tagKey != null && !visited.contains(tagKey)) {
                visited.add(tagKey);
                var nestedTag = registry.getEntryList(tagKey);
                if (nestedTag.isPresent()) {
                    for (RegistryEntry<Structure> nestedEntry : nestedTag.get()) {
                        if (nestedEntry.matchesKey(entry.getKey().get())) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }
}