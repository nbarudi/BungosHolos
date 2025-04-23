package ca.bungo.holos.abstracted.v1_21_R1;

import ca.bungo.holos.abstracted.PacketHelper;
import ca.bungo.holos.abstracted.PacketService;

public class VersionedPacketService extends PacketService {

    public VersionedPacketService(PacketHelper helper) {
        super(helper);
    }

    @Override
    public String getVersion() {
        return "PacketService designed with 1.21.4 base.";
    }


}
