package org.LostTheGame.PlayerTracker;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class LoginListenerTracker implements Listener {  
	PlayerTracker plugin;

    public LoginListenerTracker(PlayerTracker instance) {
        this.plugin = instance;
    }
	
	@EventHandler(priority = EventPriority.LOW)
    public void onPlayerJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		String playername = player.getDisplayName();
		String ip = player.getAddress().getAddress().getHostAddress();

		if ( plugin.localdb )
			plugin.db.addTracks(playername, ip);
		
        Player[] players = plugin.getServer().getOnlinePlayers();
    	String notify = plugin.getNotifyLine( playername );
    	if ( notify != null ) {
	        for (Player user : players) {
	            if (user.hasPermission("playertracker.onJoin")) {
	                user.sendMessage( ChatColor.YELLOW +"[P-Tracker] "+ notify );
	            }
	        }
    	}
    }
}
