package org.LostTheGame.PlayerTracker.Banlists;

import org.LostTheGame.PlayerTracker.PlayerTracker;

//import com.btbb.figadmin.FigAdmin;


public class FigAdminBanlist extends Banlist {
	public FigAdminBanlist( PlayerTracker instance ) {
		this.plugin = instance;
	}
	/*public boolean isBanned( String playername ) {
		FigAdmin banlistPlug = (FigAdmin) plugin.getServer().getPluginManager().getPlugin("FigAdmin");
		banlistPlug.
		return false;
	}*/
}
