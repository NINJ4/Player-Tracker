package org.LostTheGame.PlayerTracker.Database;

import static org.bukkit.Bukkit.getLogger;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.logging.Level;

import org.LostTheGame.PlayerTracker.PlayerTracker;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SQLiteDatabase extends Database {
	public String location;
	public String name;
	private File sqlFile;
	String table;
	private PlayerTracker plugin;
	private Connection conn;
	private String prefix = "[SQLite]";
	
	public SQLiteDatabase(PlayerTracker instance, String name, String location) {
		this.plugin = instance;
		this.name = name;
		this.location = location;
		File folder = new File(this.location);
		if (this.name.contains("/") ||
			this.name.contains("\\") ||
			this.name.endsWith(".db")) {
			
				PlayerTracker.log.severe( "[P-Tracker][SQLite] The database name cannot contain: /, \\, or .db" );
		}
		if (!folder.exists()) {
			folder.mkdir();
		}

		this.sqlFile = new File(folder.getAbsolutePath() + File.separator + name + ".db");
	}
	public boolean initialize() {
		try {
			Class.forName("org.sqlite.JDBC");
		
		} catch (ClassNotFoundException e) {
			PlayerTracker.log.severe("Class not found in initialize(): " + e);
			return false;
		}
		this.table = "player-tracker";
		if ( !sqlFile.exists() ) {
			try {
				sqlFile.createNewFile();
			} catch (IOException e) {
				PlayerTracker.log.severe( "[P-Tracker][SQLite] exception in initialize(), can't create SQLite databse: " + e );
				e.printStackTrace();
			}
		}

		try {
			this.conn = DriverManager.getConnection("jdbc:sqlite:" +
												sqlFile.getAbsolutePath());
			if ( conn == null )
				PlayerTracker.log.warning( "CONN IS NULL? - "+ "jdbc:sqlite:" + sqlFile.getAbsolutePath() );

			
			DatabaseMetaData dbm = conn.getMetaData();
            // Table create if not it exists
            if (!dbm.getTables(null, null, table, null).next()) {
                getLogger().log(Level.INFO, "[P-Tracker][SQLite] Creating table " + table + ".");

            	PreparedStatement ps = null;
                ps = conn.prepareStatement("CREATE TABLE \""+ table +"\" ("
						+ "\"id\" INTEGER PRIMARY KEY  AUTOINCREMENT  NOT NULL , "
						+ "\"accountname\" VARCHAR NOT NULL  DEFAULT 0, "
						+ "\"ip\" VARCHAR NOT NULL  DEFAULT 0, "
						+ "\"time\" DATETIME DEFAULT CURRENT_TIMESTAMP)");
            	//ps = conn.prepareStatement("CREATE TABLE "player-tracker" ("id" INTEGER PRIMARY KEY  AUTOINCREMENT  NOT NULL , "accountname" VARCHAR NOT NULL  DEFAULT 0, "ip" VARCHAR NOT NULL  DEFAULT 0, "time" DATETIME DEFAULT CURRENT_TIMESTAMP)");
                ps.execute();
                if (!dbm.getTables(null, null, table, null).next())
                    throw new SQLException("[P-Tracker][SQLite] Table " + table + " not found; tired to create and failed");
            }
			
			return true;
		} catch (SQLException e) {
			PlayerTracker.log.severe( "[P-Tracker][SQLite] exception in initialize(): " );
			e.printStackTrace();
		}
		return false;
	}
	public void disconnect() {
		if (conn != null) {
			try {
				conn.close();
			} catch (SQLException ex) {
				PlayerTracker.log.severe( "[P-Tracker][SQLite] exception in close(): " + ex );
			}
		}
	}
    public String PlayerTrack( String playername, CommandSender sender, boolean wildcard, boolean IPdisp ) {

    	boolean override = false;
    	if ( sender instanceof Player ) {
	    	Player player = (Player) sender;
	    	if ( player.hasPermission("playertracker.hidetracks.override") ) {
	    		override = true;
	    	}
    	}
    	else // must be console user!
    		override = true;
    	
    	PreparedStatement ps = null;
    	ResultSet rs = null;
    	ResultSet rs2 = null;
		try {
			ps	= conn.prepareStatement(
					"SELECT `ip` " +
					"FROM `"+ table + "` " +
					"WHERE LOWER(`accountname`) LIKE ? " +
					"ORDER BY `time` DESC");
			ps.setString ( 1, playername.toLowerCase() );
			rs = ps.executeQuery();

			int names = 0;
			int ips = 0;
			String thisIP = "";
			String[] acctList = new String[20];
			String[] IPlist = new String[20];
			while( rs.next() ) {
				
					// Get the other accounts linked with these IP addresses.
				thisIP = rs.getString("ip");
				if ( ( !override ) && ( plugin.untraceableIP.contains( thisIP ) ) )
					continue;
				
				ps	= conn.prepareStatement(
						"SELECT `accountname` " +
						"FROM `"+ table + "` " +
						"WHERE `ip` LIKE '"+ thisIP +"' " +
						"AND LOWER(`accountname`) != ?" +
						"ORDER BY `time` DESC");
				ps.setString ( 1, playername.toLowerCase() );
				rs2 = ps.executeQuery();
				while( rs2.next() ) {
					if ( !Arrays.asList( acctList ).contains( rs2.getString("accountname") ) ) {
						if ( ( override ) || ( !plugin.untraceable.contains( rs2.getString("accountname").toLowerCase() ) ) ) {
							if ( names < 20 ) {
								acctList[names] = rs2.getString("accountname");
								IPlist[names] = thisIP;
								names++;
							}
						}
					}
				}
				
				ips++;
			}
			if ( ( ips == 0 ) && ( wildcard ) ) {
				sender.sendMessage(ChatColor.GREEN + "" + "[P-Tracker] No exact matches, attempting wildcard search.");
				ps	= conn.prepareStatement(
						"SELECT `accountname` " +
						"FROM `"+ table + "` " +
						"WHERE LOWER(`accountname`) LIKE ('%' || ? || '%') " +
						"ORDER BY `time` DESC " +
						"LIMIT 0, 1"); // limit to one match!
				ps.setString ( 1, playername.toLowerCase() );
				rs = ps.executeQuery();

				if ( !rs.next() ) {
					sender.sendMessage(ChatColor.GREEN + "" + "[P-Tracker] No accounts match partially with \""+ playername +"\"");
					rs.close();
					return null;
				}
				else {
					playername = rs.getString("accountname");
			    	if ( ( !override ) && ( plugin.untraceable.contains( playername.toLowerCase() ) ) ) {
							sender.sendMessage(ChatColor.GREEN + "" + "[P-Tracker] Player \""+ ChatColor.UNDERLINE + playername + ChatColor.RESET + ChatColor.GREEN + "\" is not associated with any known accounts.");
							rs.close();
							return playername;
			    	}
				}
				
				ps	= conn.prepareStatement(
						"SELECT `ip` " +
						"FROM `"+ table + "` " +
						"WHERE LOWER(`accountname`) LIKE ? " +
						"ORDER BY `time` DESC"); // limit to one match!
				ps.setString ( 1, playername.toLowerCase() );
				rs = ps.executeQuery();
				
				while( rs.next() ) {
					
						// Get the other accounts linked with these IP addresses.
					thisIP = rs.getString("ip");
					if ( ( !override ) && ( plugin.untraceableIP.contains( thisIP ) ) )
						continue;
					
					ps	= conn.prepareStatement(
							"SELECT `accountname` " +
							"FROM `"+ table + "` " +
							"WHERE `ip` LIKE '"+ thisIP +"' " +
							"AND LOWER(`accountname`) != ?" +
							"ORDER BY `time` DESC");
					ps.setString ( 1, playername.toLowerCase() );
					rs2 = ps.executeQuery();
					while( rs2.next() ) {
						if ( !Arrays.asList( acctList ).contains( rs2.getString("accountname") ) ) {
							if ( ( override ) || ( !plugin.untraceable.contains( rs2.getString("accountname").toLowerCase() ) ) ) {
								if ( names < 20 ) {
									acctList[names] = rs2.getString("accountname");
									IPlist[names] = thisIP;
									names++;
								}
							}
						}
					}
					
					ips++;
				}
				rs.close();
				rs2.close();
			}
			if ( names == 0 ) {
				sender.sendMessage(ChatColor.GREEN + "" + "[P-Tracker] Player \""+ ChatColor.UNDERLINE + playername + ChatColor.RESET + ChatColor.GREEN + "\" is not associated with any known accounts.");
				return playername;
			}
			sender.sendMessage(ChatColor.GREEN + "" + "[P-Tracker] Player \""+ ChatColor.UNDERLINE + playername + ChatColor.RESET + ChatColor.GREEN + "\" is associated with the following "+ names +" account(s):");
			String s = null;
			int i = 0;
			for(String name:acctList) {
				if ( name != null ) {
					s = ChatColor.DARK_GREEN + "   - " + ChatColor.UNDERLINE + name;
					if ( plugin.banlistEnabled ) {
						if ( plugin.banlist.isBanned( name ) )
							s += ChatColor.RESET +""+ ChatColor.DARK_GREEN + "" + ChatColor.BOLD + " (BANNED)";
					}
					if ( IPdisp )
						s += ChatColor.RESET +""+ ChatColor.DARK_GREEN +" ("+ IPlist[i] +")";
					
					sender.sendMessage( s );
				}
				i++;
			}
			return playername;
			
		} catch (SQLException ex) {
			PlayerTracker.log.log(Level.SEVERE, "[P-Tracker] Couldn't execute SQL statement: ", ex);
		}
		return null;
    }
    public boolean IPTrack( String IP, CommandSender sender, boolean IPdisp ) {

    	boolean override = false;
    	if ( sender instanceof Player ) {
	    	Player player = (Player) sender;
	    	if ( player.hasPermission("playertracker.hidetracks.override") ) {
	    		override = true;
	    	}
    	}
    	else // must be console user!
    		override = true;
    	
    	PreparedStatement ps = null;
    	ResultSet rs = null;
		try {
			int names = 0;
			String[] acctList = new String[20];
			
			ps	= conn.prepareStatement(
					"SELECT `accountname`,`ip` " +
					"FROM `"+ table + "` " +
					"WHERE `ip` LIKE '"+ IP +"' " +
					"AND LOWER(`accountname`) != ?" +
					"ORDER BY `time` DESC");
			ps.setString ( 1, IP );
			rs = ps.executeQuery();
			while( rs.next() ) {
				if ( !Arrays.asList( acctList ).contains( rs.getString("accountname") ) ) {
					if ( ( override ) || ( !plugin.untraceable.contains( rs.getString("accountname").toLowerCase() ) ) ) {
						acctList[names] = rs.getString("accountname");
						names++;
					}
				}
			}
			rs.close();
			if ( names == 0 ) {
				sender.sendMessage(ChatColor.GREEN + "" + "[P-Tracker] IP Address \""+ ChatColor.UNDERLINE + IP + ChatColor.RESET + ChatColor.GREEN + "\" is not associated with any known accounts.");
				return true;
			}
			
			sender.sendMessage(ChatColor.GREEN + "" + "[P-Tracker] IP Address \""+ ChatColor.UNDERLINE + IP + ChatColor.RESET + ChatColor.GREEN + "\" is associated with the following "+ names +" account(s):");
			String line;
			for(String name:acctList) {
				if ( name != null ) {
					line = ChatColor.DARK_GREEN + "   - " + ChatColor.UNDERLINE + name;
					if ( plugin.banlistEnabled ) {
						if ( plugin.banlist.isBanned( name ) )
							line += ChatColor.RESET +""+ ChatColor.DARK_GREEN +""+ ChatColor.BOLD + " (BANNED)";
					}
					if ( IPdisp )
						line += ChatColor.RESET +""+ ChatColor.DARK_GREEN +" ("+ IP +")";
						
					sender.sendMessage( line );
				}
			}
			return true;
			
		} catch (SQLException ex) {
			PlayerTracker.log.log(Level.SEVERE, "[P-Tracker] Couldn't execute SQL statement: ", ex);
		}
		return false;
    }
    public int AliasCount( String playername ) {
    	PreparedStatement ps = null;
    	ResultSet rs = null;
    	ResultSet rs2 = null;
		int names = 0;
		try {
			ps	= conn.prepareStatement(
					"SELECT `ip` " +
					"FROM `"+ table + "` " +
					"WHERE LOWER(`accountname`) LIKE ? " +
					"ORDER BY `time` DESC");
			ps.setString ( 1, playername.toLowerCase() );
			rs = ps.executeQuery();

			int ips = 0;
			String thisIP = "";
			String[] acctList = new String[20];
			while( rs.next() ) {
				
					// Get the other accounts linked with these IP addresses.
				thisIP = rs.getString("ip");
				if ( plugin.untraceableIP.contains( thisIP ) )
					continue;
				
				ps	= conn.prepareStatement(
						"SELECT `accountname` " +
						"FROM `"+ table + "` " +
						"WHERE `ip` LIKE '"+ thisIP +"' " +
						"AND LOWER(`accountname`) != ?" +
						"ORDER BY `time` DESC");
				ps.setString ( 1, playername.toLowerCase() );
				rs2 = ps.executeQuery();
				while( rs2.next() ) {
					if ( !Arrays.asList( acctList ).contains( rs2.getString("accountname") ) ) {
						if ( !plugin.untraceable.contains( rs2.getString("accountname").toLowerCase() ) ) {
							if ( names < 20 ) {
								acctList[names] = rs2.getString("accountname");
								names++;
							}
							else break;
						}
					}
				}
				
				ips++;
			}
			rs.close();
			rs2.close();
		} catch (SQLException ex) {
			PlayerTracker.log.log(Level.SEVERE, "[P-Tracker] Couldn't execute MySQL statement: ", ex);
		}
    	return names;
    }
	public void addTracks( String playername, String IP ) {
    	PreparedStatement ps = null;
    	ResultSet rs = null;
		try {
			ps = conn.prepareStatement(
					"SELECT `id` " +
					"FROM `"+ table + "` " +
					"WHERE `ip` LIKE ? " +
					"AND LOWER(`accountname`) LIKE ?");
			ps.setString ( 1, IP );
			ps.setString ( 2, playername.toLowerCase() );
			rs = ps.executeQuery();
			
			if ( !rs.next() ) {
				ps	= conn.prepareStatement(
						"INSERT INTO `"+ table +"` " +
						"( `accountname`, `ip` ) " +
						"VALUES( ?, ? )");
				ps.setString ( 1, playername );
				ps.setString ( 2, IP );
				ps.executeUpdate();
			}
			else {
				ps	= conn.prepareStatement(
						"UPDATE `"+ table +"` " +
						"SET `time` = DATETIME('NOW') " +
						"WHERE LOWER(`accountname`) LIKE ? " +
						"AND `ip` = ?");
				ps.setString ( 1, playername.toLowerCase() );
				ps.setString ( 2, IP );
				ps.executeUpdate();
			}
		} catch (SQLException e) {
			PlayerTracker.log.severe( "[P-Tracker][SQLite] exception in addTracks(): " + e );
			e.printStackTrace();
		}
	}
    public boolean localStats( CommandSender sender ) {
    	PreparedStatement ps = null;
    	ResultSet rs = null;
		try {
			ps = this.conn.prepareStatement(
					"SELECT COUNT(DISTINCT `accountname`) AS `accounts`, " +
					"COUNT(DISTINCT `ip`) AS `ips` " +
					"FROM `"+ this.table +"`");
			rs = ps.executeQuery();
			rs.next();
			
			sender.sendMessage(ChatColor.GREEN + "[Player-Tracker] Tracking "+ rs.getInt("accounts") +" player accounts and "+ rs.getInt("ips") +" IP addresses.");
			
			rs.close();
			return true;
		} catch (SQLException e) {
			PlayerTracker.log.severe( "[P-Tracker]"+ this.prefix +" exception in localStats(): " + e );
			e.printStackTrace();
		}
    	return false;
    }
    public void cleanUp() {
    	int days = plugin.config.getInt("persistence-days", 60);
    	PreparedStatement ps = null;
    	try {
    		ps = this.conn.prepareStatement(
    				"DELETE from `"+ this.table +"` " +
    				"WHERE `time` < date('NOW', '-"+ days +" DAYS')");
    		ps.executeUpdate();
    	} catch (SQLException e) {
			PlayerTracker.log.severe( "[P-Tracker]"+ this.prefix +" exception in cleanUp(): " + e );
			e.printStackTrace();
		}
    }
}
