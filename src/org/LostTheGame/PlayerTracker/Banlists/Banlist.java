package org.LostTheGame.PlayerTracker.Banlists;

import org.LostTheGame.PlayerTracker.PlayerTracker;

public abstract class Banlist {
	PlayerTracker plugin;

	public boolean isBanned( String playername ) {
		return false;
	}
}
