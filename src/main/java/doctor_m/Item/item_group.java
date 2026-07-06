package doctor_m.Item;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import static doctor_m.Item.items.*;
import static doctor_m.module.creativity.creativity_items.*;

public class item_group {
public static void registerItems() {
            Registry.register(Registries.ITEM_GROUP, id("doctor_m_items"),
                    FabricItemGroup.builder()
                            .icon(() -> new ItemStack(TIME_KEY))
                            .displayName(Text.translatable("itemGroup.doctor_m"))
                            .entries((context, entries) -> {
                                entries.add(TIME_KEY);
                                entries.add(POCKET_WATCH);
                                entries.add(RELIC_GEM);
                                entries.add(ETERNAL_CRYSTAL);
                                entries.add(DE_MAT_GUN);
                                entries.add(RASSILON_KEY);
                                entries.add(OXYGEN_CHARGER_ITEM);
                                entries.add(TYPE_103_SPAWN);
                                entries.add(EVEREYE_SPAWN);
                                entries.add(OXYGEN_TANK);
                                entries.add(TLIPOCA_SCYTHE);
                            })
                            .build());
        }

        private static Identifier id(String path) {
            return new Identifier("doctor_m", path);
        }
}
