/*/ Part of JayWormNET source code. (C) 2013 Andrey Bobkov (MEDVEDx64).
    Licensed under the Apache License, Version 2.0.  /*/

package org.themassacre.jaywnet;

import java.util.ArrayList;

class CommandLookupException extends Exception {
	private static final long serialVersionUID = -2022485016669801767L;
	public CommandLookupException(String msg) {
		super(msg);
	}
}

// IRC macro-commands handler
public class CommandHandler {
	ArrayList<IIRCAdditionalCommand> commands = new ArrayList<IIRCAdditionalCommand>();

	public boolean isCommandExist(String cmdName) {
		for(int i = 0; i < commands.size(); i++)
			if(commands.get(i).getName().equals(cmdName) && commands.get(i).isEnabled())
				return true;
		return false;
	}

	public boolean isCommandExist(String[] args) {
		try {
			return isCommandExist(obtainCommand(args));
		} catch(ArrayIndexOutOfBoundsException | StringIndexOutOfBoundsException e) {
			return false;
		}
	}

	String obtainCommand(String[] args) throws ArrayIndexOutOfBoundsException, StringIndexOutOfBoundsException {
		return args[0].substring(1);
	}

	public void registerCommand(IIRCAdditionalCommand cmd) {
		commands.add(cmd);
	}

	public void parse(User sender, String channel, String[] args) {
		try {
			String cmd = obtainCommand(args); // warning: args contains a command too
			try {
				boolean found = false;
				for(int i = 0; i < commands.size(); i++) {
					IIRCAdditionalCommand current = commands.get(i);
					if(current.getName().equals(cmd)) {
						// Permissions check
						if(!current.isEnabled()) continue;
						if(current.getPermissionLevel() < 1 ||
								(current.getPermissionLevel() == 1 && !sender.modes['o']))
									throw new CommandLookupException("Permission denied");

						// Arguments count check
						if(args.length < current.getRequiredArgsCount())
							throw new CommandLookupException("Not enough parameters.");

						found = true;
						commands.get(i).execute(sender, channel, args);
						WNLogger.l.info("User '" + sender.getNickname() + "' (" + sender.connectingFrom +
								") invoked a command: " + cmd);
						break;
					}
				}

				if(!found)
					throw new CommandLookupException("Unknown command: " + cmd);
			} catch(CommandLookupException eCmd) {
				sender.sendSpecialMessage(eCmd.getMessage());
				WNLogger.l.warning("Execution of '" + cmd + "' invoked by user '" + sender.getNickname()
						+ "' (" + sender.connectingFrom + ") has failed: " + eCmd.getMessage());
			}

		} catch(NullPointerException | ArrayIndexOutOfBoundsException | StringIndexOutOfBoundsException e) {
			sender.sendSpecialMessage("Internal server error.");
		}
	}

	public CommandHandler() { // Standard commands set
		registerCommand(new KickCommand());
		registerCommand(new OperCommand());
		registerCommand(new ReloadCommand());
		registerCommand(new SpecialCommand());
	}

	public CommandHandler(ArrayList<IIRCAdditionalCommand> commands) {
		if(commands != null)
			this.commands = commands;
	}

}

//// Generic commands ////

class KickCommand implements IIRCAdditionalCommand {
	@Override public String getName() {
		return "kick";
	}

	@Override public boolean isEnabled() {
		return JayWormNet.config.enableKickCommand;
	}

	@Override
	public int getRequiredArgsCount() {
		return 2;
	}

	@Override
	public int getPermissionLevel() {
		return 1;
	}

	@Override
	public void execute(User sender, String channel, String[] args) {
		User user = IRCServer.getUserByNickName(args[1]);
		if(user != null) {
			user.sendln("ERROR :Kicked from server" + (args.length < 3? "": ": " + args[2]));
			user.quit("Kicked by an operator" + (args.length < 3? "": ": " + args[2]));
			IRCServer.broadcastOperSpecialMessage(sender.getNickname() + ": kicked user '" + args[1] + "'", channel);
			WNLogger.l.info(sender.getNickname() + " (" + sender.connectingFrom + ") " + "kicked "
					+ args[1] + (args.length < 3? "": ": " + args[2]));
		} else
			sender.sendSpecialMessage("No such user: " + args[1]);
	}
}

class OperCommand implements IIRCAdditionalCommand {

	@Override
	public String getName() {
		return "oper";
	}

	@Override
	public int getRequiredArgsCount() {
		return 2;
	}

	@Override
	public boolean isEnabled() {
		return JayWormNet.config.enableOperCommand;
	}

	@Override
	public int getPermissionLevel() {
		return 2;
	}

	@Override
	public void execute(User sender, String channel, String[] args) {
		if(!args[1].equals(JayWormNet.config.IRCOperPassword)) {
			sender.sendSpecialMessage("Bad password.");
			return;
		}

		if(args.length == 2) {
			sender.modes['o'] = !sender.modes['o'];
			sender.sendSpecialMessage(sender.modes['o']? "You now are an operator!": "You no longer are an operator.");
			if(JayWormNet.config.showOperatorsActions)
				IRCServer.broadcastOperSpecialMessage((sender.modes['o']?
					sender.getNickname() + " now are an operator": sender.getNickname()
					+ " no longer are an operator"), channel);
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
			if(JayWormNet.config.showOperatorsActions)
				IRCServer.broadcastOperSpecialMessage((u.modes['o']?
					u.getNickname() + " now are an operator": u.getNickname()
					+ " no longer are an operator"), channel);
			if(sender.modes['o']) WNLogger.l.info(sender.getNickname() + " (" + sender.connectingFrom + ")"
					+ " gave operator's privileges to " + u.getNickname() + " (" + u.connectingFrom + ")");
			else WNLogger.l.info(sender.getNickname() + " (" + sender.connectingFrom + ")"
					+ " revoked operator's privileges from " + u.getNickname() + " (" + u.connectingFrom + ")");
		}
	}

}

class ReloadCommand implements IIRCAdditionalCommand {

	@Override
	public String getName() {
		return "reload";
	}

	@Override
	public int getRequiredArgsCount() {
		return 0;
	}

	@Override
	public boolean isEnabled() {
		return JayWormNet.config.enableReloadCommand;
	}

	@Override
	public int getPermissionLevel() {
		return 1;
	}

	// Re-read and apply most of server configuration on-the-fly
	@Override public void execute(User sender, String channel, String[] args) {
		if(JayWormNet.config.showOperatorsActions)
			IRCServer.broadcastOperSpecialMessage(sender.getNickname() + ": reloading server configuration", channel);
		WNLogger.l.info(sender.getNickname() + " (" + sender.connectingFrom + ") initiated reloading of"
				+ " server's configuration");

		JayWormNet.config.reload();
		JayWormNet.http.readMOTD();
		JayWormNet.irc.readMOTD();
		JayWormNet.irc.reloadChannels();
		JayWormNet.irc.reloadLists();

		sender.sendSpecialMessage("Reload complete.");
		WNLogger.l.info("Configuration reload complete");

	}

}

class SpecialCommand implements IIRCAdditionalCommand {

	@Override
	public String getName() {
		return "s";
	}

	@Override
	public int getRequiredArgsCount() {
		return 2;
	}

	@Override
	public boolean isEnabled() {
		return JayWormNet.config.enableSpecialCommand;
	}

	@Override
	public int getPermissionLevel() {
		return JayWormNet.config.specialMessagesPermissionLevel;
	}

	@Override
	public void execute(User sender, String channel, String[] args) {
		String message = "";
		for(int i = 1; i < args.length; i++)
			message = message + args[i] + (i == args.length-1? "": " ");
		IRCServer.broadcastSpecialMessage("[" + sender.getNickname() + "] " + message);

	}

}
