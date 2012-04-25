package org.LostTheGame.PlayerTracker;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.LostTheGame.PlayerTracker.Banlist.BanHammerBanlist;
import org.LostTheGame.PlayerTracker.Banlist.Banlist;
import org.LostTheGame.PlayerTracker.Banlist.CommandBookBanlist;
import org.LostTheGame.PlayerTracker.Banlist.EssentialsBanlist;
import org.LostTheGame.PlayerTracker.Banlist.FigAdminBanlist;
import org.LostTheGame.PlayerTracker.Banlist.VanillaBanlist;
import org.LostTheGame.PlayerTracker.Database.Database;
import org.LostTheGame.PlayerTracker.Database.MySQLDatabase;
import org.LostTheGame.PlayerTracker.Database.SQLiteDatabase;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.JSONException;

public class PlayerTracker extends JavaPlugin {
	
	String maindir = "plugins/Player-Tracker/";
	public FileConfiguration config;
	public FileConfiguration ban_config;
	private final LoginListenerTracker playerListener = new LoginListenerTracker(this);
	public boolean debug = true;
	
	//@SuppressWarnings("unused")
	//private Plugin banPlugin;
	
	
	boolean localdb;
	public boolean mysql = false;
	
	boolean mcbans;
	private MCBansIntegration bansConn;
	
	boolean mcbouncer;
	private MCBouncerIntegration bouncerConn;
	
	boolean minebans;
	private MineBansIntegration mineConn;
	
	public List<String> untraceable = new ArrayList<String>();
	public List<String> untraceableIP = new ArrayList<String>();
	
	Database db;
	public Banlist banlist;
	public boolean banlistEnabled = false;
	
	public static Logger log = Logger.getLogger("Minecraft");
	
    public void onEnable(){ 
    	new File(maindir).mkdir();
    	setupConfig();
    	
    	this.localdb = config.getBoolean("local-db", true);
    	this.mysql = config.getBoolean("mysql-enable", false);
    	this.mcbans = config.getBoolean("mcbans-enable", false);
    	this.mcbouncer = config.getBoolean("mcbouncer-enable", false);
    	this.minebans = config.getBoolean("minebans-enable", false);
    	
    	List<String> untraceable_tmp = config.getStringList( "untraceable" );
log.warning(untraceable_tmp.toString());
    	for(int i=0,l=untraceable_tmp.size();i<l;++i) {
    		String thisEntry = untraceable_tmp.remove(0);
    		
    		if ( thisEntry.matches("(?:\\d{1,3}\\.){3}\\d{1,3}") )
    			untraceableIP.add( thisEntry );
    		else
    			untraceable.add( thisEntry.toLowerCase() );
    	}
    	
        if ( localdb ) {
        	
        	if ( mysql ) {
	            try {
	                db = new MySQLDatabase(this);
					if (!( db.initialize() )) {
	                	log.warning("[P-Tracker]: Can't setup mySQL database.");
	                }
					else if ( config.getInt("persistence-days", 60) > 0 )
							db.cleanUp();
	            } catch (Exception e) {
	                log.log(Level.CONFIG, "Unable to connect to database with provided info!");
	                log.severe("[P-Tracker]: Can't initiate connection to mySQL database.");
	                localdb = false;
	            }
        	}
        	else {
        		db = new SQLiteDatabase(this, "player-tracker", maindir );
				if ( !( db.initialize() )) {
                	log.warning("[P-Tracker]: Can't setup SQLite database.");
                }
				else if ( config.getInt("persistence-days", 60) > 0 )
						db.cleanUp();
        	}
        		
        }
        if ( mcbans ) {
        	bansConn = new MCBansIntegration(this);
        	try {
				mcbans = bansConn.init();
				if ( mcbans )
					log.info("[P-Tracker] MCBans connection successful.");
			} catch (JSONException e) {
				PlayerTracker.log.severe( "[P-Tracker] MCBans integration Error" );
				e.printStackTrace();
			} catch (IOException e) {
				PlayerTracker.log.severe( "[P-Tracker] MCBans integration Error" );
				e.printStackTrace();
			}
        }

        if ( mcbouncer ) {
        	bouncerConn = new MCBouncerIntegration(this);
        	try {
				mcbouncer = bouncerConn.init();
				if ( mcbouncer )
					log.info("[P-Tracker] MCBouncer connection successful.");
			} catch (JSONException e) {
				log.warning( "[P-Tracker]: Can't initiate connection to MCBouncer!" + e );
				mcbouncer = false;
			} catch (IOException e) {
				log.warning( "[P-Tracker]: Can't initiate connection to MCBouncer!" + e );
				mcbouncer = false;
			}
        }
        if ( minebans ) {
        	mineConn = new MineBansIntegration( this );
        	
			minebans = mineConn.init();
			if ( minebans )
				log.info("[P-Tracker] MineBans connection successful.");
			else
				log.warning("[P-Tracker]: Can't initiate connection to MineBans!");
        }
        
        // enable our events
		getServer().getPluginManager().registerEvents(playerListener, this);
    	
        if ( ( !localdb ) && ( !mcbans ) && ( !mcbouncer ) ) {
        	log.warning("[P-Tracker]: No databases in use, disabling plugin.");
        	this.getServer().getPluginManager().disablePlugin(this);
        	return;
        }
        
        	// banlist figuring:
        if ( this.getServer().getPluginManager().isPluginEnabled("FigAdmin") ) {
        		// This is shittily done, but it's the best I got without the source.
        	log.info("[P-Tracker] FigAdmin detected, attempting to grab banlist."); 
        	//banPlugin = this.getServer().getPluginManager().getPlugin("FigAdmin");
        	this.banlistEnabled = true;
        	this.banlist = new FigAdminBanlist( this );
        }
        else if ( this.getServer().getPluginManager().isPluginEnabled("BanHammer") ) {
        	log.info("[P-Tracker] BanHammer detected, attempting to use as banlist."); 
        	//banPlugin = this.getServer().getPluginManager().getPlugin("FigAdmin");
        	this.banlistEnabled = true;
        	this.banlist = new BanHammerBanlist( this );
        }
    	// Essentials/Commandbook have lowest priority, because they are not ban-specific plugins
        else if ( this.getServer().getPluginManager().isPluginEnabled("Essentials") ) {
        	log.info("[P-Tracker] Essentials detected, attempting to use as banlist."); 
        	this.banlistEnabled = true;
        	this.banlist = new EssentialsBanlist( this );
        }
        else if ( this.getServer().getPluginManager().isPluginEnabled("CommandBook") ) {
        	log.info("[P-Tracker] CommandBook detected, attempting to use as banlist."); 
        	this.banlistEnabled = true;
        	this.banlist = new CommandBookBanlist( this );
        }
        else {
        	log.info("[P-Tracker] no Banlist plugin detected, using Vanilla."); 
        	this.banlistEnabled = true;
        	this.banlist = new VanillaBanlist( this );
        }
    	log.info("[P-Tracker] Player-Tracker has been enabled.");
    }
    public void setupConfig() {
        this.config = this.getConfig();
    	config.options().copyDefaults(true);
        if ( config.contains("untraceable-players") ) {
        		// config from before 1.1.7
        	List<String> transfer = config.getStringList( "untraceable-players" );
        	config.set( "untraceable", transfer );
        	config.set( "untraceable-players", null );
        	saveConfig();
        	this.reloadConfig();
        }
        else
        	saveConfig();

    }
     
    public void onDisable(){ 
    	if ( localdb )
    		db.disconnect();
    	if ( ( banlist != null ) && ( banlist.isFig() ) )
    		( (FigAdminBanlist) banlist ).disableFig();
    	saveConfig();
    }

    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args){
    	if(cmd.getName().equalsIgnoreCase("track")){ 
    		int i = 0;
    		boolean wildcard = true;
    		boolean IPdisp = false;
    		
    		if ( args.length > 0 ) {
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

		    		i++;
	    		}
	    		else if ( ( args[0].equalsIgnoreCase("stats") ) && ( localdb ) )
	    			return db.localStats( sender );
	    		
	    		if ( args[i].matches("(?:\\d{1,3}\\.){3}\\d{1,3}") ) { 
	    				// If yes, this is an IP, otherwise treat as a playername.
	    			return IPTrack( args[i], sender, IPdisp );
	    		}
	    		else if ( args[i].matches("^([a-zA-Z0-9_]){1,31}") ) {
	    			return PlayerTrack( args[i], sender, wildcard, IPdisp );
	    		}
    		}
    		sender.sendMessage(ChatColor.GREEN + "[P-Tracker] Must supply a valid playername or IP address!");
    	}
    	else if (cmd.getName().equalsIgnoreCase("hidetracks")) { 
    		if (args.length == 1) {
    			if ( !args[0].equalsIgnoreCase( "list" ) )
    				return hideAccount( args[0], sender );
    			else {
    				if ( !( sender instanceof Player ) || ( ( (Player) sender ).hasPermission("playertracker.hidetracks.override") ) )
    					return listHiddenAccounts( sender );
    			}
    		}
    		else {
    			sender.sendMessage(ChatColor.GREEN + "[P-Tracker] Must supply a valid playername only!");
    			return true;
    		}
    	}
    	else if (cmd.getName().equalsIgnoreCase("unhidetracks")) { 
    		if (args.length == 1) {
    			return unhideAccount( args[0], sender );
    		}
    		else {
    			sender.sendMessage(ChatColor.GREEN + "[P-Tracker] Must supply a valid playername only!");
    			return true;
    		}
    	}
    	return false; 
    }

    private boolean IPTrack( String ip, CommandSender sender, boolean IPdisp ) {   
    	boolean override = false;
    	if ( sender instanceof Player ) {
	    	Player player = (Player) sender;
	    	if ( player.hasPermission("playertracker.hidetracks.override") ) {
	    		override = true;
	    	}
    	}
    	else
    		override = true;
    	
    	
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
			TrackerRunnables ipTrack1 = new TrackerRunnables( ip, sender, IPdisp ) {
				public void run() {
					if ( localdb ) {
			    		db.IPTrack(ip, sender, IPdisp);
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
    private boolean PlayerTrack( String playername, CommandSender sender, boolean wildcard, boolean IPdisp ) {
    	boolean override = false;
    	if ( sender instanceof Player ) {
	    	Player player = (Player) sender;
	    	if ( player.hasPermission("playertracker.hidetracks.override") ) {
	    		override = true;
	    	}
    	}
    	else
    		override = true;
    	
    	
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
    		TrackerRunnables pTrack1 = new TrackerRunnables(playername, sender, wildcard, IPdisp) {
    			public void run() {
			    	if ( localdb ) {
			    		String result;
			    		result = db.PlayerTrack(playername, sender, wildcard, IPdisp);
			    		if ( result != null )
			    			playername = result;
			    	}
			    	if ( ( mcbouncer ) || ( mcbans ) ) {
			    		ArrayList<String> gbans = new ArrayList<String>();
			    		if ( mcbouncer ) {
			    			List<String> mcbanlist = bouncerConn.PlayerTrack(playername, sender);
			    			if ( mcbanlist != null )
			    				gbans.addAll( mcbanlist );
			    		}
			    		if ( mcbans ) {
			    			List<String> mcbanlist = bansConn.PlayerTrack(playername, sender);
			    			if ( mcbanlist != null )
			    				gbans.addAll( mcbanlist );
			    		}
			    		if ( minebans ) {
			    			List<String> minebanlist = mineConn.PlayerTrack( playername );
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
    
    // This function called by the player-join listener:
    public String getNotifyLine( String playername ) {
    	String aliases = "";
    	String gbans = "";
		int acount = 0;
    	if ( localdb ) {
    		if ( !untraceable.contains( playername.toLowerCase() ) ) {
	    		acount = db.AliasCount(playername);
	    		if ( acount != 0 )
	    			aliases = playername +" has "+ acount +" associated accounts";
    		}
    	}
    	
    	int banCount = 0;
    	int testint;
    	if ( mcbans ) {
    		testint = bansConn.banCount( playername );
    		if ( testint > -1 )
    			banCount += testint;
    		else
    			log.warning("[P-Tracker] Failed to get banCount from MCBans: unknown error!");
    	}
    	if ( mcbouncer ) {
    		testint = bouncerConn.banCount( playername );
    		if ( testint > -1 )
        		banCount += testint;
    		else
    			log.warning("[P-Tracker] Failed to get banCount from MCBouncer: unknown error!");
    			
    	}
		if ( minebans ) {
			testint = mineConn.banCount( playername );
			if ( testint > -1 )
				banCount += testint;
			else
    			log.warning("[P-Tracker] Failed to get banCount from Minebans: unknown error!");

		}
  
    	
    	if ( banCount > 0 ) {
    		if ( acount < 1 )
    			gbans = playername +" has "+ banCount +" Global bans.";
    		else 
    			gbans = " and "+ banCount +" Global bans.";
    	}
    	else if ( aliases != "" )
    		aliases += ".";
    		
    	
    	
    	if ( ( aliases == "" ) && ( gbans  == "" ) )
    		return null;
    	else
    		return aliases + gbans;
    }

	// Add somebody to the config list of hidden players
public boolean hideAccount( String playername, CommandSender sender ) {
	List<String> temp = new ArrayList<String>( untraceableIP );
	temp.addAll(untraceable);
		
log.warning(temp.toString());
	temp.add( playername );
log.warning(temp.toString());

	config.set( "untraceable", temp );
	this.saveConfig();
	
	if ( playername.matches("(?:\\d{1,3}\\.){3}\\d{1,3}") )
		untraceableIP.add( playername );
	else
		untraceable.add( playername.toLowerCase() );

	sender.sendMessage(ChatColor.GREEN + "[P-Tracker] Successfully added " + ChatColor.UNDERLINE + playername + ChatColor.RESET + ChatColor.GREEN + " to the hidden players/IPs list.");
	return true;
}
	// Remove somebody from the config list of hidden players
	public boolean unhideAccount( String playername, CommandSender sender ) {
		List<String> UTList = new ArrayList<String>( untraceableIP );
		UTList.addAll(untraceable);
		
		
		if ( UTList.contains( playername.toLowerCase() ) ) {
			if ( playername.matches("(?:\\d{1,3}\\.){3}\\d{1,3}") )
				untraceableIP.remove( playername );
			else
				untraceable.remove( playername.toLowerCase() );
			
			UTList.remove( playername.toLowerCase() );
			sender.sendMessage(ChatColor.GREEN + "[P-Tracker] Successfully removed " + ChatColor.UNDERLINE + playername + ChatColor.RESET + ChatColor.GREEN + " from the hidden players list.");
		}
		else 
			sender.sendMessage(ChatColor.GREEN + "[P-Tracker] Failed to removed " + ChatColor.UNDERLINE + playername + ChatColor.RESET + ChatColor.GREEN + " from the hidden players list.");
		
		config.set( "untraceable", UTList );
		this.saveConfig();
		
		return true;
	}
	@SuppressWarnings("unchecked")
	public boolean listHiddenAccounts( CommandSender sender ) {
		sender.sendMessage(ChatColor.GREEN + "[P-Tracker] Untraceable Players: "+ Arrays.asList(untraceable) );
		sender.sendMessage(ChatColor.GREEN + "[P-Tracker] Untraceable IPs: "+ Arrays.asList(untraceableIP) );
		return true;
	}
}
