/*/ Part of JayWormNET source code. (C) 2013 Andrey Bobkov (MEDVEDx64).
    Licensed under the Apache License, Version 2.0.  /*/

package org.themassacre.jaywnet;

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
	
	// Anti-flood fields
	// TODO: antiflood
	Date lastMessage = new Date();
	int floodLevel = 0;

	void login() throws IOException {
		ConfigurationManager c = IRCServer.config; // For shorter access
		String addr = socket.getInetAddress().toString().substring(1);

		// Checking ban-list
		if(IRCServer.config.enableBanList) {
			if(!IRCServer.banlist.contains(nickname)) {
				if(IRCServer.isIPInBanList(addr)) {
					WNLogger.l.info("Kicked " + nickname + "(" + addr + "): banned user");
					sendln("ERROR :You are banned from this server.");
					socket.close(); socket = null;
					return;
				}
			} else {
				if(IRCServer.banlistIPs.get(IRCServer.banlist.indexOf(nickname)).contains(addr) ||
						IRCServer.banlistIPs.get(IRCServer.banlist.indexOf(nickname)).contains("*")) {
					WNLogger.l.info("Kicked " + nickname + "(" + addr + "): banned user");
					sendln("ERROR :You are banned from this server.");
					socket.close(); socket = null;
					return;
				}
			}
		}

		// Checking white-list
		if(IRCServer.config.enableWhiteList) {
			if(!IRCServer.whitelist.contains(nickname)) {
				if(!IRCServer.isIPInWhiteList(addr)) {
					WNLogger.l.info("Kicked " + nickname + ": not in white-list");
					sendln("ERROR :You are not in white-list.");
					socket.close(); socket = null;
					return;
				}
			} else {
				if(!IRCServer.whitelistIPs.get(IRCServer.whitelist.indexOf(nickname)).contains(addr) &&
						!"*".equals(IRCServer.whitelistIPs.get(IRCServer.whitelist.indexOf(nickname)))) {
					WNLogger.l.info("Kicked " + nickname + ": not in white-list (invalid IP)");
					sendln("ERROR :You are not in white-list.");
					socket.close(); socket = null;
					return;
				}
			}
		}

		WNLogger.l.info(nickname + " (" + socket.getInetAddress().toString().substring(1) + ") has logged in.");
		if(c.showIntro > 0) sendEvent(1, ":Welcome, " + nickname + "!");
		if(c.showIntro > 1) {
			sendEvent(11, ":JayWormNET " + JayWormNet.version + " IRC emulator");
			sendEvent(11, ":Based on CyberShadow's MyWormNET code");
			sendEvent(11, ":with some of StepS' improvements;");
			sendEvent(11, ":Rewrite in Java by MEDVEDx64.");

			sendEvent(2, ":Your host is " + IRCServer.config.serverHost + ", running JayWormNET " + JayWormNet.version);
		}

		if(c.showCreated)				sendEvent(3, ":This server was created " + IRCServer.created);
		if(c.showPlayersCount)			sendEvent(251, ":There are " + IRCServer.users.size() + " users on the server.");
		if(c.showCapabilities)
			sendEvent(5, (c.capPrefix.length() == 0? "": ("PREFIX=" + c.capPrefix))
					+ (c.capChanTypes.length() == 0? "": (" CHANTYPES=" + c.capChanTypes))
					+ " NICKLEN=15" + (c.capChanModes.length() == 0? "": (" CHANMODES=" + c.capChanModes + " "))
					+ ":are supported by this server");

		if(c.showOps) {
			int ops = 0;
			for(int i = 0; i < IRCServer.users.size(); i++)
				if(IRCServer.users.get(i).modes['o'])
						ops++;
			sendEvent(252, ops + " :IRC operators online");
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
		String password = "";

		try {

			do {
				// Read from client
				byte[] bytes = new byte[100000]; // it's potentially .. sux
				int bytesRead = in.read(bytes);

				if(bytesRead <= 0) {
					throw new Exception("disconnected");
				}

				String[] lines = (IRCServer.config.charset.equals("native")? WACharTable.decode(bytes):
					new String(bytes, IRCServer.config.charset)).trim().split("\n+");
				WNLogger.l.finest("Input message: " + (IRCServer.config.charset.equals("native")?
						WACharTable.decode(bytes):
					new String(bytes, IRCServer.config.charset)).trim());

				for(int i = 0; i < lines.length; i++) {
					String buffer = lines[i].trim();

					if(buffer.length() == 0) {
						sendEvent(421, ":Empty message");
						continue;
					}

					// Parsing
					String command = buffer.toUpperCase().substring(0, (buffer + " ").indexOf(" "));
					String body = buffer.contains(" ")? buffer.substring(command.length() + 1): "";

					if(command.equals("PING"))
						sendln("PONG " + (body.indexOf(':') < 0? ":" + serverHost: body.substring(body.indexOf(':'))));
					else if(command.equals("PONG")) {
						isPingPassed = true;
					} else if(command.equals("PASS")) {
						password = body.split(" +")[0];
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

							if(nickFiltered.length() == 0) {
								sendError(433, body + " :Bad nickname");
								throw new Exception("Bad nickname");
							}
							else {

								if(IRCServer.getUserByNickName(nickFiltered) != null) {
									sendln(":" + serverHost + " 433 " + nickFiltered + " :Nickname is already in use");
									throw new Exception("Nickname is already in use");
								}
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

						// Password check
						if(IRCServer.config.useIRCPassword) {
							try {
								if(!password.equals(IRCServer.config.IRCPassword))
									throw new Exception();
							} catch(Exception e) {
								sendln("ERROR :Closing Link: " + getNickname() + "[~" + username + "@" + getAddress()
										+ "] (Bad Password)");
								throw new Exception("Bad password");
							}
						}
						
						try {
							username 	= prefix[0];
							hostname 	= prefix[1];
							servername 	= prefix[2];
							realname	= splitted[1];
						} catch(ArrayIndexOutOfBoundsException | NullPointerException e) {
						}

						if(username.length() > 0)
							login();
					}

					else if(command.equals("QUIT")) {
						quit((body.length() == 0? "": body.substring(1)));
						break;
					}

					else if(command.equals("JOIN")) {
						String chName = (body.charAt(0) == '#'? body.substring(1): body).split(" +")[0];

						if(nickname.length() == 0)
							sendln(":" + serverHost + " 451 :Register first.");
						else if(Channel.indexOf(channels, chName) == -1)
							sendEvent(403, "#" + chName	+ " :No such channel.");
						else if(inChannel[Channel.indexOf(channels, chName)]) {
							sendln(":" + serverHost + " 403 " + nickname + " #" + chName
									+ " :You already are in channel #" + chName);
						} else {

							WNLogger.l.info(nickname + " has joined #" + chName);
							inChannel[Channel.indexOf(channels, chName)] = true;
							IRCServer.broadcast(formatUserID() + " JOIN :#" + chName, chName);
							if(modes['o'])
								IRCServer.broadcast(":" + serverHost + " MODE #"
										+ chName + " +o " + nickname, chName);

							String response = ":" + serverHost + " 353 " + nickname + " = #"
									+ chName + " :";
							for(int z = 0; z < IRCServer.users.size(); z++) {
								User u = IRCServer.users.get(z);
								if(u.inChannel[Channel.indexOf(channels, chName)]) {
									if(IRCServer.users.get(z).modes['o'])
										response += "@";
									response = response + u.nickname + " ";
								}
							}
							sendln(response);
							sendEvent(336, "#" + chName + " :End of /NAMES list.");

						}
					}

					else if(command.equals("PART")) {
						String chName = body.charAt(0) == '#'? body.split(" +")[0].substring(1): body.split(" +")[0];
						String reason = null;
						if(body.indexOf(':') != -1) {
							try {
								reason = body.substring(body.indexOf(':') + 1);
							} catch(StringIndexOutOfBoundsException e) {
							}
						}
						part(chName, reason);
					}

					else if(command.equals("MODE")) {
						String[] splitted = body.split(" +");
						if(splitted.length != 1)
							sendEvent(472, ":Sorry, you can't set modes for anything.");
						else {
							try {
								if(splitted[0].charAt(0) == '#' && IRCServer.isChannelExist(splitted[0].substring(1)))
									sendEvent(324, splitted[0] + " +tn");
								else if(IRCServer.getUserByNickName(splitted[0]) != null) {
									String modeStr = "";
									for(char c = 'a'; c < 'z'; c++)
										if(modes[c])
											modeStr = modeStr + c;
									sendEvent(324, splitted[0] + " +" + modeStr);
								} else
									sendEvent(401, splitted[0] + "  :No such nick/channel.");
							} catch(Exception e) {
								sendEvent(401, splitted[0] + "  :No such nick/channel.");
							}
						}
					}

					else if(command.equals("PRIVMSG") || command.equals("NOTICE")) {
						if(nickname.length() == 0)
							sendln(":" + serverHost + " 451 :Register first.");
						else {
							String target = body.substring(0, (body + " ").indexOf(' '));
							String trailer = (body.indexOf(':') < 0 || body.indexOf(':') == body.length()-1)?
									"": body.substring(body.indexOf(':'));

							if(target.length() == 0) // to avoid drop when nickname with incorrect characters requested
								sendEvent(401, ":No such nick/channel.");
							else {
								// Is a channel?
								if(target.charAt(0) == '#') {
									if(IRCServer.isChannelExist(target.substring(1))) {
										// Anti-flood
										if(IRCServer.config.antiFloodEnabled) {
											if(((new Date()).getTime() - lastMessage.getTime()) < IRCServer.config.floodGate)
												floodLevel += 2;
											else if(floodLevel > 0)
												floodLevel--;
	
											if(floodLevel > IRCServer.config.floodMaxLevel) {
												sendln("ERROR :Flooding");
												throw new Exception("disconnected: flooding");
											}
											
											lastMessage = new Date();
										}
										
										// Parsing additional commands
										if(trailer.charAt(1) == '!' && IRCServer.config.commandsEnabled) {
											new CommandHandler(this, target.substring(1),
													trailer.substring(1).trim().split(" +"), IRCServer.config);
											if(IRCServer.config.showCommandsInChat) {
												IRCServer.broadcast(formatUserID() + " " + command + " "
														+ target + " " + trailer, target.substring(1), this);
												WNLogger.l.finer(nickname + " <" + target + ">: " + trailer.substring(1));
											}
										}
										
										else {
											IRCServer.broadcast(formatUserID() + " " + command + " "
													+ target + " " + trailer, target.substring(1), this);
											WNLogger.l.finer(nickname + " <" + target + ">: " + trailer.substring(1));
										}
									}
									else
										sendEvent(401, target + " :No such nick/channel.");
								}
								else {
									User u = IRCServer.getUserByNickName(target);
									// If user doesn't exist
									if(u == null)
										sendEvent(401, target + " :No such nick/channel.");
									else {
										u.sendln(formatUserID() + " " + command + " " + target + " " + trailer);
										WNLogger.l.fine(nickname + " <" + target + ">: " + trailer.substring(1));
									}
								}
							}

						}
					}

					else if(command.equals("OPER")) {
						String[] splitted = body.split(" +");
						try {
							String operPw = splitted[0];
							if(operPw.trim().length() == 0)
								throw new Exception("Not enough params");
	
							if(operPw.equals(IRCServer.config.IRCOperPassword)) {
								WNLogger.l.info(nickname + " (" + addr + ") has registered as operator");
								modes['o'] = true;
								sendln(":" + nickname + " MODE " + nickname + " :+o");
								for(int z = 0; z < channels.length; z++)
									if(inChannel[z])
										IRCServer.broadcast(":" + serverHost + " MODE " + channels[z].name + " +o "
												+ getNickname(), channels[z].name);
							} else {
								sendError(464, ":Bad password");
								WNLogger.l.info(getNickname() + ": unsuccessful attempt to register as operator");
							}
						} catch (Exception e) {
							sendError(461, ":Need more parameters");
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
			quit(quitMessage.length() == 0? (e.getMessage().contains("disconnect")? e.getMessage():
				"Internal server error"): quitMessage);
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

			WNLogger.l.info(getNickname() + " (" + addr + ") has disconnected.");

			// Removing myself
			if(IRCServer.users.contains(this))
				IRCServer.users.remove(this);
		}
	}

	public void part(String chName, String reason) {
		if(Channel.indexOf(IRCServer.channels, chName) != -1) {
			if(inChannel[Channel.indexOf(IRCServer.channels, chName)]) {
				WNLogger.l.info(nickname + " has left #" + chName);
				IRCServer.broadcast(formatUserID() + " PART #" + chName + (reason == null? "": " :" + reason), chName);
				inChannel[Channel.indexOf(IRCServer.channels, chName)] = false;
			} else sendError(442, ":You're not on that channel");
		} else sendError(403, ":No such channel");
	}
	
	public void quit(String reason) {
		for(int z = 0; z < IRCServer.channels.length; z++) {
			if(inChannel[z]) {
				IRCServer.broadcast(formatUserID() + " QUIT :" + reason, IRCServer.channels[z].name);
				inChannel[z] = false;
			}
		}

		try {
			if(socket != null)
				socket.close();
		} catch(IOException e) {
			e.printStackTrace();
			WNLogger.l.warning("quit(): " + e);
		}
		socket = null;
	}

	public String getAddress() {
		return IRCServer.config.useStealthIP? IRCServer.config.stealthIP: connectingFrom;
	}
	
	public String formatUserID() {
		return ":" + nickname + "!" + username + "@" + getAddress();
	}
	
	public String formatMessage(String nick, String message) {
		return formatUserID() + " NOTICE " + nick + " :" + message;
	}
	
	public String formatMessage(String unused, String message, String channel) {
		return formatUserID() + " NOTICE #" + channel + " :" + message;
	}

	public String getNickname() {
		return (nickname.length() == 0? "<noname>": nickname);
	}

	public void sendln(String s) {
		if(socket != null) {
			try {
				if(IRCServer.config.charset.equals("native"))
					out.write(WACharTable.encode(s+"\r\n"));
				else
					out.write((s+"\r\n").getBytes(IRCServer.config.charset));
				WNLogger.l.finest("Output message: " + s+"\r\n");
			} catch(Exception e) {
				e.printStackTrace();
				WNLogger.l.warning("Failed to send a response: " + e);
			}
		}
	}
	
	public void sendMessage(String message) {
		sendln(formatMessage(getNickname(), message));
	}

	String getEventCode(int event) {
		String eventCode = String.valueOf(event);
		while(eventCode.length() < 3)
			eventCode = "0" + eventCode;
		return eventCode;
	}

	public void sendEvent(int event, String s) {
		sendln(":" + IRCServer.config.serverHost + " " + getEventCode(event) + " " + getNickname() + " " + s);
	}

	public void sendError(int error, String s) {
		sendln(":" + IRCServer.config.serverHost + " "+ getEventCode(error) + " " + s);
	}
	
	public void sendSpecialMessage(String s) {
		sendEvent(300, ":" + s);
	}

	public boolean inAnyChannel() {
		for(int i = 0; i < IRCServer.channels.length; i++)
			if(inChannel[i]) return true;
		return false;
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

	// Lists are read from csv, where first column is nickname, second — ip address.
	public final static ArrayList<String> banlist = new ArrayList<String>();
	public final static ArrayList<String> banlistIPs = new ArrayList<String>();
	public final static ArrayList<String> whitelist = new ArrayList<String>();
	public final static ArrayList<String> whitelistIPs = new ArrayList<String>();

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
		// Reading ban-/white-list
		reloadLists();
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
						Thread.sleep(config.IRCFailureSleepTime);
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
	
	public static void broadcastEvent(int event, String s) {
		for(int i = 0; i < users.size(); i++)
			users.get(i).sendEvent(event,  s);
	}
	
	// only for opers
	public static void broadcastOperEvent(int event, String s, String channel) {
		for(int i = 0; i < users.size(); i++) {
			User u = users.get(i);
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

	public static User getUserByNickName(String nickname) {
		for(int i = 0; i < users.size(); i++) {
			User found = users.get(i);
			if(found.nickname.equalsIgnoreCase(nickname))
				return found;
		}
		return null;
	}
	
	public IRCServer(ConfigurationManager c) {
		config = c;
		reloadChannels();
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
	
	public void reloadChannels() {
		channels = readChannelsFromFile(config.channelsFileName);
	}
	
	public void reloadLists() {
		if(config.enableBanList) readList(banlist, banlistIPs, config.banListFileName);
		if(config.enableWhiteList) readList(whitelist, whitelistIPs, config.whiteListFileName);
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
