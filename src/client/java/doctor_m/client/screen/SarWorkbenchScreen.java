package doctor_m.client.screen;

import doctor_m.menu.SARGUI;
import doctor_m.recipe.AssemblyRecipe;
import doctor_m.recipe.ModRecipes;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;

import java.util.List;

public class SarWorkbenchScreen extends HandledScreen<SARGUI> {

    // 客户端缓存配方列表
    private List<AssemblyRecipe> recipeList = DefaultedList.of();
    private int selectedIndex = 0; // 当前选中的配方索引

    public SarWorkbenchScreen(SARGUI handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 256; // 界面变宽以容纳左侧预览和右侧材料
        this.backgroundHeight = 200;
    }

    @Override
    protected void init() {
        super.init();

        // 加载所有配方
        if (this.client != null && this.client.world != null) {
            this.recipeList = this.client.world.getRecipeManager().listAllOfType(ModRecipes.ASSEMBLY_TYPE);
        }

        int leftX = (this.width - this.backgroundWidth) / 2;
        int topY = (this.height - this.backgroundHeight) / 2;

        // 1. 制造按钮 (底部居中偏右)
        this.addDrawableChild(ButtonWidget.builder(Text.literal("制造"), button -> {
            if (this.client != null && this.client.interactionManager != null) {
                // 发送当前选中的配方索引给服务端
                this.client.interactionManager.clickButton(this.handler.syncId, this.selectedIndex);
            }
        }).dimensions(leftX + 150, topY + 160, 90, 20).build());

        // 2. 上一个配方按钮 (左箭头)
        this.addDrawableChild(ButtonWidget.builder(Text.literal("<"), button -> {
            if (selectedIndex > 0) selectedIndex--;
        }).dimensions(leftX + 150, topY + 40, 20, 20).build());

        // 3. 下一个配方按钮 (右箭头)
        this.addDrawableChild(ButtonWidget.builder(Text.literal(">"), button -> {
            if (selectedIndex < recipeList.size() - 1) selectedIndex++;
        }).dimensions(leftX + 220, topY + 40, 20, 20).build());
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        // 绘制半透明黑色背景
        context.fill(x, y, x + backgroundWidth, y + backgroundHeight, 0xFF222222);
        // 绘制边框
        context.drawBorder(x, y, backgroundWidth, backgroundHeight, 0xFF888888);

        if (recipeList.isEmpty() || selectedIndex >= recipeList.size()) {
            context.drawText(this.textRenderer, "没有可用配方", x + 100, y + 80, 0xFFFFFF, false);
            return;
        }

        AssemblyRecipe currentRecipe = recipeList.get(selectedIndex);

        // --- 左侧：预览区域 ---
        context.fill(x + 10, y + 10, x + 110, y + 180, 0xFF333333); // 左底板
        // 绘制产物图标（放大显示）
        context.getMatrices().push();
        context.getMatrices().translate(x + 60, y + 80, 0);
        context.getMatrices().scale(3.0F, 3.0F, 1.0F);
        context.drawItem(currentRecipe.getOutput(), -8, -8);
        context.getMatrices().pop();

        // 显示产物名称
        Text outputName = currentRecipe.getOutput().getName();
        context.drawCenteredTextWithShadow(this.textRenderer, outputName, x + 60, y + 120, 0xFFFFFF);

        // --- 右侧：配方列表与材料区域 ---
        context.fill(x + 120, y + 10, x + 246, y + 180, 0xFF333333); // 右底板

        // 显示当前是第几个配方
        String pageText = (selectedIndex + 1) + " / " + recipeList.size();
        context.drawCenteredTextWithShadow(this.textRenderer, pageText, x + 195, y + 45, 0xFFFFFF);

        // 显示材料需求
        context.drawText(this.textRenderer, "所需材料:", x + 130, y + 80, 0xCCCCCC, false);
        int materialY = y + 95;
        for (Ingredient ingredient : currentRecipe.getInputs()) {
            ItemStack[] stacks = ingredient.getMatchingStacks();
            if (stacks.length > 0) {
                ItemStack inputStack = stacks[0];
                // 画出材料图标
                context.drawItem(inputStack, x + 130, materialY);
                // 画出材料名字和数量
                String reqText = inputStack.getName().getString() + " x" + inputStack.getCount();
                context.drawText(this.textRenderer, reqText, x + 150, materialY + 4, 0xFFFFFF, false);
                materialY += 20;
            }
        }

        // --- 底部：玩家背包区域 ---
        context.fill(x + 10, y + 184, x + 246, y + 190, 0xFF888888); // 分隔线
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);
    }
}