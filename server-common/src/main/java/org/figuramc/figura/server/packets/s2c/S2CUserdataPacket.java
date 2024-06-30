package org.figuramc.figura.server.packets.s2c;

import org.figuramc.figura.server.avatars.UserdataAvatar;
import org.figuramc.figura.server.packets.Packet;
import org.figuramc.figura.server.utils.IFriendlyByteBuf;
import org.figuramc.figura.server.utils.Identifier;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;

public class S2CUserdataPacket implements Packet {
    public static final Identifier PACKET_ID = new Identifier("figura", "userdata");

    private final UUID target;
    private final BitSet prideBadges;
    private final HashMap<String, UserdataAvatar> avatars;

    public S2CUserdataPacket(UUID target, BitSet prideBadges, HashMap<String, UserdataAvatar> avatars) {
        this.target = target;
        this.prideBadges = prideBadges;
        this.avatars = avatars;
    }

    public S2CUserdataPacket(IFriendlyByteBuf byteBuf) {
        this.target = byteBuf.readUUID();
        this.prideBadges = BitSet.valueOf(byteBuf.readByteArray(Integer.MAX_VALUE));
        avatars = new HashMap<>();
        int avatarsCount = byteBuf.readVarInt();
        for (int i = 0; i < avatarsCount; i++) {
            String avatarId = new String(byteBuf.readByteArray(Integer.MAX_VALUE), UTF_8);
            byte[] hash = byteBuf.readHash();
            byte[] ehash = byteBuf.readHash();
            avatars.put(avatarId, new UserdataAvatar(hash, ehash));
        }
    }

    public UUID target() {
        return target;
    }

    public BitSet prideBadges() {
        return prideBadges;
    }

    public HashMap<String, UserdataAvatar> avatars() {
        return avatars;
    }

    @Override
    public void write(IFriendlyByteBuf byteBuf) {
        byteBuf.writeUUID(target);
        byteBuf.writeByteArray(prideBadges.toByteArray());
        byteBuf.writeVarInt(avatars.size());
        for (Map.Entry<String, UserdataAvatar> avatar: avatars.entrySet()) {
            byteBuf.writeByteArray(avatar.getKey().getBytes(UTF_8));
            byteBuf.writeBytes(avatar.getValue().hash());
            byteBuf.writeBytes(avatar.getValue().ehash());
        }
    }

    @Override
    public Identifier getId() {
        return PACKET_ID;
    }
}
