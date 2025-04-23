package ca.bungo.holos.api.holograms;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface Editable {

    boolean onEdit(@NotNull Player editor, @Nullable String field, String... values);
    List<String> options(String option);
    List<String> fields();

}
