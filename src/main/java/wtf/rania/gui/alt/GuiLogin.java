package wtf.rania.gui.alt;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.SecureRandom;

import wtf.rania.Client;
import wtf.rania.client.font.CFontRenderer;
import wtf.rania.client.processes.FontProcess;
import org.lwjgl.input.Keyboard;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;

public class GuiLogin extends GuiScreen {
    private GuiTextField username;

    private final CFontRenderer smallTitle;
    private long initTime = System.currentTimeMillis();

    public GuiLogin() {
        smallTitle = FontProcess.getFont("client");
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

        wtf.rania.utility.render.shaders.impl.MainMenu.draw(initTime);

        float middleX = width / 2f;
        float middleY = height / 2f;

        smallTitle.drawString("Cracked Login", middleX - 143 / 2f + 3, middleY - 30 + 4.5f, -1);

        this.username.drawTextBox();

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public void initGui() {
        super.initGui();

        float middleX = width / 2f;
        float middleY = height / 2f;
        float buttonWidth = 140;

        (this.username = new GuiTextField(100, this.fontRendererObj, (int)(middleX - buttonWidth / 2f), (int)(middleY - 10), (int)buttonWidth, 20)).setFocused(true);
        Keyboard.enableRepeatEvents(true);

        buttonList.clear();
        middleY += 18;
        buttonList.add(new GuiButton(0, (int)(middleX - buttonWidth / 2f), (int)middleY, (int)buttonWidth, 20, "Login"));
        middleY += 22;
        buttonList.add(new GuiButton(1, (int)(middleX - buttonWidth / 2f), (int)middleY, (int)buttonWidth, 20, "Random"));
        middleY += 22;
        buttonList.add(new GuiButton(2, (int)(middleX - buttonWidth / 2f), (int)middleY, (int)buttonWidth, 20, "Cancel"));
    }

    private static final String NUMBERS = "0123456789";
    private static final String LETTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final SecureRandom RANDOM = new SecureRandom();

    public static String generateRandomString() {
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < 4; i++) {
            result.append(LETTERS.charAt(RANDOM.nextInt(LETTERS.length())));
        }

        for (int i = 0; i < 4; i++) {
            result.append(NUMBERS.charAt(RANDOM.nextInt(NUMBERS.length())));
        }

        return result.toString();
    }

    @Override
    protected void keyTyped(final char character, final int key) throws IOException {
        super.keyTyped(character, key);

        if (character == '\t' && !this.username.isFocused()) {
            this.username.setFocused(true);
        }
        if (character == '\r') {
            handleLogin();
        }
        this.username.textboxKeyTyped(character, key);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 0) {
            handleLogin();
        } else if (button.id == 1) {
            String text = generateRandomString();
            SessionChanger.getInstance().setUserOffline(text);
            saveAltToFile(text);
            this.mc.displayGuiScreen(new AltManagerGui());
        } else if (button.id == 2) {
            this.mc.displayGuiScreen(new AltManagerGui());
        }
    }

    @Override
    protected void mouseClicked(final int mouseX, final int mouseY, final int button) throws IOException {
        super.mouseClicked(mouseX, mouseY, button);
        this.username.mouseClicked(mouseX, mouseY, button);
    }

    private void handleLogin() {
        if (this.username.getText().equals("")) {
            this.mc.displayGuiScreen(new GuiLogin());
        } else {
            SessionChanger.getInstance().setUserOffline(this.username.getText());
            saveAltToFile(this.username.getText());
            this.mc.displayGuiScreen(new AltManagerGui());
        }
    }

    @Override
    public void onGuiClosed() {
        mc.entityRenderer.loadEntityShader(null);
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public void updateScreen() {
        this.username.updateCursorCounter();
    }

    private void saveAltToFile(String sessionUsername) {
        File dir = new File(Minecraft.getMinecraft().mcDataDir, Client.INSTANCE.name);
        File file = new File(dir, "alts.txt");

        try (FileWriter fw = new FileWriter(file, true); PrintWriter out = new PrintWriter(fw)) {
            out.println("cracked|" + sessionUsername);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}