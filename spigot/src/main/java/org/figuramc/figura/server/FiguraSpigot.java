package org.figuramc.figura.server;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.figuramc.figura.server.packets.Packets;
import org.figuramc.figura.server.packets.Side;
import org.figuramc.figura.server.utils.Identifier;

import java.util.ArrayList;
import java.util.Objects;

import static org.figuramc.figura.server.SpigotUtils.call;

public class FiguraSpigot extends JavaPlugin implements Listener {
    private FiguraServerSpigot srv;
    private BukkitTask tickTask;
    public static final boolean DEBUG = Objects.equals(System.getProperty("figuraDebug"), "true");

    private final ArrayList<Identifier> outcomingPackets = new ArrayList<>();

    @Override
    public void onEnable() {
        srv = new FiguraServerSpigot(this);
        var msg = getServer().getMessenger();
        Bukkit.getPluginManager().registerEvents(this, this);
        Packets.forEachPacket(((id, packetDescriptor) -> {
            Side side = packetDescriptor.side();
            if (side.sentBy(Side.SERVER)) {
                msg.registerOutgoingPluginChannel(this, id.toString());
                outcomingPackets.add(id);
            }
            if (side.sentBy(Side.CLIENT)) msg.registerIncomingPluginChannel(this, id.toString(), srv);
            srv.logDebug("Registered channel for %s".formatted(id));
        }));
        srv.init();
        tickTask = new BukkitTickRunnable().runTaskTimer(this, 0, 1);
    }

    @Override
    public void onDisable() {
        srv.close();
    }

    private static final Class<?>[] CHANNEL_ARGS = new Class[] {String.class};

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        var player = event.getPlayer();
        for (Identifier id: outcomingPackets) {
            call(player, "addChannel", CHANNEL_ARGS, id.toString());
            srv.logDebug("Registered %s for %s".formatted(id, player.getName()));
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        var player = event.getPlayer();
        for (Identifier id: outcomingPackets) {
            call(player, "removeChannel", CHANNEL_ARGS, id.toString());
            srv.logDebug("Unregistered %s for %s".formatted(id, player.getName()));
        }
        srv.userManager().onUserLeave(player.getUniqueId());
    }

    private class BukkitTickRunnable extends BukkitRunnable {

        @Override
        public void run() {
            srv.tick();
        }
    }
}
