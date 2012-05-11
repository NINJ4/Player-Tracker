package org.LostTheGame.PlayerTracker;

import java.util.ArrayList;

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

		if ( plugin.localdb ) {
			TrackerRunnables addT = new TrackerRunnables( playername, ip ) {
				public void run() {
					plugin.db.addTracks(playername, ip);
					return;
				}
			};
			new Thread(addT).start();
		}
		
		if ( plugin.msgonJoin ) {
	        Player[] players = plugin.getServer().getOnlinePlayers();
	        ArrayList<Player> notifyUs = new ArrayList<Player>();
	    
		    for (Player user : players) {
		    	if (user.hasPermission("playertracker.onJoin")) {
		    		notifyUs.add(user);
		        }
		    }
		    if ( notifyUs.size() > 0 ) {
		    	TrackerRunnables Tnotify = new TrackerRunnables( playername, ip, notifyUs ) {
		    		public void run() {
				    	String notify = plugin.getNotifyLine( playername, ip );
				    	if ( notify != null )
					       	for (Player user : notifyUs ) {
					       		user.sendMessage( ChatColor.YELLOW +"[P-Tracker] "+ notify );
					       	}
				    	
				    	return;
		    		}
		    	};
		    	new Thread( Tnotify ).start();
		    }
		}
    }
}
