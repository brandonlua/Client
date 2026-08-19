package net.minecraft.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.realms.RealmsBridge;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.demo.DemoWorldServer;
import net.minecraft.world.storage.ISaveFormat;
import net.minecraft.world.storage.WorldInfo;
import net.optifine.reflect.Reflector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class GuiMainMenu extends GuiScreen implements GuiYesNoCallback {
    private static final AtomicInteger field_175373_f = new AtomicInteger(0);
    private static final Logger logger = LogManager.getLogger();
    private static final Random RANDOM = new Random();
    private float updateCounter;
    private GuiButton buttonResetDemo;
    private boolean field_175375_v = true;
    private final Object threadLock = new Object();
    private String openGLWarning1;
    private String openGLWarning2;
    private String openGLWarningLink;
    public static final String field_96138_a = "Please click " + EnumChatFormatting.UNDERLINE + "here" + EnumChatFormatting.RESET + " for more information.";
    private int field_92024_r;
    private int field_92023_s;
    private int field_92022_t;
    private int field_92021_u;
    private int field_92020_v;
    private int field_92019_w;
    private GuiButton altsButton;
    private boolean field_183502_L;
    private GuiScreen field_183503_M;
    private GuiButton modButton;
    private GuiScreen modUpdateNotification;
    private long initTime = System.currentTimeMillis();

    private static final int BUTTON_WIDTH = 86;
    private static final int BUTTON_HEIGHT = 16;
    private static final int BUTTON_GAP = 6;
    private static final int ROW_GAP = 6;

    public GuiMainMenu() {
        this.openGLWarning2 = field_96138_a;
        this.field_183502_L = false;
        this.updateCounter = RANDOM.nextFloat();
        this.openGLWarning1 = "";
    }

    private boolean func_183501_a() {
        return Minecraft.getMinecraft().gameSettings.getOptionOrdinalValue(GameSettings.Options.REALMS_NOTIFICATIONS) && this.field_183503_M != null;
    }

    public void updateScreen() {
        if (this.func_183501_a()) {
            this.field_183503_M.updateScreen();
        }
    }

    public boolean doesGuiPauseGame() {
        return false;
    }

    protected void keyTyped(char typedChar, int keyCode) throws IOException {
    }

    public void initGui() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());

        this.buttonList.clear();

        int row1Width = BUTTON_WIDTH * 3 + BUTTON_GAP * 2;
        int row1X = this.width / 2 - row1Width / 2;
        int row1Y = this.height / 4 + 48 + 30;

        int row2Width = BUTTON_WIDTH * 2 + BUTTON_GAP;
        int row2X = this.width / 2 - row2Width / 2;
        int row2Y = row1Y + BUTTON_HEIGHT + ROW_GAP;

        this.buttonList.add(new GuiButton(1, row1X, row1Y, BUTTON_WIDTH, BUTTON_HEIGHT,
                I18n.format("menu.singleplayer", new Object[0])));
        this.buttonList.add(new GuiButton(2, row1X + BUTTON_WIDTH + BUTTON_GAP, row1Y, BUTTON_WIDTH, BUTTON_HEIGHT,
                I18n.format("menu.multiplayer", new Object[0])));
        this.buttonList.add(this.altsButton = new GuiButton(14, row1X + (BUTTON_WIDTH + BUTTON_GAP) * 2, row1Y, BUTTON_WIDTH, BUTTON_HEIGHT,
                "Alt Manager"));

        this.buttonList.add(new GuiButton(0, row2X, row2Y, BUTTON_WIDTH, BUTTON_HEIGHT,
                I18n.format("menu.options", new Object[0])));
        this.buttonList.add(new GuiButton(4, row2X + BUTTON_WIDTH + BUTTON_GAP, row2Y, BUTTON_WIDTH, BUTTON_HEIGHT,
                I18n.format("menu.quit", new Object[0])));

        synchronized (this.threadLock) {
            this.field_92023_s = this.fontRendererObj.getStringWidth(this.openGLWarning1);
            this.field_92024_r = this.fontRendererObj.getStringWidth(this.openGLWarning2);
            int k = Math.max(this.field_92023_s, this.field_92024_r);
            this.field_92022_t = (this.width - k) / 2;
            this.field_92021_u = row1Y - 24;
            this.field_92020_v = this.field_92022_t + k;
            this.field_92019_w = this.field_92021_u + 24;
        }

        this.mc.setConnectedToRealms(false);

        if (Minecraft.getMinecraft().gameSettings.getOptionOrdinalValue(GameSettings.Options.REALMS_NOTIFICATIONS) && !this.field_183502_L) {
            RealmsBridge realmsbridge = new RealmsBridge();
            this.field_183503_M = realmsbridge.getNotificationScreen(this);
            this.field_183502_L = true;
        }

        if (this.func_183501_a()) {
            this.field_183503_M.setGuiSize(this.width, this.height);
            this.field_183503_M.initGui();
        }
    }

    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 0) {
            this.mc.displayGuiScreen(new GuiOptions(this, this.mc.gameSettings));
        }

        if (button.id == 1) {
            this.mc.displayGuiScreen(new GuiSelectWorld(this));
        }

        if (button.id == 2) {
            this.mc.displayGuiScreen(new GuiMultiplayer(this));
        }

        if (button.id == 14 && this.altsButton.visible) {
            this.mc.displayGuiScreen(new wtf.rania.gui.alt.AltManagerGui());
        }

        if (button.id == 4) {
            this.mc.shutdown();
        }
    }

    public void confirmClicked(boolean result, int id) {
        if (result && id == 12) {
            ISaveFormat isaveformat = this.mc.getSaveLoader();
            isaveformat.flushCache();
            isaveformat.deleteWorldDirectory("Demo_World");
            this.mc.displayGuiScreen(this);
        } else if (id == 13) {
            if (result) {
                try {
                    Class<?> oclass = Class.forName("java.awt.Desktop");
                    Object object = oclass.getMethod("getDesktop", new Class[0]).invoke((Object)null, new Object[0]);
                    oclass.getMethod("browse", new Class[] {URI.class}).invoke(object, new Object[] {new URI(this.openGLWarningLink)});
                } catch (Throwable throwable) {
                    logger.error("Couldn't open link", throwable);
                }
            }

            this.mc.displayGuiScreen(this);
        }
    }

    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        wtf.rania.utility.render.shaders.impl.MainMenu.draw(initTime);

        wtf.rania.client.processes.FontProcess.getScaledFont("sans", 3.0f).drawCenteredString("Rania", this.width / 2, 35, -1);

        if (this.openGLWarning1 != null && this.openGLWarning1.length() > 0) {
            drawRect(this.field_92022_t - 2, this.field_92021_u - 2, this.field_92020_v + 2, this.field_92019_w - 1, 1428160512);
            this.drawString(this.fontRendererObj, this.openGLWarning1, this.field_92022_t, this.field_92021_u, -1);
            this.drawString(this.fontRendererObj, this.openGLWarning2, (this.width - this.field_92024_r) / 2, this.field_92021_u + 12, -1);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);

        if (this.func_183501_a()) {
            this.field_183503_M.drawScreen(mouseX, mouseY, partialTicks);
        }

        if (this.modUpdateNotification != null) {
            this.modUpdateNotification.drawScreen(mouseX, mouseY, partialTicks);
        }
    }

    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        synchronized (this.threadLock) {
            if (this.openGLWarning1.length() > 0 && mouseX >= this.field_92022_t && mouseX <= this.field_92020_v && mouseY >= this.field_92021_u && mouseY <= this.field_92019_w) {
                GuiConfirmOpenLink guiconfirmopenlink = new GuiConfirmOpenLink(this, this.openGLWarningLink, 13, true);
                guiconfirmopenlink.disableSecurityWarning();
                this.mc.displayGuiScreen(guiconfirmopenlink);
            }
        }

        if (this.func_183501_a()) {
            this.field_183503_M.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }

    public void onGuiClosed() {
        if (this.field_183503_M != null) {
            this.field_183503_M.onGuiClosed();
        }
    }
}
