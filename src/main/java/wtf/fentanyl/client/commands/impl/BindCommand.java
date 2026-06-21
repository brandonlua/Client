package wtf.fentanyl.client.commands.impl;

import wtf.fentanyl.Client;
import wtf.fentanyl.client.commands.Command;
import wtf.fentanyl.client.modules.Module;
import org.lwjgl.input.Keyboard;

public class BindCommand extends Command {

    public BindCommand() {
        super("bind", "Bind a module to a key", "b");
    }

    @Override
    public void execute(String[] args) {
        if (args.length != 2) {
            Client.INSTANCE.getCommandManager().sendMessage("Usage: .bind <module> <key>");
            return;
        }

        Module module = Client.INSTANCE.getModuleManager().getModule(args[0]);
        if (module == null) {
            Client.INSTANCE.getCommandManager().sendMessage("Module not found.");
            return;
        }

        int keyCode = Keyboard.getKeyIndex(args[1].toUpperCase());
        if (keyCode == Keyboard.KEY_NONE) {
            Client.INSTANCE.getCommandManager().sendMessage("Invalid key.");
            return;
        }

        module.setKey(keyCode);
        Client.INSTANCE.getCommandManager().sendMessage("Bound " + module.getName() + " to " + Keyboard.getKeyName(keyCode));
    }
}