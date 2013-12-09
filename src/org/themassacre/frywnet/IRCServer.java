/*/ Part of FryWormNET source code. (C) 2013 Andrey Bobkov (MEDVEDx64).
    Licensed under the Apache License, Version 2.0.  /*/

package org.themassacre.frywnet;

import org.themassacre.util.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.io.*;

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

class User extends Thread {
	public String 
		connectingFrom = "",
		nickname  = "",
		username = "",
		hostname = "",
		servername = "",
		realname = "";
	public Socket socket;
	public boolean[] inChannel;
	public boolean[] modes = new boolean[256];
	
	// Client socket I/O streams
	InputStream in = null;
	OutputStream out = null;
	
	public String	quitMessage = ""; // needed by PingWatcher
	public boolean	isPingPassed = false; // please, avoid fake PONG messages!
	
	void login() {
		ConfigurationManager c = IRCServer.config; // For shorter access
		
		WNLogger.l.info(nickname + " (" + socket.getInetAddress().toString().substring(1) + ") has logged in.");
		if(c.showIntro > 0) sendEvent(1, ":Welcome, " + nickname + "!");
		if(c.showIntro > 1) {
			sendEvent(11, ":FryWormNET " + FryWormNet.version + " IRC emulator");
			sendEvent(11, ":Based on CyberShadow's MyWormNET code");
			sendEvent(11, ":with some of StepS' improvements;");
			sendEvent(11, ":Rewrite in Java by MEDVEDx64.");
					
			sendEvent(2, ":Your host is " + IRCServer.config.serverHost + ", running FryWormNET " + FryWormNet.version);
		}
		
		if(c.showCreated)				sendEvent(3, ":This server was created " + IRCServer.created);
		if(c.showPlayersCount)			sendEvent(251, ":There are " + IRCServer.users.size() + " users on the server.");
		if(c.wallchopString != null && c.showChops)
			sendEvent(5, c.wallchopString);
		
		if(c.showOps) {
			int ops = 0;
			for(int i = 0; i < IRCServer.users.size(); i++)
				if(IRCServer.users.get(i).modes['o'])
						ops++;
			sendEvent(252, ops + " :IRC Operators online");
		}
		
		if(c.showChannelsCount)
			sendEvent(254, IRCServer.channels.length + " :pre-defined channels");
		
		if(c.ircShowMOTD && IRCServer.motdLines != null) {
			sendEvent(375, ":- " + c.serverHost + " Message of the Day - ");
			for(int i = 0; i < IRCServer.motdLines.length; i++)
				sendEvent(372, ":- " + IRCServer.motdLines[i]);
			sendEvent(376, ":End of /MOTD command.");
		}
	}
	
	@Override public void run() {
		// For shorter access
		final String serverHost = IRCServer.config.serverHost;
		final Channel[] channels = IRCServer.channels;
		String addr = socket.getInetAddress().toString().substring(1);
		
		try {
			
			do {
				// Read from client
				byte[] bytes = new byte[100000]; // it's potentially .. sux
				in.read(bytes);
				
				if(bytes.length == 0) {
					throw new Exception(addr + ": connection error.");
				}
				
				String[] lines = (new String(bytes, IRCServer.config.charset)).trim().split("\n+");
				WNLogger.l.finest("Input message: " + (new String(bytes, IRCServer.config.charset)).trim());
				
				for(int i = 0; i < lines.length; i++) {
					String buffer = lines[i].trim();
					
					if(buffer.length() == 0) continue;
					
					// Parsing
					String command = buffer.toUpperCase().substring(0, (buffer + " ").indexOf(" "));
					String body = buffer.contains(" ")? buffer.substring(command.length() + 1): "";
					
					if(command.equals("PING"))
						sendln("PONG " + (body.indexOf(':') < 0? ":" + serverHost: body.substring(body.indexOf(':'))));
					else if(command.equals("PONG")) {
						isPingPassed = true;
					} else if(command.equals("PASS")) { // ignore
					} else if(command.equals("NICK")) {
						
						if(nickname.length() != 0)
							sendln(":" + serverHost + " 400 :Nick change is not supported.");
						else {
							// Filtering nickname
							String nickFiltered = "";
							for(int z = 0; z < body.length(); z++) {
								if(IRCServer.validNickChars.indexOf(body.charAt(z)) >= 0)
									nickFiltered += body.charAt(z);
							}
							
							if(nickFiltered.length() == 0)
								sendln(":" + serverHost + " 433 " + body + " :Bad nickname");
							else {
								
								if(getUserByNickName(IRCServer.users, nickFiltered) != null)
									sendln(":" + serverHost + " 433 " + nickFiltered + " :Nickname is already in use");
								else
									nickname = nickFiltered;
								if(username.length() > 0)
									login();
							}
						}
						
					}
					
					else if(command.equals("USER")) {
						// USER username hostname servername :40 0 RO
						String[] splitted = body.split(":");
						String[] prefix = splitted[0].trim().split(" +");
						
						username 	= prefix[0];
						hostname 	= prefix[1];
						servername 	= prefix[2];
						realname	= splitted[1];
						
						if(username.length() > 0)
							login();
					}
					
					else if(command.equals("QUIT")) {
						for(int z = 0; z < IRCServer.channels.length; z++) {
							if(inChannel[z]) {
								IRCServer.broadcast(formatUserID() + " QUIT :" + (body.length() == 0? "":
									body.substring(1)), IRCServer.channels[z].name);
								inChannel[z] = false;
							}
						}
						socket.close();
						socket = null;
					}
					
					else if(command.equals("JOIN")) {
						String chName = body.charAt(0) == '#'? body.substring(1): body;
						
						if(nickname.length() == 0)
							sendln(":" + serverHost + " 451 :Register first.");
						else if(Channel.indexOf(channels, chName) == -1)
							sendEvent(403, body	+ " :No such channel.");
						else if(inChannel[Channel.indexOf(channels, chName)]) {
							sendln(":" + serverHost + " 403 " + nickname + " " + body
									+ " :You already are in channel " + body);
						} else {
							
							WNLogger.l.info(nickname + " has joined #" + chName);
							inChannel[Channel.indexOf(channels, chName)] = true;
							IRCServer.broadcast(formatUserID() + " JOIN :" + body, chName);
							if(modes['o'])
								IRCServer.broadcast(":" + serverHost + " MODE "
										+ body + " +o " + nickname, chName);
							
							String response = ":" + serverHost + " 353 " + nickname + " = "
									+ body + " :";
							for(int z = 0; z < IRCServer.users.size(); z++) {
								User u = IRCServer.users.get(z);
								if(u.inChannel[Channel.indexOf(channels, chName)]) {
									if(IRCServer.users.get(z).modes['o'])
										response += "@";
									response = response + u.nickname + " ";
								}
							}
							sendln(response);
							sendEvent(336, body + " :End of /NAMES list.");
							
						}
					}
					
					else if(command.equals("PART")) {
						String chName = body.charAt(0) == '#'? body.substring(1): body;
						if(Channel.indexOf(channels, chName) != -1)
							if(inChannel[Channel.indexOf(channels, chName)]) {
								WNLogger.l.info(nickname + " has left #" + chName);
								IRCServer.broadcast(formatUserID() + " PART "+ body, chName);
								inChannel[Channel.indexOf(channels, chName)] = false;
							}
					}
					
					else if(command.equals("MODE")) {
						String target = body.substring(0, (body + " ").indexOf(' '));
						// Does 'body' have anything after the last colon (or even that colon) — trailer?
						if(body.lastIndexOf(':') < 0 || body.lastIndexOf(':') == body.length()-1)
							sendEvent(472, ":Sorry, you can't set modes for anything.");
						else {
							// String part after the colon
							String trailer = body.substring(body.lastIndexOf(':')+1);
							if(Channel.indexOf(channels, target) == -1) { // Maybe it's a nickname, not a channel?
								User u = null;
								for(int z = 0; z < IRCServer.users.size(); z++)
									if(IRCServer.users.get(z).nickname.equals(target))
										u = IRCServer.users.get(z);
								
								if(u == null)
									sendEvent(401, target + " :No such nick/channel.");
								else {
									for(int c = 0; c < 256; c++)
										if(modes[c])
											trailer += (char)c;
									sendEvent(324, target + " +" + trailer);
											
								}
							} else
								sendEvent(324, target + " +tn");
						}
					}
					
					else if(command.equals("PRIVMSG") || command.equals("NOTICE")) {
						if(nickname.length() == 0)
							sendln(":" + serverHost + " 451 :Register first.");
						else {
							String target = body.substring(0, (body + " ").indexOf(' '));
							String trailer = (body.indexOf(':') < 0 || body.indexOf(':') == body.length()-1)?
									"": body.substring(body.indexOf(':'));
							
							// Is a channel?
							if(target.charAt(0) == '#') {
								if(IRCServer.isChannelExist(target.substring(1))) {
									IRCServer.broadcast(formatUserID() + " " + command + " "
											+ target + " " + trailer, target.substring(1), this);
									WNLogger.l.finer(nickname + " <" + target + ">: " + trailer.substring(1));
								}
								else
									sendEvent(401, target + " :No such nick/channel.");
							}
							else {
								User u = getUserByNickName(IRCServer.users, target);
								// If user doesn't exist
								if(u == null)
									sendEvent(401, target + " :No such nick/channel.");
								else {
									WNLogger.l.fine(nickname + " <" + target + ">: " + trailer.substring(1));
									u.sendln(formatUserID() + " " + command + " " + target + " " + trailer);
								}
							}
							
						}
					}
					
					else if(command.equals("OPER")) {
						String[] splitted = body.split(" +");
						String operPw = splitted.length > 1? splitted[1]: splitted[0];
						
						if(operPw.equals(IRCServer.config.IRCOperPassword)) {
							WNLogger.l.info(nickname + " (" + addr + ") has registered as operator");
							modes['o'] = true;
							sendln(":" + nickname + " MODE " + nickname + " :+o");
							for(int z = 0; z < channels.length; z++)
								if(inChannel[z])
									IRCServer.broadcast(":" + serverHost + " MODE " + channels[z].name + " +o "
											+ nickname, channels[z].name);
						}
					}
					
					else if(command.equals("WHO")) {
						for(int z = 0; z < IRCServer.users.size(); z++) {
							User u = IRCServer.users.get(z);
							for(int x = 0; x < channels.length; x++) {
								if(u.inChannel[x])
									sendln(":" + serverHost + " 352 " + nickname + " #" + channels[x].name
											+ " " + u.username + " "	+ u.connectingFrom + " " + serverHost
											+ " " + u.nickname + " H :0 " + u.realname);
								else
									sendln(":" + serverHost + " 352 " + nickname + " * " + u.username + " "
											+ u.connectingFrom + " " + serverHost + " "
											+ u.nickname + " H :0 " + u.realname);
							}
						}
						sendEvent(315, "* :End of /WHO list.");
					}
					
					else if(command.equals("LIST")) { // List of channels with users count values
						sendEvent(321, "Channel :Users  Name");
						for(int z = 0; z < channels.length; z++) {
							int usersInChannel = 0;
							for(int c = 0; c < IRCServer.users.size(); c++)
								if(IRCServer.users.get(c).inChannel[z])
									usersInChannel++;
							sendEvent(322, "#" + channels[z].name + " " + usersInChannel + " :" + channels[z].topic);
						}
						sendEvent(323, ":End of /LIST");
						
					}
					
					else sendEvent(421, command + " :Unknown command");
				}
										
			} while(socket != null);
			
		} catch(Exception e) {
			e.printStackTrace();
			IRCServer.broadcast(formatUserID() + " QUIT :" + (quitMessage.length() == 0? e: quitMessage));
			WNLogger.l.warning((quitMessage.length() == 0? e.toString(): quitMessage));
			
		} finally {
			if(socket != null) {
				try {
					socket.close();
					socket = null;
				} catch(Exception e) {
					e.printStackTrace();
				}
			}
			
			WNLogger.l.info((nickname.length() == 0? "<noname>": nickname) + " (" + addr + ") has disconnected.");
			
			// Removing myself
			if(IRCServer.users.contains(this))
				IRCServer.users.remove(this);
		}
	}
	
	public String formatUserID() {
		return ":" + nickname + "!" + username + "@" + connectingFrom;
	}
	
	public void sendln(String s) {
		if(socket != null) {
			try {
				out.write((s+"\r\n").getBytes(IRCServer.config.charset));
				WNLogger.l.finest("Output message: " + s+"\r\n");
			} catch(Exception e) {
				WNLogger.l.warning("Failed to send a response: " + e);
			}
		}
	}
	
	public void sendEvent(int event, String s) {
		String eventCode = String.valueOf(event);
		while(eventCode.length() < 3)
			eventCode = "0" + eventCode;
		sendln(":" + IRCServer.config.serverHost + " " + eventCode + " " + nickname + " " + s);
	}
	
	public boolean inAnyChannel() {
		for(int i = 0; i < IRCServer.channels.length; i++)
			if(inChannel[i]) return true;
		return false;
	}
	
	public static User getUserByNickName(ArrayList<User> users, String nickname) {
		for(int i = 0; i < users.size(); i++) {
			User found = users.get(i);
			if(found.nickname.equalsIgnoreCase(nickname))
				return found;
		}
		return null;
	}
	
	public User() {
		inChannel = new boolean[IRCServer.channels.length];
		if(IRCServer.config.pingsEnabled)
			(new UserWatcher(this, IRCServer.config)).start(); // now you own your Personal Watcher
	}
}

public class IRCServer extends Thread {
	final static String validNickChars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789`_-|";
	final static Date created = new Date();
	
	// IRC user list
	public static ArrayList<User> users = new ArrayList<User>();
	public static ConfigurationManager config;
	
	// Channels
	public static Channel[] channels;
	
	public static boolean isChannelExist(String chName) {
		return (Channel.indexOf(IRCServer.channels, chName) == -1)? false: true;
	}
	
	// MOTD
	public static String[] motdLines = null;
	public void readMOTD() {
		try(BufferedReader in = new BufferedReader(new InputStreamReader(
				StreamUtils.getResourceAsStream(config.ircMOTDFileName, this)))) {
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
		if(config.ircShowMOTD) readMOTD();
		try(ServerSocket ss = new ServerSocket(config.IRCPort)) {
			while(true) {
				try {
					Socket socket = ss.accept();
					WNLogger.l.info("connection established " + socket.getInetAddress()
							.toString().substring(1));
					
					// Registering a user
					User u = new User();
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
						Thread.sleep(5000);
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
	public static void broadcast(String s, User sender) {
		for(int i = 0; i < users.size(); i++) {
			User u = users.get(i);
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
	
	public static void broadcast(String s, String chName, User sender) {
		// Channel existence check
		if(Channel.indexOf(channels, chName) == -1) return;
		for(int i = 0; i < users.size(); i++) {
			User u = users.get(i);
			if(u.inChannel[Channel.indexOf(channels, chName)] && !u.nickname.equals(sender.nickname))
				users.get(i).sendln(s);
		}
	}
	
	public IRCServer(ConfigurationManager c) {
		config = c;
		channels = readChannelsFromFile(config.channelsFileName);
		this.start();
		WNLogger.l.info("Starting IRC server, listening on port " + config.IRCPort);
	}
	
	public Channel[] readChannelsFromFile(String fileName) {
		ArrayList<Channel> channels = new ArrayList<Channel>();
		try(BufferedReader in = new BufferedReader(new InputStreamReader(
				StreamUtils.getResourceAsStream(IRCServer.config.channelsFileName, this)))) {

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
}

class UserWatcher extends Thread {
	private int timer = 0;
	private User user = null;
	private int interval, timeout;
	
	public UserWatcher(User u, ConfigurationManager c) {
		user		= u;
		interval	= c.pingInterval;
		timeout		= c.pingTimeout;
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