package org.figuramc.figura.server.commands;


import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;

import java.util.function.Predicate;

public class FiguraServerCommands {
    private static final LiteralArgumentBuilder<FiguraServerCommandSource> AVATAR = FiguraAvatarCommand.getCommand();

    public static LiteralArgumentBuilder<FiguraServerCommandSource> getCommand() {
        return null;
    }

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
