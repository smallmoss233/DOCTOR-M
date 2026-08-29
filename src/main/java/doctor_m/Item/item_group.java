package doctor_m.Item;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import static doctor_m.Item.items.*;
import static doctor_m.module.creativity.CreativityItems.*;

public class item_group {
public static void registerItems() {
            Registry.register(Registries.ITEM_GROUP, id("doctor_m_items"),
                    FabricItemGroup.builder()
                            .icon(() -> new ItemStack(KEY_TO_TIME))
                            .displayName(Text.translatable("itemGroup.doctor_m"))
                            .entries((context, entries) -> {
                                entries.add(KEY_TO_TIME);
                                entries.add(POCKET_WATCH);
                                entries.add(RELIC_GEM);
                                entries.add(ETERNAL_CRYSTAL);
                                entries.add(TRACER);
                                entries.add(DE_MAT_GUN);
                                entries.add(KEY_OF_RASSILON);
                                entries.add(SEAL_OF_THE_HIGH_COUNCIL);
                                entries.add(OXYGEN_CHARGER);
                                entries.add(UNDERWATER_OXYGEN_GENERATOR);
                                entries.add(COFFEE_MACHINE);
                                entries.add(TOYOTA_SPINNING_ROTOR);
                                entries.add(EYE_OF_HARMONY_OBELISK);
                                entries.add(TYPE_103_SPAWN);
                                entries.add(MARIAN_JIN_SPAWN);
                                entries.add(OXYGEN_TANK);
                                entries.add(SHIELD_CORE);
                                entries.add(FORCE_FIELD_SHIELD);
                                entries.add(VORTEX_MANIPULATOR);
                                entries.add(TLIPOCA_SCYTHE);
                                entries.add(STCA);
                                entries.add(STCH);
                                entries.add(STCL);
                                entries.add(TOYMAKER_HAMMER);
                                entries.add(ENERGY_UPGRADE_MODULE);
                                entries.add(REGENERATION_MODULE);
                                entries.add(DOLL_JIN_MARY);
                                entries.add(DOLL_SMALLMOSS_OLD);
                                entries.add(DOLL_TC020);
                                entries.add(DOLL_ASDJDFK);
                                entries.add(DOLL_SIGEERTE);
                                entries.add(DOLL_TSINAFS_BCIM);
                                entries.add(DOLL_ASNIT_PNQING);
                                entries.add(DOLL_TIANX);
                                entries.add(DOLL_KILIN_MUS);
                                entries.add(DOLL_JOGGEST);
                            })
                            .build());
        }

        private static Identifier id(String path) {
            return new Identifier("doctor_m", path);
        }
}
