package org.LostTheGame.PlayerTracker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MCBansIntegration {
	private PlayerTracker plugin;
	private String APIKEY;
	//private boolean initiated = false;
	private String domain = "http://72.10.39.172/v2/";

	public MCBansIntegration ( PlayerTracker instance ) {
		this.plugin = instance;
	}
	public boolean init() throws JSONException, IOException {
		this.APIKEY = plugin.config.getString("mcbans-API-key", "0");
		if ( APIKEY == "0" ) {
			PlayerTracker.log.severe("[P-Tracker] Invalid MCBans API-key (default setting has not been changed!)");
			return false;
		}
		if ( APIKEY.length() != 40 ) {
			PlayerTracker.log.severe("[P-Tracker] Invalid MCBans API-key (key must be 40 characters!)");
			return false;
		}

		if ( banCount( "Obliviatorness" ) > -1 ) {
	//		this.initiated = true;	
			return true;
		}
		
		PlayerTracker.log.severe( "[P-Tracker] Cannot retrieve MCBans data: Unknown error!" );
		return false;
	}
	private String request_from_api(String data){		
		try {
			URL url = new URL(domain + this.APIKEY);
		     URLConnection conn = url.openConnection();
		     conn.setConnectTimeout(5000);
		     conn.setReadTimeout(5000);
		     conn.setDoOutput(true);
		     OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
		     wr.write(data);
		     wr.flush();
		     StringBuilder buf = new StringBuilder();
		     BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		     String line;
		     while ((line = rd.readLine()) != null) {
		    	 buf.append(line);
		     }
		     String result = buf.toString();
		     wr.close();
		     rd.close();
		     return result;
	     } catch (Exception e) {
			PlayerTracker.log.warning( "[P-Tracker] MCBans Fetch Data Error" );
			if ( plugin.debug == true )
				e.printStackTrace();
		}
		return "";
	}
	public JSONObject getBans( String playername ) {
		String s = request_from_api("player="+ playername +"&admin=server&exec=playerLookup");
		try {
			JSONObject json = new JSONObject(s);
			return json;
		} catch (JSONException e) {
			PlayerTracker.log.warning( "[P-Tracker] Cannot convert retrieved MCBans data to JSON, is MCBans down/unreachable? " );
			if ( plugin.debug == true )
				e.printStackTrace();
		}
		return null;
	}
	public int banCount( String playername ) {
		JSONObject raw = getBans( playername );
		try {
			if ( raw == null )
				return -1;
			
			int banCount = raw.getInt("total");
			if ( banCount == 0 ) {
				return 0;
			}
			JSONArray data = raw.getJSONArray("global");
						
			return data.length();
			
		} catch (JSONException e) {
			PlayerTracker.log.severe("[P-Tracker] Failed to getBans: "+e);
			return -1;
		}
	}
	
	public List<String> PlayerTrack( String playername, CommandSender sender ) {
		JSONObject raw = getBans( playername );
		ArrayList<String> output = new ArrayList<String>();
		try {
			int banCount = raw.getInt("total");
			if ( banCount < 1 ) {
				return null;
			}
			JSONArray data = raw.getJSONArray("global");
						
			for ( int i = 0 ; i < data.length() ; i++ ) {
				// it's a string-array
				String banString =	ChatColor.DARK_GREEN +"   - "+ data.getString(i);
				if ( data.getString(i) != null )
					output.add(banString);
			}
			return output;
		} catch (JSONException e) {
			PlayerTracker.log.severe("[P-Tracker] Failed to getBans: "+e);
			return null;
		}
	}
}
