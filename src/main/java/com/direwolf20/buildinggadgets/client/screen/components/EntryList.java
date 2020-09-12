package com.direwolf20.buildinggadgets.client.screen.components;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.widget.list.ExtendedList;
import net.minecraft.client.gui.widget.list.ExtendedList.AbstractListEntry;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.client.gui.ScrollPanel;
import org.lwjgl.opengl.GL11;

import static org.lwjgl.opengl.GL11.*;

public class EntryList<E extends AbstractListEntry<E>> extends ExtendedList<E> {

    public static final int SCROLL_BAR_WIDTH = 6;

    public EntryList(int left, int top, int width, int height, int slotHeight) {
        super(Minecraft.getInstance(), width, height, top, top + height, slotHeight);
        // Set left x and right x, somehow MCP gave it a weird name
        this.setLeftPos(left);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float partialTicks) {
        glEnable(GL_SCISSOR_TEST);
        double guiScaleFactor = Minecraft.getInstance().getMainWindow().getGuiScaleFactor();

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor((int)(getLeft()  * guiScaleFactor),
                (int)(Minecraft.getInstance().getMainWindow().getFramebufferHeight() - (getBottom() * guiScaleFactor)),
                (int)(width * guiScaleFactor),
                (int)(height * guiScaleFactor));

        renderParts(matrices, mouseX, mouseY, partialTicks);
        glDisable(GL_SCISSOR_TEST);
    }

    // Copied and modified from AbstractLists#render(int, int, float)
    private void renderParts(MatrixStack matrices, int mouseX, int mouseY, float partialTicks) {
        renderBackground(matrices);
        RenderSystem.disableLighting();
        RenderSystem.disableFog();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();

        renderContentBackground(matrices, tessellator, bufferbuilder);

        int k = getRowLeft();
        int l = getTop() + 4 - (int) getScrollAmount();
        renderHeader(matrices, k, l, tessellator);

        renderList(matrices, k, l, mouseX, mouseY, partialTicks);
        RenderSystem.disableDepthTest();

        int j1 = getMaxScroll();
        if (j1 > 0) {
            int k1 = (int) ((float) ((getBottom() - getTop()) * (getBottom() - getTop())) / (float) getMaxPosition());
            k1 = MathHelper.clamp(k1, 32, getBottom() - getTop() - 8);
            int l1 = (int) getScrollAmount() * (getBottom() - getTop() - k1) / j1 + getTop();
            if (l1 < getTop()) {
                l1 = getTop();
            }
            int x1 = getScrollbarPosition();
            int x2 = x1 + 6;

            RenderSystem.disableTexture();
            bufferbuilder.begin(GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
            bufferbuilder.pos(x1, getBottom(), 0.0D).color(0, 0, 0, 255).endVertex();
            bufferbuilder.pos(x2, getBottom(), 0.0D).color(0, 0, 0, 255).endVertex();
            bufferbuilder.pos(x2, getTop(), 0.0D).color(0, 0, 0, 255).endVertex();
            bufferbuilder.pos(x1, getTop(), 0.0D).color(0, 0, 0, 255).endVertex();

            bufferbuilder.pos(x1, (l1 + k1), 0.0D).color(128, 128, 128, 255).endVertex();
            bufferbuilder.pos(x2, (l1 + k1), 0.0D).color(128, 128, 128, 255).endVertex();
            bufferbuilder.pos(x2, l1, 0.0D).color(128, 128, 128, 255).endVertex();
            bufferbuilder.pos(x1, l1, 0.0D).color(128, 128, 128, 255).endVertex();

            bufferbuilder.pos(x1, (l1 + k1 - 1), 0.0D).color(192, 192, 192, 255).endVertex();
            bufferbuilder.pos((x2 - 1), (l1 + k1 - 1), 0.0D).color(192, 192, 192, 255).endVertex();
            bufferbuilder.pos((x2 - 1), l1, 0.0D).color(192, 192, 192, 255).endVertex();
            bufferbuilder.pos(x1, l1, 0.0D).color(192, 192, 192, 255).endVertex();
            tessellator.draw();
        }

        renderDecorations(matrices, mouseX, mouseX);
        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
    }

    protected void renderContentBackground(MatrixStack matrices, Tessellator tessellator, BufferBuilder bufferbuilder) {
        fillGradient(matrices, getLeft(), getTop(), getRight(), getBottom(), 0xC0101010, 0xD0101010);
    }

    @Override
    protected void renderBackground(MatrixStack p_230433_1_) {
        super.renderBackground(p_230433_1_);
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        setDragging(true);
        super.mouseClicked(x, y, button);
        return isMouseOver(x, y);
    }

    @Override
    public boolean mouseReleased(double x, double y, int button) {
        setDragging(false);
        return super.mouseReleased(x, y, button);
    }

    @Override
    public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        if (super.mouseDragged(x, y, button, dx, dy))
            return true;

        // Dragging elements in panel
        if (isMouseOver(x, y)) {
            setScrollAmount(getScrollAmount() - dy);
        }
        return true;
    }

    // Copied from AbstractList#getMaxScroll because it is private
    public final int getMaxScroll() {
        return Math.max(0, this.getMaxPosition() - (this.getBottom() - this.getTop() - 4));
    }
}
