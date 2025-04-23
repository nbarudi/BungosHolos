package ca.bungo.holos.api.animations.simple;

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
public class VerticalSimpleAnimation extends Animation implements Editable {

    private float rotationSpeed;

    public VerticalSimpleAnimation(int tickTime, float rotationSpeed) {
        super("vertical", tickTime, true);
        this.rotationSpeed = rotationSpeed;
    }

    @Override
    public @NotNull Vector3f getPositionOffset(float tick) {
        // No position change for vertical rotation
        return new Vector3f(0, 0, 0);
    }

    @Override
    public @NotNull Vector2f getRotationOffset(float tick) {
        float midPoint = getTickTime()/2.0f;
        if(tick < midPoint){
            float perc = tick / midPoint;
            float pitch = 90.0f - perc * 180.0f;
            return new Vector2f(0, pitch * rotationSpeed);
        }
        else {
            float perc = (tick-midPoint) / midPoint;
            float pitch = perc * 180.0f - 90.0f;
            return new Vector2f(0, pitch * rotationSpeed);
        }
    }

    @Override
    public Animation clone() {
        return new VerticalSimpleAnimation(getTickTime(), rotationSpeed);
    }

    @Override
    public boolean onEdit(@NotNull Player editor, @Nullable String field, String... values) {
        if(field == null){
            String editMessage = """
                    &cHere are the fields that you're able to edit for this Vertical Animation:
                    <hover:show_text:'&eClick to edit field'><click:suggest_command:'/holo edit animation speed VALUE'>&banimation speed Number &e- Set the rotation speed
                    <hover:show_text:'&eClick to edit field'><click:suggest_command:'/holo edit animation time VALUE'>&banimation time Ticks &e- Set the length of the animation in ticks
                    <hover:show_text:'&eClick to edit field'><click:suggest_command:'/holo edit animation loop VALUE'>&banimation loop True|False &e- Set whether the animation should loop""";
            editor.sendMessage(ComponentUtility.convertToComponent(editMessage));
            return true;
        }
        boolean succeeded = false;
        switch (field.toLowerCase()) {
            case "speed" -> {
                if (values.length < 1) {
                    editor.sendMessage(ComponentUtility.convertToComponent("&cInvalid usage! Provide a valid speed."));
                    return false;
                }
                try {
                    this.rotationSpeed = Float.parseFloat(values[0]);
                    succeeded = true;
                } catch (NumberFormatException e) {
                    editor.sendMessage(ComponentUtility.convertToComponent("&cInvalid speed! Must be a number."));
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
            case "speed" -> List.of("<number>");
            case "time" -> List.of("<ticks>");
            case "loop" -> List.of("true", "false");
            default -> List.of();
        };
    }

    @Override
    public List<String> fields() {
        return List.of("speed", "time", "loop");
    }

    @Override
    public void saveUniqueContent(Map<String, Object> map) {
        map.put("rotationSpeed", rotationSpeed);
    }

    public static VerticalSimpleAnimation deserialize(Map<String, Object> data) {
        Map<String, Object> uniqueData = (Map<String, Object>)data.get("unique_data");
        double rotationSpeed = (double)uniqueData.get("rotationSpeed");
        VerticalSimpleAnimation animation = new VerticalSimpleAnimation(20, (float)rotationSpeed);
        animation.deserializeGeneric(data);
        return animation;
    }
}