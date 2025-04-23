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

    public Animation(String name, int tickTime, boolean loopable) {
        this.name = name;
        this.tickTime = tickTime;
        this.loopable = loopable;
    }

    public abstract @NotNull Vector3f getPositionOffset(float tick);
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

    public abstract Animation clone();
    public abstract void saveUniqueContent(Map<String, Object> map);

}
