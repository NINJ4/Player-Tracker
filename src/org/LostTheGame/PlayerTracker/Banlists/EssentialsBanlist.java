package org.LostTheGame.PlayerTracker.Banlists;

import org.LostTheGame.PlayerTracker.PlayerTracker;
//import org.bukkit.entity.Player;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.User;

public class EssentialsBanlist extends Banlist {
	private Essentials banlistPlug;
	
	public EssentialsBanlist( PlayerTracker instance ) {
		this.plugin = instance;
		this.banlistPlug = (Essentials) plugin.getServer().getPluginManager().getPlugin("Essentials");
	}
	public boolean isBanned( String playername ) {
		//User user = banlistPlug.getUser( (Player) plugin.getServer().getOfflinePlayer( playername ) );
		User user = banlistPlug.getUser( playername );
		if ( user == null )
			return false;
		
		return user.isBanned();

	}
}
