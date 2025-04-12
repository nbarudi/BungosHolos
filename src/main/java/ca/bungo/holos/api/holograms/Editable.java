package ca.bungo.holos.api.holograms;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface Editable {

    boolean onEdit(@NotNull Player editor, @Nullable String field, String... values);

}
