package org.figuramc.figura.server.packets.handlers.c2s;

import org.figuramc.figura.server.FiguraServer;
import org.figuramc.figura.server.FiguraUser;
import org.figuramc.figura.server.events.Events;
import org.figuramc.figura.server.events.avatars.AvatarUploadEvent;
import org.figuramc.figura.server.packets.AllowIncomingStreamPacket;
import org.figuramc.figura.server.packets.CloseIncomingStreamPacket;
import org.figuramc.figura.server.packets.Packet;
import org.figuramc.figura.server.packets.c2s.C2SUploadAvatarPacket;
import org.figuramc.figura.server.utils.IFriendlyByteBuf;

import java.util.concurrent.CompletableFuture;

public class C2SUploadAvatarPacketHandler extends AuthorizedC2SPacketHandler<C2SUploadAvatarPacket> {
    protected C2SUploadAvatarPacketHandler(FiguraServer parent) {
        super(parent);
    }

    @Override
    protected void handle(FiguraUser sender, C2SUploadAvatarPacket packet) {
        // TODO
    }

    private int getNewAvatarsCount(FiguraUser user, String id) {
        return user.ownedAvatars().size() + (user.ownedAvatars().containsKey(id) ? 0 : 1);
    }

    @Override
    public C2SUploadAvatarPacket serialize(IFriendlyByteBuf byteBuf) {
        return new C2SUploadAvatarPacket(byteBuf);
    }
}
