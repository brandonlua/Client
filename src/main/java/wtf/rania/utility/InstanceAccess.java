package wtf.rania.utility;

import wtf.rania.Client;
import net.minecraft.client.Minecraft;

public interface InstanceAccess {

    Minecraft mc = Minecraft.getMinecraft();

    Client INSTANCE = Client.INSTANCE;
}