package wtf.fentanyl.util;

import wtf.fentanyl.Client;
import net.minecraft.client.Minecraft;

public interface InstanceAccess {

    Minecraft mc = Minecraft.getMinecraft();

    Client INSTANCE = Client.INSTANCE;
}