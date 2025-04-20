package ca.bungo.holos.api.animations.simple;

import ca.bungo.holos.BungosHolos;
import ca.bungo.holos.api.animations.Animation;
import ca.bungo.holos.api.holograms.Editable;
import ca.bungo.holos.utility.ComponentUtility;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class BounceSimpleAnimation extends Animation implements Editable {

    private Vector3f topOffset;
    private Vector3f bottomOffset;
    private float height;

    public BounceSimpleAnimation(int tickTime, float height) {
        super("bounce", tickTime, true);

        this.topOffset = new Vector3f(0, height, 0);
        this.bottomOffset = new Vector3f(0, -height, 0);
        this.height = height;
    }

    @Override
    public @NotNull Vector3f getPositionOffset(float tick) {
        float midPoint = getTickTime()/2.0f;
        Vector3f result = new Vector3f();
        if(tick < midPoint){
            topOffset.lerp(bottomOffset, tick / midPoint, result);
        }
        else {
            bottomOffset.lerp(topOffset, (tick - midPoint) / midPoint, result);
        }
        return result;
    }

    @Override
    public @NotNull Vector2f getRotationOffset(float tick) {
        return new Vector2f(0,0);
    }

    @Override
    public Animation clone() {
        return new BounceSimpleAnimation(getTickTime(), height);
    }

    @Override
    public boolean onEdit(@NotNull Player editor, @Nullable String field, String... values) {
        if(field == null){
            String editMessage = """
                    &cHere are the fields that you're able to edit for this Bounce Animation:
                    <hover:show_text:'&eClick to edit field'><click:suggest_command:'/holo edit animation height VALUE'>&banimation height Number &e- Set the height of the bounce
                    <hover:show_text:'&eClick to edit field'><click:suggest_command:'/holo edit animation time VALUE'>&banimation time Ticks &e- Set the length of the animation in ticks
                    <hover:show_text:'&eClick to edit field'><click:suggest_command:'/holo edit animation loop VALUE'>&banimation loop True|False &e- Set whether the animation should loop""";
            editor.sendMessage(ComponentUtility.convertToComponent(editMessage));
            return true;
        }
        boolean succeeded = false;
        switch (field.toLowerCase()) {
            case "height" -> {
                if (values.length < 1) {
                    editor.sendMessage(ComponentUtility.convertToComponent("&cInvalid usage! Provide a valid height."));
                    return false;
                }
                try {
                    this.height = Float.parseFloat(values[0]);
                    this.topOffset = new Vector3f(0, height, 0);
                    this.bottomOffset = new Vector3f(0, -height, 0);
                    succeeded = true;
                } catch (NumberFormatException e) {
                    editor.sendMessage(ComponentUtility.convertToComponent("&cInvalid height! Must be a number."));
                    return false;
                }
            }
            case "time" -> {
                if (values.length < 1) {
                    editor.sendMessage(ComponentUtility.convertToComponent("&cInvalid usage! Provide a valid tick time."));
                    return false;
                }
                try {
                    this.setTickTime(Integer.parseInt(values[0]));
                    succeeded = true;
                } catch (NumberFormatException e) {
                    editor.sendMessage(ComponentUtility.convertToComponent("&cInvalid time! Must be an integer."));
                    return false;
                }
            }
            case "loop" -> {
                if (values.length < 1) {
                    editor.sendMessage(ComponentUtility.convertToComponent("&cInvalid usage! Provide true or false for loop."));
                    return false;
                }
                this.setLoopable(Boolean.parseBoolean(values[0]));
                succeeded = true;
            }
        }
        return succeeded;
    }

    @Override
    public List<String> options(String option) {
        return switch (option.toLowerCase()) {
            case "height" -> List.of("<number>");
            case "time" -> List.of("<ticks>");
            case "loop" -> List.of("true", "false");
            default -> List.of();
        };
    }

    @Override
    public List<String> fields() {
        return List.of("height", "time", "loop");
    }

    @Override
    public void saveUniqueContent(Map<String, Object> map) {
        map.put("height", height);
    }

    public static BounceSimpleAnimation deserialize(Map<String, Object> data) {
        Map<String, Object> uniqueData = (Map<String, Object>)data.get("unique_data");
        double height = (double)uniqueData.get("height");
        BounceSimpleAnimation animation = new BounceSimpleAnimation(20, (float)height);
        animation.deserializeGeneric(data);
        return animation;
    }

}
