package org.LostTheGame.PlayerTracker.Banlist;

import org.LostTheGame.PlayerTracker.PlayerTracker;

public class VanillaBanlist extends Banlist {
	
	public VanillaBanlist( PlayerTracker instance ) {
		this.plugin = instance;
	}
	
	public boolean isBanned( String playername ) {
		return plugin.getServer().getOfflinePlayer( playername ).isBanned();
		//return false;
	}
}
