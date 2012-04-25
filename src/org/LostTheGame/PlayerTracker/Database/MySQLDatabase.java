package org.LostTheGame.PlayerTracker.Database;

import static org.bukkit.Bukkit.getLogger;

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
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class MySQLDatabase extends Database {

    private Connection conn = null;
    String table;
	private String prefix = "[mySQL]";
    private PlayerTracker plugin;

    public MySQLDatabase(PlayerTracker instance) {
        this.plugin = instance;
    }

    public Connection getSQLConnection(String mysqlDatabase) {
        FileConfiguration Config = plugin.getConfig();
        String mysqlUser = Config.getString("mysql-user", "root");
        String mysqlPassword = Config.getString("mysql-password", "root");
        try {

            return DriverManager.getConnection(mysqlDatabase + "?autoReconnect=true&user=" + mysqlUser + "&password="
                    + mysqlPassword);

        } catch (SQLException ex) {
        	PlayerTracker.log.log(Level.SEVERE, "[P-Tracker]: Unable to create mySQL connection.", ex);
        }
        return null;
    }

    public Connection getSQLConnection() {
        String mysqlDatabase = plugin.getConfig().getString("mysql-database", "jdbc:mysql://localhost:3306/minecraft");
        return getSQLConnection(mysqlDatabase);

    }

    public boolean initialize() {
        PreparedStatement ps = null;
        //ResultSet rs = null;

        table = plugin.getConfig().getString("mysql-table", "player-tracker");
        try {
            conn = getSQLConnection();
            DatabaseMetaData dbm = conn.getMetaData();
            // Table create if not it exists
            if (!dbm.getTables(null, null, table, null).next()) {
                getLogger().log(Level.INFO, "[P-Tracker] Creating table " + table + ".");
                ps = conn.prepareStatement("CREATE TABLE `"+ table +"` ("
						+ "`id` mediumint( 9 ) NOT NULL AUTO_INCREMENT ,"
						+ "`accountname` varchar( 32 ) NOT NULL default '0',"
						+ "`ip` varchar( 15 ) NOT NULL default '0',"
						+ "`time` timestamp NOT NULL default CURRENT_TIMESTAMP ,"
						+ "PRIMARY KEY ( `id` )"
						+ ") ENGINE = MYISAM DEFAULT CHARSET = latin1;");
                ps.execute();
                if (!dbm.getTables(null, null, table, null).next())
                    throw new SQLException("Table " + table + " not found; tired to create and failed");
            }
        } catch (SQLException ex) {
        	PlayerTracker.log.log(Level.SEVERE, "[P-Tracker] Couldn't execute MySQL statement: ", ex);
            return false;
        }
        return true;

    }
    public void disconnect() {
        try {
            if (conn != null)
                conn.close();
        } catch (SQLException ex) {
        	PlayerTracker.log.log(Level.SEVERE, "[P-Tracker] Failed to close MySQL connection: ", ex);
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
						"WHERE LOWER(`accountname`) LIKE CONCAT('%', ?, '%') " +
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
			PlayerTracker.log.log(Level.SEVERE, "[P-Tracker] Couldn't execute MySQL statement: ", ex);
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
							line += ChatColor.RESET +""+ ChatColor.DARK_GREEN + "" + ChatColor.BOLD + " (BANNED)";
					}
					if ( IPdisp )
						line += ChatColor.RESET +""+ ChatColor.DARK_GREEN +" ("+ IP +")";
						
					sender.sendMessage( line );
				}
			}
			return true;
			
		} catch (SQLException ex) {
			PlayerTracker.log.log(Level.SEVERE, "[P-Tracker] Couldn't execute MySQL statement: ", ex);
		}
		return false;
    }
    
    public void addTracks(String playername, String IP) {
    	PreparedStatement ps = null;
    	ResultSet rs = null;
    	
    	try {
			ps = conn.prepareStatement(
					"SELECT `id` " +
					"FROM `"+ table + "` " +
					"WHERE `ip` LIKE ? " +
					"AND LOWER(`accountname`) LIKE ?");
			ps.setString ( 1, IP );
			ps.setString ( 2, playername );
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
						"SET `time` = CURRENT_TIMESTAMP " +
						"WHERE LOWER(`accountname`) LIKE ? " +
						"AND `ip` = ?");
				ps.setString ( 1, playername );
				ps.setString ( 2, IP );
				ps.executeUpdate();
			}
		} catch (SQLException ex) {
			PlayerTracker.log.log(Level.SEVERE, "[P-Tracker] Couldn't execute MySQL statement: ", ex);
		}
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
			if ( rs != null )
				rs.close();
			if ( rs2 != null )
				rs2.close();
		} catch (SQLException ex) {
			PlayerTracker.log.log(Level.SEVERE, "[P-Tracker] Couldn't execute MySQL statement: ", ex);
		}
    	return names;
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
    				"DELETE FROM `"+ this.table +"` " +
    				"WHERE DATE_SUB(CURDATE(),INTERVAL "+ days +" DAY) > `time`");
    		ps.executeUpdate();
    	} catch (SQLException e) {
			PlayerTracker.log.severe( "[P-Tracker]"+ this.prefix +" exception in cleanUp(): " + e );
			e.printStackTrace();
		}
    }
}