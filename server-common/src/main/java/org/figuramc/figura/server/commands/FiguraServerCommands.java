package org.figuramc.figura.server.commands;


import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;

import java.util.function.Predicate;

public class FiguraServerCommands {

    public static final LiteralCommandNode<FiguraCommandSource> AVATAR = FiguraAvatarCommand.getCommand();

    public static class PermissionPredicate implements Predicate<FiguraCommandSource> {
        public final String permission;

        public PermissionPredicate(String permission) {
            this.permission = permission;
        }

        @Override
        public boolean test(FiguraCommandSource source) {
            return source.permission(permission);
        }
    }

    public static PermissionPredicate permissionCheck(String permission) {
        return new PermissionPredicate(permission);
    }
}
