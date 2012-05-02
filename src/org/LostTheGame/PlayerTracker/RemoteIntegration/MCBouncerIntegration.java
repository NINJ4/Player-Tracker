package org.LostTheGame.PlayerTracker.RemoteIntegration;

import java.io.IOException;
import java.util.ArrayList;

import org.LostTheGame.PlayerTracker.JsonReader;
import org.LostTheGame.PlayerTracker.PlayerTracker;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

public class MCBouncerIntegration {
	private PlayerTracker plugin;
	private String APIKEY;
//	private boolean initiated = false;
	private String domain = "http://mcbouncer.com/api/";
	
	public MCBouncerIntegration ( PlayerTracker instance ) {
		this.plugin = instance;
	}
	
	public boolean init() throws JSONException, IOException {
		this.APIKEY = plugin.config.getString("mcbouncer-API-key", "0");
		if ( APIKEY == "0" ) {
			PlayerTracker.log.severe("[P-Tracker] Invalid MCBouncer API-key (default setting has not been changed!)");
			return false;
		}
		if ( APIKEY.length() != 32 ) {
			PlayerTracker.log.severe("[P-Tracker] Invalid MCBouncer API-key (key must be 32 characters!)");
			return false;
		}
		if ( banCount( "Notch" ) > -1 ) {
//			this.initiated = true;	
			return true;
		}
		return false;
	}
	
	private JSONObject callAPI( String function, String parameter ) throws IOException, JSONException {
		String url = domain + function +"/"+ APIKEY +"/"+ parameter; 
		return JsonReader.readJsonFromUrl( url );
	}
	
	public int banCount( String playername ) {
		JSONObject json;
		try {
			json = callAPI( "getBanCount", playername );
			if ( json == null )
				return -1;
			
			return json.getInt("totalcount");
		} catch (IOException e) {
			PlayerTracker.log.severe("[P-Tracker] Failed to getBanCount: "+e);
		} catch (JSONException e) {
			PlayerTracker.log.severe("[P-Tracker] Failed to getBanCount: "+e);
		}
		
		return -1;
	}
	
	//@SuppressWarnings("null")
	public ArrayList<String> PlayerTrack(String playername, CommandSender sender) {
		JSONObject raw = getBans( playername );
		ArrayList<String> output = new ArrayList<String>();
		
		try {
			int banCount = raw.getInt("totalcount");
			if ( banCount < 1 ) {
				//output.add(ChatColor.GREEN +"[P-Tracker] No MCBouncer bans found.");
				return null;
			}
			JSONArray data = raw.getJSONArray("data");
			JSONObject thisBan;
			
			//output.add(ChatColor.GREEN +"[P-Tracker] "+ raw.getInt("totalcount") +" MCBouncer bans found:");
			
			for ( int i = 0 ; i < data.length() ; i++ ) {
				thisBan = data.getJSONObject(i);
				String banString =	ChatColor.DARK_GREEN +"   - From "+ thisBan.getString("server") 
									+" for reason: "+ ChatColor.ITALIC + thisBan.getString("reason")
									+ ChatColor.RESET + ChatColor.DARK_GREEN +" at "
									+ thisBan.getString("time");
				if ( banString != null )
					output.add(banString);
			}
			return output;
		} catch (JSONException e) {
			PlayerTracker.log.severe("[P-Tracker] Failed to getBans: "+e);
			return null;
		}
	}
	public JSONObject getBans( String playername ) {
		try {
			return callAPI( "getBans", playername );
		} catch (IOException e) {
			PlayerTracker.log.severe("[P-Tracker] Failed to getBans:"+e);
			return null;
		} catch (JSONException e) {
			PlayerTracker.log.severe("[P-Tracker] Failed to getBans:"+e);
			return null;
		}
	}

}
