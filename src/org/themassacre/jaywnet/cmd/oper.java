/*/ Part of JayWormNET source code. (C) 2013 Andrey Bobkov (MEDVEDx64).
    Licensed under the Apache License, Version 2.0.  /*/

package org.themassacre.jaywnet.cmd;
import org.themassacre.jaywnet.*;

public class oper implements IIRCAdditionalCommand {
	@Override
	public int getRequiredArgsCount() {
		return 2;
	}

	@Override
	public int getPermissionLevel() {
		return 2;
	}

	@Override
	public void execute(IRCUser sender, String channel, String[] args) {
		boolean[] modes = sender.getModes();
		
		if(!args[1].equals(JayWormNet.config.IRCOperPassword)) {
			sender.sendSpecialMessage("Bad password.");
			return;
		}

		if(args.length == 2) {
			modes['o'] = !modes['o'];
			sender.sendSpecialMessage(modes['o']? "You now are an operator!": "You no longer are an operator.");
			if(JayWormNet.config.showOperatorsActions)
				IRCServer.broadcastOperSpecialMessage((modes['o']?
					sender.getNickname() + " now are an operator": sender.getNickname()
					+ " no longer are an operator"), channel);
			if(modes['o']) WNLogger.l.info(sender.getNickname() + " has registered as an operator");

		} else {
			IRCUser u = IRCServer.getUserByNickName(args[2]);
			if(u == null) {
				sender.sendSpecialMessage("No such user: " + args[2]);
				return;
			}
			
			boolean[] usermodes = u.getModes();

			usermodes['o'] = !usermodes['o'];
			u.sendSpecialMessage(usermodes['o']? "You now are an operator!": "You no longer are an operator.");
			sender.sendSpecialMessage(usermodes['o']? args[2] + " are now an operator.": args[2] + " no longer are an operator.");
			if(JayWormNet.config.showOperatorsActions)
				IRCServer.broadcastOperSpecialMessage((usermodes['o']?
					u.getNickname() + " now are an operator": u.getNickname()
					+ " no longer are an operator"), channel);
			if(modes['o']) WNLogger.l.info(sender.getNickname() + " (" + sender.getInetAddress() + ")"
					+ " gave operator's privileges to " + u.getNickname() + " (" + u.getInetAddress() + ")");
			else WNLogger.l.info(sender.getNickname() + " (" + sender.getInetAddress() + ")"
					+ " revoked operator's privileges from " + u.getNickname() + " (" + u.getInetAddress() + ")");
		}
	}

}
