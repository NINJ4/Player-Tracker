package org.LostTheGame.PlayerTracker.Commands;

import java.util.ArrayList;
import java.util.List;

import org.LostTheGame.PlayerTracker.MCBansIntegration;
import org.LostTheGame.PlayerTracker.MCBouncerIntegration;
import org.LostTheGame.PlayerTracker.MineBansIntegration;
import org.LostTheGame.PlayerTracker.PlayerTracker;
import org.LostTheGame.PlayerTracker.TrackerRunnables;
import org.LostTheGame.PlayerTracker.Database.Database;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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

	List<String> untraceable = new ArrayList<String>();
	List<String> untraceableIP = new ArrayList<String>();
	
	private Database db;

	public TrackExecutor( PlayerTracker instance ) {
		this.plugin = instance;
		this.minebans = instance.minebans;
		this.mcbans = instance.mcbans;
		this.mcbouncer = instance.mcbouncer;
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

	    		i++;
    		}
    		else if ( ( args[0].equalsIgnoreCase("stats") ) && ( localdb ) )
    			return db.localStats( sender );
    		
    		if ( args[i].matches("(?:\\d{1,3}\\.){3}\\d{1,3}") ) { 
    				// If yes, this is an IP, otherwise treat as a playername.
    			return IPTrack( args[i], sender, IPdisp, recursive, override );
    		}
    		else if ( args[i].matches("^([a-zA-Z0-9_]){1,31}") ) {
    			return PlayerTrack( args[i], sender, wildcard, IPdisp, recursive, override );
    		}
		}
		sender.sendMessage(ChatColor.GREEN + "[P-Tracker] Must supply a valid playername or IP address!");
		
		return false;
	}


    private boolean IPTrack( String ip, CommandSender sender, boolean IPdisp, boolean recursive, boolean override ) {   
    	
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
			TrackerRunnables ipTrack1 = new TrackerRunnables( ip, sender, IPdisp, recursive, override, false ) {
				public void run() {
					if ( localdb ) {
			    		ArrayList<String> result = db.IPTrack( playerORip, IPdisp, recursive, override);
			    		if ( result.size() > 0 ) {
			    			int rsize = result.size();
			    			sender.sendMessage( ChatColor.GREEN + "[P-Tracker] IP Address \""+ ChatColor.UNDERLINE + playerORip + ChatColor.RESET + ChatColor.GREEN + "\" is associated with the following "+ rsize +" account(s):");

				    		for ( int i = 0 ; i < rsize ; i++ ) {
				    			sender.sendMessage( ChatColor.DARK_GREEN +"    - "+ result.remove(0) );
				    		}
			    		}
			    		else
			    			sender.sendMessage( ChatColor.GREEN + "[P-Tracker] No accounts matched the IP "+playerORip );
			    	}
			    	if ( mcbouncer ) {
			    		//bouncerConn.IPTrack( IP );
			    	}
			    	return;
				}
			};
			new Thread(ipTrack1).start();
    	}
    	return true;
    }
    private boolean PlayerTrack( String playername, CommandSender sender, boolean wildcard, boolean IPdisp, boolean recursive, boolean override ) {

    	
    	if ( ( !override ) && ( untraceable.contains( playername.toLowerCase() ) ) ) {
PlayerTracker.log.warning("override:"+override);
    		if ( localdb ) {
				sender.sendMessage(ChatColor.GREEN + "[P-Tracker] Player \""+ ChatColor.UNDERLINE + playername + ChatColor.RESET + ChatColor.GREEN + "\" is not associated with any known accounts.");
    		}
	    	if ( ( mcbouncer ) || ( mcbans ) || ( minebans ) ) {
				sender.sendMessage(ChatColor.GREEN + "[P-Tracker] Player \""+ ChatColor.UNDERLINE + playername + ChatColor.RESET + ChatColor.GREEN + "\" has no known global bans.");
	    	}
    		return true;
    	}
    	else {
    		TrackerRunnables pTrack1 = new TrackerRunnables(playername, sender, IPdisp, recursive, override, wildcard) {
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
					    			sender.sendMessage( ChatColor.DARK_GREEN +"    - "+ result.remove(0) );
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
				return;
    			}
    		};
    		new Thread(pTrack1).start();
    	}
    	return true;
    }

}
