package wtf.fentanyl.client.commands.impl;

import wtf.fentanyl.Client;
import wtf.fentanyl.client.commands.Command;
import wtf.fentanyl.client.modules.Module;

public class ToggleCommand extends Command {

    public ToggleCommand() {
        super("toggle", "Toggle a module", "t");
    }

    @Override
    public void execute(String[] args) {
        if (args.length != 1) {
            Client.INSTANCE.getCommandManager().sendMessage("Usage: .toggle <module>");
            return;
        }

        Module module = Client.INSTANCE.getModuleManager().getModule(args[0]);

        if (module == null) {
            Client.INSTANCE.getCommandManager().sendMessage("Module not found.");
            return;
        }

        module.toggle();
        Client.INSTANCE.getCommandManager().sendMessage(module.getName() + " " + (module.isToggled() ? "enabled" : "disabled"));
    }
}