package doctor_m;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import static doctor_m.Item.Items.PHOTO;
import static doctor_m.Weapon.Weapon.DE_MAT_GUN;
import static doctor_m.Weapon.Weapon.RASSILON_KEY;

public class DOCTORMItem_group {
public static void registerItems() {
            Registry.register(Registries.ITEM_GROUP, id("doctor_m_items"),
                    FabricItemGroup.builder()
                            .icon(() -> new ItemStack(RASSILON_KEY))
                            .displayName(Text.translatable("itemGroup.doctor_m"))
                            .entries((context, entries) -> {
                                entries.add(DE_MAT_GUN);
                                entries.add(RASSILON_KEY);
                                entries.add(PHOTO);
                            })
                            .build());
        }

        private static Identifier id(String path) {
            return new Identifier("doctor_m", path);
        }
}
