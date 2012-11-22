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
		boolean canrecurse = false;
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
		    	if ( player.hasPermission("playertracker.track.recursive") ) {
		    		canrecurse = true;
		    	}
	    	}
	    	else {
	    		override = true;
	    		canrecurse = true;
	    	}
	    	
			if ( args[0].equalsIgnoreCase("help") ) {
				sender.sendMessage(ChatColor.GREEN +"[P-Tracker] How to use Player-Tracker:");
				sender.sendMessage(ChatColor.GREEN +"   "+ ChatColor.UNDERLINE +"/track [-agir] <PLAYERNAME> or <IP ADDRESS>");
				sender.sendMessage(ChatColor.GREEN +"      - Shows all playernames associated with the given ");
				sender.sendMessage(ChatColor.GREEN +"         playername or IP address.");
				sender.sendMessage(ChatColor.GREEN +"      - Optionally include one or more of the search flags");
				sender.sendMessage(ChatColor.GREEN +"        [-agir] to change the output effect:");
				sender.sendMessage(ChatColor.GREEN +"         [-a] Disables wildcard searching of name fragments.");
				sender.sendMessage(ChatColor.GREEN +"         [-g] Attempts to geolocate the chosen player by");
				sender.sendMessage(ChatColor.GREEN +"               his/her most recent IP address.");
				sender.sendMessage(ChatColor.GREEN +"         [-i] Displays all IP addresses associated with the");
				sender.sendMessage(ChatColor.GREEN +"               associated playernames");
				if ( canrecurse ) {
					sender.sendMessage(ChatColor.GREEN +"         [-r] Enables recursive searching of all associated");
					sender.sendMessage(ChatColor.GREEN +"               accounts (may take a long time for some searches).");
				}
				sender.sendMessage(ChatColor.GREEN +"      ex. "+ ChatColor.UNDERLINE +"/track -ig 127.0.1.1");
				
				boolean perm = false;
				if ( sender instanceof Player ) {
			    	Player player = (Player) sender;
			    	if ( player.hasPermission("playertracker.hidetracks") ) 
			    		perm = true;
				}
				else
					perm = true;
				
				if ( perm ) {
		    		sender.sendMessage(ChatColor.GREEN +"   "+ ChatColor.UNDERLINE +"/hidetracks [list] or [PLAYERNAME] or [IP ADDRESS]");
		    		sender.sendMessage(ChatColor.GREEN +"      - Adds a player or IP address to the list of");
		    		sender.sendMessage(ChatColor.GREEN +"         untraceable players/IPs.");
		    		sender.sendMessage(ChatColor.GREEN +"      - When used as "+ ChatColor.UNDERLINE +"/hidetracks list"+ ChatColor.RESET + ChatColor.GREEN +", displays all");
		    		sender.sendMessage(ChatColor.GREEN +"         untraceable IPs and Playernames.");
		    		sender.sendMessage(ChatColor.GREEN +"   ex. "+ ChatColor.UNDERLINE +"/hidetracks Notch");
		    		sender.sendMessage(ChatColor.GREEN +"   "+ ChatColor.UNDERLINE +"/unhidetracks [PLAYERNAME] or [IP ADDRESS]");
		    		sender.sendMessage(ChatColor.GREEN +"      - Removes a player or IP address from the list of");
		    		sender.sendMessage(ChatColor.GREEN +"         untraceable players/IPs.");
		    		sender.sendMessage(ChatColor.GREEN +"   ex. "+ ChatColor.UNDERLINE +"/unhidetracks jeb_");
				}
	    		return true;
			}
	    	
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
    			if ( args[i].contains("r") && ( canrecurse ) ) {
					// enable recursive searching
    				recursive = true;
    			}
    			if ( args[i].contains("g") ) {
					// display geolocation/hostmask
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
		    		String geoIP = null;
			    	if ( localdb ) {
			    		
			    		ArrayList<String> result = db.PlayerTrack(playerORip, IPdisp, recursive, override, wildcard, geolocate);
			    		String newplayer = null;
			    		
			    		if ( ( result == null ) && ( wildcard ) ) {
			    			newplayer = db.wildcardMatch(playerORip, override);
			    			if ( newplayer != null) {
								sender.sendMessage(ChatColor.GREEN + "[P-Tracker] No known accounts matched exactly \""+ playerORip +",\" trying wildcard search...");
								if ( ( !override ) && ( untraceable.contains( newplayer.toLowerCase() ) ) ) {
					    			sender.sendMessage( ChatColor.GREEN + "[P-Tracker] No known accounts associated with \""+ ChatColor.UNDERLINE + newplayer + ChatColor.RESET + ChatColor.GREEN + "\"");
								}
								else {
				    				result = db.PlayerTrack(newplayer, IPdisp, recursive, override, wildcard, geolocate);
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
				    			sender.sendMessage(ChatColor.GREEN +"[P-Tracker] "
				    							+ ( (geolocate) ? (rsize - 1) : rsize )
				    							+" account(s) are associated with \""+ ChatColor.UNDERLINE + playerORip + ChatColor.RESET + ChatColor.GREEN +"\"" );
				    			
					    		for ( int i = 0 ; i < rsize ; i++ ) {
					    			String player = result.remove(0);
					    			
					    			if ( geolocate ) { // if this was a geolocation, then an IP is stuck in here hopefully at the top.
					    				if ( player.matches("(?:\\d{1,3}\\.){3}\\d{1,3}") ) {
					    					geoIP = player;
					    					continue;
					    				}
					    			}
					    			StringBuffer msg = new StringBuffer();
					    			msg.append(ChatColor.DARK_GREEN +"    - ");
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
			    	if ( geolocate ) {
			    		if ( geoIP != null ) {
				        	try {
				        	    InetAddress addr = InetAddress.getByName( geoIP );

				        	    // Get the host name
				        	    String hostname = addr.getHostName();
				        	    
				        	    String url = "http://freegeoip.net/json/"+ addr.getHostAddress();
								JSONObject json = JsonReader.readJsonFromUrl( url );
								if ( !json.getString("city").isEmpty() ) {
									sender.sendMessage(ChatColor.GREEN +"[P-Tracker] "+ChatColor.UNDERLINE+ hostname +ChatColor.RESET + ChatColor.GREEN +
														" maps to "+ json.getString("city") +", "+
														json.getString("region_name") +" ("+
														json.getString("country_code") +")"
									);
								}
								else if ( !json.getString("region_name").isEmpty() ) {
									sender.sendMessage(ChatColor.GREEN +"[P-Tracker] "+ChatColor.UNDERLINE+ hostname +ChatColor.RESET + ChatColor.GREEN +
														" maps to "+ json.getString("region_name") +" ("+
														json.getString("country_code") +")"
									);
									
								}
								else if ( !json.getString("country_name").isEmpty() ) {
									sender.sendMessage(ChatColor.GREEN +"[P-Tracker] "+ChatColor.UNDERLINE+ hostname +ChatColor.RESET + ChatColor.GREEN +
											" maps to "+ json.getString("country_name") );
								}
								else {
									sender.sendMessage(ChatColor.GREEN +"[P-Tracker] Geolocation failed: Geo-IP database returned invalid or inaccurate data.");
								}

				        	} catch (UnknownHostException e) {
				        		sender.sendMessage(ChatColor.GREEN +"[P-Tracker] Geolocation failed: Unknown Host Exception!");
				        		PlayerTracker.log.warning("[P-Tracker] Geolocation failed: Unknown Host Exception!");
				        		if ( plugin.debug )
				        			e.printStackTrace();
				        	} catch (Exception e) {
				        		sender.sendMessage(ChatColor.GREEN +"[P-Tracker] Geolocation failed: Geo-IP database failed to respond appropriately.");
				        		if ( plugin.debug )
				        			e.printStackTrace();
				        	}
			    		}
			    		else {
			    			sender.sendMessage(ChatColor.GREEN +"[P-Tracker] Geolocation failed: No IP on file for this Player.");
			    		}
			    	}
				return;
    			}
    		};
    		new Thread(pTrack1).start();
    	}
    	return true;
    }

}
