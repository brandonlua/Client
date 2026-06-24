package net.minecraft.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import wtf.fentanyl.util.animations.Animation;
import wtf.fentanyl.util.animations.Direction;
import wtf.fentanyl.util.animations.impl.DecelerateAnimation;
import wtf.fentanyl.client.font.CFontRenderer;
import wtf.fentanyl.client.processes.FontProcess;
import wtf.fentanyl.util.render.RenderUtil;

import java.awt.*;

public class GuiButton extends Gui
{
    protected static final ResourceLocation buttonTextures = new ResourceLocation("textures/gui/widgets.png");
    protected int width;
    protected int height;
    public int xPosition;
    public int yPosition;
    public String displayString;
    public int id;
    public boolean enabled;
    public boolean visible;
    protected boolean hovered;

    private final Animation hoverAnimation = new DecelerateAnimation(300, 1, Direction.BACKWARDS);
    private static final Color PRIMARY_COLOR = new Color(228, 143, 255);
    private static final Color SECONDARY_COLOR = new Color(255, 113, 82);

    public GuiButton(int buttonId, int x, int y, String buttonText)
    {
        this(buttonId, x, y, 200, 20, buttonText);
    }

    public GuiButton(int buttonId, int x, int y, int widthIn, int heightIn, String buttonText)
    {
        this.width = 200;
        this.height = 20;
        this.enabled = true;
        this.visible = true;
        this.id = buttonId;
        this.xPosition = x;
        this.yPosition = y;
        this.width = widthIn;
        this.height = heightIn;
        this.displayString = buttonText;
    }

    protected int getHoverState(boolean mouseOver)
    {
        int i = 1;

        if (!this.enabled)
        {
            i = 0;
        }
        else if (mouseOver)
        {
            i = 2;
        }

        return i;
    }

    public void drawButton(Minecraft mc, int mouseX, int mouseY)
    {
        if (this.visible)
        {
            CFontRenderer fontRenderer = FontProcess.getFont("sans");

            int textWidth = fontRenderer.getStringWidth(this.displayString);
            int textY = this.yPosition + (this.height - fontRenderer.FONT_HEIGHT) / 2;

            this.hovered = mouseX >= this.xPosition && mouseY >= this.yPosition &&
                    mouseX < this.xPosition + this.width && mouseY < this.yPosition + this.height;

            GlStateManager.enableBlend();
            GlStateManager.blendFunc(770, 771);

            // Visible rounded background panel so buttons are easy to see.
            int bgAlpha = !this.enabled ? 70 : (this.hovered ? 160 : 110);
            RenderUtil.drawRoundedRect(this.xPosition, this.yPosition, this.width, this.height, 4,
                    new Color(22, 22, 30, bgAlpha));

            if (this.hovered && this.enabled) {
                hoverAnimation.setDirection(Direction.FORWARDS);
            } else {
                hoverAnimation.setDirection(Direction.BACKWARDS);
            }

            Color accent = RenderUtil.interpolateColorsBackAndForth(15, 75, PRIMARY_COLOR, SECONDARY_COLOR, false);

            int textColor = !this.enabled ? new Color(140, 140, 145).getRGB()
                    : (this.hovered ? accent.getRGB() : new Color(225, 225, 230).getRGB());
            fontRenderer.drawCenteredString(this.displayString,
                    this.xPosition + this.width / 2,
                    this.yPosition + (this.height - 8) / 2,
                    textColor);

            int highlightHeight = 1;
            int highlightY = textY + fontRenderer.FONT_HEIGHT + 3;
            float animWidth = (float) ((textWidth + 8) * hoverAnimation.getOutput());

            RenderUtil.drawRoundedRect(this.xPosition + this.width / 2f - animWidth / 2f, highlightY,
                    animWidth, highlightHeight, 1, accent);

            this.mouseDragged(mc, mouseX, mouseY);
        }
    }

    protected void mouseDragged(Minecraft mc, int mouseX, int mouseY)
    {
    }

    public void mouseReleased(int mouseX, int mouseY)
    {
    }

    public boolean mousePressed(Minecraft mc, int mouseX, int mouseY)
    {
        return this.enabled && this.visible && mouseX >= this.xPosition && mouseY >= this.yPosition && mouseX < this.xPosition + this.width && mouseY < this.yPosition + this.height;
    }

    public boolean isMouseOver()
    {
        return this.hovered;
    }

    public void drawButtonForegroundLayer(int mouseX, int mouseY)
    {
    }

    public void playPressSound(SoundHandler soundHandlerIn)
    {
        soundHandlerIn.playSound(PositionedSoundRecord.create(new ResourceLocation("gui.button.press"), 1.0F));
    }

    public int getButtonWidth()
    {
        return this.width;
    }

    public void setWidth(int width)
    {
        this.width = width;
    }
}