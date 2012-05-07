package org.LostTheGame.PlayerTracker.RemoteIntegration;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.LostTheGame.PlayerTracker.PlayerTracker;
import org.bukkit.ChatColor;
import org.json.JSONException;
import org.json.JSONObject;

public class MineBansIntegration {
	private PlayerTracker plugin;
	private String APIKEY;
	private String mbVer = "&version=0.8.3";
	private String domain = "http://minebans.com/api.php?api_key=";
	private URL url;
	

	public MineBansIntegration(PlayerTracker instance) {
		this.plugin = instance;
		this.APIKEY = this.plugin.config.getString( "minebans-API-key", "0" );
	}
	
	public boolean init() {
		if ( APIKEY.equals("0") ) {
			PlayerTracker.log.severe("[P-Tracker] Invalid MineBans API-key (default setting has not been changed!)");
			return false;
		}
		if ( APIKEY.length() != 40 ) {
			PlayerTracker.log.severe("[P-Tracker] Invalid MineBans API-key (key must be 40 characters!)");
			return false;
		}
		try {
			this.url = new URL( this.domain + URLEncoder.encode(this.APIKEY, "UTF-8") + this.mbVer );
		} catch (MalformedURLException e) {
			PlayerTracker.log.warning("Minebans constructor: malformed URL you turd: "+ e);
			return false;
		} catch (UnsupportedEncodingException e) {
			PlayerTracker.log.warning("Minebans constructor: Unsupported encoding for URL: "+ e);
			return false;
		}
		
		if ( !this.isUp() )
			return false;
		
		return true;
		
	}
	private JSONObject API_request( String action ) {
		return API_request( action, null );
	}
	private JSONObject API_request( String action, String parameter ) {
		String data = null;
			// first build the output, then make the connection.
		try {
			JSONObject postdata = new JSONObject();
			postdata.put( "action", action );
			postdata.put( "issued_by", "console" );
			if ( parameter != null )
				postdata.put( "player_name", parameter );
			
			data = "request_data="+ postdata.toString();
		} catch (JSONException e1) {
			PlayerTracker.log.warning("[P-Tracker] Minebans API_request() failed: "+ e1);
		}
		
		String result = null;
		try {
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
		     result = buf.toString();
		     wr.close();
		     rd.close();
		     conn.getInputStream().close();
		     
		     if ( result.startsWith("E") ) {
		    	 PlayerTracker.log.warning("[P-Tracker] MineBans API error: " + result);
		    	 return null;
		     }
		    	 
		     
		     JSONObject returnVal = new JSONObject(result);
		     if ( returnVal.getBoolean("status") )
		    	 return returnVal;
		     else {
		    	 PlayerTracker.log.warning( "[P-Tracker] MineBans reports the system is down!" );
		    	 return null;
		     }
		     
	     } catch (Exception e) {
			PlayerTracker.log.warning( "[P-Tracker] MineBans Fetch Data Error" );
			if ( plugin.debug == true ) {
				PlayerTracker.log.warning( "Minebans request sent: "+data);
				if ( result != null )
					PlayerTracker.log.warning("Minebans response: " +result);
				e.printStackTrace();
			}
		}
		return null;
	}
	public boolean isUp() {
		JSONObject json = API_request( "get_system_status" );
		try {
			return json.getBoolean( "status" );
		} catch (JSONException e) { }
		
		return false;
	}
	public int banCount( String playername ) {
		try {
			JSONObject json = API_request( "get_player_bans", playername ).getJSONObject("player_info");
			if ( !json.has("ban_summary") )
				return 0;
			
			JSONObject json2 = json.getJSONObject( "ban_summary" );
			return json2.getInt("total");
		} catch (JSONException e) {	
			if ( plugin.debug ) {
				PlayerTracker.log.warning( "[P-Tracker] MineBans banCount error:" );
				e.printStackTrace();
			}
		}

		return -1;
	}
	public List<String> PlayerTrack( String playername ) {
		//{"status":true,"player_info":{"should_unban":false,"known_compromised":false,"total_bans":{"0":{"1":1}},"ban_summary":{"total":1}}}
		//"total_bans":{"0":{"1":1}}
		// reason:{severity:count}
		// severity 1-3 (low-high), 0 = unknown
		// reason 0 = theft
		// reason 1 = griefing
		// reason 2 = being "abusive"
		// reason 7 = block reach
		// reason 11 = spam client
		// reason 12 = item dropper
		try {
			JSONObject json = API_request( "get_player_join_info", playername ).getJSONObject("player_info");
			if ( !json.has("ban_summary") )
				return null;
			//int totalBans = json.getJSONObject("ban_summary").getInt("total");
			
			JSONObject bans = json.getJSONObject("total_bans");
			ArrayList<String> output = new ArrayList<String>();
			
			@SuppressWarnings("rawtypes")
			Iterator bansList = bans.keys();
			while ( bansList.hasNext() ) {
				String reason_code = (String) bansList.next();
				JSONObject banDetails = bans.getJSONObject( reason_code );
				StringBuffer thisBan = new StringBuffer();
				
					// interpret reason code
				String reason;
				if ( reason_code.equals("0") ) reason = "theft of items";
				else if ( reason_code.equals("1") ) reason = "griefing";
				else if ( reason_code.equals("2") ) reason = "acting abusively";
				else if ( reason_code.equals("7") ) reason = "griefing client";
				else if ( reason_code.equals("11") ) reason = "griefing client";
				else if ( reason_code.equals("12") ) reason = "griefing client";
				else reason = "Reason ID: "+ reason_code;
				
				@SuppressWarnings("rawtypes")
				Iterator banTypes = banDetails.keys();
				
				while ( banTypes.hasNext() ) {
					String severity_code = (String) banTypes.next();
					int severity_count = banDetails.getInt( severity_code );
					String severity;

					if ( severity_code.equals("1") ) severity = "low";
					else if ( severity_code.equals("2") ) severity = "medium";
					else if ( severity_code.equals("3") ) severity = "high";
					else severity = "unknown";
					
					if ( thisBan.length() != 0 )
						thisBan.append(", ");
					thisBan.append( severity_count +" "+ severity +" severity ban(s)" );
				}
				thisBan.append(".");
				thisBan.insert(0, ChatColor.DARK_GREEN +"   - Bans for " + reason +": ");
				
				output.add( thisBan.toString() );
			}
			
			
			return output;

		} catch (JSONException e) {	
			if ( plugin.debug )
				e.printStackTrace();
		}
		
		return null;
	}
	
}
