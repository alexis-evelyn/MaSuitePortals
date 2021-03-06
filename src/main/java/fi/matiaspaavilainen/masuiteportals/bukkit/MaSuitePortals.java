package fi.matiaspaavilainen.masuiteportals.bukkit;

import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import fi.matiaspaavilainen.masuitecore.bukkit.MaSuiteCore;
import fi.matiaspaavilainen.masuitecore.core.Updator;
import fi.matiaspaavilainen.masuitecore.core.configuration.BukkitConfiguration;
import fi.matiaspaavilainen.masuiteportals.bukkit.commands.DeleteCommand;
import fi.matiaspaavilainen.masuiteportals.bukkit.commands.ListCommand;
import fi.matiaspaavilainen.masuiteportals.bukkit.commands.SetCommand;
import fi.matiaspaavilainen.masuiteportals.bukkit.listeners.MovementListener;
import fi.matiaspaavilainen.masuiteportals.bukkit.listeners.PhysicsListener;
import fi.matiaspaavilainen.masuiteportals.bukkit.listeners.PortalsMessageListener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class MaSuitePortals extends JavaPlugin implements Listener {

    public WorldEditPlugin we = null;

    private BukkitConfiguration config = new BukkitConfiguration();

    @Override
    public void onEnable() {
        //Create configs
        config.create(this, "portals", "syntax.yml");
        config.create(this, "portals", "messages.yml");

        // Load WorldEdit
        we = (WorldEditPlugin) getServer().getPluginManager().getPlugin("WorldEdit");
        if (we == null) {
            System.out.println("[MaSuitePortals] WorldEdit not detected. Disabling...");
            getServer().getPluginManager().disablePlugin(this);
        }

        new Updator(new String[]{getDescription().getVersion(), getDescription().getName(), "62434"}).checkUpdates();
        
        // Register and load everything
        registerCommands();
        registerListener();
        initLists();
    }

    @Override
    public void onDisable() {
        PortalManager.portals.forEach(((world, portals) -> portals.forEach(Portal::clearPortal)));
    }

    /**
     * Register commands
     */
    private void registerCommands() {
        getCommand("setportal").setExecutor(new SetCommand(this));
        getCommand("delportal").setExecutor(new DeleteCommand(this));
        getCommand("portals").setExecutor(new ListCommand(this));
    }

    /**
     * Register listener
     */
    private void registerListener() {
        if (MaSuiteCore.bungee) {
            getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
            getServer().getMessenger().registerIncomingPluginChannel(this, "BungeeCord", new PortalsMessageListener(this));
        }

        getServer().getPluginManager().registerEvents(new MovementListener(this), this);
        getServer().getPluginManager().registerEvents(new PhysicsListener(), this);
        getServer().getPluginManager().registerEvents(this, this);

    }

    /**
     * Initialize lists for Portals
     */
    private void initLists() {
        getServer().getWorlds().forEach(world -> PortalManager.portals.put(world, new ArrayList<Portal>()));
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        // If list is empty when player joins, request portals
        if (PortalManager.portalNames.isEmpty()) {
            try (ByteArrayOutputStream b = new ByteArrayOutputStream();
                 DataOutputStream out = new DataOutputStream(b)) {
                out.writeUTF("MaSuitePortals");
                out.writeUTF("RequestPortals");
                getServer().getScheduler().runTaskLaterAsynchronously(this, () -> e.getPlayer().sendPluginMessage(this, "BungeeCord", b.toByteArray()), 100);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

}
