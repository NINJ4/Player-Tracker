package org.LostTheGame.PlayerTracker;

import java.util.ArrayList;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public abstract class TrackerRunnables implements Runnable {
	protected String playerORip;
	protected CommandSender sender;
	protected boolean wildcard;
	protected boolean IPdisp;
	protected boolean override;
	protected boolean recursive;
	protected String ip;
	protected String playername;
	protected ArrayList<Player> notifyUs;
	
		// used for playerTrack
	public TrackerRunnables( String playerORip, CommandSender sender, boolean IPdisp, boolean recursive, boolean override, boolean wildcard ) {
		this.playerORip = playerORip;
		this.IPdisp = IPdisp;
		this.recursive = recursive;
		this.override = override;
		this.wildcard = wildcard;
		this.sender = sender;
	}
	
		// used for addTracks:
	public TrackerRunnables( String playername, String ip ) {
		this.playername = playername;
		this.ip = ip;
	}
	
		// used for notifyLines
	public TrackerRunnables( String playername, String ip, ArrayList<Player> notifyUs ) {
		this.playername = playername;
		this.notifyUs = notifyUs;
		this.ip = ip;
	}
		
		// used for IPTrack
	public TrackerRunnables( String ip, CommandSender sender, boolean IPdisp) {
		this.ip = ip;
		this.sender = sender;
		this.IPdisp = IPdisp;
		
	}
}
