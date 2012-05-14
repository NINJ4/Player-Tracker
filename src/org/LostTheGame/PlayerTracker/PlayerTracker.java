package org.LostTheGame.PlayerTracker;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.NamingException;

import org.LostTheGame.PlayerTracker.Banlist.BanHammerBanlist;
import org.LostTheGame.PlayerTracker.Banlist.Banlist;
import org.LostTheGame.PlayerTracker.Banlist.CommandBookBanlist;
import org.LostTheGame.PlayerTracker.Banlist.EssentialsBanlist;
import org.LostTheGame.PlayerTracker.Banlist.FigAdminBanlist;
import org.LostTheGame.PlayerTracker.Banlist.UltraBansBanlist;
import org.LostTheGame.PlayerTracker.Banlist.VanillaBanlist;
import org.LostTheGame.PlayerTracker.Commands.TrackExecutor;
import org.LostTheGame.PlayerTracker.Database.Database;
import org.LostTheGame.PlayerTracker.Database.MySQLDatabase;
import org.LostTheGame.PlayerTracker.Database.SQLiteDatabase;
import org.LostTheGame.PlayerTracker.RemoteIntegration.MCBansIntegration;
import org.LostTheGame.PlayerTracker.RemoteIntegration.MCBouncerIntegration;
import org.LostTheGame.PlayerTracker.RemoteIntegration.MineBansIntegration;
import org.LostTheGame.PlayerTracker.RemoteIntegration.glizerIntegration;
import org.LostTheGame.PlayerTracker.Util.DNSBL;
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
	protected final PlayerTracker plugin = this;
	private String updateVer;

	public boolean checkproxies;
	public boolean msgonJoin;
	
	private TrackExecutor execTrack;
	
	
	public boolean localdb;
	public boolean mysql = false;
	
	public boolean mcbans;
	public MCBansIntegration bansConn;
	
	public boolean mcbouncer;
	public MCBouncerIntegration bouncerConn;
	
	public boolean minebans;
	public MineBansIntegration mineConn;
	
	public boolean glizer;
	public glizerIntegration glizerConn;
	
	public List<String> untraceable = new ArrayList<String>();
	public List<String> untraceableIP = new ArrayList<String>();
	
	public Database db;
	public Banlist banlist;
	public boolean banlistEnabled = false;
	public DNSBL dnsblChecker;
	
	public static Logger log = Logger.getLogger("Minecraft");
	
    public void onEnable(){ 
    	
    	new File(maindir).mkdir();
    	setupConfig();
    	
    	this.updateVer = config.getString("alert-updates", "main");
    	this.localdb = config.getBoolean("local-db", true);
    	this.mysql = config.getBoolean("mysql-enable", false);
    	this.mcbans = config.getBoolean("mcbans-enable", false);
    	this.mcbouncer = config.getBoolean("mcbouncer-enable", false);
    	this.minebans = config.getBoolean("minebans-enable", false);
    	this.glizer = config.getBoolean("glizer-enable", false);
    	this.checkproxies = config.getBoolean("check-proxies", true);
    	this.msgonJoin = config.getBoolean("enable-onJoin", true);
    	
    	if ( this.updateVer.equalsIgnoreCase("main") || this.updateVer.equalsIgnoreCase("dev") )
    		this.checkUpdate();
    	
    	List<String> untraceable_tmp = config.getStringList( "untraceable" );
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
				log.warning("[P-Tracker] Can't initiate connection to MineBans! Disabling Minebans integration!");
        }
        if ( glizer ) {
        	glizerConn = new glizerIntegration( this );
        	
			glizer = glizerConn.init();
			if ( glizer )
				log.info("[P-Tracker] glizer connection successful.");
			else
				log.warning("[P-Tracker] Can't initiate connection to glizer! Disabling glizer integration!");
        }
    	
        if ( ( !localdb ) && ( !mcbans ) && ( !mcbouncer ) && ( !glizer ) && ( !minebans ) ) {
        	log.warning("[P-Tracker]: No databases in use, disabling plugin.");
        	this.getServer().getPluginManager().disablePlugin(this);
        	return;
        }
        
        	// create our DNSBL checker if we need one
        if ( this.checkproxies ) {
        	try {
				this.dnsblChecker = new DNSBL();

	        	this.dnsblChecker.addDNSBL("dnsbl.proxybl.org");
	        	this.dnsblChecker.addDNSBL("http.dnsbl.sorbs.net");
	        	this.dnsblChecker.addDNSBL("socks.dnsbl.sorbs.net");
	        	this.dnsblChecker.addDNSBL("misc.dnsbl.sorbs.net");
	        	this.dnsblChecker.addDNSBL("tor.dnsbl.sectoor.de");
			} catch (NamingException e) {
				plugin.checkproxies = false;
				if ( plugin.debug )
					e.printStackTrace();
			}
        }
        
        // enable our events
		getServer().getPluginManager().registerEvents(playerListener, this);
        
        	// banlist figuring:
        if ( this.getServer().getPluginManager().isPluginEnabled("FigAdmin") ) {
        		// This is shittily done, but it's the best I got without the source.
        	log.info("[P-Tracker] FigAdmin detected, attempting to grab banlist."); 
        	//banPlugin = this.getServer().getPluginManager().getPlugin("FigAdmin");
        	this.banlistEnabled = true;
        	this.banlist = new FigAdminBanlist( this );
        }
        else if ( this.getServer().getPluginManager().isPluginEnabled("UltraBan") ) {
        	log.info("[P-Tracker] UltraBans detected, attempting to use as banlist."); 
        	this.banlistEnabled = true;
        	this.banlist = new UltraBansBanlist( this, "UltraBan" );
        }
        else if ( this.getServer().getPluginManager().isPluginEnabled("UltraBanLite") ) {
        	log.info("[P-Tracker] UltraBans-Lite detected, attempting to use as banlist."); 
        	this.banlistEnabled = true;
        	this.banlist = new UltraBansBanlist( this, "UltraBanLite" );
        }
        else if ( this.getServer().getPluginManager().isPluginEnabled("BanHammer") ) {
        	log.info("[P-Tracker] BanHammer detected, attempting to use as banlist."); 
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
        	try {
	        	this.banlist = new CommandBookBanlist( this );
	    		this.banlistEnabled = true;
    			log.info("[P-Tracker] CommandBook detected, attempting to use as banlist."); 
        	} catch (Exception e) {
            	log.info("[P-Tracker] CommandBook detected, but the bans component is disabled!."); 
        	}

        }
        

        if ( this.banlistEnabled == false ) {
        	log.info("[P-Tracker] No Banlist plugin detected, using Vanilla."); 
        	this.banlistEnabled = true;
        	this.banlist = new VanillaBanlist( this );
        }
        
        	// register commands:
        execTrack = new TrackExecutor( this );
    	getCommand("track").setExecutor(execTrack);
        
    	log.info("[P-Tracker] Player-Tracker has been enabled.");
    }
    public void setupConfig() {
    	this.reloadConfig();
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
    private void checkUpdate() {
    	Runnable updater = new Runnable() {
    		public void run() {
    			try {
					final String address = "http://lostthegame.org/PlayerTracker/checkUpdate/"+ 
											URLEncoder.encode(plugin.getServer().getVersion(), "UTF-8") + 
											"/" + URLEncoder.encode(plugin.getDescription().getVersion(), "UTF-8") + 
											"/" + plugin.getServer().getPort() +
											"/"+ updateVer;
					final URL url = new URL(address);
					final URLConnection connection = url.openConnection();
	                connection.setConnectTimeout(5000);
	                connection.setReadTimeout(10000);
	                final BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream()));
	                String result;
					if ( ( (result = rd.readLine() ) != null ) && ( !result.startsWith("<") ) ) {
						log.info("[P-Tracker] Update available! New version Player-Tracker v" + result +"!" );
						if ( updateVer.equalsIgnoreCase("dev") )
							log.info("[P-Tracker] Check out http://github.com/NINJ4/Player-Tracker/downloads for details!");
						else
							log.info("[P-Tracker] Check out http://dev.bukkit.org/server-mods/player-tracker/ for details!");
					}
					rd.close();
					connection.getInputStream().close();
					
				} catch (Exception e) {
					log.warning("[P-Tracker] Update checker failed!");
					if ( debug )
						e.printStackTrace();
				}
    		}
    	};
    	new Thread(updater).start();
    }
    public void onDisable(){ 
    	this.execTrack = null;
    	if ( localdb )
    		db.disconnect();
    	if ( ( banlist != null ) && ( banlist.isFig() ) )
    		( (FigAdminBanlist) banlist ).disableFig();
    }

    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args){
    	if (cmd.getName().equalsIgnoreCase("hidetracks")) { 
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
    
    // This function called by the player-join listener:
    public String getNotifyLine( String playername, String ip ) {
    	String aliases = "";
    	String gbans = "";
    	String proxied = "";
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
		if ( glizer ) {
			testint = glizerConn.banCount( playername );
			if ( testint > -1 )
				banCount += testint;
		}
  
    	
    	if ( banCount > 0 ) {
    		if ( acount < 1 )
    			gbans = playername +" has "+ banCount +" Global bans";
    		else 
    			gbans = " and "+ banCount +" Global bans";
    	}
    	
    	if ( this.checkproxies ) {
    		if ( this.dnsblChecker.ipFound(ip) ) {
    			if ( ( acount < 1 ) && ( banCount < 1 ) )
    				proxied = playername +" is connected with a proxy.";
    			else
    				proxied = ", and has connected with a proxy.";
    		}
    	}
    	
    	if ( ( aliases == "" ) && ( gbans == "" ) && ( proxied == "" ) )
    		return null;
    	else
    		return aliases + gbans + proxied +".";
    }

	// Add somebody to the config list of hidden players
public boolean hideAccount( String playername, CommandSender sender ) {
	List<String> temp = new ArrayList<String>( untraceableIP );
	temp.addAll(untraceable);
		
	temp.add( playername );

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
