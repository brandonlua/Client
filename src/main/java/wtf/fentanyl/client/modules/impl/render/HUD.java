package wtf.fentanyl.client.modules.impl.render;

import wtf.fentanyl.Client;
import wtf.fentanyl.client.font.CFontRenderer;
import wtf.fentanyl.client.modules.Category;
import wtf.fentanyl.client.modules.Module;
import wtf.fentanyl.client.modules.ModuleInfo;
import wtf.fentanyl.client.modules.values.impl.BoolValue;
import wtf.fentanyl.client.modules.values.impl.ModeValue;
import wtf.fentanyl.client.modules.values.impl.ColorValue;
import wtf.fentanyl.client.modules.values.impl.SliderValue;
import wtf.fentanyl.client.modules.values.impl.TextValue;
import wtf.fentanyl.client.widget.ArrayListWidget;
import wtf.fentanyl.client.widget.SessionInfoWidget;
import wtf.fentanyl.client.widget.DisplayInfoWidget;
import wtf.fentanyl.event.impl.Event2D;
import wtf.fentanyl.util.render.RenderUtil;
import wtf.fentanyl.util.render.shaders.impl.Blur;
import wtf.fentanyl.util.render.shaders.impl.Shadow;
import wtf.fentanyl.util.render.shaders.impl.Bloom;
import me.zero.alpine.listener.Listener;
import me.zero.alpine.listener.Subscribe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.shader.Framebuffer;

import java.awt.*;

@ModuleInfo(name = "HUD", category = Category.RENDER)
public class HUD extends Module {

    private ModeValue watermarkMode = new ModeValue("Watermark", new String[]{"Default", "Weedhack", "Vestige", "Custom", "None"}, "Default", this);
    private TextValue customText = new TextValue("Custom", "Fentanyl", this, () -> watermarkMode.is("Custom"));
    public ColorValue theme = new ColorValue("Theme", new Color(255, 50, 50), this);
    public ModeValue fontMode = new ModeValue("Font", new String[]{"tahoma", "arial", "client", "noto", "sans"}, "sans", this);

    private BoolValue arrayList = new BoolValue("ArrayList", true, this);
    private ModeValue arrayListpos = new ModeValue("Position", new String[]{"Top Left", "Top Right", "Bottom Left", "Bottom Right"}, "Top Right", this, () -> arrayList.get());
    private SliderValue background = new SliderValue("Opacity", 100F, 0F, 255F, this, () -> arrayList.get());
    private BoolValue suffix = new BoolValue("Suffix", true, this, () -> arrayList.get());
    private ModeValue outline = new ModeValue("Outline", new String[]{"None", "Left", "Right", "Top"}, "None", this, () -> arrayList.get());

    private BoolValue sessionInfo = new BoolValue("Session Information", false, this);

    public CFontRenderer fr;
    private String currentFont = null;
    public SessionInfoWidget sessionInfoWidget;
    public DisplayInfoWidget displayInfoWidget;
    public ArrayListWidget arrayListWidget;
    private Framebuffer stencilFramebuffer = new Framebuffer(1, 1, false);

    @Subscribe
    private final Listener<Event2D> event2DListener = new Listener<>(e -> {
        if (!fontMode.get().equals(currentFont)) {
            updateFont();
        }

        ScaledResolution sr = new ScaledResolution(mc);
        int width = sr.getScaledWidth();
        int height = sr.getScaledHeight();

        float watermarkX = 2;
        float watermarkY = 2;

        if (watermarkMode.is("Default")) {
            renderDefaultWatermark(watermarkX, watermarkY);
        } else if (watermarkMode.is("Weedhack")) {
            renderWeedhackWatermark(watermarkX, watermarkY);
        } else if (watermarkMode.is("Vestige")) {
            renderVestige(watermarkX, watermarkY);
        } else if (watermarkMode.is("Custom")) {
            renderCustomWatermark(watermarkX, watermarkY);
        }

        if (sessionInfo.get()) {
            sessionInfoWidget.render(theme.get().getRGB());
        }

        if (arrayList.get()) {
            PostProcessing postProcessing = (PostProcessing) Client.INSTANCE.getModuleManager().getModule("PostProcessing");

            if (postProcessing != null && postProcessing.isToggled() && postProcessing.bloom.get()) {
                stencilFramebuffer = RenderUtil.createFrameBuffer(stencilFramebuffer);
                stencilFramebuffer.framebufferClear();
                stencilFramebuffer.bindFramebuffer(false);
                RenderUtil.resetColor();
                arrayListWidget.render(fr, width, height, arrayListpos.get(), suffix.get(), outline.get(), background.get(), theme.get().getRGB());
                RenderUtil.resetColor();
                stencilFramebuffer.unbindFramebuffer();

                Bloom.renderBlur(stencilFramebuffer.framebufferTexture, (int) postProcessing.bloomRadius.get(), (int) postProcessing.bloomOffset.get());
            }

            if (postProcessing != null && postProcessing.isToggled() && postProcessing.blur.get()) {
                Blur.startBlur();
                arrayListWidget.render(fr, width, height, arrayListpos.get(), suffix.get(), outline.get(), background.get(), theme.get().getRGB());
                Blur.endBlur(postProcessing.blurRadius.get(), 1);
            }

            if (postProcessing != null && postProcessing.isToggled() && postProcessing.shadow.get()) {
                GlStateManager.enableAlpha();
                GlStateManager.alphaFunc(516, 0.0f);
                GlStateManager.enableBlend();
                Shadow.renderBloom(mc.getFramebuffer().framebufferTexture, (int) postProcessing.shadowRadius.get(), 1);
                GlStateManager.disableBlend();
            }

            arrayListWidget.render(fr, width, height, arrayListpos.get(), suffix.get(), outline.get(), background.get(), theme.get().getRGB());
        }
    });

    public HUD() {
        updateFont();
        sessionInfoWidget = new SessionInfoWidget();
        displayInfoWidget = new DisplayInfoWidget();
        arrayListWidget = new ArrayListWidget();
    }

    public int bgColor() {
        return new Color(0, 0, 0, (int) background.get()).getRGB();
    }

    private void renderDefaultWatermark(float x, float y) {
        String name = Client.INSTANCE.getName();
        fr.drawString(name, x + 3, y + 2, Color.WHITE.getRGB());
    }

    private void renderWeedhackWatermark(float x, float y) {
        String text = "weedhack premium beta";
        float textWidth = fr.getStringWidth(text);
        float boxWidth = textWidth + 4;
        float boxHeight = 12;


        RenderUtil.drawRect(x, y, boxWidth + 8, boxHeight + 8, new Color(60, 60, 60));
        RenderUtil.drawRect(x + 1, y + 1, boxWidth + 6, boxHeight + 6, new Color(40, 40, 40));
        RenderUtil.drawRect(x + 2, y + 2, boxWidth + 4, boxHeight + 4, new Color(60, 60, 60));
        RenderUtil.drawRect(x + 3, y + 3, boxWidth + 2, boxHeight + 2, new Color(22, 22, 22));

        float textY = fontMode.get().equals("Verdana") ? y + 6 : y + 7;
        fr.drawStringWithShadow(text, x + 5, textY, 0xFFFFFF);

        float gradient = boxWidth + 2;
        for (int i = 0; i < gradient; i++) {
            float ratio = i / gradient;
            int r = (int) (255 + (255 - 255) * ratio);
            int g = (int) (255 + (0 - 255) * ratio);
            int b = (int) (0 + (255 - 0) * ratio);
            RenderUtil.drawRect(x + 3 + i, y + 3, 1, 1, new Color(r, g, b));
        }
    }

    private void renderVestige(float x, float y) {
        String clientName = "Vestige";
        String formattedClientName = String.valueOf(clientName.charAt(0)) + "§f" + clientName.substring(1);

        String watermark = formattedClientName + " | " + mc.getDebugFPS() + "FPS | " + getCurrentServer();

        double watermarkWidth = fr.getStringWidth(watermark);

        for(int i = 0; i < (int)(7 + watermarkWidth); i++) {
            Gui.drawRect((int)(x + i - 2), (int)y, (int)(x + i - 1), (int)(y + 2.5F), theme.get().getRGB());
        }

        drawGradientSideRect((int)(x - 2), (int)(y + 1), (int)x, (int)(y + 14.5F), 0x15000000, 0x50000000);
        drawGradientSideRect((int)(x + 3 + watermarkWidth), (int)(y + 1), (int)(x + 5 + watermarkWidth), (int)(y + 14.5F), 0x50000000, 0x15000000);

        drawGradientVerticalRect((int)(x - 2), (int)(y + 14.5F), (int)(x + 5 + watermarkWidth), (int)(y + 16.5F), 0x50000000, 0x15000000);

        fr.drawStringWithShadow(watermark, x + 1, y + 5, theme.get().getRGB());
    }

    private void renderCustomWatermark(float x, float y) {
        String watermark = customText.get();

        if (watermark.length() > 0) {
            String firstChar = watermark.substring(0, 1);
            String rest = watermark.substring(1);

            fr.drawStringWithShadow(firstChar, x, y, theme.get().getRGB());
            float firstCharWidth = fr.getStringWidth(firstChar);
            fr.drawStringWithShadow(rest, x + firstCharWidth, y, Color.WHITE.getRGB());
        }
    }

    private void drawGradientSideRect(int left, int top, int right, int bottom, int startColor, int endColor) {
        int height = bottom - top;
        for (int i = 0; i < height; i++) {
            float ratio = (float)i / height;
            int color = interpolateColor(startColor, endColor, ratio);
            Gui.drawRect(left, top + i, right, top + i + 1, color);
        }
    }

    private void drawGradientVerticalRect(int left, int top, int right, int bottom, int startColor, int endColor) {
        int height = bottom - top;
        for (int i = 0; i < height; i++) {
            float ratio = (float)i / height;
            int color = interpolateColor(startColor, endColor, ratio);
            Gui.drawRect(left, top + i, right, top + i + 1, color);
        }
    }

    private int interpolateColor(int color1, int color2, float ratio) {
        int a1 = (color1 >> 24) & 0xFF;
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;

        int a2 = (color2 >> 24) & 0xFF;
        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;

        int a = (int) (a1 + (a2 - a1) * ratio);
        int r = (int) (r1 + (r2 - r1) * ratio);
        int g = (int) (g1 + (g2 - g1) * ratio);
        int b = (int) (b1 + (b2 - b1) * ratio);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private String getCurrentServer() {
        if (mc.isSingleplayer()) {
            return "Singleplayer";
        } else if (mc.getCurrentServerData() != null) {
            return mc.getCurrentServerData().serverIP;
        }
        return "Unknown";
    }

    private void updateFont() {
        fr = new CFontRenderer(fontMode.get(), 18, Font.PLAIN, true, true);
        currentFont = fontMode.get();
    }

    @Override
    public void onEnabled() {
        Client.BUS.subscribe(this);
    }

    @Override
    public void onDisabled() {
        Client.BUS.unsubscribe(this);
    }
}