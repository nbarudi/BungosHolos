package ca.bungo.holos.api.holograms.simple;

import ca.bungo.holos.api.holograms.Editable;
import ca.bungo.holos.api.holograms.Hologram;
import ca.bungo.holos.api.holograms.SimpleHologram;
import ca.bungo.holos.utility.ComponentUtility;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Color;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class TextSimpleHologram extends SimpleHologram<TextDisplay> {

    private String text;

    private boolean persistent;
    private Display.Billboard billboard = Display.Billboard.FIXED;
    private Color backgroundColor;
    private TextDisplay.TextAlignment textAlignment;


    public TextSimpleHologram(String text) {
        super(TextDisplay.class);
        this.text = text;

        this.persistent = false;
        this.backgroundColor = Color.fromARGB(0,0,0,0);
        this.textAlignment = TextDisplay.TextAlignment.CENTER;
    }

    @Override
    protected void modifyDisplay() {
        TextDisplay display = this.getDisplay();
        display.setLineWidth(10000);
        display.text(ComponentUtility.convertToComponent(text));
        display.setBackgroundColor(backgroundColor);
        display.setPersistent(persistent);
        display.setAlignment(textAlignment);
        display.setBillboard(billboard);
    }

    @Override
    public void redraw() {
        TextDisplay display = this.getDisplay();
        if(display == null) return;
        display.text(ComponentUtility.convertToComponent(text));
        display.setBackgroundColor(backgroundColor);
        display.setPersistent(persistent);
        display.setTransformationMatrix(getTransform());
        display.setAlignment(textAlignment);
        display.setBillboard(billboard);
    }

    @Override
    public void onRemove() {}

    @Override
    public boolean onEdit(@NotNull Player editor, @Nullable String field, String... values) {
        if(field == null){
            String editMessage = ComponentUtility.format(
                    """
                    &eHere are the fields that you're able to edit for this Text Hologram:
                    <hover:show_text:'&eClick to edit field'><click:suggest_command:'/holo edit text VALUE'>&btext String &e- Text of the hologram
                    <hover:show_text:'&eClick to edit field'><click:suggest_command:'/holo edit addline VALUE'>&baddline String &e- Add a line to the hologram
                    <hover:show_text:'&eClick to edit field'><click:suggest_command:'/holo edit removeline VALUE'>&bremoveline Number &e- Remove a line from the hologram
                    <hover:show_text:'&eClick to edit field'><click:suggest_command:'/holo edit lines'>&blines &e- List current lines
                    <hover:show_text:'&eClick to edit field'><click:suggest_command:'/holo edit bgcolor'>&bbgcolor Number Number Number Number &e- Background color of the hologram (Alpha, Red, Green, Blue)
                    <hover:show_text:'&eClick to edit field'><click:suggest_command:'/holo edit billboard Center'>&bbillboard Vertical|Horizontal|Center|Fixed &e- How the hologram follows the player
                    <hover:show_text:'&eClick to edit field'><click:suggest_command:'/holo edit textalign Center'>&btextalign Left|Right|Center|Fixed &e- How to align new text lines
                    <hover:show_text:'&eClick to edit field'><click:suggest_command:'/holo edit persist'>&bpersist True|False &e- Is the hologram permanent""");

            editor.sendMessage(ComponentUtility.convertToComponent(editMessage));
            return super.onEdit(editor, null, values);
        }
        boolean succeeded = false;
        switch (field.toLowerCase()){
            case "text":
                if(values.length == 0){
                    succeeded = true;
                    this.setText("");
                    this.redraw();
                    editor.sendMessage(Component.text("Set the text to: ").append(ComponentUtility.convertToComponent(text)));
                    break;
                }
                StringBuilder textBuilder = new StringBuilder();
                for(String value : values){
                    textBuilder.append(value).append(" ");
                }
                this.setText(textBuilder.substring(0, textBuilder.length()-1));
                this.redraw();
                editor.sendMessage(Component.text("Set the text to: ").append(ComponentUtility.convertToComponent(text)));
                succeeded = true;
                break;
            case "addline":
                StringBuilder addLineBuilder = new StringBuilder();
                for(String value : values){
                    addLineBuilder.append(value).append(" ");
                }
                if(addLineBuilder.isEmpty()){
                    String newText = text + "\n";
                    this.setText(newText);
                    this.redraw();
                    editor.sendMessage(Component.text("Added an empty line to the hologram.", NamedTextColor.YELLOW));
                    succeeded = true;
                    break;
                }
                String newText = text + "\n" + addLineBuilder.substring(0, addLineBuilder.length()-1);
                this.setText(newText);
                this.redraw();
                editor.sendMessage(Component.text("Added the line: ")
                        .append(ComponentUtility.convertToComponent(addLineBuilder.substring(0, addLineBuilder.length()-1))));
                succeeded = true;
                break;
            case "removeline":
                if(values.length == 0){
                    editor.sendMessage(Component.text("You must supply a number!", NamedTextColor.RED));
                    break;
                }
                try {
                    int index = Integer.parseInt(values[0]);
                    List<String> lines = getLines();
                    lines.remove(index);
                    StringBuilder builder = new StringBuilder();
                    for(String line : lines){
                        builder.append(line).append("\n");
                    }

                    if(!builder.isEmpty())
                        this.setText(builder.substring(0, builder.length()-1));
                    else
                        this.setText("");

                    this.redraw();
                    editor.sendMessage(Component.text("Removed line " + index, NamedTextColor.YELLOW));
                    succeeded = true;
                } catch (NumberFormatException e) {
                    editor.sendMessage(Component.text("Invalid number!", NamedTextColor.RED));
                }
                break;
            case "lines":
                editor.sendMessage(Component.text("Here are the current lines and their indexes:", NamedTextColor.YELLOW));
                List<String> lines = getLines();
                for(int i = 0; i < lines.size(); i++){
                    editor.sendMessage(Component.text(i,NamedTextColor.YELLOW).append(ComponentUtility.convertToComponent("&e - &r" + lines.get(i))));
                }
                succeeded = true;
                break;
            case "bgcolor":
                if(values.length != 4){
                    editor.sendMessage(Component.text("Bad syntax: Need 4 number values!", NamedTextColor.RED));
                    break;
                }
                try {
                    int a = Integer.parseInt(values[0]);
                    int r = Integer.parseInt(values[1]);
                    int g = Integer.parseInt(values[2]);
                    int b = Integer.parseInt(values[3]);
                    Color color = Color.fromARGB(a, r, g, b);
                    this.setBackgroundColor(color);
                    this.redraw();
                    succeeded = true;
                    editor.sendMessage(Component.text("Changed background color to ", NamedTextColor.YELLOW)
                            .append(Component.text("THIS", TextColor.color(color.asRGB()))));
                } catch (NumberFormatException e){
                    editor.sendMessage(Component.text("One of your numbers is invalid!", NamedTextColor.RED));
                }
                break;
            case "billboard":
                if(values.length == 0){
                    editor.sendMessage(Component.text("Invalid Billboard Type /holo edit to see types!", NamedTextColor.RED));
                    break;
                }
                String billboardType = values[0];
                try {
                    Display.Billboard toChange = Display.Billboard.valueOf(billboardType);
                    this.setBillboard(toChange);
                    this.redraw();
                    succeeded = true;
                    editor.sendMessage(Component.text("Set billboard type to: " + toChange.name(), NamedTextColor.YELLOW));
                } catch(IllegalArgumentException e){
                    editor.sendMessage(Component.text("Invalid Billboard Type /holo edit to see types!", NamedTextColor.RED));
                }
                break;
            case "textalign":
                if(values.length == 0){
                    editor.sendMessage(Component.text("Invalid Text Alignment Type /holo edit to see types!", NamedTextColor.RED));
                    break;
                }
                String textAlign = values[0];
                try {
                    TextDisplay.TextAlignment toChange = TextDisplay.TextAlignment.valueOf(textAlign);
                    this.setTextAlignment(toChange);
                    this.redraw();
                    succeeded = true;
                    editor.sendMessage(Component.text("Set text alignment type to: " + toChange.name(), NamedTextColor.YELLOW));
                } catch(IllegalArgumentException e){
                    editor.sendMessage(Component.text("Invalid Text Align Type /holo edit to see types!", NamedTextColor.RED));
                }
                break;
            case "persist":
                if(values.length == 0){
                    editor.sendMessage(Component.text("Invalid Persistance Setting! Must be True or False", NamedTextColor.RED));
                    break;
                }
                String persist = values[0];
                boolean toChange = Boolean.parseBoolean(persist);
                this.setPersistent(toChange);
                this.redraw();
                editor.sendMessage(Component.text("Set persistence to " + persist, NamedTextColor.YELLOW));
                succeeded = true;
                break;
        }

        return succeeded ? succeeded : super.onEdit(editor, field, values);
    }

    private List<String> getLines() {
        return new ArrayList<>(List.of(text.split("\n")));
    }
}
