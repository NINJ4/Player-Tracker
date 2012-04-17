package org.LostTheGame.PlayerTracker.Database;

import java.sql.Connection;
import java.sql.SQLException;

import org.bukkit.command.CommandSender;

public abstract class Database {
	private Connection conn = null;
	public boolean initialized;

	public void addTracks( String playername, String IP ) {
		//add some tracks to the database.
	}
	public boolean initialize() {
		// set up the connection and all that stuff.
		return false;
	}
    public void disconnect() {
    		try {
				conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
    }
    public String PlayerTrack( String playername, CommandSender sender, boolean wildcard, boolean IPdisp ) {
    	// track players
    	return "";
    }
    public boolean IPTrack( String IP, CommandSender sender, boolean IPdisp ) {
    	// track IPs
    	return false;
    }
    public int AliasCount( String playername ) {
    	// count aliases
    	return 0;
    }
    public boolean localStats( CommandSender sender ) {
    	return false;
    }
    public void cleanUp() {
    	// clean up old entries in the db
    }
}
