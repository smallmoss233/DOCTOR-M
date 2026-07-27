package doctor_m.Item.data_weapon;

import dev.amble.ait.module.gun.core.item.BaseGunItem;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class de_mat_gun extends BaseGunItem {

    public de_mat_gun(Settings settings) {
        super(settings);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        // 客户端逻辑已移至 DoctorMClient 的 ClientTickEvents
        // 此处可保留服务端逻辑（如冷却同步等）
    }

    @Override
    public void tryShoot(World world, Entity entity, boolean selected) {
        // 空实现，射击完全由客户端事件驱动
    }

    @Override
    public double getMaxAmmo() {
        return 128;
    }

    @Override
    public int getCooldown() {
        return 60;
    }

    @Override
    public float getAimDeviation(boolean isAds) {
        return isAds ? 0.15f : 1.2f;
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable("message.doctor_m.de_mat_gun.tooltip.line1")
                .formatted(Formatting.WHITE, Formatting.BOLD));
        MutableText line2 = Text.translatable("message.doctor_m.de_mat_gun.tooltip.line2");
        line2.setStyle(Style.EMPTY.withColor(Formatting.GRAY).withItalic(true));
        tooltip.add(line2);

        super.appendTooltip(stack, world, tooltip, context);
        tooltip.add(Text.translatable("message.doctor_m.tip.not.done"));
    }
}