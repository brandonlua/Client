package wtf.rania.client.modules.impl.misc;

import wtf.rania.client.modules.Category;
import wtf.rania.client.modules.Module;
import wtf.rania.client.modules.ModuleInfo;
import wtf.rania.client.modules.values.impl.TextValue;

@ModuleInfo(name = "NickHider", description = "Hides player username", category = Category.MISC, key = 0, enabled = false)
public class NickHider extends Module {

    public TextValue nickName = new TextValue("Name", "", this);

    public String getNick() {
        String text = nickName.get();
        return (text == null || text.isEmpty()) ? "You" : text;
    }
}