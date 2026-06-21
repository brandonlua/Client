package wtf.fentanyl.gui.alt.microsoft;

import java.awt.Color;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.util.concurrent.CompletableFuture;

import wtf.fentanyl.Client;
import wtf.fentanyl.client.font.CFontRenderer;
import wtf.fentanyl.client.processes.FontProcess;
import wtf.fentanyl.gui.alt.AltManagerGui;
import wtf.fentanyl.gui.alt.SessionChanger;
import wtf.fentanyl.util.render.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.Session;
import org.lwjgl.input.Keyboard;

public class GuiLoginMicrosoft extends GuiScreen {
    private GuiTextField username, password;

    public static String statusString;
    public static boolean didTheThing = false;

    private final CFontRenderer smallTitle;
    private final CFontRenderer statusFont;
    private long initTime = System.currentTimeMillis();

    public GuiLoginMicrosoft() {
        smallTitle = FontProcess.getFont("sans");
        statusFont = new CFontRenderer("sans", 14, 0, true, true);
    }

    @Override
    public void initGui() {
        super.initGui();

        float middleX = width / 2f;
        float middleY = height / 2f;
        float buttonWidth = 140;

        username = new GuiTextField(100, this.fontRendererObj, (int)(middleX - buttonWidth / 2f), (int)(middleY - 35), (int)buttonWidth, 15);
        password = new GuiTextField(101, this.fontRendererObj, (int)(middleX - buttonWidth / 2f), (int)(middleY - 15), (int)buttonWidth, 15);
        username.setFocused(true);

        float startY = middleY + 5;
        buttonList.clear();
        startY += 22;
        buttonList.add(new GuiButton(1, (int)(middleX - buttonWidth / 2f), (int)startY, (int)buttonWidth, 20, "Login (OAuth)"));
        startY += 22;
        buttonList.add(new GuiButton(2, (int)(middleX - buttonWidth / 2f), (int)startY, (int)buttonWidth, 20, "Back"));

        Keyboard.enableRepeatEvents(true);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {

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

        smallTitle.drawString("Microsoft Login", middleX - 143 / 2f + 3, middleY - 55 + 4.5f, -1);

        if (!didTheThing) {
            statusString = "Enter email & password";
            statusFont.drawString(statusString, middleX - statusFont.getStringWidth(statusString) / 2f, middleY - 50 + 14, new Color(125, 125, 125).getRGB());
        } else {
            statusString = "Logged in: " + mc.getSession().getUsername();
            statusFont.drawString(statusString, middleX - statusFont.getStringWidth(statusString) / 2f, middleY - 50 + 14, new Color(100, 255, 100).getRGB());
        }

        username.drawTextBox();
        password.drawTextBox();

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 0) {
            handlePasswordLogin();
        } else if (button.id == 1) {
            handleOAuthLogin();
        } else if (button.id == 2) {
            mc.displayGuiScreen(new AltManagerGui());
        }
    }

    @Override
    protected void keyTyped(char character, int key) throws IOException {
        super.keyTyped(character, key);

        if (character == '\t') {
            if (username.isFocused()) {
                username.setFocused(false);
                password.setFocused(true);
            } else {
                username.setFocused(true);
                password.setFocused(false);
            }
        }
        if (character == '\r') {
            handlePasswordLogin();
        }

        if (didTheThing) {
            didTheThing = false;
        }

        username.textboxKeyTyped(character, key);
        password.textboxKeyTyped(character, key);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        username.mouseClicked(mouseX, mouseY, mouseButton);
        password.mouseClicked(mouseX, mouseY, mouseButton);
    }

    private void handlePasswordLogin() {
        if (username.getText().isEmpty()) {
            statusString = "You need to enter an email!";
            didTheThing = false;
            return;
        }
        SessionChanger.getInstance().setUserMicrosoft(username.getText(), password.getText());
        saveAltToFile(username.getText(), password.getText(), Minecraft.getMinecraft().getSession().getUsername());
        didTheThing = true;
    }

    private void handleOAuthLogin() {
        statusString = "Awaiting for response for Microsoft login...";
        CompletableFuture<Void> future = new CompletableFuture<>();

        MicrosoftOAuthTranslation.getRefreshToken(refreshToken -> {
            if (refreshToken != null) {
                MicrosoftOAuthTranslation.LoginData login = MicrosoftOAuthTranslation.login(refreshToken);
                if (login.isGood()) {
                    setSession(new Session(login.username, login.uuid, login.mcToken, "microsoft"));
                    saveOAuthAltToFile(login.username, login.newRefreshToken);
                    didTheThing = true;
                } else {
                    statusString = "Failed to login with Microsoft OAuth";
                    didTheThing = false;
                }
                future.complete(null);
            } else {
                statusString = "Failed to get refresh token";
                didTheThing = false;
                future.complete(null);
            }
        });

        try {
            future.get();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setSession(Session session) {
        try {
            Field sessionField = Minecraft.class.getDeclaredField("session");
            sessionField.setAccessible(true);
            sessionField.set(mc, session);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveAltToFile(String email, String password, String sessionUsername) {
        File dir = new File(Minecraft.getMinecraft().mcDataDir, Client.INSTANCE.name);
        File file = new File(dir, "alts.txt");

        if (!dir.exists()) {
            dir.mkdirs();
        }

        try (FileWriter fw = new FileWriter(file, true); PrintWriter out = new PrintWriter(fw)) {
            out.println("microsoft|" + sessionUsername + "|" + email + "|" + password);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveOAuthAltToFile(String username, String refreshToken) {
        File dir = new File(Minecraft.getMinecraft().mcDataDir, Client.INSTANCE.name);
        File altsFile = new File(dir, "alts.txt");

        if (!dir.exists()) {
            dir.mkdirs();
        }

        try (FileWriter fw = new FileWriter(altsFile, true); PrintWriter out = new PrintWriter(fw)) {
            out.println("microsoftOAuth|" + username);
        } catch (IOException e) {
            e.printStackTrace();
        }

        File tokensFile = new File(dir, "tokens.txt");
        try (FileWriter fw = new FileWriter(tokensFile, true); PrintWriter out = new PrintWriter(fw)) {
            out.println(username + "|" + refreshToken);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onGuiClosed() {
        mc.entityRenderer.loadEntityShader(null);
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public void updateScreen() {
        username.updateCursorCounter();
        password.updateCursorCounter();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    public static Session createMsSession() {
        statusString = "Awaiting for response for Microsoft login...";
        CompletableFuture<Session> future = new CompletableFuture<>();
        MicrosoftOAuthTranslation.getRefreshToken(refreshToken -> {
            if (refreshToken != null) {
                System.out.println("Refresh token: " + refreshToken);
                MicrosoftOAuthTranslation.LoginData login = MicrosoftOAuthTranslation.login(refreshToken);
                future.complete(new Session(login.username, login.uuid, login.mcToken, "microsoft"));
            }
        });
        return future.join();
    }
}