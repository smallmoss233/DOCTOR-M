package doctor_m.client.gui;

import doctor_m.handler.TimeKey.TimeKeyFunction;
import doctor_m.network.TimeKeyActiveNetwork;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class TimeKeyActiveScreen extends Screen {
    private final PlayerEntity player;

    public TimeKeyActiveScreen(PlayerEntity player) {
        super(Text.translatable("gui.doctor_m.time_key.active.title"));
        this.player = player;
    }

    @Override
    protected void init() {
        if (TimeKeyFunction.getTimeKeyStack(player).isEmpty()) {
            this.close();
            return;
        }

        int cx = this.width / 2;
        int cy = this.height / 2;

        this.addDrawableChild(ButtonWidget.builder(
                buildGameModeText(),
                btn -> send(0)
        ).position(cx - 80, cy - 28).size(160, 22).build());

        this.addDrawableChild(ButtonWidget.builder(
                buildDifficultyText(),
                btn -> send(1)
        ).position(cx - 80, cy - 2).size(160, 22).build());

        // ★ 新增：任意传送
        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("gui.doctor_m.time_key.active.teleport").formatted(Formatting.AQUA),
                btn -> MinecraftClient.getInstance().setScreen(new TimeKeyTeleportScreen(player))
        ).position(cx - 80, cy + 24).size(160, 22).build());

        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("gui.doctor_m.vm.close"),
                btn -> this.close()
        ).position(cx - 40, cy + 54).size(80, 20).build());
    }

    @Override
    public void tick() {
        super.tick();
        if (TimeKeyFunction.getTimeKeyStack(player).isEmpty()) {
            this.close();
            return;
        }
        // 实时刷新当前状态
        if (this.children().size() >= 2 && this.children().get(0) instanceof ButtonWidget btn) {
            btn.setMessage(buildGameModeText());
        }
        if (this.children().size() >= 2 && this.children().get(1) instanceof ButtonWidget btn) {
            btn.setMessage(buildDifficultyText());
        }
    }

    private Text buildGameModeText() {
        var client = MinecraftClient.getInstance();
        var mode = client.interactionManager != null ? client.interactionManager.getCurrentGameMode() : null;
        String modeName = mode != null ? Text.translatable("selectWorld.gameMode." + mode.getName()).getString() : "?";
        return Text.translatable("gui.doctor_m.time_key.active.gamemode")
                .append(" [").append(Text.literal(modeName).formatted(Formatting.YELLOW)).append("]");
    }

    private Text buildDifficultyText() {
        var client = MinecraftClient.getInstance();
        var diff = client.world != null ? client.world.getDifficulty() : null;
        String diffName = diff != null ? Text.translatable("options.difficulty." + diff.getName()).getString() : "?";
        return Text.translatable("gui.doctor_m.time_key.active.difficulty")
                .append(" [").append(Text.literal(diffName).formatted(Formatting.YELLOW)).append("]");
    }

    private void send(int abilityId) {
        var buf = PacketByteBufs.create();
        buf.writeInt(abilityId);
        ClientPlayNetworking.send(TimeKeyActiveNetwork.ACTIVE_ABILITY, buf);
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        this.renderBackground(ctx);
        super.render(ctx, mx, my, delta);
        ctx.drawCenteredTextWithShadow(this.textRenderer, this.title,
                this.width / 2, this.height / 2 - 55, 0xFFFFFF);
    }

    @Override
    public boolean shouldPause() { return false; }
}