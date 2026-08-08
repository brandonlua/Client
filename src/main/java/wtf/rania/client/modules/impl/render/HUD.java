package wtf.rania.client.modules.impl.render;

import wtf.rania.Client;
import wtf.rania.client.font.CFontRenderer;
import wtf.rania.client.modules.Category;
import wtf.rania.client.modules.Module;
import wtf.rania.client.modules.ModuleInfo;
import wtf.rania.client.modules.values.impl.BoolValue;
import wtf.rania.client.modules.values.impl.ColorValue;
import wtf.rania.client.modules.values.impl.ModeValue;
import wtf.rania.client.modules.values.impl.SliderValue;
import wtf.rania.client.modules.values.impl.TextValue;
import wtf.rania.client.widget.ModuleListWidget;
import wtf.rania.client.widget.DisplayInfoWidget;
import wtf.rania.client.widget.SessionInfoWidget;
import wtf.rania.event.impl.Event2D;
import wtf.rania.utility.render.RenderUtil;
import wtf.rania.utility.render.shaders.impl.Blur;
import wtf.rania.utility.render.shaders.impl.Bloom;
import wtf.rania.utility.render.shaders.impl.Shadow;
import me.zero.alpine.listener.Listener;
import me.zero.alpine.listener.Subscribe;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.shader.Framebuffer;

import java.awt.Color;
import java.awt.Font;

@ModuleInfo(name = "HUD", category = Category.RENDER)
public class HUD extends Module {

    private final ModeValue watermarkMode = new ModeValue("Watermark", new String[]{"Default", "Weedhack", "Vestige", "Custom", "None"}, "Default", this);
    private final TextValue customText = new TextValue("Custom", "</Rania>", this, () -> watermarkMode.is("Custom"));
    public final ColorValue theme = new ColorValue("Theme", new Color(255, 50, 50), this);
    public final ModeValue fontMode = new ModeValue("Font", new String[]{"tahoma", "arial", "client", "noto", "sans"}, "sans", this);

    private final BoolValue moduleList = new BoolValue("ModuleList", true, this);
    private final ModeValue moduleListPos = new ModeValue("Position", new String[]{"Top Left", "Top Right", "Bottom Left", "Bottom Right"}, "Top Right", this, () -> moduleList.get());
    private final SliderValue background = new SliderValue("Opacity", 100F, 0F, 255F, this, () -> moduleList.get());
    private final BoolValue suffix = new BoolValue("Suffix", true, this, () -> moduleList.get());
    private final ModeValue outline = new ModeValue("Outline", new String[]{"None", "Left", "Right", "Top"}, "None", this, () -> moduleList.get());
    private final ModeValue moduleListColorMode = new ModeValue("Color Mode", new String[]{"Static", "Wave"}, "Static", this, () -> moduleList.get());
    private final ColorValue moduleListStaticColor = new ColorValue("Color", new Color(255, 50, 50), this, () -> moduleList.get() && moduleListColorMode.is("Static"));
    private final ColorValue moduleListWaveColor1 = new ColorValue("Wave Color 1", new Color(255, 50, 50), this, () -> moduleList.get() && moduleListColorMode.is("Wave"));
    private final ColorValue moduleListWaveColor2 = new ColorValue("Wave Color 2", new Color(50, 50, 255), this, () -> moduleList.get() && moduleListColorMode.is("Wave"));
    private final SliderValue moduleListWaveSpeed = new SliderValue("Wave Speed", 3000F, 500F, 10000F, 100F, this, () -> moduleList.get() && moduleListColorMode.is("Wave"));

    private final BoolValue sessionInfo = new BoolValue("Session Information", false, this);

    public CFontRenderer fr;
    private String currentFont = null;

    public SessionInfoWidget sessionInfoWidget;
    public DisplayInfoWidget displayInfoWidget;
    public ModuleListWidget moduleListWidget;

    private Framebuffer stencilFramebuffer = new Framebuffer(1, 1, false);
    private Framebuffer shadowFramebuffer = new Framebuffer(1, 1, false);

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

        if (moduleList.get()) {
            renderModuleList(width, height);
        }
    });

    public HUD() {
        updateFont();
        sessionInfoWidget = new SessionInfoWidget();
        displayInfoWidget = new DisplayInfoWidget();
        moduleListWidget = new ModuleListWidget();
    }

    private void renderModuleList(int width, int height) {
        PostProcessing postProcessing = (PostProcessing) Client.INSTANCE.getModuleManager().getModule("PostProcessing");
        boolean ppActive = postProcessing != null && postProcessing.isToggled();

        if (ppActive && postProcessing.bloom.get()) {
            stencilFramebuffer = RenderUtil.createFrameBuffer(stencilFramebuffer);
            stencilFramebuffer.framebufferClear();
            stencilFramebuffer.bindFramebuffer(false);
            RenderUtil.resetColor();
            drawModuleList(width, height);
            RenderUtil.resetColor();
            stencilFramebuffer.unbindFramebuffer();

            Bloom.renderBlur(stencilFramebuffer.framebufferTexture, (int) postProcessing.bloomRadius.get(), (int) postProcessing.bloomOffset.get());
        }

        if (ppActive && postProcessing.shadow.get()) {
            shadowFramebuffer = RenderUtil.createFrameBuffer(shadowFramebuffer);
            shadowFramebuffer.framebufferClear();
            shadowFramebuffer.bindFramebuffer(true);
            drawModuleList(width, height);
            shadowFramebuffer.unbindFramebuffer();

            Shadow.renderBloom(shadowFramebuffer.framebufferTexture, (int) postProcessing.shadowRadius.get(), 1);
        }

        if (ppActive && postProcessing.blur.get()) {
            Blur.startBlur();
            drawModuleList(width, height);
            Blur.endBlur(postProcessing.blurRadius.get(), 1);
        }

        drawModuleList(width, height);
    }

    private void drawModuleList(int width, int height) {
        moduleListWidget.render(fr, width, height, moduleListPos.get(), suffix.get(), outline.get(), background.get(),
                moduleListColorMode.get(), moduleListStaticColor.get().getRGB(), moduleListWaveColor1.get(), moduleListWaveColor2.get(), (int) moduleListWaveSpeed.get());
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
        String formattedClientName = clientName.charAt(0) + "§f" + clientName.substring(1);

        String watermark = formattedClientName + " | " + mc.getDebugFPS() + "FPS | " + getCurrentServer();
        double watermarkWidth = fr.getStringWidth(watermark);

        for (int i = 0; i < (int) (7 + watermarkWidth); i++) {
            Gui.drawRect((int) (x + i - 2), (int) y, (int) (x + i - 1), (int) (y + 2.5F), theme.get().getRGB());
        }

        drawGradientSideRect((int) (x - 2), (int) (y + 1), (int) x, (int) (y + 14.5F), 0x15000000, 0x50000000);
        drawGradientSideRect((int) (x + 3 + watermarkWidth), (int) (y + 1), (int) (x + 5 + watermarkWidth), (int) (y + 14.5F), 0x50000000, 0x15000000);
        drawGradientVerticalRect((int) (x - 2), (int) (y + 14.5F), (int) (x + 5 + watermarkWidth), (int) (y + 16.5F), 0x50000000, 0x15000000);

        fr.drawStringWithShadow(watermark, x + 1, y + 5, theme.get().getRGB());
    }

    private void renderCustomWatermark(float x, float y) {
        String watermark = customText.get();
        if (watermark.isEmpty()) return;

        String firstChar = watermark.substring(0, 1);
        String rest = watermark.substring(1);

        fr.drawStringWithShadow(firstChar, x, y, theme.get().getRGB());
        float firstCharWidth = fr.getStringWidth(firstChar);
        fr.drawStringWithShadow(rest, x + firstCharWidth, y, Color.WHITE.getRGB());
    }

    private void drawGradientSideRect(int left, int top, int right, int bottom, int startColor, int endColor) {
        int height = bottom - top;
        for (int i = 0; i < height; i++) {
            float ratio = (float) i / height;
            int color = interpolateColor(startColor, endColor, ratio);
            Gui.drawRect(left, top + i, right, top + i + 1, color);
        }
    }

    private void drawGradientVerticalRect(int left, int top, int right, int bottom, int startColor, int endColor) {
        int height = bottom - top;
        for (int i = 0; i < height; i++) {
            float ratio = (float) i / height;
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