package doctor_m.Item.data_item;

import dev.amble.ait.core.item.KeyItem;
import doctor_m.Item.Authorizable;
import doctor_m.util.tooltip.TooltipHelper;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class KeyofRassilonItem extends KeyItem {

    public KeyofRassilonItem(Settings settings) {
        super(settings, Protocols.SNAP, Protocols.HAIL, Protocols.PERCEPTION, Protocols.SKELETON);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        // 客户端不处理逻辑
        if (world.isClient) {
            return TypedActionResult.pass(player.getStackInHand(hand));
        }

        // 只有潜行时才触发授权切换
        if (!player.isSneaking()) {
            return TypedActionResult.pass(player.getStackInHand(hand));
        }

        // 获取主手和副手的物品
        ItemStack mainHand = player.getMainHandStack();
        ItemStack offHand = player.getOffHandStack();

        // 判断钥匙在哪个手，以及另一只手拿的是什么
        ItemStack keyStack = null;
        ItemStack targetStack = null;

        if (mainHand.getItem() instanceof KeyofRassilonItem && offHand.getItem() instanceof Authorizable) {
            keyStack = mainHand;
            targetStack = offHand;
        } else if (offHand.getItem() instanceof KeyofRassilonItem && mainHand.getItem() instanceof Authorizable) {
            keyStack = offHand;
            targetStack = mainHand;
        }

        // 如果目标物品为空（即副手没有实现 Authorizable 的物品），静默返回（无提示、无声音）
        if (targetStack == null) {
            return TypedActionResult.pass(player.getStackInHand(hand));
        }

        // 此时 targetStack 一定实现了 Authorizable
        Authorizable authorizable = (Authorizable) targetStack.getItem();

        // 切换授权状态
        boolean current = authorizable.isAuthorized(targetStack);
        boolean newState = !current;
        authorizable.setAuthorized(targetStack, newState);

        // 播放对应的音效（只有成功切换才播放）
        if (newState) {
            world.playSound(null, player.getBlockPos(), authorizable.getAuthorizeSound(),
                    SoundCategory.PLAYERS, 1.0f, 1.0f);
        } else {
            world.playSound(null, player.getBlockPos(), authorizable.getRevokeSound(),
                    SoundCategory.PLAYERS, 1.0f, 1.0f);
        }

        // 调用额外回调（粒子等）
        authorizable.onAuthorizationChanged(player, targetStack, newState);

        // 不消耗钥匙耐久（默认返回 success 不消耗）
        return TypedActionResult.success(player.getStackInHand(hand));
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        Text longDescription = Text.translatable("message.doctor_m.key_of_rassilon.tooltip.line");
        TooltipHelper.addWrappedTooltip(tooltip, longDescription);
        tooltip.add(Text.translatable("message.doctor_m.tip.not.done"));
        super.appendTooltip(stack, world, tooltip, context);
    }
}