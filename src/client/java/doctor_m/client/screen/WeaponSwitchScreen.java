package doctor_m.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import doctor_m.DOCTORMClient;
import doctor_m.network.SwitchWeaponNetwork;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import org.joml.Matrix4f;

public class WeaponSwitchScreen extends Screen {

    private static final int RADIUS = 80;
    private static final int SECTORS = 3;
    private int openTicks = 0; // 用于防止刚打开就瞬间关闭

    public WeaponSwitchScreen() {
        super(Text.literal("Weapon Switch"));
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        openTicks++;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        double dx = mouseX - centerX;
        double dy = mouseY - centerY;
        double distance = Math.sqrt(dx * dx + dy * dy);
        int hoveredSector = -1;

        if (distance <= RADIUS) {
            double angle = Math.toDegrees(Math.atan2(dy, dx)) + 90;
            if (angle < 0) angle += 360;
            hoveredSector = (int) (angle / (360.0 / SECTORS)) % SECTORS;
        }

        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        for (int i = 0; i < SECTORS; i++) {
            boolean isHovered = (i == hoveredSector);
            int color = isHovered ? 0xFFFFFFFF : 0x80000000;

            float r = ((color >> 16) & 0xFF) / 255.0f;
            float g = ((color >> 8) & 0xFF) / 255.0f;
            float b = (color & 0xFF) / 255.0f;
            float a = ((color >> 24) & 0xFF) / 255.0f;

            buffer.begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
            buffer.vertex(matrix, centerX, centerY, 0).color(r, g, b, a).next();

            int startAngle = (int) (i * (360.0 / SECTORS));
            int endAngle = (int) ((i + 1) * (360.0 / SECTORS));

            for (int angle = startAngle; angle <= endAngle; angle += 5) {
                double rad = Math.toRadians(angle - 90);
                float x = (float) (centerX + Math.cos(rad) * RADIUS);
                float y = (float) (centerY + Math.sin(rad) * RADIUS);
                buffer.vertex(matrix, x, y, 0).color(r, g, b, a).next();
            }

            BufferRenderer.drawWithGlobalProgram(buffer.end());
        }

        RenderSystem.enableCull();
        RenderSystem.disableBlend();

        if (hoveredSector != -1) {
            String text = getWeaponName(hoveredSector);
            context.drawCenteredTextWithShadow(this.textRenderer, text, centerX, centerY - 10, 0xFFFFFF);
        }
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        // 只有过了 4 个 tick，且确实是 I 键松开，才允许关闭
        if (openTicks > 4 && DOCTORMClient.SWITCH_WEAPON_KEY.matchesKey(keyCode, scanCode)) {
            if (this.client != null) {
                double centerX = this.width / 2.0;
                double centerY = this.height / 2.0;
                double mouseX = this.client.mouse.getX() * (double)this.client.getWindow().getScaledWidth() / (double)this.client.getWindow().getWidth();
                double mouseY = this.client.mouse.getY() * (double)this.client.getWindow().getScaledHeight() / (double)this.client.getWindow().getHeight();

                double dx = mouseX - centerX;
                double dy = mouseY - centerY;
                double distance = Math.sqrt(dx * dx + dy * dy);

                if (distance <= RADIUS) {
                    double angle = Math.toDegrees(Math.atan2(dy, dx)) + 90;
                    if (angle < 0) angle += 360;
                    int sector = (int) (angle / (360.0 / SECTORS)) % SECTORS;

                    PacketByteBuf buf = PacketByteBufs.create();
                    buf.writeInt(sector);
                    ClientPlayNetworking.send(SwitchWeaponNetwork.SWITCH_WEAPON_PACKET, buf);
                }
            }
            this.close();
            return true;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    private String getWeaponName(int sector) {
        return switch (sector) {
            case 0 -> "SAR-07H";
            case 1 -> "SAR-08A";
            case 2 -> "SAR-09L";
            default -> "";
        };
    }
}