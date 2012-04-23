package org.LostTheGame.PlayerTracker.Banlist;

import org.LostTheGame.PlayerTracker.PlayerTracker;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.bans.BanDatabase;
import com.sk89q.commandbook.bans.BansComponent;


public class CommandBookBanlist extends Banlist {
	BanDatabase bandb;

	public CommandBookBanlist( PlayerTracker instance ) {
		this.plugin = instance;
		CommandBook bans_plugin = (CommandBook) this.plugin.getServer().getPluginManager().getPlugin("CommandBook");
		this.bandb = bans_plugin.getComponentManager().getComponent(BansComponent.class).getBanDatabase();
	}
	public boolean isBanned( String playername ) {
		return bandb.isBannedName( playername );
	}
}
