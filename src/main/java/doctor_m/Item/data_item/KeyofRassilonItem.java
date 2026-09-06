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
        if (world.isClient) {
            return TypedActionResult.pass(player.getStackInHand(hand));
        }
        if (!player.isSneaking()) {
            return TypedActionResult.pass(player.getStackInHand(hand));
        }

        ItemStack mainHand = player.getMainHandStack();
        ItemStack offHand = player.getOffHandStack();
        ItemStack keyStack = null;
        ItemStack targetStack = null;

        if (mainHand.getItem() instanceof KeyofRassilonItem && offHand.getItem() instanceof Authorizable) {
            keyStack = mainHand;
            targetStack = offHand;
        } else if (offHand.getItem() instanceof KeyofRassilonItem && mainHand.getItem() instanceof Authorizable) {
            keyStack = offHand;
            targetStack = mainHand;
        }

        if (targetStack == null) {
            return TypedActionResult.pass(player.getStackInHand(hand));
        }

        Authorizable authorizable = (Authorizable) targetStack.getItem();

        boolean current = authorizable.isAuthorized(targetStack);
        boolean newState = !current;
        authorizable.setAuthorized(targetStack, newState);

        if (newState) {
            world.playSound(null, player.getBlockPos(), authorizable.getAuthorizeSound(),
                    SoundCategory.PLAYERS, 1.0f, 1.0f);
        } else {
            world.playSound(null, player.getBlockPos(), authorizable.getRevokeSound(),
                    SoundCategory.PLAYERS, 1.0f, 1.0f);
        }

        authorizable.onAuthorizationChanged(player, targetStack, newState);

        return TypedActionResult.success(player.getStackInHand(hand));
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        Text longDescription = Text.translatable("message.doctor_m.key_of_rassilon.tooltip.line");
        TooltipHelper.addWrappedTooltip(tooltip, longDescription);
        super.appendTooltip(stack, world, tooltip, context);
    }
}