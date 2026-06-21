package wtf.fentanyl.gui.alt;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMainMenu;
import wtf.fentanyl.Client;
import wtf.fentanyl.client.font.CFontRenderer;
import wtf.fentanyl.client.processes.FontProcess;
import wtf.fentanyl.gui.alt.microsoft.GuiLoginMicrosoft;
import wtf.fentanyl.gui.alt.microsoft.MicrosoftOAuthTranslation;
import wtf.fentanyl.util.render.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.Session;

import java.awt.*;
import java.io.*;
import java.lang.reflect.Field;
import java.util.ArrayList;

public class AltManagerGui extends GuiScreen {

    private final CFontRenderer smallTitle;
    private ArrayList<String> alts = new ArrayList<>();
    private long initTime = System.currentTimeMillis();

    public AltManagerGui() {
        smallTitle = FontProcess.getFont("sans");
    }

    @Override
    public void initGui() {
        super.initGui();

        alts.clear();
        loadAltsFromFile();

        float middleY = height / 2f - 50;
        float buttonWidth = 140;

        buttonList.clear();
        buttonList.add(new GuiButton(0, width / 2 - (int)buttonWidth / 2, (int)middleY, (int)buttonWidth, 20, "Cracked"));
        middleY += 22;
        buttonList.add(new GuiButton(1, width / 2 - (int)buttonWidth / 2, (int)middleY, (int)buttonWidth, 20, "Microsoft"));
        middleY += 22;

        if (!alts.isEmpty()) {
            for (int i = 0; i < alts.size(); i++) {
                String alt = alts.get(i);
                String altName = alt.split("\\|")[1];
                buttonList.add(new GuiButton(100 + i, width / 2 - (int)buttonWidth / 2, (int)middleY, (int)buttonWidth, 20, altName));
                middleY += 22;
            }
        }

        buttonList.add(new GuiButton(2, width / 2 - (int)buttonWidth / 2, (int)middleY, (int)buttonWidth, 20, "Back"));
    }

    private void loadAltsFromFile() {
        File dir = new File(Minecraft.getMinecraft().mcDataDir, Client.INSTANCE.name);
        File file = new File(dir, "alts.txt");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty() && (line.startsWith("cracked|") || line.startsWith("microsoft|") || line.startsWith("microsoftOAuth|"))) {
                    alts.add(line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveAltsToFile() {
        File dir = new File(Minecraft.getMinecraft().mcDataDir, Client.INSTANCE.name);
        File file = new File(dir, "alts.txt");

        try (PrintWriter out = new PrintWriter(file)) {
            for (String alt : alts) {
                out.println(alt);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
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
        float middleY = height / 2f - 80;

        smallTitle.drawString("hi " + mc.getSession().getUsername(), 2, height - 10, -1);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 0) {
            mc.displayGuiScreen(new GuiLogin());
        } else if (button.id == 1) {
            mc.displayGuiScreen(new GuiLoginMicrosoft());
        } else if (button.id == 2) {
            mc.displayGuiScreen(new GuiMainMenu());
        } else if (button.id >= 100) {
            int altIndex = button.id - 100;
            if (altIndex < alts.size()) {
                loginWithAlt(alts.get(altIndex));
            }
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        if (mouseButton == 1) {
            for (int i = 0; i < buttonList.size(); i++) {
                GuiButton button = buttonList.get(i);
                if (button.mousePressed(mc, mouseX, mouseY) && button.id >= 100) {
                    int altIndex = button.id - 100;
                    if (altIndex < alts.size()) {
                        alts.remove(altIndex);
                        saveAltsToFile();
                        initGui();
                    }
                }
            }
        }
    }

    private void loginWithAlt(String alt) {
        if (alt.startsWith("cracked|")) {
            String username = alt.split("\\|")[1];
            SessionChanger.getInstance().setUserOffline(username);
        } else if (alt.startsWith("microsoft|")) {
            String[] parts = alt.split("\\|");
            if (parts.length >= 3) {
                String email = parts[2];
                String pass = parts[3];
                SessionChanger.getInstance().setUserMicrosoft(email, pass);
            }
        } else if (alt.startsWith("microsoftOAuth|")) {
            String username = alt.split("\\|")[1];
            String refreshToken = loadRefreshToken(username);
            if (refreshToken != null) {
                MicrosoftOAuthTranslation.LoginData login = MicrosoftOAuthTranslation.login(refreshToken);
                setSession(new Session(login.username, login.uuid, login.mcToken, "microsoft"));
            }
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

    private String loadRefreshToken(String username) {
        File dir = new File(Minecraft.getMinecraft().mcDataDir, Client.INSTANCE.name);
        File file = new File(dir, "tokens.txt");

        if (!file.exists()) return null;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("\\|");
                if (parts.length == 2 && parts[0].equals(username)) {
                    return parts[1];
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == 1) {
            mc.displayGuiScreen(new GuiMainMenu());
            return;
        }

        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}