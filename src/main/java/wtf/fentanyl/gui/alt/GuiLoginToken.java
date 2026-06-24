package wtf.fentanyl.gui.alt;

import java.awt.Color;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import wtf.fentanyl.Client;
import wtf.fentanyl.client.font.CFontRenderer;
import wtf.fentanyl.client.processes.FontProcess;
import wtf.fentanyl.util.web.Browser;
import org.lwjgl.input.Keyboard;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;

/**
 * Session-token login, ported from the "tokenlogin" Forge mod: the user pastes
 * a Minecraft (access) token, we resolve the matching username/uuid from the
 * Mojang services API and swap the live session in. Successful logins are saved
 * to the Alt Manager so they can be re-used with one click.
 */
public class GuiLoginToken extends GuiScreen {

    private GuiTextField tokenField;

    private final CFontRenderer smallTitle;
    private final long initTime = System.currentTimeMillis();
    private volatile String status = "Paste a Minecraft session token";

    public GuiLoginToken() {
        smallTitle = FontProcess.getFont("client");
    }

    @Override
    public void initGui() {
        super.initGui();

        float middleX = width / 2f;
        float middleY = height / 2f;
        float buttonWidth = 200;

        (this.tokenField = new GuiTextField(100, this.fontRendererObj,
                (int) (middleX - buttonWidth / 2f), (int) (middleY - 10), (int) buttonWidth, 20)).setFocused(true);
        this.tokenField.setMaxStringLength(32767);
        Keyboard.enableRepeatEvents(true);

        buttonList.clear();
        middleY += 18;
        buttonList.add(new GuiButton(0, (int) (middleX - buttonWidth / 2f), (int) middleY, (int) buttonWidth, 20, "Login"));
        middleY += 22;
        buttonList.add(new GuiButton(2, (int) (middleX - buttonWidth / 2f), (int) middleY, (int) buttonWidth, 20, "Cancel"));
    }

    @Override
    public void drawScreen(final int mouseX, final int mouseY, final float partialTicks) {
        wtf.fentanyl.util.render.shaders.impl.MainMenu.draw(initTime);

        float middleX = width / 2f;
        float middleY = height / 2f;

        smallTitle.drawCenteredString("Token Login", middleX, middleY - 40, -1);
        smallTitle.drawCenteredString(status, middleX, middleY - 28, new Color(185, 167, 255).getRGB());

        this.tokenField.drawTextBox();

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void keyTyped(final char character, final int key) throws IOException {
        if (key == Keyboard.KEY_ESCAPE) {
            this.mc.displayGuiScreen(new AltManagerGui());
            return;
        }
        if (character == '\r') {
            handleLogin();
            return;
        }
        this.tokenField.textboxKeyTyped(character, key);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 0) {
            handleLogin();
        } else if (button.id == 2) {
            this.mc.displayGuiScreen(new AltManagerGui());
        }
    }

    @Override
    protected void mouseClicked(final int mouseX, final int mouseY, final int button) throws IOException {
        super.mouseClicked(mouseX, mouseY, button);
        this.tokenField.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void updateScreen() {
        this.tokenField.updateCursorCounter();
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    private void handleLogin() {
        final String token = this.tokenField.getText().trim();
        if (token.isEmpty()) {
            status = "§4Enter a token first";
            return;
        }

        status = "§eLogging in...";
        new Thread(() -> {
            String[] profile = getProfileInfo(token);
            if (profile == null) {
                status = "§4Invalid token";
                return;
            }

            String username = profile[0];
            String uuid = profile[1];
            SessionChanger.getInstance().setUserToken(username, uuid, token);
            saveAltToFile(username, uuid, token);
            status = "§2Logged in as " + username;
            mc.addScheduledTask(() -> mc.displayGuiScreen(new AltManagerGui()));
        }, "Token-Login").start();
    }

    /**
     * Resolves the username/uuid for a session token via the Mojang services
     * profile endpoint. Returns {@code [username, uuid]} or {@code null} when the
     * token is invalid.
     */
    private static String[] getProfileInfo(String token) {
        try {
            String response = Browser.getBearerResponse("https://api.minecraftservices.com/minecraft/profile", token);
            if (response == null || response.isEmpty()) {
                return null;
            }
            JsonObject json = new JsonParser().parse(response).getAsJsonObject();
            if (!json.has("name") || !json.has("id")) {
                return null;
            }
            return new String[]{json.get("name").getAsString(), json.get("id").getAsString()};
        } catch (Exception e) {
            return null;
        }
    }

    private void saveAltToFile(String username, String uuid, String token) {
        File dir = new File(Minecraft.getMinecraft().mcDataDir, Client.INSTANCE.name);
        File file = new File(dir, "alts.txt");

        try (FileWriter fw = new FileWriter(file, true); PrintWriter out = new PrintWriter(fw)) {
            out.println("token|" + username + "|" + uuid + "|" + token);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
