package org.figuramc.figura.server.commands;

import com.google.gson.JsonElement;
import org.figuramc.figura.server.FiguraServer;
import org.figuramc.figura.server.FiguraUser;

import java.util.UUID;

public abstract class FiguraServerCommandSource {
    public abstract FiguraServer getServer();
    public abstract UUID getExecutorUUID();
    public abstract FiguraUser getExecutor();
    public abstract boolean permission(String permission);
    public abstract void sendMessage(String message);
    public abstract void sendComponent(JsonElement message);
}
