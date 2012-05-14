package org.LostTheGame.PlayerTracker.Commands;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.LostTheGame.PlayerTracker.JsonReader;
import org.LostTheGame.PlayerTracker.PlayerTracker;
import org.LostTheGame.PlayerTracker.TrackerRunnables;
import org.LostTheGame.PlayerTracker.Database.Database;
import org.LostTheGame.PlayerTracker.RemoteIntegration.MCBansIntegration;
import org.LostTheGame.PlayerTracker.RemoteIntegration.MCBouncerIntegration;
import org.LostTheGame.PlayerTracker.RemoteIntegration.MineBansIntegration;
import org.LostTheGame.PlayerTracker.RemoteIntegration.glizerIntegration;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.json.JSONObject;

public class TrackExecutor implements CommandExecutor {
	private PlayerTracker plugin;
	boolean localdb;
	public boolean mysql = false;
	
	boolean mcbans;
	private MCBansIntegration bansConn;
	
	boolean mcbouncer;
	private MCBouncerIntegration bouncerConn;
	
	boolean minebans;
	private MineBansIntegration mineConn;
	
	boolean glizer;
	private glizerIntegration glizerConn;

	List<String> untraceable = new ArrayList<String>();
	List<String> untraceableIP = new ArrayList<String>();
	
	private Database db;

	public TrackExecutor( PlayerTracker instance ) {
		this.plugin = instance;
		this.minebans = instance.minebans;
		this.mcbans = instance.mcbans;
		this.mcbouncer = instance.mcbouncer;
		this.glizer = instance.glizer;
		this.glizerConn = instance.glizerConn;
		this.mineConn = instance.mineConn;
		this.bansConn = instance.bansConn;
		this.bouncerConn = instance.bouncerConn;
		this.db = instance.db;
		this.untraceableIP = instance.untraceableIP;
		this.untraceable = instance.untraceable;
		this.localdb = instance.localdb;
		
		
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String commandLabel,
			String[] args) {
		int i = 0;    	
    	

		boolean override = false;
		boolean wildcard = true;
		boolean IPdisp = false;
		boolean recursive = false;
		boolean geolocate = false;
		
		if ( args.length > 0 ) {
			
	    	if ( sender instanceof Player ) {
		    	Player player = (Player) sender;
		    	if ( player.hasPermission("playertracker.hidetracks.override") ) {
		    		override = true;
		    	}
	    	}
	    	else
	    		override = true;
	    	
    		if ( args[i].startsWith("-") ) {
    				// This is a flag!
    			if ( args[i].contains("a") ) {
    					// absolute names.
    				wildcard = false;
    			}
    			if ( args[i].contains("i") ) {
					// display IPs
    				IPdisp = true;
    			}
    			if ( args[i].contains("r") ) {
					// display IPs
    				recursive = true;
    			}
    			if ( args[i].contains("g") ) {
					// display IPs
    				geolocate = true;
    			}

	    		i++;
    		}
    		else if ( ( args[0].equalsIgnoreCase("stats") ) && ( localdb ) )
    			return db.localStats( sender );
    		
    		if ( args[i].matches("(?:\\d{1,3}\\.){3}\\d{1,3}") ) { 
    				// If yes, this is an IP, otherwise treat as a playername.
    			return IPTrack( args[i], sender, IPdisp, recursive, override, geolocate );
    		}
    		else if ( args[i].matches("^([a-zA-Z0-9_]){1,31}") ) {
    			return PlayerTrack( args[i], sender, wildcard, IPdisp, recursive, override, geolocate );
    		}
		}
		sender.sendMessage(ChatColor.GREEN + "[P-Tracker] Must supply a valid playername or IP address!");
		
		return false;
	}


    private boolean IPTrack( String ip, CommandSender sender, boolean IPdisp, boolean recursive, boolean override, boolean geolocate ) {   
    	
    	if ( ( !override ) && ( untraceableIP.contains( ip ) ) ) {
    		if ( localdb ) {
				sender.sendMessage(ChatColor.GREEN + "[P-Tracker] IP Address \""+ ChatColor.UNDERLINE + ip + ChatColor.RESET + ChatColor.GREEN + "\" is not associated with any known accounts.");
    		}
	    	/*if ( ( mcbouncer ) || ( mcbans ) || ( minebans ) ) {
				sender.sendMessage(ChatColor.GREEN + "[P-Tracker] Player \""+ ChatColor.UNDERLINE + playername + ChatColor.RESET + ChatColor.GREEN + "\" has no known global bans.");
	    	}*/
    		return true;
    	}
    	else {
			TrackerRunnables ipTrack1 = new TrackerRunnables( ip, sender, IPdisp, recursive, override, false, geolocate ) {
				public void run() {
					if ( localdb ) {
			    		ArrayList<String> result = db.IPTrack( playerORip, IPdisp, recursive, override);
			    		if ( result.size() > 0 ) {
			    			int rsize = result.size();
			    			sender.sendMessage( ChatColor.GREEN + "[P-Tracker] IP Address \""+ ChatColor.UNDERLINE + playerORip + ChatColor.RESET + ChatColor.GREEN + "\" is associated with the following "+ rsize +" account(s):");

				    		for ( int i = 0 ; i < rsize ; i++ ) {
				    			StringBuffer msg = new StringBuffer();
				    			msg.append(ChatColor.DARK_GREEN +"    - ");
				    			String player = result.remove(0);
				    			msg.append( player );
				    			if ( ( plugin.banlistEnabled ) && ( plugin.banlist.isBanned( player ) ) )
				    				msg.append( ChatColor.BOLD + " (BANNED)");
				    			sender.sendMessage( msg.toString() );
				    		}
			    		}
			    		else
			    			sender.sendMessage( ChatColor.GREEN + "[P-Tracker] No accounts matched the IP "+playerORip );
			    	}
			    	if ( mcbouncer ) {
			    		//bouncerConn.IPTrack( IP );
			    	}
			    	if ( geolocate ) {
			        	try {
			        	    InetAddress addr = InetAddress.getByName( playerORip );

			        	    // Get the host name
			        	    String hostname = addr.getHostName();
			        	    
			        	    String url = "http://freegeoip.net/json/"+ addr.getHostAddress();
							JSONObject json = JsonReader.readJsonFromUrl( url );
							if ( json.has("city") ) {
								sender.sendMessage(ChatColor.GREEN +"[P-Tracker] "+ hostname +
													" maps to "+ json.getString("city") +", "+
													json.getString("region_name") +" ("+
													json.getString("country_code") +")"
								);
							}
							else if ( json.has("region_name") ) {
								sender.sendMessage(ChatColor.GREEN +"[P-Tracker] "+ hostname +
													" maps to "+ json.getString("region_name") +" ("+
													json.getString("country_code") +")"
								);
								
							}
							else if ( json.has("country_name") ) {
								sender.sendMessage(ChatColor.GREEN +"[P-Tracker] "+ hostname +
										" maps to "+ json.getString("country_name") );
							}
							else {
								sender.sendMessage(ChatColor.GREEN +"[P-Tracker] Geolocation failed: Geo-IP database returned invalid or inaccurate data.");
							}

			        	} catch (UnknownHostException e) {
			        		sender.sendMessage("[P-Tracker] Geolocation failed: Unknown Host Exception!");
			        		PlayerTracker.log.warning("[P-Tracker] Geolocation failed: Unknown Host Exception!");
			        		if ( plugin.debug )
			        			e.printStackTrace();
			        	} catch (Exception e) {
			        		sender.sendMessage(ChatColor.GREEN +"[P-Tracker] Geolocation failed: Geo-IP database failed to respond appropriately.");
			        		if ( plugin.debug )
			        			e.printStackTrace();
			        	}
			    	}
			    	return;
				}
			};
			new Thread(ipTrack1).start();
    	}
    	return true;
    }
    private boolean PlayerTrack( String playername, CommandSender sender, boolean wildcard, boolean IPdisp, boolean recursive, boolean override, boolean geolocate ) {

    	
    	if ( ( !override ) && ( untraceable.contains( playername.toLowerCase() ) ) ) {
    		if ( localdb ) {
				sender.sendMessage(ChatColor.GREEN + "[P-Tracker] Player \""+ ChatColor.UNDERLINE + playername + ChatColor.RESET + ChatColor.GREEN + "\" is not associated with any known accounts.");
    		}
	    	if ( ( mcbouncer ) || ( mcbans ) || ( minebans ) ) {
				sender.sendMessage(ChatColor.GREEN + "[P-Tracker] Player \""+ ChatColor.UNDERLINE + playername + ChatColor.RESET + ChatColor.GREEN + "\" has no known global bans.");
	    	}
    		return true;
    	}
    	else {
    		TrackerRunnables pTrack1 = new TrackerRunnables(playername, sender, IPdisp, recursive, override, wildcard, geolocate) {
    			public void run() {
			    	if ( localdb ) {
			    			
			    		ArrayList<String> result = db.PlayerTrack(playerORip, IPdisp, recursive, override, wildcard);
			    		String newplayer = null;
			    		
			    		if ( ( result == null ) && ( wildcard ) ) {
			    			newplayer = db.wildcardMatch(playerORip, override);
			    			if ( newplayer != null) {
								sender.sendMessage(ChatColor.GREEN + "[P-Tracker] No known accounts matched exactly \""+ playerORip +",\" trying wildcard search...");
								if ( ( !override ) && ( untraceable.contains( newplayer.toLowerCase() ) ) ) {
					    			sender.sendMessage( ChatColor.GREEN + "[P-Tracker] No known accounts associated with \""+ ChatColor.UNDERLINE + newplayer + ChatColor.RESET + ChatColor.GREEN + "\"");
								}
								else {
				    				result = db.PlayerTrack(newplayer, IPdisp, recursive, override, wildcard);
								}
			    				playerORip = newplayer;
			    			}
			    			else
			    				sender.sendMessage(ChatColor.GREEN + "[P-Tracker] No known accounts match or contain \""+ playerORip +"\"");
			    		}
			    		else if ( ( result == null ) && ( !wildcard) )
		    				sender.sendMessage(ChatColor.GREEN + "[P-Tracker] No known accounts match \""+ playerORip +"\"");

			    		if ( result != null) {
				    		if ( result.size() > 0 ) {
				    			int rsize = result.size();
				    			sender.sendMessage(ChatColor.GREEN +"[P-Tracker] "+ rsize +" account(s) are associated with \""+ ChatColor.UNDERLINE + playerORip + ChatColor.RESET + ChatColor.GREEN +"\"" );
					    		for ( int i = 0 ; i < rsize ; i++ ) {
					    			StringBuffer msg = new StringBuffer();
					    			msg.append(ChatColor.DARK_GREEN +"    - ");
					    			String player = result.remove(0);
					    			msg.append( player );
					    			if ( ( plugin.banlistEnabled ) && ( plugin.banlist.isBanned( player ) ) )
					    				msg.append( ChatColor.BOLD + " (BANNED)");
					    			sender.sendMessage( msg.toString() );
					    		}
				    		}
				    		else
				    			sender.sendMessage( ChatColor.GREEN + "[P-Tracker] No known accounts associated with \""+ ChatColor.UNDERLINE + playerORip + ChatColor.RESET + ChatColor.GREEN + "\"");
			    		}
			    	}
			    	if ( ( mcbouncer ) || ( mcbans ) || ( minebans ) ) {
						if ( ( !override ) && ( untraceable.contains( playerORip.toLowerCase() ) ) ) {
			    			sender.sendMessage(ChatColor.GREEN +"[P-Tracker] No Global bans found.");
							return;
						}
			    		ArrayList<String> gbans = new ArrayList<String>();
			    		if ( mcbouncer ) {
			    			List<String> mcbanlist = bouncerConn.PlayerTrack(playerORip, sender);
			    			if ( mcbanlist != null )
			    				gbans.addAll( mcbanlist );
			    		}
			    		if ( mcbans ) {
			    			List<String> mcbanlist = bansConn.PlayerTrack(playerORip, sender);
			    			if ( mcbanlist != null )
			    				gbans.addAll( mcbanlist );
			    		}
			    		if ( minebans ) {
			    			List<String> minebanlist = mineConn.PlayerTrack( playerORip );
			    			if ( minebanlist != null )
			    				gbans.addAll( minebanlist );
			    		}
			    		if ( glizer ) {
			    			List<String> glizelist = glizerConn.PlayerTrack( playerORip );
			    			if ( glizelist != null )
			    				gbans.addAll( glizelist );
			    		}
			    		if ( gbans.size() == 0 ) {
			    			sender.sendMessage(ChatColor.GREEN +"[P-Tracker] No Global bans found.");
			    		}
			    		else {
			    			sender.sendMessage(ChatColor.GREEN +"[P-Tracker] "+ gbans.size() +" Global bans found.");
				    		for(String ban : gbans) {
				    			sender.sendMessage(ban);
				    		}
			    		}
			    	}
			    	/*if ( geolocate ) {
			        	try {
			        	    InetAddress addr = InetAddress.getByName("68.45.26.92");

			        	    // Get the host name
			        	    String hostname = addr.getHostName();
			        	} catch (UnknownHostException e) {
			        		sender.sendMessage("[P-Tracker] Geolocation failed: Unknown Host Exception!");
			        		PlayerTracker.log.warning("[P-Tracker] Geolocation failed: Unknown Host Exception!");
			        		if ( plugin.debug )
			        			e.printStackTrace();
			        	}
			    	}*/
				return;
    			}
    		};
    		new Thread(pTrack1).start();
    	}
    	return true;
    }

}
