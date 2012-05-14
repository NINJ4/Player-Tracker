package org.LostTheGame.PlayerTracker.Database;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;

import org.LostTheGame.PlayerTracker.PlayerTracker;
import org.bukkit.command.CommandSender;

public abstract class Database {
	private Connection conn = null;
	public boolean initialized;
	protected boolean isMYSQL;
	String table;
	PlayerTracker plugin;

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
    public ArrayList<String> PlayerTrack( String playername, boolean IPdisp, boolean recursive, boolean override, boolean wildcard, boolean geolocate ) {    	// track players
    	return new ArrayList<String>();
    }
    public ArrayList<String> IPTrack( String ipaddr, boolean IPdisp, boolean recursive, boolean override ) {
    	// track IPs
    	return new ArrayList<String>();
    }
    public int AliasCount( String playername ) {
    	// count aliases
    	return 0;
    }
    public boolean localStats( CommandSender sender ) {
    	return false;
    }
    public String wildcardMatch( String playername, boolean override ) {
    	return null;
    }
    public void cleanUp() {
    	// clean up old entries in the db
    }
}
