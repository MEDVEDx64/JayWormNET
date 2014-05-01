/*/ Part of JayWormNET source code. (C) 2013 Andrey Bobkov (MEDVEDx64).
    Licensed under the Apache License, Version 2.0.  /*/

package org.themassacre.jaywnet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.net.Socket;
import java.util.Date;

import org.themassacre.util.WACharTable;

public class IRCUser extends Thread {
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

	Date lastMessage = new Date();
	int floodLevel = 0;

	void login() throws IOException {
		ConfigurationManager c = JayWormNet.config; // For shorter access
		String addr = socket.getInetAddress().toString().substring(1);

		// Checking ban-list
		if(JayWormNet.config.enableBanList) {
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
		if(JayWormNet.config.enableWhiteList) {
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

			sendEvent(2, ":Your host is " + JayWormNet.config.serverHost + ", running JayWormNET " + JayWormNet.version);
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
		final String serverHost = JayWormNet.config.serverHost;
		final Channel[] channels = IRCServer.channels;
		String addr = socket.getInetAddress().toString().substring(1);
		String password = "";

		try {

			do {
				// Read from client
				byte[] bytes = new byte[JayWormNet.config.IRCBufferSize];
				int bytesRead = in.read(bytes);
				
				if(JayWormNet.config.IRCSkipBytesWhenAvailable) {
					if(JayWormNet.config.dropIRCUserOnOverflow) {
						if(in.available() > 0) {
							in.skip(in.available());
							sendln("ERROR :Input message is too large.");
							throw new Exception("disconnected: input message is too large");
						}
					} else if(in.available() > 0)
						in.skip(in.available());
				}

				if(bytesRead <= 0) {
					throw new Exception("disconnected");
				}

				String[] lines = (JayWormNet.config.charset.equals("native")? WACharTable.decode(bytes):
					new String(bytes, JayWormNet.config.charset)).trim().split("\n+");
				WNLogger.l.finest("Input message: " + (JayWormNet.config.charset.equals("native")?
						WACharTable.decode(bytes):
					new String(bytes, JayWormNet.config.charset)).trim());

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
						if(JayWormNet.config.useIRCPassword) {
							try {
								if(!password.equals(JayWormNet.config.IRCPassword))
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
								IRCUser u = IRCServer.users.get(z);
								if(u.inChannel[Channel.indexOf(channels, chName)]) {
									if(IRCServer.users.get(z).modes['o'])
										response += "@";
									response = response + u.nickname + " ";
								}
							}
							sendln(response);

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
										if(JayWormNet.config.antiFloodEnabled) {
											if(((new Date()).getTime() - lastMessage.getTime()) < JayWormNet.config.floodGate)
												floodLevel += 2;
											else if(floodLevel > 0)
												floodLevel--;

											if(floodLevel > JayWormNet.config.floodMaxLevel) {
												sendln("ERROR :Flooding");
												throw new Exception("disconnected: flooding");
											}

											lastMessage = new Date();
										}

										// Parsing additional commands
										if(trailer.charAt(1) == '!' && JayWormNet.config.commandsEnabled) {
											String cmd = trailer.substring(1).trim().split(" +")[0].substring(1);
											IIRCAdditionalCommand cmdObj = null;
											boolean exist = true;
											
											// Creating command object by it's name
											try {
												Class<?> c = Class.forName(JayWormNet.config.commandsPackageName + "." + cmd);
												Constructor<?> ctor = c.getConstructor();
												cmdObj = (IIRCAdditionalCommand)ctor.newInstance();
											} catch(NullPointerException | ClassNotFoundException | NoSuchMethodException e) {
												exist = false;
												sendSpecialMessage("No such command");
												WNLogger.l.warning("User " + nickname +
														" tried to invoke non-existant additional command: " + cmd);
											}
											
											// Commands white-list check
											if(!IRCServer.allowedAdditionalCommands.contains(cmd) && exist) {
												exist = false;
												sendSpecialMessage("No such command");
												WNLogger.l.warning("User " + nickname +
														" tried to invoke additional command: " + cmd + ", which is not white-listed");
											}
											
											if((JayWormNet.config.showCommandsInChat || !exist)
													&& !JayWormNet.config.swallowAllCommands) {
												IRCServer.broadcast(formatUserID() + " " + command + " "
														+ target + " " + trailer, target.substring(1), this);
												WNLogger.l.finer(nickname + " <" + target + ">: " + trailer.substring(1));
											}

											if(exist || JayWormNet.config.swallowAllCommands) {
												try {
													// Permissions check
													if(cmdObj.getPermissionLevel() < 1 ||
															(cmdObj.getPermissionLevel() == 1 && !modes['o']))
																throw new CommandInvocationException("Permission denied");

													// Arguments count check
													if(trailer.substring(1).trim().split(" +").length < cmdObj.getRequiredArgsCount())
														throw new CommandInvocationException("Not enough parameters.");
													
													cmdObj.execute(this, target.substring(1),
															trailer.substring(1).trim().split(" +"));
													WNLogger.l.info("User '" + getNickname() + "' (" + connectingFrom +
															") invoked a command: " + cmd);
												} catch(CommandInvocationException eCmd) {
													sendSpecialMessage(eCmd.getMessage());
												} catch(Exception e) {
													sendSpecialMessage("Internal server error");
													WNLogger.l.warning("Execution of '" + cmd + "' invoked by user '" + nickname
															+ "' (" + connectingFrom + ") has crashed: " + e);
												}
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
									IRCUser u = IRCServer.getUserByNickName(target);
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

							if(operPw.equals(JayWormNet.config.IRCOperPassword)) {
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
							IRCUser u = IRCServer.users.get(z);
							for(int x = 0; x < channels.length; x++) {
								
								try {
									String[] args = body.split(" +");
									if(!args[0].equals(u.nickname) && args[0].length() > 0 && !args[0].equals("*"))
										continue;
									if(args[0].length() > 0 && args[1].equals("o") && !u.modes['o'])
										continue;
								} catch(NullPointerException | ArrayIndexOutOfBoundsException e) {
								}
								
								if(u.inChannel[x] && !modes['i'])
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
		return JayWormNet.config.useStealthIP? JayWormNet.config.stealthIP: connectingFrom;
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
				if(JayWormNet.config.charset.equals("native"))
					out.write(WACharTable.encode(s+"\r\n"));
				else
					out.write((s+"\r\n").getBytes(JayWormNet.config.charset));
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
		sendln(":" + JayWormNet.config.serverHost + " " + getEventCode(event) + " " + getNickname() + " " + s);
	}

	public void sendError(int error, String s) {
		sendln(":" + JayWormNet.config.serverHost + " "+ getEventCode(error) + " " + s);
	}

	public void sendSpecialMessage(String s) {
		sendEvent(300, ":" + s);
	}

	public boolean inAnyChannel() {
		for(int i = 0; i < IRCServer.channels.length; i++)
			if(inChannel[i]) return true;
		return false;
	}

	public IRCUser() {
		inChannel = new boolean[IRCServer.channels.length];
		if(JayWormNet.config.pingsEnabled)
			(new UserWatcher(this)).start(); // now you own your Personal Watcher
	}
}