package doctor_m.module.Industry.data_Industry;

import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.recipe.Ingredient;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

import static doctor_m.module.Industry.Industry_items.STEEL_INGOT;

public class SteelArmorMaterial {
    public static final ArmorMaterial STEEL_ARMOR_MATERIAL = new ArmorMaterial() {
        private final int[] baseDurability = new int[]{13, 15, 16, 11}; // 靴子、护腿、胸甲、头盔的耐久基数
        private final int[] protectionValues = new int[]{2, 5, 6, 2}; // 同铁

        @Override
        public int getDurability(ArmorItem.Type type) {
            // 钻石的耐久乘数为 33，这里使用 33
            return baseDurability[type.getEquipmentSlot().getEntitySlotId()] * 33;
        }

        @Override
        public int getProtection(ArmorItem.Type type) {
            return protectionValues[type.getEquipmentSlot().getEntitySlotId()];
        }

        @Override
        public int getEnchantability() {
            return 10; // 同铁
        }

        @Override
        public SoundEvent getEquipSound() {
            return SoundEvents.ITEM_ARMOR_EQUIP_IRON;
        }

        @Override
        public Ingredient getRepairIngredient() {
            return Ingredient.ofItems(STEEL_INGOT);
        }

        @Override
        public String getName() {
            return "steel";
        }

        @Override
        public float getToughness() {
            return 0.0f; // 同铁
        }

        @Override
        public float getKnockbackResistance() {
            return 0.1f; // 下界合金的击退抗性
        }
    };
}
