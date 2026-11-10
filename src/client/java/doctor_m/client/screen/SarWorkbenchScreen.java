package doctor_m.client.screen;

import doctor_m.menu.SARGUI;
import doctor_m.recipe.AssemblyRecipe;
import doctor_m.recipe.ModRecipes;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;

import java.util.List;

public class SarWorkbenchScreen extends HandledScreen<SARGUI> {

    private List<AssemblyRecipe> recipeList = DefaultedList.of();
    private int selectedIndex = 0;
    private double scrollAmount = 0.0;

    private static final int COLOR_BORDER = 0xFF111111;
    private static final int COLOR_HIGHLIGHT = 0xFF00BFFF;
    private static final int COLOR_TEXT = 0xFF000000;
    private static final int COLOR_TEXT_DIM = 0xFF444444;
    private static final int COLOR_SCROLL_BAR_BG = 0x66000000;
    private static final int COLOR_SCROLL_BAR_FG = 0xFF00BFFF;

    private int btnX, btnY, btnW, btnH;
    private int prevX, prevY, prevW, prevH;
    private int nextX, nextY, nextW, nextH;

    public SarWorkbenchScreen(SARGUI handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 320;
        this.backgroundHeight = 180;
    }

    @Override
    protected void init() {
        super.init();

        if (this.client != null && this.client.world != null) {
            this.recipeList = this.client.world.getRecipeManager().listAllOfType(ModRecipes.ASSEMBLY_TYPE);
        }

        int leftX = (this.width - this.backgroundWidth) / 2;
        int topY = (this.height - this.backgroundHeight) / 2;

        prevX = leftX + 170; prevY = topY + 15; prevW = 20; prevH = 20;
        nextX = leftX + 280; nextY = topY + 15; nextW = 20; nextH = 20;
        btnX = leftX + 185;  btnY = topY + 140; btnW = 100; btnH = 20;
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        drawAutoScaledText(context, this.title, 15, 15, 125, COLOR_TEXT);
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        context.fillGradient(x, y, x + backgroundWidth, y + backgroundHeight, 0xFFEEEEEE, 0xFFA0A0A0);
        context.drawBorder(x, y, backgroundWidth, backgroundHeight, COLOR_BORDER);

        context.fillGradient(x + 10, y + 10, x + 150, y + 170, 0xFFEEEEEE, 0xFFA0A0A0);
        context.drawBorder(x + 10, y + 10, 140, 160, COLOR_BORDER);

        context.fillGradient(x + 160, y + 10, x + 310, y + 170, 0xFFEEEEEE, 0xFFA0A0A0);
        context.drawBorder(x + 160, y + 10, 150, 160, COLOR_BORDER);

        drawFloatingCubes(context, x + 280, y + 15);

        if (recipeList.isEmpty() || selectedIndex >= recipeList.size()) {
            drawAutoScaledText(context, Text.translatable("gui.doctor_m.sar_workbench.no_recipes"), x + 170, y + 90, 140, COLOR_TEXT_DIM);
            return;
        }

        AssemblyRecipe currentRecipe = recipeList.get(selectedIndex);

        context.getMatrices().push();
        context.getMatrices().translate(x + 80, y + 85, 0);
        context.getMatrices().scale(4.0F, 4.0F, 1.0F);
        context.drawItem(currentRecipe.getOutput(), -8, -8);
        context.getMatrices().pop();

        Text outputName = currentRecipe.getOutput().getName();
        int nameWidth = this.textRenderer.getWidth(outputName);
        int drawX = x + 80 - Math.min(nameWidth, 125) / 2;
        drawAutoScaledText(context, outputName, drawX, y + 120, 125, 0xFF00FFFF);

        String pageText = (selectedIndex + 1) + " / " + recipeList.size();
        context.drawText(this.textRenderer, pageText, x + 235 - this.textRenderer.getWidth(pageText) / 2, y + 21, COLOR_TEXT, true);

        drawAutoScaledText(context, Text.translatable("gui.doctor_m.sar_workbench.materials"), x + 165, y + 50, 140, COLOR_TEXT_DIM);

        int listTop = y + 65;
        int listBottom = y + 135;
        int listHeight = listBottom - listTop;

        context.enableScissor(x + 160, listTop, x + 270, listBottom);

        int materialY = listTop - (int) scrollAmount;
        List<Ingredient> inputs = currentRecipe.getInputs();
        List<Integer> counts = currentRecipe.getInputCounts();

        for (int i = 0; i < inputs.size(); i++) {
            Ingredient ingredient = inputs.get(i);
            int reqCount = counts.get(i);

            ItemStack[] stacks = ingredient.getMatchingStacks();
            if (stacks.length > 0) {
                ItemStack inputStack = stacks[0];

                int playerHas = 0;
                if (this.client != null && this.client.player != null) {
                    for (int j = 0; j < this.client.player.getInventory().size(); j++) {
                        ItemStack stackInInv = this.client.player.getInventory().getStack(j);
                        if (ingredient.test(stackInInv)) {
                            playerHas += stackInInv.getCount();
                        }
                    }
                }

                boolean hasEnough = playerHas >= reqCount;
                int textColor = hasEnough ? COLOR_TEXT : 0xFFE50000;

                context.drawItem(inputStack, x + 165, materialY);

                Text reqText = Text.literal(inputStack.getName().getString() + " x" + reqCount);
                drawAutoScaledText(context, reqText, x + 185, materialY + 4, 80, textColor);

                materialY += 20;
            }
        }

        context.disableScissor();

        int totalHeight = inputs.size() * 20;
        if (totalHeight > listHeight) {
            int scrollBarX = x + 299;
            int scrollBarWidth = 4;

            context.fill(scrollBarX, listTop, scrollBarX + scrollBarWidth, listBottom, COLOR_SCROLL_BAR_BG);

            float scrollProgress = (float) scrollAmount / (totalHeight - listHeight);
            if (scrollProgress < 0) scrollProgress = 0;
            if (scrollProgress > 1) scrollProgress = 1;

            int thumbHeight = Math.max(10, (int) (((float) listHeight / totalHeight) * listHeight));
            int thumbY = listTop + (int) (scrollProgress * (listHeight - thumbHeight));

            context.fill(scrollBarX, thumbY, scrollBarX + scrollBarWidth, thumbY + thumbHeight, COLOR_SCROLL_BAR_FG);
        }

        drawCustomButton(context, prevX, prevY, prevW, prevH, Text.literal("<"), mouseX, mouseY);
        drawCustomButton(context, nextX, nextY, nextW, nextH, Text.literal(">"), mouseX, mouseY);
        drawCustomButton(context, btnX, btnY, btnW, btnH, Text.translatable("gui.doctor_m.sar_workbench.craft"), mouseX, mouseY);
    }

    private void drawFloatingCubes(DrawContext context, int x, int y) {
        long currentTime = System.currentTimeMillis();
        float time = (currentTime % 3000L) / 3000.0F;

        float sin1 = (float) Math.sin(time * Math.PI * 2);
        float sin2 = (float) Math.sin(time * Math.PI * 2 + Math.PI);

        int centerY = y + 75;
        int amplitude = 55;

        int y1 = centerY + (int) (sin1 * amplitude);
        int y2 = centerY + (int) (sin2 * amplitude);

        int size = 6;

        drawMiniCube(context, x + 4, y1, size);
        drawMiniCube(context, x + 16, y2, size);
    }

    private void drawMiniCube(DrawContext context, int x, int y, int size) {
        context.fillGradient(x, y, x + size, y + size, 0xFFB0B0B0, 0xFF707070);
        context.fill(x, y, x + size, y + 1, 0xFFE0E0E0);
        context.fill(x, y, x + 1, y + size, 0xFFE0E0E0);
        context.fill(x, y + size - 1, x + size, y + size, 0xFF333333);
        context.fill(x + size - 1, y, x + size, y + size, 0xFF333333);
    }

    private void drawCustomButton(DrawContext context, int x, int y, int w, int h, Text text, int mouseX, int mouseY) {
        boolean hovered = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;

        int bgColor = hovered ? 0xFFFFFFFF : 0xFFC4C4C4;
        context.fill(x, y, x + w, y + h, bgColor);

        int borderColor = hovered ? COLOR_HIGHLIGHT : COLOR_BORDER;
        context.drawBorder(x, y, w, h, borderColor);

        int textColor = hovered ? COLOR_HIGHLIGHT : COLOR_TEXT;

        int textWidth = this.textRenderer.getWidth(text);
        float scale = 1.0F;
        if (textWidth > w - 6) {
            scale = (float) (w - 6) / textWidth;
            textWidth = w - 6;
        }

        context.getMatrices().push();
        context.getMatrices().translate(x + (w - textWidth) / 2.0F, y + (h - 8) / 2.0F + 1, 0);
        context.getMatrices().scale(scale, scale, 1.0F);
        context.drawText(this.textRenderer, text, 0, 0, textColor, true);
        context.getMatrices().pop();
    }

    private void drawAutoScaledText(DrawContext context, Text text, int x, int y, int maxWidth, int color) {
        int width = this.textRenderer.getWidth(text);
        if (width > maxWidth) {
            float scale = (float) maxWidth / width;
            context.getMatrices().push();
            context.getMatrices().translate(x, y, 0);
            context.getMatrices().scale(scale, scale, 1.0F);
            context.drawText(this.textRenderer, text, 0, 0, color, true);
            context.getMatrices().pop();
        } else {
            context.drawText(this.textRenderer, text, x, y, color, true);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (mouseX >= prevX && mouseX <= prevX + prevW && mouseY >= prevY && mouseY <= prevY + prevH) {
                if (selectedIndex > 0) selectedIndex--;
                scrollAmount = 0;
                return true;
            }
            if (mouseX >= nextX && mouseX <= nextX + nextW && mouseY >= nextY && mouseY <= nextY + nextH) {
                if (selectedIndex < recipeList.size() - 1) selectedIndex++;
                scrollAmount = 0;
                return true;
            }
            if (mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH) {
                if (this.client != null && this.client.interactionManager != null) {
                    if (!recipeList.isEmpty() && selectedIndex >= 0 && selectedIndex < recipeList.size()) {
                        int recipeHash = recipeList.get(selectedIndex).getId().toString().hashCode();
                        this.client.interactionManager.clickButton(this.handler.syncId, recipeHash);
                    }
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        int listTop = y + 65;
        int listBottom = y + 135;

        if (mouseX >= x + 160 && mouseX <= x + 295 && mouseY >= listTop && mouseY <= listBottom) {
            if (!recipeList.isEmpty() && selectedIndex < recipeList.size()) {
                AssemblyRecipe currentRecipe = recipeList.get(selectedIndex);
                int totalHeight = currentRecipe.getInputs().size() * 20;
                int listHeight = listBottom - listTop;
                int maxScroll = Math.max(0, totalHeight - listHeight);

                if (maxScroll > 0) {
                    scrollAmount -= amount * 15;
                    if (scrollAmount < 0) scrollAmount = 0;
                    if (scrollAmount > maxScroll) scrollAmount = maxScroll;
                    return true;
                }
            }
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }
}