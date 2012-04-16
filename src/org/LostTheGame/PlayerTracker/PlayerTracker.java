package org.LostTheGame.PlayerTracker;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.LostTheGame.PlayerTracker.Database.Database;
import org.LostTheGame.PlayerTracker.Database.MySQLDatabase;
import org.LostTheGame.PlayerTracker.Database.SQLiteDatabase;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.JSONException;

public class PlayerTracker extends JavaPlugin {
	
	String maindir = "plugins/Player-Tracker/";
	public FileConfiguration config;
	public FileConfiguration ban_config;
	private final LoginListenerTracker playerListener = new LoginListenerTracker(this);
	private Plugin banlist;
	
	boolean localdb;
	public boolean mysql = false;
	
	boolean mcbans;
	private MCBansIntegration bansConn;
	
	boolean mcbouncer;
	private MCBouncerIntegration bouncerConn;
	
	public List<String> untraceable;
	
	Database db;
	
	public static Logger log = Logger.getLogger("Minecraft");
	
    public void onEnable(){ 
    	new File(maindir).mkdir();
    	setupConfig();
    	
    	this.localdb = config.getBoolean("local-db", false);
    	this.mysql = config.getBoolean("mysql-enable", false);
    	this.mcbans = config.getBoolean("mcbans-enable", false);
    	this.mcbouncer = config.getBoolean("mcbouncer-enable", false);
    	
    	untraceable = config.getStringList( "untraceable-players" );

    	for(int i=0,l=untraceable.size();i<l;++i)
    	{
    	  untraceable.add(untraceable.remove(0).toLowerCase());
    	}

    	
        if ( localdb ) {
        	
        	if ( mysql ) {
	            try {
	                db = new MySQLDatabase(this);
					if (!( db.initialize() )) {
	                	log.warning("[P-Tracker]: Can't setup mySQL database.");
	                }
					else
						getServer().getPluginManager().registerEvents(playerListener, this);
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
				else
					getServer().getPluginManager().registerEvents(playerListener, this);
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
				log.severe( "[P-Tracker]: Can't initiate connection to MCBouncer!" + e );
				mcbouncer = false;
			} catch (IOException e) {
				log.severe( "[P-Tracker]: Can't initiate connection to MCBouncer!" + e );
				mcbouncer = false;
			}
        }
    	
        if ( ( !localdb ) && ( !mcbans ) && ( !mcbouncer ) ) {
        	log.warning("[P-Tracker]: No databases in use, disabling plugin.");
        	this.getServer().getPluginManager().disablePlugin(this);
        	return;
        }
        if ( this.getServer().getPluginManager().isPluginEnabled("FigAdmin") ) {
        	log.info("[P-Tracker] FigAdmin detected."); 
        	banlist = this.getServer().getPluginManager().getPlugin("FigAdmin");
        	ban_config = banlist.getConfig();
        	//log.info(  );
        }
    	log.info("[P-Tracker] Player-Tracker has been enabled.");
    }
    public void setupConfig() {
        this.config = getConfig();
        config.options().copyDefaults(true);
        saveConfig();

    }
     
    public void onDisable(){ 
    	if ( config.getBoolean("local-db", false) )
    		db.disconnect();
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

    private boolean IPTrack( String IP, CommandSender sender, boolean IPdisp ) {
    	if ( localdb ) {
    		db.IPTrack(IP, sender, IPdisp);
    	}
    	if ( mcbouncer ) {
    		//bouncerConn.IPTrack( IP );
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
	    	if ( ( mcbouncer ) || ( mcbans ) ) {
				sender.sendMessage(ChatColor.GREEN + "[P-Tracker] Player \""+ ChatColor.UNDERLINE + playername + ChatColor.RESET + ChatColor.GREEN + "\" has no known global bans.");
	    	}
    		return true;
    	}
    	else {
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
	    		if ( gbans == null ) {
	    			sender.sendMessage(ChatColor.GREEN +"[P-Tracker] No Global bans found.");
	    		}
	    		else {
	    			sender.sendMessage(ChatColor.GREEN +"[P-Tracker] "+ gbans.size() +" Global bans found.");
		    		for(String ban : gbans) {
		    			sender.sendMessage(ban);
		    		}
	    		}
	    	}
    	}
    	return true;
    }
    
    // This function called by the player-join listener:
    public String getNotifyLine( String playername ) {
    	String aliases = "";
    	String gbans = "";
    	if ( localdb ) {
    		if ( !untraceable.contains( playername.toLowerCase() ) ) {
	    		int acount;
	    		acount = db.AliasCount(playername);
	    		if ( acount != 0 )
	    			aliases = playername +" has "+ acount +" associated accounts. ";
    		}
    	}
    	
    	int banCount = 0;
    	int testint;
    	if ( mcbans ) {
    		testint = bansConn.banCount( playername );
    		if ( testint > -1 )
    			banCount += testint;
    		else
    			log.severe("[P-Tracker] Failed to getMCBans Bans: unknown error!");
    	}
    	else if ( mcbouncer ) {
    		testint = bouncerConn.banCount( playername );
    		if ( testint > -1 )
        		banCount += testint;
    		else
    			log.severe("[P-Tracker] Failed to getMCBouncer Bans: unknown error!");
    			
    	}
    	
    	if ( banCount > 0 )
    		gbans = playername +" has "+ banCount +" Global bans.";
    		
    	
    	
    	if ( ( aliases == "" ) && ( gbans  == "" ) )
    		return null;
    	else
    		return aliases + gbans;
    }

	// Add somebody to the config list of hidden players
public boolean hideAccount( String playername, CommandSender sender ) {
	untraceable = config.getStringList( "untraceable-players" );
	untraceable.add( playername );
	//this.getConfig().set( "untraceable-players", untraceable.toArray() //.asList(untraceable) );
	config.set( "untraceable-players", untraceable );
	this.saveConfig();
	for(int i=0,l=untraceable.size();i<l;++i)
	{
	  untraceable.add(untraceable.remove(0).toLowerCase());
	}
	sender.sendMessage(ChatColor.GREEN + "[P-Tracker] Successfully added " + ChatColor.UNDERLINE + playername + ChatColor.RESET + ChatColor.GREEN + " to the hidden players list.");
	return true;
}
	// Remove somebody from the config list of hidden players
	public boolean unhideAccount( String playername, CommandSender sender ) {
		List<String> UTList = config.getStringList( "untraceable-players" );
		untraceable.clear();
		String thisName;
		boolean changed = false;
		for(int i=0,l=UTList.size();i<l;++i) {
			thisName = UTList.remove(0); 
			if ( !thisName.equalsIgnoreCase( playername ) ) {
				UTList.add(thisName);
				untraceable.add( thisName.toLowerCase() );
			} else {
				changed = true;
			}
			  
		}
		config.set( "untraceable-players", UTList );
		this.saveConfig();
		if ( changed ) 
			sender.sendMessage(ChatColor.GREEN + "[P-Tracker] Successfully removed " + ChatColor.UNDERLINE + playername + ChatColor.RESET + ChatColor.GREEN + " from the hidden players list.");
		else 
			sender.sendMessage(ChatColor.GREEN + "[P-Tracker] Failed to removed " + ChatColor.UNDERLINE + playername + ChatColor.RESET + ChatColor.GREEN + " from the hidden players list.");
		
		return true;
	}
	@SuppressWarnings("unchecked")
	public boolean listHiddenAccounts( CommandSender sender ) {
		sender.sendMessage(ChatColor.GREEN + "[P-Tracker] Untraceable Players: "+ Arrays.asList(untraceable) );
		return true;
	}
}
