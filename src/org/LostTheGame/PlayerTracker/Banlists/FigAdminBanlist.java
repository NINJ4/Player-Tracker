package org.LostTheGame.PlayerTracker.Banlists;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.LostTheGame.PlayerTracker.PlayerTracker;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

//import com.btbb.figadmin.FigAdmin;


public class FigAdminBanlist extends Banlist {
	//FigAdmin fig;
	FileConfiguration ban_config;
	boolean ismySQL;
	private Connection conn;
	private String table;
	
	public FigAdminBanlist( PlayerTracker instance ) {
		this.plugin = instance;
		Plugin fig_plugin = plugin.getServer().getPluginManager().getPlugin("FigAdmin");
		//this.fig = (FigAdmin) fig_plugin;
		
			// This is a workaround until FigAdmin updates with an API (even if I have to write one)
		this.ban_config = fig_plugin.getConfig();
		
		if ( !ban_config.getBoolean( "mysql", false ) ) {
			PlayerTracker.log.warning("[P-Tracker] Figadmin not using mySQL, cannot grab banlist!");
			this.ismySQL = false;
			return;
		}
		
		this.conn = getSQLConnection();
		if ( conn == null ) {
			PlayerTracker.log.warning("[P-Tracker] Figadmin mySQL not configured correctly, cannot grab banlist!");
			this.ismySQL = false;
			return;
		}
		this.table = ban_config.getString( "mysql-table", "banlist" );
        try {
            DatabaseMetaData dbm = conn.getMetaData();
			if ( !dbm.getTables(null, null, table, null).next() ) {
				PlayerTracker.log.warning("[P-Tracker] Figadmin mySQL not configured correctly, cannot grab banlist!");
				this.ismySQL = false;
				return;
			}
		} catch (SQLException e) {
			PlayerTracker.log.warning("[P-Tracker] Figadmin mySQL not configured correctly, cannot grab banlist!");
			this.ismySQL = false;
			try {
				conn.close();
			} catch (SQLException e1) { }
			return;
		}
        this.ismySQL = true;
        return;
			
	}
	public boolean isConnected() {
		return this.ismySQL;
	}
    private Connection getSQLConnection() {
    	String mysqlDatabase = ban_config.getString("mysql-database", "root");
        String mysqlUser = ban_config.getString("mysql-user", "root");
        String mysqlPassword = ban_config.getString("mysql-password", "root");
        try {

            return DriverManager.getConnection(mysqlDatabase + "?autoReconnect=true&user=" + mysqlUser + "&password="
                    + mysqlPassword);

        } catch (SQLException ex) {
        	PlayerTracker.log.warning("[P-Tracker]: Unable to create mySQL connection.");
        }
        return null;
    }
    private void disableFig() {
    	PlayerTracker.log.warning("[P-Tracker]: mySQL error with FigAdmin, disabling Fig connection.");
    	try {
			conn.close();
		} catch (SQLException e1) { }
    	this.ismySQL = false;
    }
    public boolean isBanned( String playername ) {
    	if ( this.conn == null )
    		return false;
    	PreparedStatement ps = null;
    	ResultSet rs = null;
    	try {
			ps	= conn.prepareStatement(
					"SELECT `temptime` " +
					"FROM "+ this.table + " " +
					"WHERE LOWER(`name`) LIKE ? " +
					"AND `type` != 2");
			ps.setString ( 1, playername.toLowerCase() );
			rs = ps.executeQuery();
			if ( rs.next() ) {
				long temptime = rs.getInt("temptime");
				rs.close();
				if ( temptime == 0 )
					return true;
				if ( ( System.currentTimeMillis() / 1000 ) > temptime )
					return true;
				
			}
		} catch (SQLException e) {
			disableFig();
        	return false;
		}
    	
    	
    	return false;
    }
}
