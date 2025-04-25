package ca.bungo.holos.api.holograms;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface Editable {

    /**
     * Trigger edit logic for the editable object
     * @param editor Who is editing this object
     * @param field What field is being edited (if null generally should return a help string)
     * @param values All the options for the supplied field
     * @return True if all editing logic goes through, false if anything goes wrong
     * */
    boolean onEdit(@NotNull Player editor, @Nullable String field, String... values);

    /**
     * List all options based off the currently selected field
     * @param option Current field being edited
     * @return List of option logic for the field
     * */
    List<String> options(String option);

    /**
     * List of all editable fields
     * @return Fields that are editable for this object
     * */
    List<String> fields();

}
