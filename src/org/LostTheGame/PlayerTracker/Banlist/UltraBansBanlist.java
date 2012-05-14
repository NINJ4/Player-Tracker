package org.LostTheGame.PlayerTracker.Banlist;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.LostTheGame.PlayerTracker.PlayerTracker;

import com.modcrafting.ultrabans.UltraBan;

public class UltraBansBanlist extends Banlist {
	private UltraBan banlistPlug;
	public HashSet<String> bannedPlayers = new HashSet<String>();
	public Map<String, Long> tempBans = new HashMap<String, Long>();

	public UltraBansBanlist( PlayerTracker instance, String type ) {
			// type = UltraBan or UltraBanLite ONLY
		this.plugin = instance;
		this.banlistPlug = (UltraBan) plugin.getServer().getPluginManager().getPlugin(type);

		this.bannedPlayers = banlistPlug.bannedPlayers;
		this.tempBans = banlistPlug.tempBans;
	}
	public boolean isBanned( String playername ) {
		if ( this.bannedPlayers.contains(playername.toLowerCase()) )
			return true;
		else if ( this.tempBans.get(playername.toLowerCase()) != null ) {
			long tempTime = this.tempBans.get(playername.toLowerCase());
			long diff = tempTime - (System.currentTimeMillis()/1000);
			if ( diff > 0 )
				return true;
		}
		return false;
	}
}
