package ca.bungo.holos.abstracted.v1_21_R1;

import ca.bungo.holos.abstracted.PacketHelper;
import ca.bungo.holos.abstracted.PacketService;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.craftbukkit.entity.CraftDisplay;
import net.minecraft.world.entity.Display;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class VersionedPacketService extends PacketService {

    public VersionedPacketService(PacketHelper helper) {
        super(helper);
    }

    @Override
    public String getVersion() {
        return "PacketService designed with 1.21.4 base.";
    }

    @Override
    public boolean updateEntityData(Player player, int entityID, org.bukkit.entity.Display display) {
        CraftDisplay craftDisplay = (CraftDisplay) display;
        Display nmsDisplay = craftDisplay.getHandle();

        List<SynchedEntityData.DataValue<?>> entityData = nmsDisplay.getEntityData().getNonDefaultValues();
        if(entityData == null || entityData.isEmpty()) entityData = new ArrayList<>();

        ClientboundSetEntityDataPacket packet = new ClientboundSetEntityDataPacket(entityID, entityData);
        sendPacket(player, packet);

        return true;
    }

    private void sendPacket(Player player, Packet<?> packet) {
        ServerPlayer serverPlayer = ((CraftPlayer)player).getHandle();
        serverPlayer.connection.send(packet);
    }

}
