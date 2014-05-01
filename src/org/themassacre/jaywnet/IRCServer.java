/*/ Part of JayWormNET source code. (C) 2013 Andrey Bobkov (MEDVEDx64).
    Licensed under the Apache License, Version 2.0.  /*/

package org.themassacre.jaywnet;

import org.themassacre.util.*;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.io.*;

class CommandInvocationException extends Exception {
	private static final long serialVersionUID = -2022485016669801767L;
	public CommandInvocationException(String msg) {
		super(msg);
	}
}

class Channel {
	public String name = null;
	public String scheme = "Pf,Be";
	public String topic = "";

	public Channel(String name) {
		this.name = name;
	}

	public Channel(String name, String scheme, String topic) {
		this.name = name;
		this.scheme = scheme;
		this.topic = topic;
	}

	// Returns index of a channel in Channels array by it's name
	public static int indexOf(Channel[] channels, String chName) {
		for(int i = 0; i < channels.length; i++) {
			if(channels[i].name.equals(chName)) return i;
		}

		return -1;
	}
}

public class IRCServer extends Thread {
	final static String validNickChars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789`_-|";
	final static Date created = new Date();

	// IRC user list
	public static ArrayList<IRCUser> users = new ArrayList<IRCUser>();

	// Lists are read from csv, where first column is nickname, second — ip address.
	public final static ArrayList<String> banlist = new ArrayList<String>();
	public final static ArrayList<String> banlistIPs = new ArrayList<String>();
	public final static ArrayList<String> whitelist = new ArrayList<String>();
	public final static ArrayList<String> whitelistIPs = new ArrayList<String>();
	public final static ArrayList<String> allowedAdditionalCommands = new ArrayList<String>();

	void readSimpleList(ArrayList<String> list, String fileName) {
		list.clear();
		try(BufferedReader in = new BufferedReader(new InputStreamReader(StreamUtils
				.getResourceAsStream(fileName, this)))) {
			while(true) {
				String line = in.readLine();
				if(line == null) break;
				line = (line + "#").substring(0, (line + "#").indexOf('#'));
				if(line == null) continue;
				if(line.trim().length() == 0) continue;
				list.add(line.trim());
			}
		} catch(FileNotFoundException eNF) {
			WNLogger.l.warning("List file not found: " + fileName);
		} catch(Exception e) {
			e.printStackTrace();
			WNLogger.l.warning("Can't read list file (" + fileName + "): " + e);
		}
	}
	
	void readCommandsList() {
		readSimpleList(allowedAdditionalCommands, JayWormNet.config.commandsListFileName);
	}
	
	void readList(ArrayList<String> names, ArrayList<String> ips, String fileName) {
		names.clear();
		ips.clear();
		try(BufferedReader in = new BufferedReader(new InputStreamReader(StreamUtils.getResourceAsStream(fileName, this)))) {
			while(true) {
				String buffer = in.readLine();
				if(buffer == null) break;

				String[] row = buffer.split(",");
				if(row == null) continue;
				if(row.length < 2) continue;
				names.add(row[0].trim());
				ips.add(row[1].trim());
			}

		} catch(FileNotFoundException eNF) {
			WNLogger.l.warning("List file not found: " + fileName);
		} catch(Exception e) {
			e.printStackTrace();
			WNLogger.l.warning("Can't read list file (" + fileName + "): " + e);
		}
	}

	static boolean isIPInList(String addr, ArrayList<String> list, ArrayList<String> listIPs) {
		for(int i = 0; i < list.size(); i++)
			if(list.get(i).contains("*") && listIPs.get(i).contains(addr))
				return true;
		return false;
	}

	public static boolean isIPInWhiteList(String addr) {
		return isIPInList(addr, whitelist, whitelistIPs);
	}

	public static boolean isIPInBanList(String addr) {
		return isIPInList(addr, banlist, banlistIPs);
	}

	// Channels
	public static Channel[] channels;

	public static boolean isChannelExist(String chName) {
		return (Channel.indexOf(IRCServer.channels, chName) == -1)? false: true;
	}

	// MOTD
	public static String[] motdLines = null;
	public void readMOTD() {
		try(BufferedReader in = new BufferedReader(new InputStreamReader(
				StreamUtils.getResourceAsStream(JayWormNet.config.ircMOTDFileName, this)))) {
			String buffer = "";
			while(true) {
				String line = in.readLine();
				if(line == null) break;
				buffer = buffer + line + "\n";
			}
			motdLines = buffer.split("\n");
		} catch(Exception e) {
			WNLogger.l.warning("Can't read IRC MOTD file: " + e);
			motdLines = new String[1];
			motdLines[0] = "[MOTD file has failed to read]";
		}
	}

	@Override public void run() {
		if(JayWormNet.config.ircShowMOTD) readMOTD();
		// Reading ban-/white-list
		reloadLists();
		try(ServerSocket ss = new ServerSocket(JayWormNet.config.IRCPort)) {
			while(true) {
				try {
					Socket socket = ss.accept();
					WNLogger.l.info("connection established " + socket.getInetAddress()
							.toString().substring(1));

					// Registering a user
					IRCUser u = new IRCUser();
					u.socket = socket;
					u.connectingFrom = (socket.getInetAddress().toString().contains("/")?
							socket.getInetAddress().toString().substring(1):
							socket.getInetAddress().toString());
					u.modes['u'] = true;

					u.in = socket.getInputStream();
					u.out = socket.getOutputStream();

					users.add(u);
					u.start();

				} catch(Exception e) {
					WNLogger.l.severe("Failed to register a user: " + e);
					e.printStackTrace();
					try {
						Thread.sleep(JayWormNet.config.IRCFailureSleepTime);
					} catch(InterruptedException eInt) {
						eInt.printStackTrace();
					}
				}
			}
		} catch(Exception e) {
			e.printStackTrace();
			WNLogger.l.severe("Can't create IRC server: " + e);
			WNLogger.l.severe("Exiting now with error status.");
			System.exit(-1);
		}
	}

	// Send line to all users
	public static void broadcast(String s) {
		for(int i = 0; i < users.size(); i++) {
			if(users.get(i).inAnyChannel())
				users.get(i).sendln(s);
		}
	}

	// Send line to anybody but self
	public static void broadcast(String s, IRCUser sender) {
		for(int i = 0; i < users.size(); i++) {
			IRCUser u = users.get(i);
			if(u.inAnyChannel() && !u.nickname.equals(sender.nickname))
				users.get(i).sendln(s);
		}
	}

	// Send to channel
	public static void broadcast(String s, String chName) {
		// Channel existence check
		if(Channel.indexOf(channels, chName) == -1) return;
		for(int i = 0; i < users.size(); i++) {
			if(users.get(i).inChannel[Channel.indexOf(channels, chName)])
				users.get(i).sendln(s);
		}
	}

	public static void broadcast(String s, String chName, IRCUser sender) {
		// Channel existence check
		if(Channel.indexOf(channels, chName) == -1) return;
		for(int i = 0; i < users.size(); i++) {
			IRCUser u = users.get(i);
			if(u.inChannel[Channel.indexOf(channels, chName)] && !u.nickname.equals(sender.nickname))
				users.get(i).sendln(s);
		}
	}

	public static void broadcastEvent(int event, String s) {
		for(int i = 0; i < users.size(); i++)
			users.get(i).sendEvent(event,  s);
	}

	// only for opers
	public static void broadcastOperEvent(int event, String s, String channel) {
		for(int i = 0; i < users.size(); i++) {
			IRCUser u = users.get(i);
			try {
				if(u.modes['o'] && u.inChannel[Channel.indexOf(channels, channel)])
					u.sendEvent(event,  s);
			} catch(ArrayIndexOutOfBoundsException e) {
			}
		}
	}

	public static void broadcastSpecialMessage(String s) {
		broadcastEvent(300, ":" + s);
	}

	public static void broadcastOperSpecialMessage(String s, String channel) {
		broadcastOperEvent(300, ":* " + s, channel);
	}

	public static IRCUser getUserByNickName(String nickname) {
		for(int i = 0; i < users.size(); i++) {
			IRCUser found = users.get(i);
			if(found.nickname.equalsIgnoreCase(nickname))
				return found;
		}
		return null;
	}

	public IRCServer() {
		reloadChannels();
		this.start();
		WNLogger.l.info("Starting IRC server, listening on port " + JayWormNet.config.IRCPort);
	}

	public Channel[] readChannelsFromFile(String fileName) {
		ArrayList<Channel> channels = new ArrayList<Channel>();
		try(BufferedReader in = new BufferedReader(new InputStreamReader(
				StreamUtils.getResourceAsStream(JayWormNet.config.channelsFileName, this)))) {

			while(true) {
				String line = in.readLine();
				if(line == null) break;
				if(line.length() == 0) break;

				// Manual parsing
				StringBuffer chName = new StringBuffer();
				StringBuffer chScheme = new StringBuffer();
				int location = 0; int index = 0;
				for(index = 0; index < line.length(); index++) {
					if(location > 1) break;

					if(line.charAt(index) == ':') {
						location++;
						continue;
					}

					(location == 0? chName: chScheme).ensureCapacity(index+2);
					(location == 0? chName: chScheme).append(line.charAt(index));
				}

				// Avoiding duplicates
				if(channels.contains(chName)) continue;
				// Adding
				channels.add(new Channel(chName.toString(), chScheme.toString().length() == 0? "Pf,Be":
						chScheme.toString(), (location > 1? line.substring(index): "")));
			}

		} catch(Exception e) {
			//System.err.println(e);
			e.printStackTrace();
			WNLogger.l.warning("Can't read channels file (" + fileName + "). Default channel set will be used.");
			channels.add(new Channel("AnythingGoes", "Pf,Be", ""));
		}

		return channels.toArray(new Channel[channels.size()]);
	}

	public void reloadChannels() {
		channels = readChannelsFromFile(JayWormNet.config.channelsFileName);
	}

	public void reloadLists() {
		readCommandsList();
		if(JayWormNet.config.enableBanList) readList(banlist, banlistIPs, JayWormNet.config.banListFileName);
		if(JayWormNet.config.enableWhiteList) readList(whitelist, whitelistIPs, JayWormNet.config.whiteListFileName);
	}
}

class UserWatcher extends Thread {
	private int timer = 0;
	private IRCUser user = null;
	private int interval, timeout;

	public UserWatcher(IRCUser u) {
		user		= u;
		interval	= JayWormNet.config.pingInterval;
		timeout		= JayWormNet.config.pingTimeout;
	}

	@Override public void run() {
		while(true) {
			try {
				Thread.sleep(1000);
			}
			catch(InterruptedException e) {
				e.printStackTrace();
			}

			timer++;

			if(user.isPingPassed) {
				timer = 0;
				user.isPingPassed = false;
				user.quitMessage = "";
			}

			if(timer == interval)
				user.sendln("PING :beep!");
			if(timer == interval + timeout) {
				user.quitMessage = "Ping timeout: " + timeout;

				if(user.socket == null)
					break;
				else {
					try {
						user.socket.close();
					} catch(IOException e) {
						e.printStackTrace();
					}
				}

				user.socket = null;
			}
		}
	}
}
