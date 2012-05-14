package org.LostTheGame.PlayerTracker.Database;

import static org.bukkit.Bukkit.getLogger;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.logging.Level;

import org.LostTheGame.PlayerTracker.PlayerTracker;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

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
    public String wildcardMatch( String playername, boolean override ) {
    	PreparedStatement ps = null;
    	ResultSet rs = null;
    	try {
			ps	= conn.prepareStatement(
					"SELECT `accountname` " +
					"FROM `"+ table + "` " +
					"WHERE LOWER(`accountname`) LIKE CONCAT('%', ?, '%') " +
					"ORDER BY `time` DESC " +
					"LIMIT 0, 1");
			ps.setString ( 1, playername.toLowerCase() );
			rs = ps.executeQuery();
			if ( !rs.next() )
				return null;
			else {
				String newname = rs.getString("accountname");
				if ( ( plugin.untraceable.contains( playername.toLowerCase() ) ) && ( !override ) )
					return null;
				
				return newname;				
			}
    	} catch (SQLException ex) {
			PlayerTracker.log.log(Level.SEVERE, "[P-Tracker] Couldn't execute MySQL statement: ", ex);
		}
    	return null;
    }

    public ArrayList<String> PlayerTrack( String playername, boolean IPdisp, boolean recursive, boolean override, boolean wildcard, boolean geolocate ) {
    	ArrayList<String> output = new ArrayList<String>();
    	
    	PreparedStatement ps = null;
    	ResultSet rs = null;
		try {
			ps	= conn.prepareStatement(
					"SELECT `ip` " +
					"FROM `"+ table + "` " +
					"WHERE LOWER(`accountname`) LIKE ? " +
					"ORDER BY `time` DESC");
			ps.setString ( 1, playername.toLowerCase() );
			rs = ps.executeQuery();
			
			LinkedHashSet<String> ips = new LinkedHashSet<String>();
			while( rs.next() ) {
				if ( ( !plugin.untraceableIP.contains( rs.getString("ip") ) ) || ( override ) ) {
					ips.add( rs.getString("ip") );
					
					if ( geolocate ) {
						output.add( rs.getString("ip") );
						geolocate = false;
					}
				}
				
				//	output.addAll( IPRTrack( rs.getString("ip"), wildcard, IPdisp, recursive, override ) );
			}
			
			if ( ips.size() == 0 )
				return null;

			java.util.Iterator<String> ips_itr = ips.iterator();
			
			LinkedHashSet<String> names = new LinkedHashSet<String>();
			names.add( playername );

			while ( ips_itr.hasNext() ) {
				names.addAll( IPTrack( ips_itr.next(), IPdisp, recursive, override ) );
			}
			if ( recursive ) { // OH GOD OH GOD OH GOD
				LinkedHashSet<String> names_spent = new LinkedHashSet<String>();
				names_spent.add( playername );
				java.util.Iterator<String> names_itr = names.iterator();
				while ( names_itr.hasNext() ) {
					
					String thisName = names_itr.next();
					if ( names_spent.contains( thisName ) )
						continue;
					
					names_spent.add( thisName );
					ArrayList<String> trackThis = PlayerTrack( thisName, IPdisp, false, override, false, false );
					if ( trackThis == null ) continue;
					
					if ( names.addAll( trackThis ) )
						names_itr = names.iterator();
				}
			}
			
			
			LinkedHashSet<String> names_check = new LinkedHashSet<String>(names);
			java.util.Iterator<String> output_itr = names_check.iterator();

			while ( output_itr.hasNext() ) {
				String thisName = output_itr.next();
				if ( thisName.equalsIgnoreCase( playername ) )
					names.remove( thisName );
				
			}

			output.addAll( names );
			
		} catch (SQLException ex) {
			PlayerTracker.log.log(Level.SEVERE, "[P-Tracker] Couldn't execute MySQL statement: ", ex);
		}
    		
    	return output;
    }

    public ArrayList<String> IPTrack( String ipaddr, boolean IPdisp, boolean recursive, boolean override ) {
    	ArrayList<String> output = new ArrayList<String>();
    	
    	PreparedStatement ps = null;
    	ResultSet rs = null;
		try {
			ps	= conn.prepareStatement(
					"SELECT `accountname` " +
					"FROM `"+ table + "` " +
					"WHERE LOWER(`ip`) LIKE ? " +
					"ORDER BY `time` DESC");
			ps.setString ( 1, ipaddr );
			rs = ps.executeQuery();

			LinkedHashSet<String> names = new LinkedHashSet<String>();
			while( rs.next() ) {
				if ( ( !plugin.untraceable.contains( rs.getString("accountname") ) ) || ( override ) )
					names.add( rs.getString("accountname")
							+( ( IPdisp ) ? " ("+ ipaddr +")" : "" )
					);
				}
			if ( recursive ) { // OH GOD OH GOD OH GOD
				LinkedHashSet<String> names_spent = new LinkedHashSet<String>();
				java.util.Iterator<String> names_itr = names.iterator();
				while ( names_itr.hasNext() ) {
					
					String thisName = names_itr.next();
					if ( names_spent.contains( thisName ) )
						continue;
					
					names_spent.add( thisName );
					if ( names.addAll( PlayerTrack( ( ( thisName.indexOf(" ") != -1 ) ? thisName.substring( 0, thisName.indexOf(" ") ) : thisName ), IPdisp, false, override, false, false ) ) )
						names_itr = names.iterator();
				}
			}
			output.addAll( names );
		} catch (SQLException ex) {
			PlayerTracker.log.log(Level.SEVERE, "[P-Tracker] Couldn't execute MySQL statement: ", ex);
		}
		
		return output;
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
    	ArrayList<String> getNames = PlayerTrack( playername, false, false, false, false, false );
    	if ( getNames == null )
    		return 0;
    	
    	return getNames.size();
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