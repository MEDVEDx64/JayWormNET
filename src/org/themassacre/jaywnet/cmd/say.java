/*/ Part of JayWormNET source code. (C) 2013 Andrey Bobkov (MEDVEDx64).
    Licensed under the Apache License, Version 2.0.  /*/

package org.themassacre.jaywnet.cmd;
import org.themassacre.jaywnet.*;

public class say implements IIRCAdditionalCommand {
	@Override
	public int getRequiredArgsCount() {
		return 2;
	}

	@Override
	public int getPermissionLevel() {
		return JayWormNet.config.specialMessagesPermissionLevel;
	}

	@Override
	public void execute(IRCUser sender, String channel, String[] args) {
		String message = "";
		for(int i = 1; i < args.length; i++)
			message = message + args[i] + (i == args.length-1? "": " ");
		IRCServer.broadcastSpecialMessage("[" + sender.getNickname() + "] " + message);

	}

}
