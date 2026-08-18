package doctor_m.client.gui;

import doctor_m.handler.KeytoTime.KeytoTimeCore;
import doctor_m.network.KeytoTimeActiveNetwork;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class KeytoTimeActiveScreen extends Screen {
    private final PlayerEntity player;

    public KeytoTimeActiveScreen(PlayerEntity player) {
        super(Text.translatable("gui.doctor_m.key_to_time.active.title"));
        this.player = player;
    }

    @Override
    protected void init() {
        if (KeytoTimeCore.getTimeKeyStack(player).isEmpty()) {
            this.close();
            return;
        }

        int cx = this.width / 2;
        int cy = this.height / 2;

        // 垂直排列，统一宽度 160，间距 26
        this.addDrawableChild(ButtonWidget.builder(
                buildGameModeText(),
                btn -> send(0)
        ).position(cx - 80, cy - 40).size(160, 22).build());

        this.addDrawableChild(ButtonWidget.builder(
                buildDifficultyText(),
                btn -> send(1)
        ).position(cx - 80, cy - 12).size(160, 22).build());

        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("gui.doctor_m.key_to_time.active.teleport").formatted(Formatting.AQUA),
                btn -> MinecraftClient.getInstance().setScreen(new KeytoTimeTeleportScreen(player))
        ).position(cx - 80, cy + 16).size(160, 22).build());

        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("gui.doctor_m.vm.close"),
                btn -> this.close()
        ).position(cx - 40, cy + 52).size(80, 20).build());
    }

    @Override
    public void tick() {
        super.tick();
        if (KeytoTimeCore.getTimeKeyStack(player).isEmpty()) {
            this.close();
            return;
        }
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
        return Text.translatable("gui.doctor_m.key_to_time.active.gamemode")
                .append(" [").append(Text.literal(modeName).formatted(Formatting.YELLOW)).append("]");
    }

    private Text buildDifficultyText() {
        var client = MinecraftClient.getInstance();
        var diff = client.world != null ? client.world.getDifficulty() : null;
        String diffName = diff != null ? Text.translatable("options.difficulty." + diff.getName()).getString() : "?";
        return Text.translatable("gui.doctor_m.key_to_time.active.difficulty")
                .append(" [").append(Text.literal(diffName).formatted(Formatting.YELLOW)).append("]");
    }

    private void send(int abilityId) {
        var buf = PacketByteBufs.create();
        buf.writeInt(abilityId);
        ClientPlayNetworking.send(KeytoTimeActiveNetwork.ACTIVE_ABILITY, buf);
    }

    @Override
    public void renderBackground(DrawContext ctx) {}

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        int cx = this.width / 2;
        int cy = this.height / 2;

        // ===== Tooltip 风格背景框 =====
        int bgX = cx - 100;
        int bgY = cy - 72;
        int bgW = 200;
        int bgH = 148;
        int bgR = bgX + bgW;
        int bgB = bgY + bgH;

        ctx.fill(bgX, bgY, bgR, bgB, 0xF0100010);
        ctx.fill(bgX, bgY, bgR, bgY + 1, 0x505000FF);
        ctx.fill(bgX, bgB - 1, bgR, bgB, 0x505000FF);
        ctx.fill(bgX, bgY, bgX + 1, bgB, 0x5028007F);
        ctx.fill(bgR - 1, bgY, bgR, bgB, 0x5028007F);
        // ==============================

        ctx.drawCenteredTextWithShadow(this.textRenderer, this.title, cx, bgY + 8, 0xFFFFFF);
        ctx.fill(cx - 60, bgY + 22, cx + 60, bgY + 23, 0x30FFFFFF);

        super.render(ctx, mx, my, delta);
    }

    @Override
    public boolean shouldPause() { return false; }
}