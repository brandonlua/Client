package wtf.fentanyl.gui;

import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import wtf.fentanyl.client.processes.FontProcess;

import java.io.IOException;

/**
 * Startup intro: black screen, then the Itis icon fades in/out (~1s), then
 * "Welcome to ItisClient" fades in/out (~1s), then the main menu appears with a
 * fade-in (handled by {@link GuiMainMenu}'s own fade overlay).
 */
public class GuiSplash extends GuiScreen {

    private static final ResourceLocation ICON = new ResourceLocation("textures/itis/icon.png");

    private static final long ICON_FADE_IN = 350L;
    private static final long ICON_HOLD = 400L;
    private static final long ICON_FADE_OUT = 350L;
    private static final long ICON_END = ICON_FADE_IN + ICON_HOLD + ICON_FADE_OUT;

    private static final long GAP = 150L;
    private static final long TEXT_START = ICON_END + GAP;
    private static final long TEXT_FADE_IN = 350L;
    private static final long TEXT_HOLD = 400L;
    private static final long TEXT_FADE_OUT = 350L;
    private static final long TEXT_END = TEXT_START + TEXT_FADE_IN + TEXT_HOLD + TEXT_FADE_OUT;

    private long startTime = -1L;
    private boolean done = false;

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (this.startTime < 0L) {
            this.startTime = System.currentTimeMillis();
        }
        long t = System.currentTimeMillis() - this.startTime;

        drawRect(0, 0, this.width, this.height, 0xFF000000);

        if (t < ICON_END) {
            drawIcon(phaseAlpha(t, 0L, ICON_FADE_IN, ICON_HOLD, ICON_FADE_OUT));
        } else if (t >= TEXT_START && t < TEXT_END) {
            float alpha = phaseAlpha(t, TEXT_START, TEXT_FADE_IN, TEXT_HOLD, TEXT_FADE_OUT);
            int a = (int) (alpha * 255.0F) & 0xFF;
            FontProcess.getScaledFont("sans", 2.5f)
                    .drawCenteredString("Welcome to ItisClient", this.width / 2, this.height / 2 - 8, (a << 24) | 0xFFFFFF);
        } else if (t >= TEXT_END && !this.done) {
            this.done = true;
            this.mc.displayGuiScreen(new GuiMainMenu());
        }
    }

    private void drawIcon(float alpha) {
        int size = 128;
        int x = (this.width - size) / 2;
        int y = this.height / 2 - size / 2 - 10;

        GlStateManager.enableBlend();
        GlStateManager.blendFunc(770, 771);
        GlStateManager.color(1.0F, 1.0F, 1.0F, alpha);
        this.mc.getTextureManager().bindTexture(ICON);
        drawModalRectWithCustomSizedTexture(x, y, 0.0F, 0.0F, size, size, size, size);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.disableBlend();
    }

    private static float phaseAlpha(long t, long start, long fadeIn, long hold, long fadeOut) {
        long local = t - start;
        if (local < fadeIn) {
            return clamp((float) local / fadeIn);
        }
        if (local < fadeIn + hold) {
            return 1.0F;
        }
        return clamp(1.0F - (float) (local - fadeIn - hold) / fadeOut);
    }

    private static float clamp(float v) {
        return v < 0.0F ? 0.0F : (v > 1.0F ? 1.0F : v);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        // Let the user skip the intro.
        this.mc.displayGuiScreen(new GuiMainMenu());
    }
}
