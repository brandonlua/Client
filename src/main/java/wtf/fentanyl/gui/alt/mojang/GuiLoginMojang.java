package wtf.fentanyl.gui.alt.mojang;

import java.awt.Color;
import java.io.IOException;

import wtf.fentanyl.client.font.CFontRenderer;
import wtf.fentanyl.client.processes.FontProcess;
import wtf.fentanyl.gui.alt.AltManagerGui;
import wtf.fentanyl.gui.alt.SessionChanger;
import wtf.fentanyl.util.render.RenderUtil;
import org.lwjgl.input.Keyboard;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;

public class GuiLoginMojang extends GuiScreen {
    private GuiTextField username, password;
    private final CFontRenderer smallTitle;
    private long initTime = System.currentTimeMillis();

    public GuiLoginMojang() {
        smallTitle = FontProcess.getFont("sans");
    }

    @Override
    protected void actionPerformed(final GuiButton button) throws IOException {
        if (button.id == 0) {
            if(this.username.getText().equals("")) {
                this.mc.displayGuiScreen(new GuiLoginMojang());
            } else {
                SessionChanger.getInstance().setUser(this.username.getText(), this.password.getText());
                this.mc.displayGuiScreen(new AltManagerGui());
            }
        } else if (button.id == 1) {
            this.mc.displayGuiScreen(new AltManagerGui());
        }
    }

    @Override
    public void drawScreen(final int mouseX, final int mouseY, final float partialTicks) {

        int l = -2130706433;
        int i1 = 16777215;
        int j1 = 0;
        int k1 = Integer.MIN_VALUE;

        if (l != 0 || i1 != 0)
        {
            this.drawGradientRect(0, 0, this.width, this.height, l, i1);
        }

        if (j1 != 0 || k1 != 0)
        {
            this.drawGradientRect(0, 0, this.width, this.height, j1, k1);
        }

        wtf.fentanyl.util.render.shaders.impl.MainMenu.draw(initTime);

        float middleX = width / 2f;
        float middleY = height / 2f;

        smallTitle.drawString("Mojang Login", middleX - 143 / 2f + 3, middleY - 50 + 4.5f, -1);

        this.username.drawTextBox();
        this.password.drawTextBox();

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public void initGui() {
        super.initGui();

        float middleX = width / 2f;
        float middleY = height / 2f;
        float buttonWidth = 140;

        this.buttonList.clear();

        (this.username = new GuiTextField(100, this.mc.fontRendererObj, (int)(middleX - buttonWidth / 2f), (int)(middleY - 30), (int)buttonWidth, 20)).setFocused(true);
        (this.password = new GuiTextField(101, this.mc.fontRendererObj, (int)(middleX - buttonWidth / 2f), (int)(middleY - 5), (int)buttonWidth, 20)).setFocused(false);

        middleY += 23;
        this.buttonList.add(new GuiButton(0, (int)(middleX - buttonWidth / 2f), (int)middleY, (int)buttonWidth, 20, "Login"));
        middleY += 22;
        this.buttonList.add(new GuiButton(1, (int)(middleX - buttonWidth / 2f), (int)middleY, (int)buttonWidth, 20, "Cancel"));

        Keyboard.enableRepeatEvents(true);
    }

    @Override
    protected void keyTyped(final char character, final int key) throws IOException {
        super.keyTyped(character, key);

        if (character == '\t') {
            if (this.username.isFocused()) {
                this.username.setFocused(false);
                this.password.setFocused(true);
            } else {
                this.username.setFocused(true);
                this.password.setFocused(false);
            }
        }
        if (character == '\r') {
            this.actionPerformed(this.buttonList.get(0));
        }
        this.username.textboxKeyTyped(character, key);
        this.password.textboxKeyTyped(character, key);
    }

    @Override
    protected void mouseClicked(final int mouseX, final int mouseY, final int button) throws IOException {
        super.mouseClicked(mouseX, mouseY, button);
        this.username.mouseClicked(mouseX, mouseY, button);
        this.password.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void onGuiClosed() {
        mc.entityRenderer.loadEntityShader(null);
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public void updateScreen() {
        this.username.updateCursorCounter();
        this.password.updateCursorCounter();
    }
}