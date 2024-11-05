package org.figuramc.figura.forge;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.permission.PermissionAPI;
import net.minecraftforge.server.permission.events.PermissionGatherEvent;
import net.minecraftforge.server.permission.nodes.PermissionDynamicContext;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import net.minecraftforge.server.permission.nodes.PermissionTypes;
import org.figuramc.figura.server.FiguraModServer;
import org.figuramc.figura.server.FiguraPermissions;
import org.figuramc.figura.server.utils.Pair;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class FiguraForgePermissions {

    public PermissionNode<Boolean> createNode(Pair<String, Boolean> pair) {
        String name = pair.left();
        boolean defaultVal = pair.right();
        return new PermissionNode<>(FiguraModServer.MOD_ID, name, PermissionTypes.BOOLEAN, (a,b,c) -> defaultVal);
    }

    @SuppressWarnings("unchecked")
    @SubscribeEvent
    public void registerPermissions(PermissionGatherEvent.Nodes event) {
        event.addNodes(((Iterable<PermissionNode<?>>) FiguraPermissions.PERMISSIONS_LIST.stream().map(this::createNode)));
    }
}
