package ca.bungo.holos.api.animations;

import ca.bungo.holos.BungosHolos;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public abstract class Animation implements Cloneable, ConfigurationSerializable {

    private int tickTime;
    private boolean loopable;
    private String name;

    /**
     * Create a new animation instance
     * @param name Name of the animation created
     * @param tickTime How long does the animation last?
     * @param loopable Is this animation looping?
     * */
    public Animation(String name, int tickTime, boolean loopable) {
        this.name = name;
        this.tickTime = tickTime;
        this.loopable = loopable;
    }

    /**
     * Get the offset bassed off how many ticks have passed in the animation
     * @param tick Ticks passed in the animation
     * @return Vector3 (x, y, z) offset for the animation
     * */
    public abstract @NotNull Vector3f getPositionOffset(float tick);

    /**
     * Get the rotational offset based off how many ticks have passed in the animation
     * @param tick Ticks passed in the animation
     * @return Vector2 (yaw, pitch) rotation for the animation
     * */
    public abstract @NotNull Vector2f getRotationOffset(float tick);

    @Override
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("ticks", tickTime);
        data.put("loopable", loopable);
        Map<String, Object> uniqueData = new HashMap<>();
        saveUniqueContent(uniqueData);
        data.put("unique_data", uniqueData);
        return data;
    }

    public void deserializeGeneric(Map<String, Object> data) {
        this.tickTime = (int)data.get("ticks");
        this.loopable = (boolean)data.get("loopable");
    }

    /**
     * Create a new instance of the animation
     * */
    public abstract Animation clone();
    public abstract void saveUniqueContent(Map<String, Object> map);

}
