/*/ Part of FryWormNET source code. (C) 2013 Andrey Bobkov (MEDVEDx64).
    Licensed under the Apache License, Version 2.0.  /*/

package org.themassacre.frywnet;

// Using nProperty library to parse configuration file
import jfork.nproperty.*;
import org.themassacre.util.*;

// Stores common WormNet server settings
// (HTTP port, etc.)

@Cfg public class ConfigurationManager {

	public int HTTPPort		= 80;
	public int IRCPort		= 6667;

	// How long game should be kept in the list (in seconds)
	public int gameLifeTime	= 240;
	// IRC users pinging
	public boolean pingsEnabled		= true;
	public int pingInterval			= 60;
	public int pingTimeout			= 60;
	
	public String serverHost		= "heavie";
	public String channelsFileName	= "channels.cfg";
	
	// IRC character encoding
	public String charset			= "windows-1251";
	
	// IRC operator password
	public String IRCOperPassword = "kenny";
	
	// Login messages configuration
	public int showIntro = 2; 	// 2 shows full intro message,
								// 1 shows only "Welcome, username!"
								// 0 shows nothing.
	public boolean showCreated			= true;
	public boolean showPlayersCount		= true;
	public boolean showChannelsCount	= true;
	public boolean showOps				= false;
	public boolean showChops			= true;
	public String wallchopString = "WALLCHOPS PREFIX=(ov)@+ CHANTYPES=#& MAXCHANNELS=20"
			+ "MAXBANS=25 NICKLEN=15 TOPICLEN=120 KICKLEN=90 NETWORK=EFnet"
			+ "CHANMODES=b,k,l,imnpst MODES=4 :are supported by this server";
	
	// HTML MOTD
	public boolean	ircShowMOTD			= true;
	public boolean	httpShowMOTD		= true;
	public String	ircMOTDFileName		= "motd.txt";
	public String	httpMOTDFileName 	= "motd.html";
	
	// Logging settings
	public boolean		loggingEnabled	= true;
	public String		loggingLevel	= "FINER";
	public String		logFile			= "wn.log";
	
	// IRC announcements options
	public boolean		announceGameHosting		= false;
	
	// Various stuff
	public boolean		forceHosterIP				= false;
	public boolean		enableSabotageProtection	= true;
	public boolean		enableWheatSnooperSchemeFix	= true;
	
	// 'cfgFileName' will be loaded automatically once
	// ConfigurationManager is created
	public ConfigurationManager(String cfgFileName) {
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
