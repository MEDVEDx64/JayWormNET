/*/ Part of JayWormNET source code. (C) 2013 Andrey Bobkov (MEDVEDx64).
    Licensed under the Apache License, Version 2.0.  /*/

package org.themassacre.jaywnet;

// Using nProperty library to parse configuration file
import jfork.nproperty.*;
import org.themassacre.util.*;

// Stores common WormNet server settings
// (HTTP port, etc.)

@Cfg public class ConfigurationManager {

	public int HTTPPort		= 80;
	public int IRCPort		= 6667;

	public boolean	useStealthIP	= true;
	public String	stealthIP		= "no.address.for.you";

	// How long game should be kept in the list (in seconds)
	public int gameLifeTime	= 240;
	// IRC users pinging
	public boolean pingsEnabled		= true;
	public int pingInterval			= 60;
	public int pingTimeout			= 60;

	public String serverHost		= "localhost";
	public String networkName		= "";
	public String channelsFileName	= "channels.lst";

	// IRC character encoding
	public String charset			= "native";

	// IRC password settings
	public boolean useIRCPassword	= true;
	public String IRCPassword		= "ELSILRACLIHP";
	public String IRCOperPassword	= "kenny";

	// Login messages configuration
	public int showIntro = 2; 	// 2 shows full intro message,
								// 1 shows only "Welcome, username!"
								// 0 shows nothing.
	public boolean showCreated			= true;
	public boolean showPlayersCount		= true;
	public boolean showChannelsCount	= true;
	public boolean showOps				= false;
	public boolean showCapabilities		= true;
	
	// Server capabilities
	public String capPrefix = "(ov)@+";
	public String capChanTypes = "#";
	public String capChanModes = "b,k,l,imnpst";

	// 'Special' messages
	//		0 - nobody
	//		1 - operators only
	//		2 - anyone
	public int specialMessagesPermissionLevel = 1;
	
	// Anti-flood settings
	public boolean antiFloodEnabled = true;
	public int floodGate = 1000;
	public int floodMaxLevel = 4;
	
	// HTML MOTD
	public boolean	ircShowMOTD			= true;
	public boolean	httpShowMOTD		= true;
	public String	ircMOTDFileName		= "motd.txt";
	public String	httpMOTDFileName 	= "motd.html";

	// Logging settings
	public boolean		loggingEnabled	= false;
	public String		loggingLevel	= "FINER";
	public String		logFile			= "wn.log";

	// IRC announcements options
	public boolean		announceGameHosting		= false;
	public boolean		showOperatorsActions	= true;

	// Lists
	public boolean		enableBanList			= false;
	public boolean		enableWhiteList			= false;
	public String		banListFileName			= "banlist.csv";
	public String		whiteListFileName		= "whitelist.csv";
	public String		commandsListFileName	= "commands.lst";

	// GUI
	public boolean		guiEnabled				= true;
	public String		guiBackgroundColor		= "default";
	public String		guiForegroundColor		= "default";
	
	// Additional in-chat commands
	public boolean		commandsEnabled			= false;
	public boolean		showCommandsInChat		= false;
	public boolean		swallowAllCommands		= false; // even that command doesn't exist, overrides showCommandsInChat
	@Cfg(ignore=true) public final String commandsPackageName = "org.themassacre.jaywnet.cmd"; // specifies where to look up for command classes
	
	// Various stuff
	public boolean		forceHosterIP				= false;
	public boolean		enableSabotageProtection	= false;
	public boolean		enableWheatSnooperSchemeFix	= true;
	public boolean		enableURLSpellCheck			= false; // may be used to avoid snoopers join in
	
	// Experimental stuff
	public int			HTTPFailureSleepTime		= 750;
	public int			IRCFailureSleepTime			= 2500;
	public boolean		IRCSkipBytesWhenAvailable	= false;
	public boolean		dropIRCUserOnOverflow		= false;
	public int			IRCBufferSize				= 262144; // 256K

	@Cfg(ignore=true) private String cfgFileName = null;
	
	// 'cfgFileName' will be loaded automatically once
	// ConfigurationManager is created
	public ConfigurationManager(String cfgFileName) {
		this.cfgFileName = cfgFileName;
		reload();
	}
	
	public void reload() {
		// Attempting to parse the file
		try {
			ConfigParser.parse(this, StreamUtils.getResourceAsStream(cfgFileName, this), ""); // writing results in itself
		} catch(NoClassDefFoundError err) {
			System.err.println("Warning: nProperty classes is not available. Configuration file will not be loaded (using defaults).");
		} catch(Exception e) {
			System.err.println("Warning: can't load configuration file properly ("
					+ cfgFileName + "): " + e);
		}
	}

}
