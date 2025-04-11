package ca.bungo.holos.api.holograms.simple;

import ca.bungo.holos.api.holograms.SimpleHologram;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Color;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;

@Getter
@Setter
public class TextSimpleHologram extends SimpleHologram<TextDisplay> {

    private String text;

    private boolean persistent;
    private Display.Billboard billboard = Display.Billboard.FIXED;
    private Color textColor;
    private Color backgroundColor;
    private TextDisplay.TextAlignment textAlignment;


    public TextSimpleHologram(String text) {
        super(TextDisplay.class);
        this.text = text;

        this.persistent = false;
        this.textColor = Color.WHITE;
        this.backgroundColor = Color.fromARGB(0,0,0,0);
        this.textAlignment = TextDisplay.TextAlignment.CENTER;
    }

    @Override
    protected void modifyDisplay() {
        TextDisplay display = this.getDisplay();
        display.text(Component.text(text, TextColor.color(textColor.asRGB())));
        display.setBackgroundColor(backgroundColor);
        display.setPersistent(persistent);
        display.setAlignment(textAlignment);
        display.setBillboard(billboard);
    }

    @Override
    public void redraw() {
        TextDisplay display = this.getDisplay();
        if(display == null) return;
        display.text(Component.text(text, TextColor.color(textColor.asRGB())));
        display.setBackgroundColor(backgroundColor);
        display.setPersistent(persistent);
        display.setTransformationMatrix(getTransform());
        display.setAlignment(textAlignment);
        display.setBillboard(billboard);
    }

    @Override
    public void onRemove() {}
}
