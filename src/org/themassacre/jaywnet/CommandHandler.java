/*/ Part of JayWormNET source code. (C) 2013 Andrey Bobkov (MEDVEDx64).
    Licensed under the Apache License, Version 2.0.  /*/

package org.themassacre.jaywnet;

// IRC macro-commands handler
public class CommandHandler {
	public CommandHandler(User sender, String channel, String[] args) {
		try {
			boolean isCommandUsed = true;
			String cmd = args[0].substring(1);
			ConfigurationManager c = IRCServer.config;
			
			////// Commands implementation here //////
			
			if(cmd.equals("kick") && c.enableKickCommand)
				kickUser(sender, channel, args);
			else if(cmd.equals("reload") && c.enableReloadCommand)
				reload(sender, c);
			else if(cmd.equals("oper") && c.enableOperCommand)
				operCheckout(sender, channel, args, c);
			else if(cmd.equals("anon") && c.enableAnonCommand)
				// TODO: anon message permission control
				IRCServer.broadcastSpecialMessage(args[1]);
			
			else {
				isCommandUsed = false;
				if(!c.showCommandsInChat)
					sender.sendSpecialMessage("Unknown command: " + cmd);
			}
			
			if(isCommandUsed)
				WNLogger.l.info("User '" + sender.getNickname() + "' (" + sender.connectingFrom +
						") invoked a command: " + cmd);
			
		} catch(NullPointerException | ArrayIndexOutOfBoundsException | StringIndexOutOfBoundsException e) {
			sender.sendSpecialMessage("Internal server error.");
		}
	}
	
	void kickUser(User sender, String channel, String[] args) {
		if(args.length < 2)
			sender.sendSpecialMessage("Not enough parameters.");
		else {
			if(sender.modes['o']) {
				User user = IRCServer.getUserByNickName(args[1]);
				if(user != null) {
					user.sendln("ERROR :Kicked from server" + (args.length < 3? "": ": " + args[2]));
					user.quit("Kicked by an operator" + (args.length < 3? "": ": " + args[2]));
					WNLogger.l.info(sender.getNickname() + " (" + sender.connectingFrom + ") " + "kicked "
							+ args[1] + (args.length < 3? "": ": " + args[2]));
				} else
					sender.sendSpecialMessage("No such user: " + args[1]);
			} else {
				sender.sendSpecialMessage("Permission denied.");
				WNLogger.l.info(sender.getNickname() + " (" + sender.connectingFrom + ") attempted to kick "
						+ args[1] + ", but has no permissions");
			}
		}
	}
	
	// Re-read and apply most of server configuration on-the-fly
	void reload(User sender, ConfigurationManager c) {
		if(!sender.modes['o']) {
			sender.sendSpecialMessage("Permission denied.");
			WNLogger.l.info(sender.getNickname() + " (" + sender.connectingFrom + ") attempted to reload "
					+ "server's configuration, but has no permissions");
			return;
		}
		
		WNLogger.l.info(sender.getNickname() + " (" + sender.connectingFrom + ") initiated reloading of"
				+ " server's configuration");
		
		c.reload();
		JayWormNet.http.readMOTD();
		JayWormNet.irc.readMOTD();
		JayWormNet.irc.reloadChannels();
		JayWormNet.irc.reloadLists();
		
		sender.sendSpecialMessage("Reload complete.");
		WNLogger.l.info("Configuration reload complete");
	}
	
	void operCheckout(User sender, String channel, String[] args, ConfigurationManager c) {
		if(args.length < 2) {
			sender.sendSpecialMessage("Not enough parameters.");
			return;
		}
		
		if(!args[1].equals(c.IRCOperPassword)) {
			sender.sendSpecialMessage("Bad password.");
			return;
		}
		
		if(args.length == 2) {
			sender.modes['o'] = !sender.modes['o'];
			sender.sendSpecialMessage(sender.modes['o']? "You now are an operator!": "You no longer are an operator.");
			if(c.announceOperators)
				IRCServer.broadcast(sender.formatMessage(null, "* " + (sender.modes['o']?
					sender.getNickname() + " now are an operator": sender.getNickname()
					+ " no longer are an operator"), channel), channel, sender);
			if(sender.modes['o']) WNLogger.l.info(sender.getNickname() + " has registered as an operator");
			
		} else {
			User u = IRCServer.getUserByNickName(args[2]);
			if(u == null) {
				sender.sendSpecialMessage("No such user: " + args[2]);
				return;
			}
			
			u.modes['o'] = !u.modes['o'];
			u.sendSpecialMessage(u.modes['o']? "You now are an operator!": "You no longer are an operator.");
			sender.sendSpecialMessage(u.modes['o']? args[2] + " are now an operator.": args[2] + " no longer are an operator.");
			if(c.announceOperators)
				IRCServer.broadcast(sender.formatMessage(null, "* " + (u.modes['o']?
					u.getNickname() + " now are an operator": u.getNickname()
					+ " no longer are an operator"), channel), channel, sender);
			if(sender.modes['o']) WNLogger.l.info(sender.getNickname() + " (" + sender.connectingFrom + ")"
					+ " gave operator's privileges to " + u.getNickname() + " (" + u.connectingFrom + ")");
			else WNLogger.l.info(sender.getNickname() + " (" + sender.connectingFrom + ")"
					+ " revoked operator's privileges from " + u.getNickname() + " (" + u.connectingFrom + ")");
		}
	}
}
