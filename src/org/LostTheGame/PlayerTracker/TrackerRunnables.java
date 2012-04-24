package org.LostTheGame.PlayerTracker;

import java.util.ArrayList;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public abstract class TrackerRunnables implements Runnable {
	protected String playername;
	protected CommandSender sender;
	protected boolean wildcard;
	protected boolean IPdisp;
	protected String ip;
	protected ArrayList<Player> notifyUs;
	
		// used for playerTrack
	public TrackerRunnables( String playername, CommandSender sender, boolean wildcard, boolean IPdisp ) {
		this.playername = playername;
		this.sender = sender;
		this.wildcard = wildcard;
		this.IPdisp = IPdisp;
	}
	
		// used for addTracks:
	public TrackerRunnables( String playername, String ip ) {
		this.playername = playername;
		this.ip = ip;
	}
	
		// used for notifyLines
	public TrackerRunnables( String playername, ArrayList<Player> notifyUs ) {
		this.playername = playername;
		this.notifyUs = notifyUs;
	}
		
		// used for IPTrack
	public TrackerRunnables( String ip, CommandSender sender, boolean IPdisp) {
		this.ip = ip;
		this.sender = sender;
		this.IPdisp = IPdisp;
		
	}
}
