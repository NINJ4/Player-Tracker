package org.LostTheGame.PlayerTracker.RemoteIntegration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.LostTheGame.PlayerTracker.PlayerTracker;
import org.bukkit.ChatColor;
import org.json.JSONException;
import org.json.JSONObject;

public class glizerIntegration {
	private PlayerTracker plugin;
	private String domain = "http://api.glizer.de/";
	private URL url;
	private String APIKEY;
	
	public glizerIntegration(PlayerTracker instance) {
		this.plugin = instance;
		this.APIKEY = this.plugin.config.getString( "glizer-API-key", "0" );
	}
	
	public boolean init() {
		if ( APIKEY.equals("0") ) {
			PlayerTracker.log.severe("[P-Tracker] Invalid glizer API-key (default setting has not been changed!)");
			return false;
		}
		if ( APIKEY.length() != 32 ) {
			PlayerTracker.log.severe("[P-Tracker] Invalid glizer API-key (key must be 32 characters!)");
			return false;
		}
		try {
			this.url = new URL( this.domain + URLEncoder.encode(this.APIKEY, "UTF-8") + "/" );
		} catch (MalformedURLException e) {
			PlayerTracker.log.warning("glizer constructor: malformed URL you turd: "+ e);
			return false;
		} catch (UnsupportedEncodingException e) {
			PlayerTracker.log.warning("glizer constructor: Unsupported encoding for URL: "+ e);
			return false;
		}

		if ( !this.isUp() )
			return false;
		
		return true;
	}
	public boolean isUp() {
		JSONObject json = API_request( "data" );
		try {
			if ( json.getInt("errno") != 1007 )
				return false;
		} catch (JSONException e) {
			PlayerTracker.log.warning( "[P-Tracker] glizer connection error!" );
			if ( plugin.debug )
				e.printStackTrace();
			
			return false;
		}
					
		return true;
	}//ip=1.1.1.1&account=server&exec=data
	public JSONObject API_request( String exec ) {
		return API_request( exec, null );
	}
	public JSONObject API_request( String exec, String name ) {
		StringBuffer data = new StringBuffer( "ip=1.1.1.1&account=server&exec=" );
		data.append( exec );
		if ( name != null )
			data.append( "&do=list&name="+ name );
		
		String result = null;
		try {
		     URLConnection conn = url.openConnection();
		     conn.setConnectTimeout(5000);
		     conn.setReadTimeout(5000);
		     conn.setDoOutput(true);
		     OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
		     wr.write( data.toString() );
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
	    	 
		     if ( result.equals("false") )
		    	 return null;
		     
		     JSONObject returnVal = new JSONObject(result);

		     return returnVal;
		     
	     } catch (IOException e) {
	    	 PlayerTracker.log.warning("[P-Tracker] IOException: glizer appears to be unavailable. Disabling glizer integration...");
	    	 plugin.glizer = false;
	    	 plugin.glizerConn = null;
	    	 if ( plugin.debug)
	    		 e.printStackTrace();
	    	 try {
				return new JSONObject("{ \"data\": \"0\" }");
			} catch (JSONException e1) {  }
	    	 
		} catch (Exception e) {
			PlayerTracker.log.warning( "[P-Tracker] glizer Fetch Data Error" );
			if ( plugin.debug ) {
				PlayerTracker.log.warning( "glizer request sent: "+data);
				if ( result != null )
					PlayerTracker.log.warning("glizer response: " +result);
				e.printStackTrace();
			}
		}
		return null;
		
	}
	public int banCount( String playername ) {
		try {
			JSONObject json = API_request( "profile", playername );
			if ( json == null )
				return 0;
			if ( !json.has("name") )
				return 0;
			
			return (int) Math.floor( json.getInt("reputation") / -10 );
		} catch (JSONException e) {	
			if ( plugin.debug ) {
				PlayerTracker.log.warning( "[P-Tracker] glizer banCount error:" );
				e.printStackTrace();
			}
		}

		return 0;
	}
	public List<String> PlayerTrack( String playername ) {
		//{"name":"raphix1998","nickname":"","realname":"",
		//"age":"0","status":"","mehr":"","lastip":"93.83.158.78",
		//"reputation":"-20","userreputation":"0","lastserver":"838",
		//"lastseen":"1335284389","developer":"0","profilepic":"",
		//"profileviews":"8","deleted":"0","allowmail":"1",
		//"disable_relation":"0","lastserverurl":"Minecamp.net:25565",
		//"lastserverport":"25565"}
		
		int banCount = this.banCount( playername );
		if ( banCount < 1 ) 
			return null;
		
		else {
			ArrayList<String> result = new ArrayList<String>();
			result.add(
					ChatColor.DARK_GREEN +"   - ~"+ banCount +" glizer bans found (no details available)."
			);
			return result;
		}

	}
}
