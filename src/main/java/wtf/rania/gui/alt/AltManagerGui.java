package wtf.rania.gui.alt;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMainMenu;
import wtf.rania.Client;
import wtf.rania.client.font.CFontRenderer;
import wtf.rania.client.processes.FontProcess;
import wtf.rania.gui.alt.microsoft.GuiLoginMicrosoft;
import wtf.rania.gui.alt.microsoft.MicrosoftOAuthTranslation;
import wtf.rania.utility.render.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.Session;
import org.lwjgl.input.Mouse;

import java.awt.*;
import java.io.*;
import java.lang.reflect.Field;
import java.util.ArrayList;

public class AltManagerGui extends GuiScreen {

    private final CFontRenderer smallTitle;
    private ArrayList<String> alts = new ArrayList<>();
    private long initTime = System.currentTimeMillis();
    private int scrollOffset = 0;

    public AltManagerGui() {
        smallTitle = FontProcess.getFont("sans");
    }

    @Override
    public void initGui() {
        super.initGui();

        alts.clear();
        loadAltsFromFile();

        buttonList.clear();
        int panelW = Math.min(330, width - 36);
        int panelX = width / 2 - panelW / 2;
        int buttonY = 76;
        int buttonW = (panelW - 20) / 3;
        buttonList.add(new GuiButton(0, panelX, buttonY, buttonW, 20, "Add Cracked"));
        buttonList.add(new GuiButton(1, panelX + buttonW + 10, buttonY, buttonW, 20, "Microsoft"));
        buttonList.add(new GuiButton(3, panelX + 2 * (buttonW + 10), buttonY, buttonW, 20, "Token Login"));
        buttonList.add(new GuiButton(2, width / 2 - 60, height - 34, 120, 20, "Back"));
        clampScroll();
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
                if (!line.isEmpty() && (line.startsWith("cracked|") || line.startsWith("microsoft|") || line.startsWith("microsoftOAuth|") || line.startsWith("token|"))) {
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
        wtf.rania.utility.render.shaders.impl.MainMenu.draw(initTime);

        int panelW = Math.min(330, this.width - 36);
        int panelX = this.width / 2 - panelW / 2;
        int panelTop = 24;
        int panelBottom = this.height - 46;
        int listTop = 108;
        int listBottom = panelBottom - 14;

        RenderUtil.drawRoundedRect(panelX, panelTop, panelW, panelBottom - panelTop, 6, new Color(16, 16, 24, 190));

        smallTitle.drawCenteredString("Logged in as " + mc.getSession().getUsername(),
                this.width / 2, panelTop + 12, new Color(185, 167, 255).getRGB());
        smallTitle.drawCenteredString(alts.size() + " saved account" + (alts.size() == 1 ? "" : "s") + "  -  left-click login, right-click remove",
                this.width / 2, panelTop + 26, new Color(150, 150, 165).getRGB());

        RenderUtil.drawRoundedRect(panelX + 10, listTop - 7, panelW - 20, listBottom - listTop + 14, 5, new Color(10, 10, 16, 145));
        drawAltList(panelX + 18, listTop, panelW - 36, listBottom, mouseX, mouseY);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 0) {
            mc.displayGuiScreen(new GuiLogin());
        } else if (button.id == 1) {
            mc.displayGuiScreen(new GuiLoginMicrosoft());
        } else if (button.id == 3) {
            mc.displayGuiScreen(new GuiLoginToken());
        } else if (button.id == 2) {
            mc.displayGuiScreen(new GuiMainMenu());
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        int altIndex = getAltIndexAt(mouseX, mouseY);
        if (altIndex >= 0) {
            if (mouseButton == 1) {
                alts.remove(altIndex);
                saveAltsToFile();
                initGui();
            } else if (mouseButton == 0) {
                loginWithAlt(alts.get(altIndex));
            }
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) {
            scrollOffset += wheel < 0 ? 1 : -1;
            clampScroll();
        }
    }

    private void drawAltList(int x, int top, int width, int bottom, int mouseX, int mouseY) {
        if (alts.isEmpty()) {
            smallTitle.drawCenteredString("No saved accounts yet", this.width / 2, top + 30, new Color(190, 190, 205).getRGB());
            smallTitle.drawCenteredString("Use the buttons above to add one", this.width / 2, top + 44, new Color(125, 125, 145).getRGB());
            return;
        }

        int rowH = 34;
        int gap = 6;
        int visibleRows = Math.max(1, (bottom - top + gap) / (rowH + gap));
        int end = Math.min(alts.size(), scrollOffset + visibleRows);

        for (int i = scrollOffset; i < end; i++) {
            int y = top + (i - scrollOffset) * (rowH + gap);
            boolean hovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + rowH;
            Color fill = hovered ? new Color(34, 34, 48, 220) : new Color(25, 25, 36, 205);
            RenderUtil.drawRoundedRect(x, y, width, rowH, 5, fill);

            smallTitle.drawString(getAltName(alts.get(i)), x + 12, y + 7, -1);
            smallTitle.drawString(getAltType(alts.get(i)), x + 12, y + 20, new Color(150, 150, 168).getRGB());
        }

        if (alts.size() > visibleRows) {
            smallTitle.drawCenteredString((scrollOffset + 1) + "-" + end + " / " + alts.size(), this.width / 2, bottom - 8, new Color(120, 120, 138).getRGB());
        }
    }

    private int getAltIndexAt(int mouseX, int mouseY) {
        int panelW = Math.min(330, this.width - 36);
        int x = this.width / 2 - panelW / 2 + 18;
        int width = panelW - 36;
        int top = 108;
        int bottom = this.height - 60;
        int rowH = 34;
        int gap = 6;

        if (mouseX < x || mouseX > x + width || mouseY < top || mouseY > bottom) {
            return -1;
        }

        int row = (mouseY - top) / (rowH + gap);
        int yInRow = (mouseY - top) % (rowH + gap);
        int index = scrollOffset + row;
        return yInRow <= rowH && index >= 0 && index < alts.size() ? index : -1;
    }

    private void clampScroll() {
        int visibleRows = Math.max(1, ((this.height - 60) - 108 + 6) / 40);
        int max = Math.max(0, alts.size() - visibleRows);
        if (scrollOffset < 0) {
            scrollOffset = 0;
        }
        if (scrollOffset > max) {
            scrollOffset = max;
        }
    }

    private String getAltName(String alt) {
        String[] parts = alt.split("\\|");
        return parts.length > 1 ? parts[1] : "Unknown";
    }

    private String getAltType(String alt) {
        if (alt.startsWith("cracked|")) {
            return "Offline account";
        }
        if (alt.startsWith("microsoftOAuth|")) {
            return "Microsoft OAuth";
        }
        if (alt.startsWith("microsoft|")) {
            return "Microsoft credentials";
        }
        if (alt.startsWith("token|")) {
            return "Session token";
        }
        return "Saved account";
    }

    private void loginWithAlt(String alt) {
        if (alt.startsWith("cracked|")) {
            String username = alt.split("\\|")[1];
            SessionChanger.getInstance().setUserOffline(username);
        } else if (alt.startsWith("token|")) {
            // token|username|uuid|accessToken — swap the session straight back in,
            // just like the tokenlogin mod does (no network round-trip needed).
            String[] parts = alt.split("\\|");
            if (parts.length >= 4) {
                SessionChanger.getInstance().setUserToken(parts[1], parts[2], parts[3]);
            }
        } else if (alt.startsWith("microsoft|")) {
            String[] parts = alt.split("\\|");
            if (parts.length >= 4) {
                String email = parts[2];
                String pass = parts[3];
                SessionChanger.getInstance().setUserMicrosoft(email, pass);
            }
        } else if (alt.startsWith("microsoftOAuth|")) {
            String username = alt.split("\\|")[1];
            String refreshToken = loadRefreshToken(username);
            if (refreshToken != null) {
                MicrosoftOAuthTranslation.LoginData login = MicrosoftOAuthTranslation.login(refreshToken);
                // Only swap the session in if the refresh actually succeeded, otherwise
                // we'd replace the current session with null credentials.
                if (login != null && login.isGood()) {
                    setSession(new Session(login.username, login.uuid, login.mcToken, "microsoft"));
                }
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
