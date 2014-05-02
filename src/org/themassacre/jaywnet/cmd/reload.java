/*/ Part of JayWormNET source code. (C) 2013 Andrey Bobkov (MEDVEDx64).
    Licensed under the Apache License, Version 2.0.  /*/

package org.themassacre.jaywnet.cmd;
import org.themassacre.jaywnet.*;

public class reload implements IIRCAdditionalCommand {
	@Override
	public int getRequiredArgsCount() {
		return 0;
	}

	@Override
	public int getPermissionLevel() {
		return 1;
	}

	// Re-read and apply most of server configuration on-the-fly
	@Override public void execute(IRCUser sender, String channel, String[] args) {
		if(JayWormNet.config.showOperatorsActions)
			IRCServer.broadcastOperSpecialMessage(sender.getNickname() + ": reloading server configuration", channel);
		WNLogger.l.info(sender.getNickname() + " (" + sender.getInetAddress() + ") initiated reloading of"
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

