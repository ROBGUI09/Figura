package org.figuramc.figura.server.packets.handlers.c2s;

import org.figuramc.figura.server.FiguraServer;
import org.figuramc.figura.server.FiguraUser;
import org.figuramc.figura.server.avatars.EHashPair;
import org.figuramc.figura.server.packets.c2s.C2SFetchUserdataPacket;
import org.figuramc.figura.server.packets.s2c.S2CUserdataPacket;
import org.figuramc.figura.server.utils.Hash;
import org.figuramc.figura.server.utils.IFriendlyByteBuf;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class C2SFetchUserdataPacketHandler extends AuthorizedC2SPacketHandler<C2SFetchUserdataPacket> {

    public C2SFetchUserdataPacketHandler(FiguraServer parent) {
        super(parent);
    }

    @Override
    protected void handle(FiguraUser sender, C2SFetchUserdataPacket packet) {
        FiguraUser user = parent.userManager().getUser(packet.target());
        // Future for a packet that will be sent when done
        UUID target = user.uuid();
        HashMap<String, EHashPair> avatars = new HashMap<>();
        BitSet badges = user.prideBadges();
        sender.sendPacket(new S2CUserdataPacket(target, badges, user.equippedAvatar()));
    }

    @Override
    public C2SFetchUserdataPacket serialize(IFriendlyByteBuf byteBuf) {
        return new C2SFetchUserdataPacket(byteBuf);
    }
}
