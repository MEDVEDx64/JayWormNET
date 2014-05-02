/*/ Part of JayWormNET source code. (C) 2013 Andrey Bobkov (MEDVEDx64).
    Licensed under the Apache License, Version 2.0.  /*/

package org.themassacre.jaywnet.cmd;

import org.themassacre.jaywnet.*;

public class kick implements IIRCAdditionalCommand {
	@Override
	public int getRequiredArgsCount() {
		return 2;
	}

	@Override
	public int getPermissionLevel() {
		return 1;
	}

	@Override
	public void execute(IRCUser sender, String channel, String[] args) {
		IRCUser user = IRCServer.getUserByNickName(args[1]);
		if(user != null) {
			user.sendln("ERROR :Kicked from server" + (args.length < 3? "": ": " + args[2]));
			user.quit("Kicked by an operator" + (args.length < 3? "": ": " + args[2]));
			IRCServer.broadcastOperSpecialMessage(sender.getNickname() + ": kicked user '" + args[1] + "'", channel);
			WNLogger.l.info(sender.getNickname() + " (" + sender.getInetAddress() + ") " + "kicked "
					+ args[1] + (args.length < 3? "": ": " + args[2]));
		} else
			sender.sendSpecialMessage("No such user: " + args[1]);
	}
}
