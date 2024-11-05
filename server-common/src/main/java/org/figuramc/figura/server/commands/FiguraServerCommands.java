package org.figuramc.figura.server.commands;


import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;

import java.util.function.Predicate;

public class FiguraServerCommands {
    public static final LiteralCommandNode<FiguraServerCommandSource> AVATAR = FiguraAvatarCommand.getCommand();

    public static class PermissionPredicate implements Predicate<FiguraServerCommandSource> {
        public final String permission;

        public PermissionPredicate(String permission) {
            this.permission = permission;
        }

        @Override
        public boolean test(FiguraServerCommandSource source) {
            return source.permission(permission);
        }
    }

    public static PermissionPredicate permissionCheck(String permission) {
        return new PermissionPredicate(permission);
    }
}
