package ca.bungo.holos.abstracted;

public abstract class PacketService {

    private final PacketHelper helper;

    public PacketService(PacketHelper helper) {
        this.helper = helper;
    }

    public abstract String getVersion();

}
