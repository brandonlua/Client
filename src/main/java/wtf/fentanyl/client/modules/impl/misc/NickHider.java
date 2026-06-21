package wtf.fentanyl.client.modules.impl.misc;

import wtf.fentanyl.client.modules.Category;
import wtf.fentanyl.client.modules.Module;
import wtf.fentanyl.client.modules.ModuleInfo;
import wtf.fentanyl.client.modules.values.impl.TextValue;

@ModuleInfo(name = "NickHider", description = "Hides player username", category = Category.MISC, key = 0, enabled = false)
public class NickHider extends Module {

    public TextValue nickName = new TextValue("Name", "", this);

    public String getNick() {
        String text = nickName.get();
        return (text == null || text.isEmpty()) ? "You" : text;
    }
}