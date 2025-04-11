package ca.bungo.holos.api.holograms;

import org.bukkit.entity.Player;

public interface Editable {

    boolean onEdit(Player editor, String field, String... values);

}
