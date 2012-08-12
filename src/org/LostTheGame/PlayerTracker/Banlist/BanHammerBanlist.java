package org.LostTheGame.PlayerTracker.Banlist;

import name.richardson.james.bukkit.banhammer.BanHammer;
import name.richardson.james.bukkit.banhammer.BanHandler;

import org.LostTheGame.PlayerTracker.PlayerTracker;

public class BanHammerBanlist extends Banlist {
	BanHandler banhandler;
	
	public BanHammerBanlist( PlayerTracker instance ) {
		this.plugin = instance;
		this.banhandler = ( (BanHammer) plugin.getServer().getPluginManager().getPlugin("BanHammer") ).getHandler();
	}
	
	public boolean isBanned( String playername ) {
		return this.banhandler.isPlayerBanned( playername );
	}
}
