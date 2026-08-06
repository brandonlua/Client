package wtf.rania.client.modules.values;

import wtf.rania.client.modules.Module;
import lombok.Getter;

import java.awt.Color;
import java.util.Optional;
import java.util.function.Supplier;

@Getter
public abstract class Value {
    private final String name;
    public Supplier<Boolean> visible;
    public Color color = Color.WHITE;
    public boolean child;

    public Value(String name, Module module, Supplier<Boolean> visible, boolean child) {
        this.name = name;
        this.visible = visible;
        this.child = child;
        Optional.ofNullable(module).ifPresent(m -> m.addValue(this));
    }

    public Boolean canDisplay() {
        return this.visible.get();
    }
}