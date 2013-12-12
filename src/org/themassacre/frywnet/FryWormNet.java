/*/ Part of FryWormNET source code. (C) 2013 Andrey Bobkov (MEDVEDx64).
    Licensed under the Apache License, Version 2.0.  /*/

package org.themassacre.frywnet;

// Main class
public class FryWormNet {

	static ConfigurationManager config;
	public static final String version = "alpha-0.5";
	
	public static void main(String[] args) {
		// Creating configuration storage
		config = new ConfigurationManager("wnet.cfg"); // file name here
		
		// Initializing logging
		WNLogger.start(config);
		WNLogger.l.info("FryWormNET " + version);
		WNLogger.l.info("Server hosthame is '" + config.serverHost + "'");
		
		// Starting servers
		if(config.HTTPPort > 0)		new HTTPServer(config);
		if(config.IRCPort > 0)		new IRCServer(config);
	}

}
